package com.axelor.sn.web

import com.axelor.auth.db.Group
import com.axelor.db.*;
import com.axelor.meta.db.MetaMenu;
import com.axelor.meta.views.MenuItem;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.sn.service.SNMetaService
import com.google.inject.Inject;

class ViewRestrictionController
{
	@Inject
	SNMetaService service;

	void getSelectedValues(ActionRequest request,ActionResponse response)
	{
		List keys=request.context.keySet().asList();
		List values=request.context.values().asList();
		MetaMenu menuObj=values.get(keys.indexOf("menus"));
		Set<Group> grpObj=values.get(keys.indexOf("groups"));
		try
		{
			response.flash=service.setRestrictions(menuObj, grpObj);
		}
		catch (Exception e) 
		{
			response.flash=e.getMessage();
			e.printStackTrace();
		}

	}
}
