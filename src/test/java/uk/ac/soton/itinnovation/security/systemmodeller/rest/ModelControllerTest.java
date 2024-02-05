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
//      Created By :            Josh Wright
//      Created Date :          18/07/2018
//      Created for Project :   RESTASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import com.google.common.io.Files;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.MultiPartSpecification;
import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;
import uk.ac.soton.itinnovation.security.modelquerier.SystemModelQuerier;
import uk.ac.soton.itinnovation.security.modelvalidator.Progress;
import uk.ac.soton.itinnovation.security.systemmodeller.CommonTestSetup;
import uk.ac.soton.itinnovation.security.systemmodeller.SystemModellerApplication;
import uk.ac.soton.itinnovation.security.systemmodeller.TestHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.auth.KeycloakAdminClient;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.model.ModelFactory;
import uk.ac.soton.itinnovation.security.systemmodeller.model.WebKeyRole;
import uk.ac.soton.itinnovation.security.systemmodeller.mongodb.IModelRepository;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.LoadingProgress;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.ModelDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.StoreModelManager;
import uk.ac.soton.itinnovation.security.systemmodeller.util.SecureUrlHelper;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SystemModellerApplication.class, webEnvironment = RANDOM_PORT)
public class ModelControllerTest extends CommonTestSetup{

	private static final Logger logger = LoggerFactory.getLogger(ModelControllerTest.class);

	//System objects, spying on secureUrlHelper to control model access
	@Autowired
	private ModelObjectsHelper modelHelper;

	@Autowired
	private ModelFactory modelFactory;

	@Autowired
	private StoreModelManager storeModelManager;

	@SpyBean
	private SecureUrlHelper secureUrlHelper;

	@Autowired
	private IModelRepository modelRepository;

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
	private ModelDTO testModelDTO;
	private String testUserId;
	private static TestHelper testHelper;
	private static SystemModelQuerier querier;

	@BeforeClass
	public static void beforeClass() {
		logger.info("Setting up ModelControllerTest class");
		testHelper = new TestHelper("jena-tdb");
		testHelper.setUp();
		querier = new SystemModelQuerier(testHelper.getModel());
	}

	@Before
	public void init() {
		logger.info("Executing {}", name.getMethodName());
		initAuth(contextPath, port);
		testUserId = keycloakAdminClient.getUserByUsername(testUserName).getId();
		modelRepository.deleteAll();
	}

	//Utilities

	/**
	 * Adds the domain-shield model from a resource as a test object
	 */
	private void addShieldDomainModel() {
		logger.info("Adding {} to Store", TestHelper.DOM_SHIELD_NAME);
		storeModelManager.loadModelFromResource(
			TestHelper.DOM_SHIELD_NAME,
			TestHelper.DOM_SHIELD_URI,
			TestHelper.DOM_SHIELD_FPATH_ABS
		);
	}

	/**
	 * Creates a model an inserts it into Jena and Mongo
	 * @return The Model object created
	 */
	private Model createTestModel() {
		return modelFactory.createModel(TestHelper.DOM_SHIELD_URI, testUserId);
	}

	private void switchToSystemModel(int modelIndex) {
		switchToSystemModel(0, modelIndex);
	}
	
	private void switchToSystemModel(int domainModelIndex, int modelIndex) {
		testHelper.switchModels(domainModelIndex, modelIndex);

		//model graph(s) already in Jena - but model not in Mongo or the management graph
		testModel = modelFactory.getModel(testHelper.getModel(), testHelper.getStore());

		//insert the model into Mongo to generate a model ID
		testModel.setUserId(testUserId);
		testModel.save();

		//insert the model into the management graph
		storeModelManager.createSystemModel(
			testModel.getUri(),
			null,
			testModel.getId(),
			testModel.getUserId(),
			testModel.getDomainGraph()
		);

		//bypass looking model up in Mongo via WebKey (the WebKey is NOT the model ID)
		when(secureUrlHelper.getModelFromUrl("testModel")).thenReturn(testModel);
		when(secureUrlHelper.getRoleFromUrl("testModel")).thenReturn(WebKeyRole.NONE);
		when(secureUrlHelper.getWriteModelFromUrl("testModel")).thenReturn(testModel);
		when(secureUrlHelper.getReadModelFromUrl("testModel")).thenReturn(testModel);

		//create ModelDTO for body of POST and PUT requests
		testModelDTO = new ModelDTO(testModel);
	}

	private void clearSystemModel(int modelIndex) {
		testHelper.switchModels(0, modelIndex);

		testHelper.getStore().clearGraph(testHelper.getModel().getGraph("system"));
		testHelper.getStore().clearGraph(testHelper.getModel().getGraph("system-inf"));
		testHelper.getStore().clearGraph(testHelper.getModel().getGraph("system-ui"));
		testHelper.getStore().clearGraph(testHelper.getModel().getGraph("system-meta"));
	}

	/**
	 * Provides a dummy background task that can be used to test the REST endpoints
	 * that check validation and risk calculation progress.
	 *
	 * Once the test has completed stop() must be called to stop the background task.
	 */
	private class EmptyBackgroundTask {

		private CountDownLatch latch = new CountDownLatch(1);

