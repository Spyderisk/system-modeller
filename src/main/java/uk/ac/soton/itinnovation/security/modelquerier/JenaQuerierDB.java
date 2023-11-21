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
package uk.ac.soton.itinnovation.security.modelquerier;

import com.google.common.graph.SuccessorsFunction;
import com.google.common.graph.Traverser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.function.library.max;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.modelquerier.dto.*;
import uk.ac.soton.itinnovation.security.modelquerier.util.ModelStack;

import java.lang.Exception;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Implementation of IQuerierDB for Jena database. Uses Jena methods for querying the Jena database, as opposed to
 * SPARQL queries. Has a `cacheEnabled` flag which indicates whether entities should be "cached". If so, when entities
 * are queried they are cached in memory, and if retrieved again the in-memory instance will be returned. When entities
 * are stored they are cached and not immediately persisted to the database. Only when the sync() method is called are
 * the stored entities persisted.
 */
public class JenaQuerierDB implements IQuerierDB {
    private static final Logger logger = LoggerFactory.getLogger(JenaQuerierDB.class);

    // TODO: RDFS/RDF work-around for labels, types, etc. Can remove when old class is gone
    // TODO: Encase DB calls in try catch
    // TODO: Put gson.fromJson calls in method with error handling

    private static final String PREFIX = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/";
    private static final String[] systemGraphs = {"system", "system-inf"};
    private static final String[] allGraphs = {"system", "system-inf", "system-ui"};
    private Dataset dataset;
    private ModelStack stack;
    private Gson gson;
    private Map<String, String> prefixMap;
    private Map<String, String> prefixReverseMap;

    private Traverser<String> superTypeTree;
    private Traverser<String> subTypeTree;

    private Map<Integer, String[]> checkedOutEntityGraphs = new HashMap<>();

    private EntityCache cache;
    private boolean cacheEnabled;

    private Map<String, LevelDB> poLevels = new HashMap<>();                            // Map of domain model population levels indexed by URI
    private Map<String, LevelDB> twLevels = new HashMap<>();                            // Map of domain model trustworthiness levels indexed by URI
    private List<LevelDB> trustworthinessLevels = new ArrayList<>();                    // Array of domain model trustworthiness levels indexed by level value

    // TODO : decide if we should instead just compute any missing settings in each settings map
    Map<String, Map<String, String>> defaultByMisbehaviourByAsset = new HashMap<>();    // Map of maps of MADefaultSetting URIs, indexed by asset type and then Misbehaviour
    Map<String, Map<String, String>> defaultByTwaByAsset = new HashMap<>();             // Map of maps of TWAADefaultSetting URIs, indexed by asset type and then TWA
    Map<String, Map<String, String>> defaultByControlByAsset = new HashMap<>();         // Map of maps of CASetting URIs, indexed by asset type and then control type    

    Map<String, List<String>> linksFromAsset = new HashMap<>();                         // Map of link URI for links from an asset, indexed by asset URI
    Map<String, List<String>> linksToAsset = new HashMap<>();                           // Map of link URI for links to an asset, indexed by asset URI

    Map<String, Map<String, List<String>>> nodesByRoleByMP = new HashMap<>();           // Map of system model node URIs organised by matching pattern and role
    
    public JenaQuerierDB(Dataset dataset, ModelStack stack) {
        this.dataset = dataset;
        this.stack = stack;
        this.cacheEnabled = true;
    }

    public JenaQuerierDB(Dataset dataset, ModelStack stack, boolean cacheEnabled) {
        this.dataset = dataset;
        this.stack = stack;
        this.cacheEnabled = cacheEnabled;
    }

    /*  Helper method used to convert an array of strings to a single string. 
     *  Useful here because most 'get' methods have an argument list that ends
     *  with a list of models (referred to via strings), so any warning message
     *  may need to include this list.
     */
    private String modelsToString(String[] models){
        String output = "['";
        int i = 0;
        for(String model : models){
            if(i>0) output = output.concat("', '");
            output = output.concat(model);
            i++;
        }
        output = output.concat("']");
        return output;
    }

    public void initForValidation(){
        final long startTime = System.currentTimeMillis();

        init();
        initDefaultSettings();

        final long endTime = System.currentTimeMillis();
        logger.info("JenaQuerierDB.initForValidation(): execution time {} ms", endTime - startTime);
    }

    public void initForRiskCalculation(){
        final long startTime = System.currentTimeMillis();

        init();
        initDefaultSettings();
        initLinkMaps();

        final long endTime = System.currentTimeMillis();
        logger.info("JenaQuerierDB.initForRiskCalculation(): execution time {} ms", endTime - startTime);
    }

    public void init(){
        final long startTime = System.currentTimeMillis();

        this.prefixMap = dataset.getNamedModel(stack.getGraph("core")).getNsPrefixMap();
        this.prefixMap.put("core", PREFIX + "core#");
        this.prefixMap.put("system", PREFIX + "system#");
        this.prefixMap.put("domain", PREFIX + "domain#");

        this.prefixReverseMap = new HashMap<>();
        for (Map.Entry<String, String> entry : prefixMap.entrySet()) {
            this.prefixReverseMap.put(entry.getValue(), entry.getKey());
        }

        this.cache = new EntityCache();

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(String.class, new StringDeserializer());
        gson = gsonBuilder.create();

        createTypeTrees();

        // Get population scale in the form needed by highest/lowest TW level calculations
        poLevels = getPopulationLevels();

        // Get TW scale in the form needed by highest/lowest TW level calculations
        twLevels = getTrustworthinessLevels();
        trustworthinessLevels.addAll(twLevels.values());
        trustworthinessLevels.sort(Comparator.comparingInt(LevelDB::getLevelValue));

        final long endTime = System.currentTimeMillis();
        logger.info("JenaQuerierDB.init(): execution time {} ms", endTime - startTime);
    } 

    /** Loads default settings and organises them so they can be found for an asset,
     *  using inheritance if necessary.
     */
    private void initDefaultSettings() {
        // Get default MS settings per Misbehaviour-Asset combination, and compute a struture indexed by asset type and Misbehaviour 
        Map<String, MADefaultSettingDB> maDefaultSettings = this.getMADefaultSettings();
        for (MADefaultSettingDB maDefaultSetting : maDefaultSettings.values()) {
            Map<String, String> defaultByMisbehaviour = defaultByMisbehaviourByAsset.computeIfAbsent(maDefaultSetting.getMetaLocatedAt(), k -> new HashMap<>());
            defaultByMisbehaviour.put(maDefaultSetting.getMisbehaviour(), maDefaultSetting.getUri());
        }

        // Get default TWAS settings per TWA-Asset combination, and compute a struture indexed by asset type and TWA 
        Map<String, TWAADefaultSettingDB> twaaDefaultSettings = this.getTWAADefaultSettings();
        for (TWAADefaultSettingDB twaaDefaultSetting : twaaDefaultSettings.values()) {
            Map<String, String> defaultByTwa = defaultByTwaByAsset.computeIfAbsent(twaaDefaultSetting.getMetaLocatedAt(), k -> new HashMap<>());
            defaultByTwa.put(twaaDefaultSetting.getTrustworthinessAttribute(), twaaDefaultSetting.getUri());
        }

        // Get default CS settings per Control-Asset combination, and compute a struture indexed by asset type and control type  
        Map<String, CASettingDB> caSettings = this.getCASettings();
        for (CASettingDB caSetting : caSettings.values()) {
            Map<String, String> defaultByControl = defaultByControlByAsset.computeIfAbsent(caSetting.getMetaLocatedAt(), k -> new HashMap<>());
            defaultByControl.put(caSetting.getControl(), caSetting.getUri());
        }

    }

    /** Loads cardinality constraints and creates maps indexed by the assets at either
     *  end of links.
     */
    private void initLinkMaps(){
        // Loading the cardinality constaints will create link maps
        getCardinalityConstraints("system", "system-inf");
    }

    private boolean addMatchingPatternNodeToMap(NodeDB node, Map<String, List<String>> map){
        if(node == null) {
            return false;
        }
        String roleURI = node.getRole();
        List<String> nodesThisRoleThisMP = map.computeIfAbsent(roleURI, K -> new ArrayList<>());
        if(!nodesThisRoleThisMP.contains(node.getUri()))
            nodesThisRoleThisMP.add(node.getUri());

        return true;
    }

    @Override
    public List<String> getMatchingPatternNodes(String mpUri, String roleUri) {
        Map<String, List<String>> nodesByRoleThisMP = nodesByRoleByMP.get(mpUri);
        if(nodesByRoleThisMP != null){
            return nodesByRoleThisMP.get(roleUri);
        } 
        return null;
    }

    @Override
    public Map<String, List<String>> getMatchingPatternNodes(String mpUri) {
        return nodesByRoleByMP.get(mpUri);
    }

    /* Some 'helper' methods, moved from validator or risk calculator so they can
     * be used by both.
     */
    @Override
    public List<String> getPseudorootAssets(MatchingPatternDB threatMatchingPattern) {
        /**
         * Gets a list of assets matching non-unique roles that are really unique in a system pattern
         */

         // Create a set of assets matching unique (root node) roles in this threat pattern
        Set<String> rootAssets = new HashSet<>();
        for (String nodeURI : threatMatchingPattern.getUniqueNodes()) {
            NodeDB node = getNode(nodeURI, "system-inf");
            if(node == null) {
                logger.warn("Found a null unique node at {} in pattern {}", nodeURI, threatMatchingPattern.getUri());
            }
            String assetURI = node.getSystemAsset();
            if(!rootAssets.contains(assetURI)) rootAssets.add(assetURI);
        }

        Set<String> nonUniqueAssets = new HashSet<>();
        for (String nodeURI : threatMatchingPattern.getNecessaryNodes()) {
            NodeDB node = getNode(nodeURI, "system-inf");
            if(node == null) {
                logger.warn("Found a null necessary node at {} in pattern {}", nodeURI, threatMatchingPattern.getUri());
            }
            String assetURI = node.getSystemAsset();
            if(!nonUniqueAssets.contains(assetURI)) nonUniqueAssets.add(assetURI);
        }
        for (String nodeURI : threatMatchingPattern.getSufficientNodes()) {
            NodeDB node = getNode(nodeURI, "system-inf");
            if(node == null) {
                logger.warn("Found a null sufficient node at {} in pattern {}", nodeURI, threatMatchingPattern.getUri());
            }
            String assetURI = node.getSystemAsset();
            if(!nonUniqueAssets.contains(assetURI)) nonUniqueAssets.add(assetURI);
        }

        // Create a list of assets with non-unique roles that are really unique due to their relationships
        List<String> pseudorootAssets = new ArrayList<>();

        // Check for 1-to-x relationships from the non-unique node assets to root node assets
        for (String assetURI : nonUniqueAssets) {
            List<String> linksFromThis = linksFromAsset.getOrDefault(assetURI, new ArrayList<>());
            for(String linkURI : linksFromThis){
                CardinalityConstraintDB link = getCardinalityConstraint(linkURI, "system", "system-inf");
                if(link == null) {
                    logger.warn("Found a null link from asset {}", assetURI);
                } else {
                    if(rootAssets.contains(link.getLinksTo()) && link.getSourceCardinality() == 1) {
                        if(!pseudorootAssets.contains(assetURI)) pseudorootAssets.add(assetURI);
                    }
                }
            }
        }

        // Check for x-to-1 relationships from the root node assets to non-unique node assets
        for (String assetURI : nonUniqueAssets) {
            List<String> linksToThis = linksToAsset.getOrDefault(assetURI, new ArrayList<>());
            for(String linkURI : linksToThis){
                CardinalityConstraintDB link = getCardinalityConstraint(linkURI, "system", "system-inf");
                if(link == null) {
                    logger.warn("Found a null link to asset {}", assetURI);
                } else {
                    if(rootAssets.contains(link.getLinksFrom()) && link.getTargetCardinality() == 1) {
                        if(!pseudorootAssets.contains(assetURI)) pseudorootAssets.add(assetURI);
                    }
                }
            }
        }

        return pseudorootAssets;

    }

    /* Gets cardinality constraint DB objects for links from a specified asset
     */
    @Override
    public List<CardinalityConstraintDB> getLinksFrom(String assetURI){
        List<CardinalityConstraintDB> links = new ArrayList<>();
        List<String> linksFromThis = linksFromAsset.getOrDefault(assetURI, new ArrayList<>());
        for(String linkURI : linksFromThis){
            CardinalityConstraintDB link = getCardinalityConstraint(linkURI, systemGraphs);
            links.add(link);
        }
        return links;
    }

    /* Gets cardinality constraint DB objects for links to a specified asset
     */
    @Override
    public List<CardinalityConstraintDB> getLinksTo(String assetURI){
        List<CardinalityConstraintDB> links = new ArrayList<>();
        List<String> linksToThis = linksToAsset.getOrDefault(assetURI, new ArrayList<>());
        for(String linkURI : linksToThis){
            CardinalityConstraintDB link = getCardinalityConstraint(linkURI, systemGraphs);
            links.add(link);
        }
        return links;
    }

    @Override
    public ControlSetDB getAvgCS(String csURI, String... graphs) {
        ControlSetDB systemCS = this.getControlSet(csURI, graphs);
        if(systemCS != null) {
            if(systemCS.getMinOf() == null && systemCS.getMaxOf() == null) {
                // This is an average coverage CS
                return systemCS;
            }
            else if(systemCS.getMinOf() != null && systemCS.getMaxOf() == null) {
                // This is an minimum coverage CS
                return this.getControlSet(systemCS.getMinOf(), graphs);
            }
            else if(systemCS.getMinOf() == null && systemCS.getMaxOf() != null) {
                // This is an maximum coverage CS
                return this.getControlSet(systemCS.getMaxOf(), graphs);
            }
            else if(systemCS.getMinOf() != null && systemCS.getMaxOf() != null) {
                // Should never get this, so log it and return the average only if they agree
                if(systemCS.getMinOf().equals(systemCS.getMaxOf())){
                    logger.warn("System CS {} is both min and max coverage", csURI);
                    return this.getControlSet(systemCS.getMinOf(), graphs);
                } else {
                    logger.warn("System CS {} is both min and max coverage referring to different average CS", csURI);
                    return null;
                }
            }
        }

        // Finding no CS is expected in some situations, so just return null
        logger.debug("System CS {} could not be found in graphs " + modelsToString(graphs), csURI);
        return null;
    }

    @Override
    public MisbehaviourSetDB getAvgMS(String msURI, String... graphs) {
        MisbehaviourSetDB systemMS = this.getMisbehaviourSet(msURI, graphs);
        if(systemMS != null) {
            if(systemMS.getMinOf() == null && systemMS.getMaxOf() == null) {
                // This is an average likelihood MS
                return systemMS;
            }
            else if(systemMS.getMinOf() != null && systemMS.getMaxOf() == null) {
                // This is an minimum likelihood MS
                return this.getMisbehaviourSet(systemMS.getMinOf(), graphs);
            }
            else if(systemMS.getMinOf() == null && systemMS.getMaxOf() != null) {
                // This is an maximum likelihood MS
                return this.getMisbehaviourSet(systemMS.getMaxOf(), graphs);
            }    
            else if(systemMS.getMinOf() != null && systemMS.getMaxOf() != null) {
                // Should never get this, so log it and return the average only if they agree
                if(systemMS.getMinOf().equals(systemMS.getMaxOf())){
                    logger.warn("System MS {} is both min and max likelihood", msURI);
                    return this.getMisbehaviourSet(systemMS.getMinOf(), graphs);
                } else {
                    logger.warn("System MS {} is both min and max likelihood referring to different average MS", msURI);
                    return null;
                }
            }
        }
        
        // Normally we wouldn't expect to get an input MS that doesn't exist, so log a warning and return null
        logger.warn("System MS {} could not be found in graphs " + modelsToString(graphs), msURI);
        return null;

    }

