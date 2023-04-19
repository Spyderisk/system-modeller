/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2018
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
//      Created By :            Josh Wright
//      Created Date :          07/08/2018
//      Created for Project :   RESTASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.io.Files;

import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.specification.MultiPartSpecification;
import uk.ac.soton.itinnovation.security.systemmodeller.CommonTestSetup;
import uk.ac.soton.itinnovation.security.systemmodeller.SystemModellerApplication;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.StoreModelManager;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SystemModellerApplication.class, webEnvironment = RANDOM_PORT)
public class DomainModelControllerTest extends CommonTestSetup {

	private final static Logger logger = LoggerFactory.getLogger(DomainModelControllerTest.class);	

	//System objects to control model access
	@Autowired
	private StoreModelManager storeManager;

	@Autowired
	private ModelObjectsHelper modelHelper;

	//Auth variables
	@Value("${server.servlet.contextPath}")
	private String contextPath;

	@LocalServerPort
	int port;

	//Allows automatic logging of test names
	@Rule
	public TestName name = new TestName();

	//Test domain models
	private String NETWORK_TESTING_NAME = "domain-network-testing";
	private String NETWORK_TESTING_URI  = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network-testing";
	private String NETWORK_TESTING_PATH = "domainmanager/domain-network-testing.nq";
	private String NETWORK_TESTING_ZIP_PATH = "domainmanager/domain-network-testing.zip";

	private String NETWORK_TESTING_NEW_NAME = "domain-network-testing-new";
	private String NETWORK_TESTING_NEW_URI  = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network-testing-new";
	private String NETWORK_TESTING_NEW_ZIP_PATH = "domainmanager/domain-network-testing-new.zip";

	@Before
	public void init() {
		logger.info("Executing {}", name.getMethodName());
		initAuth(contextPath, port);
	}

	//Utilities

	/**
	 * Adds the domain-network-testing model from a file as a test object
	 */
	private void addNetworkTestingDomainModel() {
		logger.info("Adding {} to Store", NETWORK_TESTING_NAME);
		File file = new File(getClass().getClassLoader().getResource(NETWORK_TESTING_PATH).getFile());
		storeManager.loadModel(NETWORK_TESTING_NAME, NETWORK_TESTING_URI, file.getAbsolutePath());
	}

	/**
	 * Create a MultiPartSpecification for uploading a domain model.
	 * @param path The path to the domain model.
	 */
	private MultiPartSpecification createMultiPartSpecNQ(String path) {
		MultiPartSpecification mp = null;

		try {
			File file = new File(getClass().getClassLoader().getResource(path).getFile());
	
			mp = new MultiPartSpecBuilder(Files.toByteArray(file))
				.fileName("domain-network-testing.nq")
				.controlName("file")
				.mimeType("text/plain")
				.build();

		} catch (IOException ex) {
			logger.error("Could not build MultipartFile: {}", ex);
		}

		return mp;
	}

	/**
	 * Create a MultiPartSpecification for uploading a gzipped domain model.
	 * @param path The path to the domain model.
	 */
	private MultiPartSpecification createMultiPartSpecGzipNQ(String path) {
		MultiPartSpecification mp = null;

		try {
			File file = new File(getClass().getClassLoader().getResource(path).getFile());

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream gzos = new GZIPOutputStream(baos);
			gzos.write(Files.toByteArray(file));
			gzos.close();

			mp = new MultiPartSpecBuilder(baos.toByteArray())
				.fileName("domain_model_file.nq.gz")
				.controlName("file")
				.mimeType("application/gzip")
				.build();

		} catch (IOException ex) {
			logger.error("Could not build MultipartFile: {}", ex);
		}

		return mp;
	}

	/**
	 * Create a MultiPartSpecification for uploading a domain model bundle, as a .zip file
	 * @param path The path to the domain model bundle.
	 */
	private MultiPartSpecification createMultiPartSpecZip(String path) {
		MultiPartSpecification mp = null;

		try {
			File file = new File(getClass().getClassLoader().getResource(path).getFile());

			mp = new MultiPartSpecBuilder(Files.toByteArray(file))
				.fileName("domain_bundle.zip")
				.controlName("file")
				.mimeType("application/zip")
				.build();

		} catch (IOException ex) {
			logger.error("Could not build MultipartFile: {}", ex);
		}

		return mp;
	}

