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
//      Created Date :          2017-02-01
//      Created for Project :   5G-ENSURE
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.modelquerier.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.semanticstore.AStoreWrapper;

/**
 * A ModelStack is the "address book" of the ModelValidator. It shows a view from the system model's perspective:
 * There is one core model, one domain model, one system model. Each of them may have UI or INF models.
 * The validator always works on one single stack of models, that are distributed across various graphs.
 * This class helps keep track of the different graphs in one POJO.
 */
public class ModelStack {

	public static final String BASE_GRAPH = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/%s";
	public static final String BASE_NS = BASE_GRAPH + "#";
	public static final String UI_ID_SUFFIX = "-ui";
	public static final String UI_GRAPH_SUFFIX = "/ui";
	public static final String INF_ID_SUFFIX = "-inf";
	public static final String INF_GRAPH_SUFFIX = "/inf";

	private static final Logger logger = LoggerFactory.getLogger(ModelStack.class);

	protected final Map<String, String> graphs;
	protected final Map<String, String> namespaces;

	/**
	 * Creates a new model stack.
	 */
	public ModelStack() {

		graphs = new HashMap<>();

		namespaces = new HashMap<>();
		//default namespaces
		//only these default namespaces are used
		Arrays.asList("core", "domain", "system", "runtime").forEach(id -> addNS(id, String.format(BASE_NS, id)));
	}

	/**
	 * Add a graph to the list of graphs. If the identifier already exists, the graph URI will be overridden.
	 *
	 * @param prefix a short identifier for the graph which can also be used in SPARQL queries
	 * @param graphURI the URI of the graph in the store
	 */
	public void addGraph(String prefix, String graphURI) {

		if (graphs.containsKey(prefix) && (graphURI!=null && !graphURI.equals(graphs.get(prefix)))) {
			logger.info("Overriding graph {} <{}> with <{}>", prefix, graphs.get(prefix), graphURI);
		}
		graphs.put(prefix, graphURI);
	}

	/**
	 * Add a namespace to the list of namespaces. If the identifier already exists, the namespace will be overridden.
	 *
	 * @param prefix a short identifier for the graph which can also be used in SPARQL queries
	 * @param ns the namespace
	 */
	public void addNS(String prefix, String ns) {

		if (namespaces.containsKey(prefix)) {
			logger.warn("Overriding namespace {} <{}> with <{}>", prefix, namespaces.get(prefix), ns);
		}
		namespaces.put(prefix, ns);
	}

	/**
	 * Get the graph URI for the graph with the given identifier
	 *
	 * @param prefix the identifier
	 * @return the graph URI
	 */
	public String getGraph(String prefix) {

		if (!graphs.containsKey(prefix)) {
			logger.warn("No graph with the identifier {} was found. Available graphs: {}", prefix, graphs);
			return null;
		} else {
			return graphs.get(prefix);
		}
	}

	/**
	 * Get the namespaces for the given identifier
	 *
	 * @param prefix the identifier
	 * @return the namespaces
	 */
	public String getNS(String prefix) {

		if (!namespaces.containsKey(prefix)) {
			logger.error("No graph with the namespace {} was found. Available namespaces: {}", prefix, namespaces);
			return null;
		} else {
			return namespaces.get(prefix);
		}
	}

	/**
	 * Print a human-readable list of graphs in this tree
	 */
	public void print() {

		StringBuilder print = new StringBuilder("Graphs in this " + getClass().getSimpleName() + ":\n");
		graphs.entrySet().forEach(g -> {
			print.append(" * ");
			print.append(g.getKey());
			print.append(": <");
			print.append(g.getValue());
			print.append(">\n");
		});

		print.append("Namespaces in this ");
		print.append(getClass().getSimpleName());
		print.append(":\n");
		namespaces.entrySet().forEach(ns -> {
			print.append(" * ");
			print.append(ns.getKey());
			print.append(": <");
			print.append(ns.getValue());
			print.append(">\n");
		});
		logger.info(print.toString());
	}

	/**
	 * Print the size of this tree and all its related graphs
	 *
	 * @param store the store for which to print the tree sizes
	 */
	public void printSizes(AStoreWrapper store) {

		StringBuilder info = new StringBuilder("Store sizes: overall (" + store.getCount() + ")");
		for (Map.Entry<String, String> e: (new TreeMap<>(graphs)).entrySet()) {
			info.append(", ");
			info.append(e.getKey());
			info.append(" (");
			info.append(store.getCount(getGraph(e.getKey())));
			info.append(")");
		}
		logger.info(info.toString());
	}

	/**
	 * Clear all graphs in this stack from the given store
	 *
	 * @param store the store to clear
	 */
	public void clearAllGraphs(AStoreWrapper store) {
		graphs.values().forEach(g -> store.clearGraph(g));
	}

	/**
	 * Get all graphs in this stack
	 *
	 * @return a map of graphs with their identifiers
	 */
	public Map<String, String> getGraphs() {
		return graphs;
	}

	/**
	 * Get all namespaces in use in this stack
	 *
	 * @return a map of namespaces by short prefix
	 */
	public Map<String, String> getNamespaces() {
		return namespaces;
	}
}
