/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2023
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
//      Created By :            Ken Meacham
//      Created Date :          06/10/2023
//      Created for Project :   Cyberkit4SME
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import static io.restassured.RestAssured.given;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
import uk.ac.soton.itinnovation.security.model.Level;
import uk.ac.soton.itinnovation.security.model.system.Asset;
import uk.ac.soton.itinnovation.security.model.system.ControlSet;
import uk.ac.soton.itinnovation.security.model.system.MetadataPair;
import uk.ac.soton.itinnovation.security.model.system.Threat;
import uk.ac.soton.itinnovation.security.model.system.TrustworthinessAttributeSet;
import uk.ac.soton.itinnovation.security.modelquerier.SystemModelQuerier;
import uk.ac.soton.itinnovation.security.systemmodeller.CommonTestSetup;
import uk.ac.soton.itinnovation.security.systemmodeller.SystemModellerApplication;
import uk.ac.soton.itinnovation.security.systemmodeller.TestHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.auth.KeycloakAdminClient;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.model.ModelFactory;
import uk.ac.soton.itinnovation.security.systemmodeller.model.WebKeyRole;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.util.SecureUrlHelper;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SystemModellerApplication.class, webEnvironment = RANDOM_PORT)
public class EntityControllerTest extends CommonTestSetup {

	private static final Logger logger = LoggerFactory.getLogger(EntityControllerTest.class);

	//System objects, spying on secureUrlHelper to control model access
	@Autowired
	private ModelObjectsHelper modelHelper;

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
	private String testUserId;
	private static TestHelper testHelper;
	private static SystemModelQuerier querier;

