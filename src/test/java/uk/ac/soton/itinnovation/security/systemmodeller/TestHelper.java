/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.soton.itinnovation.security.systemmodeller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jw18
 */
public class TestHelper extends uk.ac.soton.itinnovation.security.modelquerier.util.TestHelper{
	private static final Logger logger = LoggerFactory.getLogger(TestHelper.class);

	public static final String DOM_SHIELD_URI = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-shield";
	public static final String DOM_SHIELD_NAME = "domain-shield";
	public static final String DOM_SHIELD_FPATH = "StoreTest/domain-shield.rdf";
	public static final String DOM_SHIELD_FPATH_ABS = "/" + DOM_SHIELD_FPATH;

	public static final String DOM_TESTING_2A2_1_URI = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network-testing";
	public static final String DOM_TESTING_2A2_1_FPATH = "StoreTest/TESTING-2a2-1.nq";

	public static final String SYS_SHIELD_URI = "http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3dda";
	public static final String SYS_SHIELD_FPATH = "StoreTest/system-shield.nq";
	public static final String SYS_SHIELD_FPATH_ABS = "/" + SYS_SHIELD_FPATH;

	//Test model with metadata pairs
	//This was derived from the SHIELD model above
	public static final String SYS_METADATA_OLD_URI = SYS_SHIELD_URI;
	public static final String SYS_METADATA_NEW_URI = "http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3ddb";
	public static final String SYS_METADATA_FPATH = "StoreTest/system-shield-with-metadata-pairs.nq";
	public static final String SYS_METADATA_FPATH_ABS = "/" + SYS_METADATA_FPATH;

	//4 Test system models, all featuring a single host and process and various stages in design and evaluation
	//Assert: host and process, no relation between them
	public static final String SYS_TEST_ASSERT = "http://it-innovation.soton.ac.uk/system/5b474b003173dc2fd8f2e649";
	public static final String SYS_TEST_ASSERT_FPATH = "StoreTest/Host-Process-Disconnected.nq";
	//Invalid: the model now has issues as the process is not hosted
	public static final String SYS_TEST_INVALID = "http://it-innovation.soton.ac.uk/system/5b474ec63173dc31743e1ffc";
	public static final String SYS_TEST_INVALID_FPATH = "StoreTest/Host-Process-Disconnected-Invalidated.nq";
	//valid: the model's issues are resolved (relation created)
	public static final String SYS_TEST_VALID = "http://it-innovation.soton.ac.uk/system/5b474f003173dc31743e1ffd";
	public static final String SYS_TEST_VALID_FPATH = "StoreTest/Host-Process-Connected-Validated.nq";
	//risk: the risk analysis has now been performed on the model
	public static final String SYS_TEST_RISK = "http://it-innovation.soton.ac.uk/system/5b474f353173dc31743e1ffe";
	public static final String SYS_TEST_RISK_FPATH = "StoreTest/Host-Process-Connected-RiskAnalysis.nq";

	//Empty Model
	public static final String SYS_EMPTY = "http://it-innovation.soton.ac.uk/system/5b474b003173dc2fd8f2e538";
	public static final String SYS_EMPTY_FPATH = "StoreTest/empty-system-model.nq";

	//Test model for exporting system model reports as JSON
	//Uses the DOM_TESTING-2A2-1_URI domain model
	public static final String SYS_JSON_REPORT_URI = "http://it-innovation.soton.ac.uk/system/5f5148fc1904940fb59846ad";
	public static final String SYS_JSON_REPORT_FPATH = "StoreTest/JSON-Report-Test.nq";

	//Groups: model to test asset groups
	public static final String SYS_GROUPS = "http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3dda";
	public static final String SYS_GROUPS_FPATH = "StoreTest/system-test-groups.nq";

	public TestHelper(String storeDir) {
		super(storeDir);
		this.addDomainAndSystemGraphs();
	}

	public void addDomainAndSystemGraphs() {
		this.addDomain(0, DOM_SHIELD_FPATH, DOM_SHIELD_URI);
		this.addDomain(1, DOM_TESTING_2A2_1_FPATH, DOM_TESTING_2A2_1_URI);

		//Test domain model for population support
		this.addDomain(2, "StoreTest/domain-ssm-testing-6a3.nq", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/ssm-testing-6a3");

		this.addSystem(0, SYS_SHIELD_FPATH, SYS_SHIELD_URI);
		this.addSystem(1, SYS_TEST_ASSERT_FPATH, SYS_TEST_ASSERT);
		this.addSystem(2, SYS_TEST_INVALID_FPATH, SYS_TEST_INVALID);
		this.addSystem(3, SYS_TEST_VALID_FPATH, SYS_TEST_VALID);
		this.addSystem(4, SYS_TEST_RISK_FPATH, SYS_TEST_RISK);
		this.addSystem(5, SYS_EMPTY_FPATH, SYS_EMPTY);
		this.addSystem(6, SYS_METADATA_FPATH, SYS_METADATA_OLD_URI, SYS_METADATA_NEW_URI);
		this.addSystem(7, SYS_JSON_REPORT_FPATH, SYS_JSON_REPORT_URI);
		this.addSystem(8, SYS_GROUPS_FPATH, SYS_GROUPS);

		//Test system model for population support
		this.addSystem(9, "StoreTest/Test-6a3-00.nq.gz",
				"http://it-innovation.soton.ac.uk/system/63971077df89a647814e6d8b");

		//Latest tests
		this.addDomain(3, "StoreTest/dataflow/domain-network-6a3-2-2.nq", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network");
		this.addSystem(10, "StoreTest/dataflow/DataFlow_Test_Singles_asserted.nq.gz",
				"http://it-innovation.soton.ac.uk/system/63d9308f8f6a206408be9010");
		this.addSystem(11, "StoreTest/dataflow/DataFlow_Test_Singles.nq.gz",
				"http://it-innovation.soton.ac.uk/system/63d9308f8f6a206408be9010");

		//Test models for recommenations
		this.addDomain(4, "modelvalidator/AttackPath/domain-6a3-3-1.nq.gz", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network");
		//this.addSystem(12, "modelvalidator/AttackPath/cyberkit4sme_demo.nq.gz",
		//		"http://it-innovation.soton.ac.uk/system/652fe5d3d20c015ba8f02fb6");
		this.addSystem(12, "modelvalidator/AttackPath/Demo_both_state_reports.nq.gz",
				"http://it-innovation.soton.ac.uk/system/65944381aa547a34a3a03f10");
				
	}
}