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
//      Created By :          Toby Wilkinson
//      Created Date :        19/08/2020
//      Created for Project : EFACTORY
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.soton.itinnovation.security.systemmodeller.auth.KeycloakAdminClient;
import uk.ac.soton.itinnovation.security.systemmodeller.mongodb.IModelRepository;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.UserDTO;

@RestController
public class UserController {

	private final Logger logger = LoggerFactory.getLogger(UserController.class);

	@Autowired
	private KeycloakAdminClient keycloakAdminClient;

	@Autowired
	private IModelRepository modelRepository;

	@Value("${admin-role}")
	private String adminRole;

	private Map<String, Integer> getUserModelCounts() {
		Map<String, Integer> counts = new HashMap<>();

		modelRepository
			.findAll()
			.forEach(m -> {
				String userId = m.getUserId();
				counts.put(userId, counts.containsKey(userId) ? counts.get(userId) + 1 : 1);
			});

		return counts;
	}

	private UserDTO getUserDTO(UserRepresentation user, Map<String, Integer> modelCounts) {
		UserDTO dto = new UserDTO();

		dto.setId(user.getId());
		dto.setFirstName(user.getFirstName());
		dto.setLastName(user.getLastName());
		dto.setEmail(user.getEmail());
		dto.setUsername(user.getUsername());
		dto.setEnabled(user.isEnabled());
		dto.setRole(keycloakAdminClient.getRoles(user).contains(adminRole) ? 1 : 2);
		dto.setModelsCount(modelCounts.containsKey(user.getId()) ? modelCounts.get(user.getId()) : 0);

		return dto;
	}

	private UserDTO getUserDTO(UserRepresentation user) {
		return getUserDTO(user, getUserModelCounts());
	}

	@RequestMapping(value = "/administration/users", method = RequestMethod.GET)
	public ResponseEntity<?> getAllUsers() {

		Map<String, Integer> modelCounts = getUserModelCounts();

		List<UserDTO> users = keycloakAdminClient
			.getAllUsers()
			.stream()
			.map(u -> getUserDTO(u, modelCounts))
			.collect(Collectors.toList());

		return ResponseEntity.ok().body(users);
	}

	@RequestMapping(value = "/administration/users/{userId}", method = RequestMethod.GET)
	public ResponseEntity<?> getUser(@PathVariable String userId) {

		UserRepresentation user = keycloakAdminClient.getUserById(userId);

		if (user == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User does not exist: " + userId);
		}

		return ResponseEntity.ok().body(getUserDTO(user));
	}

	@RequestMapping(value = "/auth/me", method = RequestMethod.GET)
	public ResponseEntity<?> getCurrentUser() {
		UserRepresentation user = keycloakAdminClient.getCurrentUser();

		return ResponseEntity.ok().body(getUserDTO(user));
	}
}
