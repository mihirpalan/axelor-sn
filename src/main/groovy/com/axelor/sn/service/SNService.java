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
	static String LINKIN_USERNAME,LINKIN_PASSWORD;
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
	static String getUrl(String consumerKey,String consumerSecret,User user,SocialNetworking snType)   // , String userName,String password)
	{
//		LINKIN_USERNAME = userName;
//		LINKIN_PASSWORD=password;
		String authUrl=null;
		
		try
		{
			consumerKeyValue = consumerKey;
			consumerSecretValue = consumerSecret;
			oauthService=LinkedInOAuthServiceFactory.getInstance().createLinkedInOAuthService(consumerKeyValue, consumerSecretValue);
			requestToken=oauthService.getOAuthRequestToken("http://localhost:8080/axelor-demo/ws/linkedin/100");
//			requestToken=oauthService.getOAuthRequestToken("http://192.168.0.155:8080/axelor-demo/#/ds/personalCredential/edit");
			authUrl=requestToken.getAuthorizationUrl();
			System.out.println(requestToken);
			setCurrentUser(user);
			setSnType(snType);
//			Verifier verifier = new Verifier(getVerifier(authUrl));
//			accessToken = oauthService.getOAuthAccessToken(requestToken,verifier.getValue());
			
		}
		catch(Exception e)
		{
			System.out.println(e.toString());
		}
//		return accessToken.getToken();
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
		
		String pin=verifier;
//		System.out.println(pin);
//		System.out.println(requestToken);
		try
		{
		
			accessToken = oauthService.getOAuthAccessToken(requestToken,verifier);
			System.out.println(accessToken.getToken());
			System.out.println(accessToken.getTokenSecret());
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
			System.out.println("Status=true");
			status=true;
		}
		catch(Exception e)
		{
			throw new Exception("There's Some Problem. Not Authorised.");
		}
		tx.commit();
		return status;
	}
	
