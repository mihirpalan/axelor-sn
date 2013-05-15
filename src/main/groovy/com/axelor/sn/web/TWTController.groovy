package com.axelor.sn.web

import com.axelor.sn.db.ApplicationCredentials
import com.axelor.sn.db.DirectMessage;
import com.axelor.sn.db.ImportContact
import com.axelor.sn.db.PersonalCredential
import com.axelor.sn.db.PostTweet
import com.axelor.sn.db.SocialNetworking
import com.axelor.sn.db.TweetComment;
import com.axelor.sn.db.TwitterHomeTimeline
import com.axelor.sn.db.TwitterInbox;
import com.axelor.sn.service.SNTWTService
import javax.persistence.*
import javax.validation.ValidationException
import com.axelor.db.*
import com.axelor.auth.db.User
import javax.inject.Inject;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Request;


class TWTController {
	@Inject
	SNTWTService service;
	String apiKey;
	String apiSecret;
	String userToken;
	String userTokenSecret;
	String ack;


	void obtainToken(ActionRequest request,ActionResponse response) {
		User user = request.context.get("__user__")
		SocialNetworking sn = request.context.get("snType")
		def context = request.context as PersonalCredential
		ack = service.obtainToken(user,sn);
		if(ack.equals("You Already Have One Account Associated..."))
			throw new javax.validation.ValidationException("You Already Have One Account Associated...");

		else
			response.flash = "<a target=_blank href="+ack+">Click Here To Authorize The Application</a>";
	}

	void saySNType(ActionRequest request, ActionResponse response) {
		try {
			SocialNetworking snType = service.saySnType();
			response.values = ["snType":snType]
		}
		catch (Exception e) {
			response.flash = e.getMessage();
		}
	}

	void directMessage(ActionRequest request, ActionResponse response) {
		try {
			def context = request.context as DirectMessage
			User user = request.context.get("__user__")

			ImportContact contact = request.context.get("userId")
			if(context.msgcontent == null)
				throw new ValidationException("Please Specify Content");

			ack = service.sentMessage(user, contact.snUserId, context.msgcontent)

			if (ack.equals("0"))
				response.flash = "Please Authorise The Applicaton First";

			else if(!ack.isEmpty())
				response.values = ["acknowledgment":ack]

			else
				response.flash = "There is Some Problem please Try again after sometime"
		}
		catch(Exception e) {
			e.printStackTrace()
			throw new ValidationException("Please Specify All information");
		}
	}


	void getBoxMsg(ActionRequest request, ActionResponse response) {
		try {
			User user = request.context.get("__user__")
			def context = request.context as TwitterInbox
			ack = service.getInbox(user);
			if (ack.equals("0"))
				response.flash = "Please Authorise The Applicaton First";

			else if(ack.equals("1"))
				response.flash = "No data found!!!,Try again after sometime";

			else
				response.flash = ack;
		}
		catch (Exception e) {
			e.printStackTrace();
			response.flash = e.getMessage();
		}
	}


