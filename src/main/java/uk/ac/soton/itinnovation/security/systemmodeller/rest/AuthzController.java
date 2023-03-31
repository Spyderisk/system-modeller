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
//      Created By :            Anna Brown
//      Created Date :          15 Jul 2021
//      Created for Project :   Spyderisk Accelerator
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.soton.itinnovation.security.systemmodeller.auth.KeycloakAdminClient;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.model.ModelACL;
import uk.ac.soton.itinnovation.security.systemmodeller.model.WebKeyRole;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.AuthzDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.util.SecureUrlHelper;

@RestController
public class AuthzController {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private SecureUrlHelper secureUrlHelper;

	@Autowired
	private KeycloakAdminClient keycloakAdminClient;

	/**
	 * REST method to GET the authzDTO for a model if user has owner permissions or is using an 
	 * owner web key
	 *
	 * @param modelId
	 * @return an AuthzDTO object containing webkeys and access control lists
	 */
	@RequestMapping(value = "/models/{modelId}/authz", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<AuthzDTO> getAuthz(@PathVariable String modelId) {

		logger.info("Called REST method to GET authz for model {}", modelId);

		// check if model valid
		Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.OWNER);

		//get authz
		// TODO -- this should perhaps be model.getAuthzDTO rather than returning the whole ModelACL object
		ModelACL modelACL = model.getModelACL();
		AuthzDTO authzDTO = new AuthzDTO(modelACL); 
		return ResponseEntity.status(HttpStatus.OK).body(authzDTO);
	}

	/**
	 * REST method to PUT authzDTO object, saving it in MongoDB for a model 
	 * if user has owner permissions or is using an owner web key
	 *
	 * @param modelId
	 * @param updatedAuthz
	 * @return status message response object
	 */
	@RequestMapping(value = "/models/{modelId}/authz", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<String> updateAuthz(@PathVariable String modelId, @RequestBody AuthzDTO updatedAuthz) {

		logger.info("Called REST method to update authz for model {}", modelId);

		// check if model valid
		Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.OWNER);

		Set<String> writeUsernames = updatedAuthz.getWriteUsernames();
		Set<String> readUsernames = updatedAuthz.getReadUsernames();
		Set<String> ownerUsernames = updatedAuthz.getOwnerUsernames();

		keycloakAdminClient.checkUsernamesExistThrowingException(writeUsernames);
		keycloakAdminClient.checkUsernamesExistThrowingException(readUsernames);
		keycloakAdminClient.checkUsernamesExistThrowingException(ownerUsernames);

		model.setWriteUsernames(writeUsernames);
		model.setReadUsernames(readUsernames);
		model.setOwnerUsernames(ownerUsernames);
		model.setNoRoleUrl(updatedAuthz.getNoRoleUrl());
		model.setReadUrl(updatedAuthz.getReadUrl());
		model.setWriteUrl(updatedAuthz.getWriteUrl());
		model.setOwnerUrl(updatedAuthz.getOwnerUrl());
		model.save();

		return ResponseEntity.status(HttpStatus.OK).body("completed"); //no need to return any data
	}

}
