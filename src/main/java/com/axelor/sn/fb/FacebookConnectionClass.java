package com.axelor.sn.fb;
import static java.lang.System.out;

//Import for SCRIBE to Authorize//
import org.scribe.builder.*;
import org.scribe.builder.api.*;
import org.scribe.model.*;
import org.scribe.oauth.*;
//ENDS HERE////

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeField;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DurationField;
import org.joda.time.LocalDate;
import org.joda.time.ReadablePartial;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

//import org.glassfish.grizzly.filterchain.RerunFilterAction;


import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;
import com.restfb.*;
import com.restfb.FacebookClient.AccessToken;
import com.restfb.Parameter;
import com.restfb.exception.FacebookOAuthException;
import com.restfb.json.*;
import com.restfb.types.*;
import com.restfb.types.Page;
import com.restfb.types.Post.Privacy;


public class FacebookConnectionClass 
{
	String userTokenTemp;
	String ack="";
	String apiKey;
	String apiSecretKey;
	String userToken;
	String tokenUrl;
	FacebookClient facebookClient;
	AccessToken accessToken;
	String fbUserID;
	String fbPwd;
	ArrayList returnBack;
	
	///==============================================================CORE Methods========================================================================================
	protected String getFbUserID() 
	{
		return fbUserID;
	}
	protected void setFbUserID(String fbUserID) 
	{
		this.fbUserID = fbUserID;
	}
	protected String getFbPwd() 
	{
		return fbPwd;
	}
	protected void setFbPwd(String fbPwd) 
	{
		this.fbPwd = fbPwd;
	}

	public String getApiKey() 
	{
		return apiKey;
	}

	public void setApiKey(String apiKey) 
	{
		this.apiKey = apiKey;
	}

	public String getApiSecretKey() 
	{
		return apiSecretKey;
	}

	public void setApiSecretKey(String apiSecretKey) 
	{
		this.apiSecretKey = apiSecretKey;
	}

	public String getUserToken() 
	{
		return userToken;
	}

	public void setUserToken(String userToken) 
	{
		this.userToken = userToken;
	}


