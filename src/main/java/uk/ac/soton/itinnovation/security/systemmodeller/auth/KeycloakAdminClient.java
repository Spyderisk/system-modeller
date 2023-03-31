/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2020
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
//      Created By :          Rayna Bozhkova
//      Created Date :        17/02/2020
//      Created for Project : EFACTORY
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.auth;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ws.rs.NotFoundException;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.UserNotFoundException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.UsernameInvalidException;

/**
 * Communicates with Keycloak server to perform user-related operations.
 */
@Service
public class KeycloakAdminClient {

    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    @Value("${user-role}")
    public String userRole;

    private Keycloak keycloak;
    private RealmResource realm;
    private UsersResource users;

    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminClient.class);

    @PostConstruct
    private void init() {
        keycloak = KeycloakBuilder.builder()
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .serverUrl(keycloakUrl)
                .realm(keycloakRealm)
                .build();
    }

    public boolean currentUserHasRole(String role) {
        realm = keycloak.realm(keycloakRealm);
        users = realm.users();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // TODO -- for tests only, authentication may be null. See comment on ModelAuditor
        if (authentication != null){
            Collection<SimpleGrantedAuthority> authorities = (Collection<SimpleGrantedAuthority>) authentication.getAuthorities();
            return authorities.stream().anyMatch(r -> r.getAuthority().equals("ROLE_" + role));
        } 
        return false;
    }

    public UserRepresentation getCurrentUser() {
        if (!currentUserHasRole(userRole)) {
            return null;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // TODO -- for tests only, authentication may be null. See comment on ModelAuditor
        if (authentication != null){
            String username = authentication.getName();
            return getUserByUsername(username);
        }
        return null;
    }

	/**
	 * Get the currently logged in user according to keycloak, throwing an exception 
     * if there is no user logged in, which will be turned into a web response with error status
	 *
	 * @return a UserRepresentation object containing user infoA
     * @throws UserNotFoundException
	 */
    public UserRepresentation getCurrentUserThrowingException() {
	    UserRepresentation user = getCurrentUser();
        if (user == null) {
            throw new UserNotFoundException();
        }
        return user;
    }

    public UserRepresentation getUserByUsername(String username) {
        realm = keycloak.realm(keycloakRealm);
        users = realm.users();

        List<UserRepresentation> matchedUsers = users.search(username);

        if (!matchedUsers.isEmpty()) {
            for (UserRepresentation matchedUser : matchedUsers) {
                if (username.equals(matchedUser.getUsername())) {
                    UserResource userResource = users.get(matchedUser.getId());
                    return userResource.toRepresentation();
                }
            }
        }

        //If we arrive here, either there are no matches at all, or no exact matches
        logger.warn("Username {} not found", username);

        return null;
    }

    public boolean checkUsernamesExistThrowingException(Set<String> usernames) {
        for (String username : usernames) {
            this.checkUsernameExistsThrowingException(username);
        }
        return true;
    }

    private boolean checkUsernameExistsThrowingException(String username) {
        realm = keycloak.realm(keycloakRealm);
        users = realm.users();

        List<UserRepresentation> matchedUsers = users.search(username);

        if (!matchedUsers.isEmpty()) {
            for (UserRepresentation matchedUser : matchedUsers) {
                if (username.equals(matchedUser.getUsername())) {
                    return true;
                }
            }
        }

        //If we arrive here, either there are no matches at all, or no exact matches
        logger.warn("Username {} not found", username);
        throw new UsernameInvalidException();
    }

    public UserRepresentation getUserById(String id) {
        realm = keycloak.realm(keycloakRealm);
        users = realm.users();

        try {
            return users.get(id).toRepresentation();
        } catch (NotFoundException e) {
            logger.warn("User ID {} not found", id);
            return null;
        }
    }

    public List<UserRepresentation> getAllUsers() {
        realm = keycloak.realm(keycloakRealm);
        users = realm.users();

        return users.list();
    }

    public List<String> getRoles(UserRepresentation user) {
        realm = keycloak.realm(keycloakRealm);
        users = realm.users();

        return users
            .get(user.getId())
            .roles()
            .getAll()
            .getRealmMappings()
            .stream()
            .map(r -> r.getName())
            .collect(Collectors.toList());
    }
}
