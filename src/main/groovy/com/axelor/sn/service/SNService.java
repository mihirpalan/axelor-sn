package com.axelor.sn.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.sn.db.Comments;
import com.axelor.sn.db.NetworkUpdates;
import com.axelor.sn.db.PostUpdates;
import com.axelor.sn.db.ApplicationCredentials;
import com.axelor.sn.db.GroupDiscussion;
import com.axelor.sn.db.GroupDiscussionComments;
import com.axelor.sn.db.GroupMember;
import com.axelor.sn.db.ImportContact;
import com.axelor.sn.db.LinkedinParameters;
import com.axelor.sn.db.PersonalCredential;
import com.axelor.sn.db.SocialNetworking;
import com.axelor.sn.likedin.LinkedinConnectionClass;
import com.gargoylesoftware.htmlunit.html.xpath.LowerCaseFunction;
import com.google.code.linkedinapi.client.LinkedInApiClient;
import com.google.code.linkedinapi.client.LinkedInApiClientException;
import com.google.code.linkedinapi.client.LinkedInApiClientFactory;
import com.google.code.linkedinapi.client.enumeration.CommentField;
import com.google.code.linkedinapi.client.enumeration.GroupMembershipField;
import com.google.code.linkedinapi.client.enumeration.NetworkUpdateType;
import com.google.code.linkedinapi.client.enumeration.PostField;
import com.google.code.linkedinapi.client.enumeration.ProfileField;
import com.google.code.linkedinapi.client.oauth.LinkedInAccessToken;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthService;
import com.google.code.linkedinapi.client.oauth.LinkedInRequestToken;
import com.google.code.linkedinapi.schema.Connections;
import com.google.code.linkedinapi.schema.GroupMembership;
import com.google.code.linkedinapi.schema.GroupMemberships;
import com.google.code.linkedinapi.schema.Network;
import com.google.code.linkedinapi.schema.Person;
import com.google.code.linkedinapi.schema.Post;
import com.google.code.linkedinapi.schema.Posts;
import com.google.code.linkedinapi.schema.Update;
import com.google.code.linkedinapi.schema.UpdateComment;
import com.google.code.linkedinapi.schema.UpdateComments;
import com.google.code.linkedinapi.schema.Updates;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import org.joda.time.DateTime;

public class SNService {
	
	@Inject
	LinkedinConnectionClass LinkedinConnect;
	
	static LinkedInApiClient client = null;
	static LinkedInApiClientFactory factory = null;
	static LinkedInAccessToken accessToken = null;
	static LinkedInRequestToken requestToken = null;
	static LinkedInOAuthService oauthService = null;
	static SocialNetworking socialType = null;
	static String consumerKeyValue = null;
	static String consumerSecretValue = null;
	SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
	
	public SocialNetworking getSocialType() {
		return socialType;
	}

	public void setSocialType(SocialNetworking snType) {
		socialType = snType;
	}

	public SocialNetworking getSnType(String sntype) {
		SocialNetworking snType = SocialNetworking.all().filter("lower(name)= ?", sntype.toLowerCase()).fetchOne();
		setSocialType(snType);
		return snType;
	}

	public PersonalCredential getPersonalCredential(User user,	SocialNetworking snType) {
			PersonalCredential personalCredential = PersonalCredential.all().filter("userId=? and snType=?", user, snType).fetchOne();
		return personalCredential;
	}

	public ApplicationCredentials getApplicationCredential(	SocialNetworking snType) {
		ApplicationCredentials credential = ApplicationCredentials.all().filter("snType=?", snType).fetchOne();
		return credential;
	}

	public String getUrl(User user, SocialNetworking snType) throws Exception {
		String authUrl = "";
		PersonalCredential personalCredential = getPersonalCredential(user, snType);
		if (personalCredential == null) {
			try {
				ApplicationCredentials applicationCredentials = ApplicationCredentials
						.all().filter("snType=?", snType).fetchOne();
				String redirectUrl = applicationCredentials.getRedirectUrl()
						+ "/" + user.getId();
				authUrl = LinkedinConnect.getUrl(applicationCredentials.getApikey(),
						applicationCredentials.getApisecret(), redirectUrl, user.getName());
				setSocialType(snType);
			} catch (NullPointerException e) {
				throw new Exception(e.toString());
			}
		} else {
			throw new Exception("You Already have One Account Associated...");
		}
		return authUrl;
	}