    @Override
    public TrustworthinessAttributeSetDB getAvgTWAS(String twasURI, String... graphs) {
        TrustworthinessAttributeSetDB systemTWAS = this.getTrustworthinessAttributeSet(twasURI, graphs);;
        if(systemTWAS != null) {
            if(systemTWAS.getMinOf() == null && systemTWAS.getMaxOf() == null) {
                // This is an average trustworthiness TWAS
                return systemTWAS;
            }
            else if(systemTWAS.getMinOf() != null && systemTWAS.getMaxOf() == null) {
                // This is an minimum trustworthiness TWAS
                return this.getTrustworthinessAttributeSet(systemTWAS.getMinOf(), graphs);
            }
            else if(systemTWAS.getMinOf() == null && systemTWAS.getMaxOf() != null) {
                // This is an maximum trustworthiness TWAS
                return this.getTrustworthinessAttributeSet(systemTWAS.getMaxOf(), graphs);
            }    
            else if(systemTWAS.getMinOf() != null && systemTWAS.getMaxOf() != null) {
                // Should never get this, so log it and return the average only if they agree
                if(systemTWAS.getMinOf().equals(systemTWAS.getMaxOf())){
                    logger.warn("System TWAS {} is both min and max TW", twasURI);
                    return this.getTrustworthinessAttributeSet(systemTWAS.getMinOf(), graphs);
                } else {
                    logger.warn("System TWAS {} is both min and max TW referring to different average TWAS", twasURI);
                    return null;
                }
            }
        }
        
        // Normally we wouldn't expect to get an input TWAS that doesn't exist, so log a warning and return null
        logger.warn("System TWAS {} could not be found in graphs " + modelsToString(graphs), twasURI);
        return null;    
    }

    @Override
    public String generateControlSetUri(String controlURI, AssetDB asset) {
        return String.format("system#CS-%s-%s", controlURI.replace("domain#", ""), asset.getId());
    }

    @Override
    public String generateMisbehaviourSetUri(String misbehviourURI, AssetDB asset) {
        return String.format("system#MS-%s-%s", misbehviourURI.replace("domain#", ""), asset.getId());
    }

    @Override
    public String generateTrustworthinessAttributeSetUri(String twaURI, AssetDB asset) {
        return String.format("system#TWAS-%s-%s", twaURI.replace("domain#", ""), asset.getId());
    }

    /* "Calculates" highest TW levels using the average TW x population x highest TW lookup table.
     *
     * @param averageTW the input average case TW level
     * @param population the input population level
     * @param independent the flag to specify whether the levels are independent within the asset population
     * @return the highest TW level based on the inputs
     */
    @Override
    public LevelDB lookupHighestTWLevel(LevelDB averageTW, LevelDB population, boolean independent) {
        // If the levels within an asset population are not independent then the highest TW case is
        // equal to the average TW case.
        if (!independent){
            return averageTW;
        }

        // Lookup tables for different average TW, population and highest TW levels
        // For the three population levels of the built in default population scale
        int[][] lut5x3x5 = {
            {0, 2, 4},
            {1, 4, 4},
            {2, 4, 4},
            {3, 4, 4},
            {4, 4, 4}};

        // For TW levels ranging from Very Low to Very High
        int[][] lut5x5x5 = {
            { 0, 2, 4, 4, 4},
            { 1, 4, 4, 4, 4}, 
            { 2, 4, 4, 4, 4}, 
            { 3, 4, 4, 4, 4}, 
            { 4, 4, 4, 4, 4}};

        // For TW levels ranging from Very Low to Safe
        int[][] lut6x5x6 = {
            { 0, 2, 5, 5, 5},
            { 1, 5, 5, 5, 5},
            { 2, 5, 5, 5, 5},
            { 3, 5, 5, 5, 5},
            { 4, 5, 5, 5, 5},
            { 5, 5, 5, 5, 5}};
    
        int highestTWValue;

        int atw = averageTW.getLevelValue();
        int p = population.getLevelValue();

        // Ideally we would specify a lookup table label in the domain model.
        // For now, we'll assume that no two lookup tables have the same dimensions.
        // That means we can figure out which one to use based on the number of levels
        // in each scale.
        int sizet = trustworthinessLevels.size();
        int sizep = poLevels.size();
        if(sizet == 5 && sizep == 5){
            highestTWValue = lut5x5x5[atw][p];
        } else if (sizet == 6 && sizep == 5){
            highestTWValue = lut6x5x6[atw][p];
        } else if (sizet == 5 && sizep == 3){
            highestTWValue = lut5x3x5[atw][p];
        } else {
            // Should throw error, for now just return the averageTW
            return averageTW;
        }

        return trustworthinessLevels.get(highestTWValue);

    }

    /* "Calculates" lowest TW levels using the average TW x population x lowest TW lookup table.
     *
     * @param averageTW the input average case TW level
     * @param population the input population level
     * @param independent the flag to specify whether the levels are independent within the asset population
     * @return the lowest TW level based on the inputs
     */
    @Override
    public LevelDB lookupLowestTWLevel(LevelDB averageTW, LevelDB population, boolean independent) {
        // If the levels within an asset population are not independent then the lowest TW case is
        // equal to the average TW case.
        if (!independent){
            return averageTW;
        }

        // Lookup tables for different average TW, population and lowest TW levels
        // For the three population levels of the built in default population scale
        int[][] lut5x3x5 = {
            {0, 0, 0},
            {1, 0, 0},
            {2, 1, 0},
            {3, 2, 1},
            {4, 3, 2}};

        // For TW levels ranging from Very Low to Very High
        int[][] lut5x5x5 = {
            { 0, 0, 0, 0, 0},
            { 1, 0, 0, 0, 0}, 
            { 2, 1, 0, 0, 0}, 
            { 3, 2, 1, 0, 0}, 
            { 4, 3, 2, 1, 0}};

        // For TW levels ranging from Very Low to Safe
        int[][] lut6x5x6 = {
            { 0, 0, 0, 0, 0},
            { 1, 0, 0, 0, 0},
            { 2, 1, 0, 0, 0},
            { 3, 2, 1, 0, 0},
            { 4, 3, 2, 1, 0},
            { 5, 5, 5, 5, 5}};
    
        int lowestTWValue;

        int atw = averageTW.getLevelValue();
        int p = population.getLevelValue();

        // Ideally we would specify a lookup table label in the domain model.
        // For now, we'll assume that no two lookup tables have the same dimensions.
        // That means we can figure out which one to use based on the number of levels
        // in each scale.
        int sizet = trustworthinessLevels.size();
        int sizep = poLevels.size();
        if(sizet == 5 && sizep == 5){
            lowestTWValue = lut5x5x5[atw][p];
        } else if (sizet == 6 && sizep == 5){
            lowestTWValue = lut6x5x6[atw][p];
        } else if (sizet == 5 && sizep == 3){
            lowestTWValue = lut5x3x5[atw][p];
        } else {
            // Should throw error, for now just return the averageTW
            return averageTW;
        }

        return trustworthinessLevels.get(lowestTWValue);

    }

    /** Performs consistency checking calculations
     *
     * @param asset the AssetDB for the asset where these TW levels apply
     * @param defaultValue the default average TW level value for this level triplet and asset type
     * @param assertedValues an array of 3 possibly inconsistent, possibly null asserted TW level values
     * @return an array of 3 consistent TW level values with a non-null average level value
     */
    @Override
    public Integer[] getAdjustedLevels(AssetDB asset, Integer defaultLevel, Integer[] assertedValues){
        // Make a copy of the values which will be adjusted to ensure consistency
        Integer[] adjustedValues = new Integer[3];
        for(int i = 0; i < 3; i++)
            adjustedValues[i] = assertedValues[i];

        LevelDB popLevel = poLevels.get(asset.getPopulation());
        if (popLevel.getLevelValue() == 0){
            // All three levels should be the same, so go with the lowest user-entered level
            Integer minimumValue = null;
            for (Integer val : assertedValues) {
                if (val != null){
                    if(minimumValue == null){
                        minimumValue = val;
                    }
                    else if (minimumValue > val){
                        minimumValue = val;
                    }
                } 
            }
            
            // Set all the levels to the minimum value found (if any)
            if(minimumValue != null)
                for(int i = 0; i < 3; i++)
                    adjustedValues[i] = minimumValue;

            // If after this, the average level is still not defined (null), use the default
            if(adjustedValues[1] == null)
                adjustedValues[1] = defaultLevel;
            
        } else {
            // The levels must satisfy highest >= lowest, if not true, reduce lowest to match highest
            if(adjustedValues[0] != null && adjustedValues[2] != null && adjustedValues[0] > adjustedValues[2])
                adjustedValues[0] = adjustedValues[2];

            // The levels must satisfy highest >= average, if not true, reduce average to match highest
            if(adjustedValues[1] != null && adjustedValues[2] != null && adjustedValues[1] > adjustedValues[2])
                adjustedValues[1] = adjustedValues[2];
            
            // The levels must satisfy average >= lowest, if not true, reduce lowest to match average
            if(adjustedValues[0] != null && adjustedValues[1] != null && adjustedValues[0] > adjustedValues[1])
                adjustedValues[0] = adjustedValues[1];

            // If after this, the average level is still not defined (null), use the default plus adjustments
            if(adjustedValues[1] == null) {
                // Start with the default for this type of asset and attribute
                adjustedValues[1] = defaultLevel;
                
                // The new average must satisfy highest >= average, if not true, reduce average to match highest
                if(adjustedValues[2] != null && adjustedValues[1] > adjustedValues[2])
                    adjustedValues[1] = adjustedValues[2];

                // The new average must satisfy average >= lowest, if not true, increase average to match lowest
                if(adjustedValues[0] != null && adjustedValues[0] > adjustedValues[1])
                    adjustedValues[1] = adjustedValues[0];
            }

        }
        
        return adjustedValues;
        
    }

    /**
     * Query the 'domain' model for the type structure and store it in two type trees: a subclass tree and a superclass
     * tree.
     */
    private void createTypeTrees() {
        Map<String,List<String>> subTypeMap = new HashMap<>();
        Map<String,List<String>> superTypeMap = new HashMap<>();

        Model model = dataset.getNamedModel(stack.getGraph("domain"));

        dataset.begin(ReadWrite.READ);

        List<Resource> resources = new ArrayList<>();
        ResIterator resIterator = model.listResourcesWithProperty(RDFS.subClassOf);
        resIterator.forEachRemaining(resources::add);
        resIterator = model.listResourcesWithProperty(RDFS.subPropertyOf);
        resIterator.forEachRemaining(resources::add);

        for (Resource resource : resources) {
            List<Statement> statements = new ArrayList<>();
            resource.listProperties(RDFS.subClassOf).forEachRemaining(statements::add);
            resource.listProperties(RDFS.subPropertyOf).forEachRemaining(statements::add);

            for (Statement property : statements) {
                String typeUri = getShortName(property.getResource());

                List<String> subTypes = subTypeMap.computeIfAbsent(typeUri, k -> new ArrayList<>());
                subTypes.add(getShortName(property.getSubject()));

                List<String> superTypes = superTypeMap.computeIfAbsent(getShortName(property.getSubject()),
                        k -> new ArrayList<>());
                superTypes.add(typeUri);
            }
        }

        dataset.end();

        SuccessorsFunction<String> subTypeFunction = type -> subTypeMap.getOrDefault(type, new ArrayList<>());
        subTypeTree = Traverser.forTree(subTypeFunction);
        SuccessorsFunction<String> superTypeFunction = type -> superTypeMap.getOrDefault(type, new ArrayList<>());
        superTypeTree = Traverser.forTree(superTypeFunction);
    }

    @Override
    public List<String> getSubTypes(String type, boolean includeSelf) {
        return getTypeList(subTypeTree, type, includeSelf);
    }

    @Override
    public List<String> getSuperTypes(String type, boolean includeSelf) {
        return getTypeList(superTypeTree, type, includeSelf);
    }

    private List<String> getTypeList(Traverser<String> tree, String type, boolean includeSelf) {
        List<String> types = new ArrayList<>();

        tree.breadthFirst(type).forEach(types::add);
        if (!includeSelf) {
            types.remove(type);
        }
        return types;
    }

    @Override
    public Set<String> getConstructionState(){
        /* Detects domain model Asset and Relationship types that have a property core#isConstructionState.
         * This denotes that the asset/relationship is used only in the construction phase of validation.
         * 
         * The validator needs a list of these URIs, so it can decide which constructed assets and asset
         * relationships can be safely deleted at the end of the construction pattern sequence.
         * 
         * These will be compared with AssetDB.getType() and CardinalityConstraintDB.getLinkType(), which
         * return URIs with the short graph prefix, so keepGraph must be set to true. Although the domain
         * modeller currently emits these values only if set to true, strictly speaking we should exclude
         * the possibility that it is set to false.
         */

        Set<String> constructionState = new HashSet<>();
        
        String propertyName = getLongName("core#isConstructionState");
        Property property = ResourceFactory.createProperty(propertyName);

        dataset.begin(ReadWrite.READ);
        Model model = dataset.getNamedModel(stack.getGraph("domain"));
        ResIterator resIterator = model.listResourcesWithProperty(property);
        while (resIterator.hasNext()) {
            Resource resource = resIterator.next();
            JsonObject j = resourceToJson(resource, null);
            JsonElement e = j.get("isConstructionState");
            boolean state = e.getAsBoolean();
            if(state) constructionState.add(getShortName(resource, true));
        }
        dataset.end();
 
        return constructionState;
    }

    /* Name and URI converter methods: some explanation.
     *
     * There are four distinct names used for each entity:
     * - the class name, e.g. 'InferredAssetDB', obtained from entity.getClass().getSimpleName().
     * - the cache type reference, e.g. 'core#Asset'
     * - the short type URI, e.g. 'domain#Human', obtained from entity.getType()
     * - the full type URI, e.g. 'http://it-innovation.soton.ac.uk/security/trustworthiness/domain#Human'
     * 
     * The full type URI is always PREFIX + (short type URI), obtained via getLongName(short type URI).
     * 
     * The short type URI is usually the same as the cache type. However,
     * - for assets it is the domain model subtype
     * - for ontologies it is 'owl#Ontology'.
     * 
     * The cache type is usually equal to "core#" + (class name).replace("DB", ""). However:
     * - for assets, both AssetDB and InferredAssetDB entities are cached under core#Asset
     * - for LevelDB objects, the cache type depends on their purpose, which may be
     *   - core#PopulationLevel
     *   - core#Likelihood
     *   - core#TrustworthinessLevel
     *   - core#ImpactLevel
     *   - core#RiskLevel
     * 
     * There are private methods to handle entity persistence in all cases, which are called by other
     * public methods associated with each entity type. In most cases, the public methods extract the
     * different names via the usual method, then call the entity persistence methods. For exceptional
     * cases, the public methods inject different strings as appropriate.
     * 
     * The fact that these names are often the same can lead to some confusion. This was not helped by
     * mostly including only a subset from which the missing name could be derived. In this update the
     * names have been made explicit in all cases.
     */
    private String getCacheTypeName(EntityDB entity) {
        /* Works for all entity types except:
         * 
         * - InferredAssetDB, where the answer is core#Asset not core#InferredAsset
         * - LevelDB, where the answer depends on which scale the entity is in
         */
        return getCacheTypeName(entity.getClass());
    }
    private String getCacheTypeName(Class entityClass) {
        /* Works for all entity types except:
         * 
         * - InferredAssetDB, which has the same cache type as AssetDB
         * - LevelDB, where the answer depends on which scale the entity is in
         */
        String className = entityClass.getSimpleName();
        return getCacheTypeName(className);
    }    
    private String getCacheTypeName(String className) {
        if(className.equals("LevelDB")){
            // Error - should never be called for this class
            UnsupportedOperationException e = 
                new UnsupportedOperationException("Trying to autogenerate cache type key for a LevelDB class, which is not supported");
            throw e;
        }
        if(className.equals("InferredAssetDB")){
            // Treat inferred assets as any other assets
            className = "AssetDB";
        } 

        // Tack on the "core#" prefix and lose the trailing "DB" and return
        return "core#".concat(className.replace("DB", ""));

    }

