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
//      Created Date :          2014-04-07
//      Created for Project:    OPTET
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.semanticstore;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.jena.query.Dataset;

/**
 * This interface specifies a triple store, in which ontology models can be saved for further processing. It can either
 * be a using native libraries or SPARQL endpoints for accessing the store.
 */
public interface IStoreWrapper {

	// Graph management ///////////////////////////////////////////////////////////////////////////
	/**
	 * Checks for the existence of a given graph
	 *
	 * @param graph the URI of the graph to check for
	 * @return whether the graph exists
	 */
	boolean graphExists(String graph);

	/**
	 * Create a new graph. The argument passed is the id or name of the graph and it is up to the implementing method to
	 * decide what to do with it.
	 *
	 * @param graph the id or name of the graph
	 */
	void createGraph(String graph);

	/**
	 * Clears the store which means it is reset to its original state, containing no triples or other content.
	 *
	 * @param graph the graph to be cleared
	 */
	void clearGraph(String graph);
	
	/**
	 * Clears the default graph in the store.
	 */
	void clearDefaultGraph();

	/**
	 * Deletes the given graph if it exists
	 *
	 * @param graph the graph's URI
	 */
	void deleteGraph(String graph);

	/**
	 * Copy the contents of one graph into another one.
	 *
	 * @param from the graph URI of the graph from which to copy
	 * @param to the graph URI of the graph to which to copy
	 */
	void copyGraph(String from, String to);

	/**
	 * Return the amount of triples contained in the store
	 *
	 * @param graph the graph(s) for which to count, no argument for all triples in 
	 * the store.
	 * @return the amount of triples
	 */
	long getCount(String ... graph);
	
	/**
	 * Return the amount of triples contained in the store
	 *
	 * @param graph the graph(s) for which to count, null for the default graph and
	 *		no argument for all triples in the store
	 * @return the amount of triples
	 */
	long getCountDefault();

	/**
	 * Get a list of all graphs in this store
	 *
	 * @return a set of all graph names in this store
	 */
	Set<String> getGraphs();

	// General actions ////////////////////////////////////////////////////////////////////////////
	/**
	 * Start a connection to the store. This does not require a specific graph to be selected yet, but a store
	 * implementation may choose to preselect a default graph.
	 */
	void connect();

	/**
	 * End the connection to the store. Release all resources.
	 */
	void disconnect();
	
	/**
	 * Adds prefixes which are to be prepended to all SPRARQL queries. This makes it unnecessary for the user to 
	 * specify all prefixes manually in each SPARQL query.
	 * 
	 * @param prefixes A map of each prefix to the full String it will be assigned to.
	 */
	void addPrefixes(Map<String, String> prefixes);

	/**
	 * Get all graphs in the store and their sizes
	 *
	 * @return a String containing all graphs and their sizes
	 */
	String getSizes();

	/**
	 * Print all graphs in the store and their sizes, preferrably using a logger
	 */
	void printSizes();

	/**
	 * Queries the store and returns the results in a persistent form (unlike the QueryResult object which is emptied
	 * upon reading/printing)
	 *
	 * @param sparql the SPARQL SELECT query (doesn't need prefix statements if previously specified)
	 * @param graph the graph on which to execute the query or null if no graph specified
	 * @return the results of the query, in whatever form the implementing class chooses
	 */
	Object querySelect(String sparql, String ... graph);

	/**
	 * Queries the store
	 *
	 * @param sparql the SPARQL CONSTRUCT query (doesn't need prefix statements if previously specified)
	 * @param graph the graph on which to execute the query or null if no graph specified
	 * @return the constructed triples in whatever form the implementing class chooses
	 */
	Object queryConstruct(String sparql, String ... graph);

	/**
	 * Queries the store and returns the resulting triples as a model
	 *
	 * @param sparql the SPARQL DESCRIBE query (doesn't need prefix statements if previously specified)
	 * @param graph the graph on which to execute the query or null if no graph specified
	 * @return the results of the query in whatever form the implementing class chooses
	 */
	Object queryDescribe(String sparql, String ... graph);

	/**
	 * Queries the store and returns the result
	 *
	 * @param sparql the SPARQL ASK query (doesn't need prefix statements if previously specified)
	 * @param graph the graph on which to execute the query or null if no graph specified
	 * @return the results of the query
	 */
	boolean queryAsk(String sparql, String ... graph);

	/**
	 * Runs an update (e.g. INSERT, DELETE, ...) on a SPARQL update compatible endpoint
	 *
	 * @param graph the graph on which to execute the query or null if no graph specified. If no graph is given,
	 *			the update is executed against the default graph.
	 * @param sparql the update query to run
	 */
	boolean update(String sparql, String ... graph);

