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
//      Created By :            Stefanie Wiegand
//      Created Date :          2018-04-30
//      Created for Project :   SHiELD
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.model.test;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.model.system.ControlSet;
import uk.ac.soton.itinnovation.security.model.system.MetadataPair;

import static org.assertj.core.api.Assertions.*;

@RunWith(JUnit4.class)
public class ObjectModelTest extends TestCase {

	private static Logger logger = LoggerFactory.getLogger(ObjectModelTest.class);;

	@Rule public TestName name = new TestName();

	@Before
	public void beforeEachTest() {
		logger.info("Running test {}", name.getMethodName());
	}

	// Tests //////////////////////////////////////////////////////////////////////////////////////

	@Test
	public void testToggleControlProposed() {
		ControlSet cs = new ControlSet();

		cs.setProposed(true);
		assertThat(cs.isProposed()).isTrue();

		cs.setProposed(false);
		assertThat(cs.isProposed()).isFalse();
	}

	@Test
	public void testToggleControlWorkInProgress() {
		ControlSet cs = new ControlSet();
		cs.setProposed(true);

		cs.setWorkInProgress(true);
		assertThat(cs.isWorkInProgress()).isTrue();

		cs.setWorkInProgress(false);
		assertThat(cs.isWorkInProgress()).isFalse();
	}

	@Test
	public void testWorkInProgressMustBeProposed() {
		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> new ControlSet("", "", "", "", "", false, true)
			)
			.withMessage(
				"Control cannot be work in progress but not proposed"
			);
	}

	@Test
	public void testCannotSetWorkInProgressIfNotProposed() {
		ControlSet cs = new ControlSet("", "", "", "", "", false, false);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> cs.setWorkInProgress(true)
			)
			.withMessage(
				"Cannot set work in progress: control is not proposed"
			);
	}

	@Test
	public void testCannotUnsetProposedIfWorkInProgress() {
		ControlSet cs = new ControlSet("", "", "", "", "", true, true);

		assertThatIllegalStateException()
			.isThrownBy(
				() -> cs.setProposed(false)
			)
			.withMessage(
				"Cannot unset proposed: control is work in progress"
			);
	}

	@Test
	public void testMetadataPairEquals() {
		MetadataPair p1 = new MetadataPair("key1", "value1");
		MetadataPair p2 = new MetadataPair("key1", "value1");

		assertThat(p1.equals(p2)).isTrue();
		assertThat(p2.equals(p1)).isTrue();
	}

	@Test
	public void testMetadataPairKeysNotEqual() {
		MetadataPair p1 = new MetadataPair("key1", "value1");
		MetadataPair p2 = new MetadataPair("key2", "value1");

		assertThat(p1.equals(p2)).isFalse();
		assertThat(p2.equals(p1)).isFalse();
	}

	@Test
	public void testMetadataPairValuesNotEqual() {
		MetadataPair p1 = new MetadataPair("key1", "value1");
		MetadataPair p2 = new MetadataPair("key1", "value2");

		assertThat(p1.equals(p2)).isFalse();
		assertThat(p2.equals(p1)).isFalse();
	}

	@Test
	public void testMetadataPairEqualsSelf() {
		MetadataPair p = new MetadataPair("key1", "value1");

		assertThat(p.equals(p)).isTrue();
	}

	@Test
	public void testMetadataPairDoesNotEqualNull() {
		MetadataPair p = new MetadataPair("key1", "value1");

		assertThat(p.equals(null)).isFalse();
	}

	@Test
	public void testMetadataPairDoesNotEqualObject() {
		MetadataPair p = new MetadataPair("key1", "value1");

		assertThat(p.equals(new Object())).isFalse();
	}

	@Test
	public void testMetadataPairDoesNotEqualNullKey() {
		MetadataPair p1 = new MetadataPair("key1", "value1");
		MetadataPair p2 = new MetadataPair(null, "value1");

		assertThat(p1.equals(p2)).isFalse();
		assertThat(p2.equals(p1)).isFalse();
	}

	@Test
	public void testMetadataPairDoesNotEqualNullValue() {
		MetadataPair p1 = new MetadataPair("key1", "value1");
		MetadataPair p2 = new MetadataPair("key1", null);

		assertThat(p1.equals(p2)).isFalse();
		assertThat(p2.equals(p1)).isFalse();
	}

	@Test
	public void testMetadataPairHashcodeKeysNotEqual() {
		MetadataPair p1 = new MetadataPair("key1", "value1");
		MetadataPair p2 = new MetadataPair("key2", "value1");

		assertThat(p1.hashCode()).isNotEqualTo(p2.hashCode());
	}

	@Test
	public void testMetadataPairHashcodeValuesNotEqual() {
		MetadataPair p1 = new MetadataPair("key1", "value1");
		MetadataPair p2 = new MetadataPair("key1", "value2");

		assertThat(p1.hashCode()).isNotEqualTo(p2.hashCode());
	}
}
