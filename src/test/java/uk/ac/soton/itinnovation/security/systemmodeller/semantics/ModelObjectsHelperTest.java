/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2018
//
// Copyright in this library belongs to the University of Southampton
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
//  Created By :            Joshua Wright
//  Created Date :          06/07/2018
//  Updated By :            
//  Created for Project :   RESTASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.semantics;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import uk.ac.soton.itinnovation.security.model.system.Asset;
import uk.ac.soton.itinnovation.security.model.system.Relation;
import uk.ac.soton.itinnovation.security.modelvalidator.ModelValidator;
import uk.ac.soton.itinnovation.security.modelvalidator.Progress;
import uk.ac.soton.itinnovation.security.systemmodeller.SystemModellerApplication;
import uk.ac.soton.itinnovation.security.systemmodeller.TestHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.model.ModelFactory;
import uk.ac.soton.itinnovation.security.systemmodeller.mongodb.IModelRepository;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.LoadingProgress;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = SystemModellerApplication.class)
@TestPropertySource(properties = {"model.management.uri=${model.management.uri.test}", "reset.on.start=false"})
public class ModelObjectsHelperTest {

	private static final Logger logger = LoggerFactory.getLogger(ModelObjectsHelperTest.class);

	//Arbitrary limit for using thread.sleep in loop, longer than 5 seconds considered test failure
	private static final int MAX_SLEEP = 5000;
	
	private static TestHelper testHelper;

	@Autowired
	private ModelObjectsHelper modelHelper;

	@Autowired
	private ModelFactory modelFactory;

	@Autowired
	private StoreModelManager storeManager;

	@Autowired
	private IModelRepository modelRepository;

	@BeforeClass
	public static void beforeClass() {
		logger.info("Setting up for ModelObjectsHelperTest class");

		testHelper = new TestHelper("jena-tdb");
		testHelper.setUp();
	}

	@Before
	public void setUp() {
		logger.info("Setting ModelObjectsHelperTest up");
		storeManager.clearMgtGraph();
		modelRepository.deleteAll();
	}

	/**
	 * Retrieves a model object for the specified index (from testHelper)
	 *
	 * @param index Index of the model to be retrieved.
	 * @return The model object that is retrieved.
	 */
	private Model createTestModel(Integer index) {
		testHelper.switchModels(0, index);

		Model testModel = modelFactory.getModel(testHelper.getModel(), testHelper.getStore());

		//model is already in Jena but add to Mongo to generate an ID
		//this is needed by loading/validation progress tests (and others?)
		testModel.save();

		return testModel;
	}

	//Non-Querying Utility Method tests
	/**
	 * Test of createNewAssetURI method Asserts method does not return null
	 * Asserts method returns different string on second attempt
	 */
	@Test
	public void testCreateNewAssetURI() {
		logger.info("Testing createNewAssetURI");

		//Check method returns value
		String firstURI = modelHelper.createNewAssetUri();
		assertNotNull(firstURI);

		//Check method does not return fixed value
		String secondURI = modelHelper.createNewAssetUri();
		assertFalse(firstURI.equals(secondURI));

		int num = 100;
		Set<String> uris = new HashSet<>();
		for (int i=0; i<num; i++) {
			uris.add(modelHelper.createNewAssetUri());
		}
		assertEquals(num, uris.size());
	}

	/**
	 * Test of getModelReasonerClassForModel method Asserts returned class for
	 * known model is as expected (see expReasoner)
	 */
	@Test
	public void testGetModelReasonerClassForModel() {
		logger.info("Testing getModelReasonerClassForModel");
		String expReasoner = "uk.ac.soton.itinnovation.security.domainreasoner.NullReasoner";

		//check correct class name returned
		Model testModel = createTestModel(0);
		assertEquals(expReasoner, modelHelper.getModelReasonerClassForModel(testModel));
	}

	/**
	 * Test of getModelValidatorForModel Asserts returned validator references
	 * correct class
	 */
	@Test
	public void testGetModelValidatorForModel() {
		logger.info("Testing getModelValidatorForModel");
		Model testModel = createTestModel(0);
		ModelValidator validator = modelHelper.getModelValidatorForModel(testModel);
		assertNotNull(validator);
		assertEquals(testModel.getUri(), validator.getModel().getGraphs().get("system"));
	}