	@Transactional
	public boolean getUserToken( String verifier, User user) throws Exception {
		boolean status = false;
		
		String userDetails = LinkedinConnect.getUserToken(verifier, user.getName());
		String[] details = userDetails.split("=");
		String token = details[0];
		String tokenSecret = details[1];
		String name = details[2];
		try {
			PersonalCredential personalCredential = new PersonalCredential();
			personalCredential.setUserToken(token);
			personalCredential.setUserTokenSecret(tokenSecret);
			personalCredential.setSnUserName(name);
			personalCredential.setUserId(user);
			personalCredential.setSnType(getSocialType());
			personalCredential.merge();
			status = true;
		} catch (Exception e) {
			throw new Exception("There's Some Problem. Not Authorised.");
		}
		return status;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Transactional
	public String fetchConnections(User user) throws Exception {
		EntityManager em = JPA.em();
		SocialNetworking snType = SocialNetworking.all().filter("lower(name)= ?", "linkedin").fetchOne();
		if (snType == null)
			throw new Exception("Network Type Not Found");
		else {
			PersonalCredential personalCredential = PersonalCredential.all().filter("userId=? and snType=?", user, snType).fetchOne();
			if (personalCredential == null)
				throw new Exception("Please Login First");
			else {
				ApplicationCredentials applicationCredential = ApplicationCredentials.all().filter("snType=?", snType).fetchOne();
				if (applicationCredential == null)
					throw new Exception("No Application Defined");
				else {
					String userToken = personalCredential.getUserToken();
					String userTokenSecret = personalCredential.getUserTokenSecret();
					ArrayList<HashMap> users = LinkedinConnect.getUserConnections(userToken, userTokenSecret);
					List<String> lstUserId = (List<String>) em.createQuery("SELECT a.userId FROM ImportContact a WHERE a.curUser="
									+ user.getId() + " AND a.snType="+ snType.getId()).getResultList();
					HashMap<String, String> userDetails = new HashMap<String, String>();
					for (int i = 0; i < users.size(); i++) {
						userDetails = (HashMap<String, String>) users.get(i);
						if (!lstUserId.contains(userDetails.get("userId").toString())) {
							ImportContact contact = new ImportContact();
							contact.setUserId(userDetails.get("userId"));
							contact.setName(userDetails.get("userName"));
							contact.setSnType(snType);
							contact.setCurUser(user);
							contact.setLink(userDetails.get("userLink"));
							contact.persist();
						}
					}
					return "Contacts Imported Successfully.";
				}
			}
		}
	}

	public String sendMessage(String userId, String subject, String message, User user) throws Exception {
		SocialNetworking snType = SocialNetworking.all().filter("lower(name)= ?", "linkedin").fetchOne();
		if (snType == null)
			throw new Exception("Network Type Not Found");
		else {
			PersonalCredential personalCredential = PersonalCredential.all().filter("userId=? and snType=?", user, snType).fetchOne();
			if (personalCredential == null)
				throw new Exception("Please Login First");
			else {
				ApplicationCredentials applicationCredential = ApplicationCredentials.all().filter("snType=?", snType).fetchOne();
				if (applicationCredential == null)
					throw new Exception("No Application Defined");
				else {
					String userToken = personalCredential.getUserToken();
					String userTokenSecret = personalCredential.getUserTokenSecret();
					ArrayList<String> lstUserId = new ArrayList<String>();
					lstUserId.add(userId);
					LinkedinConnect.sendMessage(userToken, userTokenSecret, lstUserId, subject, message);
					return "Message Sent Successfully.";
				}
			}
		}
	}

	public String updateStatus(String content, User user) throws Exception {
		SocialNetworking snType = SocialNetworking.all().filter("lower(name)= ?", "linkedin").fetchOne();
		if (snType == null)
			throw new Exception("Network Type Not Found");
		else {
			PersonalCredential personalCredential = PersonalCredential.all().filter("userId=? and snType=?", user, snType).fetchOne();
			if (personalCredential == null)
				throw new Exception("Please Login First");
			else {
				ApplicationCredentials applicationCredential = ApplicationCredentials.all().filter("snType=?", snType).fetchOne();
				if (applicationCredential == null)
					throw new Exception("No Application Defined");
				else {
					String userToken = personalCredential.getUserToken();
					String userTokenSecret = personalCredential.getUserTokenSecret();
					String updateKeyTime = LinkedinConnect.updateStatus(userToken, userTokenSecret, content);
					return updateKeyTime;
				}
			}
		}
	}

	@SuppressWarnings({  "unchecked", "rawtypes" })
	@Transactional
	public void getComments(String contentId, User user) throws Exception {
		EntityManager em = JPA.em();
		
		SocialNetworking snType = SocialNetworking.all().filter("lower(name)= ?", "linkedin").fetchOne();
		if (snType == null)
			throw new Exception("Network Type Not Found");
		else {
			PersonalCredential personalCredential = PersonalCredential.all().filter("userId=? and snType=?", user, snType).fetchOne();
			if (personalCredential == null)
				throw new Exception("Please Login First");
			else {
				ApplicationCredentials applicationCredential = ApplicationCredentials.all().filter("snType=?", snType).fetchOne();
				if (applicationCredential == null)
					throw new Exception("No Application Defined");
				else {
					String userToken = personalCredential.getUserToken();
					String userTokenSecret = personalCredential.getUserTokenSecret();
					ArrayList<HashMap> commentList = LinkedinConnect.getComments(userToken, userTokenSecret, contentId);

					List<String> lstUserId = (List<String>) em.createQuery("SELECT a.userId FROM ImportContact a WHERE a.curUser="
							+ user.getId() + " AND a.snType="+ snType.getId()).getResultList();

					PostUpdates postUpdate = PostUpdates.all().filter("contentId=?", contentId).fetchOne();
					
					List<String> lstCommentsId = em.createQuery("SELECT a.commentId FROM Comments a WHERE a.curUser="
							+ user.getId() + " AND a.contentId=" + postUpdate.getId()).getResultList();

					HashMap comments = new HashMap();
					for(int i = 0; i < commentList.size(); i++) {
						comments = commentList.get(i);
						if(lstUserId.contains(comments.get("fromUser").toString())) {
							if(!lstCommentsId.contains(comments.get("commentId").toString())) {
								Comments comment = new Comments();
								ImportContact contact = ImportContact.all().filter("userId=? and curUser=?",
										comments.get("fromUser").toString(), user).fetchOne();
								DateTime date = new DateTime(comments.get("commentTime"));
								comment.setContentId(postUpdate);
								comment.setCommentId(comments.get("commentId").toString());
								comment.setComment(comments.get("comment").toString());
								comment.setCommentTime(date);
								comment.setCurUser(user);
								comment.setFromUser(contact);
								comment.persist();
							}
						}
					}
				}
			}
		}
	}

	@SuppressWarnings({ "rawtypes" })
	@Transactional
	public void addStatusComment(User user, String contentId, String comment) throws Exception {
		SocialNetworking snType = SocialNetworking.all().filter("lower(name)= ?", "linkedin").fetchOne();
		if (snType == null)
			throw new Exception("Network Type Not Found");
		else {
			PersonalCredential personalCredential = PersonalCredential.all().filter("userId=? and snType=?", user, snType).fetchOne();
			if (personalCredential == null)
				throw new Exception("Please Login First");
			else {
				ApplicationCredentials applicationCredential = ApplicationCredentials.all().filter("snType=?", snType).fetchOne();
				if (applicationCredential == null)
					throw new Exception("No Application Defined");
				else {
					String userToken = personalCredential.getUserToken();
					String userTokenSecret = personalCredential.getUserTokenSecret();
					HashMap comments = LinkedinConnect.addStatusComment(userToken, userTokenSecret, contentId, comment);
					PostUpdates postUpdate = PostUpdates.all().filter("contentId=?", contentId).fetchOne();
					ImportContact contact = ImportContact.all().filter("userId=? and curUser=?", comments.get("fromUser").toString(), user).fetchOne();
					
					Comments commentObject = new Comments();
					DateTime date = new DateTime(comments.get("commentTime"));
					commentObject.setContentId(postUpdate);
					commentObject.setCommentId(comments.get("commentId").toString());
					commentObject.setComment(comments.get("comment").toString());
					commentObject.setCommentTime(date);
					commentObject.setCurUser(user);
					commentObject.setFromUser(contact);
					commentObject.persist();
				}
			}
		}
	}

	@SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
	@Transactional
	public String fetchNetworkUpdates(User user) throws Exception {
		EntityManager em = JPA.em();
		
		SocialNetworking snType = SocialNetworking.all().filter("lower(name)= ?", "linkedin").fetchOne();
		if (snType == null)
			throw new Exception("Network Type Not Found");
		else {
			PersonalCredential personalCredential = PersonalCredential.all().filter("userId=? and snType=?", user, snType).fetchOne();
			if (personalCredential == null)
				throw new Exception("Please Login First");
			else {
				ApplicationCredentials applicationCredential = ApplicationCredentials.all().filter("snType=?", snType).fetchOne();
				if (applicationCredential == null)
					throw new Exception("No Application Defined");
				else {
					String userToken = personalCredential.getUserToken();
					String userTokenSecret = personalCredential.getUserTokenSecret();
					LinkedinParameters parameters = LinkedinParameters.all().filter("curUser=?", user).fetchOne();
					int count = 0;
					Date startDate = null, endDate = null;
					if (parameters == null) {
						count = 0;
						startDate = null;
						endDate = null;
					}
					else if ((parameters.getDays() != 0) && (parameters.getRecordNumbers() == 0)) {
						endDate = new Date();
						Calendar c = Calendar.getInstance();
						c.add(Calendar.DATE, -(parameters.getDays()));
						startDate = new Date(sdf.format(c.getTime()));
					}
					else if ((parameters.getDays() == 0) && (parameters.getRecordNumbers() != 0)) {
						startDate = null;
						endDate = null;
						count = parameters.getRecordNumbers();
					}
					else if ((parameters.getDays() != 0) && (parameters.getRecordNumbers() != 0)) {
						endDate = new Date();
						Calendar c = Calendar.getInstance();
						c.add(Calendar.DATE, -(parameters.getDays()));
						startDate = new Date(sdf.format(c.getTime()));
						count = parameters.getRecordNumbers();
					}
					else {
						count = 0;
						startDate = null;
						endDate = null;
					}
					ArrayList<HashMap> networkUpdatesList = LinkedinConnect.fetchNetworkUpdates(userToken, userTokenSecret, count, startDate, endDate);
					
					List<String> lstUserId = (List<String>) em.createQuery("SELECT a.userId FROM ImportContact a WHERE a.curUser="
							+ user.getId() + " AND a.snType="+ snType.getId()).getResultList();
					
					List<String> lstNetworkUpdateIds = (List<String>) em.createQuery("SELECT a.contentId FROM NetworkUpdates a WHERE a.curUser="
							+ user.getId()).getResultList();
					
					HashMap networkUpdate = new HashMap();
					for(int i = 0; i < networkUpdatesList.size(); i++) {
						networkUpdate = networkUpdatesList.get(i);
						if (lstUserId.contains(networkUpdate.get("fromUser").toString())) {
							if (!lstNetworkUpdateIds.contains(networkUpdate.get("contentId").toString())) {
								NetworkUpdates networkUpdateObject = new NetworkUpdates();
								ImportContact fromUser = ImportContact.all().filter("userId=? and curUser=?",networkUpdate.get("fromUser").toString(), user).fetchOne();
								DateTime date = new DateTime(networkUpdate.get("timeStamp"));
								networkUpdateObject.setContentId(networkUpdate.get("contentId").toString());
								networkUpdateObject.setContent(networkUpdate.get("content").toString());
								networkUpdateObject.setCurUser(user);
								networkUpdateObject.setContentTime(date);
								networkUpdateObject.setSnType(snType);
								networkUpdateObject.setFromUser(fromUser);
								networkUpdateObject.persist();
							}
						}
					}
					return "Network Updates Fetched...";
				}
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Transactional
	public String getMembership(User user) throws Exception {
		EntityManager em = JPA.em();
		
		SocialNetworking snType = SocialNetworking.all().filter("lower(name)= ?", "linkedin").fetchOne();
		if (snType == null)
			throw new Exception("Network Type Not Found");
		else {
			PersonalCredential personalCredential = PersonalCredential.all().filter("userId=? and snType=?", user, snType).fetchOne();
			if (personalCredential == null)
				throw new Exception("Please Login First");
			else {
				ApplicationCredentials applicationCredential = ApplicationCredentials.all().filter("snType=?", snType).fetchOne();
				if (applicationCredential == null)
					throw new Exception("No Application Defined");
				else {
					String userToken = personalCredential.getUserToken();
					String userTokenSecret = personalCredential.getUserTokenSecret();
					ArrayList<HashMap> groupMembers = LinkedinConnect.getMemberships(userToken, userTokenSecret);
					List<String> lstGroupIds = em.createQuery("SELECT a.groupId FROM GroupMember a WHERE a.curUser=" + user.getId()).getResultList();
					HashMap members = new HashMap();
					for(int i = 0; i < groupMembers.size(); i++) {
						members = groupMembers.get(i);
						if (!lstGroupIds.contains(members.get("groupId").toString())) {
							GroupMember groupMember = new GroupMember();
							groupMember.setGroupId(members.get("groupId").toString());
							groupMember.setGroupName(members.get("groupName").toString());
							groupMember.setMembershipState(members.get("membershipState").toString());
							groupMember.setCurUser(user);
							groupMember.setSnType(snType);
							
							groupMember.persist();
						}
					}
					return "Group Memberships Obtained...";
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	@Transactional
	static void getDiscussions(String userToken, String userTokenSecret, String consumerKeyValue, String consumerSecretValue, User user,
			GroupMember groupMember, SocialNetworking snType) {
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue);
		client = factory.createLinkedInApiClient(userToken, userTokenSecret);

		LinkedinParameters parameters = null;
		Posts post = null;
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

		List<GroupDiscussion> lstGroupDiscussion = GroupDiscussion.all().filter("curUser=? and groupName=?", user, groupMember).fetch();
		List<String> lstGroupDiscussionIds = new ArrayList<String>();
		for (GroupDiscussion d : lstGroupDiscussion)
			lstGroupDiscussionIds.add(d.getDiscussionId().toString());

		Set<PostField> postField = EnumSet.of(PostField.ID, PostField.SUMMARY,
				PostField.TITLE, PostField.TYPE, PostField.CREATION_TIMESTAMP,
				PostField.CREATOR_FIRST_NAME, PostField.CREATOR_LAST_NAME);
		try {
			parameters = LinkedinParameters.all().filter("curUser=? and snType=?", user, snType).fetchOne();
			if (parameters == null)
				post = client.getPostsByGroup(groupMember.getGroupId(), postField, 0, 15);
			else if (parameters.getRecordNumbers() == 0)
				post = client.getPostsByGroup(groupMember.getGroupId(), postField, 0, 15);
			else if (parameters.getRecordNumbers() != 0) {
				if (parameters.getDays() != 0) {
					Calendar c = Calendar.getInstance();
					c.add(Calendar.DATE, -(parameters.getDays()));
					Date modified = new Date(sdf.format(c.getTime()));
					post = client.getPostsByGroup(groupMember.getGroupId(), postField, 0, parameters.getRecordNumbers(), modified);
				} else {
					post = client.getPostsByGroup(groupMember.getGroupId(), postField, 0, parameters.getRecordNumbers());
				}
			}
		} catch (NullPointerException e) {
			return;
		}

		for (Post p : post.getPostList()) {
			if (!lstGroupDiscussionIds.contains(p.getId())) {

				DateTime date = new DateTime(p.getCreationTimestamp());
				GroupDiscussion discussion = new GroupDiscussion();

				discussion.setDiscussionId(p.getId());
				if (p.getSummary() != null)
					discussion.setDiscussionSummary(p.getSummary());
				else
					discussion.setDiscussionSummary("N/A");
				
				if (p.getTitle() != null)
					discussion.setDiscussionTitle(p.getTitle());
				else
					discussion.setDiscussionTitle("N/A");
				
				discussion.setDiscussionBy(p.getCreator().getFirstName() + " "
						+ p.getCreator().getLastName());
				discussion.setDiscussionTime(date);
				discussion.setDiscussionByCurrentUser(false);
				discussion.setGroupName(groupMember);
				discussion.setCurUser(user);
				discussion.persist();
			}
		}
	}

	static String addGroupDiscussion(String userToken, String userTokenSecret, String consumerKeyValue, String consumerSecretValue, String title,
			String summary, String groupId) {
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue);
		client = factory.createLinkedInApiClient(userToken, userTokenSecret);
		client.createPost(groupId, title, summary);

		Set<PostField> postField = EnumSet.of(PostField.ID, PostField.SUMMARY,
				PostField.TITLE, PostField.TYPE, PostField.CREATION_TIMESTAMP,
				PostField.CREATOR_FIRST_NAME, PostField.CREATOR_LAST_NAME);

		Posts post = client.getPostsByGroup(groupId, postField, 0, 1);
		String discussionIdTime = post.getPostList().get(0).getId() + ":"
				+ post.getPostList().get(0).getCreationTimestamp() + ":"
				+ post.getPostList().get(0).getCreator().getFirstName() + " "
				+ post.getPostList().get(0).getCreator().getLastName();
		return discussionIdTime;
	}

	static void addDiscussionComment(String userToken, String userTokenSecret, String consumerKeyValue, String consumerSecretValue, User user,
			GroupDiscussion groupDiscussion, String discussionId, String comment, int start, SocialNetworking snType) {
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue);
		client = factory.createLinkedInApiClient(userToken, userTokenSecret);
		client.addPostComment(discussionId, comment);

		getDiscussionComments(userToken, userTokenSecret, consumerKeyValue,
				consumerSecretValue, user, groupDiscussion, snType, start);
	}

	@Transactional
	static void getDiscussionComments(String userToken, String userTokenSecret, String consumerKeyValue, String consumerSecretValue, User user,
			GroupDiscussion groupDiscussion, SocialNetworking snType, int start) {
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue);
		client = factory.createLinkedInApiClient(userToken, userTokenSecret);

		LinkedinParameters parameters = null;
		List<GroupDiscussionComments> lstGroupDiscussionComments = null;
		com.google.code.linkedinapi.schema.Comments comments = null;

		lstGroupDiscussionComments = GroupDiscussionComments.all().filter("curUser=? and discussion=?", user, groupDiscussion).fetch();
		List<String> lstCommentIds = new ArrayList<String>();

		for (int i = 0; i < lstGroupDiscussionComments.size(); i++)
			lstCommentIds.add(lstGroupDiscussionComments.get(i).getCommentId());

		Set<CommentField> commentField = EnumSet.of(CommentField.ID,
				CommentField.CREATOR, CommentField.CREATION_TIMESTAMP,
				CommentField.TEXT);
		try {
			parameters = LinkedinParameters.all().filter("curUser=? and snType=?", user, snType).fetchOne();
			if (parameters == null) {
				comments = client.getPostComments(groupDiscussion.getDiscussionId(), commentField, start, 10);
			} else if (parameters.getRecordNumbers() == 0) {
				comments = client.getPostComments(groupDiscussion.getDiscussionId(), commentField, start, 10);
			} else if (parameters.getRecordNumbers() != 0) {
				comments = client.getPostComments( groupDiscussion.getDiscussionId(), commentField, start, parameters.getRecordNumbers());
			}
		} catch (NullPointerException e) {
			return;
		}

		for (com.google.code.linkedinapi.schema.Comment comment : comments.getCommentList()) {
			if (!lstCommentIds.contains(comment.getId())) {
				GroupDiscussionComments groupDiscussionComment = new GroupDiscussionComments();
				DateTime date = new DateTime(comment.getCreationTimestamp());
				groupDiscussionComment.setCommentId(comment.getId());
				groupDiscussionComment.setComment(comment.getText());
				groupDiscussionComment.setCommentTime(date);
				groupDiscussionComment.setByUser(comment.getCreator().getFirstName()
						+ " "+ comment.getCreator().getLastName());
				groupDiscussionComment.setCurUser(user);
				groupDiscussionComment.setDiscussion(groupDiscussion);
				groupDiscussionComment.persist();
			}
		}
	}

	@Transactional
	static String deleteDiscussion(List<Integer> lstIdValues, String userToken, String userTokenSecret, String consumerKeyValue,
			String consumerSecretValue, User user) {
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue);
		client = factory.createLinkedInApiClient(userToken, userTokenSecret);

		EntityManager em = JPA.em();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		String message = "";

		for (int i = 0; i < lstIdValues.size(); i++) {
			GroupDiscussion groupDiscussion = GroupDiscussion.all().filter("id=?", lstIdValues.get(i)).fetchOne();
			try {
				client.deletePost(groupDiscussion.getDiscussionId()); 
				em.remove(groupDiscussion);
				message = "Deleted Successfully...";
			} catch (LinkedInApiClientException e) {
				message = "Something Went Wrong...: " + e.getMessage();
			}
		}
		tx.commit();
		return message;
	}

	static List<Comments> refreshComments(PostUpdates postUpdates) {
		List<Comments> lstComment = Comments.all().filter("contentId=?", postUpdates).fetch();
		return lstComment;
	}

	static List<GroupDiscussion> refreshDiscussions(GroupMember member) {
		List<GroupDiscussion> lstDiscussion = GroupDiscussion.all().filter("groupName=?", member).fetch();
		return lstDiscussion;
	}

	static List<GroupDiscussionComments> refreshDiscussionComments(GroupDiscussion discussion) {
		List<GroupDiscussionComments> lstGroupDiscussionComment = GroupDiscussionComments.all().filter("discussion=?", discussion).fetch();
		return lstGroupDiscussionComment;
	}
}
