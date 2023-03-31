/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2016
//
// Copyright in this software belongs to University of Southampton
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
//      Created By :            Vadims Krivcovs
//      Created Date :          2016-11-10
//      Created for Project :   5G-Ensure/ASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

/**
 * Simple global error controller
 *
 * note: for more specific details on error handling please refer to
 * http://docs.spring.io/spring/docs/3.2.x/spring-framework-reference/html/mvc.html (section 17.11 onwards)
 */

//KEM: commented out these lines to temporarily disable this class from being used for error handling.
//TODO: check if this controller is actually required now.
//@ControllerAdvice
//@Controller
public class GlobalErrorController {

	private final Logger logger = LoggerFactory.getLogger(GlobalErrorController.class);

	@Value("${error.message.general}")
	private String errorMessageGeneral;

	// master exception handler
	// note: currently it does not distinguish between different exception such as 404, 500 etc. and process
	// everything in a common way
	@ExceptionHandler(Exception.class)
	public ModelAndView exception(Exception e) {

		ModelAndView errorModelView = new ModelAndView("error");

		// set error name and a message
		errorModelView.addObject("name", e.getMessage());
		errorModelView.addObject("message", errorMessageGeneral);

		// return error template view
		return errorModelView;
	}

	// TODO: explicit handling (if needed) for 404, 500 etc.
	// example
	//@ExceptionHandler(ResourceNotFoundException.class) // custom resource not found exception class in exception package
	//public ModelAndView handle404(Exception e) {
	//
	//	ModelAndView errorModelView = new ModelAndView("error");
	//
	//  // set error name and a message
	//	errorModelView.addObject("name", e.getMessage());
	//	errorModelView.addObject("message", errorMessageGeneral);
	//	// return error template view
	//	return errorModelView;
	//}
}
