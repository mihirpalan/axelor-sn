package com.axelor.sn.likedin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.ws.rs.Consumes;

import org.joda.time.DateTime;

import com.google.code.linkedinapi.client.LinkedInApiClient;
import com.google.code.linkedinapi.client.LinkedInApiClientFactory;
import com.google.code.linkedinapi.client.enumeration.NetworkUpdateType;
import com.google.code.linkedinapi.client.enumeration.ProfileField;
import com.google.code.linkedinapi.client.oauth.LinkedInAccessToken;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthService;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthServiceFactory;
import com.google.code.linkedinapi.client.oauth.LinkedInRequestToken;
import com.google.code.linkedinapi.schema.Connections;
import com.google.code.linkedinapi.schema.Person;
import com.google.code.linkedinapi.schema.Update;
import com.google.code.linkedinapi.schema.UpdateComment;
import com.google.code.linkedinapi.schema.UpdateComments;
import com.google.code.linkedinapi.schema.Updates;

public class LinkedinConnectionClass {
	
	static LinkedInOAuthService oauthService=null;
	LinkedInRequestToken requestToken=null;
	static LinkedInApiClientFactory factory = null;
	LinkedInApiClient client=null;
	final Set<ProfileField> setProfileFields = EnumSet.of(ProfileField.ID, ProfileField.FIRST_NAME,
			ProfileField.LAST_NAME, ProfileField.PUBLIC_PROFILE_URL);
	final Set<NetworkUpdateType> networkUpdateType = EnumSet.of(NetworkUpdateType.SHARED_ITEM);
	
	public String getUrl(String consumerKey, String consumerSecret, String redirectUrl, String userName) throws IOException {
		String authUrl = "";
//		try {
			oauthService = LinkedInOAuthServiceFactory.getInstance().createLinkedInOAuthService(consumerKey, consumerSecret);
			requestToken = oauthService.getOAuthRequestToken(redirectUrl);
			factory = LinkedInApiClientFactory.newInstance(consumerKey, consumerSecret);
			authUrl = requestToken.getAuthorizationUrl();
//		} catch (LinkedInOAuthServiceException e) {
//			throw new LinkedInOAuthServiceException("Problem with Linkedin Authorization Service");
//		} catch (LinkedInApiClientException e) {
//			throw new LinkedInApiClientException("Problem with Linkedin Website");
//		}
		File temp = new File(userName+".txt");
		ObjectOutputStream outStream = new ObjectOutputStream( new FileOutputStream(temp));
		outStream.writeObject(requestToken);
		outStream.close();
		return authUrl;
	}

	public String getUserToken(String verifier, String userName) throws IOException, ClassNotFoundException {
		File temp = new File(userName+".txt");
		ObjectInputStream inStream = new ObjectInputStream(new FileInputStream(temp));
		requestToken=(LinkedInRequestToken)inStream.readObject();
		temp.delete();
		inStream.close();
		LinkedInAccessToken accessToken = oauthService.getOAuthAccessToken(requestToken, verifier);
		String userDetails = accessToken.getToken() + "=" + accessToken.getTokenSecret();
		client = factory.createLinkedInApiClient(accessToken);
		Person profile = client.getProfileForCurrentUser(setProfileFields);
		userDetails += "="+ profile.getFirstName() + " " + profile.getLastName();
		return userDetails;
	}
	
	@SuppressWarnings("rawtypes")
	public ArrayList<HashMap> getUserConnections(String userToken, String userTokenSecret) {
		client = factory.createLinkedInApiClient(userToken, userTokenSecret);
		Person profile = client.getProfileForCurrentUser(setProfileFields);
		Connections connections = client.getConnectionsForCurrentUser(setProfileFields);
		
		HashMap<String, String> userDetails = new HashMap<String, String>();
		userDetails.put("userId", profile.getId());
		userDetails.put("userName", profile.getFirstName() + " " + profile.getLastName());
		userDetails.put("userLink", profile.getPublicProfileUrl());
		
		ArrayList<HashMap> users = new ArrayList<HashMap>();
		users.add(userDetails);
		
		for (Person person : connections.getPersonList()) {
			userDetails = new HashMap<String, String>();
			userDetails.put("userId", person.getId());
			userDetails.put("userName", person.getFirstName() + " " + person.getLastName());
			userDetails.put("userLink", person.getPublicProfileUrl());
			users.add(userDetails);
		}
		return users;
	}
	
	public void sendMessage(String userToken, String userTokenSecret, ArrayList<String> lstUserId, String subject, String message) {
		client = factory.createLinkedInApiClient(userToken, userTokenSecret);
		client.sendMessage(lstUserId, subject, message);
	}
	
	public String updateStatus(String userToken, String userTokenSecret, String content) {
		client = factory.createLinkedInApiClient(userToken, userTokenSecret);
		client.updateCurrentStatus(content);

		Updates upd = client.getUserUpdates(networkUpdateType, 0, 1).getUpdates();
		Iterator<Update> itr = upd.getUpdateList().iterator();
		Update update = itr.next();
		
		String updateKeyTime = update.getUpdateKey() + ":" + update.getTimestamp();
		return updateKeyTime;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ArrayList<HashMap> getComments(String userToken, String userTokenSecret, String contentId) {
		client = factory.createLinkedInApiClient(userToken, userTokenSecret);
		
		UpdateComments updateComments = client.getNetworkUpdateComments(contentId);
		Iterator<UpdateComment> itr = updateComments.getUpdateCommentList().iterator();
		UpdateComment comment = null;
		ArrayList<HashMap> commentList = new ArrayList<HashMap>();
		while (itr.hasNext()) {
			comment = itr.next();
			HashMap comments  = new HashMap();
			comments.put("commentId", comment.getId());
			comments.put("comment", comment.getComment());
			comments.put("commentTime", comment.getTimestamp());
			comments.put("fromUser",comment.getPerson().getId());
			commentList.add(comments);
		}
		return commentList;
	}
}
