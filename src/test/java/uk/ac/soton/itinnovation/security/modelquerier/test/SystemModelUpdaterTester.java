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
//      Created Date :          2017-02-16
//      Created for Project :   5G-ENSURE
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.modelquerier.test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.model.Level;
import uk.ac.soton.itinnovation.security.model.system.Asset;
import uk.ac.soton.itinnovation.security.model.system.AssetGroup;
import uk.ac.soton.itinnovation.security.model.system.ControlSet;
import uk.ac.soton.itinnovation.security.model.system.MisbehaviourSet;
import uk.ac.soton.itinnovation.security.model.system.MetadataPair;
import uk.ac.soton.itinnovation.security.model.system.Model;
import uk.ac.soton.itinnovation.security.model.system.Relation;
import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;
import uk.ac.soton.itinnovation.security.model.system.Threat;
import uk.ac.soton.itinnovation.security.model.system.TrustworthinessAttributeSet;
import uk.ac.soton.itinnovation.security.modelquerier.SystemModelQuerier;
import uk.ac.soton.itinnovation.security.modelquerier.SystemModelUpdater;
import uk.ac.soton.itinnovation.security.modelquerier.util.TestHelper;
import uk.ac.soton.itinnovation.security.semanticstore.AStoreWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@RunWith(JUnit4.class)
public class SystemModelUpdaterTester extends TestCase {

	public static Logger logger = LoggerFactory.getLogger(SystemModelUpdaterTester.class);

	private static TestHelper tester;

	private static SystemModelQuerier querier;
	private static SystemModelUpdater updater;

	private long stopwatch;

	@Rule
	public TestName name = new TestName();

	@BeforeClass
	public static void beforeClass() {

		tester = new TestHelper("jena-tdb");

		tester.addDomain(0, "modelquerier/domain-shield.rdf.gz", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-shield");

		//Test domain model for population support
		tester.addDomain(1, "modelquerier/domain-ssm-testing-6a3.nq", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/ssm-testing-6a3");

		//be cautious with sharing URIs: if the validated model contains anything asserted, the unvalidated model
		//will have it too as it shared the same graph URI. For this reason, we define a new URI for this model to avoid clashes
		tester.addSystem(0, "modelquerier/system-shield.nq.gz", "http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3dda");
		tester.addSystem(1, "modelquerier/system-shield-without-metadata.nq.gz",
				"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3dda",
				"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3ddb");
		tester.addSystem(2, "modelquerier/system-network-fixedhost-lan.nq.gz", "http://it-innovation.soton.ac.uk/system/5e4eb1c2f7a27213c4e3aa81");
		tester.addSystem(3, "modelquerier/system-shield-with-metadata-pairs.nq.gz",
				"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3dda",
				"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3ddc");
		tester.addSystem(4, "modelquerier/system-shield-with-risk-calculation-mode.nq.gz",
				"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3dda",
				"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3ddd");
		tester.addSystem(5, "modelquerier/system-test-groups.nq.gz",
				"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3dda");

		//Test system model for population support
		tester.addSystem(6, "modelquerier/Test-6a3-00.nq.gz",
				"http://it-innovation.soton.ac.uk/system/63971077df89a647814e6d8b");

		tester.setUp();

		querier = new SystemModelQuerier(tester.getModel());
		updater = new SystemModelUpdater(tester.getModel());

		logger.info("SystemModelUpdater tests executing...");
	}

	@Before
	public void beforeEachTest() {

		logger.info("Running test {}", name.getMethodName());
		stopwatch = System.currentTimeMillis();
	}

	@After
	public void afterEachTest() {

		logger.debug("Test {} took {} milliseconds", name.getMethodName(), System.currentTimeMillis() - stopwatch);

		//comment in to better debug the test models
		logger.info("Exporting test model");
		tester.exportTestModel("build/build/test-results/" + name.getMethodName(), false, false, true);
	}

	// Tests //////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test
	public void testAddMetadataPairToEntityWithoutMetadata() {
		tester.switchModels(0, 3);

		AStoreWrapper store = tester.getStore();

		Asset asset = querier.getSystemAssetById(store, "df878bda");
		MetadataPair metadata = new MetadataPair("exampleKey", "exampleValue");

		assertThat(updater.addMetadataPairToEntity(store, asset, metadata)).isTrue();
		assertThat(querier.getMetadataOnEntity(store, asset)).containsExactly(metadata);
	}

	@Test
	public void testAddMetadataPairToEntityWithMetadata() {
		tester.switchModels(0, 3);

		AStoreWrapper store = tester.getStore();

		Asset asset = querier.getSystemAssetById(store, "66004713");

		List<MetadataPair> preMetadata = querier.getMetadataOnEntity(store, asset);
		assertThat(preMetadata).hasSize(3);

		MetadataPair p1 = new MetadataPair("addedKey1", "addedValue1");
		MetadataPair p2 = new MetadataPair("addedKey2", "addedValue1");
		MetadataPair p3 = new MetadataPair("addedKey1", "addedValue2");

		assertThat(updater.addMetadataPairToEntity(store, asset, p1)).isTrue();
		assertThat(updater.addMetadataPairToEntity(store, asset, p2)).isTrue();
		assertThat(updater.addMetadataPairToEntity(store, asset, p3)).isTrue();

		List<MetadataPair> postMetadata = querier.getMetadataOnEntity(store, asset);
		assertThat(postMetadata).hasSize(6);

		preMetadata.add(p1);
		preMetadata.add(p2);
		preMetadata.add(p3);

		MetadataPair[] preMetadataArray = preMetadata.toArray(new MetadataPair[0]);

		assertThat(postMetadata).containsExactlyInAnyOrder(preMetadataArray);
	}

	@Test
	public void testAddMetadataPairToEntityDuplicateMetadata() {
		tester.switchModels(0, 3);

		AStoreWrapper store = tester.getStore();

		Asset asset = querier.getSystemAssetById(store, "df878bda");
		MetadataPair metadata = new MetadataPair("exampleKey", "exampleValue");

		assertThat(updater.addMetadataPairToEntity(store, asset, metadata)).isTrue();
		assertThat(updater.addMetadataPairToEntity(store, asset, metadata)).isFalse();
	}

	@Test
	public void testAddMetadataPairToEntitySpecialCharacters() {
		tester.switchModels(0, 3);

		AStoreWrapper store = tester.getStore();

		Asset asset = querier.getSystemAssetById(store, "df878bda");

		String key = "\"#/{";
		String value = "\'";
		MetadataPair metadata = new MetadataPair(key, value);

		assertThat(updater.addMetadataPairToEntity(store, asset, metadata)).isTrue();

		List<MetadataPair> assetMetadata = querier.getMetadataOnEntity(store, asset);
		assertThat(assetMetadata).hasSize(1);
		assertThat(assetMetadata.get(0).getKey()).isEqualTo(key);
		assertThat(assetMetadata.get(0).getValue()).isEqualTo(value);
	}

	@Test
	public void testAddMetadataPairToEntitySharedPair() {
		tester.switchModels(0, 3);

		AStoreWrapper store = tester.getStore();

		Asset asset1 = querier.getSystemAssetById(store, "66004713");
		Asset asset2 = querier.getSystemAssetById(store, "ece77c11");
		MetadataPair metadata = new MetadataPair("exampleKey1", "exampleValue2");

		assertThat(updater.addMetadataPairToEntity(store, asset1, metadata)).isTrue();
		assertThat(querier.getMetadataOnEntity(store, asset1)).hasSize(4);
		assertThat(querier.getMetadataOnEntity(store, asset2)).hasSize(2);
	}

	@Test
	public void testAddMetadataPairToEntityNullMetadata() {
		tester.switchModels(0, 3);

		AStoreWrapper store = tester.getStore();

		Asset asset = querier.getSystemAssetById(store, "df878bda");

		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> updater.addMetadataPairToEntity(store, asset, null)
			)
			.withMessage(
				"MetadataPair cannot be null"
			);
	}

	@Test
	public void testAddMetadataPairToEntityNullKey() {
		tester.switchModels(0, 3);

		AStoreWrapper store = tester.getStore();

		Asset asset = querier.getSystemAssetById(store, "df878bda");
		MetadataPair metadata = new MetadataPair(null, "exampleValue");

		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> updater.addMetadataPairToEntity(store, asset, metadata)
			)
			.withMessage(
				"MetadataPair key cannot be null"
			);
	}

