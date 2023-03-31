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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
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
import uk.ac.soton.itinnovation.security.model.system.ComplianceSet;
import uk.ac.soton.itinnovation.security.model.system.ComplianceThreat;
import uk.ac.soton.itinnovation.security.model.system.ControlSet;
import uk.ac.soton.itinnovation.security.model.system.ControlStrategy;
import uk.ac.soton.itinnovation.security.model.system.MisbehaviourSet;
import uk.ac.soton.itinnovation.security.model.system.MetadataPair;
import uk.ac.soton.itinnovation.security.model.system.Model;
import uk.ac.soton.itinnovation.security.model.system.Pattern;
import uk.ac.soton.itinnovation.security.model.system.Relation;
import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;
import uk.ac.soton.itinnovation.security.model.system.SecondaryEffectStep;
import uk.ac.soton.itinnovation.security.model.system.Threat;
import uk.ac.soton.itinnovation.security.model.system.TrustworthinessAttributeSet;
import uk.ac.soton.itinnovation.security.modelquerier.SystemModelQuerier;
import uk.ac.soton.itinnovation.security.modelquerier.SystemModelUpdater;
import uk.ac.soton.itinnovation.security.modelquerier.util.TestHelper;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@RunWith(JUnit4.class)
public class SystemModelQuerierTester extends TestCase {

	public static Logger logger = LoggerFactory.getLogger(SystemModelQuerierTester.class);

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
		tester.addDomain(1, "modelquerier/FOGPROTECT-3j1-5.nq.gz", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-fogprotect");

		//be cautious with sharing URIs: if the validated model contains anything asserted, the unvalidated model
		//will have it too as it shared the same graph URI. For this reason, we define a new URI for this model to avoid clashes
		tester.addSystem(0, "modelquerier/system-shield.nq.gz",
			"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3dda");
		tester.addSystem(1, "modelquerier/system-shield-without-metadata.nq.gz",
			"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3dda",
			"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3ddb");
		tester.addSystem(2, "modelquerier/system-shield-with-metadata-pairs.nq.gz",
			"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3dda",
			"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3ddc");
		tester.addSystem(3, "modelquerier/system-shield-with-risk-calculation-mode.nq.gz",
			"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3dda",
			"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3ddd");
		tester.addSystem(4, "modelquerier/system-shield-with-invalid-risk-calculation-mode.nq.gz",
			"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3dda",
			"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3dde");
		tester.addSystem(5, "modelquerier/misbehaviour-and-twa-visibility.nq.gz",
			"http://it-innovation.soton.ac.uk/system/5ede667da04c8a2a839f809f");
		tester.addSystem(6, "modelquerier/system-test-groups.nq.gz",
				"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3dda");

		tester.setUp();

		querier = new SystemModelQuerier(tester.getModel());
		updater = new SystemModelUpdater(tester.getModel());

		logger.info("SystemModelQuerier tests executing...");
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
//		logger.info("Exporting test model");
//		tester.exportTestModel("build/build/test-results/" + name.getMethodName(), true, true, false);
	}
	
	// Tests //////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Test
	public void testGetModelInfo() {

		tester.switchModels(0, 0);

		Model result = querier.getModelInfo(tester.getStore());
		assertEquals(tester.getSystems().get(0).getEffectiveGraph(), result.getUri());
		assertEquals("GA Test", result.getLabel());
		assertEquals("Test model for the SHiELD GA", result.getDescription());
		assertEquals(tester.getDomains().get(0).getGraph(), result.getDomain());
		assertEquals(true, result.isRisksValid());
		assertEquals(false, result.isCalculatingRisk());
		assertEquals(false, result.isValidating());
		assertEquals(true, result.getValid());
		assertEquals(null, result.getRiskCalculationMode());
		//TODO: use a model that's got this asserted
		//assertEquals("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#RiskLevelVeryLow", result.getRisk().getUri());
		//TODO: check result.getCreated(), result.getModified(), result.getUser()
	}

