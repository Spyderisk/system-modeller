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
//      Created Date :          ?
//		Modified By :           Stefanie Cox, Joshua Wright
//      Created for Project :   5G-ENSURE
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.semantics;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.soton.itinnovation.security.semanticstore.IStoreWrapper;
import uk.ac.soton.itinnovation.security.systemmodeller.SystemModellerApplication;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;

/**
 * @author gc
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SystemModellerApplication.class)
@TestPropertySource(properties = {"model.management.uri=${model.management.uri.test}", "reset.on.start=false"})
public class StoreModelManagerTest {

	private static final Logger logger = LoggerFactory.getLogger(StoreModelManagerTest.class);

	@Autowired
	private StoreModelManager storeModelManager;

	//Default system and domain model 
	private final String SYSTEM_ID = "1234567890";
	private final String TEST_USER = "gc";
	private final String SYSTEM_URI = "http://it-innovation.soton.ac.uk/system/1234567890";
	private final String DOMAIN_URI = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-5g";

	@Before
	public void setUp() {
		logger.debug("Setting StoreModelManagerTest up");
		storeModelManager.clearMgtGraph();
	}

	/**
	 * Creates a new system model in the store manager with random ID
	 *
	 * @return The URI of the new model
	 */
	public String createTestSystemModel() {
		Integer newID = new Random().nextInt(1000000);
		return storeModelManager.createSystemModel(newID.toString(),
				null,
				"jw18",
				"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network-testing");
	}

	/**
	 * Test of loadModel method, of class StoreModelManager. Asserts load model
	 * returns expected URI for loaded model. Asserts store now has data at the
	 * URI.
	 *
	 * @throws java.io.IOException
	 */
	@Test
	public void testAddDomainModelFromFile() throws IOException {
		logger.info("addDomainModel");
		String domainModelName = "domain-network-testing";
		String domainModelURI = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network-testing";

		File file = new File(getClass().getClassLoader().getResource("domainmanager/domain-network-testing.nq").getFile());

		String expResult = domainModelURI;
		String result = storeModelManager.loadModel(domainModelName, domainModelURI, file.getPath());
		assertEquals(expResult, result);

		long size = storeModelManager.getStore().getCount(domainModelURI);
		assertNotEquals(size, 0);
	}

	/**
	 * Test of loadModelFromResource method, of class StoreModelManager. 
	 * Asserts load model returns expected URI for loaded model.
	 * Asserts store now has data at the URI.
	 * N.B. This test was previously attempting to load a domain model from a resource,
	 * however domain models will soon no longer be in the SSM build. We do still need
	 * this method to load the core model.
	 */
	@Test
	public void testAddCoreModelFromResource() {
		logger.info("addCoreModel");
		String coreModelName = "core";
		String coreModelURI = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/core";
		String resourcePath = "/core.rdf";

		String expResult = coreModelURI;
		String result = storeModelManager.loadModelFromResource(coreModelName, coreModelURI, resourcePath);
		assertEquals(expResult, result);
		long size = storeModelManager.getStore().getCount(coreModelURI);
		assertNotEquals(size, 0);
	}

	/**
	 * Test of deleteModel method, of class StoreModelManager. Asserts store
	 * size not 0 after model added Asserts store size 0 after model deleted
	 */
	@Test
	public void testDeleteDomainModel() {
		String domainModelName = "domain-network-testing";
		String domainModelURI = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network-testing";
		String domainModelResourcePath = "/domain-network-testing.nq";
		String expResult = domainModelURI;
		String result = storeModelManager.loadModelFromResource(domainModelName, domainModelURI, domainModelResourcePath);
		assertEquals(expResult, result);
		IStoreWrapper store = storeModelManager.getStore();
		long size = store.getCount("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network-testing");
		assertNotEquals(size, 0);

		storeModelManager.deleteModel("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network-testing");
		size = store.getCount("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network-testing");
		assertEquals(size, 0);
	}

	/**
	 * Test of systemModelExists method, of class StoreModelManager Asserts
	 * false returned before model created Asserts true returned after model
	 * created
	 */
	@Test
	public void testSystemModelExist() {
		logger.info("Testing systemModelExists()");
		assertFalse(storeModelManager.systemModelExists(SYSTEM_URI));

		String systemUri = createTestSystemModel();
		assertTrue(storeModelManager.systemModelExists(systemUri));
	}

	/**
	 * Test of createSystemModel method, of class StoreModelManager. Asserts
	 * model created with correct URI Asserts model cannot be created again
	 * (null returned, not URI)
	 */
	@Test
	public void testCreateSystemModel() {
		logger.info("Testing createSystemModel");
		long before = storeModelManager.getStore().getCount(storeModelManager.getManagementGraph());
		String result = storeModelManager.createSystemModel(SYSTEM_ID, null, TEST_USER, DOMAIN_URI);
		assertEquals(SYSTEM_URI, result);
		assertEquals(before + 14, storeModelManager.getStore().getCount(storeModelManager.getManagementGraph()));

		assertThatExceptionOfType(RuntimeException.class)
			.isThrownBy(
				() -> storeModelManager.createSystemModel(SYSTEM_ID, null, TEST_USER, DOMAIN_URI)
			)
			.withMessage(
				"System model <http://it-innovation.soton.ac.uk/system/" + SYSTEM_ID + "> already exists"
			);
	}

	/**
	 * Test of deleteSystemModel method, of class StoreModelManager. Asserts
	 * Model created at correct URI Asserts Model can be created again after
	 * deletion, (returns URI, not null)
	 */
	@Test
	public void testDeleteSystemModel() {
		logger.info("Testing deleteSystemModel");
		String result = storeModelManager.createSystemModel(SYSTEM_ID, null, TEST_USER, DOMAIN_URI);
		assertEquals(SYSTEM_URI, result);

		storeModelManager.deleteSystemModel(SYSTEM_URI);

		//Model no longer exists, result should be the same as the previous call
		result = storeModelManager.createSystemModel(SYSTEM_ID, null, TEST_USER, DOMAIN_URI);
		assertEquals(SYSTEM_URI, result);
	}

	/**
	 * Test of addImport method, of class StoreModelManager. Asserts graph not
	 * currently imported Asserts graph now imported
	 */
	@Test
	public void testAddImport() {
		logger.info("Testing addImport");
		Collection<String> imports;
		//String importableDomainURI = A_DOMAIN_URI + "1";

		//Test new import not already imported
		String result = createTestSystemModel();
		imports = storeModelManager.getImports(result);
		assertFalse(imports.contains(DOMAIN_URI));

		//Test new import now imported
		storeModelManager.addImport(result, DOMAIN_URI);
		imports = storeModelManager.getImports(result);
		assertTrue(imports.contains(DOMAIN_URI));
	}

	/**
	 * Test of deleteImport method, of class StoreModelManager. Asserts graph
	 * imported correctly Asserts graph no longer imported post deletion of
	 * import
	 */
	@Test
	public void testDeleteImport() {
		logger.info("Testing deleteImport");

		//Create model and import another domain model.
		String result = createTestSystemModel();
		storeModelManager.addImport(result, DOMAIN_URI);
		Collection<String> imports = storeModelManager.getImports(result);
		assertTrue(imports.contains(DOMAIN_URI));

		//Delete import and assert it is no longer present
		storeModelManager.deleteImport(result, DOMAIN_URI);
		imports = storeModelManager.getImports(result);
		assertFalse(imports.contains(DOMAIN_URI));
	}

	/**
	 * Test of getInferredModel method, of class StoreModelManager. Asserts
	 * returned inferred model URI is as expected
	 */
	@Test
	public void testGetInferredModel() {
		logger.info("Testing getInferredModel");
		String result = createTestSystemModel();
		String expInfURI = result + StoreModelManager.INF_GRAPH; //create inf graph uri

		assertEquals(expInfURI, storeModelManager.getInferredModel(result));
	}

	/**
	 * Test of getUIModel method, of class StoreModelManager. Asserts returned
	 * UI model URI is as expected
	 */
	@Test
	public void testgetUIModel() {
		logger.info("Testing getUIModel");
		String result = createTestSystemModel();
		String expUiURI = result + StoreModelManager.UI_GRAPH; //create ui graph uri

		assertEquals(expUiURI, storeModelManager.getUIModel(result));
	}

	/**
	 * Test of getModelURI method, of class StoreModelManager Asserts returned
	 * URI is as expected
	 */
	@Test
	public void testGetModelURI() {
		logger.info("Testing getModelURI");
		storeModelManager.createSystemModel(SYSTEM_ID, null, TEST_USER, DOMAIN_URI);
		assertEquals(SYSTEM_URI, storeModelManager.getModelURI(SYSTEM_ID));
	}

	/**
	 * Test of getSystemModelsByUserID method, of class StoreModelManager
	 * Asserts no models stored gc Asserts gc model returned after creation
	 * Asserts notGC model not returned for gc
	 */
	@Test
	public void testGetSystemModelsByUserId() {
		logger.info("testing getSystemModelsByUserId");
		assertTrue(storeModelManager.getSystemModelsByUserId("gc").isEmpty());
		storeModelManager.createSystemModel(SYSTEM_ID, null, "gc", DOMAIN_URI);
		assertEquals(1, storeModelManager.getSystemModelsByUserId("gc").size());

		//Test model has username jw18, not gc
		createTestSystemModel();
		assertEquals(1, storeModelManager.getSystemModelsByUserId("gc").size());
	}

	/**
	 * Test of getSystemModels method, of class StoreModelManager Asserts no
	 * models returned after initialisation Asserts 1 model returned after
	 * creation
	 */
	@Test
	public void testGetSystemModels() {
		logger.info("testing getSystemModels");
		assertTrue(storeModelManager.getSystemModels().isEmpty());
		createTestSystemModel();
		assertEquals(1, storeModelManager.getSystemModels().size());
	}

	/**
	 * Test of changeSystemModelURI method, of class StoreModelManager Asserts
	 * original URI matches expected Asserts new URI matches expected
	 */
	@Test
	public void testChangeSystemModelURI() {
		logger.info("Attempting to change system model URI");
		//URI set to be different from test URI
		String newURI = SYSTEM_URI + "1";
		storeModelManager.createSystemModel(SYSTEM_ID, null, TEST_USER, DOMAIN_URI);
		assertEquals(SYSTEM_URI, storeModelManager.getModelURI(SYSTEM_ID));
		storeModelManager.changeSystemModelURI(storeModelManager.getManagementGraph(), SYSTEM_URI, newURI);
		assertEquals(newURI, storeModelManager.getModelURI(SYSTEM_ID));
	}
}