		public EmptyBackgroundTask(String message) {
			// Task that waits until either the latch is decremented or it times out.
			Runnable waitToBeStopped = () -> {
				try {
					if (!latch.await(10, TimeUnit.SECONDS)) {
						logger.error("Fail: Timeout waiting for main thread to finish test.");
						fail();
				}
				} catch (InterruptedException e) {
					logger.error("Fail: Test was interrupted: {}", e);
					fail();
				}
			};

			// Start the background task.
			ScheduledFuture<?> task = Executors
				.newScheduledThreadPool(1)
				.schedule(waitToBeStopped, 0, TimeUnit.NANOSECONDS);

			// Register the task as if it were a validation or risk calculation.
			modelHelper.registerValidationExecution(testModel.getId(), task);

			// Set the progress message to the message passed in.
			// The test that generated the message can then check it is returned by the REST endpoint.
			Progress progress = modelHelper.getValidationProgressOfModel(testModel);
			progress.setMessage(message);
		}

		public void stop() {
			latch.countDown();
		}
	}

	/**
	 * Provides a dummy loading task that can be used to test the REST endpoint
	 * that checks loading progress.
	 *
	 * Once the test has completed stop() must be called to stop the background task.
	 */
	private class EmptyLoadingTask {

		private CountDownLatch latch = new CountDownLatch(1);

		public EmptyLoadingTask(String message) {
			// Task that waits until either the latch is decremented or it times out.
			Runnable waitToBeStopped = () -> {
				try {
					if (!latch.await(10, TimeUnit.SECONDS)) {
						logger.error("Fail: Timeout waiting for main thread to finish test.");
						fail();
				}
				} catch (InterruptedException e) {
					logger.error("Fail: Test was interrupted: {}", e);
					fail();
				}
			};

			// Start the background task.
			ScheduledFuture<?> task = Executors
				.newScheduledThreadPool(1)
				.schedule(waitToBeStopped, 0, TimeUnit.NANOSECONDS);

			// Register the task as if it were an actual model load.
			// Use the model ID as the loading ID. This should be safe as no concurrent loads.
			modelHelper.registerLoadingExecution(testModel.getId(), task);

			// Set the progress message to the message passed in.
			// The test that generated the message can then check it is returned by the REST endpoint.
			LoadingProgress progress = modelHelper.createLoadingProgressOfModel(testModel, testModel.getId());
			progress.setMessage(message);
		}

		public void stop() {
			latch.countDown();
		}
	}

	private void assertLoadingCompleted(String loadingId) {
		int count = 0;

		while (true) {
			String status = modelHelper.getLoadingProgressOfModel(loadingId).getStatus();
			if ("completed".equals(status)) {
				return;
			}
			else if ("failed".equals(status)) {
				logger.error("Loading failed");
				fail();
			}
			try {
				Thread.sleep(100);
				count++;
				if (count > 600) {
					logger.error("Loading timed out");
					fail();
				}
			} catch (InterruptedException e) {
				logger.error("Loading interuputed: {}", e);
				fail();
			}
		}
	}

	private void assertTaskCompleted() {
		int count = 0;

		while (true) {
			String status = modelHelper.getValidationProgressOfModel(testModel).getStatus();
			if ("completed".equals(status)) {
				return;
			}
			else if ("failed".equals(status)) {
				logger.error("Task failed");
				fail();
			}
			try {
				Thread.sleep(100);
				count++;
				if (count > 600) {
					logger.error("Task timed out");
					fail();
				}
			} catch (InterruptedException e) {
				logger.error("Task interuputed: {}", e);
				fail();
			}
		}
	}

	/*
	 * Create a MultiPartSpecification for uploading a model.
	 * @param path The path to the model.
	 */
	private MultiPartSpecification createMultiPartSpecNQ(String path) {
		MultiPartSpecification mp = null;

		try {
			File file = storeModelManager.exportModelResourceToFile(path);

			mp = new MultiPartSpecBuilder(Files.toByteArray(file))
				.fileName("model_file.nq")
				.controlName("file")
				.mimeType("text/plain")
				.build();

		} catch (IOException ex) {
			logger.error("Could not build MultipartFile: {}", ex);
		}

		return mp;
	}

	/*
	 * Create a MultiPartSpecification for uploading a gzipped model.
	 * @param path The path to the model.
	 */
	private MultiPartSpecification createMultiPartSpecGzipNQ(String path) {
		MultiPartSpecification mp = null;

		try {
			File file = storeModelManager.exportModelResourceToFile(path);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream gzos = new GZIPOutputStream(baos);
			gzos.write(Files.toByteArray(file));
			gzos.close();

			mp = new MultiPartSpecBuilder(baos.toByteArray())
				.fileName("model_file.nq.gz")
				.controlName("file")
				.mimeType("application/gzip")
				.build();

		} catch (IOException ex) {
			logger.error("Could not build MultipartFile: {}", ex);
		}

		return mp;
	}

	private String getUserIdDistinctFromTestUserId() {
		return keycloakAdminClient
			.getAllUsers()
			.stream()
			.map(UserRepresentation::getId)
			.filter(id -> !id.equals(testUserId))
			.findAny()
			.get();
	}

	//Tests

