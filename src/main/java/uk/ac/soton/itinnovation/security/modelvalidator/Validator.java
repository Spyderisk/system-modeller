/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2020
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
//      Created By:				Lee Mason
//      Created Date:			2020-08-10
//      Created for Project :   FOGPROTECT
//      Modified By:            Mike Surridge
//      Modified for Project :  FOGPROTECT
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelvalidator;

import lombok.Data;

import org.apache.jena.sparql.function.library.leviathan.sec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.core.joran.conditional.ElseAction;
import ch.qos.logback.core.joran.conditional.ThenAction;
import uk.ac.soton.itinnovation.security.modelquerier.IQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.*;
import uk.ac.soton.itinnovation.security.modelvalidator.java.GraphMatchedPattern;
import uk.ac.soton.itinnovation.security.modelvalidator.java.GraphNode;
import uk.ac.soton.itinnovation.security.modelvalidator.java.GraphPattern;
import uk.ac.soton.itinnovation.security.modelvalidator.java.PatternLink;

import java.util.*;

import javax.ws.rs.DefaultValue;

public class Validator {
    // TODO: Replace CardinalityConstraintDB with LinkDB subclass (as storing anyway, due to work-around)
    // TODO: Extend AValidator (?)

    private static final Logger logger = LoggerFactory.getLogger(Validator.class);

    private IQuerierDB querier;

    // Domain model data
    private ModelDB domainModel;                                                // Basic information about the domain model
    private Map<String, ModelFeatureDB> domainFeatures = new HashMap<>();       // Map of domain model features

    private Map<String, LevelDB> poLevels = new HashMap<>();                    // Map of domain model population levels indexed by URI
    private List<LevelDB> populationLevels = new ArrayList<>();                 // List of population levels, ordered by level value (increasing population)

    private Map<String, LevelDB> twLevels = new HashMap<>();                    // Map of domain model trustworthiness levels indexed by URI
    private List<LevelDB> trustworthinessLevels = new ArrayList<>();            // List of trustworthiness levels, ordered by level value (increasing TW)

    private Map<String, LevelDB> imLevels = new HashMap<>();                    // Map of domain model impact levels indexed by URI
    private List<LevelDB> impactLevels = new ArrayList<>();                     // List of impact levels, ordered by level value (increasing impact)

    private Map<String, Set<String>> assetRolesMap = null;                      // Map from domain model assets classes to roles that may be taken by assets in each class

    private Map<String, MisbehaviourDB> dmisbehaviours = new HashMap<>();               // Domain model misbehaviours
    Map<String, String> dmavg = new HashMap<>();
    Map<String, String> dmmin = new HashMap<>();
    Map<String, String> dmmax = new HashMap<>();

    private Map<String, ControlDB> dcontrols = new HashMap<>();                         // Domain model controls
    Map<String, String> dcavg = new HashMap<>();
    Map<String, String> dcmin = new HashMap<>();
    Map<String, String> dcmax = new HashMap<>();

    private Map<String, RoleDB> domainRoles = new HashMap<>();                          // Domain model role classes
    private Map<String, NodeDB> domainNodes = new HashMap<>();                          // Domain model nodes
    private Map<String, RootPatternDB> domainRPs = new HashMap<>();                     // Domain model root patterns
    private Map<String, MatchingPatternDB> domainMPs = new HashMap<>();                 // Domain model matching patterns
    private Map<String, ThreatDB> domainThreats = new HashMap<>();                      // Domain model Threats
    Map<String, String> dtavg = new HashMap<>();
    Map<String, String> dtmax = new HashMap<>();
    private Map<String, List<String>> uniqueRolesByThreat = new HashMap<>();            // Map of unique roles indexed by domain model threat URI
    private Map<String, List<String>> necessaryRolesByThreat = new HashMap<>();         // Map of necessary roles indexed by domain model threat URI
    private Map<String, List<String>> sufficientRolesByThreat = new HashMap<>();        // Map of sufficient roles indexed by domain model threat URI
    private Map<String, List<String>> optionalRolesByThreat = new HashMap<>();          // Map of optional roles indexed by domain model threat URI

    private Map<String, ControlStrategyDB> domainCSGs = new HashMap<>();                // Domain model CSGs
    private Map<String, Set<ControlStrategyDB>> threatCSGs = new HashMap<>();           // Map of all domain model CSGs related to each domain model threat

    private Map<String, MisbehaviourSetDB> domainMSs = new HashMap<>();                 // Domain model MS
    private Map<String, TrustworthinessAttributeSetDB> domainTWASs = new HashMap<>();   // Domain model TWAS

    private Map<String, GraphNode> graphNodeMap;                                        // Graph representation of system assets and links
    private Map<String, Set<CardinalityConstraintDB>> missingAssetLinksFrom;            // Map of links from missing (possibly inferred) assets
    private Map<String, Set<CardinalityConstraintDB>> missingAssetLinksTo;              // Map of links to missing (possibly inferred) assets

    private Map<String, NodeDB> systemNodes = new HashMap<>();                          // System model nodes: initialised after construction
    private Map<String, Map<String,NodeDB>> systemNodeMap = new HashMap<>();            // System model nodes: listed by role against the associated asset

    private Map<String, ThreatDB> systemThreats = new HashMap<>();                      // System threats, to be generated
    private Map<String, ControlStrategyDB> systemCSGs = new HashMap<>();                // System CSGs, to be generated

    private Map<String, Map<String, TrustworthinessAttributeSetDB>> twasByAsset = new HashMap<>();  // Map of system TWAS indexed by asset URI, then TWA URI
    private Map<String, Map<String, MisbehaviourSetDB>> msByAsset = new HashMap<>();                // Map of system MS indexed by asset URI, then Misbehaviour URI
    private Map<String, Map<String, ControlSetDB>> csByAsset = new HashMap<>();                     // Map of system CS indexed by asset URI, then Control URI

    Map<String, List<CardinalityConstraintDB>> linksFromAssets = new HashMap<>();                   // Lists of cardinality constraints indexed by the source asset URI

    public Validator(IQuerierDB querier) {
        final long startTime = System.currentTimeMillis();

        // Save the querier object
        this.querier = querier;
    
        // This where the spurious "Model" type is created and then added to the system graph. Using
        // a class called ModelDB means when serialised the type is set to "Model". That doesn't make
        // it a bad idea, but (a) we must add the new type to the core model, and (b) it should really
        // be attached to the system model graph URI, not the system model prefix.
        ModelDB systemModel = new ModelDB();
        systemModel.setUri("system");

        // Store the domain model version which is to be used during validation of the system model.
        domainModel = querier.getModelInfo("domain");
        if (domainModel != null) {
            systemModel.setDomainVersion(domainModel.getVersionInfo());
        }

        querier.store(systemModel, "system-inf");

        // Now get the model assets and relationships as a graph
        this.missingAssetLinksFrom = new HashMap<>();
        this.missingAssetLinksTo = new HashMap<>();
        this.graphNodeMap = getGraphNodeMap();

        final long endTime = System.currentTimeMillis();
        logger.info("Validator.Validator(): execution time {} ms", endTime - startTime);        

    }

    /** Perform a full validation and persist results to the database. This method is not
     *  actually used, because the DesignTimeValidator orchestrates the sequence itself.
     */
    public void validate(Progress progress) {
        progress.updateProgress(0.0, "Repairing asserted system model");
        repairAssertedSystemModel();

        progress.updateProgress(0.1, "Loading knowledge base");
        createDomainModelMaps();

        progress.updateProgress(0.2, "Generating implicit assets/relationships");
        calculateAssertedCardinalityConstraints();
        executeConstructionPatterns();
        deleteConstructionState();

        progress.updateProgress(0.4, "Generating asset trustworthiness attributes, behaviours and controls");
        createControlSets();
        createMisbehaviourSets();
        createTrustworthinessAttributeSets();
        createTrustworthinessImpactSets();

        progress.updateProgress(0.5, "Generating threats");
        createThreats();

        progress.updateProgress(0.7, "Generating control strategies");
        createControlStrategies();

        progress.updateProgress(0.9, "Persisting model to database");
        querier.sync("system-inf");

    }

    /** Method to repair the asserted graph before starting to validate the model.
     */
    public void repairAssertedSystemModel(){
        // Insert population levels for assets that don't have them
        querier.repairAssertedAssetPopulations();

        // Clear out old asset and relationship cardinality constraints
        querier.repairCardinalityConstraints();
    }

    /** Loads the domain model and generates maps used elsewhere.
     * 
     *  We assume the domain model will be reasonably small, so computing everything once
     *  and holding it in memory should be more efficient and reduce the need to copy code
     *  in multiple methods.
     */
    public void createDomainModelMaps () {
        final long startTime = System.currentTimeMillis();

        // Get domain model features
        domainFeatures = querier.getModelFeatures("domain");
        for(ModelFeatureDB domainFeature : domainFeatures.values())
            logger.info("Found domain model feature {}", domainFeature.getUri());

        // Load and save the population scales (used to calculate and set constructed asset population levels)
        poLevels = querier.getPopulationLevels();
        populationLevels.addAll(poLevels.values());
        populationLevels.sort(Comparator.comparingInt(LevelDB::getLevelValue));

        // Load and save the trustworthingess scales (used to calculate and set default levels for TWAS and CS)
        twLevels = querier.getTrustworthinessLevels();
        trustworthinessLevels.addAll(twLevels.values());
        trustworthinessLevels.sort(Comparator.comparingInt(LevelDB::getLevelValue));
        
        // Load and save the impact scales (used to calculate and set default levels for MS)
        imLevels = querier.getImpactLevels();
        impactLevels.addAll(imLevels.values());
        impactLevels.sort(Comparator.comparingInt(LevelDB::getLevelValue));

        // Load domain model info used to create threats and control strategies
        domainMSs = querier.getMisbehaviourSets("domain");
        domainTWASs = querier.getTrustworthinessAttributeSets("domain");

        // Get the domain model misbehaviour types, and organise them into triplets
        dmisbehaviours = querier.getMisbehaviours("domain");
        for (MisbehaviourDB s : dmisbehaviours.values()){
            if((s.getMinOf() != null) && (s.getMaxOf() == null)){
                // This is the lowest likelihood misbehaviour 
                dmmin.put(s.getMinOf(), s.getUri());
            } else if((s.getMinOf() == null) && (s.getMaxOf() != null)){
                // This is the highest likelihood misbehaviour 
                dmmax.put(s.getMaxOf(), s.getUri());
            } else {
                // This must be the average likelihood misbehaviour
                dmavg.put(s.getUri(), s.getUri());
                if((s.getMinOf() != null) && (s.getMaxOf() != null)){
                    // If it is both highest and lowest, treat it as average but log an error
                    logger.warn("Misbehaviour {} is both a highest and lowest likelihood behaviour, which should be impossible", s.getUri());
                }    
            }
        }

        // Get the domain model control types, and organise them into triplets
        dcontrols = querier.getControls("domain");
        for (ControlDB s : dcontrols.values()){
            if((s.getMinOf() != null) && (s.getMaxOf() == null)){
                // This is the lowest coverage control 
                dcmin.put(s.getMinOf(), s.getUri());
            } else if((s.getMinOf() == null) && (s.getMaxOf() != null)){
                // This is the highest coverage control 
                dcmax.put(s.getMaxOf(), s.getUri());
            } else {
                // This must be the average coverage control
                dcavg.put(s.getUri(), s.getUri());
                if((s.getMinOf() != null) && (s.getMaxOf() != null)){
                    // If it is both highest and lowest, treat it as average but log an error
                    logger.warn("Control {} is both a highest and lowest coverage control, which should be impossible", s.getUri());
                }    
            }
        }

        // Get domain model threats, patterns and nodes and create maps of roles per domain model threat
        domainRoles = querier.getRoles("domain");
        domainNodes = querier.getNodes("domain");
        domainMPs = querier.getMatchingPatterns("domain");
        domainRPs = querier.getRootPatterns("domain");
        domainThreats = querier.getThreats("domain");
        for(ThreatDB dthreat : domainThreats.values()) {
            // Create maps of roles of each type
            List<String> uniqueRoles = uniqueRolesByThreat.computeIfAbsent(dthreat.getUri(), k -> new ArrayList<>());
            List<String> necessaryRoles = necessaryRolesByThreat.computeIfAbsent(dthreat.getUri(), k -> new ArrayList<>());
            List<String> sufficientRoles = sufficientRolesByThreat.computeIfAbsent(dthreat.getUri(), k -> new ArrayList<>());
            List<String> optionalRoles = optionalRolesByThreat.computeIfAbsent(dthreat.getUri(), k -> new ArrayList<>());
            String dmpUri = dthreat.getAppliesTo();
            MatchingPatternDB dmp = domainMPs.get(dmpUri);
            String drpUri = dmp.getRootPattern();
            RootPatternDB drp = domainRPs.get(drpUri);
            for(String dn : drp.getKeyNodes()) {
                NodeDB dnode = domainNodes.get(dn);
                uniqueRoles.add(dnode.getRole());
            }
            for(String dn : drp.getRootNodes()) {
                NodeDB dnode = domainNodes.get(dn);
                uniqueRoles.add(dnode.getRole());
            }
            for(String dn : dmp.getNecessaryNodes()) {
                NodeDB dnode = domainNodes.get(dn);
                necessaryRoles.add(dnode.getRole());
            }
            for(String dn : dmp.getSufficientNodes()) {
                NodeDB dnode = domainNodes.get(dn);
                sufficientRoles.add(dnode.getRole());
            }
            for(String dn : dmp.getOptionalNodes()) {
                NodeDB dnode = domainNodes.get(dn);
                optionalRoles.add(dnode.getRole());
            }

        }

        // Set flags indicating the type of threat, using naming conventions if necessary
        if(domainFeatures.containsKey("domain#Feature-ThreatTypeFlags")){
            logger.info("Domain model supports flags for secondary and normal operational process threats");
        } else {
            if(domainFeatures.containsKey("domain#Feature-MixedThreatCauses")){
                logger.info("Setting secondary threat flags based on domain model threat naming conventions");
                for(ThreatDB dthreat : domainThreats.values())
                    dthreat.setSecondaryThreat(dthreat.getUri().contains(".0"));
            } else {
                logger.info("Setting secondary threat flags based on the presence of secondary effect causes");
                for(ThreatDB dthreat : domainThreats.values())
                    dthreat.setSecondaryThreat(!dthreat.getSecondaryEffectConditions().isEmpty());
            }
            logger.info("Setting normal operation threat flags based on domain model threat naming conventions");
            for(ThreatDB dthreat : domainThreats.values())
                dthreat.setNormalOperation(dthreat.getUri().contains(".8") || dthreat.getUri().contains(".E-"));
        }

        // Load control strategies and create references from threats to CSGs
        domainCSGs = querier.getControlStrategies("domain");
        for (ControlStrategyDB domainCSG : domainCSGs.values()) {
            for(String dthreatURI : domainCSG.getBlocks()) {
                ThreatDB s = domainThreats.get(dthreatURI);
                s.getBlockedByCSG().add(domainCSG.getUri());
                Set<ControlStrategyDB> thisThreatCSGs = threatCSGs.computeIfAbsent(dthreatURI, k -> new HashSet<>());
                thisThreatCSGs.add(domainCSG);
            }
            for(String dthreatURI : domainCSG.getMitigates()) {
                ThreatDB s = domainThreats.get(dthreatURI);
                s.getMitigatedByCSG().add(domainCSG.getUri());
                Set<ControlStrategyDB> thisThreatCSGs = threatCSGs.computeIfAbsent(dthreatURI, k -> new HashSet<>());
                thisThreatCSGs.add(domainCSG);
            }
            for(String dthreatURI : domainCSG.getTriggers()) {
                ThreatDB s = domainThreats.get(dthreatURI);
                s.getTriggeredByCSG().add(domainCSG.getUri());
                s.setTriggered(true);
                Set<ControlStrategyDB> thisThreatCSGs = threatCSGs.computeIfAbsent(dthreatURI, k -> new HashSet<>());
                thisThreatCSGs.add(domainCSG);
            }
        }

        final long endTime = System.currentTimeMillis();
        logger.info("Validator.createDomainModelMaps(): execution time {} ms", endTime - startTime);        
    }

