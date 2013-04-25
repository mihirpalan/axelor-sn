package com.axelor.sn.web
import java.util.Formatter.DateTime;
import com.axelor.auth.db.User
import com.axelor.db.*
import javax.inject.Inject
import javax.persistence.*;
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime;
import org.joda.time.format.*;
import com.axelor.db.mapper.types.JodaAdapter.DateTimeAdapter;
import com.axelor.db.mapper.types.JodaAdapter.LocalDateAdapter;
import com.axelor.meta.views.Action.Act;
import com.axelor.meta.views.Action.ActionRecord;
import com.axelor.meta.views.Search;
import com.axelor.rpc.ActionRequest
import com.axelor.rpc.ActionResponse
import com.axelor.rpc.Request;
import com.axelor.sn.db.*;
import com.axelor.meta.db.*;
import com.axelor.sn.service.SNFBService
import com.fasterxml.jackson.databind.node.NodeCursor.Array;
import com.google.inject.matcher.Matchers.Returns;


import java.util.ArrayList;

import javax.inject.Inject;

import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.sn.service.SNFBService;

public class FBController
{
	@Inject
	SNFBService service;

	String ack="";
	String apikey;
	String apisecret;
	String userName,password

	void getAuthUrlFB(ActionRequest request,ActionResponse response)
	{
		def context=request.context as PersonalCredential
		List keys=request.context.keySet().asList()
		List values=request.context.values().asList()
		User user=values.get(keys.indexOf("__user__"))
		SocialNetworking sn=values.get(keys.indexOf("snType"))
		println ("OUTSIDE Values")
		ack=service.obtainAuthUrl(user,sn);
		if(ack.startsWith("You Already Have One Account Associated..."))
		{
			response.flash=ack;
		}
		else
		{
			response.flash="<a href=$ack target=_blank> Please Click Here </a>"
		}
		println(ack)

	}
	void searchPerson(ActionRequest request,ActionResponse response)
	{
		println("Search Person"+request.context.toString())
		String userToken;
		try
		{
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"))
			def context = request.context as SearchPerson
			String param = context.searchparam
			ack=service.searchPerson(user,param)
			if(ack.equals("1"))
			{
				response.view=[title: "Search Grid", resource: SearchPerson.class.name, viewType: "grid"]
				ack="Search Completed Please Click on Reresh"
				response.flash=ack;
			}
			else
			{
				ack="Some Problem is there"
			}
		}
		catch(Exception e)
		{
			e.printStackTrace()
			ack=e.getMessage()
			response.flash=ack;
		}
	}

