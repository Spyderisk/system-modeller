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
import uk.ac.soton.itinnovation.security.modelquerier.dto.MisbehaviourSetDB;
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

        // Load domain model poulation, impact, trustworthiness, risk and likelihood scales as maps keyed on their URI
        poLevels = querier.getPopulationLevels();
        imLevels = querier.getImpactLevels();
        liLevels = querier.getLikelihoodLevels();
        twLevels = querier.getTrustworthinessLevels();
        riLevels = querier.getRiskLevels();

        // Load domain model impact, trustworthiness, risk, and likelihood scales as lists sorted by their level value
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
            logger.error("Found unexpected riskCalculationMode parameter value {}.", input);
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

    public List<String> filterMisbehaviours() throws RuntimeException {
        return filterMisbehaviours("domain#RiskLevelMedium");
    }

    public List<String> filterMisbehaviours(String riskLevel) throws RuntimeException {
        /*
         * compare MS by risk then likelihood, and return MS with likelihood or risk >= MEDIUM
         */

        List<String> msUris = new ArrayList<>();

        try {
            logger.debug("filtering misbehaviour sets...");

            List<MisbehaviourSetDB> msSorted = new ArrayList<>(misbehaviourSets.values());

            Comparator<MisbehaviourSetDB> comparator = Comparator.comparing(MisbehaviourSetDB::getRisk)
                    .thenComparing(MisbehaviourSetDB::getPrior);

            msSorted.sort(comparator);

            int threshold = riLevels.get(riskLevel).getLevelValue();
            List<MisbehaviourSetDB> msFiltered = msSorted.stream()
                    .filter(ms -> riLevels.get(ms.getRisk()).getLevelValue() >= threshold).collect(Collectors.toList());

            for (MisbehaviourSetDB ms : msFiltered) {
                AssetDB asset = assets.get(ms.getLocatedAt());
                logger.debug("filtered MS:   {} \t-> risk {} prior {} at {}", ms.getUri().substring(7),
                        ms.getRisk().substring(7), ms.getPrior().substring(7), asset.getLabel());
                msUris.add(ms.getUri());
            }

            logger.debug("filtered MS sets size: {}/{}", msUris.size(), misbehaviourSets.size());

        } catch (Exception e) {
            logger.error("got an error filtering misbehaviours: {}", e.getMessage());
            throw new RuntimeException("got an error filtering misbehavours", e);
        }

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

    // check MS list exists, no point going futher
    public boolean checkMisbehaviourList(List<String> misbehaviours) {
        boolean retVal = true;

        for (String misb : misbehaviours) {
            if (!this.isMisbehaviourSet(misb)) {
                logger.warn("failed to identify MS: {}", misb);
                retVal = false;
                break;
            }
        }

        return retVal;
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
        logger.info("changeCS list: {}", csSet);

        String logMessage = proposed ? "enabling" : "disabling";
        logger.debug("{} CS for {} controls:", logMessage, csSet.size());

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
            logger.info("Calculating risks for APD");

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

    public boolean compareOverallRiskToMedium(String overall) {
        int level = riLevels.get(overall).getLevelValue();
        int threshold = riLevels.get("domain#RiskLevelMedium").getLevelValue();
        boolean retVal = threshold >= level;
        logger.debug("Overall Risk Comparison: Medium >= {} --> {}", overall, retVal);
        return retVal;
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
                ConsequenceDTO consequence = new ConsequenceDTO();
                consequence.setUri(ms.getUri());
                consequence.setRisk(ms.getRisk());
                consequence.setImpact(ms.getImpactLevel());
                consequence.setLikelihood(ms.getPrior());
                AssetDB asset = assets.get(ms.getLocatedAt());
                AssetDTO assetDTO = new AssetDTO();
                assetDTO.setUri(asset.getUri());
                assetDTO.setType(asset.getType());
                assetDTO.setLabel(asset.getLabel());
                consequence.setAsset(assetDTO);
                consequences.add(consequence);
            }
        }

        StateDTO state = new StateDTO();
        state.setRisk(riskVector.toString());
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
