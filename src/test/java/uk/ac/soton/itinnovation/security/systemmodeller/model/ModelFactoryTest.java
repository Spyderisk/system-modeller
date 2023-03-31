/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2021
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
//      Created Date :        04/01/2021
//      Created for Project : ZDMP
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.soton.itinnovation.security.systemmodeller.SystemModellerApplication;
import uk.ac.soton.itinnovation.security.systemmodeller.TestHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.mongodb.IModelRepository;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.StoreModelManager;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SystemModellerApplication.class)
@TestPropertySource(properties = {"reset.on.start=false"})
public class ModelFactoryTest {

	private static final Logger logger = LoggerFactory.getLogger(ModelFactoryTest.class);

	private static TestHelper testHelper;

	@Autowired
	private ModelFactory modelFactory;

	@Autowired
	private StoreModelManager storeManager;

	@Autowired
	private IModelRepository modelRepository;

	//Allows automatic logging of test names
	@Rule
	public TestName name = new TestName();

	//Does not need to match anything in Keycloak
	private String testUserId = "testUserId";

	@BeforeClass
	public static void beforeClass() {
		logger.info("Setting up ModelFactoryTest class");
		testHelper = new TestHelper("jena-tdb");
		testHelper.setUp();
	}

	@Before
	public void setUp() {
		logger.info("Executing {}", name.getMethodName());
		storeManager.clearMgtGraph();
		modelRepository.deleteAll();
	}

	private Model createTestModel(Integer index) {
		testHelper.switchModels(0, index);

		Model testModel = modelFactory.getModel(testHelper.getModel(), testHelper.getStore());

		testModel.setUserId(testUserId);
		testModel.save();

		return testModel;
	}

	@Test
	public void testCreateModel() {
		String domainGraph = "http://" + UUID.randomUUID().toString();
		String userId = UUID.randomUUID().toString();

		Model model = modelFactory.createModel(domainGraph, userId);

		//Model in Mongo
		assertNotNull(modelRepository.findOneById(model.getId()));

		//Model graph exists
		assertEquals(10, storeManager.getStore().getCount(model.getUri()));

		//Model in management graph
		assertTrue(storeManager.systemModelExists(model.getUri()));

		//Model has correct domainGraph and userId
		assertEquals(domainGraph, model.getDomainGraph());
		assertEquals(userId, model.getUserId());

		//Model has an ID, URI, no-role URL, read URL, write URL and owner URL
		assertNotNull(model.getId());
		assertNotNull(model.getUri());
		assertNotNull(model.getNoRoleUrl());
		assertNotNull(model.getReadUrl());
		assertNotNull(model.getWriteUrl());
		assertNotNull(model.getOwnerUrl());

		//Model has ModelInfo
		assertTrue(model.hasModelInfo());

		//Model has no name or description
		assertNull(model.getName());
		assertNull(model.getDescription());

		//Model is not valid and risk levels are not valid
		assertFalse(model.isValid());
		assertFalse(model.riskLevelsValid());

		//Model is not validating or calculating risks
		assertFalse(model.isValidating());
		assertFalse(model.isCalculatingRisks());

		//Model has no ModelData
		assertFalse(model.hasModelData());
	}