	@Test
	public void testAddMetadataPairToEntityNullValue() {
		tester.switchModels(0, 3);

		AStoreWrapper store = tester.getStore();

		Asset asset = querier.getSystemAssetById(store, "df878bda");
		MetadataPair metadata = new MetadataPair("exampleKey", null);

		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> updater.addMetadataPairToEntity(store, asset, metadata)
			)
			.withMessage(
				"MetadataPair value cannot be null"
			);
	}

	@Test
	public void testAddMetadataPairToEntityGraphSafe() {
		tester.switchModels(0, 3);

		AStoreWrapper store = tester.getStore();

		long system     = store.getCount(tester.getGraph("system"));
		long systemInf  = store.getCount(tester.getGraph("system-inf"));
		long systemUi   = store.getCount(tester.getGraph("system-ui"));
		long systemMeta = store.getCount(tester.getGraph("system-meta"));

		Asset asset = querier.getSystemAssetById(store, "df878bda");
		MetadataPair metadata = new MetadataPair("exampleKey", "exampleValue");

		assertThat(updater.addMetadataPairToEntity(store, asset, metadata)).isTrue();

		assertThat(store.getCount(tester.getGraph("system")))
			.isEqualTo(system);

		assertThat(store.getCount(tester.getGraph("system-inf")))
			.isEqualTo(systemInf);

		assertThat(store.getCount(tester.getGraph("system-ui")))
			.isEqualTo(systemUi);

		assertThat(store.getCount(tester.getGraph("system-meta")))
			.isEqualTo(systemMeta + 3);
	}

	@Test
	public void testDeleteMetadataOnEntity() {
		tester.switchModels(0, 3);

		AStoreWrapper store = tester.getStore();

		Asset asset = querier.getSystemAssetById(store, "df878bda");
		MetadataPair metadata = new MetadataPair("exampleKey", "exampleValue");

		assertThat(updater.addMetadataPairToEntity(store, asset, metadata)).isTrue();
		assertThat(querier.getMetadataOnEntity(store, asset)).isNotEmpty();

		long before = store.getCount(tester.getGraph("system-meta"));

		updater.deleteMetadataOnEntity(store, asset);

		assertThat(querier.getMetadataOnEntity(store, asset)).isEmpty();
		assertThat(store.getCount(tester.getGraph("system-meta"))).isEqualTo(before - 3);
	}

	@Test
	public void testDeleteMetadataOnEntitySharedPair() {
		tester.switchModels(0, 3);

		AStoreWrapper store = tester.getStore();

		Asset asset1 = querier.getSystemAssetById(store, "66004713");
		Asset asset2 = querier.getSystemAssetById(store, "ece77c11");

		assertThat(querier.getMetadataOnEntity(store, asset1)).isNotEmpty();

		long before = store.getCount(tester.getGraph("system-meta"));

		updater.deleteMetadataOnEntity(store, asset1);

		assertThat(querier.getMetadataOnEntity(store, asset1)).isEmpty();
		assertThat(querier.getMetadataOnEntity(store, asset2)).hasSize(2);
		assertThat(store.getCount(tester.getGraph("system-meta"))).isEqualTo(before - 7);
	}

	@Test
	public void testDeleteAssetNoSharedMetadata() {
		tester.switchModels(0, 3);

		AStoreWrapper store = tester.getStore();

		Asset asset = querier.getSystemAssetById(store, "df878bda");
		MetadataPair metadata = new MetadataPair("exampleKey", "exampleValue");

		long before = store.getCount(tester.getGraph("system-meta"));

		assertThat(updater.addMetadataPairToEntity(store, asset, metadata)).isTrue();
		assertThat(querier.getMetadataOnEntity(store, asset)).hasSize(1);

		updater.deleteAsset(store, asset);

		assertThat(store.getCount(tester.getGraph("system-meta"))).isEqualTo(before);
	}

	@Test
	public void testDeleteAssetSharedMetadataPair() {
		tester.switchModels(0, 3);

		AStoreWrapper store = tester.getStore();

		Asset asset1 = querier.getSystemAssetById(store, "66004713");
		Asset asset2 = querier.getSystemAssetById(store, "ece77c11");

		long before = store.getCount(tester.getGraph("system-meta"));

		assertThat(querier.getMetadataOnEntity(store, asset1)).hasSize(3);

		updater.deleteAsset(store, asset2);

		assertThat(querier.getMetadataOnEntity(store, asset1)).hasSize(3);
		assertThat(store.getCount(tester.getGraph("system-meta"))).isEqualTo(before - 4);
	}

	@Test
	public void testUpdateModelInfo() {

		tester.switchModels(0, 0);

		//old data
		String uri = "http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3dda";
		String label = "test";
		String desc = null;
		String domain = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-shield";

		//get original data
		Model m = querier.getModelInfo(tester.getStore());
		logger.debug("{}", m);
		assertEquals(uri, m.getUri());
		//assertEquals(label, m.getLabel());
		//assertEquals(desc, m.getDescription());
		assertEquals(domain, m.getDomain());

		//set new data
		label = "mymodel";
		desc = "Blah";
		domain = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-assured";
		
		//KEM - system model risk level should not be updated here, as it is only set by the risk calculator
		//Map<String, Level> levels = querier.getLevels(tester.getStore(), "RiskLevel");
		//Level risk = levels.values().iterator().next();

		//the URI should never be changed and if it is, it will only result in the model info not being found
		//m.setUri("http://it-innovation.soton.ac.uk/user/594a6bd21719e03c4c38ccbd/system/594a6d7b1719e03d24fcf010/ASSURED-Q6demo/newURI");
		m.setLabel(label);
		m.setDomain(domain);
		m.setDescription(desc);
		//m.setRisk(risk); //KEM - see comment above
		updater.updateModelInfo(tester.getStore(), m);

		//check whether the update worked
		Model result = querier.getModelInfo(tester.getStore());
		//note that the URI will not ever be changed by the updater
		assertEquals(uri, result.getUri());
		assertEquals(label, result.getLabel());
		assertEquals(desc, result.getDescription());
		assertEquals(domain, result.getDomain());
		//assertEquals(risk, result.getRisk());  //KEM - see comment above

		//test with funny values
		label = "new label with spaces";
		//not supporting line breaks at the moment
		desc = "description: contains \"quotes\"";
		m.setLabel(label);
		m.setDomain(domain);
		m.setDescription(desc);
		updater.updateModelInfo(tester.getStore(), m);

		//check whether the update worked
		result = querier.getModelInfo(tester.getStore());
		assertEquals(label, result.getLabel());
		//quotes will be escaped!
		//assertEquals(desc, result.getDescription());
		assertEquals(domain, result.getDomain());
	}

	@Test
	public void testUpdateModelInfoRiskCalculationMode() {

		tester.switchModels(0, 4);

		Model m = querier.getModelInfo(tester.getStore());
		assertEquals(RiskCalculationMode.FUTURE, m.getRiskCalculationMode());

		m.setRiskCalculationMode(RiskCalculationMode.CURRENT);
		updater.updateModelInfo(tester.getStore(), m);

		Model result = querier.getModelInfo(tester.getStore());
		assertEquals(RiskCalculationMode.CURRENT, result.getRiskCalculationMode());
	}

	@Test
	public void testUpdateModelInfoRiskCalculationModeFromUnset() {

		tester.switchModels(0, 0);

		Model m = querier.getModelInfo(tester.getStore());
		assertEquals(null, m.getRiskCalculationMode());

		m.setRiskCalculationMode(RiskCalculationMode.FUTURE);
		updater.updateModelInfo(tester.getStore(), m);

		Model result = querier.getModelInfo(tester.getStore());
		assertEquals(RiskCalculationMode.FUTURE, result.getRiskCalculationMode());
	}

	@Test
	public void testUpdateAssetPosition() {

		tester.switchModels(0, 0);

		Asset a = new Asset("http://example.com/myHost", "Asset 1",
				"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#Host", 100, 200);
		updater.storeAsset(tester.getStore(), a);

		a.setIconPosition(99, 199);

		updater.updateAssetPosition(tester.getStore(), a);

		Asset a2 = querier.getSystemAsset(tester.getStore(), a.getUri());
		assertEquals(99, a2.getIconX());
		assertEquals(199, a2.getIconY());

		updater.updateAssetPosition(tester.getStore(), a.getUri(), 2, 33);

		Asset a3 = querier.getSystemAsset(tester.getStore(), a.getUri());
		assertEquals(2, a3.getIconX());
		assertEquals(33, a3.getIconY());

		tester.getModel().printSizes(tester.getStore());
	}

	@Test
	public void testUpdateAssetsPositions() {

		tester.switchModels(0, 0);

		int num = 3;
        
		List<Asset> assets = new ArrayList<Asset>();

		// Generate assets
		for (int i = 0; i < num; i++) {
			Asset a = new Asset("http://example.com/myHost" + i, "Asset " + i,
				"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#Host",
				10*i, 20*i);
			updater.storeAsset(tester.getStore(), a);
			assets.add(a);
		}
        
		// Update all asset positions
		for (int i = 0; i < num; i++) {
			assets.get(i).setIconPosition(10*i+10, 20*i+10);
		}
        
		long time = System.currentTimeMillis();
		updater.updateAssetsPositions(tester.getStore(), assets);
		logger.debug("Update took {} milliseconds", System.currentTimeMillis() - time);
        
		// Check asset positions have been correctly updated
		for (int i = 0; i < num; i++) {
			Asset au = querier.getSystemAsset(tester.getStore(), assets.get(i).getUri());
			assertEquals(10*i+10, au.getIconX());
			assertEquals(20*i+10, au.getIconY());
		}
	}

	@Test
	public void testUpdateAssetType1() {

		tester.switchModels(0, 2);

		AStoreWrapper store = tester.getStore();
		
		String systemGraph = tester.getGraph("system");
		String inferredGraph = tester.getGraph("system-inf");
		logger.debug("System graph: {}", systemGraph);
		logger.debug("Inferred graph: {}", inferredGraph);
		
		Asset a1 = querier.getSystemAsset(store, "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#4850d723"); //"FixedHost1"
		assertEquals("FixedHost1", a1.getLabel());
		assertEquals("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#FixedHost", a1.getType());
		
		Asset a2 = querier.getSystemAsset(store, "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#a614712d"); //"LAN"
		assertEquals("LAN", a2.getLabel());
		assertEquals("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#WiredLAN", a2.getType());
		
		long systemTriplesCountBefore = store.getCount(systemGraph);
		long inferredTriplesCountBefore = store.getCount(inferredGraph);
		
		logger.debug("current triples count (asserted) = {}", systemTriplesCountBefore);
		logger.debug("current triples count (inferred) = {}", inferredTriplesCountBefore);
		
		logger.debug("Changing asset type from FixedHost to MobileHost");
		updater.updateAssetType(store, a1.getUri(), "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#MobileHost");
		
		long systemTriplesCountAfter = store.getCount(systemGraph);
		long inferredTriplesCountAfter = store.getCount(inferredGraph);
		
		logger.debug("new triples count (asserted) = {}", systemTriplesCountAfter);
		logger.debug("new triples count (inferred) = {}", inferredTriplesCountAfter);
		
		//system (asserted) triples should not change
		assertEquals(systemTriplesCountBefore, systemTriplesCountAfter);
		
		//inferred triples should not change
		assertEquals(inferredTriplesCountBefore, inferredTriplesCountAfter);		
	}
	
	@Test
	public void testUpdateAssetType2() {

		tester.switchModels(0, 2);

		AStoreWrapper store = tester.getStore();
		
		String systemGraph = tester.getGraph("system");
		String inferredGraph = tester.getGraph("system-inf");
		logger.debug("System graph: {}", systemGraph);
		logger.debug("Inferred graph: {}", inferredGraph);
		
		Asset a1 = querier.getSystemAsset(store, "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#4850d723"); //"FixedHost1"
		assertEquals("FixedHost1", a1.getLabel());
		assertEquals("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#FixedHost", a1.getType());
		
		Asset a2 = querier.getSystemAsset(store, "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#a614712d"); //"LAN"
		assertEquals("LAN", a2.getLabel());
		assertEquals("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#WiredLAN", a2.getType());
		
		long systemTriplesCountBefore = store.getCount(systemGraph);
		long inferredTriplesCountBefore = store.getCount(inferredGraph);
		
		logger.debug("current triples count (asserted) = {}", systemTriplesCountBefore);
		logger.debug("current triples count (inferred) = {}", inferredTriplesCountBefore);
		
		logger.debug("Changing asset type from FixedHost to Human");
		updater.updateAssetType(store, a1.getUri(), "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#Human");
		
		long systemTriplesCountAfter = store.getCount(systemGraph);
		long inferredTriplesCountAfter = store.getCount(inferredGraph);
		
		logger.debug("new triples count (asserted) = {}", systemTriplesCountAfter);
		logger.debug("new triples count (inferred) = {}", inferredTriplesCountAfter);
		
		//system (asserted) triples should reduce by one (as asserted relation will be deleted)
		assertEquals(systemTriplesCountBefore - 1, systemTriplesCountAfter);
		
		//inferred triples should not change
		assertEquals(inferredTriplesCountBefore, inferredTriplesCountAfter);
	}
	
	@Test
	public void testStoreAsset() {

		tester.switchModels(0, 0);

		//case 1: add an asset that didn't exist before
		Map<String, Asset> result = querier.getSystemAssets(tester.getStore());
		int size = result.size();

		Asset a = new Asset("http://example.com/myHost", "Asset 1",
				"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#Host", 100, 200);
		updater.storeAsset(tester.getStore(), a);

		result = querier.getSystemAssets(tester.getStore());
		assertEquals(size + 1, result.size());

		tester.getModel().printSizes(tester.getStore());

		//case2: modify an existing asset
		a.setIconPosition(99, 199);
		a.setLabel("New name");
		a.setType("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#LogicalSubnet");

		//N.B. cardinality values are no longer stored (replaced by population level), so these values will be ignored (see below)
		a.setMinCardinality(5);
		a.setMaxCardinality(9);

		//set example population level
		a.setPopulation("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#PopLevelMany");

		updater.storeAsset(tester.getStore(), a);
		Asset a2 = querier.getSystemAsset(tester.getStore(), a.getUri());

		//N.B. as cardinality values are not stored, their default is now -1
		//At some point, these settings will be removed completely
		assertEquals(-1, a2.getMinCardinality());
		assertEquals(-1, a2.getMaxCardinality());

		//check that population level has been set
		assertEquals("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#PopLevelMany", a2.getPopulation());

		result = querier.getSystemAssets(tester.getStore());
		assertEquals(size + 1, result.size());

		updater.updateAssetLabel(tester.getStore(), a.getUri(), "A label");
		Asset a3 = querier.getSystemAsset(tester.getStore(), a.getUri());
		assertEquals("A label", a3.getLabel());

		/* No longer required
		updater.updateAssetCardinality(tester.getStore(), a.getUri(), 8, 7);
		Asset a4 = querier.getSystemAsset(tester.getStore(), a.getUri());
		assertEquals(8, a4.getMinCardinality());
		assertEquals(7, a4.getMaxCardinality());
		*/

		//new test for updateAssetPopulation
		updater.updateAssetPopulation(tester.getStore(), a.getUri(), "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#PopLevelFew");
		Asset a4 = querier.getSystemAsset(tester.getStore(), a.getUri());
		assertEquals("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#PopLevelFew", a4.getPopulation());

		tester.getModel().printSizes(tester.getStore());
	}

	@Test
	public void testDeleteAsset() {

		tester.switchModels(0, 0);

		int startNum = querier.getSystemAssets(tester.getStore()).size();

		//delete asset
		Asset a = new Asset(
			tester.getModel().getNS("system") + "c43400a1", "Data Explorer", tester.getModel().getNS("domain") + "Process", 4410, 4570
		);
		updater.deleteAsset(tester.getStore(), a);
		Map<String, Asset> result = querier.getSystemAssets(tester.getStore());
		assertEquals(startNum-1, result.size());

		//delete asset by ID
		updater.deleteAsset(tester.getStore(), "df878bda");
		result = querier.getSystemAssets(tester.getStore());
		assertEquals(startNum-2, result.size());
	}

	@Test
	public void testStoreRelation() {
		/*
		 * Test updated 2023-02-08, for consistency with changes in SystemModelUpdater.storeRelation()
		 * that were needed to support population models with non-singleton system assets.
		 * 
		 * The change removed support for asserting relationship cardinality, as this can lead to
		 * inconsistencies in the population models which cannot be avoided. See #1387.
		 * 
		 * This means the test cannot now check that the source and target cardinality values are
		 * retrieved matching those specified in the store. In fact, they should no longer be added
		 * to the Relation entity being stored.
		 */
		tester.switchModels(0, 0);

		//add a relation that didn't exist before
		Set<Relation> result = querier.getSystemRelations(tester.getStore());
		int size = result.size();

		String from = tester.getModel().getNS("system") + "75ee08d3";
		String to = tester.getModel().getNS("system") + "5237319b";
		String type = tester.getModel().getNS("domain") + "connectedTo";
		String typeLabel = "connectedTo";

		Relation r = new Relation(from , null, to, null, type, typeLabel);

		updater.storeRelation(tester.getStore(), r);

		result = querier.getSystemRelations(tester.getStore());
		assertEquals(size + 1, result.size());

		Relation resultRel = querier.getSystemRelation(tester.getStore(), from, type, to);

		assertEquals(typeLabel, resultRel.getLabel());
	}

	@Test
	public void testDeleteRelation() {

		tester.switchModels(0, 0);

		Set<Relation> result = querier.getSystemRelations(tester.getStore());
		int rels = result.size();

		Relation r = new Relation(
			tester.getModel().getNS("system") + "75ee08d3", "",
			tester.getModel().getNS("system") + "c43400a1", "",
			tester.getModel().getNS("domain") + "hosts", ""
		);

		updater.deleteRelation(tester.getStore(), r);

		result = querier.getSystemRelations(tester.getStore());
		assertEquals(rels - 1, result.size());
	}

	/* Test setting/unsetting of a control set (proposed flag)
	 * This now includes population support, so SSM must set all 3 controls in an avg/min/max triplet
	*/
	@Test
	public void testToggleControlProposed() {

		tester.switchModels(1, 6); //population domain/system model

		//Define the 3 related control URIs (used in populations support)
		String csAvgUri = tester.getModel().getNS("system") + "CS-SoftwarePatching-8e8ae2ce";
		String csMinUri = tester.getModel().getNS("system") + "CS-SoftwarePatching_Min-8e8ae2ce";
		String csMaxUri = tester.getModel().getNS("system") + "CS-SoftwarePatching_Max-8e8ae2ce";

		logger.debug("Test setting control for {}", csAvgUri);

		//Create array of these controls
		String[] csUris = {csAvgUri, csMinUri, csMaxUri};
		logger.debug("CS URIs triplet: {}", Arrays.toString(csUris));

		//Get the average control set
		ControlSet csAvg = querier.getControlSet(tester.getStore(), csAvgUri);
		assertNotNull(csAvg);

		//This should be initially false
		assertFalse(csAvg.isProposed());

		//Stage 1 - setting the control

		//Now set the average control to true (proposed) and update it
		//This should have the side-effect of setting all 3 related controls (see above)
		//These 3 controls should be returned
		logger.debug("Setting average control set to true");
		csAvg.setProposed(true);
		csAvg.setCoverageLevel(null); //we are not updating coverage level here
		Set<String> updatedControls = updater.updateControlSet(tester.getStore(), csAvg);
		logger.debug("updatedControls: {}", updatedControls);

		//Check that there are 3 URIs in the returned list
		assertEquals(3, updatedControls.size());

		//More specifically, compare the result to the expected set of URIs
		assertThat(updatedControls).containsExactlyInAnyOrder(csUris);

		//Query the average control set, and check that it is proposed
		ControlSet csAvgUpdated = querier.getControlSet(tester.getStore(), csAvgUri);
		assertNotNull(csAvgUpdated);
		logger.debug("csAvgUpdated: {}", csAvgUpdated);
		assertTrue(csAvgUpdated.isProposed());

		//Query the Min control set, and check that it is proposed
		ControlSet csMinUpdated = querier.getControlSet(tester.getStore(), csMinUri);
		assertNotNull(csMinUpdated);
		logger.debug("csMinUpdated: {}", csMinUpdated);
		assertTrue(csMinUpdated.isProposed());

		//Query the Max control set, and check that it is proposed
		ControlSet csMaxUpdated = querier.getControlSet(tester.getStore(), csMaxUri);
		assertNotNull(csMaxUpdated);
		logger.debug("csMaxUpdated: {}", csMaxUpdated);
		assertTrue(csMaxUpdated.isProposed());

		//Stage 2 - unsetting the control

		//Now set the average control to false (not proposed) and update it
		//This should have the side-effect of setting all 3 related controls (see above)
		//These 3 controls should be returned
		logger.debug("Setting average control set to false");
		csAvg.setProposed(false);
		updatedControls = updater.updateControlSet(tester.getStore(), csAvg);
		logger.debug("updatedControls: {}", updatedControls);

		//Check that there are 3 URIs in the returned list
		assertEquals(3, updatedControls.size());

		//More specifically, compare the result to the expected set of URIs
		assertThat(updatedControls).containsExactlyInAnyOrder(csUris);

		//Query the average control set, and check that it is proposed
		csAvgUpdated = querier.getControlSet(tester.getStore(), csAvgUri);
		assertNotNull(csAvgUpdated);
		logger.debug("csAvgUpdated: {}", csAvgUpdated);
		assertFalse(csAvgUpdated.isProposed());

		//Query the Min control set, and check that it is proposed
		csMinUpdated = querier.getControlSet(tester.getStore(), csMinUri);
		assertNotNull(csMinUpdated);
		logger.debug("csMinUpdated: {}", csMinUpdated);
		assertFalse(csMinUpdated.isProposed());

		//Query the Max control set, and check that it is proposed
		csMaxUpdated = querier.getControlSet(tester.getStore(), csMaxUri);
		assertNotNull(csMaxUpdated);
		logger.debug("csMaxUpdated: {}", csMaxUpdated);
		assertFalse(csMaxUpdated.isProposed());
	}

	@Test
	public void testToggleControlWorkInProgress() {

		tester.switchModels(0, 0);

		String cs = tester.getModel().getNS("system") + "CS-AccessControl-ea504f75";

		ControlSet cs1 = querier.getControlSet(tester.getStore(), cs);
		assertFalse(cs1.isProposed());
		assertFalse(cs1.isWorkInProgress());

		cs1.setProposed(true);

		cs1.setWorkInProgress(true);
		updater.updateControlSet(tester.getStore(), cs1);
		ControlSet cs2 = querier.getControlSet(tester.getStore(), cs);
		assertTrue(cs2.isWorkInProgress());

		cs2.setWorkInProgress(false);
		updater.updateControlSet(tester.getStore(), cs2);
		ControlSet cs3 = querier.getControlSet(tester.getStore(), cs);
		assertFalse(cs3.isWorkInProgress());
	}

	@Test
	public void testToggleAllControlsProposed() {

		tester.switchModels(0, 0);

		Map<String, ControlSet> css1 = querier.getControlSets(tester.getStore());

		updater.updateControlSets(tester.getStore(), css1.keySet(), true, false);

		Map<String, ControlSet> css2 = querier.getControlSets(tester.getStore());

		for (String cs : css2.keySet()) {
			assertTrue(css2.get(cs).isProposed());
			assertFalse(css2.get(cs).isWorkInProgress());
		}

		updater.updateControlSets(tester.getStore(), css2.keySet(), false, false);

		Map<String, ControlSet> css3 = querier.getControlSets(tester.getStore());

		for (String cs : css3.keySet()) {
			assertFalse(css3.get(cs).isProposed());
			assertFalse(css3.get(cs).isWorkInProgress());
		}
	}

	@Test
	public void testToggleAllControlsWorkInProgress() {

		tester.switchModels(0, 0);

		Map<String, ControlSet> css1 = querier.getControlSets(tester.getStore());

		updater.updateControlSets(tester.getStore(), css1.keySet(), true, true);

		Map<String, ControlSet> css2 = querier.getControlSets(tester.getStore());

		for (String cs : css2.keySet()) {
			assertTrue(css2.get(cs).isProposed());
			assertTrue(css2.get(cs).isWorkInProgress());
		}

		updater.updateControlSets(tester.getStore(), css2.keySet(), true, false);

		Map<String, ControlSet> css3 = querier.getControlSets(tester.getStore());

		for (String cs : css3.keySet()) {
			assertTrue(css3.get(cs).isProposed());
			assertFalse(css3.get(cs).isWorkInProgress());
		}
	}

	@Test
	public void testSetAllControlsWorkInProgressButNotProposed() {

		tester.switchModels(0, 0);

		Map<String, ControlSet> css1 = querier.getControlSets(tester.getStore());

		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> updater.updateControlSets(tester.getStore(), css1.keySet(), false, true)
			)
			.withMessage(
				"Controls cannot be work in progress but not proposed"
			);
	}

	@Test
	public void testAcceptThreat() {

		tester.switchModels(0, 0);

		String threat = tester.getModel().getNS("system") + "P.A.HP.2-HP_df878bda_2d5019c7";
		String justification = "Because!";

		Threat result = querier.getSystemThreat(tester.getStore(), threat);
		assertNotNull(threat);
		assertNull(result.getAcceptanceJustification());

		updater.acceptThreat(tester.getStore(), threat, justification);

		//Threat should be accepted
		result = querier.getSystemThreat(tester.getStore(), threat);
		assertEquals(justification, result.getAcceptanceJustification());

		updater.acceptThreat(tester.getStore(), threat, null);

		//Threat should not be accepted
		result = querier.getSystemThreat(tester.getStore(), threat);
		assertNull(result.getAcceptanceJustification());
	}

	@Test
	public void testUpdateTWAS() {

		tester.switchModels(0, 0);

		String asset1 = tester.getModel().getNS("system") + "5237319b";
		String twas = tester.getModel().getNS("system") + "TWAS-UserTW-193935ed";

		Asset a1 = querier.getSystemAsset(tester.getStore(), asset1);
		TrustworthinessAttributeSet twas1 = a1.getTrustworthinessAttributeSets().get(twas);
		assertEquals(tester.getModel().getNS("domain") + "TrustworthinessLevelVeryLow", twas1.getAssertedTWLevel().getUri());
		assertEquals(tester.getModel().getNS("domain") + "TrustworthinessLevelVeryLow", twas1.getInferredTWLevel().getUri());

		Level newLevel = new Level();
		newLevel.setUri(tester.getModel().getNS("domain") + "TrustworthinessLevelLow");
		newLevel.setLabel("Low");
		twas1.setAssertedTWLevel(newLevel);
		twas1.setInferredTWLevel(newLevel);

		updater.updateTWAS(tester.getStore(), twas1);

		Asset a2 = querier.getSystemAsset(tester.getStore(), asset1);
		TrustworthinessAttributeSet twas2 = a2.getTrustworthinessAttributeSets().get(twas);
		assertEquals(tester.getModel().getNS("domain") + "TrustworthinessLevelLow", twas2.getAssertedTWLevel().getUri());
		//does not update!
		assertEquals(tester.getModel().getNS("domain") + "TrustworthinessLevelVeryLow", twas2.getInferredTWLevel().getUri());
	}

	@Test
	public void testUpdateMS() {

		tester.switchModels(0, 0);

		String ms = tester.getModel().getNS("system") + "MS-LossOfAvailability-2d5019c7";

		MisbehaviourSet ms1 = querier.getMisbehaviourSet(tester.getStore(), ms, false); //no need for causes here
		assertEquals(tester.getModel().getNS("domain") + "ImpactLevelVeryLow", ms1.getImpactLevel().getUri());

		Level newLevel = new Level();
		newLevel.setUri(tester.getModel().getNS("domain") + "ImpactLevelMedium");
		newLevel.setLabel("Medium");
		ms1.setImpactLevel(newLevel);

		updater.updateMS(tester.getStore(), ms1);

		MisbehaviourSet ms2 = querier.getMisbehaviourSet(tester.getStore(),  ms, false); //no need for causes here
		assertEquals(tester.getModel().getNS("domain") + "ImpactLevelMedium", ms2.getImpactLevel().getUri());
	}

	@Test@Ignore
	//TODO: investigate if this is still needed
	public void testCleanAssertedModel() {

		tester.switchModels(0, 0);

		assertEquals(245, tester.getStore().getCount(tester.getModel().getGraph("system")));
		updater.cleanAssertedModel(tester.getStore());
		assertEquals(245, tester.getStore().getCount(tester.getModel().getGraph("system")));
	}

	@Test
	public void testChangeAsset() {

		tester.switchModels(0, 0);

		//case 1: add two assets that didn't exist before and a relationship between them
		Map<String, Asset> result = querier.getSystemAssets(tester.getStore());
		int size = result.size();

		Asset a = new Asset(tester.getModel().getNS("system") + "myHost", "Asset 1", tester.getModel().getNS("domain") + "FixedHost", 100, 200);
		updater.storeAsset(tester.getStore(), a);

		result = querier.getSystemAssets(tester.getStore());
		assertEquals(size + 1, result.size());

		Asset a2 = new Asset(tester.getModel().getNS("system") + "myProcess", "Asset 2", tester.getModel().getNS("domain") + "Process", 100, 400);
		updater.storeAsset(tester.getStore(), a2);

		int relsBefore = querier.getSystemRelations(tester.getStore()).size();

		Relation r = new Relation(a2.getUri(), a2.getID(), a.getUri(), a.getID(), tester.getModel().getNS("domain") + "end", "end");
		updater.storeRelation(tester.getStore(), r);

		Set<Relation> relationsResult = querier.getSystemRelations(tester.getStore());
		assertEquals(relsBefore + 1, relationsResult.size());

		//case2: modify an existing asset so that relation should remain
		a.setIconPosition(99, 199);
		a.setLabel("New name");
		a.setType(tester.getModel().getNS("domain") + "MobileHost");
		updater.storeAsset(tester.getStore(), a);

		result = querier.getSystemAssets(tester.getStore());
		assertEquals(size + 2, result.size());
		assertEquals(relationsResult.size(), querier.getSystemRelations(tester.getStore()).size());

		//case3: modify an existing asset so that relation should be deleted
		a.setIconPosition(98, 198);
		a.setLabel("New new name");
		a.setType(tester.getModel().getNS("domain") + "Human");
		updater.storeAsset(tester.getStore(), a);

		result = querier.getSystemAssets(tester.getStore());
		assertEquals(size + 2, result.size());
		assertEquals(relationsResult.size() - 1, querier.getSystemRelations(tester.getStore()).size());
	}

	@Test
	public void testSetIsValidatingUpdate() {
		// Model contains metadata
		tester.switchModels(0, 0);
		setIsValidating();
	}

	@Test
	public void testSetIsValidatingCreate() {
		// Model does not contain metadata
		tester.switchModels(0, 1);
		setIsValidating();
	}

	private void setIsValidating() {
		assertEquals(false, querier.isValidating(tester.getStore()));

		updater.setIsValidating(tester.getStore(), true);
		assertEquals(true, querier.isValidating(tester.getStore()));

		updater.setIsValidating(tester.getStore(), false);
		assertEquals(false, querier.isValidating(tester.getStore()));
	}

	@Test
	public void testSetCalculatingControlsUpdate() {
		// Model contains metadata
		tester.switchModels(0, 0);
		setCalculatingControls();
	}

	@Test
	public void testSetCalculatingControlsCreate() {
		// Model does not contain metadata
		tester.switchModels(0, 1);
		setCalculatingControls();
	}

	private void setCalculatingControls() {
		assertEquals(false, querier.isCalculatingControls(tester.getStore()));

		updater.setIsCalculatingControls(tester.getStore(), true);
		assertEquals(true, querier.isCalculatingControls(tester.getStore()));

		updater.setIsCalculatingControls(tester.getStore(), false);
		assertEquals(false, querier.isCalculatingControls(tester.getStore()));
	}

	@Test
	public void testSetCreateDateUpdate() {
		// Model contains metadata
		tester.switchModels(0, 0);
		setCreateDate();
	}

	@Test
	public void testSetCreateDateCreate() {
		// Model does not contain metadata
		tester.switchModels(0, 1);
		setCreateDate();
	}

	@Test
	public void testStoreAssetGroup() {
        tester.switchModels(0, 0);

        Asset a1 = querier.getSystemAssetById(tester.getStore(),"2d5019c7");
        Asset a2 = querier.getSystemAssetById(tester.getStore(),"e46a02c6");

        Map<String, Asset> groupAssets = new HashMap<>();
        groupAssets.put(a1.getUri(), a1);
        groupAssets.put(a2.getUri(), a2);

        AssetGroup assetGroup1 = new AssetGroup(tester.getGraph("system-ui") + "#Group-b2b2b2b2", "GroupLabel",
                groupAssets, 10, 20, 300, 400, true);
		assertTrue(updater.storeAssetGroup(tester.getStore(), assetGroup1));
		AssetGroup returnedGroup =  querier.getAssetGroupById(tester.getStore(), assetGroup1.getID());
		assertEquals("GroupLabel", returnedGroup.getLabel());
		assertEquals(10, returnedGroup.getX());
		assertEquals(2, returnedGroup.getAssets().size());
		assertEquals(300, returnedGroup.getWidth());
		assertEquals(400, returnedGroup.getHeight());
		assertTrue(returnedGroup.isExpanded());

		assetGroup1.setLabel("NewLabel");
		updater.storeAssetGroup(tester.getStore(), assetGroup1);
		assertEquals("NewLabel", querier.getAssetGroupById(tester.getStore(), assetGroup1.getID()).getLabel());
		assertEquals(2, returnedGroup.getAssets().size());

        AssetGroup assetGroup2 = new AssetGroup(tester.getGraph("system-ui") + "#Group-b0b0b0b0", null,
                new HashMap<>(), 0, 0, 0, 0, true);
        assertTrue(updater.storeAssetGroup(tester.getStore(), assetGroup2));

		AssetGroup assetGroup3 = new AssetGroup(tester.getGraph("system-ui") + "#Group-f2f2f2f2", null,
				new HashMap<>(), 0, 0, 0, 0, true);
		assetGroup3.addAsset(a2);
		assertFalse(updater.storeAssetGroup(tester.getStore(), assetGroup3));
    }

    @Test
    public void testAddAssetsToGroup() {
        tester.switchModels(0, 5);

        AssetGroup assetGroup = querier.getAssetGroupById(
                tester.getStore(), "6fa82559", querier.getSystemAssets(tester.getStore()));

        Asset a1 = querier.getSystemAssetById(tester.getStore(), "ca9c0d4");
        Asset a2 = querier.getSystemAssetById(tester.getStore(), "79c22efc");
        Asset a3 = querier.getSystemAssetById(tester.getStore(), "2d5019c7");

        Set<Asset> addAssets = new HashSet<>();
        addAssets.add(a1);
        addAssets.add(a2);
        addAssets.add(a3); // Already in the group

        assertTrue(updater.addAssetsToAssetGroup(tester.getStore(), assetGroup, addAssets));
        assetGroup = querier.getAssetGroupById(
                tester.getStore(), "6fa82559", querier.getSystemAssets(tester.getStore()));

        assertEquals(5, assetGroup.getAssets().size());
        assertTrue(assetGroup.getAssets().containsKey(a1.getUri()));
        assertTrue(assetGroup.getAssets().containsKey(a2.getUri()));
        assertTrue(assetGroup.getAssets().containsKey(a3.getUri()));

		AssetGroup assetGroup2 = querier.getAssetGroupById(
				tester.getStore(), "d0cb21cd", querier.getSystemAssets(tester.getStore()));
		assertFalse(updater.addAssetsToAssetGroup(tester.getStore(), assetGroup2, addAssets));
    }

    @Test
    public void testRemoveAssetsFromGroup() {
        tester.switchModels(0, 5);

        AssetGroup assetGroup = querier.getAssetGroupById(
                tester.getStore(), "42d51951", querier.getSystemAssets(tester.getStore()));

		Asset a1 = querier.getSystemAssetById(tester.getStore(),"2d5019c7");
		Asset a2 = querier.getSystemAssetById(tester.getStore(),"e46a02c6");
		Asset a3 = querier.getSystemAssetById(tester.getStore(), "c1078c6a");
		Set<Asset> removeAssets = new HashSet<>();
		removeAssets.add(a1);
		removeAssets.add(a2);
		removeAssets.add(a3); // Not present in group

		assertTrue(updater.removeAssetsFromAssetGroup(tester.getStore(), assetGroup, removeAssets));
		assetGroup = querier.getAssetGroupById(
				tester.getStore(), "42d51951", querier.getSystemAssets(tester.getStore()));

		assertEquals(1, assetGroup.getAssets().size());
    }

	@Test
	public void testDeleteAssetGroup() {
		tester.switchModels(0, 5);

		Map<String, Asset> assets = querier.getSystemAssets(tester.getStore());
		AssetGroup assetGroup1 = querier.getAssetGroupById(tester.getStore(), "4282e645", assets);
		assertTrue(updater.deleteAssetGroup(tester.getStore(), assetGroup1, false));
		assertFalse(updater.deleteAssetGroup(tester.getStore(), assetGroup1, false));
		for (Asset asset : assetGroup1.getAssets().values()) {
			assertNotNull(querier.getSystemAssetById(tester.getStore(), asset.getID()));
		}

		AssetGroup assetGroup2 = querier.getAssetGroupById(tester.getStore(), "77773cc9", assets);
		assertTrue(updater.deleteAssetGroup(tester.getStore(), assetGroup2, true));
		for (Asset asset : assetGroup2.getAssets().values()) {
			assertNull(querier.getSystemAssetById(tester.getStore(), asset.getID()));
		}
	}

	@Test
	public void testUpdateAssetGroupLocation() {
		tester.switchModels(0, 5);

		AssetGroup assetGroup = querier.getAssetGroupById(tester.getStore(), "42d51951");
		updater.updateAssetGroupLocation(tester.getStore(), assetGroup, 50, 100);
		assertEquals(50, assetGroup.getX());
		assertEquals(100, assetGroup.getY());
		assetGroup = querier.getAssetGroupById(tester.getStore(), "42d51951");
		assertEquals(50, assetGroup.getX());
		assertEquals(100, assetGroup.getY());
	}

	@Test
	public void testUpdateAssetGroupSize() {
		tester.switchModels(0, 5);

		AssetGroup assetGroup = querier.getAssetGroupById(tester.getStore(), "42d51951");
		logger.info("default width: {}", assetGroup.getWidth());
		logger.info("default height: {}", assetGroup.getHeight());
		assertEquals(400, assetGroup.getWidth()); //should have default value, as not specified
		assertEquals(400, assetGroup.getHeight()); //should have default value, as not specified
		
		updater.updateAssetGroupSize(tester.getStore(), assetGroup, 300, 500);
		logger.info("returned width: {}", assetGroup.getWidth());
		logger.info("returned height: {}", assetGroup.getHeight());
		assertEquals(300, assetGroup.getWidth());
		assertEquals(500, assetGroup.getHeight());
		
		AssetGroup assetGroup2 = querier.getAssetGroupById(tester.getStore(), "42d51951");
		logger.info("updated width: {}", assetGroup2.getWidth());
		logger.info("updated height: {}", assetGroup2.getHeight());
		assertEquals(300, assetGroup2.getWidth());
		assertEquals(500, assetGroup2.getHeight());
	}

	@Test
	public void testUpdateAssetGroupExpanded() {
		tester.switchModels(0, 5);

		AssetGroup assetGroup = querier.getAssetGroupById(tester.getStore(), "42d51951");
		logger.info("default expanded: {}", assetGroup.isExpanded());
		assertTrue(assetGroup.isExpanded()); //should be expanded by default, as not specified
		
		updater.updateAssetGroupExpanded(tester.getStore(), assetGroup, false); //set group collapsed
		logger.info("returned expanded: {}", assetGroup.isExpanded());
		assertFalse(assetGroup.isExpanded());
		
		AssetGroup assetGroup2 = querier.getAssetGroupById(tester.getStore(), "42d51951");
		logger.info("updated expanded: {}", assetGroup2.isExpanded());
		assertFalse(assetGroup2.isExpanded());
	}

	@Test
	public void testUpdateAssetGroupLabel() {
		tester.switchModels(0, 5);

		AssetGroup assetGroup = querier.getAssetGroupById(tester.getStore(), "42d51951");
		logger.info("initial label: {}", assetGroup.getLabel());
		assertEquals("TestGroupRemoveAssets", assetGroup.getLabel());
		
		updater.updateAssetGroupLabel(tester.getStore(), assetGroup, "New label");
		logger.info("returned label: {}", assetGroup.getLabel());
		assertEquals("New label", assetGroup.getLabel());
		
		AssetGroup assetGroup2 = querier.getAssetGroupById(tester.getStore(), "42d51951");
		logger.info("updated label: {}", assetGroup2.getLabel());
		assertEquals("New label", assetGroup.getLabel());
	}

	private void setCreateDate() {
		Date now = new Date();
		String expected = (new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")).format(now);
		updater.setCreateDate(tester.getStore(), now);
		assertEquals(expected, querier.getCreateDate(tester.getStore()));
	}
}
