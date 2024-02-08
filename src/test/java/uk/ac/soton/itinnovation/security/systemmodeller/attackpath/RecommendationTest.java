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
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.recommendations.RecommendationReportDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.StoreModelManager;

import uk.ac.soton.itinnovation.security.systemmodeller.mongodb.RecommendationRepository;
import uk.ac.soton.itinnovation.security.systemmodeller.model.RecommendationEntity;
import uk.ac.soton.itinnovation.security.systemmodeller.attackpath.RecommendationsService.RecStatus;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SystemModellerApplication.class)
@TestPropertySource(properties = {"reset.on.start=false"})
public class RecommendationTest {

	private static final Logger logger = LoggerFactory.getLogger(RecommendationTest.class);

	private static TestHelper testHelper;

	@Autowired
	private ModelObjectsHelper modelHelper;

	@Autowired
	private ModelFactory modelFactory;

	@Autowired
	private StoreModelManager storeManager;

	@Autowired
	private IModelRepository modelRepository;

	@Autowired
	private RecommendationRepository recRepository;

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
// Recommendation - stored in Mongo
//
/////////////////////////////////////////////////////////////////////////

    @Test
    public void testSaveRecommendation2() {
        logger.debug("create recommendation entity");
        RecommendationEntity rec = new RecommendationEntity();
        //rec.setId("1234");
        RecommendationReportDTO report = new RecommendationReportDTO();
        rec.setReport(report);
        rec.setStatus(RecStatus.FINISHED);
        recRepository.save(rec);
        logger.info("Created record: {}", rec.getId());
    }

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
}
