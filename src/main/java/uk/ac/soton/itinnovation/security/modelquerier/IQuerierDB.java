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
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelquerier;

import uk.ac.soton.itinnovation.security.modelquerier.dto.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface to query system entities. The intent of this interface is to be as general as possible, to allow for
 * easy replacement of the underlying database in the future. This interface refers to URIs for consistency with
 * current terminology, but implementing classes could take this to be any unique ID associated with an entity.
 */
public interface IQuerierDB {

    /* These initialisation methods have been made public so processes that need to create an IQuerierDB can
     * initialise them in a manner appropriate to their intended use.
     */
    void initForValidation();
    void initForRiskCalculation();
    void init();

    /* These 'helper' methods have been moved to the querier so they can be used by both
     * the validator and risk calculator.
     */
    List<String> getPseudorootAssets(MatchingPatternDB threatMatchingPattern);
    List<CardinalityConstraintDB> getLinksFrom(String assetURI);
    List<CardinalityConstraintDB> getLinksTo(String assetURI);
    List<String> getMatchingPatternNodes(String mpUri, String roleUri);
    Map<String, List<String>> getMatchingPatternNodes(String mpUri);
    ControlSetDB getAvgCS(String csURI, String... models);
    MisbehaviourSetDB getAvgMS(String msURI, String... models);
    TrustworthinessAttributeSetDB getAvgTWAS(String twasURI, String... models);
    String generateControlSetUri(String controlURI, AssetDB asset);
    String generateMisbehaviourSetUri(String misbehviourURI, AssetDB asset);
    String generateTrustworthinessAttributeSetUri(String twaURI, AssetDB asset);
    LevelDB lookupHighestTWLevel(LevelDB averageTW, LevelDB population, boolean independent);
    LevelDB lookupLowestTWLevel(LevelDB averageTW, LevelDB population, boolean independent);
    Integer[] getAdjustedLevels(AssetDB asset, Integer defaultLevel, Integer[] assertedValues);
    
    /* Each entity type has a get (singular) and get (plural) method.
     * Each getter takes a list of `models` from which the entities will be queried. If an entity exists
     * in multiple models, the properties from each will be combined.
     */

    /* Get model information and domain model features
     */
    ModelDB getModelInfo(String model);
    public Map<String,ModelFeatureDB> getModelFeatures(String... models);
 
    /* Get domain model scales
    */
    LevelDB getPopulationLevel(String uri);
    Map<String, LevelDB> getPopulationLevels();

    LevelDB getImpactLevel(String uri);
    Map<String, LevelDB> getImpactLevels();

    LevelDB getLikelihoodLevel(String uri);
    Map<String, LevelDB> getLikelihoodLevels();

    LevelDB getTrustworthinessLevel(String uri);
    Map<String, LevelDB> getTrustworthinessLevels();

    LevelDB getRiskLevel(String uri);
    Map<String, LevelDB> getRiskLevels();

    /* Get domain model construction state asset and relationship types
     *
     * No graph arguments because these can only come from the domain model graph
     */
    Set<String> getConstructionState();

    /* Get system model assets and relationships
     *
     * Graph argument needed as they can be in the asserted or inferred graphs. 
     */
    AssetDB getAsset(String uri, String... models);
    Map<String, AssetDB> getAssets(String... models);

    Map<String, CardinalityConstraintDB> getCardinalityConstraints(String... models);
    CardinalityConstraintDB getCardinalityConstraint(String uri, String... models);

    /* Get roles, nodes, links and patterns
    */
    RoleDB getRole(String uri, String... models);
    Map<String, RoleDB> getRoles(String... models);

    NodeDB getNode(String uri, String... models);
    Map<String, NodeDB> getNodes(String... models);

    RoleLinkDB getRoleLink(String uri, String... models);
    Map<String, RoleLinkDB> getRoleLinks(String... models);

    RootPatternDB getRootPattern(String uri, String... models);
    Map<String, RootPatternDB> getRootPatterns(String... models);

    MatchingPatternDB getMatchingPattern(String uri, String... models);
    Map<String, MatchingPatternDB> getMatchingPatterns(String... models);

    Map<String, DistinctNodeGroupDB> getDistinctNodeGroups(String... models);
    DistinctNodeGroupDB getDistinctNodeGroup(String uri, String... models);

    ConstructionPatternDB getConstructionPattern(String uri, String... models);
    Map<String, ConstructionPatternDB> getConstructionPatterns(String... models);

    InferredNodeSettingDB getInferredNodeSetting(String uri, String... models);
    Map<String, InferredNodeSettingDB> getInferredNodeSettings(String... models);

    /* Get controls, Misbehaviours, TWA and associated Sets 
    */
    ControlDB getControl(String uri, String... models);
    Map<String, ControlDB> getControls(String... models);

    Map<String, ControlSetDB> getControlSets(String... models);
    ControlSetDB getControlSet(String uri, String... models);

    Map<String, MisbehaviourDB> getMisbehaviours(String... models);
    MisbehaviourDB getMisbehaviour(String uri, String... models);

