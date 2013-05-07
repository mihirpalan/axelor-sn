package com.axelor.sn.service;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.sn.db.ApplicationCredentials;
import com.axelor.sn.db.ImportContact;
import com.axelor.sn.db.PersonalCredential;
import com.axelor.sn.db.PostTweet;
import com.axelor.sn.db.SocialNetworking;
import com.axelor.sn.db.TweetComment;
//import com.axelor.sn.db.TweetSearch;
//import com.axelor.sn.db.TweetSearchResult;
import com.axelor.sn.db.TwitterConfig;
import com.axelor.sn.db.TwitterFollowerRequest;
import com.axelor.sn.db.TwitterHomeTimeline;
import com.axelor.sn.db.TwitterInbox;
import com.axelor.sn.twitter.TwitterConnectionClass;
import com.google.inject.persist.Transactional;
import org.joda.time.DateTime;

public class SNTWTService 
{
	TwitterConnectionClass twtconnect;
	String acknowledgment,apiKey,apiSecret,userToken,userTokenSecret;


	public String obtainToken(User user,SocialNetworking sn)
	{
		try
		{

			List<ApplicationCredentials> query=ApplicationCredentials.all().filter("snType=? ",sn).fetch();
			List<PersonalCredential> credential=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			if(query.isEmpty())
			{
				acknowledgment="Sorry You can't Do anyting Admin Doesnt set Application Credentials ";
				throw new javax.validation.ValidationException("No Application Credentials Available");
			}
			else
			{

				if(!credential.isEmpty())
				{
					acknowledgment="You Already Have One Account Associated...";
					throw new javax.validation.ValidationException("You Already Have One Account Associated...");
				}
				else
				{
					for(int i=0;i<query.size();i++)
					{						
						apiKey=query.get(i).getApikey();
						apiSecret=query.get(i).getApisecret();
					}
					twtconnect=new TwitterConnectionClass();
					acknowledgment=twtconnect.getAuthURL(apiKey,apiSecret);
				}
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return acknowledgment;

	}

	@Transactional
	public String storingToken(User user,String code)
	{
		System.out.println("Its Called");
		String[] tempAck=new String[2]; 
		try
		{

			SocialNetworking sn=SocialNetworking.all().filter("name=?","Twitter").fetchOne();
			ApplicationCredentials query=ApplicationCredentials.all().filter("snType=? ", sn).fetchOne();
			twtconnect=new TwitterConnectionClass();
			acknowledgment=twtconnect.getTokens(query.getApikey(),query.getApisecret(),code);
			tempAck=acknowledgment.split(":");
			userToken=tempAck[0];
			userTokenSecret=tempAck[1];
			if(acknowledgment.contains(":"))
			{
				PersonalCredential personalCredential=new PersonalCredential();
				//						personalCredential.setSnUsername(username);
				//						personalCredential.setPassword("");
				personalCredential.setUserId(user);
				personalCredential.setSnType(sn);
				personalCredential.setUserToken(userToken);
				personalCredential.setUserTokenSecret(userTokenSecret);
				personalCredential.persist();
				acknowledgment="Succefully Login";
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return acknowledgment;
	}

	public SocialNetworking saySnType()
	{
		SocialNetworking snType=SocialNetworking.all().filter("name=?", "Twitter").fetchOne();
		return snType;
	}

	/**
	 * 
	 * @param com.axelor.auth.db.User user
	 * @param com.axelor.sn.db.SocialNetworking sn
	 * @param String twitterRegesterEmail
	 * @param String password
	 * it will split response and Store tokens and screenName into Database
	 * @return String 
	 */
	@Transactional
	public String getUserToken(User user,SocialNetworking sn,String username,String password)
	{
		String[] tempAck=new String[2]; 
		try
		{
			List<ApplicationCredentials> query=ApplicationCredentials.all().filter("userId=? and snType=?", user,sn).fetch();
			List<PersonalCredential> credential=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			if(query.isEmpty())
			{
				acknowledgment="Sorry You can't Do anyting Admin Doesnt set Application Credentials ";
				throw new javax.validation.ValidationException("No Application Credentials Available");
			}
			else
			{

				if(!credential.isEmpty())
				{
					acknowledgment="You Already Have One Account Associated...";
					throw new javax.validation.ValidationException("You Already Have One Account Associated...");
				}
				else
				{
					for(int i=0;i<query.size();i++)
					{						
						apiKey=query.get(i).getApikey();
						apiSecret=query.get(i).getApisecret();
					}
					twtconnect=new TwitterConnectionClass();
					acknowledgment=twtconnect.getUserToken(apiKey,apiSecret,username,password);
					tempAck=acknowledgment.split(":");
					userToken=tempAck[0];
					userTokenSecret=tempAck[1];
					if(acknowledgment.contains(":"))
					{
						PersonalCredential personalCredential=new PersonalCredential();
						personalCredential.setSnUserName("@"+tempAck[2]);
						personalCredential.setUserId(user);
						personalCredential.setSnType(sn);
						personalCredential.setUserToken(userToken);
						personalCredential.setUserTokenSecret(userTokenSecret);
						personalCredential.merge();
						acknowledgment="Succefully Login";
					}
				}
			}
		}
		catch(Exception e)
		{
			acknowledgment=e.getMessage();
			e.printStackTrace();
		}
		return acknowledgment;
	}


	/**
	 * 
	 * @param com.axelor.auth.db.User user
	 * @param String contentToTweet
	 * Validate user and Pass content to Custom-API class and on successful post Store details into DataBase
	 * @return String
	 */
	@Transactional
	public String postTweet(User user,String content)
	{
		try
		{
			SocialNetworking sn=SocialNetworking.all().filter("name=?", "Twitter").fetchOne();
			List<ApplicationCredentials> query=ApplicationCredentials.all().filter("userId=? and snType=? ", user,sn).fetch();
			List<PersonalCredential> credential=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();

			if(credential.isEmpty())
			{
				acknowledgment="Please Login First";

			}
			else
			{
				apiKey=query.get(0).getApikey();
				apiSecret=query.get(0).getApisecret();
				userToken=credential.get(0).getUserToken();
				userTokenSecret=credential.get(0).getUserTokenSecret();
				twtconnect=new TwitterConnectionClass();
				acknowledgment=twtconnect.postTweet(apiKey, apiSecret, userToken, userTokenSecret, content);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return acknowledgment;
	}


	/**
	 * 
	 * @param com.axelor.auth.db.User user
	 * @param java.util.List contentId
	 * it will validate user and make Call to custom-API for Deletion
	 * @return String acknowledgement 
	 */
	@SuppressWarnings("rawtypes")
	@Transactional
	public String getDeleteMessage(User user,List contentId)
	{
		PostTweet postMsg;
		try
		{
			SocialNetworking sn=SocialNetworking.all().filter("name=?", "Twitter").fetchOne();
			List<ApplicationCredentials> query=ApplicationCredentials.all().filter("userId=? and snType=? ", user,sn).fetch();
			List<PersonalCredential> credential=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			if(credential.isEmpty())
			{
				acknowledgment="Please Login First";					

			}
			else
			{
				apiKey=query.get(0).getApikey();
				apiSecret=query.get(0).getApisecret();
				userToken=credential.get(0).getUserToken();
				userTokenSecret=credential.get(0).getUserTokenSecret();
				for(int i=0;i<contentId.size();i++)
				{
					postMsg=new PostTweet();
					postMsg=PostTweet.all().filter("id=?", contentId.get(i)).fetchOne();
					if(!postMsg.getCommentsTweet().isEmpty())
					{
						List<TweetComment> cmntList=TweetComment.all().filter("id=?", contentId.get(i)).fetch();
						for(int j=0;j<cmntList.size();j++)
						{
							cmntList.get(j).remove();
						}
					}
					twtconnect=new TwitterConnectionClass();
					acknowledgment=twtconnect.deleteContent(apiKey,apiSecret,userToken,userTokenSecret,postMsg.getAcknowledgment());
					System.out.println(acknowledgment);
					if(acknowledgment.equals("Content has been deleted Successfully"))
					{
						postMsg.remove();						
						acknowledgment="Content has been Removed Successfully!";
					}
					else if(acknowledgment.equals("1"))
					{
						postMsg.remove();
						acknowledgment="Content has been Removed from Database because it won't available at Twitter!!!";
					}
					else
					{
						acknowledgment="Its not Existed or/There is Some Error Please Try after Sometimes!!";
					}
				}
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return acknowledgment;
	}


	/**
	 * 
	 * @param com.axelor.auth.db.User user
	 * it will validate user and make custom-API and retrieve response and Store it into Database with removing existing duplications
	 * @return String acknowledgement
	 */

	@Transactional
	public String importFollowers(User user)
	{
		@SuppressWarnings("rawtypes")
		ArrayList returnResponse=new ArrayList();
		try
		{
			SocialNetworking sn=SocialNetworking.all().filter("name=?", "Twitter").fetchOne();
			List<ApplicationCredentials> query=ApplicationCredentials.all().filter("userId=? and snType=? ", user,sn).fetch();
			List<PersonalCredential> credential=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			if(credential.isEmpty())
			{
				acknowledgment="Please Login First";				

			}
			else
			{
				apiKey=query.get(0).getApikey();
				apiSecret=query.get(0).getApisecret();
				userToken=credential.get(0).getUserToken();
				userTokenSecret=credential.get(0).getUserTokenSecret();
				twtconnect=new TwitterConnectionClass();
				returnResponse=twtconnect.importContact(apiKey, apiSecret, userToken, userTokenSecret);
				List<ImportContact> query1=ImportContact.all().filter("curUser=? and snType=?", user,sn).fetch();
				List<String> str=new ArrayList<String>();
				for(int i=0;i<query1.size();i++)
				{
					str.add(query1.get(i).getUserId());

				}
				for(int p=0;p<returnResponse.size();p+=3)
				{
					if(!str.contains(returnResponse.get(p).toString()))
					{
						ImportContact twtcntc=new ImportContact();
						twtcntc.setUserId(returnResponse.get(p).toString());
						twtcntc.setName(returnResponse.get((p+1)).toString());
						twtcntc.setLink(returnResponse.get((p+2)).toString());
						twtcntc.setSnType(sn);
						twtcntc.setCurUser(user);
						twtcntc.persist();
					}
				}
			}
			acknowledgment="Contacts Imported Successfully Please Go back and Press Refresh";
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * 
	 * @param com.axelor.auth.db.User user
	 * @param String twitterUserProfileId
	 * @param String msgToSend
	 * it will call to Custom-API and send your message to Twitter User 
	 * @return String acknowledgement
	 */
	@Transactional
	public String sentMessage(User user,String id,String msg)
	{
		try
		{
			SocialNetworking sn=SocialNetworking.all().filter("name=?", "Twitter").fetchOne();
			List<ApplicationCredentials> query=ApplicationCredentials.all().filter("userId=? and snType=? ", user,sn).fetch();
			List<PersonalCredential> credential=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			if(credential.isEmpty())
			{
				acknowledgment="Please Login First";
			}
			else
			{
				apiKey=query.get(0).getApikey();
				apiSecret=query.get(0).getApisecret();
				userToken=credential.get(0).getUserToken();
				userTokenSecret=credential.get(0).getUserTokenSecret();
				twtconnect=new TwitterConnectionClass();
				acknowledgment=twtconnect.sendMessage(apiKey, apiSecret, userToken, userTokenSecret,id, msg);
				if(!Character.isDigit(acknowledgment.charAt(0)))
				{
					acknowledgment="There is Some Error";
				}						
			}
		}
		catch(Exception e)
		{
			acknowledgment=e.getMessage();
			e.printStackTrace();
		}
		return acknowledgment;
	}


	/**
	 * 
	 * @param com.axelor.auth.db.User user
	 * Call to Custom API and Store Response into DB with Current-User 
	 * @return String acknowledgement
	 */
	@Transactional
	public String getInbox(User user)
	{
		@SuppressWarnings("rawtypes")
		ArrayList returnResponse=new ArrayList();
		try
		{
			List<TwitterInbox> query1=TwitterInbox.all().filter("curUser=?", user).fetch();
			List<String> str=new ArrayList<String>();
			if(query1.size()>0)
			{
				for(int i=0;i<query1.size();i++)
				{
					str.add(query1.get(i).getMsgId());
					query1.get(i).remove();
				}
			}
			SocialNetworking sn=SocialNetworking.all().filter("name=?", "Twitter").fetchOne();
			List<ApplicationCredentials> query=ApplicationCredentials.all().filter("userId=? and snType=? ", user,sn).fetch();
			List<PersonalCredential> credential=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			if(credential.isEmpty())
			{
				acknowledgment="Please Login First";
			}
			else
			{
				apiKey=query.get(0).getApikey();				
				apiSecret=query.get(0).getApisecret();				
				userToken=credential.get(0).getUserToken();
				userTokenSecret=credential.get(0).getUserTokenSecret();
				TwitterInbox twtibox=new TwitterInbox();
				twtconnect=new TwitterConnectionClass();
				returnResponse=twtconnect.getDirectMessages(apiKey,apiSecret,userToken,userTokenSecret);
				for(int p=0;p<returnResponse.size();p+=4)
				{				
					twtibox=new TwitterInbox();
					twtibox.setMsgId(returnResponse.get(p).toString());
					twtibox.setMsgContent(returnResponse.get((p+1)).toString());
					twtibox.setSenderName(returnResponse.get((p+2)).toString());
					twtibox.setSenderId(returnResponse.get((p+3)).toString());
					twtibox.setCurUser(user);
					twtibox.persist();
				}
				acknowledgment="Inbox Imported Successfully!!";
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * 
	 * @param com.axelor.auth.db.User user
	 * @param java.util.List contentId
	 * it will validate user and make Call to custom-API for Deletion
	 * @return String acknowledgement 
	 */
	@Transactional
	public String deleteDirectMessage(User user,@SuppressWarnings("rawtypes") List contentId)
	{
		try
		{
			SocialNetworking sn=SocialNetworking.all().filter("name=?", "Twitter").fetchOne();
			List<ApplicationCredentials> query=ApplicationCredentials.all().filter("userId=? and snType=? ", user,sn).fetch();
			List<PersonalCredential> credential=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			if(credential.isEmpty())
			{
				acknowledgment="Please Login First";					
			}
			else
			{
				apiKey=query.get(0).getApikey();
				apiSecret=query.get(0).getApisecret();
				userToken=credential.get(0).getUserToken();
				userTokenSecret=credential.get(0).getUserTokenSecret();
				for(int i=0;i<contentId.size();i++)
				{
					TwitterInbox delMsg=TwitterInbox.all().filter("id=?", contentId.get(i)).fetchOne();
					twtconnect=new TwitterConnectionClass();
					acknowledgment=twtconnect.deleteInbox(apiKey,apiSecret,userToken,userTokenSecret,delMsg.getMsgId());
					System.out.println(acknowledgment);
					if(acknowledgment.equals("Deleted"))
					{
						delMsg.remove();						
						acknowledgment="Content has been Removed Successfully!";
					}
					else if(acknowledgment.equals("1"))
					{
						delMsg.remove();
						acknowledgment="Content has been Removed from Database because it won't available at Twitter!!!";
					}
					else
					{
						acknowledgment="Its not Existed or/There is Some Error Please Try after Sometimes!!";
					}
				}
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * @param com.axelor.auth.db.User user
	 * it will Call to Custom-API and 
	 * @return String acknowledgement
	 */
	@Transactional
	public String getHomeTimeLine(User user)
	{
		@SuppressWarnings("rawtypes")
		ArrayList returnResponse=new ArrayList();
		try
		{
			SocialNetworking sn=SocialNetworking.all().filter("name=?", "Twitter").fetchOne();
			List<ApplicationCredentials> query=ApplicationCredentials.all().filter("userId=? and snType=? ", user,sn).fetch();
			List<PersonalCredential> credential=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			if(credential.isEmpty())
			{
				acknowledgment="Please Login First";
			}
			else
			{
				apiKey=query.get(0).getApikey();
				apiSecret=query.get(0).getApisecret();
				userToken=credential.get(0).getUserToken();
				userTokenSecret=credential.get(0).getUserTokenSecret();
				twtconnect=new TwitterConnectionClass();
				try
				{
					TwitterConfig param=TwitterConfig.all().filter("curUser=?",user).fetchOne();
					if(param==null)
					{
						throw new javax.persistence.NoResultException();
					}
					else
					{
						if (param.getSince()==false) 
						{
							returnResponse=twtconnect.fetchHomeTimeline(apiKey,apiSecret,userToken,userTokenSecret,param.getPage(),param.getContent(),null);
						}
						else 
						{
							EntityManager em=JPA.em();
							Long maxId=(Long)em.createQuery("select MAX(e.timelineContentId) from TwitterHomeTimeline e where e.curUser="+user.getId()).getSingleResult();
							System.out.println(maxId);
							if(maxId==null)
							{
								acknowledgment="No Data Is been Existed";
							}
							else
							{
								returnResponse=twtconnect.fetchHomeTimeline(apiKey,apiSecret,userToken,userTokenSecret,param.getPage(),param.getContent(),maxId);
							}
						}
					}
				}
				catch (javax.persistence.NoResultException e) 
				{
					System.out.println("No parameter has been set for This User");
					returnResponse=twtconnect.fetchHomeTimeline(apiKey,apiSecret,userToken,userTokenSecret,0,0,null);
				}
				TwitterHomeTimeline twtTimeline=new TwitterHomeTimeline();
				List<TwitterHomeTimeline> query1=TwitterHomeTimeline.all().filter("curUser=?",user).fetch();
				List<Long> str=new ArrayList<Long>();
				for(int i=0;i<query1.size();i++)
				{
					str.add(query1.get(i).getTimelineContentId());
				}
				int countNew=0;
				for(int i=0;i<returnResponse.size();i+=5)
				{
					if(!str.contains(returnResponse.get(i).toString()))
					{
						twtTimeline=new TwitterHomeTimeline();
						twtTimeline.setTimelineContentId(Long.parseLong(returnResponse.get(i).toString()));
						twtTimeline.setScreenName(returnResponse.get(i+1).toString());
						twtTimeline.setActualName(returnResponse.get(i+2).toString());
						twtTimeline.setActualContent(returnResponse.get(i+3).toString());
						twtTimeline.setContentDate(returnResponse.get(i+4).toString());
						twtTimeline.setCurUser(user);
						twtTimeline.persist();
						countNew++;
					}
					if(countNew==0)
					{
						acknowledgment="Your Existing List is Up-to-Date";
					}
					else
					{
						acknowledgment="Succesfully Rertived "+countNew+" New Tweets";
					}
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * @param com.axelor.auth.db.User user
	 * it will validate user and make Call to custom-API for retrieving Followers Request
	 * @return String acknowledgement 
	 */
	@Transactional
	public String getFollowerRequest(User user)
	{
		@SuppressWarnings("rawtypes")
		ArrayList returnResponse=new ArrayList();
		try
		{
			SocialNetworking sn=SocialNetworking.all().filter("name=?", "Twitter").fetchOne();
			List<ApplicationCredentials> query=ApplicationCredentials.all().filter("userId=? and snType=? ", user,sn).fetch();
			List<PersonalCredential> credential=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			if(credential.isEmpty())
			{
				acknowledgment="Please Login First";					

			}
			else
			{
				apiKey=query.get(0).getApikey();
				apiSecret=query.get(0).getApisecret();
				userToken=credential.get(0).getUserToken();
				userTokenSecret=credential.get(0).getUserTokenSecret();
				twtconnect=new TwitterConnectionClass();
				returnResponse=twtconnect.retrivePendingRequest(apiKey, apiSecret, userToken, userTokenSecret);
				TwitterFollowerRequest twtRequest=new TwitterFollowerRequest();
				List<TwitterFollowerRequest> query1=TwitterFollowerRequest.all().filter("curUser=?", user).fetch();
				if(query1.size()>0)
				{
					for(int i=0;i<query1.size();i++)
					{
						query1.get(i).remove();
					}
				}
				for(int i=0;i<returnResponse.size();i+=5)
				{
					twtRequest=new TwitterFollowerRequest();
					twtRequest.setLink(returnResponse.get(i).toString());
					twtRequest.setUserId(returnResponse.get(i+1).toString());
					twtRequest.setScreenName(returnResponse.get(i+2).toString());
					twtRequest.setName(returnResponse.get(i+3).toString());
					twtRequest.setSendedOn(returnResponse.get(i+4).toString());
					twtRequest.setCurUser(user);					
					twtRequest.persist();
				}
				acknowledgment="<a href=https://twitter.com/follower_requests targer=_blank>Successfully Imported!! but you Must Click Here To accept it.</a>";
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return acknowledgment;		
	}

	//Context is PostTweet Original One
	/**
	 * Call from PostTweet
	 * @param com.axelor.auth.db.User user
	 * @param tweetIdOnWhichReplayNeedToMake
	 * It will call To Custom-API and and pass tweetId on which replay(s) is being fetch but according to Tweeter Policy it will atleast take 1 Min after specific replay is being posted
	 * @return String acknowledgement
	 */
	@Transactional
	public String orgPostTweetReplay(User user,String gid)
	{
		@SuppressWarnings("rawtypes")
		ArrayList comments=new ArrayList();
		PostTweet context=PostTweet.all().filter("acknowledgment=?", gid).fetchOne();
		SocialNetworking sn=SocialNetworking.all().filter("name=?","Twitter").fetchOne();
		List<ApplicationCredentials> query=ApplicationCredentials.all().filter("snType=?",sn).fetch();
		List<PersonalCredential> credential=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
		try
		{
			if(query.isEmpty())
			{
				acknowledgment="Please Login First";
			}
			else
			{
				apiKey=query.get(0).getApikey();			
				apiSecret=query.get(0).getApisecret();			
				userToken=credential.get(0).getUserToken();			
				userTokenSecret=credential.get(0).getUserTokenSecret();
				twtconnect=new TwitterConnectionClass();
				comments=twtconnect.getCommentsOfTweet(apiKey, apiSecret, userToken, userTokenSecret,gid);
				List<TweetComment> query1=TweetComment.all().filter("curUser=?",user).fetch();
				List<String> str=new ArrayList<String>();
				for(int i=0;i<query1.size();i++)
				{
					str.add(query1.get(i).getTweetcommentid());
				}
				for(int i=0;i<comments.size();i+=5)
				{
					if(!str.contains(comments.get(i).toString()))
					{
						ImportContact impcnt=ImportContact.all().filter("userId=? and curUser=? and snType=?",comments.get(i+1),user,sn).fetchOne();
						TweetComment tcmnt=new TweetComment();
						tcmnt.setTweetcommentid(comments.get(i).toString());
						tcmnt.setContentid(context);
						tcmnt.setTweetFrom(impcnt);
						tcmnt.setTweetContent(comments.get(i+2).toString());
						tcmnt.setTweetTime(comments.get(i+3).toString());
						tcmnt.setTweetFevourite(comments.get(i+4).toString());
						tcmnt.setCurUser(user);
						tcmnt.merge();
					}
				}
				int val=0;
				if(comments.size()>=5)
				{
					val=(comments.size()/5);
				}
				acknowledgment=val+" of comments are there";
			}
		}
		catch (javax.persistence.OptimisticLockException ole)
		{
			System.out.println(ole.getEntity());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return acknowledgment;
	}

	/**
	 * It will call from Sale-Order and used to Post its date as Tweet
	 * @param com.axelor.auth.db.User user
	 * @param String contentId
	 * @param String content
	 * @return
	 */
	//Context is SALE-ORDER
	@Transactional
	public String postTweetReplay(User user,String contentId,String content)
	{
		try
		{
			SocialNetworking sn=SocialNetworking.all().filter("name=?", "Twitter").fetchOne();
			List<ApplicationCredentials> query=ApplicationCredentials.all().filter("userId=? and snType=? ", user,sn).fetch();
			List<PersonalCredential> credential=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			if(credential.isEmpty())
			{
				acknowledgment="Please Login First";
			}
			else
			{
				apiKey=query.get(0).getApikey();
				apiSecret=query.get(0).getApisecret();
				userToken=credential.get(0).getUserToken();
				userTokenSecret=credential.get(0).getUserTokenSecret();
				twtconnect=new TwitterConnectionClass();
				acknowledgment=twtconnect.postTweetReplay(apiKey, apiSecret, userToken, userTokenSecret, contentId, content);
				System.out.println("Service"+acknowledgment);
				if(!acknowledgment.startsWith("[a-zA-Z]"))
				{
					acknowledgment="Content is been Posted Successfully";
				}
			}
		}
		catch (Exception e) 
		{
			acknowledgment=e.getMessage();
			e.printStackTrace();
		}
		return acknowledgment;
	}

	//ADDED BY ME ON 16-04-2013
	public PostTweet addTweet(String content,User user,String ack) throws Exception
	{
		EntityManager em=JPA.em();
		EntityTransaction tx=em.getTransaction();
		//				String t="Twitter";
		//				SocialNetworking sn=SocialNetworking.all().filter("name=?",t).fetchOne();
		//				if(sn !=null)
		//				{
		//					ApplicationCredentials applicationCredential=ApplicationCredentials.all().filter("snType=?",sn ).fetchOne();
		//					if(applicationCredential !=null)
		//					{
		//						PersonalCredential personalCredential=PersonalCredential.all().filter("userId=? and snType=?", user,sn).fetchOne();
		//						if(personalCredential != null)
		//						{
		//							apiKey=applicationCredential.getApikey();
		//							apiSecret=applicationCredential.getApisecret();
		//							userToken=personalCredential.getUserToken();
		//							userTokenSecret=personalCredential.getUserTokenSecret();

		DateTime date=twtconnect.getStatusTime(ack); 	//apiKey,apiSecret,userToken,userTokenSecret,
		System.out.println(date);
		//						}
		//						else
		//						{
		//							throw new Exception("Please Login First");
		//						}
		//					}
		//					else
		//					{
		//						throw new Exception("No Application Defined. Contact Administrator");
		//					}
		//				}
		//				else
		//				{
		//					
		//				}
		tx.begin();
		PostTweet tweet=new PostTweet();
		tweet.setAcknowledgment(ack);
		tweet.setContent(content);
		tweet.setCurUser(user);
		tweet.setPostedTime(date);
		tweet.persist();
		tx.commit();
		return tweet;
	}

	/**
	 * 
	 * @param com.axelor.auth.db.User user
	 * @param com.axelor.sn.db.PostTweet postTweet
	 * @throws Exception
	 */
	public void getTweetsReply(User user,PostTweet postTweet) throws Exception
	{
		String id=postTweet.getAcknowledgment();
		@SuppressWarnings("rawtypes")
		ArrayList comments=new ArrayList();
		SocialNetworking snType=SocialNetworking.all().filter("name=?","Twitter").fetchOne();
		if(snType !=null)
		{
			ApplicationCredentials applicationCredential=ApplicationCredentials.all().filter("snType=?",snType ).fetchOne();
			if(applicationCredential !=null)
			{
				PersonalCredential personalCredential=PersonalCredential.all().filter("userId=? and snType=?", user,snType).fetchOne();
				if(personalCredential != null)
				{
					apiKey=applicationCredential.getApikey();
					apiSecret=applicationCredential.getApisecret();
					userToken=personalCredential.getUserToken();
					userTokenSecret=personalCredential.getUserTokenSecret();
					twtconnect=new TwitterConnectionClass();
					comments = twtconnect.getCommentsOfTweet(apiKey, apiSecret, userToken, userTokenSecret, id);
				}
				else
				{
					throw new Exception("Please Login First");
				}
			}
			else
			{
				throw new Exception("No Application Defined...");
			}
		}
		else
		{
			throw new Exception("Error");
		}
		List<TweetComment> lstTweetComment=TweetComment.all().filter("curUser=? and contentid=?", user,postTweet).fetch(); //em.createQuery("select a from TweetComment a where a.curUser="+user.getId(), TweetComment.class).getResultList()
		List<String> str=new ArrayList<String>();
		for(int i=0;i<lstTweetComment.size();i++)
		{
			str.add(lstTweetComment.get(i).getTweetcommentid());
		}
		EntityManager em=JPA.em();
		EntityTransaction tx=em.getTransaction();
		tx.begin();
		for(int i=0;i<comments.size();i+=5)
		{
			if(!str.contains(comments.get(i).toString()))
			{
				ImportContact importContact=ImportContact.all().filter("userId=? and curUser=? and snType=?",comments.get(i+1).toString(),user,snType).fetchOne();
				TweetComment tweetComment=new TweetComment();
				tweetComment.setTweetcommentid(comments.get(i).toString());
				tweetComment.setContentid(postTweet);
				tweetComment.setTweetFrom(importContact);
				tweetComment.setTweetContent(comments.get(i+2).toString());
				tweetComment.setTweetTime(comments.get(i+3).toString());
				tweetComment.setTweetFevourite(comments.get(i+4).toString());
				tweetComment.setCurUser(user);
				tweetComment.persist();
			}
		}
		tx.commit();
	}

	public List<TweetComment> fetchTweetsReply(PostTweet postTweet)
	{
		List<TweetComment> lstTweetComment=TweetComment.all().filter("contentid=?", postTweet).fetch();
		return lstTweetComment; 
	}
	/*public String searchTwitter(User user,String searchQuery)
	{
		ArrayList searchedValue=new ArrayList();
		try
		{
			ApplicationCredentials acc=new ApplicationCredentials();
			EntityManager em=JPA.em();
			EntityTransaction tx=em.getTransaction();
			List<SocialNetworking> sn=em.createQuery("select a from SocialNetworking a where name='Twitter'",SocialNetworking.class).getResultList();
			List<ApplicationCredentials> query=em.createQuery("select a from ApplicationCredentials a where a.snType="+sn.get(0).getId(), ApplicationCredentials.class).getResultList();
			List<PersonalCredential> credential=em.createQuery("select a from PersonalCredential a where a.userId="+user.getId()+"and snType="+sn.get(0).getId(), PersonalCredential.class).getResultList();

			if(credential.isEmpty())
			{
				acknowledgment="Please Login First";
			}
			else
			{
				apiKey=query.get(0).getApikey();
				apiSecret=query.get(0).getApisecret();
				userToken=credential.get(0).getUserToken();
				userTokenSecret=credential.get(0).getUserTokenSecret();
				twtconnect=new TwitterConnectionClass();
				searchedValue=twtconnect.searchPerson(apiKey, apiSecret, userToken, userTokenSecret, searchQuery);
				TweetSearchResult objSearchResult=new TweetSearchResult();
				TweetSearch objSearch=new TweetSearch();
				for(int i=0;i<searchedValue.size();i+=3)
				{
					objSearch.setTweetSource(searchQuery);					
					objSearchResult.setTwitterid(searchedValue.get(i).toString());
					objSearchResult.setName(searchedValue.get(i+1).toString());
					objSearchResult.setLink(searchedValue.get(i+2).toString());
					objSearchResult.setCurUser(user);
					tx.begin();
					em.persist(objSearchResult);
					em.persist(objSearch);
					tx.commit();
				}
				acknowledgment="Search Value="+searchedValue.size()+"";
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return acknowledgment;
	}
	 */

}