    /** Queries for assets and links, uses them to generate a graph structure (consisting of nodes and links between the
     *  nodes). This graph facilitates sub-graph pattern matching.
     */
    private Map<String, GraphNode> getGraphNodeMap() {
        Map<String, GraphNode> graphNodeMap = new HashMap<>();

        Map<String, AssetDB> assets = querier.getAssets("system", "system-inf");
        Map<String, Set<String>> assetRoles = getAssetRoles();

        for (AssetDB asset : assets.values()) {
            GraphNode node = new GraphNode(asset.getUri(), assetRoles.get(asset.getType()));
            node.setType(asset.getType());
            graphNodeMap.put(asset.getUri(), node);
        }

        /*
         * There is a problem here if the model includes 'dangling' links. In principle, they can arise if either:
         * 
         * - an older, buggier version of SSM failed to remove an asserted link to or
         *   from an asset when the asset was deleted,
         * - a user added a link to or from an inferred asset, and the asset hasn't
         *   yet been regenerated.
         * 
         * In the latter case, we need to save the link so it can be inserted when the inferred asset is created.
         * We can't easily determine that this is the case, so we should just save any dangling link. 
         */
        Map<String, CardinalityConstraintDB> links = querier.getCardinalityConstraints("system", "system-inf");
        for (CardinalityConstraintDB link : links.values()) {
            if(link.getLinksFrom() == null || link.getLinksTo() == null || link.getLinkType() == null) {
                // Bad link with a missing property, so remove it from the graph
                logger.warn("Link {} has a null source, target or type property - deleting it from the model", link.getUri());
                querier.delete(link, false);
                continue;
            }

            // Find the graph nodes referred to in the link
            GraphNode fromNode = graphNodeMap.get(link.getLinksFrom());
            GraphNode toNode = graphNodeMap.get(link.getLinksTo());

            if (fromNode != null && toNode != null) {
                // If both ends exist, insert the link into the graph
                fromNode.addForwardLink(link.getLinkType(), toNode);
                toNode.addBackwardLink(link.getLinkType(), fromNode);
            } else {
                // One or both ends of the link refers to an asset that does not exist, so save it and try to connect it up later
                if(fromNode == null) {
                    Set<CardinalityConstraintDB> missingAssetLinks = missingAssetLinksFrom.computeIfAbsent(link.getLinksTo(), k -> new HashSet<>());
                    missingAssetLinks.add(link);
                }
                if(toNode == null) {
                    Set<CardinalityConstraintDB> missingAssetLinks = missingAssetLinksTo.computeIfAbsent(link.getLinksTo(), k -> new HashSet<>());
                    missingAssetLinks.add(link);
                }
            }
        }
        return graphNodeMap;
    }

    public void createControlSets() {
        final long startTime = System.currentTimeMillis();

        // Get the domain model control types, and construct their triplets
        Map<String, ControlDB> dss = querier.getControls("domain");
        Map<String, String> dssavg = new HashMap<>();
        Map<String, String> dssmin = new HashMap<>();
        Map<String, String> dssmax = new HashMap<>();
        for (ControlDB s : dss.values()){
            if((s.getMinOf() != null) && (s.getMaxOf() == null)){
                // This is the lowest coverage control
                dssmin.put(s.getMinOf(), s.getUri());
            } else if((s.getMinOf() == null) && (s.getMaxOf() != null)){
                // This is the highest coverage control
                dssmax.put(s.getMaxOf(), s.getUri());
            } else {
                // This must be the average coverage control
                dssavg.put(s.getUri(), s.getUri());
                if((s.getMinOf() != null) && (s.getMaxOf() != null)){
                    // If it is both highest and lowest, treat it as average but log an error
                    logger.warn("Control {} is both a highest and lowest coverage control, which should be impossible", s.getUri());
                }    
            }
        }

        // Get the domain model control locations, and find all asset classes that should have the control
        Map<String, Set<String>> dsassets = new HashMap<>();
        for (String sUri : dssavg.values()){
            ControlDB s = dss.get(sUri);
            Set<String> dassetsByType = dsassets.computeIfAbsent(sUri, k -> new HashSet<>());
            Collection<String> dslocations = s.getMetaLocatedAt();
            for(String loc : dslocations){
                List<String> assetSubTypes = querier.getSubTypes(loc, true);
                for(String assetType : assetSubTypes){
                    dassetsByType.add(assetType);
                }
            }
        }

        // Get the system assets, and iterate over them creating their ControlSets
        Map<String,AssetDB> assets = querier.getAssets("system", "system-inf");
        for (AssetDB asset : assets.values()){
            Map<String, ControlSetDB> csThisAsset = csByAsset.computeIfAbsent(asset.getUri(), k -> new HashMap<>());
            String assetType = asset.getType();
            LevelDB popLevel = poLevels.get(asset.getPopulation());
            for(String avgControl : dssavg.values()){
                // If the asset is a subtype of a Control location, try to create control set(s)
                if(dsassets.get(avgControl).contains(assetType)){
                    // Find the default average level and statistical distribution type
                    CASettingDB setting = querier.getCASetting(asset, avgControl);
                    Integer defaultLevel = twLevels.size()-1;
                    Boolean assertible = false;
                    Boolean independentLevels = false;
                    if (setting != null) {
                        // Old domain models have no levels, so check for that
                        if (setting.getLevel() != null) {
                            defaultLevel = twLevels.get(setting.getLevel()).getLevelValue();
                            independentLevels = setting.getIndependentLevels();
                        }
                        assertible = setting.getAssertible();
                    }
                    if(!assertible) {
                        // Skip this control type, as it can't be selected at this asset
                        continue;
                    }

                    // Create the average coverage CS, and get any properties defined in the asserted graph
                    String uri = querier.generateControlSetUri(avgControl, asset);
                    ControlSetDB savg = new ControlSetDB();
                    savg.setUri(uri);
                    savg.setControl(avgControl);
                    savg.setLocatedAt(asset.getUri());
                    ControlSetDB savgInput = querier.getControlSet(savg.getUri(), "system");

                    /* If the asset is non-singleton and the domain model specifies a full triplet is needed, create
                     * the other members. Note that new domain models with population support will have both the min 
                     * and max members (if required for this type of control), old domain models will have neither.
                     */ 
                    // Check if we have min/max controls (new domain models have both, old have neither)
                    if (dssmin.containsKey(avgControl) && dssmax.containsKey(avgControl) && popLevel.getLevelValue() > 0){
                        // Create the lowest coverage CS, and get any properties defined in the asserted graph
                        String minControl = dssmin.get(avgControl);
                        String uriMin = querier.generateControlSetUri(minControl, asset);
                        ControlSetDB smin = new ControlSetDB();
                        smin.setUri(uriMin);
                        smin.setControl(minControl);
                        smin.setLocatedAt(asset.getUri());
                        smin.setMinOf(savg.getUri());
                        savg.setHasMin(smin.getUri());
                        ControlSetDB sminInput = querier.getControlSet(smin.getUri(), "system");
    
                        // Create the highest coverage CS, and get any properties defined in the asserted graph
                        String maxControl = dssmax.get(avgControl);
                        String uriMax = querier.generateControlSetUri(maxControl, asset);
                        ControlSetDB smax = new ControlSetDB();
                        smax.setUri(uriMax);
                        smax.setControl(maxControl);
                        smax.setLocatedAt(asset.getUri());
                        smax.setMaxOf(savg.getUri());
                        savg.setHasMax(smax.getUri());
                        ControlSetDB smaxInput = querier.getControlSet(smax.getUri(), "system");

                        // Assemble asserted graph levels and proposed status into triplets of values
                        Integer[] assertedValues = {null, null, null};
                        Boolean[] proposedStatus = {false, false, false};
                        if (sminInput != null) {
                            if (sminInput.getCoverageLevel() != null){
                                assertedValues[0] = twLevels.get(sminInput.getCoverageLevel()).getLevelValue();
                            }
                            proposedStatus[0] = sminInput.isProposed();
                        }
                        if (savgInput != null) {
                            if (savgInput.getCoverageLevel() != null){
                                assertedValues[1] = twLevels.get(savgInput.getCoverageLevel()).getLevelValue();
                            }
                            proposedStatus[1] = savgInput.isProposed();
                        }
                        if (smaxInput != null) {
                            if (smaxInput.getCoverageLevel() != null){
                                assertedValues[2] = twLevels.get(smaxInput.getCoverageLevel()).getLevelValue();
                            }
                            proposedStatus[2] = smaxInput.isProposed();
                        }

                        // Find a consistent set of levels obtained by adjusting the asserted graph levels
                        Integer[] adjustedValues = querier.getAdjustedLevels(asset, defaultLevel, assertedValues);

                        // Now compare the levels and update asserted graph values and/or set inferred graph values
                        LevelDB averageLevel = trustworthinessLevels.get(adjustedValues[1]); 
                        LevelDB inferredLevel = null;
                        LevelDB adjustedLevel = null;

                        if(assertedValues[0] != null) {
                            if(adjustedValues[0] != assertedValues[0]) {
                                // Need to modify the lowest level in the asserted graph
                                adjustedLevel = trustworthinessLevels.get(adjustedValues[0]); 
                                querier.updateCoverageLevel(adjustedLevel, smin, "system");
                            }
                        }
                        // Insert a calculated lowest value in the inferred graph
                        if(adjustedValues[0] == null)
                            adjustedValues[0] = querier.lookupLowestTWLevel(averageLevel, popLevel, independentLevels).getLevelValue();
                        inferredLevel = trustworthinessLevels.get(adjustedValues[0]); 
                        smin.setCoverageLevel(inferredLevel.getUri());

                        if(assertedValues[1] != null) {
                            if(adjustedValues[1] != assertedValues[1]) {
                                // Need to modify the average level in the asserted graph
                                adjustedLevel = trustworthinessLevels.get(adjustedValues[1]); 
                                querier.updateCoverageLevel(adjustedLevel, savg, "system");
                            }
                        }
                        // Insert a default or constrained average value in the inferred graph
                        if(adjustedValues[1] == null)
                            adjustedValues[1] = defaultLevel;
                        inferredLevel = trustworthinessLevels.get(adjustedValues[1]); 
                        savg.setCoverageLevel(inferredLevel.getUri());

                        if(assertedValues[2] != null) {
                            if(adjustedValues[2] != assertedValues[2]) {
                                // Need to modify the highest level in the asserted graph
                                adjustedLevel = trustworthinessLevels.get(adjustedValues[2]); 
                                querier.updateCoverageLevel(adjustedLevel, smax, "system");
                            }
                        } 
                        // Insert a calculated highest value in the inferred graph
                        if(adjustedValues[2] == null)
                            adjustedValues[2] = querier.lookupHighestTWLevel(averageLevel, popLevel, independentLevels).getLevelValue();
                        inferredLevel = trustworthinessLevels.get(adjustedValues[2]); 
                        smax.setCoverageLevel(inferredLevel.getUri());

                        // Ensure the asserted graph proposed status is consistent across the triplet
                        Boolean enabled = proposedStatus[0] || proposedStatus[1] || proposedStatus[2];
                        if(enabled && !proposedStatus[0]) 
                                querier.updateProposedStatus(enabled, smin, "system");
                        if(enabled && !proposedStatus[1]) 
                                querier.updateProposedStatus(enabled, savg, "system");
                        if(enabled && !proposedStatus[2]) 
                                querier.updateProposedStatus(enabled, smax, "system");

                        // Store the new control sets in the inferred graph and in the map used later to create CSGs
                        csThisAsset.put(smin.getControl(), smin);
                        querier.store(smin, "system-inf");

                        csThisAsset.put(savg.getControl(), savg);
                        querier.store(savg, "system-inf");
                        
                        csThisAsset.put(smax.getControl(), smax);
                        querier.store(smax, "system-inf");

                    }
                    else {
                        // Just put the default level into the inferred graph
                        savg.setCoverageLevel(trustworthinessLevels.get(defaultLevel).getUri());

                        // Store the new control set in the inferred graph and in the map used later to create CSGs
                        csThisAsset.put(savg.getControl(), savg);
                        querier.store(savg, "system-inf");
                    }

                }

            }

        }

        final long endTime = System.currentTimeMillis();
        logger.info("Validator.createControlSets(): execution time {} ms", endTime - startTime);

    }

    public void createMisbehaviourSets() {

        final long startTime = System.currentTimeMillis();

        // Get the domain model misbehaviour types, and construct their triplets
        Map<String, MisbehaviourDB> dss = querier.getMisbehaviours("domain");
        Map<String, String> dssavg = new HashMap<>();
        Map<String, String> dssmin = new HashMap<>();
        Map<String, String> dssmax = new HashMap<>();
        for (MisbehaviourDB s : dss.values()){
            if((s.getMinOf() != null) && (s.getMaxOf() == null)){
                // This is the lowest likelihood misbehaviour 
                dssmin.put(s.getMinOf(), s.getUri());
            } else if((s.getMinOf() == null) && (s.getMaxOf() != null)){
                // This is the highest likelihood misbehaviour 
                dssmax.put(s.getMaxOf(), s.getUri());
            } else {
                // This must be the average likelihood misbehaviour
                dssavg.put(s.getUri(), s.getUri());
                if((s.getMinOf() != null) && (s.getMaxOf() != null)){
                    // If it is both highest and lowest, treat it as average but log an error
                    logger.warn("Misbehaviour {} is both a highest and lowest likelihood behaviour, which should be impossible", s.getUri());
                }    
            }
        }

        // Get the domain model Misbehaviour locations, and find all asset classes that should have the Misbehaviour
        Map<String, Set<String>> dsassets = new HashMap<>();
        for (String sUri : dssavg.values()){
            MisbehaviourDB s = dss.get(sUri);
            Set<String> dassetsByType = dsassets.computeIfAbsent(sUri, k -> new HashSet<>());
            Collection<String> dslocations = s.getMetaLocatedAt();
            for(String loc : dslocations){
                List<String> assetSubTypes = querier.getSubTypes(loc, true);
                for(String assetType : assetSubTypes){
                    dassetsByType.add(assetType);
                }
            }
        }

        // Get the fallback default level = lowest impact level
        String minImpactLevel = impactLevels.get(0).getUri();

        // Get the system assets, and iterate over them creating their MisbehaviourSets
        Map<String,AssetDB> assets = querier.getAssets("system", "system-inf");
        for (AssetDB asset : assets.values()){
            Map<String, MisbehaviourSetDB> msThisAsset = msByAsset.computeIfAbsent(asset.getUri(), k -> new HashMap<>());
            LevelDB popLevel = poLevels.get(asset.getPopulation());
            String assetType = asset.getType();

            for(String avgMisbehaviour : dssavg.values()){
                // Check if the asset is a subtype of a TWA location - if yes, create a triplet
                if(dsassets.get(avgMisbehaviour).contains(assetType)){
                    // Create the average MS.
                    MADefaultSettingDB setting = querier.getMADefaultSetting(asset, avgMisbehaviour);
                    MisbehaviourSetDB savg = new MisbehaviourSetDB();
                    savg.setUri(querier.generateMisbehaviourSetUri(avgMisbehaviour, asset));
                    savg.setMisbehaviour(avgMisbehaviour);
                    savg.setLocatedAt(asset.getUri());

                    /* Put the default impact level in the inferred graph, whether or not there is a user/
                     * client specified impact level in the asserted graph. See note in issue #1364. */
                    if(setting != null) {
                        savg.setImpactLevel(setting.getLevel());
                    } else {
                        savg.setImpactLevel(minImpactLevel);
                    }

                    /* If the asset is non-singleton and the domain model specifies a full triplet is needed, create
                     * the other members. Note that new domain models with population support will have both the min 
                     * and max members (if required for this type of misbehaviour), old domain models will have neither.
                     * 
                     * The default impact levels for min and max likelihood MS is always the minimum possible level. 
                     */ 
                    if (dssmin.containsKey(avgMisbehaviour) && dssmax.containsKey(avgMisbehaviour) && popLevel.getLevelValue()>0){
                        // Create the minimum likelihood MS, with a minimum default impact level
                        String minMisbehaviour = dssmin.get(avgMisbehaviour);
                        MisbehaviourSetDB smin = new MisbehaviourSetDB();
                        smin.setUri(querier.generateMisbehaviourSetUri(minMisbehaviour, asset));
                        smin.setMisbehaviour(minMisbehaviour);
                        smin.setLocatedAt(asset.getUri());
                        smin.setMinOf(savg.getUri());
                        savg.setHasMin(smin.getUri());

                        /* Put the default impact level in the inferred graph, whether or not there is a user/
                         * client specified impact level in the asserted graph. See note in issue #1364. */
                        smin.setImpactLevel(minImpactLevel);

                        // Now save this MS in the map used to create threats, and via the querier
                        msThisAsset.put(smin.getMisbehaviour(), smin);
                        querier.store(smin, "system-inf");

                        // Create the maximum likelihood MS, with a minimum default impact level
                        String maxMisbehaviour = dssmax.get(avgMisbehaviour);
                        MisbehaviourSetDB smax = new MisbehaviourSetDB();
                        smax.setUri(querier.generateMisbehaviourSetUri(maxMisbehaviour, asset));
                        smax.setMisbehaviour(maxMisbehaviour);
                        smax.setLocatedAt(asset.getUri());
                        smax.setMaxOf(savg.getUri());
                        savg.setHasMax(smax.getUri());

                        /* Put the default impact level in the inferred graph, whether or not there is a user/
                         * client specified impact level in the asserted graph. See note in issue #1364. */
                        smax.setImpactLevel(minImpactLevel);

                        // Now save this MS in the map used to create threats, and via the querier
                        msThisAsset.put(smax.getMisbehaviour(), smax);
                        querier.store(smax, "system-inf");

                    }

                    // Now save the average MS (including hasMin and hasMax, if any) in the map used to create threats, and via the querier
                    msThisAsset.put(savg.getMisbehaviour(), savg);
                    querier.store(savg, "system-inf");

                }

            }

        }

        final long endTime = System.currentTimeMillis();
        logger.info("Validator.createMisbehavioursets(): execution time {} ms", endTime - startTime);
    
    }

