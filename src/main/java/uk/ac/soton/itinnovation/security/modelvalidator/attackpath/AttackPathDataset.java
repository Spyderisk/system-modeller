/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2023
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
//      Created By:             Panos Melas
//      Created Date:           2023-01-24
//      Created for Project :   Cyberkit4SME
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelvalidator.attackpath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.soton.itinnovation.security.model.Level;
import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;
import uk.ac.soton.itinnovation.security.model.system.RiskVector;
import uk.ac.soton.itinnovation.security.modelquerier.IQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.AssetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ControlDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ControlSetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ControlStrategyDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.LevelDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.MisbehaviourDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.MisbehaviourSetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ModelDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ThreatDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.TrustworthinessAttributeSetDB;
import uk.ac.soton.itinnovation.security.modelquerier.util.QuerierUtils;
import uk.ac.soton.itinnovation.security.modelvalidator.Progress;
import uk.ac.soton.itinnovation.security.modelvalidator.RiskCalculator;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.recommendations.AssetDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.recommendations.ConsequenceDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.recommendations.ControlDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.recommendations.StateDTO;


public class AttackPathDataset {

    private static final Logger logger = LoggerFactory.getLogger(AttackPathDataset.class);

    protected IQuerierDB querier;

    protected Map<String, LevelDB> poLevels = new HashMap<>(); // Map of domain model population levels indexed by URI
    protected Map<String, LevelDB> liLevels = new HashMap<>(); // Map of domain model likelihood levels indexed by URI
    protected Map<String, LevelDB> twLevels = new HashMap<>(); // Map of domain model trustworthiness levels indexed by URI
    protected Map<String, LevelDB> imLevels = new HashMap<>(); // Map of domain model impact levels indexed by URI
    protected Map<String, LevelDB> riLevels = new HashMap<>(); // Map of domain model risk levels indexed by URI

    protected List<LevelDB> riskLevels = new ArrayList<>(); // List of risk levels, ordered by level value (increasing risk)

    protected Map<String, AssetDB> assets = new HashMap<>(); // Map of system model assets, indexed by asset URI
    protected Map<String, ThreatDB> threats = new HashMap<>(); // Map of system model threats, indexed by threat URI

    protected Map<String, MisbehaviourSetDB> misbehaviourSets = new HashMap<>(); // Map of system model misbehaviour sets (MS), indexed by URI
    protected Map<String, TrustworthinessAttributeSetDB> trustworthinessAttributeSets = new HashMap<>(); // Map of system model trustworthiness attribute sets (TWAS), indexed by URI
    protected Map<String, ControlSetDB> controlSets = new HashMap<>(); // Map of system model control sets (CS), indexed by URI
    protected Map<String, ControlStrategyDB> controlStrategies = new HashMap<>(); // Map of system model control strategies (CSG), indexed by URI

    protected Map<String, MisbehaviourSetDB> entryPointMisbehaviour = new HashMap<>(); // Map of system model MS associated with a TWAS, indexed by the TWAS URI
    protected Map<String, String> misbehaviourTWAS = new HashMap<>(); // Map of system model TWAS URI associated with an MS, indexed by the MS URI

    ////////////////////////////////////////////////
    // Attack path datasets only
    private Map<String, String> likelihoods = new HashMap<>(); // Map of likelihoods for threats and misbehaviours
    private Set<String> normalOps = new HashSet<>(); // Set of normal ops

    public AttackPathDataset(IQuerierDB querier) {

        final long startTime = System.currentTimeMillis();

        // Save the querier reference for use in other methods
        this.querier = querier;

        // Load domain model poulation, impact, trustworthiness, risk and likelihood scales as maps keyed on their short URI (e.g. "domain#RiskLevelMedium")
        poLevels = querier.getPopulationLevels();
        imLevels = querier.getImpactLevels();
        liLevels = querier.getLikelihoodLevels();
        twLevels = querier.getTrustworthinessLevels();
        riLevels = querier.getRiskLevels();

        // Make a sorted list of the LevelDB objects by their risk level values
        riskLevels.addAll(riLevels.values());
        riskLevels.sort(Comparator.comparingInt(LevelDB::getLevelValue));

        // Load system model assets, matching patterns and nodes
        assets = querier.getAssets("system", "system-inf");

        updateDatasets();

        final long endTime = System.currentTimeMillis();
        logger.info("AttackPathDataset.AttackPathDataset(IQuerierDB querier): execution time {} ms",
                endTime - startTime);

    }

