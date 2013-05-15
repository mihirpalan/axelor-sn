package com.axelor.sn.service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import com.axelor.auth.db.User;
import com.axelor.sn.db.ApplicationCredentials;
import com.axelor.sn.db.Comment;
import com.axelor.sn.db.FBFriendrequest;
import com.axelor.sn.db.ConfigParameter;
import com.axelor.sn.db.FBInbox;
import com.axelor.sn.db.FBNewsFeed;
import com.axelor.sn.db.FBPageComment;
import com.axelor.sn.db.FBPagePost;
import com.axelor.sn.db.FBPages;
import com.axelor.sn.db.FacebookNotification;
import com.axelor.sn.db.ImportContact;
import com.axelor.sn.db.PersonalCredential;
import com.axelor.sn.db.PostEvent;
import com.axelor.sn.db.PostMessage;
import com.axelor.sn.db.SearchPerson;
import com.axelor.sn.db.SearchResult;
import com.axelor.sn.db.SocialNetworking;
import com.axelor.sn.fb.FacebookConnectionClass;
import com.google.common.base.Stopwatch;
import com.google.inject.persist.Transactional;
import com.google.inject.Inject;

/**
 * 
 * @author axelor-APAR
 * 
 */
public class SNFBService {

	@Inject
	FacebookConnectionClass fbconnect;

	String acknowledgment = "";
	String apikey, apisecret, userToken, redirectUrl;
	@SuppressWarnings("rawtypes")
	ArrayList<HashMap> lstReturnResponse;
	@SuppressWarnings("rawtypes")
	HashMap mapReturnValues = new HashMap();

	/**
	 * 
	 * @param user
	 * @param sn
	 * @return
	 */
	public String obtainAuthUrl(User user, SocialNetworking sn) {
		ApplicationCredentials query = ApplicationCredentials.all()
				.filter("snType = ?", sn).fetchOne();
		PersonalCredential credential = PersonalCredential.all()
				.filter("userId = ? and snType = ?", user, sn).fetchOne();
		if (query.getApikey().isEmpty() && query.getApisecret().isEmpty())
			acknowledgment = "0";

		else {
			if (credential != null)
				acknowledgment = "1";

			else {
				apikey = query.getApikey();
				apisecret = query.getApisecret();
				redirectUrl = query.getRedirectUrl();
				if (redirectUrl.isEmpty())
					redirectUrl = "http://127.0.0.1:8080/axelor-demo-sn/snapp/100";

				acknowledgment = fbconnect.getAuthorizationURL(apikey,
						apisecret, redirectUrl);

			}
		}
		return acknowledgment;
	}

	/**
	 * Verifies current user and Call to Custom-API Class which will Used to
	 * Store Generated Token to The Database for Later use
	 * 
	 * @param user
	 * @param code
	 * @return
	 */
	@Transactional
	public String storingToken(User user, String code) {
		SocialNetworking sn = SocialNetworking.all()
				.filter("name = ?", "facebook").fetchOne();
		ApplicationCredentials query = ApplicationCredentials.all()
				.filter("snType = ?", sn).fetchOne();
		PersonalCredential credential = PersonalCredential.all()
				.filter("userId = ? and snType = ?", user, sn).fetchOne();

		if (query.getApikey().isEmpty() && query.getApisecret().isEmpty())
			acknowledgment = "Sorry You can't Do anyting Admin Doesnt set Application Credentials ";

		else {
			if (credential != null)
				acknowledgment = "You Already Have One Account Associated.";

			else {
				apikey = query.getApikey();
				apisecret = query.getApisecret();
				redirectUrl = query.getRedirectUrl();
				if (redirectUrl.isEmpty())
					redirectUrl = "http://127.0.0.1:8080/axelor-demo-sn/snapp/100";

				mapReturnValues = fbconnect.getAccessToken(apikey, apisecret,
						code, redirectUrl);
				if (!mapReturnValues.isEmpty()) {

					PersonalCredential personalCredential = new PersonalCredential();
					personalCredential.setUserId(user);
					personalCredential.setUserToken(mapReturnValues
							.get("token").toString());
					personalCredential.setSnUserName(mapReturnValues.get(
							"snUserName").toString());
					personalCredential.setSnType(sn);
					personalCredential.persist();
					importContactsFB(user);
					acknowledgment = "1";
				}
			}
		}
		return acknowledgment;
	}

	/**
	 * It will Gives Social-Networking Object for Facebook
	 * 
	 * @return
	 */
	public SocialNetworking sayType() {
		SocialNetworking snType = SocialNetworking.all()
				.filter("name = ?", "facebook").fetchOne();
		return snType;
	}

