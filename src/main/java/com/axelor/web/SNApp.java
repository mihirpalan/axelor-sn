package com.axelor.web;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import com.axelor.auth.db.User;
import com.axelor.rpc.Response;
import com.axelor.sn.service.SNFBService;
import com.google.inject.Inject;

//http://localhost:8080/axelor-demo/ws/snapps/100
@Path("/snapps")
public class SNApp 
{
	@Inject
	SNFBService service; 
	@GET
	@Path("{id}")
	public String get(@PathParam("id") Long id,@QueryParam("code") String code)
	{
		String stat="";
		try
		{
		System.err.println("ID: " + id);
		System.err.println("Code: " + code);
		Subject subject = SecurityUtils.getSubject();
		User currentUser = User.all().filter("self.code = ?1", subject.getPrincipal()).fetchOne();
		
			stat=service.storingToken(currentUser, code);
			if(stat.equals("Login Successfull!!"))
			{
				code="<HTML><body><h1><center>Login Successfull!! You may Close it and go back to ABS</center></h1></body></html> ";
			}
		}
		catch (Exception e) 
		{
				code=e.getMessage();
				e.printStackTrace();
		}
//		System.out.println("CurrentUser___:" + currentUser.getName()
//				+ currentUser.getCode());

		//authService.authorize(currentUser, authCode);
		
		return code;		
	}
}
