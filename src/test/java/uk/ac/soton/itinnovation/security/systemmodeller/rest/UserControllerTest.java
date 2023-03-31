/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2018
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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import uk.ac.soton.itinnovation.security.systemmodeller.CommonTestSetup;
import uk.ac.soton.itinnovation.security.systemmodeller.SystemModellerApplication;
import uk.ac.soton.itinnovation.security.systemmodeller.TestHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.auth.KeycloakAdminClient;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.model.ModelFactory;
import uk.ac.soton.itinnovation.security.systemmodeller.mongodb.IModelRepository;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SystemModellerApplication.class, webEnvironment = RANDOM_PORT)
public class UserControllerTest extends CommonTestSetup{

	private static final Logger logger = LoggerFactory.getLogger(UserControllerTest.class);

	//System objects
	@Autowired
	private IModelRepository modelRepository;

	@Autowired
	private ModelFactory modelFactory;

	@Autowired
	private KeycloakAdminClient keycloakAdminClient;

	//Auth variables
	@Value("${server.servlet.contextPath}")
        private String contextPath;

	@LocalServerPort
	int port;

	//Allows automatic logging of test names
	@Rule
	public TestName name = new TestName();

	private String testAdminId;
	private String testUserId;

	@Before
	public void init() {
		logger.info("Executing {}", name.getMethodName());
		initAuth(contextPath, port);
		testAdminId = keycloakAdminClient.getUserByUsername(testAdminName).getId();
		testUserId = keycloakAdminClient.getUserByUsername(testUserName).getId();
		modelRepository.deleteAll();
	}

	private Map<String, Object> testAdmin;
	private Map<String, Object> testUser;
	private List<Map<String, Object>> allUsers;

	//Content needs to match the test accounts in the Keycloak ssm-realm
	{
		testAdmin = new HashMap<>();
		testAdmin.put("id", "b32c55ab-43f0-41d0-b983-e4992d488015");
		testAdmin.put("firstName", "Admin");
		testAdmin.put("lastName", "User");
		testAdmin.put("email", "testadmin@example.com");
		testAdmin.put("username", "testadmin");
		testAdmin.put("enabled", true);
		testAdmin.put("role", 1);
		testAdmin.put("modelsCount", 0);

		testUser = new HashMap<>();
		testUser.put("id", "1b9e0a00-6b4f-4379-8355-2f8321ba6698");
		testUser.put("firstName", "Normal");
		testUser.put("lastName", "User");
		testUser.put("email", "testuser@example.com");
		testUser.put("username", "testuser");
		testUser.put("enabled", true);
		testUser.put("role", 2);
		testUser.put("modelsCount", 0);

		allUsers = new ArrayList<>();
		allUsers.add(testAdmin);
		allUsers.add(testUser);
	}

	/**
	 * Test get all users
	 * Assert OK 200 status
	 * Assert testAdmin and testUser returned
	 */
	@Test
	public void testGetAllUsers() {
		given().
			filter(adminSession).
		when().
			get("/administration/users").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("", containsInAnyOrder(allUsers.toArray()));
	}

	/**
	 * Test get user
	 * Assert OK 200 status
	 * Assert testAdmin returned
	 */
	@Test
	public void testGetUser() {
		given().
			filter(adminSession).
		when().
			get("/administration/users/" + testAdminId).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("", is(testAdmin));

	}

	/**
	 * Test get unknown user
	 * Assert NOT FOUND 404 status
	 * Assert error message returned
	 */
	@Test
	public void testGetUserUnknownUser() {
		given().
			filter(adminSession).
		when().
			get("/administration/users/notAUserID").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND).
			and().
			assertThat().body(is("User does not exist: notAUserID"));
	}

	/**
	 * Test user model count updates when model added then deleted
	 * Assert OK 200 status
	 * Assert testUser model count is 0
	 * Assert OK 200 status
	 * Assert testUser model count is 1
	 * Assert OK 200 status
	 * Assert testUser model count is 0
	 */
	@Test
	public void testGetUserModelsCount() {
		given().
			filter(adminSession).
		when().
			get("/administration/users/" + testUserId).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("", is(testUser));

		//Insert new model
		Model testModel = modelFactory.createModel(TestHelper.DOM_SHIELD_URI, testUserId);

		Map<String, Object> updatedTestUser = new HashMap<>(testUser);
		Integer oldCount = (Integer) updatedTestUser.get("modelsCount");
		updatedTestUser.put("modelsCount", oldCount + 1);

		given().
			filter(adminSession).
		when().
			get("/administration/users/" + testUserId).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("", is(updatedTestUser));

		//Remove the model
		testModel.delete();

		given().
			filter(adminSession).
		when().
			get("/administration/users/" + testUserId).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("", is(testUser));
	}

	/**
	 * Test get current user (non-admin)
	 * Assert OK 200 status
	 * Assert testUser returned
	 */
	@Test
	public void testGetCurrentUser() {
		given().
			filter(userSession).
		when().
			get("/auth/me").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("", is(testUser));
	}

	/**
	 * Test get current user (admin)
	 * Assert OK 200 status
	 * Assert testUser returned
	 */
	@Test
	public void testGetCurrentUserAdminUser() {
		given().
			filter(adminSession).
		when().
			get("/auth/me").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("", is(testAdmin));
	}

	/**
	 * Test get current user (unknown user)
	 * Assert UNAUTHORIZED 401 status
	 * Assert error message returned
	 */
	@Test
	public void testGetCurrentUserUnknownUser() {
		given().
			//No session - i.e. not logged in
			//Header forces a 401 rather than a 302
			header("X-Requested-With", "XMLHttpRequest").
		when().
			get("/auth/me").
		then().
			assertThat().statusCode(HttpStatus.SC_UNAUTHORIZED).
			and().
			assertThat().body("message", is("Unauthorized"));
	}
}
