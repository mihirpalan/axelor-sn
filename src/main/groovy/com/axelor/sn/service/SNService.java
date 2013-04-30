package com.axelor.sn.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.sn.db.Comments;
import com.axelor.sn.db.NetworkUpdates;
import com.axelor.sn.db.PostUpdates;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.sn.db.ApplicationCredentials;
import com.axelor.sn.db.DirectMessages;
import com.axelor.sn.db.GroupDiscussion;
import com.axelor.sn.db.GroupDiscussionComments;
import com.axelor.sn.db.GroupMember;
import com.axelor.sn.db.ImportContact;
//import com.axelor.sn.db.ImportContacts;
import com.axelor.sn.db.LinkedinParameters;
import com.axelor.sn.db.PersonalCredential;
import com.axelor.sn.db.SocialNetworking;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
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
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthServiceFactory;
import com.google.code.linkedinapi.client.oauth.LinkedInRequestToken;
import com.google.code.linkedinapi.schema.Comment;
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
import com.google.code.linkedinapi.schema.impl.UpdateCommentsImpl;
import com.google.inject.persist.Transactional;

import org.joda.time.DateTime;
import org.scribe.model.Verifier;

public class SNService
{
	static LinkedInApiClient client=null;
	static LinkedInApiClientFactory factory=null;
	static LinkedInAccessToken accessToken=null;
	static LinkedInRequestToken requestToken=null;
	static LinkedInOAuthService oauthService=null;
	static User currentUser=null;
	static SocialNetworking snType=null;
	static String consumerKeyValue = null;
	static String consumerSecretValue=null;
	
	public static SocialNetworking getSnType() {
		return snType;
	}

	public static void setSnType(SocialNetworking snType) {
		SNService.snType = snType;
	}

	public static User getCurrentUser() {
		return currentUser;
	}

	public static void setCurrentUser(User currentUser) {
		SNService.currentUser = currentUser;
	}

	@Transactional
	static String getUrl(String consumerKey,String consumerSecret,User user,SocialNetworking snType)
	{
		String authUrl=null;
		
		try
		{
			consumerKeyValue = consumerKey;
			consumerSecretValue = consumerSecret;
			ApplicationCredentials applicationCredentials=ApplicationCredentials.all().filter("snType=?", snType).fetchOne();
			oauthService=LinkedInOAuthServiceFactory.getInstance().createLinkedInOAuthService(consumerKeyValue, consumerSecretValue);
			requestToken=oauthService.getOAuthRequestToken(applicationCredentials.getRedirectUrl());
//			requestToken=oauthService.getOAuthRequestToken("http://localhost:8080/axelor-demo/ws/linkedin/100");
			authUrl=requestToken.getAuthorizationUrl();
			setCurrentUser(user);
			setSnType(snType);
			
		}
		catch(Exception e)
		{
			System.out.println(e.toString());
		}
		return authUrl;
	}
	
