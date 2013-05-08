package com.axelor.sn.likedin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EnumSet;
import java.util.Set;

import com.google.code.linkedinapi.client.LinkedInApiClient;
import com.google.code.linkedinapi.client.LinkedInApiClientFactory;
import com.google.code.linkedinapi.client.enumeration.ProfileField;
import com.google.code.linkedinapi.client.oauth.LinkedInAccessToken;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthService;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthServiceFactory;
import com.google.code.linkedinapi.client.oauth.LinkedInRequestToken;
import com.google.code.linkedinapi.schema.Person;

public class LinkedinConnectionClass {
	
	static LinkedInOAuthService oauthService=null;
	LinkedInAccessToken accessToken=null;
	LinkedInRequestToken requestToken=null;
	static LinkedInApiClientFactory factory = null;
	LinkedInApiClient client=null;
	final Set<ProfileField> setProfileFields = EnumSet.of(ProfileField.ID, ProfileField.FIRST_NAME,
			ProfileField.LAST_NAME, ProfileField.PUBLIC_PROFILE_URL);
	
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
		accessToken = oauthService.getOAuthAccessToken(requestToken, verifier);
		String userDetails = accessToken.getToken() + "=" + accessToken.getTokenSecret();
		client = factory.createLinkedInApiClient(accessToken);
		Person profile = client.getProfileForCurrentUser(setProfileFields);
		userDetails += "="+ profile.getFirstName() + " " + profile.getLastName();
		return userDetails;
	}
	
	public String getName(String token, String tokenSecret) {
		client = factory.createLinkedInApiClient(token, tokenSecret);
		Person profile = client.getProfileForCurrentUser(EnumSet.of(ProfileField.FIRST_NAME, ProfileField.LAST_NAME));
		String name = profile.getFirstName() + " " + profile.getLastName();
		return name;
	}
}