	@Test
	public void testGetModelInfoRiskCalculationMode() {

		tester.switchModels(0, 3);

		Model result = querier.getModelInfo(tester.getStore());
		assertEquals(tester.getSystems().get(3).getEffectiveGraph(), result.getUri());
		assertEquals("GA Test", result.getLabel());
		assertEquals("Test model for the SHiELD GA", result.getDescription());
		assertEquals(tester.getDomains().get(0).getGraph(), result.getDomain());
		assertEquals(true, result.isRisksValid());
		assertEquals(false, result.isCalculatingRisk());
		assertEquals(false, result.isValidating());
		assertEquals(true, result.getValid());
		assertEquals(RiskCalculationMode.FUTURE, result.getRiskCalculationMode());
	}

	@Test
	public void testGetModelInfoInvalidRiskCalculationMode() {
		tester.switchModels(0, 4);

		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> querier.getModelInfo(tester.getStore())
			)
			.withMessage(
				"No enum constant uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode.NotAValidMode"
			);
	}

	@Test
	public void testGetLevels() {

		tester.switchModels(0, 0);

		Map<String, Level> result = querier.getLevels(tester.getStore(), "TrustworthinessLevel");
		assertEquals(5, result.size());

		result = querier.getLevels(tester.getStore(), "Likelihood", "RiskLevel");
		assertEquals(10, result.size());

		//TODO: check the sorting is correct
		List<Level> levels = new ArrayList<>();
		levels.addAll(result.values());
		Collections.sort(levels);
		logger.debug("{}", levels);
	}

	@Test
	public void testGetSystemAssets() {

		tester.switchModels(0, 0);

		//all
		Map<String, Asset> assets = querier.getSystemAssets(tester.getStore());
		assertEquals(83, assets.size());

		//by URI
		String uri = tester.getNS("system") + "7a1c3d65";
		Asset result = querier.getSystemAsset(tester.getStore(), uri);

		Asset a = new Asset();
		a.setUri(uri);

		assertTrue(a.getID().equals(result.getID()));

		//by ID
		result = querier.getSystemAssetById(tester.getStore(), "9e438871");

		a = new Asset();
		a.setUri(uri);

		assertTrue(a.equals(result));

		//using a host here which has inferred assets
		result = querier.getSystemAsset(tester.getStore(), tester.getNS("system") + "1ec8173cba2a379");
		assertEquals(12, result.getInferredAssets().size());
		assertEquals(8, result.getMisbehaviourSets().size());
		assertEquals(4, result.getTrustworthinessAttributeSets().size());
		assertEquals(13, result.getControlSets().size());
	}

	@Test
	public void testGetBasicSystemAssets() {

		tester.switchModels(0, 0);

		Map<String, Asset> assets = querier.getBasicSystemAssets(tester.getStore());
		assertEquals(83, assets.size());

		for (Asset asset : assets.values()) {
			assertEquals(0, asset.getInferredAssets().size());
			assertEquals(0, asset.getMisbehaviourSets().size());
			assertEquals(0, asset.getTrustworthinessAttributeSets().size());
			assertEquals(0, asset.getControlSets().size());
		}
	}

	@Test
	public void testGetSystemRelations() {

		tester.switchModels(0, 0);

		//all
		Set<Relation> relations = querier.getSystemRelations(tester.getStore());
		assertEquals(235, relations.size());

		//by URI
		Relation result = querier.getSystemRelation(tester.getStore(),
				tester.getNS("system") + "75ee08d3", tester.getNS("domain") + "hosts", tester.getNS("system") + "c43400a1");
		assertNotNull(result);
		assertEquals(1, result.getSourceCardinality());
		assertEquals(1, result.getTargetCardinality());
		//logger.debug("{}", result.getID());

		result = querier.getSystemRelation(tester.getStore(),
				tester.getNS("system") + "75ee08d3", tester.getNS("domain") + "uses", tester.getNS("system") + "c43400a1");
		assertNull(result);

		//by ID
		result = querier.getSystemRelationById(tester.getStore(), "3813bd4a");
		assertNotNull(result);

		result = querier.getSystemRelationById(tester.getStore(), "123");
		assertNull(result);
	}

	@Test
	public void testGetSystemPatterns() {

		tester.switchModels(0, 0);

		//all
		Map<String, Pattern> patterns = querier.getSystemPatterns(tester.getStore());
		assertEquals(156, patterns.size());

		//TODO: check in the validator why the URI has a reference to the root label
		String uri = tester.getNS("system") + "HP_a9feda22_79c22efc";
		Pattern result = querier.getSystemPattern(tester.getStore(), uri);

		assertEquals(uri, result.getUri());
		assertEquals("HP_Doctor Tablet_Healthcare App", result.getLabel());
		assertEquals(2, result.getNodes().size());
		assertEquals(1, result.getLinks().size());
		
		patterns = querier.getSystemPatterns(tester.getStore(), tester.getNS("system") + "d01d057a");
		assertEquals(5, patterns.size());
		
		patterns = querier.getSystemPatternsForValidation(tester.getStore());
		assertEquals(8, patterns.size());
	}

	@Test
	public void testGetSystemThreats() {

		tester.switchModels(0, 0);

		//all
		logger.debug("Getting all threats");
		Map<String, Threat> threats = querier.getSystemThreats(tester.getStore());
		logger.debug("done");
		assertEquals(264, threats.size());

		//by asset
		logger.debug("Getting all threats for one asset");
		threats = querier.getSystemThreats(tester.getStore(), tester.getNS("system") + "c43400a1");
		logger.debug("done");
		assertEquals(12, threats.size());

		//by URI
		String uri = tester.getNS("system") + "P.UTW.HP.1-HP_66004713_63f2779b";
		Threat result = querier.getSystemThreat(tester.getStore(), uri);

		Threat t = new Threat();
		t.setUri(uri);
		assertEquals(t.getID(), result.getID());

		//--check other threat properties
		assertEquals(tester.getNS("system") + "a7d173d694df404", result.getThreatensAssets());
		assertEquals(tester.getNS("domain") + "P.UTW.HP.1", result.getType());
		assertEquals(tester.getNS("system") + "HP_66004713_63f2779b", result.getPattern().getUri());
		assertEquals(1, result.getMisbehaviours().size());

		//by ID
		String id = "NotAValidID";
		result = querier.getSystemThreatById(tester.getStore(), id);
		assertNull(result);
		id = "81c840c3";
		result = querier.getSystemThreatById(tester.getStore(), id);
		assertEquals(id, result.getID());

		
		
		//--check other threat properties
		//assertEquals(1, result.getDirectEffects().size()); //no longer stored
		assertEquals(1, result.getIndirectEffects().size());
		assertEquals(1, result.getControlStrategies().size());
		assertEquals(1, result.getEntryPoints().size());
		assertTrue(result.getEntryPoints().containsKey(tester.getNS("system") + "TWAS-ManagementTW-ece77c11"));
		assertEquals("Low", result.getLikelihood().getLabel());
		assertEquals("Medium", result.getRiskLevel().getLabel());

		//a secondary effect
		result = querier.getSystemThreat(tester.getStore(), tester.getNS("system") + "C.U.CS.2-CS_e46a02c6_63f2779b");
		assertEquals(1, result.getSecondaryEffectConditions().size());
		assertEquals(1, result.getMisbehaviours().size());
	}

	@Test
	public void testGetMisbehaviourSets() {

		tester.switchModels(0, 0);

		//all
		Map<String, MisbehaviourSet> result = querier.getMisbehaviourSets(tester.getStore(), false); //no need for causes here
		assertEquals(226, result.size());

		//by URI
		String uri = tester.getNS("system") + "MS-LossOfConfidentiality-9e438871";
		MisbehaviourSet ms = querier.getMisbehaviourSet(tester.getStore(), uri, true); //get causes too
		assertEquals(uri, ms.getUri());
		assertEquals(tester.getNS("system") + "7a1c3d65", ms.getAsset());
		assertEquals("Patient Genome", ms.getAssetLabel());
		assertEquals(tester.getNS("domain") + "LossOfConfidentiality", ms.getMisbehaviour());
		assertEquals("Loss of Confidentiality", ms.getMisbehaviourLabel());
		assertEquals(tester.getNS("domain") + "ImpactLevelHigh", ms.getImpactLevel().getUri());
		assertEquals(tester.getNS("domain") + "LikelihoodLow", ms.getLikelihood().getUri());
		assertEquals(tester.getNS("domain") + "RiskLevelMedium", ms.getRiskLevel().getUri());
		assertEquals(6, ms.getDirectCauses().size());
		assertEquals(6, ms.getIndirectCauses().size());
		assertEquals(0, ms.getRootCauses().size());

		//by ID
		//TODO: not sure if they have got IDs in the store...
//		ms = querier.getMisbehaviourSetByID(tester.getStore(), "115acffe");
//		assertEquals(tester.getNS("system") + "MS-LossOfConfidentiality-9e438871", ms.getUri());
	}

	@Test
	public void testGetMisbehaviourSetVisibility() {

		tester.switchModels(1, 5);

		//Visible
		String uri1 = tester.getNS("system") + "MS-LossOfAvailability-19c8a165";
		assertTrue(querier.getMisbehaviourSet(tester.getStore(), uri1, false).isVisible()); //no need for causes here

		//Not visible
		String uri2 = tester.getNS("system") + "MS-LossOfSecurityTW-19c8a165";
		assertFalse(querier.getMisbehaviourSet(tester.getStore(), uri2, false).isVisible()); //no need for causes here
	}

	@Test
	public void testGetMisbehaviourSetVisibilityNotInDomainModel() {

		tester.switchModels(0, 0);

		//Misbehaviour visibility not defined in domain model - defaults to visible
		String uri = tester.getNS("system") + "MS-LossOfConfidentiality-9e438871";
		assertTrue(querier.getMisbehaviourSet(tester.getStore(), uri, false).isVisible()); //no need for causes here
	}

	@Test
	public void testGetMisbehaviourSetDirectEffectsPrimary() {

		tester.switchModels(1, 5);

		//MS that undermines a TWAS causes a primary threat
		//Linked via TWIS-19c8a165-ManagementTW-LossOfControl
		String uri = tester.getNS("system") + "MS-LossOfControl-19c8a165";
		//Use true below to get causes and effects
		assertThat(querier.getMisbehaviourSet(tester.getStore(), uri, true).getDirectEffects())
			.containsExactly(tester.getNS("system") + "S.UTW.S.1-S_19c8a165");
	}

	@Test
	public void testGetMisbehaviourSetDirectEffectsSecondary() {

		tester.switchModels(1, 5);

		//MS unrelated to a TWAS causes a secondary threat
		String uri = tester.getNS("system") + "MS-Overloaded-19c8a165";
		//Use true below to get causes and effects
		assertThat(querier.getMisbehaviourSet(tester.getStore(), uri, true).getDirectEffects())
			.containsExactly(tester.getNS("system") + "DC.A.DC.1-DC_19c8a165");
	}

	@Test
	public void testGetControlSets() {

		tester.switchModels(0, 0);

		//all
		Map<String, ControlSet> css = querier.getControlSets(tester.getStore());
		assertEquals(382, css.size());

		//by asset URI
		String assetURI = tester.getNS("system") + "7a1c3d65";
		css = querier.getControlSets(tester.getStore(), assetURI, null);
		logger.info("Getting control sets for asset: {}", assetURI);
		logger.info("no. contrpl sets = {}", css.size());
		assertEquals(3, css.size());

		//by URI
		String uri = tester.getNS("system") + "CS-SoftwarePatching-ece77c11";
		ControlSet result = querier.getControlSet(tester.getStore(), uri);

		//check other properties
		assertEquals(false, result.isProposed());
		assertEquals(uri, result.getUri());
		assertEquals("SoftwarePatching", result.getLabel());
		assertEquals("b27190f0", result.getID());
		assertEquals(tester.getNS("system") + "805c6969", result.getAssetUri());
		assertEquals(tester.getNS("domain") + "SoftwarePatching", result.getControl());
		assertEquals("The software running on the host is maintained by systematic and prompt application of software patches, once they have been appropriately tested. TW=High, but reduced to Very Low when there is a known vulnerability for which a patch is not yet available.", result.getDescription());
		assertEquals(true, result.isAssertable());
	}

	@Test
	public void testGetControlStrategies() {

		tester.switchModels(0, 0);

		Map<String, ControlStrategy> result = querier.getControlStrategies(tester.getStore());
		assertEquals(278, result.size());

		//check control strategies with more than one control set
		ControlStrategy csg = result.get(tester.getNS("system") + "SP.Auth.CSSP.1-CSSP_79c22efc_30502f2a_b2cfe39c-CSG-ServiceAuthentication");
		logger.debug("csg: \n{}", csg.toString());
		logger.info("Mandatory control sets: {}", csg.getMandatoryControlSets().size());
		logger.info("Optional control sets: {}", csg.getOptionalControlSets().size());
		assertEquals(2, csg.getMandatoryControlSets().size());
		//assertEquals(tester.getNS("system") + "SP.Auth.CSSP.1-CSSP_79c22efc_30502f2a_b2cfe39c", csg.getThreat());
		assertEquals("ServiceAuthentication", csg.getLabel());
	}

	@Test
	public void testGetTrustworthinessAttributeSets() {

		tester.switchModels(0, 0);

		//all
		Map<String, TrustworthinessAttributeSet> result = querier.getTrustworthinessAttributeSets(tester.getStore());
		assertEquals(75, result.size());

		//by URI
		String uri = tester.getNS("system") + "TWAS-ManagementTW-2d5019c7";
		TrustworthinessAttributeSet twas = querier.getTrustworthinessAttributeSet(tester.getStore(), uri);
		assertEquals(uri, twas.getUri());
		assertEquals(tester.getNS("system") + "c43400a1", twas.getAsset());
		assertEquals(tester.getNS("domain") + "ManagementTW", twas.getAttribute().getUri());
		assertEquals(tester.getNS("domain") + "TrustworthinessLevelVeryHigh", twas.getAssertedTWLevel().getUri());
		assertEquals(tester.getNS("domain") + "TrustworthinessLevelVeryHigh", twas.getInferredTWLevel().getUri());

		//by threat
		result = querier.getTrustworthinessAttributeSets(tester.getStore(), tester.getNS("system") + "SP.Auth.CSSP.1-CSSP_e46a02c6_63f2779b_8ab343a5");
		assertEquals(1, result.size());
		logger.debug("{}", result);
		assertTrue(result.containsKey(tester.getNS("system") + "TWAS-UserTW-e46a02c6"));

		//by asset URI
		result = querier.getTrustworthinessAttributeSetsForAssetURI(tester.getStore(), tester.getNS("system") + "891b8c1ac105485");
		assertEquals(4, result.size());
		assertTrue(result.containsKey(tester.getNS("system") + "TWAS-ExtrinsicTW-a9feda22"));
		assertTrue(result.containsKey(tester.getNS("system") + "TWAS-IntrinsicTW-a9feda22"));
		assertTrue(result.containsKey(tester.getNS("system") + "TWAS-ManagementTW-a9feda22"));
		assertTrue(result.containsKey(tester.getNS("system") + "TWAS-UserTW-a9feda22"));

		//by asset ID
		result = querier.getTrustworthinessAttributeSetsForAssetID(tester.getStore(), "a9feda22");
		assertEquals(4, result.size());
		assertTrue(result.containsKey(tester.getNS("system") + "TWAS-ExtrinsicTW-a9feda22"));
		assertTrue(result.containsKey(tester.getNS("system") + "TWAS-IntrinsicTW-a9feda22"));
		assertTrue(result.containsKey(tester.getNS("system") + "TWAS-ManagementTW-a9feda22"));
		assertTrue(result.containsKey(tester.getNS("system") + "TWAS-UserTW-a9feda22"));
	}

	@Test
	public void testGetTrustworthinessAttributeSetVisibility() {

		tester.switchModels(1, 5);

		//Visible
		String uri1 = tester.getNS("system") + "TWAS-UserTW-19c8a165";
		assertTrue(querier.getTrustworthinessAttributeSet(tester.getStore(), uri1).isVisible());

		//Not visible
		String uri2 = tester.getNS("system") + "TWAS-SecurityTW-19c8a165";
		assertFalse(querier.getTrustworthinessAttributeSet(tester.getStore(), uri2).isVisible());
	}

	@Test
	public void testGetTrustworthinessAttributeSetVisibilityNotInDomainModel() {

		tester.switchModels(0, 0);

		//TWA visibility not defined in domain model - defaults to visible
		String uri = tester.getNS("system") + "TWAS-ManagementTW-2d5019c7";
		assertTrue(querier.getTrustworthinessAttributeSet(tester.getStore(), uri).isVisible());
	}

	@Test
	public void testGetSecondaryEffectSteps() {

		tester.switchModels(0, 0);

		Set<SecondaryEffectStep> result = querier.getSecondaryEffectSteps(tester.getStore(), false);
		assertEquals(276, result.size());

		result = querier.getSecondaryEffectSteps(tester.getStore(), true);
		assertEquals(107, result.size());
	}

	@Test
	public void testGetUpdatedThreatStatus() {

		tester.switchModels(0, 0);

		String threatURI = tester.getNS("system") + "H.A.H.1-H_df878bda";
		String csURI = tester.getNS("system") + "CS-SoftwarePatching-df878bda";

		Map<String, Threat> threatMap = new HashMap<>();
		for (Threat t : querier.getSystemThreats(tester.getStore()).values()) {
			threatMap.put(t.getUri(), t);
			if (t.getUri().equals(threatURI)) {
				assertEquals(false, t.isResolved());
			}
		}
		
		// Get a subset of the control sets to NOT propose, to test update keeps them the same.
		Map<String, ControlSet> controlSets = querier.getControlSets(tester.getStore());
		int subsetLimit = 5;
		for (ControlSet cs : controlSets.values()) {
			if (!cs.getUri().equals(csURI)) {
				updater.updateControlSet(tester.getStore(), cs);
				subsetLimit--;
			}
			
			if (subsetLimit < 0) {
				break;
			}
		}
		
		// Propose and update control set.
		ControlSet updateCs = controlSets.get(csURI);
		logger.debug("Proposing {}", updateCs.getUri());
		updateCs.setProposed(true);
		updater.updateControlSet(tester.getStore(), updateCs);

		for (Threat t : querier.getUpdatedThreatStatus(tester.getStore(), querier.getSystemThreats(tester.getStore())).values()) {
			if (t.getUri().equals(threatURI)) {
				assertEquals(true, t.isResolved());
			}
		}

		controlSets = querier.getControlSets(tester.getStore());
		updateCs = controlSets.get(csURI);
		updateCs.setProposed(false);
		updater.updateControlSet(tester.getStore(), updateCs);

		for (Threat t : querier.getUpdatedThreatStatus(tester.getStore(), querier.getSystemThreats(tester.getStore())).values()) {
			if (t.getUri().equals(threatURI)) {
				assertEquals(false, t.isResolved());
			}
		}
	}

	@Test
	public void testGetComplianceSets() {

		//test with actual threats (SHiELD model)
		tester.switchModels(0, 0);

		//compliance threats
		Map<String, ComplianceThreat> result = querier.getComplianceThreats(tester.getStore());
		assertEquals(12, result.values().size());

		//compliance sets
		Map<String, ComplianceSet> compMap = querier.getComplianceSets(tester.getStore());
		assertEquals(5, compMap.values().size());
	}

	@Test
	public void testGetCreateDatePresent() {
		//Model contains metadata
		tester.switchModels(0, 0);
		assertEquals("2018/06/22 10:10:09", querier.getCreateDate(tester.getStore()));
	}

	@Test
	public void testGetCreateDateMissing() {
		//Model does not contain metadata
		tester.switchModels(0, 1);
		assertEquals(null, querier.getCreateDate(tester.getStore()));
	}

	@Test
	public void testGetMetadataOnEntity() {
		tester.switchModels(0, 2);

		Asset asset1 = querier.getSystemAssetById(tester.getStore(), "66004713");
		Asset asset2 = querier.getSystemAssetById(tester.getStore(), "ece77c11");

		MetadataPair p1 = new MetadataPair("exampleKey1", "exampleValue1");
		MetadataPair p2 = new MetadataPair("exampleKey1", "exampleValue2");
		MetadataPair p3 = new MetadataPair("exampleKey2", "exampleValue1");
		MetadataPair p4 = new MetadataPair("exampleKey2", "exampleValue2");

		assertThat(querier.getMetadataOnEntity(tester.getStore(), asset1))
			.containsExactlyInAnyOrder(p1, p3, p4);

		assertThat(querier.getMetadataOnEntity(tester.getStore(), asset2))
			.containsExactlyInAnyOrder(p2, p4);
	}

	@Test
	public void testGetSystemAssetsByMetadataSingleKeySingleValue() {
		tester.switchModels(0, 2);

		List<MetadataPair> query = new ArrayList<>();
		query.add(new MetadataPair("exampleKey2", "exampleValue2"));

		assertThat(getAssetsByMetadata(query)).containsExactlyInAnyOrder("66004713", "ece77c11");
	}

	@Test
	public void testGetSystemAssetsByMetadataSingleKeyMultipleValues() {
		tester.switchModels(0, 2);

		List<MetadataPair> query = new ArrayList<>();
		query.add(new MetadataPair("exampleKey2", "exampleValue1"));
		query.add(new MetadataPair("exampleKey2", "exampleValue2"));

		assertThat(getAssetsByMetadata(query)).containsExactlyInAnyOrder("66004713", "ece77c11");
	}

	@Test
	public void testGetSystemAssetsByMetadataMultipleKeysMultipleValues() {
		tester.switchModels(0, 2);

		List<MetadataPair> query = new ArrayList<>();
		query.add(new MetadataPair("exampleKey1", "exampleValue1"));
		query.add(new MetadataPair("exampleKey1", "exampleValue2"));
		query.add(new MetadataPair("exampleKey2", "exampleValue1"));

		assertThat(getAssetsByMetadata(query)).containsExactly("66004713");
	}

	@Test
	public void testGetSystemAssetsByMetadataNullList() {
		tester.switchModels(0, 2);

		assertThatIllegalArgumentException()
			.isThrownBy(
				() -> querier.getSystemAssetsByMetadata(tester.getStore(), null)
			)
			.withMessage(
				"MetadataPair list cannot be null"
			);
	}

	@Test
	public void testGetSystemAssetsByMetadataEmptyList() {
		tester.switchModels(0, 2);

		List<MetadataPair> query = new ArrayList<>();

		assertThat(querier.getSystemAssetsByMetadata(tester.getStore(), query)).hasSize(83);
	}

	@Test
	public void testGetSystemAssetsByMetadataDuplicateQuery() {
		tester.switchModels(0, 2);

		List<MetadataPair> query = new ArrayList<>();
		query.add(new MetadataPair("exampleKey2", "exampleValue2"));
		query.add(new MetadataPair("exampleKey2", "exampleValue2"));

		assertThat(getAssetsByMetadata(query)).containsExactlyInAnyOrder("66004713", "ece77c11");
	}

	@Test
	public void testGetSystemAssetsByMetadataNoMatches() {
		tester.switchModels(0, 2);

		List<MetadataPair> query = new ArrayList<>();
		query.add(new MetadataPair("exampleKey3", "exampleValue3"));

		assertThat(querier.getSystemAssetsByMetadata(tester.getStore(), query)).isEmpty();
	}

	private List<String> getAssetsByMetadata(List<MetadataPair> query) {
		return querier
			.getSystemAssetsByMetadata(tester.getStore(), query)
			.values()
			.stream()
			.map(Asset::getID)
			.collect(toList());
	}

	@Test
	public void testGetAssetGroups() {
		tester.switchModels(0, 6);

		Map<String, AssetGroup> assetGroups = querier.getAssetGroups(tester.getStore());
		assertEquals(5, assetGroups.size());

		AssetGroup assetGroup1 = assetGroups.get(
				"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3dda/ui#Group-a3a3a3a3");
		assertNotNull(assetGroup1);
		assertEquals(3, assetGroup1.getAssets().size());
		assertEquals(10, assetGroup1.getX());
		assertEquals(20, assetGroup1.getY());
		assertEquals(400, assetGroup1.getWidth()); //default value, as not defined
		assertEquals(400, assetGroup1.getHeight()); //default value, as not defined
		assertTrue(assetGroup1.isExpanded()); //default value, as not defined
		assertEquals("GroupLabel", assetGroup1.getLabel());
		assertEquals("6fa82559", assetGroup1.getID());

		AssetGroup assetGroup2 = assetGroups.get(
				"http://it-innovation.soton.ac.uk/system/5b15202b567d9478125b3dda/ui#Group-a0a0a0a0");
		assertNotNull(assetGroup2);
		assertEquals(0, assetGroup2.getAssets().size());
		assertEquals(0, assetGroup2.getX());
		assertEquals(0, assetGroup2.getY());
		assertEquals(400, assetGroup2.getWidth()); //default value, as not defined
		assertEquals(400, assetGroup2.getHeight()); //default value, as not defined
		assertTrue(assetGroup2.isExpanded()); //default value, as not defined
		assertEquals("Group-a0a0a0a0", assetGroup2.getLabel());
		assertEquals("d0cb21cd", assetGroup2.getID());
	}

	@Test
	public void testGetAssetGroupById() {
		tester.switchModels(0, 6);

		Map<String, Asset> assets = querier.getSystemAssets(tester.getStore());
		AssetGroup assetGroup1 = querier.getAssetGroupById(tester.getStore(), "6fa82559", assets);
		assertNotNull(assetGroup1);
		assertEquals(3, assetGroup1.getAssets().size());
		assertEquals(10, assetGroup1.getX());
		assertEquals(20, assetGroup1.getY());
		assertEquals("GroupLabel", assetGroup1.getLabel());
		assertEquals("6fa82559", assetGroup1.getID());

		assertNull(querier.getAssetGroupById(tester.getStore(), "incorrect", assets));
	}

	@Test
	public void testGetAssetGroupOfAsset() {
		tester.switchModels(0, 6);

		Asset asset = querier.getSystemAssetById(tester.getStore(),"2d5019c7");
		AssetGroup assetGroup = querier.getAssetGroupOfAsset(tester.getStore(), asset);

		assertEquals("6fa82559", assetGroup.getID());

		asset = querier.getSystemAssetById(tester.getStore(),"30502f2a");
		assetGroup = querier.getAssetGroupOfAsset(tester.getStore(), asset);
		assertNull(assetGroup);
	}
}