	/**
	 * Verifies current user and Call to Custom-API Class which will provide
	 * search functionality of facebook and stored Search Result into Database
	 * which user can delete anytime from ERP
	 * 
	 * @param user
	 * @param criteria
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	@Transactional
	public String searchPerson(User user, String criteria) {
		boolean checkVal = true;
		ArrayList<HashMap> mapValues = new ArrayList<HashMap>();
		String userToken;
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name = ?", "facebook").fetchOne();
			List<PersonalCredential> query = PersonalCredential.all()
					.filter("userId = ? and snType = ?", user, sn).fetch();
			if (query.isEmpty())
				acknowledgment = "0";

			else {
				SearchResult result = new SearchResult();
				SearchPerson searchKeyword = new SearchPerson();
				userToken = query.get(0).getUserToken();
				mapValues = fbconnect.fetchObjectOfPerson(criteria, userToken);
				SearchPerson objPerson = null;
				if (!mapValues.isEmpty()) {
					mapReturnValues = mapValues.get(0);
					if (mapReturnValues.containsKey("oauthError")) {
						acknowledgment = "1";
						PersonalCredential.all()
								.filter("userId=? and snType=?", user, sn)
								.remove();
					} else {
						try {
							objPerson = SearchPerson
									.all()
									.filter("searchparam = ? and curUser = ?",
											criteria, user).fetchOne();

							if (objPerson == null)
								throw new javax.persistence.NoResultException();
						} catch (javax.persistence.NoResultException e) {
							checkVal = false;
							// EXCEPTION HANDLE WHENE THERE IS NO EXISTING ROW
							// OF CONTACT INTO DB
							// FOR LOOP USED TO CONVERT RESPONSE FROM ARRYALIST
							// TO ONE ROW OF D/B AND PERSIST INTO IT
							for (int i = 0; i < mapValues.size(); i++) {
								mapReturnValues = mapValues.get(i);
								result = new SearchResult();
								searchKeyword.setSearchparam(criteria);
								searchKeyword.setCurUser(user);
								result.setUserid(mapReturnValues
										.get("personId").toString());
								result.setFirstname(mapReturnValues.get(
										"personFirstname").toString());
								result.setLastname(mapReturnValues.get(
										"personLastname").toString());
								result.setGender(mapReturnValues.get(
										"personGender").toString());
								result.setLink(mapReturnValues.get(
										"personProfileLink").toString());
								result.setCurUser(user);
								result.setSearchPerson(searchKeyword);
								result.persist();
							}
						}
						if (checkVal == true) {
							List<SearchResult> objResult = SearchResult
									.all()
									.filter("searchPerson=? and curUser=?",
											objPerson, user).fetch();
							List<String> str = new ArrayList<String>();

							for (int i = 0; i < objResult.size(); i++)
								str.add(objResult.get(i).getUserid());

							// FOR LOOP USED TO CONVERT RESPONSE FROM ARRYALIST
							// TO ONE ROW OF D/B AND PERSIST INTO IT
							for (int i = 0; i < mapValues.size(); i++) {
								if (!str.contains(mapValues.get(i).toString())) {
									mapReturnValues = mapValues.get(i);
									result = new SearchResult();
									searchKeyword.setSearchparam(criteria);
									searchKeyword.setCurUser(user);
									result.setUserid(mapReturnValues.get(
											"personId").toString());
									result.setFirstname(mapReturnValues.get(
											"personFirstname").toString());
									result.setLastname(mapReturnValues.get(
											"personLastname").toString());
									result.setGender(mapReturnValues.get(
											"personGender").toString());
									result.setLink(mapReturnValues.get(
											"personProfileLink").toString());
									result.setCurUser(user);
									result.setSearchPerson(searchKeyword);
									result.persist();
								}
							}
						}
					}
				} else
					acknowledgment = "2";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * Verifies current user and Call to Custom-API Class for updating Status
	 * onto Facebook
	 * 
	 * @param user
	 * @param pmsg
	 * @param paramPrivacy
	 * @return
	 */
	@Transactional
	public String postStatus(User user, String pmsg, String paramPrivacy) {
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name = ?", "facebook").fetchOne();
			List<PersonalCredential> query = PersonalCredential.all()
					.filter("userId = ? and snType = ?", user, sn).fetch();
			if (query.isEmpty())
				acknowledgment = "0";
			else {
				userToken = query.get(0).getUserToken();
				mapReturnValues = fbconnect.publishMessage(pmsg, userToken,
						paramPrivacy);
				if (mapReturnValues.containsKey("oauthError")) {
					acknowledgment = "1";
					PersonalCredential.all()
							.filter("userId = ? and snType = ?", user, sn)
							.remove();
				} else {
					if (mapReturnValues.size() > 0)
						acknowledgment = mapReturnValues.get("acknowledgment")
								.toString();
				}
			}
		} catch (Exception e) {
			acknowledgment = e.getMessage();
		}
		return acknowledgment;
	}

	/**
	 * Varifies User and Call to Custom-API Class for posting Comment on Passed
	 * Content's Id
	 * 
	 * @param user
	 * @param contentId
	 * @param commentContent
	 * @return
	 */
	@Transactional
	public String postCommmentonStatus(User user, String contentId,
			String commentContent) {

		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name=?", "facebook").fetchOne();
			List<PersonalCredential> query = PersonalCredential.all()
					.filter("userId=? and snType=?", user, sn).fetch();
			if (query.isEmpty())
				acknowledgment = "Please Authorise The Applicaton First";
			else {

				userToken = query.get(0).getUserToken();
				mapReturnValues = fbconnect.postStatusComemnt(userToken,
						contentId, commentContent);
				if (mapReturnValues.containsKey("oauthError")) {
					acknowledgment = "You need to Re-Authorise The Application Please go to Personal Credential";
					PersonalCredential.all()
							.filter("userId=? and snType=?", user, sn).remove();
				} else {
					if (mapReturnValues.size() > 0)
						acknowledgment = "Comment Posted Successfully";
					else
						acknowledgment = "Some problem is there";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * Verifies current user and Call to Custom-API Class and It will retrieve
	 * Comment made upon particular Status/Post
	 * 
	 * @param user
	 * @param contentId
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Transactional
	public ArrayList getCommentsFB(User user, String contentId) {
		ArrayList<HashMap> lstReturnResponse = new ArrayList<HashMap>();
		HashMap mapRetrivedComments;
		ArrayList lstSelectedValue = new ArrayList();
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name = ?", "facebook").fetchOne();
			List<PersonalCredential> query = PersonalCredential.all()
					.filter("userId = ? and snType = ?", user, sn).fetch();
			PostMessage status = PostMessage.all()
					.filter("acknowledgment = ?", contentId).fetchOne();
			if (query.isEmpty())
				acknowledgment = "Please Authorise The Applicaton First";
			else {
				userToken = query.get(0).getUserToken();
				lstReturnResponse = fbconnect.getComments(userToken, contentId);
				if (!lstReturnResponse.isEmpty()) {

					mapRetrivedComments = lstReturnResponse.get(0);

					if (mapRetrivedComments.containsKey("oauthError")) {
						acknowledgment = "You need to Re-Authorise The Application Please go to Personal Credential";
						PersonalCredential.all()
								.filter("userId=? and snType=?", user, sn)
								.remove();
					}

					else {

						Comment comnt;
						List<Comment> query1 = Comment
								.all()
								.filter("curUser = ? and contentid = ?", user,
										status).fetch();
						List<String> str = new ArrayList<String>();
						for (int i = 0; i < query1.size(); i++)
							str.add(query1.get(i).getCommentid());

						// FOR LOOP USED TO CONVERT RESPONSE FROM ARRYALIST TO
						// ONE ROW OF D/B AND PERSIST INTO IT
						for (int i = 0; i < lstReturnResponse.size(); i++) {
							mapRetrivedComments = lstReturnResponse.get(i);
							if (!str.contains(mapRetrivedComments.get(
									"commentId").toString())) {
								comnt = new Comment();
								ImportContact impcnt = ImportContact
										.all()
										.filter("snUserId = ? and curUser = ?",
												mapRetrivedComments.get(
														"commentFrom")
														.toString(), user)
										.fetchOne();
								comnt.setCommentid(mapRetrivedComments.get(
										"commentId").toString());
								comnt.setContentid(status);
								comnt.setFrom_user(impcnt);
								comnt.setComment(mapRetrivedComments.get(
										"commentContent").toString());
								comnt.setCommentTime(mapRetrivedComments.get(
										"commentTime").toString());
								comnt.setCommentLikes(mapRetrivedComments.get(
										"commentLike").toString());
								comnt.setCurUser(user);
								comnt.merge();
							}
						}
						List<Comment> selectedComment = Comment
								.all()
								.filter("contentid = ? and curUser = ?",
										status, user).fetch();

						for (int indexVal = 0; indexVal < selectedComment
								.size(); indexVal++) {
							lstSelectedValue.add(selectedComment.get(indexVal)
									.getFrom_user().getName());
							lstSelectedValue.add(selectedComment.get(indexVal)
									.getComment());
							lstSelectedValue.add(selectedComment.get(indexVal)
									.getCommentTime());
						}
					}
				} else {
					acknowledgment = "No comment(s) is there to fetch";
					lstSelectedValue.add(acknowledgment);
				}
			}
		} catch (javax.persistence.OptimisticLockException ole) {
			System.out.println(ole.getEntity());
			ole.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lstSelectedValue;
	}

	/**
	 * Pass all Facebook id(s) to custom API class to Delete
	 * 
	 * @param user
	 * @param contentId
	 * @return
	 */
	@Transactional
	public String getDeleteMessage(User user,
			@SuppressWarnings("rawtypes") List contentId) {
		PostMessage postMsg;
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name = ?", "facebook").fetchOne();
			List<PersonalCredential> query = PersonalCredential.all()
					.filter("userId = ? and snType = ?", user, sn).fetch();
			if (query.isEmpty())
				acknowledgment = "Please Authorise The Applicaton First";

			else {
				userToken = query.get(0).getUserToken();
				for (int i = 0; i < contentId.size(); i++) {
					postMsg = new PostMessage();
					postMsg = PostMessage.all()
							.filter("id = ?", contentId.get(i)).fetchOne();
					if (!postMsg.getComments().isEmpty()) {
						List<Comment> cmnt = Comment.all()
								.filter("contentid = ?", postMsg).fetch();
						for (int j = 0; j < cmnt.size(); j++)
							cmnt.get(j).remove();
					}

					mapReturnValues = fbconnect.delete(
							postMsg.getAcknowledgment(), userToken);
					if (mapReturnValues.containsKey("oauthError")) {
						acknowledgment = "You need to Re-Authorise The Application Please go to Personal Credential";
						PersonalCredential.all()
								.filter("userId=? and snType=?", user, sn)
								.remove();
					} else if (mapReturnValues.containsKey("status"))
						postMsg.remove();

					else
						acknowledgment = "Some Problem is there Please Try Again Later";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * Verifies current user and Call to Custom-API Class which will used to
	 * Post Status
	 * 
	 * @param user
	 * @param pmsg
	 * @param paramPrivacy
	 * @return
	 */
	@Transactional
	public String postStatusFromOther(User user, String pmsg,
			String paramPrivacy) {
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name = ?", "facebook").fetchOne();
			List<PersonalCredential> query = PersonalCredential.all()
					.filter("userId = ? and snType = ?", user, sn).fetch();
			if (query.isEmpty())
				acknowledgment = "Please Authorise The Applicaton First";
			else {
				userToken = query.get(0).getUserToken();
				mapReturnValues = fbconnect.publishMessage(pmsg, userToken,
						paramPrivacy);
				if (mapReturnValues.containsKey("oauthError")) {
					acknowledgment = "You need to Re-Authorise The Application Please go to Personal Credential";
					PersonalCredential.all()
							.filter("userId=? and snType=?", user, sn).remove();
				} else if (mapReturnValues.containsKey("acknowledgment")) {
					PostMessage posted = new PostMessage();
					posted.setContent(pmsg);
					posted.setAcknowledgment(mapReturnValues.get(
							"acknowledgment").toString());
					posted.setCurUser(user);
					posted.setPrivacy(paramPrivacy);
					org.joda.time.DateTime timeNow = new org.joda.time.DateTime();
					posted.setPostedTime(timeNow);
					posted.persist();
					acknowledgment = mapReturnValues.get("acknowledgment")
							.toString();
				} else
					acknowledgment = "Some Problem is there";
			}
		} catch (Exception e) {
			acknowledgment = e.getMessage();
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * 
	 * Verifies current user and Call to Custom-API Class which Used to Post
	 * Event onto Facebook
	 * 
	 * @param user
	 * @param startDate
	 * @param endDate
	 * @param eventName
	 * @param location
	 * @param privacy
	 * @return
	 */

	@Transactional
	public String fbPostEvent(User user, Date startDate, Date endDate,
			String eventName, String location, String privacy) {
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name = ?", "facebook").fetchOne();
			List<PersonalCredential> query = PersonalCredential.all()
					.filter("userId = ? and snType = ?", user, sn).fetch();
			if (query.isEmpty())
				acknowledgment = "0";
			else {
				userToken = query.get(0).getUserToken();
				mapReturnValues = fbconnect.publishEvent(startDate, endDate,
						eventName, location, userToken, privacy);
				if (mapReturnValues.containsKey("oauthError")) {
					acknowledgment = "1";
					PersonalCredential.all()
							.filter("userId=? and snType=?", user, sn).remove();
				} else {
					if (mapReturnValues.size() > 0)
						acknowledgment = mapReturnValues.get("acknowledgment")
								.toString();
				}
			}
		} catch (Exception e) {
			acknowledgment = e.getMessage();
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * Pass all Facebook id's to custom API class to Delete
	 * 
	 * @param user
	 * @param contentId
	 * @return
	 */
	@Transactional
	public String getDeleteEvent(User user,
			@SuppressWarnings("rawtypes") List contentId) {
		PostEvent postEvent;
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name = ?", "facebook").fetchOne();
			List<PersonalCredential> query = PersonalCredential.all()
					.filter("userId = ? and snType = ?", user, sn).fetch();
			if (query.isEmpty())
				acknowledgment = "Please Authorise The Applicaton First";

			else {
				userToken = query.get(0).getUserToken();
				for (int i = 0; i < contentId.size(); i++) {
					postEvent = new PostEvent();
					postEvent = PostEvent.all()
							.filter("id = ?", contentId.get(i)).fetchOne();
					mapReturnValues = fbconnect.delete(
							postEvent.getAcknowledgment(), userToken);
					if (mapReturnValues.containsKey("oauthError")) {
						acknowledgment = "You need to Re-Authorise The Application Please go to Personal Credential";
						PersonalCredential.all()
								.filter("userId=? and snType=?", user, sn)
								.remove();
					} else if (mapReturnValues.containsKey("status"))
						postEvent.remove();
					else
						acknowledgment = "Some Problem is there Please Try Again Later";
				}
			}
		} catch (Exception e) {
			acknowledgment = e.getMessage();
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * Verifies current user and Call to Custom-API Class and It will used to
	 * Import Contacts of Facebook to ERP and saved to Database
	 * 
	 * @param user
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	@Transactional
	public String importContactsFB(User user) {
		Stopwatch stopwatch = new Stopwatch();
		ArrayList<HashMap> lstReturnResponse = new ArrayList<HashMap>();
		HashMap mapFriendData;
		String userToken;
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name = ?", "facebook").fetchOne();
			List<PersonalCredential> query = PersonalCredential.all()
					.filter("userId = ? and snType = ?", user, sn).fetch();
			if (query.isEmpty())
				acknowledgment = "Please Authorise The Applicaton First";

			else {
				userToken = query.get(0).getUserToken();
				ImportContact fbcntc;
				List<ImportContact> query1 = ImportContact.all()
						.filter("curUser = ? and snType = ?", user, sn).fetch();
				lstReturnResponse = fbconnect.getListOfFriends(userToken);
				stopwatch.start();

				if (!lstReturnResponse.isEmpty()) {

					mapFriendData = lstReturnResponse.get(0);

					if (mapFriendData.containsKey("oauthError")) {
						acknowledgment = "You need to Re-Authorise The Application Please go to Personal Credential";
						PersonalCredential.all()
								.filter("userId=? and snType=?", user, sn)
								.remove();
					}

					else {

						List<String> str = new ArrayList<String>();
						for (int i = 0; i < query1.size(); i++)
							str.add(query1.get(i).getSnUserId());

						// FOR LOOP USED TO CONVERT RESPONSE FROM ARRYALIST TO
						// ONE ROW OF D/B AND PERSIST INTO IT
						for (int p = 0; p < lstReturnResponse.size(); p++) {
							mapFriendData = lstReturnResponse.get(p);
							if (!str.contains(mapFriendData.get("facebookId")
									.toString())) {
								fbcntc = new ImportContact();
								fbcntc.setSnUserId(mapFriendData.get(
										"facebookId").toString());
								fbcntc.setName(mapFriendData
										.get("facebookName").toString());
								fbcntc.setLink(mapFriendData
										.get("facebookLink").toString());
								fbcntc.setSnType(sn);
								fbcntc.setCurUser(user);
								fbcntc.persist();
							}
						}
					}
				} else
					acknowledgment = "Please Try Later, There is Some Problem";

				stopwatch.stop();
				System.out.println("Database Part Completed in "
						+ stopwatch.elapsedTime(TimeUnit.SECONDS) + "Seconds");
			}
		} catch (Exception e) {
			acknowledgment = e.toString();
			e.printStackTrace();
		} finally {
			System.gc();
		}
		return acknowledgment;
	}

	/**
	 * Verifies current user and Call to Custom-API Class for retriving Facebook
	 * - inbox
	 * 
	 * @param user
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	@Transactional
	public String getInbox(User user) {

		ArrayList<HashMap> lstReturnResponse = new ArrayList<HashMap>();
		HashMap mapInbox;
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name = ?", "facebook").fetchOne();
			List<PersonalCredential> query = PersonalCredential.all()
					.filter("userId = ? and snType = ?", user, sn).fetch();
			if (query.isEmpty()) {
				acknowledgment = "Please Authorise The Applicaton First";
			} else {
				userToken = query.get(0).getUserToken();
				FBInbox fibox;
				lstReturnResponse = fbconnect.retriveMessage(userToken);

				if (!lstReturnResponse.isEmpty()) {
					mapInbox = lstReturnResponse.get(0);
					if (mapInbox.containsKey("oauthError")) {
						acknowledgment = "You need to Re-Authorise The Application Please go to Personal Credential";
						PersonalCredential.all()
								.filter("userId=? and snType=?", user, sn)
								.remove();
					} else {

						List<FBInbox> query1 = FBInbox.all()
								.filter("curUser = ?", user).fetch();
						for (int i = 0; i < query1.size(); i++)
							query1.get(i).remove();

						// FOR LOOP USED TO CONVERT RESPONSE FROM ARRYALIST TO
						// ONE ROW OF D/B AND PERSIST INTO IT
						for (int i = 0; i < lstReturnResponse.size(); i++) {
							mapInbox = lstReturnResponse.get(i);
							fibox = new FBInbox();
							fibox.setMsgId(mapInbox.get("messageId").toString());
							fibox.setFromName(mapInbox.get("messageFrom")
									.toString());
							fibox.setMsgBody(mapInbox.get("messageContent")
									.toString());
							fibox.setSendDate(mapInbox.get("sentTime")
									.toString());
							fibox.setCurUser(user);
							fibox.persist();
						}
					}
				} else {
					acknowledgment = "No New Message it there or Some Problem Occure!,Please Try Later";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * It will call to Custom-API after verifying current user and pass the
	 * content id to it for deletion
	 * 
	 * @param user
	 * @param contentId
	 * @return
	 */
	@Transactional
	public String deleteInBox(User user,
			@SuppressWarnings("rawtypes") List contentId) {
		FBInbox inboxMsg;
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name=?", "facebook").fetchOne();
			List<PersonalCredential> query = PersonalCredential.all()
					.filter("userId = ? and snType = ?", user, sn).fetch();
			if (query.isEmpty())
				acknowledgment = "Please Authorise The Applicaton First";

			else {
				userToken = query.get(0).getUserToken();
				for (int i = 0; i < contentId.size(); i++) {
					inboxMsg = new FBInbox();
					inboxMsg = FBInbox.all().filter("id=?", contentId.get(i))
							.fetchOne();
					mapReturnValues = fbconnect.delete(inboxMsg.getMsgId(),
							userToken);
					if (mapReturnValues.containsKey("oauthError")) {
						acknowledgment = "You need to Re-Authorise The Application Please go to Personal Credential";
						PersonalCredential.all()
								.filter("userId=? and snType=?", user, sn)
								.remove();
					} else if (mapReturnValues.containsKey("status"))
						inboxMsg.remove();
					else
						acknowledgment = "Some Problem is there Please Try Again Later";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * Verifies current user and Call to Custom-API Class which will retrieve
	 * Facebook Notifications
	 * 
	 * @param user
	 * @return
	 */
	@SuppressWarnings({ "deprecation", "rawtypes" })
	@Transactional
	public String getNotifications(User user) {
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		Date sinceDate = null;
		Date untilDate = null;

		ArrayList<HashMap> lstReturnResponse = new ArrayList<HashMap>();
		HashMap mapNotification;

		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name = ?", "facebook").fetchOne();
			List<PersonalCredential> query = PersonalCredential.all()
					.filter("userId = ? and snType = ?", user, sn).fetch();
			if (query.isEmpty())
				acknowledgment = "Please Authorise The Applicaton First";

			else {
				userToken = query.get(0).getUserToken();
				FacebookNotification fbnotif;
				ConfigParameter param;
				try {
					param = ConfigParameter.all().filter("curUser=?", user)
							.fetchOne();
					if (param == null)
						throw new javax.persistence.NoResultException();

					else {
						if (param.getParamSince() > 0) {
							Calendar calender = Calendar.getInstance();
							calender.add(Calendar.DATE,
									-(param.getParamSince()));
							sinceDate = new Date(sdf.format(calender.getTime()));
						}

						if (param.getParamUntil() > 0) {
							Calendar calender = Calendar.getInstance();
							calender.add(Calendar.DATE,
									-(param.getParamUntil()));
							untilDate = new Date(sdf.format(calender.getTime()));
						}
						lstReturnResponse = fbconnect.getNotification(
								userToken, param.getParamLimit(), sinceDate,
								untilDate);
					}
				} catch (javax.persistence.NoResultException e) {
					lstReturnResponse = fbconnect.getNotification(userToken, 0,
							null, null);
				}

				if (!lstReturnResponse.isEmpty()) {
					mapNotification = lstReturnResponse.get(0);
					if (mapNotification.containsKey("oauthError")) {
						acknowledgment = "You need to Re-Authorise The Application Please go to Personal Credential";
						PersonalCredential.all()
								.filter("userId=? and snType=?", user, sn)
								.remove();
					} else {
						List<FacebookNotification> query1 = FacebookNotification
								.all().filter("curUser = ?", user).fetch();
						for (int i = 0; i < query1.size(); i++)
							query1.get(i).remove();

						// FOR LOOP USED TO CONVERT RESPONSE FROM ARRYALIST TO
						// ONE ROW OF D/B AND PERSIST INTO IT
						for (int i = 0; i < lstReturnResponse.size(); i++) {
							mapNotification = lstReturnResponse.get(i);
							fbnotif = new FacebookNotification();
							fbnotif.setNotifId(mapNotification.get("notifId")
									.toString());
							fbnotif.setTitle(mapNotification.get("notifTitle")
									.toString());
							fbnotif.setLink(mapNotification.get("notifLink")
									.toString());
							fbnotif.setUpdateTime(mapNotification.get(
									"updateTime").toString());
							fbnotif.setCurUser(user);
							fbnotif.persist();
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * Verifies the User and call to Custom-API class
	 * 
	 * @param user
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Transactional
	public String getFriendRequest(User user) {
		lstReturnResponse = new ArrayList();
		HashMap mapRequestedFriend;
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name = ?", "facebook").fetchOne();
			List<PersonalCredential> query = PersonalCredential.all()
					.filter("userId = ? and snType = ?", user, sn).fetch();
			if (query.isEmpty())
				acknowledgment = "Please Authorise The Applicaton First";
			else {
				userToken = query.get(0).getUserToken();
				FBFriendrequest fbreq;
				lstReturnResponse = fbconnect.getFriendRequest(userToken);
				if (!lstReturnResponse.isEmpty()) {
					mapRequestedFriend = lstReturnResponse.get(0);
					if (mapRequestedFriend.containsKey("oauthError")) {
						acknowledgment = "You need to Re-Authorise The Application Please go to Personal Credential";
						PersonalCredential.all()
								.filter("userId=? and snType=?", user, sn)
								.remove();
					} else {
						List<FBFriendrequest> query1 = FBFriendrequest.all()
								.filter("curUser = ?", user).fetch();
						for (int i = 0; i < query1.size(); i++)
							query1.get(i).remove();

						for (int i = 0; i < lstReturnResponse.size(); i++) {
							mapRequestedFriend = lstReturnResponse.get(i);
							fbreq = new FBFriendrequest();
							fbreq.setLink(mapRequestedFriend.get("friendLink")
									.toString());
							fbreq.setName(mapRequestedFriend.get("friendName")
									.toString());
							fbreq.setGender(mapRequestedFriend.get(
									"friendGender").toString());
							fbreq.setCurUser(user);
							fbreq.persist();
						}
					}
				} else
					acknowledgment = "You do not have Pending Friend Request(s)";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * It will verify the User and call to Custom-API class for Fetching
	 * News-Feed
	 * 
	 * @param user
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "deprecation" })
	@Transactional
	public String getNewsFeeds(User user) {
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		Date sinceDate = null;
		Date untilDate = null;
		lstReturnResponse = new ArrayList<HashMap>();
		HashMap mapRetrivedFeeds = new HashMap();
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name = ?", "facebook").fetchOne();
			List<PersonalCredential> query = PersonalCredential.all()
					.filter("userId = ? and snType = ?", user, sn).fetch();
			if (query.isEmpty())
				acknowledgment = "Please Authorise The Applicaton First";

			else {
				userToken = query.get(0).getUserToken();
				FBNewsFeed newsFeed;
				ConfigParameter param;
				try {
					param = ConfigParameter.all().filter("curUser = ?", user)
							.fetchOne();
					if (param == null)
						throw new javax.persistence.NoResultException();

					else {
						if (param.getParamSince() > 0) {
							Calendar calender = Calendar.getInstance();
							calender.add(Calendar.DATE,
									-(param.getParamSince()));
							sinceDate = new Date(sdf.format(calender.getTime()));
						}

						if (param.getParamUntil() > 0) {
							Calendar calender = Calendar.getInstance();
							calender.add(Calendar.DATE,
									-(param.getParamUntil()));
							untilDate = new Date(sdf.format(calender.getTime()));
						}
						lstReturnResponse = fbconnect.getNewsFeed(userToken,
								param.getParamLimit(), sinceDate, untilDate);
					}
				} catch (javax.persistence.NoResultException e) {
					lstReturnResponse = fbconnect.getNewsFeed(userToken, 0,
							null, null);
				}

				if (!lstReturnResponse.isEmpty()) {
					mapRetrivedFeeds = lstReturnResponse.get(0);
					if (mapRetrivedFeeds.containsKey("oauthError")) {
						acknowledgment = "You need to Re-Authorise The Application Please go to Personal Credential";
						PersonalCredential.all()
								.filter("userId=? and snType=?", user, sn)
								.remove();
					} else {
						List<FBNewsFeed> query1 = FBNewsFeed.all()
								.filter("curUser = ?", user).fetch();
						if (query1.size() > 0)
							for (int i = 0; i < query1.size(); i++)
								query1.get(i).remove();

						// FOR LOOP USED TO CONVERT RESPONSE FROM ARRYALIST TO
						// ONE ROW OF D/B AND PERSIST INTO IT
						System.out.println(lstReturnResponse);
						for (int i = 0; i < lstReturnResponse.size(); i++) {

							mapRetrivedFeeds = lstReturnResponse.get(i);
							newsFeed = new FBNewsFeed();
							newsFeed.setFeedid(mapRetrivedFeeds.get("feedId")
									.toString());
							newsFeed.setName(mapRetrivedFeeds.get(
									"feedUpdateFrom").toString());
							newsFeed.setContentdate(mapRetrivedFeeds.get(
									"feedCreationTime").toString());
							newsFeed.setType(mapRetrivedFeeds.get("feedType")
									.toString());

							if (mapRetrivedFeeds.get("feedLink") != null)
								newsFeed.setLink(mapRetrivedFeeds.get(
										"feedLink").toString());
							else
								newsFeed.setLink("Not Available");

							if (mapRetrivedFeeds.get("feedMessage") != null)
								newsFeed.setMessage(mapRetrivedFeeds.get(
										"feedMessage").toString());
							else
								newsFeed.setMessage("Not Available");
							// newsFeed.setMessage(mapRetrivedFeeds.get("feedMessage").toString());
							newsFeed.setCurUser(user);
							newsFeed.persist();
						}
					}
				} else
					acknowledgment = "There is No new Update / Some Problem";
			}
		} catch (Exception e) {

			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * It will verify the Current User and Call to custom-API to retrieve
	 * Associated Pages
	 * 
	 * @param user
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	@Transactional
	public String getPageFB(User user) {

		lstReturnResponse = new ArrayList<HashMap>();
		HashMap mapRetrivePage;
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name = ?", "facebook").fetchOne();
			List<PersonalCredential> query = PersonalCredential.all()
					.filter("userId = ? and snType = ?", user, sn).fetch();
			if (query.isEmpty())
				acknowledgment = "Please Authorise The Applicaton First";

			else {
				userToken = query.get(0).getUserToken();
				FBPages page;
				lstReturnResponse = fbconnect.getPageDetail(userToken);
				if (!lstReturnResponse.isEmpty()) {
					mapRetrivePage = lstReturnResponse.get(0);
					if (mapRetrivePage.containsKey("oauthError")) {
						acknowledgment = "You need to Re-Authorise The Application Please go to Personal Credential";
						PersonalCredential.all()
								.filter("userId=? and snType=?", user, sn)
								.remove();
					} else {
						List<FBPages> query1 = FBPages.all()
								.filter("curUser = ?", user).fetch();
						List<String> str = new ArrayList<String>();
						for (int i = 0; i < query1.size(); i++)
							str.add(query1.get(i).getPageId());

						// FOR LOOP USED TO CONVERT RESPONSE FROM ARRYALIST TO
						// ONE ROW OF D/B AND MERGE INTO IT
						for (int i = 0; i < lstReturnResponse.size(); i++) {
							mapRetrivePage = lstReturnResponse.get(i);
							if (!str.contains(mapRetrivePage.get("pageId")
									.toString())) {
								page = new FBPages();
								page.setPageId(mapRetrivePage.get("pageId")
										.toString());
								page.setName(mapRetrivePage.get("pageName")
										.toString());
								page.setPageUrl(mapRetrivePage.get("pageUrl")
										.toString());
								page.setUsername(mapRetrivePage.get(
										"pageUsername").toString());
								page.setCurUser(user);
								page.persist();
							}
						}
					}
				} else
					acknowledgment = "You do not OWN page(s)";
			}
		} catch (Exception e) {
			acknowledgment = e.getMessage();
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * It will used to Post Content to Page by Verify User
	 * 
	 * @param user
	 * @param page
	 * @param content
	 * @return
	 */
	@Transactional
	public String postPageContent(User user, FBPages page, FBPagePost content) {
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name=?", "facebook").fetchOne();
			List<PersonalCredential> query = PersonalCredential.all()
					.filter("userId = ? and snType = ?", user, sn).fetch();
			if (query.isEmpty())
				acknowledgment = "0";
			else {
				userToken = query.get(0).getUserToken();
				String value = content.getContent();
				String pageId = page.getPageId();
				mapReturnValues = fbconnect
						.postToPgae(value, pageId, userToken);
				if (mapReturnValues.containsKey("oauthError")) {
					acknowledgment = "1";
					PersonalCredential.all()
							.filter("userId=? and snType=?", user, sn).remove();
				} else {
					if (mapReturnValues.size() > 0)
						acknowledgment = mapReturnValues.get("acknowledgment")
								.toString();
				}
			}
		} catch (Exception e) {
			acknowledgment = e.getMessage();
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * It will call to Custom-API after verifying current user and pass the
	 * content id to it for deletion
	 * 
	 * @param user
	 * @param contentId
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	@Transactional
	public String deletePagePost(User user, List contentId) {
		FBPagePost postMsg;
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name = ?", "facebook").fetchOne();
			List<PersonalCredential> query = PersonalCredential.all()
					.filter("userId = ? and snType=?", user, sn).fetch();
			if (query.isEmpty())
				acknowledgment = "Please Authorise The Applicaton First";
			else {
				userToken = query.get(0).getUserToken();
				for (int i = 0; i < contentId.size(); i++) {
					postMsg = new FBPagePost();
					postMsg = FBPagePost.all().filter("id=?", contentId.get(i))
							.fetchOne();
					mapReturnValues = fbconnect.delete(
							postMsg.getAcknowledgment(), userToken);

					if (mapReturnValues.containsKey("oauthError")) {
						acknowledgment = "You need to Re-Authorise The Application Please go to Personal Credential";
						PersonalCredential.all()
								.filter("userId=? and snType=?", user, sn)
								.remove();
					} else if (mapReturnValues.containsKey("status"))
						postMsg.remove();
					else
						acknowledgment = "Some Problem is there Please Try Again Later";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * It will verify Current User and Pass the Content to Custom-API for
	 * retrieving comments
	 * 
	 * @param user
	 * @param contentId
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	@Transactional
	public String getPageCommentsFB(User user, String contentId) {
		lstReturnResponse = new ArrayList<HashMap>();
		HashMap mapRetriveComment;

		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name = ?", "facebook").fetchOne();
			List<PersonalCredential> query = PersonalCredential.all()
					.filter("userId = ? and snType = ?", user, sn).fetch();
			FBPagePost status = FBPagePost.all()
					.filter("acknowledgment = ?", contentId).fetchOne();
			if (query.isEmpty())
				acknowledgment = "Please Authorise The Applicaton First";

			else {
				userToken = query.get(0).getUserToken();
				lstReturnResponse = fbconnect.getPageComments(userToken,
						contentId);
				if (!lstReturnResponse.isEmpty()) {
					mapRetriveComment = lstReturnResponse.get(0);
					if (mapRetriveComment.containsKey("oauthError")) {
						acknowledgment = "You need to Re-Authorise The Application Please go to Personal Credential";
						PersonalCredential.all()
								.filter("userId=? and snType=?", user, sn)
								.remove();
					} else {

						FBPageComment comnt;
						List<FBPageComment> query1 = FBPageComment
								.all()
								.filter("curUser = ? and contentid=?", user,
										status).fetch();
						List<String> str = new ArrayList<String>();
						for (int i = 0; i < query1.size(); i++)
							str.add(query1.get(i).getCommentid());

						// FOR LOOP USED TO CONVERT RESPONSE FROM ARRYALIST TO
						// ONE ROW OF D/B AND MERGE INTO IT
						for (int i = 0; i < lstReturnResponse.size(); i++) {
							if (!str.contains(mapRetriveComment
									.get("commentId").toString())) {
								mapRetriveComment = lstReturnResponse.get(0);
								comnt = new FBPageComment();
								comnt.setCommentid(mapRetriveComment.get(
										"commentId").toString());
								comnt.setContentid(status);
								comnt.setFrom_user(mapRetriveComment.get(
										"commentBy").toString());
								comnt.setComment(mapRetriveComment.get(
										"commentContent").toString());
								comnt.setCommentTime(mapRetriveComment.get(
										"commentTime").toString());
								comnt.setCommentLikes(mapRetriveComment.get(
										"commentLike").toString());
								comnt.setCurUser(user);
								comnt.merge();
							}
						}
					}
				} else
					acknowledgment = "No Comment available";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * It will Post Like on Passed ContentId on FB
	 * 
	 * @param user
	 * @param contentId
	 * @return
	 */
	@Transactional
	public String getLike(User user, Long contentId) {
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name = ?", "facebook").fetchOne();
			List<PersonalCredential> query = PersonalCredential.all()
					.filter("userId = ? and snType = ?", user, sn).fetch();
			if (query.isEmpty())
				acknowledgment = "Please Authorise The Applicaton First";

			else {
				userToken = query.get(0).getUserToken();
				FBNewsFeed newsFeed = FBNewsFeed.all()
						.filter("id = ?  and curUser = ?", contentId, user)
						.fetchOne();
				mapReturnValues = fbconnect.postLike(newsFeed.getFeedid(),
						userToken);
				if (mapReturnValues.containsKey("oauthError")) {
					acknowledgment = "You need to Re-Authorise The Application Please go to Personal Credential";
					PersonalCredential.all()
							.filter("userId = ? and snType = ?", user, sn)
							.remove();
				} else {
					boolean status = Boolean.parseBoolean(mapReturnValues.get(
							"status").toString());
					if (status) {
						newsFeed.setContentLike(status);
						newsFeed.merge();
						acknowledgment = "Content has been Liked Successfully!";
					} else
						acknowledgment = "Its not Existed or/There is Some Error Please Try after Sometimes!!";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * it will used to Delete all the detail from Database
	 * 
	 * @param user
	 * @param idVal
	 * @return
	 */
	@Transactional
	public String removeAllDetail(User user, long idVal) {
		try {
			SocialNetworking sn = SocialNetworking.all()
					.filter("name=?", "facebook").fetchOne();
			PersonalCredential credential = PersonalCredential.all()
					.filter("id=? and userId=? and snType=?", idVal, user, sn)
					.fetchOne();
			Comment.all().filter("curUser = ?", user).remove();
			FBPageComment.all().filter("curUser = ?", user).remove();
			FBPagePost.all().filter("curUser=?", user).remove();
			FBPages.all().filter("curUser=?", user).remove();
			FBFriendrequest.all().filter("curUser=?", user).remove();
			FBNewsFeed.all().filter("curUser=?", user).remove();
			SearchResult.all().filter("curUser=?", user).remove();
			SearchPerson.all().filter("curUser=?", user).remove();
			ConfigParameter.all().filter("curUser=?", user).remove();
			FBInbox.all().filter("curUser=?", user).remove();
			FacebookNotification.all().filter("curUser=?", user).remove();
			PostEvent.all().filter("curUser=?", user).remove();
			ImportContact.all().filter("curUser=? and snType=?", user, sn)
					.remove();
			PostMessage.all().filter("curUser=?", user).remove();
			credential.remove();
			acknowledgment = "You have successfully Removed all associated Data with this account From here";
		} catch (Exception e) {
			acknowledgment = e.getMessage();
			e.printStackTrace();
		}
		return acknowledgment;
	}
}
