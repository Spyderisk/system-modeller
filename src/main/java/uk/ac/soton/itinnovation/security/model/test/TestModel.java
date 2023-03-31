/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2016
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
//      Modified By :
//      Created Date :          2017-08-31
//      Created for Project :   5G-ENSURE
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.model.test;

/**
 * A test model class for easier model switching during unit tests. This can represent a domain model or a system model.
 */
public final class TestModel {

	private static Class<? extends Object> c = TestModel.class;

	private String graph;
	private String file;
	private String newgraph;

	/**
	 * Create a new test model
	 */
	public TestModel() {
		newgraph = null;
	}

	/**
	 * Create a new test model
	 *
	 * @param graph the graph for the system model
	 * @param file the relative path of the system model file in src/test/resources
	 */
	public TestModel(String file, String graph) {

		this();
		setGraph(graph);
		setFile(file);
	}

	/**
	 * Create a new test model using an alternative graph
	 *
	 * @param graph the graph for the system model
	 * @param file the relative path of the system model file in src/test/resources
	 * @param newgraph the new graph URI to be used instead of the other one. Ignore if null
	 */
	public TestModel(String file, String graph, String newgraph) {

		this(file, graph);
		setNewgraph(newgraph);
	}

	@Override
	public String toString() {
		return "TestModel <" + getEffectiveGraph() + ">, loaded from " + file
				+ (newgraph!=null?", previously <" + graph + ">":"");
	}

	// Getters/Setters ////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configure the TestModel's class loader
	 * @param c the class whose classloader should be used to load the test models (most likely the test class)
	 */
	public static void setClass(Class<? extends Object> c) {
		TestModel.c = c;
	}

	public String getGraph() {
		return graph;
	}

	public void setGraph(String graph) {
		this.graph = graph;
	}

	public String getFile() {
		return file;
	}

	/**
	 * Set the path to the domain model file
	 *
	 * @param modelFile the file relative to the src/test/resources folder
	 */
	public void setFile(String modelFile) {
		this.file = modelFile;
	}

	public String getNewgraph() {
		return newgraph;
	}

	public void setNewgraph(String newgraph) {
		this.newgraph = newgraph;
	}

	/**
	 * Get the actual graph that should be used for this model.
	 * If a new graph has been set use it, otherwise use the normal graph.
	 *
	 * @return the effective graph to be used for this system
	 */
	public String getEffectiveGraph() {
		return (newgraph!=null?newgraph:graph);
	}
}
