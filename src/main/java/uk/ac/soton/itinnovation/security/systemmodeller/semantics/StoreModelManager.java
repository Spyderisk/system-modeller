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
//      Created By :            Gianluca Correndo
//      Created Date :          2017.02.08
//      Modified By :           Stefanie Wiegand
//      Created for Project :   5g-Ensure
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.semantics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import uk.ac.soton.itinnovation.security.semanticstore.AStoreWrapper;
import uk.ac.soton.itinnovation.security.semanticstore.IStoreWrapper;
import uk.ac.soton.itinnovation.security.semanticstore.util.SparqlHelper;

/**
 * Implementation of a IModelManager using a triple store via its IStoreWrapper interface. It defines the methods for
 * managing Domain and System models within the system
 *
 * @author gc
 */
@Component
public class StoreModelManager {

	private static final Logger logger = LoggerFactory.getLogger(StoreModelManager.class);

	private AStoreWrapper store;

	private Map<String, Map<String, String>> queries;
	
	//uris
	private static final String CORE_URI = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/core";
	private static final String DOMAIN_URI = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain";
	private static final String SYSTEM_URI = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system";

	//graph appendices
	public static final String UI_GRAPH = "/ui";
	public static final String INF_GRAPH = "/inf";
	public static final String META_GRAPH = "/meta";

	//Template for the creation of system model uris
	private static final String SYSTEM_TEMPLATE = "http://it-innovation.soton.ac.uk/system/%s";

	@Autowired
	private StoreFactory storeFactory;

	@Value("${model.management.uri}")
	private String managementGraph;

	/**
	 * Initialises this component.
	 */
	@PostConstruct
	public void init() {
		logger.debug("Initialising Store Model manager");
		store = storeFactory.getInstance();
		
		Map<String, String> prefixes = new HashMap<>();
		prefixes.put("void", "http://rdfs.org/ns/void#");
		prefixes.put("dcterms", "http://purl.org/dc/terms/");
		prefixes.put("core", CORE_URI + "#");
		prefixes.put("domain", DOMAIN_URI + "#");
		prefixes.put("system", SYSTEM_URI + "#");
		prefixes.put("acl", "http://www.w3.org/ns/auth/acl#");
		prefixes.put("v", "http://www.w3.org/2006/vcard/ns#");
		store.addPrefixes(prefixes);
		
		String sparqlPrefixes = store.getSPARQLPrefixes();
		logger.debug("SPARQL prefixes:\n" + sparqlPrefixes);

		if (!store.graphExists(managementGraph)) {
			logger.debug("Initialising new graph: {}", managementGraph);
			store.createGraph(managementGraph);
		} else {
			logger.debug("Graph '{}' already exists", managementGraph);
		}

		queries = (Map<String, Map<String, String>>) (new Yaml()).load(StoreModelManager.class.getResourceAsStream("/sparql/managementGraph.yaml"));

		logger.debug("Finished initialising Store Model manager");
	}

	/**
	 * Delete the persistence directory for the store
	 */
	public void deleteDirectory() {
		try {
			FileUtils.deleteDirectory(new File(storeFactory.getFolder()));
			logger.info("Deleted semantic store persistence directory {}", storeFactory.getFolder());
		} catch (IOException ex) {
			logger.error("Could not delete directory {}", storeFactory.getFolder(), ex);
		}
	}

