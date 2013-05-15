package com.axelor.sn.twitter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.joda.time.DateTime;
import twitter4j.*;
import twitter4j.auth.*;

/**
 * 
 * @author axelor-APAR
 * 
 */
public class TwitterConnectionClass {
	// Main TwitterFactory Object by which all operations can be performed
	Twitter twitter = new TwitterFactory().getInstance();

	// To Storing AccessToken
	AccessToken accessToken;

	// For Geting Current User from its code Temporary File will be created
	// storing objects
	Subject subject = SecurityUtils.getSubject();
	com.axelor.auth.db.User currentUser = com.axelor.auth.db.User.all()
			.filter("self.code = ?1", subject.getPrincipal()).fetchOne();

	// For Acknowledgement
	String acknowledgment = "";

	String userid, content, time, fav;

	@SuppressWarnings("rawtypes")
	HashMap mapReturnValue;

	// ========== CORE METHOD ===============
	/**
	 * IT WILL GENERATE UNIQUE AUTHENTICATION URL WHICH WILL ALLOW USER TO
	 * CONNECT WITH TWITTER FROM ABS
	 * 
	 * @param apiKey
	 * @param apiSecret
	 * @param redirectUrl
	 * @return
	 */
	public String getAuthUrl4j(String apiKey, String apiSecret,
			String redirectUrl) {
		twitter.setOAuthConsumer(apiKey, apiSecret);
		try {
			RequestToken requestToken = twitter
					.getOAuthRequestToken(redirectUrl);
			acknowledgment = requestToken.getAuthorizationURL();
			File file = new File(currentUser.getCode());
			ObjectOutputStream outStream = new ObjectOutputStream(
					new FileOutputStream(file));
			outStream.writeObject(twitter);
			outStream.writeObject(requestToken);
			outStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return acknowledgment;
	}

	// ========= METHODS FOR RETRIVAL OF DATA =========

	/**
	 * THIS METHOD IS USED TO RETRIVE ALL FOLLOWERS
	 * 
	 * @param apiKey
	 * @param apiSecret
	 * @param token
	 * @param tokenSecret
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ArrayList<HashMap> importContact(String apiKey, String apiSecret,
			String token, String tokenSecret) {
		ArrayList<HashMap> listContacts = new ArrayList<HashMap>();

		try {
			twitter.setOAuthConsumer(apiKey, apiSecret);
			accessToken = new AccessToken(token, tokenSecret);
			twitter.setOAuthAccessToken(accessToken);
			Paging paging = new Paging(1, 1);
			Collection<Status> statuses = twitter.getUserTimeline(paging);
			for (Status status : statuses) {
				mapReturnValue = new HashMap();
				mapReturnValue.put("twitterId", status.getUser().getId());
				mapReturnValue.put("twitterName", status.getUser().getName());
				mapReturnValue.put("twitterLink", "http://twitter.com/"
						+ status.getUser().getScreenName());
				listContacts.add(mapReturnValue);
			}
			String twitterScreenName = twitter.getScreenName();
			PagableResponseList<User> followerIds = twitter.getFollowersList(
					twitterScreenName, -1);// IDs(, -1);
			for (User user : followerIds) {
				mapReturnValue = new HashMap();
				mapReturnValue.put("twitterId", user.getId());
				mapReturnValue.put("twitterName", user.getName());
				mapReturnValue.put("twitterLink",
						"http://twitter.com/" + user.getScreenName());
				listContacts.add(mapReturnValue);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return listContacts;
	}

	/**
	 * METHOD USED TO RETRIVE COMMENTS OF PASSED CONTENT
	 * 
	 * @param apiKey
	 * @param apiSecret
	 * @param token
	 * @param tokenSecret
	 * @param gid
	 * @return
	 */

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ArrayList<HashMap> getCommentsOfTweet(String apiKey,
			String apiSecret, String token, String tokenSecret, String gid) {
		ArrayList<HashMap> lstResult = new ArrayList<HashMap>();
		Long id = Long.parseLong(gid);
		try {
			twitter.setOAuthConsumer(apiKey, apiSecret);
			accessToken = new AccessToken(token, tokenSecret);
			twitter.setOAuthAccessToken(accessToken);
			RelatedResults results = twitter.getRelatedResults(id.longValue());
			List<Status> lstConversations = results.getTweetsWithConversation();

			for (int i = 0; i < lstConversations.size(); i++) {
				mapReturnValue = new HashMap();
				mapReturnValue.put("userid", lstConversations.get(i).getUser()
						.getId()
						+ "");
				mapReturnValue
						.put("content", lstConversations.get(i).getText());
				mapReturnValue.put("time", lstConversations.get(i)
						.getCreatedAt().toString());
				mapReturnValue.put("fav", lstConversations.get(i).isFavorited()
						+ "");
				mapReturnValue
						.put("commentId", lstConversations.get(i).getId());
				lstResult.add(mapReturnValue);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lstResult;
	}
	
	/*@SuppressWarnings({ "unchecked", "rawtypes" })
	public ArrayList<HashMap> getCommentsOfTweet(String apiKey,
			String apiSecret, String token, String tokenSecret, String gid) {
		ArrayList<HashMap> lstResult = new ArrayList<HashMap>();
		Long id = Long.parseLong(gid);
		System.out.println(id);
		try {
			twitter.setOAuthConsumer(apiKey, apiSecret);
			accessToken = new AccessToken(token, tokenSecret);
			twitter.setOAuthAccessToken(accessToken);
			Paging page = new Paging(1,200);
			//page.setMaxId(id);
			System.out.println(page);
			Status replayStatus = twitter.showStatus(id);
			System.out.println(twitter.getMentionsTimeline(new Paging(1,200)));	
//			twitter.getUserListStatuses(arg0, arg1)
			ResponseList<Status> allStatus = twitter.getMentions(page);
			
			for(Status oneStatus : allStatus)
			{
				System.out.println(oneStatus.getId());
				
			}
			/*RelatedResults results = twitter.getRelatedResults(id.longValue());
			List<Status> lstConversations = results.getTweetsWithConversation();

			for (int i = 0; i < lstConversations.size(); i++) {
				mapReturnValue = new HashMap();
				mapReturnValue.put("userid", lstConversations.get(i).getUser()
						.getId()
						+ "");
				mapReturnValue
						.put("content", lstConversations.get(i).getText());
				mapReturnValue.put("time", lstConversations.get(i)
						.getCreatedAt().toString());
				mapReturnValue.put("fav", lstConversations.get(i).isFavorited()
						+ "");
				mapReturnValue
						.put("commentId", lstConversations.get(i).getId());
				lstResult.add(mapReturnValue);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return lstResult;
	}
*/
	/**
	 * THIS METHOD USED TO RETRIVE WHOLE INBOX OF TWITTER
	 * 
	 * @param apiKey
	 * @param apiSecret
	 * @param token
	 * @param tokenSecret
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ArrayList<HashMap> getDirectMessages(String apiKey,
			String apiSecret, String token, String tokenSecret) {
		ArrayList<HashMap> lstResult = new ArrayList<HashMap>();
		try {
			twitter.setOAuthConsumer(apiKey, apiSecret);
			accessToken = new AccessToken(token, tokenSecret);
			twitter.setOAuthAccessToken(accessToken);
			ResponseList<twitter4j.DirectMessage> sender = twitter
					.getDirectMessages();
			for (twitter4j.DirectMessage dm : sender) {
				mapReturnValue = new HashMap();
				mapReturnValue.put("msgId", dm.getId());
				mapReturnValue.put("msgContent", dm.getText());
				mapReturnValue.put("senderName", dm.getSender().getName());
				mapReturnValue.put("senderId", dm.getSenderId());
				lstResult.add(mapReturnValue);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return lstResult;
	}

	/**
	 * METHOD IS USED TO RETRIVE USER's HOME TIMELINE
	 * 
	 * @param apiKey
	 * @param apiSecret
	 * @param userToken
	 * @param userTokenSecret
	 * @param pageNo
	 * @param contentNo
	 * @param updateId
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ArrayList<HashMap> fetchHomeTimeline(String apiKey,
			String apiSecret, String userToken, String userTokenSecret,
			int pageNo, int contentNo, Long updateId) {
		ArrayList<HashMap> lstTimeLineValues = new ArrayList<HashMap>();
		Paging page = null;
		ResponseList<Status> statuses;
		try {
			twitter.setOAuthConsumer(apiKey, apiSecret);
			accessToken = new AccessToken(userToken, userTokenSecret);
			twitter.setOAuthAccessToken(accessToken);
			if (pageNo >= 0 && contentNo > 0 && updateId == null) {
				page = new Paging(pageNo, contentNo);
				statuses = twitter.getHomeTimeline(page);
			} else if (pageNo >= 0 && contentNo > 0 && updateId != null) {
				page = new Paging(pageNo, contentNo, updateId.longValue());
				statuses = twitter.getHomeTimeline(page);
			} else if (pageNo <= 0 && contentNo <= 0 && updateId != null) {
				page = new Paging(updateId.longValue());
				statuses = twitter.getHomeTimeline(page);
			} else if (pageNo > 0 && updateId != null) {
				page = new Paging(pageNo, updateId.longValue());
				statuses = twitter.getHomeTimeline(page);
			} else {
				statuses = twitter.getHomeTimeline();
			}

			for (Status status : statuses) {
				mapReturnValue = new HashMap();
				mapReturnValue.put("updateId", status.getId() + "");
				mapReturnValue.put("screenName", "@"
						+ status.getUser().getScreenName());
				mapReturnValue.put("twitterName", status.getUser().getName());
				mapReturnValue.put("updateContent", status.getText());
				mapReturnValue.put("updateCreationTime", status.getCreatedAt());
				lstTimeLineValues.add(mapReturnValue);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lstTimeLineValues;
	}

	/**
	 * IT WILL RETRIVE ALL INCOMING FOLLOWER'S REQUEST
	 * 
	 * @param apiKey
	 * @param apiSecret
	 * @param userToken
	 * @param userTokenSecret
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ArrayList<HashMap> retrivePendingRequest(String apiKey,
			String apiSecret, String userToken, String userTokenSecret) {
		ArrayList lstReturnBack = new ArrayList();

		twitter.setOAuthConsumer(apiKey, apiSecret);
		accessToken = new AccessToken(userToken, userTokenSecret);
		twitter.setOAuthAccessToken(accessToken);
		try {
			IDs ids = twitter.getIncomingFriendships(-1);
			long[] followerIds = ids.getIDs();
			for (int i = 0; i < followerIds.length; i++) {
				mapReturnValue = new HashMap();
				User user = twitter.showUser(followerIds[i]);
				mapReturnValue.put("twitterLink",
						"www.twitter.com/" + user.getScreenName());
				mapReturnValue.put("twitterId", user.getId());
				mapReturnValue.put("twitterScreenName", user.getScreenName());
				mapReturnValue.put("twitterName", user.getName());
				mapReturnValue.put("sendedOn", user.getCreatedAt().toString());
				lstReturnBack.add(mapReturnValue);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lstReturnBack;
	}

	/**
	 * THIS METHOD USED TO ALLOW SEARCH ON PARTICULAR TWEET
	 * 
	 * @param apiKey
	 * @param apiSecret
	 * @param userToken
	 * @param userTokenSecret
	 * @param searchKeyword
	 * @return
	 */

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ArrayList<HashMap> searchTweet(String apiKey, String apiSecret,
			String userToken, String userTokenSecret, String searchKeyword) {
		ArrayList<HashMap> lstSearchTweet = new ArrayList<HashMap>();
		twitter.setOAuthConsumer(apiKey, apiSecret);
		accessToken = new AccessToken(userToken, userTokenSecret);
		twitter.setOAuthAccessToken(accessToken);
		try {
			System.out.println("Executed");
			Query query = new Query();
			query.setQuery(searchKeyword);
			QueryResult result = twitter.search(query);
			List<Status> statuses = result.getTweets();
			for (int i = 0; i < statuses.size(); i++) {
				mapReturnValue = new HashMap();
				mapReturnValue.put("userScreennName", "@"
						+ statuses.get(i).getUser().getScreenName());
				mapReturnValue.put("tweetText", statuses.get(i).getText());
				lstSearchTweet.add(mapReturnValue);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lstSearchTweet;
	}

	/**
	 * THIS METHOD USED TO SEARCH A PARTICUAL PERSON ON TWITTER
	 * 
	 * @param apiKey
	 * @param apiSecret
	 * @param userToken
	 * @param userTokenSecret
	 * @param searchKeyword
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ArrayList<HashMap> searchPerson(String apiKey, String apiSecret,
			String userToken, String userTokenSecret, String searchKeyword) {
		ArrayList<HashMap> lstSearchValue = new ArrayList<HashMap>();
		try {
			twitter.setOAuthConsumer(apiKey, apiSecret);
			accessToken = new AccessToken(userToken, userTokenSecret);
			twitter.setOAuthAccessToken(accessToken);
			ResponseList<User> searchedPerson = twitter.searchUsers(
					searchKeyword, 0);
			for (int i = 0; i < searchedPerson.size(); i++) {
				mapReturnValue = new HashMap();
				mapReturnValue.put("twitterUserId", searchedPerson.get(i)
						.getId());
				mapReturnValue.put("twitterUserName", searchedPerson.get(i)
						.getName());
				mapReturnValue.put("twitterProfileLink", "www.twitter.com/"
						+ searchedPerson.get(i).getScreenName());
				lstSearchValue.add(mapReturnValue);
			}
		} catch (TwitterException te) {
			te.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lstSearchValue;
	}

	public DateTime getStatusTime(String id) throws NumberFormatException,
			TwitterException // String apiKey,String apiSecret,String
								// userToken,String userTokenSecret,
	{
		Status s = twitter.showStatus(Long.parseLong(id));
		Date d = s.getCreatedAt();
		DateTime date = new DateTime(d);
		return date;
	}

	// =========== POST METHODS ============

	/**
	 * USED FOR POSTING TWEET
	 * 
	 * @param apiKey
	 * @param apiSecret
	 * @param token
	 * @param tokenSecret
	 * @param content
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public HashMap postTweet(String apiKey, String apiSecret, String token,
			String tokenSecret, String content) {
		mapReturnValue = new HashMap();
		try {
			twitter.setOAuthConsumer(apiKey, apiSecret);
			accessToken = new AccessToken(token, tokenSecret);
			twitter.setOAuthAccessToken(accessToken);
			Status status = twitter.updateStatus(content);
			mapReturnValue.put("acknowledgment", status.getId() + "");
		} catch (Exception e) {
			e.printStackTrace();
			acknowledgment = e.getMessage();
		}
		return mapReturnValue;
	}

	/**
	 * IT WILL USED TO POST REPLAY ON PARTICULAR TWEET
	 * 
	 * @param apiKey
	 * @param apiSecret
	 * @param userToken
	 * @param userTokenSecret
	 * @param cotnentId
	 * @param content
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public HashMap postTweetReplay(String apiKey, String apiSecret,
			String userToken, String userTokenSecret, String cotnentId,
			String content) {
		mapReturnValue = new HashMap();
		twitter.setOAuthConsumer(apiKey, apiSecret);
		accessToken = new AccessToken(userToken, userTokenSecret);
		twitter.setOAuthAccessToken(accessToken);
		try {
			Long id = Long.parseLong(cotnentId);
			StatusUpdate statusUpdate = new StatusUpdate(content);
			statusUpdate.inReplyToStatusId(id.longValue());
			Status status = twitter.updateStatus(statusUpdate);
			mapReturnValue.put("acknowledgment", status.getId() + "");
		} catch (Exception e) {
			acknowledgment = e.getMessage();
			e.printStackTrace();
		}
		return mapReturnValue;
	}

	/**
	 * METHOD USED TO SEND DIRECT MESSAGE TO FRIEND/FOLLOWER
	 * 
	 * @param apiKey
	 * @param apiSecret
	 * @param token
	 * @param tokenSecret
	 * @param toId
	 * @param msgToSend
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public HashMap sendMessage(String apiKey, String apiSecret, String token,
			String tokenSecret, String gid, String msg) {
		mapReturnValue = new HashMap();
		Long id = Long.parseLong(gid);
		try {
			twitter.setOAuthConsumer(apiKey, apiSecret);
			accessToken = new AccessToken(token, tokenSecret);
			twitter.setOAuthAccessToken(accessToken);
			twitter4j.DirectMessage sender = twitter.sendDirectMessage(id, msg);
			mapReturnValue.put("acknowledgment", sender.getId() + "");
		} catch (Exception e) {
			acknowledgment = "Exception Generated";
			e.printStackTrace();
		}
		return mapReturnValue;
	}

	// =========== DELETION METHOD =============
	/**
	 * METHOD USED FOR DELETION OF PARTICULAR INBOX MESSAGE
	 * 
	 * @param apiKey
	 * @param apiSecret
	 * @param userToken
	 * @param userTokenSecret
	 * @param msgId
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public HashMap deleteInbox(String apiKey, String apiSecret,
			String userToken, String userTokenSecret, String msgId) {
		mapReturnValue = new HashMap();
		Long id = Long.parseLong(msgId);
		boolean valueStatus = false;
		try {
			twitter.setOAuthConsumer(apiKey, apiSecret);
			accessToken = new AccessToken(userToken, userTokenSecret);
			twitter.setOAuthAccessToken(accessToken);
			DirectMessage msgDeleted = twitter.destroyDirectMessage(id);
			if (msgDeleted != null)
				mapReturnValue.put("acknowledgment", valueStatus);

		} catch (TwitterException te) {
			System.out.println(te.getErrorMessage());
			if (te.getErrorCode() == 34)
				mapReturnValue.put("acknowledgment", valueStatus);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return mapReturnValue;
	}

	/**
	 * METHOD USED TO DELETE POSTED COMMENT
	 * 
	 * @param apiKey
	 * @param apiSecret
	 * @param token
	 * @param tokenSecret
	 * @param contentId
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public HashMap deleteContent(String apiKey, String apiSecret, String token,
			String tokenSecret, String contentId) {
		mapReturnValue = new HashMap();
		boolean valueStatus = true;
		Long id = Long.parseLong(contentId);
		twitter.setOAuthConsumer(apiKey, apiSecret);
		accessToken = new AccessToken(token, tokenSecret);
		twitter.setOAuthAccessToken(accessToken);
		try {
			Status status = twitter.destroyStatus(id);
			if (status != null)
				mapReturnValue.put("acknowledgment", valueStatus);

		} catch (TwitterException te) {
			if (te.getErrorCode() == 34)
				mapReturnValue.put("acknowledgment", valueStatus);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return mapReturnValue;
	}

	// =========== ALL ENDS HERE ==========
	public static void main(String args[]) {
		try {
			TwitterConnectionClass twc = new TwitterConnectionClass();
			// twc.getAuthURL("1t5oXnt5p4wTElFhD5to0g",
			// "yBXa9R05dpIyplzomCw0noJlUItD8s0GCfAbRZqnMc");
			// System.out.println(twc.getAuthURL("3BuEKFJMxBEBM2XoglkKiw","oHqo9q8VH2ys89nUESYfWzJRBXDa5MMUhxe73SW4Dc"));
			// twc.fetchHomeTimeline("7Bt5TV83SR92wP4cpBntw","lUMRE8QfUZlT8CHn0sLuYFUeoRuPr07GxJqCU2sB1g","39463017-gRg7Gg0wdzzWhNLBapZpGoDfxWYcNOYsXHtUBwKnr","YeeG8bDyyzsm3VV5p5WAneccYntYEGdV1fCwaihixPw");
			// twc.getDirectMessages("7Bt5TV83SR92wP4cpBntw","lUMRE8QfUZlT8CHn0sLuYFUeoRuPr07GxJqCU2sB1g","39463017-gRg7Gg0wdzzWhNLBapZpGoDfxWYcNOYsXHtUBwKnr","YeeG8bDyyzsm3VV5p5WAneccYntYEGdV1fCwaihixPw");
			// System.out.println(twc.deleteInbox("7Bt5TV83SR92wP4cpBntw","lUMRE8QfUZlT8CHn0sLuYFUeoRuPr07GxJqCU2sB1g","39463017-gRg7Gg0wdzzWhNLBapZpGoDfxWYcNOYsXHtUBwKnr","YeeG8bDyyzsm3VV5p5WAneccYntYEGdV1fCwaihixPw","316127053277708288"));
			// twc.getCommentsOfTweet("1t5oXnt5p4wTElFhD5to0g","yBXa9R05dpIyplzomCw0noJlUItD8s0GCfAbRZqnMc","39463017-gRg7Gg0wdzzWhNLBapZpGoDfxWYcNOYsXHtUBwKnr","YeeG8bDyyzsm3VV5p5WAneccYntYEGdV1fCwaihixPw","326935995540987905");
			// twc.searchPerson("7Bt5TV83SR92wP4cpBntw","lUMRE8QfUZlT8CHn0sLuYFUeoRuPr07GxJqCU2sB1g","39463017-gRg7Gg0wdzzWhNLBapZpGoDfxWYcNOYsXHtUBwKnr","YeeG8bDyyzsm3VV5p5WAneccYntYEGdV1fCwaihixPw","apar amin");
			// System.out.println(twc.getStatusTime("326935995540987905"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