	Token EMPTY_TOKEN = null;
	OAuthService service;
	public String changeAuthorization(String apiKey,String apiSecret,String redirectUrl)
	{
		//OLD URL Static One
		//"http://192.168.0.159:8080/axelor-demo/ws/snapps/100"
		service = new ServiceBuilder()
		.provider(FacebookApi.class)
		.apiKey(apiKey)
		.apiSecret(apiSecret)
		.callback(redirectUrl)
		.scope("manage_friendlists,manage_notifications,manage_pages,publish_stream,read_stream,read_friendlists,read_mailbox,create_event,read_requests,email,user_about_me,user_activities,user_birthday,user_education_history,user_groups,user_hometown,user_interests,user_likes,user_location,user_questions,user_relationships,user_relationship_details,user_religion_politics,user_subscriptions,user_website,user_work_history,user_events,user_games_activity,user_notes,user_photos,user_status,user_videos,friends_about_me,friends_activities,friends_birthday,friends_education_history,friends_groups,friends_hometown,friends_interests,friends_likes,friends_location,friends_questions,friends_relationships,friends_relationship_details,friends_religion_politics,friends_subscriptions,friends_website,friends_work_history,friends_events,friends_notes,friends_photos,friends_status,friends_videos")
		.build();
		Scanner in = new Scanner(System.in);
		// Obtain the Authorization URL
		System.out.println("Fetching the Authorization URL...");
		String authorizationUrl = service.getAuthorizationUrl(EMPTY_TOKEN);
		System.out.println(authorizationUrl);
		return authorizationUrl;
		/*
		System.out.println("Got the Authorization URL!");
		System.out.println("Now go and authorize Scribe here:");
		System.out.println(authorizationUrl);
		System.out.println("And paste the authorization code here");
		System.out.print(">>");
		Verifier verifier = new Verifier(in.nextLine());
		Token accessToken = service.getAccessToken(EMPTY_TOKEN, verifier);
	    System.out.println("Got the Access Token!");
	    System.out.println("(if your curious it looks like this: " + accessToken.getToken() + " )");
		return ack;*/
	}
/*
 * AQAKBRzwGO-YHddMzbJyCaSNAHi2NvuVHIi_DZi4PJ7UPIcua8R_um-HYEM3MbO_MzW5vQclj-GzRlshx1xoVn2cv0WG4515b3VjqeZ0lKZUbiPKyU1RtUhJXhQ-PfXvo1f3IlOL-TX_gc_5qMsskzigZaBKpwIhzsEbKFZD6QVwC_0HU9dzG4SOXCl_W55cYHDnoRvMN6AxSowvO61XSL0
 * */
	public String getAccessToken(String apiKey,String apiSecret,String code,String redirectUrl)
	{
//		OAuthService service=(OAuthService) new ServiceBuilder();
		service = new ServiceBuilder()
		.provider(FacebookApi.class)
		.apiKey(apiKey)
		.apiSecret(apiSecret)
		.callback(redirectUrl)
		.scope("manage_friendlists,manage_notifications,manage_pages,publish_stream,read_stream,read_friendlists,read_mailbox,create_event,read_requests,email,user_about_me,user_activities,user_birthday,user_education_history,user_groups,user_hometown,user_interests,user_likes,user_location,user_questions,user_relationships,user_relationship_details,user_religion_politics,user_subscriptions,user_website,user_work_history,user_events,user_games_activity,user_notes,user_photos,user_status,user_videos,friends_about_me,friends_activities,friends_birthday,friends_education_history,friends_groups,friends_hometown,friends_interests,friends_likes,friends_location,friends_questions,friends_relationships,friends_relationship_details,friends_religion_politics,friends_subscriptions,friends_website,friends_work_history,friends_events,friends_notes,friends_photos,friends_status,friends_videos")
		.build();
		Verifier verifier=new Verifier(code);
		String authorizationUrl = service.getAuthorizationUrl(EMPTY_TOKEN);
		
		//System.out.println(authorizationUrl);
		Token accessToken = service.getAccessToken(EMPTY_TOKEN, verifier);
	    System.out.println("Got the Access Token!");
	    System.out.println("(if your curious it looks like this: " + accessToken.getToken() + " )");
	    facebookClient=new DefaultFacebookClient(accessToken.getToken());
	    User user=facebookClient.fetchObject("me", User.class);
	    String token_name=accessToken.getToken()+":"+user.getName();
		//return accessToken.getToken();
	    return token_name;
	}
	/**
	 * 
	 * THIS METHOD USED TO AUTHENTICATE USER BY PARSING WEB PAGE
	 * @param String apiKey
	 * @param String apiSecret
	 * @param String userName
	 * @param String password
	 * @see HtmlUnit
	 * 	Parse the Facebook Page with specified parameters and return back extended Token from Facebook which will valid for 60 Days
	 * @return String
	 */
	public String getFBLogin(String apiKey,String apiSecret,String userName,String password)
	{

		setApiKey(apiKey);
		setApiSecretKey(apiSecret);
		setFbUserID(userName);
		setFbPwd(password);	

		try
		{
			WebClient webClient = new WebClient();	
			webClient.setJavaScriptEnabled(false);
			HtmlPage page1 = webClient.getPage("http://www.facebook.com");
			HtmlForm form = (HtmlForm) page1.getElementById("login_form");
			HtmlSubmitInput button = (HtmlSubmitInput) form.getInputsByValue("Log In").get(0);
			HtmlTextInput textField = form.getInputByName("email");
			textField.setValueAttribute(getFbUserID());
			HtmlPasswordInput textField2 = form.getInputByName("pass");
			textField2.setValueAttribute(getFbPwd());
			HtmlPage page2 = button.click();
			String url="https://www.facebook.com/dialog/oauth?client_id="+getApiKey()+"&scope=manage_friendlists,manage_notifications,manage_pages,publish_stream,read_stream,read_friendlists,read_mailbox,create_event,read_requests,email,user_about_me,user_activities,user_birthday,user_education_history,user_groups,user_hometown,user_interests,user_likes,user_location,user_questions,user_relationships,user_relationship_details,user_religion_politics,user_subscriptions,user_website,user_work_history,user_events,user_games_activity,user_notes,user_photos,user_status,user_videos,friends_about_me,friends_activities,friends_birthday,friends_education_history,friends_groups,friends_hometown,friends_interests,friends_likes,friends_location,friends_questions,friends_relationships,friends_relationship_details,friends_religion_politics,friends_subscriptions,friends_website,friends_work_history,friends_events,friends_notes,friends_photos,friends_status,friends_videos&response_type=token&redirect_uri=http://localhost:8080/axelor-demo/ws/snapps/100";
			HtmlPage page3=webClient.getPage(url);
			System.out.println("P-3"+page3.getUrl());
			if(page3.asXml().contains("Success"))
			{
				String[] tempUserT=page3.getUrl().toString().split("=");
				String[] tempUserT1=tempUserT[1].split("&");
				userTokenTemp=tempUserT1[0];
				System.out.println(userTokenTemp);
				setUserToken(userTokenTemp);
				System.out.println("If Block Called");
			}
			else
			{

				HtmlForm authForm=(HtmlForm) page3.getElementById("u_0_o");
				//System.out.println(page3.asXml());
				if(authForm.asXml().contains("__CONFIRM__"))
				{
					HtmlButton accetButtonNew=(HtmlButton)authForm.getButtonByName("__CONFIRM__");
					//HtmlSubmitInput acceptButton = (HtmlSubmitInput) authForm.getInputByName("grant_clicked");//("grant_required_clicked");//.get(0);//Value("Allow");
					HtmlPage page4=accetButtonNew.click();
					System.out.println("P-4"+page4.asText());
					String[] tempUserT=page4.getUrl().toString().split("=");
					String[] tempUserT1=tempUserT[1].split("&");
					userTokenTemp=tempUserT1[0];
					System.out.println(userTokenTemp);
					setUserToken(userTokenTemp);
					System.out.println("if of Else Block Called");
				}
				else
				{
					HtmlSubmitInput acceptButton = (HtmlSubmitInput) authForm.getInputByName("grant_required_clicked");//("grant_required_clicked");//.get(0);//Value("Allow");
					HtmlPage page4=acceptButton.click();
					System.out.println("P-4"+page4.getUrl());
					System.out.println("P-4"+page4.asXml());
					String[] tempUserT=page4.getUrl().toString().split("=");
					System.out.println(tempUserT);
					String[] tempUserT1=tempUserT[1].split("&");
					userTokenTemp=tempUserT1[0];
					System.out.println(userTokenTemp);
					setUserToken(userTokenTemp);
					System.out.println("Else of Else Block Called");
				}
			}

			accessToken=new AccessToken();
			accessToken= new DefaultFacebookClient().obtainExtendedAccessToken(getApiKey(),getApiSecretKey(),getUserToken());//.obtainExtendedAccessToken(getApiKey(),getApiSecretKey(), accessTokenString);
			facebookClient=new DefaultFacebookClient(accessToken.getAccessToken());
			System.out.println("Extended AccessToken="+accessToken);
			ack=userTokenTemp;
		}
		catch(Exception e)
		{
			ack="Opps Something Went wrong"+e.getMessage();
			e.printStackTrace();
		}
		return ack;
	}




	/**
	 * THIS METHOD IS USED TO REFRESH EXTENDED TOKEN AFTED 60 DAYS
	 * @param String apiKey
	 * @param String apiSecret
	 * @param String userToken
	 * @return String
	 */

	public String getRefreshAccessToken(String apiKey,String apiSecret,String userToken)
	{
		tokenUrl="https://graph.facebook.com/oauth/access_token?grant_type=fb_exchange_token&client_id="+apiKey+"&client_secret="+apiSecret+"&fb_exchange_token="+userToken;
		try
		{
			WebClient webClient = new WebClient();		
			TextPage page1= webClient.getPage(tokenUrl);
			String tempToken=page1.getContent();
			String temparr[]=tempToken.split("=");
			String tempArr[]=temparr[1].split("&");
			ack=tempArr[0];
			System.out.println("Refresh One"+ack);
			Long l=Long.parseLong(temparr[2]);			
		}
		catch(Exception e)
		{
			ack=e.getMessage();
			e.printStackTrace();
		}
		return ack;
	}

