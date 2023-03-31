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

package uk.ac.soton.itinnovation.security.modelvalidator.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.jena.query.Dataset;
import org.apache.jena.tdb.TDBFactory;
import org.junit.*;
import org.junit.rules.ErrorCollector;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.model.Level;
import uk.ac.soton.itinnovation.security.model.system.Asset;
import uk.ac.soton.itinnovation.security.model.system.ControlSet;
import uk.ac.soton.itinnovation.security.model.system.MisbehaviourSet;
import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;
import uk.ac.soton.itinnovation.security.model.system.Threat;
import uk.ac.soton.itinnovation.security.model.system.TrustworthinessAttributeSet;
import uk.ac.soton.itinnovation.security.modelquerier.IQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.JenaQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.SystemModelQuerier;
import uk.ac.soton.itinnovation.security.modelquerier.SystemModelUpdater;
import uk.ac.soton.itinnovation.security.modelquerier.dto.AssetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.EntityDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.MisbehaviourDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.MisbehaviourSetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.RiskCalcResultsDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ThreatDB;
import uk.ac.soton.itinnovation.security.modelquerier.util.TestHelper;
import uk.ac.soton.itinnovation.security.modelvalidator.Progress;
import uk.ac.soton.itinnovation.security.modelvalidator.RiskCalculator;
import uk.ac.soton.itinnovation.security.modelvalidator.Validator;

@RunWith(JUnit4.class)
public class RiskLevelCalculatorTester extends TestCase {

    public static Logger logger = LoggerFactory.getLogger(RiskLevelCalculatorTester.class);

	private static TestHelper tester;
	private static Dataset dataset;
	private long stopwatch;

	private static SystemModelQuerier smq;
	private static SystemModelUpdater smu;

	private static Map<String, Level> inverseLikelihoods = new HashMap<>();

	@Rule public TestName name = new TestName();

	@Rule public ErrorCollector collector = new ErrorCollector();

