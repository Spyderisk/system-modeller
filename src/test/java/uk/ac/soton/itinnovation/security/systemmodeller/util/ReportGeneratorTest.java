/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2020
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
//  Created By :            Lee Mason
//  Created Date :          08/06/2020
//  Created for Project :   ZDMP
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collection;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.soton.itinnovation.security.model.Level;
import uk.ac.soton.itinnovation.security.model.SemanticEntity;
import uk.ac.soton.itinnovation.security.model.domain.ControlStrategy.ControlStrategyType;
import uk.ac.soton.itinnovation.security.model.system.Asset;
import uk.ac.soton.itinnovation.security.model.system.ControlSet;
import uk.ac.soton.itinnovation.security.model.system.ControlStrategy;
import uk.ac.soton.itinnovation.security.model.system.MisbehaviourSet;
import uk.ac.soton.itinnovation.security.model.system.Relation;
import uk.ac.soton.itinnovation.security.model.system.Threat;
import uk.ac.soton.itinnovation.security.model.system.TrustworthinessAttributeSet;
import uk.ac.soton.itinnovation.security.systemmodeller.CommonTestSetup;
import uk.ac.soton.itinnovation.security.systemmodeller.SystemModellerApplication;
import uk.ac.soton.itinnovation.security.systemmodeller.TestHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.model.ModelFactory;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SystemModellerApplication.class)
public class ReportGeneratorTest extends CommonTestSetup {

    private static final Logger logger = LoggerFactory.getLogger(ReportGeneratorTest.class);

    //System objects for model access
    @Autowired
    private ModelObjectsHelper modelHelper;

    @Autowired
    private ModelFactory modelFactory;

    //Provides model control and access
    private static TestHelper testHelper;

    private static final String DOMAIN_URI_PREFIX = "domain#";
    private static final String SYSTEM_URI_PREFIX = "system#";

    private static final String ASSET_1_URI = SYSTEM_URI_PREFIX + "Asset1";
    private static final String ASSET_1_LABEL = "Asset1";
    private static final String ASSET_1_TYPE_URI = DOMAIN_URI_PREFIX + "Asset1Type";
    private static final String ASSET_1_TYPE_LABEL = "Asset1Type";

    private static final String ASSET_2_URI = SYSTEM_URI_PREFIX + "Asset2";
    private static final String ASSET_2_LABEL = "Asset2";
    private static final String ASSET_2_TYPE_URI = DOMAIN_URI_PREFIX + "Asset2Type";
    private static final String ASSET_2_TYPE_LABEL = "Asset2Type";

    private static final String ASSET_3_URI = SYSTEM_URI_PREFIX + "Asset3";
    private static final String ASSET_3_LABEL = "Asset3";
    private static final String ASSET_3_TYPE_URI = DOMAIN_URI_PREFIX + "Asset3Type";
    private static final String ASSET_3_TYPE_LABEL = "Asset3Type";

    private static final String RELATION_1_TYPE_URI = DOMAIN_URI_PREFIX + "Relation1Type";
    private static final String RELATION_1_TYPE_LABEL = "Relation1Type";

    private static final String RELATION_2_TYPE_URI = DOMAIN_URI_PREFIX + "Relation2Type";
    private static final String RELATION_2_TYPE_LABEL = "Relation2Type";

    private static final String IMPACT_LEVEL_LOW_URI = "domain#ImpactLevelLow";
    private static final String IMPACT_LEVEL_HIGH_URI = "domain#ImpactLevelHigh";
    private static final String LIKELIHOOD_LEVEL_LOW_URI = "domain#LikelihoodLow";
    private static final String LIKELIHOOD_LEVEL_HIGH_URI = "domain#LikelihoodHigh";
    private static final String RISK_LEVEL_LOW_URI = "domain#RiskLevelLow";
    private static final String RISK_LEVEL_HIGH_URI = "domain#RiskLevelHigh";
    private static final String TW_LEVEL_LOW_URI = "domain#TrustworthinessLevelLow";
    private static final String TW_LEVEL_HIGH_URI = "domain#TrustworthinessLevelHigh";
    private static final String LEVEL_LOW_LABEL = "Low";
    private static final String LEVEL_HIGH_LABEL = "High";

    private static final String MISBEHAVIOUR_1_LABEL = "Misbehaviour1";
    private static final String MISBEHAVIOUR_2_LABEL = "Misbehaviour2";

    private static final String MISBEHAVIOUR_SET_1_URI = SYSTEM_URI_PREFIX + "MisbehaviourSet1";
    private static final String MISBEHAVIOUR_SET_2_URI = SYSTEM_URI_PREFIX + "MisbehaviourSet2";

    private static final String ATTRIBUTE_1_LABEL = "Attribute1";
    private static final String ATTRIBUTE_2_LABEL = "Attribute2";

    private static final String TWAS_1_URI = SYSTEM_URI_PREFIX + "TrustworthinessAttributeSet1";
    private static final String TWAS_2_URI = SYSTEM_URI_PREFIX + "TrustworthinessAttributeSet2";

    private static final String CONTROL_SET_1_LABEL = "ControlSet1";
    private static final String CONTROL_SET_1_URI = SYSTEM_URI_PREFIX + CONTROL_SET_1_LABEL;

    private static final String CONTROL_SET_2_LABEL = "ControlSet2";
    private static final String CONTROL_SET_2_URI = SYSTEM_URI_PREFIX + CONTROL_SET_2_LABEL;

    private static final String CONTROL_STRATEGY_1_LABEL = "ControlStrategy1";
    private static final String CONTROL_STRATEGY_1_URI = SYSTEM_URI_PREFIX + CONTROL_STRATEGY_1_LABEL;
    private static final String CONTROL_STRATEGY_1_DESCRIPTION = "Control strategy 1 description.";

    private static final String CONTROL_STRATEGY_2_LABEL = "ControlStrategy2";
    private static final String CONTROL_STRATEGY_2_URI = SYSTEM_URI_PREFIX + CONTROL_STRATEGY_2_LABEL;
    private static final String CONTROL_STRATEGY_2_DESCRIPTION = "Control strategy 2 description.";

