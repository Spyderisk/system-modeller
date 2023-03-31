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
//      Created Date :          31/07/2018
//      Created for Project :   RESTASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import io.restassured.http.ContentType;
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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import uk.ac.soton.itinnovation.security.model.Level;
import uk.ac.soton.itinnovation.security.model.system.MisbehaviourSet;
import uk.ac.soton.itinnovation.security.model.system.Threat;
import uk.ac.soton.itinnovation.security.modelquerier.SystemModelQuerier;
import uk.ac.soton.itinnovation.security.systemmodeller.TestHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.auth.KeycloakAdminClient;
import uk.ac.soton.itinnovation.security.systemmodeller.CommonTestSetup;
import uk.ac.soton.itinnovation.security.systemmodeller.SystemModellerApplication;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.model.ModelFactory;
import uk.ac.soton.itinnovation.security.systemmodeller.model.WebKeyRole;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.util.SecureUrlHelper;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SystemModellerApplication.class, webEnvironment = RANDOM_PORT)
public class ThreatControllerTest extends CommonTestSetup{

	private static final Logger logger = LoggerFactory.getLogger(ThreatControllerTest.class);

	//System objects, spying on secureUrlHelper to control model access
	@Autowired
	private ModelObjectsHelper modelHelper;

	@Autowired
	private ModelFactory modelFactory;

	@Autowired
	private KeycloakAdminClient keycloakAdminClient;

	@SpyBean
	private SecureUrlHelper secureUrlHelper;

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
	private String testUserId;
	private static TestHelper testHelper;
	private static SystemModelQuerier querier;

	@BeforeClass
	public static void beforeClass() {
		logger.info("Setting up ThreatControllerTest class");
		testHelper = new TestHelper("jena-tdb");
		testHelper.setUp();
		querier = new SystemModelQuerier(testHelper.getModel());
	}

	@Before
	public void init() {
		logger.info("Executing {}", name.getMethodName());
		initAuth(contextPath, port);
		testUserId = keycloakAdminClient.getUserByUsername(testUserName).getId();
	}

	//Utilities

	/**
	 * Sets up testHelper and reloads the given graph, creates matching Model object 
	 * in testModel. The threats cached in modelHelper are refreshed for this model
	 * and the secureUrlHelper spy is set up to return the model when queried with "testModel"
	 * @param modelIndex The index of loaded models to be used (see testHelper)
	 */
	private void switchToSystemModel(int modelIndex) {
		testHelper.switchModels(0, modelIndex);

		//model graph(s) already in Jena - but model not in Mongo or the management graph
		testModel = modelFactory.getModel(testHelper.getModel(), testHelper.getStore());

		modelHelper.getThreatsForModel(testModel, true);
		testModel.setUserId(testUserId);
		testModel.save();

		//bypass looking model up in Mongo via WebKey (the WebKey is NOT the model ID)
		when(secureUrlHelper.getModelFromUrl("testModel")).thenReturn(testModel);
		when(secureUrlHelper.getRoleFromUrl("testModel")).thenReturn(WebKeyRole.NONE);
		when(secureUrlHelper.getWriteModelFromUrl("testModel")).thenReturn(testModel);
		when(secureUrlHelper.getReadModelFromUrl("testModel")).thenReturn(testModel);
	}

	//Tests

	/**
	 * Tests getting threats for a model with 14 threats
	 * Asserts OK 200 status
	 * Asserts response body has 14 threat IDs
	 */
	@Test
	public void testGetThreats() {
		switchToSystemModel(3);

		given().
			filter(userSession).
		when().
			get("/models/testModel/threats").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("id", iterableWithSize(14));
	}

	/**
	 * Tests getting threats for a model with no threats.
	 * Asserts OK 200 status
	 * Asserts response contains no threats.
	 */
	@Test
	public void testGetThreatsNoThreatsInModel() {
		switchToSystemModel(1);

		given().
			filter(userSession).
		when().
			get("/models/testModel/threats").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("id", empty());
	}

	/**
	 * Tests getting threats for an invalid model ID (secureUrlHelper will return null model)
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testGetThreatsInvalidModel() {
		given().
			filter(userSession).
		when().
			get("/models/notATestModel/threats").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests getting a particular threat in a model
	 * Threat ID "18f97f2e" taken from Host-Process-Connected-Validated.nq (model 3)
	 * Asserts OK 200 status
	 * Asserts threat label in body is as expected
	 */
	@Test
	public void testGetThreatInModel() {
		switchToSystemModel(3);

		given().
			filter(userSession).
		when().
			get("/models/testModel/threats/18f97f2e").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("label", is("P.A.P.2_P_Process5le6y"));
	}
	