    private String getLongName(String uri) {
        /**
         * Gets the full form of a short URI. Short URI must be of form {graph}#{local_name}.
         */
        if (uri.startsWith("http")) {
            return uri;
        }

        String[] uriSplit = uri.split("#");
        String prefix;
        if (uriSplit.length > 1) {
            prefix = prefixMap.get(uriSplit[0]);
            uri = prefix + uriSplit[1];
        } else {
            uri = PREFIX + uriSplit[0];
        }

        return uri;
    }
    private String getShortName(Resource resource, boolean keepGraph) {
        /**
         * Gets a short form of a Jena resources URI. Short form is either:
         * - keepGraph = true => {short graph name}#{local_name}
         * - keepGraph = false => {local_name}
         * 
         * Normally we want to retain the short graph name for resource URIs, as this makes it easier to
         * convert from a JSON object back to a JENA resource. We don't want it for properties as it allows
         * or easier conversion from a JSON object to a POJO using GSON.
         */

        String[] uriSplit = resource.getURI().split("#");
        String prefix;
        String name;

        if (uriSplit.length > 1) {
            prefix = prefixReverseMap.getOrDefault(uriSplit[0] + "#", "") + "#";
            name = uriSplit[1];
        } else {
            prefix = "";
            name = uriSplit[0];
        }

        // TODO: compatability workaround, delete AFTER REFACTOR, change EntityDB fields to match
        if ("label".equals(name) || "type".equals(name) || "comment".equals(name)) {
            keepGraph = true;
        }

        if (keepGraph) {
            return prefix + name;
        } else {
            return name;
        }
    }
    private String getShortName(Resource resource) {
        return getShortName(resource, true);
    }

    /**Get all entities of type `shortEntityType`, and return them as POJOs of type `entityClass`.
     * The `mainGraph` will be queried for the entities. The other `graphs` will then be queried
     * for additional triples associated with each retrieved entity, which will be merged in.
     * 
     * This should work for all entities except AssetDB and ModelDB, if the shortEntityType and
     * entityCacheType strings are set correctly.
     */
    private <T extends EntityDB> Map<String, T> getEntities(String shortEntityType, String entityCacheType, Class<T> entityClass, String... graphs) {
        Map<String, T> allEntities = new HashMap<>();
        List<String> queryGraphs = new ArrayList<>(Arrays.asList(graphs));

        if (cacheEnabled) {
            String[] queryArr = queryGraphs.toArray(new String[0]);
            // Get all entities of the specified type based on triples in all the graphs
            allEntities = cache.getAll(entityCacheType, entityClass, queryArr);

            // If all entities are in cache, return them without querying the triple store
            if (cache.checkTypeValid(entityCacheType, queryArr)) return allEntities;
        }

        Map<String, JsonObject> jsonEntities = new HashMap<>();
        Map<String, String> entityMainGraph = new HashMap<>();
        Map<String, List<String>> entitiesByGraph = new HashMap<>();

        dataset.begin(ReadWrite.READ);

        for (String queryGraph : queryGraphs) {
            String mainGraphUri = stack.getGraph(queryGraph);
            Model mainModel = dataset.getNamedModel(mainGraphUri);

            Resource typeResource = ResourceFactory.createResource(getLongName(shortEntityType));
            ResIterator resIterator = mainModel.listResourcesWithProperty(RDF.type, typeResource);
            while (resIterator.hasNext()) {
                // Get the resource of this type = one with an RDF.type property in this graph
                Resource resource = resIterator.nextResource();
                String uri = getShortName(resource);

                if (cache.checkEntityValid(uri, graphs)) {
                    // Don't bother to read and convert this entity in this graph
                    continue;
                }

                // Merge the resource (as Json) with any previously found resources
                JsonObject existingJsonObject = jsonEntities.get(uri);
                JsonObject jsonObject = resourceToJson(resource, existingJsonObject);
                jsonEntities.put(uri, jsonObject);

                /* Save the graph where the resource was found
                 *
                 * Note: this assumes there is only one 'main' graph per resource which is not
                 *       always true.
                 * TODO - decide if that is a problem, and if so, fix it.
                 */
                entityMainGraph.put(uri, queryGraph);

                // Save the resource URI in a list of those found in this graph
                List<String> entities = entitiesByGraph.computeIfAbsent(queryGraph, k ->  new ArrayList<>());
                entities.add(uri);
            }
        }

        // For each entity returned from the graphs, query the other graphs for 'loose' triples.
        for (String graph : graphs) {
            String graphUri = stack.getGraph(graph);
            Model model = dataset.getNamedModel(graphUri);

            for (Map.Entry<String, String> entityGraphEntry : entityMainGraph.entrySet()) {
                String entityUri = entityGraphEntry.getKey();
                String mainGraph = entityGraphEntry.getValue();

                if (!mainGraph.equals(graph)) {
                    JsonObject mainEntity = jsonEntities.get(entityUri);
                    Resource resource = model.getResource(getLongName(entityUri));
                    if (model.containsResource(resource)) {
                        // Merge the resource (as Json) with the previously found resources
                        resourceToJson(resource, mainEntity);
                    }
                }
            }
        }

        dataset.end();

        // Convert the Json entity to an EntityDB object, and save the graphs it came from
        for (JsonObject jsonEntity : jsonEntities.values()) {
            T entity = gson.fromJson(jsonEntity, entityClass);
            allEntities.put(entity.getUri(), entity);
            checkedOutEntityGraphs.put(System.identityHashCode(entity), graphs);
        }

        // Put the entities into the cache if they weren't already included there
        if (cacheEnabled) {
            for (String graph : entitiesByGraph.keySet()) {
                Map<String, EntityDB> cacheEntities = new HashMap<>();
                for (String entityUri : entitiesByGraph.get(graph)) {
                    cacheEntities.put(entityUri, allEntities.get(entityUri));
                }
                /* Cache these entities. Since this method got all entities of the specified type,
                 * the cache will be valid for that type, so pass in validateCache = true. 
                 */
                cache.cacheEntities(cacheEntities, entityCacheType, true, graph, graphs);
            }
        }

        //System.out.println("GOT " + allEntities.size() + " of " + entityType + " in " + Arrays.toString(graphs));
        return allEntities;
    }

    /**Get all entities of class `entityClass`, and return them as POJOs of type `entityClass`.
     * 
     * This uses the previous method, but sets the shortEntityType and entityCacheType in a way
     * that works for everything except AssetDB, LevelDB or ModelDB entities.
     */
    private <T extends EntityDB> Map<String, T> getEntities(Class<T> entityClass, String... graphs) {
        String entityCacheType = getCacheTypeName(entityClass);
        String shortEntityType = entityCacheType;
        return getEntities(shortEntityType, entityCacheType, entityClass, graphs);
    }

    /**Get one entity of type `entityClass`, and return it as a POJO of type `entityClass`
     * The `mainGraph` will be queried for the entity. The other `graphs` will then be queried
     * for additional triples associated with the same entity, which will be merged in.
     * 
     * The short entity type is not needed here - we specified the entity URI, so its type
     * can be found from the response. But we still need a cacheTypeName which must be set
     * correctly.
     */
    private <T extends EntityDB> T getEntity(String uri, String cacheTypeName, Class<T> entityClass, String... graphs) {
        if (cacheEnabled) {
            // Try to find the cached value
            T cachedEntity = cache.get(uri, cacheTypeName, entityClass, graphs);

            // Return the cached value including a null value if valid
            Boolean valid = cache.checkEntityValid(uri, graphs);
            if (cachedEntity != null || valid) {
                return cachedEntity;
            }
        }

        JsonObject returnJsonObject = getEntityAsJson(uri, graphs);
        if (returnJsonObject == null) {
            return null;
        }
        T entity = gson.fromJson(returnJsonObject, entityClass);
        checkedOutEntityGraphs.put(System.identityHashCode(entity), graphs);

        String mainGraph;
        if (returnJsonObject.has("_graph")) {
            mainGraph = returnJsonObject.getAsJsonPrimitive("_graph").getAsString();
        } else {
            mainGraph = graphs[0];
        }

        if (cacheEnabled) {
            cache.cacheEntity(entity, cacheTypeName, mainGraph, graphs);
        }

        return entity;
    }

    /**Get one entity of type `entityClass`, and return it as a POJO of type `entityClass`
     * 
     * This uses the previous method, but sets the entityCacheType in a way that works for
     * everything except AssetDB, LevelDB or ModelDB entities.
     */
    private <T extends EntityDB> T getEntity(String uri, Class<T> entityClass, String... graphs) {
        String cacheTypeName = getCacheTypeName(entityClass);
        return getEntity(uri, cacheTypeName, entityClass, graphs);
    }

    /**
     * Converts a JENA resource to a JsonObject. If 'existingJsonObject' is not null, then the newly created JsonObject
     * will be combined with it.
     */
    private JsonObject resourceToJson(Resource resource, JsonObject existingJsonObject) {
        String resUri = getShortName(resource, true);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("uri", resUri);
        for (StmtIterator it = resource.listProperties(); it.hasNext(); ) {
            Statement property = it.next();
            parseStatementToJson(property, jsonObject);
        }

        if (existingJsonObject != null) {
            jsonObject = combineJsonObjects(jsonObject, existingJsonObject);
        }

        return jsonObject;
    }

    private JsonObject combineJsonObjects(JsonObject from, JsonObject into) {
        // TODO: Handle case where same property has values in two different graphs (currently overrides, potentially
        //       better to add to array for resource properties

        for (Map.Entry<String, JsonElement> propertyEntry : from.entrySet()) {
            into.add(propertyEntry.getKey(), propertyEntry.getValue());
        }
        return into;
    }

    /**
     * Takes a Jena `statement` and puts it in `jsonObject`.
     */
    private void parseStatementToJson(Statement statement, JsonObject jsonObject) {
        String predicate = getShortName(statement.getPredicate(), false);
        RDFNode object = statement.getObject();

        if (predicate.equals("uri")) {
            return;
        }

        if (object.isLiteral()) {
            Literal literal = object.asLiteral();
            RDFDatatype type = literal.getDatatype();
            if (type.equals(XSDDatatype.XSDinteger)) {
                jsonObject.addProperty(predicate, literal.getInt());
            } else if (type.equals(XSDDatatype.XSDboolean)) {
                jsonObject.addProperty(predicate, literal.getBoolean());
            } else if (type.equals(XSDDatatype.XSDstring)) {
                jsonObject.addProperty(predicate, literal.getString());
            }
        } else if (object.isResource()) {
            String objectLocal = getShortName(object.asResource(), true);
            if (objectLocal == null) {
                return;
            }

            // Non-primitive properties are added to JsonArrays, for consistency. One-to-one and one-to-many
            // relations are later differentiated by GSON's JSON to POJO logic
            JsonArray property = jsonObject.getAsJsonArray(predicate);
            if (property == null) {
                property = new JsonArray();
                jsonObject.add(predicate, property);
            }

            property.add(objectLocal);
        }
    }

    private <T extends EntityDB> JsonObject getEntityAsJson(String uri, String... graphs) {
        boolean found = false;
        uri = getLongName(uri);

        JsonObject returnJsonObject = null;
        dataset.begin(ReadWrite.READ);
        for (String graph : graphs) {
            String graphUri = stack.getGraph(graph);
            if (graphUri == null) {
                continue;
            }

            Model model = dataset.getNamedModel(graphUri);
            Resource resource = model.getResource(uri);

            /* 
             * org.apache.jena.rdf.model.Model has changed. The createResource(String uri) method now returns
             * an existing resource if already defined in the Model. The getResource method is now identical,
             * so it creates and returns an empty resource if there is no resource with the specified URI in
             * the model.
             * 
             * The problem is that createResource associates the new resource with the model, so the call to
             * model.containsResource returns true even if the model didn't contain this resource. So if the
             * URI is not defined in any graph, we will still get a non-null JSon object back, albeit one in
             * which there is no "_graph" member (nor any other member).
             * 
             * It is at least arguable that in that case we should return 'null' which was probably what Lee
             * expected would happen when he wrote this code. That may cause other problems (e.g., if untyped
             * resources are retrieved by this method, which is possible), so we'll try to detect where there
             * was no resource later, at the point of use.
             */
            if (model.containsResource(resource)) {
                returnJsonObject = resourceToJson(resource, returnJsonObject);
                if (resource.hasProperty(RDF.type)) {
                    // Add a special property tracking the graph in which the type was found
                    returnJsonObject.addProperty("_graph", graph);
                    found = true;
                }
            }
        }
        dataset.end();

        return returnJsonObject;
    }