    private static final String CONTROL_STRATEGY_3_LABEL = "ControlStrategy3";
    private static final String CONTROL_STRATEGY_3_URI = SYSTEM_URI_PREFIX + CONTROL_STRATEGY_3_LABEL;
    private static final String CONTROL_STRATEGY_3_DESCRIPTION = "Control strategy 3 description.";

    private static final String THREAT_1_URI = SYSTEM_URI_PREFIX + "Threat1";
    private static final String THREAT_1_DESCRIPTION = "Threat 1 description.";

    private static final String THREAT_2_URI = SYSTEM_URI_PREFIX + "Threat2";
    private static final String THREAT_2_DESCRIPTION = "Threat 2 description.";

    private static final String THREAT_3_URI = SYSTEM_URI_PREFIX + "Threat3";
    private static final String THREAT_3_DESCRIPTION = "Threat 3 description.";

    private static final String ACCEPTANCE_JUSTIFICATION = "Acceptance justification.";

    @BeforeClass
    public static void beforeClass() {
        logger.info("Setting up for ReportGeneratorTest class");

        testHelper = new TestHelper("jena-tdb");
        testHelper.setUp();
    }

    @Test
    public void testGenerateAssetsJson() {
        ReportGenerator reportGenerator = new ReportGenerator();

        // We don't have a real domain model so we must "cheat" here
        reportGenerator.getDomainAssetLabels().put(ASSET_1_TYPE_URI, ASSET_1_TYPE_LABEL);
        reportGenerator.getDomainAssetLabels().put(ASSET_2_TYPE_URI, ASSET_2_TYPE_LABEL);
        reportGenerator.getDomainAssetLabels().put(ASSET_3_TYPE_URI, ASSET_3_TYPE_LABEL);

        ControlSet cs1 = new ControlSet();
        cs1.setUri(CONTROL_SET_1_URI);

        ControlSet cs2= new ControlSet();
        cs2.setUri(CONTROL_SET_2_URI);

        MisbehaviourSet ms1 = new MisbehaviourSet();
        ms1.setUri(MISBEHAVIOUR_SET_1_URI);

        MisbehaviourSet ms2 = new MisbehaviourSet();
        ms2.setUri(MISBEHAVIOUR_SET_2_URI);

        TrustworthinessAttributeSet twas1 = new TrustworthinessAttributeSet();
        twas1.setUri(TWAS_1_URI);

        TrustworthinessAttributeSet twas2 = new TrustworthinessAttributeSet();
        twas2.setUri(TWAS_2_URI);

        Asset asset1 = new Asset();
        asset1.setType(ASSET_1_TYPE_URI);
        asset1.setUri(ASSET_1_URI);
        asset1.setLabel(ASSET_1_LABEL);
        asset1.setAsserted(false);
        asset1.getMisbehaviourSets().put(ms1.getUri(), ms1);
        asset1.getTrustworthinessAttributeSets().put(twas1.getUri(), twas1);
        asset1.getTrustworthinessAttributeSets().put(twas2.getUri(), twas2);

        Asset asset2 = new Asset();
        asset2.setType(ASSET_2_TYPE_URI);
        asset2.setUri(ASSET_2_URI);
        asset2.setLabel(ASSET_2_LABEL);
        asset2.setAsserted(true);
        asset2.getControlSets().put(cs1.getUri(), cs1);
        asset2.getControlSets().put(cs2.getUri(), cs2);
        asset2.getTrustworthinessAttributeSets().put(twas2.getUri(), twas2);

        Asset asset3 = new Asset();
        asset3.setType(ASSET_3_TYPE_URI);
        asset3.setUri(ASSET_3_URI);
        asset3.setLabel(ASSET_3_LABEL);
        asset3.setAsserted(false);
        asset3.getControlSets().put(cs1.getUri(), cs1);
        asset3.getMisbehaviourSets().put(ms1.getUri(), ms1);
        asset3.getMisbehaviourSets().put(ms2.getUri(), ms2);

        Collection<Asset> assets = new ArrayList<>();
        assets.add(asset1);
        assets.add(asset2);
        assets.add(asset3);

        String expectedJson =
            "{\n" +
            "  \"" + asset1.getUri() + "\": {\n" +
            "    \"label\": \"" + ASSET_1_LABEL + "\",\n" +
            "    \"type\": \"" + ASSET_1_TYPE_URI + "\",\n" +
            "    \"typeLabel\": \"" + ASSET_1_TYPE_LABEL + "\",\n" +
            "    \"controls\": [\n" +
            "    ],\n" +
            "    \"misbehaviours\": [\n" +
            "      \"" + ms1.getUri() + "\"\n" +
            "    ],\n" +
            "    \"trustworthinessAttributes\": [\n" +
            "      \"" + twas1.getUri() + "\",\n" +
            "      \"" + twas2.getUri() + "\"\n" +
            "    ]\n" +
            "  },\n" +
            "  \"" + asset2.getUri() + "\": {\n" +
            "    \"label\": \"" + ASSET_2_LABEL + "\",\n" +
            "    \"type\": \"" + ASSET_2_TYPE_URI + "\",\n" +
            "    \"typeLabel\": \"" + ASSET_2_TYPE_LABEL + "\",\n" +
            "    \"controls\": [\n" +
            "      \"" + cs1.getUri() + "\",\n" +
            "      \"" + cs2.getUri() + "\"\n" +
            "    ],\n" +
            "    \"misbehaviours\": [\n" +
            "    ],\n" +
            "    \"trustworthinessAttributes\": [\n" +
            "      \"" + twas2.getUri() + "\"\n" +
            "    ]\n" +
            "  },\n" +
            "  \"" + asset3.getUri() + "\": {\n" +
            "    \"label\": \"" + ASSET_3_LABEL + "\",\n" +
            "    \"type\": \"" + ASSET_3_TYPE_URI + "\",\n" +
            "    \"typeLabel\": \"" + ASSET_3_TYPE_LABEL + "\",\n" +
            "    \"controls\": [\n" +
            "      \"" + cs1.getUri() + "\"\n" +
            "    ],\n" +
            "    \"misbehaviours\": [\n" +
            "      \"" + ms1.getUri() + "\",\n" +
            "      \"" + ms2.getUri() + "\"\n" +
            "    ],\n" +
            "    \"trustworthinessAttributes\": [\n" +
            "    ]\n" +
            "  }\n" +
            "}";
        logger.debug("Expected JSON:\n{}", expectedJson);

        JsonObject result = reportGenerator.generateAssetsJson(assets);

        assertThat(result, is(JsonParser.parseString(expectedJson)));
    }

