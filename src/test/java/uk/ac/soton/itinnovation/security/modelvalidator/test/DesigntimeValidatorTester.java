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
//      Created By :            Stefanie Cox
//      Created Date :          2018-04-09
//      Created for Project :   RESTASSURED
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.modelvalidator.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

import org.apache.jena.query.Dataset;
import org.apache.jena.tdb.TDBFactory;

import org.apache.commons.cli.MissingArgumentException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;
import uk.ac.soton.itinnovation.security.domainreasoner.IDomainReasoner;
import uk.ac.soton.itinnovation.security.domainreasoner.NullReasoner;
import uk.ac.soton.itinnovation.security.model.Level;
import uk.ac.soton.itinnovation.security.model.system.Asset;
import uk.ac.soton.itinnovation.security.model.system.MisbehaviourSet;
import uk.ac.soton.itinnovation.security.model.system.Relation;
import uk.ac.soton.itinnovation.security.model.system.Threat;
import uk.ac.soton.itinnovation.security.model.system.TrustworthinessAttributeSet;
import uk.ac.soton.itinnovation.security.modelquerier.IQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.JenaQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.SystemModelQuerier;
import uk.ac.soton.itinnovation.security.modelquerier.SystemModelUpdater;
import uk.ac.soton.itinnovation.security.modelquerier.dto.AssetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.CardinalityConstraintDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.LevelDB;
import uk.ac.soton.itinnovation.security.modelquerier.util.TestHelper;
import uk.ac.soton.itinnovation.security.modelvalidator.DesigntimeValidator;
import uk.ac.soton.itinnovation.security.modelvalidator.Progress;
import uk.ac.soton.itinnovation.security.semanticstore.IStoreWrapper;

@RunWith(JUnit4.class)
public class DesigntimeValidatorTester extends TestCase {

	public static Logger logger = LoggerFactory.getLogger(DesigntimeValidatorTester.class);

	private static TestHelper tester;
	private static Dataset dataset;
	private long stopwatch;

	private static DesigntimeValidator dv;
	private static SystemModelQuerier smq;
	private static SystemModelUpdater smu;

	//temporary domain/system model information when switching for individual tests
	private static IDomainReasoner tmpReasoner;

	@Rule public TestName name = new TestName();

	@Rule public ErrorCollector collector = new ErrorCollector();

