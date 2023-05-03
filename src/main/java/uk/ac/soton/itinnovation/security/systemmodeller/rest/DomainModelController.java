/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2017
//
// Copyright in this library belongs to the University of Southampton
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
//  Created By :            Oliver Hayes
//  Created Date :          2017-08-21
//  Created for Project :   5G-ENSURE
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.UnexpectedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.zip.GZIPInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import uk.ac.soton.itinnovation.security.semanticstore.IStoreWrapper;
import uk.ac.soton.itinnovation.security.systemmodeller.auth.KeycloakAdminClient;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.Message;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.StoreModelManager;
import uk.ac.soton.itinnovation.security.systemmodeller.util.DomainModelUtils;
import uk.ac.soton.itinnovation.security.systemmodeller.util.PaletteGenerator;

@RestController
@RequestMapping("/domains")
public class DomainModelController {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private StoreModelManager storeModelManager;

	@Autowired
	private KeycloakAdminClient keycloakAdminClient;

	@Autowired
	private ModelObjectsHelper modelObjectsHelper;

	@Value("${admin-role}")
	public String adminRole;

	@Value("${knowledgebases.install.folder}")
	private String kbInstallFolder;

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public Map<String, Map<String, Object>> getDomainModels(){
		String username = keycloakAdminClient.getCurrentUser().getUsername();
		logger.debug("Getting domain models for: {}", username);

		// If this is the user's first login we need to add them to
		// the management graph and set their default domain model
		// access.
		modelObjectsHelper.syncUsers();

		if (keycloakAdminClient.currentUserHasRole(adminRole)) {
			logger.debug("Admin user: returning all domain models");
			return storeModelManager.getDomainModels();
		} else {
			Map<String, Map<String, Object>> x = modelObjectsHelper.getDomainModelsForUser(username);;
			logger.debug("{}", x);
			return x;
		}
	}

	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	public ResponseEntity<?> uploadNewDomainVersion(@RequestParam("file") MultipartFile file,
						@RequestParam(value = "domainUri", required = false) String domainUri,
						@RequestParam(value = "newDomain", required = false) boolean newDomain
						) throws IOException {

		String domainModelName;
		String domainModelFolder = null;

		if (newDomain) {
			logger.info("Uploading new domain model");
			domainModelName = "newDomain"; //provisional name until we determine domain name from mapping file
		}
		else {
			logger.info("Uploading domain model with given domain URI: {}", domainUri);
			domainModelName = domainUri.substring(domainUri.lastIndexOf('/') + 1);
		}

		logger.debug("domainModelName: {}", domainModelName);
	
		boolean rdf = file.getOriginalFilename().endsWith(".rdf") || file.getOriginalFilename().endsWith(".rdf.gz");
		boolean gz = file.getOriginalFilename().endsWith(".gz");
		boolean zip = file.getOriginalFilename().endsWith(".zip");
	
		File f = File.createTempFile(domainModelName, zip ? ".zip" : (rdf ? ".rdf" : ".nq"));

		logger.debug(f.getAbsolutePath());

		try {
			if (gz) {
				InputStream inputStream;
			
				inputStream = new GZIPInputStream(file.getInputStream());

				FileOutputStream output = new FileOutputStream(f.getAbsolutePath());
				byte[] buffer = new byte[1024];
				int len;
			
				while ((len = inputStream.read(buffer)) > 0) {
					output.write(buffer, 0, len);
				}

			} else {
				file.transferTo(f);
			}
		} catch (IOException e) {
			logger.error("Could not upload domain model", e);
		}
		
		File iconMappingFile = null;

		if (zip) {
			DomainModelUtils domainModelUtils= new DomainModelUtils();
			Map<String, String> result = domainModelUtils.extractDomainBundle(kbInstallFolder, f, newDomain, domainUri, domainModelName);
			logger.debug("Removing tmp zipfile: {}", f);
			f.delete();

			if (result.containsKey("domainUri")) {
				domainUri = result.get("domainUri");
			}

			if (result.containsKey("domainModelName")) {
				domainModelName = result.get("domainModelName");
			}

			if (result.containsKey("domainModelFolder")) {
				domainModelFolder = result.get("domainModelFolder");
			}
		
			if (result.containsKey("nqFilepath")) {
				f = new File(result.get("nqFilepath"));
			}
			
			if (result.containsKey("iconMappingFile")) {
				iconMappingFile = new File(result.get("iconMappingFile"));
			}
		}

		//storeModelManager.deleteModel(domainUri); //KEM loadModel handles the delete
		logger.debug("domainUri: {}", domainUri);
		logger.info("domainModelName: {}", domainModelName);
		logger.info("domainModelFolder: {}", domainModelFolder);
		logger.debug("domain model file: {}", f.getAbsolutePath());
		logger.debug("icon mapping file: {}", iconMappingFile.getAbsolutePath());
		storeModelManager.loadModel(domainModelName, domainUri, f.getAbsolutePath());
   
		boolean paletteCreated = false;

		//First, use icon mapping file, if available (specific for this domain model)
		if (iconMappingFile != null) {
			logger.debug("Loading icon mappings from file: {}", iconMappingFile.getAbsolutePath());
			paletteCreated = PaletteGenerator.createPalette(domainModelFolder, domainUri, modelObjectsHelper, new FileInputStream(iconMappingFile));
		}

		if (!paletteCreated) {
			logger.error("Palette not generated for: " + domainUri);
		}

		return ResponseEntity.ok(getDomainModels());
	}

