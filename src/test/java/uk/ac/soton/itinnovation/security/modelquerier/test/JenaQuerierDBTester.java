/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2017
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
//      Created By :            Stefanie Cox
//      Created Date :          2017-02-16
//      Created for Project :   5G-ENSURE
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelquerier.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import static java.util.stream.Collectors.filtering;

import java.util.ArrayList;
import java.util.List;

import uk.ac.soton.itinnovation.security.modelquerier.util.TestHelper;

import uk.ac.soton.itinnovation.security.modelquerier.SystemModelQuerier;
import uk.ac.soton.itinnovation.security.modelquerier.SystemModelUpdater;

import org.apache.jena.query.Dataset;
import org.apache.jena.tdb.TDBFactory;

import junit.framework.TestCase;
import uk.ac.soton.itinnovation.security.modelquerier.IQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.JenaQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ControlSetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.MisbehaviourSetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.TrustworthinessAttributeSetDB;

@RunWith(JUnit4.class)
public class JenaQuerierDBTester extends TestCase {
	/* Member variables shared across tests.
	   Note that there is no JenaQuerierDB member variable because they are meant to be
	   instantiated when needed.
	*/
	public static Logger logger = LoggerFactory.getLogger(SystemModelQuerierTester.class);
	private static TestHelper tester;
	private static Dataset dataset;
	private static SystemModelQuerier querier;
	private static SystemModelUpdater updater;
	private long stopwatch;

	@Rule
	public TestName name = new TestName();