	/**
	 * Test of list models
	 * Asserts OK 200 status
	 * Asserts number of models correct
	 * Asserts correct dates and number of each returned in list
	 */
	@Test
	public void testListModels() {
		//Insert models at modelRepository layer
		HashSet<String> usernameSet = new HashSet<>();
		usernameSet.add(testUserName);

		Model testModelA = createTestModel();
		Long dateA = testModelA.getCreated().getTime();

		//Model B has owner and write access but will not be duplicated
		Model testModelB = createTestModel();
		Long dateB = testModelB.getCreated().getTime();
		testModelB.setWriteUsernames(usernameSet);
		testModelB.save();

		//Model C has owner and read access but will not be duplicated
		Model testModelC = createTestModel();
		Long dateC = testModelC.getCreated().getTime();
		testModelC.setReadUsernames(usernameSet);
		testModelC.save();

		//Most data nullified by populateModelInfo, use created date
		List<Long> creationDates =
			given().
				filter(userSession).
			when().
				get("/models").
			then().
				assertThat().statusCode(HttpStatus.SC_OK).
			extract().
				body().path("created");

		assertEquals(3, creationDates.size());

		assertTrue(creationDates.remove(dateA));
		assertTrue(creationDates.remove(dateB));
		assertTrue(creationDates.remove(dateC));
	}

	/**
	 * Test list models when no models
	 * Asserts OK 200 status
	 * Asserts no model in returned list
	 */
	@Test
	public void testListModelsNoModels() {
		given().
			filter(userSession).
		when().
			get("/models").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("created", empty());
	}

	/**
	 * Test list models by an unknown user
	 * Asserts UNAUTHORIZED 401 status
	 * Asserts error message returned
	 */
	@Test
	public void testListModelsUnknownUser() {
		given().
			//No session - i.e. not logged in
			//Header forces a 401 rather than a 302
			header("X-Requested-With", "XMLHttpRequest").
		when().
			get("/models").
		then().
			assertThat().statusCode(HttpStatus.SC_UNAUTHORIZED).
			and().
			assertThat().body("message", is("Unauthorized"));
	}

	/**
	 * Test of list models for a user
	 * Asserts OK 200 status
	 * Asserts number of models correct
	 * Asserts correct dates and number of each returned in list
	 */
	@Test
	public void testListModelsForUser() {
		//Insert models at modelRepository layer
		Model testModelA = createTestModel();
		Long dateA = testModelA.getCreated().getTime();

		Model testModelB = createTestModel();
		Long dateB = testModelB.getCreated().getTime();

		Model testModelC = createTestModel();
		Long dateC = testModelC.getCreated().getTime();

		//Most data nullified by populateModelInfo, use created date
		List<Long> creationDates =
			given().
				filter(adminSession).
			when().
				get("/usermodels/" + testUserId).
			then().
				assertThat().statusCode(HttpStatus.SC_OK).
			extract().
				body().path("created");

		assertEquals(3, creationDates.size());

		assertTrue(creationDates.remove(dateA));
		assertTrue(creationDates.remove(dateB));
		assertTrue(creationDates.remove(dateC));
	}

	/**
	 * Test list models for a user when no models
	 * Asserts OK 200 status
	 * Asserts no model in returned list
	 */
	@Test
	public void testListModelsForUserNoModels() {
		given().
			filter(adminSession).
		when().
			get("/usermodels/" + testUserId).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("created", empty());
	}

