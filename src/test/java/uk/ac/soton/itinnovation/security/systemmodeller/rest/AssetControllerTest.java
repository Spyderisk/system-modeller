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
//      Created Date :          01/08/2018
//      Created for Project :   RESTASSURED
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
public class AssetControllerTest extends CommonTestSetup{

	private static final Logger logger = LoggerFactory.getLogger(AssetControllerTest.class);

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
		logger.info("Setting up AssetControllerTest class");
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

		modelHelper.getAssetsForModel(testModel, true);
		testModel.setUserId(testUserId);
		testModel.save();

		//bypass looking model up in Mongo via WebKey (the WebKey is NOT the model ID)
		when(secureUrlHelper.getModelFromUrl("testModel")).thenReturn(testModel);
		when(secureUrlHelper.getRoleFromUrl("testModel")).thenReturn(WebKeyRole.NONE);
		when(secureUrlHelper.getWriteModelFromUrl("testModel")).thenReturn(testModel);
		when(secureUrlHelper.getReadModelFromUrl("testModel")).thenReturn(testModel);
	}

	/**
	 * Ensure that a given asset has a population level set (may not be the case in older system models)
	 * @param asset
	 */
	private void ensureAssetPopulation(Asset asset) {
		if (asset.getPopulation() == null) {
			//Set default population level
			asset.setPopulation("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#PopLevelSingleton");
		}
	}

	//Tests

	/**
	 * Testing getting assets from model 1 (2 assets in model)
	 * Asserts OK 200 status
	 * Asserts 2 assets in returned JSON
	 */
	@Test
	public void testGetAssets() {
		switchToSystemModel(1);

		given().
			filter(userSession).
		when().
			get("/models/testModel/assets").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("id", iterableWithSize(2));
	}

	/**
	 * Testing getting assets from a blank model
	 * Asserts OK 200 status
	 * Asserts 0 assets returned in JSON
	 */
	@Test
	public void testGetAssetsNoAssetsInModel() {
		switchToSystemModel(5);

		given().
			filter(userSession).
		when().
			get("/models/testModel/assets").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("id", empty());
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
			get("/models/notATestModel/assets").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Test adding an asset to a model
	 * Asserts CREATED 201 status
	 * Asserts returned asset label matches
	 * Asserts 1 asset now in model
	 */
	@Test
	public void testAddAssetToModel() {
		switchToSystemModel(1);

		Asset testAsset = querier.getSystemAssets(testHelper.getStore()).values().iterator().next();
		ensureAssetPopulation(testAsset);

		switchToSystemModel(5);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAsset).
		when().
			post("/models/testModel/assets").
		then().
			assertThat().statusCode(HttpStatus.SC_CREATED).
			and().
			assertThat().body("asset.label", is(testAsset.getLabel()));

		assertEquals(1, querier.getSystemAssets(testHelper.getStore()).size());
	}

	/**
	 * Test adding identical asset to model, current behaviour is to add asset with new ID
	 * Asserts CREATED 201 status
	 * Asserts label returned matches label sent
	 * Asserts number of assets is now 3
	 */
	@Test
	public void testAddAssetToModelDuplicateAsset() {
		switchToSystemModel(1);

		Asset testAsset = querier.getSystemAssets(testHelper.getStore()).values().iterator().next();
		ensureAssetPopulation(testAsset);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAsset).
		when().
			post("/models/testModel/assets").
		then().
			assertThat().statusCode(HttpStatus.SC_CREATED).
			and().
			assertThat().body("asset.label", is(testAsset.getLabel()));

		assertEquals(3, querier.getSystemAssets(testHelper.getStore()).size());
	}

	/**
	 * Test adding an empty asset object to a model
	 * Asserts CREATED 201 status
	 * 
	 * KEM: I can't see the purpose of this test. Why would anyone want to create an empty asset
	 * that has nothing defined except its id (and now population). TODO: decide if this is still required
	 */
	@Test
	public void testAddAssetToModelEmptyAssetObject() {
		switchToSystemModel(5);

		Asset asset = new Asset();

		//Asset must have a population level at least
		ensureAssetPopulation(asset);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(asset).
		when().
			post("/models/testModel/assets").
		then().
			assertThat().statusCode(HttpStatus.SC_CREATED);
	}

	/**
	 * Test adding an asset to an invalid model id
	 * Asserts NOT FOUND 404 status 
	 */
	@Test
	public void testAddAssetToModelInvalidModel() {
		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(new Asset()).
		when().
			post("/models/testModel/assets").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Test getting a known asset from a model
	 * Asset ID "3e77c20d" taken from Host-Process-Disconnected.nq (model 1)
	 * Asserts OK 200 status
	 * Asserts id returned is as expected
	 */
	@Test
	public void testGetAssetInModel() {
		switchToSystemModel(1);

		given().
			filter(userSession).
		when().
			get("/models/testModel/assets/3e77c20d").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("id", is("3e77c20d"));
	}

	/**
	 * Test get invalid asset from model
	 * Asserts NOT FOUND 404 returned
	 */
	@Test
	public void testGetAssetInModelInvalidAsset() {
		switchToSystemModel(1);

		given().
			filter(userSession).
		when().
			get("/models/testModel/assets/notAnAssetID").
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
			get("/models/notATestModel/assets/notAnAssetID").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Test getting TWAS of a known asset from a model
	 * Asset ID "3e77c20d" taken from Host-Process-Connected-Validated.nq (model 3)
	 * Asserts OK 200 status
	 * Asserts that the TWAS contains 4 objects
	 */
	@Test
	public void testGetAssetTwas() {
		switchToSystemModel(3);

		given().
			filter(userSession).
		when().
			get("/models/testModel/assets/3e77c20d/twas").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("size()", is(4));
	}

	/**
	 * Test getting TWAS from an invalid asset
	 * Asserts NOT FOUND 404 returned
	 */
	@Test
	public void testGetAssetTwasInvalidAsset() {
		switchToSystemModel(1);

		given().
			filter(userSession).
		when().
			get("/models/testModel/assets/notAnAssetID/twas").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Test getting TWAS from an invalid model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testGetAssetTwasInvalidModel() {
		given().
			filter(userSession).
		when().
			get("/models/notATestModel/assets/notAnAssetID/twas").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Test getting control sets of a known asset from a model
	 * Asset ID "3e77c20d" taken from Host-Process-Connected-Validated.nq (model 3)
	 * Asserts OK 200 status
	 * Asserts that the control set contains 12 objects
	 */
	@Test
	public void testGetAssetControlSets() {
		switchToSystemModel(3);

		given().
			filter(userSession).
		when().
			get("/models/testModel/assets/3e77c20d/controlsets").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("size()", is(12));
	}

	/**
	 * Test getting control sets from an invalid asset
	 * Asserts NOT FOUND 404 returned
	 */
	@Test
	public void testGetAssetControlSetsInvalidAsset() {
		switchToSystemModel(1);

		given().
			filter(userSession).
		when().
			get("/models/testModel/assets/notAnAssetID/controlsets").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Test getting control sets from an invalid model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testGetAssetControlSetsInvalidModel() {
		given().
			filter(userSession).
		when().
			get("/models/notATestModel/assets/notAnAssetID/controlsets").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Testing moving asset to another valid location
	 * Asset ID "3e77c20d" and its URI taken from Host-Process-Disconnected.nq (model 1)
	 * Asserts OK 200 status
	 */
	@Test
	public void testUpdateAssetLocation() {
		switchToSystemModel(1);

		Asset testAsset = querier
			.getSystemAssets(testHelper.getStore())
			.get("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#5c6848a5");

		int x = testAsset.getIconX() + 10;
		int y = testAsset.getIconY() + 10;
		testAsset.setIconPosition(x, y);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAsset).
		when().
			put("/models/testModel/assets/3e77c20d/location").
		then().
			assertThat().statusCode(HttpStatus.SC_OK);
	}

	/**
	 * Test moving asset to the same location
	 * Asserts OK 200 status
	 */
	@Test
	public void testUpdateAssetLocationSameLocation() {
		switchToSystemModel(1);

		Asset testAsset = querier
			.getSystemAssets(testHelper.getStore())
			.get("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#5c6848a5");

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAsset).
		when().
			put("/models/testModel/assets/3e77c20d/location").
		then().
			assertThat().statusCode(HttpStatus.SC_OK);
	}

	/**
	 * Test moving asset to bad location 
	 * No validation on location, happy to store but will be a problem for UI
	 * Asserts OK 200 status
	 */
	@Test
	public void testUpdateAssetLocationBadLocation() {
		switchToSystemModel(1);

		Asset testAsset = querier
			.getSystemAssets(testHelper.getStore())
			.get("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#5c6848a5");

		int x = -10000;
		int y = -20000;
		testAsset.setIconPosition(x, y);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAsset).
		when().
			put("/models/testModel/assets/3e77c20d/location").
		then().
			assertThat().statusCode(HttpStatus.SC_OK);
	}

	/**
	 * Test moving unknown asset to new location
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testUpdateAssetLocationInvalidAsset() {
		switchToSystemModel(0);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(new Asset()).
		when().
			put("/models/testModel/assets/notAnAssetID/location").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Test moving asset in unknown model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testUpdateAssetLocationInvalidModel() {
		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(new Asset()).
		when().
			put("/models/notATestModel/assets/notAnAssetID/location").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Testing updating an assets label
	 * Asset ID "3e77c20d" and its URI taken from Host-Process-Disconnected.nq (model 1)
	 * Asserts OK 200 status
	 * Asserts returned label matches the new label
	 */
	@Test
	public void testUpdateAssetInModelLabelChange() {
		switchToSystemModel(1);

		Asset testAsset = querier
			.getSystemAssets(testHelper.getStore())
			.get("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#5c6848a5");

		testAsset.setLabel(testAsset.getLabel() + "NewLabel");

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAsset).
		when().
			put("/models/testModel/assets/3e77c20d/label").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("label", is(testAsset.getLabel()));
	}

	/**
	 * Testing updating an assets label
	 * Asset ID "3e77c20d" and its URI taken from Host-Process-Disconnected.nq (model 1)
	 * Asserts BAD REQUEST 400 status
	 * Asserts returned message is "Invalid asset label"
	 */
	@Test
	public void testUpdateAssetInModelLabelChangeToUri() {
		switchToSystemModel(1);

		Asset testAsset = querier
			.getSystemAssets(testHelper.getStore())
			.get("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#5c6848a5");

		testAsset.setLabel("5c6848a5");

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAsset).
		when().
			put("/models/testModel/assets/3e77c20d/label").
		then().
			assertThat().statusCode(HttpStatus.SC_BAD_REQUEST).
			and().
			assertThat().body("message", is("Invalid asset label"));
	}

	/**
	 * Testing updating an assets label to null
	 * Asset ID "3e77c20d" and its URI taken from Host-Process-Disconnected.nq (model 1)
	 * Asserts BAD REQUEST 400 status
	 * Asserts returned message is "Invalid asset label"
	 */
	@Test
	public void testUpdateAssetInModelLabelChangeToNull() {
		switchToSystemModel(1);

		Asset testAsset = querier
			.getSystemAssets(testHelper.getStore())
			.get("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#5c6848a5");

		testAsset.setLabel(null);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAsset).
		when().
			put("/models/testModel/assets/3e77c20d/label").
		then().
			assertThat().statusCode(HttpStatus.SC_BAD_REQUEST).
			and().
			assertThat().body("message", is("Invalid asset label"));
	}

	/**
	 * Testing updating an assets type
	 * Asset ID "3e77c20d" and its URI taken from Host-Process-Disconnected.nq (model 1)
	 * Asserts OK 200 status
	 * Asserts returned assets type matches the new type
	 */
	@Test
	public void testUpdateAssetInModelTypeChange() {
		switchToSystemModel(1);

		Asset testAsset = querier
			.getSystemAssets(testHelper.getStore())
			.get("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#5c6848a5");

		testAsset.setType(testAsset.getType() + "NewType");

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAsset).
		when().
			put("/models/testModel/assets/3e77c20d/type").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("asset.type", is(testAsset.getType()));
	}

	/**
	 * Testing updating an asset's cardinality
	 * Asset ID "3e77c20d" and its URI taken from Host-Process-Disconnected.nq (model 1)
	 * Asserts OK 200 status
	 * Asserts returned asset's cardinality matches new cardinality (min and max)
	 */
	@Test
	public void testUpdateAssetInModelCardinalityChange() {
		switchToSystemModel(1);

		String assetUri = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#5c6848a5";

		Asset testAsset = querier
			.getSystemAssets(testHelper.getStore())
			.get(assetUri);

		testAsset.setMaxCardinality(testAsset.getMaxCardinality() + 1);
		testAsset.setMinCardinality(testAsset.getMinCardinality() + 1);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAsset).
		when().
			put("/models/testModel/assets/3e77c20d/cardinality").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("asset.maxCardinality", is(testAsset.getMaxCardinality())).
			and().
			assertThat().body("asset.minCardinality", is(testAsset.getMinCardinality()));

		//Check the change was written to the triple store
		Asset result = querier
			.getSystemAssets(testHelper.getStore())
			.get(assetUri);

		assertEquals(testAsset.getMinCardinality(), result.getMinCardinality());
		assertEquals(testAsset.getMaxCardinality(), result.getMaxCardinality());
	}

	/**
	 * Testing updating an asset's cardinality to (*, *)
	 * Asset ID "3e77c20d" and its URI taken from Host-Process-Disconnected.nq (model 1)
	 * Asserts OK 200 status
	 * Asserts returned asset's cardinality matches new cardinality (min and max)
	 */
	@Test
	public void testUpdateAssetInModelCardinalityChangeToStarStar() {
		switchToSystemModel(1);

		String assetUri = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#5c6848a5";

		Asset testAsset = querier
			.getSystemAssets(testHelper.getStore())
			.get(assetUri);

		//Make sure the cardinalities are not (*, *)
		testAsset.setMaxCardinality(1);
		testAsset.setMinCardinality(1);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAsset).
		when().
			put("/models/testModel/assets/3e77c20d/cardinality").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("asset.maxCardinality", is(1)).
			and().
			assertThat().body("asset.minCardinality", is(1));

		//Check the change was written to the triple store
		Asset result1 = querier
			.getSystemAssets(testHelper.getStore())
			.get(assetUri);

		assertEquals(1, result1.getMinCardinality());
		assertEquals(1, result1.getMaxCardinality());

		//Set cardinalities to (*, *)
		testAsset.setMaxCardinality(-1);
		testAsset.setMinCardinality(-1);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAsset).
		when().
			put("/models/testModel/assets/3e77c20d/cardinality").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("asset.maxCardinality", is(-1)).
			and().
			assertThat().body("asset.minCardinality", is(-1));

		//Check the change was written to the triple store
		Asset result2 = querier
			.getSystemAssets(testHelper.getStore())
			.get(assetUri);

		assertEquals(-1, result2.getMinCardinality());
		assertEquals(-1, result2.getMaxCardinality());
	}

	/**
	 * Testing updating an asset's cardinality with min larger than max
	 * Asset ID "3e77c20d" and its URI taken from Host-Process-Disconnected.nq (model 1)
	 * Asserts BAD REQUEST 400 status
	 */
	@Test
	public void testUpdateAssetInModelCardinalityChangeMinLargerThanMax() {
		switchToSystemModel(1);

		Asset testAsset = querier
			.getSystemAssets(testHelper.getStore())
			.get("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#5c6848a5");

		testAsset.setMaxCardinality(1);
		testAsset.setMinCardinality(2);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAsset).
		when().
			put("/models/testModel/assets/3e77c20d/cardinality").
		then().
			assertThat().statusCode(HttpStatus.SC_BAD_REQUEST).
			and().
			assertThat().body("message", is("Minimum cardinality larger than maximum"));
	}

	/**
	 * Testing updating an asset's cardinality with negative values (note: -1 means *)
	 * Asset ID "3e77c20d" and its URI taken from Host-Process-Disconnected.nq (model 1)
	 * Asserts BAD REQUEST 400 status
	 */
	@Test
	public void testUpdateAssetInModelCardinalityChangeNegativeValues() {
		switchToSystemModel(1);

		Asset testAsset = querier
			.getSystemAssets(testHelper.getStore())
			.get("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#5c6848a5");

		testAsset.setMaxCardinality(-2);
		testAsset.setMinCardinality(-3);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAsset).
		when().
			put("/models/testModel/assets/3e77c20d/cardinality").
		then().
			assertThat().statusCode(HttpStatus.SC_BAD_REQUEST).
			and().
			assertThat().body("message", is("Minimum or maximum cardinality negative"));
	}

	/**
	 * Testing updating an invalid asset ID
	 * Asserts NOT FOUND 404 status for all 3 fields
	 */
	@Test
	public void testUpdateAssetInModelInvalidAsset() {
		switchToSystemModel(1);

		Asset testAsset = new Asset();

		// Label
		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAsset).
		when().
			put("/models/testModel/assets/notAnAssetID/label").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);

		// Type
		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAsset).
		when().
			put("/models/testModel/assets/notAnAssetID/type").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);

		// Cardinality
		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAsset).
		when().
			put("/models/testModel/assets/notAnAssetID/cardinality").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Testing updating an invalid model ID
	 * Asserts NOT FOUND 404 status for all 3 fields
	 */
	@Test
	public void testUpdateAssetInModelInvalidModel() {
		Asset testAsset = new Asset();

		// Label
		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAsset).
		when().
			put("/models/notATestModel/assets/notAnAssetID/label").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);

		// Type
		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAsset).
		when().
			put("/models/notATestModel/assets/notAnAssetID/type").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);

		// Cardinality
		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testAsset).
		when().
			put("/models/notATestModel/assets/notAnAssetID/cardinality").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Testing deleting an asset from a model
	 * Asset ID "3e77c20d" and its URI taken from Host-Process-Disconnected.nq (model 1)
	 * Asserts OK 200 status
	 * Asserts asset ID returned in list of deleted objects
	 */
	@Test
	public void testDeleteAssetInModel() {
		switchToSystemModel(1);

		given().
			filter(userSession).
		when().
			delete("/models/testModel/assets/3e77c20d").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("assets", contains("3e77c20d"));
	}

	/**
	 * Testing deleting asset with relations
	 * Asserts OK 200 status
	 * Asserts asset ID returned in list of deleted objects
	 * Asserts relations list non-empty
	 */
	@Test
	public void testDeleteAssetInModelAssetWithRelations() {
		switchToSystemModel(3);

		given().
			filter(userSession).
		when().
			delete("/models/testModel/assets/3e77c20d").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("assets", contains("3e77c20d")).
			and().
			assertThat().body("relations", not(empty()));
	}

	/**
	 * Testing deleting asset from a model with no assets
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testDeleteAssetInModelEmptyModel() {
		switchToSystemModel(5);

		given().
			filter(userSession).
		when().
			delete("/models/testModel/assets/3e77c20d").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Testing deleting unknown asset from a model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testDeleteAssetInModelInvalidAsset() {
		switchToSystemModel(0);

		given().
			filter(userSession).
		when().
			delete("/models/testModel/assets/3e77c20d").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Testing deleting asset from an unknown model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testDeleteAssetInModelInvalidModel() {
		given().
			filter(userSession).
		when().
			delete("/models/notATestModel/assets/3e77c20d").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Testing getting controls and threats for an asset
	 * Asserts OK 200 status
	 * Asserts all expected threat IDs in response
	 * Asserts all expected ControlSet IDs in response
	 */
	@Test
	public void testGetControlsAndThreatsForAsset() {
		switchToSystemModel(0);

		Asset testAsset = querier
			.getSystemAssets(testHelper.getStore())
			.get("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#5218e39c");

		List<String> threatIdList = querier
			.getSystemThreats(testHelper.getStore(), testAsset.getUri())
			.values()
			.stream()
			.map(Threat::getID)
			.collect(toList());

		String[] threatIds = threatIdList.toArray(new String[threatIdList.size()]);

		List<String> controlSetIdList = querier
			.getControlSets(testHelper.getStore())
			.values()
			.stream()
			.map(ControlSet::getID)
			.collect(toList());

		String[] controlSetIds = controlSetIdList.toArray(new String[controlSetIdList.size()]);

		given().
			filter(userSession).
		when().
			get("/models/testModel/assets/15f45e41/controls_and_threats").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("threats.id", containsInAnyOrder(threatIds)).
			and().
			assertThat().body("controlSets.id", containsInAnyOrder(controlSetIds));
	}

	/**
	 * testing getting Controls and threats for an unknown asset
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testGetControlsAndthreaetsForAssetUnknownAsset() {
		switchToSystemModel(1);

		given().
			filter(userSession).
		when().
			get("/models/testModel/assets/notAnAssetID/controls_and_threats").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Testing getting Controls and threats for unknown model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testGetControlsAndthreaetsForAssetUnknownModel() {
		given().
			filter(userSession).
		when().
			get("/models/notATestModel/assets/1679f4bc/controls_and_threats").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Test updating a controlset in the model
	 * ControlSet URI CS-AccessControl-1679f4bc from Host-Process-Connected-Validated.nq (model 3)
	 * Asserts OK 200 status
	 * Asserts response proposed flag has been flipped
	 */
	@Test
	public void testUpdateControlsForAsset() {
		switchToSystemModel(3);

		ControlSet testCS = querier
			.getControlSet(
				testHelper.getStore(),
				"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#CS-AccessControl-1679f4bc"
			);

		//Toggling proposed flag
		testCS.setProposed(true);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testCS).
		when().
			put("/models/testModel/assets/1679f4bc/control").
		then().
			assertThat().statusCode(HttpStatus.SC_OK);
	}

	/**
	 * Test updating a controlSet that does not exist on the asset
	 * Asserts 500 Internal server error
	 */
	@Test
	public void testUpdateControlsForAssetUnknownControl() {
		switchToSystemModel(3);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(new ControlSet()).
		when().
			put("/models/testModel/assets/1679f4bc/control").
		then().
			assertThat().statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
	}

	/**
	 * Test updating a control set on an unknown asset
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testUpdateControlsForAssetUnknownAsset() {
		switchToSystemModel(3);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(new ControlSet()).
		when().
			put("/models/testModel/assets/notAnAssetID/control").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Test updating Control set on an unknown model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testUpdatecontrolsForAssetUnknownModel() {
		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(new ControlSet()).
		when().
			put("/models/notATestModel/assets/1679f4bc/control").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Testing updating TWAS for an asset
	 * Asserts OK 200 status
	 * Asserts description returned matches expected
	 */
	@Test
	public void testUpdateTwasForAsset() {
		switchToSystemModel(3);

		String mediumLevelUri = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#TrustworthinessLevelMedium";

		TrustworthinessAttributeSet testTwas = querier
			.getTrustworthinessAttributeSetsForAssetID(testHelper.getStore(), "3e77c20d")
			.values()
			.iterator()
			.next();

		//Creating new level object, level originally VeryHigh, changing to Medium
		Level updatedTwasLevel = new Level();
		updatedTwasLevel.setUri(mediumLevelUri);
		testTwas.setAssertedTWLevel(updatedTwasLevel);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(testTwas).
		when().
			put("/models/testModel/assets/3e77c20d/twas").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body(is("completed"));
	}

	/**
	 * Test updating TWAS for unknown asset
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testUpdateTwasForAssetUnknownAsset() {
		switchToSystemModel(3);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(new TrustworthinessAttributeSet()).
		when().
			put("/models/testModel/assets/notAnAssetID/twas").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Test updating TWAS for unknown model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testUpdateTwasForAssetUnknownModel() {
		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(new TrustworthinessAttributeSet()).
		when().
			put("/models/notATestModel/assets/notAnAssetID/twas").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests requesting all metadata on a single asset
	 * Asserts OK 200 status
	 * Asserts correct number of metadata pairs returned
	 */
	@Test
	public void testGetMetadataOnAsset() {
		switchToSystemModel(6);

		given().
			filter(userSession).
		when().
			get("/models/testModel/assets/ece77c11/meta").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("id", containsInAnyOrder("69488096", "694880a3"));
	}

	/**
	 * Tests requesting all metadata for unknown asset
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testGetMetadataOnAssetUnknownAsset() {
		switchToSystemModel(6);

		given().
			filter(userSession).
		when().
			get("/models/testModel/assets/notAnAssetID/meta").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests requesting all metadata for unknown model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testGetMetadataOnAssetUnknownModel() {
		given().
			filter(userSession).
		when().
			get("/models/notATestModel/assets/ece77c11/meta").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests querying assets by their metadata
	 * Asserts OK 200 status
	 * Asserts that the correct assets are returned
	 */
	@Test
	public void testGetAssetsByMetadata() {
		switchToSystemModel(6);

		ObjectMapper mapper = new ObjectMapper();

		ObjectNode metadataPair1 = mapper.createObjectNode();
		metadataPair1.put("key", "exampleKey2");
		metadataPair1.put("value", "exampleValue1");

		ObjectNode metadataPair2 = mapper.createObjectNode();
		metadataPair2.put("key", "exampleKey2");
		metadataPair2.put("value", "exampleValue2");

		ArrayNode arrayNode = mapper.createArrayNode();
		arrayNode.add(metadataPair2);
		arrayNode.add(metadataPair2);

		given().
			filter(userSession).
			param("metadataJson", arrayNode).
		when().
			get("/models/testModel/assets/meta").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("id", containsInAnyOrder("66004713", "ece77c11"));
	}

	/**
	 * Tests querying assets by their metadata unknown model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testGetAssetsByMetadataUnknownModel() {
		switchToSystemModel(6);

		ObjectMapper mapper = new ObjectMapper();
		ArrayNode arrayNode = mapper.createArrayNode();

		given().
			filter(userSession).
			param("metadataJson", arrayNode).
		when().
			get("/models/notATestModel/assets/meta").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests querying assets by their metadata bad JSON
	 * Asserts BAD REQUEST 400 status
	 */
	@Test
	public void testGetAssetsByMetadataBadJSON() {
		switchToSystemModel(6);

		given().
			filter(userSession).
			param("metadataJson", "[{+++@").
		when().
			get("/models/testModel/assets/meta").
		then().
			assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);
	}

	/**
	 * Tests deleting all metadata on a single asset
	 * Asserts OK 200 status
	 * Asserts that no metadata pairs are returned in response
	 */
	@Test
	public void testDeleteMetadataOnAsset() {
		switchToSystemModel(6);

		given().
			filter(userSession).
		when().
			delete("/models/testModel/assets/66004713/meta").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("id", empty());
	}

	/**
	 * Tests deleting all metadata on unknown asset
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testDeleteMetadataOnAssetUnknownAsset() {
		switchToSystemModel(6);

		given().
			filter(userSession).
		when().
			delete("/models/testModel/assets/notAnAssetID/meta").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests deleting all metadata on unknown model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testDeleteMetadataOnAssetUnknownModel() {
		switchToSystemModel(6);

		given().
			filter(userSession).
		when().
			delete("/models/notATestModel/assets/66004713/meta").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests replacing all metadata on an asset
	 * Asserts OK 200 status
	 * Asserts the correct metadata pairs are returned in the response (using the values)
	 */
	@Test
	public void testReplaceMetadataOnAsset() {
		switchToSystemModel(6);

		List<MetadataPair> metadata = new ArrayList<>();
		metadata.add(new MetadataPair("exampleKey1", "exampleValue1"));
		metadata.add(new MetadataPair("exampleKey1", "newValue2"));
		metadata.add(new MetadataPair("newKey1", "newValue1"));

		String[] values = {"exampleValue1", "newValue1", "newValue2"};

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(metadata).
		when().
			put("/models/testModel/assets/66004713/meta").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("value", containsInAnyOrder(values));
	}

	/**
	 * Tests replacing all metadata on unknown asset
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testReplaceMetadataOnAssetUnknownAsset() {
		switchToSystemModel(6);

		List<MetadataPair> metadata = new ArrayList<>();

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(metadata).
		when().
			put("/models/testModel/assets/notAnAssetID/meta").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests replacing all metadata on unknown model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testReplaceMetadataOnAssetUnknownModel() {
		switchToSystemModel(6);

		List<MetadataPair> metadata = new ArrayList<>();

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(metadata).
		when().
			put("/models/notATestModel/assets/66004713/meta").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests adding metadata to an asset
	 * Asserts OK 200 status
	 * Asserts the correct metadata pairs are returned in the response (using the values)
	 */
	@Test
	public void testAddMetadataOnAsset() {
		switchToSystemModel(6);

		List<MetadataPair> metadata = new ArrayList<>();
		metadata.add(new MetadataPair("exampleKey1", "exampleValue1"));
		metadata.add(new MetadataPair("exampleKey1", "newValue2"));
		metadata.add(new MetadataPair("newKey1", "newValue1"));

		String[] values = {"exampleValue1", "exampleValue1", "exampleValue2", "newValue1", "newValue2"};

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(metadata).
		when().
			patch("/models/testModel/assets/66004713/meta").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("value", containsInAnyOrder(values));
	}

	/**
	 * Tests adding metadata to unknown asset
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testAddMetadataOnAssetUnknownAsset() {
		switchToSystemModel(6);

		List<MetadataPair> metadata = new ArrayList<>();

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(metadata).
		when().
			patch("/models/testModel/assets/notAnAssetID/meta").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests adding metadata to unknown model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testAddMetadataOnAssetUnknownModel() {
		switchToSystemModel(6);

		List<MetadataPair> metadata = new ArrayList<>();

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(metadata).
		when().
			patch("/models/notATestModel/assets/66004713/meta").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}
}
