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
//		Modified By :           Stefanie Cox
//      Created for Project :   5G-ENSURE
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.semantics;

import org.junit.After;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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

/**
 * Test unit for StoreFactory
 *
 * @author gc
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SystemModellerApplication.class)
@TestPropertySource(properties = {"model.management.uri=${model.management.uri.test}", "reset.on.start=false"})
public class StoreFactoryTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private IStoreWrapper result;

	@Autowired
	private StoreFactory storeFactory;

	public StoreFactoryTest() {
	}

	@Before
	public void before() {
		logger.info("get store wrapper instance");
		result = storeFactory.getInstance();
	}

	@After
	public void after() {
		result.disconnect();
	}

	/**
	 * Test of getInstance method, of class StoreFactory.
	 */
	@Test
	public void testGetInstance() {
		assertNotNull(result);
	}

	@Test
	public void testLoadDeleteGraph() {
		String path = this.getClass().getClassLoader().getResource("StoreTest/A.rdf").getPath();
		result.loadIntoGraph(path, "http://example.org/a", IStoreWrapper.Format.RDF);
		assertTrue(result.graphExists("http://example.org/a"));
		//TODO delete graph requires transaction ON
		//result.deleteGraph("http://example.org/a");
		//assertFalse(result.graphExists("http://example.org/a"));
	}

}
