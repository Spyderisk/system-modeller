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

		ArrayList<String> ontologyNames = new ArrayList<>();
		ArrayList<String> defaultUserOntologies = new ArrayList<>();

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
						PaletteGenerator pg = PaletteGenerator.createPalette(domainModelFolder, domainUri, modelObjectsHelper, new FileInputStream(iconMappingFile));
						paletteCreated = pg.isPaletteCreated();
						if (paletteCreated) {
							ontologyNames.add(domainModelName);
							logger.info("Added domain model: {}", domainModelName);

							boolean defaultUserAccess = pg.isDefaultUserAccess();
							if (defaultUserAccess) {
								logger.info("Adding default user access for {}", domainModelName);
								defaultUserOntologies.add(domainModelName);
							}
						}
					}

					if (!paletteCreated) {
						logger.error("Palette not generated for: " + domainUri);
					}

					//store the list of default user domain models
					modelObjectsHelper.setDefaultUserDomainModels(defaultUserOntologies);				
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

			logger.info("Setting default user access for domain models");
			for (String ontologyName : ontologyNames) {
				if (defaultUserOntologies.contains(ontologyName)) {
					modelObjectsHelper.setUsersForDomainModel(ontologyName, userNames);
					logger.info(ontologyName + ": " + userNames);
				}
			}
		}

		//Final check of installed domain models and palettes
		Map<String, Map<String, Object>> domainModels = storeModelManager.getDomainModels();
		//logger.debug("Domain models in management graph: {}", domainModels);

		logger.info("Checking domain models and palettes...");
		logger.info("");

		boolean palettesExist = true;

		for (String domainModelUri : domainModels.keySet()) {
			Map<String, Object> domainModel = domainModels.get(domainModelUri);
			String title = (String) domainModel.get("title");
			String label = (String) domainModel.get("label");
			String version = (String) domainModel.get("version");

			logger.info("URI: {}", domainModelUri);
			logger.info("title: {}", title);
			logger.info("label: {}", label);
			logger.info("version: {}", version);

			String domainModelName = title;
			String domainModelFolderPath = kbInstallFolder + File.separator + domainModelName;
			String palettePath = domainModelFolderPath + File.separator + "palette.json";
			File paletteFile = new File(palettePath);

			if (paletteFile.exists()) {
				logger.info("palette: {}", paletteFile.getAbsolutePath());
			}
			else {
				logger.error("palette: {}", paletteFile.getAbsolutePath() + " (missing)");
				palettesExist = false;
			}

			logger.info("");
		}

		if (!palettesExist) {
			logger.error("One or more palettes are missing (see details above)");
			System.exit(1);
		}

		logger.info("Finished management graph initialisation");
		logger.info("Spyderisk startup complete!");
	}

}
