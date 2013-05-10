package com.axelor.sn.likedin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;

import org.joda.time.DateTime;

import com.google.code.linkedinapi.client.LinkedInApiClient;
import com.google.code.linkedinapi.client.LinkedInApiClientFactory;
import com.google.code.linkedinapi.client.enumeration.CommentField;
import com.google.code.linkedinapi.client.enumeration.GroupMembershipField;
import com.google.code.linkedinapi.client.enumeration.NetworkUpdateType;
import com.google.code.linkedinapi.client.enumeration.PostField;
import com.google.code.linkedinapi.client.enumeration.ProfileField;
import com.google.code.linkedinapi.client.oauth.LinkedInAccessToken;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthService;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthServiceFactory;
import com.google.code.linkedinapi.client.oauth.LinkedInRequestToken;
import com.google.code.linkedinapi.schema.Comments;
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
import com.google.code.linkedinapi.schema.Comment;

public class LinkedinConnectionClass {
	
	static LinkedInOAuthService oauthService=null;
	LinkedInRequestToken requestToken=null;
	static LinkedInApiClientFactory factory = null;
	LinkedInApiClient client=null;
	final Set<ProfileField> setProfileFields = EnumSet.of(ProfileField.ID, ProfileField.FIRST_NAME,
			ProfileField.LAST_NAME, ProfileField.PUBLIC_PROFILE_URL);
	final Set<NetworkUpdateType> networkUpdateType = EnumSet.of(NetworkUpdateType.SHARED_ITEM);
	final Set<GroupMembershipField> groupFields = EnumSet.of(GroupMembershipField.GROUP_ID,
			GroupMembershipField.MEMBERSHIP_STATE, GroupMembershipField.GROUP_NAME);
	final Set<PostField> postField = EnumSet.of(PostField.ID, PostField.SUMMARY, PostField.TITLE,
			PostField.CREATION_TIMESTAMP, PostField.CREATOR_FIRST_NAME, PostField.CREATOR_LAST_NAME);
	final Set<CommentField> commentField = EnumSet.of(CommentField.ID, CommentField.CREATOR,
			CommentField.CREATION_TIMESTAMP, CommentField.TEXT);
	
	public String getUrl(String consumerKey, String consumerSecret, String redirectUrl, String userName) throws IOException {
		String authUrl = "";
//		try {
			oauthService = LinkedInOAuthServiceFactory.getInstance().createLinkedInOAuthService(consumerKey, consumerSecret);
			requestToken = oauthService.getOAuthRequestToken(redirectUrl);
			factory = LinkedInApiClientFactory.newInstance(consumerKey, consumerSecret);
			authUrl = requestToken.getAuthorizationUrl();
//		} catch (LinkedInOAuthServiceException e) {
//			throw new LinkedInOAuthServiceException("Problem with Linkedin Authorization Service");
//		} catch (LinkedInApiClientException e) {
//			throw new LinkedInApiClientException("Problem with Linkedin Website");
//		}
		File temp = new File(userName+".txt");
		ObjectOutputStream outStream = new ObjectOutputStream( new FileOutputStream(temp));
		outStream.writeObject(requestToken);
		outStream.close();
		return authUrl;
	}

	public String getUserToken(String verifier, String userName) throws IOException, ClassNotFoundException {
		File temp = new File(userName+".txt");
		ObjectInputStream inStream = new ObjectInputStream(new FileInputStream(temp));
		requestToken=(LinkedInRequestToken)inStream.readObject();
		temp.delete();
		inStream.close();
		LinkedInAccessToken accessToken = oauthService.getOAuthAccessToken(requestToken, verifier);
		String userDetails = accessToken.getToken() + "=" + accessToken.getTokenSecret();
		client = factory.createLinkedInApiClient(accessToken);
		Person profile = client.getProfileForCurrentUser(setProfileFields);
		userDetails += "="+ profile.getFirstName() + " " + profile.getLastName();
		return userDetails;
	}
	
	@SuppressWarnings("rawtypes")
	public ArrayList<HashMap> getUserConnections(String userToken, String userTokenSecret) {
		client = factory.createLinkedInApiClient(userToken, userTokenSecret);
		Person profile = client.getProfileForCurrentUser(setProfileFields);
		Connections connections = client.getConnectionsForCurrentUser(setProfileFields);
		
		HashMap<String, String> userDetails = new HashMap<String, String>();
		userDetails.put("userId", profile.getId());
		userDetails.put("userName", profile.getFirstName() + " " + profile.getLastName());
		userDetails.put("userLink", profile.getPublicProfileUrl());
		
		ArrayList<HashMap> users = new ArrayList<HashMap>();
		users.add(userDetails);
		
		for (Person person : connections.getPersonList()) {
			userDetails = new HashMap<String, String>();
			userDetails.put("userId", person.getId());
			userDetails.put("userName", person.getFirstName() + " " + person.getLastName());
			userDetails.put("userLink", person.getPublicProfileUrl());
			users.add(userDetails);
		}
		return users;
	}
	
