package com.axelor.sn.twitter;
import com.axelor.auth.db.User;
import com.google.inject.servlet.SessionScoped;


@SessionScoped
public class TWTVerficationBean 
{
	public String pinVerification;
	public User user;

	public void setUser(User user)
	{
		this.user=user;
	}

	public User getUser()
	{
		return user;
	}

	public void setPinVerification(String pinVerification)
	{
		this.pinVerification=pinVerification;
	}

	public String getPinVerification()
	{
		return pinVerification;
	}

}
