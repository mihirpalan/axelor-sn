package com.axelor.web;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import com.axelor.rpc.ActionResponse;
import com.axelor.sn.service.SNService;

@Path("/linkedin")
public class Linkedin {
	
	@Inject SNService service;
	@GET
	@Path("{id}")
	public String get(@PathParam("id") Long id, @QueryParam("oauth_verifier") String verifier) throws Exception 
	{
		String str="Hello";
		boolean status=SNService.getUserToken(verifier);		
//		String str="<html><head><script type=\"text/javascript\">  function onLoadPage(){  window.close(); }   </script></head><body onLoad=\"onLoadPage()\">PPP</body> </HTML>";
		if(status)
		{
			str="Successfully logged In..";
		}
		else
		{
			str="There's Some Problem";
		}
		return str;
	}
}