	/**
	 * Tests getting a threat that does not exist in a model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testGetThreatInModelInvalidThreatId() {
		switchToSystemModel(3);

		given().
			filter(userSession).
		when().
			get("/models/testModel/threats/notAThreatID").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}
	
	/**
	 * Tests getting a threat from an invalid model ID
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testGetThreatInModelInvalidModel() {
		given().
			filter(userSession).
		when().
			get("/models/notATestModel/threats/irrelevantID").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests accepting a known threat in a known model
	 * Threat ID "18f97f2e" taken from Host-Process-Connected-Validated.nq (model 3)
	 * Asserts OK 200 status
	 */
	@Test
	public void testAcceptThreat() {
		switchToSystemModel(3);

		Threat testThreat = querier.getSystemThreatById(testHelper.getStore(), "18f97f2e");

		testThreat.setAcceptanceJustification("TestAcceptance");

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testThreat).
		when().
			post("/models/testModel/threats/18f97f2e/accept").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("acceptanceJustification", is("TestAcceptance"));
	}

	/**
	 * Test accepting a threat multiple times, focuses issue in shield-review-sep-2018 first deployment
	 * Asserts OK 200 status
	 * Asserts Acceptance Justification returned
	 * Asserts threat resolved in store
	 * Above asserted for ALL threats in model
	 */
	@Test
	public void testAcceptThreatMultipleThreats() {
		switchToSystemModel(3);

		for (Threat t: querier.getSystemThreats(testHelper.getStore()).values()) {
			String id = t.getID();
			String aj = "Accepted threat " + id;

			t.setAcceptanceJustification(aj);
			
			given().
				filter(userSession).
				contentType(ContentType.JSON).
				body(t).
			when().
				post("/models/testModel/threats/" + id + "/accept").
			then().
				assertThat().statusCode(HttpStatus.SC_OK).
				and().
				assertThat().body("acceptanceJustification", is(aj));
		}
	}

	/**
	 * Tests accepting a known threat in a known model with no justification
	 * Asserts OK 200 status
	 * Asserts justification still null.
	 */
	@Test
	public void testAcceptThreatNoJustification() {
		switchToSystemModel(3);

		Threat testThreat = querier.getSystemThreatById(testHelper.getStore(), "18f97f2e");

		testThreat.setAcceptanceJustification(null);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testThreat).
		when().
			post("/models/testModel/threats/18f97f2e/accept").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("acceptanceJustification", nullValue());
	}

	/**
	 * Tests accepting a threat that does not exist in a model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testAcceptThreatInvalidThreat() {
		switchToSystemModel(3);

		Threat testThreat = new Threat();

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testThreat).
		when().
			post("/models/testModel/threats/" + testThreat.getID() + "/accept").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests accepting a threat for an unknown model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testAcceptThreatInvalidModel() {
		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(new Threat()).
		when().
			post("/models/notATestModel/threats/irrelevantID/accept").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests updating the impact level of a misbehaviour set from low to very high
	 * MS URI taken from Host-Process-Connected-Validated.nq file (model 3)
	 * ImpactLevel URI taken from domain model. (dependent on domain-shield in StoreTest)
	 * Asserts OK 200 status
	 * Asserts returned impact level URI matches the assigned value
	 */
	@Test
	public void testUpdateMisbehaviourImpact() {
		switchToSystemModel(3);

		Level updatedLevel = new Level();
		updatedLevel.setValue(4);
		updatedLevel.setLabel("Very High");
		updatedLevel.setUri("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#ImpactLevelVeryHigh");

		MisbehaviourSet testMS = querier
			.getMisbehaviourSet(
				testHelper.getStore(),
				"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#MS-LossOfControl-3e77c20d",
				false
			); //no need for causes and effects here

		testMS.setImpactLevel(updatedLevel);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testMS).
		when().
			put("/models/testModel/misbehaviours/" + testMS.getID() + "/impact").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body(is("completed"));
	}

	/**
	 * Tests updating a misbehaviour impact for an unknown misbehaviour
	 * Misbehaviour taken from model 3, attempt to update on model 0
	 * Current behaviour is to return OK with no change, could return 404 instead
	 * Asserts OK 200 status
	 */
	@Test
	public void testUpdateMisbehaviourImpactUnknownMisbehaviour() {
		switchToSystemModel(3);

		MisbehaviourSet testMS = querier
			.getMisbehaviourSet(
				testHelper.getStore(),
				"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#MS-LossOfControl-3e77c20d",
				false
			);//no need for causes and effects here

		switchToSystemModel(0);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testMS).
		when().
			put("/models/testModel/misbehaviours/" + testMS.getID() + "/impact").
		then().
			assertThat().statusCode(HttpStatus.SC_OK);
	}

	/**
	 * Tests updating misbehaviour impact on an unknown model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testUpdateMisbehaviourImpactUnknownModel() {
		switchToSystemModel(3);

		MisbehaviourSet testMS = querier
			.getMisbehaviourSet(
				testHelper.getStore(),
				"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#MS-LossOfControl-3e77c20d",
				false
			); //no need for causes and effects here

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testMS).
		when().
			put("/models/notATestModel/misbehaviours/" + testMS.getID() + "/impact").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests getting threats for a model with 26 control sets
	 * Asserts OK 200 status
	 * Asserts response body has 26 control sets
	 */
	@Test
	public void testGetControlSets() {
		switchToSystemModel(3);

		given().
			filter(userSession).
		when().
			get("/models/testModel/controlsets").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("size()", is(26));
	}

	/**
	 * Tests getting control sets for a model with no control sets
	 * Asserts OK 200 status
	 * Asserts response contains no control sets
	 */
	@Test
	public void testGetControlSetsNoControlSetsInModel() {
		switchToSystemModel(1);

		given().
			filter(userSession).
		when().
			get("/models/testModel/controlsets").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("size()", is(0));
	}

	/**
	 * Tests getting control sets for an invalid model ID (secureUrlHelper will return null model)
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testGetControlSetsInvalidModel() {
		given().
			filter(userSession).
		when().
			get("/models/notATestModel/controlsets").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}
	
	/**
	 * Tests getting controls from domain model
	 * Asserts OK 200 status
	 * Asserts there are 36 controls
	 */
	@Test
	public void testGetControls() {
		switchToSystemModel(2);
	
		given().
			filter(userSession).
		when().
			get("/models/testModel/controls").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("size()", is(36));	
	}
	
}