	///==============================================================Fetch Methods========================================================================================	


	/**
	 * 
	 * THIS METHOD IS USED TO RETRIVE INFORMATION OF PARTICULAR PERSON (SEARCHING PURPOSE)
	 * @param String personName or Id
	 * @param String userToken
	 * @return ArrayList
	 */



	public ArrayList fetchObjectOfPerson(String detailParam,String userToken)
	{
		//getFBLogin();
		facebookClient=new DefaultFacebookClient(userToken);

		System.out.println("Extended AccessToken="+userToken);
		ArrayList returnBack=new ArrayList();
		System.out.println("Fetching Provided Detail");
		Connection<User> user=facebookClient.fetchConnection("search", User.class,Parameter.with("q", detailParam),Parameter.with("type", "user"));
		for(int i=0;i<user.getData().size();i++)
		{
			User detailValue=facebookClient.fetchObject(user.getData().get(i).getId(),User.class);
			returnBack.add(user.getData().get(i).getId());
			returnBack.add(detailValue.getFirstName());
			returnBack.add(detailValue.getLastName());
			returnBack.add(detailValue.getGender());
			returnBack.add(detailValue.getLink());
			System.out.println(user.getData().get(i).getId());
			System.out.println(detailValue.getFirstName());
			System.out.println(detailValue.getLastName());
			System.out.println(detailValue.getGender());
			System.out.println(detailValue.getLink());
		}
		return returnBack;
	}



	/**
	 * THIS METHOD IS USED TO RETRIVE LIST OF FRIENDS FOR CURRENT SESSION USER
	 * @param String userToken
	 * @return ArrayList
	 */