	/**
	 * Testing getDomainSpecificReasoner method Asserts returned reasoner is of
	 * the correct class for the given domain model
	 */
	@Test
	public void testGetDomainSpecificReasoner() {
		logger.info("Testing getDomainSpecificReasoner");
		Model testModel = createTestModel(0);

		//Warning: in event of ShieldDomainReasoner creation, this may break.
		assertTrue(modelHelper.getDomainSpecificReasoner(testModel) instanceof uk.ac.soton.itinnovation.security.domainreasoner.NullReasoner);
	}

	/**
	 * Test of registerValidationExecution method Asserts mock task successfully
	 * adds to execution list Asserts delaying task adds successfully to
	 * execution list Asserts new task fails to add while a task is still
	 * executing
	 */
	@Test
	public void testRegisterValidationExecution() {
		logger.info("Testing registerValidationExecution");
		String mockModelId = Integer.toString(new Random().nextInt(100000), 36);
		int sleepTotal = 0;
		
		//Countdown latch ensures thread safety with scheduled threads
		ScheduledFuture<?> delayedTask;
		ScheduledFuture<?> instantTask;

		//CountDownLatch to wait for task to finish. register returns true as no other execution running
		CountDownLatch instantTaskFinished = new CountDownLatch(1);
		instantTask = Executors.newScheduledThreadPool(1).schedule((Runnable) (() -> {
			instantTaskFinished.countDown();
		}), 0, TimeUnit.NANOSECONDS);

		assertTrue(modelHelper.registerValidationExecution(mockModelId, instantTask));

		//Wait for instantTask to finish before attempting next register assertion
		try {
			if (!instantTaskFinished.await(10, TimeUnit.SECONDS)) {
				logger.error("Fail: Timeout waiting for instantTask to finish");
				fail();
			}
			while (!instantTask.isDone() && sleepTotal < MAX_SLEEP) {
				Thread.sleep(25);
				sleepTotal += 25;
			}
		} catch (InterruptedException e) {
			logger.error("Fail: Test was interrupted: {}", e);
			fail();
		}

		//Execution waiting for countdown, cannot finish before countdown called
		CountDownLatch delayedTaskStart = new CountDownLatch(1);
		delayedTask = Executors.newScheduledThreadPool(1).schedule((Runnable) () -> {
			try {
				if (!delayedTaskStart.await(10, TimeUnit.SECONDS)) {
					logger.error("Fail: Timeout waiting for main thread to finish test.");
					fail();
				}
			} catch (InterruptedException e) {
				logger.error("Fail: Test was interrupted: {}", e);
				fail();
			}
		}, 0, TimeUnit.NANOSECONDS);

		//Task registered, still waiting for countdown call, returns true as no others running
		assertTrue(modelHelper.registerValidationExecution(mockModelId, delayedTask));

		//Returns false as task is already registered for this model
		assertFalse(modelHelper.registerValidationExecution(mockModelId, instantTask));

		//Allowing task to finish, all threads concluded after this point
		delayedTaskStart.countDown();
	}

	/**
	 * Test of registerLoadingExecution method Asserts mock task successfully
	 * adds to execution list Asserts delaying task adds successfully to
	 * execution list Asserts new task fails to add while a task is still
	 * executing
	 */
	@Test
	public void testRegisterLoadingExecution() {
		logger.info("Testing registerLoadingExecution");
		String mockModelId = Integer.toString(new Random().nextInt(100000), 36);
		int sleepTotal = 0;
		
		ScheduledFuture<?> delayedTask;
		ScheduledFuture<?> instantTask;
		
			//CountDownLatch to wait for task to finish. returns true as no other execution running.
		CountDownLatch instantTaskFinished = new CountDownLatch(1);
		instantTask = Executors.newScheduledThreadPool(1).schedule((Runnable) () -> {
			instantTaskFinished.countDown();
		}, 0, TimeUnit.NANOSECONDS);

		assertTrue(modelHelper.registerLoadingExecution(mockModelId, instantTask));

		//Wait for futureNow to finish before attempting next register assertion
		try {
			if (!instantTaskFinished.await(10, TimeUnit.SECONDS)) {
				logger.error("Fail: Timeout waiting for instantTask to finish");
				fail();
			}
			while (!instantTask.isDone() && sleepTotal < MAX_SLEEP) {
				Thread.sleep(25);
				sleepTotal += 25;
			}
		} catch (InterruptedException e) {
			logger.error("Fail: Test was interrupted: {}", e);
			fail();
		}

		//Execution waiting for countdown, cannot finish before countdown called
		CountDownLatch delayedTaskStart = new CountDownLatch(1);
		delayedTask = Executors.newScheduledThreadPool(1).schedule((Runnable) () -> {
			try {
				if (!delayedTaskStart.await(10, TimeUnit.SECONDS)) {
					logger.error("Fail: Timeout waiting for main thread to finish test.");
					fail();
				}
			} catch (InterruptedException e) {
				logger.error("Fail: Test was interrupted: {}", e);
				fail();
			}
		}, 0, TimeUnit.NANOSECONDS);

		//Task registered, still waiting for countdown call, returns true as no others running
		assertTrue(modelHelper.registerLoadingExecution(mockModelId, delayedTask));

		//Returns false as task is already registered for this model
		assertFalse(modelHelper.registerLoadingExecution(mockModelId, instantTask));

		//Allowing task to finish, all threads concluded after this point
		delayedTaskStart.countDown();
	}