	@BeforeClass
    public static void beforeClass() {

		tester = new TestHelper("jena-tdb");

		tester.addDomain(0, "modelvalidator/domain-network.rdf.gz", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network");
		tester.addDomain(1, "modelvalidator/domain-shield.rdf.gz", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-shield");
		tester.addDomain(2, "modelvalidator/FOGPROTECT-3j1-5.nq.gz", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-fogprotect");
		tester.addDomain(3, "modelvalidator/domain-shield-with-frequency.rdf.gz", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-shield-with-frequency");
		tester.addDomain(4, "modelvalidator/TESTING-2a6-1.nq.gz", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network-testing");
		tester.addDomain(5, "modelvalidator/domain-network_8.0.0-SNAPSHOT-NETWORK-3j1-5.nq.gz","http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network-8_0_0");

		//Test domain model for population support
		tester.addDomain(6, "modelvalidator/domain-ssm-testing-6a3.nq", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/ssm-testing-6a3");
		tester.addDomain(7, "modelvalidator/ssm-testing-6a3-0-16-auto-expanded.nq", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/ssm-testing-6a3-expanded");

		//unvalidated system model for testing risk calculator
		tester.addSystem(0, "modelvalidator/system-network.nq.gz",
			"http://it-innovation.soton.ac.uk/system/5ad09178567d94846a9aeaec");

		//validated system model for testing risk calculator
		tester.addSystem(1, "modelvalidator/system-shield-valid.nq.gz",
			"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3dda");

		//system model for testing attack paths
		tester.addSystem(2, "modelvalidator/system-attack-path-simple-test.nq.gz",
			"http://it-innovation.soton.ac.uk/system/5c6687d4567d94019ca7e022");

		//system model for testing 'current' vs 'future' risk calculation
		tester.addSystem(3, "modelvalidator/Current_Risk_Test_Case.nq.gz",
			"http://it-innovation.soton.ac.uk/system/5eb3b4c33fd34e33c59df896");

		//system model for testing intrinsic threat likelihoods
		tester.addSystem(4, "modelvalidator/system-shield-validated-with-frequencies.nq.gz",
			"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3dda",
			"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3ddb");

		//validated simple test model
		tester.addSystem(5, "modelvalidator/PatternMatchingAndControlStrategyTest-v2a-valid.nq.gz",
			"http://it-innovation.soton.ac.uk/system/5d99e263884bd32188c3282b");

		//test model to test threat causation
		tester.addSystem(6, "modelvalidator/ThreatCausationTest_-_v2a6_-_valid.nq.gz",
			"http://it-innovation.soton.ac.uk/system/5ea812ae3fd34e04ce51ea06");

		tester.addSystem(7, "modelvalidator/RC-rerun-test-V.nq.gz",
				"http://it-innovation.soton.ac.uk/system/5f241c02992e8308e4a9aeb4");

		//Test system model for population support
		tester.addSystem(8, "modelvalidator/Test-6a3-00.nq.gz",
				"http://it-innovation.soton.ac.uk/system/63971077df89a647814e6d8b");

		tester.addSystem(9, "modelvalidator/Test-6a3-1ANB-HighSatC-asserted.nq",
				"http://it-innovation.soton.ac.uk/system/63b2f38af03b473a0ce2a3b9");

		tester.setUp();

		tester.switchModels(0, 0);

		smq = new SystemModelQuerier(tester.getModel());
		smu = new SystemModelUpdater(tester.getModel());

		// Set up invert likelihood operation.
		Collection<Level> likelihoods = smq.getLevels(tester.getStore(), "Likelihood").values();
		List<Level> sortedAsc = new ArrayList<>();
		sortedAsc.addAll(likelihoods);
		Collections.sort(sortedAsc);
		List<Level> sortedDesc = new ArrayList<>();
		sortedDesc.addAll(likelihoods);
		Collections.sort(sortedDesc, Collections.reverseOrder());
		for (int i = 0; i<likelihoods.size(); i++) {
			inverseLikelihoods.put(sortedAsc.get(i).getUri(), sortedDesc.get(i));
		}

		dataset = TDBFactory.createDataset("jena-tdb");

		logger.info("RiskLevelCalculator tests executing...");
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
		logger.debug("Exporting test model");
		tester.exportTestModel("build/build/test-results/" + name.getMethodName(), true, false, true);
	}

	// "Random" data generation methods for the trustworthiness calculations //////////////////////////////////////////

	/**
	 * Set the trustworthiness level of TWAS
	 *
	 * @param level the new level, only using the actual value, i.e. VeryLow, High,...
	 * @param number the number of triples to be modified
	 */
	private void setTrustworthinessLevel(String level, int number) {
		String sparql = "DELETE {\n" +
		"	GRAPH <" + tester.getGraph("system") + "> { ?twas core:hasAssertedLevel ?al }\n" +
		"	GRAPH <" + tester.getGraph("system-inf") + "> { ?twas core:hasInferredLevel ?il }\n" +
		"} INSERT {\n" +
		"	GRAPH <" + tester.getGraph("system") + "> { ?twas core:hasAssertedLevel domain:TrustworthinessLevel" + level + " }\n" +
		"} WHERE {\n" +
		"	SELECT DISTINCT ?twas ?al ?il WHERE {\n" +
		"		?twas a core:TrustworthinessAttributeSet .\n" +
		"		?twas core:parent ?twasp .\n" +
		"		OPTIONAL { ?twas core:hasAssertedLevel ?al }\n" +
		"		OPTIONAL { ?twas core:hasInferredLevel ?il }\n" +
		"	} ORDER BY RAND() LIMIT " + number + "\n" +
		"}";
		tester.getStore().update(sparql, tester.getModel().getGraph("system"), tester.getModel().getGraph("system-inf"));
	}

	/**
	 * Set the blocking effect of control strategies
	 *
	 * @param level the new level, only using the actual value, i.e. VeryLow, High,...
	 * @param number the number of triples to be modified
	 */
	private void setBlockingEffect(String level, int number) {
		String sparql = "DELETE {\n" +
		"	?csg core:hasBlockingEffect ?be .\n" +
		"} INSERT {\n" +
		"	?csg core:hasBlockingEffect domain:BlockingEffect" + level + " .\n" +
		"} WHERE {\n" +
		"	SELECT * WHERE {\n" +
		"		?csg a core:ControlStrategy .\n" +
		"		?csg core:hasBlockingEffect ?be .\n" +
		"	} ORDER BY RAND() LIMIT " + number + "\n" +
		"}";
		tester.getStore().update(sparql, tester.getModel().getGraph("domain"));
	}

	/**
	 * Set the impact level of misbehaviour sets
	 *
	 * @param level the new level, only using the actual value, i.e. VeryLow, High,...
	 * @param number the number of triples to be modified
	 */
	private void setImpactLevel(String level, int number) {
		String sparql = "DELETE {\n" +
		"	GRAPH <" + tester.getGraph("system") + "> { ?ms core:hasImpactLevel ?l }\n" +
		"	GRAPH <" + tester.getGraph("system-inf") + "> { ?ms core:hasImpactLevel ?l }\n" +
		"} INSERT {\n" +
		"	GRAPH <" + tester.getGraph("system") + "> { ?ms core:hasImpactLevel domain:ImpactLevel" + level + " }\n" +
		"} WHERE {\n" +
		"	SELECT * WHERE {\n" +
		"		?ms a core:MisbehaviourSet .\n" +
		"		OPTIONAL { ?ms core:hasImpactLevel ?l }\n" +
		"	} ORDER BY RAND() LIMIT " + number + "\n" +
		"}";
		tester.getStore().update(sparql, tester.getGraph("system"), tester.getGraph("system-inf"));
	}

	// Tests //////////////////////////////////////////////////////////////////////////////////////////////////////////

	// Doesn't assert anything, so therefore crap.
	// However does test whether calculateRiskLevels() crashes on a model with no threats (it used to!).
	@Test
	public void testCalculateRiskLevelsUnvalidatedModel() {
		tester.switchModels(0, 0);

		try {
			IQuerierDB querierDB = new JenaQuerierDB(dataset, tester.getModel(), true);
			querierDB.initForRiskCalculation();
			RiskCalculator rc = new RiskCalculator(querierDB);
			rc.calculateRiskLevels(RiskCalculationMode.FUTURE, false, new Progress(tester.getGraph("system")));
		} catch (Exception e) {
			logger.error("Exception thrown by risk level calculator", e);
			fail("Exception thrown by risk level calculator");
		}
	}

	// Doesn't assert anything, so therefore crap.
	// However does test whether calculateRiskLevels() crashes on real data.
	@Test
	public void testCalculateRiskLevelsRandom() {
		tester.switchModels(6, 8); //use population domain and system model

		//make data a bit more interesting:
		//	- CS
		logger.debug("Propose some control sets");
		String sparql = "DELETE {\n" +
		"	GRAPH <" + tester.getGraph("system") + "> { ?cs core:isProposed ?prop }\n" +
		"	GRAPH <" + tester.getGraph("system-inf") + "> { ?cs core:isProposed ?prop }\n" +
		"} INSERT {\n" +
		"	GRAPH <" + tester.getGraph("system") + "> { ?cs core:isProposed true }\n" +
		"} WHERE {\n" +
		"	SELECT * WHERE {\n" +
		"		?cs a core:ControlSet .\n" +
		"		OPTIONAL { ?cs core:isProposed ?prop }\n" +
		"		?cs core:hasID ?id .\n" +
		"		?cs core:locatedAt ?a .\n" +
		"		FILTER NOT EXISTS { ?a core:createdByPattern ?p }\n" +
		"	} ORDER BY DESC(?id) LIMIT 50\n" +
		"}";
		tester.getStore().update(sparql, tester.getGraph("system"), tester.getGraph("system-inf"));

		//	- TWAS
		logger.debug("Assign some random trustworthiness levels to TWAS");
		setTrustworthinessLevel("VeryLow", 5);
		setTrustworthinessLevel("Low", 5);
		setTrustworthinessLevel("High", 5);
		setTrustworthinessLevel("VeryHigh", 5);

		//	- CSG
		logger.debug("Change the domain blocking effects");
		setBlockingEffect("Low", 5);
		setBlockingEffect("High", 5);
		setBlockingEffect("VeryHigh", 5);

		//	- MS
		logger.debug("Change the impact level of misbehaviour sets");
		setImpactLevel("VeryLow", 10);
		setImpactLevel("Low", 10);
		setImpactLevel("High", 10);
		setImpactLevel("VeryHigh", 10);

		try {
			IQuerierDB querierDB = new JenaQuerierDB(dataset, tester.getModel(), true);
			querierDB.initForRiskCalculation();
			RiskCalculator rc = new RiskCalculator(querierDB);
			rc.calculateRiskLevels(RiskCalculationMode.FUTURE, false, new Progress(tester.getGraph("system")));
		} catch (Exception e) {
			logger.error("Exception thrown by risk level calculator", e);
			fail("Exception thrown by risk level calculator");
		}
	}

	/*
	 * Tests that the threat frequency functionality is working correctly. This uses a full domain and system model,
	 * both of which should be replaced by simple test models in the future.
	 */
	@Ignore("Fails due to bad input. See https://iglab.it-innovation.soton.ac.uk/Security/system-modeller/-/merge_requests/655#note_28417")
	@Test
	public void testThreatFrequency() {
		tester.switchModels(3, 4);

		try {
			IQuerierDB querierDB = new JenaQuerierDB(dataset, tester.getModel(), true);
			querierDB.initForRiskCalculation();
			RiskCalculator rc = new RiskCalculator(querierDB);
			rc.calculateRiskLevels(RiskCalculationMode.FUTURE, true, new Progress(tester.getGraph("system"))); //save results, as queried below
		} catch (Exception e) {
			logger.error("Exception thrown by risk level calculator", e);
			fail("Exception thrown by risk level calculator");
		}

		Map<String, Threat> threats = smq.getSystemThreats(tester.getStore());
		for (Threat threat : threats.values()) {
			if (threat.getFrequency() != null &&
					threat.getLikelihood().getValue() > threat.getFrequency().getValue()) {
				fail("Found threat with likelihood greater than its frequency");
			}
		}

		// If the frequency functionality is  working then the likelihood of this threat will be VeryLow, otherwise it
		// will be Low
		Threat threat = threats.get("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#H.A.H.3-H_a9feda22");
		assertEquals(threat.getLikelihood().getValue(), 0);
	}

	@Test
	public void testAttackPaths() {
		logger.info("Switching to selected domain and system model test cases");
		tester.switchModels(7, 9);

		logger.info("Creating a querierDB object");
		IQuerierDB querierDB = new JenaQuerierDB(dataset, tester.getModel(), true);
		querierDB.initForValidation();
		try {
			logger.info("Validating the model - ensures no dependence on bugs in older SSM validators");
			Validator validator = new Validator(querierDB);
			validator.validate(new Progress(tester.getGraph("system")));
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception thrown by validator preparing attack path test case");
			return;
		}

		try {
			logger.info("Calculating risks and generating attack graph");
			RiskCalculator rc = new RiskCalculator(querierDB);
			rc.calculateRiskLevels(RiskCalculationMode.FUTURE, true, new Progress(tester.getGraph("system"))); //save results, as queried below
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception thrown by risk level calculator");
			return;
		}

		// Get the high likelihood threat caused directly by the least secure [C:A-B]
		ThreatDB tmax = querierDB.getThreat("system#A.Se_Max.AtsAt.1-MP-AtsAt_b64ab6ce", "system-inf");
		if(tmax == null) {
			fail("Unable to find highest likelihood threat 'system#A.Se_Max.AtsAt.1-MP-AtsAt_b64ab6ce'");
		} else {
			// Should be a root cause threat of Loss of Performance at A, but not Loss of Performance at B
			assertTrue(tmax.isRootCause());
			assertTrue(tmax.getIndirectMisbehaviours().contains("system#MS-LossOfPerformance-8e8ae2ce"));
			assertFalse(tmax.getIndirectMisbehaviours().contains("system#MS-LossOfPerformance-b64ab6ce"));
		}

		// Get the average likelihood threat caused directly by the average [C:A-B]
		ThreatDB tavg = querierDB.getThreat("system#A.Se.AtsAt.1-MP-AtsAt_b64ab6ce", "system-inf");
		if(tavg == null) {
			fail("Unable to find average likelihood threat 'system#A.Se.AtsAt.1-MP-AtsAt_b64ab6ce'");
		} else {
			// Should be a root cause threat of Loss of Performance at B, but not Loss of Performance at A
			assertTrue(tavg.isRootCause());
			assertFalse(tavg.getIndirectMisbehaviours().contains("system#MS-LossOfPerformance-8e8ae2ce"));
			assertTrue(tavg.getIndirectMisbehaviours().contains("system#MS-LossOfPerformance-b64ab6ce"));
		}

		// Get the low likelihood threat caused directly by the most secure [C:A-B]
		ThreatDB tmin = querierDB.getThreat("system#A.Se_Min.AtsAt.1-MP-AtsAt_b64ab6ce", "system-inf");
		if(tmin == null) {
			fail("Unable to find lowest likelihood threat 'system#A.Se_Min.AtsAt.1-MP-AtsAt_b64ab6ce'");
		} else {
			// Should not be a root cause threat
			assertFalse(tmin.isRootCause());
			assertFalse(tmin.getIndirectMisbehaviours().contains("system#MS-LossOfPerformance-8e8ae2ce"));
			assertFalse(tmin.getIndirectMisbehaviours().contains("system#MS-LossOfPerformance-b64ab6ce"));
		}

	}

	//TODO: fix or delete this test
	@Ignore("This test fails for refactored validator but we don't yet know why. Testing the two risk calculations separately works fine (see below).")
	@Test
	public void testCurrentOrFutureRiskCalculation() {
		tester.switchModels(2, 3);

		try {
			IQuerierDB querierDB = new JenaQuerierDB(dataset, tester.getModel(), true);
			querierDB.initForRiskCalculation();
			RiskCalculator rc = new RiskCalculator(querierDB);
			rc.calculateRiskLevels(RiskCalculationMode.CURRENT, true, new Progress(tester.getGraph("system"))); //save results, as queried below

			MisbehaviourSet ms = smq.getMisbehaviourSet(tester.getStore(),
					"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#MS-LossOfAvailability-d7369b42",
					false); //no need for causes here
			assertEquals(2, ms.getLikelihood().getValue());

			rc.calculateRiskLevels(RiskCalculationMode.FUTURE, true, new Progress(tester.getGraph("system"))); //save results, as queried below
			ms = smq.getMisbehaviourSet(tester.getStore(),
					"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#MS-LossOfAvailability-d7369b42",
					false); //no need for causes here
			assertEquals(1, ms.getLikelihood().getValue());
		} catch (Exception e) {
			logger.error("Exception thrown by risk level calculator", e);
			fail("Exception thrown by risk level calculator");
		}
	}

	@Test
	public void testCurrentRiskCalculation() {
		tester.switchModels(2, 3);

		try {
			IQuerierDB querierDB = new JenaQuerierDB(dataset, tester.getModel(), true);
			querierDB.initForRiskCalculation();
			RiskCalculator rc = new RiskCalculator(querierDB);
			rc.calculateRiskLevels(RiskCalculationMode.CURRENT, true, new Progress(tester.getGraph("system"))); //save results, as queried below

			MisbehaviourSet ms = smq.getMisbehaviourSet(tester.getStore(),
					"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#MS-LossOfAvailability-d7369b42",
					false); //no need for causes here
			assertEquals(2, ms.getLikelihood().getValue());
		} catch (Exception e) {
			logger.error("Exception thrown by risk level calculator", e);
			fail("Exception thrown by risk level calculator");
		}
	}

	@Test
	public void testFutureRiskCalculation() {
		tester.switchModels(2, 3);

		try {
			IQuerierDB querierDB = new JenaQuerierDB(dataset, tester.getModel(), true);
			querierDB.initForRiskCalculation();
			RiskCalculator rc = new RiskCalculator(querierDB);
			rc.calculateRiskLevels(RiskCalculationMode.FUTURE, false, new Progress(tester.getGraph("system"))); //don't save results

			RiskCalcResultsDB results = rc.getRiskCalcResults();
			Map<String, MisbehaviourSetDB> misbehaviourSets = results.getMisbehaviourSets();
			logger.debug("returning {} misbehaviourSets", misbehaviourSets.size());
			for (String msKey : misbehaviourSets.keySet()) {
				MisbehaviourSetDB ms = misbehaviourSets.get(msKey);
				logger.debug("{}: {}", msKey, ms);
			}

			MisbehaviourSet ms = smq.getMisbehaviourSet(tester.getStore(),
					"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#MS-LossOfAvailability-d7369b42",
					false); //no need for causes here
			assertEquals(1, ms.getLikelihood().getValue());
		} catch (Exception e) {
			logger.error("Exception thrown by risk level calculator", e);
			fail("Exception thrown by risk level calculator");
		}
	}

	/**
	 * This is a placeholder test method for risk calculation of the test model. It is in a poor state at the minute, only
	 * testing the lack of exceptions (and any cases which we add to it as and when they are needed). In the future,
	 * we hope to move to a much more robust set of tests for the risk calculator.
	 *
	 * Domain Model: domain-network-testing (TESTING-2a6-1)
	 * System Model: PatternMatchingAndControlStrategyTest-v2a-valid
	 */
	@Test
	public void testRiskCalculationTestModel() {
		tester.switchModels(4, 5);

		try {
			IQuerierDB querierDB = new JenaQuerierDB(dataset, tester.getModel(), true);
			querierDB.initForRiskCalculation();
			RiskCalculator rc = new RiskCalculator(querierDB);
			rc.calculateRiskLevels(RiskCalculationMode.FUTURE, true, new Progress(tester.getGraph("system")));
		} catch (Exception e) {
			logger.error("Exception thrown by risk level calculator", e);
			fail("Exception thrown by risk level calculator");
		}

		// Check that risk level are calculated correctly for threats with multiple entry points with different likelihoods.
		Map<String, Threat> threats = smq.getSystemThreats(tester.getStore());
		// Test   A.M.A1mB.1: A1->(B1, C1)   likelihood is Low
		Assert.assertEquals(1, threats.get("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#A.M.A1mB.1-A1mB_ef2fee92")
				.getLikelihood().getValue());
		// Test   A.M.A1mB.1: A3->(C3,C4)   likelihood is Low
		Assert.assertEquals(1, threats.get("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#A.M.A1mB.1-A1mB_95c9e105")
				.getLikelihood().getValue());
	}

    /**
     * Tests a variety of threat causation related cases in the simple system model to test risk calculation.
     *
     * Domain Model: domain-network-testing (TESTING-2a6-1)
     * System Model: ThreatCausationTest_-_v2a6_-_valid.nq.gz
     */
	@Test
	public void testThreatCausationModel() {
		tester.switchModels(4, 6);

		try {
			IQuerierDB querierDB = new JenaQuerierDB(dataset, tester.getModel(), true);
			querierDB.initForRiskCalculation();
			RiskCalculator rc = new RiskCalculator(querierDB);
			rc.calculateRiskLevels(RiskCalculationMode.FUTURE, true, new Progress(tester.getGraph("system")));
		} catch (Exception e) {
			logger.error("Exception thrown by risk level calculator", e);
			fail("Exception thrown by risk level calculator");
		}

		// Assert threat A.M.A1mB.1 with pattern A2-(B1,B2,C1,C2) likelihood Medium
        assertEquals(2, smq.getSystemThreat(tester.getStore(),
                "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#A.M.A1mB.1-A1mB_968dfa7c").getLikelihood().getValue());
        // Assert threat A.M.A1mB.1 with pattern A3-(C3) likelihood Low
        assertEquals(1, smq.getSystemThreat(tester.getStore(),
                "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#A.M.A1mB.1-A1mB_f23096dc").getLikelihood().getValue());
        // Assert threat A.M.A1mB.1 with pattern A4-(C4) likelihood Medium
        assertEquals(2, smq.getSystemThreat(tester.getStore(),
                "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#A.M.A1mB.1-A1mB_eae0ed8f").getLikelihood().getValue());

        // Assert threat A.M.A6mD.1 with pattern A1-(D1,D2,D3) likelihood Medium
        assertEquals(1, smq.getSystemThreat(tester.getStore(),
                "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#A.M.A6mD.1-A6mD_856b9ac6").getLikelihood().getValue());
        // Assert threat A.M.A6mD.1 with pattern A2-(D1) likelihood Medium
        assertEquals(2, smq.getSystemThreat(tester.getStore(),
                "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#A.M.A6mD.1-A6mD_968dfa7c").getLikelihood().getValue());
        // Assert threat A.M.A6mD.1 with pattern A3-(D3) likelihood Medium
        assertEquals(1, smq.getSystemThreat(tester.getStore(),
                "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#A.M.A6mD.1-A6mD_f23096dc").getLikelihood().getValue());
        // Assert threat A.M.A6mD.1 with pattern A4-(D2) likelihood Medium
        assertEquals(2, smq.getSystemThreat(tester.getStore(),
                "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#A.M.A6mD.1-A6mD_eae0ed8f").getLikelihood().getValue());

        // Assert threat A.M.A6oD.1 with pattern A1-(D1,D2,D3) likelihood Medium
        assertEquals(1, smq.getSystemThreat(tester.getStore(),
                "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#A.M.A6oD.1-A6oD_856b9ac6").getLikelihood().getValue());
        // Assert threat A.M.A6mD.1 with pattern A2-(D1) likelihood Medium
        assertEquals(2, smq.getSystemThreat(tester.getStore(),
                "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#A.M.A6oD.1-A6oD_968dfa7c").getLikelihood().getValue());
        // Assert threat A.M.A6mD.1 with pattern A3-(D3) likelihood Medium
        assertEquals(1, smq.getSystemThreat(tester.getStore(),
                "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#A.M.A6oD.1-A6oD_f23096dc").getLikelihood().getValue());
        // Assert threat A.M.A6mD.1 with pattern A4-(D2) likelihood Medium
        assertEquals(2, smq.getSystemThreat(tester.getStore(),
                "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#A.M.A6oD.1-A6oD_eae0ed8f").getLikelihood().getValue());
	}

	/**
	 * Test re-running of risk calculation. RC is run with initial values, then again with all control set 'isProposed'
	 * values inverted and all TWAS set to Low, then one final time with all control set 'isProposed' values and TWAS
	 * re-set to their initial values. Key properties are tested to check that they are equal after the initial
	 * risk calculation and the final risk calculation.
	 */
	@Test
	public void testRerunRiskCalculation() {
		tester.switchModels(5, 7);

		try {
			IQuerierDB querierDB = new JenaQuerierDB(dataset, tester.getModel(), true);
			querierDB.initForRiskCalculation();
			RiskCalculator rc = new RiskCalculator(querierDB);
			rc.calculateRiskLevels(RiskCalculationMode.FUTURE, true, new Progress(tester.getGraph("system")));
		} catch (Exception e) {
			logger.error("Exception thrown by risk level calculator", e);
			fail("Exception thrown by risk level calculator");
		}

		Map<String, TrustworthinessAttributeSet> priorTwass = smq.getTrustworthinessAttributeSets(tester.getStore());
		Map<String, MisbehaviourSet> priorMss = smq.getMisbehaviourSets(tester.getStore(), true); //get causes and effects
		Map<String, Threat> priorThreats = smq.getSystemThreats(tester.getStore());

		for (ControlSet cs : smq.getControlSets(tester.getStore()).values()) {
			cs.setProposed(!cs.isProposed());
			smu.updateControlSet(tester.getStore(), cs);
		}
		Map<String, Level> priorTwasLevel = new HashMap<>();
		Level lowLevel = smq.getLevels(tester.getStore(), "TrustworthinessLevel")
				.get("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#TrustworthinessLevelLow");
		for (TrustworthinessAttributeSet twas : smq.getTrustworthinessAttributeSets(tester.getStore()).values()) {
			priorTwasLevel.put(twas.getUri(), twas.getAssertedTWLevel());
			twas.setAssertedTWLevel(lowLevel);
			smu.updateTWAS(tester.getStore(), twas);
		}

		try {
			IQuerierDB querierDB = new JenaQuerierDB(dataset, tester.getModel(), true);
			querierDB.initForRiskCalculation();
			RiskCalculator rc = new RiskCalculator(querierDB);
			rc.calculateRiskLevels(RiskCalculationMode.FUTURE, true, new Progress(tester.getGraph("system")));
		} catch (Exception e) {
			logger.error("Exception thrown by risk level calculator", e);
			fail("Exception thrown by risk level calculator");
		}

		for (ControlSet cs : smq.getControlSets(tester.getStore()).values()) {
			cs.setProposed(!cs.isProposed());
			smu.updateControlSet(tester.getStore(), cs);
		}
		for (TrustworthinessAttributeSet twas : smq.getTrustworthinessAttributeSets(tester.getStore()).values()) {
			twas.setAssertedTWLevel(priorTwasLevel.get(twas.getUri()));
			smu.updateTWAS(tester.getStore(), twas);
		}

		try {
			IQuerierDB querierDB = new JenaQuerierDB(dataset, tester.getModel(), true);
			querierDB.initForRiskCalculation();
			RiskCalculator rc = new RiskCalculator(querierDB);
			rc.calculateRiskLevels(RiskCalculationMode.FUTURE, true, new Progress(tester.getGraph("system")));
		} catch (Exception e) {
			logger.error("Exception thrown by risk level calculator", e);
			fail("Exception thrown by risk level calculator");
		}

		Map<String, TrustworthinessAttributeSet> afterTwass = smq.getTrustworthinessAttributeSets(tester.getStore());
		Map<String, MisbehaviourSet> afterMss = smq.getMisbehaviourSets(tester.getStore(), true); //get causes and effects
		Map<String, Threat> afterThreats = smq.getSystemThreats(tester.getStore());

		for (TrustworthinessAttributeSet twas : priorTwass.values()) {
			assertEquals(twas.getInferredTWLevel(), afterTwass.get(twas.getUri()).getInferredTWLevel());
		}
		for (MisbehaviourSet ms : priorMss.values()) {
			MisbehaviourSet afterMs = afterMss.get(ms.getUri());
			assertEquals(ms.getLikelihood(), afterMs.getLikelihood());
			assertEquals(ms.getRiskLevel(),  afterMs.getRiskLevel());
		}
		for (Threat threat : priorThreats.values()) {
			Threat afterThreat = afterThreats.get(threat.getUri());
			assertEquals(threat.getLikelihood(), afterThreat.getLikelihood());
			assertEquals(threat.getRiskLevel(), afterThreat.getRiskLevel());
			assertEquals(threat.isRootCause(), afterThreat.isRootCause());
			assertTrue(threat.getIndirectEffects().keySet().containsAll(afterThreat.getIndirectEffects().keySet()));
			assertTrue(afterThreat.getIndirectEffects().keySet().containsAll(threat.getIndirectEffects().keySet()));
		}
	}

}