	@BeforeClass
	public static void beforeClass() {
		logger.info("Setting up EntityControllerTest class");
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
		testHelper.switchModels(3, modelIndex);

		//model graph(s) already in Jena - but model not in Mongo or the management graph
		testModel = modelFactory.getModel(testHelper.getModel(), testHelper.getStore());

		//modelHelper.getAssetsForModel(testModel, true);
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
	 * Testing getting asserted assets from model 10 (12 assets in model)
	 * Asserts OK 200 status
	 * Asserts 12 assets in returned JSON
	 */
	@Test
	public void testGetAssertedAssets() {
		switchToSystemModel(10);

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/system/assets").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("size()", is(12));
	}

	/**
	 * Testing getting all assets from model 11 (59 assets in model)
	 * Asserts OK 200 status
	 * Asserts 59 assets in returned JSON
	 */
	@Test
	public void testGetAllAssets() {
		switchToSystemModel(11);

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/system/assets").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("size()", is(59));
	}

	/**
	 * Test getting assets from an invalid model ID
	 * Assert NOT FOUND 404 status
	 */
	@Test
	public void testGetAssetsInvalidModel() {
		given().
			filter(userSession).
		when().
			get("/models/notATestModel/entity/system/assets").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Test getting a known asset from a model
	 * Asset URI queried is "system#b3007cf5"
	 * Asserts OK 200 status
	 * Asserts URI returned is as expected
	 */
	@Test
	public void testGetAssetInModel() {
		switchToSystemModel(10);

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/system/assets/system#b3007cf5").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("uri", is("system#b3007cf5"));
	}

	/**
	 * Test get invalid asset from model
	 * Asserts NOT FOUND 404 returned
	 */
	@Test
	public void testGetAssetInModelInvalidAsset() {
		switchToSystemModel(11);

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/system/assets/system#notAnAssetID").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Test getting asset from an invalid model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testGetAssetModelInvalidModel() {
		given().
			filter(userSession).
		when().
			get("/models/notATestModel/entity/system/assets/notAnAssetID").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Testing getting all threats from model 11 (550 threats in model)
	 * Asserts OK 200 status
	 * Asserts 550 threats in returned JSON
	 */
	@Test
	public void testGetThreats() {
		switchToSystemModel(11);

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/system/threats").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("size()", is(550));
	}

	/**
	 * Test getting a known threat from a model
	 * Threat URI queried is "system#H.L.IoH.3-MP-IoH_3be54ba5_1c22bad9_a51becab_7ce5d07_3be54ba5_2b9585d2"
	 * Asserts OK 200 status
	 * Asserts URI returned is as expected
	 */
	@Test
	public void testGetThreat() {
		switchToSystemModel(11);

		String threat = "system#H.L.IoH.3-MP-IoH_3be54ba5_1c22bad9_a51becab_7ce5d07_3be54ba5_2b9585d2";

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/system/threats/" + threat).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("uri", is(threat));
	}

	/**
	 * Testing getting all misbehaviourSets from model 11 (388 misbehaviourSets in model)
	 * Asserts OK 200 status
	 * Asserts 388 misbehaviourSets in returned JSON
	 */
	@Test
	public void testGetMisbehaviourSets() {
		switchToSystemModel(11);

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/system/misbehaviourSets").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("size()", is(388));
	}

	/**
	 * Test getting a known misbehaviourSet from a model
	 * MisbehaviourSet URI queried is "system#MS-LossOfAuthenticity-a40e98cc"
	 * Asserts OK 200 status
	 * Asserts URI returned is as expected
	 */
	@Test
	public void testGetMisbehaviourSet() {
		switchToSystemModel(11);

		String misbehaviourSet = "system#MS-LossOfAuthenticity-a40e98cc";

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/system/misbehaviourSets/" + misbehaviourSet).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("uri", is(misbehaviourSet));
	}

	/**
	 * Testing getting all controlStrategies (CSGs) from model 11 (255 CSGs in model)
	 * Asserts OK 200 status
	 * Asserts 255 controlStrategies in returned JSON
	 */
	@Test
	public void testGetControlStrategies() {
		switchToSystemModel(11);

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/system/controlStrategies").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("size()", is(255));
	}

	/**
	 * Test getting a known controlStrategy (CSG) from a model
	 * ControlStrategy URI queried is "system#CSG-ChipAndPinAccessControlAtHost_M_cae3ab52_ea018765"
	 * Asserts OK 200 status
	 * Asserts URI returned is as expected
	 */
	@Test
	public void testGetControlStrategy() {
		switchToSystemModel(11);

		String controlStrategy = "system#CSG-ChipAndPinAccessControlAtHost_M_cae3ab52_ea018765";

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/system/controlStrategies/" + controlStrategy).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("uri", is(controlStrategy));
	}

	/**
	 * Testing getting all controlSets from model 11 (323 controlSets in model)
	 * Asserts OK 200 status
	 * Asserts 323 controlSets in returned JSON
	 */
	@Test
	public void testGetControlSetsNoControlSetsInModel() {
		switchToSystemModel(11);

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/system/controlSets").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("size()", is(323));
	}

	/**
	 * Test getting a known controlSet from a model
	 * ControlSet URI queried is "system#CS-PenetrationTesting-a51becab"
	 * Asserts OK 200 status
	 * Asserts URI returned is as expected
	 */
	@Test
	public void testGetControlSet() {
		switchToSystemModel(11);

		String controlSet = "system#CS-PenetrationTesting-a51becab";

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/system/controlSets/" + controlSet).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("uri", is(controlSet));
	}

	/**
	 * Testing getting all trustworthinessAttributeSets (TWASs) from model 11 (362 trustworthinessAttributeSets in model)
	 * Asserts OK 200 status
	 * Asserts 362 trustworthinessAttributeSets in returned JSON
	 */
	@Test
	public void testGetTWASs() {
		switchToSystemModel(11);

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/system/trustworthinessAttributeSets").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("size()", is(362));
	}

	/**
	 * Test getting a known trustworthinessAttributeSet (TWAS) from a model
	 * TWAS URI queried is "system#TWAS-ExploitTW-40cad76f"
	 * Asserts OK 200 status
	 * Asserts URI returned is as expected
	 */
	@Test
	public void testGetTWAS() {
		switchToSystemModel(11);

		String trustworthinessAttributeSet = "system#TWAS-ExploitTW-40cad76f";

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/system/trustworthinessAttributeSets/" + trustworthinessAttributeSet).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("uri", is(trustworthinessAttributeSet));
	}


	// Domain model queries

	/**
	 * Testing getting all trustworthinessAttributes (TWAs) from domain model used by system model 11 (159 trustworthinessAttributes in domain model)
	 * Asserts OK 200 status
	 * Asserts 159 trustworthinessAttributes in returned JSON
	 */
	@Test
	public void testGetDomainTWAs() {
		switchToSystemModel(11);

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/domain/trustworthinessAttributes").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("size()", is(159));
	}

	/**
	 * Test getting a known trustworthinessAttribute (TWA) from domain model used by system model 11
	 * TWA URI queried is "domain#Authenticity"
	 * Asserts OK 200 status
	 * Asserts URI returned is as expected
	 */
	@Test
	public void testGetDomainTWA() {
		switchToSystemModel(11);

		String trustworthinessAttribute = "domain#Authenticity";

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/domain/trustworthinessAttributes/" + trustworthinessAttribute).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("uri", is(trustworthinessAttribute));
	}

	/**
	 * Testing getting all controls from domain model used by system model 11 (429 controls in domain model)
	 * Asserts OK 200 status
	 * Asserts 429 controls in returned JSON
	 */
	@Test
	public void testGetDomainControls() {
		switchToSystemModel(11);

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/domain/controls").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("size()", is(429));
	}

	/**
	 * Test getting a known control from domain model used by system model 11
	 * Control URI queried is "domain#AccessKey"
	 * Asserts OK 200 status
	 * Asserts URI returned is as expected
	 */
	@Test
	public void testGetDomainControl() {
		switchToSystemModel(11);

		String control = "domain#AccessKey";

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/domain/controls/" + control).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("uri", is(control));
	}

	/**
	 * Testing getting all misbehaviours from domain model used by system model 11 (177 misbehaviours in domain model)
	 * Asserts OK 200 status
	 * Asserts 177 misbehaviours in returned JSON
	 */
	@Test
	public void testGetDomainMisbehaviours() {
		switchToSystemModel(11);

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/domain/misbehaviours").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("size()", is(177));
	}

	/**
	 * Test getting a known misbehaviour from domain model used by system model 11
	 * Misbehaviour URI queried is "domain#LossOfControl"
	 * Asserts OK 200 status
	 * Asserts URI returned is as expected
	 */
	@Test
	public void testGetDomainMisbehaviour() {
		switchToSystemModel(11);

		String misbehaviour = "domain#LossOfControl";

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/domain/misbehaviours/" + misbehaviour).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("uri", is(misbehaviour));
	}

	/**
	 * Testing getting all impact levels from domain model used by system model 11 (6 levels in domain model)
	 * Asserts OK 200 status
	 * Asserts 6 levels in returned JSON
	 */
	@Test
	public void testGetDomainImpactLevels() {
		switchToSystemModel(11);

		String metric = "impact";

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/domain/levels/" + metric).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("size()", is(6));
	}

	/**
	 * Testing getting all population levels from domain model used by system model 11 (5 levels in domain model)
	 * Asserts OK 200 status
	 * Asserts 5 levels in returned JSON
	 */
	@Test
	public void testGetDomainPopulationLevels() {
		switchToSystemModel(11);

		String metric = "population";

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/domain/levels/" + metric).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("size()", is(5));
	}

	/**
	 * Testing getting all likelihood levels from domain model used by system model 11 (6 levels in domain model)
	 * Asserts OK 200 status
	 * Asserts 6 levels in returned JSON
	 */
	@Test
	public void testGetDomainLikelihoodLevels() {
		switchToSystemModel(11);

		String metric = "likelihood";

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/domain/levels/" + metric).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("size()", is(6));
	}

	/**
	 * Testing getting all trustworthiness levels from domain model used by system model 11 (6 levels in domain model)
	 * Asserts OK 200 status
	 * Asserts 6 levels in returned JSON
	 */
	@Test
	public void testGetDomainTrustworthinessLevels() {
		switchToSystemModel(11);

		String metric = "trustworthiness";

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/domain/levels/" + metric).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("size()", is(6));
	}

	/**
	 * Testing getting all risk levels from domain model used by system model 11 (5 levels in domain model)
	 * Asserts OK 200 status
	 * Asserts 5 levels in returned JSON
	 */
	@Test
	public void testGetDomainRiskLevels() {
		switchToSystemModel(11);

		String metric = "risk";

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/domain/levels/" + metric).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("size()", is(5));
	}

	/**
	 * Testing getting specific impact level from domain model used by system model 11
	 * Asserts OK 200 status
	 * Asserts URI returned is as expected
	 */
	@Test
	public void testGetDomainImpactLevel() {
		switchToSystemModel(11);

		String metric = "impact";
		String levelUri = "domain#ImpactLevelHigh";

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/domain/levels/" + metric + "/" + levelUri).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("uri", is(levelUri));
	}

	/**
	 * Testing getting specific population level from domain model used by system model 11
	 * Asserts OK 200 status
	 * Asserts URI returned is as expected
	 */
	@Test
	public void testGetDomainPopulationLevel() {
		switchToSystemModel(11);

		String metric = "population";
		String levelUri = "domain#PopLevelVeryHigh";

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/domain/levels/" + metric + "/" + levelUri).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("uri", is(levelUri));
	}

	/**
	 * Testing getting specific likelihood level from domain model used by system model 11
	 * Asserts OK 200 status
	 * Asserts URI returned is as expected
	 */
	@Test
	public void testGetDomainLikelihoodLevel() {
		switchToSystemModel(11);

		String metric = "likelihood";
		String levelUri = "domain#LikelihoodHigh";

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/domain/levels/" + metric + "/" + levelUri).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("uri", is(levelUri));
	}

	/**
	 * Testing getting specific trustworthiness level from domain model used by system model 11
	 * Asserts OK 200 status
	 * Asserts URI returned is as expected
	 */
	@Test
	public void testGetDomainTrustworthinessLevel() {
		switchToSystemModel(11);

		String metric = "trustworthiness";
		String levelUri = "domain#TrustworthinessLevelHigh";

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/domain/levels/" + metric + "/" + levelUri).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("uri", is(levelUri));
	}

	/**
	 * Testing getting specific risk level from domain model used by system model 11
	 * Asserts OK 200 status
	 * Asserts URI returned is as expected
	 */
	@Test
	public void testGetDomainRiskLevel() {
		switchToSystemModel(11);

		String metric = "risk";
		String levelUri = "domain#RiskLevelHigh";

		given().
			filter(userSession).
		when().
			get("/models/testModel/entity/domain/levels/" + metric + "/" + levelUri).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("uri", is(levelUri));
	}

}