    @Test
    public void testGenerateRelationsJson() {
        ReportGenerator reportGenerator = new ReportGenerator();

        Asset asset1 = new Asset();
        asset1.setUri(ASSET_1_URI);

        Asset asset2 = new Asset();
        asset2.setUri(ASSET_2_URI);

        Relation relation1 = new Relation();
        relation1.setFrom(ASSET_1_URI);
        relation1.setTo(ASSET_2_URI);
        relation1.setType(RELATION_1_TYPE_URI);
        relation1.setLabel(RELATION_1_TYPE_LABEL);

        Relation relation2 = new Relation();
        relation2.setFrom(ASSET_2_URI);
        relation2.setTo(ASSET_1_URI);
        relation2.setType(RELATION_2_TYPE_URI);
        relation2.setLabel(RELATION_2_TYPE_LABEL);

        Collection<Relation> relations = new ArrayList<>();
        relations.add(relation1);
        relations.add(relation2);

        String expectedJson =
            "{\n" +
            "  \"" + relation1.getID() + "\": {\n" +
            "    \"from\": \"" + asset1.getUri() + "\",\n" +
            "    \"to\": \"" + asset2.getUri() + "\",\n" +
            "    \"type\": \"" + RELATION_1_TYPE_URI + "\"\n" +
            "  },\n" +
            "  \"" + relation2.getID() + "\": {\n" +
            "    \"from\": \"" + asset2.getUri() + "\",\n" +
            "    \"to\": \"" + asset1.getUri() + "\",\n" +
            "    \"type\": \"" + RELATION_2_TYPE_URI + "\"\n" +
            "  }\n" +
            "}";
        logger.debug("Expected JSON:\n{}", expectedJson);

        JsonObject result = reportGenerator.generateRelationsJson(relations);

        assertThat(result, is(JsonParser.parseString(expectedJson)));
    }

    @Test
    public void testGenerateThreatsJson() {
        ReportGenerator reportGenerator = new ReportGenerator();

        ControlSet cs1 = new ControlSet();
        cs1.setUri(CONTROL_SET_1_URI);
        cs1.setProposed(false);

        ControlSet cs2 = new ControlSet();
        cs2.setUri(CONTROL_SET_2_URI);
        cs2.setProposed(true);

        ControlStrategy csg1 = new ControlStrategy();
        csg1.setUri(CONTROL_STRATEGY_1_URI);
        csg1.getMandatoryControlSets().put(cs1.getUri(), cs1);

        ControlStrategy csg2 = new ControlStrategy();
        csg2.setUri(CONTROL_STRATEGY_2_URI);
        csg2.getMandatoryControlSets().put(cs2.getUri(), cs2);

        MisbehaviourSet ms1 = new MisbehaviourSet();
        ms1.setUri(MISBEHAVIOUR_SET_1_URI);

        MisbehaviourSet ms2 = new MisbehaviourSet();
        ms2.setUri(MISBEHAVIOUR_SET_2_URI);

        Level likelihoodLevelLow = new Level();
        likelihoodLevelLow.setUri(LIKELIHOOD_LEVEL_LOW_URI);
        likelihoodLevelLow.setLabel(LEVEL_LOW_LABEL);

        Level riskLevelLow = new Level();
        riskLevelLow.setUri(RISK_LEVEL_LOW_URI);
        riskLevelLow.setLabel(LEVEL_LOW_LABEL);

        Level likelihoodLevelHigh = new Level();
        likelihoodLevelHigh.setUri(LIKELIHOOD_LEVEL_HIGH_URI);
        likelihoodLevelHigh.setLabel(LEVEL_HIGH_LABEL);

        Level riskLevelHigh = new Level();
        riskLevelHigh.setUri(RISK_LEVEL_HIGH_URI);
        riskLevelHigh.setLabel(LEVEL_HIGH_LABEL);

        Threat threat1 = new Threat();
        threat1.setUri(THREAT_1_URI);
        threat1.setDescription(THREAT_1_DESCRIPTION);
        threat1.getControlStrategies().put(csg1.getUri(), csg1);
        threat1.getControlStrategies().put(csg2.getUri(), csg2);
        threat1.getMisbehaviours().put(ms1.getUri(), ms1);
        threat1.setLikelihood(likelihoodLevelLow);
        threat1.setRiskLevel(riskLevelHigh);

        Threat threat2 = new Threat();
        threat2.setUri(THREAT_2_URI);
        threat2.setDescription(THREAT_2_DESCRIPTION);
        threat2.setAcceptanceJustification(ACCEPTANCE_JUSTIFICATION);
        threat2.getMisbehaviours().put(ms1.getUri(), ms1);
        threat2.getMisbehaviours().put(ms2.getUri(), ms2);

        Threat threat3 = new Threat();
        threat3.setUri(THREAT_3_URI);
        threat3.setDescription(THREAT_3_DESCRIPTION);
        threat3.getControlStrategies().put(csg1.getUri(), csg1);
        threat3.setLikelihood(likelihoodLevelHigh);
        threat3.setRiskLevel(riskLevelLow);

        Collection<Threat> threats = new ArrayList<>();
        threats.add(threat1);
        threats.add(threat2);
        threats.add(threat3);

        String expectedJson =
            "{\n" +
            "  \"" + threat1.getUri() + "\": {\n" +
            "    \"description\": \"" + THREAT_1_DESCRIPTION + "\",\n" +
            "    \"resolved\": true,\n" +
            "    \"controlStrategies\": [\n" +
            "      \"" + csg1.getUri() + "\",\n" +
            "      \"" + csg2.getUri() + "\"\n" +
            "    ],\n" +
            "    \"misbehaviours\": [\n" +
            "      \"" + ms1.getUri() + "\"\n" +
            "    ],\n" +
            "    \"likelihoodLabel\": \"" + LEVEL_LOW_LABEL + "\",\n" +
            "    \"likelihood\": \"" + LIKELIHOOD_LEVEL_LOW_URI + "\",\n" +
            "    \"riskLabel\": \"" + LEVEL_HIGH_LABEL + "\",\n" +
            "    \"risk\": \"" + RISK_LEVEL_HIGH_URI + "\",\n" +
            "    \"acceptanceJustification\": null\n" +
            "  },\n" +
            "  \"" + threat2.getUri() + "\": {\n" +
            "    \"description\": \"" + THREAT_2_DESCRIPTION + "\",\n" +
            "    \"resolved\": true,\n" +
            "    \"controlStrategies\": [\n" +
            "    ],\n" +
            "    \"misbehaviours\": [\n" +
            "      \"" + ms1.getUri() + "\",\n" +
            "      \"" + ms2.getUri() + "\"\n" +
            "    ],\n" +
            "    \"likelihoodLabel\": null,\n" +
            "    \"likelihood\": null,\n" +
            "    \"riskLabel\": null,\n" +
            "    \"risk\": null,\n" +
            "    \"acceptanceJustification\": \"" + ACCEPTANCE_JUSTIFICATION + "\"\n" +
            "  },\n" +
            "  \"" + threat3.getUri() + "\": {\n" +
            "    \"description\": \"" + THREAT_3_DESCRIPTION + "\",\n" +
            "    \"resolved\": false,\n" +
            "    \"controlStrategies\": [\n" +
            "      \"" + csg1.getUri() + "\"\n" +
            "    ],\n" +
            "    \"misbehaviours\": [\n" +
            "    ],\n" +
            "    \"likelihoodLabel\": \"" + LEVEL_HIGH_LABEL + "\",\n" +
            "    \"likelihood\": \"" + LIKELIHOOD_LEVEL_HIGH_URI + "\",\n" +
            "    \"riskLabel\": \"" + LEVEL_LOW_LABEL + "\",\n" +
            "    \"risk\": \"" + RISK_LEVEL_LOW_URI + "\",\n" +
            "    \"acceptanceJustification\": null\n" +
            "  }\n" +
            "}";
        logger.debug("Expected JSON:\n{}", expectedJson);

        JsonObject result = reportGenerator.generateThreatsJson(threats);

        assertThat(result, is(JsonParser.parseString(expectedJson)));
    }

