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
//    Created By :          Josh Wright
//    Created Date :        04/09/18
//    Created for Project : RESTASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
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

import uk.ac.soton.itinnovation.security.systemmodeller.CommonTestSetup;
import uk.ac.soton.itinnovation.security.systemmodeller.SystemModellerApplication;
import uk.ac.soton.itinnovation.security.systemmodeller.TestHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.auth.KeycloakAdminClient;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.model.ModelFactory;
import uk.ac.soton.itinnovation.security.systemmodeller.model.WebKeyRole;
import uk.ac.soton.itinnovation.security.systemmodeller.util.SecureUrlHelper;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SystemModellerApplication.class, webEnvironment = RANDOM_PORT)
public class RedirectControllerTest extends CommonTestSetup{

	private static final Logger logger = LoggerFactory.getLogger(RedirectControllerTest.class);

	//System objects, spying on secureUrlHelper to control model access
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

	//System Modeller name is configurable
	@Value("${spring.application.name}")
	private String applicationName;

	//Allows automatic logging of test names
	@Rule
	public TestName name = new TestName();

	//Provides model control and access
	private Model testModel;
	private String testUserId;
	private static TestHelper testHelper;

	@BeforeClass
	public static void beforeClass() {
		logger.info("Setting up RedirectControllerTest class");
		testHelper = new TestHelper("jena-tdb");
		testHelper.setUp();
	}

	@Before
	public void init() {
		logger.info("Executing {}", name.getMethodName());
		initAuth(contextPath, port);
		testUserId = keycloakAdminClient.getUserByUsername(testUserName).getId();
	}

	//Utilities

	private void switchToSystemModel(int modelIndex) {
		testHelper.switchModels(0, modelIndex);

		//model graph(s) already in Jena - but model not in Mongo or the management graph
		testModel = modelFactory.getModel(testHelper.getModel(), testHelper.getStore());
		testModel.setUserId(testUserId);

		//bypass looking model up in Mongo via WebKey (the WebKey is NOT the model ID)
		when(secureUrlHelper.getModelFromUrl("testModel")).thenReturn(testModel);
		when(secureUrlHelper.getRoleFromUrl("testModel")).thenReturn(WebKeyRole.NONE);
		when(secureUrlHelper.getWriteModelFromUrl("testModel")).thenReturn(testModel);
		when(secureUrlHelper.getReadModelFromUrl("testModel")).thenReturn(testModel);
	}

	//Tests

	/**
	 * Test getModelEditor under expected conditions
	 * Asserts OK 200 status
	 * Asserts editor html returned
	 */
	@Test
	public void testGetModelEditor() {
		switchToSystemModel(1);		

		given().
			filter(userSession).
		when().
			get("/models/testModel/edit").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("html.head.title", is(applicationName + " - Editor")).
			and().
			assertThat().body("html.head.meta.@content", hasItem("testModel"));

	}

	/**
	 * Test getModelEditor with a non-public model (the default)
	 * Asserts OK 200 status
	 * asserts editor html returned.
	 */
	@Test
	public void testGetModelEditorNotPublicWriteUrl() {
		switchToSystemModel(1);

		given().
			filter(userSession).
		when().
			get("/models/testModel/edit").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("html.head.title", is(applicationName + " - Editor")).
			and().
			assertThat().body("html.head.meta.@content", hasItem("testModel"));
	}

	/**
	 * Test getModelEditor for a user that is not logged in.
	 * Asserts OK 200 status
	 * Asserts error html returned
	 */
	@Test
	public void testGetModelEditorNotLoggedIn() {
		switchToSystemModel(0);

		given(). //No session - i.e. not logged in
		when().
			get("/models/testModel/edit").
		then().
			assertThat().statusCode(HttpStatus.SC_UNAUTHORIZED);
	}

	/**
	 * Test getModelEditor with an unknown model
	 * Assert OK 200 status
	 * Assert error message in html
	 */
	@Test
	public void testGetModelEditorUnknownModel() {
		given().
			filter(userSession).
		when().
			get("/models/notATestModel/edit").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Test getModelEditor without write access to model
	 * Assert OK 200 status
	 * Assert error message in html
	 */
	@Test
	public void testGetModelEditorNoWriteAccess() {
		switchToSystemModel(0);

		testModel.setUserId("notTheTestUser");

		given().
			filter(userSession).
		when().
			get("/models/testModel/edit").
		then().
			assertThat().statusCode(HttpStatus.SC_FORBIDDEN);
	}

	/**
	 * Test getModelEditor when model being edited by another user
	 * Assert OK 200 status
	 * ASsert error message in html
	 */
	@Test
	public void testGetModelEditorNotEditor() {
		switchToSystemModel(1);		

		testModel.setEditorId("notTheTestUser");

		given().
			filter(userSession).
		when().
			get("/models/testModel/edit").
		then().
			assertThat().statusCode(HttpStatus.SC_LOCKED);
	}

	/**
	 * Test getModelViewer under expected conditions
	 * Asserts OK 200 status
	 * Asserts viewer html returned
	 */
	@Test
	public void testGetModelViewer() {
		switchToSystemModel(1);

		given().
			filter(userSession).
		when().
			get("/models/testModel/read").
		then().
			assertThat().statusCode(HttpStatus.SC_OK);
	}

	/**
	 * Test getModelViewer for a user that is not logged in.
	 * Asserts OK 200 status
	 * Asserts error html returned
	 */
	@Test
	public void testGetModelViewerNotLoggedIn() {
		switchToSystemModel(0);

		given(). //No session - i.e. not logged in
		when().
			get("/models/testModel/read").
		then().
			assertThat().statusCode(HttpStatus.SC_UNAUTHORIZED);
	}

	/**
	 * Test getModelViewer with an invalid model
	 * Assert OK 200 status
	 * Assert Not Found error html
	 */
	@Test
	public void testGetModelViewerInvalidModel() {
		given().
			filter(userSession).
		when().
			get("/models/notATestModel/read").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Test getModelViewer without read access
	 * Assert OK 200 status
	 * Assert error html returned
	 */
	@Test
	public void testGetModelViewerNoReadAccess() {
		switchToSystemModel(0);		

		testModel.setUserId("notTheTestUser");

		given().
			filter(userSession).
		when().
			get("/models/testModel/read").
		then().
			assertThat().statusCode(HttpStatus.SC_FORBIDDEN);
	}
}
