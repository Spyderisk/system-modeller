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
//      Created Date :          2014-12-04
//      Created for Project :   OPTET
//		Modified for Project :	ASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.semanticstore;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.semanticstore.util.SparqlHelper;

/**
 * This class provides the basis for store wrapper implementations. A store wrapper is a connection to *one* store.
 *
 * @author Stefanie Wiegand
 */
public abstract class AStoreWrapper implements IStoreWrapper {

	private static final Logger logger = LoggerFactory.getLogger(AStoreWrapper.class);

	protected Properties props;
	protected Map<String, String> prefixURIMap;
	protected String sparqlPrefixes;
	protected boolean connected;

	/**
	 * It is highly recommended to call this constructor in any implementing classes' constructor. This preloads a
	 * number of commonly used prefixes for easier querying.
	 */
	protected AStoreWrapper() {

		props = new Properties();
		sparqlPrefixes = "";
		connected = false;
		prefixURIMap = new HashMap<>();

		//add a couple of standard prefixes - also see JenaOntologyManager
		Map<String, String> prefixes = new HashMap<>();
		prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		prefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");
		prefixes.put("owl", "http://www.w3.org/2002/07/owl#");
		prefixes.put("fn", "http://www.w3.org/2005/xpath-functions#");
		addPrefixes(prefixes);	
	}

	// static methods /////////////////////////////////////////////////////////////////////////////
	/**
	 * Use graphs (FROM (NAMED) <http://graphURI>) in query if any are given
	 *
	 * @param sparql the SPARQL query without graphs
	 * @param type QUERY or UPDATE
	 * @param graph the graph(s) if any
	 * @return the modified SPARQL query
	 */
	public static String addGraphsToSparql(String sparql, SparqlType type, String... graph) {

		//use graph if given
		if (graph != null && graph.length > 0) {

			String keyword = type == SparqlType.QUERY ? "FROM" : "USING";

			String graphs = "";
			String graphsString = "";
			for (String g : graph) {
				if (g != null) {
					//use both ways of referring to graphs:

					//--for "normal" querying/updating
					String normal = "\n" + keyword + " <" + SparqlHelper.escapeURI(g) + ">";
					if (!sparql.contains(normal) && !graphs.contains(normal)) {
						graphs += normal;
					}
					//--for using GRAPH blocks within the query/update
					String named = "\n" + keyword + " NAMED <" + SparqlHelper.escapeURI(g) + ">";
					if (!sparql.contains(named) && !graphs.contains(named)) {
						graphs += named;
					}
					//for logging if something goes wrong
					graphsString += "<" + g + "> ";
				} else {
					throw new IllegalArgumentException("'null' not a valid graph.");
				}
			}
			graphs += "\nWHERE";
			if (sparql.contains("WHERE")) {
				sparql = sparql.replaceFirst("WHERE", graphs);
			} else if (sparql.contains("where")) {
				sparql = sparql.replaceFirst("where", graphs);
			} else {
				if (type.equals(SparqlType.QUERY)) {
					logger.warn("The SPARQL query doesn't contain a WHERE clause, so the given graphs ({}) "
							+ "could not be used:\n{}\nPlease use WHERE when using graphs",
							graphsString, sparql);
				} else {
					logger.warn("The SPARQL update doesn't contain a WHERE clause, so the given graphs ({}) "
							+ "could not be used:\n{}\nPlease add them to your query manually! "
							+ "See https://www.w3.org/TR/sparql11-update/#graphUpdate for more information.",
							graphsString, sparql);
				}
			}
		}
		return sparql;
	}

	// Graph management ///////////////////////////////////////////////////////////////////////////
	@Override
	public void copyGraph(String from, String to) {
		logger.error("Copying graphs currently not supported for store of type {}", this.getClass());
	}

	@Override
	public Set<String> getGraphs() {
		logger.info("Getting sizes not supported for store of type {}", this.getClass());
		return null;
	}

	// General actions ////////////////////////////////////////////////////////////////////////////
	@Override
	public String getSizes() {
		logger.info("Getting sizes not supported for store of type {}", this.getClass());
		return null;
	}

	@Override
	public void printSizes() {
		logger.info("Printing sizes not supported for store of type {}", this.getClass());
	}

