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
//      Created By :            Lee Mason
//      Created Date :          08/07/2020
//		Modified By :
//      Created for Project :   ZDMP
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
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
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.soton.itinnovation.security.model.system.Asset;
import uk.ac.soton.itinnovation.security.model.system.AssetGroup;
import uk.ac.soton.itinnovation.security.modelquerier.SystemModelQuerier;
import uk.ac.soton.itinnovation.security.systemmodeller.CommonTestSetup;
import uk.ac.soton.itinnovation.security.systemmodeller.SystemModellerApplication;
import uk.ac.soton.itinnovation.security.systemmodeller.TestHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.auth.KeycloakAdminClient;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.model.ModelFactory;
import uk.ac.soton.itinnovation.security.systemmodeller.model.WebKeyRole;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.AssetGroupDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.StoreModelManager;
import uk.ac.soton.itinnovation.security.systemmodeller.util.SecureUrlHelper;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SystemModellerApplication.class, webEnvironment = RANDOM_PORT)
public class GroupControllerTest extends CommonTestSetup {

    private static final Logger logger = LoggerFactory.getLogger(GroupControllerTest.class);

    //System objects, spying on secureUrlHelper to control model access
    @Autowired
    private ModelObjectsHelper modelHelper;
	
	@Autowired
	private ModelFactory modelFactory;

    @Autowired
    private StoreModelManager storeManager;
	
    @SpyBean
    private SecureUrlHelper secureUrlHelper;

	@Autowired
	private KeycloakAdminClient keycloakAdminClient;

    //Auth variables
    @Value("${server.servlet.contextPath}")
    private String contextPath;

    @LocalServerPort
    int port;

    //Provides model control and access
    private Model testModel;
	private String testUserId;
    private static TestHelper testHelper;
    private static SystemModelQuerier querier;


    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void beforeClass() {
        logger.info("Setting up GroupControllerTest class");
        testHelper = new TestHelper("jena-tdb");
        testHelper.setUp();
        querier = new SystemModelQuerier(testHelper.getModel());
    }

    @Before
    public void init() {
        logger.info("Executing {}", name.getMethodName());
        initAuth(contextPath, port);
        storeManager.clearMgtGraph();
		testUserId = keycloakAdminClient.getUserByUsername(testUserName).getId();
    }

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
		testModel.setUserId(testUserId);
		testModel.save(); //after save, model has an id, which is required by getAssetsForModel below

        modelHelper.getAssetsForModel(testModel, true);
		
