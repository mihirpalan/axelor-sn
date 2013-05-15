package com.axelor.sn.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.sn.db.ApplicationCredentials;
import com.axelor.sn.db.DirectMessage;
import com.axelor.sn.db.ImportContact;
import com.axelor.sn.db.PersonalCredential;
import com.axelor.sn.db.PostTweet;
import com.axelor.sn.db.SocialNetworking;
import com.axelor.sn.db.TweetComment;
import com.axelor.sn.db.TwitterConfig;
import com.axelor.sn.db.TwitterFollowerRequest;
import com.axelor.sn.db.TwitterHomeTimeline;
import com.axelor.sn.db.TwitterInbox;
import com.axelor.sn.twitter.TwitterConnectionClass;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.joda.time.DateTime;

/**
 * 
 * @author axelor-APAR
 * 
 */
public class SNTWTService {
	@Inject
	public TwitterConnectionClass twtconnect;
	String acknowledgment, apiKey, apiSecret, userToken, userTokenSecret,
			redirectUrl;

	@SuppressWarnings("rawtypes")
	ArrayList<HashMap> lstReturnResponse = new ArrayList<HashMap>();
	@SuppressWarnings("rawtypes")
	HashMap mapRetriveValues = new HashMap();

	/**
	 * @author axelor-APAR
	 * @param user
	 * @param sn
	 * @return authorizationUrl
	 */
	@Transactional
	public String obtainToken(User user, SocialNetworking sn) {
		try {

			List<ApplicationCredentials> query = ApplicationCredentials.all()
					.filter("snType=? ", sn).fetch();
			List<PersonalCredential> credential = PersonalCredential.all()
					.filter("userId=? and snType=?", user, sn).fetch();
			if (query.isEmpty()) {
				acknowledgment = "Sorry You can't Do anyting Admin Doesnt set Application Credentials ";
				throw new javax.validation.ValidationException(
						"No Application Credentials Available");
			} else {
				if (!credential.isEmpty()) {
					acknowledgment = "You Already Have One Account Associated...";
					throw new javax.validation.ValidationException(
							"You Already Have One Account Associated...");
				} else {
					for (int i = 0; i < query.size(); i++) {
						apiKey = query.get(i).getApikey();
						apiSecret = query.get(i).getApisecret();
						redirectUrl = query.get(i).getRedirectUrl();
					}
					acknowledgment = twtconnect.getAuthUrl4j(apiKey, apiSecret,
							redirectUrl);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return acknowledgment;

	}

	public SocialNetworking saySnType() {
		SocialNetworking snType = SocialNetworking.all()
				.filter("name=?", "twitter").fetchOne();
		return snType;
	}

	/**
	 * @author axelor-APAR
	 * @param currentUser
	 * @param contentToTweet
	 * @return acknowledgement
	 */
	@Transactional
	public String postTweet(User user, String content) {
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name=?", "twitter").fetchOne();
			List<ApplicationCredentials> query = ApplicationCredentials.all()
					.filter("snType=? ", sn).fetch();
			List<PersonalCredential> credential = PersonalCredential.all()
					.filter("userId=? and snType=?", user, sn).fetch();

			if (credential.isEmpty())
				acknowledgment = "0";
			else {
				apiKey = query.get(0).getApikey();
				apiSecret = query.get(0).getApisecret();
				userToken = credential.get(0).getUserToken();
				userTokenSecret = credential.get(0).getUserTokenSecret();
				mapRetriveValues = twtconnect.postTweet(apiKey, apiSecret,
						userToken, userTokenSecret, content);
				if (mapRetriveValues.size() > 0)
					acknowledgment = mapRetriveValues.get("acknowledgment")
							.toString();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * @author axelor-APAR
	 * @param currentUser
	 * @param contentId
	 *            - it will validate user and make Call to custom-API for
	 *            Deletion
	 * @return acknowledgement
	 */
	@SuppressWarnings("rawtypes")
	@Transactional
	public String getDeleteMessage(User user, List contentId) {
		PostTweet postMsg;
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name=?", "twitter").fetchOne();
			List<ApplicationCredentials> query = ApplicationCredentials.all()
					.filter("snType=? ", sn).fetch();
			List<PersonalCredential> credential = PersonalCredential.all()
					.filter("userId=? and snType=?", user, sn).fetch();
			if (credential.isEmpty())
				acknowledgment = "Please Login First";

			else {
				apiKey = query.get(0).getApikey();
				apiSecret = query.get(0).getApisecret();
				userToken = credential.get(0).getUserToken();
				userTokenSecret = credential.get(0).getUserTokenSecret();
				for (int i = 0; i < contentId.size(); i++) {
					postMsg = new PostTweet();
					postMsg = PostTweet.all().filter("id=?", contentId.get(i))
							.fetchOne();
					if (!postMsg.getCommentsTweet().isEmpty()) {
						List<TweetComment> cmntList = TweetComment.all()
								.filter("id=?", contentId.get(i)).fetch();
						for (int j = 0; j < cmntList.size(); j++)
							cmntList.get(j).remove();
					}

					mapRetriveValues = twtconnect.deleteContent(apiKey,
							apiSecret, userToken, userTokenSecret,
							postMsg.getAcknowledgment());

					if (mapRetriveValues.containsKey("acknowledgment"))
						postMsg.remove();
					else
						acknowledgment = "There is Some Problem, Please Try late";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * @author axelor-APAR
	 * @param currentUser
	 * @return String acknowledgement
	 */

	@Transactional
	public String importFollowers(User user) {

		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name=?", "twitter").fetchOne();
			List<ApplicationCredentials> query = ApplicationCredentials.all()
					.filter("snType=? ", sn).fetch();
			List<PersonalCredential> credential = PersonalCredential.all()
					.filter("userId=? and snType=?", user, sn).fetch();
			if (credential.isEmpty())
				acknowledgment = "0";

			else {
				apiKey = query.get(0).getApikey();
				apiSecret = query.get(0).getApisecret();
				userToken = credential.get(0).getUserToken();
				userTokenSecret = credential.get(0).getUserTokenSecret();
				lstReturnResponse = twtconnect.importContact(apiKey, apiSecret,
						userToken, userTokenSecret);
				if (!lstReturnResponse.isEmpty()) {
					List<ImportContact> query1 = ImportContact.all()
							.filter("curUser=? and snType=?", user, sn).fetch();
					List<String> str = new ArrayList<String>();
					for (int i = 0; i < query1.size(); i++)
						str.add(query1.get(i).getSnUserId());

					// FOR LOOP USED TO CONVERT RESPONSE FROM ARRYALIST TO ONE
					// ROW OF D/B AND PERSIST INTO IT
					for (int p = 0; p < lstReturnResponse.size(); p++) {
						mapRetriveValues = lstReturnResponse.get(p);
						if (!str.contains(mapRetriveValues.get("twitterId")
								.toString())) {
							ImportContact twtcntc = new ImportContact();
							twtcntc.setSnUserId(mapRetriveValues.get(
									"twitterId").toString());
							twtcntc.setName(mapRetriveValues.get("twitterName")
									.toString());
							twtcntc.setLink(mapRetriveValues.get("twitterLink")
									.toString());
							twtcntc.setSnType(sn);
							twtcntc.setCurUser(user);
							twtcntc.persist();
						}
					}

				} else
					acknowledgment = "1";
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * @author axelor-APAR
	 * @param currentUser
	 * @param twitterUserProfileId
	 * @param msgToSend
	 * @return acknowledgement
	 */
	@Transactional
	public String sentMessage(User user, String id, String msg) {
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name=?", "twitter").fetchOne();
			List<ApplicationCredentials> query = ApplicationCredentials.all()
					.filter("snType=? ", sn).fetch();
			List<PersonalCredential> credential = PersonalCredential.all()
					.filter("userId=? and snType=?", user, sn).fetch();
			if (credential.isEmpty())
				acknowledgment = "0";
			else {
				apiKey = query.get(0).getApikey();
				apiSecret = query.get(0).getApisecret();
				userToken = credential.get(0).getUserToken();
				userTokenSecret = credential.get(0).getUserTokenSecret();
				mapRetriveValues = twtconnect.sendMessage(apiKey, apiSecret,
						userToken, userTokenSecret, id, msg);
				if (mapRetriveValues.containsKey("acknowledgment"))
					acknowledgment = mapRetriveValues.get("acknowledgment")
							.toString();

			}
		} catch (Exception e) {
			acknowledgment = e.getMessage();
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * @author axelor-APAR
	 * @param currentUser
	 * @return acknowledgement
	 */
	@Transactional
	public String getInbox(User user) {

		try {
			List<TwitterInbox> query1 = TwitterInbox.all()
					.filter("curUser=?", user).fetch();
			List<String> str = new ArrayList<String>();
			if (query1.size() > 0) {
				for (int i = 0; i < query1.size(); i++) {
					str.add(query1.get(i).getMsgId());
					query1.get(i).remove();
				}
			}
			SocialNetworking sn = SocialNetworking.all()
					.filter("name=?", "twitter").fetchOne();
			List<ApplicationCredentials> query = ApplicationCredentials.all()
					.filter("snType=? ", sn).fetch();
			List<PersonalCredential> credential = PersonalCredential.all()
					.filter("userId=? and snType=?", user, sn).fetch();
			if (credential.isEmpty())
				acknowledgment = "0";
			else {
				apiKey = query.get(0).getApikey();
				apiSecret = query.get(0).getApisecret();
				userToken = credential.get(0).getUserToken();
				userTokenSecret = credential.get(0).getUserTokenSecret();
				TwitterInbox twtibox = new TwitterInbox();
				lstReturnResponse = twtconnect.getDirectMessages(apiKey,
						apiSecret, userToken, userTokenSecret);
				if (!lstReturnResponse.isEmpty()) {
					List<TwitterInbox> inboxRecords = TwitterInbox.all()
							.filter("curUser = ?", user).fetch();
					for (int i = 0; i < inboxRecords.size(); i++)
						inboxRecords.get(i).remove();
					// FOR LOOP USED TO CONVERT RESPONSE FROM ARRYALIST TO ONE
					// ROW OF D/B AND PERSIST INTO IT
					for (int p = 0; p < lstReturnResponse.size(); p++) {
						mapRetriveValues = lstReturnResponse.get(p);
						twtibox = new TwitterInbox();
						twtibox.setMsgId(mapRetriveValues.get("msgId")
								.toString());
						twtibox.setMsgContent(mapRetriveValues
								.get("msgContent").toString());
						twtibox.setSenderName(mapRetriveValues
								.get("senderName").toString());
						twtibox.setSenderId(mapRetriveValues.get("senderId")
								.toString());
						twtibox.setCurUser(user);
						twtibox.persist();
					}
				} else
					acknowledgment = "1";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * @author axelor-APAR
	 * @param currentUser
	 * 
	 * @param contentId
	 *            - it will validate user and make Call to custom-API for
	 *            Deletion
	 * @return acknowledgement
	 */
	@Transactional
	public String deleteDirectMessage(User user,
			@SuppressWarnings("rawtypes") List contentId) {
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name=?", "twitter").fetchOne();
			List<ApplicationCredentials> query = ApplicationCredentials.all()
					.filter("snType=? ", sn).fetch();
			List<PersonalCredential> credential = PersonalCredential.all()
					.filter("userId=? and snType=?", user, sn).fetch();
			if (credential.isEmpty())
				acknowledgment = "Please Login First";

			else {
				apiKey = query.get(0).getApikey();
				apiSecret = query.get(0).getApisecret();
				userToken = credential.get(0).getUserToken();
				userTokenSecret = credential.get(0).getUserTokenSecret();
				for (int i = 0; i < contentId.size(); i++) {
					TwitterInbox delMsg = TwitterInbox.all()
							.filter("id=?", contentId.get(i)).fetchOne();

					mapRetriveValues = twtconnect.deleteInbox(apiKey,
							apiSecret, userToken, userTokenSecret,
							delMsg.getMsgId());

					if (mapRetriveValues.containsKey("acknowledgment"))
						delMsg.remove();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * @author axelor-APAR It will used to retrieve twitter's HomeTimeLine
	 * @param currentUser
	 * @return String acknowledgement
	 */
	@Transactional
	public String getHomeTimeLine(User user) {

		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name=?", "twitter").fetchOne();
			List<ApplicationCredentials> query = ApplicationCredentials.all()
					.filter("snType=? ", sn).fetch();
			List<PersonalCredential> credential = PersonalCredential.all()
					.filter("userId=? and snType=?", user, sn).fetch();
			if (credential.isEmpty())
				acknowledgment = "0";
			else {
				apiKey = query.get(0).getApikey();
				apiSecret = query.get(0).getApisecret();
				userToken = credential.get(0).getUserToken();
				userTokenSecret = credential.get(0).getUserTokenSecret();

				try {
					TwitterConfig param = TwitterConfig.all()
							.filter("curUser=?", user).fetchOne();
					if (param == null)
						throw new javax.persistence.NoResultException();
					else {
						if (param.getSince() == false)
							lstReturnResponse = twtconnect.fetchHomeTimeline(
									apiKey, apiSecret, userToken,
									userTokenSecret, param.getPage(),
									param.getContent(), null);

						else {
							EntityManager em = JPA.em();
							Long maxId = (Long) em.createQuery(
									"select MAX(e.timelineContentId) from TwitterHomeTimeline e where e.curUser="
											+ user.getId()).getSingleResult();

							if (maxId == null)
								acknowledgment = "1";
							else
								lstReturnResponse = twtconnect
										.fetchHomeTimeline(apiKey, apiSecret,
												userToken, userTokenSecret,
												param.getPage(),
												param.getContent(), maxId);

						}
					}
				} catch (javax.persistence.NoResultException e) {
					System.out
							.println("No parameter has been set for This User");
					lstReturnResponse = twtconnect.fetchHomeTimeline(apiKey,
							apiSecret, userToken, userTokenSecret, 0, 0, null);
				}
				if (!lstReturnResponse.isEmpty()) {
					TwitterHomeTimeline twtTimeline = new TwitterHomeTimeline();
					List<TwitterHomeTimeline> query1 = TwitterHomeTimeline
							.all().filter("curUser=?", user).fetch();

					for (int i = 0; i < query1.size(); i++)
						query1.get(i).remove();

					// FOR LOOP USED TO CONVERT RESPONSE FROM ARRYALIST TO ONE
					// ROW OF D/B AND PERSIST INTO IT
					for (int i = 0; i < lstReturnResponse.size(); i++) {

						mapRetriveValues = lstReturnResponse.get(i);
						twtTimeline = new TwitterHomeTimeline();
						twtTimeline.setTimelineContentId(Long
								.parseLong(mapRetriveValues.get("updateId")
										.toString()));
						twtTimeline.setScreenName(mapRetriveValues.get(
								"screenName").toString());
						twtTimeline.setActualName(mapRetriveValues.get(
								"twitterName").toString());
						twtTimeline.setActualContent(mapRetriveValues.get(
								"updateContent").toString());
						twtTimeline.setContentDate(mapRetriveValues.get(
								"updateCreationTime").toString());
						twtTimeline.setCurUser(user);
						twtTimeline.persist();
					}
				} else
					acknowledgment = "1";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * @author axelor-APAR
	 * @param currentUser
	 * @return String acknowledgement
	 */
	@Transactional
	public String getFollowerRequest(User user) {

		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name=?", "twitter").fetchOne();
			List<ApplicationCredentials> query = ApplicationCredentials.all()
					.filter("snType=? ", sn).fetch();
			List<PersonalCredential> credential = PersonalCredential.all()
					.filter("userId=? and snType=?", user, sn).fetch();
			if (credential.isEmpty())
				acknowledgment = "Please Login First";

			else {
				apiKey = query.get(0).getApikey();
				apiSecret = query.get(0).getApisecret();
				userToken = credential.get(0).getUserToken();
				userTokenSecret = credential.get(0).getUserTokenSecret();

				lstReturnResponse = twtconnect.retrivePendingRequest(apiKey,
						apiSecret, userToken, userTokenSecret);

				if (!lstReturnResponse.isEmpty()) {
					TwitterFollowerRequest twtRequest = new TwitterFollowerRequest();
					List<TwitterFollowerRequest> query1 = TwitterFollowerRequest
							.all().filter("curUser=?", user).fetch();

					if (query1.size() > 0) {
						for (int i = 0; i < query1.size(); i++)
							query1.get(i).remove();

					}

					// FOR LOOP USED TO CONVERT RESPONSE FROM ARRYALIST TO ONE
					// ROW OF D/B AND PERSIST INTO IT
					for (int i = 0; i < lstReturnResponse.size(); i++) {
						mapRetriveValues = lstReturnResponse.get(i);
						twtRequest = new TwitterFollowerRequest();
						twtRequest.setLink(mapRetriveValues.get("twitterLink")
								.toString());
						twtRequest.setUserId(mapRetriveValues.get("twitterId")
								.toString());
						twtRequest.setScreenName(mapRetriveValues.get(
								"twitterScreenName").toString());
						twtRequest.setName(mapRetriveValues.get("twitterName")
								.toString());
						twtRequest.setSendedOn(mapRetriveValues.get("sendedOn")
								.toString());
						twtRequest.setCurUser(user);
						twtRequest.persist();

					}

				} else
					acknowledgment = "You do not have any Pending Request";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return acknowledgment;
	}

	// Context is PostTweet Original One
	/**
	 * @author axelor-APAR Call from PostTweet
	 * @param currentUser
	 * @param tweetIdOnWhichReplayNeedToMake
	 * 
	 * @return acknowledgement
	 */
	@Transactional
	public String orgGetTweetReplay(User user, String tweetId) {
		@SuppressWarnings("rawtypes")
		ArrayList<HashMap> lstComments = new ArrayList<HashMap>();

		PostTweet context = PostTweet.all().filter("acknowledgment=?", tweetId)
				.fetchOne();
		SocialNetworking sn = SocialNetworking.all()
				.filter("name=?", "twitter").fetchOne();
		List<ApplicationCredentials> query = ApplicationCredentials.all()
				.filter("snType=?", sn).fetch();
		List<PersonalCredential> credential = PersonalCredential.all()
				.filter("userId=? and snType=?", user, sn).fetch();
		try {
			if (query.isEmpty())
				acknowledgment = "0";
			else {
				apiKey = query.get(0).getApikey();
				apiSecret = query.get(0).getApisecret();
				userToken = credential.get(0).getUserToken();
				userTokenSecret = credential.get(0).getUserTokenSecret();

				lstComments = twtconnect.getCommentsOfTweet(apiKey, apiSecret,
						userToken, userTokenSecret, tweetId);
				
				if (!lstComments.isEmpty()) {
					List<TweetComment> query1 = TweetComment.all()
							.filter("curUser=?", user).fetch();
					List<String> str = new ArrayList<String>();
					for (int i = 0; i < query1.size(); i++)
						str.add(query1.get(i).getTweetcommentid());

					// FOR LOOP USED TO CONVERT RESPONSE FROM ARRYALIST TO ONE
					// ROW OF D/B AND PERSIST INTO IT
					for (int i = 0; i < lstComments.size(); i++) {
						mapRetriveValues = lstComments.get(i);
						if (!str.contains(mapRetriveValues.get("commentId")
								.toString())) {
							ImportContact impcnt = ImportContact
									.all()
									.filter("snUserId=? and curUser=? and snType=?",
											mapRetriveValues.get("userid"),
											user, sn).fetchOne();
							TweetComment tcmnt = new TweetComment();
							tcmnt.setTweetcommentid(mapRetriveValues.get(
									"commentId").toString());
							tcmnt.setContentid(context);
							tcmnt.setTweetFrom(impcnt);
							tcmnt.setTweetContent(mapRetriveValues.get(
									"content").toString());
							tcmnt.setTweetTime(mapRetriveValues.get("time")
									.toString());
							tcmnt.setTweetFevourite(mapRetriveValues.get("fav")
									.toString());
							tcmnt.setCurUser(user);
							tcmnt.merge();
						} else
							acknowledgment = "1";
					}
				} else
					acknowledgment = "1";

			}
		} catch (javax.persistence.OptimisticLockException ole) {
			System.out.println(ole.getEntity());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * @author axelor-APAR It will call from Sale-Order and used to Post its
	 *         date as Tweet
	 * @param com
	 *            .axelor.auth.db.User user
	 * @param String
	 *            contentId
	 * @param String
	 *            content
	 * @return
	 */

	@Transactional
	public String postTweetReplay(User user, String contentId, String content) {
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name=?", "twitter").fetchOne();
			List<ApplicationCredentials> query = ApplicationCredentials.all()
					.filter("snType=? ", sn).fetch();
			List<PersonalCredential> credential = PersonalCredential.all()
					.filter("userId=? and snType=?", user, sn).fetch();
			if (credential.isEmpty())
				acknowledgment = "Please Login First";
			else {
				apiKey = query.get(0).getApikey();
				apiSecret = query.get(0).getApisecret();
				userToken = credential.get(0).getUserToken();
				userTokenSecret = credential.get(0).getUserTokenSecret();
				String replayTo = credential.get(0).getSnUserName() + content;
				mapRetriveValues = twtconnect.postTweetReplay(apiKey,
						apiSecret, userToken, userTokenSecret, contentId,
						replayTo);
				if (!mapRetriveValues.containsKey("acknowledgment"))
					acknowledgment = "There is Some Problem";

			}
		} catch (Exception e) {
			acknowledgment = e.getMessage();
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * @author axelor-APAR it will used to Delete all the detail from Database
	 * @param currentUser
	 * @param idValOfPersonalCredential
	 * @return acknowledgement
	 */
	@Transactional
	public String removeAllDetail(User user, long idVal) {
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name=?", "twitter").fetchOne();
			PersonalCredential credential = PersonalCredential.all()
					.filter("id=? and userId=? and snType=?", idVal, user, sn)
					.fetchOne();

			TweetComment.all().filter("curUser=?", user).remove();
			TwitterConfig.all().filter("curUser=?", user).remove();
			DirectMessage.all().filter("curUser=?", user).remove();
			TwitterHomeTimeline.all().filter("curUser=?", user).remove();
			TwitterFollowerRequest.all().filter("curUser=?", user).remove();
			ImportContact.all().filter("snType=? and curUser=?", sn, user)
					.remove();
			TwitterInbox.all().filter("curUser=?", user).remove();
			PostTweet.all().filter("curUser=?", user).remove();
			credential.remove();
			acknowledgment = "You have successfully Removed all associated Data with this account From here";
		}

		catch (Exception e) {
			acknowledgment = e.getMessage();
			e.printStackTrace();
		}
		return acknowledgment;
	}

	// ADDED BY MIHIR ON 16-04-2013 from HERE on to END
	@Transactional
	public PostTweet addTweet(String content, User user, String ack)
			throws Exception {

		DateTime date = twtconnect.getStatusTime(ack);
		PostTweet tweet = new PostTweet();
		tweet.setAcknowledgment(ack);
		tweet.setContent(content);
		tweet.setCurUser(user);
		tweet.setPostedTime(date);
		tweet.persist();
		return tweet;
	}

	public List<TweetComment> fetchTweetsReply(PostTweet postTweet) {
		List<TweetComment> lstTweetComment = TweetComment.all()
				.filter("contentid=?", postTweet).fetch();
		return lstTweetComment;
	}

}
