package com.axelor.sn.fb;

import com.axelor.sn.db.PersonalCredential;
import com.axelor.sn.db.SocialNetworking;
import com.google.inject.Inject;
import com.restfb.DefaultFacebookClient;

public class FacebookUtilityClass {

	@Inject
	FacebookConnectionClass fbconnect;

	public boolean isFBLivecheck(com.axelor.auth.db.User user) {
		boolean liveStatus = false;
		SocialNetworking sn = SocialNetworking.all()
				.filter("name = ?", "facebook").fetchOne();
		PersonalCredential credential = PersonalCredential.all()
				.filter("userId = ? and snType = ?", user, sn).fetchOne();

		if (credential != null) {
			if (fbconnect.isNullFBObj(fbconnect.facebookClient)) {
				fbconnect.facebookClient = new DefaultFacebookClient(
						credential.getUserToken());
				liveStatus = true;
			}
			else
				liveStatus = true;
		}

		return liveStatus;
	}

}
