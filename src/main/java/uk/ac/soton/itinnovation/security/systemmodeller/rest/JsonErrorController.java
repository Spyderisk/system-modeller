/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2017
//
// Copyright in this library belongs to the University of Southampton
// IT Innovation Centre of Gamma House, Enterprise Road,
// Chilworth Science Park, Southampton, SO16 7NS, UK.
//
// This software may not be used, sold, licensed, transferred, copied
// or reproduced in whole or in part in any manner or form or in or
// on any media by any person other than in accordance with the terms
// of the Licence Agreement supplied with the software, or otherwise
// without the prior written consent of the copyright owners.
//
// This software is distributed WITHOUT ANY WARRANTY, without even the
// implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
// PURPOSE, except where stated in the Licence Agreement supplied with
// the software.
//
//  Created By :            Oliver Hayes
//  Created Date :          2017-08-21
//  Created for Project :   5G-ENSURE
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

@RestController
public class JsonErrorController implements ErrorController {

	private static final String PATH = "/error";

	@Autowired
	private Environment environment;

	@Autowired
	private ErrorAttributes errorAttributes;

	@RequestMapping(value = PATH)
	ErrorResponse error(HttpServletRequest request, HttpServletResponse response) {
		// Appropriate HTTP response code (e.g. 404 or 500) is automatically set by Spring.
		// Here we just define response body.
		return new ErrorResponse(response.getStatus(), getErrorAttributes(request, Arrays.asList(this.environment.getActiveProfiles()).contains("dev")));
	}

	ModelAndView error(Exception e) {
		// Appropriate HTTP response code (e.g. 404 or 500) is automatically set by Spring.
		// Here we just define response body.
		HashMap<String, String> map = new HashMap<>();
		map.put("message", "Please check your request and try again later");
		map.put("name", e.getMessage());
		return new ModelAndView("error", map);
	}

	@Override
	public String getErrorPath() {
		return PATH;
	}

	private Map<String, Object> getErrorAttributes(HttpServletRequest request, boolean includeStackTrace) {

		WebRequest webRequest = new ServletWebRequest(request);
		return errorAttributes.getErrorAttributes(webRequest, includeStackTrace);
	}

	public class ErrorResponse {

		public Integer status;
		public String error;
		public String message;
		public String timeStamp;
		public String trace;
		public String path;

		public ErrorResponse(int status, Map<String, Object> errorAttributes) {
			
			this.status = status;
			this.error = (String) errorAttributes.get("error");
			this.message = (String) errorAttributes.get("message");
			this.timeStamp = errorAttributes.get("timestamp").toString();
			this.trace = (String) errorAttributes.get("trace");
			this.path = (String) errorAttributes.get("path");
		}
	}
}


