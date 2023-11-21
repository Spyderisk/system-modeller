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

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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

import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;
import uk.ac.soton.itinnovation.security.modelquerier.SystemModelQuerier;
import uk.ac.soton.itinnovation.security.modelquerier.SystemModelUpdater;
import uk.ac.soton.itinnovation.security.modelquerier.util.ModelStack;
import uk.ac.soton.itinnovation.security.semanticstore.AStoreWrapper;
import uk.ac.soton.itinnovation.security.systemmodeller.SystemModellerApplication;
import uk.ac.soton.itinnovation.security.systemmodeller.TestHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.mongodb.IModelRepository;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.LoadingProgress;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.StoreModelManager;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SystemModellerApplication.class)
@TestPropertySource(properties = {"reset.on.start=false"})
public class ModelTest {

	private static final Logger logger = LoggerFactory.getLogger(ModelTest.class);

	private static TestHelper testHelper;

	@Autowired
	private ModelObjectsHelper modelHelper;

	@Autowired
	private ModelFactory modelFactory;

	@Autowired
	private StoreModelManager storeManager;

	@Autowired
	private IModelRepository modelRepository;

	//Allows automatic logging of test names
	@Rule
	public TestName name = new TestName();

	@BeforeClass
	public static void beforeClass() {
		logger.info("Setting up ModelTest class");
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

		return modelFactory.getModel(testHelper.getModel(), testHelper.getStore());
	}

	private Model createTestModelWithNoModelInfo(Integer index) {
		testHelper.switchModels(0, index);

		return modelFactory.getModelWithoutModelInfo(testHelper.getModel(), testHelper.getStore());
	}

/////////////////////////////////////////////////////////////////////////
//
// Constructor
//
/////////////////////////////////////////////////////////////////////////

	@Test
	public void testNewModel() {
		Model model = new Model(new ModelACL(), modelRepository, storeManager);

		//Model is not validating or calculating risks
		assertFalse(model.isValidating());
		assertFalse(model.isCalculatingRisks());

		//Model has no ModelInfo
		assertFalse(model.hasModelInfo());
	}

	@Test
	public void testNewModelWithNullModelACL() {
		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> new Model(null, modelRepository, storeManager)
			)
			.withMessage(
				"Attempting to create Model with null modelACL"
			);
	}

	@Test
	public void testNewModelWithNullModelRepository() {
		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> new Model(new ModelACL(), null, storeManager)
			)
			.withMessage(
				"Attempting to create Model with null modelRepository"
			);
	}

	@Test
	public void testNewModelWithNullStore() {
		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> new Model(new ModelACL(), modelRepository, (AStoreWrapper) null)
			)
			.withMessage(
				"Attempting to create Model with null store"
			);
	}

