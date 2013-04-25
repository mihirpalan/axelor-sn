package com.axelor.sn.service
import java.lang.annotation.Retention;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter.DateTime;
import javax.persistence.EntityManager
import javax.persistence.EntityTransaction
import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException
import javax.persistence.RollbackException

import com.axelor.auth.db.User
import com.axelor.db.JPA
import com.axelor.db.Query
import com.axelor.db.mapper.types.JodaAdapter.LocalDateAdapter;
import com.axelor.rpc.ActionRequest
import com.axelor.rpc.ActionResponse
import com.axelor.sn.db.ApplicationCredentials
import com.axelor.sn.db.Comment
import com.axelor.sn.db.FBFriendrequest;
import com.axelor.sn.db.ConfigParameter;
import com.axelor.sn.db.FBFriendrequest;
import com.axelor.sn.db.FBInbox
import com.axelor.sn.db.FBNewsFeed;
import com.axelor.sn.db.FBPageComment
import com.axelor.sn.db.FBPagePost
import com.axelor.sn.db.FBPages;
import com.axelor.sn.db.FacebookNotification
import com.axelor.sn.db.ImportContact
import com.axelor.sn.db.PersonalCredential
import com.axelor.sn.db.PostEvent
import com.axelor.sn.db.PostMessage;
import com.axelor.sn.db.SearchPerson
import com.axelor.sn.db.SearchResult;
import com.axelor.sn.db.SocialNetworking
import com.axelor.sn.fb.FacebookConnectionClass;

import com.fasterxml.jackson.databind.node.NodeCursor.Array;
import com.google.inject.matcher.Matchers.Returns;
import com.google.inject.persist.Transactional;

import org.hibernate.annotations.OptimisticLockType;
import org.joda.time.Chronology;
import org.joda.time.Chronology;
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

class SNFBService
{
	FacebookConnectionClass fbconnect;
	String acknowledgment="";
	String apikey,apisecret,userToken,redirectUrl;


	public String obtainAuthUrl(User user,SocialNetworking sn)
	{

		ApplicationCredentials query=ApplicationCredentials.all().filter("snType=?", sn).fetchOne()
		PersonalCredential credential=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetchOne();
		if(query.apikey.isEmpty() && query.apisecret.isEmpty())
		{
			acknowledgment="Sorry You can't Do anyting Admin Doesnt set Application Credentials ";
		}
		else
		{
			if(credential!=null)
			{
				acknowledgment="You Already Have One Account Associated...";
			}
			else
			{
				apikey=query.getApikey()
				apisecret=query.getApisecret()
				redirectUrl=query.getRedirectUrl();
				fbconnect=new FacebookConnectionClass();
				if(redirectUrl.isEmpty())
				{
					redirectUrl="http://127.0.0.1:8080/axelor-demo-sn/snapp/100"
				}
				acknowledgment=fbconnect.changeAuthorization(apikey,apisecret,redirectUrl)
				println(acknowledgment)
			}
		}
		return acknowledgment;
	}

	@Transactional
	public String storingToken(User user,String code)
	{

		SocialNetworking sn=SocialNetworking.all().filter("name=?", "Facebook").fetchOne()
		ApplicationCredentials query=ApplicationCredentials.all().filter("snType=?", sn).fetchOne()
		PersonalCredential credential=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetchOne();
		if(query.apikey.isEmpty() && query.apisecret.isEmpty())
		{
			acknowledgment="Sorry You can't Do anyting Admin Doesnt set Application Credentials ";
		}
		else
		{
			if(credential!=null)
			{
				acknowledgment="You Already Have One Account Associated...";
			}
			else
			{
				apikey=query.getApikey()
				apisecret=query.getApisecret()
				redirectUrl=query.getRedirectUrl();
				fbconnect=new FacebookConnectionClass();
				if(redirectUrl.isEmpty())
				{
					redirectUrl="http://127.0.0.1:8080/axelor-demo-sn/snapp/100"
				}
				acknowledgment=fbconnect.getAccessToken(apikey,apisecret,code,redirectUrl)
				//acknowledgment=fbconnect.getAccessToken(query.apikey,query.apisecret,code);
				if(!acknowledgment.startsWith("Opps Something Went wrong"))
				{
					String[] token_name=acknowledgment.split(":");
					Boolean b=false;
					PersonalCredential personalCredential=new PersonalCredential();
					personalCredential.setUserId(user);
					personalCredential.setUserToken(token_name[0]);
					personalCredential.setSnUserName(token_name[1]);
					personalCredential.setKnowStat(b);
					personalCredential.setSnType(sn);
					personalCredential.persist();
					println(personalCredential);

				}
			}
		}
		return "Login Successfull!!"
	}

