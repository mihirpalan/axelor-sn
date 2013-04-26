package com.axelor.sn.web

import java.util.EnumSet;

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
import com.google.code.linkedinapi.client.enumeration.NetworkUpdateType;
import com.google.code.linkedinapi.client.oauth.LinkedInAccessToken;
import com.google.code.linkedinapi.schema.Connections
import com.google.code.linkedinapi.schema.Person
import com.google.common.cache.LocalCache.Values;

class Controller
{
	/**
	 *This function is used to get  the Authorization URL
	 *Clicking this will go to a page where our application will be Authorized by the user 
	 */
	void getUrl(ActionRequest request, ActionResponse response)
	{
		def context=request.context as PersonalCredential

		User user=request.context.get("__user__")
		SocialNetworking snType=request.context.get("snType")
		PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
		if(personalCredential !=null)
		{
			throw new Exception("You Already Have One Account Associated...")
		}
		else
		{
			String consumerKey,consumerSecret;
			ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)
			if(applicationCredential != null)
			{
				consumerKey=applicationCredential.apikey
				consumerSecret=applicationCredential.apisecret
				String authUrl=SNService.getUrl(consumerKey, consumerSecret,user,snType)
				response.flash ="Click the link to get access <a href="+authUrl+" target='_blank'>"+authUrl+"</a>"
			}
			else
			{
				response.flash="No Application Defined..."
			}
		}
	}
	
	void networkType(ActionRequest request, ActionResponse response)
	{
		SocialNetworking snType=SNService.getSnType("Linkedin")
		if(snType!=null)
			response.values=["snType":snType]
		else
			throw new Exception("Network Type not Found...")
	}
	
	/**
	 *This function is used to get our connections from Linkedin 
	 */
	void fetchConnections(ActionRequest request, ActionResponse response)
	{
		def context=request.context as ImportContact
		User user=request.context.get("__user__")
		SocialNetworking snType=SNService.getSnType("Linkedin")
		if(snType !=null)
		{
			PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
			if(personalCredential==null)
			{
				throw new Exception("Please Login First")
			}
			else
			{
				ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)
				if(applicationCredential!=null)
				{
					String consumerKeyValue=applicationCredential.apikey
					String consumerSecretValue=applicationCredential.apisecret
					String userToken=personalCredential.userToken
					String userTokenSecret=personalCredential.userTokenSecret

					SNService.fetchConnections(consumerKeyValue, consumerSecretValue, userToken, userTokenSecret,user,snType)
					response.flash="Imported Contacts Successfully..."
				}
				else
				{
					response.flash = "No Application Defined..."
				}
			}
		}
		else
		{
			throw new Exception("Network Type not Found...")
		}
	}
	
	/**
	 *This function is used to Send a Direct Message to a Contact on Linkedin  
	 */
	void sendMessage(ActionRequest  request, ActionResponse response)
	{
		User user=request.context.get("__user__")
		ImportContact contact=request.context.get("userid")

		String userId=contact.getUserId()
		String subject=request.context.get("subject")
		String message=request.context.get("msgcontent").toString()

		SocialNetworking snType=SNService.getSnType("Linkedin")
		if(snType !=null)
		{
			PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
			if(personalCredential != null)
			{
				ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)
				if(applicationCredential != null)
				{
					String consumerKeyValue=applicationCredential.apikey
					String consumerSecretValue=applicationCredential.apisecret
					String userToken=personalCredential.userToken
					String userTokenSecret=personalCredential.userTokenSecret

					SNService.sendMessage(userId,subject,message,userToken,userTokenSecret,consumerKeyValue,consumerSecretValue)
					response.flash = "Message Sent..."
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
		else
		{
			throw new Exception("Network Type not Found...")
		}
	}
	
	/**
	 *This function is used to post a new Status to Linkedin 
	 */
	void updateStatus(ActionRequest request, ActionResponse response)
	{
		def context=request.context as PostUpdates

		if(context.getId() == null)
		{
			User user=request.context.get("__user__")
			String message=request.context.get("content").toString()
			SocialNetworking snType=SNService.getSnType("Linkedin")
			if(snType != null)
			{
				PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
				if(personalCredential != null)
				{
					ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)
					if(applicationCredential != null)
					{
						String consumerKeyValue=applicationCredential.apikey
						String consumerSecretValue=applicationCredential.apisecret
						String userToken=personalCredential.userToken
						String userTokenSecret=personalCredential.userTokenSecret

						String updateKeyTime=SNService.updateStatus(message, userToken, userTokenSecret,consumerKeyValue, consumerSecretValue)
						String[] array=updateKeyTime.split(":")
						DateTime date=new DateTime(Long.parseLong(array[1]));
						response.values=["contentId":array[0],"postTime":date]
						response.flash="Status Successfully Updated to LinkedIn..."
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
			else
			{
				throw new Exception("Network Type not Found...")
			}
		}
	}
	
	/**
	 *This function is used to get the Comments of a Status from Linkedin
	 */
	void getComments(ActionRequest request, ActionResponse response)
	{

		String contentId=request.context.get("contentId")
		if(!contentId.equals(null))
		{
			User user=request.context.get("__user__")

			SocialNetworking snType=SNService.getSnType("Linkedin")
			if(snType!=null)
			{
				PersonalCredential personalCredential=SNService.getPersonalCredential(user, snType)
				if(personalCredential !=null)
				{
					ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)

					if(applicationCredential != null)
					{
						String consumerKeyValue=applicationCredential.apikey
						String consumerSecretValue=applicationCredential.apisecret
						String userToken=personalCredential.userToken
						String userTokenSecret=personalCredential.userTokenSecret

						SNService.getComments(contentId, userToken, userTokenSecret, consumerKeyValue, consumerSecretValue,user,snType)
						response.flash="Comments Retrieved..."
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
			else
			{
				throw new Exception("Network Type not Found...")
			}
		}
		else
		{
			response.flash="Select A Status to Fetch Comments..."
		}
	}

	//Clears the Comment Field	
	void clearCommentfield(ActionRequest request, ActionResponse response)
	{
		response.values=["comment":""]
	}
	
	
	/**
	 *This function is used to refresh the Comments O2M field  
	 */
	void refreshComments(ActionRequest request,ActionResponse response)
	{
		def context =request.context as PostUpdates
		
		List<Comments> lstComments=context.getComments()
		
		List<Comments> lstComment=SNService.refreshComments(context)

		for(int i=0;i<lstComment.size();i++)
		{
			if(!lstComments.contains(lstComment.get(i)))
				lstComments.add(lstComment.get(i))
		}
		context.setComments(lstComments)
		response.values=context
	}
	
	/**
	 *This function is used to add a Comment to Status  
	 */
	void addStatusComment(ActionRequest request, ActionResponse response)
	{
		
		String contentId=request.context.get("contentId")
		String comment=request.context.get("comment")
		User user=request.context.get("__user__")
		PostUpdates postUpdates=request.context.get("__self__")
		
		SocialNetworking snType=request.context.get("snType")

		PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
		if(personalCredential != null)
		{
			ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)

			if(applicationCredential != null )
			{
				String consumerKeyValue=applicationCredential.apikey
				String consumerSecretValue=applicationCredential.apisecret
				String userToken=personalCredential.userToken
				String userTokenSecret=personalCredential.userTokenSecret

				SNService.addStatusComment(userToken, userTokenSecret, consumerKeyValue, consumerSecretValue, user, contentId, comment)
				response.flash="Comment Added..."
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
	
	void getNetworkUpdates(ActionRequest request, ActionResponse response)
	{
		def context=request.context as NetworkUpdates
		User user=request.context.get("__user__")

		SocialNetworking snType=SNService.getSnType("Linkedin")
		if(snType!=null)
		{
			PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
			if(personalCredential!=null)
			{
				ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)

				if(applicationCredential!=null)
				{
					String consumerKeyValue=applicationCredential.apikey
					String consumerSecretValue=applicationCredential.apisecret
					String userToken=personalCredential.userToken
					String userTokenSecret=personalCredential.userTokenSecret

					SNService.getNetworkUpdates(userToken,userTokenSecret,consumerKeyValue,consumerSecretValue,user,snType)
					response.flash="Networks Updates Fetched..."
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
		else
		{
			throw new Exception("Network Type not Found...")
		}
	}

	/**
	 *This function is used to obtain the Memberships to a Group from Linkedin 
	 */
	void getMembership(ActionRequest request, ActionResponse response)
	{
		User user=request.context.get("__user__")

		SocialNetworking snType=SNService.getSnType("Linkedin")
		if(snType!=null)
		{
			PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
			if(personalCredential !=null)
			{
				ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)
				if(applicationCredential !=null)
				{
					String consumerKeyValue=applicationCredential.apikey
					String consumerSecretValue=applicationCredential.apisecret
					String userToken=personalCredential.userToken
					String userTokenSecret=personalCredential.userTokenSecret

					SNService.getMembership(userToken, userTokenSecret, consumerKeyValue, consumerSecretValue, user, snType)
					response.flash="Group Memberships Obtained..."
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
		else
		{
			throw new Exception("Network Type not Found...")
		}
	}

	/**
	 *This function is used to get the Discussions from a particular Group 	
	 */
	void getDiscussions(ActionRequest request, ActionResponse response)
	{
		
		User user=request.context.get("__user__")
		GroupMember groupMember=request.context.get("__self__")

		SocialNetworking snType=SNService.getSnType("Linkedin")
		if(snType!=null)
		{
			PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
			if(personalCredential !=null)
			{
				ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)

				if(applicationCredential !=null)
				{
					String consumerKeyValue=applicationCredential.apikey
					String consumerSecretValue=applicationCredential.apisecret
					String userToken=personalCredential.userToken
					String userTokenSecret=personalCredential.userTokenSecret

					SNService.getDiscussions(userToken, userTokenSecret, consumerKeyValue, consumerSecretValue, user, groupMember,snType)
					response.flash="Group Discussions Obtained..."
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
		else
		{
			throw new Exception("Network Type not Found...")
		}
	}
	
	/**
	 *This function is used to refresh the Discussions field in the view 
	 */
	void refreshDiscussions(ActionRequest request,ActionResponse response)
	{
		def context =request.context as GroupMember
		
		List<GroupDiscussion> lstDiscussions=context.getDiscussions()
		
		List<GroupDiscussion> lstDiscussion=SNService.refreshDiscussions(context)

		for(int i=0;i<lstDiscussion.size();i++)
		{
			if(!lstDiscussions.contains(lstDiscussion.get(i)))
				lstDiscussions.add(lstDiscussion.get(i))
		}
		context.setDiscussions(lstDiscussions)
		response.values=context
	}
	
	/**
	 * This function is used to get the comments from a particular Discussion 
	 */
	void getDiscussionComments(ActionRequest request, ActionResponse response)
	{
			
		List<GroupDiscussionComments> posts=request.context.get("discussionComments")
		int start=posts.size()
		User user=request.context.get("__user__")
		String postId=request.context.get("discussionId")

		GroupDiscussion groupDiscussion=request.context.get("__self__")

		SocialNetworking snType=SNService.getSnType("Linkedin")
		if(snType!=null)
		{
			PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
			if(personalCredential!=null)
			{
				ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)
				if(applicationCredential != null)
				{
					String consumerKeyValue=applicationCredential.apikey
					String consumerSecretValue=applicationCredential.apisecret
					String userToken=personalCredential.userToken
					String userTokenSecret=personalCredential.userTokenSecret

					SNService.getDiscussionComments(userToken, userTokenSecret, consumerKeyValue, consumerSecretValue, user, groupDiscussion,snType,start)
					response.flash="Comments Fetched Successfully..."
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
		else
		{
			throw new Exception("Network Type not Found...")
		}
	}
	
	/**
	 *This function is used to post a new discussion to a particular Group 
	 */
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
			if(snType!=null)
			{
				PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
				if(personalCredential !=null)
				{
					ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)
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
			else
			{
				throw new Exception("Network Type not Found...")
			}
		}
	}
	
	//This function is used to add anew comment on a particular Discussion in a group
	void addDiscussionComment(ActionRequest request, ActionResponse response)
	{
		List<GroupDiscussionComments> lstGroupDiscussionComments=request.context.get("discussionComments")
		int start=lstGroupDiscussionComments.size()
		String discussionId=request.context.get("discussionId")
		String comment=request.context.get("comment")
		User user=request.context.get("__user__")
		GroupDiscussion groupDiscussion=request.context.get("__self__")

		SocialNetworking snType=SNService.getSnType("Linkedin")
		if(snType!=null)
		{
			PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
			if(personalCredential != null)
			{
				ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)
				if(applicationCredential != null)
				{
					String consumerKeyValue=applicationCredential.apikey
					String consumerSecretValue=applicationCredential.apisecret
					String userToken=personalCredential.userToken
					String userTokenSecret=personalCredential.userTokenSecret
					SNService.addDiscussionComment(userToken, userTokenSecret, consumerKeyValue, consumerSecretValue, user,groupDiscussion,discussionId,comment,start,snType)
					response.flash="Comment Added..."
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
		else
		{
			throw new Exception("Network Type not Found...")
		}
	}
	
	//This function refreshes the view with Discussion Comments
	void refreshDiscussionComments(ActionRequest request,ActionResponse response)
	{
		def context =request.context as GroupDiscussion
		
		List<GroupDiscussionComments> lstGroupDiscussionComments=context.getDiscussionComments()
		
		List<GroupDiscussionComments> lstGroupDiscussionComment=SNService.refreshDiscussionComments(context)
		
		for(int i=0;i<lstGroupDiscussionComment.size();i++)
		{
			if(!lstGroupDiscussionComments.contains(lstGroupDiscussionComment.get(i)))
				lstGroupDiscussionComments.add(lstGroupDiscussionComment.get(i))
		}
		context.setDiscussionComments(lstGroupDiscussionComments)
		response.values=context
	}
	
	//This function is used to delete a discussion from Linkedin
	void deleteDiscussion(ActionRequest request, ActionResponse response)
	{
		User user=request.context.get("__user__")
		List lstIdValues=request.context.get("_ids")

		SocialNetworking snType=SNService.getSnType("Linkedin")
		if(snType!=null)
		{
			PersonalCredential personalCredential=SNService.getPersonalCredential(user,snType)
			if(personalCredential !=null)
			{
				ApplicationCredentials applicationCredential=SNService.getApplicationCredential(snType)
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
		else
		{
			throw new Exception("Network Type not Found...")
		}
	}
}