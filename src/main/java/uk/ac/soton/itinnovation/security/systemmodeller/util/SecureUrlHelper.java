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
package uk.ac.soton.itinnovation.security.systemmodeller.util;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.soton.itinnovation.security.systemmodeller.auth.KeycloakAdminClient;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.model.ModelFactory;
import uk.ac.soton.itinnovation.security.systemmodeller.model.WebKeyRole;
import uk.ac.soton.itinnovation.security.systemmodeller.mongodb.IModelRepository;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.ModelCheckedOutException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.ModelInvalidException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.UserForbiddenException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.UserNotFoundException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.UsernameInvalidException;

@Component
public class SecureUrlHelper {

	// private static final Logger logger = LoggerFactory.getLogger(SecureUrlHelper.class);

	@Autowired
	private IModelRepository modelRepository;

	@Autowired
	private ModelFactory modelFactory;

	@Autowired
	private KeycloakAdminClient keycloakAdminClient;

	private SecureRandom random = new SecureRandom();

	public String generateHardToGuessUrl() {
		return new BigInteger(512, random).toString(32);
	}

	public Model getModelFromUrlThrowingException(String url, WebKeyRole requiredRole){
		return getModelFromUrlThrowingException(url, requiredRole, false, false);
	}

	/**
	 * Retrieve the model for a particular web key, checking that the model exists and
	 * that the user is authorized to access that model through either the web key or
	 * through access control lists.
	 *
	 * @param url the string representation of the model object to fetch
	 * @param requiredRole the permission level that we have attempted to access the model at
	 * @param forceCheckout whether on retrieval the model should be checked out by the current user regardless of whether it
	 * has already been checked out by another user
	 * @param forceCheckin whether on retrieval the model should be checked in, and therefore should not be automatically
	 * checked out again
	 * @return Model model object
     * @throws UserNotFoundException if the user is not logged in or username is invalid
     * @throws UserForbiddenException if the user is not authorized
     * @throws ModelInvalidException if the url doesn't match any model
     * @throws ModelCheckedOutException if the model is already being edited by another user
	 *
	 */
	 // TODO -- add logging to all failure modes
	public Model getModelFromUrlThrowingException(String url, WebKeyRole requiredRole, boolean forceCheckout, boolean forceCheckin){
		// logger.debug("getModelFromUrlThrowingException: {}, {}, {}, {}", url, requiredRole, forceCheckout, forceCheckin);
		Model model = getModelFromUrl(url);
		UserRepresentation user = keycloakAdminClient.getCurrentUser();

		// check that the model exists
		if (model == null){
			// TODO -- pass through model id
			throw new ModelInvalidException();
		}

		// check if our webkey grants us access to the model with the required permissions
		WebKeyRole providedRole = getRoleFromUrl(url);
		// WebKeyRole elements are ordered such that later roles grant all permissions of earlier roles in the enum 
		int requiredRoleVal = requiredRole.ordinal();

		if (requiredRoleVal > providedRole.ordinal()) {
			// the web key itself doesn't grant the required permissions -- check access control lists

			// first the user must be logged in
			if (user == null) {
				throw new UserNotFoundException();
			}

			// check the user is in the appropriate access control list
			// if you're the owner (stored on model.getUserId()) you can do anything
			Set<String> allowedUsernames = new HashSet<>();

			if (requiredRoleVal <= WebKeyRole.OWNER.ordinal()) allowedUsernames.addAll(model.getOwnerUsernames());
			if (requiredRoleVal <= WebKeyRole.WRITE.ordinal()) allowedUsernames.addAll(model.getWriteUsernames());
			if (requiredRoleVal <= WebKeyRole.READ.ordinal()) allowedUsernames.addAll(model.getReadUsernames());

			// the model owner (model.getUserId()) should never be null in normal circumstances
			if (model.getUserId() == null) {
				throw new UsernameInvalidException();
			}
			// if you're not in an appropriate ACL and you're not the owner
			if (!allowedUsernames.contains(user.getUsername()) && !model.getUserId().equals(user.getId())) {
				throw new UserForbiddenException();
			}
		}

		// check that no one else has checked out the model if we want more than read access
		// if we are forcing checkout, we don't care if the model is already checked out 
		// TODO -- It is not possible to distinguish between different users who are not logged in
		// accessing the same model through the same webkey. We set a common editorId for all such users
		// to avoid a ModelCheckoutOutException on every refresh but this will not distinguish
		// between different users correctly. In the future, we could generate a seperate webkey for
		// each user. 
		String editorId = url;
		if (user!= null){
			editorId = user.getId();
		}
		if (forceCheckout == false &&
				requiredRoleVal == WebKeyRole.WRITE.ordinal() && 
				model.getEditorId() != null &&
				!model.getEditorId().equals(editorId)) {
			throw new ModelCheckedOutException();
		}

		// for all write operations, check out the model before performing any operations
		// we don't want to automatically checkout if we're performing a checkin operation instead
		if (requiredRoleVal == WebKeyRole.WRITE.ordinal() && forceCheckin == false){
			model.setEditorId(editorId);
			model.saveModelACLonly();
		}

		// there were no problems
		return model;
	}

	public Model getModelFromUrl(String url){
		Model model = getWriteModelFromUrl(url);
		if(model == null){
			model = getReadModelFromUrl(url);
			if (model == null){
				model = getNoRoleModelFromUrl(url);
				if (model == null){
					model = getOwnerModelFromUrl(url);
				}
			}
		}
		return model;
	}

	public WebKeyRole getRoleFromUrl(String url){
		Model model = getWriteModelFromUrl(url);
		if (model != null){
			return WebKeyRole.WRITE;
		}
		model = getReadModelFromUrl(url);
		if (model != null){
			return WebKeyRole.READ;
		}
		model = getNoRoleModelFromUrl(url);
	    if (model != null){
			return WebKeyRole.NONE;
		}
		model = getOwnerModelFromUrl(url);
	    if (model != null){
			return WebKeyRole.OWNER;
		}

		return null;
	}

	public Model getOwnerModelFromUrl(String url){
		return modelFactory.getModelOrNull(modelRepository.findOneByOwnerUrl(url));
	}

	public Model getWriteModelFromUrl(String url){
		return modelFactory.getModelOrNull(modelRepository.findOneByWriteUrl(url));
	}

	public Model getReadModelFromUrl(String url){
		return modelFactory.getModelOrNull(modelRepository.findOneByReadUrl(url));
	}

	public Model getNoRoleModelFromUrl(String url){
		return modelFactory.getModelOrNull(modelRepository.findOneByNoRoleUrl(url));
	}


}