/////////////////////////////////////////////////////////////////////////
//
// ModelACL - stored in Mongo
//
/////////////////////////////////////////////////////////////////////////

	@Test
	public void testSaveModelACL() {
		Model testModel = createTestModel(0);

		//No model ID, modified time, or write URL initially in Mongo
		assertNull(testModel.getId());
		assertNull(testModel.getModified());
		assertNull(testModel.getWriteUrl());

		//Update write URL, and then save in Mongo
		testModel.setWriteUrl("writeUrl");
		testModel.saveModelACL();

		//Model now has an ID and a modified time and date (stored in Mongo)
		assertNotNull(testModel.getId());
		assertNotNull(testModel.getModified());

		//Get a fresh copy of the model by looking it up by its ID
		Model savedModel = modelFactory.getModel(modelRepository.findOneById(testModel.getId()));

		//Updated values successfully retrieved from Mongo
		assertEquals(testModel.getId(), savedModel.getId());
		assertEquals(testModel.getModified(), savedModel.getModified());
		assertEquals("writeUrl", savedModel.getWriteUrl());
	}

	@Test
	public void testSaveModelACLWhenAlreadySavedInMongo() {
		Model testModel = createTestModel(0);

		//Set read URL, and then save in Mongo
		testModel.setReadUrl("readUrl");
		testModel.saveModelACL();

		String id = testModel.getId();
		Date modified = testModel.getModified();

		//Wait briefly, then save again.
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			logger.error("Sleeping interuputed: {}", e);
			fail();
		}
		testModel.saveModelACL();

		//Read URL and ID unchanged, modified time increased
		assertEquals(id, testModel.getId());
		assertTrue(testModel.getModified().after(modified));
		assertEquals("readUrl", testModel.getReadUrl());
	}

	@Test
	public void testDeleteModelACL() {
		Model testModel = createTestModel(0);

		//Save model in Mongo
		testModel.saveModelACL();

		//Model in Mongo
		assertNotNull(modelRepository.findOneById(testModel.getId()));

		testModel.deleteModelACL();

		//Model no longer in Mongo
		assertNull(modelRepository.findOneById(testModel.getId()));
	}

	@Test
	public void testGetId() {
		Model testModel = createTestModel(0);

		//No model ID
		assertNull(testModel.getId());

		//Save model in Mongo
		testModel.saveModelACL();

		//Model now has an ID
		assertNotNull(testModel.getId());

		//Get a fresh copy of the model by looking it up by its ID
		Model savedModel = modelFactory.getModel(modelRepository.findOneById(testModel.getId()));

		//Model retrieved from Mongo has the right ID
		assertEquals(testModel.getId(), savedModel.getId());
	}

	@Test
	public void testSetUri() {
		Model testModel = createTestModel(0);

		String testString = UUID.randomUUID().toString();

		testModel.setUri(testString);

		assertEquals(testString, testModel.getUri());
	}

	@Test
	public void testSetDomainGraph() {
		Model testModel = createTestModel(0);

		String testString = UUID.randomUUID().toString();

		testModel.setDomainGraph(testString);

		assertEquals(testString, testModel.getDomainGraph());
	}

	@Test
	public void testSetCreated() {
		Model testModel = createTestModel(0);

		Date testDate = new Date(UUID.randomUUID().getMostSignificantBits());

		testModel.setCreated(testDate);

		assertEquals(testDate, testModel.getCreated());
	}

	@Test
	public void testSetUserId() {
		Model testModel = createTestModel(0);

		String testString = UUID.randomUUID().toString();

		testModel.setUserId(testString);

		assertEquals(testString, testModel.getUserId());
	}

	@Test
	public void testSetModified() {
		Model testModel = createTestModel(0);

		Date testDate = new Date(UUID.randomUUID().getMostSignificantBits());

		testModel.setModified(testDate);

		assertEquals(testDate, testModel.getModified());
	}

	@Test
	public void testSetModifiedBy() {
		Model testModel = createTestModel(0);

		String testString = UUID.randomUUID().toString();

		testModel.setModifiedBy(testString);

		assertEquals(testString, testModel.getModifiedBy());
	}

	@Test
	public void testSetNoRoleUrl() {
		Model testModel = createTestModel(0);

		String testString = UUID.randomUUID().toString();

		testModel.setNoRoleUrl(testString);

		assertEquals(testString, testModel.getNoRoleUrl());
	}

	@Test
	public void testSetReadUrl() {
		Model testModel = createTestModel(0);

		String testString = UUID.randomUUID().toString();

		testModel.setReadUrl(testString);

		assertEquals(testString, testModel.getReadUrl());
	}

	@Test
	public void testSetReadUsernames() {
		Model testModel = createTestModel(0);

		Set<String> testStrings = new HashSet<>();

		testStrings.add(UUID.randomUUID().toString());
		testStrings.add(UUID.randomUUID().toString());
		testStrings.add(UUID.randomUUID().toString());

		assertEquals(3, testStrings.size());

		testModel.setReadUsernames(testStrings);

		assertEquals(testStrings, testModel.getReadUsernames());
	}

	@Test
	public void testSetWriteUrl() {
		Model testModel = createTestModel(0);

		String testString = UUID.randomUUID().toString();

		testModel.setWriteUrl(testString);

		assertEquals(testString, testModel.getWriteUrl());
	}

	@Test
	public void testSetWriteUsernames() {
		Model testModel = createTestModel(0);

		Set<String> testStrings = new HashSet<>();

		testStrings.add(UUID.randomUUID().toString());
		testStrings.add(UUID.randomUUID().toString());
		testStrings.add(UUID.randomUUID().toString());

		assertEquals(3, testStrings.size());

		testModel.setWriteUsernames(testStrings);

		assertEquals(testStrings, testModel.getWriteUsernames());
	}

	@Test
	public void testSetOwnerUrl() {
		Model testModel = createTestModel(0);

		String testString = UUID.randomUUID().toString();

		testModel.setOwnerUrl(testString);

		assertEquals(testString, testModel.getOwnerUrl());
	}

	@Test
	public void testSetOwnerUsernames() {
		Model testModel = createTestModel(0);

		Set<String> testStrings = new HashSet<>();

		testStrings.add(UUID.randomUUID().toString());
		testStrings.add(UUID.randomUUID().toString());
		testStrings.add(UUID.randomUUID().toString());

		assertEquals(3, testStrings.size());

		testModel.setOwnerUsernames(testStrings);

		assertEquals(testStrings, testModel.getOwnerUsernames());
	}

	@Test
	public void testSetEditorId() {
		Model testModel = createTestModel(0);

		String testString = UUID.randomUUID().toString();

		testModel.setEditorId(testString);

		assertEquals(testString, testModel.getEditorId());
	}

	@Test
	public void testSetValidating() {
		Model testModel = createTestModel(0);

		testModel.setValidating(true);
		assertTrue(testModel.isValidating());

		testModel.setValidating(false);
		assertFalse(testModel.isValidating());
	}

	@Test
	public void testSetValidatingWhileValidating() {
		Model testModel = createTestModel(0);

		testModel.setValidating(true);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.setValidating(true)
			)
			.withMessage(
				"Cannot set validating: already validating"
			);
	}

	@Test
	public void testSetValidatingWhileCalculatingRisks() {
		Model testModel = createTestModel(0);

		testModel.setCalculatingRisks(true);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.setValidating(true)
			)
			.withMessage(
				"Cannot set validating: already calculating risks"
			);
	}

	@Test
	public void testSetValidatingFalseWhileNotValidating() {
		Model testModel = createTestModel(0);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.setValidating(false)
			)
			.withMessage(
				"Cannot clear validating: not validating"
			);
	}

	@Test
	public void testSetCalculatingRisks() {
		Model testModel = createTestModel(0);

		testModel.setCalculatingRisks(true);
		assertTrue(testModel.isCalculatingRisks());

		testModel.setCalculatingRisks(false);
		assertFalse(testModel.isCalculatingRisks());
	}

	@Test
	public void testSetCalculatingRisksWhileCalculatingRisks() {
		Model testModel = createTestModel(0);

		testModel.setCalculatingRisks(true);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.setCalculatingRisks(true)
			)
			.withMessage(
				"Cannot set calculatingRisk: already calculating risks"
			);
	}

	@Test
	public void testSetCalculatingRisksWhileValidating() {
		Model testModel = createTestModel(0);

		testModel.setValidating(true);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.setCalculatingRisks(true)
			)
			.withMessage(
				"Cannot set calculatingRisk: already validating"
			);
	}

	@Test
	public void testSetCalculatingRisksFalseWhileNotCalculatingRisks() {
		Model testModel = createTestModel(0);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.setCalculatingRisks(false)
			)
			.withMessage(
				"Cannot clear calculatingRisk: not calculating risks"
			);
	}

