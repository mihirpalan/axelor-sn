package com.axelor.sn.twitter;

import java.util.*;
import org.joda.time.DateTime;
import org.scribe.builder.*;
import org.scribe.builder.api.*;
import org.scribe.model.*;
import org.scribe.oauth.*;
import twitter4j.*;
import twitter4j.auth.*;
import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;

public class TwitterConnectionClass 
{
	Twitter twitter = new TwitterFactory().getInstance();
	AccessToken accessToken;
	String ack="";
	String pin;
	String acknowledgment="";
	String CONSUMER_KEY;
	String CONSUMER_SECRET_KEY;
	String userid,content,time,fav;

	/**
	 * 
	 * METHOD ALLOWS TO AUTHENTICATE SESSION USER 
	 * @param String apiKey
	 * @param String apiSecret
	 * @param String userName
	 * @param String password
	 * @return String 
	 */
	public String getUserToken(String apiKey, String apiSecret, String userName,String password) 
	{

		try 
		{
			twitter.setOAuthConsumer(apiKey, apiSecret);
			WebClient webClient = new WebClient();
			webClient.setJavaScriptEnabled(false);
			RequestToken requestToken = twitter.getOAuthRequestToken();
			System.out.println(requestToken.getAuthorizationURL());
			HtmlPage page3 = webClient.getPage(requestToken.getAuthorizationURL());
			HtmlForm authForm = (HtmlForm) page3.getElementById("oauth_form");
			HtmlTextInput textbox1 = (HtmlTextInput) authForm.getInputByName("session[username_or_email]");
			textbox1.setValueAttribute(userName);
			HtmlPasswordInput textbox2 = (HtmlPasswordInput) authForm.getInputByName("session[password]");
			textbox2.setValueAttribute(password);
			HtmlSubmitInput button = (HtmlSubmitInput) authForm.getInputByValue("Authorize app");
			HtmlPage page4 = button.click();
			HtmlCode code = (HtmlCode) page4.getElementsByTagName("code").get(0);
			System.out.println(code.asText());
			pin=code.asText();
			accessToken=twitter.getOAuthAccessToken(requestToken,pin);
			System.out.println(accessToken.getToken());
			System.out.println(accessToken.getTokenSecret());
			acknowledgment=accessToken.getToken()+":"+accessToken.getTokenSecret()+":"+accessToken.getScreenName();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return acknowledgment;
	}

	RequestToken requestToken;
	public String getAuthURL(String apiKey, String apiSecret)
	{
		try
		{
			twitter.setOAuthConsumer(apiKey, apiSecret);
			requestToken = twitter.getOAuthRequestToken("http://127.0.0.1:8080/axelor-demo/ws/twitterauth?100");
			acknowledgment=requestToken.getAuthorizationURL();
			System.out.println(requestToken.getAuthorizationURL());		
			/*OAuthService service = new ServiceBuilder()
			.provider(TwitterApi.class)
			.apiKey(apiKey)
			.apiSecret(apiSecret)
			.callback("http://127.0.0.1:8080/axelor-demo/ws/twitterauth?100")
			.build();
			//			Scanner in = new Scanner(System.in);

			System.out.println("=== Twitter's OAuth Workflow ===");
			System.out.println();

			// Obtain the Request Token
			System.out.println("Fetching the Request Token...");
			Token requestToken = service.getRequestToken();
			//giveToken(requestToken);
			System.out.println("Got the Request Token!");
			System.out.println();

			System.out.println("Now go and authorize Scribe here:");
			System.out.println(service.getAuthorizationUrl(requestToken));
			acknowledgment=service.getAuthorizationUrl(requestToken);*/
			acknowledgment=requestToken.getAuthorizationURL();
			
		}
		catch (Exception e)
		{
			acknowledgment=e.getMessage();
			e.printStackTrace();
		}
		return acknowledgment;
	}


	Token EMPTY_TOKEN = null;
	public String getTokens(String apiKey, String apiSecret,String pin)
	{
		try
		{
			/*OAuthService service = new ServiceBuilder()
			.provider(TwitterApi.class)
			.apiKey(apiKey)
			.apiSecret(apiSecret)
			.callback("http://127.0.0.1:8080/axelor-demo/ws/twitterauth?100")
			.build();

			Token requestToken = service.getRequestToken();
			System.out.println(service.getAuthorizationUrl(requestToken));
			Verifier verifier = new Verifier(pin);
			System.out.println(verifier.getValue());
			//		    Token requestToken = service.getRequestToken();

			// Trade the Request Token and Verfier for the Access Token
			System.out.println("Trading the Request Token for an Access Token...");
			
			Token accessToken = service.getAccessToken(requestToken, verifier);
			System.out.println("Got the Access Token!");
			System.out.println("(if your curious it looks like this: " + accessToken + " )");
			System.out.println();
			acknowledgment=accessToken.getToken()+":"+accessToken.getSecret();*/
			twitter.setOAuthConsumer(apiKey, apiSecret);
			requestToken = twitter.getOAuthRequestToken("http://127.0.0.1:8080/axelor-demo/ws/twitterauth?100");
			accessToken=twitter.getOAuthAccessToken(requestToken,pin);
			System.out.println(accessToken.getToken());
			System.out.println(accessToken.getTokenSecret());
			acknowledgment=accessToken.getToken()+":"+accessToken.getTokenSecret();
			System.out.println(accessToken.getUserId());
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			acknowledgment=e.getMessage();

		}
		return acknowledgment;
	}


	/**
	 * 
	 * USED FOR POSTING TWEET
	 * @param String apiKey
	 * @param String apiSecret
	 * @param String token
	 * @param String tokenSecret
	 * @param String content
	 * @return String
	 */
	public String postTweet(String apiKey,String apiSecret,String token,String tokenSecret,String content)
	{
		try
		{
			System.out.println(apiKey);
			System.out.println(apiSecret);
			twitter.setOAuthConsumer(apiKey,apiSecret);
			accessToken = new AccessToken(token,tokenSecret);
			twitter.setOAuthAccessToken(accessToken);
			Status status = twitter.updateStatus(content);
			acknowledgment=status.getId()+"";		
		}
		catch(Exception e)
		{
			e.printStackTrace();
			acknowledgment=e.getMessage();
		}
		return acknowledgment;
	}

	/**
	 * METHOD USED TO DELETE POSTED COMMENT
	 * @param String apiKey
	 * @param String apiSecret
	 * @param String token
	 * @param String tokenSecret
	 * @param String contentId
	 * @return String
	 */
	public String deleteContent(String apiKey,String apiSecret,String token,String tokenSecret,String contentId)
	{
		Long id=Long.parseLong(contentId);
		twitter.setOAuthConsumer(apiKey, apiSecret);
		accessToken=new AccessToken(token, tokenSecret);
		twitter.setOAuthAccessToken(accessToken);
		try 
		{
			Status status=twitter.destroyStatus(id);	
			if(status!=null)
			{
				acknowledgment="Content has been deleted Successfully";
			}
		} 
		catch(TwitterException te)
		{
			if(te.getErrorMessage().endsWith("Sorry, that page does not exist"))
			{
				acknowledgment="1";
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * Method used to Import Followers List
	 * @param String apiKey
	 * @param String apiSecret
	 * @param String token
	 * @param String tokenSecret
	 * @return ArrayList
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ArrayList importContact(String apiKey,String apiSecret,String token,String tokenSecret)
	{
		ArrayList listContact=new ArrayList();
		try
		{
			twitter.setOAuthConsumer(apiKey, apiSecret);
			accessToken=new AccessToken(token, tokenSecret);
			twitter.setOAuthAccessToken(accessToken);
			Paging paging=new Paging(1,1);
			Collection<Status> statuses = twitter.getUserTimeline(paging);
			for (Status status : statuses) 
			{		    	
				listContact.add(status.getUser().getId());
				listContact.add(status.getUser().getName());
				listContact.add("http://twitter.com/"+status.getUser().getScreenName());
			}
			String twitterScreenName=twitter.getScreenName();
			PagableResponseList<User> followerIds=twitter.getFollowersList(twitterScreenName, -1);//IDs(, -1);
			for (User user : followerIds) 
			{
				System.out.println(user.getId());
				System.out.println(user.getScreenName());
				System.out.println("http://twitter.com/"+user.getScreenName());
				listContact.add(user.getId());
				listContact.add(user.getName());
				listContact.add("http://twitter.com/"+user.getScreenName());
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return listContact;
	}


	/**
	 * METHOD USED TO RETRIVE COMMENTS OF PASSED CONTENT 
	 * @param String apiKey
	 * @param String apiSecret
	 * @param String token
	 * @param String tokenSecret
	 * @param String contentId
	 * @return ArrayList
	 */

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ArrayList getCommentsOfTweet(String apiKey,String apiSecret,String token,String tokenSecret,String gid)
	{
		System.out.println(apiKey);
		ArrayList res=new ArrayList();
		Long id=Long.parseLong(gid);
		try
		{
			System.out.println("Custom API Called");
			twitter.setOAuthConsumer(apiKey, apiSecret);
			accessToken=new AccessToken(token, tokenSecret);
			twitter.setOAuthAccessToken(accessToken);
			System.out.println(id.longValue());
			RelatedResults results=twitter.getRelatedResults(id.longValue());
			List<Status> conversations=results.getTweetsWithConversation();
			System.out.println(twitter.showStatus(id).getInReplyToScreenName());
			for(int i=0;i<conversations.size();i++)
			{				
				System.out.println(results.getTweetsWithConversation().get(i).getUser().getScreenName());
				userid=results.getTweetsWithConversation().get(i).getUser().getId()+"";
				content=results.getTweetsWithConversation().get(i).getText();
				time=results.getTweetsWithConversation().get(i).getCreatedAt().toString();
				fav=results.getTweetsWithConversation().get(i).isFavorited()+"";
				res.add(results.getTweetsWithConversation().get(i).getId());
				System.out.println(results.getTweetsWithConversation().get(i).getId());
				res.add(userid);
				System.out.println(userid);
				res.add(content);
				System.out.println(content);
				res.add(time);
				System.out.println(time);
				res.add(fav);
				System.out.println(fav);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return res;
	}

	/**
	 * 
	 * METHOD USED TO SEND DIRECT MESSAGE TO FRIEND/FOLLOWER
	 * @param String apiKey
	 * @param String apiSecret
	 * @param String token
	 * @param String tokenSecret
	 * @param String contentId
	 * @param message
	 * @return String 
	 */
	public String sendMessage(String apiKey,String apiSecret,String token,String tokenSecret,String gid,String msg)
	{
		Long id=Long.parseLong(gid);

		try
		{
			twitter.setOAuthConsumer(apiKey, apiSecret);
			accessToken=new AccessToken(token, tokenSecret);
			twitter.setOAuthAccessToken(accessToken);
			twitter4j.DirectMessage sender=twitter.sendDirectMessage(id, msg);
			System.out.println(sender.getRecipient());
			System.out.println(sender.getText());
			System.out.println(sender.getId());
			ack=sender.getId()+"";
		}
		catch (Exception e) 
		{
			ack="Exception Generated";
			e.printStackTrace();
		}
		return ack;
	}


	/**
	 * THIS METHOD USED TO RETRIVE WHOLE INBOX OF TWITTER
	 * @param String apiKey
	 * @param String apiSecret
	 * @param String token
	 * @param String tokenSecret
	 * @return ArrayList
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ArrayList getDirectMessages(String apiKey,String apiSecret,String token,String tokenSecret)
	{
		ArrayList res=new ArrayList();
		try
		{
			twitter.setOAuthConsumer(apiKey, apiSecret);
			accessToken=new AccessToken(token, tokenSecret);
			twitter.setOAuthAccessToken(accessToken);
			ResponseList<twitter4j.DirectMessage> sender=twitter.getDirectMessages();
			for(twitter4j.DirectMessage dm:sender)
			{
				res.add(dm.getId());
				res.add(dm.getText());
				res.add(dm.getSender().getName());
				res.add(dm.getSenderId());
			}

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return res;		
	}


	/**
	 * METHOD USED TO RETRIVE HOME_TIME_LINE
	 * @param String apiKey
	 * @param String apiSecret
	 * @param String userToken
	 * @param String userTokenSecret
	 * @return ArrayList
	 * 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ArrayList fetchHomeTimeline(String apiKey,String apiSecret,String userToken,String userTokenSecret,int pageNo,int contentNo,Long updateId)
	{
		ArrayList timeLineValues=new ArrayList();
		Paging page = null;
		ResponseList<Status> statuses;
		try
		{
			twitter.setOAuthConsumer(apiKey,apiSecret);
			accessToken=new AccessToken(userToken,userTokenSecret);
			twitter.setOAuthAccessToken(accessToken);
			if(pageNo >= 0 && contentNo>0 && updateId==null )
			{
				System.out.println("Content id is Not Available");
				page=new Paging(pageNo,contentNo);
				statuses=twitter.getHomeTimeline(page);
			}
			else if(pageNo >= 0 && contentNo>0 && updateId!=null)
			{
				System.out.println("Everything is Available");
				page=new Paging(pageNo, contentNo,updateId.longValue());
				statuses=twitter.getHomeTimeline(page);
			}
			else if(pageNo<=0 && contentNo<=0 && updateId!=null)
			{
				System.out.println("Only ContentID is Available");
				page=new Paging(updateId.longValue());
				statuses=twitter.getHomeTimeline(page);
			}
			else if(pageNo>0 && updateId!=null)
			{
				System.out.println("Only ContentID and Page Number is Available");
				page=new Paging(pageNo,updateId.longValue());
				statuses=twitter.getHomeTimeline(page);
			}
			else
			{
				statuses=twitter.getHomeTimeline();
			}

			for(Status status:statuses)
			{
				timeLineValues.add(status.getId()+"");
				timeLineValues.add("@"+status.getUser().getScreenName());
				timeLineValues.add(status.getUser().getName());
				timeLineValues.add(status.getText());
				timeLineValues.add(status.getCreatedAt());
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return timeLineValues;
	}

	/**
	 * METHOD USED FOR DELETION OF PARTICULAR INBOX MESSAGE
	 * @param String apiKey
	 * @param String apiSecret
	 * @param String userToken
	 * @param String userTokenSecret
	 * @param String msgId
	 * @return String
	 */
	public String deleteInbox(String apiKey,String apiSecret,String userToken,String userTokenSecret,String msgId)
	{
		Long id=Long.parseLong(msgId);
		try
		{
			System.out.println("Message ID is:="+msgId);
			twitter.setOAuthConsumer(apiKey,apiSecret);
			accessToken=new AccessToken(userToken,userTokenSecret);
			twitter.setOAuthAccessToken(accessToken);
			DirectMessage msgDeleted=twitter.destroyDirectMessage(id);
			if(msgDeleted!=null)
			{
				ack="Deleted";
			}
		}
		catch(TwitterException te)
		{
			System.out.println(te.getErrorMessage());
			if(te.getErrorMessage().endsWith("Sorry, that page does not exist"))
			{
				ack="1";
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return ack;
	}



	/**
	 * IT WILL RETRIVE ALL INCOMING FOLLOWER'S REQUEST
	 * @param String apiKey
	 * @param String apiSecret
	 * @param String userToken
	 * @param String userTokenSecret
	 * @return ArrayList
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ArrayList retrivePendingRequest(String apiKey,String apiSecret,String userToken,String userTokenSecret)
	{
		ArrayList returnBck=new ArrayList();
		twitter.setOAuthConsumer(apiKey,apiSecret);
		accessToken=new AccessToken(userToken,userTokenSecret);
		twitter.setOAuthAccessToken(accessToken);
		try
		{
			IDs ids=twitter.getIncomingFriendships(-1);
			long[] followerIds=ids.getIDs();
			for(int i=0;i<followerIds.length;i++)
			{
				User user=twitter.showUser(followerIds[i]);
				returnBck.add("www.twitter.com/"+user.getScreenName());
				returnBck.add(user.getId());
				returnBck.add(user.getScreenName());
				returnBck.add(user.getName());
				returnBck.add(user.getCreatedAt().toString());
				System.out.println(user.getId());
				System.out.println(user.getScreenName());
				System.out.println(user.getName());
				System.out.println("www.twitter.com/"+user.getScreenName());
			}			
		}
		catch (Exception e)		
		{
			e.printStackTrace();
		}
		return returnBck;
	}

	public String postTweetReplay(String apiKey,String apiSecret,String userToken,String userTokenSecret,String cotnentId,String content)
	{
		twitter.setOAuthConsumer(apiKey,apiSecret);
		accessToken=new AccessToken(userToken,userTokenSecret);
		twitter.setOAuthAccessToken(accessToken);
		try
		{
			Long id=Long.parseLong(cotnentId);
			StatusUpdate statusUpdate= new StatusUpdate(content);
			statusUpdate.inReplyToStatusId(id.longValue());
			Status status=twitter.updateStatus(statusUpdate);
			ack=status.getId()+"";			
		}
		catch (Exception e) 
		{
			ack=e.getMessage();
			e.printStackTrace();
		}
		return ack;
	}


	public void searchTweet(String apiKey,String apiSecret,String userToken,String userTokenSecret,String searchKeyword)
	{
		ArrayList lstSearchTweet=new ArrayList();
		twitter.setOAuthConsumer(apiKey, apiSecret);
		accessToken=new AccessToken(userToken, userTokenSecret);
		twitter.setOAuthAccessToken(accessToken);
		try
		{
			System.out.println("Executed");
			Query query = new Query();
			query.setQuery(searchKeyword);
			//ResponseList<User> personResult=twitter.searchUsers(query.getQuery(),1);

			QueryResult result = twitter.search(query);
			List<Status> statuses=result.getTweets();
			for(int i=0;i<statuses.size();i++)
			{
				lstSearchTweet.add(statuses.get(i).getUser().getScreenName());
				lstSearchTweet.add(statuses.get(i).getText());
				/*System.out.println(statuses.get(i).getUser().getScreenName());
				System.out.println(statuses.get(i).getText());*/
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

	public ArrayList searchPerson(String apiKey,String apiSecret,String userToken,String userTokenSecret,String searchKeyword)
	{
		ArrayList lstSearchValue=new ArrayList();
		try
		{
			twitter.setOAuthConsumer(apiKey, apiSecret);
			accessToken=new AccessToken(userToken, userTokenSecret);
			twitter.setOAuthAccessToken(accessToken);
			ResponseList<User> searchedPerson=twitter.searchUsers(searchKeyword, 0);
			System.out.println(searchedPerson.size());
			for(int i=0;i<searchedPerson.size();i++)
			{
				lstSearchValue.add(searchedPerson.get(i).getId());
				lstSearchValue.add(searchedPerson.get(i).getName());
				lstSearchValue.add("www.twitter.com/"+searchedPerson.get(i).getScreenName());
				System.out.println(searchedPerson.get(i).getId());
			}
			//FriendsFollowersResources requesr=twitter.createFriendship(userId, follow)
		}
		catch (TwitterException te) 
		{
			System.out.println(te.getErrorMessage());
			System.out.println(te.getCause());
			te.printStackTrace();
		}
		catch (Exception e) 
		{

			e.printStackTrace();
		}
		return lstSearchValue;
	}

	//ADDED BY ME ON 16-04-2013
	public DateTime getStatusTime(String id) throws NumberFormatException, TwitterException	//String apiKey,String apiSecret,String userToken,String userTokenSecret,
	{
		//				twitter.setOAuthConsumer(apiKey,apiSecret);
		//				accessToken=new AccessToken(userToken,userTokenSecret);
		//				twitter.setOAuthAccessToken(accessToken);

		Status s=twitter.showStatus(Long.parseLong(id));
		Date d=s.getCreatedAt();
		DateTime date=new DateTime(d);
		return date;
	}
	public static void main(String args[]) 
	{
		try 
		{
			TwitterConnectionClass twc = new TwitterConnectionClass();
			//System.out.println(twc.getAuthURL("3BuEKFJMxBEBM2XoglkKiw","oHqo9q8VH2ys89nUESYfWzJRBXDa5MMUhxe73SW4Dc"));
			//twc.fetchHomeTimeline("7Bt5TV83SR92wP4cpBntw","lUMRE8QfUZlT8CHn0sLuYFUeoRuPr07GxJqCU2sB1g","39463017-gRg7Gg0wdzzWhNLBapZpGoDfxWYcNOYsXHtUBwKnr","YeeG8bDyyzsm3VV5p5WAneccYntYEGdV1fCwaihixPw");
			//twc.getDirectMessages("7Bt5TV83SR92wP4cpBntw","lUMRE8QfUZlT8CHn0sLuYFUeoRuPr07GxJqCU2sB1g","39463017-gRg7Gg0wdzzWhNLBapZpGoDfxWYcNOYsXHtUBwKnr","YeeG8bDyyzsm3VV5p5WAneccYntYEGdV1fCwaihixPw");
			//System.out.println(twc.deleteInbox("7Bt5TV83SR92wP4cpBntw","lUMRE8QfUZlT8CHn0sLuYFUeoRuPr07GxJqCU2sB1g","39463017-gRg7Gg0wdzzWhNLBapZpGoDfxWYcNOYsXHtUBwKnr","YeeG8bDyyzsm3VV5p5WAneccYntYEGdV1fCwaihixPw","316127053277708288"));
			//twc.getCommentsOfTweet("1t5oXnt5p4wTElFhD5to0g","yBXa9R05dpIyplzomCw0noJlUItD8s0GCfAbRZqnMc","39463017-gRg7Gg0wdzzWhNLBapZpGoDfxWYcNOYsXHtUBwKnr","YeeG8bDyyzsm3VV5p5WAneccYntYEGdV1fCwaihixPw","326935995540987905");
			//twc.searchPerson("7Bt5TV83SR92wP4cpBntw","lUMRE8QfUZlT8CHn0sLuYFUeoRuPr07GxJqCU2sB1g","39463017-gRg7Gg0wdzzWhNLBapZpGoDfxWYcNOYsXHtUBwKnr","YeeG8bDyyzsm3VV5p5WAneccYntYEGdV1fCwaihixPw","apar amin");
			//System.out.println(twc.getStatusTime("326935995540987905"));
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
}
