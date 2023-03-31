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
//      Created Date :          2018-03-20
//      Created for Project :   SHiELD
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.modelquerier.util;


import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.model.test.TestModel;
import uk.ac.soton.itinnovation.security.semanticstore.AStoreWrapper;
import uk.ac.soton.itinnovation.security.semanticstore.IStoreWrapper.Format;
import uk.ac.soton.itinnovation.security.semanticstore.JenaTDBStoreWrapper;

/**
 * A class to help manage test models, graph URIs etc. to support unit tests
 */
public class TestHelper {

	private static final Logger logger = LoggerFactory.getLogger(TestHelper.class);

	protected final AStoreWrapper store;
	protected final ModelStack model;
	protected final ClassLoader classLoader;

	protected final Map<Integer, TestModel> domains;
	protected final Map<Integer, TestModel> systems;


	public TestHelper(String storeDir) {

		domains = new HashMap<>();
		systems = new HashMap<>();

		model = new ModelStack();
		store = new JenaTDBStoreWrapper(storeDir);

		classLoader = getClass().getClassLoader();
	}

	public void addDomain(int key, String filename, String originalGraph) {
		addDomain(key, filename, originalGraph, null);
	}

	public void addDomain(int key, String filename, String originalGraph, String newGraph) {
		addModel(domains, key, filename, originalGraph, newGraph);
	}

	public void addSystem(int key, String filename, String originalGraph) {
		addSystem(key, filename, originalGraph, null);
	}

	public void addSystem(int key, String filename, String originalGraph, String newGraph) {
		addModel(systems, key, filename, originalGraph, newGraph);
	}

	private void addModel(Map<Integer, TestModel> models, int key, String filename, String originalGraph, String newGraph) {
		if (classLoader.getResource(filename)==null) {
			logger.error("Could not add model; file {} does not exist", filename);
			return;
		}

		models.put(key, new TestModel(classLoader.getResource(filename).getFile(), originalGraph, newGraph));
	}

	/**
	 * Sets up the store importing all the domain modela and the core model.
	 * This needs to be called after adding models in the @BeforeClass method..
	 */
	public void setUp() {

		//add core model to model stack
		model.addGraph("core", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/core");

		//set up store
		store.addPrefixes(model.getNamespaces());
		model.clearAllGraphs(store);
		store.clear();

		//import core model
		store.loadIntoGraph(classLoader.getResourceAsStream("core.rdf"), model.getGraph("core"), Format.RDF);
		
		//import domain models
		domains.entrySet().stream().forEachOrdered(d -> {
			if (d.getValue().getFile().endsWith(".nq") || d.getValue().getFile().endsWith(".nq.gz")) {
				if (d.getValue().getNewgraph() == null) {
					store.load(d.getValue().getFile());
				} else {
					store.load(d.getValue().getFile(), d.getValue().getGraph(), d.getValue().getNewgraph());
				}
			} else {
				if (d.getValue().getNewgraph() == null) {
					store.loadIntoGraph(d.getValue().getFile(), d.getValue().getGraph(), Format.RDF);
				} else {
					store.loadIntoGraph(d.getValue().getFile(), d.getValue().getNewgraph(), Format.RDF);
				}
			}
		});

		//check graphs have been correctly imported by printing the sizes
		model.printSizes(store);
		store.printSizes();
	}
	
	/**
	 * Select the domain and system models for a test.
	 * This must be call at the start of every test that uses the triple store.
	 *
	 * @param domain the domain model index
	 * @param system the system model index
	 */
	public void switchModels(int domain, int system) {
		TestModel domainModel = domains.get(domain);
		TestModel systemModel = systems.get(system);

		assert domainModel != null;
		assert systemModel != null;

		String domainGraph = domainModel.getEffectiveGraph();
		String systemGraph = systemModel.getEffectiveGraph();

		assert domainGraph != null;
		assert systemGraph != null;

		String domainFile = domainModel.getFile();
		String systemFile = systemModel.getFile();

		logger.warn(String.format("Switching to domain model <{%s}> at {%s}", domainGraph, domainFile));
		logger.warn(String.format("Switching to system model <{%s}> at {%s}", systemGraph, systemFile));

		model.addGraph("domain", domainGraph);

		String systemGraphInf  = systemGraph + "/inf";
		String systemGraphUI   = systemGraph + "/ui";
		String systemGraphMeta = systemGraph + "/meta";

		store.clearGraph(systemGraph);
		store.clearGraph(systemGraphInf);
		store.clearGraph(systemGraphUI);
		store.clearGraph(systemGraphMeta);

		if (systemModel.getNewgraph() == null) {
			store.load(systemModel.getFile());
		} else {
			store.load(systemModel.getFile(), systemModel.getGraph(), systemModel.getNewgraph());
		}

		model.addGraph("system", systemGraph);
		model.addGraph("system-inf", systemGraphInf);
		model.addGraph("system-ui", systemGraphUI);
		model.addGraph("system-meta", systemGraphMeta);

		//check graphs have been correctly imported by printing the sizes
		model.printSizes(store);
		store.printSizes();
	}

	/**
	 * Save the test model to file for better debugging if tests fail.
	 *
	 * @param name the name of the file in which to save the test model
	 * @param compressed use compression?
	 * @param ttl export as TTL?
	 * @param nq export as NQ?
	 */
	public void exportTestModel(String name, boolean compressed, boolean ttl, boolean nq) {

		//add "housekeeping" triples
		String sparql = "INSERT DATA {\n"
				+ "	GRAPH <" + getGraph("system") + "> {\n"
				+ "		<" + getGraph("system") + "> core:domainGraph <" + getGraph("domain") + "> . \n"
				+ "		<" + getNS("system").replace("#", "") + "> a owl:Ontology .\n"
				+ "		<" + getNS("system").replace("#", "") + "> owl:imports <" + getNS("domain").replace("#", "") + "> .\n"
				+ "	}\n"
				+ "}";
		getStore().update(sparql);

		if (ttl) {
			getStore().save(name, Format.TTL, getGraph("system"), compressed,
					getGraph("system"), getGraph("system-inf"), getGraph("system-ui"), getGraph("system-meta"));
		}

		if (nq) {
			getStore().save(name, Format.NQ, null, compressed,
					getGraph("system"), getGraph("system-inf"), getGraph("system-ui"), getGraph("system-meta"));
		}
	}

	// Getters/Setters ////////////////////////////////////////////////////////////////////////////

	public AStoreWrapper getStore() {
		return store;
	}

	public ModelStack getModel() {
		return model;
	}

	public Map<Integer, TestModel> getDomains() {
		return domains;
	}

	public Map<Integer, TestModel> getSystems() {
		return systems;
	}

	public String getGraph(String graph) {
		return model.getGraph(graph);
	}

	public String getNS(String graph) {
		return model.getNS(graph);
	}
}