/////////////////////////////////////////////////////////////////////////
//
// ModelInfo - stored in Jena
//
/////////////////////////////////////////////////////////////////////////

	@Test
	public void testHasModelInfo() {
		Model testModel = createTestModel(0);

		assertTrue(testModel.hasModelInfo());
	}

	@Test
	public void testHasModelInfoForModelWithNoModelInfo() {
		Model testModel = createTestModelWithNoModelInfo(0);

		assertFalse(testModel.hasModelInfo());
	}

	@Test
	public void testLoadModelInfo() {
		Model testModel = createTestModelWithNoModelInfo(0);

		//Model has no ModelInfo
		assertFalse(testModel.hasModelInfo());

		testModel.loadModelInfo();

		//Model now has ModelInfo
		assertTrue(testModel.hasModelInfo());
	}

	@Test
	public void testLoadModelInfoForModelWithInconsistentURIs() {
		Model testModel = createTestModelWithNoModelInfo(0);

		//The SPARQL query uses the URI from the ModelACL as the graph name.
		//This is always returned unaltered in the ModelInfo if it is a VALID URI.
		//If it is not a valid URI it tries to turn it into a URI.
		testModel.setUri("notAValidUri");

		assertThatExceptionOfType(RuntimeException.class)
			.isThrownBy(
				() -> testModel.loadModelInfo()
			)
			.withMessageMatching(
				"Inconsistent URIs in modelACL <notAValidUri>" +
				" and modelInfo <file:///.*/notAValidUri>"
			);
	}

	@Test
	public void testLoadModelInfoForModelWithInconsistentDomainGraphURIs() {
		Model testModel = createTestModelWithNoModelInfo(0);

		//Set the domain graph to something different from that stored in the model graph.
		testModel.setDomainGraph("notAValidUri");

		assertThatExceptionOfType(RuntimeException.class)
			.isThrownBy(
				() -> testModel.loadModelInfo()
			)
			.withMessage(
				"Inconsistent domain graph URIs in modelACL <notAValidUri>" +
				" and modelInfo <" + TestHelper.DOM_SHIELD_URI + ">"
			);
	}

	@Test
	public void testGetName() {
		Model testModel = createTestModel(0);

		assertEquals("GA Test", testModel.getName());
	}

	@Test
	public void testGetNameForModelWithNoModelInfo() {
		Model testModel = createTestModelWithNoModelInfo(0);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.getName()
			)
			.withMessage(
				"modelInfo is null: loadModelInfo() must be called first"
			);
	}

	@Test
	public void testSetName() {
		Model testModel = createTestModel(0);

		String testString = UUID.randomUUID().toString();

		testModel.setName(testString);

		assertEquals(testString, testModel.getName());
	}

	@Test
	public void testSetNameForModelWithNoModelInfo() {
		Model testModel = createTestModelWithNoModelInfo(0);

		String testString = UUID.randomUUID().toString();

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.setName(testString)
			)
			.withMessage(
				"modelInfo is null: loadModelInfo() must be called first"
			);
	}

	@Test
	public void testGetDescription() {
		Model testModel = createTestModel(0);

		assertEquals("Test model for the SHiELD GA", testModel.getDescription());
	}

	@Test
	public void testGetDescriptionForModelWithNoModelInfo() {
		Model testModel = createTestModelWithNoModelInfo(0);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.getDescription()
			)
			.withMessage(
				"modelInfo is null: loadModelInfo() must be called first"
			);
	}

	@Test
	public void testSetDescription() {
		Model testModel = createTestModel(0);

		String testString = UUID.randomUUID().toString();

		testModel.setDescription(testString);

		assertEquals(testString, testModel.getDescription());
	}

	@Test
	public void testSetDescriptionForModelWithNoModelInfo() {
		Model testModel = createTestModelWithNoModelInfo(0);

		String testString = UUID.randomUUID().toString();

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.setDescription(testString)
			)
			.withMessage(
				"modelInfo is null: loadModelInfo() must be called first"
			);
	}

	@Test
	public void testIsValid() {
		Model testModel = createTestModel(0);

		assertTrue(testModel.isValid());
	}

	@Test
	public void testIsValidForModelWithNoModelInfo() {
		Model testModel = createTestModelWithNoModelInfo(0);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.isValid()
			)
			.withMessage(
				"modelInfo is null: loadModelInfo() must be called first"
			);
	}

	@Test
	public void testSetValid() {
		Model testModel = createTestModel(0);

		//Model and risks valid
		testModel.setValid(true);
		testModel.setRiskLevelsValid(true);

		testModel.setValid(false);

		//Risks now also invalid
		assertFalse(testModel.isValid());
		assertFalse(testModel.riskLevelsValid());

		testModel.setValid(true);

		//Risks still invalid
		assertTrue(testModel.isValid());
		assertFalse(testModel.riskLevelsValid());
	}

	@Test
	public void testSetValidForModelWithNoModelInfo() {
		Model testModel = createTestModelWithNoModelInfo(0);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.setValid(true)
			)
			.withMessage(
				"modelInfo is null: loadModelInfo() must be called first"
			);
	}

	@Test
	public void testSetValidWhileValidating() {
		Model testModel = createTestModel(0);

		testModel.setValidating(true);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.setValid(true)
			)
			.withMessage(
				"Cannot set valid: model is validating"
			);
	}

	@Test
	public void testSetValidFalseWhileValidating() {
		Model testModel = createTestModel(0);

		testModel.setValidating(true);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.setValid(false)
			)
			.withMessage(
				"Cannot set valid: model is validating"
			);
	}

	@Test
	public void testSetValidWhileCalculatingRisks() {
		Model testModel = createTestModel(0);

		testModel.setCalculatingRisks(true);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.setValid(true)
			)
			.withMessage(
				"Cannot set valid: model is calculating risks"
			);
	}

	@Test
	public void testSetValidFalseWhileCalculatingRisks() {
		Model testModel = createTestModel(0);

		testModel.setCalculatingRisks(true);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.setValid(false)
			)
			.withMessage(
				"Cannot set valid: model is calculating risks"
			);
	}

	@Test
	public void testRiskLevelsValid() {
		Model testModel = createTestModel(0);

		assertTrue(testModel.riskLevelsValid());
	}

	@Test
	public void testRiskLevelsValidForModelWithNoModelInfo() {
		Model testModel = createTestModelWithNoModelInfo(0);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.riskLevelsValid()
			)
			.withMessage(
				"modelInfo is null: loadModelInfo() must be called first"
			);
	}

	@Test
	public void testSetRiskLevelsValid() {
		Model testModel = createTestModel(0);

		//Model and risks valid
		testModel.setValid(true);
		testModel.setRiskLevelsValid(true);

		testModel.setRiskLevelsValid(false);

		//Model still valid
		assertTrue(testModel.isValid());
		assertFalse(testModel.riskLevelsValid());

		testModel.setRiskLevelsValid(true);

		//Model again still valid
		assertTrue(testModel.isValid());
		assertTrue(testModel.riskLevelsValid());
	}

	@Test
	public void testSetRiskLevelsValidForModelWithNoModelInfo() {
		Model testModel = createTestModelWithNoModelInfo(0);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.setRiskLevelsValid(true)
			)
			.withMessage(
				"modelInfo is null: loadModelInfo() must be called first"
			);
	}

	@Test
	public void testSetRiskLevelsValidWhileInvalid() {
		Model testModel = createTestModel(0);

		testModel.setValid(false);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.setRiskLevelsValid(true)
			)
			.withMessage(
				"Cannot set riskLevelsValid: model is invalid"
			);
	}

	@Test
	public void testSetRiskLevelsValidWhileValidating() {
		Model testModel = createTestModel(0);

		testModel.setValidating(true);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.setRiskLevelsValid(true)
			)
			.withMessage(
				"Cannot set riskLevelsValid: model is validating"
			);
	}

	@Test
	public void testSetRiskLevelsValidFalseWhileValidating() {
		Model testModel = createTestModel(0);

		testModel.setValidating(true);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.setRiskLevelsValid(false)
			)
			.withMessage(
				"Cannot set riskLevelsValid: model is validating"
			);
	}

	@Test
	public void testSetRiskLevelsValidWhileCalculatingRisks() {
		Model testModel = createTestModel(0);

		testModel.setCalculatingRisks(true);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.setRiskLevelsValid(true)
			)
			.withMessage(
				"Cannot set riskLevelsValid: model is calculating risks"
			);
	}

	@Test
	public void testSetRiskLevelsValidFalseWhileCalculatingRisks() {
		Model testModel = createTestModel(0);

		testModel.setCalculatingRisks(true);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.setRiskLevelsValid(false)
			)
			.withMessage(
				"Cannot set riskLevelsValid: model is calculating risks"
			);
	}

	@Test
	public void testGetRiskLevel() {
		Model testModel = createTestModel(0);

		//It shouldn't be null
		//But the test model has it in the asserted graph
		//Whereas the code looks in the inferred graph
		assertNull(testModel.getRiskLevel());
	}

	@Test
	public void testGetRiskLevelForModelWithNoModelInfo() {
		Model testModel = createTestModelWithNoModelInfo(0);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.getRiskLevel()
			)
			.withMessage(
				"modelInfo is null: loadModelInfo() must be called first"
			);
	}

	@Test
	public void testGetRiskLevelWhileRiskLevelsInvalid() {
		Model testModel = createTestModel(0);

		testModel.setRiskLevelsValid(false);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.getRiskLevel()
			)
			.withMessage(
				"Cannot get riskLevel: risk levels are invalid"
			);
	}

	@Test
	public void testGetRiskCalculationMode() {
		Model testModel = createTestModel(0);

		assertNull(testModel.getRiskCalculationMode());
	}

	@Test
	public void testGetRiskCalculationModeForModelWithNoModelInfo() {
		Model testModel = createTestModelWithNoModelInfo(0);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.getRiskCalculationMode()
			)
			.withMessage(
				"modelInfo is null: loadModelInfo() must be called first"
			);
	}

	@Test
	public void testSetRiskCalculationMode() {
		Model testModel = createTestModel(0);

		RiskCalculationMode testMode = RiskCalculationMode.FUTURE;

		testModel.setRiskCalculationMode(testMode);

		assertEquals(testMode, testModel.getRiskCalculationMode());
	}

	@Test
	public void testSetRiskCalculationModeForModelWithNoModelInfo() {
		Model testModel = createTestModelWithNoModelInfo(0);

		RiskCalculationMode testMode = RiskCalculationMode.FUTURE;

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.setRiskCalculationMode(testMode)
			)
			.withMessage(
				"modelInfo is null: loadModelInfo() must be called first"
			);
	}