    Map<String, MisbehaviourSetDB> getMisbehaviourSets(String... models);
    MisbehaviourSetDB getMisbehaviourSet(String uri, String... models);
    MisbehaviourSetDB getMisbehaviourSet(AssetDB asset, String misbehaviour, String... models);

    TrustworthinessAttributeDB getTrustworthinessAttribute(String uri, String... models);
    Map<String, TrustworthinessAttributeDB> getTrustworthinessAttributes(String... models);

    TrustworthinessAttributeSetDB getTrustworthinessAttributeSet(AssetDB asset, String twa, String... models);
    TrustworthinessAttributeSetDB getTrustworthinessAttributeSet(String uri, String... models);
    Map<String, TrustworthinessAttributeSetDB> getTrustworthinessAttributeSets(String... models);

    Map<String, MisbehaviourInhibitionSetDB> getMisbehaviourInhibitionSets(String... models);
    MisbehaviourInhibitionSetDB getMisbehaviourInhibitionSet(String uri, String... models);

    Map<String, TrustworthinessImpactSetDB> getTrustworthinessImpactSets(String... models);
    TrustworthinessImpactSetDB getTrustworthinessImpactSet(String uri, String... models);

    /* Get threats and control strategies
    */
    ThreatDB getThreat(String uri, String... models);
    Map<String, ThreatDB> getThreats(String... models);

    Map<String, ControlStrategyDB> getControlStrategies(String... models);
    ControlStrategyDB getControlStrategy(String uri, String... models);

    /* Get asset attribute, behaviour and control default settings
    */
    CASettingDB getCASetting(AssetDB asset, String control);
    CASettingDB getCASetting(String uri);
    Map<String, CASettingDB> getCASettings();

    MADefaultSettingDB getMADefaultSetting(AssetDB asset, String misbehaviour);
    MADefaultSettingDB getMADefaultSetting(String uri);
    Map<String, MADefaultSettingDB> getMADefaultSettings();

    TWAADefaultSettingDB getTWAADefaultSetting(AssetDB asset, String twa);
    TWAADefaultSettingDB getTWAADefaultSetting(String uri);
    Map<String, TWAADefaultSettingDB> getTWAADefaultSettings();

    /* Each entity type has a store method.
     * Each store method takes a `model`, this indicates the 'main model'. If this is a new entity (i.e. not checked
     * out from the store), then the entire entity will be stored in the main model. If this is an existing entity
     * (i.e. checked out from the store), then the main model should be the model which contains the entity's type
     * relation. An existing entity may have 'loose properties', properties of the entity which are not stored in the
     * main model. A loose property should remain in its current model UNLESS it has changed value, in which
     * case it should be removed from its current model and moved to the main model.
     */

    boolean store(AssetDB asset, String model);
    boolean store(NodeDB node, String model);
    boolean store(MatchingPatternDB matchingPattern, String model);
    boolean store(ThreatDB threat, String model);
    boolean store(MisbehaviourInhibitionSetDB misbehaviourInhibitionSet, String model);
    boolean store(MisbehaviourSetDB misbehaviourSet, String model);
    boolean store(TrustworthinessAttributeSetDB trustworthinessAttributeSet, String model);
    boolean store(TrustworthinessImpactSetDB trustworthinessImpactSet, String model);
    boolean store(ControlStrategyDB controlStrategy, String model);
    boolean store(ControlSetDB controlSet, String model);
    boolean store(CardinalityConstraintDB cardinalityConstraint, String model);
    boolean store(ModelDB modelInfo, String model);

    /* Asset and relationship deletion (everything else is inferred so cannot be deleted).
     *
     * These are still 'work in progress'. They all return 'false' if the deletion is not
     * successful, including cases that just aren't implemented yet.
     */
    boolean delete(CardinalityConstraintDB link, boolean invalidateModel);
    boolean delete(AssetDB asset, boolean invalidateModel);
    boolean deleteAssets(Map<String, AssetDB> assets, boolean invalidateModel);
    boolean deleteCardinalityConstraints(Map<String, CardinalityConstraintDB> links, boolean invalidateModel);

    /* Special cases used to fill in or correct data in the asserted graph
     */
    void repairAssertedAssetPopulations();
    void repairCardinalityConstraints();
    boolean updateAssertedLevel(LevelDB level, TrustworthinessAttributeSetDB twas, String model);
    boolean updateCoverageLevel(LevelDB level, ControlSetDB cs, String model);
    boolean updateProposedStatus(Boolean status, ControlSetDB cs, String model);

    /**
     * Get the sub types of `type`. If `includeSelf` is true, then `type` will be included in the returned list.
     */
    List<String> getSubTypes(String type, boolean includeSelf);
    /**
     * Get the sub types of `type`. If `includeSelf` is true, then `type` will be included in the returned list.
     */
    List<String> getSuperTypes(String type, boolean includeSelf);

    /**
     * OPTIONAL: This method synchronises the in-java memory and the database, if such an operation is required.
     */
    void sync(String... models);
}