	@RequestMapping(value = "/{domain}", method = RequestMethod.DELETE)
	public ResponseEntity<?> deleteDomainModel(@PathVariable String domain) throws IOException {

		logger.info("DELETE domain model: {}", domain);

		String modelGraph = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/" + domain;

		if (! storeModelManager.deleteModel(modelGraph)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Unknown domain model: " + domain);
		}

		return ResponseEntity.ok(getDomainModels());
	}
						
	@RequestMapping(value = "/{domain}/palette", method = RequestMethod.POST)
	public ResponseEntity<?> updatePalette(@PathVariable String domain, @RequestBody Map<?, ?> body) {

		String rdfName = domain.substring(domain.lastIndexOf('/') + 1);
		File f = new File(this.getClass().getResource("/static/data/palette-"+rdfName+".json").getPath());
		if(!f.exists()){
		    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Message("Could not identify this domain's palette", 1));
		}
		ObjectMapper objectMapper = new ObjectMapper();

		try {
		    objectMapper.writerWithDefaultPrettyPrinter().writeValue(f, body);
		} catch (IOException e) {
		    logger.error(e.getMessage());
		}
		return ResponseEntity.ok().body(body);
	}

	@RequestMapping(value = "/{domain}/users", method = RequestMethod.GET)
	public ResponseEntity<?> getDomainUsers(@PathVariable String domain) {

		// Need to update the mangement graph with new users who have not
		// yet logged in for the first time.
		modelObjectsHelper.syncUsers();

		Map<String, List<String>> map = new HashMap<>();
		map.put("users", modelObjectsHelper.getUsersForDomainModel(domain));
		return ResponseEntity.ok().body(map);
	}

	@RequestMapping(value = "/users", method = RequestMethod.GET)
	public ResponseEntity<?> getAllUsers() {

		// Need to update the mangement graph with new users who have not
		// yet logged in for the first time.
		modelObjectsHelper.syncUsers();

		return ResponseEntity.ok().body(modelObjectsHelper.getUsers().toArray());
	}

	@RequestMapping(value = "/{domain}/users", method = RequestMethod.POST)
	public ResponseEntity<?> updateDomainUsers(@PathVariable String domain, @RequestBody Map<?, ?> body) {

//        String rdfName = domain.substring(domain.lastIndexOf('/') + 1);
//        File f = new File(this.getClass().getResource("/static/data/users-" + rdfName + ".rdf.json").getPath());
//        if(!f.exists()){
//            try {
//                f.createNewFile();
//            } catch (IOException e) {
//                logger.error("Error creating file {}", rdfName, e);
//            }
//        }
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        try {
//            objectMapper.writerWithDefaultPrettyPrinter().writeValue(f, body);
//        } catch (IOException e) {
//            logger.error(e.getMessage());
//        }
		logger.debug("{}", body);
		modelObjectsHelper.setUsersForDomainModel(domain, (List<String>) body.get("users"));

		return ResponseEntity.ok().body(body.get("users"));
	}

	@RequestMapping(value = "/{graphName}/export", method = RequestMethod.GET)
	public ResponseEntity<?> export(@PathVariable String graphName) throws UnexpectedException {

		logger.info("Called REST method to GET serialised domain model {}", graphName);

		//get domain model from store
		String quads = "";
		String version = "";
		for (Map.Entry<String, Map<String, Object>> dm: storeModelManager.getDomainModels().entrySet()) {
			if (dm.getValue().get("title").equals(graphName)) {
				//includes domain-ui graph if it exists
				quads = storeModelManager.getStore().export(IStoreWrapper.Format.NQ, null,
					dm.getKey(), dm.getKey() + StoreModelManager.UI_GRAPH);
				version = (String) dm.getValue().get("version");
				break;
			}
		}
		if(quads.equals("")) {
			logger.info("Could not find domain model for ID: {}", graphName);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Unknown domain model");
		}
		//prepare response (quads in textfile to download)
		ByteArrayResource resource = new ByteArrayResource(quads.getBytes(Charset.forName("UTF-8")));
		HttpHeaders headers = new HttpHeaders();
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");
		headers.add("Content-disposition", "attachment;filename=" + graphName + "_" + version + ".nq");

		return ResponseEntity.status(HttpStatus.OK).headers(headers).contentType(MediaType.TEXT_PLAIN).body(resource);
	}

	private ResponseEntity<?> returnFile(Path p, String filename) {
		
		try {
			ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(p));
			HttpHeaders headers = new HttpHeaders();
			headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
			headers.add("Pragma", "no-cache");
			headers.add("Expires", "0");
			headers.add("Content-disposition", "attachment;filename=" + filename);

			return ResponseEntity.status(HttpStatus.OK)
					.headers(headers)
					.contentType(MediaType.TEXT_PLAIN)
					.body(resource);
		} catch (IOException e) {
			logger.error("Could not return file {}", filename, e);
		}

		return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
	}

}