	/**
	 * Test of getValidationProgressOfModel method Asserts validation progress
	 * is created once and returned on consecutive calls Asserts progress set to
	 * 1.0 once task finishes
	 */
	@Test
	public void testGetValidationProgressOfModel() {
		logger.info("Testing getValidationProgressOfModel");
		Model testModel = createTestModel(0);
		int sleepTotal = 0;
		
		//First get creates new validation progress, test this matches following results
		Progress validationProgress = modelHelper.getValidationProgressOfModel(testModel);
		Progress anotherProgress = modelHelper.getValidationProgressOfModel(testModel);
		assertTrue(validationProgress == anotherProgress);

		//Countdown latch ensures thread safety with scheduled thread
		CountDownLatch taskFinished = new CountDownLatch(1);
		ScheduledFuture<?> markRunningTask;
		
		//task sets validation to running, then concludes (i.e. finished validating)
		markRunningTask = Executors.newScheduledThreadPool(1).schedule((Runnable) () -> {
			validationProgress.setStatus("running");
			taskFinished.countDown();
		}, 0, TimeUnit.NANOSECONDS);

		modelHelper.registerValidationExecution(testModel.getId(), markRunningTask);

		//Wait for validation to be marked running and for thread to complete
		try {
			if (!taskFinished.await(10, TimeUnit.SECONDS)) {
				logger.error("Fail: Timeout waiting for markRunningTask to finish.");
				fail();
			}
			while(!markRunningTask.isDone() && sleepTotal < MAX_SLEEP){
					Thread.sleep(25);
					sleepTotal += 25;
			}
		} catch (InterruptedException e) {
			logger.error("Fail: Test was interrupted: {}", e);
			fail();
		}

		//task finished, should update progress to 1.0
		//Warning, process fails as no value returned (for thread safety reasons)
		logger.warn("Expecting Error for Progress object with unknown error.");
		modelHelper.getValidationProgressOfModel(testModel);
		assertEquals(1.0, validationProgress.getProgress(), 0.1);
	}

	/**
	 * Test of createLoadingProgressOfModel method Asserts loading progress is
	 * created and the model ID is correct
	 */
	@Test
	public void testCreateLoadingProgressOfModel() {
		Model testModel = createTestModel(0);

		String loadingId = UUID.randomUUID().toString();

		LoadingProgress loadingProgress = modelHelper.createLoadingProgressOfModel(testModel, loadingId);
		assertNotNull(loadingProgress);
		assertEquals(testModel.getId(), loadingProgress.getModelId());
	}