    public void createTrustworthinessAttributeSets() {
        final long startTime = System.currentTimeMillis();

        // Get the domain model TWA types, and construct their triplets
        Map<String, TrustworthinessAttributeDB> dss = querier.getTrustworthinessAttributes("domain");
        Map<String, String> dssavg = new HashMap<>();
        Map<String, String> dssmin = new HashMap<>();
        Map<String, String> dssmax = new HashMap<>();
        for (TrustworthinessAttributeDB s : dss.values()){
            if((s.getMinOf() != null) && (s.getMaxOf() == null)){
                // This is the lowest level TW attribute
                dssmin.put(s.getMinOf(), s.getUri());
            } else if((s.getMinOf() == null) && (s.getMaxOf() != null)){
                // This is the highest level TW attribute
                dssmax.put(s.getMaxOf(), s.getUri());
            } else {
                // This must be the average level TW attribute
                dssavg.put(s.getUri(), s.getUri());
                if((s.getMinOf() != null) && (s.getMaxOf() != null)){
                    // If it is both highest and lowest, treat it as average but log an error
                    logger.warn("TWA {} is both a highest and lowest level TW attribute, which should be impossible", s.getUri());
                }    
            }
        }

        // Get the domain model TWA locations, and find all asset classes that should have the TWA
        Map<String, Set<String>> dsassets = new HashMap<>();
        for (String sUri : dssavg.values()){
            TrustworthinessAttributeDB s = dss.get(sUri);
            Set<String> dassetsByType = dsassets.computeIfAbsent(sUri, k -> new HashSet<>());
            Collection<String> dslocations = s.getMetaLocatedAt();
            for(String loc : dslocations){
                List<String> assetSubTypes = querier.getSubTypes(loc, true);
                for(String assetType : assetSubTypes){
                    dassetsByType.add(assetType);
                }
            }
        }

        // Get the system assets, and iterate over them creating their TWAS
        Map<String,AssetDB> assets = querier.getAssets("system", "system-inf");
        for (AssetDB asset : assets.values()){
            Map<String, TrustworthinessAttributeSetDB> twasThisAsset = twasByAsset.computeIfAbsent(asset.getUri(), k -> new HashMap<>());
            String assetType = asset.getType();
            LevelDB popLevel = poLevels.get(asset.getPopulation());
            for(String avgTWA : dssavg.values()){
                // Check if the asset is a subtype of a TWA location - if yes, create a triplet
                if(dsassets.get(avgTWA).contains(assetType)){
                    TWAADefaultSettingDB setting = querier.getTWAADefaultSetting(asset, avgTWA);
                    Integer defaultLevel = twLevels.size()-1;
                    Boolean independentLevels = false;
                    if (setting != null) {
                        // Old domain models have no levels, so check for that
                        if (setting.getLevel() != null) {
                            defaultLevel = twLevels.get(setting.getLevel()).getLevelValue();
                            independentLevels = setting.getIndependentLevels();
                        }
                    }

                    // Create the average TWAS, and get any properties defined in the asserted graph
                    String uri = querier.generateTrustworthinessAttributeSetUri(avgTWA, asset);
                    TrustworthinessAttributeSetDB savg = new TrustworthinessAttributeSetDB();
                    savg.setUri(uri);
                    savg.setTrustworthinessAttribute(avgTWA);
                    savg.setLocatedAt(asset.getUri());
                    TrustworthinessAttributeSetDB savgInput = querier.getTrustworthinessAttributeSet(savg.getUri(), "system");

                    /* If the asset is non-singleton and the domain model specifies a full triplet is needed, create
                     * the other members. Note that new domain models with population support will have both the min 
                     * and max members (if required for this type of TWA), old domain models will have neither.
                     */ 
                    if (dssmin.containsKey(avgTWA) && dssmax.containsKey(avgTWA) && popLevel.getLevelValue() > 0){
                        // Create the lowest level TWAS, and get any properties defined in the asserted graph
                        String minTWA = dssmin.get(avgTWA);
                        String uriMin = querier.generateTrustworthinessAttributeSetUri(minTWA, asset);
                        TrustworthinessAttributeSetDB smin = new TrustworthinessAttributeSetDB();
                        smin.setUri(uriMin);
                        smin.setTrustworthinessAttribute(minTWA);
                        smin.setLocatedAt(asset.getUri());
                        smin.setMinOf(savg.getUri());
                        savg.setHasMin(smin.getUri());
                        TrustworthinessAttributeSetDB sminInput = querier.getTrustworthinessAttributeSet(smin.getUri(), "system");
    
                        // Create the highest level TWAS, and get any properties defined in the asserted graph
                        String maxTWA = dssmax.get(avgTWA);
                        String uriMax = querier.generateTrustworthinessAttributeSetUri(maxTWA, asset);
                        TrustworthinessAttributeSetDB smax = new TrustworthinessAttributeSetDB();
                        smax.setUri(uriMax);
                        smax.setTrustworthinessAttribute(maxTWA);
                        smax.setLocatedAt(asset.getUri());
                        smax.setMaxOf(savg.getUri());
                        savg.setHasMax(smax.getUri());
                        TrustworthinessAttributeSetDB smaxInput = querier.getTrustworthinessAttributeSet(smax.getUri(), "system");

                        // Assemble asserted graph levels and proposed status into triplets of values
                        Integer[] assertedValues = {null, null, null};
                        if (sminInput != null && sminInput.getAssertedLevel() != null){
                            assertedValues[0] = twLevels.get(sminInput.getAssertedLevel()).getLevelValue();
                        }
                        if (savgInput != null && savgInput.getAssertedLevel() != null) {
                            assertedValues[1] = twLevels.get(savgInput.getAssertedLevel()).getLevelValue();
                        }
                        if (smaxInput != null && smaxInput.getAssertedLevel() != null) {
                            assertedValues[2] = twLevels.get(smaxInput.getAssertedLevel()).getLevelValue();
                        }

                        // Find a consistent set of levels obtained by adjusting the asserted graph levels
                        Integer[] adjustedValues = querier.getAdjustedLevels(asset, defaultLevel, assertedValues);

                        // Now compare the levels and update asserted graph values and/or set inferred graph values
                        LevelDB averageLevel = trustworthinessLevels.get(adjustedValues[1]); 
                        LevelDB inferredLevel = null;
                        LevelDB adjustedLevel = null;

                        if(assertedValues[0] != null) {
                            if(adjustedValues[0] != assertedValues[0]) {
                                // Need to modify the lowest level in the asserted graph
                                adjustedLevel = trustworthinessLevels.get(adjustedValues[0]); 
                                querier.updateAssertedLevel(adjustedLevel, smin, "system");
                            }
                        }
                        // Insert a calculated lowest value in the inferred graph
                        if(adjustedValues[0] == null)
                            adjustedValues[0] = querier.lookupLowestTWLevel(averageLevel, popLevel, independentLevels).getLevelValue();
                        inferredLevel = trustworthinessLevels.get(adjustedValues[0]); 
                        smin.setAssertedLevel(inferredLevel.getUri());

                        if(assertedValues[1] != null) {
                            if(adjustedValues[1] != assertedValues[1]) {
                                // Need to modify the average level in the asserted graph
                                adjustedLevel = trustworthinessLevels.get(adjustedValues[1]); 
                                querier.updateAssertedLevel(adjustedLevel, savg, "system");
                            }
                        }
                        // Insert a default or constrained average value in the inferred graph
                        if(adjustedValues[1] == null)
                            adjustedValues[1] = defaultLevel;
                        inferredLevel = trustworthinessLevels.get(adjustedValues[1]); 
                        savg.setAssertedLevel(inferredLevel.getUri());

                        if(assertedValues[2] != null) {
                            if(adjustedValues[2] != assertedValues[2]) {
                                // Need to modify the highest level in the asserted graph
                                adjustedLevel = trustworthinessLevels.get(adjustedValues[2]); 
                                querier.updateAssertedLevel(adjustedLevel, smax, "system");
                            }
                        } 
                        // Insert a calculated highest value in the inferred graph
                        if(adjustedValues[2] == null)
                            adjustedValues[2] = querier.lookupHighestTWLevel(averageLevel, popLevel, independentLevels).getLevelValue();
                        inferredLevel = trustworthinessLevels.get(adjustedValues[2]); 
                        smax.setAssertedLevel(inferredLevel.getUri());

                        // Store the new TWAS in the inferred graph, and save them in the map used later in threat creation
                        twasThisAsset.put(smin.getTrustworthinessAttribute(),smin);
                        querier.store(smin, "system-inf");
                        twasThisAsset.put(savg.getTrustworthinessAttribute(),savg);
                        querier.store(savg, "system-inf");
                        twasThisAsset.put(smax.getTrustworthinessAttribute(),smax);
                        querier.store(smax, "system-inf");

                    }
                    else {
                        // Just put the default level into the inferred graph
                        savg.setAssertedLevel(trustworthinessLevels.get(defaultLevel).getUri());

                        // Store the new TWAS in the inferred graph, and save them in the map used later in threat creation
                        twasThisAsset.put(savg.getTrustworthinessAttribute(),savg);
                        querier.store(savg, "system-inf");
                    }

                }

            }

        }

        final long endTime = System.currentTimeMillis();
        logger.info("Validator.createTrustworthinessAttributeSets(): execution time {} ms", endTime - startTime);

    }

    public void createTrustworthinessImpactSets() {
        Map<String, List<MisbehaviourSetDB>> mssByAsset = new HashMap<>();
        for (MisbehaviourSetDB ms : querier.getMisbehaviourSets("system-inf").values()) {
            List<MisbehaviourSetDB> mss = mssByAsset.computeIfAbsent(ms.getLocatedAt(), k -> new ArrayList<>());
            mss.add(ms);
        }
        Map<String, List<TrustworthinessAttributeSetDB>> twassByAsset = new HashMap<>();
        for (TrustworthinessAttributeSetDB twas : querier.getTrustworthinessAttributeSets("system-inf").values()) {
            List<TrustworthinessAttributeSetDB> twass = twassByAsset.computeIfAbsent(twas.getLocatedAt(), k -> new ArrayList<>());
            twass.add(twas);
        }

        Map<String, TrustworthinessImpactSetDB> twisByMsTwas = new HashMap<>();
        for (TrustworthinessImpactSetDB twis : querier.getTrustworthinessImpactSets("domain").values()) {
            twisByMsTwas.put(twis.getAffectedBy() + "-" + twis.getAffects(), twis);
        }

        Map<String, AssetDB> assets = querier.getAssets("system", "system-inf");
        for (AssetDB asset : assets.values()) {
            String assetUri = asset.getUri();
            for (MisbehaviourSetDB ms : mssByAsset.getOrDefault(assetUri, new ArrayList<>())) {
                for (TrustworthinessAttributeSetDB twas : twassByAsset.getOrDefault(assetUri, new ArrayList<>())) {
                    String key = ms.getMisbehaviour()+"-"+twas.getTrustworthinessAttribute();
                    TrustworthinessImpactSetDB twis = twisByMsTwas.get(key);
                    if (twis != null) {
                        String uri = String.format("system#TWIS-%s-%s-%s",
                                asset.getId(),
                                twis.getAffects().replace("domain#", ""),
                                twis.getAffectedBy().replace("domain#", ""));
                        TrustworthinessImpactSetDB infTwis = new TrustworthinessImpactSetDB();
                        infTwis.setUri(uri);
                        infTwis.setAffects(twas.getUri());
                        infTwis.setAffectedBy(ms.getUri());
                        querier.store(infTwis, "system-inf");
                    }
                }
            }
        }
    }