/////////////////////////////////////////////////////////////////////////
//
// ModelData - stored in Jena
//
/////////////////////////////////////////////////////////////////////////

	@Test
	public void testHasModelData() {
		Model testModel = createTestModel(0);

		assertFalse(testModel.hasModelData());
	}

	@Test
	public void testLoadModelData() {
		Model testModel = createTestModel(0);

		//Model has no ModelData
		assertFalse(testModel.hasModelData());

		testModel.loadModelData(modelHelper, new LoadingProgress(null));

		//Model now has ModelData
		assertTrue(testModel.hasModelData());
	}

	@Test
	public void testLoadModelDataForModelWithNoAssets() {
		Model testModel = createTestModel(5);

		//Model has no ModelData
		assertFalse(testModel.hasModelData());

		testModel.loadModelData(modelHelper, new LoadingProgress(null));

		//Model now has ModelData
		assertTrue(testModel.hasModelData());
	}

	@Test
	public void testGetAssets() {
		Model testModel = createTestModel(0);

		testModel.loadModelData(modelHelper, new LoadingProgress(null));

		assertEquals(83, testModel.getAssets().size());
	}

	@Test
	public void testGetAssetsForModelWithNoModelData() {
		Model testModel = createTestModel(0);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.getAssets()
			)
			.withMessage(
				"modelData is null: loadModelData() must be called first"
			);
	}

	@Test
	public void testGetRelations() {
		Model testModel = createTestModel(0);

		testModel.loadModelData(modelHelper, new LoadingProgress(null));

		assertEquals(235, testModel.getRelations().size());
	}

	@Test
	public void testGetRelationsForModelWithNoModelData() {
		Model testModel = createTestModel(0);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.getRelations()
			)
			.withMessage(
				"modelData is null: loadModelData() must be called first"
			);
	}

	@Test
	public void testGetMisbehaviourSets() {
		Model testModel = createTestModel(0);

		testModel.loadModelData(modelHelper, new LoadingProgress(null));

		assertEquals(226, testModel.getMisbehaviourSets().size());
	}

	@Test
	public void testGetMisbehaviourSetsForModelWithNoModelData() {
		Model testModel = createTestModel(0);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.getMisbehaviourSets()
			)
			.withMessage(
				"modelData is null: loadModelData() must be called first"
			);
	}

	@Test
	public void testGetTwas() {
		Model testModel = createTestModel(0);

		testModel.loadModelData(modelHelper, new LoadingProgress(null));

		assertEquals(75, testModel.getTwas().size());
	}

	@Test
	public void testGetTwasForModelWithNoModelData() {
		Model testModel = createTestModel(0);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.getTwas()
			)
			.withMessage(
				"modelData is null: loadModelData() must be called first"
			);
	}

	@Test
	public void testGetControlSets() {
		Model testModel = createTestModel(0);

		testModel.loadModelData(modelHelper, new LoadingProgress(null));

		assertEquals(382, testModel.getControlSets().size());
	}

	@Test
	public void testGetControlSetsForModelWithNoModelData() {
		Model testModel = createTestModel(0);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.getControlSets()
			)
			.withMessage(
				"modelData is null: loadModelData() must be called first"
			);
	}

	@Test
	public void testGetControlStrategies() {
		Model testModel = createTestModel(0);

		testModel.loadModelData(modelHelper, new LoadingProgress(null));

		assertEquals(278, testModel.getControlStrategies().size());
	}

	@Test
	public void testGetControlStrategiesForModelWithNoModelData() {
		Model testModel = createTestModel(0);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.getControlStrategies()
			)
			.withMessage(
				"modelData is null: loadModelData() must be called first"
			);
	}

	@Test
	public void testGetThreats() {
		Model testModel = createTestModel(0);

		testModel.loadModelData(modelHelper, new LoadingProgress(null));

		assertEquals(264, testModel.getThreats().size());
	}

	@Test
	public void testGetThreatsForModelWithNoModelData() {
		Model testModel = createTestModel(0);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.getThreats()
			)
			.withMessage(
				"modelData is null: loadModelData() must be called first"
			);
	}

	@Test
	public void testGetComplianceThreats() {
		Model testModel = createTestModel(0);

		testModel.loadModelData(modelHelper, new LoadingProgress(null));

		assertEquals(12, testModel.getComplianceThreats().size());
	}

	@Test
	public void testGetComplianceThreatsForModelWithNoModelData() {
		Model testModel = createTestModel(0);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.getComplianceThreats()
			)
			.withMessage(
				"modelData is null: loadModelData() must be called first"
			);
	}

	@Test
	public void testGetComplianceSets() {
		Model testModel = createTestModel(0);

		testModel.loadModelData(modelHelper, new LoadingProgress(null));

		assertEquals(5, testModel.getComplianceSets().size());
	}

	@Test
	public void testGetComplianceSetsForModelWithNoModelData() {
		Model testModel = createTestModel(0);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.getComplianceSets()
			)
			.withMessage(
				"modelData is null: loadModelData() must be called first"
			);
	}

	@Test
	public void testGetLevels() {
		Model testModel = createTestModel(0);

		testModel.loadModelData(modelHelper, new LoadingProgress(null));

		assertEquals(5, testModel.getLevels().size());
	}

	@Test
	public void testGetLevelsForModelWithNoModelData() {
		Model testModel = createTestModel(0);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.getLevels()
			)
			.withMessage(
				"modelData is null: loadModelData() must be called first"
			);
	}

