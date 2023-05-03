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
//      Created for Project :   5g-Ensure
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.ac.soton.itinnovation.security.systemmodeller.auth.KeycloakAdminClient;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.StoreModelManager;
import uk.ac.soton.itinnovation.security.systemmodeller.util.DomainModelUtils;
import uk.ac.soton.itinnovation.security.systemmodeller.util.PaletteGenerator;

/**
 * Set up the management graph in the triple store
 */
@SpringBootApplication
public class InitializeManagementGraph implements CommandLineRunner {

	private final Logger logger = LoggerFactory.getLogger(InitializeManagementGraph.class);

	@Autowired
	private StoreModelManager storeModelManager;
	
	@Autowired
	private ModelObjectsHelper modelObjectsHelper;

	@Autowired
	private KeycloakAdminClient keycloakAdminClient;

	@Value("${reset.on.start}")
	private boolean resetOnStart;
	
	@Value("${knowledgebases.source.folder}")
	private String kbSourceFolder;

	@Value("${knowledgebases.install.folder}")
	private String kbInstallFolder;

	@Override
	public void run(String... args) {

		//logger.info("Getting list of domain models from ontologies.json");

		ArrayList<String> ontologyNames = new ArrayList<>();
		ArrayList<String> defaultUserOntologies = new ArrayList<>();

		/*
		try {
			//open ontologies.json
			JSONArray json = new JSONArray(IOUtils.toString(
				PaletteGenerator.class.getClassLoader().getResourceAsStream("static/data/ontologies.json")
			));

			//for each ontology:
			for (int i = 0; i < json.length(); i++) {
				//parse the JSON file to get the ontology name
				JSONObject ont = (JSONObject) json.get(i);
				String ontology = ont.getString("ontology");
				String ontologyName = ontology.replace(".rdf", "").replace(".nq", "");
				ontologyNames.add(ontologyName);

				boolean defaultUserAccess = ont.getBoolean("defaultUserAccess");
				logger.info("Found {}: default user access = {}", ontology, defaultUserAccess);
				if (defaultUserAccess) {
					defaultUserOntologies.add(ontologyName);
				}
			}
		} catch (IOException ex) {
			logger.error("Could not load ontologies from ontologies.json", ex);
			System.exit(1);
		}

		//store the list of default user domain models
		modelObjectsHelper.setDefaultUserDomainModels(defaultUserOntologies);
		*/

		if (resetOnStart || storeModelManager.storeIsEmpty()) {
			logger.info("Running management graph initialisation");

			//clear all graphs - this will include orphaned models
			String sparql = "SELECT ?g (STR(COUNT(?s)) AS ?num) WHERE {\n" +
			"	GRAPH ?g { ?s ?p ?o }\n" +
			"} GROUP BY ?g ORDER BY ?g";
			List<Map<String, String>> result = storeModelManager.getStore().translateSelectResult(storeModelManager.getStore().querySelect(sparql));
			for (Map<String, String> row: result) {
				logger.debug("Deleting graph {}, containing {} triples", row.get("g"), row.get("num"));
				if(row.get("g")!= null){
					storeModelManager.getStore().clearGraph(row.get("g"));
				}
			}

			//load core model directly from core.rdf in the CoreModel dependency (jar file)
			String coreModelName = "core";
			String coreModelGraph = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/core";
			String resourcePath = "/core.rdf";
			storeModelManager.loadModelFromResource(coreModelName, coreModelGraph, resourcePath);
			logger.info("Added core model to the management graph");

			//get resources dir in build folder
			//String resourcesDir = PaletteGenerator.class.getClassLoader().getResource("static/data").getPath();
					
			/*
			for (String ontologyName : ontologyNames) {
				String graph = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/" + ontologyName;
				String ontology = ontologyName + ".nq";
				logger.info("Adding domain model {} to the management graph", ontology);

				String ontologyResourcePath = "/" + ontology;
				storeModelManager.loadModelFromResource(ontologyName, graph, ontologyResourcePath);

				generatePaletteOrExit(ontologyName);
			}
			*/
			
			logger.info("Checking for knowledgebases source folder...");
	
			//List of identified zip files located in knowledgebases source folder
			ArrayList<File> zipfiles = new ArrayList<>();
	
			try {
				File kbDataDir = new File(kbSourceFolder);
				if (kbDataDir.isDirectory()) {
					File[] fileList = kbDataDir.listFiles();
					logger.info("Located .zip files: ");
					if (fileList != null) {
						for (File file : fileList) {
							if (!file.isDirectory()) {
								String filename = file.getName();
								if (filename.endsWith(".zip")) {
									logger.info(filename);
									zipfiles.add(file);
								}
							}
						}
					}
				}
				else {
					logger.error("Cannot locate knowledgebases source folder: {}", kbDataDir);
					System.exit(1);
				}
	
				DomainModelUtils domainModelUtils= new DomainModelUtils();
		
				for (File zipfile : zipfiles) {
					Map<String, String> results = domainModelUtils.extractDomainBundle(kbInstallFolder, zipfile, true, null, null);

					String domainUri = null;
					String domainModelName = null;
					String domainModelFolder = null;
					File iconMappingFile = null;
					String nqFilepath = null;

					if (results.containsKey("domainUri")) {
						domainUri = results.get("domainUri");
					}
		
					if (results.containsKey("domainModelName")) {
						domainModelName = results.get("domainModelName");
					}

					if (results.containsKey("domainModelFolder")) {
						domainModelFolder = results.get("domainModelFolder");
					}
		
					if (results.containsKey("nqFilepath")) {
						File f = new File(results.get("nqFilepath"));
						nqFilepath = f.getAbsolutePath();
					}
					
					if (results.containsKey("iconMappingFile")) {
						iconMappingFile = new File(results.get("iconMappingFile"));
					}
		
					logger.debug("domainUri: {}", domainUri);
					logger.info("domainModelName: {}", domainModelName);
					logger.debug("domain model file: {}", nqFilepath);

					logger.info("Adding domain model {} to the management graph", domainModelName);
					storeModelManager.loadModel(domainModelName, domainUri, nqFilepath);

					//Create palette using icon mappings file
					boolean paletteCreated = false;

					if (iconMappingFile != null) {
						logger.debug("Loading icon mappings from file: {}", iconMappingFile.getAbsolutePath());
						logger.info("Creating palette for {}", domainUri);
						paletteCreated = PaletteGenerator.createPalette(domainModelFolder, domainUri, modelObjectsHelper, new FileInputStream(iconMappingFile));
					}

					if (!paletteCreated) {
						logger.error("Palette not generated for: " + domainUri);
					}
					
				}
	
			}
			catch (IOException ex) {
				logger.error("Could not load domain models", ex);
				System.exit(1);
			}
			
			logger.debug("Domain models in management graph: {}", storeModelManager.getDomainModels());

			logger.info("Putting all users in the management graph");
			List<UserRepresentation> users = keycloakAdminClient.getAllUsers();
			ArrayList<String> userNames = new ArrayList<>();

			for (UserRepresentation u : users){
				logger.debug("{}", u.getUsername());
				modelObjectsHelper.createNewUser(u.getUsername(), u.getEmail());
				userNames.add(u.getUsername());
			}

			/*
			logger.info("Setting default user access for domain models");
			for (String ontologyName : ontologyNames) {
				if (defaultUserOntologies.contains(ontologyName)) {
					modelObjectsHelper.setUsersForDomainModel(ontologyName, userNames);
					logger.info(ontologyName + ": " + userNames);
				}
			}
			*/
		} else {
			// Jena is not empty so we presume BOTH Jena and Mongo have been initialised.
			// However, it is still possible that this is a new SSM container using
			// previously created DB volumes. So we must check that the palettes exist
			// as they reside within the container itself.
			logger.info("Checking for palettes");

			//TODO: rewrite this

			/*
			for (String ontologyName : ontologyNames) {
				String paletteFile = "/static/data/palette-" + ontologyName + ".json";

				if (this.getClass().getResource(paletteFile) == null) {
					logger.info("Palette does not exist: {}. Regenerating...", paletteFile);
					generatePaletteOrExit(ontologyName);
				}
				else {
					logger.info("Found {}", paletteFile);
				}
			}
			*/
		}

		logger.info("Finished management graph initialisation");
	}

	private void generatePaletteOrExit(String ontologyName) {
		logger.info("Generating palette for {}", ontologyName);

		String graph = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/" + ontologyName;
		String domainModelFolder = kbInstallFolder + File.separator + ontologyName;

		boolean paletteCreated = false;

		try {
			paletteCreated = PaletteGenerator.createPalette(domainModelFolder, graph, modelObjectsHelper);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (!paletteCreated) {
			System.exit(1);
		}
	}
}