    public void createThreats() {

        final long startTime = System.currentTimeMillis();

        // Create a list of domain model matching patterns related to threats
        Set<String> threatMatchingPatterns = new HashSet<>();
        for (ThreatDB s : domainThreats.values()){
            threatMatchingPatterns.add(s.getAppliesTo());
        }

        // Generate system model threat matching patterns - unlike construction matching patterns these must be stored
        Map<String, List<MatchingPatternDB>> matchingPatternsByParent = new HashMap<>();
        for (String threatMatchingPatternUri : threatMatchingPatterns) {
            MatchingPatternDB domainMatchingPattern = querier.getMatchingPattern(threatMatchingPatternUri, "domain");
            List<MatchingPatternDB> systemMatchingPatterns = createAndStoreMatchingPatterns(domainMatchingPattern);
            matchingPatternsByParent.put(threatMatchingPatternUri, systemMatchingPatterns);
        }

        // Generate system model cardinality constraints organised by source asset URI
        Map<String, CardinalityConstraintDB> systemCCs = querier.getCardinalityConstraints("system", "system-inf");
        for(CardinalityConstraintDB cc : systemCCs.values()){
            String fromAsset = cc.getLinksFrom();
            List<CardinalityConstraintDB> linksFromAsset = linksFromAssets.computeIfAbsent(fromAsset, k -> new ArrayList<>());
            linksFromAsset.add(cc);
        }

        for (String domainThreatURI : domainThreats.keySet()) {
            // Get domain model parent
            ThreatDB domainThreat = domainThreats.get(domainThreatURI);

            // Get lists of roles used by that parent
            List<String> uniqueRoles = uniqueRolesByThreat.getOrDefault(domainThreat.getUri(), new ArrayList<>());
            List<String> necessaryRoles = necessaryRolesByThreat.getOrDefault(domainThreat.getUri(), new ArrayList<>());
            List<String> sufficientRoles = sufficientRolesByThreat.getOrDefault(domainThreat.getUri(), new ArrayList<>());

            // Loop over matches to the domain threat matching pattern
            List<MatchingPatternDB> matchingPatterns = matchingPatternsByParent.get(domainThreat.getAppliesTo());
            for (MatchingPatternDB threatMatchingPattern : matchingPatterns) {
                // Get the population size of the matching pattern
                Integer mpPopulation = poLevels.get(threatMatchingPattern.getPopulation()).getLevelValue();

                // Find the system assets involved in this threat pattern, organised by role  
                Map<String, List<String>> threatAssetsByRole = new HashMap<>();
                for (String nodeURI : threatMatchingPattern.getNodes()) {
                    NodeDB node = systemNodes.get(nodeURI);
                    List<String> assets = threatAssetsByRole.computeIfAbsent(node.getRole(), k -> new ArrayList<>());
                    assets.add(node.getSystemAsset());
                }

                // Find the threatened asset, i.e. the asset against which the threat is displayed by the SSM GUI
                /* 
                 * TODO : fix this, which wrongly assumes there will be only one threatened asset.
                 * 
                 * The domain model threat only specifies a threatened node (role), but this may be filled by
                 * multiple assets. Which one is saved here depends on how the querier decides which of several
                 * candidates should be returned. This is clearly not what should happen.
                 * 
                 * https://iglab.it-innovation.soton.ac.uk/Security/system-modeller/-/issues/1180#note_32525.
                 */
                String threatensAsset = null;
                for (String nodeURI : threatMatchingPattern.getNodes()) {
                    NodeDB node = querier.getNode(nodeURI, "system", "system-inf");
                    if (node.getRole().equals(domainThreat.getThreatens())) {
                        // Found a threatened asset, and we only need one, so we can skip the remaining nodes
                        threatensAsset = node.getSystemAsset();
                        break;
                    }
                }

                // Find assets matching non-unique roles that are really unique due to relationship constraints
                List<String> pseudorootAssets = querier.getPseudorootAssets(threatMatchingPattern);

                // Create the average likelihood threat, which should always exist
                ThreatDB systemThreatAvg = new ThreatDB();
                systemThreatAvg.setUri(String.format("%s-%s", domainThreat.getUri().replace("domain#", "system#"),
                                                threatMatchingPattern.getUri().replace("system#", "")));
                systemThreatAvg.setLabel(String.format("%s_%s", domainThreat.getUri().replace("domain#", ""), 
                                                threatMatchingPattern.getLabel()));
                systemThreatAvg.setDescription(generateDescription(domainThreat.getDescription(), threatMatchingPattern));
                systemThreatAvg.setParent(domainThreat.getUri());
                systemThreatAvg.setAppliesTo(threatMatchingPattern.getUri());
                systemThreatAvg.setThreatens(threatensAsset);
                if(domainThreat.isTriggered()) systemThreatAvg.setTriggered(true);
                // These need different handling for compliance threats with no effects
                if(domainThreat.getMisbehaviours().isEmpty()){
                    systemThreatAvg.setSecondaryThreat(false);
                    systemThreatAvg.setNormalOperation(false);    
                } else {
                    systemThreatAvg.setFrequency(domainThreat.getFrequency());
                    systemThreatAvg.setSecondaryThreat(domainThreat.isSecondaryThreat());
                    systemThreatAvg.setNormalOperation(domainThreat.isNormalOperation());    
                }

                // Create the minimum likelihood threat, if the domain model has one and the system pattern is a non-singleton
                ThreatDB systemThreatMin = null;
                if(domainThreat.getHasMin() != null && mpPopulation > 0) {
                    String domainThreatMinURI = domainThreat.getHasMin();
                    systemThreatMin = new ThreatDB();
                    systemThreatMin.setUri(String.format("%s-%s", domainThreatMinURI.replace("domain#", "system#"),
                                                    threatMatchingPattern.getUri().replace("system#", "")));
                    systemThreatMin.setLabel(String.format("%s_%s", domainThreatMinURI.replace("domain#", ""),
                                                    threatMatchingPattern.getLabel()));
                    systemThreatMin.setDescription(generateDescription(domainThreat.getDescription(), threatMatchingPattern));
                    systemThreatMin.setParent(domainThreat.getUri());
                    systemThreatMin.setAppliesTo(threatMatchingPattern.getUri());
                    systemThreatMin.setThreatens(threatensAsset);
                    if(domainThreat.isTriggered()) systemThreatMin.setTriggered(true);
                    // These need different handling for compliance threats with no effects
                    if(domainThreat.getMisbehaviours().isEmpty()){
                        systemThreatMin.setSecondaryThreat(false);
                        systemThreatMin.setNormalOperation(false);    
                    } else {
                        systemThreatMin.setFrequency(domainThreat.getFrequency());
                        systemThreatMin.setSecondaryThreat(domainThreat.isSecondaryThreat());
                        systemThreatMin.setNormalOperation(domainThreat.isNormalOperation());
                    }
                    systemThreatMin.setMinOf(systemThreatAvg.getUri());
                    systemThreatAvg.setHasMin(systemThreatMin.getUri());
                }

                // Create the maximum likelihood threat, if the domain model has one and the system pattern is a non-singleton
                ThreatDB systemThreatMax = null;
                if(domainThreat.getHasMax() != null && mpPopulation > 0) {
                    String domainThreatMaxURI = domainThreat.getHasMax();
                    systemThreatMax = new ThreatDB();
                    systemThreatMax.setUri(String.format("%s-%s", domainThreatMaxURI.replace("domain#", "system#"),
                                                    threatMatchingPattern.getUri().replace("system#", "")));
                    systemThreatMax.setLabel(String.format("%s_%s", domainThreatMaxURI.replace("domain#", ""),
                                                    threatMatchingPattern.getLabel()));
                    systemThreatMax.setDescription(generateDescription(domainThreat.getDescription(), threatMatchingPattern));
                    systemThreatMax.setParent(domainThreat.getUri());
                    systemThreatMax.setAppliesTo(threatMatchingPattern.getUri());
                    systemThreatMax.setThreatens(threatensAsset);
                    if(domainThreat.isTriggered()) systemThreatMax.setTriggered(true);
                    // These need different handling for compliance threats with no effects
                    if(domainThreat.getMisbehaviours().isEmpty()){
                        systemThreatMax.setSecondaryThreat(false);
                        systemThreatMax.setNormalOperation(false);    
                    } else {
                        systemThreatMax.setFrequency(domainThreat.getFrequency());
                        systemThreatMax.setSecondaryThreat(domainThreat.isSecondaryThreat());
                        systemThreatMax.setNormalOperation(domainThreat.isNormalOperation());    
                    }
                    systemThreatMax.setMaxOf(systemThreatAvg.getUri());
                    systemThreatAvg.setHasMax(systemThreatMax.getUri());
                }

                // Find the system MS caused by the threat
                for (String domainMSUri : domainThreat.getMisbehaviours()) {
                    MisbehaviourSetDB dms = domainMSs.get(domainMSUri);
                    String roleURI = dms.getLocatedAt();
                    String misbehaviourUri = dms.getMisbehaviour();

                    for (String assetURI : threatAssetsByRole.getOrDefault(roleURI, new ArrayList<>())) {
                        Map<String, MisbehaviourSetDB> msThisAsset = msByAsset.get(assetURI);
                        if(msThisAsset == null){
                            String message = String.format("Found no MS for any misbehaviour at asset %s which should be affected by threat %s, role %s, misbehaviour %s", 
                                assetURI, systemThreatAvg.getUri(), roleURI, misbehaviourUri);
                            throw new RuntimeException(message);
                        }
                        MisbehaviourSetDB msavg = msThisAsset.get(misbehaviourUri);
                        if(msavg == null){
                            String message = String.format("Found a null MS at asset %s which should be affected by threat %s, role %s, misbehaviour %s", 
                                                assetURI, systemThreatAvg.getUri(), roleURI, misbehaviourUri);
                            throw new RuntimeException(message);
                        }

                        // Now create the connections between these threats and effects
                        if(msavg.getHasMin() == null || msavg.getHasMax() == null) {
                            /* The MS is not a triplet, which means either:
                             * - the domain model is a pre-population model
                             * - the asset is a singleton
                             * Either way, the threat(s) must be connected to the average MS
                             */
                            systemThreatAvg.getMisbehaviours().add(msavg.getUri());
                            if(systemThreatMax != null) systemThreatMax.getMisbehaviours().add(msavg.getUri());
                            if(systemThreatMin != null) systemThreatMin.getMisbehaviours().add(msavg.getUri());
                        } else {
                            /* Non-singleton asset with a full MS triplet, so connect the min/avg/max likelihood
                             * threat to the min/avg/max likelihood effect
                             */
                            systemThreatAvg.getMisbehaviours().add(msavg.getUri());
                            if(systemThreatMax != null) systemThreatMax.getMisbehaviours().add(msavg.getHasMax());
                            if(systemThreatMin != null) systemThreatMin.getMisbehaviours().add(msavg.getHasMin());
                        }
                    }
                }

                // Find the system MS that are secondary effect causes of the threat
                for (String domainMSUri : domainThreat.getSecondaryEffectConditions()) {
                    MisbehaviourSetDB dms = domainMSs.get(domainMSUri);
                    String roleURI = dms.getLocatedAt();
                    String misbehaviourUri = dms.getMisbehaviour();

                    for (String assetURI : threatAssetsByRole.getOrDefault(roleURI, new ArrayList<>())) {
                        Map<String, MisbehaviourSetDB> msThisAsset = msByAsset.get(assetURI);
                        if(msThisAsset == null){
                            String message = String.format("Found no MS for any misbehaviour at asset %s which should cause threat %s, role %s, misbehaviour %s",
                                                assetURI, systemThreatAvg.getUri(), roleURI, misbehaviourUri);
                            throw new RuntimeException(message);
                        }
                        MisbehaviourSetDB msavg = msThisAsset.get(misbehaviourUri);
                        if(msavg == null){
                            String message = String.format("Found a null MS at asset %s which should cause threat %s, role %s, misbehaviour %s",
                                                assetURI, systemThreatAvg.getUri(), roleURI, misbehaviourUri);
                            throw new RuntimeException(message);
                        }

                        // Now create the connections between these secondary effect causes and the threats
                        if(msavg.getHasMin() == null || msavg.getHasMax() == null) {
                            /* The MS is not a triplet, which means either:
                             * - the domain model is a pre-population model
                             * - the asset is a singleton
                             * Either way, the threat(s) must be connected to the average MS
                             */
                            systemThreatAvg.getSecondaryEffectConditions().add(msavg.getUri());
                            if(systemThreatMax != null) systemThreatMax.getSecondaryEffectConditions().add(msavg.getUri());
                            if(systemThreatMin != null) systemThreatMin.getSecondaryEffectConditions().add(msavg.getUri());
                        }
                        else if(uniqueRoles.contains(roleURI) || pseudorootAssets.contains(assetURI)) {
                            /* Non-singleton asset with a full MS triplet matching a unique node, or with relationships
                             * with root node assets that mean it is unique within the threat, so wire the min/avg/max 
                             * likelihood threat to the min/avg/max likelihood cause.
                             */
                            systemThreatAvg.getSecondaryEffectConditions().add(msavg.getUri());
                            if(systemThreatMax != null) systemThreatMax.getSecondaryEffectConditions().add(msavg.getHasMax());
                            if(systemThreatMin != null) systemThreatMin.getSecondaryEffectConditions().add(msavg.getHasMin());
                        }
                        else if(necessaryRoles.contains(roleURI)) {
                            /* Non-singleton, non-pseudoroot asset with a full MS triplet matching a necessary node,
                             * so wire the min/avg/max likelihood threats to the minimum likelihood cause.
                             */
                            systemThreatAvg.getSecondaryEffectConditions().add(msavg.getHasMin());
                            if(systemThreatMax != null) systemThreatMax.getSecondaryEffectConditions().add(msavg.getHasMin());
                            if(systemThreatMin != null) systemThreatMin.getSecondaryEffectConditions().add(msavg.getHasMin());
                        }
                        else if(sufficientRoles.contains(roleURI)) {
                            /* Non-singleton non-pseudoroot asset with a full MS triplet matching a sufficient node,
                             * so wire the min/avg/max likelihood threats to the maximum likelihood cause.
                             */
                            systemThreatAvg.getSecondaryEffectConditions().add(msavg.getHasMax());
                            if(systemThreatMax != null) systemThreatMax.getSecondaryEffectConditions().add(msavg.getHasMax());
                            if(systemThreatMin != null) systemThreatMin.getSecondaryEffectConditions().add(msavg.getHasMax());
                        }
                        else {
                            // Node type cannot be determined, which is an error
                            String message = String.format("Found a secondary effect cause %s for threat %s at role %s which has no node type",
                                        misbehaviourUri, domainThreatURI, roleURI);
                            throw new RuntimeException(message);
                        }

                    }
                }

                // Find the system TWAS that are primary causes of the threat
                for (String domainTWASUri : domainThreat.getEntryPoints()) {
                    TrustworthinessAttributeSetDB dtwas = domainTWASs.get(domainTWASUri);
                    String roleURI = dtwas.getLocatedAt();
                    String twaUri = dtwas.getTrustworthinessAttribute();

                    for (String assetURI : threatAssetsByRole.getOrDefault(roleURI, new ArrayList<>())) {
                        Map<String, TrustworthinessAttributeSetDB> twasThisAsset = twasByAsset.get(assetURI);
                        if(twasThisAsset == null){
                            String message = String.format("Found no TWAS for any attribute at asset %s which should cause threat %s, role %s, twa %s", 
                                        assetURI, systemThreatAvg.getUri(), roleURI, twaUri);
                            throw new RuntimeException(message);
                        }
                        TrustworthinessAttributeSetDB twasavg = twasThisAsset.get(twaUri);
                        if(twasavg == null){
                            String message = String.format("Found a null TWAS at asset %s which should cause threat %s, role %s, twa %s", 
                                        assetURI, systemThreatAvg.getUri(), roleURI, twaUri);
                            throw new RuntimeException(message);
                        }

                        // Now create the connections between the primary threat cause(s) and the threat(s)
                        if(twasavg.getHasMin() == null || twasavg.getHasMax() == null) {
                            /* The TWAS is not a triplet, which means either:
                             * - the domain model is a pre-population model
                             * - the asset is a singleton
                             * Either way, the threat(s) must be connected to the average TWAS
                             */
                            systemThreatAvg.getEntryPoints().add(twasavg.getUri());
                            if(systemThreatMax != null) systemThreatMax.getEntryPoints().add(twasavg.getUri());
                            if(systemThreatMin != null) systemThreatMin.getEntryPoints().add(twasavg.getUri());
                        }
                        else if(uniqueRoles.contains(roleURI) || pseudorootAssets.contains(assetURI)) {
                            /* Non-singleton asset with a full TWAS triplet matching a unique node, or with relationships
                             * with root node assets that mean it is unique within the threat, so wire the min/avg/max 
                             * likelihood threat to the min/avg/max likelihood cause.
                             */
                            systemThreatAvg.getEntryPoints().add(twasavg.getUri());
                            if(systemThreatMax != null) systemThreatMax.getEntryPoints().add(twasavg.getHasMin());
                            if(systemThreatMin != null) systemThreatMin.getEntryPoints().add(twasavg.getHasMax());
                        }
                        else if(necessaryRoles.contains(roleURI)) {
                            /* Non-singleton, non-pseudoroot asset with a full TWAS triplet matching a necessary node,
                             * so wire the min/avg/max likelihood threats to the minimum likelihood cause.
                             */
                            systemThreatAvg.getEntryPoints().add(twasavg.getHasMax());
                            if(systemThreatMax != null) systemThreatMax.getEntryPoints().add(twasavg.getHasMax());
                            if(systemThreatMin != null) systemThreatMin.getEntryPoints().add(twasavg.getHasMax());
                        }
                        else if(sufficientRoles.contains(roleURI)) {
                            /* Non-singleton non-pseudoroot asset with a full TWAS triplet matching a sufficient node,
                             * so wire the min/avg/max likelihood threats to the maximum likelihood cause.
                             */
                            systemThreatAvg.getEntryPoints().add(twasavg.getHasMin());
                            if(systemThreatMax != null) systemThreatMax.getEntryPoints().add(twasavg.getHasMin());
                            if(systemThreatMin != null) systemThreatMin.getEntryPoints().add(twasavg.getHasMin());
                        }
                        else {
                            // Node type cannot be determined, which is an error
                            String message = String.format("Found a primary effect cause %s for threat %s at role %s which has no node type",
                                        twaUri, domainThreatURI, roleURI);
                            throw new RuntimeException(message);
                        }
                        
                    }
                }

                // Add the threats to the system threat map
                systemThreats.put(systemThreatAvg.getUri(), systemThreatAvg);
                if(systemThreatMax != null) systemThreats.put(systemThreatMax.getUri(), systemThreatMax);
                if(systemThreatMin != null) systemThreats.put(systemThreatMin.getUri(), systemThreatMin);

                /*
                 * But don't store the threat in the inferred graph until we can check if the threat is a
                 * triggered threat with no trigger CSGs. That is done in the CSG creation method.
                 */
            }
        }

        final long endTime = System.currentTimeMillis();
        logger.info("Validator.createThreats(): execution time {} ms", endTime - startTime);

    }

