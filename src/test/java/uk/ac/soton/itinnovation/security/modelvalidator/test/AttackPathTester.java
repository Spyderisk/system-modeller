/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2023
//
// Copyright in this software belongs to University of Southampton
// IT Innovation Centre, Highfield Campus, SO17 1BJ, UK.
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
//      Created By :            Panos Melas
//      Created Date :          2023-02-16
//      Created for Project :   Cyberkit4SME
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.modelvalidator.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.tdb.TDBFactory;
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

import junit.framework.TestCase;
import uk.ac.soton.itinnovation.security.modelquerier.IQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.JenaQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.SystemModelQuerier;
import uk.ac.soton.itinnovation.security.modelquerier.SystemModelUpdater;
import uk.ac.soton.itinnovation.security.modelquerier.util.TestHelper;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.AttackPathAlgorithm;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.RecommendationsAlgorithm;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.RecommendationsAlgorithmConfig;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.TreeJsonDoc;

import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;
import uk.ac.soton.itinnovation.security.modelvalidator.Progress;
import uk.ac.soton.itinnovation.security.modelvalidator.RiskCalculator;
import uk.ac.soton.itinnovation.security.modelvalidator.Validator;

import uk.ac.soton.itinnovation.security.model.system.MisbehaviourSet;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.recommendations.RecommendationReportDTO;
import uk.ac.soton.itinnovation.security.modelquerier.dto.RiskCalcResultsDB;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@RunWith(JUnit4.class)
public class AttackPathTester extends TestCase {

	public static Logger logger = LoggerFactory.getLogger(RiskLevelCalculatorTester.class);

	private static TestHelper tester;
	private static Dataset dataset;
	private long stopwatch;

	private static SystemModelQuerier smq;
	private static SystemModelUpdater smu;

	@Rule
	public TestName name = new TestName();

	@Rule
	public ErrorCollector collector = new ErrorCollector();

	@BeforeClass
	public static void beforeClass() {

		tester = new TestHelper("jena-tdb");

        tester.addDomain(0, "modelvalidator/domain-network-6a1-3-5-auto-expanded-unfiltered.nq.gz",
                "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network");

		tester.addSystem(0, "modelvalidator/system-dataflow-test-singles.nq.gz",
                "http://it-innovation.soton.ac.uk/system/63d9308f8f6a206408be9010");

		tester.setUp();

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

		// comment in to better debug the test models
		logger.debug("Exporting test model");
		tester.exportTestModel("build/build/test-results/" + name.getMethodName(), true, false, true);
	}

	// Tests //////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test
	public void testAttackPathGraph() {
		logger.info("Switching to selected domain and system model test cases");
		tester.switchModels(0, 0);

		logger.info("Creating a querierDB object");
		IQuerierDB querierDB = new JenaQuerierDB(dataset, tester.getModel(), true);
		querierDB.init();

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
            //RiskCalcResultsDB results = rc.getRiskCalcResults();
            //logger.debug("RiskResutlst: {}", results);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception thrown by risk level calculator");
			return;
		}

		try {
			logger.info("Gathering datasets for the attack graph");

			AttackPathAlgorithm apa = new AttackPathAlgorithm(querierDB);

			List<String> targetUris = new ArrayList<>();
			targetUris.add("system#MS-LossOfAuthenticity-a40e98cc");

			Assert.assertTrue(apa.checkTargetUris(targetUris));

			apa.checkRequestedRiskCalculationMode("FUTURE");

			TreeJsonDoc treeDoc = apa.calculateAttackTreeDoc(targetUris, "FUTURE", false, false);

		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception thrown by attack path dataset gathering");
			return;
		}
	}
}