	void getAllContactsTwt(ActionRequest request, ActionResponse response) {

		String ack, sntype;
		User user = request.context.get("__user__")
		try {
			def context = request.context as ImportContact
			ack = service.importFollowers(user);
			if (ack.equals("0"))
				response.flash = "Please Authorise The Applicaton First";

			else if(ack.equals("1"))
				response.flash = "No data found!!!,Try again after sometime";

			else
				response.flash = ack;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}


	public void postOnTwitter(ActionRequest request, ActionResponse response) {
		try {
			User user = request.context.get("__user__")
			def context = request.context as PostTweet
			if( context.id == null ) {
				ack = service.postTweet(user, context.content)
				if (ack.equals("0"))
					response.flash = "Please Authorise The Applicaton First";

				else if(ack!= null)
					response.values = ["acknowledgment":ack];

				else
					response.flash = "There is Some Problem please Try again after sometime";
			}
		}
		catch (Exception e) {
			e.printStackTrace()
			response.flash = e.getMessage();
		}
	}


	public void fetchTimeline(ActionRequest request, ActionResponse response) {
		try {
			User user = request.context.get("__user__")
			String ack;
			def context = request.context as TwitterHomeTimeline
			ack = service.getHomeTimeLine(user);
			if (ack.equals("0"))
				response.flash = "Please Authorise The Applicaton First";

			else if(ack.equals("1"))
				response.flash = "No data found!!!";

			else
				response.flash = ack;
		}
		catch (Exception e) {
			e.printStackTrace();
			response.flash = e.getMessage();
		}
	}

	void getDeleteMessage(ActionRequest request, ActionResponse response) {
		try {
			User user = request.context.get("__user__")
			List lstIds = request.context.get("_ids");
			if(lstIds.empty)
				ack = "Please Select Record(s) and Click on Delete"

			else
				ack = service.getDeleteMessage(user,lstIds);

			response.flash = ack;
		}
		catch (Exception e) {
			e.printStackTrace();
			response.flash = e.getMessage();
		}
	}

	void deleteDirectMessage(ActionRequest request, ActionResponse response) {
		try {
			User user = request.context.get("__user__")
			List lstIds = request.context.get("_ids");
			if(lstIds.empty)
				ack = "Please Select Record(s) and Click on Delete"

			else
				ack = service.deleteDirectMessage(user, lstIds);
			response.flash = ack;
		}
		catch (Exception e) {
			e.printStackTrace();
			response.flash = e.getMessage();
		}
	}

	void getFollowerRequest(ActionRequest request, ActionResponse response) {
		try {
			User user = request.context.get("__user__")
			ack = service.getFollowerRequest(user);
			response.flash = ack;
		}
		catch (Exception e) {
			e.printStackTrace();
			response.flash = e.getMessage();
		}
	}

	public void postTweetReplay(ActionRequest request, ActionResponse response) {
		try {
			User user = request.context.get("__user__")
			String postedComment = request.context.get("postComment")
			def context = request.context as PostTweet
			ack = service.postTweetReplay(user, context.acknowledgment, postedComment)
			response.values = ["postComment":""]
			response.flash = ack;
		}
		catch (Exception e) {
			e.printStackTrace();
			response.flash = e.getMessage();
		}
	}

	public String getComments(ActionRequest request,ActionResponse response) {
		try {
			User user = request.context.get("__user__")
			def context = request.context as PostTweet
			String gid = context.getAcknowledgment();
			ack = service.orgGetTweetReplay(user,gid);
			if(ack.equals("0"))
				response.flash = "Please Authorise The Applicaton First";
			else if(ack.equals("1"))
				response.flash = "Please Try after Sometime, Twitter will takes time to return back Replay"
			else
				response.values = ["commentsTweet" : TweetComment.all().filter("curUser = ? and contentid = ?", user , context).fetch()]
		}
		catch(Exception e) {
			response.flash = e.getMessage();
			e.printStackTrace();
		}
	}

	public void clearAllData(ActionRequest request, ActionResponse response) {
		try {
			User user = request.context.get("__user__")
			List lstIds = request.context.get("_ids")
			if(lstIds.empty)
				ack = "Please Select Record and Click on Remove"

			else {
				long idVal = lstIds.get(0);
				ack = service.removeAllDetail(user, idVal)
			}
			response.flash = ack;
		}
		catch (java.lang.IndexOutOfBoundsException index) {
			response.flash = index.getMessage();
			index.printStackTrace();
		}
		catch (Exception e) {
			response.flash = e.getMessage();
			e.printStackTrace();
		}
	}

	// ADDED BY Mihir Palan From Here on To END
	void tweetToTwitter(ActionRequest request, ActionResponse response)
	{
		String order = "";
		com.axelor.contact.db.Contact c = request.context.get("customer")
		List prod = request.context.getAt("items")
		String orderNo = request.context.get("name")
		String date = request.context.get("orderDate").toString()
		order = c.fullName +" has ordered" + " " + prod.size() + " items with Order No. " + orderNo + " on Date: " + date
		response.values = ["content":order]
	}

	void postToTwitter(ActionRequest request, ActionResponse response)
	{
		String content = request.context.get("content")
		User user = request.context.get("__user__")
		String ack = service.postTweet(user, content)
		if (ack.equals("0"))
			response.flash = "Please Authorise The Applicaton First";

		else if(ack!= null)
		{
			PostTweet tweet = service.addTweet(content,user,ack)
			response.flash = "Successfully posted to Twitter"
			response.values = ["postTweet":tweet]
		}

		else
			response.flash = "There is Some Problem please Try again after sometime";



	}

	void getTweetsReply(ActionRequest request, ActionResponse response)
	{
		PostTweet postTweet = request.context.get("postTweet")
		User user = request.context.get("__user__")
		service.orgGetTweetReplay(user, postTweet.acknowledgment)
	}

	void fetchTweetsReply(ActionRequest request, ActionResponse response)
	{

		long id = request.context.get("id")
		if(id != null)
		{
			PostTweet postTweet = request.context.get("postTweet")
			if(postTweet != null)
			{
				List<TweetComment> lstTweetComment = service.fetchTweetsReply(postTweet)
				response.values=["tweets":lstTweetComment]
			}
		}

	}
	void clearTweetReply(ActionRequest request, ActionResponse response) {
		response.values=["tweetReply":""]
	}

	public void postTweetAsReplay(ActionRequest request, ActionResponse response) {
		try	{
			User user = request.context.get("__user__")
			String postComment = request.context.get("tweetReply")
			PostTweet postTweet = request.context.get("postTweet")
			ack = service.postTweetReplay(user, postTweet.acknowledgment, postComment)
			if(ack != null)
				response.flash = ack;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			response.flash = e.getMessage();
		}
	}



}