	/**
	 * Test of createLoadingProgressOfModel called twice with the same loading ID
	 * Asserts an IllegalArgumentException is thrown
	 */
	@Test
	public void testCreateLoadingProgressOfModelDuplicate() {
		Model testModel = createTestModel(0);

		String loadingId = UUID.randomUUID().toString();

		modelHelper.createLoadingProgressOfModel(testModel, loadingId);

		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> modelHelper.createLoadingProgressOfModel(testModel, loadingId)
			)
			.withMessage(
				"Loading ID " + loadingId + " already exists for model " + testModel.getId()
			);
	}

	/**
	 * Test of getLoadingProgressOfModel method Asserts the same loading progress is
	 * returned as that created (with the same loading ID) Asserts loading
	 * progress set to 1.0 once task finishes
	 */
	@Test
	public void testGetLoadingProgressOfModel() {
		logger.info("Testing getLoadingProgressOfModel");
		Model testModel = createTestModel(0);
		int sleepTotal = 0;
		
		//Create loading progress and assert that get returns it
		String loadingId = UUID.randomUUID().toString();
		LoadingProgress loadingProgress = modelHelper.createLoadingProgressOfModel(testModel, loadingId);
		LoadingProgress anotherProgress = modelHelper.getLoadingProgressOfModel(loadingId);
		assertTrue(loadingProgress == anotherProgress);

		//Countdown latch ensures thread safety with scheduled thread
		CountDownLatch taskFinished = new CountDownLatch(1);
		ScheduledFuture<?> markLoadingTask;

		//task sets execution to loading then concludes (i.e. finished loading)
		markLoadingTask = Executors.newScheduledThreadPool(1).schedule((Runnable) () -> {
			loadingProgress.setStatus("loading");
			taskFinished.countDown();
		}, 0, TimeUnit.NANOSECONDS);

		modelHelper.registerLoadingExecution(loadingId, markLoadingTask);
		//Wait for execution to be marked loading and thread to complete
		try {
			if (!taskFinished.await(10, TimeUnit.SECONDS)) {
				logger.error("Fail: Timout waiting for markLoadingTask to finish.");
				fail();
			}
			while (!markLoadingTask.isDone() && sleepTotal < MAX_SLEEP) {
				Thread.sleep(25);
				sleepTotal += 25;
			}
		} catch (InterruptedException e) {
			logger.error("Fail: Test was interrupted: {}", e);
			fail();
		}

		//task finished, should update progress to 1.0
		//Warning, process fails as no value returned (for thread safety reasons)
		logger.warn("Expecting Error for LoadingProgress object with unknown error.");
		modelHelper.getLoadingProgressOfModel(loadingId);
		assertEquals(1.0, loadingProgress.getProgress(), 0.1);
		
	}

	/**
	 * Test of getLoadingProgressOfModel called with an invalid loading ID
	 * Asserts null is returned
	 */
	@Test
	public void testGetLoadingProgressOfModelInvalidId() {
		assertNull(modelHelper.getLoadingProgressOfModel("notALoadingId"));
	}

	//Get from store
	/**
	 * Test for getAssetsForModel method Asserts each asset matches an expected
	 * asset Asserts number of assets returned is correct
	 */
	@Test
	public void testGetAssetsForModel() {
		logger.info("Testing getAssetsForModel");
		Model testModel = createTestModel(1);
		int expNumAssets = 2;
		String expAssetOneID = "1679f4bc";
		String expAssetTwoID = "3e77c20d";
		Set<Asset> result = modelHelper.getAssetsForModel(testModel);

		//Set collection has no guarantee on order, so logical OR used
		result.forEach(asset -> {
			assertTrue(asset.getID().equals(expAssetOneID)
					|| asset.getID().equals(expAssetTwoID));
		});
		assertEquals(expNumAssets, modelHelper.getAssetsForModel(testModel).size());
	}

	/**
	 * Test of getAssetById method
	 */
	@Test
	public void testGetValidAssetById() {
		logger.info("Testing getAssetById with a valid ID");
		Model testModel = createTestModel(1);
		String testAssetId = "1679f4bc";

		//Throws NullPointerException and fails if asset not found.
		Asset testAsset = modelHelper.getAssetById(testAssetId, testModel, false);
	}

	@Test
	public void testGetInvalidAssetById() throws NullPointerException {
		logger.info("Testing getAssetById with a valid ID");
		Model testModel = createTestModel(1);
		Asset testAsset = modelHelper.getAssetById("InvalidID", testModel, false);
		assertNull(testAsset);
	}

	@Test
	public void testGetRelationForModel() {
		Model testModel = createTestModel(3);
		
		Relation result = modelHelper.getRelationForModel(testModel, "e50a2f3c");
		assertEquals("3e77c20d", result.getFromID());
		assertEquals("1679f4bc", result.getToID());
		assertEquals("hosts", result.getLabel());
	}
	
	@Test
	public void testGetRelationForModelInvalidRelation() {
		Model testModel = createTestModel(3);
		
		Relation result = modelHelper.getRelationForModel(testModel, "invalidRelID");
		assertNull(result);
	}
	
}
