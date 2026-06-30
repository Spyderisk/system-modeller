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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

/**
 * Replaces org.keycloak.representations.idm.UserRepresentation.
 * Includes userProfileMetadata available from Keycloak 24+.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRepresentation {

    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean enabled;
    private Boolean emailVerified;
    private Map<String, List<String>> attributes;
    private UserProfileMetadata userProfileMetadata;

    // ── Nested classes ─────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserProfileMetadata {
        private List<UserProfileAttributeMetadata> attributes;

        public List<UserProfileAttributeMetadata> getAttributes() { return attributes; }
        public void setAttributes(List<UserProfileAttributeMetadata> attributes) { this.attributes = attributes; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserProfileAttributeMetadata {
        private String name;
        private String displayName;
        private Boolean required;
        private Boolean readOnly;
        private Map<String, Object> annotations;
        private Map<String, Map<String, Object>> validators;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public Boolean getRequired() { return required; }
        public void setRequired(Boolean required) { this.required = required; }

        public Boolean getReadOnly() { return readOnly; }
        public void setReadOnly(Boolean readOnly) { this.readOnly = readOnly; }

        public Map<String, Object> getAnnotations() { return annotations; }
        public void setAnnotations(Map<String, Object> annotations) { this.annotations = annotations; }

        public Map<String, Map<String, Object>> getValidators() { return validators; }
        public void setValidators(Map<String, Map<String, Object>> validators) { this.validators = validators; }
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public boolean isEnabled() { return enabled != null && enabled; }

    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }

    public Map<String, List<String>> getAttributes() { return attributes; }
    public void setAttributes(Map<String, List<String>> attributes) { this.attributes = attributes; }

    public UserProfileMetadata getUserProfileMetadata() { return userProfileMetadata; }
    public void setUserProfileMetadata(UserProfileMetadata userProfileMetadata) {
        this.userProfileMetadata = userProfileMetadata;
    }
}