	@BeforeClass
	public static void beforeClass() {

		tester = new TestHelper("jena-tdb");

		tester.addDomain(0, "modelvalidator/domain-test.rdf.gz", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-test");
		tester.addDomain(1, "modelvalidator/domain-network.rdf.gz", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network");
		tester.addDomain(2, "modelvalidator/domain-shield.rdf.gz", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-shield");
		tester.addDomain(3, "modelvalidator/domain-network-ra.rdf.gz", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network-ra");
		tester.addDomain(4, "modelvalidator/TESTING-2a6-1.nq.gz", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network-testing");
		tester.addDomain(5, "modelvalidator/TESTING-2a4-1.nq.gz",
			"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network-testing",
			"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/TESTING-2a4-1");
		tester.addDomain(6, "modelvalidator/TESTING-2b1-1.nq.gz",
			"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network-testing",
			"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/TESTING-2b1-1");

		//be cautious with sharing URIs: if the validated model contains anything asserted, the unvalidated model
		//will have it too as it shared the same graph URI. For this reason, we define a new URI for this model to avoid clashes
		tester.addSystem(0, "modelvalidator/system-test.nq.gz",
			"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system",
			"http://it-innovation.soton.ac.uk/user/594a6bd21719e03c4c38ccbd/system/5950bf301719e018be44477a/Test");
		tester.addSystem(1, "modelvalidator/system-network.nq.gz", "http://it-innovation.soton.ac.uk/system/5ad09178567d94846a9aeaec");
		tester.addSystem(2, "modelvalidator/system-shield.nq.gz", "http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3dda");
		tester.addSystem(3, "modelvalidator/system-shield-valid.nq.gz",
			"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3dda",
			"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3ddb");
		tester.addSystem(4, "modelvalidator/melchett.nq.gz", "http://it-innovation.soton.ac.uk/system/5b360450757fbc0b601f0c16");
		tester.addSystem(5, "modelvalidator/PatternMatchingAndControlStrategyTest-v2a.nq.gz", "http://it-innovation.soton.ac.uk/system/5d99e263884bd32188c3282b");
		tester.addSystem(6, "modelvalidator/ConstructionPatternTest-v2a.nq.gz", "http://it-innovation.soton.ac.uk/system/5d9b4af3884bd304c91ef4e0");
		tester.addSystem(7, "modelvalidator/ConstructionPatternTest_-_v2b.nq.gz",
			"http://it-innovation.soton.ac.uk/system/5d9b4af3884bd304c91ef4e0",
			"http://it-innovation.soton.ac.uk/system/5d9b4af3884bd304c91ef4e1");
		tester.addSystem(8, "modelvalidator/ConstructionPatternTest_v2_-_v2b.nq.gz",
			"http://it-innovation.soton.ac.uk/system/5d9b4af3884bd304c91ef4e0",
			"http://it-innovation.soton.ac.uk/system/5d9b4af3884bd304c91ef4e2");

		tester.setUp();

		tester.switchModels(0, 0);

		try {
			dv = new DesigntimeValidator(tester.getStore(), tester.getModel(), new NullReasoner());
		} catch (MissingArgumentException ex) {
			logger.error("Could not create model validator. Exiting...", ex);
			System.exit(-1);
		}

		/*
		// Get population level objects and store them in a hash map
		Collection<Level> populations = smq.getLevels(tester.getStore(), "PopulationLevel").values();
		for(Level p : populations) {
			populationLevels.put(p.getUri(), p);
		}
		*/

		smq = new SystemModelQuerier(tester.getModel());
		smu = new SystemModelUpdater(tester.getModel());

		dataset = TDBFactory.createDataset("jena-tdb");

		logger.info("DesigntimeValidator tests executing...");
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
		
		setReasoner(null);
	}

	/**
	 * Build a test model
	 *
	 * @param prohibitedNode whether there is a prohibited node connected with the matching pattern. Use 0 or 1
	 * @param mandatoryNodes the amount of mandatory nodes. Test with 0, 1 or 2
	 * @param optionalNodes the amount of optional nodes. Test with 0, 1 or 2
	 * @param prohibitedRel whether a prohibited relation exists. Only makes sense if the node exists. Use 0, 1 or 2
	 */
	private void buildTestModel(int prohibitedNode, int mandatoryNodes, int optionalNodes, int prohibitedRel) {

		//root nodes
		Asset a1 = new Asset();
		a1.setUri(tester.getNS("system") + "A1");
		a1.setType(tester.getNS("domain") + "A");
		a1.setLabel("A1");
		Asset e1 = new Asset();
		e1.setUri(tester.getNS("system") + "E1");
		e1.setType(tester.getNS("domain") + "E");
		e1.setLabel("E1");
		Asset d1 = new Asset();
		d1.setUri(tester.getNS("system") + "D1");
		d1.setType(tester.getNS("domain") + "D");
		d1.setLabel("D1");

		//secondary nodes
		//--prohibited
		Asset g1 = new Asset();
		g1.setUri(tester.getNS("system") + "G1");
		g1.setType(tester.getNS("domain") + "G");
		g1.setLabel("G1");

		//--mandatory
		List<Asset> mandatory = new ArrayList<>();
		Asset f1 = new Asset();
		f1.setUri(tester.getNS("system") + "F1");
		f1.setType(tester.getNS("domain") + "F");
		f1.setLabel("F1");
		mandatory.add(f1);
		Asset f2 = new Asset();
		f2.setUri(tester.getNS("system") + "F2");
		f2.setType(tester.getNS("domain") + "F");
		f2.setLabel("F2");
		mandatory.add(f2);

		//--optional
		List<Asset> optional = new ArrayList<>();
		Asset b1 = new Asset();
		b1.setUri(tester.getNS("system") + "B1");
		b1.setType(tester.getNS("domain") + "B");
		b1.setLabel("B1");
		optional.add(b1);
		Asset b2 = new Asset();
		b2.setUri(tester.getNS("system") + "B2");
		b2.setType(tester.getNS("domain") + "B");
		b2.setLabel("B2");
		optional.add(b2);

		String sparql = "INSERT DATA { GRAPH <" + tester.getGraph("system") + "> {\n" +
		//import (for easier debugging in topbraid)
		"	<" + tester.getNS("system").replace("#", "") + "> owl:imports <" + tester.getNS("domain").replace("#", "") + "> .\n" +
		//root nodes
		"	<" + a1.getUri() + "> a <" + a1.getType() + "> .\n" +
		"	<" + a1.getUri() + "> core:hasID \"" + a1.getID() + "\" .\n" +
		"	<" + a1.getUri() + "> rdfs:label \"" + a1.getLabel() + "\" .\n" +
		"	<" + e1.getUri() + "> a <" + e1.getType() + "> .\n" +
		"	<" + e1.getUri() + "> core:hasID \"" + e1.getID() + "\" .\n" +
		"	<" + e1.getUri() + "> rdfs:label \"" + e1.getLabel() + "\" .\n" +
		"	<" + d1.getUri() + "> a <" + d1.getType() + "> .\n" +
		"	<" + d1.getUri() + "> core:hasID \"" + d1.getID() + "\" .\n" +
		"	<" + d1.getUri() + "> rdfs:label \"" + d1.getLabel() + "\" .\n" +
		// E1->A1
		"	<" + e1.getUri() + "> <" + tester.getNS("domain") + "rel2> <" + a1.getUri() + "> .\n" +
		// E1->D1
		"	<" + e1.getUri() + "> <" + tester.getNS("domain") + "rel2> <" + d1.getUri() + "> .\n" +
		// A1->D1
		"	<" + a1.getUri() + "> <" + tester.getNS("domain") + "rel1> <" + d1.getUri() + "> .\n";

		//prohibited node
		if (prohibitedNode>0) {
			sparql += "	<" + g1.getUri() + "> a <" + g1.getType() + "> .\n" +
			"	<" + g1.getUri() + "> core:hasID \"" + g1.getID() + "\" .\n" +
			"	<" + g1.getUri() + "> rdfs:label \"" + g1.getLabel() + "\" .\n" +
			// G1->E1
			"	<" + g1.getUri() + "> <" + tester.getNS("domain") + "rel4> <" + e1.getUri() + "> .\n";
		}

		//mandatory node
		if (mandatoryNodes>0) {
			for (int i=0; i<mandatoryNodes; i++) {
				sparql += "	<" + mandatory.get(i).getUri() + "> a <" + mandatory.get(i).getType() + "> .\n" +
				"	<" + mandatory.get(i).getUri() + "> core:hasID \"" + mandatory.get(i).getID() + "\" .\n" +
				"	<" + mandatory.get(i).getUri() + "> rdfs:label \"" + mandatory.get(i).getLabel() + "\" .\n" +
				// Fx->A1
				"	<" + mandatory.get(i).getUri() + "> <" + tester.getNS("domain") + "rel3> <" + a1.getUri() + "> .\n";
			}
		}

		//optional node
		if (optionalNodes>0) {
			for (int i=0; i<optionalNodes; i++) {
				sparql += "	<" + optional.get(i).getUri() + "> a <" + optional.get(i).getType() + "> .\n" +
				"	<" + optional.get(i).getUri() + "> core:hasID \"" + optional.get(i).getID() + "\" .\n" +
				"	<" + optional.get(i).getUri() + "> rdfs:label \"" + optional.get(i).getLabel() + "\" .\n" +
				// E1->Bx
				"	<" + e1.getUri() + "> <" + tester.getNS("domain") + "rel2> <" + optional.get(i).getUri() + "> .\n";

				//check relationship from A1 to each optional node
				if (i==0 && prohibitedRel==1) {
					// A1->B1
					sparql += "	<" + a1.getUri() + "> <" + tester.getNS("domain") + "rel1> <" + b1.getUri() + "> .\n";
				} else if (i==1 && prohibitedRel==2) {
					// A1->B1
					sparql += "	<" + a1.getUri() + "> <" + tester.getNS("domain") + "rel1> <" + b1.getUri() + "> .\n";
					// A1->B2
					sparql += "	<" + a1.getUri() + "> <" + tester.getNS("domain") + "rel1> <" + b2.getUri() + "> .\n";
				}
			}
		}

		sparql += "}}";
		//logger.debug(sparql);
		tester.getStore().update(sparql);
	}

	/**
	 * Switch the models for testing
	 *
	 * @param domainModel
	 * @param systemModel
	 * @param reasoner
	 */
	private void setReasoner(IDomainReasoner reasoner) {

		if (tmpReasoner!=null) {
			//reasoner may be null
			tmpReasoner = dv.getReasoner();
			try {
				dv = new DesigntimeValidator(tester.getStore(), tester.getModel(), reasoner);
			} catch (MissingArgumentException ex) {
				logger.error("Could not update domain-specific reasoner", ex);
			}
		}
	}

	// Tests //////////////////////////////////////////////////////////////////////////////////////////////////////////

	//Apologies for storing this here: it's a utility method to quickly create merged models
	@Test@Ignore
	public void testMergeModels() {

		String path = "/home/stef/desktop/";

		tester.getStore().clearGraph("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network");
		tester.getStore().clearGraph("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network-ui");

		tester.getStore().loadIntoGraph(path + "risk-ui.ttl", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network-ui", IStoreWrapper.Format.TTL);
		tester.getStore().loadIntoGraph(path + "risk.ttl", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network", IStoreWrapper.Format.TTL);

		tester.getStore().save(path + "domain-network", IStoreWrapper.Format.NQ, null, true, "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network-ui");
		tester.getStore().save(path + "domain-network", IStoreWrapper.Format.RDF, "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network", false, "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network-ui");
	}

/*
	@Test
	public void testCreateRootPattern() {

		tester.switchModels(0, 0);

		//make sure the model is NOT validated
		tester.getStore().clearGraph(tester.getGraph("system-inf"));

		//create a single root pattern
		dv.createRootPattern(tester.getNS("domain") + "R-ADE");
		//check that...
		//all matches (system-specific root patterns) are found and they are the correct ones
		Map<String, Pattern> rps = smq.getRootPatterns(tester.getStore());
		assertEquals(4, rps.size());
		//R-ADE_A1_E1
		assertTrue(rps.containsKey(tester.getNS("system") + "R-ADE_8383a615_8383a672_8383a691"));
		//R-ADE_A1_E2
		assertTrue(rps.containsKey(tester.getNS("system") + "R-ADE_8383a615_8383a672_8383a692"));
		//R-ADE_A2_E1
		assertTrue(rps.containsKey(tester.getNS("system") + "R-ADE_8383a616_8383a673_8383a694"));
		//R-ADE_A2_E4
		assertTrue(rps.containsKey(tester.getNS("system") + "R-ADE_8383a616_8383a673_8383a691"));

		Pattern rp = smq.getRootPattern(tester.getStore(), tester.getNS("system") + "R-ADE_8383a615_8383a672_8383a691");
		//check that the the label is made up of only key nodes (but the URI contains all key and root nodes)
		assertEquals("R-ADE_A1_E1", rp.getLabel());
		//check that the matches contain all nodes (no links are contained in any pattern instance -
		//they are defined in generic patterns only)
		assertEquals(3, rp.getNodes().size());

		tester.getStore().clearGraph(tester.getGraph("system-inf"));

		//create multiple root patterns
		dv.createRootPatterns(true, true, false, new Progress("test"), 1.0);
		//check that R-AE and R-ADE are created
		rps = smq.getRootPatterns(tester.getStore());
		assertEquals(11, rps.size());
		//R-ADE_A1_E1
		assertTrue(rps.containsKey(tester.getNS("system") + "R-ADE_8383a615_8383a672_8383a691"));
		//R-ADE_A1_E2
		assertTrue(rps.containsKey(tester.getNS("system") + "R-ADE_8383a615_8383a672_8383a692"));
		//R-ADE_A2_E1
		assertTrue(rps.containsKey(tester.getNS("system") + "R-ADE_8383a616_8383a673_8383a691"));
		//R-ADE_A2_E4
		assertTrue(rps.containsKey(tester.getNS("system") + "R-ADE_8383a616_8383a673_8383a694"));
		//R-AE_A1_E1
		assertTrue(rps.containsKey(tester.getNS("system") + "R-AE_8383a615_8383a691"));
		//R-AE_A1_E2
		assertTrue(rps.containsKey(tester.getNS("system") + "R-AE_8383a615_8383a692"));
		//R-AE_A1_E3
		assertTrue(rps.containsKey(tester.getNS("system") + "R-AE_8383a615_8383a693"));
		//R-AE_A1_E4
		assertTrue(rps.containsKey(tester.getNS("system") + "R-AE_8383a615_8383a694"));
		//R-AE_A1_E5
		assertTrue(rps.containsKey(tester.getNS("system") + "R-AE_8383a615_8383a695"));
		//R-AE_A2_E1
		assertTrue(rps.containsKey(tester.getNS("system") + "R-AE_8383a616_8383a691"));
		//R-AE_A2_E4
		assertTrue(rps.containsKey(tester.getNS("system") + "R-AE_8383a616_8383a694"));
		tester.getStore().clearGraph(tester.getGraph("system-inf"));

		dv.createRootPatterns(true, false, false, new Progress("test"), 1.0);
		rps = smq.getRootPatterns(tester.getStore());
		//check that R-AE is created
		assertEquals(7, rps.size());
		//R-AE_A1_E1
		assertTrue(rps.containsKey(tester.getNS("system") + "R-AE_8383a615_8383a691"));
		//R-AE_A1_E2
		assertTrue(rps.containsKey(tester.getNS("system") + "R-AE_8383a615_8383a692"));
		//R-AE_A1_E3
		assertTrue(rps.containsKey(tester.getNS("system") + "R-AE_8383a615_8383a693"));
		//R-AE_A1_E4
		assertTrue(rps.containsKey(tester.getNS("system") + "R-AE_8383a615_8383a694"));
		//R-AE_A1_E5
		assertTrue(rps.containsKey(tester.getNS("system") + "R-AE_8383a615_8383a695"));
		//R-AE_A2_E1
		assertTrue(rps.containsKey(tester.getNS("system") + "R-AE_8383a616_8383a691"));
		//R-AE_A2_E4
		assertTrue(rps.containsKey(tester.getNS("system") + "R-AE_8383a616_8383a694"));
		tester.getStore().clearGraph(tester.getGraph("system-inf"));

		dv.createRootPatterns(false, true, false, new Progress("test"), 1.0);
		rps = smq.getRootPatterns(tester.getStore());
		//check that R-ADE is created
		assertEquals(4, rps.size());
		//R-ADE_A1_E1
		assertTrue(rps.containsKey(tester.getNS("system") + "R-ADE_8383a615_8383a672_8383a691"));
		//R-ADE_A1_E2
		assertTrue(rps.containsKey(tester.getNS("system") + "R-ADE_8383a615_8383a672_8383a692"));
		//R-ADE_A2_E1
		assertTrue(rps.containsKey(tester.getNS("system") + "R-ADE_8383a616_8383a673_8383a691"));
		//R-ADE_A2_E4
		assertTrue(rps.containsKey(tester.getNS("system") + "R-ADE_8383a616_8383a673_8383a694"));
		tester.getStore().clearGraph(tester.getGraph("system-inf"));

		dv.createRootPatterns(false, false, false, new Progress("test"), 1.0);
		rps = smq.getRootPatterns(tester.getStore());
		//check that no patterns are created
		assertEquals(0, rps.size());
		tester.getStore().clearGraph(tester.getGraph("system-inf"));
	}

	@Test
	public void testCreateSystemPattern() {

		tester.switchModels(0, 0);

		//use this graph for storing the synthetic test models
		TestModel test = new TestModel();
		test.setGraph("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system-test");
		tester.switchModels(tester.getDomains().get(0), test);

		int prohibitedNode = 1;
		int mandatoryNodes = 2;
		int optionalNodes = 2;
		int prohibitedRel = 2;

		String patternID;
		for (int p=0; p<=prohibitedNode; p++) {
			//logger.debug("Prohibited node present? {}", p);

			for (int m=0; m<=mandatoryNodes; m++) {
				//logger.debug("Amount of mandatory nodes: {}", m);

				for (int o=0; o<=optionalNodes; o++) {
					//logger.debug("Amount of optional nodes: {}", o);

					for (int r=0; r<=prohibitedRel; r++) {

						//skip cases that don't make sense
						if ((o==0 && r>0) || (o==1 && r>1)) {
							continue;
						}
						//logger.debug("Amount of prohibited relationships: {}", r);

						patternID = "p" + p + "m" + m + "o" + o + "r" + r;
						logger.info("Testing case {}", patternID);

						//clear system graphs - models are constructed on the fly
						tester.getStore().clearGraph(tester.getGraph("system"));
						tester.getStore().clearGraph(tester.getGraph("system-inf"));

						//set up test model
						buildTestModel(p, m, o, r);
						//exportTestModel("build/build/test-results/" + patternID);

						//create root pattern
						String uri = tester.getNS("system") + "R-ADE_8383a615_8383a672_8383a691";
						dv.createRootPattern(tester.getNS("domain") + "R-ADE");
						Pattern rp = smq.getRootPattern(tester.getStore(), uri);
						//make sure it was successful
						assertEquals(uri, rp.getUri());
						assertEquals("R-ADE_A1_E1", rp.getLabel());
						assertEquals(3, rp.getNodes().size());

						//create matching pattern
						dv.createMatchingPattern(tester.getNS("domain") + "ADEF");
						//exportTestModel("build/build/test-results/" + patternID + "-matched");
						Map<String, Pattern> matches = smq.getSystemPatterns(tester.getStore());

						//prohibited node contained: should fail
						if (p>0) {
							assertTrue(matches.isEmpty());
						}

						//mandatory nodes missing: should fail
						if (mandatoryNodes==0) {
							assertTrue(matches.isEmpty());
						}

						//prohibited relationship to optional node contained: should fail
						if (r>0) {
							assertTrue(matches.isEmpty());
						}

						//check the expected matches
						List<String> expectedMatches = new ArrayList<>();
						expectedMatches.add("p0m1o0r0");
						expectedMatches.add("p0m1o1r0");
						expectedMatches.add("p0m1o2r0");
						expectedMatches.add("p0m2o0r0");
						expectedMatches.add("p0m2o1r0");
						expectedMatches.add("p0m2o2r0");
						//logger.debug("Matches found: {}", matches);

						//only those should be matched
						if (expectedMatches.contains(patternID)) {
							assertFalse(matches.isEmpty());

							//one match is expected
							Pattern match = matches.values().iterator().next();

							//test URI/label generation
							assertEquals(tester.getNS("system") + "ADEF_8383a615_8383a672_8383a691", match.getUri());
							assertEquals("ADEF_A1_E1", match.getLabel());

							//test at least one mandatory node exists
							boolean found = false;
							for (Node n: match.getNodes()) {
								if (n.getRole().equals(tester.getNS("domain") + "FRole")) {
									found = true;
									break;
								}
							}
							assertTrue(found);

							//TODO: test relationships:

							//--check all mandatory relationships exist:
							//get rels from (generic) parent pattern (incl root pattern rels)
							//and check they all exist in the system pattern

							//--check no prohibited relationships exist
							//get prohibited rels from (generic) parent pattern
							//and check none of them exists in the system pattern

						} else {
							assertTrue(matches.isEmpty());
						}
					}
				}
			}
		}
	}

	@Test
	public void testCreateMatchingPattern() {

		tester.switchModels(1, 1);

		//make sure the model is NOT validated
		tester.getStore().clearGraph(tester.getGraph("system-inf"));

		dv.createRootPattern(tester.getNS("domain") + "R-25fe70a-25fe70a-3a94024c-c8ff31a8");
		dv.createMatchingPattern(tester.getNS("domain") + "HP");

		Map<String, Pattern> patterns = smq.getSystemPatterns(tester.getStore());
		assertEquals(3, patterns.size());
	}

	@Test
	public void testCreateConstructionPattern() {

		tester.switchModels(0, 0);

		//prerequisites
		dv.createRootPatterns(true, false, false, new Progress("test"), 1.0);

		//node construction (typically includes link construction)
		dv.createConstructionPattern(tester.getNS("domain") + "D-Node-Construction", tester.getNS("domain") + "AEF");
		//exportTestModel("build/build/test-results/" + "D-Node-Construction");
		//check whether all Ds have been created
		boolean found1 = false;
		boolean found2 = false;
		for (Asset a: smq.getSystemAssets(tester.getStore()).values()) {
			if (a.getLabel().equals("D-A1-E1") && !a.isAsserted()) {
				found1 = true;
			} else if (a.getLabel().equals("D-A1-E3") && !a.isAsserted()) {
				found2 = true;
			}
		}
		assertTrue(found1);
		assertTrue(found2);
		//check if relations have been created too
		boolean foundR1 = false;
		boolean foundR2 = false;
		boolean foundR3 = false;
		boolean foundR4 = false;
		for (Relation r: smq.getSystemRelations(tester.getStore())) {
			if (r.getFromID().equals("8383a615") && r.getType().equals(tester.getNS("domain") + "rel1")) {
				if (r.getTo().equals(tester.getNS("system") + "D_8383a615_8383a691")) {
					foundR1 = true;
				} else if (r.getTo().equals(tester.getNS("system") + "D_8383a615_8383a693")) {
					foundR2 = true;
				}
			}
			if (r.getType().equals(tester.getNS("domain") + "rel2")) {
				if (r.getFromID().equals("8383a691") && r.getTo().equals(tester.getNS("system") + "D_8383a615_8383a691")) {
					foundR3 = true;
				} else if (r.getFromID().equals("8383a693") && r.getTo().equals(tester.getNS("system") + "D_8383a615_8383a693")) {
					foundR4 = true;
				}
			}
		}
		assertTrue(foundR1);
		assertTrue(foundR2);
		assertTrue(foundR3);
		assertTrue(foundR4);

		tester.getStore().clearGraph(tester.getGraph("system-inf"));

		//prerequisites
		dv.createRootPatterns(true, false, false, new Progress("test"), 1.0);

		//link construction
		dv.createConstructionPattern(tester.getNS("domain") + "D-Link-Construction", tester.getNS("domain") + "ADEF2");
		//check whether E1-[2]->D1 has been created
		assertTrue(tester.getStore().queryAsk("ASK WHERE { system:E1 domain:rel2 system:D1 }", tester.getGraph("system-inf")));
		//check whether E3-[2]->D1 has been created
		assertTrue(tester.getStore().queryAsk("ASK WHERE { system:E3 domain:rel2 system:D1 }", tester.getGraph("system-inf")));

		//exportTestModel("build/build/test-results/" + "D-Link-Construction");
	}
*/

/*
	@Test
	public void testCreateCompositions() {

		tester.switchModels(1, 1);

		//make sure the model is NOT validated.
		//Note that this will result in fewer control sets as we also drop the inferred assets!
		tester.getStore().clearGraph(tester.getGraph("system-inf"));

		dv.createCompositions(new Progress("test"), 1.0);

		assertEquals(118, smq.getControlSets(tester.getStore()).size());

		Map<String, MisbehaviourSet> misbehaviourSets = smq.getMisbehaviourSets(tester.getStore());
		assertEquals(92, misbehaviourSets.size());
		//global default impact levels
		assertEquals(tester.getNS("domain") + "ImpactLevelVeryLow", misbehaviourSets.get(tester.getNS("system") + "MS-LossOfControl-9127bc56").getImpactLevel().getUri());
		assertEquals(tester.getNS("domain") + "ImpactLevelVeryLow", misbehaviourSets.get(tester.getNS("system") + "MS-Unreliable-9127bc56").getImpactLevel().getUri());
		//domain model default impact levels
		assertEquals(tester.getNS("domain") + "ImpactLevelLow", misbehaviourSets.get(tester.getNS("system") + "MS-LossOfIntegrity-b1b82d8d").getImpactLevel().getUri());
		assertEquals(tester.getNS("domain") + "ImpactLevelHigh", misbehaviourSets.get(tester.getNS("system") + "MS-LossOfIntegrity-346939be").getImpactLevel().getUri());
		//user asserted impact levels
		assertEquals(tester.getNS("domain") + "ImpactLevelHigh", misbehaviourSets.get(tester.getNS("system") + "MS-LossOfAvailability-346939be").getImpactLevel().getUri());
		assertEquals(tester.getNS("domain") + "ImpactLevelVeryHigh", misbehaviourSets.get(tester.getNS("system") + "MS-LossOfConfidentiality-346939be").getImpactLevel().getUri());
	}

	@Test@Ignore
	public void testCreateSystemThreats() {

		tester.switchModels(0, 0);

		Progress vp = new Progress("test");
		dv.createConstructionPatterns(vp, 0.4);
		dv.createMatchingPatterns(vp, 0.5);
		dv.createCompositions(vp, 0.1);
		dv.createSystemThreats();

		Map<String, Threat> threats = smq.getSystemThreats(tester.getStore());
		assertEquals(7, threats.size());
		for (Threat t: threats.values()) {
			if (t.getLabel().equals("Threat1_ADEF2_A1_E1")) {
				assertEquals("A Threat1 causes an A1 to misbehave and the D-A1-E1, D-A1-E3, D1 misbehaves too.", t.getDescription());
			}
			if (t.getLabel().equals("Threat2_ADE_A1_E3")) {
				assertEquals("In a Threat2 a misbehaving A1 causes the E3 to misbehave too.", t.getDescription());
			}
		}
	}
*/

	@Test
	public void testValidateModel() {
		try {
			tester.switchModels(1, 1);
			setReasoner(new NullReasoner());

			//make sure the model is NOT validated - there's a separate test for revalidating
			tester.getStore().clearGraph(tester.getGraph("system-inf"));

			dv.validateDesigntimeModel(new Progress("test"));
			//dv.validateDesigntimeModelOld(new Progress("test"));

			logger.debug("=== Validation Results ===");
			logger.debug("Assets: {}", smq.getSystemAssets(tester.getStore()).size());
			logger.debug("Threats: {}", smq.getSystemThreats(tester.getStore()).size());
			logger.debug("ControlSets: {}", smq.getControlSets(tester.getStore()).size());
			logger.debug("ControlStrategies: {}", smq.getControlStrategies(tester.getStore()).size());

			//assertEquals(-1, smq.getSystemThreats(tester.getStore()).size());
			//assertEquals(44, smq.getTrustworthinessAttributeSets(tester.getStore()).size());
			//assertEquals(150, smq.getMisbehaviourSets(tester.getStore()).size());

			//very crude test for issue ModelValidator#18
			//it doesn't directly test that inferred asset URIs are in "Role" order
			//but it does fail for the old "Asset Class" order version of the code
			//this should catch any attempt to blindly merge in branches that don't include the ModelValidator#18 fix
			assertNotNull(smq.getSystemAsset(tester.getStore(), tester.getNS("system") + "LogicalSegment_6e274853_7ff051ef_54ff1044"));

			tester.exportTestModel("build/build/test-results/" + name.getMethodName(), true, false, true);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception thrown by design time validator:" + e.getMessage());
		}
	}

    /**
     * This is a placeholder test method for validation of the test model. It is in a poor state at the minute, only
     * testing the lack of exceptions (and any cases which we add to it as and when they are needed). In the future,
     * we hope to move to a much more robust set of tests for the validator.
     */
	@Test
    public void testValidateTestModel() {
    try {
        tester.switchModels(4, 5);
        setReasoner(new NullReasoner());

        //make sure the model is NOT validated - there's a separate test for revalidating
        tester.getStore().clearGraph(tester.getGraph("system-inf"));

        long time = System.currentTimeMillis();
        dv.validateDesigntimeModel(new Progress("test"));
        logger.debug("TOTAL TIME: {}", (System.currentTimeMillis() - time));

        // Test TWAS creation for non-unique nodes
        Threat testThreat = smq.getSystemThreat(tester.getStore(),
				"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#A.M.A1mB.1-A1mB_95c9e105");
        assertEquals(2, testThreat.getEntryPoints().size());

        // Test secondary effect condition creation for non-unique nodes
        testThreat = smq.getSystemThreat(tester.getStore(),
                "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#A.M.A6mD.1-A6mD_ef2fee92");
        assertEquals(2, testThreat.getSecondaryEffectConditions().size());

        // Test MS creation for non-unique nodes
		testThreat = smq.getSystemThreat(tester.getStore(),
				"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#A.M.A1oB.1-A1oB_ef2fee92");
		assertEquals(2, testThreat.getMisbehaviours().size());

       /* String query = String.format("SELECT DISTINCT ?s ?p ?o WHERE { GRAPH <%s> {"
 				+ " ?s ?p ?o .\n"
 				//+ " ?s a <http://it-innovation.soton.ac.uk/ontologies/trustworthiness/core#TrustworthinessAttributeSet> .\n"
 				+ "}}", tester.getGraph("system"));
        List<Map<String, String>> results = smq.testQuery(query, tester.getStore());
 		logger.debug("Results: {}", results.size());
 		for (Map<String, String>  result : results) {
 			if (result.toString().contains("core#Trust")) {
 	 			logger.debug(result.toString());
 			}
 		}*/

        /*Map<String, Asset> assets = smq.getSystemAssets(tester.getStore());
        List<String> sortAssets = new ArrayList<>();
        for (Asset asset : assets.values()) {
            sortAssets.add(asset.getLabel());
        }
        Collections.sort(sortAssets);*/

        /*Map<String, Asset> assets = smq.getSystemAssets(tester.getStore());
        List<String> sortAssets = new ArrayList<>();
        for (Asset asset : assets.values()) {
            sortAssets.add(asset.getUri());
        }
        Collections.sort(sortAssets);*/

        /*logger.debug("--- CS ---");
    	for (ControlSet cs : smq.getControlSets(tester.getStore()).values()) {
    		String csString = cs.getUri();
    		csString = csString.replace(csString.split("-")[csString.split("-").length-1], "") +
    				assets.get(cs.getAssetUri()).getLabel();

    		String string = cs.isAssertable() + ", "+ cs.isProposed() + "," +
    					cs.getControl();

    		logger.debug("{}: {}", csString, string);
    	}*/

        Map<String, Threat> threats = smq.getSystemThreats(tester.getStore());
    	/*logger.debug("--- CSG ---");
		for (ControlStrategy csg : smq.getControlStrategies(tester.getStore()).values()) {
			Threat threat = threats.get(csg.getThreat());
    		String key = threat.getLabel() + " " + csg.getLabel();
    		String val = csg.getBlockingEffect() + " " + csg.getControlSets();
    		String line = key + ": " + val;
			logger.debug(line.replaceAll("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#", ""));
		}*/
        /*for (Threat threat : threats.values()) {
        	//logger.debug("{} {}", threat.getLabel(), smq.getControlStrategiesForThreat(tester.getStore(), threat.getUri()));
        	for (ControlStrategy csg : threat.getControlStrategies().values()) {
    			String key = threat.getLabel() + " " + csg.getLabel();
    			String val = " ";
        		logger.debug("{}: {}", key, val);
        	}
        }*/

        /*logger.debug("--- TWAS ---");
        for (String assetUri : sortAssets) {
        	Asset asset = assets.get(assetUri);
        	for (TrustworthinessAttributeSet twas : asset.getTrustworthinessAttributeSets().values()) {
        		String string = asset.getLabel() + " ";
        		//string += twas.getUri().replace("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#", "") + ": ";
        		string += twas.getAttribute().getLabel() + ": ";
        		string += (twas.getAssertedTWLevel() != null ? twas.getAssertedTWLevel().getLabel() : "null") + ", ";
        		string += (twas.getInferredTWLevel() != null ? twas.getInferredTWLevel().getLabel() : "null") + ", ";
        		String ms = twas.getCausingMisbehaviourSet() != null ? twas.getCausingMisbehaviourSet().replace("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/", "") : "null";
        		if (!ms.equals("null")) {
        			ms = ms.replace(ms.split("-")[ms.split("-").length-1], "");
        		}
        		string += ms;
        		logger.debug(string);
        	}
        }*/
        /*logger.debug("--- MS ---");
        for (String assetUri : sortAssets) {
        	Asset asset = assets.get(assetUri);
        	for (MisbehaviourSet ms : asset.getMisbehaviourSets().values()) {
        		String string = asset.getLabel() + " ";
        		//string += twas.getUri().replace("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#", "") + ": ";
        		String msString = ms.getLabel();
        		msString = msString.replace(msString.split("-")[msString.split("-").length-1], "");
        		string += msString + ": ";
        		string += ms.toString();
        		logger.debug(string);
        	}
        }*/

        List<String> sortThreats = new ArrayList<>();
        for (Threat threat : threats.values()) {
            sortThreats.add(threat.getLabel());
        }
        Collections.sort(sortThreats);

        /*logger.debug("\n");
        logger.debug("--- Threats ---");
        for (String threat : sortThreats) {
        	logger.debug(threat);
        }*/



        // TODO: Finish
        /*assertEquals(new Integer(4), threatTypeCounts.get("I.A.HLSI.1"));
        assertEquals(new Integer(2), threatTypeCounts.get("LSg.A.LSgH.1"));
        assertEquals(new Integer(1), threatTypeCounts.get("H.A.HnP.1"));
        assertEquals(new Integer(2), threatTypeCounts.get("H.A.HPPd-u.1"));
        assertEquals(new Integer(2), threatTypeCounts.get("H.A.HmP.1"));
        assertEquals(new Integer(2), threatTypeCounts.get("H.A.HoP.1"));*/
        
        logger.debug("Threats: {}", threats.size());
		logger.debug("Assets: {}", smq.getSystemAssets(tester.getStore()).size());
		logger.debug("MisbehaviourSets: {}",  smq.getMisbehaviourSets(tester.getStore(), false).size()); //no need for causes here
		logger.debug("TrustworthinessAttributeSets: {}",  smq.getTrustworthinessAttributeSets(tester.getStore()).size());
		logger.debug("ControlSets: {}", smq.getControlSets(tester.getStore()).size());
		logger.debug("ControlStrategies: {}", smq.getControlStrategies(tester.getStore()).size());

		for (Relation relation :smq.getSystemRelations(tester.getStore())) {
			logger.debug("{}->{}: {}, {}", relation.getFrom(), relation.getTo(), relation.getSourceCardinality(), relation.getTargetCardinality());
		}

        tester.exportTestModel("build/build/test-results/" + name.getMethodName(), false, false, true);
    } catch (Exception e) {
		e.printStackTrace();
		fail("Exception thrown by design time validator: " + e.getMessage());
    }
    }

	/**
	 * Test validation logic related to construction patterns.
	 */
	@Test
    public void testConstructionPatterns() {
		try {
			tester.switchModels(6, 7);
			setReasoner(new NullReasoner());

			// Make sure the model is not validated
			tester.getStore().clearGraph(tester.getGraph("system-inf"));

			dv.validateDesigntimeModel(new Progress("test"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception thrown by design time validator: " + e.getMessage());
		}

		// --- Case: check creation of asset without INS included at node ---

		Map<String, Asset> assets = smq.getSystemAssets(tester.getStore());

		// Check that the system#F asset was created correctly
		Asset assetF = assets.get(tester.getNS("system")+"F");
		assertNotNull(assetF);

		// Sort relations by their 'from' asset, for the purposes of this test case
		Map<String, List<Relation>> relationsByAssetURI = new HashMap<>();
		for (Relation relation : smq.getSystemRelations(tester.getStore())) {
			List<Relation> relations = relationsByAssetURI.get(relation.getFrom());
			if (relations == null) {
				relations = new ArrayList<>();
				relationsByAssetURI.put(relation.getFrom(), relations);
			}
			relations.add(relation);
		}

		// Check that all B and C type assets have an r16 relationship to the system#F asset
		String assetTypeB = tester.getNS("domain")+"B";
		String assetTypeC = tester.getNS("domain")+"C";
		String relationType = tester.getNS("domain")+"r16";
		for (Asset asset : assets.values()) {
			if (asset.getType().equals(assetTypeB) || asset.getType().equals(assetTypeC)) {
				boolean hasLinkToF = false;
				for (Relation relation : relationsByAssetURI.get(asset.getUri())) {
					if (relation.getTo().equals(assetF.getUri()) && relation.getType().equals(relationType)) {
						hasLinkToF = true;
						break;
					}
				}
				assertTrue(hasLinkToF);
			}
		}
	}
	
	@Test
	public void testRevalidateModel() {
    try{
  		tester.switchModels(2, 3);
  		setReasoner(new NullReasoner());

  		String twasURI = tester.getNS("system") + "TWAS-UserTW-ece77c11";
  		String msURI = tester.getNS("system") + "MS-LossOfAvailability-79c22efc";

  		//check original levels
  		TrustworthinessAttributeSet twas = smq.getTrustworthinessAttributeSet(tester.getStore(), twasURI);
  		assertNotNull(twas);
  		assertEquals(tester.getNS("domain") + "TrustworthinessLevelVeryHigh", twas.getAssertedTWLevel().getUri());
  		MisbehaviourSet ms = smq.getMisbehaviourSet(tester.getStore(), msURI, false); //no need for causes here
  		assertNotNull(ms);
  		assertEquals(tester.getNS("domain") + "ImpactLevelVeryLow", ms.getImpactLevel().getUri());

  		//update levels
  		Level atwl = new Level();
  		atwl.setUri(tester.getNS("domain") + "TrustworthinessLevelHigh");
  		atwl.setLabel("High");
  		atwl.setValue(3);
  		twas.setAssertedTWLevel(atwl);
  		logger.debug("{} {}", tester.getStore(), twas);
  		smu.updateTWAS(tester.getStore(), twas);
  		twas = smq.getTrustworthinessAttributeSet(tester.getStore(), twasURI);
  		assertEquals(tester.getNS("domain") + "TrustworthinessLevelHigh", twas.getAssertedTWLevel().getUri());

  		Level ail = new Level();
  		ail.setUri(tester.getNS("domain") + "ImpactLevelHigh");
  		ail.setLabel("High");
  		ail.setValue(3);
  		ms.setImpactLevel(ail);
  		smu.updateMS(tester.getStore(), ms);
  		ms = smq.getMisbehaviourSet(tester.getStore(), msURI, false); //no need for causes here
  		assertEquals(tester.getNS("domain") + "ImpactLevelHigh", ms.getImpactLevel().getUri());

  		//revalidate
  		dv.validateDesigntimeModel(new Progress("test"));

  		//check if the asserted levels have been preserved
  		twas = smq.getTrustworthinessAttributeSet(tester.getStore(), twasURI);
          assertEquals(tester.getNS("domain") + "TrustworthinessLevelHigh", twas.getAssertedTWLevel().getUri());
  		ms = smq.getMisbehaviourSet(tester.getStore(), msURI, false); //no need for causes here
  		assertEquals(tester.getNS("domain") + "ImpactLevelHigh", ms.getImpactLevel().getUri());
    } catch (Exception e) {
		e.printStackTrace();
		fail("Exception thrown by design time validator: " + e.getMessage());
    }
	}

	/**
	 * Tests that links are correctly created between root nodes and secondary nodes in construction patterns.
	 * DOMAIN MODEL: domain-network-testing (test created on: TESTING-2a4-1.nq.gz)
	 * SYSTEM MODEL: ConstructionPatternTest-v2a.nq.gz
	 */
	@Test
	public void testConstructionLinkBetweenRootAndSecondary() {
		tester.switchModels(5, 6);

		//make sure the model is NOT validated - there's a separate test for revalidating
		tester.getStore().clearGraph(tester.getGraph("system-inf"));

		try {
			dv.validateDesigntimeModel(new Progress("test"));

			Map<String, Map<String, Threat>> threatsByType = new HashMap<>();
			for (Threat threat : smq.getSystemThreats(tester.getStore()).values()) {
				Map<String, Threat> threats = threatsByType.get(threat.getType());
				if (threats == null) {
					threats = new HashMap<>();
					threatsByType.put(threat.getType(), threats);
				}
				threats.put(threat.getUri(), threat);
			}

			Map<String, Threat> testThreats = threatsByType.get(
					"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#E.M.B9E.1");

			// Check (C1)-[r09]->(E-C1-D1)
			assertTrue(testThreats.containsKey(
					"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#E.M.B9E.1-B9E_d3ceb3a3_6937c94e"));
			// Check (B1)-[r09]->(E-D1)
			assertTrue(testThreats.containsKey(
					"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#E.M.B9E.1-B9E_c908315f_cff7e9eb"));
			// Check (B2)-[r09]->(E-D1)
			assertTrue(testThreats.containsKey(
					"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#E.M.B9E.1-B9E_50fbe66e_cff7e9eb"));
			// Check (C1)-[r09]->(E-D1)
			assertTrue(testThreats.containsKey(
					"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#E.M.B9E.1-B9E_d3ceb3a3_cff7e9eb"));

			assertEquals(4, threatsByType.get(
					"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#E.M.B9E.1").size());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception thrown by design time validator: " + e.getMessage());
		}
	}

	/**
	 * Test aspects of asset and relationship cardinality inference
	 *
	 * DOMAIN: TESTING-2b1-1.nq.gz
	 * SYSTEM: ConstructionPatternTest_v2_-_v2b.nq.gz
	 */
	/*
	 * @Ignore("This test fails for the population supporting validator but we don't yet know why. Manual tests via the SSM GUI show the correct behaviour.")
	 */
	@Test
	public void testCardinalityInference() {
		tester.switchModels(6, 8);

		setReasoner(new NullReasoner());

		try {
			logger.info("Validating model");
			dv.validateDesigntimeModel(new Progress("test"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception thrown by design time validator: " + e.getMessage());
		}

		logger.info("Creating a querierDB object to access the validated model");
		IQuerierDB querierDB = new JenaQuerierDB(dataset, tester.getModel(), true);
		querierDB.init();
		
		// Get the population levels
		logger.info("Getting the population scale");
		Map<String, LevelDB> poLevels = querierDB.getPopulationLevels();

		// Get the assets and links we need
		Map<String, AssetDB> assets = querierDB.getAssets("system", "system-inf");
		for(AssetDB a : assets.values()) {
			String assetURI = a.getUri();
			String assetLabel = a.getLabel();
			String assetID = a.getId();
			String assetPop = a.getPopulation();
			Integer assetPopLevel = poLevels.get(assetPop).getLevelValue();
			logger.debug("Asset {} has label {}, id {}, population {} (level {})", assetURI, assetLabel, assetID, assetPop, assetPopLevel);
		}
		Map<String, CardinalityConstraintDB> ccs = querierDB.getCardinalityConstraints("system", "system-inf");
		for(CardinalityConstraintDB cc : ccs.values()) {
			logger.debug("Link {} is {}-{}-{}, has cardinality [{}]-to-[{}]", 
					cc.getUri(), 
					assets.get(cc.getLinksFrom()).getLabel(),
					cc.getLinkType().replace("domain#", ""),
					assets.get(cc.getLinksTo()).getLabel(),
					cc.getSourceCardinality(), cc.getTargetCardinality());
		}

		String populationURI;
		int populationLevel;
		int sourceCardinality;
		int targetCardinality;

		// B1 -r07-> D1: asserted link from non-singleton B1 to singleton D1, should be *-to-1
		CardinalityConstraintDB b1_r07_d1 = querierDB.getCardinalityConstraint("system#c908315f-r07-9402e213", "system", "system-inf");
		sourceCardinality = b1_r07_d1.getSourceCardinality();
		targetCardinality = b1_r07_d1.getTargetCardinality();
		assertEquals(-1, sourceCardinality);
		assertEquals(1, targetCardinality);

		// C1 -r13-> D2: asserted link from singleton C1 to non-singleton D2, should be 1-to-*
		CardinalityConstraintDB c1_r13_d2 = querierDB.getCardinalityConstraint("system#d3ceb3a3-r13-2e25c3f8", "system", "system-inf");
		sourceCardinality = c1_r13_d2.getSourceCardinality();
		targetCardinality = c1_r13_d2.getTargetCardinality();
		assertEquals(1, sourceCardinality);
		assertEquals(-1, targetCardinality);

		// C1 -r07-> D1: asserted link from singleton C1 to singleton D1, should be 1-to-1
		CardinalityConstraintDB c1_r07_d1 = querierDB.getCardinalityConstraint("system#d3ceb3a3-r07-9402e213", "system", "system-inf");
		sourceCardinality = c1_r07_d1.getSourceCardinality();
		targetCardinality = c1_r07_d1.getTargetCardinality();
		assertEquals(1, sourceCardinality);
		assertEquals(1, targetCardinality);

		// B2 -r07-> D1: asserted link from singleton B2 to singleton D1, should be 1-to-1
		CardinalityConstraintDB b2_r07_d1 = querierDB.getCardinalityConstraint("system#50fbe66e-r07-9402e213", "system", "system-inf");
		sourceCardinality = b2_r07_d1.getSourceCardinality();
		targetCardinality = b2_r07_d1.getTargetCardinality();
		assertEquals(1, sourceCardinality);
		assertEquals(1, targetCardinality);

		// Asset E-C1-D2: INS are C1 (1, 1) -> Singleton, and D1 (1, 2) -> Few, inferred E-C1-D2 should be (1, 2) -> Few
		AssetDB e_c1_d2 = querierDB.getAsset("system#E_d3ceb3a3_2e25c3f8", "system", "system-inf");
		populationURI = e_c1_d2.getPopulation();
		populationLevel = querierDB.getPopulationLevel(populationURI).getLevelValue();
		assertEquals(1, populationLevel);

		// E-C1-D2 -r10-> D2: inferred link from non-singleton E-C1-D2 to non-singleton D2, should be 1-to-1
		CardinalityConstraintDB e_c1_d2_r10_d2 = querierDB.getCardinalityConstraint("system#b5b3ffda-r10-2e25c3f8", "system", "system-inf");
		sourceCardinality = e_c1_d2_r10_d2.getSourceCardinality();
		targetCardinality = e_c1_d2_r10_d2.getTargetCardinality();
		assertEquals(1, sourceCardinality);
		assertEquals(1, targetCardinality);

		// C1 -r14-> E-C1-D2: inferred link from singleton C1 to non-singleton E-C1-D2, should be 1-to-*
		CardinalityConstraintDB c1_r14_e_c1_d2 = querierDB.getCardinalityConstraint("system#d3ceb3a3-r14-b5b3ffda", "system", "system-inf");
		sourceCardinality = c1_r14_e_c1_d2.getSourceCardinality();
		targetCardinality = c1_r14_e_c1_d2.getTargetCardinality();
		assertEquals(1, sourceCardinality);
		assertEquals(-1, targetCardinality);

		// C1 -r11-> E-C1-D2: inferred link from singleton C1 to non-singleton E-C1-D2, should be 1-to-*
		CardinalityConstraintDB c1_r11_e_c1_d2 = querierDB.getCardinalityConstraint("system#d3ceb3a3-r11-b5b3ffda", "system", "system-inf");
		sourceCardinality = c1_r11_e_c1_d2.getSourceCardinality();
		targetCardinality = c1_r11_e_c1_d2.getTargetCardinality();
		assertEquals(1, sourceCardinality);
		assertEquals(-1, targetCardinality);

		// Asset E-D1: INS is only singleton D1, E-D1 should be singleton
		AssetDB e_d1 = querierDB.getAsset("system#E_9402e213", "system", "system-inf");
		populationURI = e_d1.getPopulation();
		populationLevel = querierDB.getPopulationLevel(populationURI).getLevelValue();
		assertEquals(0, populationLevel);

		// E-D1 -r10-> D1: inferred link from singleton E-D1 to singleton D1, should be 1-to-1
		CardinalityConstraintDB e_d1_r10_d1 = querierDB.getCardinalityConstraint("system#cff7e9eb-r10-9402e213", "system", "system-inf");
		sourceCardinality = e_d1_r10_d1.getSourceCardinality();
		targetCardinality = e_d1_r10_d1.getTargetCardinality();
		assertEquals(1, sourceCardinality);
		assertEquals(1, targetCardinality);

		// B2 -r09-> E-D1: inferred link from singleton B2 to singleton E-D1, should be 1-to-1
		CardinalityConstraintDB b2_r09_e_d1 = querierDB.getCardinalityConstraint("system#50fbe66e-r09-cff7e9eb", "system", "system-inf");
		sourceCardinality = b2_r09_e_d1.getSourceCardinality();
		targetCardinality = b2_r09_e_d1.getTargetCardinality();
		assertEquals(1, sourceCardinality);
		assertEquals(1, targetCardinality);

		// B1 -r09-> E-D1: inferred link from non-singleton B1 to singleton E-D1, should be *-to-1
		CardinalityConstraintDB b1_r09_e_d1 = querierDB.getCardinalityConstraint("system#c908315f-r09-cff7e9eb", "system", "system-inf");
		sourceCardinality = b1_r09_e_d1.getSourceCardinality();
		targetCardinality = b1_r09_e_d1.getTargetCardinality();
		assertEquals(-1, sourceCardinality);
		assertEquals(1, targetCardinality);
	}

	// New unit tests for bugs we found ///////////////////////////////////////////////////////////////////////////////
	@Test@Ignore
	//check whether control strategies get created correctly (i.e. only if the relevant control sets exist)
	public void testCreateCSGs() {
    try{
  		tester.switchModels(1, 1);

  		//make sure the model is NOT validated - there's a separate test for revalidating
  		tester.getStore().clearGraph(tester.getGraph("system-inf"));

  		dv.validateDesigntimeModel(new Progress("test"));

  		//check whether the correct CSGs exist
  		assertEquals(219, smq.getControlStrategies(tester.getStore()).size());
    } catch (Exception e) {
		e.printStackTrace();
		fail("Exception thrown by design time validator: " + e.getMessage());
    }
	}

/*
    @Test
	public void testSubProperties() {

		tester.switchModels(3, 4);

		//make sure the model is NOT validated
		tester.getStore().clearGraph(tester.getGraph("system-inf"));

		//create a single root pattern
        String uri = tester.getNS("domain") + "R-3a94024c-933a82e8-e3008e31";
		dv.createRootPattern(uri);

        Map<String, Pattern> result = smq.getRootPatterns(tester.getStore(), uri);
        assertEquals(2, result.size());

        //create the matching pattern
        String mp = tester.getNS("domain") + "PD";
        dv.createMatchingPattern(mp);

        result = smq.getSystemPatterns(tester.getStore(), "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#8dd9f99e");
        logger.debug("{}", result);

    }
*/
}
