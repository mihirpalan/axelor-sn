package com.axelor.sn.web

import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Request;
import com.axelor.rpc.Response;
import com.axelor.sn.db.PostUpdates;
import com.axelor.sn.db.Comments;
import com.axelor.auth.db.User;
import com.axelor.sn.db.GroupDiscussion;
import com.axelor.sn.db.GroupDiscussionComments
import com.axelor.sn.db.GroupMember;
import com.axelor.sn.db.ImportContact
import com.axelor.sn.db.NetworkUpdates;
import com.axelor.sn.db.PersonalCredential;
import com.axelor.sn.db.SocialNetworking;
import com.axelor.sn.db.ApplicationCredentials;
import javax.persistence.EntityManager
import javax.persistence.EntityTransaction
import com.axelor.db.*;
import org.joda.time.DateTime;

import com.axelor.sn.service.SNService;
import com.google.code.linkedinapi.client.LinkedInApiClient;
import com.google.code.linkedinapi.client.LinkedInApiClientFactory
import com.google.code.linkedinapi.client.oauth.LinkedInAccessToken;
import com.google.code.linkedinapi.schema.Connections
import com.google.code.linkedinapi.schema.Person
import com.google.common.cache.LocalCache.Values;

class Controller
{
	//FUNCTION TO GET THE USER TOKEN FROM LINKEDIN
	void getUrl(ActionRequest request, ActionResponse response)
	{
		def context=request.context as PersonalCredential

		//GET THE CURRENT USER LOGGED IN
		User user=request.context.get("__user__")
		//GET THE SOCIAL NETWORKING TYPE
		SocialNetworking snType=request.context.get("snType")

		EntityManager em=JPA.em()
		EntityTransaction tx=em.getTransaction()
		tx.begin()

		//FETCH THE PERSONAL CREDENTIAL OF THE USER
		PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)

		//CHECK IF PERSONAL CREDENTIALS ARE NOT EMPTY
		if(personalCredential !=null)
		{
//			response.flash="You Already Have One Account Associated..."
			throw new Exception("You Already Have One Account Associated...")
		}
		//FOR PERSONAL CREDENTIALS
		else
		{
			String consumerKey,consumerSecret,userName,password

			//GET CREDENTIALS OF THE APPLICATION TO BE USED
			ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)

			//CHECK IF APPLICATION CREDENTIALS ARE NOT EMPTY
			if(applicationCredential != null)
			{
				consumerKey=applicationCredential.apikey
				consumerSecret=applicationCredential.apisecret
				
				String authUrl=SNService.getUrl(consumerKey, consumerSecret,user,snType)
				response.flash ="Click the link to get access <a href="+authUrl+" target='_blank'>"+authUrl+"</a>"
//				response.flash ="Click the link to get access <a href="+authUrl+">"+authUrl+"</a>"
//				response.view = [title: "Linkedin", resource: "http://www.google.co.in" , viewType: "html"]
				//FUNCTION CALLS TO SERVICE FOR GETTING USER-TOKEN AND USER-TOKEN-SECRET
//				try
//				{
//					response.values=["userToken":SNService.fetchUserToken(consumerKey, consumerSecret, userName, password),"userTokenSecret":SNService.getTokenSecret(),"password":""]
//					response.flash="Successfully Logged in to Linkedin..."
//				}
//				catch(NullPointerException e)
//				{
//					response.flash="Either User Name or Password is Invalid..."
//				}
//				response.view = [title: "Axelor.com", resource: "http://www.axelor.com/", viewType: "html"]
				tx.commit()
			}
			//FOR APPLICATION CREDENTIALS
			else
			{
				response.flash="No Application Defined..."
			}
		}
	}