	public void sendMessage(String userToken, String userTokenSecret, ArrayList<String> lstUserId, String subject, String message) {
		client = factory.createLinkedInApiClient(userToken, userTokenSecret);
		client.sendMessage(lstUserId, subject, message);
	}
	
	public String updateStatus(String userToken, String userTokenSecret, String content) {
		client = factory.createLinkedInApiClient(userToken, userTokenSecret);
		client.updateCurrentStatus(content);

		Updates upd = client.getUserUpdates(networkUpdateType, 0, 1).getUpdates();
		Iterator<Update> itr = upd.getUpdateList().iterator();
		Update update = itr.next();
		
		String updateKeyTime = update.getUpdateKey() + ":" + update.getTimestamp();
		return updateKeyTime;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ArrayList<HashMap> getComments(String userToken, String userTokenSecret, String contentId) {
		client = factory.createLinkedInApiClient(userToken, userTokenSecret);
		
		UpdateComments updateComments = client.getNetworkUpdateComments(contentId);
		Iterator<UpdateComment> itr = updateComments.getUpdateCommentList().iterator();
		UpdateComment comment = null;
		ArrayList<HashMap> commentList = new ArrayList<HashMap>();
		while (itr.hasNext()) {
			comment = itr.next();
			HashMap comments  = new HashMap();
			comments.put("commentId", comment.getId());
			comments.put("comment", comment.getComment());
			comments.put("commentTime", comment.getTimestamp());
			comments.put("fromUser",comment.getPerson().getId());
			commentList.add(comments);
		}
		return commentList;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public HashMap addStatusComment(String userToken, String userTokenSecret, String contentId, String comment) {
		client = factory.createLinkedInApiClient(userToken, userTokenSecret);
		client.postComment(contentId, comment);
		UpdateComments updateComments = client.getNetworkUpdateComments(contentId);
		Iterator<UpdateComment> itr = updateComments.getUpdateCommentList().iterator();
		UpdateComment coment = null;
		HashMap comments = new HashMap();
		while (itr.hasNext()) {
			coment = itr.next();
			if (!itr.hasNext()) {
				comments  = new HashMap();
				comments.put("commentId", coment.getId());
				comments.put("comment", coment.getComment());
				comments.put("commentTime", coment.getTimestamp());
				comments.put("fromUser",coment.getPerson().getId());
			}
		}
		return comments;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ArrayList<HashMap> fetchNetworkUpdates(String userToken, String userTokenSecret, int count, Date startDate, Date endDate) {
		client = factory.createLinkedInApiClient(userToken, userTokenSecret);
		Network network;
		if(count != 0 && startDate == null)
			network = client.getNetworkUpdates(networkUpdateType, 0, count);
		else if(count == 0 && startDate != null) 
			network = client.getNetworkUpdates(networkUpdateType, startDate, endDate);
		else if(count != 0 && startDate != null)
			network = client.getNetworkUpdates(networkUpdateType, 0, count, startDate, endDate);
		else
			network = client.getNetworkUpdates(networkUpdateType, 0, 15);
		
		Iterator<Update> itr = network.getUpdates().getUpdateList().iterator();
		Update update = null;
		HashMap networkUpdates = new HashMap();
		ArrayList networkUpdatesList = new ArrayList();
		while (itr.hasNext()) {
			update = itr.next();
			if (update.getUpdateContent().getPerson().getCurrentShare().getComment() == null)
				continue;
			networkUpdates = new HashMap();
			networkUpdates.put("contentId", update.getUpdateKey());
			networkUpdates.put("content", update.getUpdateContent().getPerson().getCurrentShare().getComment());
			networkUpdates.put("timeStamp", update.getTimestamp());
			networkUpdates.put("fromUser", update.getUpdateContent().getPerson().getId());
			networkUpdatesList.add(networkUpdates);
		}
		return networkUpdatesList;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ArrayList<HashMap> getMemberships(String userToken, String userTokenSecret) {
		client = factory.createLinkedInApiClient(userToken, userTokenSecret);
		GroupMemberships memberships = client.getGroupMemberships(groupFields);
		ArrayList groupMembers = new ArrayList();
		HashMap member = new HashMap();
		for(GroupMembership membership : memberships.getGroupMembershipList()) {
			member = new HashMap();
			member.put("groupId", membership.getGroup().getId());
			member.put("groupName", membership.getGroup().getName());
			member.put("membershipState", membership.getMembershipState().getCode().toString());
			groupMembers.add(member);
		}
		return groupMembers;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ArrayList<HashMap> getDiscussions(String userToken, String userTokenSecret, String groupId, int count, Date modifiedDate) {
		client = factory.createLinkedInApiClient(userToken, userTokenSecret);
		Posts post = null;
		if(count != 0 && modifiedDate == null ) 
			post = client.getPostsByGroup(groupId, postField, 0, count);
		else if(count != 0 && modifiedDate != null)
			post = client.getPostsByGroup(groupId, postField, 0, count, modifiedDate);
		else
			post = client.getPostsByGroup(groupId, postField, 0, 15);
		
		ArrayList<HashMap> groupDiscussions = new ArrayList<HashMap>();
		HashMap discussion;
		
		for (Post p : post.getPostList()) {
			discussion=new HashMap();
			discussion.put("id", p.getId());
			if(p.getTitle() != null)
				discussion.put("title", p.getTitle());
			else
				discussion.put("title", "");
			
			if(p.getSummary() != null)
				discussion.put("summary", p.getSummary());
			else
				discussion.put("summary", "");
			discussion.put("by", p.getCreator().getFirstName() + " " + p.getCreator().getLastName());
			discussion.put("time",p.getCreationTimestamp());
			
			groupDiscussions.add(discussion);
		}
		return groupDiscussions;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public HashMap addGroupDiscussion(String userToken, String userTokenSecret, String groupId, String title, String summary) {
		client = factory.createLinkedInApiClient(userToken, userTokenSecret);
		client.createPost(groupId, title, summary);
		
		Posts post = client.getPostsByGroup(groupId, postField, 0, 1);
		HashMap discussionIdTime = new HashMap();
		discussionIdTime.put("id",post.getPostList().get(0).getId());
		discussionIdTime.put("time",post.getPostList().get(0).getCreationTimestamp());
		discussionIdTime.put("by", post.getPostList().get(0).getCreator().getFirstName()
				+ " " + post.getPostList().get(0).getCreator().getLastName());
		
		return discussionIdTime;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ArrayList<HashMap> getDiscussionComments(String userToken, String userTokenSecret, String discussionId) {
		client = factory.createLinkedInApiClient(userToken, userTokenSecret);
		Comments cmnt = client.getPostComments(discussionId, commentField);
		Comments comments = client.getPostComments(discussionId, commentField, 0, Integer.parseInt(cmnt.getTotal().toString()) );
		ArrayList<HashMap> commentList = new ArrayList<HashMap>();
		HashMap comment;
		for(int i = 0; i < comments.getTotal(); i++) {
			Comment c = comments.getCommentList().get(i);
			comment = new HashMap();
			comment.put("id", c.getId());
			comment.put("text", c.getText());
			comment.put("time", c.getCreationTimestamp());
			comment.put("by", c.getCreator().getFirstName() + " "+ c.getCreator().getLastName());
			commentList.add(comment);
		}
		return commentList;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public HashMap addDiscussionComment(String userToken, String userTokenSecret, String discussionId, String comment) {
		client = factory.createLinkedInApiClient(userToken, userTokenSecret);
		client.addPostComment(discussionId, comment);
		Comments cmnt = client.getPostComments(discussionId, commentField);
		Comments comments = client.getPostComments(discussionId, commentField, Integer.parseInt(cmnt.getTotal().toString()) - 1, 1);
		HashMap commentMap = new HashMap();
		Comment c = comments.getCommentList().get(0);
		commentMap.put("id", c.getId());
		commentMap.put("text", c.getText());
		commentMap.put("time", c.getCreationTimestamp());
		commentMap.put("by", c.getCreator().getFirstName() + " "+ c.getCreator().getLastName());
		return commentMap;
	}
	
	public boolean deleteDiscussion(String userToken, String userTokenSecret, String discussionId) {
		boolean status = false;
		client = factory.createLinkedInApiClient(userToken, userTokenSecret);
		try {
			client.deletePost(discussionId);
			status = true;
		}
		catch(Exception e) {
			
		}
		return status;	
	}
}
