/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton
// Digital Health and Biomedical Engineering (DHBE),
// IT Innovation Centre, 2026
//
// Copyright in this software belongs to University of Southampton,
// Digital Health and Biomedical Engineering (DHBE), IT Innovation Centre,
// Highfield Campus, SO17 1BJ, UK.
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
//  Created By :            Panos Melas
//  Created Date :          30/06/2026
//  Created for Project :   FAITH
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.systemmodeller.auth;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;

import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.UserNotFoundException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.UsernameInvalidException;

/**
 * Communicates with Keycloak server to perform user-related operations.
 *
 * Replaces keycloak-admin-client library with direct REST calls via WebClient
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

    private WebClient webClient;

    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminClient.class);

    @PostConstruct
    private void init() {
        webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        logger.info("KeycloakAdminClient initialised against {}/realms/{}", keycloakUrl, keycloakRealm);
    }

    private String getAdminToken() {
        String tokenUrl = keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        JsonNode response = webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null || !response.has("access_token")) {
            throw new IllegalStateException("Failed to obtain admin token from Keycloak");
        }

        return response.get("access_token").asText();
    }

    private String adminUrl() {
        return keycloakUrl + "/admin/realms/" + keycloakRealm;
    }

    public boolean currentUserHasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // TODO -- for tests only, authentication may be null. See comment on ModelAuditor
        if (authentication != null) {
            Collection<SimpleGrantedAuthority> authorities =
                    (Collection<SimpleGrantedAuthority>) authentication.getAuthorities();
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
        if (authentication != null) {
            String username = authentication.getName();
            return getUserByUsername(username);
        }
        return null;
    }

    /**
     * Get the currently logged in user according to keycloak, throwing an exception
     * if there is no user logged in, which will be turned into a web response with error status
     *
     * @return a UserRepresentation object containing user info
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
        String token = getAdminToken();

        List<UserRepresentation> matchedUsers = webClient.get()
                .uri(adminUrl() + "/users?username={u}&exact=true", username)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToFlux(UserRepresentation.class)
                .collectList()
                .block();

        if (matchedUsers != null) {
            for (UserRepresentation matchedUser : matchedUsers) {
                if (username.equals(matchedUser.getUsername())) {
                    // Fetch full representation (includes userProfileMetadata)
                    return getUserById(matchedUser.getId());
                }
            }
        }

        logger.warn("Username {} not found", username);
        return null;
    }

    public UserRepresentation getUserById(String id) {
        String token = getAdminToken();

        try {
            return webClient.get()
                    .uri(adminUrl() + "/users/{id}", id)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(UserRepresentation.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            logger.warn("User ID {} not found", id);
            return null;
        }
    }

    public List<UserRepresentation> getAllUsers() {
        logger.info("keycloak URL: {}", keycloakUrl);
        logger.info("keycloak realm: {}", keycloakRealm);

        String token = getAdminToken();

        logger.warn("got realm");

        List<UserRepresentation> userList = webClient.get()
                .uri(adminUrl() + "/users?max=1000")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToFlux(UserRepresentation.class)
                .collectList()
                .block();

        userList = userList != null ? userList : Collections.emptyList();
        logger.warn("Retrieved {} users", userList.size());
        return userList;
    }

    public boolean checkUsernamesExistThrowingException(Set<String> usernames) {
        for (String username : usernames) {
            this.checkUsernameExistsThrowingException(username);
        }
        return true;
    }

    private boolean checkUsernameExistsThrowingException(String username) {
        String token = getAdminToken();

        List<UserRepresentation> matchedUsers = webClient.get()
                .uri(adminUrl() + "/users?username={u}&exact=true", username)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToFlux(UserRepresentation.class)
                .collectList()
                .block();

        if (matchedUsers != null) {
            for (UserRepresentation matchedUser : matchedUsers) {
                if (username.equals(matchedUser.getUsername())) {
                    return true;
                }
            }
        }

        logger.warn("Username {} not found", username);
        throw new UsernameInvalidException();
    }

    public List<String> getRoles(UserRepresentation user) {
        String token = getAdminToken();

        JsonNode rolesNode = webClient.get()
                .uri(adminUrl() + "/users/{id}/role-mappings/realm", user.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (rolesNode == null || !rolesNode.isArray()) {
            return Collections.emptyList();
        }

        List<String> roles = new java.util.ArrayList<>();
        rolesNode.forEach(roleNode -> {
            if (roleNode.has("name")) {
                roles.add(roleNode.get("name").asText());
            }
        });

        return roles;
    }
}
