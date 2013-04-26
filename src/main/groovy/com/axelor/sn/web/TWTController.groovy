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
import com.axelor.db.*
import com.axelor.auth.db.User
import javax.inject.Inject;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Request;


class TWTController
{
	@Inject
	SNTWTService service;
	String apiKey;
	String apiSecret;
	String userToken;
	String userTokenSecret;
	String ack;

	void enterPin(ActionRequest request,ActionResponse response)
	{
		List keys=request.context.keySet().asList()
		List values=request.context.values().asList()
		User user=values.get(keys.indexOf("__user__"))
		SocialNetworking sn=values.get(keys.indexOf("snType"))
		String pin=request.context.get("pinTwt")
		ack=service.storingToken(user, pin);
		response.flash=ack;

	}
	void obtainToken(ActionRequest request,ActionResponse response)
	{
		def context=request.context as PersonalCredential
		List keys=request.context.keySet().asList()
		List values=request.context.values().asList()
		User user=values.get(keys.indexOf("__user__"))
		SocialNetworking sn=values.get(keys.indexOf("snType"))
		ack=service.obtainToken(user,sn);
		if(ack.equals("You Already Have One Account Associated..."))
		{
			throw new javax.validation.ValidationException("You Already Have One Account Associated...");
		}
		else
		{
			response.flash="<a href=$ack target=_blank> Please Click Here </a>"
		}
		println(ack)
	}


	void saySNType(ActionRequest request,ActionResponse response)
	{
		try
		{
			println("called!!!!")
			SocialNetworking snType=service.saySnType();
			println(snType);
			response.values=["snType":snType]
		}
		catch (Exception e)
		{
			response.flash=e.getMessage();
		}
	}

	void getAuthentication(ActionRequest request,ActionResponse response)
	{

		String userName;
		String password,ack;
		String[] serviceValue=new String[2]
		try
		{
			userName=request.context.get("snUsername");
			password=request.context.get("snPassword");
			def context=request.context as PersonalCredential
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"))
			SocialNetworking sn=values.get(keys.indexOf("snType"))

			ack=service.getUserToken(user,sn,userName,password);

			String noVal=""
			response.values=["password":noVal]
			response.flash=ack;
		}
		catch(Exception e)
		{
			response.flash=e.getMessage()
		}
	}


	void directMessage(ActionRequest request,ActionResponse response)
	{
		try
		{
			def context=request.context as DirectMessage


			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"))
			ImportContact contact=values.get(keys.indexOf("userId"))
			ack=service.sentMessage(user,contact.userId, context.msgcontent)
			if(Character.isDigit(ack.charAt(0)))
			{
				response.values=["acknowledgment":ack]
				response.flash="Message Sent Successfully!!!"
			}
			else
			{
				response.flash="There is Some Problem please Try again after sometime"
			}

		}
		catch(Exception e)
		{
			e.printStackTrace()
			response.flash=e.getMessage();
		}
	}


	void getBoxMsg(ActionRequest request,ActionResponse response)
	{
		try
		{
			def context=request.context as TwitterInbox
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"))
			ack=service.getInbox(user);
			response.flash=ack;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			response.flash=e.getMessage();
		}
	}