	@BeforeClass
	public static void beforeClass() {

		// Create tester class
		tester = new TestHelper("jena-tdb");

		// Add domain models to the test model store for domain models
		tester.addDomain(0, "modelquerier/domain-network-v6a4-1-1-unfiltered.nq.gz", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network");

		// Add test cases to the test model store for system models
		tester.addSystem(0, "modelquerier/JenaQuerierDB-Test-1-CurrentRisks.nq.gz", "http://it-innovation.soton.ac.uk/system/65d4ca5106806a701bf11f2a");
		tester.addSystem(1, "modelquerier/JenaQuerierDB-Test-2-Asserted.nq.gz", "http://it-innovation.soton.ac.uk/system/65d4ca5106806a701bf11f2b");

		// Finish setting up the store
		tester.setUp();

		// Create a dataset object for accessing the Jena API
		dataset = TDBFactory.createDataset("jena-tdb");

		// Get alternative queriers to provide an independent way to check the triple store
		querier = new SystemModelQuerier(tester.getModel());
		updater = new SystemModelUpdater(tester.getModel());

		logger.info("SystemModelQuerier tests executing...");
	}

	@Before
	public void beforeEachTest() {

		logger.info("Running test {}", name.getMethodName());
		stopwatch = System.currentTimeMillis();
	}

	@After
	public void afterEachTest() {

		logger.debug("Test {} took {} milliseconds", name.getMethodName(), System.currentTimeMillis() - stopwatch);

		//comment in to better debug the test models
//		logger.info("Exporting test model");
//		tester.exportTestModel("build/build/test-results/" + name.getMethodName(), true, true, false);
	}

	// Helper methods /////////////////////////////////////////////////////////////////////////////////////////////////
	
	// Tests //////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Test
	public void testReadDataCacheEnabled() {
		// Select the pre-validated test model
		tester.switchModels(0, 0);

		// Create and initialise a JenaQuerierDB object with caching enabled
		IQuerierDB querierDB = new JenaQuerierDB(dataset, tester.getModel(), true);
		querierDB.init();

		// Get and check some model data
		ControlSetDB cs;
		MisbehaviourSetDB ms;
		TrustworthinessAttributeSetDB twas;

		// Read some asserted control set properties and check they are set correctly
		// Should be set in the asserted graph
		cs = querierDB.getControlSet("system#CS-ImpersonalData-e5440a91","system");
		logger.warn("Control set {} has proposed status {} in the asserted graph", cs.getUri(), cs.isProposed());
		assertTrue(cs.isProposed());
		// Should be copied in the inferred graph
		cs = querierDB.getControlSet("system#CS-ImpersonalData-e5440a91","system-inf");
		logger.warn("Control set {} has proposed status {} in the inferred graph", cs.getUri(), cs.isProposed());
		assertTrue(cs.isProposed());
		// Should be set in the merged graph
		cs = querierDB.getControlSet("system#CS-ImpersonalData-e5440a91","system","system-inf");
		logger.warn("Control set {} has proposed status {} in the combined graph", cs.getUri(), cs.isProposed());
		assertTrue(cs.isProposed());

		// Read some asserted misbehaviour set properties and check they are set correctly
		// Should be set in the asserted graph
		ms = querierDB.getMisbehaviourSet("system#MS-LossOfAuthenticity-e5440a91","system");
		logger.warn("Misbehaviour set {} has impact level {} in the asserted graph", ms.getUri(), ms.getImpactLevel());
		assertEquals("domain#ImpactLevelHigh", ms.getImpactLevel());
		// Should be reset to the default level in the inferred graph
		ms = querierDB.getMisbehaviourSet("system#MS-LossOfAuthenticity-e5440a91","system-inf");
		logger.warn("Misbehaviour set {} has impact level {} in the inferred graph", ms.getUri(), ms.getImpactLevel());
		assertEquals("domain#ImpactLevelNegligible", ms.getImpactLevel());
		// Should be set in the combined graph
		ms = querierDB.getMisbehaviourSet("system#MS-LossOfAuthenticity-e5440a91","system","system-inf");
		logger.warn("Misbehaviour set {} has impact level {} in the combined graph", ms.getUri(), ms.getImpactLevel());
		assertEquals("domain#ImpactLevelHigh", ms.getImpactLevel());

		// Read some  control set properties that are not asserted and check they are set correctly
		cs = querierDB.getControlSet("system#CS-AccessPolicy-e5440a91","system");
		if(cs != null){
			logger.warn("Control set {} has proposed status {} in the asserted graph", cs.getUri(), cs.isProposed());
			assertFalse(cs.isProposed());
		} else {
			logger.warn("Control set {} does not exist in the asserted graph", "system#CS-AccessPolicy-e5440a91");
			assertEquals(null, cs);
		}
		cs = querierDB.getControlSet("system#CS-AccessPolicy-e5440a91","system-inf");
		logger.warn("Control set {} has proposed status {} in the inferred graph", cs.getUri(), cs.isProposed());
		assertFalse(cs.isProposed());
		cs = querierDB.getControlSet("system#CS-AccessPolicy-e5440a91","system","system-inf");
		logger.warn("Control set {} has proposed status {} in the combined graph", cs.getUri(), cs.isProposed());
		assertFalse(cs.isProposed());

		// Read some misbehaviour set properties that are not asserted and check they are set correctly
		ms = querierDB.getMisbehaviourSet("system#MS-LossOfAuthenticity-1579e6d8","system");
		if(ms != null){
			logger.warn("Misbehaviour set {} has impact level {} in the asserted graph", ms.getUri(), ms.getImpactLevel());
			assertEquals("domain#ImpactLevelNegligible", ms.getImpactLevel());
		} else {
			logger.warn("Misbehaviour set {} does not exist in the asserted graph", "system#MS-LossOfAuthenticity-1579e6d8");
			assertEquals(null, ms);
		}
		ms = querierDB.getMisbehaviourSet("system#MS-LossOfAuthenticity-1579e6d8","system-inf");
		logger.warn("Misbehaviour set {} has impact level {} in the inferred graph", ms.getUri(), ms.getImpactLevel());
		assertEquals("domain#ImpactLevelNegligible", ms.getImpactLevel());
		ms = querierDB.getMisbehaviourSet("system#MS-LossOfAuthenticity-1579e6d8","system","system-inf");
		logger.warn("Misbehaviour set {} has impact level {} in the combined graph", ms.getUri(), ms.getImpactLevel());
		assertEquals("domain#ImpactLevelNegligible", ms.getImpactLevel());

	}

	@Test
	public void testReadDataCacheDisabled() {
		// Select the pre-validated test model
		tester.switchModels(0, 0);

		// Create and initialise a JenaQuerierDB object with caching enabled
		IQuerierDB querierDB = new JenaQuerierDB(dataset, tester.getModel(), false);
		querierDB.init();

		// Get and check some model data
		ControlSetDB cs;
		MisbehaviourSetDB ms;
		TrustworthinessAttributeSetDB twas;

		// Read some asserted control set properties and check they are set correctly
		// Should be set in the asserted graph
		cs = querierDB.getControlSet("system#CS-ImpersonalData-e5440a91","system");
		logger.warn("Control set {} has proposed status {} in the asserted graph", cs.getUri(), cs.isProposed());
		assertTrue(cs.isProposed());
		// Should be copied in the inferred graph
		cs = querierDB.getControlSet("system#CS-ImpersonalData-e5440a91","system-inf");
		logger.warn("Control set {} has proposed status {} in the inferred graph", cs.getUri(), cs.isProposed());
		assertTrue(cs.isProposed());
		// Should be set in the merged graph
		cs = querierDB.getControlSet("system#CS-ImpersonalData-e5440a91","system","system-inf");
		logger.warn("Control set {} has proposed status {} in the combined graph", cs.getUri(), cs.isProposed());
		assertTrue(cs.isProposed());

		// Read some asserted misbehaviour set properties and check they are set correctly
		// Should be set in the asserted graph
		ms = querierDB.getMisbehaviourSet("system#MS-LossOfAuthenticity-e5440a91","system");
		logger.warn("Misbehaviour set {} has impact level {} in the asserted graph", ms.getUri(), ms.getImpactLevel());
		assertEquals("domain#ImpactLevelHigh", ms.getImpactLevel());
		// Should be reset to the default level in the inferred graph
		ms = querierDB.getMisbehaviourSet("system#MS-LossOfAuthenticity-e5440a91","system-inf");
		logger.warn("Misbehaviour set {} has impact level {} in the inferred graph", ms.getUri(), ms.getImpactLevel());
		assertEquals("domain#ImpactLevelNegligible", ms.getImpactLevel());
		// Should be set in the combined graph
		ms = querierDB.getMisbehaviourSet("system#MS-LossOfAuthenticity-e5440a91","system","system-inf");
		logger.warn("Misbehaviour set {} has impact level {} in the combined graph", ms.getUri(), ms.getImpactLevel());
		assertEquals("domain#ImpactLevelHigh", ms.getImpactLevel());

		// Read some  control set properties that are not asserted and check they are set correctly
		cs = querierDB.getControlSet("system#CS-AccessPolicy-e5440a91","system");
		if(cs != null){
			logger.warn("Control set {} has proposed status {} in the asserted graph", cs.getUri(), cs.isProposed());
			assertFalse(cs.isProposed());
		} else {
			logger.warn("Control set {} does not exist in the asserted graph", "system#CS-AccessPolicy-e5440a91");
			assertEquals(null, cs);
		}
		cs = querierDB.getControlSet("system#CS-AccessPolicy-e5440a91","system-inf");
		logger.warn("Control set {} has proposed status {} in the inferred graph", cs.getUri(), cs.isProposed());
		assertFalse(cs.isProposed());
		cs = querierDB.getControlSet("system#CS-AccessPolicy-e5440a91","system","system-inf");
		logger.warn("Control set {} has proposed status {} in the combined graph", cs.getUri(), cs.isProposed());
		assertFalse(cs.isProposed());

		// Read some misbehaviour set properties that are not asserted and check they are set correctly
		ms = querierDB.getMisbehaviourSet("system#MS-LossOfAuthenticity-1579e6d8","system");
		if(ms != null){
			logger.warn("Misbehaviour set {} has impact level {} in the asserted graph", ms.getUri(), ms.getImpactLevel());
			assertEquals("domain#ImpactLevelNegligible", ms.getImpactLevel());
		} else {
			logger.warn("Misbehaviour set {} does not exist in the asserted graph", "system#MS-LossOfAuthenticity-1579e6d8");
			assertEquals(null, ms);
		}
		ms = querierDB.getMisbehaviourSet("system#MS-LossOfAuthenticity-1579e6d8","system-inf");
		logger.warn("Misbehaviour set {} has impact level {} in the inferred graph", ms.getUri(), ms.getImpactLevel());
		assertEquals("domain#ImpactLevelNegligible", ms.getImpactLevel());
		ms = querierDB.getMisbehaviourSet("system#MS-LossOfAuthenticity-1579e6d8","system","system-inf");
		logger.warn("Misbehaviour set {} has impact level {} in the combined graph", ms.getUri(), ms.getImpactLevel());
		assertEquals("domain#ImpactLevelNegligible", ms.getImpactLevel());
	}

}