	// CREATE /////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Add a domain model
	 *
	 * @param modelName name of the domain model
	 * @param modelGraph URI string to uniquely identify the model within the store
	 * @param filePath full file path of the RDF model to load
	 * @return the URI of the domain model stored in the system, null otherwise
	 */
	public String loadModel(String modelName, String modelGraph, String filePath) {
		//test loading of model, to ensure it will parse, prior to deleting it!
		store.loadDataset(filePath);
		
		modelGraph = SparqlHelper.escapeURI(modelGraph);
		if (store.graphExists(modelGraph)) {
			logger.info("Graph <{}> for model {} already exists and will be deleted now", modelGraph, modelName);
			deleteModel(modelGraph);
		}

		//The model is loaded and stored in the modelURI graph
		if (filePath.endsWith(".nq.gz") || filePath.endsWith(".nq")) {
			store.load(filePath);
		} else {
			store.loadIntoGraph(filePath, modelGraph, IStoreWrapper.Format.RDF);
		}
		
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		String insertDataSetQuery = String.format(queries.get("insertModel").get("sparql"),
				SparqlHelper.escapeURI(managementGraph),
				modelGraph, SparqlHelper.escapeLiteral(modelName), df.format(new Date())
		);
		store.update(insertDataSetQuery);

		logger.debug("Loaded model {} into graph <{}>: {}", modelName, modelGraph, insertDataSetQuery);
		return modelGraph;
	}
	
	/**
	 * Add a domain model directly from a resource
	 *
	 * @param modelName name of the domain model
	 * @param modelGraph URI string to uniquely identify the model within the store
	 * @param resourcePath resource path of the RDF model to load
	 * @return the URI of the domain model stored in the system, null otherwise
	 */
	public String loadModelFromResource(String modelName, String modelGraph, String resourcePath) {
		
		try {
			File file = exportModelResourceToFile(resourcePath);
			
			if (file != null) {
				if (resourcePath.endsWith(".nq")) {
					store.load(file.getPath());
				}
				else {
					store.loadIntoGraph(file.getPath(), modelGraph, IStoreWrapper.Format.RDF);
				}
			}
			else {
				logger.warn("Cannot load model from file (null)");
			}
			
			store.printSizes();
			
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			String insertDataSetQuery = String.format(queries.get("insertModel").get("sparql"),
					SparqlHelper.escapeURI(managementGraph),
					modelGraph, SparqlHelper.escapeLiteral(modelName), df.format(new Date())
			);
			logger.debug("Adding model {} with graph <{}> into management graph", modelName, modelGraph);
			store.update(insertDataSetQuery);

		} catch (IOException e) {
			logger.error("Could not create temporary file", e);
		}

		//logger.debug("Loaded model {} into graph <{}>: {}", modelName, modelGraph, insertDataSetQuery);
		return modelGraph;
	}
	
	public File exportModelResourceToFile(String resourcePath) throws IOException {
		String sm = getSerialisedModel(resourcePath);
		
		if (sm == null) {
			logger.warn("Not exporting model resource {} to file (getSerialisedModel returned null)", resourcePath);
			return null;
		}
		
		File file;

		if (resourcePath.endsWith(".nq")) {
			file = File.createTempFile("model-", ".nq");
		}
		else {
			file = File.createTempFile("model-", ".rdf");
		}

		//Ensure that file is encoded as UTF8
		try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			writer.write(sm);
		}
		
