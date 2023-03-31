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
//    Created By :          Oliver Hayes
//    Created Date :        2017-08-21
//    Created for Project : 5G-ENSURE
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.model.WebKeyRole;
import uk.ac.soton.itinnovation.security.systemmodeller.util.SecureUrlHelper;

@Profile("!dev")
@RestController
public class RedirectController {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private SecureUrlHelper secureUrlHelper;

	/**
	 * This REST method returns the editor view for the model id used in the URI path.
	 *
	 * @param objid the String representation of the model object to fetch
	 * @return the Model instance
	 * TODO Add to Rest API document
	 */
	@RequestMapping(value = "/models/{objid}/edit", method = RequestMethod.GET)
	public ModelAndView getModelEditor(@PathVariable String objid) {

		logger.info("Called REST method to edit model {}", objid);

		Model model = secureUrlHelper.getModelFromUrlThrowingException(objid, WebKeyRole.WRITE);
		ModelMap modelMap = new ModelMap("model", objid);
		modelMap.addAttribute("mode", "edit");
		return new ModelAndView("modeller", modelMap);
	}

	@RequestMapping(value = "/models/{objid}/read", method = RequestMethod.GET)
	public ModelAndView getModelViewer(@PathVariable String objid) {

		logger.info("Called REST method to read model {}", objid);

		Model model = secureUrlHelper.getModelFromUrlThrowingException(objid, WebKeyRole.READ);
		ModelMap modelMap = new ModelMap("model", objid);
		modelMap.addAttribute("mode", "view");
		return new ModelAndView("modeller", modelMap);
	}

	@RequestMapping(value = "/dashboard/{userid}", method = RequestMethod.GET)
	public ModelAndView getDashboardForUser(@PathVariable String userid) {
		logger.info("Loading dashboard for user {}", userid);
		ModelMap modelMap = new ModelMap("userid", userid);
		return new ModelAndView("dashboard", modelMap);
	}
}