    @Test
    public void testGenerateMisbehaviourSets() {
        ReportGenerator reportGenerator = new ReportGenerator();

        Asset asset1 = new Asset();
        asset1.setUri(ASSET_1_URI);

        Asset asset2 = new Asset();
        asset2.setUri(ASSET_2_URI);

        Level impactLevelLow = new Level();
        impactLevelLow.setUri(IMPACT_LEVEL_LOW_URI);
        impactLevelLow.setLabel(LEVEL_LOW_LABEL);

        Level likelihoodLevelHigh = new Level();
        likelihoodLevelHigh.setUri(LIKELIHOOD_LEVEL_HIGH_URI);
        likelihoodLevelHigh.setLabel(LEVEL_HIGH_LABEL);

        Level riskLevelHigh = new Level();
        riskLevelHigh.setUri(RISK_LEVEL_HIGH_URI);
        riskLevelHigh.setLabel(LEVEL_HIGH_LABEL);

        MisbehaviourSet ms1 = new MisbehaviourSet();
        ms1.setUri(MISBEHAVIOUR_SET_1_URI);
        ms1.setMisbehaviourLabel(MISBEHAVIOUR_1_LABEL);
        ms1.setAsset(ASSET_1_URI);
        ms1.setImpactLevel(impactLevelLow);
        ms1.setLikelihood(likelihoodLevelHigh);
        ms1.setRiskLevel(riskLevelHigh);

        MisbehaviourSet ms2 = new MisbehaviourSet();
        ms2.setUri(MISBEHAVIOUR_SET_2_URI);
        ms2.setMisbehaviourLabel(MISBEHAVIOUR_2_LABEL);
        ms2.setAsset(ASSET_2_URI);

        Collection<MisbehaviourSet> misbehaviourSets = new ArrayList<>();
        misbehaviourSets.add(ms1);
        misbehaviourSets.add(ms2);

        String expectedJson =
            "{\n" +
            "  \"" + ms1.getUri() + "\": {\n" +
            "    \"label\": \"" + MISBEHAVIOUR_1_LABEL + "\",\n" +
            "    \"asset\": \"" + asset1.getUri() + "\",\n" +
            "    \"impactLabel\": \"" + LEVEL_LOW_LABEL + "\",\n" +
            "    \"impact\": \"" + IMPACT_LEVEL_LOW_URI + "\",\n" +
            "    \"likelihoodLabel\": \"" + LEVEL_HIGH_LABEL + "\",\n" +
            "    \"likelihood\": \"" + LIKELIHOOD_LEVEL_HIGH_URI + "\",\n" +
            "    \"riskLabel\": \"" + LEVEL_HIGH_LABEL + "\",\n" +
            "    \"risk\": \"" + RISK_LEVEL_HIGH_URI + "\"\n" +
            "  },\n" +
            "  \"" + ms2.getUri() + "\": {\n" +
            "    \"label\": \"" + MISBEHAVIOUR_2_LABEL + "\",\n" +
            "    \"asset\": \"" + asset2.getUri() + "\",\n" +
            "    \"impactLabel\": null,\n" +
            "    \"impact\": null,\n" +
            "    \"likelihoodLabel\": null,\n" +
            "    \"likelihood\": null,\n" +
            "    \"riskLabel\": null,\n" +
            "    \"risk\": null\n" +
            "  }\n" +
            "}";
        logger.debug("Expected JSON:\n{}", expectedJson);

        JsonObject result = reportGenerator.generateMisbehaviourSetsJson(misbehaviourSets);

        assertThat(result, is(JsonParser.parseString(expectedJson)));
    }