    public void createControlStrategies() {

        final long startTime = System.currentTimeMillis();

        for (ThreatDB threatAvg : systemThreats.values()) {
            // Other members of the threat triplet
            ThreatDB threatMax = null;
            ThreatDB threatMin = null;
            
            // Always start from the average likelihood threat
            if(threatAvg.getMinOf() == null && threatAvg.getMaxOf() == null) {
                // Find the other members of the triplet
                threatMax = systemThreats.get(threatAvg.getHasMax());
                threatMin = systemThreats.get(threatAvg.getHasMin());

                // Get the system model matching pattern, and a map of its nodes organised by role
                MatchingPatternDB matchingPattern = querier.getMatchingPattern(threatAvg.getAppliesTo(), "system-inf");
                Map<String, List<String>> nodesByRole = querier.getMatchingPatternNodes(matchingPattern.getUri());

                // Find assets matching non-unique roles that are really unique due to relationship constraints
                List<String> pseudorootAssets = querier.getPseudorootAssets(matchingPattern);

                // Find the domain model parent threat type and role lists
                ThreatDB dthreat = domainThreats.get(threatAvg.getParent());
                List<String> uniqueRoles = uniqueRolesByThreat.getOrDefault(dthreat.getUri(), new ArrayList<>());
                List<String> optionalRoles = optionalRolesByThreat.getOrDefault(dthreat.getUri(), new ArrayList<>());
                List<String> sufficientRoles = sufficientRolesByThreat.getOrDefault(dthreat.getUri(), new ArrayList<>());
                List<String> necessaryRoles = necessaryRolesByThreat.getOrDefault(dthreat.getUri(), new ArrayList<>());

                // Get all related domain model CSGs
                Set<ControlStrategyDB> dcsgs = threatCSGs.getOrDefault(threatAvg.getParent(), new HashSet<>());
                for(ControlStrategyDB dcsg : dcsgs) {
                    // Declare the CSG triplet here so it is in scope throughout what follows
                    ControlStrategyDB controlStrategyAvg = null;
                    ControlStrategyDB controlStrategyMax = null;
                    ControlStrategyDB controlStrategyMin = null;

                    // Create the CSG(s) and set some properties but not yet the URI
                    controlStrategyAvg = new ControlStrategyDB();
                    controlStrategyAvg.setParent(dcsg.getUri());
                    controlStrategyAvg.setDescription(generateDescription(dcsg.getDescription(), matchingPattern));
                    if(threatMax != null) {
                        controlStrategyMax = new ControlStrategyDB();
                        controlStrategyMax.setParent(dcsg.getUri());
                        controlStrategyMax.setDescription(generateDescription(dcsg.getDescription(), matchingPattern));
                    }
                    if(threatMin != null) {
                        controlStrategyMin = new ControlStrategyDB();
                        controlStrategyMin.setParent(dcsg.getUri());
                        controlStrategyMin.setDescription(generateDescription(dcsg.getDescription(), matchingPattern));    
                    }

                    // Assemble a complete list of domain CS to be found, with a deterministic ordering
                    List<String> allCS = new ArrayList<>();
                    List<String> optionalCS = dcsg.getOptionalCS();
                    List<String> mandatoryCS = dcsg.getMandatoryCS();
                    if(mandatoryCS.isEmpty() && optionalCS.isEmpty()) {
                        // This is an old domain model with no population support
                        mandatoryCS = dcsg.getControlSets();
                    }
                    allCS.addAll(optionalCS);
                    allCS.addAll(mandatoryCS);

                    // Initialise flags that track which types of CS were found or not found
                    Boolean foundOptionalCS = false;
                    Boolean foundMandatoryCS = false;
                    Boolean missingMandatoryCS = false;

                    for (String dcsUri : allCS) {
                        // Get the domanin model CS
                        ControlSetDB dcs = querier.getControlSet(dcsUri, "domain");
                        String roleURI = dcs.getLocatedAt();

                        // Determine whether this CS is optional or mandatory
                        Boolean optional = optionalCS.contains(dcsUri);

                        // Find system CS at assets in the role (in this pattern) specified in the domain model CS
                        for(String nodeUri : nodesByRole.getOrDefault(roleURI, new ArrayList<>())){
                            // Get the node and asset
                            NodeDB node = querier.getNode(nodeUri, "system-inf");
                            String assetURI = node.getSystemAsset();
                            AssetDB asset = querier.getAsset(assetURI, "system", "system-inf");

                            // Find the system CS
                            String controlSetUri = querier.generateControlSetUri(dcs.getControl(), asset);
                            ControlSetDB csavg = querier.getAvgCS(controlSetUri, "system-inf");
                            if(csavg != null) {
                                // We have a control set
                                if(optional){
                                    // There is at least one optional CS
                                    foundOptionalCS = true;
                                } else {
                                    // There is at least one mandatory CS
                                    foundMandatoryCS = true;
                                }

                                // Now create the connections between these CS and CSGs
                                if(csavg.getHasMax() == null || csavg.getHasMin() == null) {
                                    /* The CS is not a triplet, which means either:
                                    * - the domain model is a pre-population model
                                    * - the asset is a singleton
                                    * Either way, the CSG(s) must be connected to the average CS
                                    */
                                    if(optional){
                                        controlStrategyAvg.getOptionalCS().add(csavg.getUri());
                                        if(controlStrategyMax != null) controlStrategyMax.getOptionalCS().add(csavg.getUri());
                                        if(controlStrategyMin != null) controlStrategyMin.getOptionalCS().add(csavg.getUri());    
                                    } else {
                                        controlStrategyAvg.getMandatoryCS().add(csavg.getUri());
                                        if(controlStrategyMax != null) controlStrategyMax.getMandatoryCS().add(csavg.getUri());
                                        if(controlStrategyMin != null) controlStrategyMin.getMandatoryCS().add(csavg.getUri());
                                    }
                                }
                                else if(uniqueRoles.contains(roleURI) || optionalRoles.contains(roleURI) || pseudorootAssets.contains(assetURI)) {
                                    /* Asset is a non-singleton but matches a unique or optional node, so the min CSG should
                                        * use the max CS and vice versa.
                                        * 
                                        * Or the asset is a non-singleton and matches a mandatory, non-unique role, but in this
                                        * case the asset must be unique within the threat due to the cardinality of relns, so
                                        * treat it as though it matched a unique node.
                                        * See https://iglab.it-innovation.soton.ac.uk/Security/system-modeller/-/issues/1362
                                        */
                                    if(optional){
                                        controlStrategyAvg.getOptionalCS().add(csavg.getUri());
                                        if(controlStrategyMax != null) controlStrategyMax.getOptionalCS().add(csavg.getHasMin());
                                        if(controlStrategyMin != null) controlStrategyMin.getOptionalCS().add(csavg.getHasMax());
                                    } else {
                                        controlStrategyAvg.getMandatoryCS().add(csavg.getUri());
                                        if(controlStrategyMax != null) controlStrategyMax.getMandatoryCS().add(csavg.getHasMin());
                                        if(controlStrategyMin != null) controlStrategyMin.getMandatoryCS().add(csavg.getHasMax());
                                    }
                                }
                                else if(sufficientRoles.contains(roleURI)) {
                                    /* Asset is a non-singleton and matches a sufficient role, so would use the min CS in all
                                        * three variants.
                                        * 
                                        * Note that if this is a trigger the risk calculator will flip to the max CS, as discussed
                                        * in:
                                        * 
                                        * https://iglab.it-innovation.soton.ac.uk/Security/system-modeller/-/issues/1360#note_32462.
                                        */
                                    if(optional){
                                        controlStrategyAvg.getOptionalCS().add(csavg.getHasMin());
                                        if(controlStrategyMax != null) controlStrategyMax.getOptionalCS().add(csavg.getHasMin());
                                        if(controlStrategyMin != null) controlStrategyMin.getOptionalCS().add(csavg.getHasMin());    
                                    } else {
                                        controlStrategyAvg.getMandatoryCS().add(csavg.getHasMin());
                                        if(controlStrategyMax != null) controlStrategyMax.getMandatoryCS().add(csavg.getHasMin());
                                        if(controlStrategyMin != null) controlStrategyMin.getMandatoryCS().add(csavg.getHasMin());
                                    }
                                }
                                else if(necessaryRoles.contains(roleURI)) {
                                    /* Asset is a non-singleton and matches a necessary role, so would use the max CS in all
                                        * three variants.
                                        * 
                                        * Note that if this is a trigger the risk calculator will flip to the min CS, as discussed
                                        * in:
                                        * 
                                        * https://iglab.it-innovation.soton.ac.uk/Security/system-modeller/-/issues/1360#note_32462.
                                        */
                                    if(optional){
                                        controlStrategyAvg.getOptionalCS().add(csavg.getHasMax());
                                        if(controlStrategyMax != null) controlStrategyMax.getOptionalCS().add(csavg.getHasMax());
                                        if(controlStrategyMin != null) controlStrategyMin.getOptionalCS().add(csavg.getHasMax());
                                    } else {
                                        controlStrategyAvg.getMandatoryCS().add(csavg.getHasMax());
                                        if(controlStrategyMax != null) controlStrategyMax.getMandatoryCS().add(csavg.getHasMax());
                                        if(controlStrategyMin != null) controlStrategyMin.getMandatoryCS().add(csavg.getHasMax());
                                    }
                                }
                                else {
                                    // Node type cannot be determined, which is an error
                                    String message = String.format("Control %s at role %s needed in CSG %s, but in threat %s there is no valid role type",
                                                csavg.getControl(), roleURI, dcsg.getUri(), threatAvg.getUri());
                                    throw new RuntimeException(message);
                                }

                            } else {
                                // This asset lacks a control
                                if(!optional){
                                    // The missing control is mandatory, so we can skip the remaining assets
                                    missingMandatoryCS = true;
                                    break;
                                }
                            }

                        }

                        if(missingMandatoryCS){
                            // No point looking at other CS if we're already missing a mandatory CS
                            break; 
                        }

                    }

                    // We have now checked all the CS. The CSG is good iff some CS were found and no mandatory CS were missing
                    if(!missingMandatoryCS && (foundMandatoryCS || foundOptionalCS)) {

                        // Set the URI for the control strategies and check if we already created it
                        if(controlStrategyAvg != null) {
                            String csgURI = getControlStrategyUri(controlStrategyAvg);
                            if(systemCSGs.containsKey(csgURI)) {
                                // We already created this CSG, so switch to that
                                controlStrategyAvg = systemCSGs.get(csgURI);
                            } else {
                                // We haven't yet created this CSG, so set its URI and add it to the list
                                controlStrategyAvg.setUri(csgURI);
                                systemCSGs.put(controlStrategyAvg.getUri(), controlStrategyAvg);
                            }
                        }

                        if(controlStrategyMax != null) {
                            String csgURI = getControlStrategyUri(controlStrategyMax);
                            if(systemCSGs.containsKey(csgURI)) {
                                // We already created this CSG, so switch to that
                                controlStrategyMax = systemCSGs.get(csgURI);
                            } else {
                                // We haven't yet created this CSG, so set its URI and add it to the list
                                controlStrategyMax.setUri(csgURI);
                                systemCSGs.put(controlStrategyMax.getUri(), controlStrategyMax);
                            }
                        }

                        if(controlStrategyMin != null) {
                            String csgURI = getControlStrategyUri(controlStrategyMin);
                            if(systemCSGs.containsKey(csgURI)) {
                                // We already created this CSG, so switch to that
                                controlStrategyMin = systemCSGs.get(csgURI);
                            } else {
                                // We haven't yet created this CSG, so set its URI and add it to the list
                                controlStrategyMin.setUri(csgURI);
                                systemCSGs.put(controlStrategyMin.getUri(), controlStrategyMin);
                            }
                        }

                        // Add blocks relationships to and from the threats
                        if(dcsg.getBlocks().contains(threatAvg.getParent())) {
                            controlStrategyAvg.getBlocks().add(threatAvg.getUri());
                            threatAvg.getBlockedByCSG().add(controlStrategyAvg.getUri()); 
                            if(controlStrategyMax != null && threatMax != null) {
                                controlStrategyMax.getBlocks().add(threatMax.getUri());
                                threatMax.getBlockedByCSG().add(controlStrategyMax.getUri()); 
                            }
                            if(controlStrategyMin != null && threatMin != null) {
                                controlStrategyMin.getBlocks().add(threatMin.getUri());
                                threatMin.getBlockedByCSG().add(controlStrategyMin.getUri()); 
                            }
                        }

                        // Add mitigates relationships to and from the threats
                        if(dcsg.getMitigates().contains(threatAvg.getParent())) {
                            controlStrategyAvg.getMitigates().add(threatAvg.getUri());
                            threatAvg.getMitigatedByCSG().add(controlStrategyAvg.getUri()); 
                            if(controlStrategyMax != null && threatMax != null) {
                                controlStrategyMax.getMitigates().add(threatMax.getUri());
                                threatMax.getMitigatedByCSG().add(controlStrategyMax.getUri()); 
                            }
                            if(controlStrategyMin != null && threatMin != null) {
                                controlStrategyMin.getMitigates().add(threatMin.getUri());
                                threatMin.getMitigatedByCSG().add(controlStrategyMin.getUri()); 
                            }
                        }

                        // Add triggers relationships to and from the threats (Min to Max and vice versa)
                        if(dcsg.getTriggers().contains(threatAvg.getParent())) {
                            controlStrategyAvg.getTriggers().add(threatAvg.getUri());
                            threatAvg.getTriggeredByCSG().add(controlStrategyAvg.getUri());
                            if(controlStrategyMin != null && threatMax != null) {
                                controlStrategyMin.getTriggers().add(threatMax.getUri());
                                threatMax.getTriggeredByCSG().add(controlStrategyMin.getUri());                
                            }
                            if(controlStrategyMax != null && threatMin != null) {
                                controlStrategyMax.getTriggers().add(threatMin.getUri());
                                threatMin.getTriggeredByCSG().add(controlStrategyMax.getUri());                
                            }
                        }

                        /* Don't save the CSGs to the inferred graph yet. We want to save them if the threat
                         * is non-triggered or triggered, but not if it is untriggerable (a triggered threat
                         * that has no triggering CSGs).
                         */
                    }                    

                }

            }

            // At this point it is possible to work out whether the threat is untriggered
            if(!threatAvg.isTriggered() || !threatAvg.getTriggeredByCSG().isEmpty()) {
                // If the avg threat is non-triggered or has a triggering CSG, we keep it and its CSGs
                querier.store(threatAvg, "system-inf");
                for(String csgURI : threatAvg.getBlockedByCSG())
                    querier.store(systemCSGs.get(csgURI), "system-inf");
                for(String csgURI : threatAvg.getMitigatedByCSG()) 
                    querier.store(systemCSGs.get(csgURI), "system-inf");                    
                for(String csgURI : threatAvg.getTriggeredByCSG()) 
                    querier.store(systemCSGs.get(csgURI), "system-inf");
            } else {
                // If it is a triggered threat but it has no triggers, we just unlink it from blocking CSGs
                for(String csgURI : threatAvg.getBlockedByCSG()) {
                    ControlStrategyDB csg = systemCSGs.get(csgURI);
                    csg.getBlocks().remove(threatAvg.getUri());
                }
                for(String csgURI : threatAvg.getMitigatedByCSG()) {
                    ControlStrategyDB csg = systemCSGs.get(csgURI);
                    csg.getMitigates().remove(threatAvg.getUri());
                }
            }

            // Similarly for the min and max threats, if they exist
            if(threatMin != null) {
                if(!threatMin.isTriggered() || !threatMin.getTriggeredByCSG().isEmpty()) {
                    querier.store(threatMin, "system-inf");
                    for(String csgURI : threatMin.getBlockedByCSG())
                        querier.store(systemCSGs.get(csgURI), "system-inf");
                    for(String csgURI : threatMin.getMitigatedByCSG()) 
                        querier.store(systemCSGs.get(csgURI), "system-inf");                    
                    for(String csgURI : threatMin.getTriggeredByCSG()) 
                        querier.store(systemCSGs.get(csgURI), "system-inf");
                } else {
                    // If it is a triggered threat but it has no triggers, we just unlink it from blocking CSGs
                    for(String csgURI : threatMin.getBlockedByCSG()) {
                        ControlStrategyDB csg = systemCSGs.get(csgURI);
                        csg.getBlocks().remove(threatMin.getUri());
                    }
                    for(String csgURI : threatMin.getMitigatedByCSG()) {
                        ControlStrategyDB csg = systemCSGs.get(csgURI);
                        csg.getMitigates().remove(threatMin.getUri());
                    }
                }
            }
            
            if(threatMax != null) {
                if(!threatMax.isTriggered() || !threatMax.getTriggeredByCSG().isEmpty()) {
                    querier.store(threatMax, "system-inf");
                    for(String csgURI : threatMax.getBlockedByCSG())
                        querier.store(systemCSGs.get(csgURI), "system-inf");
                    for(String csgURI : threatMax.getMitigatedByCSG()) 
                        querier.store(systemCSGs.get(csgURI), "system-inf");                    
                    for(String csgURI : threatMax.getTriggeredByCSG()) 
                        querier.store(systemCSGs.get(csgURI), "system-inf");
                } else {
                    // If it is a triggered threat but it has no triggers, we just unlink it from blocking CSGs
                    for(String csgURI : threatMax.getBlockedByCSG()) {
                        ControlStrategyDB csg = systemCSGs.get(csgURI);
                        csg.getBlocks().remove(threatMax.getUri());
                    }
                    for(String csgURI : threatMax.getMitigatedByCSG()) {
                        ControlStrategyDB csg = systemCSGs.get(csgURI);
                        csg.getMitigates().remove(threatMax.getUri());
                    }
                }
            }

        }

        final long endTime = System.currentTimeMillis();
        logger.info("Validator.createControlStrategies(): execution time {} ms", endTime - startTime);

    }

