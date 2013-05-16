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
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

class Controller {
	
	@Inject
	SNService LinkedinService;
	
	/**
	 *This function is used to get  the Authorization URL
	 *Clicking this will go to a page where our application will be Authorized by the user 
	 */
	void getUrl(ActionRequest request, ActionResponse response) {

		def context = request.context as PersonalCredential
		User user = request.context.get("__user__")
		SocialNetworking snType = request.context.get("snType")
		String authUrl = LinkedinService.getUrl(user, snType)
		response.flash = "Click the link to get access <a href=" + authUrl + " target='_blank'>" + authUrl + "</a>"
	}

	void networkType(ActionRequest request, ActionResponse response) {
		SocialNetworking snType = LinkedinService.getSnType("Linkedin")
		if(snType != null)
			response.values = ["snType":snType]
		else
			throw new Exception("Network Type not Found...")
	}

	
	//This function is used to get our connections from Linkedin 
	void fetchConnections(ActionRequest request, ActionResponse response) {
		String acknowlegement
		User user = request.context.get("__user__")
		acknowlegement = LinkedinService.fetchConnections(user)
		response.flash = acknowlegement
	}

	 //This function is used to Send a Direct Message to a Contact on Linkedin  
	void sendMessage(ActionRequest  request, ActionResponse response) {
		User user = request.context.get("__user__")
		ImportContact contact = request.context.get("userid")
		String userId = contact.getSnUserId()
		String subject = request.context.get("subject")
		String message = request.context.get("msgcontent")
		String acknowlegement = LinkedinService.sendMessage(userId, subject, message, user)
		response.flash = acknowlegement
	}

	 //This function is used to post a new Status to Linkedin 
	void updateStatus(ActionRequest request, ActionResponse response) {
		def context = request.context as PostUpdates
		if(context.getId() == null) {
			User user = request.context.get("__user__")
			String content = request.context.get("content").toString()
			HashMap updateKeyTime = LinkedinService.updateStatus(content, user)
			String contentId = updateKeyTime.get("updateId")
			DateTime date = new DateTime(updateKeyTime.get("updateTimeStamp"));
			response.values = ["contentId" : contentId, "postTime" : date]
			response.flash = "Status Successfully Updated to LinkedIn..."
		}
	}

	 //This function is used to get the Comments of a Status from Linkedin
	void getComments(ActionRequest request, ActionResponse response) {
		String contentId = request.context.get("contentId")
		if(!contentId.equals(null)) {
			User user = request.context.get("__user__")
			LinkedinService.getComments(contentId, user)
		}
		else
			response.flash="Select A Status to Fetch Comments..."
	}

	//Clears the Comment Field
	void clearCommentfield(ActionRequest request, ActionResponse response) {
		response.values=["comment":""]
	}

	 //This function is used to refresh the Comments O2M field  
	void refreshComments(ActionRequest request, ActionResponse response) {
		def context = request.context as PostUpdates
		List<Comments> lstComments = context.getComments()
		List<Comments> lstComment = LinkedinService.refreshComments(context)
		for(int i=0; i<lstComment.size(); i++) {
			if(!lstComments.contains(lstComment.get(i)))
				lstComments.add(lstComment.get(i))
		}
		context.setComments(lstComments)
		response.values = context
	}

	 //This function is used to add a Comment to Status  
	void addStatusComment(ActionRequest request, ActionResponse response) {
		String contentId = request.context.get("contentId")
		String comment = request.context.get("comment")
		User user = request.context.get("__user__")
		LinkedinService.addStatusComment(user, contentId, comment)
	}

	void getNetworkUpdates(ActionRequest request, ActionResponse response) {
		User user = request.context.get("__user__")
		String acknowledgement = LinkedinService.fetchNetworkUpdates(user)
		response.flash = acknowledgement
	}