//	void getUserToken(ActionRequest request, ActionResponse response)
//	{
//		String verifier=request.context.get("verifier")
//		
//		response.values=["userToken":SNService.getUserToken(verifier),"userTokenSecret":SNService.getTokenSecret()]
//		response.flash="Successfully Logged in to Linkedin..."
//	}
	
	void networkType(ActionRequest request, ActionResponse response)
	{
		SocialNetworking snType=SNService.getSnType("Linkedin")
		response.values=["snType":snType]
	}
	
	//FUNCTION TO GET THE CONNECTIONS FROM LINKEDIN
	void fetchConnections(ActionRequest request, ActionResponse response)
	{
		def context=request.context as ImportContact

		//GET THE CURRENT USER
		User user=request.context.get("__user__")

		SocialNetworking snType=SNService.getSnType("Linkedin")

		//GET THE PERSONAL CREDENTIALS
		PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
		//CHECK PERSONAL CREDENTIAL
		if(personalCredential==null)
		{
//			response.flash="Please Login First"
			throw new Exception("Please Login First")
		}
		//FOR PERSONAL CREDENTIAL
		else
		{
			//GET THE APPLICATION CREDENTIALS
			ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)
			
			//CHECK APPLICATION CREDENTIALS
			if(applicationCredential!=null)
			{
				String consumerKeyValue=applicationCredential.apikey
				String consumerSecretValue=applicationCredential.apisecret
				String userToken=personalCredential.userToken
				String userTokenSecret=personalCredential.userTokenSecret

				//FUNCTION TO FETCH THE CONNECTIONS
				SNService.fetchConnections(consumerKeyValue, consumerSecretValue, userToken, userTokenSecret,user,snType)
				response.flash="Imported Contacts Successfully..."
			}
			//FOR APPLICATION CREDENTIAL
			else
			{
				response.flash = "No Application Defined..."
			}
		}
	}
	
	//FUNTION TO SEND A DIRECT MESSAGE TO A CONTACT ON LINKEDIN
	void sendMessage(ActionRequest  request, ActionResponse response)
	{
		//GET THE CURRENT USER
		User user=request.context.get("__user__")

		//GET THE DETAILS OF THE CONTACT TO SEND THE MESSAGE TO
		ImportContact contact=request.context.get("userid")

		String userId=contact.getUserId()
		String subject=request.context.get("subject")
		String message=request.context.get("msgcontent").toString()

		SocialNetworking snType=SNService.getSnType("Linkedin")

		//GET PERSONAL DETAILS
		PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
		//CHECK PERSONAL DETAILS
		if(personalCredential != null)
		{
			//GET THE APPLICATION DETAILS
			ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)
			//CHECK APPLICATION DETAILS
			if(applicationCredential != null)
			{
				String consumerKeyValue=applicationCredential.apikey
				String consumerSecretValue=applicationCredential.apisecret
				String userToken=personalCredential.userToken
				String userTokenSecret=personalCredential.userTokenSecret

				//CALL THE FUNCTION TO SEND A MESSAGE
				SNService.sendMessage(userId,subject,message,userToken,userTokenSecret,consumerKeyValue,consumerSecretValue)
				response.flash = "Message Sent..."
			}
			//FOR APPLICATION CREDENTIALS
			else
			{
				throw new Exception("No Application Defined")
			}
		}
		//FOR PERSONAL CREDENTIALS
		else
		{
			throw new Exception("Please Login First")
		}
	}
	
	//FUNCTION TO POST A STATUS TO LINKEDIN
	void updateStatus(ActionRequest request, ActionResponse response)
	{
		def context=request.context as PostUpdates

		if(context.getId() == null)
		{
			//GET THE CURRENT USER
			User user=request.context.get("__user__")

			//GET THE MESSAGE TO BE POSTED
			String message=request.context.get("content").toString()

			SocialNetworking snType=SNService.getSnType("Linkedin")

			//GET THE PERSONAL CREDENTIAL
			PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
			//CHECK PERSONAL CREDENTIAL
			if(personalCredential != null)
			{
				//GET THE APPLICATION CREDENTIAL
				ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)
				
				//CHECK APPLICATION CREDENTIAL
				if(applicationCredential != null)
				{
					String consumerKeyValue=applicationCredential.apikey
					String consumerSecretValue=applicationCredential.apisecret
					String userToken=personalCredential.userToken
					String userTokenSecret=personalCredential.userTokenSecret
					
					//CALL THE FUNCTION TO UPDATE THE STATUS
					String updateKeyTime=SNService.updateStatus(message, userToken, userTokenSecret,consumerKeyValue, consumerSecretValue)
					String[] array=updateKeyTime.split(":")
					DateTime date=new DateTime(Long.parseLong(array[1]));
//					response.values=["postId":array[0],"postTime":date]
					response.values=["contentId":array[0],"postTime":date]
					response.flash="Status Successfully Updated to Linkedin..."
//					response.view = [ title:"Posts", resource: PostUpdates.class.name, viewType: "grid",name:"post-grid"]
				}
				//FOR APPLICATION CREDENTIAL
				else
				{
					throw new Exception("No Application Defined")
				}
			}
			//FOR PERSONAL CREDENTIAL
			else
			{
				throw new Exception("Please Login First")
			}
		}
	}
	
	//FUNCTION TO GET THE COMMENTS OF A STATUS FROM LINKEDIN
	void getComments(ActionRequest request, ActionResponse response)
	{

		String contentId=request.context.get("contentId")
		//CHECK TO SEE IF WE ARE GETTING THE ID OF THE UPDATE TO GET IT'S COMMENTS
		if(!contentId.equals(null))
		{
			//GET THE CURRENT USER
			User user=request.context.get("__user__")

			EntityManager em=JPA.em()
			EntityTransaction tx=em.getTransaction()
			tx.begin()

			SocialNetworking snType=SNService.getSnType("Linkedin")

			//GET PERSONAL CREDENTIALS
			PersonalCredential personalCredential=SNService.getPersonalCredential(user, snType)
			if(personalCredential !=null)
			{
				//GET APPLICATION CREDENTIALS
				ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)
				
				if(applicationCredential != null)
				{
					String consumerKeyValue=applicationCredential.apikey
					String consumerSecretValue=applicationCredential.apisecret
					String userToken=personalCredential.userToken
					String userTokenSecret=personalCredential.userTokenSecret

					//CALL THE FUNTION FOR GETTING THE COMMENTS
					SNService.getComments(contentId, userToken, userTokenSecret, consumerKeyValue, consumerSecretValue,user,snType)
					tx.commit()
					response.flash="Comments Retrieved..."
				}
				//FOR APPLICATION CREDENTIALS
				else
				{
					throw new Exception("No Application Defined")
				}
			}
			//FOR PERSONAL CREDENTIALS
			else
			{
				throw new Exception("Please Login First")
			}
		}
		//IF WE ARE GETTING THE ID OF THE UPDATE TO GET IT'S COMMENTS AS NULL
		else
		{
			response.flash="Select A Status to Fetch Comments..."
		}
	}
	
	void clearCommentfield(ActionRequest request, ActionResponse response)
	{
		response.values=["comment":""]
	}
	
	void refreshComments(ActionRequest request,ActionResponse response)
	{
		def context =request.context as PostUpdates
		
		List<Comments> lstComments=context.getComments()
		
		List<Comments> lstComment=Comments.all().filter("contentId=?", context).fetch()
		 //=JPA.em().createQuery("Select a from Comments a where a.contentId="+context.id,Comments.class).getResultList()

		for(int i=0;i<lstComment.size();i++)
		{
			if(!lstComments.contains(lstComment.get(i)))
				lstComments.add(lstComment.get(i))
		}
		context.setComments(lstComments)
		response.values=context
	}
	
	void addStatusComment(ActionRequest request, ActionResponse response)
	{
		
		String contentId=request.context.get("contentId")
		String comment=request.context.get("comment")
		User user=request.context.get("__user__")
		PostUpdates postUpdates=request.context.get("__self__")
		
		SocialNetworking snType=request.context.get("snType")

		//GET THE PERSONAL CREDENTIAL
		PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
		//CHECK PERSONAL CREDENTIAL
		if(personalCredential != null)
		{
			//GET THE APPLICATION CREDENTIAL
			ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)

			//CHECK APPLICATION CREDENTIAL
			if(applicationCredential != null )
			{
				String consumerKeyValue=applicationCredential.apikey
				String consumerSecretValue=applicationCredential.apisecret
				String userToken=personalCredential.userToken
				String userTokenSecret=personalCredential.userTokenSecret
				SNService.addStatusComment(userToken, userTokenSecret, consumerKeyValue, consumerSecretValue, user, contentId, comment)
				response.flash="Comment Added..."
			}
			//FOR APPLICATION CREDENTIAL
			else
			{
				throw new Exception("No Application Defined")
			}
		}
		//FOR PERSONAL CREDENTIAL
		else
		{
			throw new Exception("Please Login First")
		}
	}
	
	//FUNCTION TO GET THE NETWORK UPDATES FROM LINKEDIN
	void getNetworkUpdates(ActionRequest request, ActionResponse response)
	{
		def context=request.context as NetworkUpdates
		//GET THE CURRENT USER
		User user=request.context.get("__user__")

		EntityManager em=JPA.em()
		EntityTransaction tx=em.getTransaction()
		tx.begin()
		
		SocialNetworking snType=SNService.getSnType("Linkedin")
		
		//GET PERSONAL CREDENTIALS
		PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
		
		//CHECK PERSONAL CREDENTIALS
		if(personalCredential!=null)
		{
			//GET APPLICATION CREDENTIALS
			ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)
			
			//CHECK APPLICATION CREDENTILAS
			if(applicationCredential!=null)
			{
				String consumerKeyValue=applicationCredential.apikey
				String consumerSecretValue=applicationCredential.apisecret
				String userToken=personalCredential.userToken
				String userTokenSecret=personalCredential.userTokenSecret

				//FUNCTION TO GET THE NETWORK UPDATES
				SNService.getNetworkUpdates(userToken,userTokenSecret,consumerKeyValue,consumerSecretValue,user,snType)
				tx.commit()
				response.flash="Networks Updates Fetched..."
			}
			//FOR APPLICATION CREDENTIALS
			else
			{
				throw new Exception("No Application Defined")
			}
		}
		//FOR PERSONAL CREDENTIAL
		else
		{
			throw new Exception("Please Login First")
		}
	}

	void getMembership(ActionRequest request, ActionResponse response)
	{
		User user=request.context.get("__user__")

		EntityManager em=JPA.em()
		EntityTransaction tx=em.getTransaction()
		tx.begin()

		SocialNetworking snType=SNService.getSnType("Linkedin")

		//GET THE PERSONAL CREDENTIAL
		PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
		//CHECK PERSONAL CREDENTIAL
		if(personalCredential !=null)
		{
			//GET THE APPLICATION CREDENTIAL
			ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)
			//CHECK APPLICATION CREDENTIAL
			if(applicationCredential !=null)
			{
				String consumerKeyValue=applicationCredential.apikey
				String consumerSecretValue=applicationCredential.apisecret
				String userToken=personalCredential.userToken
				String userTokenSecret=personalCredential.userTokenSecret
				SNService.getMembership(userToken, userTokenSecret, consumerKeyValue, consumerSecretValue, user, snType)
				tx.commit()
				response.flash="Group Memberships Obtained..."
			}
			//FOR APPLICATION CREDENTIAL
			else
			{
				throw new Exception("No Application Defined")
			}
		}
		//FOR PERSONAL CREDENTIAL
		else
		{
			throw new Exception("Please Login First")
		}
	}
	
	void getDiscussions(ActionRequest request, ActionResponse response)
	{
		
		User user=request.context.get("__user__")
		GroupMember groupMember=request.context.get("__self__")

		SocialNetworking snType=SNService.getSnType("Linkedin")

		//GET THE PERSONAL CREDENTIAL
		PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
		//CHECK PERSONAL CREDENTIAL
		if(personalCredential !=null)
		{
			//GET THE APPLICATION CREDENTIAL
			ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)

			//CHECK APPLICATION CREDENTIAL
			if(applicationCredential !=null)
			{
				String consumerKeyValue=applicationCredential.apikey
				String consumerSecretValue=applicationCredential.apisecret
				String userToken=personalCredential.userToken
				String userTokenSecret=personalCredential.userTokenSecret
				SNService.getDiscussions(userToken, userTokenSecret, consumerKeyValue, consumerSecretValue, user, groupMember,snType)
				response.flash="Group Discussions Obtained..."
			}
			//FOR APPLICATION CREDENTIAL
			else
			{
				throw new Exception("No Application Defined")
			}
		}
		//FOR PERSONAL CREDENTIAL
		else
		{
			throw new Exception("Please Login First")
		}
	}
	
	void refreshDiscussions(ActionRequest request,ActionResponse response)
	{
		def context =request.context as GroupMember
//		println(context.toString())
		
		List<GroupDiscussion> lstDiscussions=context.getDiscussions()
		
		List<GroupDiscussion> lstDiscussion=GroupDiscussion.all().filter("groupName=?", context).fetch()
		 //=JPA.em().createQuery("Select a from Comments a where a.contentId="+context.id,Comments.class).getResultList()

		for(int i=0;i<lstDiscussion.size();i++)
		{
			if(!lstDiscussions.contains(lstDiscussion.get(i)))
				lstDiscussions.add(lstDiscussion.get(i))
		}
		context.setDiscussions(lstDiscussions)
		response.values=context
	}
	
	void getDiscussionComments(ActionRequest request, ActionResponse response)
	{
			
		List<GroupDiscussionComments> posts=request.context.get("discussionComments")
		int start=posts.size()
		User user=request.context.get("__user__")
		String postId=request.context.get("discussionId")
		
		GroupDiscussion groupDiscussion=request.context.get("__self__")
		
		EntityManager em=JPA.em()
		EntityTransaction tx=em.getTransaction()
		tx.begin()

		SocialNetworking snType=SNService.getSnType("Linkedin")

		//GET THE PERSONAL CREDENTIAL
		PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
		//CHECK PERSONAL CREDENTIAL
		if(personalCredential!=null)
		{
			//GET THE APPLICATION CREDENTIAL
			ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)
			//CHECK APPLICATION CREDENTIAL
			if(applicationCredential != null)
			{
				String consumerKeyValue=applicationCredential.apikey
				String consumerSecretValue=applicationCredential.apisecret
				String userToken=personalCredential.userToken
				String userTokenSecret=personalCredential.userTokenSecret
				SNService.getDiscussionComments(userToken, userTokenSecret, consumerKeyValue, consumerSecretValue, user, groupDiscussion,snType,start)
				tx.commit()
				response.flash="Comments Fetched Successfully..."
			}
			//FOR APPLICATION CREDENTIAL
			else
			{
				throw new Exception("No Application Defined")
			}
		}
		//FOR PERSONAL CREDENTIAL
		else
		{
			throw new Exception("Please Login First")
		}
	}
	
	void postDiscussion(ActionRequest request, ActionResponse response)
	{
		def context=request.context as GroupDiscussion
		if(context.getId() == null)
		{
			User user=request.context.get("__user__")

			String title=request.context.get("discussionTitle")
			String summary=request.context.get("discussionSummary")
			GroupMember groupMember=request.context.get("groupName")
			String groupId=groupMember.groupId
			
			SocialNetworking snType=SNService.getSnType("Linkedin")

			//GET THE PERSONAL CREDENTIAL
			PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
			//CHECK PERSONAL CREDENTIAL
			if(personalCredential !=null)
			{
				//GET THE APPLICATION CREDENTIAL
				ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)
				//CHECK APPLICATION CREDENTIAL
				if(applicationCredential !=null)
				{
					String consumerKeyValue=applicationCredential.apikey
					String consumerSecretValue=applicationCredential.apisecret
					String userToken=personalCredential.userToken
					String userTokenSecret=personalCredential.userTokenSecret
					String postIdTime= SNService.addGroupDiscussion(userToken, userTokenSecret, consumerKeyValue, consumerSecretValue, title, summary, groupId)
					String[] array=postIdTime.split(":")
					DateTime date=new DateTime(Long.parseLong(array[1]));
					response.values=["discussionId":array[0],"discussionTime":date,"discussionBy":array[2]]
					response.flash="Succesfully Posted to Group "+groupMember.groupName.toUpperCase()+"..."
				}
				else
				{
					throw new Exception("No Application Defined")
				}
			}
			else
			{
				throw new Exception("Please Login First")
			}
		}
	}
	
	void addDiscussionComment(ActionRequest request, ActionResponse response)
	{
		List<GroupDiscussionComments> lstGroupDiscussionComments=request.context.get("discussionComments")
		int start=lstGroupDiscussionComments.size()
		String discussionId=request.context.get("discussionId")
		String comment=request.context.get("comment")
		User user=request.context.get("__user__")
		GroupDiscussion groupDiscussion=request.context.get("__self__")
		
		EntityManager em=JPA.em()
		EntityTransaction tx=em.getTransaction()
		tx.begin()
		
		SocialNetworking snType=SNService.getSnType("Linkedin")

		//GET THE PERSONAL CREDENTIAL
		PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
		//CHECK PERSONAL CREDENTIAL
		if(personalCredential != null)
		{
			//GET THE APPLICATION CREDENTIAL
			ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)
			//CHECK APPLICATION CREDENTIAL
			if(applicationCredential != null)
			{
				String consumerKeyValue=applicationCredential.apikey
				String consumerSecretValue=applicationCredential.apisecret
				String userToken=personalCredential.userToken
				String userTokenSecret=personalCredential.userTokenSecret
				SNService.addDiscussionComment(userToken, userTokenSecret, consumerKeyValue, consumerSecretValue, user,groupDiscussion,discussionId,comment,start,snType)
				tx.commit()
				response.flash="Comment Added..."
			}
			//FOR APPLICATION CREDENTIAL
			else
			{
				throw new Exception("No Application Defined")
			}
		}
		//FOR PERSONAL CREDENTIAL
		else
		{
			throw new Exception("Please Login First")
		}
	}
	
	void refreshDiscussionComments(ActionRequest request,ActionResponse response)
	{
		def context =request.context as GroupDiscussion
//		println(context.toString())
		
		List<GroupDiscussionComments> lstGroupDiscussionComments=context.getDiscussionComments()
		
//		List<GroupDiscussionComments> comment=JPA.em().createQuery("Select a from GroupDiscussionComments a where a.post="+context.id,GroupDiscussionComments.class).getResultList()
		List<GroupDiscussionComments> lstGroupDiscussionComment=GroupDiscussionComments.all().filter("discussion=?",context).fetch();
		
		for(int i=0;i<lstGroupDiscussionComment.size();i++)
		{
			if(!lstGroupDiscussionComments.contains(lstGroupDiscussionComment.get(i)))
				lstGroupDiscussionComments.add(lstGroupDiscussionComment.get(i))
		}
		context.setDiscussionComments(lstGroupDiscussionComments)
		response.values=context
	}
	
	void deleteDiscussion(ActionRequest request, ActionResponse response)
	{
		User user=request.context.get("__user__")
		List lstIdValues=request.context.get("_ids")
			
		SocialNetworking snType=SNService.getSnType("Linkedin")

		//GET THE PERSONAL CREDENTIAL
		PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
		//CHECK PERSONAL CREDENTIAL
		if(personalCredential !=null)
		{
			//GET THE APPLICATION CREDENTIAL
			ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)
			//CHECK APPLICATION CREDENTIAL
			if(applicationCredential!=null)
			{
				String consumerKeyValue=applicationCredential.apikey
				String consumerSecretValue=applicationCredential.apisecret
				String userToken=personalCredential.userToken
				String userTokenSecret=personalCredential.userTokenSecret
				String str=SNService.deleteDiscussion(lstIdValues, userToken, userTokenSecret, consumerKeyValue, consumerSecretValue, user)
				response.flash=str
			}
			else
			{
				throw new Exception("No Application Defined")
			}
		}
		else
		{
			throw new Exception("Please Login First")
		}
			
	}
}