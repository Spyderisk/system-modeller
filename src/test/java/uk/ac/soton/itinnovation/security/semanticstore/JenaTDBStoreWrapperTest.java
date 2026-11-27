/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2014
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
//      Created By :            Stefanie Wiegand
//      Created Date :          2014-10-16
//      Created for Project :   OPTET
//		Modified for Project :	ASSURED
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.semanticstore;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.semanticstore.IStoreWrapper.Format;
import uk.ac.soton.itinnovation.security.semanticstore.util.Triple;
import uk.ac.soton.itinnovation.security.semanticstore.util.Triple.TripleType;

import static java.util.stream.Collectors.toList;

@RunWith(JUnit4.class)
public class JenaTDBStoreWrapperTest extends TestCase {

	private AStoreWrapper store;
	private static Logger logger;

	private static final String GRAPH_A = "http://example.com/a";
	private static final String GRAPH_B = "http://example.com/b";
	private static final String GRAPH_C = "http://example.com/c";

	@Rule public TestName name = new TestName();
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	private File jenaFolder;
	
	@BeforeClass
	public static void beforeClass() {
		logger = LoggerFactory.getLogger(JenaTDBStoreWrapperTest.class);
		logger.info("JenaTDBStoreWrapper tests executing...");
	}

	@Before
	public void beforeEachTest() throws IOException {
		logger.info("Running test {}", name.getMethodName());
		
		//FileUtils.deleteDirectory(new File("jena_tdb"));
		jenaFolder = folder.newFolder();
		store = new JenaTDBStoreWrapper(jenaFolder.toString());
		
	}

	@After
	public void afterEachTest() {
		try {
			if (store !=null) {

				store.getGraphs().forEach(g -> store.clearGraph(g));

				store.clearGraph("http://example.com/bm5/");
				store.clearGraph("http://example.com/bm25/");
				store.clearGraph("http://example.com/bm50/");

				//tidy up
				store.clearGraph(GRAPH_A);
				store.clearGraph(GRAPH_B);
				store.clearGraph(GRAPH_C);

				store.clearDefaultGraph();
				store = null;
			}
		} catch (Exception e) {
			logger.error("Error destroying JenaTDBStoreWrapper", e);
		}
	}

	// Tests //////////////////////////////////////////////////////////////////////////////////////

	@Test
	public void testRealQuery() {
		store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/domain-assured.rdf").getPath(),
			"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-assured", Format.RDF);
		store.load(getClass().getClassLoader().getResource("semanticstore/ASSURED-Q6-validated.nq").getPath());

		String graph = "http://it-innovation.soton.ac.uk/user/594a6bd21719e03c4c38ccbd/system/594a6d7b1719e03d24fcf010/ASSURED-Q6demo";

		String sparql = "PREFIX  core: <http://it-innovation.soton.ac.uk/ontologies/trustworthiness/core#>\n" +
		"PREFIX  system: <http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#>\n" +
		"PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
		"PREFIX  owl:  <http://www.w3.org/2002/07/owl#>\n" +
		"PREFIX  spin: <http://spinrdf.org/spin#>\n" +
		"PREFIX  domain: <http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#>\n" +
		"PREFIX  xsd:  <http://www.w3.org/2001/XMLSchema#>\n" +
		"PREFIX  fn:   <http://www.w3.org/2005/xpath-functions#>\n" +
		"PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
		"PREFIX  sp:   <http://spinrdf.org/sp#>\n" +
		"PREFIX  spl:  <http://spinrdf.org/spl#>" +
		"SELECT DISTINCT  ?a (str(?minC) AS ?min) (str(?maxC) AS ?max) ?aid ?label ?type (str(?xp) AS ?x) (str(?yp) AS ?y) ?asserted ?visible ?cs ?ci ?cl ?proposed ?ia\n" +
		"WHERE\n" +
		"  { ?a  rdf:type    ?type ;\n" +
		"        core:hasID  ?aid .\n" +
		"    ?type (rdfs:subClassOf)* core:Asset\n" +
		"    GRAPH <" + graph + "/ui>\n" +
		"      { OPTIONAL\n" +
		"          { ?a  core:positionX  ?xp ;\n" +
		"                core:positionY  ?yp\n" +
		"          }\n" +
		"      }\n" +
		"    OPTIONAL\n" +
		"      { ?a  rdfs:label  ?label }\n" +
		"    OPTIONAL\n" +
		"      { ?a  core:minCardinality  ?minC }\n" +
		"    OPTIONAL\n" +
		"      { ?a  core:maxCardinality  ?maxC }\n" +
		"    OPTIONAL\n" +
		"      { ?a  core:createdByPattern  ?createdBy }\n" +
		"    OPTIONAL\n" +
		"      { ?ia  core:displayedAtAsset  ?a }\n" +
		"    BIND(str(if(bound(?createdBy), false, true)) AS ?asserted)\n" +
		"    OPTIONAL\n" +
		"      { ?type  core:isVisible  ?vis }\n" +
		"    BIND(str(if(bound(?vis), if(( ?vis = true ), true, false), ?asserted)) AS ?visible)\n" +
		"    OPTIONAL\n" +
		"      { ?cs  rdf:type         core:ControlSet ;\n" +
		"             core:locatedAt   ?a ;\n" +
		"             core:hasControl  ?ci\n" +
		"        OPTIONAL\n" +
		"          { ?cs  core:isProposed  ?prop }\n" +
		"        ?ci  rdf:type  ?c .\n" +
		"        ?c  (rdfs:subClassOf)* core:Control .\n" +
		"        ?c  rdfs:label  ?cl\n" +
		"        BIND(str(if(bound(?prop), ?prop, false)) AS ?proposed)\n" +
		"      }\n" +
		"  }";