    @Test
    public void testGenerateTrustworthinessAttributeSets() {
        ReportGenerator reportGenerator = new ReportGenerator();

        Asset asset1 = new Asset();
        asset1.setUri(ASSET_1_URI);

        Asset asset2 = new Asset();
        asset2.setUri(ASSET_2_URI);

        SemanticEntity attribute1 = new SemanticEntity();
        attribute1.setUri(TWAS_1_URI);
        attribute1.setLabel(ATTRIBUTE_1_LABEL);

        SemanticEntity attribute2 = new SemanticEntity();
        attribute2.setUri(TWAS_2_URI);
        attribute2.setLabel(ATTRIBUTE_2_LABEL);

        Level twLevelLow = new Level();
        twLevelLow.setUri(TW_LEVEL_LOW_URI);
        twLevelLow.setLabel(LEVEL_LOW_LABEL);

        Level twLevelHigh = new Level();
        twLevelHigh.setUri(TW_LEVEL_HIGH_URI);
        twLevelHigh.setLabel(LEVEL_HIGH_LABEL);

        TrustworthinessAttributeSet twas1 = new TrustworthinessAttributeSet();
        twas1.setUri(TWAS_1_URI);
        twas1.setAsset(ASSET_1_URI);
        twas1.setAttribute(attribute1);
        twas1.setAssertedTWLevel(twLevelLow);
        twas1.setInferredTWLevel(twLevelHigh);

        TrustworthinessAttributeSet twas2 = new TrustworthinessAttributeSet();
        twas2.setUri(TWAS_2_URI);
        twas2.setAsset(ASSET_2_URI);
        twas2.setAttribute(attribute2);

        Collection<TrustworthinessAttributeSet> trustworthinessAttributeSets = new ArrayList<>();
        trustworthinessAttributeSets.add(twas1);
        trustworthinessAttributeSets.add(twas2);

        String expectedJson =
            "{\n" +
            "  \"" + twas1.getUri() + "\": {\n" +
            "    \"attribute\": \"" + TWAS_1_URI + "\",\n" +
            "    \"attributeLabel\": \"" + ATTRIBUTE_1_LABEL + "\",\n" +
            "    \"asset\": \"" + asset1.getUri() + "\",\n" +
            "    \"assumedTWLabel\": \"" + LEVEL_LOW_LABEL + "\",\n" +
            "    \"assumedTW\": \"" + TW_LEVEL_LOW_URI + "\",\n" +
            "    \"calculatedTWLabel\": \"" + LEVEL_HIGH_LABEL + "\",\n" +
            "    \"calculatedTW\": \"" + TW_LEVEL_HIGH_URI + "\"\n" +
            "  },\n" +
            "  \"" + twas2.getUri() + "\": {\n" +
            "    \"attribute\": \"" + TWAS_2_URI + "\",\n" +
            "    \"attributeLabel\": \"" + ATTRIBUTE_2_LABEL + "\",\n" +
            "    \"asset\": \"" + asset2.getUri() + "\",\n" +
            "    \"assumedTWLabel\": null,\n" +
            "    \"assumedTW\": null,\n" +
            "    \"calculatedTWLabel\": null,\n" +
            "    \"calculatedTW\": null\n" +
            "  }\n" +
            "}";
        logger.debug("Expected JSON:\n{}", expectedJson);

        JsonObject result = reportGenerator.generateTrustworthinessAttributeSetsJson(trustworthinessAttributeSets);

        assertThat(result, is(JsonParser.parseString(expectedJson)));
    }

    @Test
    public void testGenerateControlSets() {
        ReportGenerator reportGenerator = new ReportGenerator();

        Asset asset1 = new Asset();
        asset1.setUri(ASSET_1_URI);

        Asset asset2 = new Asset();
        asset2.setUri(ASSET_2_URI);

        ControlSet cs1 = new ControlSet();
        cs1.setUri(CONTROL_SET_1_URI);
        cs1.setLabel(CONTROL_SET_1_LABEL);
        cs1.setAssetUri(asset1.getUri());
        cs1.setAssertable(true);
        cs1.setProposed(false);
        cs1.setWorkInProgress(false);

        ControlSet cs2 = new ControlSet();
        cs2.setUri(CONTROL_SET_2_URI);
        cs2.setLabel(CONTROL_SET_2_LABEL);
        cs2.setAssetUri(asset2.getUri());
        cs2.setAssertable(false);
        cs2.setProposed(true);
        cs2.setWorkInProgress(true);

        Collection<ControlSet> controlSets = new ArrayList<>();
        controlSets.add(cs1);
        controlSets.add(cs2);

        String expectedJson =
            "{\n" +
            "  \"" + cs1.getUri() + "\": {\n" +
            "    \"label\": \"" + CONTROL_SET_1_LABEL + "\",\n" +
            "    \"asset\": \"" + asset1.getUri() + "\",\n" +
            "    \"assertable\": true,\n" +
            "    \"proposed\": false,\n" +
            "    \"workInProgress\": false\n" +
            "  },\n" +
            "  \"" + cs2.getUri() + "\": {\n" +
            "    \"label\": \"" + CONTROL_SET_2_LABEL + "\",\n" +
            "    \"asset\": \"" + asset2.getUri() + "\",\n" +
            "    \"assertable\": false,\n" +
            "    \"proposed\": true,\n" +
            "    \"workInProgress\": true\n" +
            "  }\n" +
            "}";
        logger.debug("Expected JSON:\n{}", expectedJson);

        JsonObject result = reportGenerator.generateControlSetsJson(controlSets);

        assertThat(result, is(JsonParser.parseString(expectedJson)));
    }

