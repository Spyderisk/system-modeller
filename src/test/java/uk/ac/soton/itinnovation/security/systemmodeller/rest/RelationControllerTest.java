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
//      Created Date :          25/07/2018
//      Created for Project :   RESTASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
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
import uk.ac.soton.itinnovation.security.model.system.Relation;
import uk.ac.soton.itinnovation.security.modelquerier.SystemModelQuerier;
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
public class RelationControllerTest extends CommonTestSetup{

	private static final Logger logger = LoggerFactory.getLogger(RelationControllerTest.class);

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
		logger.info("Setting up RelationControllerTest class");
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

	private void switchToSystemModel(int modelIndex) {
		testHelper.switchModels(0, modelIndex);

		//model graph(s) already in Jena - but model not in Mongo or the management graph
		testModel = modelFactory.getModel(testHelper.getModel(), testHelper.getStore());

		//logger.debug("switchToSystemModel: test model userid: {}", testModel.getUserId());
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
	 * Test listing relations for a model with many relations (shield demo model)
	 * Asserts OK 200 status
	 * Asserts number of relations returned matches those found by querying the model locally
	 */
	@Test
	public void testListRelationsLargeModel() {
		switchToSystemModel(0);

		int numOfSystemRelations = querier.getSystemRelations(testHelper.getStore()).size();

		given().
			filter(userSession).
		when().
			get("models/testModel/relations").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("id", iterableWithSize(numOfSystemRelations));
	}

	/**
	 * Test listing relations for a model with a single relation (Host-process-disconnected)
	 * Asserts OK 200 status
	 * Asserts number of relations returned matches those found by querying the model locally
	 */
	@Test
	public void testListRelationsOneRelation() {
		switchToSystemModel(3);

		int numOfSystemRelations = querier.getSystemRelations(testHelper.getStore()).size();

		given().
			filter(userSession).
		when().
			get("models/testModel/relations").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("id", iterableWithSize(numOfSystemRelations));
	}

	/**
	 * Tests listing relations for a model with no relations
	 * Asserts OK 200 status
	 * Asserts number of relations returned matches those found by querying the model locally (0)
	 */
	@Test
	public void testListRelationsNoRelations() {
		switchToSystemModel(1);

		int numOfSystemRelations = querier.getSystemRelations(testHelper.getStore()).size();

		given().
			filter(userSession).
		when().
			get("models/testModel/relations").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("id", iterableWithSize(numOfSystemRelations));
	}

	/**
	 * Tests listing relations for an unregistered model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testListRelationsUnknownModel() {
		given().
			filter(userSession).
		when().
			get("models/notATestModel/relations").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests creating a new relation
	 * Asserts CREATED 201 status
	 * Asserts relation is returned (id not null, id not known)
	 */
	@Test
	public void testCreateRelation() {
		switchToSystemModel(1);

		Relation newRelation = new Relation(
			"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#5c6848a5",
			"3e77c20d",
			"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#601f0f2d",
			"1679f4bc",
			"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#hosts",
			"hosts"
		);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(newRelation).
		when().
			post("/models/testModel/relations").
		then().
			assertThat().statusCode(HttpStatus.SC_CREATED).
			and().
			assertThat().body("relation.id", not(isEmptyOrNullString()));
	}

	/**
	 * Tests creating a relation identical to one that already exists in the model
	 * Current expected behaviour is to return created status while not duplicating the relation
	 * Asserts CREATED 201 status
	 * Asserts numRelations unchanged
	 */
	@Test
	public void testCreateDuplicateRelation() {
		switchToSystemModel(3);

		int numRelationsBefore = querier.getSystemRelations(testHelper.getStore()).size();

		Relation newRelation = new Relation(
			"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#5c6848a5",
			"3e77c20d",
			"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#601f0f2d",
			"1679f4bc",
			"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#hosts",
			"hosts"
		);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(newRelation).
		when().
			post("/models/testModel/relations").
		then().
			assertThat().statusCode(HttpStatus.SC_CREATED);

		int numRelationsAfter = querier.getSystemRelations(testHelper.getStore()).size();

		assertEquals(numRelationsBefore, numRelationsAfter);
	}

	/**
	 * Tests creating a relation on an unknown model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testCreateRelationOnUnknownModel() {
		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(new Relation()).
		when().
			post("/models/notATestModel/relations").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Testing creating a relation with no fromID
	 * Assert NOT FOUND 404 status
	 */
	@Test
	public void testCreateRelationNoFromId() {
		switchToSystemModel(1);

		Relation newRelation = new Relation(
			null,
			null,
			"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#601f0f2d",
			"1679f4bc",
			"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#hosts",
			"hosts"
		);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(newRelation).
		when().
			post("/models/testModel/relations").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Testing creating a relation with no toID
	 * Assert NOT FOUND 404 status
	 */
	@Test
	public void testCreateRelationNoToId() {
		switchToSystemModel(1);

		Relation newRelation = new Relation(
			"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#5c6848a5",
			"3e77c20d",
			null,
			null,
			"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#hosts",
			"hosts"
		);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(newRelation).
		when().
			post("/models/testModel/relations").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}	

	/**
	 * Tests getting a known relation from a model
	 * Asserts OK 200 status
	 * Asserts From ID matches expected
	 * Asserts To ID matches expected
	 * Asserts label matches expected
	 */
	@Test
	public void testGetRelation() {
		switchToSystemModel(3);

		String relID = "e50a2f3c";

		Relation rel = querier.getSystemRelationById(testHelper.getStore(), relID);

		given().
			filter(userSession).
		when().
			get("/models/testModel/relations/" + relID).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("fromID", is(rel.getFromID())).
			and().
			assertThat().body("toID", is(rel.getToID())).
			and().
			assertThat().body("label", is(rel.getLabel()));
	}

	/** 
	 * Tests getting a relation with a relation ID that does not exist
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testGetRelationInvalidRelationId() {
		switchToSystemModel(3);

		given().
			filter(userSession).
		when().
			get("/models/testModel/relations/invalidRel").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/** 
	 * Tests getting an existing relation not in the chosen model.
	 * Asserts NOT FOUND 404 error 
	 */
	@Test
	public void testGetRelationRelationNotInModel() {
		switchToSystemModel(2);

		given().
			filter(userSession).
		when().
			get("/models/testModel/relations/e50a2f3c").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests getting relation for an invalid model ID
	 * Asserts NOT FOUND 404 error
	 */
	@Test
	public void testGetRelationInvalidModel() {
		given().
			filter(userSession).
		when().
			get("/models/notATestModel/relations/e50a2f3c").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests updating a relation found in a model by appending "2" to the label
	 * Asserts OK 200 status
	 */
	@Test
	public void testUpdateRelation() {
		switchToSystemModel(3);

		String relID = "e50a2f3c";

		Relation rel = querier.getSystemRelationById(testHelper.getStore(), relID);

		rel.setType("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#hosts2");
		rel.setLabel("hosts2");

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(rel).
		when().
			put("/models/testModel/relations/" + relID).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("fromID", is(rel.getFromID())).
			and().
			assertThat().body("toID", is(rel.getToID())).
			and().
			assertThat().body("label", is(rel.getLabel()));
	}

	/** 
	 * Tests updating an unknown relation on a known model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testUpdateRelationInvalidRelation() {
		switchToSystemModel(3);

		Relation rel = querier.getSystemRelationById(testHelper.getStore(), "e50a2f3c");

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(rel).
		when().
			put("/models/testModel/relations/notARelationId").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/** 
	 * Tests updating an unknown relation
	 * Asserts NOT FOUND 404 error
	 */
	@Test
	public void testUpdateRelationNoRelations() {
		switchToSystemModel(3);

		String relID = "e50a2f3c";

		Relation rel = querier.getSystemRelationById(testHelper.getStore(), relID);

		switchToSystemModel(2);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(rel).
		when().
			put("/models/testModel/relations/" + relID).
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests updating a relation for an unknown model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testUpdateRelationInvalidModel() {
		switchToSystemModel(3);

		String relID = "e50a2f3c";

		Relation rel = querier.getSystemRelationById(testHelper.getStore(), relID);

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(rel).
		when().
			put("/models/notATestModel/relations/" + relID).
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests updating relation's fromID to a different asset
	 * Asserts OK 200 status 
	 * Asserts new fromID returned
	 */
	@Test
	public void testUpdateRelationNewFromAsset() {
		switchToSystemModel(3);

		String relID = "e50a2f3c";

		Relation rel = querier.getSystemRelationById(testHelper.getStore(), relID);

		rel.setFrom("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#601f0f2d");
		rel.setFromID("1679f4bc");

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(rel).
		when().
			put("/models/testModel/relations/" + relID).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("fromID", is(rel.getFromID()));
	}

	/**
	 * Tests updating relation's fromID to an invalid asset
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testUdpateRelationInvalidFromAsset() {
		switchToSystemModel(3);

		String relID = "e50a2f3c";

		Relation rel = querier.getSystemRelationById(testHelper.getStore(), relID);

		rel.setFrom("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#badURI");
		rel.setFromID("badID");

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(rel).
		when().
			put("/models/testModel/relations/" + relID).
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests updating relation's toID to a different asset
	 * Asserts OK 200 status 
	 * Asserts new fromID returned
	 */
	@Test
	public void testUpdateRelationNewToAsset() {
		switchToSystemModel(3);

		String relID = "e50a2f3c";

		Relation rel = querier.getSystemRelationById(testHelper.getStore(), relID);

		rel.setTo("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#5c6848a5");
		rel.setToID("3e77c20d");

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(rel).
		when().
			put("/models/testModel/relations/" + relID).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("toID", is(rel.getToID()));
	}

	/**
	 * Tests updating relation's toID to an invalid asset
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testUpdateRelationInvalidToAsset() {
		switchToSystemModel(3);

		String relID = "e50a2f3c";

		Relation rel = querier.getSystemRelationById(testHelper.getStore(), relID);

		rel.setTo("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#badURI");
		rel.setToID("badID");

		given().
			filter(userSession).
			contentType(ContentType.JSON).
			body(rel).
		when().
			put("/models/testModel/relations/" + relID).
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests deleting a known relation from a model
	 * Asserts OK 200 status
	 * Asserts deleted relation ID is as expected
	 */
	@Test
	public void testDeleteRelation() {
		switchToSystemModel(3);

		String relID = "e50a2f3c";

		given().
			filter(userSession).
		when().
			delete("/models/testModel/relations/" + relID).
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("relations", contains(relID));
	}

	/** 
	 * Tests deleting an unknown relation from a known model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testDeleteRelationUnknownRelation() {
		switchToSystemModel(3);

		given().
			filter(userSession).
		when().
			delete("/models/testModel/relations/notARelationID").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/** 
	 * Tests deleting a relation from a model with no relations
	 */
	@Test
	public void testDeleteRelationNoRelations() {
		switchToSystemModel(1);

		given().
			filter(userSession).
		when().
			delete("/models/testModel/relations/irrelevantID").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

	/**
	 * Tests deleting relation from an unknown model
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testDeleteRelationInvalidModel() {		
		given().
			filter(userSession).
		when().
			delete("/models/notATestModel/relations/irrelevantID").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}
}
