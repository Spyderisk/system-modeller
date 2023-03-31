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
//  Created By :            Anna Brown
//  Created Date :          21-Oct-2021
//  Created for Project :   Spyderisk-Accelerator
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.systemmodeller.mongodb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.keycloak.representations.idm.UserRepresentation;
import java.util.Optional;
import uk.ac.soton.itinnovation.security.systemmodeller.auth.KeycloakAdminClient;

/** 
 * Configures MongoDB automatic auditing, which is enabled in MongoConfig, to know about
 * keycloak users. Used for the LastModifiedBy, CreatedBy annotations on ModelACL.
 */
@Component
public class ModelAuditor implements AuditorAware<String> {

	@Autowired
	private KeycloakAdminClient keycloakAdminClient;
 
    /** 
     * Get the id of the currently logged in user if there is a user logged in,
     * otherwise return anonymous.
     */
    // TODO -- for the test framework only, this listener appears to be launched on a
    // separate thread to all other testing work. This means that getCurrentUser does not
    // have the correct SecurityContext as set up by the test initialisation, and so it 
    // can't find the testuser. This does not occur when the getCurrentAuditor functionality
    // is being accessed from the web client rather than the testing system. 
    // For reference: when manually investigating, in the real application execution was all on the 
    // 'http-nio-8081-exec-6' thread. In the tests, the tests that work are on 
    // 'http-nio-auto-1-exec-5' and the listener and failing keycloak access are both on the
    // 'Test Worker' thread. 
    // As @LastModifiedBy is not checked by the test framework and we're not using @CreatedBy, 
    // we get around this by having getCurrentUser return null if it finds and empty 
    // SecurityContext. However, it would be better to launch AuditorAware on the same thread so
    // these annotations can be tested.
    @Override
    public Optional<String> getCurrentAuditor() {
		UserRepresentation user = keycloakAdminClient.getCurrentUser();
        String userId;
        if (user != null){
            userId = user.getId(); 
        } else {
            // if we return an empty optional here, the last modified by field will 
            // not be updated at all, which is not what we want here. 
            // TODO -- it's a little hacky that we need to set id to anonymous and then
            // rely on a failed keycloak lookup in ModelController:setModelNamesFromIds to correctly
            // set the display username to "anonymous"
            userId = "anonymous";
        }
        return Optional.of(userId);
    }
}