//	static String getTokenSecret()
//	{
//		return accessToken.getTokenSecret();
//	}
//	
//	static String getVerifier(String url) throws Exception 
//	{
//		String pin=null;
//		try
//		{
//			final WebClient webClient = new WebClient();
//			webClient.setJavaScriptEnabled(false);
//			webClient.setCssEnabled(false);
//  
//			// Get the first page
//			final HtmlPage page1 = webClient.getPage(url);
//  
//			// Get the form that we are dealing with and within that form,
//			// find the submit button and the field that we want to change.
//			final HtmlForm form = page1.getFormByName("oauthAuthorizeForm");
//  
//			final HtmlSubmitInput button = form.getInputByName("authorize");
//			final HtmlTextInput textField = form.getInputByName("session_key");
//			final HtmlPasswordInput textField2 = form.getInputByName("session_password");
//			// Change the value of the text field
//			textField.setValueAttribute(LINKIN_USERNAME);
//			textField2.setValueAttribute(LINKIN_PASSWORD);
//  
//			// Now submit the form by clicking the button and get back the second page.
//			final HtmlPage page2 = button.click();
//  
//			// Obtain the 5-digit access code from the returned page
//			String text = page2.asText();
//			int i = 0;
//			while (text.charAt(i)>'9' || text.charAt(i)<'0') i++;
//			
//			pin = text.substring(i, i+5);
//			webClient.closeAllWindows();
//		}
//		catch(Exception e)
//		{
//			e.printStackTrace();
//		}
//		return pin;
//	}
	
	static SocialNetworking getSnType(String sntype)
	{
		SocialNetworking snType=SocialNetworking.all().filter("name=?", sntype).fetchOne();
		return snType;
	}
	
	static PersonalCredential getPersonalCredential(User user,SocialNetworking snType)
	{
		PersonalCredential query=null;
		try
		{
		 query=PersonalCredential.all().filter("userId=? and snType=?", user,snType).fetchOne();
		System.out.println(query);
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
	
	@Transactional
	static void fetchConnections(String consumerKeyValue,String consumerSecretValue, String userToken,String userTokenSecret,User user,SocialNetworking snType)
	{
		EntityManager em=JPA.em();
		EntityTransaction tx=em.getTransaction();
		tx.begin();
		
//		List<ImportContacts> contacts=em.createQuery("select a from ImportContacts a where a.curUser="+user.getId()+" and a.sntype='"+snType.getName()+"'", ImportContacts.class).getResultList();
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
			JPA.em().flush();
		}
		catch(Exception e)
		{
			System.out.println(e.toString());
		}
		tx.commit();
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
	
	//FUNCTION TO GET THE COMMENTS OF A STATUS FROM LINKEDIN
	@Transactional
	static void getComments(String contentId, String userToken,String tokenSecret,String consumerKeyValue,String consumerSecretValue,User user,SocialNetworking snType)
	{
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue); 
		client=factory.createLinkedInApiClient(userToken,tokenSecret);
		
		List<String> lstUserIds=new ArrayList<String>();
		
		//GET ALL THE CONTACTS OF THE CURRENT USER INTO A LIST
//		List<ImportContacts> contacts=em.createQuery("select a from ImportContacts a where a.curUser="+user.getId()+" and a.sntype='"+snType+"'", ImportContacts.class).getResultList();
		List<ImportContact> lstImportContact=ImportContact.all().filter("curUser=? and snType=?",user,snType).fetch(); 
		for(int i=0;i<lstImportContact.size();i++)
			lstUserIds.add(lstImportContact.get(i).getUserId().toString());
		
		//SELECT AN OBJECT OF A POST WITH THE GIVEN CONTENT-ID
//		PostUpdates post=em.createQuery("select a from PostUpdates a where a.contentId='"+contentId+"'",PostUpdates.class).getSingleResult();
		PostUpdates postUpdate=PostUpdates.all().filter("contentId=?",contentId).fetchOne();
		
		//GET ALL THE COMMENTS OF A PARTICULAR POST INTO A LIST
//		List<Comments> comments=em.createQuery("select a from Comments a where a.curUser="+user.getId()+" and a.contentId="+post.getId(), Comments.class).getResultList();
		List<Comments> lstComments=Comments.all().filter("curUser=? and contentId=?",user,postUpdate).fetch();
		List<String> lstCommentIds=new ArrayList<String>();
		for(int i=0;i<lstComments.size();i++)
			lstCommentIds.add(lstComments.get(i).getCommentId().toString());
			
		UpdateComments updateComments=client.getNetworkUpdateComments(contentId);
//		System.out.println(client.getNetworkUpdateLikes(content_id));
			
		Iterator<UpdateComment> itr=updateComments.getUpdateCommentList().iterator();
		UpdateComment comment=null;
		while(itr.hasNext())
		{
			comment=itr.next();
			if(lstUserIds.contains(comment.getPerson().getId()))
	       	{
	       		if(!lstCommentIds.contains(comment.getId()))
	       		{
	               	//SET THE FIELDS FOR THE COMMENT
	       			DateTime date=new DateTime(comment.getTimestamp());
	       			Comments comments=new Comments();
	       			comments.setContentId(postUpdate);
	       			comments.setCommentId(comment.getId());
	       			comments.setComment( comment.getComment());
	       			comments.setCommentTime(date);
	       			comments.setCurUser(user);
	               	
	               	//SELECT FROM CLASS:ImportContacts TO SET THE USER WHO COMMENTED
//	               	ImportContacts contact=em.createQuery("select a from ImportContacts a where a.userid='"+comment.getPerson().getId()+"' and a.curUser="+user.getId(),ImportContacts.class).getSingleResult();
	   	           	ImportContact contact=ImportContact.all().filter("userId=? and curUser=?",comment.getPerson().getId(),user).fetchOne();
	   	           	comments.setFromUser(contact);
	   	           	comments.persist();
	       		}
	       	}
		}
	}
	
	static void addStatusComment(String userToken,String userTokenSecret,String consumerKeyValue,String consumerSecretValue,User user,String contentId,String comment)
	{
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue); 
		client=factory.createLinkedInApiClient(userToken,userTokenSecret);

		client.postComment(contentId, comment);
		EntityManager em=JPA.em();
		EntityTransaction tx=em.getTransaction();
		tx.begin();

//		PostUpdates post=em.createQuery("select a from PostUpdates a where a.contentId='"+contentId+"'",PostUpdates.class).getSingleResult();
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
		LinkedinParameters parameters=null;
		Network network=null;
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue); 
		client=factory.createLinkedInApiClient(userToken,userTokenSecret);
		SimpleDateFormat sdf=new SimpleDateFormat("MM/dd/yyyy");