    private void updateDatasets() {

        // Load system model trustworthiness attribute sets
        trustworthinessAttributeSets = querier.getTrustworthinessAttributeSets("system-inf");

        // Load system model control sets
        controlSets = querier.getControlSets("system-inf");

        // Load system model misbehaviour sets
        misbehaviourSets = querier.getMisbehaviourSets("system-inf");

        // Load system model threats
        threats = querier.getThreats("system-inf");

        // Load system model control strategies and determine whether they are enabled
        controlStrategies = querier.getControlStrategies("system-inf");

        // Create likelihood maps
        for (ThreatDB threat : threats.values()) {
            likelihoods.put(threat.getUri(), threat.getPrior());
        }
        for (MisbehaviourSetDB miss : misbehaviourSets.values()) {
            likelihoods.put(miss.getUri(), miss.getPrior());
        }

    }


    /*
     * Create maps required by the risk calculation to find TWAS, MS and their relationship to roles
     */
    protected void createMaps() {

        final long startTime = System.currentTimeMillis();

        // Create likelihood maps
        for (ThreatDB threat : threats.values()) {
            likelihoods.put(threat.getUri(), threat.getPrior());
        }
        for (MisbehaviourSetDB miss : misbehaviourSets.values()) {
            likelihoods.put(miss.getUri(), miss.getPrior());
        }

        final long endTime = System.currentTimeMillis();

        logger.debug("*********CREATE MAPS*********");
        logger.debug("AttackPathDataset threats: {}", threats.size());
        logger.debug("AttackPathDataset MS: {}", misbehaviourSets.size());
        logger.debug("AttackPathDataset likelihoods: {}", likelihoods.size());
        logger.debug("*****************************");
        logger.info("AttackPathDataset.CreateMaps(): execution time {} ms", endTime - startTime);
    }

    public boolean isFutureRisk(String input) {
        try {
            RiskCalculationMode requestedMode = RiskCalculationMode.valueOf(input);
            return requestedMode == RiskCalculationMode.FUTURE;
        } catch (IllegalArgumentException e) {
            // TODO: throw an exception
            logger.warn("Found unexpected riskCalculationMode parameter value {}.", input);
            return false;
        }
    }

    public String getCSGDescription(String uri) {
        ControlStrategyDB csg = controlStrategies.get(uri);
        return csg.getDescription();
    }

    public Map<String, String> getLikelihoods() {
        return this.likelihoods;
    }

    public Set<String> getNormalOps() {
        return this.normalOps;
    }