	/**
	 * Test list models for an unknown user
	 * Asserts NOT FOUND 404 status
	 * Asserts error message returned
	 */
	@Test
	public void testListModelsForUserUnknownUser() {
		given().
			filter(adminSession).
		when().
			get("/usermodels/notAUserID").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND).
			and().
			assertThat().body("message", is("Unknown user: notAUserID"));
	}

	/**
	 * Test creating a new model in the system
	 * Asserts CREATED 201 status
	 * Asserts returned model has correct name
	 */
	@Test
	public void testCreateModel() {
		switchToSystemModel(0);

		//User needs access to the domain model before they can create a model.
		addShieldDomainModel();
		modelHelper.setUsersForDomainModel(TestHelper.DOM_SHIELD_NAME, Arrays.asList(testUserName));

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testModelDTO).
		when().
			post("/models").
		then().
			assertThat().statusCode(HttpStatus.SC_CREATED).
			and().
			assertThat().body("name", is(testModel.getName()));
	}

	/**
	 * Test creating a new model by an admin user
	 * Asserts CREATED 201 status
	 * Asserts returned model has correct name
	 */
	@Test
	public void testCreateModelAdminUser() {
		switchToSystemModel(0);

		given().
			filter(adminSession).
			contentType(ContentType.JSON).
			body(testModelDTO).
		when().
			post("/models").
		then().
			assertThat().statusCode(HttpStatus.SC_CREATED).
			and().
			assertThat().body("name", is(testModel.getName()));
	}

	/**
	 * Test creating a new model by an unknown user
	 * Asserts UNAUTHORIZED 401 status
	 * Asserts error message returned
	 */
	@Test
	public void testCreateModelUnknownUser() {
		switchToSystemModel(0);

		given().
			//No session - i.e. not logged in
			//Header forces a 401 rather than a 302
			header("X-Requested-With", "XMLHttpRequest").
			contentType(ContentType.JSON).
			body(testModelDTO).
		when().
			post("/models").
		then().
			assertThat().statusCode(HttpStatus.SC_UNAUTHORIZED).
			and().
			assertThat().body("message", is("Unauthorized"));
	}

	/**
	 * Test creating a system model for a domain model to which the user has no access
	 * Asserts FORBIDDEN 403 status
	 * Asserts error message returned
	 */
	@Test
	public void testCreateModelNoDomainAccess() {
		switchToSystemModel(0);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testModelDTO).
		when().
			post("/models").
		then().
			assertThat().statusCode(HttpStatus.SC_FORBIDDEN).
			and().
			assertThat().body("message", is("Unauthorised access to domain model"));
	}

	/**
	 * Test of getting another user's model with read access
	 * Asserts OK 200 status
	 * Asserts returned model has correct name
	 * Asserts that the loading completes successfully
	 */
	@Test
	public void testGetReadModel() {
		switchToSystemModel(1);
		// set the model to be owned by someone else
		testModel.setUserId(keycloakAdminClient.getUserByUsername("testadmin").getId());
        Set<String> readUsernames = new HashSet<>(Arrays.asList(testUserName));
		testModel.setReadUsernames(readUsernames);

		String loadingId =
			given().
				filter(userSession). //Model owned by testuser
			when().
				get("/models/testModel").
			then().
				assertThat().statusCode(HttpStatus.SC_OK).
				and().
				assertThat().body("name", is(testModel.getName())).
			extract().
				body().path("loadingId");

		assertLoadingCompleted(loadingId);
	}

	/**
	 * Test of getting model that does not exist
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testGetInvalidModel() {
		given().
			filter(userSession).
		when().
			get("/models/notATestModel").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}
	/**
	 * Test of getPalette
	 * Asserts OK 200 status
	 * Asserts palette given in response is populated
	 */
	@Test
	// All the test models in src/test/resources/StoreTest use domain-shield.
	// However domain-shield is no longer in the palette.
	// TODO: fix this.
	@Ignore
	public void testGetPalette() {
		switchToSystemModel(1);

		given().
			filter(userSession).
		when().
			get("/models/testModel/palette").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("assets", not(empty()));
	}

	/**
	 * Testing update model with no change
	 * Asserts OK 200 status
	 */
	@Test
	public void testUpdateModel() {
		switchToSystemModel(1);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testModelDTO).
		when().
			put("/models/testModel").
		then().
			assertThat().statusCode(HttpStatus.SC_OK);
	}

	/**
	 * Test updating model by unknown user
	 * Asserts UNAUTHORIZED 401 status
	 * Assert error message returned
	 */
	@Test
	public void testUpdateModelUnknownUser() {
		switchToSystemModel(1);

		given().
			//No session - i.e. not logged in
			//Header forces a 401 rather than a 302
			header("X-Requested-With", "XMLHttpRequest").
			contentType(ContentType.JSON).
			body(testModelDTO).
		when().
			put("/models/testModel").
		then().
			assertThat().statusCode(HttpStatus.SC_UNAUTHORIZED).
			and().
			assertThat().body("message", is("Unauthorized"));
	}

	/**
	 * Tests updating unknown model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testUpdateModelInvalidModel() {
		switchToSystemModel(1);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testModelDTO).
		when().
			put("/models/notATestModel").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests checking out model
	 * Asserts OK 200 status
	 * Asserts editorID matches test user
	 */
	@Test
	public void testCheckoutModel() {
		switchToSystemModel(1);

		given().
			filter(userSession).
		when().
			post("/models/testModel/checkout").
		then().
			assertThat().statusCode(HttpStatus.SC_OK);

		assertNotNull(testModel.getEditorId());
	}

	/**
	 * Test checking out an invalid model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testCheckoutModelInvalidModel() {
		given().
			filter(userSession).
		when().
			post("/models/notATestModel/checkout").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Test checking out with an unknown user
	 * Asserts UNAUTHORIZED 401 status
	 * Asserts error message returned
	 */
	@Test
	public void testCheckoutModelUnknownUser() {
		switchToSystemModel(0);
		given().
			//No session - i.e. not logged in
			//Header forces a 401 rather than a 302
			header("X-Requested-With", "XMLHttpRequest").
		when().
			post("/models/testModel/checkout").
		then().
			assertThat().statusCode(HttpStatus.SC_UNAUTHORIZED).
			and().
			assertThat().body("message", is("Attempting access without a valid username. Are you logged in?"));
	}

	/**
	 * tests checking in a model
	 * Asserts OK 200 status
	 * Asserts Model no longer checked out
	 */
	@Test
	public void testCheckinModel() {
		switchToSystemModel(1);

		testModel.setEditorId(testUserId);

		given().
			filter(userSession).
		when().
			post("/models/testModel/checkin").
		then().
			assertThat().statusCode(HttpStatus.SC_OK);

		assertNull(testModel.getEditorId());
	}

	/**
	 * Test checking in an invalid model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testCheckinModelInvalidModel() {
		given().
			filter(userSession).
		when().
			post("/models/notATestModel/checkin").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Test checking in model that is not checked out
	 * Asserts OK 200 status
	 * Asserts model still not checked out.
	 */
	@Test
	public void testCheckinModelCheckedIn() {
		switchToSystemModel(1);

		given().
			filter(userSession).
		when().
			post("/models/testModel/checkin").
		then().
			assertThat().statusCode(HttpStatus.SC_OK);

		assertNull(testModel.getEditorId());
	}

	/**
	 * Tests checking in another user's model
	 * Asserts FORBIDDEN 403 status
	 * Asserts message says model is already checked out
	 */
	@Test
	public void testCheckinModelOtherUserIsEditor() {
		switchToSystemModel(1);

		String notTestUserId = getUserIdDistinctFromTestUserId();

		testModel.setEditorId(notTestUserId);

		given().
			filter(userSession).
		when().
			post("/models/testModel/checkin").
		then().
			assertThat().statusCode(HttpStatus.SC_LOCKED).
			and().
			assertThat().body("message", is("The model you are trying to access is already being edited by another user"));
	}

	/**
	 * Test checking in a model by unknown user
	 * Asserts UNAUTHORIZED 401 status
	 * Asserts error message returned
	 */
	@Test
	public void testCheckinModelUnknownUser() {
		switchToSystemModel(0);
		given().
			//No session - i.e. not logged in
			//Header forces a 401 rather than a 302
			header("X-Requested-With", "XMLHttpRequest").
		when().
			post("/models/testModel/checkin").
		then().
			assertThat().statusCode(HttpStatus.SC_UNAUTHORIZED).
			and().
			assertThat().body("message", is("Attempting access without a valid username. Are you logged in?"));
	}

	/**
	 * Testing delete model
	 * Asserts create model returns CREATED 201 status
	 * Asserts load model returns OK 200 status
	 * Asserts load model background task completes
	 * Asserts deleteModel returns OK 200 status
	 * Asserts load model returns NOT FOUND 404 status
	 */
	@Test
	public void testDeleteModel() {
		switchToSystemModel(0);

		//User needs access to the domain model before they can create a model.
		addShieldDomainModel();
		modelHelper.setUsersForDomainModel(TestHelper.DOM_SHIELD_NAME, Arrays.asList(testUserName));

		//Create model
		String modelWebKey =
			given().
				filter(userSession).
				contentType(ContentType.JSON).
				body(testModelDTO).
			when().
				post("/models").
			then().
				assertThat().statusCode(HttpStatus.SC_CREATED).
			extract().
				body().path("id");

		//Load model to confirm it exists
		String loadingId =
			given().
				filter(userSession).
			when().
				get("/models/" + modelWebKey).
			then().
				assertThat().statusCode(HttpStatus.SC_OK).
			extract().
				body().path("loadingId");

		assertLoadingCompleted(loadingId);

		//Delete model
		given().
			filter(userSession).
		when().
			delete("/models/" + modelWebKey).
		then().
			assertThat().statusCode(HttpStatus.SC_OK);

		//Confirm model no longer exists
		given().
			filter(userSession).
		when().
			get("/models/" + modelWebKey).
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Testing delete model (for non created model)
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testDeleteInvalidModel() {
		given().
			filter(userSession).
		when().
			delete("/models/badModelKey").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Test exporting a model from the repository
	 * Asserts OK 200 status
	 * Asserts body populated
	 */
	@Test
	public void testExport() {
		switchToSystemModel(1);

		given().
			filter(userSession).
		when().
			get("/models/testModel/export").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body(not(isEmptyOrNullString()));
	}

	/**
	 * Tests exporting an unknown model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testExportInvalidModel() {
		given().
			filter(userSession).
		when().
			get("/models/notATestModel/export").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Test exporting a model's assertions from the repository
	 * Asserts OK 200 status
	 * Asserts body is not empty (populated)
	 */
	@Test
	public void testExportAsserted() {
		switchToSystemModel(1);

		given().
			filter(userSession).
		when().
			get("/models/testModel/exportAsserted").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body(not(isEmptyOrNullString()));
	}

	/**
	 * Tests exporting an unknown model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testExportAssertedInvalidModel() {
		given().
			filter(userSession).
		when().
			get("/models/notATestModel/exportAsserted").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Test importing a model
	 * Asserts OK 200 status
	 * Asserts name returned is as expected
	 * Asserts model is valid
	 */
	@Test
	public void testImportModel() {
		//User needs access to the domain model before they can upload a model.
		addShieldDomainModel();
		modelHelper.setUsersForDomainModel(TestHelper.DOM_SHIELD_NAME, Arrays.asList(testUserName));

		//Clear the graphs into which the model will be loaded
		clearSystemModel(0);

		given().
			filter(userSession).
			multiPart(createMultiPartSpecNQ(TestHelper.SYS_SHIELD_FPATH_ABS)).
			queryParam("asserted", "false").
			queryParam("overwrite", "true"). //Ignored
		when().
			post("/models/import").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("name", contains("GA Test")).
			and().
			assertThat().body("valid", contains(true));
	}

	/**
	 * Test importing just the asserted graph of a model
	 * Asserts OK 200 status
	 * Asserts name returned is as expected
	 * Asserts model is not valid
	 */
	@Test
	public void testImportModelAssertedOnly() {
		//User needs access to the domain model before they can upload a model.
		addShieldDomainModel();
		modelHelper.setUsersForDomainModel(TestHelper.DOM_SHIELD_NAME, Arrays.asList(testUserName));

		//Clear the graphs into which the model will be loaded
		clearSystemModel(0);

		given().
			filter(userSession).
			multiPart(createMultiPartSpecNQ(TestHelper.SYS_SHIELD_FPATH_ABS)).
			queryParam("asserted", "true").
			queryParam("overwrite", "true"). //Ignored
		when().
			post("/models/import").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("name", contains("GA Test")).
			and().
			assertThat().body("valid", contains(false));
	}

	/**
	 * Test importing and renaming a model
	 * Asserts OK 200 status
	 * Asserts name returned is as expected
	 * Asserts model is not valid
	 */
	@Test
	public void testImportModelRename() {
		//User needs access to the domain model before they can upload a model.
		addShieldDomainModel();
		modelHelper.setUsersForDomainModel(TestHelper.DOM_SHIELD_NAME, Arrays.asList(testUserName));

		//Clear the graphs into which the model will be loaded
		clearSystemModel(0);

		given().
			filter(userSession).
			multiPart(createMultiPartSpecNQ(TestHelper.SYS_SHIELD_FPATH_ABS)).
			queryParam("asserted", "true").
			queryParam("overwrite", "true"). //Ignored
			queryParam("newName", "New Model Name").
		when().
			post("/models/import").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("name", contains("New Model Name")).
			and().
			assertThat().body("valid", contains(false));
	}

	/**
	 * Test importing a model that already exists
	 * Asserts OK 200 status
	 * Asserts name returned is as expected
	 * Asserts model is not valid
	 */
	@Test
	public void testImportModelDuplicate() {
		//User needs access to the domain model before they can upload a model.
		addShieldDomainModel();
		modelHelper.setUsersForDomainModel(TestHelper.DOM_SHIELD_NAME, Arrays.asList(testUserName));

		//Add the model that will be imported
		//This copy copy of the model is valid
		switchToSystemModel(0);

		given().
			filter(userSession).
			multiPart(createMultiPartSpecNQ(TestHelper.SYS_SHIELD_FPATH_ABS)).
			queryParam("asserted", "true").  //Just the asserted graph
			queryParam("overwrite", "true"). //Ignored
		when().
			post("/models/import").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("name", contains("GA Test", "GA Test")).   //Two copies of the model
			and().
			assertThat().body("valid", containsInAnyOrder(true, false)); //One valid (original), one not (duplicate)
	}

	/**
	 * Testing importing a model from a .gz file
	 * Asserts OK 200 status
	 * Asserts name returned as expected
	 * Asserts model is not valid
	 */
	@Test
	public void testImportModelGzipped() {
		//User needs access to the domain model before they can upload a model.
		addShieldDomainModel();
		modelHelper.setUsersForDomainModel(TestHelper.DOM_SHIELD_NAME, Arrays.asList(testUserName));

		//Clear the graphs into which the model will be loaded
		clearSystemModel(0);

		given().
			filter(userSession).
			multiPart(createMultiPartSpecGzipNQ(TestHelper.SYS_SHIELD_FPATH_ABS)).
			queryParam("asserted", "true").
			queryParam("overwrite", "true"). //Ignored
		when().
			post("/models/import").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("name", contains("GA Test")).
			and().
			assertThat().body("valid", contains(false));
	}

	/**
	 * Test importing a model by an admin user
	 * Asserts OK 200 status
	 * Asserts name returned is as expected
	 * Asserts model is not valid
	 */
	@Test
	public void testImportModelAdminUser() {
		//Clear the graphs into which the model will be loaded
		clearSystemModel(0);

		given().
			filter(adminSession).
			multiPart(createMultiPartSpecNQ(TestHelper.SYS_SHIELD_FPATH_ABS)).
			queryParam("asserted", "true").
			queryParam("overwrite", "true"). //Ignored
		when().
			post("/models/import").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("name", contains("GA Test")).
			and().
			assertThat().body("valid", contains(false));
	}

	/**
	 * Test importing a model by an unknown user
	 * Asserts UNAUTHORIZED 401 status
	 * Asserts error message returned
	 */
	@Test
	public void testImportModelUnknownUser() {
		given().
			//No session - i.e. not logged in
			//Header forces a 401 rather than a 302
			header("X-Requested-With", "XMLHttpRequest").
			multiPart(createMultiPartSpecNQ(TestHelper.SYS_SHIELD_FPATH_ABS)).
			queryParam("asserted", "true").
			queryParam("overwrite", "true").
		when().
			post("/models/import").
		then().
			assertThat().statusCode(HttpStatus.SC_UNAUTHORIZED).
			and().
			assertThat().body("message", is("Attempting access without a valid username. Are you logged in?"));
	}

	/**
	 * Test importing a system model for a domain model to which the user has no access
	 * Asserts FORBIDDEN 403 status
	 * Asserts error message returned
	 */
	@Test
	public void testImportModelNoDomainAccess() {
		given().
			filter(userSession).
			multiPart(createMultiPartSpecNQ(TestHelper.SYS_SHIELD_FPATH_ABS)).
			queryParam("asserted", "true").
			queryParam("overwrite", "true").
		when().
			post("/models/import").
		then().
			assertThat().statusCode(HttpStatus.SC_FORBIDDEN).
			and().
			assertThat().body("message", is("Unauthorised access to domain model"));
	}

	/**
	 * Test copying a model from the repository to a new model, with the specified name
	 * Asserts CREATED 201 status
	 * Asserts returned name is set to the copied model name (specified by the user)
	 * Asserts that the new model has a different id
	 */
	@Test
	public void testCopyModel() {
		switchToSystemModel(0);

		//User needs access to the domain model before they can create a model.
		addShieldDomainModel();
		modelHelper.setUsersForDomainModel(TestHelper.DOM_SHIELD_NAME, Arrays.asList(testUserName));

		testModelDTO.setName("Copied Model");

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testModelDTO).
		when().
			post("/models/testModel/copyModel").
		then().
			assertThat().statusCode(HttpStatus.SC_CREATED).
			and().
			assertThat().body("name", is("Copied Model")).
			and().
			assertThat().body("id", not(testModel.getId()));
	}

	/**
	 * Test get loading progress under expected conditions
	 * Asserts OK 200 status
	 * Asserts progress message matches the set message.
	 */
	@Test
	public void testGetLoadingProgress() {
		switchToSystemModel(0);

		String message = UUID.randomUUID().toString();

		EmptyLoadingTask task = new EmptyLoadingTask(message);

		given().
			filter(userSession).
		when().
			get("/models/testModel/" + testModel.getId() + "/loadingprogress").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("message", is(message));

		task.stop();
	}

	/**
	 * Test get loading progress with no registered loading progress
	 * Asserts NOT FOUND 404 status with specified error message
	 */
	@Test
	public void testGetLoadingProgressNoLoadingRegistered() {
		switchToSystemModel(0);

		given().
			filter(userSession).
		when().
			get("/models/testModel/notALoadingID/loadingprogress").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND).
			and().
			assertThat().body("message", is("Unknown model loading id: notALoadingID"));
	}

	/**
	 * Test get loading progress with undefined loadingId
	 * Asserts BAD REQUEST 400 status with specified error message
	 */
	@Test
	public void testGetLoadingProgressForUndefinedLoadingId() {
		switchToSystemModel(0);

		given().
			filter(userSession).
		when().
			get("/models/testModel/undefined/loadingprogress").
		then().
			assertThat().statusCode(HttpStatus.SC_BAD_REQUEST).
			and().
			assertThat().body("message", is("Undefined loadingID in URL"));
	}

	/**
	 * Test get loading progress with invalid model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testGetLoadingProgressInvalidModel() {
		given().
			filter(userSession).
		when().
			get("/models/notATestModel/notALoadingID/loadingprogress").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Test calculating future risks
	 * Asserts flag is false before calling REST method
	 * Asserts ACCEPTED 202 status
	 * Asserts flag is true after calling REST method
	 * Asserts riskCalculationMode model parameter is set to FUTURE
	 */
	@Test
	public void testCalculateRisksFuture() {
		switchToSystemModel(2, 9); //use population domain and system model

		assertFalse(testModel.isCalculatingRisks());

		given().
			filter(userSession).
			param("mode", "FUTURE").
		when().
			get("/models/testModel/calc_risks").
		then().
			assertThat().statusCode(HttpStatus.SC_ACCEPTED);

		assertTrue(testModel.isCalculatingRisks());

		assertTaskCompleted();

		assertEquals(RiskCalculationMode.FUTURE, querier.getModelInfo(storeModelManager.getStore()).getRiskCalculationMode());
	}

	/**
	 * Test calculating recommendations for model (blocking call)
	 * Asserts OK 200 status
	 */
	@Test
	public void testRecommendations() {
		switchToSystemModel(4, 12); //use recommendations domain and system model

		given().
			filter(userSession).
		when().
			get("/models/testModel/recommendations_blocking").
		then().
			assertThat().statusCode(HttpStatus.SC_OK);
	}

	/**
	 * Test calculating current risks
	 * Asserts flag is false before calling REST method
	 * Asserts ACCEPTED 202 status
	 * Asserts flag is true after calling REST method
	 * Asserts riskCalculationMode model parameter is set to CURRENT
	 */
	@Test
	public void testCalculateRisksCurrent() {
		switchToSystemModel(2, 9); //use population domain and system model

		assertFalse(testModel.isCalculatingRisks());

		given().
			filter(userSession).
			param("mode", "CURRENT").
		when().
			get("/models/testModel/calc_risks").
		then().
			assertThat().statusCode(HttpStatus.SC_ACCEPTED);

		assertTrue(testModel.isCalculatingRisks());

		assertTaskCompleted();

		assertEquals(RiskCalculationMode.CURRENT, querier.getModelInfo(storeModelManager.getStore()).getRiskCalculationMode());
	}

	/**
	 * Test calculating risk with invalid mode
	 * Asserts BAD REQUEST 400 status
	 */
	@Test
	public void testCalculateRisksWithInvalidModeParameter() {
		switchToSystemModel(0);

		given().
			filter(userSession).
			param("mode", "INVALID_PARAM_VALUE").
		when().
			get("/models/testModel/calc_risks").
		then().
			assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);
	}

	/**
	 * Test calculating risks while model is validating
	 * Asserts Model is not calculating risks before calling REST method
	 * Asserts ACCEPTED 202 status
	 * Asserts Model is not calculating risks after calling REST method
	 */
	@Test
	public void testCalculateRisksModelValidating() {
		switchToSystemModel(0);

		testModel.markAsValidating();

		assertFalse(testModel.isCalculatingRisks());

		given().
			filter(userSession).
			param("mode", "FUTURE").
		when().
			get("/models/testModel/calc_risks").
		then().
			assertThat().statusCode(HttpStatus.SC_ACCEPTED);

		assertFalse(testModel.isCalculatingRisks());
	}

	/**
	 * Test calculating risks with invalid model id
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testCalculateRisksInvalidModel() {
		given().
			filter(userSession).
			param("mode", "FUTURE").
		when().
			get("/models/notATestModel/calc_risks").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests calculate risks while model is already calculating risks
	 * Asserts ACCEPTED 202 status
	 * Asserts Model calculating risks after REST method called
	*/
	@Test
	public void testCalculateRisksWhenCalculatingRisks() {
		switchToSystemModel(0);

		testModel.markAsCalculatingRisks(RiskCalculationMode.FUTURE, true);

		given().
			filter(userSession).
			param("mode", "FUTURE").
		when().
			get("/models/testModel/calc_risks").
		then().
			assertThat().statusCode(HttpStatus.SC_ACCEPTED);

		assertTrue(testModel.isCalculatingRisks());
	}

	/**
	 * Tests validate model
	 * Asserts model not validating before REST call
	 * Asserts ACCEPTED 202 status
	 * Asserts model validating after REST call
	 */
	@Test
	public void testValidateModel() {
		switchToSystemModel(0);

		assertFalse(testModel.isValidating());

		given().
			filter(userSession).
		when().
			get("/models/testModel/validated").
		then().
			assertThat().statusCode(HttpStatus.SC_ACCEPTED);

		assertTrue(testModel.isValidating());

		assertTaskCompleted();
	}

	/**
	 * Test validate model when model already validating
	 * Asserts ACCEPTED 202 status
	 * Asserts model validating after REST call
	 * //TODO: make better
	 */
	@Test
	public void testValidateModelWhenValidating() {
		switchToSystemModel(0);

		testModel.markAsValidating();

		given().
			filter(userSession).
		when().
			get("/models/testModel/validated").
		then().
			assertThat().statusCode(HttpStatus.SC_ACCEPTED);

		assertTrue(testModel.isValidating());
	}

	/**
	 * Test validate model for an invalid model id
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testValidateModelInvalidModel() {
		given().
			filter(userSession).
		when().
			get("/models/notATestModel/validated").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Test getting progress of validation
	 * Asserts OK 200 status
	 * Asserts progress message is as set.
	 */
	@Test
	public void testGetValidationProgress() {
		switchToSystemModel(0);

		String message = UUID.randomUUID().toString();

		EmptyBackgroundTask task = new EmptyBackgroundTask(message);

		given().
			filter(userSession).
		when().
			get("/models/testModel/validationprogress").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("message", is(message));

		task.stop();
	}

	/**
	 * Tests get validation progress for an unknown model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testGetValidationProgressInvalidModel() {
		given().
			filter(userSession).
		when().
			get("/models/notATestModel/validationprogress").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Test getting progress of risk calculation
	 * Asserts OK 200 status
	 * Asserts progress message is as set.
	 */
	@Test
	public void testGetRiskCalculationProgress() {
		switchToSystemModel(0);

		String message = UUID.randomUUID().toString();

		EmptyBackgroundTask task = new EmptyBackgroundTask(message);

		given().
			filter(userSession).
		when().
			get("/models/testModel/riskcalcprogress").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("message", is(message));

		task.stop();
	}

	/**
	 * Tests get risk calculation progress for an unknown model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testGetRiskCalculationProgressInvalidModel() {
		given().
			filter(userSession).
		when().
			get("/models/notATestModel/riskcalcprogress").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Testing JSON report generation
	 * Asserts OK 200 status
	 * Asserts basic elements of JSON report are present
	 */
	@Test
	public void testGenerateReport() {
		switchToSystemModel(0);

		given().
			filter(userSession).
		when().
			get("/models/testModel/report").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("threats", not(empty())).
			and().
			assertThat().body("assertedAssets", not(empty())).
			and().
			assertThat().body("inferredAssets", not(empty())).
			and().
			assertThat().body("relations", not(empty())).
			and().
			assertThat().body("misbehaviours", not(empty())).
			and().
			assertThat().body("controls", not(empty())).
			and().
			assertThat().body("controlStrategies", not(empty())).
			and().
			assertThat().body("trustworthinessAttributes", not(empty()));
	}

	/**
	 * Tests getting basic model info for a validated and an unvalidated model
	 * Asserts OK 200 status
	 * Asserts there are 32 key/value pairs
	 * Asserts basic flags are correct for the respective models, and that all other model data collections are empty
	 */
	@Test
	public void testGetModelInfo() {
		switchToSystemModel(0);

		given().
			filter(userSession).
		when().
			get("models/testModel/info").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("size()", is(32)).
			and().
			assertThat().body("valid", is(true)).
			and().
			assertThat().body("riskLevelsValid", is(true)).
			and().
			assertThat().body("riskCalculationMode", isEmptyOrNullString()).
			and().
			assertThat().body("threats", empty()).
			and().
			assertThat().body("complianceThreats", empty()).
			and().
			assertThat().body("complianceSets", empty()).
			and().
			assertThat().body("misbehaviourSets", is(new HashMap<>())).
			and().
			assertThat().body("twas", is(new HashMap<>())).
			and().
			assertThat().body("controlSets", empty()).
			and().
			assertThat().body("levels", is(new HashMap<>())).
			and().
			assertThat().body("assets", empty()).
			and().
			assertThat().body("relations", empty()).
			and().
			assertThat().body("groups", empty());

		switchToSystemModel(2);

		given().
			filter(userSession).
		when().
			get("models/testModel/info").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("valid", is(false)).
			and().
			assertThat().body("riskLevelsValid", is(false)).
			and().
			assertThat().body("riskCalculationMode", isEmptyOrNullString());
	}
}
