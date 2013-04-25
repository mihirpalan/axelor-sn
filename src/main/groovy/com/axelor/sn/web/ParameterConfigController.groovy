package com.axelor.sn.web

import com.axelor.sn.db.ConfigParameter;
import com.axelor.sn.service.SNMetaService
import javax.inject.Inject;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Request;


class ParameterConfigController 
{
	@Inject
	SNMetaService service;
	
	void getConfigDetail(ActionRequest request,ActionResponse response)
	{
		response.flash="Parameter Set Successfully Now You can't set Another Parameter";
		println(request.context.toString())
	}

}