	/* Method to compute the URI of a CSG based on its type and control sets
	 * Do not use until the control sets have been found and added to the
	 * mandatory and optional CS lists.
	 */
    private String getControlStrategyUri(ControlStrategyDB csg) {
        // Bale if the argument is not a CSG
        if(csg == null) return null;

        // Start the URI using the parent CSG
        String uri = csg.getParent().replace("domain#", "system#");

        // Append the IDs of mandatory control sets, sorted for reproducibility
        if(!csg.getMandatoryCS().isEmpty()) {
            List<String> cslist = new ArrayList<>();
            cslist.addAll(csg.getMandatoryCS());
            Collections.sort(cslist);
            uri = uri.concat("_M");
            for(String csURI : cslist){
                ControlSetDB cs = querier.getControlSet(csURI, "system-inf");
                uri = uri.concat("_").concat(cs.generateID());    
            }
        }

        // Append the IDs of optional control sets, sorted for reproducibility
        if(!csg.getOptionalCS().isEmpty()) {
            List<String> cslist = new ArrayList<>();
            cslist.addAll(csg.getOptionalCS());
            Collections.sort(cslist);
            uri = uri.concat("_O");
            for(String csURI : cslist){
                ControlSetDB cs = querier.getControlSet(csURI, "system-inf");
                uri = uri.concat("_").concat(cs.generateID());    
            }
        }

		return uri;

	}
    
    private String generateDescription(String description, MatchingPatternDB matchingPattern) {
        if (description == null) {
            return null;
        }

        Map<String, List<String>> substitutions = new HashMap<>();
        List<String> nonUniqueRoles = new ArrayList<>();

        // Iterate through the nodes creating lists of asset names for each role label to be substituted
        for (String nodeUri : matchingPattern.getNodes()) {
            NodeDB node = querier.getNode(nodeUri, "system", "system-inf");
            RoleDB role = domainRoles.get(node.getRole());
            String roleLabel = role.getLabel();
            String assetLabel = querier.getAsset(node.getSystemAsset(), "system", "system-inf").getLabel();

            String replaceString = roleLabel;
            if (!(replaceString.startsWith("_") && replaceString.endsWith("_"))) {
                replaceString = "_" + replaceString + "_";
            }
            if(description.contains(replaceString)){
                List<String> assetLabels = substitutions.computeIfAbsent(roleLabel, k -> new ArrayList<>());
                assetLabels.add(assetLabel);
                if(!matchingPattern.getUniqueNodes().contains(nodeUri) && !nonUniqueRoles.contains(roleLabel)){
                    nonUniqueRoles.add(roleLabel);
                }
            }
        }

        // Now iterate through the lists of asset names to create and apply the substitutions
        for(String roleLabel : substitutions.keySet()){
            List<String> assetLabels = substitutions.get(roleLabel);
            Collections.sort(assetLabels);

            String replaceString = roleLabel;
            if (!(replaceString.startsWith("_") && replaceString.endsWith("_"))) {
                replaceString = "_" + replaceString + "_";
            }

            String substitution;
            if(nonUniqueRoles.contains(roleLabel)){
                substitution = "{";
            } else {
                substitution = "";
            }

            int i = 0;
            for(String assetLabel : assetLabels){
                if(assetLabels.size() > 1 && i == assetLabels.size() - 1) {
                    substitution = substitution.concat(" or \"".concat(assetLabel.concat("\"")));
                }
                else if(assetLabels.size() > 1 && i > 0) {
                    substitution = substitution.concat(", \"".concat(assetLabel.concat("\"")));
                }
                else {
                    substitution = substitution.concat("\"".concat(assetLabel.concat("\"")));
                }
                i++;
            }

            if(nonUniqueRoles.contains(roleLabel)){
                substitution = substitution.concat("}");
            }

            description = description.replaceAll(replaceString, substitution);

        }

        return description;
    }

    /** Execute each construction pattern defined in the domain model (in order of priority).
     */
    public void executeConstructionPatterns() {
        final long startTime = System.currentTimeMillis();

        Map<String, ConstructionPatternDB> constructionPatterns = querier.getConstructionPatterns("domain");

        // Sort construction patterns by their priority (lower value => higher priority)
        List<ConstructionPatternDB> orderedConstructionPatterns = new ArrayList<>(constructionPatterns.values());
        orderedConstructionPatterns.sort(Comparator.comparing(ConstructionPatternDB::getPriority));

        for (ConstructionPatternDB constructionPattern : orderedConstructionPatterns) {
            executeConstructionPattern(constructionPattern);
        }

        final long endTime = System.currentTimeMillis();
        logger.info("Validator.executeConstructionPatterns(): execution time {} ms", endTime - startTime);        

    }

    /** Execute the given `constructionPattern`: find all matches of its matching pattern in
     *  the system graph, and for each match create the construction pattern's specified assets
     *  and asset links.
     */
    private void executeConstructionPattern(ConstructionPatternDB constructionPattern) {
        logger.debug("Creating construction pattern {}", constructionPattern.getUri());

        MatchingPatternDB matchingPattern = querier.getMatchingPattern(constructionPattern.getMatchingPattern(), "domain");
        GraphPattern graphPattern = matchingPatternToGraphPattern(matchingPattern);

        // Map inferred nodes to their inferred node settings (if one exists)
        Map<String, InferredNodeSettingDB> nodeInferredSetting = new HashMap<>();
        for (String inferredNodeSettingUri : constructionPattern.getInferredNodeSettings()) {
            InferredNodeSettingDB inferredNodeSetting = querier.getInferredNodeSetting(inferredNodeSettingUri, "domain");
            nodeInferredSetting.put(inferredNodeSetting.getNode(), inferredNodeSetting);
        }

        // Keep executing the construction pattern until either:
        //   a) no new assets and links are created
        //   b) the construction pattern's maximum number of iterations are exceeded
        boolean iterate;
        int remainingIterations = constructionPattern.getMaxIterations();
        do {
            remainingIterations--;
            iterate = false;

            List<GraphMatchedPattern> matchedPatterns = matchPattern(graphPattern);
            for (GraphMatchedPattern matchedPattern : matchedPatterns) {
                List<InferredAssetDB> inferredAssets = getInferredNodesForConstructionMatch(matchedPattern, graphPattern, constructionPattern, nodeInferredSetting);
                for (InferredAssetDB inferredAsset : inferredAssets) {
                    querier.store(inferredAsset, "system-inf");
                }

                List<CardinalityConstraintDB> inferredLinks = getInferredLinksForConstructionMatch(matchedPattern, graphPattern, constructionPattern);
                for (CardinalityConstraintDB link : inferredLinks) {
                    calculateRelationCardinalityConstraint(link);
                    querier.store(link, "system-inf");
                }

                // Keep iterating if the CP caused an update to the graph
                if (!inferredAssets.isEmpty() || !inferredLinks.isEmpty()) {
                    iterate = constructionPattern.getIterate();
                }
            }
        } while (iterate && remainingIterations > 0);
    }

    /** Calculates and stores the cardinality constraints for all asserted relationships.
     */
    public void calculateAssertedCardinalityConstraints() {
        final long startTime = System.currentTimeMillis();

        Map<String, CardinalityConstraintDB> ccs = querier.getCardinalityConstraints("system");
        for (CardinalityConstraintDB cc : ccs.values()) {
            AssetDB fromAsset = querier.getAsset(cc.getLinksFrom(), "system");
            AssetDB toAsset = querier.getAsset(cc.getLinksTo(), "system");
            if(fromAsset != null && toAsset != null) {
                // calculate the cardinality constraints
                calculateRelationCardinalityConstraint(cc);
                querier.store(cc, "system-inf");
            } else {
                logger.warn(String.format("Orphaned CardinalityConstraint found: [%s]-[%s]-[%s]", cc.getLinksFrom(), cc.getLinkType(), cc.getLinksTo()));
            }
        }

        final long endTime = System.currentTimeMillis();
        logger.info("Validator.calculateAssertedCardinalityConstraints(): execution time {} ms", endTime - startTime);        
    }

    /** public List<CardinalityConstraintDB> getInferredNodesForConstructionMatch
     *   @param matchedPattern: representation of a system model matching pattern that matches graph pattern
     *   @param graphPattern: representation of the domain model matching pattern
     *   @param constructionPattern: the domain model construction pattern
     *   @param nodeInferredSetting: the inferred node settings for this construction pattern
     */
    private List<InferredAssetDB> getInferredNodesForConstructionMatch(GraphMatchedPattern matchedPattern, 
                                            GraphPattern graphPattern, ConstructionPatternDB constructionPattern,
                                            Map<String, InferredNodeSettingDB> nodeInferredSetting) {
        // TODO: Extract side effects from method (?)

        Map<String, Set<String>> assetRoles = getAssetRoles();

        List<InferredAssetDB> inferredAssets = new ArrayList<>();
        for (String inferredNodeUri : constructionPattern.getInferredNodes()) {
            NodeDB inferredNodeTemplate = querier.getNode(inferredNodeUri, "domain");
            AssetDB inferredAssetTemplate = querier.getAsset(inferredNodeTemplate.getAsset(), "domain");
            InferredNodeSettingDB inferredNodeSetting = nodeInferredSetting.get(inferredNodeUri);

            // Create a representation of the newly inferred asset, now including its cardinality based on INS includes
            InferredAssetDB inferredAsset = getInferredAssetBase(inferredNodeSetting, inferredAssetTemplate, matchedPattern);

            matchedPattern.addRoleAsset(inferredNodeTemplate.getRole(), inferredAsset.getUri());

            // Check if inferred asset already exists
            AssetDB existingAsset = querier.getAsset(inferredAsset.getUri(), "system-inf");
            if (existingAsset != null) {
                continue;
            }

            // Inferred asset will have all the roles of its asset type AND its parent asset types
            Set<String> allRoles = new HashSet<>();
            List<String> types = querier.getSuperTypes(inferredAsset.getType(), true);
            for (String type : types) {
                Set<String> roles = assetRoles.get(type);
                if (roles != null) {
                    allRoles.addAll(roles);
                }
            }

            //GraphNode node = new GraphNode(inferredAsset.getUri(), assetRoles.get(inferredAsset.getType()));
            GraphNode node = new GraphNode(inferredAsset.getUri(), allRoles);

            // Combine created asset with any links from the node which may exist from before validation
            Set<CardinalityConstraintDB> linksFrom = missingAssetLinksFrom.getOrDefault(inferredAsset.getUri(), new HashSet<>());
            for (CardinalityConstraintDB link : linksFrom) {
                // Check if the other end exists, and if so, add the link to the graph
                GraphNode graphNode = graphNodeMap.get(link.getLinksTo());
                if(graphNode != null){
                    graphNode.addBackwardLink(link.getLinkType(), node);
                    node.addForwardLink(link.getLinkType(), graphNode);    
                }
            }

            // Combine created asset with any links to the node which may exist from before validation
            Set<CardinalityConstraintDB> linksTo = missingAssetLinksTo.getOrDefault(inferredAsset.getUri(), new HashSet<>());
            for (CardinalityConstraintDB link : linksTo) {
                // Check if the other end exists, and if so, add the link to the graph
                GraphNode graphNode = graphNodeMap.get(link.getLinksFrom());
                if(graphNode != null){
                    node.addBackwardLink(link.getLinkType(), graphNode);
                    graphNode.addForwardLink(link.getLinkType(), node);
                }
            }

            // Now remove the entries for the new asset from the missing links maps
            missingAssetLinksFrom.remove(inferredAsset.getUri());
            missingAssetLinksTo.remove(inferredAsset.getUri());

            inferredAsset.setCreatedByPattern(constructionPattern.getUri());
            inferredAsset.setType(inferredAssetTemplate.getUri());

            if (inferredNodeSetting.getDisplayedAtLink() != null) {
                RoleLinkDB displayedAtLink = querier.getRoleLink(inferredNodeSetting.getDisplayedAtLink(), "domain");
                inferredAsset.setDisplayedAtRelationFrom(
                        matchedPattern.getRoleAsset(displayedAtLink.getLinksFrom()));
                inferredAsset.setDisplayedAtRelationTo(
                        matchedPattern.getRoleAsset(displayedAtLink.getLinksTo()));
                inferredAsset.setDisplayedAtRelationType(
                        displayedAtLink.getLinkType());
            }

            // TODO: Display at multiple nodes (?)
            for (String nodeUri : inferredNodeSetting.getDisplayedAtNode()) {
                NodeDB displayNode = querier.getNode(nodeUri, "domain");
                inferredAsset.setDisplayedAtAsset(
                        matchedPattern.getRoleAsset(displayNode.getRole()));
            }

            inferredAssets.add(inferredAsset);
            node.setType(inferredAsset.getType());
            graphNodeMap.put(inferredAsset.getUri(), node);
        }

        return inferredAssets;
    }

    /** public List<CardinalityConstraintDB> getInferredLinksForConstructionMatch
     *   @param matchedPattern: a system model matching pattern that matches graph pattern
     *   @param graphPattern: representation of the domain model matching pattern
     *   @param constructionPattern: the domain model construction pattern
     */
    public List<CardinalityConstraintDB> getInferredLinksForConstructionMatch(GraphMatchedPattern matchedPattern, 
                                            GraphPattern graphPattern, ConstructionPatternDB constructionPattern) {
        // TODO: Extract side effects (?)

        List<CardinalityConstraintDB> inferredLinks = new ArrayList<>();

        for (String linkUri : constructionPattern.getInferredLinks()) {
            RoleLinkDB infLink = querier.getRoleLink(linkUri, "domain");

            for (String fromAssetUri : matchedPattern.getRoleAssets(infLink.getLinksFrom())) {
                for (String toAssetUri : matchedPattern.getRoleAssets(infLink.getLinksTo())) {
                    GraphNode fromNode = graphNodeMap.get(fromAssetUri);
                    GraphNode toNode = graphNodeMap.get(toAssetUri);

                    // Don't create link if it already exists
                    if (getNodesFromLink(fromNode, infLink.getLinkType()).contains(toNode)) {
                        continue;
                    }

                    fromNode.addForwardLink(infLink.getLinkType(), toNode);
                    toNode.addBackwardLink(infLink.getLinkType(), fromNode);

                    AssetDB fromAsset = querier.getAsset(fromAssetUri, "system", "system-inf");
                    AssetDB toAsset = querier.getAsset(toAssetUri, "system", "system-inf");

                    CardinalityConstraintDB link = new CardinalityConstraintDB();
                    link.setUri("system#" +
                            fromAsset.getId() + "-" +
                            infLink.getLinkType().replace("domain#", "") + "-" +
                            toAsset.getId());
                    link.setLinksFrom(fromAssetUri);
                    link.setLinksTo(toAssetUri);
                    link.setLinkType(infLink.getLinkType());
                    //asset.getLinks().add(link);
                    inferredLinks.add(link);
                }
            }
        }

        return inferredLinks;
    }