	 //This function is used to obtain the Memberships to a Group from Linkedin 
	void getMembership(ActionRequest request, ActionResponse response) {
		User user = request.context.get("__user__")
		String acknowledgement = LinkedinService.getMembership(user)
		response.flash = acknowledgement
	}

	 //This function is used to get the Discussions from a particular Group 	
	void getDiscussions(ActionRequest request, ActionResponse response) {
		User user = request.context.get("__user__")
		GroupMember groupMember = request.context.get("__self__")
		LinkedinService.getDiscussions(user, groupMember)
	}

	 //This function is used to refresh the Discussions field in the view 
	void refreshDiscussions(ActionRequest request,ActionResponse response) {
		def context = request.context as GroupMember
		List<GroupDiscussion> lstDiscussions = context.getDiscussions()
		List<GroupDiscussion> lstDiscussion = LinkedinService.refreshDiscussions(context)
		for(int i=0; i<lstDiscussion.size(); i++) {
			if(!lstDiscussions.contains(lstDiscussion.get(i)))
				lstDiscussions.add(lstDiscussion.get(i))
		}
		context.setDiscussions(lstDiscussions)
		response.values = context
	}
	
	//This function is used to post a new discussion to a particular Group
   void postDiscussion(ActionRequest request, ActionResponse response) {
	   def context = request.context as GroupDiscussion
	   if(context.getId() == null) 	{
		   User user = request.context.get("__user__")
		   String title = request.context.get("discussionTitle")
		   String summary = request.context.get("discussionSummary")
		   GroupMember groupMember = request.context.get("groupName")
		   String groupId = groupMember.groupId
		   HashMap discussionIdTime = LinkedinService.addGroupDiscussion(title, summary, groupId, user)
		   String discussionId = discussionIdTime.get("id").toString()
		   DateTime date = new DateTime(discussionIdTime.get("time"))
		   String discussionBy = discussionIdTime.get("by").toString()
		   response.values = ["discussionId" : discussionId, "discussionTime" : date, "discussionBy" : discussionBy]
		   response.flash = "Succesfully Posted to Group "+groupMember.groupName.toUpperCase()+"..."
	   }
   }

	 // This function is used to get the comments from a particular Discussion 
	void getDiscussionComments(ActionRequest request, ActionResponse response) {
		User user = request.context.get("__user__")
		GroupDiscussion groupDiscussion = request.context.get("__self__")
		LinkedinService.getDiscussionComments(user, groupDiscussion)
	}

	//This function is used to add anew comment on a particular Discussion in a group
	void addDiscussionComment(ActionRequest request, ActionResponse response) {
		String comment = request.context.get("comment")
		User user = request.context.get("__user__")
		GroupDiscussion groupDiscussion = request.context.get("__self__")
		LinkedinService.addDiscussionComment(user, groupDiscussion, comment)
	}

	//This function refreshes the view with Discussion Comments
	void refreshDiscussionComments(ActionRequest request,ActionResponse response) {
		def context = request.context as GroupDiscussion
		List<GroupDiscussionComments> lstGroupDiscussionComments = context.getDiscussionComments()
		List<GroupDiscussionComments> lstGroupDiscussionComment = LinkedinService.refreshDiscussionComments(context)
		for(int i=0; i<lstGroupDiscussionComment.size(); i++) {
			if(!lstGroupDiscussionComments.contains(lstGroupDiscussionComment.get(i)))
				lstGroupDiscussionComments.add(lstGroupDiscussionComment.get(i))
		}
		context.setDiscussionComments(lstGroupDiscussionComments)
		response.values = context
	}

	//This function is used to delete a discussion from Linkedin
	void deleteDiscussion(ActionRequest request, ActionResponse response) {
		User user = request.context.get("__user__")
		List lstIdValues = request.context.get("_ids")
		response.flash = LinkedinService.deleteDiscussion(lstIdValues, user)
	}
	
	void unAuthorizeApp(ActionRequest request, ActionResponse response) {
		User user = request.context.get("__user__")
		response.flash = LinkedinService.unAuthorize(user)
	}
}