		List<Map<String, String>> result = store.translateSelectResult(store.querySelect(sparql,
			"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-assured",
			graph, graph + "/inf", graph + "/ui"
		));
		assertEquals(334, result.size());
	}

	@Test
	public void testCount() {
		store.update("INSERT DATA {\n" +
			"	GRAPH <" + GRAPH_A + "> {" +
			"		<http://example.com/a> <http://example.com/b> <http://example.com/c> ." +
			"		<http://example.com/d> <http://example.com/e> <http://example.com/f> ." +
			"		<http://example.com/g> <http://example.com/h> <http://example.com/i> ." +
			"	}\n" +
			"	GRAPH <" + GRAPH_B + "> {" +
			"		<http://example.com/g> <http://example.com/h> <http://example.com/i> ." +
			"		<http://example.com/j> <http://example.com/k> <http://example.com/l> ." +
			"	}\n" +
			"	GRAPH <" + "urn:x-arq:DefaultGraph" + "> {" +
			"		<http://example.com/m> <http://example.com/n> <http://example.com/o> ." +
			"	}\n" +
			"	<http://example.com/p> <http://example.com/q> <http://example.com/r> ." +
			"}");

		assertEquals(3, store.getCount(GRAPH_A));
		assertEquals(2, store.getCount(GRAPH_B));
		assertEquals(2, store.getCount("urn:x-arq:DefaultGraph"));
		assertEquals(2, store.getCountDefault());
		assertEquals(7, store.getCount());
		assertEquals(0, store.getCount("NONEXISTENT_GRAPH"));
		
		
		// Test exception cases.
		Assertions.assertThatThrownBy(() -> {
			store.getCount(new String[] {null});}).isInstanceOf(RuntimeException.class);
		Assertions.assertThatThrownBy(() -> {
			store.getCount(GRAPH_A, null, GRAPH_B);}).isInstanceOf(RuntimeException.class);
	}

	@Test
	public void testClear() {
		store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm5.rdf").getPath(),
			"http://example.com/bm5/", Format.RDF);
		store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm25.rdf").getPath(),
			"http://example.com/bm25/", Format.RDF);
		assertEquals(436, store.getCount("http://example.com/bm5/", "http://example.com/bm25/"));

		store.clear();
		assertEquals(0, store.getCount("http://example.com/bm5/"));
		assertEquals(0, store.getCount("http://example.com/bm25/"));
		assertEquals(0, store.getCountDefault());
		assertEquals(0, store.getCount());
	}

	@Test
	public void testUpdate() {
		long aGraphSize;
		long defaultGraphSize;	
		

		// ------ Test 1: Inserting of data ------
		
		logger.info("Test 1: Testing inserting of data.");
		store.clearGraph(GRAPH_A);
		store.clearDefaultGraph();

		aGraphSize = store.getCount(GRAPH_A);
		defaultGraphSize = store.getCountDefault();

		//adds triple to default graph
		store.update("INSERT DATA {" +
			"	<http://example.com/a1> <http://example.com/b1> <http://example.com/c1> ." +
			"}", "urn:x-arq:DefaultGraph");
		assertEquals(aGraphSize, store.getCount(GRAPH_A));
		assertEquals(defaultGraphSize + 1, store.getCountDefault());
		aGraphSize = store.getCount(GRAPH_A);
		defaultGraphSize = store.getCountDefault();

		//adds triple to graph a
		store.update("INSERT DATA { GRAPH <" + GRAPH_A + "> {" +
			"	<http://example.com/a3> <http://example.com/b3> <http://example.com/c3> ." +
			"}}");
		assertEquals(aGraphSize + 1, store.getCount(GRAPH_A));
		assertEquals(defaultGraphSize, store.getCountDefault());
		aGraphSize = store.getCount(GRAPH_A);
		defaultGraphSize = store.getCountDefault();
		//store.printSizes();

		//adds triple to graph a: the graph is given as well, which does not change where the GRAPH block writes to
		store.update("INSERT DATA { GRAPH <" + GRAPH_A + "> {" +
			"	<http://example.com/a4> <http://example.com/b4> <http://example.com/c4> ." +
			"}}", GRAPH_A);
		assertEquals(aGraphSize + 1, store.getCount(GRAPH_A));
		assertEquals(defaultGraphSize, store.getCountDefault());
		aGraphSize = store.getCount(GRAPH_A);
		defaultGraphSize = store.getCountDefault();
		
		
		
		// ------ Test 2: Deleting of data ------
		
		logger.info("Test 2: Deleting of data.");
		store.clearGraph(GRAPH_A);
		store.clearDefaultGraph();

		//set up store
		store.update("INSERT DATA { GRAPH <" + GRAPH_A + "> {" +
			"	<http://example.com/a5> <http://example.com/b5> <http://example.com/c5> ." +
			"	<http://example.com/a6> <http://example.com/b6> <http://example.com/c6> ." +
			"	<http://example.com/a7> <http://example.com/b7> <http://example.com/c7> ." +
			"	<http://example.com/a8> <http://example.com/b8> <http://example.com/c8> ." +
			"}}");
		store.update("INSERT DATA { GRAPH <" + "urn:x-arq:DefaultGraph" + "> {" +
			"	<http://example.com/a5> <http://example.com/b5> <http://example.com/c5> ." +
			"	<http://example.com/a6> <http://example.com/b6> <http://example.com/c6> ." +
			"	<http://example.com/a7> <http://example.com/b7> <http://example.com/c7> ." +
			"	<http://example.com/a8> <http://example.com/b8> <http://example.com/c8> ." +
			"}}");

		aGraphSize = store.getCount(GRAPH_A);
		defaultGraphSize = store.getCountDefault();

		//deletes triple from default graph
		store.update("DELETE DATA {" +
			"	<http://example.com/a5> <http://example.com/b5> <http://example.com/c5> ." +
			"}");
		assertEquals(aGraphSize, store.getCount(GRAPH_A));
		assertEquals(defaultGraphSize-1, store.getCountDefault());

		//deletes triple from graph a
		store.update("DELETE DATA { GRAPH <" + GRAPH_A + "> {" +
			"	<http://example.com/a7> <http://example.com/b7> <http://example.com/c7> ." +
			"}}");
		assertEquals(aGraphSize - 1, store.getCount(GRAPH_A));
		assertEquals(defaultGraphSize-1, store.getCountDefault());

		//deletes triple from graph a: the graph is given as well, which does not change where the GRAPH block deletes from
		store.update("DELETE DATA { GRAPH <" + GRAPH_A + "> {" +
			"	<http://example.com/a8> <http://example.com/b8> <http://example.com/c8> ." +
			"}}", GRAPH_A);
		assertEquals(aGraphSize - 2, store.getCount(GRAPH_A));
		assertEquals(defaultGraphSize-1, store.getCountDefault());
		
		

		// ------ Test 3: DELETE/INSERT -----
		
		logger.info("Test 3");
		store.clearGraph(GRAPH_A);
		store.clearGraph(GRAPH_B);
		store.clearDefaultGraph();

		//set up store
		store.update("INSERT DATA {\n" +
			"	GRAPH <" + GRAPH_A + "> {" +
			"		<http://example.com/a> <http://example.com/b> <http://example.com/c> ." +
			"		<http://example.com/a> <http://example.com/b> <http://example.com/d> ." +
			"		<http://example.com/a> <http://example.com/b> <http://example.com/e> ." +
			"	}\n" +
			"	GRAPH <" + GRAPH_B + "> {" +
			"		<http://example.com/b> <http://example.com/f> <http://example.com/g> ." +
			"		<http://example.com/b> <http://example.com/f> <http://example.com/h> ." +
			"	}\n" +
			"	GRAPH <" + "urn:x-arq:DefaultGraph" + "> {" +
			"		<http://example.com/d> <http://example.com/b> <http://example.com/c> ." +
			"	}\n" +
			"	<http://example.com/d> <http://example.com/b> <http://example.com/f> ." +
			"}");

		aGraphSize = store.getCount(GRAPH_A);
		long bGraphSize = store.getCount(GRAPH_B);
		defaultGraphSize = store.getCountDefault();

		//delete/insert using graphs
		store.update("DELETE {\n" +
		"	GRAPH <" + GRAPH_A + "> {\n" +
		"		?a <http://example.com/b> ?o .\n" +
		"	}\n" +
		"} INSERT {\n" +
		"	GRAPH <" + GRAPH_A + "> {\n" +
		"		?a <http://example.com/f> ?o .\n" +
		"		?a <http://example.com/g> ?o .\n" +
		"	}\n" +
		"} WHERE {\n" +
		"	BIND (<http://example.com/a> AS ?a)\n" +
		"	GRAPH <" + GRAPH_A + "> {\n" +
		"		?a <http://example.com/b> ?o .\n" +
		"	}\n" +
		"}");
		assertEquals(aGraphSize * 2, store.getCount(GRAPH_A));
		assertEquals(bGraphSize, store.getCount(GRAPH_B));
		assertEquals(defaultGraphSize, store.getCountDefault());

		//delete/insert using graphs
		store.update("DELETE {\n" +
		"	GRAPH <" + GRAPH_A + "> {\n" +
		"		?a <http://example.com/b> ?o .\n" +
		"	}\n" +
		"} INSERT {\n" +
		"	GRAPH <" + GRAPH_A + "> {\n" +
		"		?a <http://example.com/f> ?o .\n" +
		"		?a <http://example.com/g> ?o .\n" +
		"	}\n" +
		"} WHERE {\n" +
		"	BIND (<http://example.com/a> AS ?a)\n" +
		"	GRAPH <" + GRAPH_A + "> {\n" +
		"		?a <http://example.com/b> ?o .\n" +
		"	}\n" +
		"}", GRAPH_A);
		assertEquals(aGraphSize * 2, store.getCount(GRAPH_A));
		assertEquals(bGraphSize, store.getCount(GRAPH_B));
		assertEquals(defaultGraphSize, store.getCountDefault());
		
		
		
		// ------ Test 4: Return value ------

		logger.info("Test 4: Testing the return value (whether the store was changed).");
		store.clearGraph(GRAPH_A);
		store.clearGraph(GRAPH_B);
		store.clearDefaultGraph();

		//adds a triple
		assertTrue(store.update("INSERT DATA {\n" +
			"	GRAPH <" + GRAPH_A + "> {" +
			"		<http://example.com/a> <http://example.com/b> <http://example.com/c> ." +
			"	}\n" +
			"}"));

		//the triple is already there
		assertFalse(store.update("INSERT DATA {\n" +
			"	GRAPH <" + GRAPH_A + "> {" +
			"		<http://example.com/a> <http://example.com/b> <http://example.com/c> ." +
			"	}\n" +
			"}"));

		//deletes a triple that is not there
		assertFalse(store.update("DELETE DATA {\n" +
			"	GRAPH <" + GRAPH_A + "> {" +
			"		<http://example.com/a> <http://example.com/b> <http://example.com/x> ." +
			"	}\n" +
			"}"));

		//nothing matches
		assertFalse(store.update("DELETE WHERE {\n" +
			"	GRAPH <" + GRAPH_A + "> {\n" +
			"		?s <http://example.com/none> ?o .\n" +
			"	}\n" +
			"}"));

		//replaces the triple: the number of triples stays the same, but the store is changed
		aGraphSize = store.getCount(GRAPH_A);
		assertTrue(store.update("DELETE {\n" +
		"	GRAPH <" + GRAPH_A + "> {\n" +
		"		?s <http://example.com/b> ?o .\n" +
		"	}\n" +
		"} INSERT {\n" +
		"	GRAPH <" + GRAPH_A + "> {\n" +
		"		?s <http://example.com/b> <http://example.com/d> .\n" +
		"	}\n" +
		"} WHERE {\n" +
		"	GRAPH <" + GRAPH_A + "> {\n" +
		"		?s <http://example.com/b> ?o .\n" +
		"	}\n" +
		"}"));
		assertEquals(aGraphSize, store.getCount(GRAPH_A));

		//clears an empty graph
		assertFalse(store.update("CLEAR SILENT GRAPH <" + GRAPH_B + ">"));

		//clears a graph
		store.update("INSERT DATA {\n" +
			"	GRAPH <" + GRAPH_B + "> {" +
			"		<http://example.com/a> <http://example.com/b> <http://example.com/c> ." +
			"	}\n" +
			"}");
		assertTrue(store.update("CLEAR GRAPH <" + GRAPH_B + ">"));

		//drops a graph
		store.update("INSERT DATA {\n" +
			"	GRAPH <" + GRAPH_B + "> {" +
			"		<http://example.com/a> <http://example.com/b> <http://example.com/c> ." +
			"	}\n" +
			"}");
		assertTrue(store.update("DROP GRAPH <" + GRAPH_B + ">"));



		// ------ Test 5: Graphs given to an update ------

		logger.info("Test 5: Testing the graphs given to an update.");
		store.clearGraph(GRAPH_A);
		store.clearGraph(GRAPH_B);
		store.clearGraph(GRAPH_C);
		store.clearDefaultGraph();

		//set up store
		store.update("INSERT DATA {\n" +
			"	GRAPH <" + GRAPH_A + "> {" +
			"		<http://example.com/a> <http://example.com/b> <http://example.com/c> ." +
			"		<http://example.com/a> <http://example.com/b> <http://example.com/d> ." +
			"	}\n" +
			"	GRAPH <" + GRAPH_B + "> {" +
			"		<http://example.com/e> <http://example.com/b> <http://example.com/f> ." +
			"	}\n" +
			"}");

		aGraphSize = store.getCount(GRAPH_A);
		bGraphSize = store.getCount(GRAPH_B);
		defaultGraphSize = store.getCountDefault();

		//the graph is only used in a WHERE clause, so it does not decide where INSERT DATA writes to: the default graph
		store.update("INSERT DATA {" +
			"	<http://example.com/g> <http://example.com/h> <http://example.com/i> ." +
			"}", GRAPH_A);
		assertEquals(aGraphSize, store.getCount(GRAPH_A));
		assertEquals(defaultGraphSize + 1, store.getCountDefault());
		defaultGraphSize = store.getCountDefault();

		//one graph: the WHERE clause reads this graph (like for any number of graphs), the template writes to graph c
		store.update("INSERT {\n" +
		"	GRAPH <" + GRAPH_C + "> {\n" +
		"		?s <http://example.com/q> ?o .\n" +
		"	}\n" +
		"} WHERE {\n" +
		"	?s <http://example.com/b> ?o .\n" +
		"}", GRAPH_A);
		assertEquals(2, store.getCount(GRAPH_C));
		assertEquals(aGraphSize, store.getCount(GRAPH_A));
		assertEquals(bGraphSize, store.getCount(GRAPH_B));
		assertEquals(defaultGraphSize, store.getCountDefault());

		//two graphs: the WHERE clause reads both
		store.clearGraph(GRAPH_C);
		store.update("INSERT {\n" +
		"	GRAPH <" + GRAPH_C + "> {\n" +
		"		?s <http://example.com/q> ?o .\n" +
		"	}\n" +
		"} WHERE {\n" +
		"	?s <http://example.com/b> ?o .\n" +
		"}", GRAPH_A, GRAPH_B);
		assertEquals(3, store.getCount(GRAPH_C));
		
		
		
		// Test exception cases.
		Assertions.assertThatThrownBy(() -> {
			store.update(null);}).isInstanceOf(RuntimeException.class);
		Assertions.assertThatThrownBy(() -> {
			store.update("INVALID QUERY");}).isInstanceOf(RuntimeException.class);
		Assertions.assertThatThrownBy(() -> {
			store.update("INSERT DATA {" +
					"	<http://example.com/a1> <http://example.com/b1> <http://example.com/c1> ." +
					"}", new String[] {null});}).isInstanceOf(RuntimeException.class);
	}

	@Test
	public void testUpdateInGraph() {
		long aGraphSize;
		long defaultGraphSize;
		

		// ------ Test 1: Inserting of data ------
		
		logger.info("Test 1: Testing inserting of data in a graph.");
		store.clearGraph(GRAPH_A);
		store.clearDefaultGraph();

		aGraphSize = store.getCount(GRAPH_A);
		defaultGraphSize = store.getCountDefault();

		//adds triple to default graph
		assertTrue(store.updateInGraph("INSERT DATA {" +
			"	<http://example.com/a1> <http://example.com/b1> <http://example.com/c1> ." +
			"}", "urn:x-arq:DefaultGraph"));
		assertEquals(aGraphSize, store.getCount(GRAPH_A));
		assertEquals(defaultGraphSize + 1, store.getCountDefault());
		aGraphSize = store.getCount(GRAPH_A);
		defaultGraphSize = store.getCountDefault();

		//adds triple to graph a
		assertTrue(store.updateInGraph("INSERT DATA {" +
			"	<http://example.com/a2> <http://example.com/b2> <http://example.com/c2> ." +
			"}", GRAPH_A));
		assertEquals(aGraphSize + 1, store.getCount(GRAPH_A));
		assertEquals(defaultGraphSize, store.getCountDefault());
		aGraphSize = store.getCount(GRAPH_A);
		defaultGraphSize = store.getCountDefault();

		//the triple is already there
		assertFalse(store.updateInGraph("INSERT DATA {" +
			"	<http://example.com/a2> <http://example.com/b2> <http://example.com/c2> ." +
			"}", GRAPH_A));
		assertEquals(aGraphSize, store.getCount(GRAPH_A));

		//doesn't add anything: OK because we're talking directly to the graph so the GRAPH block will not refer to any known graph
		assertFalse(store.updateInGraph("INSERT DATA { GRAPH <" + GRAPH_A + "> {" +
			"	<http://example.com/a4> <http://example.com/b4> <http://example.com/c4> ." +
			"}}", GRAPH_A));
		assertEquals(aGraphSize, store.getCount(GRAPH_A));
		assertEquals(defaultGraphSize, store.getCountDefault());
		
		
		
		// ------ Test 2: Deleting of data ------
		
		logger.info("Test 2: Deleting of data in a graph.");
		store.clearGraph(GRAPH_A);
		store.clearDefaultGraph();

		//set up store
		store.update("INSERT DATA { GRAPH <" + GRAPH_A + "> {" +
			"	<http://example.com/a5> <http://example.com/b5> <http://example.com/c5> ." +
			"	<http://example.com/a6> <http://example.com/b6> <http://example.com/c6> ." +
			"	<http://example.com/a7> <http://example.com/b7> <http://example.com/c7> ." +
			"	<http://example.com/a8> <http://example.com/b8> <http://example.com/c8> ." +
			"}}");
		store.update("INSERT DATA { GRAPH <" + "urn:x-arq:DefaultGraph" + "> {" +
			"	<http://example.com/a5> <http://example.com/b5> <http://example.com/c5> ." +
			"	<http://example.com/a6> <http://example.com/b6> <http://example.com/c6> ." +
			"	<http://example.com/a7> <http://example.com/b7> <http://example.com/c7> ." +
			"	<http://example.com/a8> <http://example.com/b8> <http://example.com/c8> ." +
			"}}");

		aGraphSize = store.getCount(GRAPH_A);
		defaultGraphSize = store.getCountDefault();

		//deletes triple from graph a
		assertTrue(store.updateInGraph("DELETE DATA {" +
			"	<http://example.com/a6> <http://example.com/b6> <http://example.com/c6> ." +
			"}", GRAPH_A));
		assertEquals(aGraphSize-1, store.getCount(GRAPH_A));
		assertEquals(defaultGraphSize, store.getCountDefault());

		//the triple is already deleted
		assertFalse(store.updateInGraph("DELETE DATA {" +
			"	<http://example.com/a6> <http://example.com/b6> <http://example.com/c6> ." +
			"}", GRAPH_A));
		assertEquals(aGraphSize-1, store.getCount(GRAPH_A));

		//doesn't delete anything: OK because we're talking directly to the graph so the GRAPH block will not refer to any known graph
		assertFalse(store.updateInGraph("DELETE DATA { GRAPH <" + GRAPH_A + "> {" +
			"	<http://example.com/a8> <http://example.com/b8> <http://example.com/c8> ." +
			"}}", GRAPH_A));
		assertEquals(aGraphSize-1, store.getCount(GRAPH_A));
		assertEquals(defaultGraphSize, store.getCountDefault());
		
		
		
		// ------ Test 3: DELETE/INSERT -----
		
		logger.info("Test 3: DELETE/INSERT in a graph.");
		store.clearGraph(GRAPH_A);
		store.clearGraph(GRAPH_B);
		store.clearDefaultGraph();

		//set up store
		store.updateInGraph("INSERT DATA {" +
			"	<http://example.com/a> <http://example.com/b> <http://example.com/c> ." +
			"	<http://example.com/d> <http://example.com/b> <http://example.com/c> ." +
			"}", GRAPH_A);

		aGraphSize = store.getCount(GRAPH_A);
		defaultGraphSize = store.getCountDefault();

		//delete/insert: a pattern without GRAPH reads the graph, also in the WHERE clause
		String replace = "DELETE {\n" +
		"	?s <http://example.com/b> <http://example.com/c> .\n" +
		"} INSERT {\n" +
		"	?s <http://example.com/b> <http://example.com/e> .\n" +
		"} WHERE {\n" +
		"	?s <http://example.com/b> <http://example.com/c> .\n" +
		"}";
		assertTrue(store.updateInGraph(replace, GRAPH_A));
		assertEquals(aGraphSize, store.getCount(GRAPH_A));
		assertEquals(defaultGraphSize, store.getCountDefault());
		assertFalse(store.queryAsk("ASK { GRAPH <" + GRAPH_A + "> { ?s <http://example.com/b> <http://example.com/c> } }"));
		assertTrue(store.queryAsk("ASK { GRAPH <" + GRAPH_A + "> { ?s <http://example.com/b> <http://example.com/e> } }"));

		//nothing left to replace
		assertFalse(store.updateInGraph(replace, GRAPH_A));
		
		
		
		// Test exception cases.
		Assertions.assertThatThrownBy(() -> {
			store.updateInGraph("INSERT DATA {" +
					"	<http://example.com/a1> <http://example.com/b1> <http://example.com/c1> ." +
					"}", null);}).isInstanceOf(RuntimeException.class);
		Assertions.assertThatThrownBy(() -> {
			store.updateInGraph(null, GRAPH_A);}).isInstanceOf(RuntimeException.class);
		Assertions.assertThatThrownBy(() -> {
			store.updateInGraph("INVALID QUERY", GRAPH_A);}).isInstanceOf(RuntimeException.class);
	}

	@Test
	public void testUpdateBatch() {
		//no updates
		store.updateBatch(new ArrayList<String>(), GRAPH_A);
		assertEquals(0, store.getCount(GRAPH_A));

		store.updateBatch(Arrays.asList(
			"INSERT DATA { GRAPH <" + GRAPH_A + "> { <http://example.com/a> <http://example.com/b> <http://example.com/c> } }",
			"INSERT DATA { GRAPH <" + GRAPH_A + "> { <http://example.com/d> <http://example.com/e> <http://example.com/f> } }",
			"INSERT DATA { GRAPH <" + GRAPH_A + "> { <http://example.com/g> <http://example.com/h> <http://example.com/i> } }"
		), GRAPH_A);
		assertEquals(3, store.getCount(GRAPH_A));

		//all or nothing: if one of the updates fails, none of them is applied
		Assertions.assertThatThrownBy(() -> {
			store.updateBatch(Arrays.asList(
				"INSERT DATA { GRAPH <" + GRAPH_A + "> { <http://example.com/j> <http://example.com/k> <http://example.com/l> } }",
				"INVALID QUERY",
				"INSERT DATA { GRAPH <" + GRAPH_A + "> { <http://example.com/m> <http://example.com/n> <http://example.com/o> } }"
			), GRAPH_A);}).isInstanceOf(RuntimeException.class);
		assertEquals(3, store.getCount(GRAPH_A));


		// Test exception cases.
		Assertions.assertThatThrownBy(() -> {
			store.updateBatch(null, GRAPH_A);}).isInstanceOf(RuntimeException.class);
	}

	@Test
	public void testDatasets() {
		store.createGraph(GRAPH_A);
		store.createGraph(GRAPH_B);

		// Add two triples to GRAPH_A and three to GRAPH_B
		store.update("INSERT DATA { GRAPH <" + GRAPH_A + "> {"
				+ "	<http://example.com/a1> <http://example.com/a2> <http://example.com/a3> ."
				+ "	<http://example.com/a1> <http://example.com/a2> <http://example.com/a4> }}");
		store.update("INSERT DATA { GRAPH <" + GRAPH_B + "> {"
				+ "	<http://example.com/b1> <http://example.com/b2> <http://example.com/b3> ."
				+ "	<http://example.com/b1> <http://example.com/b2> <http://example.com/b4> ."
				+ "	<http://example.com/b1> <http://example.com/b2> <http://example.com/b5>"
				+ "}}");

		logger.debug("Graph sizes: A: {}, B: {}", store.getCount(GRAPH_A), store.getCount(GRAPH_B));

		List<Map<String, String>> result;

		
		// TODO: Modified createQueryExecution() method no longer uses Jena pre-filtering, so remove?
		//  ----------- First test: don't use the GRAPH keyword (Jena pre-filtering) -----------
		String sparql = "SELECT * WHERE { ?s ?p ?o }";
		result = store.translateSelectResult(store.querySelect(sparql));
		assertEquals(5, result.size());

		result = store.translateSelectResult(store.querySelect(sparql, GRAPH_A));
		assertEquals(2, result.size());

		result = store.translateSelectResult(store.querySelect(sparql, GRAPH_B + "Z"));
		assertEquals(0, result.size());

		result = store.translateSelectResult(store.querySelect(sparql, GRAPH_A, GRAPH_B));
		assertEquals(5, result.size());

		result = store.translateSelectResult(store.querySelect(sparql, GRAPH_A, GRAPH_B + "Z"));
		assertEquals(2, result.size());

		
		// Above methods already use SPARQL filtering (but not GRAPH keyword), how to reconcile?
		// ---------- Second test: use the GRAPH keyword (filter using SPARQL) ----------
		//sparql = "SELECT * WHERE { GRAPH ?g { ?s ?p ?o } }";
		/*sparql = "SELECT * WHERE { GRAPH <%s> { ?s ?p ?o } }";
		
		result = store.translateSelectResult(store.querySelect("SELECT * WHERE { GRAPH ?g { ?s ?p ?o } }"));
		assertEquals(5, result.size());

		//result = store.translateSelectResult(store.querySelect("SELECT * WHERE { GRAPH <http://example.com/a> { ?s ?p ?o } }"));
		result = store.translateSelectResult(store.querySelect(String.format(sparql, GRAPH_A)));
		assertEquals(2, result.size());


		result = store.translateSelectResult(store.querySelect(String.format(sparql, GRAPH_B + "Z")));
		assertEquals(0, result.size());

		result = store.translateSelectResult(store.querySelect(
				String.format("SELECT * WHERE { GRAPH <%s> { ?s ?p ?o } GRAPH <%s> { ?a ?b ?c } }", GRAPH_A, GRAPH_B)));
		assertEquals(5, result.size());

		result = store.translateSelectResult(store.querySelect(sparql, GRAPH_A, GRAPH_B + "Z"));
		assertEquals(2, result.size());*/
	}

	@Test
	public void testQuerySelectWithGraph() {
		store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm5.rdf").getPath(),
			"http://example.com/bm5/", Format.RDF);
		store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm25.rdf").getPath(),
			"http://example.com/bm25/", Format.RDF);
		store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm50.rdf").getPath(),
			"http://example.com/bm50/", Format.RDF);

		List<Map<String, String>> result;
		try {
			String sparql = "SELECT * WHERE { ?s ?p ?o }";
			String sparqlG = "SELECT * WHERE { GRAPH ?g { ?s ?p ?o } }";

			//query one graph
			result = store.translateSelectResult(store.querySelect(sparql,
					"http://example.com/bm5/"
			));
			assertEquals(41, result.size());

			//query multiple graphs
			result = store.translateSelectResult(store.querySelect(sparql,
				"http://example.com/bm5/",
				"http://example.com/bm25/"
			));
			assertEquals(436, result.size());

			//query default graph
			result = store.translateSelectResult(store.querySelect(sparqlG));
			assertEquals(1215, result.size());
		} catch (Exception e) {
			logger.error("Error connecting to store {}", store, e);
			fail("Error connecting to store");
		}
	}

	@Test
	public void testQuerySelect() {
		store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm5.rdf").getPath(),
			"http://example.com/bm5/", Format.RDF);
		store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm25.rdf").getPath(),
			"http://example.com/bm25/", Format.RDF);

		List<Map<String, String>> result;
		try {

			//with graph
			result = store.translateSelectResult(store.querySelect("SELECT * WHERE { ?s ?p ?o }", "http://example.com/bm5/"));
			assertEquals(41, result.size());

			//with graphs
			result = store.translateSelectResult(store.querySelect("SELECT * WHERE { ?s ?p ?o }", "http://example.com/bm5/", "http://example.com/bm25/"));
			assertEquals(436, result.size());

			//with Jena TDB union graph
			result = store.translateSelectResult(store.querySelect("SELECT * WHERE { ?s ?p ?o }", "urn:x-arq:UnionGraph"));
			assertEquals(436, result.size());

			//without graph: default graph is empty in TDB unless specified!
			result = store.translateSelectResult(store.querySelect("SELECT * WHERE { ?s ?p ?o }", "urn:x-arq:DefaultGraph"));
			assertEquals(0, result.size());

			//everything
			result = store.translateSelectResult(store.querySelect("SELECT * WHERE { ?s ?p ?o }"));
			assertEquals(436, result.size());

		} catch (Exception e) {
			logger.error("Error connecting to store {}", store, e);
			fail("Error connecting to store");
		}
		
		
		// Test exception cases.	
		Assertions.assertThatThrownBy(() -> {
			store.querySelect(null);}).isInstanceOf(RuntimeException.class);
		Assertions.assertThatThrownBy(() -> {
			store.querySelect("SELECT * WHERE { ?s ?p ?o }", new String[] {null});}).isInstanceOf(RuntimeException.class);
		Assertions.assertThatThrownBy(() -> {
			store.querySelect("INVALID QUERY", new String[] {null});}).isInstanceOf(RuntimeException.class);
		Assertions.assertThatThrownBy(() -> {
			store.querySelect("SELECT * WHERE { ?s ?p ?o }", null, null);}).isInstanceOf(RuntimeException.class);
	}

	@Test
	public void testQueryConstruct() {
		

		store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm5.rdf").getPath(),
			"http://example.com/bm5/", Format.RDF);
		store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm25.rdf").getPath(),
			"http://example.com/bm25/", Format.RDF);

		Model result;
		try {

			//default graph (set to all graphs in the tdb wrapper)
			result = (Model) store.queryConstruct("CONSTRUCT { ?s <http://example.com/b> <http://example.com/c> } WHERE { ?s ?p ?o }");
			assertEquals(131, result.size());

			//all graphs
			result = (Model) store.queryConstruct("CONSTRUCT { ?s <http://example.com/b> <http://example.com/c> } WHERE { ?s ?p ?o }", "urn:x-arq:UnionGraph");
			assertEquals(131, result.size());

			//with graph
			result = (Model) store.queryConstruct("CONSTRUCT { ?s <http://example.com/b> <http://example.com/c> } WHERE { ?s ?p ?o }", "http://example.com/bm5/");
			assertEquals(13, result.size());

		} catch (Exception e) {
			logger.error("Error connecting to store {}", store, e);
			fail("Error connecting to store");
		}
		
		
		// Exception cases.
		Assertions.assertThatThrownBy(() -> {
			store.queryConstruct(null);}).isInstanceOf(RuntimeException.class);
		Assertions.assertThatThrownBy(() -> {
			store.queryConstruct("CONSTRUCT { ?s <http://example.com/b> <http://example.com/c> } WHERE { ?s ?p ?o }", null, null);}).isInstanceOf(RuntimeException.class);
		Assertions.assertThatThrownBy(() -> {
			store.queryConstruct("INVALID QUERY");}).isInstanceOf(RuntimeException.class);
	}

	@Test
	public void testQueryAsk() {
		

		boolean result;
		try {

			//without graph
			result = store.queryAsk("ASK { <http://example.com/x> <http://example.com/y> <http://example.com/z> }");
			logger.debug("{}", result);
			result = store.queryAsk("ASK { <http://example.com/x> <http://example.com/y> <http://example.com/x> }");
			logger.debug("{}", result);

			//with graph
			result = store.queryAsk("ASK WHERE { <http://example.com/x> <http://example.com/y> <http://example.com/z> }", "http://example.com/");
			logger.debug("{}", result);
			result = store.queryAsk("ASK WHERE { <http://example.com/x> <http://example.com/y> <http://example.com/x> }", "http://example.com/");
			logger.debug("{}", result);

		} catch (Exception e) {
			logger.error("Error connecting to store {}", store, e);
			fail("Error connecting to store");
		}
	}

	@Test
	public void testClearGraph() {
		store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm5.rdf").getPath(),
			"http://example.com/bm5/", Format.RDF);
		assertEquals(41, store.getCount("http://example.com/bm5/"));

		store.clearGraph("http://example.com/bm5/");
		assertEquals(0, store.getCount("http://example.com/bm5/"));
		store.clearGraph("Non-Existent Graph"); // No exception should be thrown.
		
		
		// Test exception cases.
		Assertions.assertThatThrownBy(() -> {
			store.clearGraph(null);}).isInstanceOf(RuntimeException.class);
	}

	@Test
	public void testGraphExists() {
		String graph = "http://example.com/";
		assertFalse(store.graphExists(graph));
		store.createGraph(graph);
		//stores don't need to record the existence of empty graphs so we need to add some triples
		store.update("INSERT DATA { GRAPH <" + graph + "> { <http://example.com/a> <http://example.com/b> <http://example.com/c> }}");
		assertTrue(store.graphExists(graph));
		store.clearGraph(graph);
		

		// Test exception cases.
		Assertions.assertThatThrownBy(() -> {
			store.graphExists(null);}).isInstanceOf(RuntimeException.class);
	}

	@Test
	public void testCreateGraph() {
		String graph = "http://example.com/Testrepo-7485738475347584375";
		//delete if it exists
		if (store.graphExists(graph)) {
			store.deleteGraph(graph);
			//should not exist now
			assertFalse(store.graphExists(graph));
		}

		//create graph
		if (!store.graphExists(graph)) {
			store.createGraph(graph);
			//stores don't need to record the existence of empty graphs so we need to add some triples
			store.update("INSERT DATA { GRAPH <" + graph + "> { <http://example.com/a> <http://example.com/b> <http://example.com/c> }}");
			assertTrue(store.graphExists(graph));
		}
		//delete again
		store.deleteGraph(graph);
		

		// Test exception cases.
		Assertions.assertThatThrownBy(() -> {
			store.createGraph(null);}).isInstanceOf(RuntimeException.class);
	}

	@Test
	public void testCopyGraph() {
		//init graphs: all empty to begin with
		String from = GRAPH_A;
		String to = GRAPH_B;
		String other = GRAPH_C;
		store.clearGraph(from);
		store.clearGraph(to);
		store.clearGraph(other);
		assertEquals(0, store.getCount(from));
		assertEquals(0, store.getCount(to));
		assertEquals(0, store.getCount(other));

		//add some test data
		store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm5.rdf").getPath(), from, Format.RDF);
		assertEquals(41, store.getCount(from));
		store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/core.rdf").getPath(), other, Format.RDF);
		assertEquals(6172, store.getCount(other));

		//copy graph to empty graph
		assertEquals(0, store.getCount(to));
		store.copyGraph(from, to);
		assertEquals(41, store.getCount(to));

		//copy graph to non-empty graph
		assertEquals(6172, store.getCount(other));
		store.copyGraph(from, other);
		assertEquals(6213, store.getCount(other));


		// Test exception cases.
		Assertions.assertThatThrownBy(() -> {
			store.copyGraph(null, other);}).isInstanceOf(RuntimeException.class);
		Assertions.assertThatThrownBy(() -> {
			store.copyGraph(from, null);}).isInstanceOf(RuntimeException.class);
	}

	@Test
	public void testGetGraphs() {
		String graph = "http://example.com/";
		store.createGraph(graph);
		//stores don't need to record the existence of empty graphs so we need to add some triples
		store.update("INSERT DATA { GRAPH <" + graph + "> { <http://example.com/a> <http://example.com/b> <http://example.com/c> }}");
		Set<String> graphs = store.getGraphs();
		assertEquals(1, graphs.size());
		assertTrue(graphs.contains(graph));
		store.clearGraph(graph);
	}

	@Test
	public void testLoadIntoGraphRDF() {
		//single graph
		store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm25.rdf").getPath(),
			"http://example.com/bm25/a", Format.RDF);
		assertEquals(395, store.getCount("http://example.com/bm25/a"));
		store.clearGraph("http://example.com/bm25/a");

		//multiple graphs
		assertEquals(0, store.getCount("http://example.com/bm5/"));
		assertEquals(0, store.getCount("http://example.com/bm25/"));
		assertEquals(0, store.getCount("http://example.com/bm50/"));
	}

	@Test
	public void testLoadIntoGraphTTL() {
		//single graph
		store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm25.ttl").getPath(),
			"http://example.com/bm25/a", Format.TTL);
		assertEquals(395, store.getCount("http://example.com/bm25/a"));
		store.clearGraph("http://example.com/bm25/a");

		//multiple graphs
		assertEquals(0, store.getCount("http://example.com/bm5/"));
		assertEquals(0, store.getCount("http://example.com/bm25/"));
		assertEquals(0, store.getCount("http://example.com/bm50/"));
	}

	@Test
	public void testLoadIntoGraphCompressed() {
		//single graph
		store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm25.rdf.gz").getPath(),
			"http://example.com/bm25/a", Format.RDF);
		assertEquals(395, store.getCount("http://example.com/bm25/a"));
		store.clearGraph("http://example.com/bm25/a");

		//multiple graphs
		assertEquals(0, store.getCount("http://example.com/bm5/"));
		assertEquals(0, store.getCount("http://example.com/bm25/"));
		assertEquals(0, store.getCount("http://example.com/bm50/"));
	}

	@Test
	public void testLoadIntoGraphExceptions() {
		//Illegal arguments
		Assertions.assertThatIllegalArgumentException()
			.isThrownBy(
				() -> store.loadIntoGraph((String) null, "http://example.com/bm25/a", Format.RDF)
			)
			.withMessage("Path null or empty");
		Assertions.assertThatIllegalArgumentException()
			.isThrownBy(
				() -> store.loadIntoGraph("", "http://example.com/bm25/a", Format.RDF)
			)
			.withMessage("Path null or empty");
		Assertions.assertThatIllegalArgumentException()
			.isThrownBy(
				() -> store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm25.rdf").getPath(), null, Format.RDF)
			)
			.withMessage("Graph null");
		Assertions.assertThatIllegalArgumentException().isThrownBy(
				() -> store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm25.rdf").getPath(), "http://example.com/bm25/a", null)
			)
			.withMessage("Format null");
		Assertions.assertThatIllegalArgumentException().isThrownBy(
				() -> store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm25.rdf").getPath(), "http://example.com/bm25/a", Format.NQ)
			)
			.withMessage("Invalid format: NQ");

		//RDF file but TTL format - Jena fails to parse it
		Assertions.assertThatThrownBy(
			() -> store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm25.rdf").getPath(), "http://example.com/bm25/a", Format.TTL)
		).isInstanceOf(RuntimeException.class);
	}

	@Test
	public void testLoadIntoGraphInputStream() {
		//single graph
		store.loadIntoGraph(getClass().getClassLoader().getResourceAsStream("semanticstore/bm25.rdf"), "http://example.com/bm25/a", Format.RDF);
		assertEquals(395, store.getCount("http://example.com/bm25/a"));
		store.clearGraph("http://example.com/bm25/a");

		//multiple graphs
		assertEquals(0, store.getCount("http://example.com/bm5/"));
		assertEquals(0, store.getCount("http://example.com/bm25/"));
		assertEquals(0, store.getCount("http://example.com/bm50/"));

		//Illegal arguments
		Assertions.assertThatIllegalArgumentException()
			.isThrownBy(
				() -> store.loadIntoGraph((InputStream) null, "http://example.com/bm25/a", Format.RDF)
			)
			.withMessage("Input stream null");
		Assertions.assertThatIllegalArgumentException()
			.isThrownBy(
				() -> store.loadIntoGraph(getClass().getClassLoader().getResourceAsStream("semanticstore/bm25.rdf"), null, Format.RDF)
			)
			.withMessage("Graph null");
		Assertions.assertThatIllegalArgumentException().isThrownBy(
				() -> store.loadIntoGraph(getClass().getClassLoader().getResourceAsStream("semanticstore/bm25.rdf"), "http://example.com/bm25/a", null)
			)
			.withMessage("Format null");
		Assertions.assertThatIllegalArgumentException().isThrownBy(
				() -> store.loadIntoGraph(getClass().getClassLoader().getResourceAsStream("semanticstore/bm25.rdf"), "http://example.com/bm25/a", Format.NQ)
			)
			.withMessage("Invalid format: NQ");

		//RDF file but TTL format - Jena fails to parse it
		Assertions.assertThatThrownBy(
			() -> store.loadIntoGraph(getClass().getClassLoader().getResourceAsStream("semanticstore/bm25.rdf"), "http://example.com/bm25/a", Format.TTL)
		).isInstanceOf(RuntimeException.class);
	}

	@Test
	public void testLoad() {
		//uncompressed NQ
		store.load(getClass().getClassLoader().getResource("semanticstore/quads.nq").getPath());
		assertEquals(41, store.getCount("http://example.com/bm5/"));
		assertEquals(395, store.getCount("http://example.com/bm25/"));
		assertEquals(779, store.getCount("http://example.com/bm50/"));
		store.clear();

		//compressed NQ
		store.load(getClass().getClassLoader().getResource("semanticstore/quads.nq.gz").getPath());
		assertEquals(41, store.getCount("http://example.com/bm5/"));
		assertEquals(395, store.getCount("http://example.com/bm25/"));
		assertEquals(779, store.getCount("http://example.com/bm50/"));
		store.clear();

		// Test exception cases.
		Assertions.assertThatIllegalArgumentException()
			.isThrownBy(
				() -> store.load((String) null)
			)
			.withMessage("Path null or empty");
		Assertions.assertThatIllegalArgumentException()
			.isThrownBy(
				() -> store.load("")
			)
			.withMessage("Path null or empty");
	}

	@Test
	public void testLoadInputStream() {
		InputStream input = getClass().getClassLoader().getResourceAsStream("semanticstore/quads.nq");

		//uncompressed NQ
		store.load(input);
		assertEquals(41, store.getCount("http://example.com/bm5/"));
		assertEquals(395, store.getCount("http://example.com/bm25/"));
		assertEquals(779, store.getCount("http://example.com/bm50/"));
		store.clear();

		// Test exception cases.
		Assertions.assertThatIllegalArgumentException()
			.isThrownBy(
				() -> store.load((InputStream) null)
			)
			.withMessage("Input stream null");
	}

	@Test
	public void testLoadAndRenameGraphsUncompressed() {
		store.load(getClass().getClassLoader().getResource("semanticstore/rename-graphs.nq").getPath(), "a", "b");
		assertRenameGraphs();
	}

	@Test
	public void testLoadAndRenameGraphsCompressed() {
		store.load(getClass().getClassLoader().getResource("semanticstore/rename-graphs.nq.gz").getPath(), "a", "b");
		assertRenameGraphs();
	}

	@Test
	public void testLoadAndRenameGraphsPrefixUncompressed() {
		store.load(getClass().getClassLoader().getResource("semanticstore/rename-graphs.nq").getPath(), "http://e.c/a", "http://e.c/b");
		assertRenameGraphsPrefix();
	}

	@Test
	public void testLoadAndRenameGraphsPrefixCompressed() {
		store.load(getClass().getClassLoader().getResource("semanticstore/rename-graphs.nq.gz").getPath(), "http://e.c/a", "http://e.c/b");
		assertRenameGraphsPrefix();
	}

	@Test
	public void testLoadInputStreamAndRenameGraphs() {
		store.load(getClass().getClassLoader().getResourceAsStream("semanticstore/rename-graphs.nq"), "a", "b");
		assertRenameGraphs();
	}

	@Test
	public void testLoadInputStreamAndRenameGraphsPrefix() {
		store.load(getClass().getClassLoader().getResourceAsStream("semanticstore/rename-graphs.nq"), "http://e.c/a", "http://e.c/b");
		assertRenameGraphsPrefix();
	}

	private void assertRenameGraphs() {
		assertEquals(8, store.getCount("http://e.c/b"));
		assertEquals(8, store.getCount("http://e.c/Xb"));
		assertEquals(8, store.getCount("http://e.c/bY"));
		assertEquals(8, store.getCount("http://e.c/XbY"));

		Triple[] expected = {
			new Triple("http://e.c/b",   "http://e.c/p", "http://e.c/o",   TripleType.UNKNOWN),
			new Triple("http://e.c/s",   "http://e.c/p", "http://e.c/b",   TripleType.UNKNOWN),
			new Triple("http://e.c/Xb",  "http://e.c/p", "http://e.c/o",   TripleType.UNKNOWN),
			new Triple("http://e.c/s",   "http://e.c/p", "http://e.c/Xb",  TripleType.UNKNOWN),
			new Triple("http://e.c/bY",  "http://e.c/p", "http://e.c/o",   TripleType.UNKNOWN),
			new Triple("http://e.c/s",   "http://e.c/p", "http://e.c/bY",  TripleType.UNKNOWN),
			new Triple("http://e.c/XbY", "http://e.c/p", "http://e.c/o",   TripleType.UNKNOWN),
			new Triple("http://e.c/s",   "http://e.c/p", "http://e.c/XbY", TripleType.UNKNOWN)
		};

		Assertions.assertThat(getAllTriplesInGraph("http://e.c/b")).containsExactlyInAnyOrder(expected);
		Assertions.assertThat(getAllTriplesInGraph("http://e.c/Xb")).containsExactlyInAnyOrder(expected);
		Assertions.assertThat(getAllTriplesInGraph("http://e.c/bY")).containsExactlyInAnyOrder(expected);
		Assertions.assertThat(getAllTriplesInGraph("http://e.c/XbY")).containsExactlyInAnyOrder(expected);
	}

	private void assertRenameGraphsPrefix() {
		assertEquals(8, store.getCount("http://e.c/b"));
		assertEquals(8, store.getCount("http://e.c/Xa"));  //unchanged
		assertEquals(8, store.getCount("http://e.c/bY"));
		assertEquals(8, store.getCount("http://e.c/XaY")); //unchanged

		Triple[] expected = {
			new Triple("http://e.c/b",   "http://e.c/p", "http://e.c/o",   TripleType.UNKNOWN),
			new Triple("http://e.c/s",   "http://e.c/p", "http://e.c/b",   TripleType.UNKNOWN),
			new Triple("http://e.c/Xa",  "http://e.c/p", "http://e.c/o",   TripleType.UNKNOWN), //unchanged
			new Triple("http://e.c/s",   "http://e.c/p", "http://e.c/Xa",  TripleType.UNKNOWN), //unchanged
			new Triple("http://e.c/bY",  "http://e.c/p", "http://e.c/o",   TripleType.UNKNOWN),
			new Triple("http://e.c/s",   "http://e.c/p", "http://e.c/bY",  TripleType.UNKNOWN),
			new Triple("http://e.c/XaY", "http://e.c/p", "http://e.c/o",   TripleType.UNKNOWN), //unchanged
			new Triple("http://e.c/s",   "http://e.c/p", "http://e.c/XaY", TripleType.UNKNOWN)  //unchanged
		};

		Assertions.assertThat(getAllTriplesInGraph("http://e.c/b")).containsExactlyInAnyOrder(expected);
		Assertions.assertThat(getAllTriplesInGraph("http://e.c/Xa")).containsExactlyInAnyOrder(expected);
		Assertions.assertThat(getAllTriplesInGraph("http://e.c/bY")).containsExactlyInAnyOrder(expected);
		Assertions.assertThat(getAllTriplesInGraph("http://e.c/XaY")).containsExactlyInAnyOrder(expected);
	}

	private Triple[] getAllTriplesInGraph(String graph) {
		return store
			.translateSelectResult(store.querySelect("SELECT * WHERE { ?s ?p ?o }", graph))
			.stream()
			.map(this::createTriple)
			.collect(toList())
			.toArray(new Triple[0]);
	}

	private Triple createTriple(Map<String, String> rawTriple) {
		return new Triple(rawTriple.get("s"), rawTriple.get("p"), rawTriple.get("o"), TripleType.UNKNOWN);
	}

	@Test
	public void testLoadAndRenameGraphsExceptions() {
		String path = getClass().getClassLoader().getResource("semanticstore/rename-graphs.nq").getPath();

		Assertions.assertThatIllegalArgumentException()
			.isThrownBy(
				() -> store.load((String) null, "a", "b")
			)
			.withMessage("Path null or empty");
		Assertions.assertThatIllegalArgumentException()
			.isThrownBy(
				() -> store.load("", "a", "b")
			)
			.withMessage("Path null or empty");
		Assertions.assertThatIllegalArgumentException()
			.isThrownBy(
				() -> store.load((InputStream) null, "a", "b")
			)
			.withMessage("Input stream null");
		Assertions.assertThatIllegalArgumentException()
			.isThrownBy(
				() -> store.load(path, null, "b")
			)
			.withMessage("Old URI null or empty");
		Assertions.assertThatIllegalArgumentException()
			.isThrownBy(
				() -> store.load(path, "", "b")
			)
			.withMessage("Old URI null or empty");
		Assertions.assertThatIllegalArgumentException()
			.isThrownBy(
				() -> store.load(path, "a", null)
			)
			.withMessage("New URI null or empty");
		Assertions.assertThatIllegalArgumentException()
			.isThrownBy(
				() -> store.load(path, "a", "")
			)
			.withMessage("New URI null or empty");
	}

	@Test
	public void testExport() {
		//need to import first
		store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm5.rdf").getPath(),
			"http://example.com/bm5/", Format.RDF);
		store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm25.rdf").getPath(),
			"http://example.com/bm25/", Format.RDF);
		store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm50.rdf").getPath(),
			"http://example.com/bm50/", Format.RDF);

		String result = store.export(Format.RDF, "http://example.com/bm50/", "http://example.com/bm25/");
		assertEquals(702, result.split("\n").length);

		result = store.export(Format.TTL, null);
		assertEquals(1417, result.split("\n").length);

		result = store.export(Format.NQ, null);
		assertEquals(1215, result.split("\n").length);
		
		
		// Test exception cases.
		Assertions.assertThatThrownBy(() -> {
			store.export(null, null);}).isInstanceOf(RuntimeException.class);
		Assertions.assertThatThrownBy(() -> {
			store.export(Format.NQ, null, null, null);}).isInstanceOf(RuntimeException.class);
	}

	@Test
	public void testSave() {
		//need to import first
		store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm5.rdf").getPath(),
			"http://example.com/bm5/", Format.RDF);
		store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm25.rdf").getPath(),
			"http://example.com/bm25/", Format.RDF);
		store.loadIntoGraph(getClass().getClassLoader().getResource("semanticstore/bm50.rdf").getPath(),
			"http://example.com/bm50/", Format.RDF);
		
		File dir = null;
		try {
			dir = folder.newFolder();
			
			store.save(new File(dir, "uncompressed").getPath(), Format.RDF, "http://example.com/bm50/", false, "http://example.com/bm25/");
			store.save(new File(dir, "compressed").getPath(), Format.RDF, "http://example.com/bm50/", true, "http://example.com/bm25/");
			store.save(new File(dir, "uncompressed").getPath(), Format.NQ, "http://example.com/bm50/", false, "http://example.com/bm25/");
			store.save(new File(dir, "compressed").getPath(), Format.NQ, "http://example.com/bm50/", true, "http://example.com/bm25/");
			store.save(new File(dir, "uncompressed").getPath(), Format.TTL, "http://example.com/bm50/", false, "http://example.com/bm25/");
			store.save(new File(dir, "compressed").getPath(), Format.TTL, "http://example.com/bm50/", true, "http://example.com/bm25/");
		} catch (IOException e) {
			logger.error("Failure to create necessary files.", e);
			fail();
		}
		
		JenaTDBStoreWrapper jStore = (JenaTDBStoreWrapper) store;
		
		File file;
		//file sizes of compressed files seem to vary to just checking approximate size
		try {
			file = new File(dir, "uncompressed.rdf");
			assertFalse(jStore.isCompressed(file.getPath()));
			long fileSize = file.length();
			
			file = new File(dir, "compressed.rdf.gz");
			assertTrue(file.length() < fileSize);
			assertTrue(jStore.isCompressed(file.getPath()));
			
			file = new File(dir, "uncompressed.nq");
			assertFalse(jStore.isCompressed(file.getPath()));
			fileSize = file.length();
			
			file = new File(dir, "compressed.nq.gz");
			assertTrue(file.length() < fileSize);
			assertTrue(jStore.isCompressed(file.getPath()));
			
			file = new File(dir, "uncompressed.ttl");
			assertFalse(jStore.isCompressed(file.getPath()));
			fileSize = file.length();
			
			file = new File(dir, "compressed.ttl.gz");
			assertTrue(file.length() < fileSize);
			assertTrue(jStore.isCompressed(file.getPath()));
		} catch (Exception ex) {
			logger.error("Could not check file size of exported files", ex);
			fail();
		} 
	}
	
	@Test
	public void testIsCompressed() {
		JenaTDBStoreWrapper jStore = (JenaTDBStoreWrapper) store;
		
		assertFalse(jStore.isCompressed(getClass().getClassLoader().getResource("semanticstore/bm25.rdf").getPath()));
		assertTrue(jStore.isCompressed(getClass().getClassLoader().getResource("semanticstore/bm25.rdf.gz").getPath()));
		assertFalse(jStore.isCompressed("non-existent-path"));
	}
	
	@Test
	public void testStoreModels() {
		int combinedSize = 0;
		
		Set<Object> models = new HashSet<Object>();
		for (String fileName : new String[] {"bm5.rdf", "bm25.rdf", "bm50.rdf"}) {
			Model model = RDFDataMgr.loadModel(getClass().getClassLoader().getResource("semanticstore/" + fileName).getPath());
			combinedSize += model.size();
			models.add(model);
		}
		store.storeModels(models, "MULTIPLE_MODELS");
		
		// Test that only one graph has been added, and that graph contains the triples from each of the models.
		assertEquals(1, store.getGraphs().size());
		assertEquals(combinedSize, store.getCount("MULTIPLE_MODELS"));
	}
	
	@Test
	public void testStoreModel() {
		Model model = RDFDataMgr.loadModel(getClass().getClassLoader().getResource("semanticstore/bm5.rdf").getPath());
		long modelSize = model.size();
		store.storeModel(model, GRAPH_A);
		
		assertEquals(modelSize, store.getCount(GRAPH_A));
		
		// Test exception cases.
		Assertions.assertThatThrownBy(() -> {
			store.storeModel(model, null);}).isInstanceOf(RuntimeException.class);
		Assertions.assertThatThrownBy(() -> {
			store.storeModel(null, GRAPH_A);}).isInstanceOf(RuntimeException.class);
		Assertions.assertThatThrownBy(() -> {
			store.storeModel(new String("WRONGOBJECT"), GRAPH_A);}).isInstanceOf(RuntimeException.class);
	}
}