	@Test
	public void testCreateModelWithNullDomainGraph() {
		String userId = UUID.randomUUID().toString();

		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> modelFactory.createModel(null, userId)
			)
			.withMessage(
				"Attempting to create Model with null domainGraph"
			);
	}

	@Test
	public void testCreateModelWithEmptyDomainGraph() {
		String userId = UUID.randomUUID().toString();

		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> modelFactory.createModel("", userId)
			)
			.withMessage(
				"Attempting to create Model with empty domainGraph"
			);
	}

	@Test
	public void testCreateModelWithNullUserId() {
		String domainGraph = "http://" + UUID.randomUUID().toString();

		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> modelFactory.createModel(domainGraph, null)
			)
			.withMessage(
				"Attempting to create Model with null userId"
			);
	}

	@Test
	public void testCreateModelWithEmptyUserId() {
		String domainGraph = "http://" + UUID.randomUUID().toString();

		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> modelFactory.createModel(domainGraph, "")
			)
			.withMessage(
				"Attempting to create Model with empty userId"
			);
	}

	@Test
	public void testCreateModelForImport() {
		String uri = "http://" + UUID.randomUUID().toString();
		String domainGraph = "http://" + UUID.randomUUID().toString();
		String userId = UUID.randomUUID().toString();

		Model model = modelFactory.createModelForImport(uri, domainGraph, userId);

		//Model in Mongo
		assertNotNull(modelRepository.findOneById(model.getId()));

		//Model graph exists
		assertEquals(3, storeManager.getStore().getCount(model.getUri()));

		//Model in management graph
		assertTrue(storeManager.systemModelExists(model.getUri()));

		//Model has correct URI, domainGraph and userId
		assertEquals(uri, model.getUri());
		assertEquals(domainGraph, model.getDomainGraph());
		assertEquals(userId, model.getUserId());

		//Model has an ID, read URL, and write URL
		assertNotNull(model.getId());
		assertNotNull(model.getReadUrl());
		assertNotNull(model.getWriteUrl());

		//Model has no ModelInfo
		assertFalse(model.hasModelInfo());

		//Model is not validating or calculating risks
		assertFalse(model.isValidating());
		assertFalse(model.isCalculatingRisks());

		//Model has no ModelData
		assertFalse(model.hasModelData());
	}

	@Test
	public void testCreateModelForImportWithDuplicateUri() {
		String uri = "http://" + UUID.randomUUID().toString();
		String domainGraph = "http://" + UUID.randomUUID().toString();
		String userId = UUID.randomUUID().toString();

		//Create pre-existing model with the same URI
		modelFactory.createModelForImport(uri, domainGraph, userId);

		Model model = modelFactory.createModelForImport(uri, domainGraph, userId);

		//Model in Mongo
		assertNotNull(modelRepository.findOneById(model.getId()));

		//Model graph exists
		assertEquals(3, storeManager.getStore().getCount(model.getUri()));

		//Model in management graph
		assertTrue(storeManager.systemModelExists(model.getUri()));

		//Model has a different URI
		assertNotNull(model.getUri());
		assertNotEquals(uri, model.getUri());

		//Model has correct domainGraph and userId
		assertEquals(domainGraph, model.getDomainGraph());
		assertEquals(userId, model.getUserId());

		//Model has an ID, read URL, and write URL
		assertNotNull(model.getId());
		assertNotNull(model.getReadUrl());
		assertNotNull(model.getWriteUrl());

		//Model has no ModelInfo
		assertFalse(model.hasModelInfo());

		//Model is not validating or calculating risks
		assertFalse(model.isValidating());
		assertFalse(model.isCalculatingRisks());

		//Model has no ModelData
		assertFalse(model.hasModelData());
	}

	@Test
	public void testCreateModelForImportWithNullUri() {
		String domainGraph = "http://" + UUID.randomUUID().toString();
		String userId = UUID.randomUUID().toString();

		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> modelFactory.createModelForImport(null, domainGraph, userId)
			)
			.withMessage(
				"Attempting to create Model with null uri"
			);
	}

	@Test
	public void testCreateModelForImportWithEmptyUri() {
		String domainGraph = "http://" + UUID.randomUUID().toString();
		String userId = UUID.randomUUID().toString();

		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> modelFactory.createModelForImport("", domainGraph, userId)
			)
			.withMessage(
				"Attempting to create Model with empty uri"
			);
	}

	@Test
	public void testCreateModelForImportWithNullDomainGraph() {
		String uri = "http://" + UUID.randomUUID().toString();
		String userId = UUID.randomUUID().toString();

		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> modelFactory.createModelForImport(uri, null, userId)
			)
			.withMessage(
				"Attempting to create Model with null domainGraph"
			);
	}

	@Test
	public void testCreateModelForImportWithEmptyDomainGraph() {
		String uri = "http://" + UUID.randomUUID().toString();
		String userId = UUID.randomUUID().toString();

		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> modelFactory.createModelForImport(uri, "", userId)
			)
			.withMessage(
				"Attempting to create Model with empty domainGraph"
			);
	}

	@Test
	public void testCreateModelForImportWithNullUserId() {
		String uri = "http://" + UUID.randomUUID().toString();
		String domainGraph = "http://" + UUID.randomUUID().toString();

		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> modelFactory.createModelForImport(uri, domainGraph, null)
			)
			.withMessage(
				"Attempting to create Model with null userId"
			);
	}

	@Test
	public void testCreateModelForImportWithEmptyUserId() {
		String uri = "http://" + UUID.randomUUID().toString();
		String domainGraph = "http://" + UUID.randomUUID().toString();

		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> modelFactory.createModelForImport(uri, domainGraph, "")
			)
			.withMessage(
				"Attempting to create Model with empty userId"
			);
	}

	@Test
	public void testCreateModelForCopy() {
		Model testModel = createTestModel(0);
		String domainGraph = testModel.getDomainGraph();
		String userId = testModel.getUserId();
		logger.debug("testModel: {}", testModel);

		//Get ModelACL for the model from Mongo
		ModelACL modelACL = modelRepository.findOneById(testModel.getId());
		logger.debug("modelACL: {}", modelACL);

		//Test createModelForCopy method
		Model modelCopy = modelFactory.createModelForCopy(testModel.getUri(), domainGraph, userId);
		logger.debug("Copied model: {}", modelCopy);

		//Model in Mongo
		assertNotNull(modelRepository.findOneById(modelCopy.getId()));

		//Model in management graph
		assertTrue(storeManager.systemModelExists(modelCopy.getUri()));

		//Get system graph URIs for the 2 models
		String testModelUri = testModel.getUri();
		String modelCopyUri = modelCopy.getUri();

		logger.debug("testModel system graph size: {}", storeManager.getStore().getCount(testModelUri));
		logger.debug("modelCopy system graph size: {}", storeManager.getStore().getCount(modelCopyUri));

		//System graph sizes match
		assertEquals(storeManager.getStore().getCount(testModelUri), storeManager.getStore().getCount(modelCopyUri));
		logger.info("System graph sizes match");

		//Inferred graph sizes match
		assertEquals(storeManager.getStore().getCount(testModelUri + StoreModelManager.INF_GRAPH),
					 storeManager.getStore().getCount(modelCopyUri + StoreModelManager.INF_GRAPH));
		logger.info("Inferred graph sizes match");

		//UI graph sizes match
		assertEquals(storeManager.getStore().getCount(testModelUri + StoreModelManager.UI_GRAPH),
					 storeManager.getStore().getCount(modelCopyUri + StoreModelManager.UI_GRAPH));
		logger.info("UI graph sizes match");

		//Metadata graph sizes match
		assertEquals(storeManager.getStore().getCount(testModelUri + StoreModelManager.META_GRAPH),
					 storeManager.getStore().getCount(modelCopyUri + StoreModelManager.META_GRAPH));
		logger.info("Metadata graph sizes match");

		//Model has a new URI; domainGraph and userId should match
		assertNotEquals(testModel.getUri(), modelCopy.getUri());
		assertEquals(domainGraph, modelCopy.getDomainGraph());
		assertEquals(userId, modelCopy.getUserId());

		//Model has an ID, read URL, and write URL
		assertNotNull(modelCopy.getId());
		assertNotNull(modelCopy.getReadUrl());
		assertNotNull(modelCopy.getWriteUrl());

		//Model has no ModelInfo
		assertFalse(modelCopy.hasModelInfo());

		//Model is not validating or calculating risks
		assertFalse(modelCopy.isValidating());
		assertFalse(modelCopy.isCalculatingRisks());

		//Model has no ModelData
		assertFalse(modelCopy.hasModelData());
	}

	@Test
	public void testGetModel() {
		Model testModel = createTestModel(0);

		//Get ModelACL for the model from Mongo
		ModelACL modelACL = modelRepository.findOneById(testModel.getId());

		Model retrievedModel = modelFactory.getModel(modelACL);

		//Model returned has the correct ID
		assertNotNull(retrievedModel);
		assertEquals(testModel.getId(), retrievedModel.getId());

		//Basic properties of the retrieved model are correct
		//(example fields from both Mongo and Jena)
		assertEquals(testModel.getUri(),         retrievedModel.getUri());
		assertEquals(testModel.getDomainGraph(), retrievedModel.getDomainGraph());
		assertEquals(testModel.getName(),        retrievedModel.getName());
		assertEquals(testModel.getDescription(), retrievedModel.getDescription());
	}

	@Test
	public void testGetModelForNullModelACL() {
		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> modelFactory.getModel(null)
			)
			.withMessage(
				"Attempting to get Model for null modelACL"
			);
	}

	@Test
	public void testGetModelOrNull() {
		Model testModel = createTestModel(0);

		//Get ModelACL for the model from Mongo
		ModelACL modelACL = modelRepository.findOneById(testModel.getId());

		Model retrievedModel = modelFactory.getModelOrNull(modelACL);

		//Model returned has the correct ID
		assertNotNull(retrievedModel);
		assertEquals(testModel.getId(), retrievedModel.getId());

		//Basic properties of the retrieved model are correct
		//(example fields from both Mongo and Jena)
		assertEquals(testModel.getUri(),         retrievedModel.getUri());
		assertEquals(testModel.getDomainGraph(), retrievedModel.getDomainGraph());
		assertEquals(testModel.getName(),        retrievedModel.getName());
		assertEquals(testModel.getDescription(), retrievedModel.getDescription());
	}

	@Test
	public void testGetModelOrNullForNullModelACL() {
		assertNull(modelFactory.getModelOrNull(null));
	}

	@Test
	public void testGetModels() {
		List<Model> testModels = new ArrayList<>();

		testModels.add(createTestModel(0));
		testModels.add(createTestModel(1));
		testModels.add(createTestModel(2));

		//Extract test fields from test models
		List<String> testModelIds = testModels
			.stream()
			.map(Model::getId)
			.collect(Collectors.toList());

		List<String> testModelUris = testModels
			.stream()
			.map(Model::getUri)
			.collect(Collectors.toList());

		List<String> testModelDomainGraphs = testModels
			.stream()
			.map(Model::getDomainGraph)
			.collect(Collectors.toList());

		List<String> testModelNames = testModels
			.stream()
			.map(Model::getName)
			.collect(Collectors.toList());

		List<String> testModelDescriptions = testModels
			.stream()
			.map(Model::getDescription)
			.collect(Collectors.toList());

		//Get ModelACLs for the models from Mongo
		List<ModelACL> modelACLs = modelRepository.findByUserId(testUserId);

		Set<Model> retrievedModels = modelFactory.getModels(modelACLs);

		assertEquals(3, retrievedModels.size());

		//Extract fields from both Mongo and Jena in retrieved models
		List<String> retrievedModelIds = retrievedModels
			.stream()
			.map(Model::getId)
			.collect(Collectors.toList());

		List<String> retrievedModelUris = retrievedModels
			.stream()
			.map(Model::getUri)
			.collect(Collectors.toList());

		List<String> retrievedModelDomainGraphs = retrievedModels
			.stream()
			.map(Model::getDomainGraph)
			.collect(Collectors.toList());

		List<String> retrievedModelNames = retrievedModels
			.stream()
			.map(Model::getName)
			.collect(Collectors.toList());

		List<String> retrievedModelDescriptions = retrievedModels
			.stream()
			.map(Model::getDescription)
			.collect(Collectors.toList());

		//Assert results match
		assertThat(retrievedModelIds,          containsInAnyOrder(testModelIds.toArray()));
		assertThat(retrievedModelUris,         containsInAnyOrder(testModelUris.toArray()));
		assertThat(retrievedModelDomainGraphs, containsInAnyOrder(testModelDomainGraphs.toArray()));
		assertThat(retrievedModelNames,        containsInAnyOrder(testModelNames.toArray()));
		assertThat(retrievedModelDescriptions, containsInAnyOrder(testModelDescriptions.toArray()));
	}

	@Test
	public void testGetModelsForNullList() {
		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> modelFactory.getModels(null)
			)
			.withMessage(
				"Attempting to get Models for null list"
			);
	}

	@Test
	public void testGetModelsForEmptyList() {
		assertThat(modelFactory.getModels(Collections.emptyList()), is(empty()));
	}
}