    /** Clears out assets and links created by construction that are only needed during construction.
     * 
     * At the point this is used (just after construction) there should be no CS, MS or TWAS referring to
     * those assets, nor any Nodes or related MatchingPatterns (since these are not stored until we start
     * creating Threats), and of course, no Threats or ControlStrategies.
     * 
     * However, there will be some GraphNodes associated with assets, added by construction to graphNodeMap.
     */
    public void deleteConstructionState(){
        final long startTime = System.currentTimeMillis();

        // First - delete any links that couldn't be connected to assets at both ends
        deleteDanglingLinks();

        // Then - delete assets and links flagged in the domain model as construction state
        // Note - these flags refer to exact types to be removed, so don't delete subclasses
        Set<String> constructionStateResources = querier.getConstructionState();

        // Get all system assets, and all inferred links, nodes and matching patterns
        Map<String, AssetDB> assets = querier.getAssets("system", "system-inf");
        Map<String, CardinalityConstraintDB> links = querier.getCardinalityConstraints("system-inf");

        Map<String, AssetDB> deleteAssets = new HashMap<>();
        Map<String, CardinalityConstraintDB> deleteLinks = new HashMap<>();

        // Find links to delete based on their types, and also remove them from assets and graphNodes
        for(CardinalityConstraintDB link : links.values()){
            if(constructionStateResources.contains(link.getLinkType())) {
                // Remove the link from graphnode outbound and inbound links
                GraphNode fromNode = graphNodeMap.get(link.getLinksFrom());
                GraphNode toNode = graphNodeMap.get(link.getLinksTo());
                fromNode.delForwardLink(link.getLinkType(), toNode);
                toNode.delBackwardLink(link.getLinkType(), fromNode);

                // Add the link to the removal list
                deleteLinks.put(link.getUri(), link);
            }
        }

        // Find assets to delete based on their types, along with related links and graphNodes
        for(AssetDB asset : assets.values()){
            if(constructionStateResources.contains(asset.getType())) {
                // Find the graphnode for this asset
                GraphNode gnode = graphNodeMap.get(asset.getUri());

                // Remove links from and to this graphnode
                for(CardinalityConstraintDB link : querier.getLinksFrom(asset.getUri())){
                    GraphNode toNode = graphNodeMap.get(link.getLinksTo());
                    gnode.delForwardLink(link.getLinkType(), toNode);
                    toNode.delBackwardLink(link.getLinkType(), gnode);
                }
                for(CardinalityConstraintDB link : querier.getLinksFrom(asset.getUri())){
                    GraphNode fromNode = graphNodeMap.get(link.getLinksFrom());
                    fromNode.delForwardLink(link.getLinkType(), gnode);
                    gnode.delBackwardLink(link.getLinkType(), fromNode);
                }

                // Add the asset to the removal list
                deleteAssets.put(asset.getUri(), asset);
            }
        }

        // Now remove the graphnodes corresponding to the deleted assets
        for(String assetUri : deleteAssets.keySet()){
            graphNodeMap.remove(assetUri);
        }

        logger.info("Found {} assets and {} links for removal as construction state", deleteAssets.size(), deleteLinks.size());

        // Node delete the assets and cardinality constraints found
        querier.deleteAssets(deleteAssets, false);
        querier.deleteCardinalityConstraints(deleteLinks, false);

        final long endTime = System.currentTimeMillis();
        logger.info("Validator.deleteConstructionState(): execution time {} ms", endTime - startTime);        
    }

    /** Clears out links that were found in the model but one or both assets are missing.
     * 
     *  Assets may be added during construction, so this should be used afterwards, just
     *  before clearing out construction state.
     */
    public void deleteDanglingLinks(){
        Map<String, CardinalityConstraintDB> deleteLinks = new HashMap<>();

        // Include links from assets that don't exist (even after construction)
        for(String assetURI : missingAssetLinksFrom.keySet()){
            Set<CardinalityConstraintDB> linksFrom = missingAssetLinksFrom.get(assetURI);
            if(linksFrom.size() > 0) {
                logger.warn("Deleting {} links from asset {} which does not exist", linksFrom.size(), assetURI);
            }
            for(CardinalityConstraintDB link : linksFrom){
                deleteLinks.put(link.getUri(), link);
            }
        }

        // Include links to assets that don't exist (even after construction)
        for(String assetURI : missingAssetLinksTo.keySet()){
            Set<CardinalityConstraintDB> linksTo = missingAssetLinksTo.get(assetURI);
            if(linksTo.size() > 0) {
                logger.warn("Deleting {} links to asset {} which does not exist", linksTo.size(), assetURI);
            }
            for(CardinalityConstraintDB link : linksTo){
                deleteLinks.put(link.getUri(), link);
            }
        }

        // Delete the missing links - this should not change whether the model is valid
        querier.deleteCardinalityConstraints(deleteLinks, false);

    }

    /** Creates system matching patterns with associated system nodes.
     */
    public List<MatchingPatternDB> createAndStoreMatchingPatterns(MatchingPatternDB matchingPattern) {
        final long startTime = System.currentTimeMillis();

        List<MatchingPatternDB> systemMatchingPatterns = new ArrayList<>();

        GraphPattern graphPattern = matchingPatternToGraphPattern(matchingPattern);

        if (graphPattern == null) {
            return new ArrayList<>();
        }

        for (GraphMatchedPattern matchedPattern : matchPattern(graphPattern)) {
            MatchingPatternDB systemMatchingPattern = createMatchingPattern(matchingPattern, matchedPattern);
            querier.store(systemMatchingPattern, "system-inf");
            systemMatchingPatterns.add(systemMatchingPattern);
        }

        final long endTime = System.currentTimeMillis();
        logger.debug("Validator.createAndStoreMatchingPatterns({}): found {} patterns in {} ms", 
                    matchingPattern.getLabel(), systemMatchingPatterns.size(), endTime - startTime);

        return systemMatchingPatterns;

    }

    /** Creates a system matching pattern with domain template `matchingPattern` corresponding to the
     *  graph match `matchedPattern`, including system nodes of each type within the pattern.
     */
    private MatchingPatternDB createMatchingPattern(MatchingPatternDB matchingPattern, GraphMatchedPattern matchedPattern) {
        MatchingPatternDB systemMatchingPattern = new MatchingPatternDB();
        RootPatternDB rootPattern = querier.getRootPattern(matchingPattern.getRootPattern(), "domain");

        // TODO: Generalise with construction patterns code
        // Sort nodes by URI for URI/label generation
        List<NodeDB> orderedNodes = new ArrayList<>();
        rootPattern.getKeyNodes().forEach(keyNodeUri -> orderedNodes.add(querier.getNode(keyNodeUri, "domain")));
        rootPattern.getRootNodes().forEach(keyNodeUri -> orderedNodes.add(querier.getNode(keyNodeUri, "domain")));
        orderedNodes.sort(Comparator.comparing(EntityDB::getUri));

        // TODO: Don't store graphs in URI (instead on EntityDB object)
        // Generate system matching pattern URI and label from the root nodes
        StringBuilder uriBuilder = new StringBuilder("system#" + matchingPattern.getUri().replace("domain#", ""));
        StringBuilder labelBuilder = new StringBuilder(matchingPattern.getLabel());
        for (NodeDB node : orderedNodes) {
            String assetUri = matchedPattern.getRoleAsset(node.getRole());
            AssetDB asset = querier.getAsset(assetUri, "system", "system-inf");
            uriBuilder.append("_");
            uriBuilder.append(asset.getId());

            // Only use key nodes in label
            if (rootPattern.getKeyNodes().contains(node.getUri())) {
                labelBuilder.append("_");
                labelBuilder.append(asset.getLabel());
            }

        }
        systemMatchingPattern.setUri(uriBuilder.toString());
        systemMatchingPattern.setLabel(labelBuilder.toString());
        systemMatchingPattern.setParent(matchingPattern.getUri());

        // Create a list of domain model matching pattern node URIs
        List<String> nodeUris = new ArrayList<>();
        nodeUris.addAll(rootPattern.getKeyNodes());
        nodeUris.addAll(rootPattern.getRootNodes());
        nodeUris.addAll(matchingPattern.getMandatoryNodes());
        nodeUris.addAll(matchingPattern.getNecessaryNodes());
        nodeUris.addAll(matchingPattern.getSufficientNodes());
        nodeUris.addAll(matchingPattern.getOptionalNodes());

        // Finding assets matching each domain model node, create system model nodes and add them to the system model pattern
        Integer populationLevel = 0;
        for (String nodeUri : nodeUris) {
            NodeDB domainNode = querier.getNode(nodeUri, "domain");
            for (String assetUri : matchedPattern.getAllFeasible().getOrDefault(domainNode.getRole(), new HashSet<>())) {
                // Get the list of nodes at this asset
                Map<String,NodeDB> thisAssetNodes = systemNodeMap.computeIfAbsent(assetUri, k -> new HashMap<>());

                // Get the asset and find its population 
                Integer level = 0;
                AssetDB systemAsset = querier.getAsset(assetUri, "system", "system-inf");
                String populationURI = systemAsset.getPopulation();
                if(populationURI != null) {
                    level = poLevels.get(populationURI).getLevelValue();
                }   

                // Create the system model node
                NodeDB systemNode = new NodeDB();
                String systemNodeUri = String.format("system#Node-%s-%s",
                        systemAsset.getId(), domainNode.getRole().replace("domain#", ""));
                systemNode.setSystemAsset(assetUri);
                systemNode.setRole(domainNode.getRole());
                systemNode.setUri(systemNodeUri);

                // Add to the relevant lists of nodes in the system model pattern.
                /* 
                 * TODO : work out how we can avoid adding each node to 2 or 3 lists.
                 */

                if(rootPattern.getKeyNodes().contains(nodeUri)){
                    // Unique nodes that contribute to the pattern ID and determine population
                    systemMatchingPattern.getUniqueNodes().add(systemNode.getUri());
                    if(populationLevel < level) populationLevel = level;
                }
                if(rootPattern.getRootNodes().contains(nodeUri)){
                    // Unique nodes that don't contribute, being 1-to-1 with key nodes
                    systemMatchingPattern.getUniqueNodes().add(systemNode.getUri());
                }
                if(matchingPattern.getMandatoryNodes().contains(nodeUri)){
                    systemMatchingPattern.getMandatoryNodes().add(systemNode.getUri());
                }
                if(matchingPattern.getNecessaryNodes().contains(nodeUri)){
                    systemMatchingPattern.getNecessaryNodes().add(systemNode.getUri());
                }
                if(matchingPattern.getSufficientNodes().contains(nodeUri)){
                    systemMatchingPattern.getSufficientNodes().add(systemNode.getUri());
                }
                if(matchingPattern.getOptionalNodes().contains(nodeUri)){
                    systemMatchingPattern.getOptionalNodes().add(systemNode.getUri());
                }
                systemMatchingPattern.getNodes().add(systemNode.getUri());

                thisAssetNodes.put(systemNode.getRole(), systemNode);
                systemNodes.put(systemNode.getUri(), systemNode);
                querier.store(systemNode, "system-inf");
            }
        }

        systemMatchingPattern.setPopulation(populationLevels.get(populationLevel).getUri());
        
        return systemMatchingPattern;
    }

    /**Given a relationship (as a CardinalityConstraintDB), calculates its cardinality constraints
     * based on the cardinality of the source and target assets.
     */
    private void calculateRelationCardinalityConstraint(CardinalityConstraintDB link) {
        AssetDB fromAsset = querier.getAsset(link.getLinksFrom(), "system", "system-inf");
        AssetDB toAsset = querier.getAsset(link.getLinksTo(), "system", "system-inf");

        // Find the link source and target cardinality. We should have population levels because
        // if the domain model doesn't have one, we use a default scale based on the asset's max
        // cardinality.
        String sourcePopulation = fromAsset.getPopulation();
        String targetPopulation = toAsset.getPopulation();

        int sourcePopulationLevel = poLevels.get(sourcePopulation).getLevelValue();
        int targetPopulationLevel = poLevels.get(targetPopulation).getLevelValue();

        // Now set the cardinality of the link from a comparison of source and target populations
        /*
         * At present we just want to know if it is 1-to-1, 1-to-* or *-to-1. Need to decide if
         * we should try to find a population level for each link to/from 'many'.
         */
        Integer sourceCardinality = null;
        Integer targetCardinality = null;
        if(sourcePopulationLevel == targetPopulationLevel){
            // link should be 1-to-1, unless specified as all-to-all (i.e. both ends cardinality is -1)
            sourceCardinality = link.getSourceCardinality();
            targetCardinality = link.getTargetCardinality();
            if (sourceCardinality == null || sourceCardinality != -1 || targetCardinality == null || targetCardinality != -1){
                sourceCardinality = 1;
                targetCardinality = 1;
            }
        }
        if(sourcePopulationLevel > targetPopulationLevel){
            // link should be *-to-1
            sourceCardinality = -1;
            targetCardinality = 1;
        }
        if(sourcePopulationLevel < targetPopulationLevel){
            // link should be 1-to-*
            sourceCardinality = 1;
            targetCardinality = -1;
        }

        link.setSourceCardinality(sourceCardinality);
        link.setTargetCardinality(targetCardinality);

    }

    /**Create a basic inferred asset (only URI and label populated) from the specification 
     * in the `inferredNodeSetting`.
     */
    private InferredAssetDB getInferredAssetBase(InferredNodeSettingDB inferredNodeSetting,
                                                 AssetDB inferredAssetTemplate,
                                                 GraphMatchedPattern matchedPattern) {

        // Nodes are sorted first by their role and then their asset
        List<NodeDB> orderedIncludeNodes = new ArrayList<>();
        for (String includeNodeUri : inferredNodeSetting.getIncludesNodeInURI()) {
            NodeDB includeNode = querier.getNode(includeNodeUri, "domain");
            orderedIncludeNodes.add(includeNode);
        }
        orderedIncludeNodes.sort(Comparator.comparing(node -> (node.getRole() + "-" + node.getAsset())));

        // Create inferred asset URI and label from the URIs and labels of the ordered nodes
        String inferredAssetUri =  inferredAssetTemplate.getUri().replace("domain#", "system#");
        // Wrap inferred asset labels with square brackets.
        // Put a colon after the initial class name.
        String inferredAssetLabel = "[" + inferredAssetTemplate.getLabel() + ":";

        // Set the default population level
        Integer inferredAssetPopulationLevel = 0;

        // Also the min and max cardinality
        Integer inferredAssetMinCardinality = 1;
        Integer inferredAssetMaxCardinality = 1;

        for (NodeDB node : orderedIncludeNodes) {
            // Find the asset corresponding to this 'include node'
            AssetDB matchedAsset = querier.getAsset(matchedPattern.getRoleAsset(
                    node.getRole()), "system", "system-inf");

            // Use information from the include node to generate the inferred node URI and Label 
            inferredAssetUri += "_" + matchedAsset.getId();
            // If an asset label has a space in (happens with asserted assets) then wrap it with parentheses.
            // However, if the asset label is already delimited by brackets, don't bother.
            String label = matchedAsset.getLabel();
            if (label.contains(" ") && label.charAt(0) != '[') {
                label = "(" + label + ")";
            }
            inferredAssetLabel += label + "-";

            // Use information from the include node to work out the inferred asset population
            Integer includeNodePopulationLevel = poLevels.get(matchedAsset.getPopulation()).getLevelValue();
            if (inferredAssetPopulationLevel < includeNodePopulationLevel) {
                inferredAssetPopulationLevel = includeNodePopulationLevel;
            }

        }

        inferredAssetLabel = inferredAssetLabel.substring(0, inferredAssetLabel.length() - 1) + "]";

        // Create and populate the inferred asset object
        InferredAssetDB inferredAssetBase = new InferredAssetDB();
        inferredAssetBase.setUri(inferredAssetUri);
        inferredAssetBase.setLabel(inferredAssetLabel);
        inferredAssetBase.setType(inferredAssetTemplate.getUri());
        inferredAssetBase.setId(inferredAssetBase.generateID());
        inferredAssetBase.setPopulation(populationLevels.get(inferredAssetPopulationLevel).getUri());
        inferredAssetBase.setMinCardinality(inferredAssetMinCardinality);
        inferredAssetBase.setMaxCardinality(inferredAssetMaxCardinality);

        return inferredAssetBase;
    }

    /** Get all sub-graphs in the system graph which match the given pattern. Pattern matching is a
     *  three stage process:
     *   1) The matching pattern's root pattern is matched using a standard sub-graph search algorithm
     *   2) Secondary nodes (optional and mandatory) are added to each matched root pattern if they
     *      have all necessary links with the root pattern
     *   3) Matched patterns are discarded if they fail certain checks (prohibited nodes and links,
     *      distinct sub-groups)
     */
    private List<GraphMatchedPattern> matchPattern(GraphPattern graphPattern) {
        List<GraphMatchedPattern> matchedPatterns = new ArrayList<>();

        GraphMatchedPattern initialPattern = new GraphMatchedPattern(graphPattern);
        for (String patternNode : graphPattern.getNodes()) {
            NodeDB node = querier.getNode(patternNode, "domain");
            String role = node.getRole();
            String assetType = node.getAsset();
            initialPattern.getAllFeasible().put(role, findFeasible(assetType));
        }
        simpleSim(graphPattern, initialPattern);
        search(matchedPatterns, graphPattern, initialPattern, 0);

        Set<String> secondaryNodes = new HashSet<>();
        secondaryNodes.addAll(graphPattern.getMandatoryNodes());
        secondaryNodes.addAll(graphPattern.getNecessaryNodes());
        secondaryNodes.addAll(graphPattern.getSufficientNodes());
        secondaryNodes.addAll(graphPattern.getOptionalNodes());

        Iterator<GraphMatchedPattern>  iterator = matchedPatterns.iterator();
        while (iterator.hasNext()) {
            GraphMatchedPattern matchedPattern = iterator.next();

            if (!checkNodeAssetTypes(matchedPattern)) {
                iterator.remove();
                continue;
            }

            // Match and add secondary nodes (optional and mandatory)
            Map<String, List<String>> secondaryNodeMatches = matchExternalNodes(matchedPattern, graphPattern, secondaryNodes);
            for (String secondaryRole : secondaryNodeMatches.keySet()) {
                for (String secondaryAsset : secondaryNodeMatches.get(secondaryRole)) {
                    matchedPattern.addFeasibleFrom(secondaryRole, secondaryAsset);
                }
            }

            // Remove pattern if it is missing mandatory nodes (including necessary/sufficient nodes)
            if (!checkMatchedPatternMandatoryNodes(matchedPattern, graphPattern)) {
                iterator.remove();
                continue;
            }

            // Remove pattern if it is contains prohibited links
            if (checkPatternViolatesProhibitedLinks(matchedPattern, graphPattern)) {
                iterator.remove();
                continue;
            }

            // Remove pattern if it links with prohibited nodes
            if (checkPatternViolatesProhibitedNodes(matchedPattern, graphPattern)) {
                iterator.remove();
                continue;
            }

            // Remove pattern if it doesn't satisfy distinct node requirements
            if (!checkPatternSatisfiesDistinctNodeGroups(matchedPattern, graphPattern)) {
                iterator.remove();
            }
        }

        return matchedPatterns;
    }