	@Transactional
	public String getUserToken(User user,SocialNetworking snn,String username,String password)
	{
		try
		{
			SocialNetworking sn=snn;
			ApplicationCredentials query=ApplicationCredentials.all().filter("snType=?", sn).fetchOne()
			PersonalCredential credential=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetchOne();
			if(query.apikey.isEmpty() && query.apisecret.isEmpty())
			{
				acknowledgment="Sorry You can't Do anyting Admin Doesnt set Application Credentials ";
			}
			else
			{
				if(credential!=null)
				{
					acknowledgment="You Already Have One Account Associated...";
				}
				else
				{
					apikey=query.getApikey()
					apisecret=query.getApisecret()
					fbconnect=new FacebookConnectionClass();
					acknowledgment=fbconnect.getFBLogin(apikey,apisecret,username,password)
					if(!acknowledgment.startsWith("Opps Something Went wrong"))
					{
						Boolean b=false;
						PersonalCredential personalCredential=new PersonalCredential();
						personalCredential.setUserId(user);
						personalCredential.setSnUsername(username);
						personalCredential.setPassword("");
						personalCredential.setUserToken(acknowledgment);
						personalCredential.setKnowStat(b);
						personalCredential.setSnType(snn);
						personalCredential.merge();
						println(personalCredential);

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

	@Transactional
	public String getRefreshToken(User user,SocialNetworking sn)
	{
		try
		{
			ApplicationCredentials query=ApplicationCredentials.all().filter("snType=?", sn).fetchOne()
			PersonalCredential credential=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetchOne()
			if(query.apikey.isEmpty() && query.apisecret.isEmpty())
			{
				acknowledgment="Sorry You can't Do anyting Admin Doesnt set Application Credentials ";
			}
			else
			{
				if(credential==null)
				{
					acknowledgment="Please Login First";
				}
				else
				{
					apikey=query.getApikey()
					apisecret=query.getApisecret()
					userToken=credential.getUserToken();
					fbconnect=new FacebookConnectionClass();
					PersonalCredential pcreden=credential;
					acknowledgment=fbconnect.getRefreshAccessToken(apikey,apisecret,userToken);
					pcreden.setUserToken(acknowledgment);
					println("Return Value="+acknowledgment);
					pcreden.merge();

					acknowledgment="Refresh Token Stored Successfully You may Proceed Now";
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			acknowledgment=e.getMessage();
		}
	}



	public SocialNetworking sayType()
	{
		SocialNetworking snType=SocialNetworking.all().filter("name=?", "Facebook").fetchOne()
		//em.createQuery("select a from SocialNetworking a where a.name='Facebook'",SocialNetworking.class).getSingleResult()
		return snType
	}
	/**
	 * 
	 * @param criteria specify Search
	 * @return ArrayList of values
	 */

	@Transactional
	public String searchPerson(User user,String criteria)
	{
		boolean checkVal=true;
		ArrayList value=new ArrayList();
		fbconnect=new FacebookConnectionClass()
		String ack,userToken;
		try
		{
			SocialNetworking sn=SocialNetworking.all().filter("name=?","Facebook").fetchOne();
			List<PersonalCredential> query=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch()
			//em.createQuery("select a from PersonalCredential a where a.userId='"+user.getId()+"'and snType="+sn.getId(), PersonalCredential.class).getResultList()
			if(query.isEmpty())
			{
				ack="Please Login First"
			}
			else
			{

				SearchResult result=new SearchResult();
				SearchPerson searchKeyword=new SearchPerson();
				userToken=query.get(0).getUserToken()
				value=fbconnect.fetchObjectOfPerson(criteria,userToken);
				println("Value size is"+value.size())
				SearchPerson objPerson;
				try
				{
					objPerson=SearchPerson.all().filter("searchparam=? and curUser=?",criteria,user).fetchOne()
					if(objPerson == null)
					{
						throw new javax.persistence.NoResultException();
					}
				}
				catch (javax.persistence.NoResultException e)
				{
					checkVal=false;
					for(int i=0;i<value.size();i+=5)
					{

						result=new SearchResult();
						searchKeyword.searchparam=criteria;
						searchKeyword.curUser=user;
						result.userid=value.get(i);
						result.firstname=value.get(i+1);
						result.lastname=value.get(i+2);
						result.gender=value.get(i+3);
						result.link=value.get(i+4);
						result.curUser=user;
						result.searchPerson=searchKeyword;
						result.persist();
					}
				}
				if(checkVal == true)
				{
					List<SearchResult> objResult=SearchResult.all().filter("searchPerson=? and curUser=?",objPerson,user).fetch();
					List<String> str=new ArrayList<String>();
					List<String> userId=new ArrayList<String>();
					for(int i=0;i<objResult.size();i++)
					{
						str.add(objResult.get(i).getUserid());
					}
					for(int i=0;i<value.size();i+=5)
					{
						if(!str.contains(value.get(i).toString()))
						{
							result=new SearchResult();
							searchKeyword.searchparam=criteria;
							searchKeyword.curUser=user;
							result.userid=value.get(i);
							result.firstname=value.get(i+1);
							result.lastname=value.get(i+2);
							result.gender=value.get(i+3);
							result.link=value.get(i+4);
							result.curUser=user;
							result.searchPerson=searchKeyword;
							result.persist();
						}
					}
				}
				acknowledgment="1"
				println(acknowledgment)
			}
		}
		catch(Exception e)
		{
			e.printStackTrace()
			ack=e.getMessage()
		}
		return acknowledgment;
	}



	/**
	 * 
	 * @param pmsg specify Message to Post
	 * @return FB generated ID of status
	 */
	@Transactional
	public String postStatus(User user,String pmsg,String paramPrivacy)
	{
		try
		{
			SocialNetworking sn=SocialNetworking.all().filter("name=?","Facebook").fetchOne();
			List<PersonalCredential> query=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch()
			if(query.isEmpty())
			{
				acknowledgment="Please Login First"
			}
			else
			{
				userToken=query.get(0).getUserToken()
				fbconnect=new FacebookConnectionClass()
				acknowledgment=fbconnect.publishMessage(pmsg,userToken,paramPrivacy)
				if(acknowledgment.equals("Please Go to Refresh Page for Refreshing the AccessToken and Try Again"))
				{
					acknowledgment="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
					query.get(0).knowStat=true;
					query.get(0).merge()
				}
			}
		}
		catch(Exception e)
		{
			acknowledgment=e.message
		}
		return acknowledgment
	}

	@Transactional
	public String postStatusFromOther(User user,String pmsg,String paramPrivacy)
	{
		try
		{
			System.out.println("postStatusFromOther");
			SocialNetworking sn=SocialNetworking.all().filter("name=?","Facebook").fetchOne();
			List<PersonalCredential> query=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch()
			if(query.isEmpty())
			{
				acknowledgment="Please Login First"
			}
			else
			{
				userToken=query.get(0).getUserToken()
				fbconnect=new FacebookConnectionClass()
				acknowledgment=fbconnect.publishMessage(pmsg,userToken,paramPrivacy)
				if(acknowledgment.equals("Please Go to Refresh Page for Refreshing the AccessToken and Try Again"))
				{
					acknowledgment="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
					query.get(0).knowStat=true;
					query.get(0).merge()
				}
				else
				{
					System.out.println("Else");
					PostMessage posted=new PostMessage();
					posted.content=pmsg;
					posted.acknowledgment=acknowledgment;
					posted.curUser=user;
					posted.privacy=paramPrivacy;
					org.joda.time.DateTime timeNow=new org.joda.time.DateTime();
					posted.postedTime=timeNow;
					posted.persist();
				}
			}
		}
		catch(Exception e)
		{
			acknowledgment=e.message
			e.printStackTrace()
		}
		return acknowledgment
	}
	/**
	 * 
	 * @param startDate java.util.Date Object specify start date along with Time
	 * @param endDate java.util.Date Object specify end date along with Time
	 * @param eventName String event name which specify to FB
	 * @param location String Location of the event
	 * @return FB generated ID of Event
	 */
	@Transactional
	public String fbPostEvent(User user,Date startDate,Date endDate,String eventName,String location,String privacy)
	{
		try
		{
			SocialNetworking sn=SocialNetworking.all().filter("name=?","Facebook").fetchOne();
			List<PersonalCredential> query=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch()
			if(query.isEmpty())
			{
				acknowledgment="Please Login First";
			}
			else
			{
				userToken=query.get(0).getUserToken()
				fbconnect=new FacebookConnectionClass()
				acknowledgment=fbconnect.publishEvent(startDate,endDate,eventName,location,userToken,privacy)
				if(acknowledgment.equals("Please Go to Refresh Page for Refreshing the AccessToken and Try Again"))
				{
					acknowledgment="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
					query.get(0).knowStat=true;
					query.get(0).merge()
				}
			}
		}
		catch(Exception e)
		{
			acknowledgment=e.message;
			e.printStackTrace()
		}
		return acknowledgment
	}

	/**
	 * 
	 * @return ArrayList of all ur FB Contact which contain FBUSERID,NAME,LINK to their Account 
	 */

	@Transactional
	public String importContactsFB(User user)
	{
		ArrayList returnResponse=new ArrayList();
		String userToken,sntype;
		try
		{
			SocialNetworking sn=SocialNetworking.all().filter("name=?","Facebook").fetchOne();
			List<PersonalCredential> query=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch()
			if(query.isEmpty())
			{
				acknowledgment="Please Login First"
			}
			else
			{
				sntype=query.get(0).getSnType().getName()
				userToken=query.get(0).getUserToken()
				ImportContact fbcntc=new ImportContact()
				List<ImportContact> query1=ImportContact.all().filter("curUser=? and snType=?",user,sn).fetch();
				//em.createQuery("select a from ImportContact a where a.curUser="+user.getId()+"and sntype='"+sntype+"'", ImportContact.class).getResultList()
				fbconnect=new FacebookConnectionClass()
				returnResponse=fbconnect.getListOfFriends(userToken);
				if(returnResponse.contains("Please Go to Refresh Page for Refreshing the AccessToken and Try Again"))
				{
					acknowledgment="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
					query.get(0).knowStat=true;
					query.get(0).merge()
				}
				else
				{
					println("Databse Existing Rows="+query1.size())
					List<String> str=new ArrayList<String>();
					List<String> userId=new ArrayList<String>();
					for(int i=0;i<query1.size();i++)
					{
						str.add(query1.get(i).getUserId());
					}
					for(int p=0;p<returnResponse.size();p+=3)
					{
						if(!str.contains(returnResponse.get(p).toString()))
						{

							fbcntc=new ImportContact()
							fbcntc.userId=returnResponse.get(p)
							fbcntc.name=returnResponse.get((p+1))
							fbcntc.link=returnResponse.get((p+2))
							fbcntc.snType=sn;
							fbcntc.curUser=user;
							fbcntc.persist();
						}
					}
					acknowledgment="Contacts Imported Successfully Please Press Refresh";
				}
			}
		}
		catch(Exception e)
		{
			acknowledgment=e.toString();
			e.printStackTrace()
		}
		return acknowledgment;
	}


	@Transactional
	public String getCommentsFB(User user,String pmsg)
	{
		ArrayList returnResponse=new ArrayList();
		String contentId;
		try
		{
			contentId=pmsg
			SocialNetworking sn=SocialNetworking.all().filter("name=?","Facebook").fetchOne();
			List<PersonalCredential> query=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch()
			PostMessage status=PostMessage.all().filter("acknowledgment=?",contentId).fetchOne()
			//em.createQuery("select a from PostMessage a where a.acknowledgment='"+contentId+"'").getSingleResult();
			if(query.isEmpty())
			{
				acknowledgment="Please Login First"
			}
			else
			{
				userToken=query.get(0).getUserToken()
				fbconnect=new FacebookConnectionClass()
				returnResponse=fbconnect.getComments(userToken,contentId);
				if(returnResponse.contains("Please Go to Refresh Page for Refreshing the AccessToken and Try Again"))
				{
					acknowledgment="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
					query.get(0).knowStat=true;
					query.get(0).merge();
				}
				else
				{
					Comment comnt=new Comment()
					List<Comment> query1=Comment.all().filter("curUser=?",user).fetch()
					//em.createQuery("select a from Comment a where a.curUser='"+user.getId()+"'", Comment.class).getResultList()
					List<String> str=new ArrayList<String>();
					//List<String> userId=new ArrayList<String>();
					for(int i=0;i<query1.size();i++)
					{
						str.add(query1.get(i).getCommentid());
					}
					for(int i=0;i<returnResponse.size();i+=5)
					{
						if(!str.contains(returnResponse.get(i).toString()))
						{
							comnt=new Comment()
							ImportContact impcnt=ImportContact.all().filter("userId=? and curUser=?",returnResponse.get(i+1),user).fetchOne()
							//em.createQuery("select a from ImportContact a where a.userId='"+returnResponse.get(i+1)+"' and curUser='"+user.etId()+"'", ImportContact.class).getSingleResult()
							comnt.commentid=returnResponse.get(i);
							comnt.contentid=status;
							comnt.from_user=impcnt
							comnt.comment=returnResponse.get(i+2)
							comnt.commentTime=returnResponse.get(i+3)
							comnt.commentLikes=returnResponse.get(i+4)
							comnt.curUser=user
							//em.lock(comnt,LockModeType.PESSIMISTIC_READ);
							comnt.merge();//.persist();
							//em.flush();
							acknowledgment="Comment Will only Display if there is Some Comment on Content";
						}
					}
				}
			}
		}
		catch (javax.persistence.OptimisticLockException ole)
		{
			System.out.println(ole.getEntity());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		return acknowledgment;
	}


	/*public ArrayList getABSContacts()
	 {
	 ArrayList<Contact> emails=new ArrayList<Contact>();
	 try
	 {
	 JPA.runInTransaction
	 {
	 Query<Contact> contact=Contact.all();
	 emails=contact.fetch();
	 println(emails.size());
	 for(int i=0;i<emails.size();i++)
	 {
	 println(emails.get(i));
	 }
	 }
	 }
	 catch(Exception e)
	 {
	 e.printStackTrace();
	 }
	 return emails;
	 }*/

	@Transactional
	public String getInbox(User user)
	{

		ArrayList returnResponse=new ArrayList();
		String sntype;
		try
		{

			SocialNetworking sn=SocialNetworking.all().filter("name=?","Facebook").fetchOne();
			//em.createQuery("select a from SocialNetworking a where a.name='Facebook'", SocialNetworking.class).getSingleResult();
			List<PersonalCredential> query=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			//em.createQuery("select a from PersonalCredential a where a.userId='"+user.getId()+"'and snType="+sn.getId(), PersonalCredential.class).getResultList()
			if(query.isEmpty())
			{
				acknowledgment="Please Login First"
			}
			else
			{
				sntype=query.get(0).getSnType().getName()
				userToken=query.get(0).getUserToken()
				FBInbox fibox=new FBInbox();
				fbconnect=new FacebookConnectionClass()
				returnResponse=fbconnect.retriveMessage(userToken);
				if(returnResponse.contains("Please Go to Refresh Page for Refreshing the AccessToken and Try Again"))
				{
					acknowledgment="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
					query.get(0).knowStat=true;
					query.get(0).merge()
				}
				else
				{
					List<FBInbox> query1=FBInbox.all().filter("curUser=?",user).fetch();

					//em.createQuery("select a from FBInbox a where a.curUser="+user.getId(), FBInbox.class).getResultList()
					List<String> str=new ArrayList<String>();
					List<String> userId=new ArrayList<String>();
					for(int i=0;i<query1.size();i++)
					{
						query1.get(i).remove();
						//str.add(query1.get(i).getMsgId());
					}
					for(int i=0;i<returnResponse.size();i+=5)
					{
						//					if(!str.contains(returnResponse.get(i).toString()))
						//					{
						fibox=new FBInbox();
						fibox.msgId=returnResponse.get(i);
						fibox.fromId=returnResponse.get(i+1);
						fibox.toName=returnResponse.get(i+2);
						fibox.msgBody=returnResponse.get(i+3);
						fibox.sendDate=returnResponse.get(i+4);
						fibox.curUser=user;
						fibox.persist()

						//					}
					}
					acknowledgment="Message Syncronization Completed!!";
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return acknowledgment;
	}

	@Transactional
	public String getNotifications(User user)
	{
		SimpleDateFormat sdf=new SimpleDateFormat("MM/dd/yyyy");
		Date sinceDate=null;
		Date untilDate=null;
		ArrayList newReturnResponse=new ArrayList();
		String sntype;
		String newNotif;
		int countUpdates=0;
		ArrayList returnResponse=new ArrayList();
		try
		{
			println(new LocalDateTime())
			SocialNetworking sn=SocialNetworking.all().filter("name=?","Facebook").fetchOne();
			List<PersonalCredential> query=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			if(query.isEmpty())
			{
				acknowledgment="Please Login First";
			}
			else
			{
				sntype=query.get(0).getSnType().getName();
				userToken=query.get(0).getUserToken();
				FacebookNotification fbnotif=new FacebookNotification();
				fbconnect=new FacebookConnectionClass();
				ConfigParameter param;
				try
				{
					param = ConfigParameter.all().filter("curUser=?", user).fetchOne()
					if(param == null)
					{
						throw new javax.persistence.NoResultException();
					}
					else
					{
						//Date sinceDate=new Date();
						if(param.getParamSince()>0)
						{
							Calendar c=Calendar.getInstance();
							c.add(Calendar.DATE, -(param.getParamSince()));							
							sinceDate=new Date(sdf.format(c.getTime()));
						}
						
						if(param.getParamUntil()>0)
						{
							Calendar c=Calendar.getInstance();
							c.add(Calendar.DATE, -(param.getParamUntil()));
							untilDate=new Date(sdf.format(c.getTime()));
						}
						println(sinceDate)
						println(untilDate)
						returnResponse=fbconnect.getNotification(userToken,param.paramLimit,sinceDate,untilDate);
					}
				}
				catch (javax.persistence.NoResultException e)
				{
					System.out.println("Parameters not setted for Current User");
					returnResponse=fbconnect.getNotification(userToken,0,null,null);
				}
				if(returnResponse.contains("Please Go to Refresh Page for Refreshing the AccessToken and Try Again"))
				{
					acknowledgment="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
					query.get(0).knowStat=true;
					query.get(0).merge()
				}
				else
				{

					if(!returnResponse.empty)
					{
						if(!returnResponse.get(0).equals("[]"))
						{
							println(returnResponse)

							newReturnResponse.addAll(returnResponse);
							countUpdates=newReturnResponse.size()/4;
							List<FacebookNotification> query1=FacebookNotification.all().filter("curUser=?",user).fetch()
							//em.createQuery("select a from FacebookNotification a where a.curUser="+user.getId(), FacebookNotification.class).getResultList()
							List<String> str=new ArrayList<String>();
							for(int i=0;i<query1.size();i++)
							{
								query1.get(i).remove();
								//str.add(query1.get(i).getNotifId());
							}
							println(newReturnResponse)
							int value=0;
							for(int i=0;i<newReturnResponse.size();i+=4)
							{
								fbnotif=new FacebookNotification();
								fbnotif.notifId=newReturnResponse.get(i);
								fbnotif.title=newReturnResponse.get(i+1);
								fbnotif.link=newReturnResponse.get(i+2);
								fbnotif.updateTime=newReturnResponse.get(i+3);
								fbnotif.curUser=user;
								fbnotif.persist();
								value=i+3;

							}
							//							countUpdates=value/4;
							acknowledgment=countUpdates;
							println(countUpdates);
						}
					}
					else
					{
						acknowledgment="No"
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

	@Transactional
	public String getDeleteEvent(User user,List contentId)
	{
		PostEvent postEvent;
		String sntype;
		try
		{
			SocialNetworking sn=SocialNetworking.all().filter("name=?","Facebook").fetchOne();
			List<PersonalCredential> query=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			if(query.isEmpty())
			{
				acknowledgment="Please Login First";
			}
			else
			{
				sntype=query.get(0).getSnType().getName();
				userToken=query.get(0).getUserToken();
				for(int i=0;i<contentId.size();i++)
				{
					postEvent=new PostEvent();
					postEvent=PostEvent.all().filter("id=?",contentId.get(i)).fetchOne()
					fbconnect=new FacebookConnectionClass()
					Boolean statusDeleted=fbconnect.delete(postEvent.getAcknowledgment(),userToken);
					if(statusDeleted)
					{
						postEvent.remove();
						acknowledgment="Content has been Removed Successfully!";
					}
					else
					{
						acknowledgment="Its not Existed or/There is Some Error Please Try after Sometimes!!"
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

	@Transactional
	public String getDeleteMessage(User user,List contentId)
	{
		PostMessage postMsg=new PostMessage();
		String sntype;
		try
		{
			SocialNetworking sn=SocialNetworking.all().filter("name=?","Facebook").fetchOne();
			List<PersonalCredential> query=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			if(query.isEmpty())
			{
				acknowledgment="Please Login First";
			}
			else
			{
				sntype=query.get(0).getSnType().getName();
				userToken=query.get(0).getUserToken();
				for(int i=0;i<contentId.size();i++)
				{

					postMsg=new PostMessage();
					postMsg=PostMessage.all().filter("id=?",contentId.get(i)).fetchOne()
					if(!postMsg.getComments().isEmpty())
					{
						List<Comment> cmnt=Comment.all().filter("contentid=?", postMsg).fetch();
						for(int j=0;j<cmnt.size();j++)
						{
							cmnt.get(j).remove();
						}
					}
					fbconnect=new FacebookConnectionClass();
					Boolean statusDeleted=fbconnect.delete(postMsg.getAcknowledgment(),userToken);
					if(statusDeleted)
					{
						postMsg.remove();
						acknowledgment="Content has been Removed Successfully!";
					}
					else
					{
						acknowledgment="Its not Existed or/There is Some Error Please Try after Sometimes!!"
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Transactional
	public String getFriendRequest(User user)
	{
		ArrayList returnResponse=new ArrayList();
		try
		{
			SocialNetworking sn=SocialNetworking.all().filter("name=?","Facebook").fetchOne();
			List<PersonalCredential> query=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			if(query.isEmpty())
			{
				acknowledgment="Please Login First";
			}
			else
			{
				//sntype=query.get(0).getSnType().getName();
				userToken=query.get(0).getUserToken();
				FBFriendrequest fbreq=new FBFriendrequest();
				fbconnect=new FacebookConnectionClass();
				returnResponse=fbconnect.getFriendRequest(userToken);
				if(returnResponse.contains("Please Go to Refresh Page for Refreshing the AccessToken and Try Again"))
				{
					acknowledgment="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
					query.get(0).knowStat=true;
					query.get(0).merge()
				}
				else
				{
					List<FBFriendrequest> query1=FBFriendrequest.all().filter("curUser=?", user).fetch()
					List<String> str=new ArrayList<String>();
					for(int i=0;i<query1.size();i++)
					{
						query1.get(i).remove();
					}

					int sizeOfResponse=(returnResponse.size())/3;
					for(int i=0;i<returnResponse.size();i+=3)
					{
						fbreq=new FBFriendrequest();
						fbreq.link=returnResponse.get(i);
						fbreq.name=returnResponse.get(i+1);
						fbreq.gender=returnResponse.get(i+2);
						fbreq.curUser=user;
						fbreq.persist();
					}
					acknowledgment="You have "+sizeOfResponse+" new Friend Request(s)";
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
	public String getNewsFeeds(User user)
	{
		SimpleDateFormat sdf=new SimpleDateFormat("MM/dd/yyyy");
		Date sinceDate=null;
		Date untilDate=null;
		int size1=0;
		ArrayList returnResponse=new ArrayList();
		try
		{

			SocialNetworking sn=SocialNetworking.all().filter("name=?","Facebook").fetchOne();
			List<PersonalCredential> query=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			if(query.isEmpty())
			{
				acknowledgment="Please Login First";
			}
			else
			{
				userToken=query.get(0).getUserToken();
				FBNewsFeed newsFeed=new FBNewsFeed();
				fbconnect=new FacebookConnectionClass();
				ConfigParameter param;
				try
				{
					param=ConfigParameter.all().filter("curUser=?",user).fetchOne()
					if(param == null)
					{
						throw new javax.persistence.NoResultException();
					}
					else
					{
						if(param.getParamSince()>0)
						{
							Calendar c=Calendar.getInstance();
							c.add(Calendar.DATE, -(param.getParamSince()));
							sinceDate=new Date(sdf.format(c.getTime()));
						}
						
						if(param.getParamUntil()>0)
						{
							Calendar c=Calendar.getInstance();
							c.add(Calendar.DATE, -(param.getParamUntil()));
							untilDate=new Date(sdf.format(c.getTime()));
						}
						returnResponse=fbconnect.getNewsFeed(userToken,param.paramLimit,sinceDate,untilDate);
					}
					//em.createQuery("select a from ConfigParameter a where a.curUser="+user.getId(),ConfigParameter.class).getSingleResult();
					
				}
				catch (javax.persistence.NoResultException e)
				{
					System.out.println("No parameter has been set for This User");
					returnResponse=fbconnect.getNewsFeed(userToken,0,null,null);
				}

				if(returnResponse.contains("Please Go to Refresh Page for Refreshing the AccessToken and Try Again"))
				{
					acknowledgment="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
					query.get(0).knowStat=true;
					query.get(0).merge()
				}
				else
				{
					if (returnResponse.size>0)
					{
						List<FBNewsFeed> query1=FBNewsFeed.all().filter("curUser=?",user).fetch()
						//em.createQuery("select a from FBNewsFeed a where a.curUser="+user.getId(), FBNewsFeed.class).getResultList()
						List<String> str=new ArrayList<String>();
						if(query1.size()>0)
						{
							for(int i=0;i<query1.size();i++)
							{
								query1.get(i).remove();
								//str.add(query1.get(i).getFeedid());
							}
						}
						for(int i=0;i<returnResponse.size();i+=6)
						{
							newsFeed=new FBNewsFeed();
							newsFeed.feedid=returnResponse.get(i);
							newsFeed.name=returnResponse.get(i+1);
							newsFeed.contentdate=returnResponse.get(i+2);
							newsFeed.type=returnResponse.get(i+3);
							if(returnResponse.get(i+4).equals("null"))
							{
								newsFeed.link="N/A"
							}
							else
							{
								newsFeed.link=returnResponse.get(i+4)
							}

							if(returnResponse.get(i+5).equals("null"))
							{
								newsFeed.message="N/A"
							}
							else
							{
								newsFeed.message=returnResponse.get(i+5)
							}

							newsFeed.curUser=user;
							newsFeed.persist();


						}

						size1=(returnResponse.size()/5);
						acknowledgment="You have "+size1+" new News Feed Content";
					}

					else
					{
						acknowledgment="There is No new Update / Some Problem";
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

	@Transactional
	public String getPageFB(User user)
	{
		int size1=0;
		ArrayList returnResponse=new ArrayList();
		try
		{
			SocialNetworking sn=SocialNetworking.all().filter("name=?","Facebook").fetchOne();
			List<PersonalCredential> query=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			if(query.isEmpty())
			{
				acknowledgment="Please Login First";
			}
			else
			{
				userToken=query.get(0).getUserToken();
				FBPages page=new FBPages();
				fbconnect=new FacebookConnectionClass();
				returnResponse=fbconnect.getPageDetail(userToken);
				if(returnResponse.contains("Please Go to Refresh Page for Refreshing the AccessToken and Try Again"))
				{
					acknowledgment="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
					query.get(0).knowStat=true;
					query.get(0).merge()
				}
				else
				{
					if (returnResponse.size>0)
					{
						size1=(returnResponse.size()/4);
						List<FBPages> query1=FBPages.all().filter("curUser=?",user).fetch()
						//em.createQuery("select a from FBPages a where a.curUser="+user.getId(), FBPages.class).getResultList()
						List<String> str=new ArrayList<String>();
						for(int i=0;i<query1.size();i++)
						{
							str.add(query1.get(i).getPageId());
						}
						for(int i=0;i<returnResponse.size();i+=4)
						{
							if(!str.contains(returnResponse.get(i).toString()))
							{
								page=new FBPages();
								page.pageId=returnResponse.get(i);
								page.name=returnResponse.get(i+1);
								page.pageUrl=returnResponse.get(i+2);
								page.username=returnResponse.get(i+3);
								page.curUser=user;
								page.persist();
							}
						}
						acknowledgment="You Own "+size1+" Page(s)";
					}
					else
					{
						acknowledgment="You do not OWN page(s)";
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

	@Transactional
	public String postPageContent(User user,FBPages page,FBPagePost content)
	{
		try
		{
			SocialNetworking sn=SocialNetworking.all().filter("name=?","Facebook").fetchOne();
			List<PersonalCredential> query=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			if(query.isEmpty())
			{
				acknowledgment="Please Login First";
			}
			else
			{
				FBPagePost pagePost=new FBPagePost();
				userToken=query.get(0).getUserToken();
				fbconnect=new FacebookConnectionClass();
				String value=content.content;
				String pageId=page.getPageId();
				acknowledgment=fbconnect.postToPgae(value,pageId,userToken);
				/*	if(!acknowledgment.startsWith("[a-zA-Z]"))
				 {
				 JPA.runInTransaction
				 {
				 pagePost=new FBPagePost();
				 pagePost.content=value;
				 pagePost.curUser=user;
				 pagePost.page=page;
				 pagePost.postedTime=content.postedTime;
				 pagePost.acknowledgment=acknowledgment;
				 pagePost.merge();
				 }
				 }*/
			}
		}
		catch (Exception e)
		{
			acknowledgment=e.getMessage();
			e.printStackTrace();
		}
		return acknowledgment;
	}

	@Transactional
	public String deletePagePost(User user,List contentId)
	{
		FBPagePost postMsg=new FBPagePost();
		String sntype;
		try
		{
			SocialNetworking sn=SocialNetworking.all().filter("name=?","Facebook").fetchOne();
			List<PersonalCredential> query=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			if(query.isEmpty())
			{
				acknowledgment="Please Login First";
			}
			else
			{
				sntype=query.get(0).getSnType().getName();
				userToken=query.get(0).getUserToken();
				for(int i=0;i<contentId.size();i++)
				{

					postMsg=new FBPagePost();
					postMsg=FBPagePost.all().filter("id=?",+contentId.get(i)).fetchOne();
					fbconnect=new FacebookConnectionClass()
					Boolean statusDeleted=fbconnect.delete(postMsg.getAcknowledgment(),userToken);
					if(statusDeleted)
					{
						postMsg.remove();
						acknowledgment="Content has been Removed Successfully!";
					}
					else
					{
						acknowledgment="Its not Existed or/There is Some Error Please Try after Sometimes!!"
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


	@Transactional
	public String getPageCommentsFB(User user,String pmsg)
	{
		ArrayList returnResponse=new ArrayList();
		String contentId;
		try
		{
			contentId=pmsg;
			SocialNetworking sn=SocialNetworking.all().filter("name=?","Facebook").fetchOne();
			List<PersonalCredential> query=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			FBPagePost status=FBPagePost.all().filter("acknowledgment=?",contentId).fetchOne();
			if(query.isEmpty())
			{
				acknowledgment="Please Login First"
			}
			else
			{
				userToken=query.get(0).getUserToken()
				fbconnect=new FacebookConnectionClass()
				returnResponse=fbconnect.getPageComments(userToken,contentId);
				if(returnResponse.contains("Please Go to Refresh Page for Refreshing the AccessToken and Try Again"))
				{
					acknowledgment="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
					query.get(0).knowStat=true;
					query.get(0).merge();
				}
				else
				{
					FBPageComment comnt=new FBPageComment()
					println(returnResponse.size())
					List<FBPageComment> query1=FBPageComment.all().filter("curUser=?",user).fetch();
					List<String> str=new ArrayList<String>();
					List<String> userId=new ArrayList<String>();
					for(int i=0;i<query1.size();i++)
					{
						str.add(query1.get(i).getCommentid());
					}

					for(int i=0;i<returnResponse.size();i+=5)
					{
						if(!str.contains(returnResponse.get(i).toString()))
						{
							comnt=new FBPageComment()
							//ImportContact impcnt=em.createQuery("select a from ImportContact a where a.userId='"+returnResponse.get(i+1)+"' and curUser='"+user.getId()+"'", ImportContact.class).getSingleResult()
							comnt.commentid=returnResponse.get(i);
							comnt.contentid=status;
							comnt.from_user=returnResponse.get(i+1)
							comnt.comment=returnResponse.get(i+2)
							comnt.commentTime=returnResponse.get(i+3)
							comnt.commentLikes=returnResponse.get(i+4)
							comnt.curUser=user
							comnt.merge();
							//em.lock(comnt,OptimisticLockType.NONE)
						}
					}
					acknowledgment="Comment Will only Display if there is Some Comment on Content";
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		return acknowledgment;
	}

	@Transactional
	public String deleteInBox(User user,List contentId)
	{
		FBInbox inboxMsg=new FBInbox();
		String sntype;
		try
		{
			SocialNetworking sn=SocialNetworking.all().filter("name=?","Facebook").fetchOne();
			List<PersonalCredential> query=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			if(query.isEmpty())
			{
				acknowledgment="Please Login First";
			}
			else
			{
				sntype=query.get(0).getSnType().getName();
				userToken=query.get(0).getUserToken();
				for(int i=0;i<contentId.size();i++)
				{

					inboxMsg=new FBInbox();
					inboxMsg=FBInbox.all().filter("id=?",contentId.get(i)).fetchOne();
					fbconnect=new FacebookConnectionClass()
					Boolean statusDeleted=fbconnect.delete(inboxMsg.getMsgId(),userToken);
					if(statusDeleted)
					{
						inboxMsg.remove();
						acknowledgment="Content has been Removed Successfully!";
					}
					else
					{
						acknowledgment="Its not Existed or/There is Some Error Please Try after Sometimes!!"
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

	@Transactional
	public String getLike(User user,Long contentId)
	{
		//FBNewsFeed newsFeed=new FBNewsFeed();
		String sntype;
		try
		{
			SocialNetworking sn=SocialNetworking.all().filter("name=?","Facebook").fetchOne();
			List<PersonalCredential> query=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			if(query.isEmpty())
			{
				acknowledgment="Please Login First";
			}
			else
			{
				sntype=query.get(0).getSnType().getName();
				userToken=query.get(0).getUserToken();
				//	println("select a from FBNewsFeed a where a.id="+contentId+" and a.curUser="+user.getId());
				FBNewsFeed newsFeed=FBNewsFeed.all().filter("id=? and curUser=?",contentId,user).fetchOne();
				fbconnect=new FacebookConnectionClass()
				Boolean status=fbconnect.postLike(newsFeed.getFeedid(),userToken);
				if(status)
				{
					newsFeed.merge();
					acknowledgment="Content has been Liked Successfully!";
				}
				else
				{
					acknowledgment="Its not Existed or/There is Some Error Please Try after Sometimes!!"
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
	public String postCommmentonStatus(User user,String contentId,String commentContent)
	{
		ArrayList returnResponse=new ArrayList();
		String sntype;
		try
		{
			SocialNetworking sn=SocialNetworking.all().filter("name=?","Facebook").fetchOne();
			List<PersonalCredential> query=PersonalCredential.all().filter("userId=? and snType=?",user,sn).fetch();
			if(query.isEmpty())
			{
				acknowledgment="Please Login First";
			}
			else
			{
				sntype=query.get(0).getSnType().getName();
				userToken=query.get(0).getUserToken();
				fbconnect=new FacebookConnectionClass();
				returnResponse=fbconnect.postStatusComemnt(userToken, contentId, commentContent);
				if(returnResponse.contains("Please Go to Refresh Page for Refreshing the AccessToken and Try Again"))
				{
					acknowledgment="Please Go to Refresh Page for Refreshing the AccessToken and Try Again";
					query.get(0).knowStat=true;
					query.get(0).merge()
				}
				else
				{
					if(returnResponse.size()>0)
					{
						acknowledgment="Comment Will only Display if there is Some Comment on Content";
					}
					else
					{
						acknowledgment="Some problem is there"
					}
				}
				/*PostMessage pmsg=em.createQuery("select a from PostMessage a where a.acknowledgment='"+contentId+"'",PostMessage.class).getSingleResult();
				 Comment comnt=new Comment()
				 List<Comment> query1=em.createQuery("select a from Comment a where a.curUser='"+user.getId()+"'", Comment.class).getResultList()
				 List<String> str=new ArrayList<String>();
				 List<String> userId=new ArrayList<String>();
				 for(int i=0;i<query1.size();i++)
				 {
				 str.add(query1.get(i).getCommentid());
				 }
				 for(int i=0;i<returnResponse.size();i+=3)
				 {
				 if(!str.contains(returnResponse.get(i).toString()))
				 {
				 println("Inside")
				 ImportContact fbcntc=new ImportContact()
				 fbcntc.userId=returnResponse.get(i)
				 fbcntc.name=returnResponse.get((i+1))
				 fbcntc.link=returnResponse.get((i+2))
				 fbcntc.sntype=sntype;
				 fbcntc.curUser=user;
				 tx.begin()
				 fbcntc.persist();
				 tx.commit()
				 /*ImportContact impcnt=em.createQuery("select a from ImportContact a where a.userId='"+returnResponse.get(i)+"' and curUser='"+user.getId()+"'",ImportContact.class).getSingleResult()
				 comnt=new Comment()
				 comnt.commentid=returnResponse.get(i+3);
				 comnt.contentid=pmsg;
				 comnt.from_user=impcnt
				 comnt.comment=commentContent
				 comnt.commentTime=new DateTime();
				 comnt.commentLikes="0";
				 comnt.curUser=user
				 comnt.merge();
				 println("Leaving")
				 }
				 }*/



			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}

	/*public String postPagePostComment(User user,String contentId,String commentContent)
	 {
	 ArrayList returnResponse=new ArrayList();
	 String sntype;
	 try
	 {
	 EntityManager em=JPA.em();
	 EntityTransaction tx=em.getTransaction();
	 SocialNetworking sn=em.createQuery("select a from SocialNetworking a where a.name='Facebook'", SocialNetworking.class).getSingleResult();
	 List<PersonalCredential> query=em.createQuery("select a from PersonalCredential a where a.userId='"+user.getId()+"'and snType="+sn.getId(), PersonalCredential.class).getResultList()
	 if(query.isEmpty())
	 {
	 acknowledgment="Please Login First";
	 }
	 else
	 {
	 sntype=query.get(0).getSnType().getName();
	 userToken=query.get(0).getUserToken();
	 fbconnect=new FacebookConnectionClass();
	 returnResponse=fbconnect.postStatusComemnt(userToken, contentId, commentContent);
	 if(returnResponse.size()>0)
	 {
	 acknowledgment="Comment Will only Display if there is Some Comment on Content";
	 }
	 else
	 {
	 acknowledgment="Some problem is there"
	 }
	 /*PostMessage pmsg=em.createQuery("select a from PostMessage a where a.acknowledgment='"+contentId+"'",PostMessage.class).getSingleResult();
	 Comment comnt=new Comment()
	 List<Comment> query1=em.createQuery("select a from Comment a where a.curUser='"+user.getId()+"'", Comment.class).getResultList()
	 List<String> str=new ArrayList<String>();
	 List<String> userId=new ArrayList<String>();
	 for(int i=0;i<query1.size();i++)
	 {
	 str.add(query1.get(i).getCommentid());
	 }
	 for(int i=0;i<returnResponse.size();i+=3)
	 {
	 if(!str.contains(returnResponse.get(i).toString()))
	 {
	 println("Inside")
	 ImportContact fbcntc=new ImportContact()
	 fbcntc.userId=returnResponse.get(i)
	 fbcntc.name=returnResponse.get((i+1))
	 fbcntc.link=returnResponse.get((i+2))
	 fbcntc.sntype=sntype;
	 fbcntc.curUser=user;
	 tx.begin()
	 fbcntc.persist();
	 tx.commit()
	 /*ImportContact impcnt=em.createQuery("select a from ImportContact a where a.userId='"+returnResponse.get(i)+"' and curUser='"+user.getId()+"'",ImportContact.class).getSingleResult()
	 comnt=new Comment()
	 comnt.commentid=returnResponse.get(i+3);
	 comnt.contentid=pmsg;
	 comnt.from_user=impcnt
	 comnt.comment=commentContent
	 comnt.commentTime=new DateTime();
	 comnt.commentLikes="0";
	 comnt.curUser=user
	 comnt.merge();
	 println("Leaving")
	 }
	 }
	 }
	 }
	 catch (Exception e)
	 {
	 e.printStackTrace();
	 }
	 }*/
}