/////////////////////////////////////////////////////////////////////////
//
// Validation and risk calculation state machine
//
/////////////////////////////////////////////////////////////////////////

	@Test
	public void testInvalidate() {
		Model testModel = createTestModel(0);

		//Model and risks valid
		testModel.setValid(true);
		testModel.setRiskLevelsValid(true);

		testModel.invalidate();

		//Risks also invalid
		assertFalse(testModel.isValid());
		assertFalse(testModel.riskLevelsValid());
	}

	@Test
	public void testInvalidateRiskLevels() {
		Model testModel = createTestModel(0);

		//Model and risks valid
		testModel.setValid(true);
		testModel.setRiskLevelsValid(true);

		testModel.invalidateRiskLevels();

		//Model still valid
		assertTrue(testModel.isValid());
		assertFalse(testModel.riskLevelsValid());
	}

	@Test
	public void testMarkAsValidating() {
		Model testModel = createTestModel(0);

		//Model invalid
		testModel.setValid(false);

		testModel.markAsValidating();

		//Model and risks invalid
		assertFalse(testModel.isValid());
		assertFalse(testModel.riskLevelsValid());

		//Validating but not calculating risks
		assertTrue(testModel.isValidating());
		assertFalse(testModel.isCalculatingRisks());
	}

	@Test
	public void testMarkAsValidatingWhileValid() {
		Model testModel = createTestModel(0);

		//Model and risks valid
		testModel.setValid(true);
		testModel.setRiskLevelsValid(true);

		testModel.markAsValidating();

		//Model and risks invalid
		assertFalse(testModel.isValid());
		assertFalse(testModel.riskLevelsValid());

		//Validating but not calculating risks
		assertTrue(testModel.isValidating());
		assertFalse(testModel.isCalculatingRisks());
	}

	@Test
	public void testMarkAsValidatingWhileValidating() {
		Model testModel = createTestModel(0);

		//Model validating
		testModel.markAsValidating();

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.markAsValidating()
			)
			.withMessage(
				"Cannot set validating: already validating"
			);
	}

	@Test
	public void testMarkAsValidatingWhileCalculatingRisks() {
		Model testModel = createTestModel(0);

		//Model calculating risks
		testModel.markAsCalculatingRisks(RiskCalculationMode.FUTURE, true);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.markAsValidating()
			)
			.withMessage(
				"Cannot set validating: already calculating risks"
			);
	}

	@Test
	public void testfinishedValidatingWithSuccess() {
		Model testModel = createTestModel(0);

		//Model validating
		testModel.markAsValidating();

		testModel.finishedValidating(true);

		//Model valid and risks invalid
		assertTrue(testModel.isValid());
		assertFalse(testModel.riskLevelsValid());

		//Not validating or calculating risks
		assertFalse(testModel.isValidating());
		assertFalse(testModel.isCalculatingRisks());
	}

	@Test
	public void testfinishedValidatingWithFailure() {
		Model testModel = createTestModel(0);

		//Model validating
		testModel.markAsValidating();

		testModel.finishedValidating(false);

		//Model and risks invalid
		assertFalse(testModel.isValid());
		assertFalse(testModel.riskLevelsValid());

		//Not validating or calculating risks
		assertFalse(testModel.isValidating());
		assertFalse(testModel.isCalculatingRisks());
	}

	@Test
	public void testfinishedValidatingWhileNotValidating() {
		Model testModel = createTestModel(0);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.finishedValidating(true)
			)
			.withMessage(
				"Cannot clear validating: not validating"
			);
	}

	@Test
	public void testfinishedValidatingWhileCalculatingRisks() {
		Model testModel = createTestModel(0);

		//Calculating risks
		testModel.markAsCalculatingRisks(RiskCalculationMode.FUTURE, true);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.finishedValidating(true)
			)
			.withMessage(
				"Cannot clear validating: not validating"
			);
	}

	@Test
	public void testMarkAsCalculatingRisksWithModeFuture() {
		Model testModel = createTestModel(0);

		//Risk levels invalid
		testModel.setRiskLevelsValid(false);

		testModel.markAsCalculatingRisks(RiskCalculationMode.FUTURE, true);

		//Model valid but risk levels invalid
		assertTrue(testModel.isValid());
		assertFalse(testModel.riskLevelsValid());

		//Calculating FUTURE risks but not validating
		assertFalse(testModel.isValidating());
		assertTrue(testModel.isCalculatingRisks());
		assertEquals(RiskCalculationMode.FUTURE, testModel.getRiskCalculationMode());
	}

	@Test
	public void testMarkAsCalculatingRisksWithModeCurrent() {
		Model testModel = createTestModel(0);

		//Risk levels invalid
		testModel.setRiskLevelsValid(false);

		testModel.markAsCalculatingRisks(RiskCalculationMode.CURRENT, true);

		//Model valid but risk levels invalid
		assertTrue(testModel.isValid());
		assertFalse(testModel.riskLevelsValid());

		//Calculating CURRENT risks but not validating
		assertFalse(testModel.isValidating());
		assertTrue(testModel.isCalculatingRisks());
		assertEquals(RiskCalculationMode.CURRENT, testModel.getRiskCalculationMode());
	}

	@Test
	public void testMarkAsCalculatingRisksWhileRiskLevelsValid() {
		Model testModel = createTestModel(0);

		//Risk levels valid
		testModel.setRiskLevelsValid(true);

		testModel.markAsCalculatingRisks(RiskCalculationMode.FUTURE, true);

		//Model valid but risk levels invalid
		assertTrue(testModel.isValid());
		assertFalse(testModel.riskLevelsValid());

		//Calculating FUTURE risks but not validating
		assertFalse(testModel.isValidating());
		assertTrue(testModel.isCalculatingRisks());
		assertEquals(RiskCalculationMode.FUTURE, testModel.getRiskCalculationMode());
	}

	@Test
	public void testMarkAsCalculatingRisksWhileValidating() {
		Model testModel = createTestModel(0);

		//Model validating
		testModel.markAsValidating();

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.markAsCalculatingRisks(RiskCalculationMode.FUTURE, true)
			)
			.withMessage(
				"Cannot set calculatingRisk: already validating"
			);
	}

	@Test
	public void testMarkAsCalculatingRisksWhileCalculatingRisks() {
		Model testModel = createTestModel(0);

		//Model calculating risks
		testModel.markAsCalculatingRisks(RiskCalculationMode.FUTURE, true);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.markAsCalculatingRisks(RiskCalculationMode.FUTURE, true)
			)
			.withMessage(
				"Cannot set calculatingRisk: already calculating risks"
			);
	}

	@Test
	public void testfinishedCalculatingRisksWithSuccess() {
		Model testModel = createTestModel(0);

		//Model calculating FUTURE risks
		testModel.markAsCalculatingRisks(RiskCalculationMode.FUTURE, true);

		testModel.finishedCalculatingRisks(true, RiskCalculationMode.FUTURE, true);

		//Model and risks valid
		assertTrue(testModel.isValid());
		assertTrue(testModel.riskLevelsValid());
		assertEquals(RiskCalculationMode.FUTURE, testModel.getRiskCalculationMode());

		//Not validating or calculating risks
		assertFalse(testModel.isValidating());
		assertFalse(testModel.isCalculatingRisks());
	}

	@Test
	public void testfinishedCalculatingRisksWithFailure() {
		Model testModel = createTestModel(0);

		//Model calculating FUTURE risks
		testModel.markAsCalculatingRisks(RiskCalculationMode.FUTURE, true);

		testModel.finishedCalculatingRisks(false, RiskCalculationMode.FUTURE, true);

		//Model valid and risks invalid
		assertTrue(testModel.isValid());
		assertFalse(testModel.riskLevelsValid());
		assertEquals(RiskCalculationMode.FUTURE, testModel.getRiskCalculationMode());

		//Not validating or calculating risks
		assertFalse(testModel.isValidating());
		assertFalse(testModel.isCalculatingRisks());
	}

	@Test
	public void testfinishedCalculatingRisksWhileNotCalculatingRisks() {
		Model testModel = createTestModel(0);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.finishedCalculatingRisks(true, RiskCalculationMode.FUTURE, true)
			)
			.withMessage(
				"Cannot clear calculatingRisk: not calculating risks"
			);
	}

	@Test
	public void testfinishedCalculatingRisksWhileValidating() {
		Model testModel = createTestModel(0);

		//Validating
		testModel.markAsValidating();

		assertThatIllegalStateException()
			.isThrownBy(
				() -> testModel.finishedCalculatingRisks(true, RiskCalculationMode.FUTURE, true)
			)
			.withMessage(
				"Cannot clear calculatingRisk: not calculating risks"
			);
	}