		//bypass looking model up in Mongo via WebKey (the WebKey is NOT the model ID)
        when(secureUrlHelper.getModelFromUrl("testModel")).thenReturn(testModel);
		when(secureUrlHelper.getRoleFromUrl("testModel")).thenReturn(WebKeyRole.NONE);
        when(secureUrlHelper.getWriteModelFromUrl("testModel")).thenReturn(testModel);
        when(secureUrlHelper.getReadModelFromUrl("testModel")).thenReturn(testModel);
    }

    //Tests

    /**
     * Asserts OK 200 status
     * Asserts 5 asset groups in returned JSON
     */
    @Test
    public void testGetAssetGroups() {
        switchToSystemModel(8);
        Response response = given().filter(userSession).when().get("/models/testModel/assetGroups");
        assertEquals(HttpStatus.OK.value(), response.statusCode());
        assertTrue(response.body().jsonPath().getList("id").size() > 0);
    }

    /**
     * Asserts CREATED 201 status
     * Asserts response body label is equal to input
     * Asserts that the new asset group has been successfully added to the database
     */
    @Test
    public void testAddAssetGroupToModel() {
        // Isn't asset group system model to avoid clashes
        switchToSystemModel(0);

        Asset a1 = modelHelper.getAssetById("2d5019c7", testModel, false);

        AssetGroup testGroup = new AssetGroup();
        testGroup.setLabel("TestGroup");
        testGroup.addAsset(a1);

        AssetGroupDTO assetGroupDTO = new AssetGroupDTO(testGroup);
        Response response = given().filter(userSession).contentType(ContentType.JSON)
                .body(assetGroupDTO).when().post("/models/testModel/assetGroups");
        assertEquals(HttpStatus.CREATED.value(), response.statusCode());
        assertEquals(testGroup.getLabel(), response.body().jsonPath().getString("label"));
        Map<String, AssetGroup> assetGroups = querier.getAssetGroups(testHelper.getStore());
        assetGroups.containsKey(response.body().jsonPath().getString("uri"));
    }

    /**
     * Asserts OK 200 status
     * Asserts asset present in response body asset group
     * Asserts that the new asset  has been successfully added to the asset group in the database
     * Asserts that added asset has been repositioned correctly (new position sent via request)
     * Asserts that a BAD_REQUEST status is returned if an invalid add is attempted
     */
    @Test
    public void testAddAssetToGroup() {
        switchToSystemModel(8);

        Asset addAsset = modelHelper.getAssetById("ca9c0d4", testModel, false);
		//here we mimic the UI setting the position of the asset within the group
		addAsset.setIconPosition(25, 25);
		
        Response response = given().filter(userSession).contentType(ContentType.JSON)
                .body(addAsset).when().post(
                "/models/testModel/assetGroups/6fa82559/addAsset/ca9c0d4");
        assertEquals(HttpStatus.OK.value(), response.statusCode());
        assertEquals(4, response.getBody().jsonPath().getList("assetIds").size());
        assertEquals(4, querier.getAssetGroupById(testHelper.getStore(), "6fa82559").getAssets().size());

		//Confirm that the asset (added to group) has updated location
        Asset addedAsset = modelHelper.getAssetById("ca9c0d4", testModel, true);
		assertEquals(25, addedAsset.getIconX());
		assertEquals(25, addedAsset.getIconY());

        response = given().filter(userSession).contentType(ContentType.JSON)
                .body(addAsset).when().post(
                "/models/testModel/assetGroups/6fa82559/addAsset/ca9c0d4");
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
    }

    /**
     * Asserts OK 200 status
     * Asserts 2 assets in JSON response group
     * Asserts 2 assets present in the group in database (indicating that remove was successful)
     * Asserts that removed asset has been repositioned correctly (new position sent via request)
     * Asserts that a BAD_REQUEST status is returned if an invalid remove is attempted
     */
    @Test
    public void testRemoveAssetFromGroup() {
        switchToSystemModel(8);

        Asset removeAsset = modelHelper.getAssetById("2d5019c7", testModel, false);
		//here we mimic the UI setting the new position of the asset on the canvas, after removal from group
		removeAsset.setIconPosition(5000, 5000);
		
        Response response = given().filter(userSession).contentType(ContentType.JSON)
				.body(removeAsset).when().post("/models/testModel/assetGroups/42d51951/removeAsset/2d5019c7");
        assertEquals(HttpStatus.OK.value(), response.statusCode());
        assertEquals(2, response.getBody().jsonPath().getList("assetIds").size());
        assertEquals(2, querier.getAssetGroupById(testHelper.getStore(), "42d51951").getAssets().size());
		
		//Confirm that the asset (removed from group) has updated location
        Asset removedAsset = modelHelper.getAssetById("2d5019c7", testModel, true);
		assertEquals(5000, removedAsset.getIconX());
		assertEquals(5000, removedAsset.getIconY());

		//test attempted removal of asset that is not in the group
        removeAsset = modelHelper.getAssetById("c1078c6a", testModel, false);
		//here we mimic the UI setting the new position of the asset on the canvas, after removal from group
		removeAsset.setIconPosition(5000, 5000);
        response = given().filter(userSession).contentType(ContentType.JSON).body(removeAsset).when().post(
                "/models/testModel/assetGroups/42d51951/removeAsset/c1078c6a");
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
    }

    /**
     * Asserts OK 200 status
     * Asserts updated x, y values are in database
     */
    @Test
    public void testUpdateAssetGroupLocation() {
        switchToSystemModel(8);

        AssetGroup testGroup = querier.getAssetGroupById(testHelper.getStore(), "42d51951");
        testGroup.setX(50);
        testGroup.setY(100);
        AssetGroupDTO assetGroupDTO = new AssetGroupDTO(testGroup);
        Response response = given().filter(userSession).contentType(ContentType.JSON)
                .body(assetGroupDTO).when().put("/models/testModel/assetGroups/42d51951/location");
        assertEquals(HttpStatus.OK.value(), response.statusCode());
        testGroup = querier.getAssetGroupById(testHelper.getStore(), "42d51951");
        assertEquals(50, testGroup.getX());
        assertEquals(100, testGroup.getY());
    }

    /**
     * Asserts OK 200 status
     * Asserts that an asset group is absent from the database after it has been deleted
     * Asserts that assets remain in the database if their group has been deleted with the deleteAssets = false
     * Asserts that assets are absent from the database if their group has been deleted with deleteAssets = true
     */
    @Test
    public void testDeleteGroupInModel() {
        switchToSystemModel(8);

        AssetGroup testGroup = querier.getAssetGroupById(testHelper.getStore(), "4282e645");
        Response response = given().filter(userSession).when().delete(
                "/models/testModel/assetGroups/4282e645");
        assertEquals(HttpStatus.OK.value(), response.statusCode());
        assertNull(querier.getAssetGroupById(testHelper.getStore(), "4282e645"));
        for (Asset asset : testGroup.getAssets().values()) {
            assertNotNull(querier.getSystemAssetById(testHelper.getStore(), asset.getID()));
        }

        testGroup = querier.getAssetGroupById(testHelper.getStore(), "77773cc9");
        response = given().filter(userSession).
                param("deleteAssets", true).when().delete(
                "/models/testModel/assetGroups/77773cc9");
        assertEquals(HttpStatus.OK.value(), response.statusCode());
        assertNull(querier.getAssetGroupById(testHelper.getStore(), "77773cc9"));
        for (Asset asset : testGroup.getAssets().values()) {
            assertNull(querier.getSystemAssetById(testHelper.getStore(), asset.getID()));
        }
    }
}