    @Test
    public void testGenerateControlStrategies() {
        ReportGenerator reportGenerator = new ReportGenerator();

        ControlSet cs1 = new ControlSet();
        cs1.setUri(CONTROL_SET_1_URI);
        cs1.setProposed(true);

        ControlSet cs2 = new ControlSet();
        cs2.setUri(CONTROL_SET_2_URI);
        cs2.setProposed(false);

        Threat threat1 = new Threat();
        threat1.setUri(THREAT_1_URI);

        Level levelLow = new Level();
        levelLow.setUri(TW_LEVEL_LOW_URI);
        levelLow.setLabel(LEVEL_LOW_LABEL);

        Level levelHigh = new Level();
        levelHigh.setUri(TW_LEVEL_HIGH_URI);
        levelHigh.setLabel(LEVEL_HIGH_LABEL);

        ControlStrategy csg1 = new ControlStrategy();
        csg1.setUri(CONTROL_STRATEGY_1_URI);
        csg1.setDescription(CONTROL_STRATEGY_1_DESCRIPTION);
        //csg1.setThreat(THREAT_1_URI);
        csg1.getThreatCsgTypes().put(THREAT_1_URI, ControlStrategyType.BLOCK); //TODO: check this is correct
        csg1.setLabel(CONTROL_STRATEGY_1_LABEL);
        csg1.setBlockingEffect(levelLow);
        csg1.getMandatoryControlSets().put(cs1.getUri(), cs1);
        csg1.getMandatoryControlSets().put(cs2.getUri(), cs2);

        ControlStrategy csg2 = new ControlStrategy();
        csg2.setUri(CONTROL_STRATEGY_2_URI);
        csg2.setDescription(CONTROL_STRATEGY_2_DESCRIPTION);
        //csg2.setThreat(THREAT_1_URI);
        csg2.getThreatCsgTypes().put(THREAT_1_URI, ControlStrategyType.BLOCK); //TODO: check this is correct
        csg2.setLabel(CONTROL_STRATEGY_2_LABEL);
        csg2.setBlockingEffect(levelHigh);
        csg2.getMandatoryControlSets().put(cs1.getUri(), cs1);

        ControlStrategy csg3 = new ControlStrategy();
        csg3.setUri(CONTROL_STRATEGY_3_URI);
        csg3.setDescription(CONTROL_STRATEGY_3_DESCRIPTION);
        //csg3.setThreat(THREAT_1_URI);
        csg3.getThreatCsgTypes().put(THREAT_1_URI, ControlStrategyType.BLOCK); //TODO: check this is correct
        csg3.setLabel(CONTROL_STRATEGY_3_LABEL);
        csg3.setBlockingEffect(levelHigh);

        Collection<ControlStrategy> controlStrategies = new ArrayList<>();
        controlStrategies.add(csg1);
        controlStrategies.add(csg2);
        controlStrategies.add(csg3);

        String expectedJson =
            "{\n" +
            "  \"" + csg1.getUri() + "\": {\n" +
            "    \"label\": \"" + CONTROL_STRATEGY_1_LABEL + "\",\n" +
            "    \"description\": \"" + CONTROL_STRATEGY_1_DESCRIPTION + "\",\n" +
            "    \"enabled\": false,\n" +
            "    \"blockingEffect\": \"" + TW_LEVEL_LOW_URI + "\",\n" +
            "    \"blockingEffectLabel\": \"" + LEVEL_LOW_LABEL + "\",\n" +
            "    \"mandatoryControls\": [\n" +
            "      \"" + cs1.getUri() + "\",\n" +
            "      \"" + cs2.getUri() + "\"\n" +
            "    ],\n" +
            "    \"optionalControls\": [\n" +
            "    ]\n" +
            "  },\n" +
            "  \"" + csg2.getUri() + "\": {\n" +
            "    \"label\": \"" + CONTROL_STRATEGY_2_LABEL + "\",\n" +
            "    \"description\": \"" + CONTROL_STRATEGY_2_DESCRIPTION + "\",\n" +
            "    \"enabled\": true,\n" +
            "    \"blockingEffect\": \"" + TW_LEVEL_HIGH_URI + "\",\n" +
            "    \"blockingEffectLabel\": \"" + LEVEL_HIGH_LABEL + "\",\n" +
            "    \"mandatoryControls\": [\n" +
            "      \"" + cs1.getUri() + "\"\n" +
            "    ],\n" +
            "    \"optionalControls\": [\n" +
            "    ]\n" +
            "  },\n" +
            "  \"" + csg3.getUri() + "\": {\n" +
            "    \"label\": \"" + CONTROL_STRATEGY_3_LABEL + "\",\n" +
            "    \"description\": \"" + CONTROL_STRATEGY_3_DESCRIPTION + "\",\n" +
            "    \"enabled\": true,\n" +
            "    \"blockingEffect\": \"" + TW_LEVEL_HIGH_URI + "\",\n" +
            "    \"blockingEffectLabel\": \"" + LEVEL_HIGH_LABEL + "\",\n" +
            "    \"mandatoryControls\": [\n" +
            "    ],\n" +
            "    \"optionalControls\": [\n" +
            "    ]\n" +
            "  }\n" +
            "}";
        logger.debug("Expected JSON:\n{}", expectedJson);

        JsonObject result = reportGenerator.generateControlStrategiesJson(controlStrategies);

        logger.debug("Actual result:\n{}", result.toString());

        assertThat(result, is(JsonParser.parseString(expectedJson)));
    }

