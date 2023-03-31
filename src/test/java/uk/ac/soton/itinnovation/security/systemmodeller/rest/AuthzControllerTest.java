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
//      Created By :            Anna Brown
//      Created Date :          19/07/2021
//      Created for Project :   Spyderisk Accelerator
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import io.restassured.http.ContentType;
import uk.ac.soton.itinnovation.security.systemmodeller.CommonTestSetup;
import uk.ac.soton.itinnovation.security.systemmodeller.SystemModellerApplication;
import uk.ac.soton.itinnovation.security.systemmodeller.TestHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.auth.KeycloakAdminClient;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.model.ModelACL;
import uk.ac.soton.itinnovation.security.systemmodeller.model.ModelFactory;
import uk.ac.soton.itinnovation.security.systemmodeller.model.WebKeyRole;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.AuthzDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.util.SecureUrlHelper;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SystemModellerApplication.class, webEnvironment = RANDOM_PORT)
public class AuthzControllerTest extends CommonTestSetup{

	private static final Logger logger = LoggerFactory.getLogger(AssetControllerTest.class);

	@Autowired
	private ModelFactory modelFactory;

	@SpyBean
	private SecureUrlHelper secureUrlHelper;

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

	//Provides model control and access
	private Model testModel;
	private static TestHelper testHelper;
	private String testUserId;

	@BeforeClass
	public static void beforeClass() {
		logger.info("Setting up AuthzControllerTest class");
		testHelper = new TestHelper("jena-tdb");
		testHelper.setUp();
	}

	@Before
	public void init() {
		logger.info("Executing {}", name.getMethodName());
		testUserId = keycloakAdminClient.getUserByUsername(testUserName).getId();
		initAuth(contextPath, port);
	}

	//Utilities

	private void switchToSystemModel(int modelIndex) {
		testHelper.switchModels(0, modelIndex);

		//model graph(s) already in Jena - but model not in Mongo or the management graph
		testModel = modelFactory.getModel(testHelper.getModel(), testHelper.getStore());

		//insert the model into Mongo to generate a model ID
		testModel.setUserId(testUserId);

		// add authz data here to be able to test we can correctly GET it
		// TODO -- we may want to change the way the testModel object is created instead, 
		// and set the web keys there
		testModel.setNoRoleUrl(secureUrlHelper.generateHardToGuessUrl());
		testModel.setWriteUrl(secureUrlHelper.generateHardToGuessUrl());
		testModel.setReadUrl(secureUrlHelper.generateHardToGuessUrl());
		testModel.setOwnerUrl(secureUrlHelper.generateHardToGuessUrl());

		testModel.save();

		//bypass looking model up in Mongo via WebKey (the WebKey is NOT the model ID)
		when(secureUrlHelper.getModelFromUrl("testModel")).thenReturn(testModel);
		when(secureUrlHelper.getRoleFromUrl("testModel")).thenReturn(WebKeyRole.NONE);
		when(secureUrlHelper.getWriteModelFromUrl("testModel")).thenReturn(testModel);
		when(secureUrlHelper.getReadModelFromUrl("testModel")).thenReturn(testModel);
	}

	//Tests

	/**
	 * Testing getting authz info from model 1
	 * Asserts OK 200 status
	 * Asserts authz has a valid no role web key
	 */
	@Test
	public void testGetAuthz() {
		switchToSystemModel(1);

		given().
			filter(userSession).
		when().
			get("/models/testModel/authz").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("noRoleUrl", notNullValue());

	}

  	/**
  	 * Test getting authz from an invalid model ID
  	 * Assert NOT FOUND 404 status
  	 */
  	@Test
  	public void testGetAuthzInvalidModel() {
  		given().
  			filter(userSession).
  		when().
  			get("/models/notATestModel/authz").
  		then().
  			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
  	}

  
  	/**
  	 * Test replacing model authz info with a new authz object
  	 * Asserts OK 200 status
  	 * Asserts 1 user with write permissions now in model
  	 */
  	@Test
  	public void testSaveAuthzToModel() {
  		switchToSystemModel(1);
  
		ModelACL modelACL = new ModelACL();
		// create new AuthzDTO to use to update model
		AuthzDTO testAuthzDTO = new AuthzDTO(modelACL);
		String testAdmin = "testadmin";
		testAuthzDTO.addWriteUsername(testAdmin);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAuthzDTO).
		when().
			put("/models/testModel/authz").
		then().
			assertThat().statusCode(HttpStatus.SC_OK);

		assertTrue(testModel.getWriteUsernames().contains(testAdmin));
	}

	/**
  	 * Test trying to replace model authz info with a new authz object for an invalid model
  	 * Assert NOT FOUND 404 status
  	 */
  	@Test
  	public void testSaveAuthzToInvalidModel() {
  		switchToSystemModel(1);
  
		ModelACL modelACL = new ModelACL();
		// create new AuthzDTO to use to update model
		AuthzDTO testAuthzDTO = new AuthzDTO(modelACL);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAuthzDTO).
		when().
			put("/models/notATestModel/authz").
		then().
  			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
  	 * Test trying to replace model authz info with a new authz object with an invalid
	 * username in the ACL. 
  	 * Assert UNPROCESSABLE_ENTITY 422 status
  	 */
  	@Test
  	public void testSaveAuthzWithInvalidUsername() {
  		switchToSystemModel(1);
  
		ModelACL modelACL = new ModelACL();
		// create new AuthzDTO to use to update model
		AuthzDTO testAuthzDTO = new AuthzDTO(modelACL);
		// username that does not exist
		String notAUser = "notAUser";
		testAuthzDTO.addWriteUsername(notAUser);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAuthzDTO).
		when().
			put("/models/testModel/authz").
		then().
  			assertThat().statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
	}

	/**
  	 * Test trying to replace model authz info with a new authz object with an invalid
	 * username in the ACL. In this case, the username is a substring of a valid user,
	 * which should throw an error.
  	 * Assert UNPROCESSABLE_ENTITY 422 status
  	 */
  	@Test
  	public void testSaveAuthzWithInvalidUsername2() {
  		switchToSystemModel(1);
  
		ModelACL modelACL = new ModelACL();
		// create new AuthzDTO to use to update model
		AuthzDTO testAuthzDTO = new AuthzDTO(modelACL);
		// username that does not exist, but is substring of "testuser" or "testadmin"
		String notAUser = "test";
		testAuthzDTO.addWriteUsername(notAUser);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAuthzDTO).
		when().
			put("/models/testModel/authz").
		then().
  			assertThat().statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
	}
}