    /**
     * get misbehaviour direct cause uris.
     *
     * @param uri
     * @return
     */
    public List<String> getMisbehaviourDirectCauseUris(String misbUri) throws RuntimeException {
        try {
            MisbehaviourSetDB ms = misbehaviourSets.get(misbUri);
            return new ArrayList<>(ms.getCausedBy());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * get threat direct cause uris.
     *
     * @param uri
     * @return
     */
    public List<String> getThreatDirectCauseUris(String threatUri) throws RuntimeException {
        try {
            ThreatDB threat = threats.get(threatUri);
            return new ArrayList<>(threat.getCausedBy());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Check if a Control Strategy Group (CSG) is activated.
     *
     * This method evaluates whether all mandatory Control Sets (CS) associated
     * with the given CSG are proposed.
     *
     * @param csg The Control Strategy Group to be checked.
     * @return {@code true} if all mandatory Control Sets are proposed,
     * otherwise {@code false}.
     */
    public boolean isCSGActivated(ControlStrategyDB csg) {
        return csg.getMandatoryCS().stream().allMatch(cs -> controlSets.get(cs).isProposed());
    }

    /**
     * Check if control strategy plan exists and is activated need to have a
     * different way checking for contingency plans
     *
     * @param csg the control stragegy
     * @return {@code true} if contingency plan exists and is activated,
     * otherwise {@code false}
     */
    public boolean hasContingencyPlan(String csgUri) throws RuntimeException {
        try {
            String contingencyPlan;
            if (csgUri.contains("-Implementation")) {
                contingencyPlan = csgUri.replaceAll("-Implementation-Runtime|-Implementation", "");
            } else {
                return true;
            }

            if (controlStrategies.containsKey(contingencyPlan)) {
                return isCSGActivated(controlStrategies.get(contingencyPlan));
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * return false when this CSG
     *  - has no effect in future risk calculations
     *  - has no effect in current risk calculations
     *  - cannot be changed at runtime
     * @param csg
     * @param future
     * @return 
     */
    boolean considerCSG(ControlStrategyDB csg, boolean future) {
        if (future) {
            return csg.isFutureRisk();
        } else {
            return csg.isCurrentRisk() && isRuntimeMalleable(csg);
        }
    }

    /**
     * Check if CS is runtime malleable assume all -Implementation,
     * -Implementation-Runtime CSGs have contingency plans activated.
     *
     * @param csg
     * @return boolean
     */
    Boolean isRuntimeMalleable(ControlStrategyDB csg) {
        if (csg.getUri().contains("-Implementation")) {
            return true;
            //return hasContingencyPlan(csg.getUri());
        } else if (csg.getUri().contains("-Runtime")) {
            return true;
        }
        return false;
    }

    public Set<String> getThreatControlStrategyUris(String threatUri, boolean future) throws RuntimeException {
        // Return list of control strategies (urirefs) that block a threat (uriref)

        Set<String> csgToConsider = new HashSet<>();
        ThreatDB threat = this.threats.get(threatUri);
        try {
            for (String csgURI : threat.getBlockedByCSG()) {
                ControlStrategyDB csg = querier.getControlStrategy(csgURI, "system-inf");
                if (considerCSG(csg, future)) {
                    csgToConsider.add(csgURI);
                } else {
                    logger.debug("CSG {} is NOT considered", csgURI);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return csgToConsider;
    }

    /**
     * get CSG control sets uris
     *
     * @param csgUri
     * @return
     */
    public List<String> getCsgControlSetsUris(String csgUri) throws RuntimeException {
        try {
            List<String> csList = new ArrayList<>();
            for (String csUri : controlStrategies.get(csgUri).getMandatoryCS()) {
                csList.add(csUri);
            }
            return csList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * get CSG control sets
     *
     * @param csgUri
     * @return
     */
    public List<ControlSetDB> getCsgControlSets(String csgUri) throws RuntimeException {
        try {
            List<ControlSetDB> csList = new ArrayList<>();
            for (String csUri : controlStrategies.get(csgUri).getMandatoryCS()) {
                csList.add(controlSets.get(csUri));
            }
            return csList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * get CSG inactive CS
     *
     * @param csgUri
     * @return
     */
    public List<String> getCsgInactiveControlSets(String csgUri) {
        try {
            return controlSets.values().stream()
                    .filter(cs -> !cs.isProposed() && (isMandatoryCS(csgUri, cs) || isOptionalCS(csgUri, cs)))
                    .map(ControlSetDB::getUri)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isMandatoryCS(String csgUri, ControlSetDB cs) {
        return controlStrategies.get(csgUri).getMandatoryCS().contains(cs.getUri());
    }

    private boolean isOptionalCS(String csgUri, ControlSetDB cs) {
        return controlStrategies.get(csgUri).getOptionalCS().contains(cs.getUri());
    }

    /**
     * get threat inactive CSGs
     *
     * @param threatUri
     * @return
     */
    public List<String> getThreatInactiveCSGs(String threatUri, boolean future) throws RuntimeException {
        try {
            List<String> csgUriList = new ArrayList<>();
            for (String csgUri : getThreatControlStrategyUris(threatUri, future)) {
                if (!getCsgInactiveControlSets(csgUri).isEmpty()) {
                    csgUriList.add(csgUri);
                }
            }
            return csgUriList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
      * Return MS with risk level > acceptableRiskLevel
      */
    public List<String> filterMisbehavioursByRiskLevel(String acceptableRiskLevel) {

        List<String> msUris = new ArrayList<>();

        logger.debug("filtering misbehaviour sets...");

        int acceptableThreshold = riLevels.get(acceptableRiskLevel).getLevelValue();
        for (MisbehaviourSetDB ms : misbehaviourSets.values()) {
            if (riLevels.get(ms.getRisk()).getLevelValue() > acceptableThreshold) {
                msUris.add(ms.getUri());
            }
        }

        logger.debug("filtered MS sets size: {}/{}", msUris.size(), misbehaviourSets.size());

        return msUris;
    }


    public boolean isExternalCause(String uri) {
        if (misbehaviourSets.containsKey(uri)) {
            MisbehaviourSetDB ms = querier.getMisbehaviourSet(uri, "system-inf");
            return (ms != null) && ms.isExternalCause();
        } else if (trustworthinessAttributeSets.containsKey(uri)) {
            TrustworthinessAttributeSetDB twa = trustworthinessAttributeSets.get(uri);
            return (twa != null) && twa.isExternalCause();
        }
        return false;
    }

    /**
     * check URI is a normal operation
     *
     * @param uri string of a threat or misbehaviour
     * @rerutn boolean
     */
    public boolean isNormalOp(String uri) {

        // check if we have to deal with a threat URI
        if (this.threats.containsKey(uri)) {
            ThreatDB threat = this.querier.getThreat(uri, "system-inf");
            return (threat != null) && threat.isNormalOperation();
        } else if (misbehaviourSets.containsKey(uri)) {
            MisbehaviourSetDB ms = querier.getMisbehaviourSet(uri, "system-inf");
            return (ms != null) && ms.isNormalOpEffect();
        } else if (trustworthinessAttributeSets.containsKey(uri)) {
            return false;
        } else {
            logger.warn("Not sure what is this: {}", uri);
            return false;
        }
    }

    // describes if the URI refers to an initial cause misbehaviour
    public boolean isInitialCause(String uri) {
        return threats.containsKey(uri) && threats.get(uri).isInitialCause();
    }

    public boolean isThreatSimple(String uri) {
        return this.threats.keySet().contains(uri);
    }

    public boolean isMisbehaviourSet(String uri) {
        return this.misbehaviourSets.keySet().contains(uri);
    }

    public boolean isTrustworthinessAttributeSets(String uri) {
        return this.trustworthinessAttributeSets.keySet().contains(uri);
    }

    public boolean isThreat(String uri) {
        return this.threats.keySet().contains(uri);
    }

    public ThreatDB getThreat(String uri) {
        return threats.get(uri);
    }

    public void printThreatUris() {
        for (String uri : this.threats.keySet()) {
            logger.debug("Threat: {}", uri);
        }
    }

    public boolean isSecondaryThreat(String uri) {
        return threats.containsKey(uri) && threats.get(uri).getSecondaryEffectConditions().size() > 0;
    }

    public boolean isRootCause(String uri) {
        if (threats.keySet().contains(uri)) {
            ThreatDB threat = this.querier.getThreat(uri, "system-inf");
            if (threat != null) {
                return threat.isRootCause();
            }
        }
        return false;
    }

    public String getLikelihood(String uri) {
        if (this.likelihoods.keySet().contains(uri)) {
            return this.likelihoods.get(uri);
        }
        return "";
    }

    /**
     * Check risk calculation mode is the same as the requested one
     * @param input
     * @return 
     */
    public boolean checkRiskCalculationMode(String input) {
        ModelDB model = querier.getModelInfo("system");
        logger.info("Model info: {}", model);

        RiskCalculationMode modelRiskCalculationMode;
        RiskCalculationMode requestedMode;

        try {
            logger.info("riskCalculationMode: {}", model.getRiskCalculationMode());
            modelRiskCalculationMode = model.getRiskCalculationMode() != null ? RiskCalculationMode.valueOf(model.getRiskCalculationMode()) : null;
            requestedMode = RiskCalculationMode.valueOf(input);

            return modelRiskCalculationMode == requestedMode;

        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean checkRiskLevelKey(String riskKey) {
        return riLevels.containsKey(riskKey);
    }

    /** Checks if all elements in the given list represent a valid misbehaviour
     * set.
     *
     * This method iterates through the list of misbehaviour set identifiers
     * and checks each one to determine if it corresponds to a valid
     * misbehaviour set.
     *
     * @param misbehaviourSetList A list of misbehavour set short URIs as
     * strings
     * @return {@code true} if every identifier in the list corresponds to a valid
     * misbehaviour set, otherwise {@code false}.
     */
    public boolean checkMisbehaviourList(List<String> misbehaviourSetList) {

        for (String misb : misbehaviourSetList) {
            if (!this.isMisbehaviourSet(misb)) {
                logger.warn("failed to identify MS: {}", misb);
                return false;
            }
        }

        return true;
    }

    public AssetDTO fillAssetDTO(String assetUri) {
        AssetDB asset = assets.get(assetUri);
        AssetDTO assetDTO = new AssetDTO();
        assetDTO.setUri(asset.getUri());
        assetDTO.setType(asset.getType());
        assetDTO.setLabel(asset.getLabel());
        assetDTO.setIdentifier(asset.getId());
        return assetDTO;
    }

    public ControlDTO fillControlDTO(String csUri) {
        ControlDTO ctrl = new ControlDTO();
        ControlSetDB cs = controlSets.get(csUri);
        ControlDB control = querier.getControl(cs.getControl(), "domain");

        ctrl.setUri(csUri);
        ctrl.setLabel(control.getLabel());
        ctrl.setDescription(control.getDescription());
        ctrl.setAsset(fillAssetDTO(cs.getLocatedAt()));
        ctrl.setAction("Enable control");

        return ctrl;
    }

    public void changeCS(Set<String> csSet, boolean proposed) {
        logger.info("changeCS list ({} {}): {}", proposed ? "enabling" : "disabling", csSet.size(), csSet);

        for (String csURIa : csSet) {

            logger.debug("  └──> {}", csURIa);

            Set<String> csTriplet = QuerierUtils.getControlTriplet(csURIa);

            for (String csURI : csTriplet) {
                logger.debug("     Set triplet {}: proposed -> {}", csURI, proposed);
                querier.updateProposedStatus(proposed, csURI, "system");
            }

        }

    }

    public RiskVector calculateRisk(String modelId, RiskCalculationMode riskMode) throws RuntimeException {
        try {

            RiskCalculator rc = new RiskCalculator(querier);
            rc.calculateRiskLevels(riskMode, false, new Progress(modelId));

            updateDatasets();

            return getRiskVector();
        } catch (Exception e) {
            logger.error("Error calculating risks for APD", e);
            throw new RuntimeException("Failed to calculate risk", e);
        }

    }

    public RiskVector getRiskVector() {

        Map<String, Integer> riskVector = new HashMap<>();
        Collection<Level> rvRiskLevels = new ArrayList<>();
        for (LevelDB level : riLevels.values()) {
            riskVector.put(level.getUri(), 0);
            Level l = new Level();
            l.setValue(Integer.valueOf(level.getLevelValue()));
            l.setUri(level.getUri());
            rvRiskLevels.add(l);
        }

        for (MisbehaviourSetDB ms : misbehaviourSets.values()) {
            riskVector.put(ms.getRisk(), riskVector.get(ms.getRisk()) + 1);
        }

        return new RiskVector(rvRiskLevels, riskVector);
    }

    public String validateRiskLevel(String uri) {
        return uri;
    }

    /**
     * Compare two risk levels specified by URI fragments
     */
    public int compareRiskLevelURIs(String overallRiskA, String overallRiskB) {
        logger.debug("Overall Risk Comparison: riskA({}) ? riskB({})", overallRiskA, overallRiskB);

        int levelA = riLevels.get(overallRiskA).getLevelValue();
        int levelB = riLevels.get(overallRiskB).getLevelValue();

        // Compare levelA and levelB and return -1, 0, or 1
        return Integer.compare(levelA, levelB);
    }

    /*
     * Compare the risk levels of a list of misbehaviour sets with another single level
     */
    public int compareMSListRiskLevel(List<String> targetMSURIs, String otherRiskURI) {
        int targetRiskLevel = riLevels.get(otherRiskURI).getLevelValue();
        int maxRiskLevel = 0;

        for (String msURI : targetMSURIs) {
            int riskLevel = riLevels.get(misbehaviourSets.get(msURI).getRisk()).getLevelValue();
            if (riskLevel > maxRiskLevel) {
                maxRiskLevel = riskLevel;
            }
        }

        return Integer.compare(maxRiskLevel, targetRiskLevel);
    }

    public StateDTO getState() {
        // state is risk + list of consequences

        Map<String, Integer> riskVector = new HashMap<>();
        Collection<Level> rvRiskLevels = new ArrayList<>();
        for (LevelDB level : riLevels.values()) {
            riskVector.put(level.getUri(), 0);
            Level l = new Level();
            l.setValue(Integer.valueOf(level.getLevelValue()));
            l.setUri(level.getUri());
            rvRiskLevels.add(l);
        }

        List<ConsequenceDTO> consequences = new ArrayList<>();
        for (MisbehaviourSetDB ms : misbehaviourSets.values()) {

            riskVector.put(ms.getRisk(), riskVector.get(ms.getRisk()) + 1);
            int threshold = riLevels.get("domain#RiskLevelMedium").getLevelValue();
            if (riLevels.get(ms.getRisk()).getLevelValue() >= threshold) {
                MisbehaviourDB msdb = querier.getMisbehaviour(ms.getMisbehaviour(), "domain");
                ConsequenceDTO consequence = new ConsequenceDTO();
                consequence.setLabel(msdb.getLabel().replaceAll("(?<!^)([A-Z])", " $1"));
                consequence.setDescription(msdb.getDescription());
                consequence.setUri(ms.getUri());
                consequence.setRisk(ms.getRisk());
                consequence.setImpact(ms.getImpactLevel());
                consequence.setLikelihood(ms.getPrior());
                AssetDB asset = assets.get(ms.getLocatedAt());
                AssetDTO assetDTO = fillAssetDTO(asset.getUri());
                consequence.setAsset(assetDTO);
                consequences.add(consequence);
            }
        }

        StateDTO state = new StateDTO();
        state.setRisk(riskVector);
        state.setConsequences(consequences);

        return state;

    }

    public Set<String> getAllCS() {
        Set<String> css = new HashSet<>();
        for (ControlSetDB cs : controlSets.values()) {
            css.add(cs.getUri());
        }
        return css;
    }
}
