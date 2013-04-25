package com.axelor.sn.web
import java.util.Formatter.DateTime;
import com.axelor.auth.db.User
//import com.axelor.contact.db.Contact
import com.axelor.db.*
import javax.inject.Inject
import javax.persistence.EntityManager
import javax.persistence.EntityTransaction;

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
import com.axelor.sn.service.SNFBService
import com.google.inject.matcher.Matchers.Returns;


class FbPersonSearchController
{
	@Inject
	SNFBService service//=new SNFBService();

	String apikey;
	String apisecret;
	String userName,password
	ArrayList al=new ArrayList();



	void searchPerson(ActionRequest request,ActionResponse response)
	{
		println("Search Person"+request.context.toString())
		String ack,userToken;
		try
		{/**			println(request.context.toString())
			List lval=request.context.values().asList()
			User user=lval.get(3)
			user.getId()
			println(request.context.toString())**/
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"))
			EntityManager em=JPA.em()
			EntityTransaction tx=em.getTransaction()
			tx.begin()
			List<PersonalCredential> query=em.createQuery("select a from PersonalCredential a where a.userId="+user.getId(), PersonalCredential.class).getResultList()
			if(query.isEmpty())
			{
				ack="Please Login First"
				response.flash=ack

			}
			else
			{	//println(request.context.toString())
				userToken=query.get(0).getUserToken()
				def context = request.context as SearchPerson
				String param = context.searchparam
				ArrayList val=service.searchPerson(param,userToken)
				if(val.size()>0)
				{
					response.setValues(["userid":val.getAt(0),"firstname":val.getAt(1),"lastname":val.getAt(2),"gender":val.getAt(3),"link":val.getAt(4)])
				}
				else
				{
					response.flash="No Search Found!!! Try Again!!"
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace()
			ack=e.getMessage()
			response.flash=ack

		}
	}



	void say(ActionRequest request, ActionResponse response)
	{
		println("called!!!!")
		String str="Event for grid"
		/*println(request.context.toString())
		List lval=request.context.values().asList()
		def context= request.context as ApplicationCredentials
		apikey=context.apikey
		apisecret=context.apisecret
		println("API Key  "+apikey)
		println("API Secret  "+apisecret)
		User user=lval.get(2)
		al.add(apikey);
		al.add(apisecret);
		println(user.getId())
		println(user.getName())
		response.setValues("userId":user.getName())
		println("ListValie"+al.get(0))*/
		response.values=["testVal": str]

	}

	void getAuthentication(ActionRequest request,ActionResponse response)
	{

		println("Its been Called")
		def context=request.context as PersonalCredential
		SocialNetworking sn=context.snType
		println(context.snType)
		println (sn.getId())
		ApplicationCredentials acc=new ApplicationCredentials();
		EntityManager em=JPA.em()
		EntityTransaction tx=em.getTransaction()
		tx.begin()
		List<ApplicationCredentials> query=em.createQuery("select a from ApplicationCredentials a where a.snType="+sn.getId(), ApplicationCredentials.class).getResultList()
		println("Executed")
		tx.commit()
		for(int i=0;i<query.size();i++)
		{
			query.get(i).getApikey()
			query.get(i).getApisecret()
			apikey=query.get(i).getApikey()
			apisecret=query.get(i).getApisecret()
		}
		println ("OUTSIDE Values")
		userName=context.snUsername
		password=context.password
		response.values=["userToken":service.getUserToken(apikey, apisecret, userName, password)]
	}

	void getVal(ActionRequest request, ActionResponse response)
	{
		println("getVal() is Called  "+al.size())
		for(int i=0;i<al.size();i++)
		{
			println("API Key  "+al.get(i))
		}
	}
	void getLoginAuthorization(ActionRequest request,ActionResponse response)
	{
		def context=request.context
		println(context.values)
	}




	void fbPostStatus(ActionRequest request,ActionResponse response)
	{
		println("STATUS::::"+request.context.toString())
		String ack,userToken;
		try
		{	/**println(request.context.toString())
			List lval=request.context.values().asList()
			User user=lval.get(4)
			user.getId()**/
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"))
			println(request.context.toString())
			EntityManager em=JPA.em()
			EntityTransaction tx=em.getTransaction()
			tx.begin()
			List<PersonalCredential> query=em.createQuery("select a from PersonalCredential a where a.userId="+user.getId(), PersonalCredential.class).getResultList()
			if(query.isEmpty())
			{
				ack="Please Login First"
				response.flash=ack;

			}
			else
			{
				userToken=query.get(0).getUserToken()
				def context=request.context as PostMessage
				ack=service.postStatus(context.content,userToken)
				response.values=["acknowledgment":ack];
			}
			println("Executed")
			tx.commit()
		}
		catch(Exception e)
		{
			e.printStackTrace()
			ack=e.getMessage()
		}

	}


	void postEvent(ActionRequest request,ActionResponse response)
	{
		println("Events::::"+request.context.toString())
		String ack,userToken;
		try
		{	/**List lval=request.context.values().asList()
			User user=lval.get(6)
			user.getId()**/
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"))
			println(request.context.toString())
			EntityManager em=JPA.em()
			EntityTransaction tx=em.getTransaction()
			tx.begin()
			List<PersonalCredential> query=em.createQuery("select a from PersonalCredential a where a.userId="+user.getId(), PersonalCredential.class).getResultList()
			if(query.isEmpty())
			{
				ack="Please Login First"
				response.flash=ack

			}
			else
			{	
				userToken=query.get(0).getUserToken()
				def context=request.context as PostEvent
				LocalDate startDate=context.startdate
				Date startD=startDate.toDateTimeAtStartOfDay().toDate()
				LocalDate endDate=context.enddate
				Date endD=endDate.toDateTimeAtCurrentTime().toDate()
				context.startdate
				context.enddate
				context.occession
				context.location
				ack=service.fbPostEvent(startD,endD, context.occession, context.location,userToken)
				response.values=["acknowledgment":ack];
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
		String ack,userToken,sntype;
		try
		{	/**List lval=request.context.values().asList()
			User user=lval.get(2)
			user.getId()*/
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"))
			println(request.context.toString())
			EntityManager em=JPA.em()
			EntityTransaction tx=em.getTransaction()
			tx.begin()
			List<PersonalCredential> query=em.createQuery("select a from PersonalCredential a where a.userId="+user.getId(), PersonalCredential.class).getResultList()
			if(query.isEmpty())
			{
				ack="Please Login First"
				response.flash=ack

			}
			else
			{	//println(request.context.toString())
				sntype=query.get(0).getSnType().getName()
				userToken=query.get(0).getUserToken()
				def context=request.context as ImportContact

				ImportContact fbcntc=new ImportContact()
				ArrayList returnResponse=service.importContactsFB(userToken)
				println("RESPONSE SIZE "+returnResponse.size())
				int j=(returnResponse.size())/3;
				if(returnResponse.size()>0)
				{
					List<ImportContact> query1=em.createQuery("select a from ImportContact a", ImportContact.class).getResultList()
					println(query1.size())
					for(int i=0;i<returnResponse.size();i+=3)
					{

						fbcntc=new ImportContact()
						JPA.runInTransaction
						{
							//							if(query1.get(j).getUserid().equals(returnResponse.get(i)))
							//							{
							//								println("Eliminating Duplication of Row")
							//							}

							//							else
							//							{
							fbcntc.userId=returnResponse.get(i)
							fbcntc.name=returnResponse.get((i+1))
							fbcntc.link=returnResponse.get((i+2))
							fbcntc.sntype=sntype;
							fbcntc.curUser=user.getId();
							em.persist(fbcntc)
							tx.commit()
							//Here we Already Declared EntityManager so now we have to go with tht only can't use inbuilt methods
							/*
							 JPA.persist(fbcntc)
							 JPA.save(fbcntc)
							 fbcntc.save()
							 fbcntc.persist();
							 */	
							//							}
						}
						//j++
					}
					response.flash="Contacts Imported Successfully Please Go back and Press Refresh"
				}
				else
				{
					response.flash="There is Some problem please Try Later!!"
				}
			}
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
		String ack,userToken,sntype,statusId;
		try
		{
			List keys=request.context.keySet().asList()
			List values=request.context.values().asList()
			User user=values.get(keys.indexOf("__user__"))
			PostMessage pmsg=values.get(keys.indexOf("contentid"))
			statusId=pmsg.getAcknowledgment()
			EntityManager em=JPA.em()
			EntityTransaction tx=em.getTransaction()
			tx.begin()
			List<PersonalCredential> query=em.createQuery("select a from PersonalCredential a where a.userId="+user.getId(), PersonalCredential.class).getResultList()
			if(query.isEmpty())
			{
				ack="Please Login First"
				response.flash=ack

			}
			else
			{
				userToken=query.get(0).getUserToken()
				def context=request.context as Comment
				Comment comnt=new Comment()
				ArrayList returnResponse=new ArrayList()
				ArrayList a1=new ArrayList();
				ArrayList a2=new ArrayList();
				ArrayList a3=new ArrayList();
				ArrayList a4=new ArrayList();
				returnResponse=service.getCommentsFB(userToken, statusId);
				println(returnResponse.size())
				for(int i=0;i<returnResponse.size();i+=4)
				{
					JPA.runInTransaction
					{
						comnt=new Comment()

						try
						{
							ImportContact impcnt=em.createQuery("select a from ImportContact a where a.userId='"+returnResponse.get(i)+"' and curUser='"+user.getId()+"'", ImportContact.class).getSingleResult()
							comnt.contentid=pmsg;
							comnt.from_user=impcnt
							comnt.comment=returnResponse.get(i+1)
							comnt.commentTime=returnResponse.get(i+2)
							comnt.commentLikes=returnResponse.get(i+3)
							comnt.curUser=user
							em.persist(comnt)
							tx.commit()
						}
						catch(Exception e)
						{
							e.printStackTrace()
						}
						
						println(returnResponse.get(i))
						println(returnResponse.get(i+1))
						println(returnResponse.get(i+2))
						println(returnResponse.get(i+3))
						
					}
				}
			}
		}
		catch (Exception e)
		{
			response.flash=e.toString()
			e.printStackTrace()
		}
	}


	ArrayList getContactsOfABS(ActionRequest request,ActionResponse response)
	{
		try
		{
			ArrayList val=service.getABSContacts()
			for(int i =0;i<val.size();i++)
			{
				println(val.get(i))
			}
		}
		catch(Exception e)
		{
			e.printStackTrace()
		}
	}



	//Call when need to search for Person onChange of searchforperson.xml
	//	/**
	//	 *
	//	 * @param request
	//	 * @param response
	//	 * it will return response which will give details
	//	 */
	//	void onPress(ActionRequest request,ActionResponse response)
	//	{
	//		def context = request.context as SearchPerson
	//		FacebookConnectorFetch fcf=new FacebookConnectorFetch()
	//		String param = context.searchparam
	//		SearchResult sr=new SearchResult()
	//		ArrayList val=fcf.fetchObjectOfPerson(param)
	//		if(val.size()>0)
	//		{
	//			sr.userid=val.getAt(0)
	//			sr.firstname=val.getAt(1)
	//			sr.lastname=val.getAt(2)
	//			sr.gender=val.getAt(3)
	//			sr.link=val.getAt(4)
	//			response.values=sr
	//
	//		}
	//	}



	//	/**
	//	 *
	//	 * @param request
	//	 * @param response
	//	 * method used for Post the Status on Facebook
	//	 */
	//	void postStatus(ActionRequest request, ActionResponse response)
	//	{
	//		def context=request.context as PostMessage
	//		PostMessage pmsg=new PostMessage()
	//		FacebookConnectorPost fcpost=new FacebookConnectorPost()
	//		String content=context.content
	//		pmsg.content=content
	//		pmsg.acknowledgment=fcpost.publishMessage(content)
	//		response.flash=pmsg.acknowledgment
	//		response.values=pmsg
	//
	//	}

	//	void postEvent(ActionRequest request,ActionResponse response)
	//	{
	//		try
	//		{
	//			def context=request.context as PostEvent
	//			PostEvent postEvent=new PostEvent()
	//			FacebookConnectorPost fcpost=new FacebookConnectorPost()
	//			LocalDate startDate=context.startdate
	//			Date startD=startDate.toDateTimeAtStartOfDay().toDate()
	//			println (startD)
	//			LocalDate endDate=context.enddate
	//			Date endD=endDate.toDateTimeAtCurrentTime().toDate()
	//			println(endD)
	//			postEvent.startdate=context.startdate
	//			postEvent.enddate=context.enddate
	//			postEvent.occession=context.occession
	//			postEvent.location=context.location
	//			postEvent.acknowledgment=fcpost.publishEvent(startD,endD, context.occession, context.location)
	//			response.flash=postEvent.acknowledgment
	//			response.values=postEvent
	//		}
	//		catch(Exception e)
	//		{
	//			e.printStackTrace()
	//		}
	//
	//	}


	//	void getAllContactsFB(ActionRequest request,ActionResponse response)
	//	{
	//		try
	//		{
	//			def context=request.context as FBImportContact
	//			FacebookConnectorFetch fcf=new FacebookConnectorFetch()
	//			FBImportContact fbcntc=new FBImportContact()
	//			ArrayList returnResponse=fcf.getListOfFriends()
	//			println(returnResponse.size())
	//			ArrayList al=new ArrayList()
	//			ArrayList al1=new ArrayList()
	//			ArrayList al2=new ArrayList()
	//			println (new Date())
	//			for(int i=0;i<returnResponse.size();i+=3)
	//			{
	//				JPA.runInTransaction
	//				{
	//					fbcntc=new FBImportContact()
	//					al.add(returnResponse.get(i))
	//					fbcntc.userid=returnResponse.get(i)
	//					println(fbcntc.userid)
	//					al1.add(returnResponse.get((i+1)))
	//					fbcntc.name=returnResponse.get((i+1))
	//					println(fbcntc.name)
	//					al2.add(returnResponse.get((i+2)))
	//					fbcntc.link=returnResponse.get((i+2))
	//					fbcntc.persist()
	//					println(fbcntc.link)
	//				}
	//			}
	//			response.flash="Contacts Imported Successfully Please Go back and Press Refresh"
	//		}
	//		catch(Exception e)
	//		{
	//			response.flash=e.toString()
	//			e.printStackTrace()
	//		}
	//	}
}