/////////////////////////////////////////////////////////////////////////
//
// Other methods
//
/////////////////////////////////////////////////////////////////////////

	@Test
	public void testToString() {
		Model testModel = createTestModel(0);

		String expectedString =
			"\nid: null" +
			"\nuri: " + TestHelper.SYS_SHIELD_URI +
			"\ndomainGraph: " + TestHelper.DOM_SHIELD_URI +
			"\ncreated: null" +
			"\nuserId: null" +
			"\nmodified: null" +
			"\nmodifiedBy: null" +
			"\nnoRoleUrl: null" +
			"\nreadUrl: null" +
			"\nreadUsernames: []" +
			"\nwriteUrl: null" +
			"\nwriteUsernames: []" +
			"\nownerUrl: null" +
			"\nownerUsernames: []" +
			"\neditorId: null" +
			"\nvalidating: false" +
			"\ncalculatingRisk: false" +
			"\nname: GA Test" +
			"\ndescription: Test model for the SHiELD GA" +
			"\nvalidatedDomainVersion: null" +
			"\nvalid: true" +
			"\nriskLevelsValid: true" +
			"\nriskLevel: null" +
			"\nriskCalculationMode: null" +
			"\ndomainVersion: null";

			logger.debug("expected:\n" + expectedString);
			logger.debug("actual:\n" + testModel.toString());

		assertEquals(expectedString, testModel.toString());
	}

	@Test
	public void testToStringForModelWithNoModelInfo() {
		Model testModel = createTestModelWithNoModelInfo(0);

		String expectedString =
			"\nid: null" +
			"\nuri: " + TestHelper.SYS_SHIELD_URI +
			"\ndomainGraph: " + TestHelper.DOM_SHIELD_URI +
			"\ncreated: null" +
			"\nuserId: null" +
			"\nmodified: null" +
			"\nmodifiedBy: null" +
			"\nnoRoleUrl: null" +
			"\nreadUrl: null" +
			"\nreadUsernames: []" +
			"\nwriteUrl: null" +
			"\nwriteUsernames: []" +
			"\nownerUrl: null" +
			"\nownerUsernames: []" +
			"\neditorId: null" +
			"\nvalidating: false" +
			"\ncalculatingRisk: false" +
			"\ndomainVersion: null";

		assertEquals(expectedString, testModel.toString());
	}

	@Test
	public void testToStringForModelWithRiskLevelsInvalid() {
		Model testModel = createTestModel(3);

		String expectedString =
			"\nid: null" +
			"\nuri: " + TestHelper.SYS_TEST_VALID +
			"\ndomainGraph: " + TestHelper.DOM_SHIELD_URI +
			"\ncreated: null" +
			"\nuserId: null" +
			"\nmodified: null" +
			"\nmodifiedBy: null" +
			"\nnoRoleUrl: null" +
			"\nreadUrl: null" +
			"\nreadUsernames: []" +
			"\nwriteUrl: null" +
			"\nwriteUsernames: []" +
			"\nownerUrl: null" +
			"\nownerUsernames: []" +
			"\neditorId: null" +
			"\nvalidating: false" +
			"\ncalculatingRisk: false" +
			"\nname: Host-Process-Connected-Validated" +
			"\ndescription: " +
			"\nvalidatedDomainVersion: null" +
			"\nvalid: true" +
			"\nriskLevelsValid: false" +
			"\nriskCalculationMode: null" +
			"\ndomainVersion: null";

		assertEquals(expectedString, testModel.toString());
	}

	@Test
	public void testSave() {
		Model testModel = createTestModel(0);

		//No model ID, modified time, or write URL initially in Mongo
		assertNull(testModel.getId());
		assertNull(testModel.getModified());
		assertNull(testModel.getWriteUrl());

		//Initial name and description from Jena
		assertEquals("GA Test", testModel.getName());
		assertEquals("Test model for the SHiELD GA", testModel.getDescription());

		//Update name, description and write URL, and then save in Mongo and Jena
		testModel.setName("New Model Name");
		testModel.setDescription("New model description");
		testModel.setWriteUrl("writeUrl");
		testModel.save();

		//Model now has an ID and a modified time and date (stored in Mongo)
		assertNotNull(testModel.getId());
		assertNotNull(testModel.getModified());

		//Get a fresh copy of the model by looking it up by its ID
		Model savedModel = modelFactory.getModel(modelRepository.findOneById(testModel.getId()));

		//Updated values successfully retrieved from Mongo
		assertEquals(testModel.getId(), savedModel.getId());
		assertEquals(testModel.getModified(), savedModel.getModified());
		assertEquals("writeUrl", savedModel.getWriteUrl());

		//Updated values successfully retrieved from Jena
		assertEquals("New Model Name", savedModel.getName());
		assertEquals("New model description", savedModel.getDescription());
	}

	@Test
	public void testDelete() {
		Model testModel = modelFactory.createModel("http://domainGraph", "userId");

		//Model in Mongo
		assertNotNull(modelRepository.findOneById(testModel.getId()));

		//Model graph exists
		assertEquals(10, storeManager.getStore().getCount(testModel.getUri()));

		//Model in management graph
		assertTrue(storeManager.systemModelExists(testModel.getUri()));

		testModel.delete();

		//Model no longer in Mongo
		assertNull(modelRepository.findOneById(testModel.getId()));

		//Model graph is empty
		//(Jena does not distinguish between empty and non-existant graphs)
		assertEquals(0, storeManager.getStore().getCount(testModel.getUri()));

		//Model no longer in management graph
		assertFalse(storeManager.systemModelExists(testModel.getUri()));
	}

	@Test
	public void testGetModelStack() {
		Model testModel = createTestModel(0);

		ModelStack stack = testModel.getModelStack();

		assertEquals(stack.getGraph("core"), "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/core");
		assertEquals(stack.getGraph("domain"), testModel.getDomainGraph());
		assertEquals(stack.getGraph("system"), testModel.getUri());
		assertEquals(stack.getGraph("system-ui"), testModel.getUri() + "/ui");
		assertEquals(stack.getGraph("system-inf"), testModel.getUri() + "/inf");
		assertEquals(stack.getGraph("system-meta"), testModel.getUri() + "/meta");
	}

	@Test
	public void testGetModelStackForModelWithMissingUri() {
		Model testModel = createTestModelWithNoModelInfo(0);

		//Unset URI
		testModel.setUri(null);

		assertThatExceptionOfType(RuntimeException.class)
			.isThrownBy(
				() -> testModel.getModelStack()
			)
			.withMessage(
				"Missing URI or domainGraph from model"
			);
	}

	@Test
	public void testGetModelStackForModelWithMissingDomainGraph() {
		Model testModel = createTestModelWithNoModelInfo(0);

		//Unset domainGraph
		testModel.setDomainGraph(null);

		assertThatExceptionOfType(RuntimeException.class)
			.isThrownBy(
				() -> testModel.getModelStack()
			)
			.withMessage(
				"Missing URI or domainGraph from model"
			);
	}

	@Test
	public void testGetQuerier() {
		Model testModel = createTestModel(0);

		SystemModelQuerier querier = testModel.getQuerier();

		assertEquals(querier.getModel().getGraph("system"), testModel.getUri());
		assertEquals(querier.getModel().getGraph("domain"), testModel.getDomainGraph());
	}

	@Test
	public void testGetUpdater() {
		Model testModel = createTestModel(0);

		SystemModelUpdater updater = testModel.getUpdater();

		assertEquals(updater.getModel().getGraph("system"), testModel.getUri());
		assertEquals(updater.getModel().getGraph("domain"), testModel.getDomainGraph());
	}
}