	/**
	 * Translate the result of a select query
	 *
	 * @param results the results object as returned by querySelect(...)
	 * @return a list containing the results where each element is a map of all variables
	 */
	List<Map<String, String>> translateSelectResult(Object results);

	/**
	 * Clear the entire store
	 */
	void clear();

	// Actions that might be executed on a particular graph ///////////////////////////////////////
	/**
	 * Imports an ontology from a document into the store.
	 *
	 * @param path where to find the ontology document. This can be a URL or a path on disk.
	 * @param graph which graph to save it into - default graph is this is null
	 * @param format the format the document is in
	 */
	void loadIntoGraph(String path, String graph, Format format);

	/**
	 * Imports an ontology from a document into the store.
	 *
	 * @param input InputStream for the ontology document (automatically closed on return)
	 * @param graph which graph to save it into - default graph is this is null
	 * @param format the format the document is in
	 */
	void loadIntoGraph(InputStream input, String graph, Format format);

	/**
	 * Load a document into the store. This document should contain quads,
	 * i.e. a set of graphs rather than just a single ontology document
	 *
	 * @param path the path of the file to import
	 */
	void load(String path);

	/**
	 * Load a document into the store. This document should contain quads,
	 * i.e. a set of graphs rather than just a single ontology document
	 *
	 * @param input InputStream to import (automatically closed on return)
	 */
	void load(InputStream input);

	/**
	 * Similar to the above, but only loads into a dataset (useful for parsing)
	 * @param path the path of the file to load
	 * @return dataset
	 */
	Dataset loadDataset(String path);
	
	/**
	 * Load a document into the store. This document should contain quads,
	 * i.e. a set of graphs rather than just a single ontology document
	 *
	 * @param path the path of the file to import
	 * @param oldURI a URI to replace when found as part of a graph URI
	 * @param newURI what to replace it with
	 */
	void load(String path, String oldURI, String newURI);

	/**
	 * Load a document into the store. This document should contain quads,
	 * i.e. a set of graphs rather than just a single ontology document
	 *
	 * @param input InputStream to import (automatically closed on return)
	 * @param oldURI a URI to replace when found as part of a graph URI
	 * @param newURI what to replace it with
	 */
	void load(InputStream input, String oldURI, String newURI);

	/**
	 * Get a serialised representation of the contents of the repository. The implementation decides about the format
	 *
	 * @param format the export format (rdf, turtle or nquads)
	 * @param xmlBase the xml:base for this ontology or null if none. this is only needed for RDF
	 * @param graph the URI of the graph; default graph if this is null
	 * @return the serialised string
	 */
	String export(Format format, String xmlBase, String ... graph);

	/**
	 * Save a data export to file
	 *
	 * @param filename the name of the file without the type
	 * @param format the format (RDF, TTL or NQ)
	 * @param xmlBase the XML base (only needed for RDF - ignored otherwise)
	 * @param compressed whether the output file should be (gz) compressed
	 * @param graph the graph(s) to export
	 */
	public void save(String filename, Format format, String xmlBase, boolean compressed, String... graph);

	/**
	 * Stores a model in the store
	 *
	 * @param m the model to store
	 * @param graph the URI of the graph; default graph if this is null
	 */
	public void storeModel(Object m, String graph);

	/**
	 * Stores models in the store
	 *
	 * @param models the models to store
	 * @param graph the URI of the graph; default graph if this is null
	 */
	public void storeModels(Set<Object> models, String graph);

	/**
	 * Removes a model from the store
	 *
	 * @param m the model to remove
	 * @param graph the URI of the graph; default graph if this is null
	 */
	public void removeModel(Object m, String graph);

	// Simple Getters /////////////////////////////////////////////////////////////////////////////
	/**
	 * Get a String containing of all the prefixes used in the store for querying as they would appear at the beginning
	 * of each SPARQL query
	 *
	 * @return the prefixes
	 */
	String getSPARQLPrefixes();

	/**
	 * Find out if there is an active connection to the store at this instant.
	 *
	 * @return whether it is connected
	 */
	boolean isConnected();

	/**
	 * Gets a copy of the namespace mappings used in the store
	 *
	 * @return the namespace mapping
	 */
	Map<String, String> getPrefixURIMap();

	/**
	 * Get the properties object of this store. May be null if the implementation does not use properties.
	 *
	 * @return the properties
	 */
	Properties getProperties();

	// format enum ////////////////////////////////////////////////////////////////////////////////
	public enum Format {
		RDF, TTL, NQ
	}
}
