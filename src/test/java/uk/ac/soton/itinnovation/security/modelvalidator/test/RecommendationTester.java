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

import org.apache.jena.query.Dataset;
import org.apache.jena.tdb.TDBFactory;
import org.junit.After;
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
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.RecommendationsAlgorithm;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.RecommendationsAlgorithmConfig;
import uk.ac.soton.itinnovation.security.modelvalidator.Progress;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.recommendations.RecommendationReportDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@RunWith(JUnit4.class)
public class RecommendationTester extends TestCase {

	public static Logger logger = LoggerFactory.getLogger(RecommendationTester.class);

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

        // cyberkit demo,
        tester.addDomain(0, "modelvalidator/AttackPath/domain-6a3-3-1.nq.gz",
                "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network");

        tester.addSystem(0, "modelvalidator/AttackPath/Demo_both_state_reports.nq.gz",
                "http://it-innovation.soton.ac.uk/system/65944381aa547a34a3a03f10");
        //tester.addSystem(0, "modelvalidator/AttackPath/cyberkit4sme_demo.nq.gz",
        //        "http://it-innovation.soton.ac.uk/system/652fe5d3d20c015ba8f02fb6");

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
	public void testRecommendations() {
		logger.info("Switching to selected domain and system model test cases");
		tester.switchModels(0, 0);

		logger.info("Creating a querierDB object ");
		IQuerierDB querierDB = new JenaQuerierDB(dataset, tester.getModel(), true);
		logger.info("Calling querierDB.init");
		querierDB.init();

        /*
        try {
		    querierDB.initForValidation();
            logger.info("Validating the model - ensures no dependence on bugs in older SSM validators");
            Validator validator = new Validator(querierDB);
            validator.validate(new Progress(tester.getGraph("system")));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception thrown by validator preparing attack path test case");
            return;
		}

        try {
		    querierDB.initForRiskCalculation();
			logger.info("Calculating risks and generating attack graph");
			RiskCalculator rc = new RiskCalculator(querierDB);
			rc.calculateRiskLevels(RiskCalculationMode.FUTURE, true, new Progress(tester.getGraph("system"))); //save results, as queried below
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception thrown by risk level calculator");
			return;
		}
        */

		try {
			logger.info("Gathering datasets for Recommendations");

			RecommendationsAlgorithmConfig config = new RecommendationsAlgorithmConfig(querierDB, tester.getGraph("system"), "CURRENT");
			RecommendationsAlgorithm reca = new RecommendationsAlgorithm(config);

			reca.checkRequestedRiskCalculationMode("CURRENT");

			RecommendationReportDTO report = reca.recommendations(new Progress(config.getModelId()));

            // display recommendations in readable json format
            //ObjectMapper objectMapper = new ObjectMapper();
            //objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            //String json = objectMapper.writeValueAsString(report);
            //logger.info("Recommendation report: {}", json);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception thrown by attack path recommendations");
			return;
		}
	}

    @Test
	public void testStoreRecommendation() {
        logger.debug("Testing Store recommendations");
    }

}