	//Tests

	/**
	 * Testing get domain models for general user
	 * Asserts OK 200 status
	 * 
	 * TODO: re-work this test, as it no longer works for domain-network-testing, as this has been removed
	 * from ontologies.json, so the user therefore has no default access to it. We may need to revisit
	 * how domain models are authorized.
	 */
	@Test
	public void testGetDomainModels() {
		addNetworkTestingDomainModel();

		modelHelper.addAccessToDefaultDomainModels(Arrays.asList(testUserName));

		given().
			filter(userSession).
		when().
			get("/domains/").
		then().
			assertThat().statusCode(HttpStatus.SC_OK);
			//and().
			//assertThat().body("collect {it.key}", contains(NETWORK_TESTING_URI)).
			//and().
			//assertThat().body("collect {it.value}.title", contains(NETWORK_TESTING_NAME));
	}

	/**
	 * Testing get domain models for an admin user
	 * Asserts OK 200 status
	 * Asserts 1 domain model returned
	 * Asserts domain model returned is domain-network-testing
	 */
	@Test
	public void testGetDomainModelsAdminUser() {
		addNetworkTestingDomainModel();

		given().
			filter(adminSession).
		when().
			get("/domains/").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("collect {it.key}", contains(NETWORK_TESTING_URI)).
			and().
			assertThat().body("collect {it.value}.title", contains(NETWORK_TESTING_NAME));
	}

	/**
	 * Testing get domin models when no models in store
	 * Asserts OK 200 status
	 * Asserts 0 models returned
	 */
	@Test
	public void testGetDomainModelsNoModels() {
		given().
			filter(userSession).
		when().
			get("/domains/").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("collect {it.key}", empty());
	}

	/** 
	 * Tests getting users for the domain-network-testing model
	 * Asserts OK 200 status
	 * ASserts test user returned in body
	 */
	@Test
	public void testGetNetworkDomainUsers() {
		addNetworkTestingDomainModel();

		modelHelper.setUsersForDomainModel(NETWORK_TESTING_NAME, Arrays.asList(testUserName));

		given().
			filter(adminSession).
		when().
			get("/domains/" + NETWORK_TESTING_NAME + "/users").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("users", contains(testUserName));
	}

	/**
	 * Test getting all users
	 * Assert OK 200 status
	 * Assert test user returned in body
	 */
	@Test
	public void testGetAllUsers() {
		given().
			filter(adminSession).
		when().
			get("/domains/users").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("", containsInAnyOrder(testUserName, testAdminName));
	}

	/**
	 * Testing uploading a new domain model version from .nq file
	 * Asserts OK 200 status
	 * Asserts 1 domain model in store post REST
	 * N.B. Not currently working as PaletteGenerator assumes that it should read ontologies.json
	 */
	@Test
	@Ignore
	public void testUploadNewNetworkDomainVersion() {
		assertEquals(0, storeManager.getDomainModels().size());

		given().
			filter(adminSession).
			multiPart(createMultiPartSpecNQ(NETWORK_TESTING_PATH)).
			queryParam("domainUri", NETWORK_TESTING_URI).
		when().
			post("/domains/upload").
		then().
			assertThat().statusCode(HttpStatus.SC_OK);

		assertEquals(1, storeManager.getDomainModels().size());
	}

	/**
	 * Testing uploading a new network-testing domain version when one already exists
	 * Asserts 1 model in store pre REST
	 * Asserts OK 200 status
	 * Asserts 1 model in store post REST
	 * N.B. Not currently working as PaletteGenerator assumes that it should read ontologies.json
	 */
	@Test
	@Ignore
	public void testUploadNewNetworkDomainVersionDomainExists() {
		addNetworkTestingDomainModel();

		assertEquals(1, storeManager.getDomainModels().size());

		given().
			filter(adminSession).
			multiPart(createMultiPartSpecNQ(NETWORK_TESTING_PATH)).
			queryParam("domainUri", NETWORK_TESTING_URI).
		when().
			post("/domains/upload").
		then().
			assertThat().statusCode(HttpStatus.SC_OK);

		assertEquals(1, storeManager.getDomainModels().size());
	}