	void saySNType(ActionRequest request, ActionResponse response)
	{
		try
		{
			println("saySnType")
			SocialNetworking snType=SocialNetworking.all().filter("name=?", "Facebook").fetchOne()
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
		try
		{
			//request.context.
			String noVal=""
			println("Its been Called")
			def context=request.context as PersonalCredential
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"))
			SocialNetworking sn=values.get(keys.indexOf("snType"))
			println ("OUTSIDE Values")

			userName=context.snUsername
			password=context.password
			ack=service.getUserToken(user,sn, userName, password);
			if(ack.startsWith("AA"))
			{
				response.values=["password":noVal,"userToken":ack]
				response.flash="Login Successfully You may Proceed!!";
			}
			else
			{
				response.values=["password":noVal]
				response.flash=ack;
			}
		}
		catch (Exception e)
		{
			response.flash=e.getMessage();
		}
	}


	void getRefresh(ActionRequest request,ActionResponse response)
	{
		try
		{
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"))
			SocialNetworking sn=values.get(keys.indexOf("snType"))
			ack=service.getRefreshToken(user,sn);
			response.flash=ack;
		}
		catch (Exception e)
		{
			response.flash=e.getMessage();
		}
	}

	void getVal(ActionRequest request, ActionResponse response)
	{
		try
		{
			ArrayList al=new ArrayList();
			println("getVal() is Called  "+al.size())
			for(int i=0;i<al.size();i++)
			{
				println("API Key  "+al.get(i))
			}
		}
		catch (Exception e)
		{
			response.flash=e.getMessage();
		}
	}
	void getLoginAuthorization(ActionRequest request,ActionResponse response)
	{
		def context=request.context
		println(context.values)
	}




	void fbPostStatus(ActionRequest request,ActionResponse response)
	{
		def context=request.context as PostMessage
		if(context.getId()==null)
		{
			try
			{
				List keys=request.context.keySet().asList()
				List values=request.context.values().asList()
				User user=values.get(keys.indexOf("__user__"))
				println(request.context.toString())


				String privacy=values.get(keys.indexOf("privacy"))
				ack=service.postStatus(user,context.content,privacy)
				if(ack.startsWith("[a-zA-Z]"))
				{
					response.flash=ack;
				}
				else
				{
					response.values=["acknowledgment":ack];
					response.flash="Successfully Updated status"
				}
			}
			catch(Exception e)
			{
				e.printStackTrace()
				ack=e.getMessage()
			}

		}
		else
		{
			response.flash="Record Already Saved"
		}
	}


	public void postEvent(ActionRequest request,ActionResponse response)
	{
		println("Events::::"+request.context.toString())

		try
		{
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"))
			println(request.context.toString())
			String privacy=values.get(keys.indexOf("privacy"))
			def context=request.context as PostEvent


			org.joda.time.DateTime startDate=context.startdate
			Date startD=startDate.toDate()
			org.joda.time.DateTime endDate=context.enddate
			Date endD=endDate.toDate()
			ack=service.fbPostEvent(user,startD,endD, context.occession, context.location,privacy)
			if(ack.startsWith("[a-zA-Z]"))
			{
				response.flash=ack;
			}
			else
			{

				response.values=["acknowledgment":ack]
				response.flash="Successfully Updated Event"
				//response.view=[title:"Post Even" , resource:PostEvent.class.name,viewType:"grid",domain: "self.curUser = '${__user__}'"];
			}

		}
		catch(Exception e)
		{
			e.printStackTrace()
		}
	}


	/**
	 * Need To Remove Duplications from List
	 * @param request
	 * @param response
	 */
	void getAllContactsFB(ActionRequest request,ActionResponse response)
	{
		println(request.context.toString())

		try
		{
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"))
			ack=service.importContactsFB(user)
			response.flash=ack;
		}
		catch(Exception e)
		{
			response.flash=e.toString()
			e.printStackTrace()
		}
	}


	void getCommentsOfStatus(ActionRequest request,ActionResponse response)
	{
		println("Comments:::"+request.context.toString())
		String statusId;
		def context= request.context as PostMessage
		try
		{
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"))
			ack=service.getCommentsFB(user, context.getAcknowledgment());
			response.flash=ack;

		}
		catch (Exception e)
		{
			response.flash=e.toString()
			e.printStackTrace()
		}
	}

	void fetchInbox(ActionRequest request,ActionResponse response)
	{

		try
		{
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"))
			ack=service.getInbox(user)
			if(ack.equals("Message Syncronization Completed!!"))
			{
				response.flash=ack;
			}
			else
			{
				response.flash=ack;
			}

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	void getNotificationsFromFB(ActionRequest request,ActionResponse response)
	{

		try
		{
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"))
			ack=service.getNotifications(user);
			if(ack.startsWith("[a-zA-Z]"))
			{
				response.flash="You have "+ack+" new Notification";
			}
			else
			{
				response.flash="You have "+ack+" new Notification";
			}

		}
		catch(Exception e)
		{
			response.flash=e.getMessage();
			e.printStackTrace();
		}

	}



	void getFriendRequest(ActionRequest request,ActionResponse response)
	{
		try
		{
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"));
			ack=service.getFriendRequest(user);
			response.flash=ack;
		}
		catch (Exception e)
		{
			response.flash=e.getMessage();
		}

	}

	void retriveNewsFeed(ActionRequest request,ActionResponse response)
	{
		try
		{
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"));
			ack=service.getNewsFeeds(user);
			response.flash=ack;
		}
		catch (Exception e)
		{
			response.flash=e.getMessage();
		}
	}

	void getPages(ActionRequest request,ActionResponse response)
	{
		try
		{
			List keys=request.context.keySet().asList();
			List values=request.context.values().asList();
			User user=values.get(keys.indexOf("__user__"));
			ack=service.getPageFB(user);
			response.flash=ack;
		}
		catch (Exception e)
		{
			response.flash=e.getMessage();
		}
	}


	void postToPage(ActionRequest request,ActionResponse response)
	{
		try
		{
			def context = request.context as FBPagePost;
			if(context.id==null)
			{
				List keys=request.context.keySet().asList();
				List values=request.context.values().asList();
				User user=values.get(keys.indexOf("__user__"));
				FBPages page=values.get(keys.indexOf("page"))
				ack=service.postPageContent(user,page,context);
				if(ack.startsWith("[a-zA-Z]"))
				{
					response.flash="Some Error is there Please Try Later!!";
				}
				else
				{
					response.values=["acknowledgment":ack]
					//response.values=["acknowledgment":"","curUser":"","postedTime":"","content":"","page":""]
					response.flash="Successfully Posted to " + page.name;
				}
			}
			else
			{
				response.flash="Record Already Saved"
			}
		}
		catch (Exception e)
		{
			response.flash=e.getMessage();
		}
	}

	void deletePagePost(ActionRequest request,ActionResponse response)
	{
		try
		{
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"));
			List ids=values.get(keys.indexOf("_ids"));
			ack=service.deletePagePost(user,ids);
			response.flash=ack;
		}
		catch (Exception e)
		{
			response.flash=e.getMessage();
		}
	}

	void getPagePostComment(ActionRequest request,ActionResponse response)
	{
		println(request.context.toString())
		String statusId;
		def context= request.context as FBPagePost
		try
		{
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"))
			ack=service.getPageCommentsFB(user, context.getAcknowledgment());
			if(ack.equals("Comment Will only Display if there is Some Comment on Content"))
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
			response.flash=e.toString()
			e.printStackTrace()
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
			response.flash=e.getMessage();
		}
	}


	void getDeleteEvent(ActionRequest request,ActionResponse response)
	{
		try
		{
			println(request.context.toString())
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"));
			List ids=values.get(keys.indexOf("_ids"));
			ack=service.getDeleteEvent(user,ids);
			response.flash=ack;
		}
		catch (Exception e)
		{
			response.exception=e.getMessage();
		}
	}

	void deleteInBox(ActionRequest request,ActionResponse response)
	{
		try
		{
			println(request.context.toString())
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"));
			List ids=values.get(keys.indexOf("_ids"));
			ack=service.deleteInBox(user,ids);
			response.flash=ack;
		}
		catch (Exception e)
		{
			response.exception=e.getMessage();
		}
	}


	void getLikes(ActionRequest request,ActionResponse response)
	{
		try
		{
			println(request.context.toString())
			def context = request.context as FBNewsFeed
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"));
			ack=service.getLike(user,context.id);
			response.flash=ack;
		}
		catch (Exception e)
		{
			response.exception=e.getMessage();
		}


	}

	void postComment(ActionRequest request,ActionResponse response)
	{
		try
		{
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"));
			String postedComment=values.get(keys.indexOf("postComment"))
			def context = request.context as PostMessage
			context.acknowledgment;
			ack=service.postCommmentonStatus(user,context.acknowledgment,postedComment)
			response.flash=ack;
			println context.acknowledgment;
			println postedComment;
		}
		catch (Exception e)
		{
			response.exception=e.getMessage();
		}
	}

	void postPagePostComment(ActionRequest request,ActionResponse response)
	{
		try
		{

			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"));
			String postedComment=values.get(keys.indexOf("postComment"))
			def context = request.context as FBPagePost
			context.acknowledgment;
			ack=service.postCommmentonStatus(user,context.acknowledgment,postedComment)
			response.flash=ack;
		}
		catch (Exception e)
		{
			response.exception=e.getMessage();
		}
	}

}
