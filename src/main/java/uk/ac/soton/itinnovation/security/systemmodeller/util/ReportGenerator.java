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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.model.SemanticEntity;
import uk.ac.soton.itinnovation.security.model.system.Asset;
import uk.ac.soton.itinnovation.security.model.system.ControlSet;
import uk.ac.soton.itinnovation.security.model.system.ControlStrategy;
import uk.ac.soton.itinnovation.security.model.system.MisbehaviourSet;
import uk.ac.soton.itinnovation.security.model.system.Relation;
import uk.ac.soton.itinnovation.security.model.system.Threat;
import uk.ac.soton.itinnovation.security.model.system.TrustworthinessAttributeSet;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.LoadingProgress;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;

public class ReportGenerator {

    public static Logger logger = LoggerFactory.getLogger(ReportGenerator.class);

    private Map<String, String> domainAssetLabels = new HashMap<>();

    public String generate(ModelObjectsHelper modelObjectsHelper, Model model) {
        setUp(modelObjectsHelper, model);

        JsonObject reportJson = new JsonObject();

        // Model meta information
        reportJson.addProperty("name", model.getName());
        reportJson.addProperty("domain", model.getDomainGraph());

        // Split assets into asserted and inferred
        List<Asset> assertedAssets = model.getAssets().stream().filter(a -> a.isAsserted()).collect(Collectors.toList());
        List<Asset> inferredAssets = model.getAssets().stream().filter(a -> !a.isAsserted()).collect(Collectors.toList());

        reportJson.add("assertedAssets", generateAssetsJson(assertedAssets));
        reportJson.add("inferredAssets", generateAssetsJson(inferredAssets));
        reportJson.add("relations", generateRelationsJson(model.getRelations()));
        reportJson.add("threats", generateThreatsJson(model.getThreats()));
        reportJson.add("misbehaviours", generateMisbehaviourSetsJson(model.getMisbehaviourSets().values()));
        reportJson.add("trustworthinessAttributes", generateTrustworthinessAttributeSetsJson(model.getTwas().values()));
        reportJson.add("controls", generateControlSetsJson(model.getControlSets()));
        reportJson.add("controlStrategies", generateControlStrategiesJson(model.getControlStrategies().values()));

        // Null fields are not serialised
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(reportJson);
    }

    private void setUp(ModelObjectsHelper modelObjectsHelper, Model model) {
        List<Map<String, String>> domainAssets;

        // Populating the model must be atomic
        synchronized(modelObjectsHelper.getModelLock(model)) {
            model.loadModelData(modelObjectsHelper, new LoadingProgress(null));

            // Needed to map asset types to labels
            domainAssets = modelObjectsHelper.getPaletteAssets(model.getDomainGraph());
        }

        // Map domain asset URIs to their label
        domainAssetLabels = new HashMap<>();
        for (Map<String, String> domainAssetRow : domainAssets) {
            String assetUri = domainAssetRow.get("asset");
            String assetLabel = domainAssetRow.get("al");
            if (assetUri != null && assetLabel != null) {
                domainAssetLabels.put(assetUri, assetLabel);
            }
        }
    }

    protected JsonObject generateAssetsJson(Collection<Asset> assets) {
        return generateCollectionJson(assets, this::generateAssetJson);
    }

    // This should also be done via a call to generateCollectionJson() but for
    // some inexplicable reason Relation is not a subclass of SemanticEntity
    protected JsonObject generateRelationsJson(Collection<Relation> relations) {
        JsonObject json = new JsonObject();
        for (Relation relation : relations) {
            json.add(relation.getID(), generateRelationJson(relation));
        }
        return json;
    }

    protected JsonObject generateThreatsJson(Collection<Threat> threats) {
        return generateCollectionJson(threats, this::generateThreatJson);
    }

    protected JsonObject generateMisbehaviourSetsJson(Collection<MisbehaviourSet> misbehaviourSets) {
        return generateCollectionJson(misbehaviourSets, this::generateMisbehaviourSetJson);
    }

    protected JsonObject generateTrustworthinessAttributeSetsJson(Collection<TrustworthinessAttributeSet> trustworthinessSets) {
        return generateCollectionJson(trustworthinessSets, this::generateTrustworthinessAttributeSetJson);
    }

    protected JsonObject generateControlSetsJson(Collection<ControlSet> controlSets) {
        return generateCollectionJson(controlSets, this::generateControlSetJson);
    }

    protected JsonObject generateControlStrategiesJson(Collection<ControlStrategy> controlStrategies) {
        return generateCollectionJson(controlStrategies, this::generateControlStrategyJson);
    }

    private <T extends SemanticEntity> JsonObject generateCollectionJson(Collection<T> ts, Function<T, JsonObject> generateEntityJson) {
        JsonObject json = new JsonObject();
        for (T t : ts) {
            json.add(getShortUri(t.getUri()), generateEntityJson.apply(t));
        }
        return json;
    }

    private JsonObject generateAssetJson(Asset asset) {
        JsonObject assetJson = new JsonObject();

        assetJson.addProperty("label", asset.getLabel());
        assetJson.addProperty("type", getShortUri(asset.getType()));
        assetJson.addProperty("typeLabel", domainAssetLabels.get(asset.getType()));
        assetJson.add("controls", entityIDs(asset.getControlSets()));
        assetJson.add("misbehaviours", entityIDs(asset.getMisbehaviourSets()));
        assetJson.add("trustworthinessAttributes", entityIDs(asset.getTrustworthinessAttributeSets()));

        return assetJson;
    }