    private boolean checkPatternSatisfiesDistinctNodeGroups(GraphMatchedPattern matchedPattern, GraphPattern graphPattern) {
        for (String distinctNodeGroupUri : graphPattern.getDistinctNodeGroups()) {
            DistinctNodeGroupDB distinctNodeGroup = querier.getDistinctNodeGroup(distinctNodeGroupUri, "domain");
            Set<String> assets = new HashSet<>();
            for (String node : distinctNodeGroup.getNodes()) {
                String role = querier.getNode(node, "domain").getRole();
                if (!assets.add(matchedPattern.getRoleAsset(role))) {
                    return false;
                }
            }
        }

        return true;
    }

    /** Returns true if any of the nodes in `matchedPattern` contain links to prohibited nodes (as
     *  specified in `graphPattern`) False otherwise.
     */
    private boolean checkPatternViolatesProhibitedNodes(GraphMatchedPattern matchedPattern, GraphPattern graphPattern) {
        Map<String, List<String>> prohibitedNodeMatches = matchExternalNodes(
                matchedPattern, graphPattern, graphPattern.getProhibitedNodes());
        return !prohibitedNodeMatches.isEmpty();
    }

    /** Returns true if `matchedPattern` contains prohibited links (as specified in `graphPattern`) between
     *  its nodes, and false otherwise.
     */
    private boolean checkPatternViolatesProhibitedLinks(GraphMatchedPattern matchedPattern, GraphPattern graphPattern) {
        for (PatternLink prohibitedLink : graphPattern.getProhibitedLinks()) {
            Set<String> fromAssets = matchedPattern.getRoleAssets(prohibitedLink.getFromNode());
            Set<String> toAssets = matchedPattern.getRoleAssets(prohibitedLink.getToNode());

            for (String fromAsset : fromAssets) {
                for (GraphNode connected : getNodesFromLink(graphNodeMap.get(fromAsset), prohibitedLink.getLinkType())) {
                    if (toAssets.contains(connected.getAsset())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /** Return true if all of the `matchedPattern` nodes have an asset type allowed by the domain
     *  model (this method is necessary as the initial checking considers only that the roles match).
     */
    private boolean checkNodeAssetTypes(GraphMatchedPattern matchedPattern) {
        for (String nodeUri : matchedPattern.getParent().getNodes()) {
            NodeDB node = querier.getNode(nodeUri, "domain");
            AssetDB systemAsset = querier.getAsset(matchedPattern.getRoleAsset(node.getRole()), "system", "system-inf");

            if (!checkNodeAssetType(node, systemAsset.getType())) {
                return false;
            }
        }

        return true;
    }

    private boolean checkNodeAssetType(NodeDB node, String assetType) {
        List<String> allowedAssetTypes = querier.getSubTypes(node.getAsset(), true);
        if (!allowedAssetTypes.contains(assetType)) {
            return false;
        }
        return true;
    }

    /** Matches external nodes to the given `matchedPattern`. External nodes can be any nodes which are
     *  not part of the matched pattern's root pattern. A system node is a match for an external node if
     *  it is connected to nodes in the matched pattern through ALL of the relevant links specified in 
     *  the `graphPattern`. The returned Map consists of role URIs (each one corresponding to the role 
     *  of a given external node) paired with the found matching system node assets.
     */
    private Map<String, List<String>> matchExternalNodes(GraphMatchedPattern matchedPattern, GraphPattern graphPattern,
                                                         Collection<String> externalNodes) {
        Map<String, List<String>> externalNodeMatches = new HashMap<>();

        for (String externalNodeUri : externalNodes) {
            NodeDB externalNode = querier.getNode(externalNodeUri, "domain");
            String externalRole = externalNode.getRole();

            Set<GraphNode> feasibleExternalNodes = new HashSet<>();
            List<PatternLink> matchLinksForExternalNode = new ArrayList<>();

            for (PatternLink matchLink : graphPattern.getMatchLinks()) {
                String baseNode;
                Set<GraphNode> connectedNodes;

                if (matchLink.getFromNode().equals(externalRole)) {
                    matchLinksForExternalNode.add(matchLink);
                    baseNode = matchLink.getToNode();
                    String matchedAsset = matchedPattern.getRoleAsset(baseNode);
                    GraphNode matchedNode = graphNodeMap.get(matchedAsset);
                    connectedNodes = getNodesToLink(matchedNode, matchLink.getLinkType());
                } else if (matchLink.getToNode().equals(externalRole)) {
                    matchLinksForExternalNode.add(matchLink);
                    baseNode = matchLink.getFromNode();
                    String matchedAsset = matchedPattern.getRoleAsset(baseNode);
                    GraphNode matchedNode = graphNodeMap.get(matchedAsset);
                    connectedNodes = getNodesFromLink(matchedNode, matchLink.getLinkType());
                } else {
                    continue;
                }

                for (GraphNode connectedNode : connectedNodes) {
                    if (connectedNode.getRoles().contains(externalRole) &&
                            checkNodeAssetType(externalNode, connectedNode.getType())) {
                        feasibleExternalNodes.add(connectedNode);
                    }
                }
            }

            for (GraphNode feasibleSecondaryNode : feasibleExternalNodes) {
                if (checkNodeHasAllLinksWithPattern(feasibleSecondaryNode, matchLinksForExternalNode, matchedPattern, externalRole)) {
                    List<String> externalMatches = externalNodeMatches.computeIfAbsent(externalRole, k -> new ArrayList<>());
                    externalMatches.add(feasibleSecondaryNode.getAsset());
                }
            }
        }

        return externalNodeMatches;
    }

    private boolean checkMatchedPatternMandatoryNodes(GraphMatchedPattern matchedPattern, GraphPattern graphPattern) {
        for (String mandatoryNodeUri : graphPattern.getMandatoryNodes()) {
            NodeDB mandatoryNode = querier.getNode(mandatoryNodeUri, "domain");

            if (matchedPattern.getRoleAssets(mandatoryNode.getRole()).isEmpty()) {
                return false;
            }
        }

        for (String mandatoryNodeUri : graphPattern.getNecessaryNodes()) {
            NodeDB mandatoryNode = querier.getNode(mandatoryNodeUri, "domain");

            if (matchedPattern.getRoleAssets(mandatoryNode.getRole()).isEmpty()) {
                return false;
            }
        }

        for (String mandatoryNodeUri : graphPattern.getSufficientNodes()) {
            NodeDB mandatoryNode = querier.getNode(mandatoryNodeUri, "domain");

            if (matchedPattern.getRoleAssets(mandatoryNode.getRole()).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    /** Checks that a `node` is connected with nodes in `matchedPattern` through ALL of the given `links`.
     *  FROM and TO connections are both valid, depending on the link.
     */
    private boolean checkNodeHasAllLinksWithPattern(GraphNode node, List<PatternLink> links, GraphMatchedPattern matchedPattern, String externalRole) {
        for (PatternLink matchLink : links) {
            String baseNode;
            Set<GraphNode> connectedNodes;
            
            if (matchLink.getFromNode().equals(externalRole)) {
                baseNode = matchLink.getToNode();
                connectedNodes = getNodesFromLink(node, matchLink.getLinkType());
            } else if (matchLink.getToNode().equals(externalRole)) {
                baseNode = matchLink.getFromNode();
                connectedNodes = getNodesToLink(node, matchLink.getLinkType());
            } else {
                return false;
            }

            boolean match = false;
            for (GraphNode connectedNode : connectedNodes) {
                if (matchedPattern.getRoleAsset(baseNode).equals(connectedNode.getAsset())) {
                    match = true;
                    break;
                }
            }

            if (!match) {
                return false;
            }
        }

        return true;
    }

    /** Remove feasible nodes which are not actually feasible. A node is unfeasible if it does not link
     *  to another feasible node through a link specified in the `graphPattern`.
     */
    private void simpleSim(GraphPattern graphPattern, GraphMatchedPattern matchedPattern) {
        boolean changed = true;

        while (changed) {
            changed = false;

            // Collect all pattern links which we have to check
            List<PatternLink> patternLinks = new ArrayList<>();
            for (String patternNode : graphPattern.getRoles()) {
                patternLinks.addAll(graphPattern.getLinksFrom(patternNode));
            }

            for (PatternLink patternLink : patternLinks) {
                Set<String> fromFeasibleNodes = matchedPattern.getFeasibleFrom(patternLink.getFromNode());

                Iterator<String> fromFeasibleIterator = fromFeasibleNodes.iterator();
                while (fromFeasibleIterator.hasNext()) {
                    String fromFeasibleNode = fromFeasibleIterator.next();
                    boolean match = checkMatch(patternLink, fromFeasibleNode, matchedPattern);
                    if (!match) {
                        fromFeasibleIterator.remove();
                        changed = true;
                    }

                    // Stop early if a role in the pattern no longer has any feasible nodes
                    if (fromFeasibleNodes.isEmpty()) {
                        return;
                    }
                }
            }
        }
    }

    /** Recursive method which assists in performing the sub-pattern matching. Takes a domain `graphPattern`
     *  and a `matchedPattern` which encompasses all potential matches. Populates `matches` with separated
     *  matched sub-patterns.
     */
    private void search(List<GraphMatchedPattern> matches, GraphPattern graphPattern,
                        GraphMatchedPattern matchedPattern, int depth) {
        if (depth == graphPattern.getRoles().size()) {
            matches.add(matchedPattern);
        } else {
            String fromNode = graphPattern.getRoles().get(depth);
            for (String nodeV : matchedPattern.getFeasibleFrom(fromNode)) {
                GraphMatchedPattern copyMatched = matchedPattern.clone();
                Set<String> set = new HashSet<>();
                set.add(nodeV);
                copyMatched.getAllFeasible().put(fromNode, set);
                simpleSim(graphPattern, copyMatched);
                if (!copyMatched.checkInvalidMatch()) {
                    search(matches, graphPattern, copyMatched, depth+1);
                }
            }
        }
    }

    /** Check whether a feasible node `fromFeasibleNode` remains feasible when matched against `patternLink` 
     *  from `matchedPattern`.
     */
    private boolean checkMatch(PatternLink patternLink, String fromFeasibleNode, GraphMatchedPattern matchedPattern) {
        Set<String> toFeasibleNodes = matchedPattern.getFeasibleFrom(patternLink.getToNode());

        for (GraphNode graphNode : getNodesFromLink(graphNodeMap.get(fromFeasibleNode), patternLink.getLinkType())) {
            if (toFeasibleNodes.contains(graphNode.getAsset())) {
                return true;
            }
        }

        return false;
    }

    private Set<GraphNode> getNodesFromLink(GraphNode node, String linkType) {
        List<String> subTypes = querier.getSubTypes(linkType, true);
        return node.getNodesFromLinks(subTypes);
    }

    private Set<GraphNode> getNodesToLink(GraphNode node, String linkType) {
        List<String> subTypes = querier.getSubTypes(linkType, true);
        return node.getNodesToLinks(subTypes);
    }

    /** Find all assets which may match a given `node`, based on the required asset type.
     */
    private Set<String> findFeasible(String assetType) {
        Set<String> feasibleNodes = new HashSet<>();

        /*
         * Originally the argument was a role, and the method returned all system assets that could
         * fulfil that role. This is inefficient, because roles are shared between matching patterns
         * but in some patterns the asset type is more restricted than in others. For those patterns
         * we should use the type restriction to get a smaller set of potential candidates.
         */
        List<String> assetSubtypes = querier.getSubTypes(assetType, true);
        for (GraphNode graphNode : graphNodeMap.values()) {
            String nodeType = graphNode.getType();
            if(assetSubtypes.contains(nodeType)){
                feasibleNodes.add(graphNode.getAsset());
            }
        }

        return feasibleNodes;
    }

    /** Converts a MatchingPatternDB entity to a GraphPattern, used for pattern matching.
     */
    private GraphPattern matchingPatternToGraphPattern(MatchingPatternDB matchingPattern) {
        GraphPattern graphPattern = new GraphPattern(matchingPattern.getUri());

        RootPatternDB rootPattern = querier.getRootPattern(matchingPattern.getRootPattern(), "domain");

        // Root nodes and key nodes are treated the same during pattern matching
        Set<String> rootNodes = new HashSet<>();
        rootNodes.addAll(rootPattern.getKeyNodes());
        rootNodes.addAll(rootPattern.getRootNodes());
        for (String nodeUri : rootNodes) {
            NodeDB node = querier.getNode(nodeUri, "domain");
            graphPattern.addRole(node.getRole());
            graphPattern.addNode(nodeUri);
        }

        for (String prohibitedNode : matchingPattern.getProhibitedNodes()) {
            graphPattern.addProhibitedNode(prohibitedNode);
        }
        for (String optionalNode : matchingPattern.getOptionalNodes()) {
            graphPattern.addOptionalNode(optionalNode);
        }
        for (String mandatoryNode : matchingPattern.getMandatoryNodes()) {
            graphPattern.addMandatoryNode(mandatoryNode);
        }
        for (String necessaryNode : matchingPattern.getNecessaryNodes()) {
            graphPattern.addNecessaryNode(necessaryNode);
        }
        for (String sufficientNode : matchingPattern.getSufficientNodes()) {
            graphPattern.addSufficientNode(sufficientNode);
        }
        for (String distinctNodeGroup : matchingPattern.getDistinctNodeGroups()) {
            graphPattern.addDistinctNodeGroup(distinctNodeGroup);
        }

        for (String linkUri : rootPattern.getLinks()) {
            RoleLinkDB link = querier.getRoleLink(linkUri, "domain");
            graphPattern.addLink(link.getLinksFrom(), link.getLinksTo(), link.getLinkType());
        }
        for (String linkUri : matchingPattern.getLinks()) {
            RoleLinkDB matchLink = querier.getRoleLink(linkUri, "domain");
            graphPattern.addMatchLink(matchLink.getLinksFrom(), matchLink.getLinksTo(), matchLink.getLinkType());
        }
        for (String prohibitedLinkUri : matchingPattern.getProhibitedLinks()) {
            RoleLinkDB link = querier.getRoleLink(prohibitedLinkUri, "domain");
            graphPattern.addProhibitedLink(link.getLinksFrom(), link.getLinksTo(), link.getLinkType());
        }

        return graphPattern;
    }

    /** Gets a Map of each asset (short URI) to the roles it can take on (list of short URIs)
     */
    private Map<String, Set<String>> getAssetRoles() {
        if (assetRolesMap != null) {
            return assetRolesMap;
        }

        assetRolesMap = new HashMap<>();

        for (RoleDB role : querier.getRoles("domain").values()) {
            for (String assetUri : role.getMetaLocatedAt()) {
                Set<String> aRoles = assetRolesMap.computeIfAbsent(assetUri, k -> new HashSet<>());
                // If an asset has a role it also has all parents of that role
                aRoles.addAll(querier.getSuperTypes(role.getUri(), true));
            }
        }

        // An asset has the roles of all its parent assets
        Map<String, Set<String>> assetRolesCompleteMap = new HashMap<>();
        for (String assetUri : querier.getSubTypes("core#Asset", false)) {
            Set<String> assetRoles = assetRolesCompleteMap.computeIfAbsent(assetUri, k -> new HashSet<>());

            for (String superAsset : querier.getSuperTypes(assetUri, true)) {
                assetRoles.addAll(assetRolesMap.getOrDefault(superAsset, new HashSet<>()));
            }
        }

        return assetRolesCompleteMap;
    }

}