	@Override
	public Object querySelect(String sparql, String... graph) {
		logger.error("SELECT queries currently not supported for store of type {}", this.getClass());
		return null;
	}

	@Override
	public Object queryConstruct(String sparql, String... graph) {
		logger.error("CONSTRUCT queries currently not supported for store of type {}", this.getClass());
		return null;
	}

	@Override
	public Object queryDescribe(String sparql, String... graph) {
		logger.error("DESCRIBE queries currently not supported for store of type {}", this.getClass());
		return null;
	}

	@Override
	public boolean queryAsk(String sparql, String... graph) {
		logger.error("ASK queries currently not supported for store of type {}", this.getClass());
		return false;
	}

	@Override
	public boolean update(String sparql, String... graph) {
		logger.error("UPDATEs not currently supported for store of type {}", this.getClass());
		return false;
	}

	@Override
	public List<Map<String, String>> translateSelectResult(Object results) {
		logger.error("Translating SELECT results has not yet been implemented for store of type {}", this.getClass());
		return null;
	}

	@Override
	public void clear() {
		logger.error("Clearing not currently supported for store of type {}", this.getClass());
	}

	// Actions on a particular graph //////////////////////////////////////////////////////////////
	@Override
	public void loadIntoGraph(String path, String graph, Format format) {
		logger.error("Importing models from file not currently supported for store of type {}", this.getClass());
	}

	@Override
	public void loadIntoGraph(InputStream input, String graph, Format format) {
		logger.error("Importing models from input streams not currently supported for store of type {}", this.getClass());
	}

	@Override
	public Dataset loadDataset(String path) {
		logger.error("Importing dataset not currently supported for store of type {}", this.getClass());
		return null;
	}
	
	@Override
	public void load(String path) {
		logger.error("Importing multi-graph models from file not currently supported for store of type {}", this.getClass());
	}

	@Override
	public void load(InputStream input) {
		logger.error("Importing multi-graph models from input streams not currently supported for store of type {}", this.getClass());
	}

	@Override
	public void load(String path, String oldURI, String newURI) {
		logger.error("Importing multi-graph models with updating graph URIs from file not currently supported for store of type {}", this.getClass());
	}

	@Override
	public void load(InputStream input, String oldURI, String newURI) {
		logger.error("Importing multi-graph models with updating graph URIs from input streams not currently supported for store of type {}", this.getClass());
	}

	@Override
	public String export(Format format, String xmlBase, String... graph) {
		logger.error("Exporting store contents not currently supported for store of type {}", this.getClass());
		return null;
	}

	@Override
	public void save(String filename, Format format, String xmlBase, boolean compressed, String... graph) {
		logger.error("Exporting store contents not currently supported for store of type {}", this.getClass());
	}

	@Override
	public void storeModel(Object m, String graph) {
		logger.error("Storing models currently not supported for store of type {}", this.getClass());
	}

	@Override
	public void storeModels(Set<Object> m, String graph) {
		logger.error("Storing models currently not supported for store of type {}", this.getClass());
	}

	@Override
	public void removeModel(Object m, String graph) {
		logger.error("Removing models currently not supported for store of type {}", this.getClass());
	}

	// Getters/Setters ////////////////////////////////////////////////////////////////////////////
	@Override
	public String getSPARQLPrefixes() {
		return sparqlPrefixes;
	}

	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	public Map<String, String> getPrefixURIMap() {
		Map<String, String> prefixMapCopy = new HashMap<>();
		prefixMapCopy.putAll(prefixURIMap);
		return prefixMapCopy;
	}

	@Override
	public Properties getProperties() {
		return props;
	}
	
	@Override
	public void addPrefixes(Map<String, String> prefixes) {
		prefixURIMap.putAll(prefixes);
		sparqlPrefixes = "";
		prefixURIMap.entrySet().stream().filter(e -> e.getKey() != null && e.getValue() != null).forEach(
				e -> sparqlPrefixes += "PREFIX " + e.getKey() + ":<" + e.getValue() + ">\n");
	}

	// ENUM ///////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Whether a SPARQL request is a read-only query (SELECT, CONSTRUCT, DESCRIBE, ASK) or a read-write update (DELETE,
	 * INSERT,...)
	 */
	public static enum SparqlType {
		QUERY, UPDATE
	}

}