	/**
	 * Testing uploading a new domain model version from .nq.gz file
	 * Asserts OK 200 status
	 * Asserts 1 domain model in store post REST
	 * N.B. Not currently working as PaletteGenerator assumes that it should read ontologies.json
	 */
	@Test
	@Ignore
	public void testUploadNewDomainVersionGzipFile() {
		assertEquals(0, storeManager.getDomainModels().size());

		given().
			filter(adminSession).
			multiPart(createMultiPartSpecGzipNQ(NETWORK_TESTING_PATH)).
			queryParam("domainUri", NETWORK_TESTING_URI).
		when().
			post("/domains/upload").
		then().
			assertThat().statusCode(HttpStatus.SC_OK);

		assertEquals(1, storeManager.getDomainModels().size());
	}

	/**
	 * Testing uploading a new domain model version from a .zip bundle,
	 * containing the domain model, icons and icon mapping file
	 * Asserts OK 200 status
	 * Asserts 1 domain model in store post REST
	 * Asserts that images folder exists
	 * Asserts that this folder contains the expected image files
	 */
	@Test
	public void testUploadNewDomainVersionZipFile() {
		assertEquals(0, storeManager.getDomainModels().size());

		given().
			filter(adminSession).
			multiPart(createMultiPartSpecZip(NETWORK_TESTING_ZIP_PATH)).
			queryParam("domainUri", NETWORK_TESTING_URI).
			queryParam("newDomain", false). //update existing domain model
		when().
			post("/domains/upload").
		then().
			assertThat().statusCode(HttpStatus.SC_OK);

		assertEquals(1, storeManager.getDomainModels().size());

		String imagesDir = this.getClass().getResource("/static/images/").getPath();
		String domainImagesPath = imagesDir + NETWORK_TESTING_NAME;
		logger.info("Checking images folder: {}", domainImagesPath);

		File domainImagesDir = new File(domainImagesPath);

		//icons folder must exist
		assertTrue(domainImagesDir.exists() && domainImagesDir.isDirectory());

		try {
			//Get list of filenames in folder
			List<String> files = java.nio.file.Files.list(Paths.get(domainImagesPath))
				.map(Path::toFile)
				.filter(File::isFile)
				.map(File::getName)
				.collect(Collectors.toList());

			//Print them out
			files.forEach(logger::info);

			//Assert that all expected image files are in the list
			String[] expectedImages = new String[] {"a.png", "b.png", "c.png", "d.png", "e.png"};
			assertTrue(files.containsAll(Arrays.asList(expectedImages)));
			logger.info("All expected image files exist");
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * Testing uploading a completely new domain model from a .zip bundle,
	 * containing the domain model, icons and icon mapping file
	 * Asserts OK 200 status
	 * Asserts 1 domain model in store post REST
	 * Asserts that this domain model is domain-network-testing-new
	 * Asserts that images folder exists
	 * Asserts that this folder contains the expected image files
	 */
	@Test
	public void testUploadNewDomainModelZipFile() {
		assertEquals(0, storeManager.getDomainModels().size());

		given().
			filter(adminSession).
			multiPart(createMultiPartSpecZip(NETWORK_TESTING_NEW_ZIP_PATH)).
			queryParam("domainUri", NETWORK_TESTING_NEW_URI).
			queryParam("newDomain", true). //create new domain model
		when().
			post("/domains/upload").
		then().
			assertThat().statusCode(HttpStatus.SC_OK);

		Map<String, Map<String, Object>> domainModels = storeManager.getDomainModels();
		assertEquals(1, domainModels.size());
		logger.info("domain models: {}", domainModels);

		//Assert that domain model has the correct URI
		assertEquals(domainModels.keySet().iterator().next(), NETWORK_TESTING_NEW_URI);

		String imagesDir = this.getClass().getResource("/static/images/").getPath();
		String domainImagesPath = imagesDir + NETWORK_TESTING_NEW_NAME;
		logger.info("Checking images folder: {}", domainImagesPath);

		File domainImagesDir = new File(domainImagesPath);

		//icons folder must exist
		assertTrue(domainImagesDir.exists() && domainImagesDir.isDirectory());

		try {
			//Get list of filenames in folder
			List<String> files = java.nio.file.Files.list(Paths.get(domainImagesPath))
				.map(Path::toFile)
				.filter(File::isFile)
				.map(File::getName)
				.collect(Collectors.toList());

			//Print them out
			files.forEach(logger::info);

			//Assert that all expected image files are in the list
			String[] expectedImages = new String[] {"a.png", "b.png", "c.png", "d.png", "e.png"};
			assertTrue(files.containsAll(Arrays.asList(expectedImages)));
			logger.info("All expected image files exist");
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/** Ignored:  domainUri not required by API but used without checking if null.
	 *				Either: Add a check, or change to a required param
	 * Testing upoading a domain version without a uri (required = false as of 08/08)
	 * Asserts 0 domain model in store pre REST
	 * Asserts OK 200 status
	 * Asserts 1 domain model in store post REST
	 */
	@Test
	@Ignore
	public void testUploadNewDomainVersionNoUriProvided() {
		assertEquals(0, storeManager.getDomainModels().size());
		
		given().
			filter(adminSession).
			multiPart(createMultiPartSpecNQ(NETWORK_TESTING_PATH)).
		when().
			post("/domains/upload").
		then().
			assertThat().statusCode(HttpStatus.SC_OK);

		assertEquals(1, storeManager.getDomainModels().size());
	}

	/**
	 * Testing delete domain model
	 * Asserts OK 200 status
	 * Asserts 0 domain model in store post REST
	 */
	@Test
	public void testDeleteDomainModel() {
		assertEquals(0, storeManager.getDomainModels().size());

		//First, we upload/create a new domain model
		given().
			filter(adminSession).
			multiPart(createMultiPartSpecZip(NETWORK_TESTING_NEW_ZIP_PATH)).
			queryParam("domainUri", NETWORK_TESTING_NEW_URI).
			queryParam("newDomain", true). //create new domain model
		when().
			post("/domains/upload").
		then().
			assertThat().statusCode(HttpStatus.SC_OK);

		//Basic check that we have a new domain model
		assertEquals(1, storeManager.getDomainModels().size());

		//Now delete this domain model
		given().
			filter(adminSession).
		when().
			delete("/domains/" + NETWORK_TESTING_NEW_NAME).
		then().
			assertThat().statusCode(HttpStatus.SC_OK);

		assertEquals(0, storeManager.getDomainModels().size());
	}

	/**
	 * Tests updating the users for a domain model
	 * Asserts no users pre REST
	 * Asserts OK 200 status
	 * Asserts body contains user name
	 * Asserts 1 user post REST
	 */
	@Test
	public void testUpdateDomainUsers() {
		addNetworkTestingDomainModel();

		assertEquals(0, modelHelper.getUsersForDomainModel(NETWORK_TESTING_NAME).size());

		List<String> users = new ArrayList<>();
		users.add(testUserName);

		Map<String, List<String>> body = new HashMap<>();
		body.put("users", users);

		given().
			filter(adminSession).
			contentType(ContentType.JSON).
			body(body).
		when().
			post("/domains/" + NETWORK_TESTING_NAME + "/users").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body("", contains(testUserName));

		assertEquals(1, modelHelper.getUsersForDomainModel(NETWORK_TESTING_NAME).size());
	}

	/**
	 * Testing exporting a domain model
	 * Asserts OK 200 status
	 * Asserts body is populated
	 * //TODO: check body matches SPOG. regex?
	 */
	@Test
	public void testExport() {
		addNetworkTestingDomainModel();

		given().
			filter(adminSession).
		when().
			get("/domains/" + NETWORK_TESTING_NAME + "/export").
		then().
			assertThat().statusCode(HttpStatus.SC_OK).
			and().
			assertThat().body(not(isEmptyOrNullString()));
	}

	/**
	 * Testing exporting a domain model that does not exist
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testExportInvalidModel() {
		given().
			filter(adminSession).
		when().
			get("/domains/notADomainGraphID/export").
		then().
			assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
	}

}