    /**
     * Update the given JENA 'resource' to match the given 'jsonObject'.
     */
    private void parseJsonToResource(JsonObject jsonObject, Resource resource) {

        // TODO: May be faster to only remove properties which are different, but JENA may handle this on it's own
        //  when commit() is called.
        resource.removeProperties();

        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String propertyName = entry.getKey();
            JsonElement valueElement = entry.getValue();

            // Properties without an explicit graph declaration are assumed to be core graph properties
            if (!propertyName.contains("#")) {
                propertyName = "core#" + propertyName;
            }
            propertyName = getLongName(propertyName);

            List<JsonElement> values = new ArrayList<>();
            if (valueElement.isJsonArray()) {
                JsonArray jsonArray = valueElement.getAsJsonArray();
                jsonArray.forEach(values::add);
            } else {
                values.add(valueElement);
            }

            Property property = ResourceFactory.createProperty(propertyName);

            for (JsonElement value : values) {
                JsonPrimitive primitive = value.getAsJsonPrimitive();
                if (primitive.isString()) {
                    String objectString = primitive.getAsString();
                    if (objectString.contains("#")) {
                        // Statement object is (probably) a URI
                        Resource objectResource = ResourceFactory.createResource(getLongName(objectString));
                        resource.addProperty(property, objectResource);
                    } else {
                        // TODO: HACK: Escape slashes in strings are added to if saved multiple times, this is a
                        //  temporary hack fix for this.
                        objectString = objectString.replace("\\", "");

                        resource.addProperty(property, objectString);
                    }
                } else if (primitive.isBoolean()) {
                    resource.addLiteral(property, primitive.getAsBoolean());
                } else if (primitive.isNumber()) {
                    resource.addLiteral(property, primitive.getAsNumber());
                }
            }
        }
    }

    /* METHODS FOR RETRIEVING ENTITIES
     * 
     * There are two basic types of methods:
     * 
     * - A single entity method which retrieves an entity with a specific URI. This deserialises onto an
     *   EntityDB object belonging to a subclass specified in the method.
     * - A multiple entity method which retrieves all entities with a specific type, and deserialises to
     *   a HashMap, with keys equal to URI of the retrieved entities.
     * 
     * The first type of method could be passed a URI that corresponds to an entity of the wrong type. It
     * seems this is not checked for, so presumably the resulting object would have:
     * 
     * - member variables set to null where the RDF did not contain the corresponding property
     * - no information about properties that don't correspond to member variables
     * 
     * The second type of method doesn't have that problem because it will only retrieve objects of the
     * correct rdf:type, which is created within the method and passed to the generic getEntities method.
     * Because both methods can cache whatever they find, both require a cache type key to be generated.
     * In most cases, both these type strings are related in a simple way to the EntityDB subclass name.
     * 
     * The issue here is that there are exceptions, and the rdf:type may vary depending which graph is
     * being queried. The main problems are with assets and relationships:
     * 
     * - system model assets rdf:type can be any domain model subtype of 'core#Asset', and they can be
     *   deserialised as AssetDB or InferredAssetDB entities, but all have cache type key 'core#Asset'.
     * - system model asset relationships are represented as CardinalityConstraintDB objects, but in 
     *   RDF are represented by a CardinalityConstraint entity, plus a property of the source asset.
     * - system model AssetDB and InferredAssetDB have a list of CardinalityConstraintDB objects that
     *   represent the asset relationships, but these are not included in the RDF entities.
     * - domain model assets and relationships are totally different from the above
     * 
     * Consequently, there custom methods plus some private helper methods are used for deserialising
     * and (for system models) serialising assets and relationships.
     * 
     * The same is true for the ModelDB entity which holds attributes of entire system or domain models. 
     */

    @Override
    public AssetDB getAsset(String uri, String... models) {
        /* Assets are a special case for two reasons:
         *
         *  (a) they are not saved in the store as type 'Asset', but as a sub-type of 'Asset', yet we
         *      still need to store them under 'AssetDB' in the cache
         *  (b) they have links to other assets defined by special predicates which differ for each
         *      domain model
        */
        String cacheTypeKey = getCacheTypeName(AssetDB.class);

        if (cacheEnabled) {
            // Try to find the cached asset
            AssetDB cachedEntity = cache.get(uri, cacheTypeKey, AssetDB.class, models);

            // Return the cached value including a null value if valid
            Boolean valid = cache.checkEntityValid(uri, models);
            if (cachedEntity != null || valid) {
                return cachedEntity;
            }

        }

        // Not in the cache, so try to get it from the triple store
        JsonObject assetJson = getEntityAsJson(uri, models);

        AssetDB asset = null;

        if (assetJson != null) {
            // Find out in which graph the asset was defined (asserted or inferred)
            JsonPrimitive assetGraph = assetJson.getAsJsonPrimitive("_graph");
            if (assetGraph != null) {
                // Convert this graph to a string
                String mainGraph = assetGraph.getAsString();

                // Determine if asset is inferred
                boolean inferred = mainGraph.equals("system-inf");

                // Add inferred flag to JSON object, to indicate which asset object type to create in jsonToAsset below 
                assetJson.addProperty("inferred", inferred);

                // Convert the asset to an AssetDB (or InferredAssetDB) object to be returned
                asset = jsonToAsset(assetJson);

                // Add the asset to the cache (in the main graph) if enabled
                if (cacheEnabled) {
                    cache.cacheEntity(asset, cacheTypeKey, mainGraph, models);
                }

            }

        }

        return asset;

    }

    @Override
    public Map<String, AssetDB> getAssets(String... models) {
        /* Assets are a special case for two reasons:
         *
         *  (a) they are not saved in the store as type 'Asset', but as a sub-type of 'Asset', yet we
         *      still need to store them under 'AssetDB' in the cache
         *  (b) they have links to other assets defined by special predicates which differ for each
         *      domain model
        */

        Map<String, AssetDB> assets = new HashMap<>();

        List<String> queryModels = Arrays.asList(models);

        String assetTypeKey = getCacheTypeName(AssetDB.class);
        if (cacheEnabled) {
            queryModels = new ArrayList<>();
            for (String model : models) {
                Map<String, AssetDB> cachedAssets = cache.getAll(assetTypeKey, AssetDB.class, model);
                assets.putAll(cachedAssets);
                if (!cache.checkTypeValid(assetTypeKey, model)) {
                    queryModels.add(model);
                }
            }
        }

        String [] queryModelsArray = new String[queryModels.size()];
        queryModels.toArray(queryModelsArray);

        Map<String, Map<String, JsonObject>> assetJsonObjectsByModel = getAssetJsonObjects(queryModelsArray);
        for (String model : queryModelsArray) {
            Map<String, EntityDB> modelAssets = new HashMap<>();

            Map<String, JsonObject> assetJsonObjects = assetJsonObjectsByModel.get(model);
            for (JsonObject assetJsonObject : assetJsonObjects.values()) {
                // `jsonToAsset()` method handles special case (b)
                AssetDB asset = jsonToAsset(assetJsonObject);
                assets.put(asset.getUri(), asset);
                modelAssets.put(asset.getUri(), asset);
            }

            if (cacheEnabled) {
                /* Cache these entities. Since this method got all entities of the specified type,
                 * the cache will be valid for that type, so pass in validateCache = true. 
                 */
                cache.cacheEntities(modelAssets, assetTypeKey, true, model, models);

                /*
                 * No need to connect links to the assets, because if any of their links had been loaded
                 * then the assets would also have been loaded.
                 * 
                 * Any links not yet loaded will be connected when the link is loaded.
                 */

            }
        }

        return assets;
    }

    /**Query the database for all entities with an asset type. An asset type is any sub-type of `core#Asset`.
     * Assets will be returned as a GSON JsonObject representation of the asset entity.
     * Returns a Map: graph sURI -> Asset sURI -> Asset JsonObject.
     * 
     * This doesn't work for domain model asset classes, which are not all sub-types of 'core#Asset', but
     * are all sub-types of 'owl#Class'.
     */
    private Map<String, Map<String, JsonObject>> getAssetJsonObjects(String... models) {
        List<String> assetTypes = getSubTypes("core#Asset", false);

        Map<String, Map<String, JsonObject>> assetJsonObjectsByGraph = new HashMap<>();

        dataset.begin(ReadWrite.READ);
        for (String graph : models) {
            boolean inferred = graph.equals("system-inf");

            Map<String, JsonObject> assetJsonObjects = new HashMap<>();
            assetJsonObjectsByGraph.put(graph, assetJsonObjects);

            Model model = dataset.getNamedModel(stack.getGraph(graph));

            for (String assetType : assetTypes) {
                Resource typeResource = ResourceFactory.createResource(getLongName(assetType));
                ResIterator resIterator = model.listResourcesWithProperty(RDF.type, typeResource);
                while (resIterator.hasNext()) {
                    Resource resource = resIterator.nextResource();
                    String uri = getShortName(resource);
                    JsonObject jsonObject = resourceToJson(resource, assetJsonObjects.get(uri));
                    /*
                     * Add a special property to indicate to later processing that the JsonObject
                     * should be parsed to a InferredAssetDB POJO. ( see: jsonToAsset() )
                     */
                    jsonObject.addProperty("inferred", inferred);
                    assetJsonObjects.put(uri, jsonObject);
                }
            }
        }
        dataset.end();

        return assetJsonObjectsByGraph;
    }

    /**Converts a naive JSON representation of an asset (i.e. directly from the `resourceToJson()` method) 
     * to one which better corresponds with the AssetDB object. This is necessary as assets are a special
     * case due to their many potential links with other assets.
     */
    private AssetDB jsonToAsset(JsonObject jsonObject) {
        jsonObject.add("linksFrom", new JsonArray());
        jsonObject.add("linksTo", new JsonArray());

        Type assetClassType;
        if (jsonObject.has("inferred") && jsonObject.get("inferred").isJsonPrimitive()) {
            assetClassType = jsonObject.getAsJsonPrimitive("inferred").getAsBoolean() ?
                    InferredAssetDB.class : AssetDB.class;
        } else {
            assetClassType = AssetDB.class;
        }

        AssetDB asset = gson.fromJson(jsonObject, assetClassType);

        return asset;
    }

    @Override
    public Map<String, CardinalityConstraintDB> getCardinalityConstraints(String... models) {
        /* The default short type URI and cache type should work in this case,
         * but we do need to do some post-processing.
         */
        Map<String, CardinalityConstraintDB> links = getEntities(CardinalityConstraintDB.class, models);

        // Create the connections between cached assets and cardinality constraints
        if(links != null) for(CardinalityConstraintDB link : links.values()){
            List<String> linksFromSource = linksFromAsset.computeIfAbsent(link.getLinksFrom(), K -> new ArrayList<>());
            List<String> linksToTarget = linksToAsset.computeIfAbsent(link.getLinksTo(), K -> new ArrayList<>());
            if(!linksFromSource.contains(link.getUri())) linksFromSource.add(link.getUri());
            if(!linksToTarget.contains(link.getUri())) linksToTarget.add(link.getUri());
        }

        return links;
    }

    @Override
    public CardinalityConstraintDB getCardinalityConstraint(String uri, String... models) {
        /* The default short type URI and cache type should work in this case,
         * but we do need to do some post-processing.
         */
        CardinalityConstraintDB link = getEntity(uri, CardinalityConstraintDB.class, models);

        // Create the connections between cached assets and cardinality constraints
        if(link != null) {
            List<String> linksFromSource = linksFromAsset.computeIfAbsent(link.getLinksFrom(), K -> new ArrayList<>());
            List<String> linksToTarget = linksToAsset.computeIfAbsent(link.getLinksTo(), K -> new ArrayList<>());
            if(!linksFromSource.contains(link.getUri())) linksFromSource.add(link.getUri());
            if(!linksToTarget.contains(link.getUri())) linksToTarget.add(link.getUri());
        }

        return link;

    }

    @Override
    public RoleDB getRole(String uri, String... models) {
        // The default cache type should work in this case
        String entityCacheType = getCacheTypeName(RoleDB.class);

        // No need for any pre- or post-processing
        return getEntity(uri, entityCacheType, RoleDB.class, models);
    }

    @Override
    public Map<String, RoleDB> getRoles(String... models) {
        /* The default short type URI and cache type should work in this case,
         * and there is no need for any pre- or post-processing.
         */
        return getEntities(RoleDB.class, models);
    }

    @Override
    public RoleLinkDB getRoleLink(String uri, String... models) {
        // The default cache type should work in this case
        String entityCacheType = getCacheTypeName(RoleDB.class);

        // No need for any pre- or post-processing
        return getEntity(uri, entityCacheType, RoleLinkDB.class, models);
    }

    @Override
    public Map<String, RoleLinkDB> getRoleLinks(String... models) {
        /* The default short type URI and cache type should work in this case,
         * and there is no need for any pre- or post-processing.
         */
        return getEntities(RoleLinkDB.class, models);
    }

    @Override
    public ConstructionPatternDB getConstructionPattern(String uri, String... models) {
        // The default cache type should work in this case
        String entityCacheType = getCacheTypeName(ConstructionPatternDB.class);
        
        // No need for any pre- or post-processing
        return getEntity(uri, entityCacheType, ConstructionPatternDB.class, models);
    }

    @Override
    public Map<String, ConstructionPatternDB> getConstructionPatterns(String... models) {
        /* The default short type URI and cache type should work in this case,
         * and there is no need for any pre- or post-processing.
         */
        return getEntities(ConstructionPatternDB.class, models);
    }

    @Override
    public MatchingPatternDB getMatchingPattern(String uri, String... models) {
        // Get the matching pattern but don't return immediately unless the result is null
        MatchingPatternDB entity = getEntity(uri, MatchingPatternDB.class, models);
        if(entity == null){
            logger.warn("No matching pattern found at uri {} in graphs " + modelsToString(models), uri);
            return null;
        }

        // Find out if we are loading a system model matching pattern (stored in "system-inf")
        boolean systemMP = false;
        String [] modelsArray = models.clone();
        for(String model : modelsArray){
            if(model.equals("system-inf")) {
                systemMP = true;
                break;
            }
        }

        // If it is a system model pattern, we should create an entry for it in the node map
        if(systemMP){
            // Get the map of nodes for this matching pattern
            Map<String, List<String>> nodesByRoleThisMP = nodesByRoleByMP.computeIfAbsent(entity.getUri(), K -> new HashMap<>());
            
            // Matching patterns can't change so the map only needs filling if it is empty
            if(nodesByRoleThisMP.isEmpty()){
                for(String nodeURI : entity.getUniqueNodes()){
                    NodeDB node = getNode(nodeURI, models);
                    boolean ok = addMatchingPatternNodeToMap(node, nodesByRoleThisMP);
                    if(!ok) {
                        logger.warn("Tried to add a null unique node to the map for pattern {}", entity.getUri());
                    }
                }
                for(String nodeURI : entity.getNecessaryNodes()){
                    NodeDB node = getNode(nodeURI, models);
                    boolean ok = addMatchingPatternNodeToMap(node, nodesByRoleThisMP);
                    if(!ok) {
                        logger.warn("Tried to add a null necessary node to the map for pattern {}", entity.getUri());
                    }
                }
                for(String nodeURI : entity.getSufficientNodes()){
                    NodeDB node = getNode(nodeURI, models);
                    boolean ok = addMatchingPatternNodeToMap(node, nodesByRoleThisMP);
                    if(!ok) {
                        logger.warn("Tried to add a null sufficient node to the map for pattern {}", entity.getUri());
                    }
                }
                for(String nodeURI : entity.getOptionalNodes()){
                    NodeDB node = getNode(nodeURI, models);
                    boolean ok = addMatchingPatternNodeToMap(node, nodesByRoleThisMP);
                    if(!ok) {
                        logger.warn("Tried to add a null optional node to the map for pattern {}", entity.getUri());
                    }
                }
            }
        }
         
        return entity;
    }

    @Override
    public Map<String, MatchingPatternDB> getMatchingPatterns(String... models) {
        // Get the matching pattern but don't return immediately unless the result is null
        Map<String, MatchingPatternDB> entities = getEntities(MatchingPatternDB.class, models);
        if(entities == null){
            return null;
        }

        // Find out if we are loading a system model matching pattern (stored in "system-inf")
        boolean systemMP = false;
        String [] modelsArray = models.clone();
        for(String model : modelsArray){
            if(model.equals("system-inf")) {
                systemMP = true;
                break;
            }
        }

        // If it is a system model pattern, we should create an entry for it in the node map
        if(systemMP){
            for(MatchingPatternDB entity : entities.values()){
                // Get the map of nodes for this matching pattern
                Map<String, List<String>> nodesByRoleThisMP = nodesByRoleByMP.computeIfAbsent(entity.getUri(), K -> new HashMap<>());
                
                // Matching patterns can't change so the map only needs filling if it is empty
                if(nodesByRoleThisMP.isEmpty()){
                    for(String nodeURI : entity.getUniqueNodes()){
                        NodeDB node = getNode(nodeURI, models);
                        boolean ok = addMatchingPatternNodeToMap(node, nodesByRoleThisMP);
                        if(!ok) {
                            logger.warn("Tried to add a null unique node to the map for pattern {}", entity.getUri());
                        }
                    }
                    for(String nodeURI : entity.getNecessaryNodes()){
                        NodeDB node = getNode(nodeURI, models);
                        boolean ok = addMatchingPatternNodeToMap(node, nodesByRoleThisMP);
                        if(!ok) {
                            logger.warn("Tried to add a null necessary node to the map for pattern {}", entity.getUri());
                        }
                    }
                    for(String nodeURI : entity.getSufficientNodes()){
                        NodeDB node = getNode(nodeURI, models);
                        boolean ok = addMatchingPatternNodeToMap(node, nodesByRoleThisMP);
                        if(!ok) {
                            logger.warn("Tried to add a null sufficient node to the map for pattern {}", entity.getUri());
                        }
                    }
                    for(String nodeURI : entity.getOptionalNodes()){
                        NodeDB node = getNode(nodeURI, models);
                        boolean ok = addMatchingPatternNodeToMap(node, nodesByRoleThisMP);
                        if(!ok) {
                            logger.warn("Tried to add a null optional node to the map for pattern {}", entity.getUri());
                        }
                    }
                }
            }
        }
         
        return entities;

    }

    @Override
    public RootPatternDB getRootPattern(String uri, String... models) {
        return getEntity(uri, RootPatternDB.class, models);
    }

    @Override
    public Map<String, RootPatternDB> getRootPatterns(String... models) {
        /* The default short type URI and cache type should work in this case,
         * and there is no need for any pre- or post-processing.
         */
        return getEntities(RootPatternDB.class, models);
    }

    @Override
    public NodeDB getNode(String uri, String... models) {
        return getEntity(uri, NodeDB.class, models);
    }

    @Override
    public Map<String, NodeDB> getNodes(String... models) {
        /* The default short type URI and cache type should work in this case,
         * and there is no need for any pre- or post-processing.
         */
        return getEntities(NodeDB.class, models);
    }

    @Override
    public InferredNodeSettingDB getInferredNodeSetting(String uri, String... models) {
        return getEntity(uri, InferredNodeSettingDB.class, models);
    }

    @Override
    public Map<String, InferredNodeSettingDB> getInferredNodeSettings(String... models) {
        /* The default short type URI and cache type should work in this case,
         * and there is no need for any pre- or post-processing.
         */
        return getEntities(InferredNodeSettingDB.class, models);
    }

    @Override
    public ThreatDB getThreat(String uri, String... models) {
        return getEntity(uri, ThreatDB.class, models);
    }

    @Override
    public Map<String, ThreatDB> getThreats(String... models) {
        /* The default short type URI and cache type should work in this case,
         * and there is no need for any pre- or post-processing.
         */
        return getEntities(ThreatDB.class, models);
    }

    @Override
    public MisbehaviourDB getMisbehaviour(String uri, String... models) {
        return getEntity(uri, MisbehaviourDB.class, models);
    }

    @Override
    public Map<String, MisbehaviourDB> getMisbehaviours(String... models) {
        /* The default short type URI and cache type should work in this case,
         * and there is no need for any pre- or post-processing.
         */
        return getEntities(MisbehaviourDB.class, models);
    }

    @Override
    public MisbehaviourSetDB getMisbehaviourSet(String uri, String... models) {
        return getEntity(uri, MisbehaviourSetDB.class, models);
    }

    @Override
    public MisbehaviourSetDB getMisbehaviourSet(AssetDB asset, String misbehaviour, String... models) {
        // Generate the URI using the same method as the Validator 'createMisbehaviourSet' method
        String uri = String.format("system#MS-%s-%s", misbehaviour.replace("domain#", ""), asset.getId());        
        return getEntity(uri, MisbehaviourSetDB.class, models);
    }

    @Override
    public Map<String, MisbehaviourSetDB> getMisbehaviourSets(String... models) {
        /* The default short type URI and cache type should work in this case,
         * and there is no need for any pre- or post-processing.
         */
        return getEntities(MisbehaviourSetDB.class, models);
    }

    /* TODO : pre-compute the settings for each asset type based on the class hierarchy,
     *        instead of searching the class hierarchy every time we need a setting
     */
    private String getSettingURI(List<String> superTypes, String settingType, Map<String, Map<String, String>> settingsMap) {
        if(superTypes != null){
            // Try to find a setting belonging to a parent type (usually there is only one parent with a setting) 
            for(String parentType : superTypes) {
                String settingURI = getSettingURI(parentType, settingType, settingsMap);
                if(settingURI != null) {
                    return settingURI;
                }
            }            
        }
        return null;
    }

    private String getSettingURI(String assetType, String settingType, Map<String, Map<String, String>> settingsMap) {
        String settingURI;
        // Get the settings for this asset type
        Map<String, String> settings = settingsMap.get(assetType);
        if(settings != null) {
            // Get the asset's setting for this setting type
            settingURI = settings.get(settingType);
            if(settingURI != null){
                return settingURI;
            }
        }

        // No setting for this asset type, so recursively check parent types
        List<String> superTypes = getSuperTypes(assetType, false);
        settingURI = getSettingURI(superTypes, settingType, settingsMap);
        return settingURI;
    }

    @Override
    public CASettingDB getCASetting(AssetDB asset, String controlURI) {
        String settingURI = getSettingURI(asset.getType(), controlURI, defaultByControlByAsset);
        if(settingURI != null) {
            return getCASetting(settingURI);
        } else {
            return null;
        }
    }

    @Override
    public CASettingDB getCASetting(String uri) {
        /* The default short type URI and cache type should work in this case,
         * and there is no need for any pre- or post-processing.
         * 
         * Results can only come from the domain model.
         */
        return getEntity(uri, CASettingDB.class, "domain");
    }

    @Override
    public Map<String, CASettingDB> getCASettings() {
        /* The default short type URI and cache type should work in this case,
         * and there is no need for any pre- or post-processing.
         * 
         * Results can only come from the domain model.
         */
        return getEntities(CASettingDB.class, "domain");
    }

    @Override
    public MADefaultSettingDB getMADefaultSetting(AssetDB asset, String misbehaviourURI) {
        String settingURI = getSettingURI(asset.getType(), misbehaviourURI, defaultByMisbehaviourByAsset);
        if(settingURI != null) {
            return getMADefaultSetting(settingURI);
        } else {
            return null;
        }
    }

    @Override
    public MADefaultSettingDB getMADefaultSetting(String uri) {
        /* The default short type URI and cache type should work in this case,
         * and there is no need for any pre- or post-processing.
         * 
         * Results can only come from the domain model.
         */
        return getEntity(uri, MADefaultSettingDB.class, "domain");
    }

    @Override
    public Map<String, MADefaultSettingDB> getMADefaultSettings() {
        /* The default short type URI and cache type should work in this case,
         * and there is no need for any pre- or post-processing.
         * 
         * Results can only come from the domain model.
         */
        return getEntities(MADefaultSettingDB.class, "domain");
    }

    @Override
    public TWAADefaultSettingDB getTWAADefaultSetting(AssetDB asset, String twaURI) {
        String settingURI = getSettingURI(asset.getType(), twaURI, defaultByTwaByAsset);
        if(settingURI != null) {
            return getTWAADefaultSetting(settingURI);
        } else {
            return null;
        }
    }

    @Override
    public TWAADefaultSettingDB getTWAADefaultSetting(String uri) {
        /* This is a domain model entity only */
        return getEntity(uri, TWAADefaultSettingDB.class, "domain");
    }

    @Override
    public Map<String, TWAADefaultSettingDB> getTWAADefaultSettings() {
        /* The default short type URI and cache type should work in this case,
         * and there is no need for any pre- or post-processing.
         * 
         * Results can only come from the domain model.
         */
        return getEntities(TWAADefaultSettingDB.class, "domain");
    }

    @Override
    public LevelDB getPopulationLevel(String uri) {
        /* This case requires special treatment, because if the domain model is too old to
         * have a population scale, one must be created.
         * 
         * To ensure that happens, if a single population level is requested, the full scale
         * must first be loaded/created, and then the result found within the scale.
         */
        Map<String, LevelDB> poLevels = getPopulationLevels();
        return poLevels.get(uri);
    }

    @Override
    public Map<String, LevelDB> getPopulationLevels() {
        /* The short type URI and cache type must be set separately in this case, and the results
         * can only come from the domain model.
         * 
         * To support backward compatibility, if the domain model is too old to have a population
         * scale, a default scale must be created.
         */
        String entityCacheType = "core#PopulationLevel";
        String shortEntityType = "core#PopulationLevel";
        Map<String, LevelDB> poLevels = getEntities(shortEntityType, entityCacheType, LevelDB.class, "domain");

        /* If there are no population levels, this must be an old domain model. In
         * that case, insert a default scale to support backward compatibility.
         */
        if(poLevels.isEmpty()) {
            LevelDB p0 = new LevelDB();
            LevelDB p1 = new LevelDB();
            LevelDB p2 = new LevelDB();

            p0.setUri("domain#PopLevelSingleton");      // Use this for singleton assets
            p1.setUri("domain#PopLevelFew");            // Use this for non-singletons with max cardinality > 0
            p2.setUri("domain#PopLevelMany");           // Use this for non-singletons with max cardinality = -1

            p0.setLabel("A singleton asset");
            p1.setLabel("A few assets");
            p2.setLabel("Many assets");

            p0.setDescription("A singleton asset");
            p1.setDescription("A few assets");
            p2.setDescription("Many assets");

            p0.setLevelValue(0);
            p1.setLevelValue(1);
            p2.setLevelValue(2);

            poLevels.put(p0.getUri(), p0);
            poLevels.put(p1.getUri(), p1);
            poLevels.put(p2.getUri(), p2);

            if(cacheEnabled) {
                Map<String, EntityDB> levels = new HashMap<>();
                for(LevelDB p : poLevels.values()){
                    levels.put(p.getUri(), (EntityDB)p);
                }
                String[] graphs = {"domain"};
                /* Cache these entities. Since this method got all entities of the specified type,
                 * the cache will be valid for that type, so pass in validateCache = true. 
                 */
                cache.cacheEntities(levels, entityCacheType, true, "domain", graphs);
            }
        }


        return poLevels;
    }

    @Override
    public LevelDB getImpactLevel(String uri) {
        /* The cache type must be set separately in this case, and the results
         * can only come from the domain model.
         */
        String entityCacheType = "core#ImpactLevel";
        return getEntity(uri, entityCacheType, LevelDB.class, "domain");
    }

    @Override
    public Map<String, LevelDB> getImpactLevels() {
        /* The short type URI and cache type must be set separately in this case, and the results
         * can only come from the domain model.
         */
        String entityCacheType = "core#ImpactLevel";
        String shortEntityType = "core#ImpactLevel";
        return getEntities(shortEntityType, entityCacheType, LevelDB.class, "domain");
    }

    @Override
    public LevelDB getTrustworthinessLevel(String uri) {
        /* The cache type must be set separately in this case, and the results
         * can only come from the domain model.
         */
        String entityCacheType = "core#TrustworthinessLevel";
        return getEntity(uri, entityCacheType, LevelDB.class, "domain");
    }

    @Override
    public Map<String, LevelDB> getTrustworthinessLevels() {
        /* The short type URI and cache type must be set separately in this case, and the results
         * can only come from the domain model.
         */
        String entityCacheType = "core#TrustworthinessLevel";
        String shortEntityType = "core#TrustworthinessLevel";
        return getEntities(shortEntityType, entityCacheType, LevelDB.class, "domain");
    }

    @Override
    public LevelDB getLikelihoodLevel(String uri) {
        /* The cache type must be set separately in this case, and the results
         * can only come from the domain model.
         */
        String entityCacheType = "core#Likelihood";
        return getEntity(uri, entityCacheType, LevelDB.class, "domain");
    }

    @Override
    public Map<String, LevelDB> getLikelihoodLevels() {
        /* The short type URI and cache type must be set separately in this case, and the results
         * can only come from the domain model.
         */
        String entityCacheType = "core#Likelihood";
        String shortEntityType = "core#Likelihood";
        return getEntities(shortEntityType, entityCacheType, LevelDB.class, "domain");
    }

    @Override
    public LevelDB getRiskLevel(String uri) {
        /* The cache type must be set separately in this case, and the results
         * can only come from the domain model.
         */
        String entityCacheType = "core#RiskLevel";
        return getEntity(uri, entityCacheType, LevelDB.class, "domain");
    }

    @Override
    public Map<String, LevelDB> getRiskLevels() {
        /* The short type URI and cache type must be set separately in this case, and the results
         * can only come from the domain model.
         */
        String entityCacheType = "core#RiskLevel";
        String shortEntityType = "core#RiskLevel";
        return getEntities(shortEntityType, entityCacheType, LevelDB.class, "domain");
    }

    @Override
    public TrustworthinessAttributeSetDB getTrustworthinessAttributeSet(AssetDB asset, String twa, String... models) {
        // Generate the URI using the same method as the Validator 'createEntryPoint' method
        String uri = String.format("system#TWAS-%s-%s", twa.replace("domain#", ""), asset.getId());        
        return getEntity(uri, TrustworthinessAttributeSetDB.class, models);
    }

    @Override
    public TrustworthinessAttributeSetDB getTrustworthinessAttributeSet(String uri, String... models) {
        return getEntity(uri, TrustworthinessAttributeSetDB.class, models);
    }

    @Override
    public Map<String, TrustworthinessAttributeSetDB> getTrustworthinessAttributeSets(String... models) {
        return getEntities(TrustworthinessAttributeSetDB.class, models);
    }

    @Override
    public TrustworthinessAttributeDB getTrustworthinessAttribute(String uri, String... models) {
        return getEntity(uri, TrustworthinessAttributeDB.class, models);
    }

    @Override
    public Map<String, TrustworthinessAttributeDB> getTrustworthinessAttributes(String... models) {
        return getEntities(TrustworthinessAttributeDB.class, models);
    }

    @Override
    public MisbehaviourInhibitionSetDB getMisbehaviourInhibitionSet(String uri, String... models) {
        return getEntity(uri, MisbehaviourInhibitionSetDB.class, models);
    }

    @Override
    public Map<String, MisbehaviourInhibitionSetDB> getMisbehaviourInhibitionSets(String... models) {
        return getEntities(MisbehaviourInhibitionSetDB.class, models);
    }

    @Override
    public TrustworthinessImpactSetDB getTrustworthinessImpactSet(String uri, String... models) {
        return getEntity(uri, TrustworthinessImpactSetDB.class, models);
    }

    @Override
    public Map<String, TrustworthinessImpactSetDB> getTrustworthinessImpactSets(String... models) {
        return getEntities(TrustworthinessImpactSetDB.class, models);
    }

    @Override
    public Map<String, ControlStrategyDB> getControlStrategies(String... models) {
        return getEntities(ControlStrategyDB.class, models);
    }

    @Override
    public ControlStrategyDB getControlStrategy(String uri, String... models) {
        return getEntity(uri, ControlStrategyDB.class, models);
    }

    @Override
    public Map<String, ControlSetDB> getControlSets(String... models) {
        return getEntities(ControlSetDB.class, models);
    }

    @Override
    public ControlSetDB getControlSet(String uri, String... models) {
        return getEntity(uri, ControlSetDB.class, models);
    }

    @Override
    public ControlDB getControl(String uri, String... models) {
        return getEntity(uri, ControlDB.class, models);
    }

    @Override
    public Map<String, ControlDB> getControls(String... models) {
        return getEntities(ControlDB.class, models);
    }

    @Override
    public Map<String, DistinctNodeGroupDB> getDistinctNodeGroups(String... models) {
        return getEntities(DistinctNodeGroupDB.class, models);
    }

    @Override
    public DistinctNodeGroupDB getDistinctNodeGroup(String uri, String... models) {
        return getEntity(uri, DistinctNodeGroupDB.class, models);
    }

    @Override
    public ModelDB getModelInfo(String model) {
        // We need to look for the correct URI, which must be extracted before calling getEntity
        String uri;
        if (model.equals("domain")) {
            // For a domain model, this shoud be PREFIX + "domain", which is not the same as the graph URI
            uri = PREFIX + "domain";
        } else if (model.equals("system") || model.equals("system-inf")) {
            // For a system model it should be the graph URI
            uri = stack.getGraph(model);
        } else {
            // Any other value of 'model' makes no sense, so we'll add a warning message before trying the graph URI
            logger.warn("Should not be trying to get model info for something other than a system or domain model");
            uri = stack.getGraph(model);
        }

        String cacheTypeName = "owl#Ontology";
        return getEntity(uri, cacheTypeName, ModelDB.class, model);

    }

    @Override
    public Map<String,ModelFeatureDB> getModelFeatures(String... models){
        return getEntities(ModelFeatureDB.class, models);
    }

    /* Persist an 'entity' to the given 'graph' as a resource with a specified
     * short form URI 'uri' (allows us to override entity.getUri()).
     */
    private <T extends EntityDB> boolean persistEntity(T entity, String uri, String graph) {
        // MUST BE IN TRANSACTION

        if(entity == null){
            // Nothing to persist, but emit a warning
            logger.warn("Attempting to persist a null entity at uri '{}' to graph '{}'", uri, graph);
            return false;
        }
        if(entity.getType() == null) {
            // Something to persist, but emit a warning
            logger.warn("Attempting to persist entity {} with no rdf:type to graph '{}'", entity.getUri(), uri, graph);
        }

        // Get the long URI
        String entityUri = getLongName(uri);

        // Convert to Json and delete the URI property
        JsonObject jsonObject = gson.toJsonTree(entity).getAsJsonObject();
        jsonObject.remove("uri");

        // Find graphs containing the entity
        String[] checkedOutGraphsArray = checkedOutEntityGraphs.getOrDefault(System.identityHashCode(entity), new String[0]);
        List<String> secondaryCheckedOutGraphs = new ArrayList<>();
        for (String checkedOutGraph : checkedOutGraphsArray) {
            if (!checkedOutGraph.equals(graph)) {
                secondaryCheckedOutGraphs.add(checkedOutGraph);
            }
        }

        // Remove properties from the to-persist-entity if they are present in secondary graphs with an equal value.
        // This keeps triples in their original graphs if unchanged.
        for (String checkoutOutGraph : secondaryCheckedOutGraphs) {
            String graphUri = stack.getGraph(checkoutOutGraph);
            if (graphUri == null) {
                continue;
            }
            List<Statement> removeStatements = new ArrayList<>();
            Model model = dataset.getNamedModel(graphUri);
            Resource resource = model.getResource(entityUri);
            if (resource != null) {
                StmtIterator propertyIterator = resource.listProperties();
                while (propertyIterator.hasNext()) {
                    Statement statement = propertyIterator.next();
                    String predicateShort = getShortName(statement.getPredicate(), false);
                    JsonElement element = jsonObject.get(predicateShort);

                    if (element != null) {
                        String previousValue;
                        if (statement.getObject().isLiteral()) {
                            previousValue = statement.getObject().asLiteral().getString();
                        } else {
                            previousValue = statement.getObject().toString();
                        }
                        String updateValue = getLongName(element.getAsString());

                        if (previousValue.equals(updateValue)) {
                            jsonObject.remove(predicateShort);
                        } else {
                            removeStatements.add(statement);
                        }
                    }
                }
            }

            for (Statement statement : removeStatements) {
                model.remove(statement);
            }
        }
 
        // Then save the new values to the entity's main graph
        String graphUri = stack.getGraph(graph);
        if (graphUri == null) {
            logger.warn("Attempting to store new properties from entity {} in null graph {}", entity.getUri(), graph);
            return false;
        }
        Model model = dataset.getNamedModel(graphUri);
        Resource resource = model.createResource(entityUri);
        parseJsonToResource(jsonObject, resource);

        return true;
    }

    /* Persist an 'entity' to the given 'graph' using its assigned short form URI.
     * In almost all cases this is what we need to do.
     */
    private <T extends EntityDB> boolean persistEntity(T entity, String graph) {
        /* In almost all cases, an entity can be persisted under its own URI.
         */
        if(entity == null) {
            return false;
        }
        if (entity.getType() == null) {
            entity.setType("core#" + entity.getClass().getSimpleName().replace("DB", ""));
        }
        try{
            boolean success = true;
            if(success) success = persistEntity(entity, entity.getUri(), graph);
            return success;
		} catch (Exception e) {
            String message = String.format("Failed to persist entity %s of class %s", entity.getUri(), entity.getClass().getSimpleName());
            logger.error(message);
            throw new RuntimeException(message, e);
        }

    }

    /* Method to write a link to the triple store as a single property of the
     * source asset referring to the target asset.
     */
    private <T extends EntityDB> boolean persistLink(T entity, String model) {
        // MUST BE IN TRANSACTION

        String graphUri = stack.getGraph(model);
        if (graphUri == null) {
            return false;
        }
        if(entity == null){
            // Nothing to persist, but emit a warning
            logger.warn("Attempting to persist a link triple for a null entity in graph {}", model);
            return false;
        }

        // Store the link as a single triple between asset resources
        CardinalityConstraintDB link = (CardinalityConstraintDB)entity;
        Model datasetModel = dataset.getNamedModel(graphUri);
        Resource resource = datasetModel.getResource( getLongName(link.getLinksFrom()));
        Property property = ResourceFactory.createProperty(getLongName(link.getLinkType()));
        RDFNode object = ResourceFactory.createResource(getLongName(link.getLinksTo()));

        // If the triple already exists in the asserted graph, this could create a duplicate
        // in the inferred graph, which seems to cause problems. So check that before saving.
        boolean linkExists = false;
        for (String g : systemGraphs) {
            String gUri = stack.getGraph(g);
            if(gUri != null){
                Model d = dataset.getNamedModel(gUri);
                if(d.contains(resource, property, object)) {
                    linkExists = true;
                    break;
                }
            }
        }

        // Iff the link does not exist, save it
        if(!linkExists) {
            resource.addProperty(property, object);
        }

        return true;
    }

    /* Stores 'entity' in the provided 'graph' (short-name of model graph, e.g. 'system-inf'). All existing properties
     * will be overridden. If the entity object was created by this querier, then a check will be made for which graphs
     * it originates from. Properties present in originating graphs not equal to 'graph' will be ignored when saving iff
     * they are unchanged.
     * 
     * Note that the argument cacheTypeKey is used if the cache is enabled, but not otherwise. The type used when the
     * entity is written to the triple store is 'core#' followed by the entity class name without the final 'DB'.
     */
    private <T extends EntityDB> boolean storeEntity(T entity, String uri, String cacheTypeKey, String graph) {
        if (cacheEnabled) {
            cache.storeEntity(entity, cacheTypeKey, graph);
            return true;
        }

        boolean persisted = true;

        dataset.begin(ReadWrite.WRITE);
        if(persisted) persisted = persistEntity(entity, uri, graph);

        // If that was successful, commit the changes
        if(persisted) {
            dataset.commit();
        } else {
            logger.warn("Aborting transaction");
            dataset.abort();
        }

        dataset.end();

        return persisted;
    }

    private <T extends EntityDB> boolean storeEntity(T entity, String graph) {
        /* Almost all the time, the entity can be stored under its own URI,
         * and cached under its own cache type key.
         */
        String cacheTypeName = getCacheTypeName(entity); 
        String uri = entity.getUri();
        return storeEntity(entity, uri, cacheTypeName, graph);
    }

    /* Synchronise the cache, so afterwards the triple store matches the cache contents.
     */
    @Override
    public void sync(String... models) {
        final long startTime = System.currentTimeMillis();
        if(!cacheEnabled){
            // No need to synchronise if the cache isn't being used
            return;
        }
        logger.info("Synchronising model with triple store");

        // Get entities to be deleted and try to delete them from the triple store
        Map<String, Map<String, EntityDB>> deleteEntitiesByType = cache.getDeleteCache();
        try{
            // Start a transaction
            dataset.begin(ReadWrite.WRITE);

            // Delete the entities
            for(String typeKey : deleteEntitiesByType.keySet()){
                boolean deletingLinks = typeKey.equals(getCacheTypeName(CardinalityConstraintDB.class));
                Map<String, EntityDB> entities = deleteEntitiesByType.get(typeKey);
                if(entities != null && entities.size() > 0){
                    logger.info("Deleting {} entities cached as {}", entities.size(), typeKey);
                    if(deletingLinks){
                        // Delete the extra triples representing asset relationships
                        deleteLinks(entities);
                    }
                    deleteEntities(entities);
                }
            }

            // Commit the changes
            dataset.commit();
            
            // If successful, clear the map of entities to be deleted
            for(String typeKey : deleteEntitiesByType.keySet()){
                Map<String, EntityDB> entities = deleteEntitiesByType.get(typeKey);
                entities.clear();
            }
            deleteEntitiesByType.clear();

        }
        catch (Exception e) {
            // Abort the changes and signal that there has been an error
            dataset.abort();
            String message = "Error occurred when synchronising deletions to triple store";
            logger.error(message);
            throw new RuntimeException(message, e);
        }
        finally {
            // Close the transaction
            dataset.end();
        }

        logger.info("Saving new/modified entities");
        // Get the entities that need to be updated and try to save the changes
        for(String graph : models){
            // Get entities to be stored/updated
            Map<String, Map<String, EntityDB>> storeEntitiesByType = cache.getStoreCache(graph);

            try{
                // Start a transaction per graph
                dataset.begin(ReadWrite.WRITE);

                // Important to store assets first
                String assetTypeKey = getCacheTypeName(AssetDB.class);
                Map<String, EntityDB> entities = storeEntitiesByType.get(assetTypeKey);
                if(entities != null){
                    if(entities != null && entities.size() > 0){
                        logger.info("Saving {} new/modified entities cached as {} to graph {}", entities.size(), assetTypeKey, graph);
                    }
                    for(EntityDB entity : entities.values()){
                        persistEntity(entity, graph);
                    }
                }

                // Store/update the entities
                for(String typeKey : storeEntitiesByType.keySet()){
                    if(typeKey.equals(getCacheTypeName(AssetDB.class))) {
                        // Skip the assets, because they were already saved
                        continue;
                    }
                    boolean savingLinks = typeKey.equals(getCacheTypeName(CardinalityConstraintDB.class));
                    entities = storeEntitiesByType.get(typeKey);
                    if(entities != null){
                        if(entities != null && entities.size() > 0){
                            logger.info("Saving {} new/modified entities cached as {} to graph {}", entities.size(), typeKey, graph);
                        }
                        for(EntityDB entity : entities.values()){
                            if(savingLinks){
                                // Save the extra triples representing asset relationships
                                persistLink(entity, graph);
                            }
                            // Save the entity
                            persistEntity(entity, graph);
                        }
                    }
                }

                // Commit the changes
                dataset.commit();

                // If successful, clear the map of entities to be saved
                for(String typeKey : storeEntitiesByType.keySet()){
                    entities = storeEntitiesByType.get(typeKey);
                    entities.clear();
                }
                storeEntitiesByType.clear();

            } catch (Exception e) {
                // Abort the changes and signal that there has been an error
                dataset.abort();
                String message = String.format("Error occurred when synchronising updates to triple store graph %s", graph);
                logger.error(message);
                throw new RuntimeException(message, e);
            }
            finally {
                // Close the transaction
                dataset.end();
            }

        }

        // No need to clear the cache content, as it now matches the triple store

        final long endTime = System.currentTimeMillis();
        logger.info("JenaQuerierDB.sync(): execution time {} ms", endTime - startTime);
        
    }

    @Override
    public boolean store(AssetDB asset, String model) {
        /* AssetDB and its subclass InferredAssetDB should be stored with the same
         * cache type "core#AssetDB". But this is handled by getCacheTypeName().
         * 
         * When the asset is persisted it will need special treatment, but a store
         * to the cache is OK.
         */        
        return storeEntity(asset, model);
    }

    @Override
    public boolean store(NodeDB entity, String model) {
        /* Uses the standard cache type key, no special treatment needed.
         */
        return storeEntity(entity, model);
    }

    @Override
    public boolean store(MatchingPatternDB entity, String model) {
        // Get the matching pattern but don't return immediately unless the result is null
        if(entity == null){
            logger.warn("Attempted to store a null matching pattern to graph {}", model);
            return false;
        }

        boolean persisted = storeEntity(entity, model);
        if(!persisted){
            logger.warn("Failed to store matching pattern entity {} to graph {}", entity.getUri(), model);
        }

        // If it is a system model pattern, we should create an entry for it in the node map
        boolean systemMP = model.equals("system-inf");
        if(systemMP){
            // Get the map of nodes for this matching pattern
            Map<String, List<String>> nodesByRoleThisMP = nodesByRoleByMP.computeIfAbsent(entity.getUri(), K -> new HashMap<>());
            
            // Matching patterns can't change so the map only needs filling if it is empty
            if(nodesByRoleThisMP.isEmpty()){
                for(String nodeURI : entity.getUniqueNodes()){
                    NodeDB node = getNode(nodeURI, model);
                    boolean ok = addMatchingPatternNodeToMap(node, nodesByRoleThisMP);
                    if(!ok) {
                        logger.warn("Tried to add a null unique node to the map for pattern {}", entity.getUri());
                    }
                }
                for(String nodeURI : entity.getNecessaryNodes()){
                    NodeDB node = getNode(nodeURI, model);
                    boolean ok = addMatchingPatternNodeToMap(node, nodesByRoleThisMP);
                    if(!ok) {
                        logger.warn("Tried to add a null necessary node to the map for pattern {}", entity.getUri());
                    }
                }
                for(String nodeURI : entity.getSufficientNodes()){
                    NodeDB node = getNode(nodeURI, model);
                    boolean ok = addMatchingPatternNodeToMap(node, nodesByRoleThisMP);
                    if(!ok) {
                        logger.warn("Tried to add a null sufficient node to the map for pattern {}", entity.getUri());
                    }
                }
                for(String nodeURI : entity.getOptionalNodes()){
                    NodeDB node = getNode(nodeURI, model);
                    boolean ok = addMatchingPatternNodeToMap(node, nodesByRoleThisMP);
                    if(!ok) {
                        logger.warn("Tried to add a null optional node to the map for pattern {}", entity.getUri());
                    }
                }
            }
        }
         
        return persisted;

    }

    @Override
    public boolean store(ThreatDB entity, String model) {
        /* Uses the standard cache type key, no special treatment needed.
         */
        return storeEntity(entity, model);
    }

    @Override
    public boolean store(MisbehaviourInhibitionSetDB entity, String model) {
        /* Uses the standard cache type key, no special treatment needed.
         */
        return storeEntity(entity, model);
    }

    @Override
    public boolean store(MisbehaviourSetDB entity, String model) {
        /* Uses the standard cache type key, no special treatment needed.
         */
        return storeEntity(entity, model);
    }

    @Override
    public boolean store(TrustworthinessAttributeSetDB entity, String model) {
        /* Uses the standard cache type key, no special treatment needed.
         */
        return storeEntity(entity, model);
    }

    @Override
    public boolean store(TrustworthinessImpactSetDB entity, String model) {
        /* Uses the standard cache type key, no special treatment needed.
         */
        return storeEntity(entity, model);
    }

    @Override
    public boolean store(ControlStrategyDB entity, String model) {
        /* Uses the standard cache type key, no special treatment needed.
         */
        return storeEntity(entity, model);
    }

    @Override
    public boolean store(ControlSetDB entity, String model) {
        /* Uses the standard cache type key, no special treatment needed.
         */
        return storeEntity(entity, model);
    }

    @Override
    public boolean store(ModelDB entity, String model) {
        /* Special case: need to
         * - determine the correct resource URI on which the model info should be saved
         * - set the persisted entity type, since it isn't the usual core#ClassName[-DB]
         */
        String uri;
        if(model.equals("domain")) {
            // For a domain model, this should be PREFIX + "domain", which is not the same as the graph URI
            uri = PREFIX + "domain";
        } else if(model.equals("system") || model.equals("system-inf")){
            // For a system model it should be the graph URI
            uri = stack.getGraph(model);
        } else {
            // Any other value of 'model' makes no sense, so we'll add a debug message before trying the graph URI
            logger.debug("Should not be trying to store model info for something other than a system or domain model");
            uri = stack.getGraph(model);
        }

        // And the type of a ModelDB should be "owl#Ontology",
        entity.setType("owl#Ontology");

        String typeName = getCacheTypeName(entity.getClass());
        return storeEntity(entity, uri, typeName, model);

    }

    @Override
    public boolean store(CardinalityConstraintDB link, String model) {
        /* Special case, because each CardinalityConstraintDB encodes a relationship between assets.
         *
         * This is represented differently in the triple store and the object model:
         * 
         * - in the triple store, the entity is stored along with an extra triple whose domain is
         *   the source asset URI, the range is the target asset URI, and whose property is the
         *   relationship type.
         * 
         * - in the object model, the CardinalityConstraintDB does not have this extra triple,
         *   but it is stored in two lists: 
         *   - the 'from' links indexed by source asset URI
         *   - the 'to' links indexed by target asset URI
         * 
         * These lists are needed virtually the whole time assets are referred to, so they aren't
         * stored in the cache but in member variables, created when CardinalityConstraintDB are
         * loaded from the triple store. When a new CardinalityConstraintDB has been created and
         * is stored or deleted, the lists must also be updated.
         * 
         * If the cache is not enabled, we must also persist the extra triple before persisting
         * the rest of the entity.
         */

        // Create the connections between cached assets and cardinality constraints
        List<String> linksFromSource = linksFromAsset.computeIfAbsent(link.getLinksFrom(), K -> new ArrayList<>());
        List<String> linksToTarget = linksToAsset.computeIfAbsent(link.getLinksTo(), K -> new ArrayList<>());
        if(!linksFromSource.contains(link.getUri())) linksFromSource.add(link.getUri());
        if(!linksToTarget.contains(link.getUri())) linksToTarget.add(link.getUri());
        
        String cacheTypeKey = getCacheTypeName(link);
        if (cacheEnabled) {
            cache.storeEntity(link, cacheTypeKey, model);
            return true;
        } else {
            boolean persisted = true;
            // Start a read-write transaction
            dataset.begin(ReadWrite.WRITE);

            // Try to write the separate link triple
            if(persisted) persisted = persistLink(link, model);

            // If that worked, try to write the rest of the entity
            if(persisted) persisted = persistEntity(link, model);

            // If that was successful, commit the changes
            if(persisted) {
                dataset.commit();
            } else {
                logger.warn("Aborting transaction");
                dataset.abort();
            }

            // Close the transaction and return
            dataset.end();

            return persisted;

        }

    }

    /* Deletion methods are only needed for assets and relationships, since all other
     * entities are inferred from the assets and relationships.
     * 
     * What this means is that if you delete an asset or relationship, you should then
     * revalidate. That not only takes care of  other entities related to assets or
     * their relationships (e.g. CS, MS, TWAS, CSGs and Threats). It also ensures that
     * any construction sequences involving the deleted asset/relationship are redone.
     * 
     * However, it is OK to delete construction state assets and relationships at the
     * end of construction. These are assets/relationships that play no role in threats
     * and cannot be asserted. That means they can't exist before construction, and make
     * no difference after construction, so deleting them doesn't invalidate the model.
     * 
     * To make this work, the public deletion methods have a boolean flag which should
     * be set if the model should be invalidated. This shoud be set to 'true' unless
     * the method is being used by the validator to delete construction state at the
     * end of the construction sequence.
     */

    /* Delete an 'entity' from all 'model' graphs. Specified via a URI, so
     * we can override the entity's URI if necessary.
     */
    private boolean deleteEntity(String uri, String[] graphs) {
        // MUST BE IN TRANSACTION

        // Check the entity URI is not a null pointer
        if(uri == null) {
            logger.warn("Attempted to delete an entity with no URI");
            return false;
        }

        boolean deleted = false;

        for(String model : graphs) {
            String graphUri = stack.getGraph(model);
            if (graphUri == null) {
                continue;
            }
        
            String entityUri = getLongName(uri);
    
            // Get the model for the specified graph
            Model datasetModel = dataset.getNamedModel(graphUri);
            
            // Get the resource corresponding to the deleted entity
            Resource resource = datasetModel.getResource(entityUri);
    
            // Remove triples in which the domain or range refer to this resource
            datasetModel.removeAll(null, null, (RDFNode)resource);
            datasetModel.removeAll(resource, null, null);
    
            deleted = true;
        }

        return deleted;
    }

    /* Wrapper allowing deletion of an entity passed as an object
     */
    private <T extends EntityDB> boolean deleteEntity(T entity){
        // MUST BE IN TRANSACTION

        // Check the entity URI is not a null pointer
        if(entity == null) {
            logger.warn("Attempted to delete a null entity");
            return false;
        }

        // Set the list of graphs - includes the UI for asserted assets, not for others
        String[] graphs;
        if(entity.getClass().getSimpleName().equals("AssetDB")) {
            graphs = allGraphs;
        } else {
            graphs = systemGraphs;
        }

        // Try to delete stuff
        boolean deleted = true;
        if(deleted) deleted = deleteEntity(entity.getUri(), graphs);

        return deleted;

    }

    /* Wrapper allowing deletion of a set of entities passed as an map
     */
    private <T extends EntityDB> boolean deleteEntities(Map<String, T> entities) {
        // MUST BE IN A TRANSACTION

        // Check the set of entities is not null
        if(entities == null) {
            logger.warn("Attempted to delete a null set of entities");
            return false;
        }

        // Try to delete the entities
        boolean deleted = true;

        /*
         * It seems like the deleteEntity method can be embedded in a loop
         * without adding too much cost
         */
        for(EntityDB entity : entities.values()) {
            if(deleted) {
                deleted = deleteEntity(entity);
            }
        }

        return deleted;

    }

    /* Method to remove from all 'model' graphs the single triple representing an asset
     * relationship as a property of the source asset.
     * 
     * A separate method is needed for this because that triple is not part of the link
     * entity.
     */
    private <T extends EntityDB> boolean deleteLink(T entity) {
        // MUST BE IN TRANSACTION
        // Check the entity URI is not a null pointer
        if(entity == null) {
            logger.warn("Attempted to delete a link for a null entity");
            return false;
        }

        boolean deleted = false;
        
        // Get the URI of the graphs - deletion should be done across all system graphs (but not the UI)
        for(String model : systemGraphs) {
            String graphUri = stack.getGraph(model);
            if (graphUri == null) {
                continue;
            }
    
            CardinalityConstraintDB link = (CardinalityConstraintDB)entity;
            if(link.getLinksFrom() == null || link.getLinkType() == null || link.getLinksTo() == null) {
                // Don't bother deleting the separate link triple because it doesn't exist 
                continue;
            }

            Model datasetModel = dataset.getNamedModel(graphUri);
            Resource resource = datasetModel.getResource( getLongName(link.getLinksFrom()));
            Property property = ResourceFactory.createProperty(getLongName(link.getLinkType()));
            RDFNode object = ResourceFactory.createResource(getLongName(link.getLinksTo()));
            if(datasetModel.contains(resource, property, object)) {
                datasetModel.remove(resource, property, object);
            }

            deleted = true;

        }

        // The link entity can then be deleted

        return deleted;
    }

    /* Method to remove from all 'model' graphs the single triples representing a set of
     * asset relationships as properties of the source assets.
     * 
     * A separate method is needed for this because that triple is not part of the link
     * entity.
     */
    private <T extends EntityDB> boolean deleteLinks(Map<String, T> entities) {
        // MUST BE IN TRANSACTION
        // Check the entity URI is not a null pointer
        if(entities == null) {
            logger.warn("Attempted to delete link triples for a null set of entities");
            return false;
        }

        boolean deleted = false;

        // Get the URI of the graphs - deletion should be done across all system graphs (but not the UI)
        for(String model : systemGraphs) {
            String graphUri = stack.getGraph(model);
            if (graphUri == null) {
                continue;
            }

            for(EntityDB entity : entities.values()){
                CardinalityConstraintDB link = (CardinalityConstraintDB)entity;
                if(link.getLinksFrom() == null || link.getLinkType() == null || link.getLinksTo() == null) {
                    // Don't bother deleting the separate link triple because it doesn't exist 
                    continue;
                }

                Model datasetModel = dataset.getNamedModel(graphUri);
                Resource resource = datasetModel.getResource(getLongName(link.getLinksFrom()));
                Property property = ResourceFactory.createProperty(getLongName(link.getLinkType()));
                RDFNode object = ResourceFactory.createResource(getLongName(link.getLinksTo()));
                if(datasetModel.contains(resource, property, object)) {
                    datasetModel.remove(resource, property, object);
                }    
            }

            deleted = true;

        }

        // The link entity can then be deleted

        return deleted;
    }

    /* A method to delete an asset is obviously useful.
     * 
     * Note that this must work across all system model graphs, as we must delete the
     * whole asset even if split across graphs.
     * 
     * The invalidateModel flag specifies whether to invalidate the model. This would
     * not be done if cleaning up construction state because assets used for this must
     * be inferred and non-assertible. It would be done in most other situations.
     */
    @Override
    public boolean delete(AssetDB asset, boolean invalidateModel) {
        /* Special case, for the following reasons.
         *
         * Both AssetDB and InferredAssetDB entities are stored in the same cache under
         * the parent class name 'AssetDB'.
         * 
         * If an asset is deleted, we should also delete its relationships.
         */

        // Find links to be deleted along with the asset
        Map<String, CardinalityConstraintDB> deleteLinks = new HashMap<>();
        List<String> linksFromThis = linksFromAsset.getOrDefault(asset.getUri(), new ArrayList<>());
        List<String> linksToThis = linksToAsset.getOrDefault(asset.getUri(), new ArrayList<>());
        for(String linkURI : linksFromThis){
            CardinalityConstraintDB link = getCardinalityConstraint(linkURI, systemGraphs);
            deleteLinks.put(link.getUri(), link);
        }
        for(String linkURI : linksToThis){
            CardinalityConstraintDB link = getCardinalityConstraint(linkURI, systemGraphs);
            deleteLinks.put(link.getUri(), link);
        }

        // Start trying to delete stuff
        boolean deleted = true;

        if(cacheEnabled) {
            // Delete the cardinality constraints first, which also removes them from the associated assets
            if(deleted) deleted = deleteCardinalityConstraints(deleteLinks, false);

            // Then delete the asset from the cache, also marking it for deletion in the triple store
            String cacheTypeKey = getCacheTypeName(asset);
            if(deleted) cache.deleteEntity(asset, cacheTypeKey);

            if(deleted && invalidateModel) {
                // TODO : add a way to invalidate the system model
            }

        } else {
            dataset.begin(ReadWrite.WRITE);
            
            if(deleted) deleted = deleteLinks(deleteLinks);
            if(deleted) deleted = deleteEntities(deleteLinks);
            if(deleted) deleted = deleteEntity(asset);

            if(deleted) {
                dataset.commit();
            } else {
                logger.warn("Aborting transaction");
                dataset.abort();
            }
            dataset.end();

            if(deleted && invalidateModel) {
                // TODO : add a way to invalidate the system model
                // Should be in a separate transaction
            }
        }

        // Remove asset from the lists of links from and to the assets
        if(deleted){
            linksFromAsset.remove(asset.getUri());
            linksToAsset.remove(asset.getUri());
        }

        return deleted;

    }

    /* A method to delete a list of assets is also useful, and needed by the validator
     * to clean up unwanted construction state asset classes.
     * 
     * Note that this must work across all system model graphs, as we must delete the
     * whole of each asset even if split across graphs.
     * 
     * The invalidateModel flag specifies whether to invalidate the model.
     */
    @Override
    public boolean deleteAssets(Map<String, AssetDB> assets, boolean invalidateModel){
        /* Special case, for the following reasons.
         *
         * Both AssetDB and InferredAssetDB entities are stored in the same cache under
         * the parent class name 'AssetDB'.
         * 
         * If an asset is deleted, we should also delete its relationships.
         */

        // Find the links to or from the assets that will need to be deleted with the assets
        Map<String, CardinalityConstraintDB> deleteLinks = new HashMap<>();
        for(AssetDB asset : assets.values()) {
            // Find links to be deleted along with the asset
            List<String> linksFromThis = linksFromAsset.getOrDefault(asset.getUri(), new ArrayList<>());
            List<String> linksToThis = linksToAsset.getOrDefault(asset.getUri(), new ArrayList<>());
            for(String linkURI : linksFromThis){
                CardinalityConstraintDB link = getCardinalityConstraint(linkURI, systemGraphs);
                deleteLinks.put(link.getUri(), link);
            }
            for(String linkURI : linksToThis){
                CardinalityConstraintDB link = getCardinalityConstraint(linkURI, systemGraphs);
                deleteLinks.put(link.getUri(), link);
            }    
        }

        // Start trying to delete stuff
        boolean deleted = true;

        if(cacheEnabled) {
            // Delete the cardinality constraints first, which also removes them from the associated assets
            if(deleted) deleted = deleteCardinalityConstraints(deleteLinks, false);

            // Then delete the asset from the cache, also marking it for deletion in the triple store
            String cacheTypeKey = getCacheTypeName(AssetDB.class);
            if(deleted) cache.deleteEntities(assets, cacheTypeKey);

            if(deleted && invalidateModel) {
                // TODO : add a way to invalidate the system model
            }

        } else {
            dataset.begin(ReadWrite.WRITE);
            
            if(deleted) deleted = deleteLinks(deleteLinks);
            if(deleted) deleted = deleteEntities(deleteLinks);

            if(deleted) deleted = deleteEntities(assets);

            if(deleted) {
                dataset.commit();
            } else {
                logger.warn("Aborting transaction");
                dataset.abort();
            }
            dataset.end();

            if(deleted && invalidateModel) {
                // TODO : add a way to invalidate the system model
                // Should be in a separate transaction
            }
        }

        // Remove asset from the lists of links from and to the assets
        if(deleted) for(String assetURI : assets.keySet()) {
            linksFromAsset.remove(assetURI);
            linksToAsset.remove(assetURI);    
        }

        return deleted;

    }

    /* A method to delete an asset relationship is obviously useful.
     * 
     * Note that this must work across all system model graphs, as we must delete the
     * whole link even if split across graphs.
     * 
     * The invalidateModel flag specifies whether to invalidate the model. This would
     * not be done if cleaning up construction state because links used for this must
     * be inferred and non-assertible. It would be done in most other situations.
     */
    @Override
    public boolean delete(CardinalityConstraintDB link, boolean invalidateModel){
        /* Special case, because each CardinalityConstraintDB encodes a relationship between
         * assets, and should be removed from lists indexed by source and destination asset.
         */

        // Initialise return value, assuming success until it goes wrong
        boolean deleted = true;

        if (cacheEnabled) {
            // Delete the entity from the cache
            String cacheTypeKey = getCacheTypeName(link);
            cache.deleteEntity(link, cacheTypeKey);

            if(deleted && invalidateModel) {
                // TODO : add a way to invalidate the system model
            }
        } else {
            // Delete the entity from the triple store inside a transaction
            dataset.begin(ReadWrite.WRITE);
            if(deleted) deleted = deleteLink(link);
            if(deleted) deleted = deleteEntity(link);

            if(deleted) {
                dataset.commit();
            } else {
                logger.warn("Aborting transaction");
                dataset.abort();
            }
            dataset.end();

            if(deleted && invalidateModel) {
                // TODO : add a way to invalidate the system model
            }

        }

        if(deleted){
            // Remove the link from the lists associated with its source and target assets
            List<String> linksFromSource = linksFromAsset.getOrDefault(link.getLinksFrom(), new ArrayList<>());
            List<String> linksToTarget = linksToAsset.getOrDefault(link.getLinksTo(), new ArrayList<>());
            linksFromSource.remove(link.getUri());
            linksToTarget.remove(link.getUri());
        }

        return deleted;

    }

    /* A method to delete a list of asset relationships is also useful, and needed by
     * the validator to clean up unwanted construction state asset classes.
     * 
     * Note that this must work across all system model graphs, as we must delete the
     * whole of each link even if split across graphs.
     * 
     * The invalidateModel flag specifies whether to invalidate the model.
     */
    @Override
    public boolean deleteCardinalityConstraints(Map<String, CardinalityConstraintDB> links, boolean invalidateModel){
        /* Special case, because each CardinalityConstraintDB encodes a relationship between
         * assets, and should be removed from those assets.
         */

        // Initialise return value, assuming success until it goes wrong
        boolean deleted = true;

        if (cacheEnabled) {
            // Delete the entities from the cache
            String cacheTypeKey = getCacheTypeName("CardinalityConstraintDB");
            cache.deleteEntities(links, cacheTypeKey);

            if(deleted && invalidateModel) {
                // TODO : add a way to invalidate the system model
            }

        } else {

            // Delete the entities from the triple store inside a transaction
            dataset.begin(ReadWrite.WRITE);

            if(deleted) deleted = deleteLinks(links);
            if(deleted) deleted = deleteEntities(links);

            if(deleted) {
                dataset.commit();
            } else {
                logger.warn("Aborting transaction");
                dataset.abort();
            }
            dataset.end();

            if(deleted && invalidateModel) {
                // TODO : add a way to invalidate the system model
            }

        }

        // Remove the link from source and target assets, and create the entities map
        if(deleted) for(CardinalityConstraintDB link : links.values()) {
            // Remove the link from the lists associated with its source and target assets
            List<String> linksFromSource = linksFromAsset.getOrDefault(link.getLinksFrom(), new ArrayList<>());
            List<String> linksToTarget = linksToAsset.getOrDefault(link.getLinksTo(), new ArrayList<>());
            linksFromSource.remove(link.getUri());
            linksToTarget.remove(link.getUri());    
        }

        return deleted;

    }

    /* We still do need some methods to update the asserted graph in specific ways.
     * These are used by the validator to fix inconsistencies in the asserted graph,
     * providing a sort-of 'automated repair' facility for old/broken system models.
     */

    /** Method to insert asset populations where they don't yet exist.
     */
    @Override
    public void repairAssertedAssetPopulations(){
        // First get the population scale indexed by URI, and create one indexed by level
        Map<String, LevelDB> poLevels = this.getPopulationLevels();
        List<LevelDB> populationLevels = new ArrayList<>();
        populationLevels.addAll(poLevels.values());
        populationLevels.sort(Comparator.comparingInt(LevelDB::getLevelValue));

        // Then get a list of assets defined in the asserted graph
        Map<String, AssetDB> assets = this.getAssets("system");

        // Repair assets that have no populations
        for(AssetDB asset : assets.values()){
            String popURI = asset.getPopulation();
            if((popURI == null) || (poLevels.get(popURI) == null)) {
                int maxCardinality = asset.getMaxCardinality();
                if(maxCardinality == 1) {
                    // Means cardinality is set to 'singleton'
                    this.repairAssetPopulation(populationLevels.get(0), asset);
                } else if(maxCardinality > 1) {
                    // Means cardinality is set to 'non-singleton' or 'a few'
                    this.repairAssetPopulation(populationLevels.get(1), asset);
                } else if(maxCardinality < 0) {
                    // Means cardinality is set to 'several' or 'more' or 'many'
                    this.repairAssetPopulation(populationLevels.get(2), asset);
                } else {
                    // Should not be found, so log an error and treat as a singleton
                    logger.warn("Asset {} has maximum cardinality set to zero, treating as singleton", asset.getUri());
                    this.repairAssetPopulation(populationLevels.get(0), asset);
                }
            }
        }		
    }

    /** Method to store the population level of an asset that exists in the asserted graph.
     * 
     *  @param pop = the LevelDB object representing the new population level
     *  @param asset = the AssetDB object to which this level applies
     * 
     * We are assuming that the AssetDB object will be updated by the validator, but will
     * be saved in the inferred graph, so the asserted graph must be updated separately.
     */
    private boolean repairAssetPopulation(LevelDB pop, AssetDB asset){
        if(asset == null) {
            logger.warn("Tried to set the population of a null asset in the asserted graph");
            return false;
        }
        if(pop == null){
            logger.warn("Tried to set a null population level for asset {} in the asserted graph", asset.getUri());
            return false;
        }

        // Should only be applied to properties in the asserted graph
        String graphUri = stack.getGraph("system");
        Model datasetModel = dataset.getNamedModel(graphUri);

        // Get the resource representing the asset
        Resource resource = datasetModel.getResource(getLongName(asset.getUri()));
        if(resource == null) {
            logger.warn("Tried to repair asserted asset {}, but it could not be found in the asserted graph", asset.getUri());
            return false;
        }

        // Create the property that should be updated, and the resource representing the new population level
        Property populationProperty = ResourceFactory.createProperty(getLongName("core#population"));
        RDFNode object = ResourceFactory.createResource(getLongName(pop.getUri()));    

        // Create the properties that should be deleted
        Property minCardinalityProp = ResourceFactory.createProperty(getLongName("core#minCardinality"));
        Property maxCardinalityProp = ResourceFactory.createProperty(getLongName("core#maxCardinality"));

        // Perform the updates in a transaction
        boolean success = true;
        try {
            dataset.begin(ReadWrite.WRITE);
            resource.removeAll(minCardinalityProp);
            resource.removeAll(maxCardinalityProp);
            resource.removeAll(populationProperty);
            resource.addProperty(populationProperty, object);
            dataset.commit();
        } 
        catch (Exception e) {
            // Abort the changes and signal that there has been an error
            success = false;
            dataset.abort();
            String message = String.format("Error occurred while auto repairing cardinality for asserted asset '%s'", asset.getUri());
            logger.error(message, e);
            throw new RuntimeException(message, e);
        }
        finally {
            dataset.end();
        }

        // Now if the AssetDB object is cached, update the loaded copy
        if(cacheEnabled) {
            asset.setPopulation(pop.getUri());
        }

        return success;

    }

    /* Methods to fix errors caused by bugs or outdated functions in older versions of system-modeller,
     * which may still cause problems if old system models are imported.
     * 
     * Currently two aspects are repaired:
     * - source and target cardinality can no longer be asserted, as they should be inferred
     * - the URI of a cardinality constraint should be based on its type and asset IDs, but
     *   older versions of system-modeller were not consistent about this
     */
    @Override
    public void repairCardinalityConstraints(){
        // Changes should only be applied to properties in the asserted graph
        String graphUri = stack.getGraph("system");
        Model datasetModel = dataset.getNamedModel(graphUri);

        // Get the asserted cardinality constraints, so if caching is enabled we know they are cached
        Map<String, CardinalityConstraintDB> ccs = getCardinalityConstraints("system");
        Map<String, AssetDB> assets = getAssets("system");

        /* The source and target cardinality of cardinality constraint entities should be calculated and
         * stored in the inferred graph. Any asserted values will override these so should be deleted.
         */
        this.removeRelationshipCardinality(ccs, datasetModel);

        /* The URI for a cardinality constraint should be derived from the IDs of assets plus the
         * URI of the corresponding relationship type. Any with the wrong URI should be replaced by
         * one that uses the correct URI
         */
        this.fixCardinalityConstraintURI(ccs, assets, datasetModel);

    }

    private void removeRelationshipCardinality(Map<String, CardinalityConstraintDB> ccs, Model datasetModel) {
         // Create the properties that need to be removed
         Property sourceCardinalityProp = ResourceFactory.createProperty(getLongName("core#sourceCardinality"));
         Property targetCardinalityProp = ResourceFactory.createProperty(getLongName("core#targetCardinality"));
 
         // Delete these properties in a transaction
         try {
             dataset.begin(ReadWrite.WRITE);
             datasetModel.removeAll(null, sourceCardinalityProp, null);
             datasetModel.removeAll(null, targetCardinalityProp, null);
             dataset.commit();
         } 
         catch (Exception e) {
             // Abort the changes and signal that there has been an error
             dataset.abort();
             String message = String.format("Error occurred while auto repairing asserted cardinality constraints");
             logger.error(message, e);
             throw new RuntimeException(message, e);
         }
         finally {
             dataset.end();
         }
 
         if(cacheEnabled){
             // Remove these properties in the cached objects
             for(CardinalityConstraintDB cc : ccs.values()){
                 cc.setSourceCardinality(null);
                 cc.setTargetCardinality(null);
             }
         }
    }
    private void fixCardinalityConstraintURI(Map<String, CardinalityConstraintDB> ccs, Map<String, AssetDB> assets, Model datasetModel) {
        // If the cache is enabled, temporarily disable it
        boolean cacheDisabled = cacheEnabled;
        cacheEnabled = false;

        // Get the cache type key for this type of entity
        String cacheTypeKey = getCacheTypeName("CardinalityConstraintDB");

        // Check and repair the cardinality constraint (link) entities
        for(CardinalityConstraintDB oldcc : ccs.values()){
            // Get the relationship entity properties
            String oldURI = oldcc.getUri();
            String linkFromURI = oldcc.getLinksFrom();
            String linkToURI = oldcc.getLinksTo();
            String linkType = oldcc.getLinkType();

            // Get the asset entities at each end of the relationship
            AssetDB fromAsset = assets.get(linkFromURI);
            AssetDB toAsset = assets.get(linkToURI);

            /* There is a problem here if the model includes 'dangling' links. In principle, they can arise if either:
             * 
             * - an older, buggier version of SSM failed to remove an asserted link to or
             *   from an asset when the asset was deleted, leaving a null asset URI
             * - a user added a link to or from an inferred asset, and the asset hasn't
             *   yet been regenerated.
             * 
             * In the former case, we should just ignore the link entity.
             */
            if(linkFromURI == null || linkToURI == null || linkType == null) {
                // Bad link with a missing property, so remove it from the graph
                logger.warn("Link {} has a null source, target or type property - ignoring URI check", oldURI);
                continue;
            }

            /* In the latter case must retain the link entity but will need to create a fake asset (not stored)
             * entity from which to get a hashed ID reference for use in creating the correct link URI.
             */
            if(fromAsset == null) {
                fromAsset = new AssetDB();
                fromAsset.setUri(linkFromURI);
            }
            if(toAsset == null) {
                toAsset = new AssetDB();
                toAsset.setUri(linkToURI);
            }

            // Get the correct relationship entity URI
            String newURI = "system#" + fromAsset.generateID() + "-" + linkType.replace("domain#", "") + "-" + toAsset.generateID();

            if(!oldURI.equals(newURI)) {
                // The URI of this CC is incorrect
                logger.info("Replacing asserted relationship entity {} which should be {}", oldURI, newURI);

                // Create a new CC with the correct URI
                CardinalityConstraintDB newcc = new CardinalityConstraintDB();
                newcc.setUri(newURI);
                newcc.setLinksFrom(fromAsset.getUri());
                newcc.setLinksTo(toAsset.getUri());
                newcc.setLinkType(oldcc.getLinkType());

                // Remove the old CC and replace it by the new one
                delete(oldcc, false);
                store(newcc, "system");

                // If the cache was enabled, update the cache separately
                if(cacheDisabled){
                    cache.deleteEntity(oldcc, cacheTypeKey, true);
                    cache.storeEntity(newcc, cacheTypeKey, "system");
                }
            }

        }

        // If the cache was temporarily disabled, enable it again
        cacheEnabled = cacheDisabled;

    }

    /* Method to override the assumed TW level of a TWAS in a graph without creating the TWAS
     * in the same graph. Needed to adjust user/client asserted levels which appear as single
     * triples in the asserted graph, but without the TWAS entity (which is added later by the
     * validator in the inferred graph).
     */
    @Override
    public boolean updateAssertedLevel(LevelDB level, TrustworthinessAttributeSetDB twas, String model){
        String graphUri = stack.getGraph(model);
        if (graphUri == null) {
            return false;
        }
        Model datasetModel = dataset.getNamedModel(graphUri);

        // Encode the population level as a single property of the asset resource
        Resource resource = datasetModel.getResource(getLongName(twas.getUri()));
        Property property = ResourceFactory.createProperty(getLongName("core#hasAssertedLevel"));
        RDFNode object = ResourceFactory.createResource(getLongName(level.getUri()));

        // Now remove the old value and save the new value
        dataset.begin(ReadWrite.WRITE);
        resource.removeAll(property);
        resource.addProperty(property, object);
        dataset.commit();
        dataset.end();

        return true;
    }

    /* Method to override the coverage level of a CS in a graph without creating the CS in the
     * same graph. Needed to adjust user/client supplied coverage levels which appear as single
     * triples in the asserted graph, but without the CS entity (which is added later by the
     * validator in the inferred graph).
     */
    @Override
    public boolean updateCoverageLevel(LevelDB level, ControlSetDB cs, String model){
        String graphUri = stack.getGraph(model);
        if (graphUri == null) {
            return false;
        }
        Model datasetModel = dataset.getNamedModel(graphUri);

        // Encode the population level as a single property of the asset resource
        Resource resource = datasetModel.getResource(getLongName(cs.getUri()));
        Property property = ResourceFactory.createProperty(getLongName("core#hasCoverageLevel"));
        RDFNode object = ResourceFactory.createResource(getLongName(level.getUri()));

        // Now remove the old value and save the new value
        dataset.begin(ReadWrite.WRITE);
        resource.removeAll(property);
        resource.addProperty(property, object);
        dataset.commit();
        dataset.end();

        return true;
    }

    /* Method to override the proposed status of a CS in a graph without creating the CS in the
     * same graph. Needed to adjust user/client supplied status flags which appear as single
     * triples in the asserted graph, but without the CS entity (which is added later by the
     * validator in the inferred graph).
     */
    @Override
    public boolean updateProposedStatus(Boolean status, ControlSetDB cs, String model){
        String graphUri = stack.getGraph(model);
        if (graphUri == null) {
            return false;
        }
        Model datasetModel = dataset.getNamedModel(graphUri);

        // Encode the population level as a single property of the asset resource
        Resource resource = datasetModel.getResource(getLongName(cs.getUri()));
        Property property = ResourceFactory.createProperty(getLongName("core#isProposed"));

        // Now remove the old value and save the new value
        dataset.begin(ReadWrite.WRITE);
        resource.removeAll(property);
        resource.addLiteral(property, status.booleanValue());
        dataset.commit();
        dataset.end();

        return true;
    }


    /* Internal class passed to the Querier's GsonBuilder 
    */
    public static class StringDeserializer implements JsonDeserializer<String> {
        @Override
        public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (json.isJsonArray()) {
                json = json.getAsJsonArray().iterator().next();
            }
            String string = json.toString();
            return string.substring(1, string.length()-1);
        }
    }

}