	void getAllContactsTwt(ActionRequest request,ActionResponse response)
	{

		String ack,sntype;
		try
		{	def context=request.context as ImportContact
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"))
			ack=service.importFollowers(user);
			response.flash=ack;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}


	public void postOnTwitter(ActionRequest request,ActionResponse response)
	{
		try
		{
			def context=request.context as PostTweet
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"))
			ack=service.postTweet(user, context.content)
			if(!ack.startsWith("[a-zA-Z]"))
			{
				response.values=["acknowledgment":ack]
				response.flash="Your Tweet is been Posted Successfully!!";
			}
			else
			{
				response.flash=ack;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace()
			response.flash=e.getMessage();
		}
	}


	public void fetchTimeline(ActionRequest request,ActionResponse response)
	{
		try
		{
			String ack;
			def context=request.context as TwitterHomeTimeline
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"))
			ack=service.getHomeTimeLine(user);
			response.flash=ack;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			response.flash=e.getMessage();
		}
	}

	void getDeleteMessage(ActionRequest request,ActionResponse response)
	{
		try
		{
			println(request.context.toString())
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"));
			List ids=values.get(keys.indexOf("_ids"));
			ack=service.getDeleteMessage(user,ids);
			response.flash=ack;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			response.flash=e.getMessage();
		}
	}

	void deleteDirectMessage(ActionRequest request,ActionResponse response)
	{
		try
		{
			println(request.context.toString())
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"));
			List ids=values.get(keys.indexOf("_ids"));
			ack=service.deleteDirectMessage(user,ids);
			response.flash=ack;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			response.flash=e.getMessage();
		}
	}

	void getFollowerRequest(ActionRequest request,ActionResponse response)
	{
		try
		{
			println(request.context.toString())
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"));
			ack=service.getFollowerRequest(user);
			response.flash=ack;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			response.flash=e.getMessage();
		}
	}

	public void sayResponse(ActionRequest request,ActionResponse respose)
	{
		println(request.context.toString())
	}

	public void postTweetReplay(ActionRequest request,ActionResponse response)
	{
		try
		{
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"));
			String postedComment=values.get(keys.indexOf("postComment"))
			def context = request.context as PostTweet
			context.acknowledgment;
			ack=service.postTweetReplay(user,context.acknowledgment,postedComment)
			response.flash=ack;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			response.flash=e.getMessage();
		}
	}

	//	public void getSearchResult(ActionRequest request, ActionResponse response)
	//	{
	//		println request.context.toString();
	//		try
	//		{
	//			List keys=request.context.keySet().asList();
	//			List values=request.context.values().asList();
	//			User user=values.get(keys.indexOf("__user__"));
	//			ack=service.searchTwitter(user,values.get(keys.indexOf("tweetSearch")) )//
	//			response.flash=ack;
	//		}
	//		catch (Exception e)
	//		{
	//			e.printStackTrace();
	//		}
	//	}

	public String getComments(ActionRequest request,ActionResponse response)
	{
		try
		{	def context=request.context as PostTweet
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"))
			String gid=context.getAcknowledgment();
			ack=service.orgPostTweetReplay(user,gid);
			response.flash=ack;			
		}
		catch(Exception e)
		{
			response.flash=e.getMessage();
			e.printStackTrace();
		}
		
	}
	
	// ADDED BY ME ON 15-04-2013
	void tweetToTwitter(ActionRequest request, ActionResponse response)
	{
		//		List keys=request.context.keySet().asList()
		//		List values=request.context.values().asList()

		//		for(int i=0;i<keys.size;i++)
		//			println(keys.get(i).toString() +":"+ values.get(i).toString())

		String order="";
		com.axelor.contact.db.Contact c=request.context.get("customer")
		List prod=request.context.getAt("items")
		String orderNo=request.context.get("name")
		String date=request.context.get("orderDate").toString()

		//		String d=sdf.format(date)

		//		order=c.firstName+" "+c.lastName+" has ordered"+" "+prod.size() + " items with Order No. "+orderNo+" on Date: "+date
		order=c.fullName+" has ordered"+" "+prod.size() + " items with Order No. "+orderNo+" on Date: "+date

		//		order +="\n"+prod.size()
		//		response.flash=order
		response.values=["content":order]
	}

	void postToTwitter(ActionRequest request,ActionResponse response)
	{
		List keys=request.context.keySet().asList()
		List values=request.context.values().asList()

		//		for(int i=0;i<keys.size;i++)
		//			println(keys.get(i).toString() +":"+ values.get(i).toString())

		String content=request.context.get("content")
		User user=request.context.get("__user__")
		String ack=service.postTweet(user, content)
		println("Mihir ACK"+ack)
		if(!ack.startsWith("[a-zA-Z]"))
		{
			PostTweet tweet= service.addTweet(content,user,ack)
			response.flash="Successfully posted to Twitter"
			response.values=["postTweet":tweet]
		}
		else
		{
			response.flash=ack;
			//throw new Exception("There's some Problem")
		}
	}

	void getTweetsReply(ActionRequest request,ActionResponse response)
	{
		PostTweet postTweet=request.context.get("postTweet")
		User user=request.context.get("__user__")
		service.orgPostTweetReplay(user, postTweet.acknowledgment)
	}

	void fetchTweetsReply(ActionRequest request,ActionResponse response)
	{

		long id=request.context.get("id")
		if(id !=null)
		{
			PostTweet postTweet=request.context.get("postTweet")
			if(postTweet != null)
			{
				List<TweetComment> lstTweetComment=service.fetchTweetsReply(postTweet)
				response.values=["tweets":lstTweetComment]
			}
		}

	}
	void clearTweetReply(ActionRequest request,ActionResponse response)
	{
		response.values=["tweetReply":""]
	}

	public void postTweetAsReplay(ActionRequest request,ActionResponse response)
	{
		try
		{
			User user=request.context.get("__user__")
			String postComment=request.context.get("tweetReply")
			PostTweet postTweet = request.context.get("postTweet")
			ack=service.postTweetReplay(user,postTweet.acknowledgment,postComment)
			if(!ack.startsWith("[a-zA-Z]"))
			{
				response.flash=ack;
			}
			else
			{
				response.flash=ack;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			response.flash=e.getMessage();
		}
	}
}
