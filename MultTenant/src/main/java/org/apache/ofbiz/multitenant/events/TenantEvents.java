package org.apache.ofbiz.multitenant.events;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class TenantEvents {
    private static final String MODULE = TenantEvents.class.getName();

    public static String checkTenantDatabaseStatus(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        String tenantId = request.getParameter("tenantId");
        
        try {
            // Call the service
            Map<String, Object> serviceContext = new HashMap<>();
            serviceContext.put("tenantId", tenantId);
            Map<String, Object> result = dispatcher.runSync("checkDatabaseStatus", serviceContext);
            
            if (ServiceUtil.isError(result)) {
                request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(result));
                return "error";
            }
            
            // Set the results in the request attributes
            request.setAttribute("dbExists", result.get("dbExists"));
            request.setAttribute("databaseStatus", result.get("status"));
            
            return "success";
        } catch (Exception e) {
            Debug.logError(e, "Error checking database status for tenant: " + tenantId, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", "Error checking database status: " + e.getMessage());
            return "error";
        }
    }
}