		return file;
	}

	private String getSerialisedModel(String resource) {

		String result = null;

		InputStream inputStream = null;
		
		try {
			inputStream = getClass().getResourceAsStream(resource);

			final int bufferSize = 1024;
			final char[] buffer = new char[bufferSize];
			final StringBuilder out = new StringBuilder();
			Reader in = new InputStreamReader(inputStream, "UTF-8");
			for (; ; ) {
				int rsz = in.read(buffer, 0, buffer.length);
				if (rsz < 0)
					break;
				out.append(buffer, 0, rsz);
			}
			result = out.toString();
		} catch (FileNotFoundException e) {
			logger.error("Could not find resource {}", resource, e);
		} catch (UnsupportedEncodingException e) {
			logger.error("Encoding in resource {} not supported", resource, e);
		} catch (IOException e) {
			logger.error("Could not read resource {}", resource, e);
		} catch (Exception e) {
			logger.error("Could not read resource {}", resource, e);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					logger.error("Could not close input stream after reading file {}", resource, e);
				}
			}
		}

		return result;
	}
	
	/**
	 * Creates a new system model in the system. Optionally copies from a source model
	 *
	 * @param toModelId the mongoDB ID of the new model. This will be used to create a new URI
	 * @param fromModelUri uri of a source model to copy (or null for a new/empty model)
	 * @param userName name of the user owner of the system
	 * @param domainModelGraph name of the domain model from which this system model extends from.
	 * @return the URI of the system model created
	 */
	public String createSystemModel(String toModelId, String fromModelUri, String userName, String domainModelGraph) {
		return createSystemModel(
			SparqlHelper.escapeURI(String.format(SYSTEM_TEMPLATE, toModelId)), fromModelUri, toModelId, userName, domainModelGraph
		);
	}
	
	/**
	 * Creates a new system model
	 *
	 * @param toModelURI the modelURI to use
	 * @param fromModelUri uri of a source model to copy (or null for a new/empty model)
	 * @param toModelId the mongoDB ID of the new model
	 * @param userName name of the user owner of the system
	 * @param domainModelGraph name of the domain model from which this system model extends from.
	 * @return the URI of the system model created
	 */
	public String createSystemModel(String toModelURI, String fromModelURI, String toModelId, String userName, String domainModelGraph) {
		String systemModelURI = toModelURI;
		String systemModelName = toModelId;
		
		//check if exists
		if (!systemModelExists(systemModelURI)) {
			//create system model
			logger.info("Creating system model <{}> for user '{}' using domain model <{}>",
					systemModelURI, userName, domainModelGraph);
			if (fromModelURI == null) {
				store.createGraph(systemModelURI);

				//Assert import to the domain model
				if (domainModelGraph!=null) {
					addImport(systemModelURI, domainModelGraph);
				}

			} else {
				//N.B. no need to assert domainModelGraph here, as this triple is copied along with the system graph
				store.copyGraph(fromModelURI, systemModelURI);
			}

			Date now = new Date();
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			String insertSystemModelQuery = String.format(
				queries.get("insertSystemModel").get("sparql"),
				SparqlHelper.escapeURI(managementGraph),
				SparqlHelper.escapeURI(systemModelURI),
				SparqlHelper.escapeLiteral(systemModelName),
				SparqlHelper.escapeLiteral(df.format(now)),
				SparqlHelper.escapeLiteral(userName)
			);
			//logger.debug(insertSystemModelQuery);
			store.update(insertSystemModelQuery);

			insertSecondaryGraph(systemModelURI, fromModelURI, UI_GRAPH, "UI", df.format(now));
			insertSecondaryGraph(systemModelURI, fromModelURI, INF_GRAPH, "inference", df.format(now));
			insertSecondaryGraph(systemModelURI, fromModelURI, META_GRAPH, "metadata", df.format(now));

			return systemModelURI;
		} else {
			throw new RuntimeException("System model <" + systemModelURI + "> already exists");
		}
	}
	
	private void insertSecondaryGraph(String systemModelURI, String fromModelURI, String suffix, String name, String date) {
		String graphURI = systemModelURI + suffix;

		logger.debug("Generating {} graph <{}>", name, graphURI);
		if (fromModelURI == null) {
			store.createGraph(graphURI);
		} else {
			String fromGraphURI = fromModelURI + suffix;
			store.copyGraph(fromGraphURI, graphURI);
		}

		String insertQuery = String.format(
			queries.get("insertModel").get("sparql"),
			SparqlHelper.escapeURI(managementGraph),
			SparqlHelper.escapeURI(graphURI),
			SparqlHelper.escapeLiteral(systemModelURI + " " + name + " graph"),
			SparqlHelper.escapeLiteral(date)
		);

		logger.debug("Inserting {} graph <{}>", name, graphURI);
		store.update(insertQuery);
	}

	// READ ///////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public String getManagementGraph() {
		return managementGraph;
	}
	
	/**
	 * Find out if a system model with the given URI exists
	 *
	 * @param systemModelGraph the graph URI of the system model
	 * @return true if it exists, false if not
	 */
	public boolean systemModelExists(String systemModelGraph) {

		String alreadyExistsQuery = String.format(queries.get("askSystemModel").get("sparql"),
			SparqlHelper.escapeLiteral(systemModelGraph)
		);
		boolean result = store.queryAsk(alreadyExistsQuery, managementGraph);
		logger.debug("Find out if the system model <{}> already exists: {}", systemModelGraph, result);

		return result;
	}
	
	/**
	 * Returns the URIs of all graphs imported by the system model
	 *
	 * @param modelURI
	 * @param systemModelURI String URI of the system model
	 * @return collection of String URIs
	 */
	public Collection<String> getImports(String modelURI) {

		String getImportsQuery = String.format(queries.get("getImports").get("sparql"),
			SparqlHelper.escapeURI(modelURI)
		);
		logger.debug("Retrieve all URIs imported by {}", modelURI);
		List<Map<String, String>> importedURIs = store.translateSelectResult(
			store.querySelect(getImportsQuery, modelURI)
		);
		Collection<String> result = importedURIs.stream().map(binding -> binding.get("uri")).collect(Collectors.toList());
		logger.debug("Imports for <{}>: {}", modelURI, result);
		return result;
	}

	/**
	 * Get the URI of the graph in which a model is saved
	 *
	 * @param modelName the name of the model
	 * @return the URI or null if the model doesn't exist in the store
	 */
	public String getModelURI(String modelName) {

		logger.debug("Finding graph for model {}", modelName);
		String getModelURIQuery = String.format(queries.get("selectModel").get("sparql"),
				SparqlHelper.escapeLiteral(modelName)
		);
		List<Map<String, String>> modelURI = store.translateSelectResult(
				store.querySelect(getModelURIQuery, managementGraph)
		);
		if (!modelURI.isEmpty()) {
			return modelURI.iterator().next().get("uri");
		} else {
			return null;
		}
	}

	/**
	 * Returns all domain models stored in the system.
	 *
	 * @return a map of Strings where each key is the String URIs of the domain model and the value is its name.
	 */
	public Map<String, Map<String, Object>> getDomainModels() {

		HashMap<String, String> domainModelNames = new HashMap<>();
		
		//Get names (and graph names) of models listed in management graph
		List<Map<String, String>> modelsList = store.translateSelectResult(
			store.querySelect(queries.get("getDomainModelNames").get("sparql"))
		);
		
		modelsList.forEach(model -> {
			//select domain models from results
			if (model.get("title").startsWith("domain")) {
				domainModelNames.put(model.get("title"), model.get("g"));
			}
		});
		
		List<Map<String, String>> domainModels = store.translateSelectResult(
			store.querySelect(queries.get("getDomainModels").get("sparql"))
		);
		
		Map<String, Map<String, Object>> result = new HashMap<>();
		
		domainModels.forEach(solution -> {
			Map<String, Object> domain = new HashMap<>();
			domain.put("label", solution.get("label"));
			domain.put("title", solution.get("title"));
			domain.put("comment", solution.get("comment"));
			domain.put("version", solution.get("version"));
			domain.put("loaded", true);
			result.put(solution.get("g"), domain);
			
			//if retrieved model is in the list of model names, remove it
			if (domainModelNames.containsKey(solution.get("title"))) {
				domainModelNames.remove(solution.get("title"));
			}
		});
		
		//check remaining domain models - if any still exist, they haven't been loaded, so create an entry using available data
		//N.B. this allows the domain model to be listed for the user, so they can choose to upload a new version
		domainModelNames.keySet().forEach(title -> {
			String graph = domainModelNames.get(title);
			logger.warn("Domain model not available: {}, {}", title, graph);
			Map<String, Object> domain = new HashMap<>();
			domain.put("label", title);
			domain.put("title", title);
			domain.put("comment", "Not available (may still be uploaded)");
			domain.put("version", "N/A");
			domain.put("loaded", false);
			result.put(graph, domain);
		});
		
		return result;
	}

	/**
	 * Retrieve or generate the inferred model of a system model
	 *
	 * @param systemModelURI URI string of the system model the inferred model is generated from
	 * @return String URI of the system model's inferred model
	 */
	public String getInferredModel(String systemModelURI) {
		//no need to check for existence: will be created along with the system model and never be dropped, only cleared
		return systemModelURI + INF_GRAPH;
	}
	
	/**
	 * Retrieve or generate the model containing the UI information of a system model
	 *
	 * @param systemModelURI URI string of the system model the UI model is generated from
	 * @return String URI of the system model's UI model
	 */
	public String getUIModel(String systemModelURI) {
		//no need to check for existence: will be created along with the system model and never be dropped, only cleared
		return systemModelURI + UI_GRAPH;
	}

	/**
	 * Retrieve or generate the model containing the UI information of a system model
	 *
	 * @param systemModelURI URI string of the system model the UI model is generated from
	 * @return String URI of the system model's UI model
	 */
	public String getMetaModel(String systemModelURI) {
		//no need to check for existence: will be created along with the system model and never be dropped, only cleared
		return systemModelURI + META_GRAPH;
	}
	
	/**
	 * Retrieve a list of system model's URIs created by the user.
	 *
	 * @param userName user name
	 * @return list of graph uris for the user's system models
	 */
	public Collection<String> getSystemModelsByUserId(String userName) {

		//TODO: find out why this returns no models
		List<Map<String, String>> systemModels = store.translateSelectResult(store.querySelect(
				String.format(queries.get("getSystemModelsByUsername").get("sparql"),
						SparqlHelper.escapeLiteral(userName)), managementGraph)
		);
		ArrayList<String> result = new ArrayList<>();
		systemModels.forEach(solution -> result.add(solution.get("system")));
		return result;
	}

	public Collection<String> getSystemModels() {

		//logger.debug("{}", queries.get("getSystemModels").get("sparql"));
		List<Map<String, String>> systemModels = store.translateSelectResult(
			store.querySelect(queries.get("getSystemModels").get("sparql"), managementGraph)
		);
		ArrayList<String> result = new ArrayList<>();
		systemModels.forEach(solution -> result.add(solution.get("domain")));
		return result;
	}

	// UPDATE /////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Change the URI of a system model. Don't forget that ultimately it needs to correspond to the graph URI!
	 * 
	 * @param graphURI the URI of the graph in which the system model is stored
	 * @param oldURI the old URI
	 * @param newURI the new URI with which the old URI is to be replaced
	 */
	public void changeSystemModelURI(String graphURI, String oldURI, String newURI) {
		
		String sparql = String.format(queries.get("changeSystemModelURI").get("sparql"),
			SparqlHelper.escapeURI(graphURI),
			SparqlHelper.escapeURI(graphURI),
			SparqlHelper.escapeURI(oldURI),
			SparqlHelper.escapeURI(newURI),
			SparqlHelper.escapeURI(graphURI)
		);
		logger.debug("Changing system model URI from <{}> to <{}> in graph <{}>", oldURI, newURI, graphURI);
		store.update(sparql);
	}
	
	/**
	 * Assert an import relationship between two models in the system.
	 *
	 * @param systemGraph String URI of the model which imports
	 * @param domainGraph String URI of the model which is imported
	 */
	public void addImport(String systemGraph, String domainGraph) {

		String addImportQuery = String.format(queries.get("addImport").get("sparql"),
			SparqlHelper.escapeURI(systemGraph),
			SparqlHelper.escapeURI(systemGraph), SparqlHelper.escapeURI(domainGraph),
			SparqlHelper.escapeURI(SYSTEM_URI),
			SparqlHelper.escapeURI(SYSTEM_URI), SparqlHelper.escapeURI(DOMAIN_URI)
		);
		logger.debug("Asserting <{}> imports <{}> ", systemGraph, domainGraph);
		store.update(addImportQuery);
	}
	
	/**
	 * Delete an import relationship between two models in the system.
	 *
	 * @param systemGraph String URI of the model which imports
	 * @param domainGraph String URI of the model which is imported
	 */
	public void deleteImport(String systemGraph, String domainGraph) {

		String deleteImportQuery = String.format(queries.get("deleteImport").get("sparql"),
			SparqlHelper.escapeURI(systemGraph),
			SparqlHelper.escapeURI(systemGraph), SparqlHelper.escapeURI(domainGraph),
			SparqlHelper.escapeURI(SYSTEM_URI),
			SparqlHelper.escapeURI(SYSTEM_URI), SparqlHelper.escapeURI(DOMAIN_URI)
		);
		logger.debug("Retracting {} imports {} ", systemGraph, domainGraph);
		store.update(deleteImportQuery);
	}
	
	/**
	 * Delete the inferred model of a system model
	 *
	 * @param systemModelURI URI string of the system model the inferred model is generated from
	 */
	public void deleteInferredModel(String systemModelURI) {
		clearGraph(systemModelURI + INF_GRAPH);
	}
	
	/**
	 * Delete the UI model of a system model
	 *
	 * @param systemModelURI URI string of the system model the UI model is generated from
	 */
	public void deleteUIModel(String systemModelURI) {
		clearGraph(systemModelURI + UI_GRAPH);
	}
	
	/**
	 * Delete the Meta model of a system model
	 *
	 * @param systemModelURI URI string of the system model the UI model is generated from
	 */
	public void deleteMetaModel(String systemModelURI) {
		clearGraph(systemModelURI + META_GRAPH);
	}
	
	// DELETE /////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Deletes a domain model from the system
	 *
	 * @param modelGraph String URI of the domain model to delete
	 */
	public boolean deleteModel(String modelGraph) {

		modelGraph = SparqlHelper.escapeURI(modelGraph);
		if (store.graphExists(modelGraph)) {
			//clear before delete to be on the safe side!
			clearGraph(modelGraph);
			logger.debug("Deleting graph for model <{}>", modelGraph);
			store.deleteGraph(modelGraph);
		} else {
			logger.error("Cannot delete model: graph <{}> doesn't exist", modelGraph);
			return false;
		}

		//delete from management graph
		logger.debug("Deleting model <{}> from management graph", modelGraph);
		store.update(String.format(queries.get("deleteSystemModel").get("sparql"),
			managementGraph, modelGraph, managementGraph)
		);

		return true;
	}

	/**
	 * Delete a system model in the system by using the unique URI associated with it.
	 *
	 * @param systemModelURI String URI of the system model to delete
	 */
	public void deleteSystemModel(String systemModelURI) {

		logger.info("Deleting system model from graph <{}>", systemModelURI);
		deleteModel(systemModelURI);
		deleteInferredModel(systemModelURI);
		deleteUIModel(systemModelURI);
		deleteMetaModel(systemModelURI);
	}

	private void clearGraph(String uri) {
		logger.debug("Clearing graph: {}", uri);
		store.clearGraph(SparqlHelper.escapeURI(uri));
	}

	/**
	 * Clears management graph.
	 */
	public void clearMgtGraph() {
		clearGraph(managementGraph);
	}
	
	// Getters/Setters ////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Returns store object.
	 *
	 * @return
	 */
	public AStoreWrapper getStore() {
		return store;
	}

	public boolean storeIsEmpty() {
		boolean isEmpty = store.getCount() == 0;

		logger.info("Store is empty: {}", isEmpty);

		return isEmpty;
	}
}