    @Test
    public void testGenerateReport() {
        testHelper.switchModels(1, 7);
        Model testModel = createTestModel();

        ReportGenerator reportGenerator = new ReportGenerator();

        String expectedJson =
            "{\n" +
            "  \"name\": \"JSON Report Test\",\n" +
            "  \"domain\": \"http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-network-testing\",\n" +
            "  \"assertedAssets\": {\n" +
            "    \"system#8667a66c\": {\n" +
            "      \"label\": \"D\",\n" +
            "      \"type\": \"domain#D\",\n" +
            "      \"typeLabel\": \"D\",\n" +
            "      \"controls\": [\n" +
            "        \"system#CS-Patching-29a92ee6\"\n" +
            "      ],\n" +
            "      \"misbehaviours\": [\n" +
            "        \"system#MS-Broken-29a92ee6\",\n" +
            "        \"system#MS-NotOK-29a92ee6\"\n" +
            "      ],\n" +
            "      \"trustworthinessAttributes\": [\n" +
            "        \"system#TWAS-OK-29a92ee6\"\n" +
            "      ]\n" +
            "    },\n" +
            "    \"system#65518fe1\": {\n" +
            "      \"label\": \"B\",\n" +
            "      \"type\": \"domain#B\",\n" +
            "      \"typeLabel\": \"B\",\n" +
            "      \"controls\": [\n" +
            "        \"system#CS-Patching-22e54aba\"\n" +
            "      ],\n" +
            "      \"misbehaviours\": [\n" +
            "        \"system#MS-Broken-22e54aba\",\n" +
            "        \"system#MS-NotOK-22e54aba\"\n" +
            "      ],\n" +
            "      \"trustworthinessAttributes\": [\n" +
            "        \"system#TWAS-OK-22e54aba\"\n" +
            "      ]\n" +
            "    }\n" +
            "  },\n" +
            "  \"inferredAssets\": {\n" +
            "    \"system#E_29a92ee6\": {\n" +
            "      \"label\": \"E-D\",\n" +
            "      \"type\": \"domain#E\",\n" +
            "      \"typeLabel\": \"E\",\n" +
            "      \"controls\": [],\n" +
            "      \"misbehaviours\": [\n" +
            "        \"system#MS-Broken-553889e2\",\n" +
            "        \"system#MS-NotOK-553889e2\"\n" +
            "      ],\n" +
            "      \"trustworthinessAttributes\": [\n" +
            "        \"system#TWAS-OK-553889e2\"\n" +
            "      ]\n" +
            "    }\n" +
            "  },\n" +
            "  \"relations\": {\n" +
            "    \"e449073d\": {\n" +
            "      \"from\": \"system#E_29a92ee6\",\n" +
            "      \"to\": \"system#8667a66c\",\n" +
            "      \"type\": \"domain#r10\"\n" +
            "    },\n" +
            "    \"9653403d\": {\n" +
            "      \"from\": \"system#65518fe1\",\n" +
            "      \"to\": \"system#8667a66c\",\n" +
            "      \"type\": \"domain#r07\"\n" +
            "    },\n" +
            "    \"a05d3853\": {\n" +
            "      \"from\": \"system#65518fe1\",\n" +
            "      \"to\": \"system#E_29a92ee6\",\n" +
            "      \"type\": \"domain#r09\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"threats\": {\n" +
            "    \"system#E.M.B9E.1-B9E_22e54aba_553889e2\": {\n" +
            "      \"description\": \"E.M.B9E.1: checks construction of links to multiply matched secondary nodes, in this case results from construction pattern B7D+E10D+B9E which creates asset \\\"E-D\\\" with relationships to root node D and (possibly multiple matches to) mandatory node B.\",\n" +
            "      \"resolved\": false,\n" +
            "      \"controlStrategies\": [],\n" +
            "      \"misbehaviours\": [\n" +
            "        \"system#MS-Broken-553889e2\"\n" +
            "      ],\n" +
            "      \"likelihoodLabel\": \"Very Low\",\n" +
            "      \"likelihood\": \"domain#LikelihoodVeryLow\",\n" +
            "      \"riskLabel\": \"Very Low\",\n" +
            "      \"risk\": \"domain#RiskLevelVeryLow\"\n" +
            "    },\n" +
            "    \"system#B.M.B.1-SoloB_22e54aba\": {\n" +
            "      \"description\": \"B.M.B.1: checks asset class inheritance is taken into account when matching a unique nodes. Here asset \\\"B\\\" must be of class B or its subclass C.\",\n" +
            "      \"resolved\": false,\n" +
            "      \"controlStrategies\": [\n" +
            "        \"system#B.M.B.1-SoloB_22e54aba-CSG-PatchingAtB\"\n" +
            "      ],\n" +
            "      \"misbehaviours\": [\n" +
            "        \"system#MS-Broken-22e54aba\"\n" +
            "      ],\n" +
            "      \"likelihoodLabel\": \"Very Low\",\n" +
            "      \"likelihood\": \"domain#LikelihoodVeryLow\",\n" +
            "      \"riskLabel\": \"Very Low\",\n" +
            "      \"risk\": \"domain#RiskLevelVeryLow\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"misbehaviours\": {\n" +
            "    \"system#MS-Broken-22e54aba\": {\n" +
            "      \"label\": \"Broken\",\n" +
            "      \"asset\": \"system#65518fe1\",\n" +
            "      \"impactLabel\": \"Very Low\",\n" +
            "      \"impact\": \"domain#ImpactLevelVeryLow\",\n" +
            "      \"likelihoodLabel\": \"Very Low\",\n" +
            "      \"likelihood\": \"domain#LikelihoodVeryLow\",\n" +
            "      \"riskLabel\": \"Very Low\",\n" +
            "      \"risk\": \"domain#RiskLevelVeryLow\"\n" +
            "    },\n" +
            "    \"system#MS-NotOK-22e54aba\": {\n" +
            "      \"label\": \"NotOK\",\n" +
            "      \"asset\": \"system#65518fe1\",\n" +
            "      \"impactLabel\": \"Very Low\",\n" +
            "      \"impact\": \"domain#ImpactLevelVeryLow\",\n" +
            "      \"likelihoodLabel\": \"Very Low\",\n" +
            "      \"likelihood\": \"domain#LikelihoodVeryLow\",\n" +
            "      \"riskLabel\": \"Very Low\",\n" +
            "      \"risk\": \"domain#RiskLevelVeryLow\"\n" +
            "    },\n" +
            "    \"system#MS-Broken-29a92ee6\": {\n" +
            "      \"label\": \"Broken\",\n" +
            "      \"asset\": \"system#8667a66c\",\n" +
            "      \"impactLabel\": \"Very Low\",\n" +
            "      \"impact\": \"domain#ImpactLevelVeryLow\",\n" +
            "      \"likelihoodLabel\": \"Very Low\",\n" +
            "      \"likelihood\": \"domain#LikelihoodVeryLow\",\n" +
            "      \"riskLabel\": \"Very Low\",\n" +
            "      \"risk\": \"domain#RiskLevelVeryLow\"\n" +
            "    },\n" +
            "    \"system#MS-Broken-553889e2\": {\n" +
            "      \"label\": \"Broken\",\n" +
            "      \"asset\": \"system#E_29a92ee6\",\n" +
            "      \"impactLabel\": \"Very Low\",\n" +
            "      \"impact\": \"domain#ImpactLevelVeryLow\",\n" +
            "      \"likelihoodLabel\": \"Very Low\",\n" +
            "      \"likelihood\": \"domain#LikelihoodVeryLow\",\n" +
            "      \"riskLabel\": \"Very Low\",\n" +
            "      \"risk\": \"domain#RiskLevelVeryLow\"\n" +
            "    },\n" +
            "    \"system#MS-NotOK-553889e2\": {\n" +
            "      \"label\": \"NotOK\",\n" +
            "      \"asset\": \"system#E_29a92ee6\",\n" +
            "      \"impactLabel\": \"Very Low\",\n" +
            "      \"impact\": \"domain#ImpactLevelVeryLow\",\n" +
            "      \"likelihoodLabel\": \"Very Low\",\n" +
            "      \"likelihood\": \"domain#LikelihoodVeryLow\",\n" +
            "      \"riskLabel\": \"Very Low\",\n" +
            "      \"risk\": \"domain#RiskLevelVeryLow\"\n" +
            "    },\n" +
            "    \"system#MS-NotOK-29a92ee6\": {\n" +
            "      \"label\": \"NotOK\",\n" +
            "      \"asset\": \"system#8667a66c\",\n" +
            "      \"impactLabel\": \"Very Low\",\n" +
            "      \"impact\": \"domain#ImpactLevelVeryLow\",\n" +
            "      \"likelihoodLabel\": \"Very Low\",\n" +
            "      \"likelihood\": \"domain#LikelihoodVeryLow\",\n" +
            "      \"riskLabel\": \"Very Low\",\n" +
            "      \"risk\": \"domain#RiskLevelVeryLow\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"trustworthinessAttributes\": {\n" +
            "    \"system#TWAS-OK-553889e2\": {\n" +
            "      \"attribute\": \"domain#OK\",\n" +
            "      \"attributeLabel\": \"OK\",\n" +
            "      \"asset\": \"system#E_29a92ee6\",\n" +
            "      \"assumedTWLabel\": \"Very High\",\n" +
            "      \"assumedTW\": \"domain#TrustworthinessLevelVeryHigh\",\n" +
            "      \"calculatedTWLabel\": \"Very High\",\n" +
            "      \"calculatedTW\": \"domain#TrustworthinessLevelVeryHigh\"\n" +
            "    },\n" +
            "    \"system#TWAS-OK-29a92ee6\": {\n" +
            "      \"attribute\": \"domain#OK\",\n" +
            "      \"attributeLabel\": \"OK\",\n" +
            "      \"asset\": \"system#8667a66c\",\n" +
            "      \"assumedTWLabel\": \"Very High\",\n" +
            "      \"assumedTW\": \"domain#TrustworthinessLevelVeryHigh\",\n" +
            "      \"calculatedTWLabel\": \"Very High\",\n" +
            "      \"calculatedTW\": \"domain#TrustworthinessLevelVeryHigh\"\n" +
            "    },\n" +
            "    \"system#TWAS-OK-22e54aba\": {\n" +
            "      \"attribute\": \"domain#OK\",\n" +
            "      \"attributeLabel\": \"OK\",\n" +
            "      \"asset\": \"system#65518fe1\",\n" +
            "      \"assumedTWLabel\": \"Very High\",\n" +
            "      \"assumedTW\": \"domain#TrustworthinessLevelVeryHigh\",\n" +
            "      \"calculatedTWLabel\": \"Very High\",\n" +
            "      \"calculatedTW\": \"domain#TrustworthinessLevelVeryHigh\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"controls\": {\n" +
            "    \"system#CS-Patching-22e54aba\": {\n" +
            "      \"label\": \"Patching\",\n" +
            "      \"asset\": \"system#65518fe1\",\n" +
            "      \"assertable\": false,\n" +
            "      \"proposed\": false,\n" +
            "      \"workInProgress\": false\n" +
            "    },\n" +
            "    \"system#CS-Patching-29a92ee6\": {\n" +
            "      \"label\": \"Patching\",\n" +
            "      \"asset\": \"system#8667a66c\",\n" +
            "      \"assertable\": true,\n" +
            "      \"proposed\": false,\n" +
            "      \"workInProgress\": false\n" +
            "    }\n" +
            "  },\n" +
            "  \"controlStrategies\": {\n" +
            "    \"system#B.M.B.1-SoloB_22e54aba-CSG-PatchingAtB\": {\n" +
            "      \"label\": \"PatchingAtB\",\n" +
            "      \"description\": \"Patching at asset with role B.\",\n" +
            "      \"enabled\": false,\n" +
            "      \"blockingEffect\": \"domain#TrustworthinessLevelVeryHigh\",\n" +
            "      \"blockingEffectLabel\": \"Very High\",\n" +
            "      \"mandatoryControls\": [\n" +
            "        \"system#CS-Patching-22e54aba\"\n" +
            "      ],\n" +
            "      \"optionalControls\": [\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}";
        logger.debug("Expected JSON:\n{}", expectedJson);

        String result = reportGenerator.generate(modelHelper, testModel);
        JsonElement actualJson = JsonParser.parseString(result);
        logger.debug("Actual result:\n{}", result);

        assertThat(actualJson, is(JsonParser.parseString(expectedJson)));
    }

    private Model createTestModel() {
        return modelFactory.getModel(testHelper.getModel(), testHelper.getStore());
    }
}