    private JsonObject generateRelationJson(Relation relation) {
        JsonObject relationJson = new JsonObject();

        relationJson.addProperty("from", getShortUri(relation.getFrom()));
        relationJson.addProperty("to", getShortUri(relation.getTo()));
        relationJson.addProperty("type", getShortUri(relation.getType()));

        return relationJson;
    }

    private JsonObject generateThreatJson(Threat threat) {
        JsonObject threatJson = new JsonObject();

        threatJson.addProperty("description", threat.getDescription());
        threatJson.addProperty("resolved", threat.isResolved());
        threatJson.add("controlStrategies", entityIDs(threat.getControlStrategies()));
        threatJson.add("misbehaviours", entityIDs(threat.getMisbehaviours()));

        // Optional properties
        threatJson.addProperty("likelihoodLabel", labelOrNull(threat.getLikelihood()));
        threatJson.addProperty("likelihood", shortUriOrNull(threat.getLikelihood()));
        threatJson.addProperty("riskLabel", labelOrNull(threat.getRiskLevel()));
        threatJson.addProperty("risk", shortUriOrNull(threat.getRiskLevel()));
        threatJson.addProperty("acceptanceJustification", threat.getAcceptanceJustification());

        return threatJson;
    }

    private JsonObject generateMisbehaviourSetJson(MisbehaviourSet misbehaviourSet) {
        JsonObject msJson = new JsonObject();

        msJson.addProperty("label", misbehaviourSet.getMisbehaviourLabel());
        msJson.addProperty("asset", getShortUri(misbehaviourSet.getAsset()));

        // Optional properties
        msJson.addProperty("impactLabel", labelOrNull(misbehaviourSet.getImpactLevel()));
        msJson.addProperty("impact", shortUriOrNull(misbehaviourSet.getImpactLevel()));
        msJson.addProperty("likelihoodLabel", labelOrNull(misbehaviourSet.getLikelihood()));
        msJson.addProperty("likelihood", shortUriOrNull(misbehaviourSet.getLikelihood()));
        msJson.addProperty("riskLabel", labelOrNull(misbehaviourSet.getRiskLevel()));
        msJson.addProperty("risk", shortUriOrNull(misbehaviourSet.getRiskLevel()));

        return msJson;
    }

    private JsonObject generateTrustworthinessAttributeSetJson(TrustworthinessAttributeSet twas) {
        JsonObject twasJson = new JsonObject();

        twasJson.addProperty("attribute", getShortUri(twas.getAttribute().getUri()));
        twasJson.addProperty("attributeLabel", twas.getAttribute().getLabel());
        twasJson.addProperty("asset", getShortUri(twas.getAsset()));

        // Optional properties
        twasJson.addProperty("assumedTWLabel", labelOrNull(twas.getAssertedTWLevel()));
        twasJson.addProperty("assumedTW", shortUriOrNull(twas.getAssertedTWLevel()));
        twasJson.addProperty("calculatedTWLabel", labelOrNull(twas.getInferredTWLevel()));
        twasJson.addProperty("calculatedTW", shortUriOrNull(twas.getInferredTWLevel()));

        return twasJson;
    }

    private JsonObject generateControlSetJson(ControlSet controlSet) {
        JsonObject csJson = new JsonObject();

        csJson.addProperty("label", controlSet.getLabel());
        csJson.addProperty("asset", getShortUri(controlSet.getAssetUri()));
        csJson.addProperty("assertable", controlSet.isAssertable());
        csJson.addProperty("proposed", controlSet.isProposed());
        csJson.addProperty("workInProgress", controlSet.isWorkInProgress());

        return csJson;
    }

    private JsonObject generateControlStrategyJson(ControlStrategy controlStrategy) {
        JsonObject csgJson = new JsonObject();

        csgJson.addProperty("label", controlStrategy.getLabel());
        csgJson.addProperty("description", controlStrategy.getDescription());
        //csgJson.addProperty("threat", getShortUri(controlStrategy.getThreat())); //TODO: get list of threats
        csgJson.addProperty("enabled", controlStrategy.isEnabled());
        csgJson.addProperty("blockingEffect", getShortUri(controlStrategy.getBlockingEffect().getUri()));
        csgJson.addProperty("blockingEffectLabel", controlStrategy.getBlockingEffect().getLabel());
        csgJson.add("mandatoryControls", entityIDs(controlStrategy.getMandatoryControlSets()));
        csgJson.add("optionalControls", entityIDs(controlStrategy.getOptionalControlSets()));

        return csgJson;
    }

    private <T extends SemanticEntity> JsonArray entityIDs(Map<String, T> entityMap) {
        JsonArray array = new JsonArray();

        // JSON arrays are ordered but entityMap is not.
        // To make testing easier we make the output deterministic
        // by sorting based on the URIs.
        entityMap
            .values()
            .stream()
            .sorted(Comparator.comparing(t -> t.getUri()))
            .forEach(t -> array.add(getShortUri(t.getUri())));

        return array;
    }

    private String labelOrNull(SemanticEntity entity) {
        return entity != null ? entity.getLabel() : null;
    }

    private String shortUriOrNull(SemanticEntity entity) {
        return entity != null ? getShortUri(entity.getUri()) : null;
    }

    // return the final part of a URI, e.g. "domain#something"
    private String getShortUri(String uri) {
        String[] parts = uri.split("/");
        return parts[parts.length - 1];
    }

    // Back channel for use by the unit tests
    protected Map<String, String> getDomainAssetLabels() {
        return domainAssetLabels;
    }
}