//		List<NetworkUpdates> networkUpdates=em.createQuery("select a from NetworkUpdates a where a.curUser="+user.getId()+" and a.snType="+sntype.getId(), NetworkUpdates.class).getResultList();
		List<NetworkUpdates> lstNetworkUpdates= NetworkUpdates.all().filter("curUser=? and snType=?",user,snType).fetch();
		List<String> lstUpdateIds=new ArrayList<String>();
		for(int i=0;i<lstNetworkUpdates.size();i++)
			lstUpdateIds.add(lstNetworkUpdates.get(i).getContentId().toString());
		try
		{
//			parameters=em.createQuery("select a from LinkedinParameters a where a.curUser="+user.getId()+" and a.snType="+sntype.getId(),LinkedinParameters.class).getSingleResult();
			parameters=LinkedinParameters.all().filter("curUser=? and snType=?",user,snType).fetchOne();
		}
		catch(Exception e)
		{

		}
		try
		{
			if(parameters == null)
			{
				network = client.getNetworkUpdates(EnumSet.of(NetworkUpdateType.SHARED_ITEM));
			}
			else if((parameters.getDays() == 0) && (parameters.getRecordNumbers() ==0) )
			{
				network = client.getNetworkUpdates(EnumSet.of(NetworkUpdateType.SHARED_ITEM));
			}
			else if((parameters.getDays() != 0) && (parameters.getRecordNumbers() ==0) )
			{
//				System.out.println("Day Values");
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
			return;
		}
		Iterator<Update> itr=network.getUpdates().getUpdateList().iterator();
		Update update=null;

//		List<ImportContacts> contacts=em.createQuery("select a from ImportContacts a where a.curUser="+user.getId()+" and a.sntype='"+sntype.getName()+"'", ImportContacts.class).getResultList();
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
//					System.out.println("Likes:"+update.getNumLikes());
//					ImportContacts fromUser=em.createQuery("select a from ImportContacts a where a.userid='"+update.getUpdateContent().getPerson().getId()+"' and a.curUser="+user.getId(),ImportContacts.class).getSingleResult();
					ImportContact fromUser=ImportContact.all().filter("userId=? and curUser=?",update.getUpdateContent().getPerson().getId(),user).fetchOne();
					networkUpdate.setFromUser(fromUser);
					networkUpdate.setCurUser(user);
					networkUpdate.setContentTime(date);
					networkUpdate.setSnType(snType);

					networkUpdate.persist();	
				}
			}
		}
	}
	
	@Transactional
	static void getMembership(String userToken,String userTokenSecret,String consumerKeyValue,String consumerSecretValue,User user,SocialNetworking snType)
	{
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue); 
		client=factory.createLinkedInApiClient(userToken,userTokenSecret);
		
