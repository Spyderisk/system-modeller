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
//      Created Date :          2017-04-03
//      Created for Project :   ASSURED
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.modelquerier.test;

import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.model.system.Asset;

@RunWith(JUnit4.class)
public class SemanticEntityTester extends TestCase {

    public static Logger logger = LoggerFactory.getLogger(SemanticEntityTester.class);

	@Rule public TestName name = new TestName();

	@BeforeClass
    public static void beforeClass() {
		logger.info("URI tests executing...");
    }

	// Tests //////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test
	public void testURI() {
		logger.info("Running test {}", name.getMethodName());

		String uri = "http://it-innovation.soton.ac.uk/user/67842674784/system/392109302193921#asset-eeab2656";

		Asset a = new Asset(uri, "Asset 1",
			"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#Host", 100, 200);

		logger.debug("URI: {}, ID: {}", a.getUri(), a.getID());
		assertEquals("11b189ec", a.getID());
	}
}