	public static boolean getUserToken(String verifier ) throws Exception
	{
		boolean status=false;
		EntityManager em=JPA.em();
		EntityTransaction tx=em.getTransaction();
		tx.begin();
		
		User user=getCurrentUser();
		SocialNetworking snType=getSnType();
		
		try
		{
		
			accessToken = oauthService.getOAuthAccessToken(requestToken,verifier);
			
			factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue);
			client=factory.createLinkedInApiClient(accessToken.getToken(),accessToken.getTokenSecret());
			
			PersonalCredential personalCredential=new PersonalCredential();
			
			personalCredential.setUserToken(accessToken.getToken());
			personalCredential.setUserTokenSecret(accessToken.getTokenSecret());
			Person profile= client.getProfileForCurrentUser(EnumSet.of(ProfileField.FIRST_NAME,ProfileField.LAST_NAME));
			personalCredential.setSnUserName(profile.getFirstName()+" "+profile.getLastName());
			personalCredential.setUserId(user);
			personalCredential.setSnType(snType);

			personalCredential.merge();
			status=true;
		}
		catch(Exception e)
		{
			throw new Exception("There's Some Problem. Not Authorised.");
		}
		tx.commit();
		return status;
	}
	
	static SocialNetworking getSnType(String sntype)
	{
		SocialNetworking snType=SocialNetworking.all().filter("lower(name)= ?", sntype.toLowerCase()).fetchOne();
		return snType;
	}
	
	static PersonalCredential getPersonalCredential(User user,SocialNetworking snType)
	{
		PersonalCredential query=null;
		try
		{
			query=PersonalCredential.all().filter("userId=? and snType=?", user,snType).fetchOne();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return query;
	}
	
	static ApplicationCredentials getApplicationCredential(SocialNetworking snType)
	{
		ApplicationCredentials credential=ApplicationCredentials.all().filter("snType=?",snType).fetchOne();
		return credential;
	}
	
	static void fetchConnections(String consumerKeyValue,String consumerSecretValue, String userToken,String userTokenSecret,User user,SocialNetworking snType)
	{
//		EntityManager em=JPA.em();
//		EntityTransaction tx=em.getTransaction();
//		tx.begin();
		
		List<ImportContact> lstImportContact=ImportContact.all().filter("curUser=? and snType=?",user,snType).fetch();
		
		List<String> lstUserId=new ArrayList<String>();
				
		for(int i=0;i<lstImportContact.size();i++)
			lstUserId.add(lstImportContact.get(i).getUserId());
		
		try
		{
			factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue);
			client=factory.createLinkedInApiClient(userToken,userTokenSecret);
			
			final Set<ProfileField> setProfileFields = EnumSet.of(ProfileField.ID,ProfileField.FIRST_NAME,ProfileField.LAST_NAME,
																	ProfileField.PUBLIC_PROFILE_URL);
						
			Person ownProfile=client.getProfileForCurrentUser(setProfileFields);
			if(!lstUserId.contains(ownProfile.getId()))
			{
				ImportContact cntct=new ImportContact();
				cntct.setUserId(ownProfile.getId());
				cntct.setName(ownProfile.getFirstName()+" "+ownProfile.getLastName());
				cntct.setSnType(snType);
				cntct.setCurUser(user);
				cntct.setLink(ownProfile.getPublicProfileUrl());
				cntct.persist();
			}
			Connections connections = client.getConnectionsForCurrentUser(setProfileFields);
			
			for (Person person : connections.getPersonList())
			{
				if(!lstUserId.contains(person.getId()))
				{
					ImportContact contact=new ImportContact();
					contact.setUserId(person.getId());
					contact.setName(person.getFirstName()+" "+person.getLastName());
					contact.setSnType(snType);
					contact.setCurUser(user);
					contact.setLink(person.getPublicProfileUrl());
					
					contact.persist();
				}					
			}
		}
		catch(Exception e)
		{
			System.out.println(e.toString());
		}
//		tx.commit();
	}
	
	static void sendMessage(String userId,String subject,String message,String userToken,String userTokenSecret,String consumerKeyValue,String consumerSecretValue)
	{
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue); 
		client=factory.createLinkedInApiClient(userToken,userTokenSecret);
		ArrayList<String> lstUserId=new ArrayList<String>();
		lstUserId.add(userId);
		client.sendMessage(lstUserId, subject, message);
	}
	
	@Transactional
	static String updateStatus(String message, String userToken,String userTokenSecret,String consumerKeyValue,String consumerSecretValue)
	{
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue); 
		client=factory.createLinkedInApiClient(userToken,userTokenSecret);
		client.updateCurrentStatus(message);
		Updates upd= client.getUserUpdates(0,1).getUpdates();
		Iterator<Update> itr=upd.getUpdateList().iterator();
		Update update=itr.next();
	
		String updateKeyTime=update.getUpdateKey()+":"+update.getTimestamp();
		return updateKeyTime;
	}
	
	@Transactional
	static void getComments(String contentId, String userToken,String tokenSecret,String consumerKeyValue,String consumerSecretValue,User user,SocialNetworking snType)
	{
		EntityManager em=JPA.em();
		EntityTransaction tx=em.getTransaction();
		tx.begin();
		
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue); 
		client=factory.createLinkedInApiClient(userToken,tokenSecret);
		
		List<String> lstUserIds=new ArrayList<String>();
		
		List<ImportContact> lstImportContact=ImportContact.all().filter("curUser=? and snType=?",user,snType).fetch();
		for(int i=0;i<lstImportContact.size();i++)
			lstUserIds.add(lstImportContact.get(i).getUserId().toString());
		
		PostUpdates postUpdate=PostUpdates.all().filter("contentId=?",contentId).fetchOne();
		
		List<Comments> lstComments=Comments.all().filter("curUser=? and contentId=?",user,postUpdate).fetch();
		List<String> lstCommentIds=new ArrayList<String>();
		for(int i=0;i<lstComments.size();i++)
			lstCommentIds.add(lstComments.get(i).getCommentId().toString());
			
		UpdateComments updateComments=client.getNetworkUpdateComments(contentId);
			
		Iterator<UpdateComment> itr=updateComments.getUpdateCommentList().iterator();
		UpdateComment comment=null;
		while(itr.hasNext())
		{
			comment=itr.next();
			if(lstUserIds.contains(comment.getPerson().getId()))
	       	{
	       		if(!lstCommentIds.contains(comment.getId()))
	       		{
	       			DateTime date=new DateTime(comment.getTimestamp());
	       			Comments comments=new Comments();
	       			comments.setContentId(postUpdate);
	       			comments.setCommentId(comment.getId());
	       			comments.setComment( comment.getComment());
	       			comments.setCommentTime(date);
	       			comments.setCurUser(user);
	               	
	   	           	ImportContact contact=ImportContact.all().filter("userId=? and curUser=?",comment.getPerson().getId(),user).fetchOne();
	   	           	comments.setFromUser(contact);
	   	           	comments.persist();
	       		}
	       	}
		}
		tx.commit();
	}
	
	@Transactional
	static void addStatusComment(String userToken,String userTokenSecret,String consumerKeyValue,String consumerSecretValue,User user,String contentId,String comment)
	{
		EntityManager em=JPA.em();
		EntityTransaction tx=em.getTransaction();
		tx.begin();
		
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue); 
		client=factory.createLinkedInApiClient(userToken,userTokenSecret);

		client.postComment(contentId, comment);

		PostUpdates postUpdate=PostUpdates.all().filter("contentId=?",contentId).fetchOne();
		UpdateComments updateComments=client.getNetworkUpdateComments(contentId);
		
		Iterator<UpdateComment> itr=updateComments.getUpdateCommentList().iterator();
		UpdateComment coment=null;
		while(itr.hasNext())
		{
			coment=itr.next();
			if(!itr.hasNext())
			{
				DateTime date=new DateTime(coment.getTimestamp());
    			Comments comments=new Comments();
    			comments.setContentId(postUpdate);
    			comments.setCommentId(coment.getId());
    			comments.setComment(coment.getComment());
    			comments.setCommentTime(date);
    			comments.setCurUser(user);
            	ImportContact contact=ImportContact.all().filter("userId=? and curUser=?",coment.getPerson().getId(),user).fetchOne();
            	comments.setFromUser(contact);
            	comments.persist();
			}
		}
		tx.commit();
	}
	
	@SuppressWarnings("deprecation")
	@Transactional
	static void getNetworkUpdates(String userToken,String userTokenSecret,String consumerKeyValue,String consumerSecretValue,User user,SocialNetworking snType)
	{
		EntityManager em=JPA.em();
		EntityTransaction tx=em.getTransaction();
		tx.begin();
		
		LinkedinParameters parameters=null;
		Network network=null;
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue); 
		client=factory.createLinkedInApiClient(userToken,userTokenSecret);
		SimpleDateFormat sdf=new SimpleDateFormat("MM/dd/yyyy");

		List<NetworkUpdates> lstNetworkUpdates= NetworkUpdates.all().filter("curUser=? and snType=?",user,snType).fetch();
		List<String> lstUpdateIds=new ArrayList<String>();

		for(int i=0;i<lstNetworkUpdates.size();i++)
			lstUpdateIds.add(lstNetworkUpdates.get(i).getContentId().toString());
		
		try
		{
			parameters=LinkedinParameters.all().filter("curUser=? and snType=?",user,snType).fetchOne();
		
			if(parameters == null)
			{
				throw new NullPointerException();
			}
			else if((parameters.getDays() == 0) && (parameters.getRecordNumbers() ==0) )
			{
				network = client.getNetworkUpdates(EnumSet.of(NetworkUpdateType.SHARED_ITEM));
			}
			else if((parameters.getDays() != 0) && (parameters.getRecordNumbers() ==0) )
			{
				Date endDate=new Date();
				Calendar c=Calendar.getInstance();
				c.add(Calendar.DATE, -(parameters.getDays()));
				Date startDate=new Date(sdf.format(c.getTime()));
				network = client.getNetworkUpdates(EnumSet.of(NetworkUpdateType.SHARED_ITEM),startDate,endDate);
			}
			else if((parameters.getDays() == 0) && (parameters.getRecordNumbers() !=0) )
			{
				network = client.getNetworkUpdates(EnumSet.of(NetworkUpdateType.SHARED_ITEM),0,parameters.getRecordNumbers());
			}
			else if((parameters.getDays() != 0) && (parameters.getRecordNumbers() !=0) )
			{
				Date endDate=new Date();
				Calendar c=Calendar.getInstance();
				c.add(Calendar.DATE, -(parameters.getDays()));
				Date startDate=new Date(sdf.format(c.getTime()));
				network = client.getNetworkUpdates(EnumSet.of(NetworkUpdateType.SHARED_ITEM),0,parameters.getRecordNumbers(),startDate,endDate);
			}
		}
		catch(NullPointerException e)
		{
			network = client.getNetworkUpdates(EnumSet.of(NetworkUpdateType.SHARED_ITEM));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		Iterator<Update> itr=network.getUpdates().getUpdateList().iterator();
		Update update=null;

		List<ImportContact> lstImportContact=ImportContact.all().filter("curUser=? and snType=?",user,snType).fetch();
		List<String> lstUserIds=new ArrayList<String>();

		for(int i=0;i<lstImportContact.size();i++)
			lstUserIds.add(lstImportContact.get(i).getUserId().toString());

		while(itr.hasNext())
		{
			update=itr.next();
			if(lstUserIds.contains(update.getUpdateContent().getPerson().getId()))
			{
				if(!lstUpdateIds.contains(update.getUpdateKey()))
				{
					NetworkUpdates networkUpdate=new NetworkUpdates();
					DateTime date=new DateTime(update.getTimestamp());
					networkUpdate.setContentId(update.getUpdateKey());
					if(update.getUpdateContent().getPerson().getCurrentShare().getComment()==null)
						continue;
					networkUpdate.setContent( update.getUpdateContent().getPerson().getCurrentShare().getComment());
					ImportContact fromUser=ImportContact.all().filter("userId=? and curUser=?",update.getUpdateContent().getPerson().getId(),user).fetchOne();
					networkUpdate.setFromUser(fromUser);
					networkUpdate.setCurUser(user);
					networkUpdate.setContentTime(date);
					networkUpdate.setSnType(snType);

					networkUpdate.persist();	
				}
			}
		}
		tx.commit();
	}
	
	@Transactional
	static void getMembership(String userToken,String userTokenSecret,String consumerKeyValue,String consumerSecretValue,User user,SocialNetworking snType)
	{
		EntityManager em=JPA.em();
		EntityTransaction tx=em.getTransaction();
		tx.begin();
		
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue); 
		client=factory.createLinkedInApiClient(userToken,userTokenSecret);
		
		List<GroupMember> lstGroupMember=GroupMember.all().filter("curUser=? and snType=?", user,snType).fetch();
		
		List<String> lstGroupIds=new ArrayList<String>();
		for(int i=0;i<lstGroupMember.size();i++)
			lstGroupIds.add(lstGroupMember.get(i).getGroupId().toString());
		
		Set<GroupMembershipField> groupFields=EnumSet.of(GroupMembershipField.GROUP_ID,GroupMembershipField.MEMBERSHIP_STATE,
        		GroupMembershipField.GROUP_NAME);
        
        GroupMemberships memberships = client.getGroupMemberships(groupFields);
        for (GroupMembership membership : memberships.getGroupMembershipList())
        {
        	if(!lstGroupIds.contains(membership.getGroup().getId().toString()))
        	{
        		GroupMember groupMember=new GroupMember();
            	groupMember.setGroupId(membership.getGroup().getId());
            	groupMember.setGroupName(membership.getGroup().getName());
            	groupMember.setMembershipState(membership.getMembershipState().getCode().toString());
            	groupMember.setCurUser(user);
            	groupMember.setSnType(snType);
            	
            	groupMember.persist();
        	}
        }
        tx.commit();
	}
	
	@SuppressWarnings("deprecation")
	@Transactional
	static void getDiscussions(String userToken,String userTokenSecret,String consumerKeyValue,String consumerSecretValue,User user,GroupMember groupMember,SocialNetworking snType)
	{
		EntityManager em=JPA.em();
		EntityTransaction tx=em.getTransaction();
		tx.begin();
		
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue); 
		client=factory.createLinkedInApiClient(userToken,userTokenSecret);
		LinkedinParameters parameters=null;
		Posts post=null;
		SimpleDateFormat sdf=new SimpleDateFormat("MM/dd/yyyy");
		
		List<GroupDiscussion> lstGroupDiscussion=GroupDiscussion.all().filter("curUser=? and groupName=?",user,groupMember).fetch();
		List<String> lstGroupDiscussionIds=new ArrayList<String>();
		for(GroupDiscussion d: lstGroupDiscussion)
			lstGroupDiscussionIds.add(d.getDiscussionId().toString());
		
		try
		{
			parameters=LinkedinParameters.all().filter("curUser=? and snType=?",user,snType).fetchOne();
		
			if(parameters == null)
			{
				post=client.getPostsByGroup(groupMember.getGroupId(),EnumSet.of(PostField.ID,PostField.SUMMARY,PostField.TITLE,PostField.TYPE,PostField.CREATION_TIMESTAMP,
						PostField.CREATOR_FIRST_NAME,PostField.CREATOR_LAST_NAME,PostField.TYPE),0,15);
			}
			else if(parameters.getRecordNumbers() ==0)
			{
				post=client.getPostsByGroup(groupMember.getGroupId(),EnumSet.of(PostField.ID,PostField.SUMMARY,PostField.TITLE,PostField.TYPE,PostField.CREATION_TIMESTAMP,
						PostField.CREATOR_FIRST_NAME,PostField.CREATOR_LAST_NAME,PostField.TYPE),0,15);
			}
			else if(parameters.getRecordNumbers() !=0)
			{
				if(parameters.getDays()!=0)
				{
					Calendar c=Calendar.getInstance();
					c.add(Calendar.DATE, -(parameters.getDays()));
					Date modified=new Date(sdf.format(c.getTime()));
					post=client.getPostsByGroup(groupMember.getGroupId(),EnumSet.of(PostField.ID,PostField.SUMMARY,PostField.TITLE,PostField.TYPE,PostField.CREATION_TIMESTAMP,
							PostField.CREATOR_FIRST_NAME,PostField.CREATOR_LAST_NAME,PostField.TYPE),0,parameters.getRecordNumbers(),modified);
				}
				else
				{
					post=client.getPostsByGroup(groupMember.getGroupId(),EnumSet.of(PostField.ID,PostField.SUMMARY,PostField.TITLE,PostField.TYPE,PostField.CREATION_TIMESTAMP,
							PostField.CREATOR_FIRST_NAME,PostField.CREATOR_LAST_NAME,PostField.TYPE),0,parameters.getRecordNumbers());
				}
			}
		}
		catch(NullPointerException e)
		{
			return;
		}
		
		for(Post p: post.getPostList())
		{
			if(!lstGroupDiscussionIds.contains(p.getId()))
			{
				
				DateTime date=new DateTime(p.getCreationTimestamp());
				GroupDiscussion discuss=new GroupDiscussion();
	
				discuss.setDiscussionId(p.getId());
				if(p.getSummary() !=null)
					discuss.setDiscussionSummary(p.getSummary());
				else
					discuss.setDiscussionSummary("N/A");
				if(p.getTitle()!=null)
					discuss.setDiscussionTitle(p.getTitle());	
				else
					discuss.setDiscussionTitle("N/A");
				discuss.setDiscussionBy(p.getCreator().getFirstName()+" "+p.getCreator().getLastName());
				discuss.setDiscussionTime(date);
				discuss.setDiscussionByCurrentUser(false);
				discuss.setGroupName(groupMember);
				discuss.setCurUser(user);
				discuss.persist();
				
			}
		}
		tx.commit();
	}
	
	static String addGroupDiscussion(String userToken,String userTokenSecret,String consumerKeyValue,String consumerSecretValue,String title,String summary,String groupId)
	{
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue); 
		client=factory.createLinkedInApiClient(userToken,userTokenSecret);
		client.createPost(groupId, title, summary);
        
        Posts post=client.getPostsByGroup(groupId,EnumSet.of(PostField.ID,PostField.SUMMARY,PostField.TITLE,PostField.TYPE,PostField.CREATION_TIMESTAMP,
        		PostField.CREATOR_FIRST_NAME,PostField.CREATOR_LAST_NAME),0,1);
        String discussionIdTime=post.getPostList().get(0).getId()+":"+post.getPostList().get(0).getCreationTimestamp()+":"+post.getPostList().get(0).getCreator().getFirstName()+" "+post.getPostList().get(0).getCreator().getLastName();
        return discussionIdTime;
	}
	
	static void addDiscussionComment(String userToken,String userTokenSecret,String consumerKeyValue,String consumerSecretValue,User user,GroupDiscussion groupDiscussion,String discussionId,String comment,int start,SocialNetworking snType)
	{
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue); 
		client=factory.createLinkedInApiClient(userToken,userTokenSecret);
		client.addPostComment(discussionId, comment);
		
		getDiscussionComments(userToken, userTokenSecret, consumerKeyValue, consumerSecretValue, user, groupDiscussion,snType,start);
		
	}
	
	@Transactional
	static void getDiscussionComments(String userToken,String userTokenSecret,String consumerKeyValue,String consumerSecretValue,User user,GroupDiscussion groupDiscussion,SocialNetworking snType, int start)
	{
		EntityManager em=JPA.em();
		EntityTransaction tx=em.getTransaction();
		tx.begin();
		
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue); 
		client=factory.createLinkedInApiClient(userToken,userTokenSecret);
		LinkedinParameters parameters=null;
		List<GroupDiscussionComments> lstGroupDiscussionComments=null;
		com.google.code.linkedinapi.schema.Comments comments=null;
		
		lstGroupDiscussionComments=GroupDiscussionComments.all().filter("curUser=? and discussion=?",user,groupDiscussion).fetch();
		List<String> lstCommentIds=new ArrayList<String>();
		
		for(int i=0;i<lstGroupDiscussionComments.size();i++)
			lstCommentIds.add(lstGroupDiscussionComments.get(i).getCommentId());
		
		try
		{
			parameters=LinkedinParameters.all().filter("curUser=? and snType=?",user,snType).fetchOne();
			if(parameters == null)
			{
				comments=client.getPostComments(groupDiscussion.getDiscussionId(), EnumSet.of(CommentField.ID,CommentField.CREATOR,CommentField.CREATION_TIMESTAMP,
						CommentField.TEXT),start,10);
			}
			else if(parameters.getRecordNumbers() ==0)
			{
				comments=client.getPostComments(groupDiscussion.getDiscussionId(), EnumSet.of(CommentField.ID,CommentField.CREATOR,CommentField.CREATION_TIMESTAMP,
						CommentField.TEXT),start,10);
			}
			else if(parameters.getRecordNumbers() !=0)
			{
				comments=client.getPostComments(groupDiscussion.getDiscussionId(), EnumSet.of(CommentField.ID,CommentField.CREATOR,CommentField.CREATION_TIMESTAMP,
						CommentField.TEXT),start,parameters.getRecordNumbers());
			}
		}
		catch(NullPointerException e)
		{
			return;
		}
		
		for(com.google.code.linkedinapi.schema.Comment comment:comments.getCommentList())
		{
			if(!lstCommentIds.contains(comment.getId()))
			{
				GroupDiscussionComments groupDiscussionComment=new GroupDiscussionComments();
				DateTime date=new DateTime(comment.getCreationTimestamp());
				groupDiscussionComment.setCommentId(comment.getId());
				groupDiscussionComment.setComment(comment.getText());
				groupDiscussionComment.setCommentTime(date);
				groupDiscussionComment.setByUser(comment.getCreator().getFirstName()+" "+comment.getCreator().getLastName());
				groupDiscussionComment.setCurUser(user);
				groupDiscussionComment.setDiscussion(groupDiscussion);
				groupDiscussionComment.persist();
			}
		}
		tx.commit();
	}
	
	static String deleteDiscussion(List<Integer> lstIdValues,String userToken,String userTokenSecret,String consumerKeyValue,String consumerSecretValue,User user)
	{
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue); 
		client=factory.createLinkedInApiClient(userToken,userTokenSecret);
		EntityManager em=JPA.em();
		EntityTransaction tx=em.getTransaction();
		tx.begin();
		String message="";

		for(int i=0;i<lstIdValues.size();i++)
		{
			GroupDiscussion groupDiscussion=GroupDiscussion.all().filter("id=?", lstIdValues.get(i)).fetchOne();
			try
			{
				client.deletePost(groupDiscussion.getDiscussionId());  //This API call takes about 2 minutes...
				em.remove(groupDiscussion);
				message="Deleted Successfully...";
			}
			catch(LinkedInApiClientException e)
			{
				message="Something Went Wrong...:"+e.getMessage();
			}
		}
		tx.commit();
		return message;
	}
	static List<Comments> refreshComments(PostUpdates postUpdates)
	{
		List<Comments> lstComment=Comments.all().filter("contentId=?", postUpdates).fetch();
		return lstComment;
	}
	static List<GroupDiscussion> refreshDiscussions(GroupMember member)
	{
		List<GroupDiscussion> lstDiscussion=GroupDiscussion.all().filter("groupName=?", member).fetch();
		return lstDiscussion;
	}
	static List<GroupDiscussionComments> refreshDiscussionComments(GroupDiscussion discussion)
	{
		List<GroupDiscussionComments> lstGroupDiscussionComment=GroupDiscussionComments.all().filter("discussion=?",discussion).fetch();
		return lstGroupDiscussionComment;
	}
}