//		List<GroupMember> member=em.createQuery("select a from GroupMember a where a.curUser="+user.getId()+" and a.snType="+sntype.getId(), GroupMember.class).getResultList();
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
	}
	
	@SuppressWarnings("deprecation")
	static void getDiscussions(String userToken,String userTokenSecret,String consumerKeyValue,String consumerSecretValue,User user,GroupMember groupMember,SocialNetworking snType)
	{
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue); 
		client=factory.createLinkedInApiClient(userToken,userTokenSecret);
		LinkedinParameters parameters=null;
		Posts post=null;
		SimpleDateFormat sdf=new SimpleDateFormat("MM/dd/yyyy");
		
		EntityManager em=JPA.em();
		EntityTransaction tx=em.getTransaction();
		tx.begin();
		List<GroupDiscussion> lstGroupDiscussion=GroupDiscussion.all().filter("curUser=? and groupName=?",user,groupMember).fetch();
		List<String> lstGroupDiscussionIds=new ArrayList<String>();
		for(GroupDiscussion d: lstGroupDiscussion)
			lstGroupDiscussionIds.add(d.getDiscussionId().toString());
		
		try
		{
			parameters=LinkedinParameters.all().filter("curUser=? and snType=?",user,snType).fetchOne();
		}
		catch(Exception e)
		{

		}
		try
		{
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
//				else
//					discuss.setPostSummary("N/A");
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
	
	@Transactional
	static void addDiscussionComment(String userToken,String userTokenSecret,String consumerKeyValue,String consumerSecretValue,User user,GroupDiscussion groupDiscussion,String discussionId,String comment,int start,SocialNetworking snType)
	{
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue); 
		client=factory.createLinkedInApiClient(userToken,userTokenSecret);
		client.addPostComment(discussionId, comment);
		
		getDiscussionComments(userToken, userTokenSecret, consumerKeyValue, consumerSecretValue, user, groupDiscussion,snType,start);
		
//		EntityManager em=JPA.em();
//		EntityTransaction tx=em.getTransaction();
//		tx.begin();
//
//		com.google.code.linkedinapi.schema.Comments cmts=client.getPostComments(postId, EnumSet.of(CommentField.ID,CommentField.CREATOR,CommentField.CREATION_TIMESTAMP,
//				CommentField.TEXT));
//		Iterator<Comment> itr=cmts.getCommentList().iterator();
//		Comment comments=null;
//
//		while(itr.hasNext())
//		{
//			comments=itr.next();
//			if(!itr.hasNext())
//			{
//				DateTime date=new DateTime(comments.getCreationTimestamp());
//				GroupDiscussionComments cmt=new GroupDiscussionComments();
//
//				cmt.setCommentId(comments.getId());
//				cmt.setComment(comments.getText());
//				cmt.setCommentTime(date);
//				cmt.setByUser(comments.getCreator().getFirstName()+" "+comments.getCreator().getLastName());
//				cmt.setCurUser(user);
//				cmt.setPost(grpDiscuss);
//				cmt.persist();
//				cmt.flush();
//			}
//		}
//		tx.commit();
	}
	
	@Transactional
	static void getDiscussionComments(String userToken,String userTokenSecret,String consumerKeyValue,String consumerSecretValue,User user,GroupDiscussion groupDiscussion,SocialNetworking snType, int start)
	{
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue); 
		client=factory.createLinkedInApiClient(userToken,userTokenSecret);
		LinkedinParameters parameters=null;
		List<GroupDiscussionComments> lstGroupDiscussionComments=null;
		com.google.code.linkedinapi.schema.Comments comments=null;
		
//		grpDiscussComments=em.createQuery("select a from GroupDiscussionComments a where a.curUser="+user.getId()+" and a.post="+groupdiscussion.getId(),GroupDiscussionComments.class).getResultList();
		lstGroupDiscussionComments=GroupDiscussionComments.all().filter("curUser=? and discussion=?",user,groupDiscussion).fetch();
		List<String> lstCommentIds=new ArrayList<String>();
		
		for(int i=0;i<lstGroupDiscussionComments.size();i++)
			lstCommentIds.add(lstGroupDiscussionComments.get(i).getCommentId());
		
		try
		{
//			parameters=em.createQuery("select a from LinkedinParameters a where a.curUser="+user.getId()+" and a.snType="+sntype.getId(),LinkedinParameters.class).getSingleResult();
			parameters=LinkedinParameters.all().filter("curUser=? and snType=?",user,snType).fetchOne();
		}
		catch(Exception e)
		{

		}
		try
		{
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
	}
	
	static String deleteDiscussion(List<Integer> lstIdValues,String userToken,String userTokenSecret,String consumerKeyValue,String consumerSecretValue,User user)
	{
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue, consumerSecretValue); 
		client=factory.createLinkedInApiClient(userToken,userTokenSecret);
		EntityManager em=JPA.em();
		EntityTransaction tx=em.getTransaction();
		tx.begin();
		String message="";

//		List<String> array=new ArrayList<String>();
		for(int i=0;i<lstIdValues.size();i++)
		{
//			GroupDiscussion query=em.createQuery("select a from GroupDiscussion a where a.id="+idvalues.get(i),GroupDiscussion.class).getSingleResult();
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
}