	public ArrayList getListOfFriends(String userToken)
	{		
		try
		{
			facebookClient=new DefaultFacebookClient(userToken);
			System.out.println("Extended AccessToken="+userToken);
			Connection<User> friends = facebookClient.fetchConnection("me/friends", User.class);  
			returnBack=new ArrayList();
			for(int i=0;i<friends.getData().size();i++)
			{
				User link=facebookClient.fetchObject(friends.getData().get(i).getId(), User.class);
				returnBack.add(friends.getData().get(i).getId());
				returnBack.add(friends.getData().get(i).getName());
				returnBack.add(link.getLink());

			}
			User link=facebookClient.fetchObject("me", User.class);
			returnBack.add(link.getId());
			returnBack.add(link.getName());
			returnBack.add(link.getLink());
		}
		catch(FacebookOAuthException oe)
		{
			if(oe.getErrorMessage().startsWith("Session has expired at unix time"))
			{
				ack="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
				returnBack.add(ack);
			}
			else
			{
				oe.printStackTrace();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return returnBack;
	}



	/**
	 * THIS METHOD IS USED TO RETRIVE COMMENTS OF THE STATUS 
	 * @param String userToken
	 * @param String contentId
	 * @return ArrayList
	 */

	public ArrayList getComments(String userToken,String contentId)
	{
		try
		{
			facebookClient=new DefaultFacebookClient(userToken);
			Connection<Comment> commentFeeds=facebookClient.fetchConnection(contentId+"/comments",Comment.class);
			returnBack=new ArrayList();
			for(int i=0;i<commentFeeds.getData().size();i++)
			{
				System.out.println(commentFeeds.getData().get(i).getId());
				System.out.println(commentFeeds.getData().get(i).getFrom().getId());
				System.out.println(commentFeeds.getData().get(i).getMessage());
				System.out.println(commentFeeds.getData().get(i).getCreatedTime());
				System.out.println(commentFeeds.getData().get(i).getLikeCount());
				returnBack.add(commentFeeds.getData().get(i).getId());
				returnBack.add(commentFeeds.getData().get(i).getFrom().getId());
				returnBack.add(commentFeeds.getData().get(i).getMessage());
				returnBack.add(commentFeeds.getData().get(i).getCreatedTime());
				returnBack.add(commentFeeds.getData().get(i).getLikeCount());
			}
		}

		catch(FacebookOAuthException oe)
		{
			if(oe.getErrorMessage().startsWith("Session has expired at unix time"))
			{
				ack="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
				returnBack.add(ack);
			}
			else
			{
				oe.printStackTrace();
			}
		}

		catch(Exception e)
		{
			e.printStackTrace();
		}
		return returnBack;
	}




	/**
	 * THIS METHOD USED TO RETRIVE INBOX OF FB
	 * @param String userToken
	 * @return ArrayList
	 */

	public ArrayList retriveMessage(String userToken)
	{
		ArrayList boxMsg=new ArrayList();
		try
		{
			facebookClient=new DefaultFacebookClient(userToken);
			accessToken=new AccessToken();
			System.out.println("Extended AccessToken="+userToken);
			System.out.println("Retrving Your Messages");
			Connection<Post> myBox = facebookClient.fetchConnection("me/inbox",Post.class);
			System.out.println(myBox.getData().size());
			for(int i=0;i<myBox.getData().size();i++)
			{
				System.out.println("-------------------------------------------------------------------------------");
				Message usr = facebookClient.fetchObject(myBox.getData().get(i).getId(),Message.class);				
				List<NamedFacebookType> lst=usr.getTo();
				if(lst.size()<2)
				{
					System.out.println(lst.get(0));
				}
				else
				{
					boxMsg.add(myBox.getData().get(i).getId());
					boxMsg.add(usr.getTo().get(0).getName());
					boxMsg.add(usr.getTo().get(1).getName());
					boxMsg.add(usr.getMessage());
					boxMsg.add(myBox.getData().get(i).getUpdatedTime().toString());
					System.out.println(myBox.getData().get(i).getUpdatedTime().toString());
				}
				System.out.println("*******************************************************************************");
			}
		}
		catch(FacebookOAuthException oe)
		{
			if(oe.getErrorMessage().startsWith("Session has expired at unix time"))
			{
				ack="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
				returnBack.add(ack);
			}
			else
			{
				oe.printStackTrace();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			ack="Oops Something Went Wrong\n"+e;
		}
		return boxMsg;
	}	



	public void test(String userToken)
	{
		facebookClient=new DefaultFacebookClient(userToken);
		Connection<Post> noti=facebookClient.fetchConnection("me/notifications",Post.class);
		for(int i=0;i<noti.getData().size();i++)
		{
			System.out.println(noti.getData().get(i).getId());
		}
	}



	/**
	 * THIS METHOD WILL RETRIVE NEW PENDING NOTIFICATIONS
	 * @param String userToken
	 * @return
	 */

	public ArrayList getNotification(String userToken,int paramLimit,Date sinceValue,Date untilValue)
	{
		facebookClient=new DefaultFacebookClient(userToken);
		ArrayList notifications=new ArrayList();
		Notification jsonNotification;
		try
		{
			if(paramLimit > 0 && sinceValue==null && untilValue==null)
			{
				jsonNotification = facebookClient.fetchObject("me/notifications", Notification.class,Parameter.with("limit",paramLimit));
			}
			else if(sinceValue!=null && paramLimit <=0 && untilValue==null)
			{
				jsonNotification = facebookClient.fetchObject("me/notifications", Notification.class,Parameter.with("since", sinceValue));
			}
			else if(untilValue!=null && sinceValue==null && paramLimit <=0)
			{
				jsonNotification = facebookClient.fetchObject("me/notifications", Notification.class,Parameter.with("until", untilValue));//.toDateMidnight().toDate()
			}
			else if(paramLimit > 0 && sinceValue != null && untilValue==null)
			{
				jsonNotification = facebookClient.fetchObject("me/notifications", Notification.class,Parameter.with("limit",paramLimit),Parameter.with("since", sinceValue));
			}
			else if(paramLimit>0 && untilValue!=null && sinceValue==null)
			{
				jsonNotification = facebookClient.fetchObject("me/notifications", Notification.class,Parameter.with("limit",paramLimit),Parameter.with("until", untilValue));
			}
			else if(untilValue !=null && sinceValue!=null && paramLimit<=0)
			{
				jsonNotification = facebookClient.fetchObject("me/notifications", Notification.class,Parameter.with("until", untilValue),Parameter.with("since", sinceValue));
			}
			else if(paramLimit>0 && untilValue !=null && sinceValue!=null)
			{
				jsonNotification = facebookClient.fetchObject("me/notifications", Notification.class,Parameter.with("limit", paramLimit),Parameter.with("until", untilValue),Parameter.with("since", sinceValue));
			}
			else
			{
				jsonNotification = facebookClient.fetchObject("me/notifications", Notification.class);
			}
			//notifications.add(0,jsonNotification.summary);
			for(int i=0;i < jsonNotification.data.size();i++)
			{
				System.out.println(jsonNotification.data.get(i).updated_time);
				notifications.add(jsonNotification.data.get(i).id);
				notifications.add(jsonNotification.data.get(i).title);
				notifications.add(jsonNotification.data.get(i).link);
				notifications.add(jsonNotification.data.get(i).updated_time);
			}			
		}
		catch(FacebookOAuthException oe)
		{
			if(oe.getErrorMessage().startsWith("Session has expired at unix time"))
			{
				ack="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
				returnBack.add(ack);
			}
			else
			{
				oe.printStackTrace();
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return notifications;
	}



	/**
	 * @see Json Parsing
	 * @author APAR-AXELOR
	 * @see Data and Summary 
	 *
	 */

	public static class Notification 
	{

		@Facebook
		Summary summary;

		@Facebook
		List<Data> data;

		/*@Override
		public String toString() 
		{
			return "abc";
		}*/
	}

	public static class Data
	{
		@Facebook
		String id;

		@Facebook
		String title;

		@Facebook
		String link;

		@Facebook
		String updated_time;

	}

	public static class Summary
	{
		@Facebook
		Long unseen_count;

		/*public String toString() 
		{
			return String.format("\n%s",unseen_count);
		}*/
	}




	/**
	 * THIS METHOD 'll RETRIVE PENDING FRIEND REQUEST
	 * @param String userToken
	 * @return ArrayList
	 */

	public ArrayList getFriendRequest(String userToken)
	{

		returnBack=new ArrayList();
		User user=null;
		facebookClient=new DefaultFacebookClient(userToken);
		System.out.println("* FQL Query *");
		try
		{
			List<FqlUser> users = facebookClient.executeQuery("SELECT uid_from FROM friend_request WHERE uid_to = me()",FqlUser.class);
			System.out.println(users.size());
			//System.out.println);
			for(int i=0;i<users.size();i++)
			{
				String valToPass=users.get(i).toString();
				user=facebookClient.fetchObject(users.get(i).uid_from,User.class);
				System.out.println(user.getLink());
				returnBack.add(user.getLink());
				returnBack.add(user.getName());
				returnBack.add(user.getGender());
			}
		}
		catch(FacebookOAuthException oe)
		{
			if(oe.getErrorMessage().startsWith("Session has expired at unix time"))
			{
				ack="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
				returnBack.add(ack);
			}
			else
			{
				oe.printStackTrace();
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return returnBack;
	}




	/**
	 * JSON PARSING CLASS FOR FQL USER QUERY
	 * @author APAR-AXELOR
	 *
	 */

	public static class FqlUser 
	{
		@Facebook
		String uid_from;


		@Override
		public String toString() 
		{
			return String.format("%s ", uid_from);
		}
	}



	/**
	 * THIS METHOD IS USED TO RETRIVE NEWS FEED ALONG WITH THEIR UNSEEN COUNT
	 * @param String userToken
	 * @return ArrayList
	 */

	@SuppressWarnings({ "unused", "rawtypes" })
	public ArrayList getNewsFeed(String userToken,int paramLimit,Date sinceValue,Date untilValue)
	{
		Connection<Post> connection;
		returnBack=new ArrayList();
		facebookClient=new DefaultFacebookClient(userToken);
		int i=0;
		try
		{
			if(paramLimit > 0 && sinceValue==null && untilValue==null)
			{
				System.out.println("Limit Called");
				connection=facebookClient.fetchConnection("me/home", Post.class, Parameter.with("limit",paramLimit));
			}
			else if(sinceValue!=null && paramLimit <=0 && untilValue==null)
			{
				System.out.println("Since Called");
				connection=facebookClient.fetchConnection("me/home",Post.class,Parameter.with("since", sinceValue));
			}
			else if(untilValue!=null && sinceValue==null && paramLimit <=0)
			{
				System.out.println("Untill Called");
				connection=facebookClient.fetchConnection("me/home",Post.class,Parameter.with("until", untilValue));
			}
			else if(paramLimit > 0 && sinceValue != null && untilValue==null)
			{
				System.out.println("Limit and Since Called");
				connection=facebookClient.fetchConnection("me/home",Post.class,Parameter.with("limit",paramLimit),Parameter.with("since", sinceValue));
			}
			else if(paramLimit>0 && untilValue!=null && sinceValue==null)
			{
				System.out.println("Untill and Limit Called");
				connection=facebookClient.fetchConnection("me/home",Post.class,Parameter.with("limit",paramLimit),Parameter.with("until", untilValue));
			}
			else if(untilValue !=null && sinceValue!=null && paramLimit<=0)
			{
				System.out.println("Since and Until Called");
				connection=facebookClient.fetchConnection("me/home",Post.class,Parameter.with("until", untilValue),Parameter.with("since", sinceValue));
			}
			else if(paramLimit>0 && untilValue !=null && sinceValue!=null)
			{
				System.out.println("All Parameter");
				connection=facebookClient.fetchConnection("me/home",Post.class,Parameter.with("limit",paramLimit),Parameter.with("until", untilValue),Parameter.with("since", sinceValue));
			}
			else
			{
				System.out.println("Without Parameter Called");
				connection=facebookClient.fetchConnection("me/home",Post.class);
			}
			System.out.println(connection.getNextPageUrl());

			for(int i1=0;i1<connection.getData().size();i1++)
			{
				//System.out.println(connection.getData().get(i1).getCreatedTime());
				returnBack.add(connection.getData().get(i1).getId());
				returnBack.add(connection.getData().get(i1).getFrom().getName());
				returnBack.add(connection.getData().get(i1).getCreatedTime());
				returnBack.add(connection.getData().get(i1).getType());
				returnBack.add(connection.getData().get(i1).getLink());
				returnBack.add(connection.getData().get(i1).getMessage());
				System.out.println(i1);
			}/*
			for(List<Post> fetchData:connection)
			{
				for(Post post:fetchData)
				{
					i++;
					System.out.println((post.getCreatedTime()));
					returnBack.add(post.getId());
					returnBack.add(post.getFrom().getName());
					returnBack.add(post.getCreatedTime());
					returnBack.add(post.getType());
					returnBack.add(post.getLink());
					returnBack.add(post.getMessage());
				}
			}*/
			System.out.println(i);
		}

		catch(FacebookOAuthException oe)
		{
			if(oe.getErrorMessage().startsWith("Session has expired at unix time"))
			{
				ack="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
				returnBack.add(ack);
			}
			else
			{
				oe.printStackTrace();
			}
		}

		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return returnBack;
	}




	/**
	 * IT WILL RETURN PAGE DETAIL WHICH OWN BY CURRENT SESSION USER'S
	 * @param String userToken
	 * @return ArrayList
	 */

	public ArrayList getPageDetail(String userToken)
	{
		returnBack=new ArrayList();
		facebookClient=new DefaultFacebookClient(userToken);
		User user=facebookClient.fetchObject("me", User.class);
		String query=" SELECT page_id, name,page_url,username From page WHERE page_id IN (SELECT page_id FROM page_admin WHERE uid = '"+user.getId()+"')";
		try
		{
			List<FqlPage> page=facebookClient.executeQuery(query, FqlPage.class);
			for(int i=0;i<page.size();i++)
			{
				System.out.println(page.get(i).page_id);
				System.out.println(page.get(i).name);
				System.out.println(page.get(i).page_url);
				System.out.println(page.get(i).username);
				returnBack.add(page.get(i).page_id);
				returnBack.add(page.get(i).name);
				returnBack.add(page.get(i).page_url);
				returnBack.add(page.get(i).username);
			}
		}

		catch(FacebookOAuthException oe)
		{
			if(oe.getErrorMessage().startsWith("Session has expired at unix time"))
			{
				ack="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
				returnBack.add(ack);
			}
			else
			{
				oe.printStackTrace();
			}
		}

		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return returnBack;
	}



	/**
	 * JSON PARSING CLASS FOR FQL PAGE QUERY
	 * @author APAR-AXELOR
	 *
	 */

	public static class FqlPage
	{
		@Facebook
		String page_id;

		@Facebook
		String name;

		@Facebook
		String page_url;

		@Facebook
		String username;
	}



	/**
	 * IT WILL FETCH PAGE POST'S COMMENT
	 * @param String userToken
	 * @param String contentId
	 * @return ArrayList
	 */

	@SuppressWarnings({ "unused", "rawtypes", "unchecked" })
	public ArrayList getPageComments(String userToken,String contentId)
	{
		try
		{
			facebookClient=new DefaultFacebookClient(userToken);
			Connection<Comment> commentFeeds=facebookClient.fetchConnection(contentId+"/comments",Comment.class);	
			returnBack=new ArrayList();
			for(int i=0;i<commentFeeds.getData().size();i++)
			{
				System.out.println(commentFeeds.getData().get(i).getId());
				System.out.println(commentFeeds.getData().get(i).getFrom().getName());
				System.out.println(commentFeeds.getData().get(i).getMessage());
				System.out.println(commentFeeds.getData().get(i).getCreatedTime());
				System.out.println(commentFeeds.getData().get(i).getLikeCount());
				returnBack.add(commentFeeds.getData().get(i).getId());
				returnBack.add(commentFeeds.getData().get(i).getFrom().getName());
				returnBack.add(commentFeeds.getData().get(i).getMessage());
				returnBack.add(commentFeeds.getData().get(i).getCreatedTime());
				returnBack.add(commentFeeds.getData().get(i).getLikeCount());
			}
		}
		catch(FacebookOAuthException oe)
		{
			if(oe.getErrorMessage().startsWith("Session has expired at unix time"))
			{
				ack="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
				returnBack.add(ack);
			}
			else
			{
				oe.printStackTrace();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return returnBack;
	}

	//============================================================POST METHODS=====================================================================================================	


	/**
	 * PASSED CONTENT POSTED TO CURRENT SESSION USER'S WALL 
	 * @param String message
	 * @param String userToken
	 * @return String
	 */

	public String publishMessage(String msg,String userToken,String paramPrivacy)
	{		
		Privacy privacy=new Privacy();
		privacy.value=paramPrivacy;
		try
		{
			facebookClient=new DefaultFacebookClient(userToken);
			System.out.println("Extended AccessToken="+userToken);
			System.out.println("Publishing Your Message");
			FacebookType publishMessageResponse=facebookClient.publish("me/feed", FacebookType.class, Parameter.with("message", msg),Parameter.with("privacy",privacy));
			ack=publishMessageResponse.getId();
			System.out.println(ack);
		}
		catch(FacebookOAuthException oe)
		{
			if(oe.getErrorMessage().startsWith("Session has expired at unix time"))
			{
				ack="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
				//				returnBack.add(ack);
			}
			else
			{
				oe.printStackTrace();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			ack="Oops Something Went Wrong\n"+e;
		}
		return ack;
	}	

	public static class Privacy
	{
		@Facebook
		public String value;
	}


	/**
	 * THIS METHOD WILL ALLOW TO POST EVENT ONTO FB 
	 * @param String startDaTE
	 * @param String endDATE
	 * @param String occasion
	 * @param String location
	 * @param String userToken
	 * @return String
	 */

	public String publishEvent(Date startD,Date endD,String ocession,String location,String userToken,String paramPrivacy)
	{		
		Privacy privacy=new Privacy();
		privacy.value=paramPrivacy;
		try
		{
			facebookClient=new DefaultFacebookClient(userToken);
			System.out.println("Extended AccessToken="+userToken);
			System.out.println("Event Publishing");		
			FacebookType publishEventResponse=facebookClient.publish("me/events",FacebookType.class,Parameter.with("name", ocession),Parameter.with("location", location),Parameter.with("Start_time",startD),Parameter.with("end_time",endD),Parameter.with("privacy",privacy.value));	
			System.out.println("Successfully Event ID="+publishEventResponse.getId());
			ack=publishEventResponse.getId();
		}
		catch(FacebookOAuthException oe)
		{
			if(oe.getErrorMessage().startsWith("Session has expired at unix time"))
			{
				ack="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
				//				returnBack.add(ack);
			}
			else
			{
				oe.printStackTrace();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			ack=e.getMessage();
		}
		return ack;
	}



	/**
	 * 
	 * METHOD USED TO POST CONTENT TO PARTICULAR PAGE WHOESE ID BEEN PASS HERE
	 * @param String postContent
	 * @param String pageId
	 * @param String userToken
	 * @return String
	 */

	public String postToPgae(String postContent,String pageId,String userToken)
	{
		facebookClient=new DefaultFacebookClient(userToken);
		try
		{
			FacebookType publishToPage=facebookClient.publish(pageId+"/feed", FacebookType.class,Parameter.with("message", postContent));
			ack=publishToPage.getId();
			System.out.println(ack);
		}

		catch(FacebookOAuthException oe)
		{
			if(oe.getErrorMessage().startsWith("Session has expired at unix time"))
			{
				ack="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
				//				returnBack.add(ack);
			}
			else
			{
				oe.printStackTrace();
			}
		}

		catch(Exception e)
		{
			e.printStackTrace();
		}

		return ack;
	}


	public Boolean postLike(String contentId,String userToken) 
	{
		Boolean value=null;
		facebookClient=new DefaultFacebookClient(userToken);
		try
		{
			Boolean publishToPage=facebookClient.publish(contentId+"/likes", Boolean.class);
			value=publishToPage.booleanValue();

		}

		catch(FacebookOAuthException oe)
		{
			if(oe.getErrorMessage().startsWith("Session has expired at unix time"))
			{
				ack="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
				//				returnBack.add(ack);
			}
			else
			{
				oe.printStackTrace();
			}
		}

		catch (Exception e) 
		{
			e.printStackTrace();
		}

		return value;
	}




	//============================================================DELETION METHODS=====================================================================================================	

	/**
	 * COMMON METHOD TO DELETE WHATEVER BEEN POSTED BY PASSING THEIR ID
	 * @param String contentId
	 * @param String userToken
	 * @return Boolean
	 */
	public Boolean delete(String contentId,String userToken) 
	{
		Boolean stat = null;
		try
		{
			facebookClient=new DefaultFacebookClient(userToken);
			System.out.println(ack);
			stat=facebookClient.deleteObject(contentId);
			System.out.println(stat);

			if(stat)
			{
				ack="Content Deleted Successfully!!!";
			}
			else
			{
				ack="There is some Problem Please Try again";
			}
			System.out.println(contentId);
			System.out.println(ack);

		}
		catch(FacebookOAuthException oe)
		{
			if(oe.getErrorMessage().startsWith("Session has expired at unix time"))
			{
				ack="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
				//				returnBack.add(ack);
			}
			else
			{
				oe.printStackTrace();
			}
		}

		catch (Exception e) 
		{
			ack=e.getMessage();
			e.printStackTrace();
		}
		return stat;
	}

	public ArrayList postStatusComemnt(String userToken,String contentd,String commentContent)
	{
		returnBack=new ArrayList();
		facebookClient=new DefaultFacebookClient(userToken);
		try
		{
			FacebookType publishComment=facebookClient.publish(contentd+"/comments", FacebookType.class,Parameter.with("message",commentContent));

			Post knwUser=facebookClient.fetchObject(publishComment.getId(), Post.class);
			User knwTrueUser=facebookClient.fetchObject(knwUser.getFrom().getId(), User.class);
			System.out.println(knwTrueUser.getId());
			System.out.println(knwTrueUser.getName());
			System.out.println(knwTrueUser.getLink());
			System.out.println(publishComment.getId());
			returnBack.add(knwTrueUser.getId());
			returnBack.add(knwTrueUser.getName());
			returnBack.add(knwTrueUser.getLink());
			//returnBack.add(publishComment.getId());			
		}

		catch(FacebookOAuthException oe)
		{

			if(oe.getErrorMessage().startsWith("Session has expired at unix time"))
			{
				ack="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
				returnBack.add(ack);
			}
			else
			{
				oe.printStackTrace();
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return returnBack;
	}

	//============================================================ENDS OF ALL METHODS=====================================================================================================


	public String postMsgToFriend(String msg,String frnId,String userToken)
	{
		facebookClient =new DefaultFacebookClient(userToken);
		try
		{
			FacebookType publishMsg=facebookClient.publish("/dialog/feed",FacebookType.class,Parameter.with("app_id","593379047358957"),Parameter.with("description", msg),Parameter.with("to",frnId),Parameter.with("caption", msg));
			ack=publishMsg.getId();
			System.out.println(ack);
		}
		catch(Exception e)
		{
			ack=e.getMessage();
			e.printStackTrace();
		}
		return ack;
	}


	public void removeFriend(String id,String userToken)
	{
		facebookClient=new DefaultFacebookClient(userToken);
		Boolean val=facebookClient.deleteObject("me/friends/"+id);
		System.out.println(val);
	}


	public String sendPersonalMessage(String userId,String userToken,String msg)
	{
		facebookClient=new DefaultFacebookClient(userToken);
		try
		{
			FacebookType personalMessage=facebookClient.publish("messages/"+userId, FacebookType.class, Parameter.with("message", msg));
			ack=personalMessage.getId();
			System.out.println(ack);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return ack;
	}
	/**
	 * 
	 * https://www.facebook.com/dialog/oauth?client_id=457254604345188&scope=user_about_me,manage_pages,publish_stream,read_stream,read_friendlists,read_mailbox,create_event,email,user_likes,user_birthday,user_events,user_photos,user_photos,user_photos&response_type=token&redirect_uri=https://www.facebook.com/connect/login_success.html
	 * 
	 * 
	 * 
	 * 
	 * IMPOROVED
	 * 
	 * https://www.facebook.com/dialog/oauth?client_id=457254604345188&scope=manage_pages,publish_stream,read_stream,read_friendlists,read_mailbox,create_event,email,email,user_about_me,user_activities,user_birthday,user_education_history,user_groups,user_hometown,user_interests,user_likes,user_location,user_questions,user_relationships,user_relationship_details,user_religion_politics,user_subscriptions,user_website,user_work_history,user_events,user_games_activity,user_notes,user_photos,user_status,user_videos,friends_about_me,friends_activities,friends_birthday,friends_education_history,friends_groups,friends_hometown,friends_interests,friends_likes,friends_location,friends_questions,friends_relationships,friends_relationship_details,friends_religion_politics,friends_subscriptions,friends_website,friends_work_history,friends_events,friends_notes,friends_photos,friends_status,friends_videos&response_type=token&redirect_uri=https://www.facebook.com/connect/login_success.html
	 */

	/**
	 * SEARCH METHOD
	  I'm trying to get all the post messages, my code is as follows 

public Connection<Post> publicSearchMessages(Date fromDate, Date 
toDate) { 
    Connection<Post> messages = 
publicFbClient.fetchConnection("search", 
            Post.class, 
            Parameter.with("q", "Watermelon"), 
            Parameter.with("since", fromDate), 
            Parameter.with("until", toDate), 
            Parameter.with("type", "post")); 

    return messages; 
} 

When i test this code, it gives latest 25 post messages. 
AQAARLS0EVGmKL29OOTsgwgMqXw1frRZP1ZY-Vh9xQZ-5I-6PWdy_7UQECN7pN8bCaP6Ga4tHysNA5Hrn2pS4yY2C001DIu5vzb64WbVTnEmsV91T5NzI9gU3-SKoiOu41ymL18G3Zw3rcDv3wrEUf5qdTfHh7wyoYh639pDXUS7Obyrcp9TpLFwFa5UIpvkt1qEG11Ao-1e8M38WpfDc6hO
Parameter.with("limit",100 ) 

If i set limit parameter, it gives 100 messages but i don't want to 
limit to fetching post messages. So, 

Is there way to get a list of post messages matching the search 
criteria without setting limit parameter?

	 * 
	 */


	public static void main(String[] args) 
	{
		FacebookConnectionClass fbcon=new FacebookConnectionClass();
		
		//fbcon.getAccessToken("593379047358957", "a542e3849a7ae72b5be41a5c75f72a6e", "AQDGyIAzVppZ_cGKPHu7SCj0HbDgpxW7iFOMofTlDSaWf84wwao3m2yWDmfgkJcaO8Ihumh-v9VApsbWkj9356QRR1vQLw4s7NVlDdtQi4wzzLsqqCJMNaXiYK97gAoODh-itBjgz7aZlu84MbKF8yVefQjXePrlcxTTTC5oMXC-zkKKSKtyIn6hofIRGDehz-NXEMv2SiMSkedywWwjt2xJ");
		//fbcon.fetchObjectOfPerson("Apar Amin", "BAAIbrNNVwe0BAABhXhDqZBWqTMfRYN9tIuMsHpZAAe5yZB75IUyJpekOlp3EtspzdP4ycgty0OJCpCTDZBcvcHTVZBfwXG1rGlTRkG2bzOs01olh8tYRvHUiu4YJIZA6xjn9ksUXGZCbXgqZArooIZCyzHbSeGQNK1i18qF96uM6P8qV017hFGZCa4");
		//fbcon.getFBLogin("1","1", "1", "1");
		//fbcon.fetchObjectOfPerson("Apar Amin", "AAAIbrNNVwe0BALCzJN3rECxb2KDZBxvZAKjW7aoY8LZBoCIYthylJFbAELzf6dm4RonC3pIvHn3pisSioeLmd1IwFUBZBDKfv5bpZBxFyVQZDZD");
		//fbcon.testNotif("AAAIbrNNVwe0BALCzJN3rECxb2KDZBxvZAKjW7aoY8LZBoCIYthylJFbAELzf6dm4RonC3pIvHn3pisSioeLmd1IwFUBZBDKfv5bpZBxFyVQZDZD");
		//fbcon.retriveMessage("AAAIbrNNVwe0BALCzJN3rECxb2KDZBxvZAKjW7aoY8LZBoCIYthylJFbAELzf6dm4RonC3pIvHn3pisSioeLmd1IwFUBZBDKfv5bpZBxFyVQZDZD");
		//fbcon.getNotification("AAAIbrNNVwe0BALCzJN3rECxb2KDZBxvZAKjW7aoY8LZBoCIYthylJFbAELzf6dm4RonC3pIvHn3pisSioeLmd1IwFUBZBDKfv5bpZBxFyVQZDZD",3,null,null);
		//System.out.println(fbcon.postStatusComemnt("AAAIbrNNVwe0BALCzJN3rECxb2KDZBxvZAKjW7aoY8LZBoCIYthylJFbAELzf6dm4RonC3pIvHn3pisSioeLmd1IwFUBZBDKfv5bpZBxFyVQZDZD", "100001571838867_513537028708687","Demo Test"));
		//fbcon.getNewsFeed("AAAIbrNNVwe0BALCzJN3rECxb2KDZBxvZAKjW7aoY8LZBoCIYthylJFbAELzf6dm4RonC3pIvHn3pisSioeLmd1IwFUBZBDKfv5bpZBxFyVQZDZD", 0,new DateTime(2013, 04, 01, 00, 8, 00, 00),null);
		//fbcon.getNotification("BAAIbrNNVwe0BAABhXhDqZBWqTMfRYN9tIuMsHpZAAe5yZB75IUyJpekOlp3EtspzdP4ycgty0OJCpCTDZBcvcHTVZBfwXG1rGlTRkG2bzOs01olh8tYRvHUiu4YJIZA6xjn9ksUXGZCbXgqZArooIZCyzHbSeGQNK1i18qF96uM6P8qV017hFGZCa4",0,null,null);
		//fbcon.getRefreshAccessToken("457254604345188", "69e4bf4c07266fbc176cf7737176fa7c", "AAAGf3uJDj2QBAKsjHLgHrAI9vfOl1TM8LHCAv5oyh0Naf8s4fDccMAZC2LBI8KAkksfvlL6y79MARJZBANz2iCWUbC5RaO9fX0aX5ZCOQZDZD");
		//System.out.println(fbcon.sendMessage("678241762","Hi","AAAGf3uJDj2QBAKsjHLgHrAI9vfOl1TM8LHCAv5oyh0Naf8s4fDccMAZC2LBI8KAkksfvlL6y79MARJZBANz2iCWUbC5RaO9fX0aX5ZCOQZDZD"));
		//System.out.println(fbcon.getFBLogin("593379047358957","a542e3849a7ae72b5be41a5c75f72a6e","apar.amin1","O6bO01@p@r"));
		//fbcon.getRefreshAccessToken("457254604345188", "69e4bf4c07266fbc176cf7737176fa7c", "AAAGf3uJDj2QBAKsjHLgHrAI9vfOl1TM8LHCAv5oyh0Naf8s4fDccMAZC2LBI8KAkksfvlL6y79MARJZBANz2iCWUbC5RaO9fX0aX5ZCOQZDZD");
		//fbcon.removeFriend("100004325733115","AAAGf3uJDj2QBAKsjHLgHrAI9vfOl1TM8LHCAv5oyh0Naf8s4fDccMAZC2LBI8KAkksfvlL6y79MARJZBANz2iCWUbC5RaO9fX0aX5ZCOQZDZD");
		//.delete("364371463667700","AAAGf3uJDj2QBAKsjHLgHrAI9vfOl1TM8LHCAv5oyh0Naf8s4fDccMAZC2LBI8KAkksfvlL6y79MARJZBANz2iCWUbC5RaO9fX0aX5ZCOQZDZD");
		//fbcon.postMsgToFriend("Hi","100000165610558","AAAIbrNNVwe0BALCzJN3rECxb2KDZBxvZAKjW7aoY8LZBoCIYthylJFbAELzf6dm4RonC3pIvHn3pisSioeLmd1IwFUBZBDKfv5bpZBxFyVQZDZD");
		//fbcon.getPageDetail("AAAIbrNNVwe0BALCzJN3rECxb2KDZBxvZAKjW7aoY8LZBoCIYthylJFbAELzf6dm4RonC3pIvHn3pisSioeLmd1IwFUBZBDKfv5bpZBxFyVQZDZD");
		//fbcon.retriveMessage("AAAIbrNNVwe0BALCzJN3rECxb2KDZBxvZAKjW7aoY8LZBoCIYthylJFbAELzf6dm4RonC3pIvHn3pisSioeLmd1IwFUBZBDKfv5bpZBxFyVQZDZD");
		//fbcon.sendPersonalMessage("100004325733115","AAAIbrNNVwe0BALCzJN3rECxb2KDZBxvZAKjW7aoY8LZBoCIYthylJFbAELzf6dm4RonC3pIvHn3pisSioeLmd1IwFUBZBDKfv5bpZBxFyVQZDZD","hi");
	}

}

