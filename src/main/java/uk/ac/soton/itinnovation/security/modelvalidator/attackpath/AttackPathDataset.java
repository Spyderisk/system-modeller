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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.soton.itinnovation.security.modelquerier.IQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.AssetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ControlSetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ControlStrategyDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.LevelDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.MisbehaviourSetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ThreatDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.TrustworthinessAttributeSetDB;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import java.util.stream.Collectors;

public class AttackPathDataset {
    private static final Logger logger = LoggerFactory.getLogger(AttackPathDataset.class);

    protected IQuerierDB querier;

    protected Map<String, LevelDB> poLevels = new HashMap<>();                // Map of domain model population levels indexed by URI
    protected Map<String, LevelDB> liLevels = new HashMap<>();                // Map of domain model likelihood levels indexed by URI
    protected Map<String, LevelDB> twLevels = new HashMap<>();                // Map of domain model trustworthiness levels indexed by URI
    protected Map<String, LevelDB> imLevels = new HashMap<>();                // Map of domain model impact levels indexed by URI
    protected Map<String, LevelDB> riLevels = new HashMap<>();                // Map of domain model risk levels indexed by URI

    protected List<LevelDB> riskLevels = new ArrayList<>();                   // List of risk levels, ordered by level value (increasing risk)

    protected Map<String, AssetDB> assets = new HashMap<>();                                              // Map of system model assets, indexed by asset URI
    protected Map<String, ThreatDB> threats = new HashMap<>();                                            // Map of system model threats, indexed by threat URI

    protected Map<String, MisbehaviourSetDB> misbehaviourSets = new HashMap<>();                          // Map of system model misbehaviour sets (MS), indexed by URI
    protected Map<String, TrustworthinessAttributeSetDB> trustworthinessAttributeSets = new HashMap<>();  // Map of system model trustworthiness attribute sets (TWAS), indexed by URI
    protected Map<String, ControlSetDB> controlSets = new HashMap<>();                                    // Map of system model control sets (CS), indexed by URI
    protected Map<String, ControlStrategyDB> controlStrategies = new HashMap<>();                         // Map of system model control strategies (CSG), indexed by URI

    protected Map<String, MisbehaviourSetDB> entryPointMisbehaviour = new HashMap<>();                    // Map of system model MS associated with a TWAS, indexed by the TWAS URI
    protected Map<String, String> misbehaviourTWAS = new HashMap<>();                                     // Map of system model TWAS URI associated with an MS, indexed by the MS URI

    ////////////////////////////////////////////////
    // Attack path datasets only
    private Map<String, String> likelihoods = new HashMap<>();             // Map of likelihoods for threats and misbehaviours
    private Set<String> normalOps = new HashSet<>();                       // Set of normal ops

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

        final long endTime = System.currentTimeMillis();
        logger.info("AttackPathDataset.AttackPathDataset(IQuerierDB querier): execution time {} ms", endTime - startTime);

    }

    /* Create maps required by the risk calculation to find TWAS, MS and their relationship to roles
    */
    protected void createMaps(){

        final long startTime = System.currentTimeMillis();

        // Create likelihood maps
        for(ThreatDB threat : threats.values()) {
            likelihoods.put(threat.getUri(), threat.getPrior());
        }
        for(MisbehaviourSetDB miss : misbehaviourSets.values()) {
            likelihoods.put(miss.getUri(), miss.getPrior());
        }

        logger.debug("*********CREATE MAPS*********");
        logger.debug("AttackPathDataset threats: {}", threats.size());
        logger.debug("AttackPathDataset MS: {}", misbehaviourSets.size());
        logger.debug("AttackPathDataset likelihoods: {}", likelihoods.size());
        logger.debug("*****************************");

        final long endTime = System.currentTimeMillis();
        logger.info("AttackPathDataset.CreateMaps(): execution time {} ms", endTime - startTime);
    }

    public boolean calculateAttackPath()  throws RuntimeException {
        try {
            createMaps();
            return true;
        } catch (Exception e) {
            logger.error("calculating attack path dataset failed", e);
            throw new RuntimeException(e);
        }
    }

    private void printAttackPathDataset() {
        logger.debug("*******************************************************");
        logger.debug("*******************************************************");
        logger.debug("Threat CSGs:");
        for(ThreatDB threat : threats.values()){
            int csgsSize = threat.getBlockedByCSG().size() + threat.getMitigatedByCSG().size();
            if (csgsSize>0){
                logger.debug(" {}, blocked: {} mitigated: {}",
                        threat.getUri(), threat.getBlockedByCSG().size(),
                        threat.getMitigatedByCSG().size());
            }
            Collection<String> csgsBlocked = threat.getBlockedByCSG();
            if(csgsBlocked.size()>0){
                for(String csg : csgsBlocked){
                    List<String> css = this.controlStrategies.get(csg).getMandatoryCS();
                    logger.debug("      CSG blocked: {}, cs: {}", csg, css.size());
                    for(String cs : css){
                        logger.debug("      cs: {}", cs);
                    }
                }
            }
        }

        logger.debug("Control Strategies");
        for(ControlStrategyDB csg : controlStrategies.values()) {
            logger.debug("CSG: {} cs: {}", csg.getUri(), csg.getMandatoryCS().size());
            for(String cs : csg.getMandatoryCS()) {
                logger.debug("      cs: {}", cs);
            }
        }

        logger.debug("ContolSets:");
        for(ControlSetDB cs : controlSets.values()){
            logger.debug("ControlSet: {}, proposed {}", cs.getUri(), cs.isProposed());
        }
        logger.debug("Misbehaviours");
        for(MisbehaviourSetDB ms : misbehaviourSets.values()){
            AssetDB asset = assets.get(ms.getLocatedAt());
            logger.debug("  MS {}, likelihood: {}, risk: {}, asset: {}",
                    ms.getUri(), ms.getPrior(), ms.getRisk(), asset.getLabel());
        }
        logger.debug("*******************************************************");
        logger.debug("*******************************************************");
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
    //TODO MS will provide a direct call to get uris
    public List<String> getMisbehaviourDirectCauseUris(String misbUri)   throws RuntimeException {
        try {
            MisbehaviourSetDB ms = misbehaviourSets.get(misbUri);
            return new ArrayList<>(ms.getCausedBy());
        } catch (Exception e) {
            return new ArrayList<String>();
        }
    }

     /**
     * get threat direct cause uris.
     *
     * @param uri
     * @return
     */
    public List<String> getThreatDirectCauseUris(String threatUri)   throws RuntimeException {
        try {
            ThreatDB threat = threats.get(threatUri);
            return new ArrayList<>(threat.getCausedBy());
        } catch (Exception e) {
            return new ArrayList<String>();
        }
    }

    /**
     * check if CSG ends in -Runtime or -Implementation
     *
     * @param csgUri
     * @return
     */
    public boolean isCurrentRiskCSG(String csgUri) {
        return this.checkImplementationRuntime(csgUri);
    }

    private boolean checkImplementationRuntime(String csgUri) {
        Pattern pattern = Pattern.compile("\\b-Implementation-Runtime\\b|\\b-Implementation\\b");
        Matcher matcher = pattern.matcher(csgUri);
        if (matcher.find()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * check if CSG ends in -Implementation-Runtime or -Implementation
     *
     * @param csgUri
     * @return
     */
    public boolean isFutureRiskCSG(String csgUri) {
        // TODO: REGEX is now changed!!!
        return !(csgUri.endsWith("-Implementation-Runtime") || csgUri.endsWith("-Implementation"));
    }

    /**
     * check if CSG has a contingency plan
     *
     * @param csgUri
     * @return
     */
    public boolean checkContingencyPlan(String csgUri)   throws RuntimeException {
        try {
            String contingencyPlan;
            if(this.checkImplementationRuntime(csgUri)){
                contingencyPlan = csgUri.replaceAll("-Implementation-Runtime|-Implementation", "");
            } else {
                return false;
            }

            if (controlStrategies.containsKey(contingencyPlan)) {
            boolean activated = true;
            for (String cs : controlStrategies.get(contingencyPlan).getMandatoryCS() ) {
                if (! controlSets.get(cs).isProposed()){
                    activated = false;
                    break;
                }
            }
            return activated;
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * get threat CSGs
     *
     * @param threatUri
     * @return
     */
    public Set<String> getThreatControlStrategyUris(String threatUri, boolean future)   throws RuntimeException {
        // Return list of control strategies (urirefs) that block a threat
        // (uriref)

        /* "blocks": means a CSG appropriate for current or future risk calc
         * "mitigates": means a CSG appropriate for furture risk (often a
         * contingency plan for a current risk CSG); excluded from likelihood
         * calc in current risk
         */

        Set<String> csgURIs = new HashSet<String>();
        Set<String> csgToConsider = new HashSet<>();
        ThreatDB threat = this.threats.get(threatUri);
        try {
            csgURIs.addAll(threat.getBlockedByCSG());
            if (future) {
                csgURIs.addAll(threat.getMitigatedByCSG());
            }
            for(String csgURI : csgURIs) {
                ControlStrategyDB csg = querier.getControlStrategy(csgURI, "system-inf");
                if (csg.isCurrentRisk()) {
                   csgToConsider.add(csgURI);
               }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return csgToConsider;
    }

    /**
     * get CSG control sets  uris
     *
     * @param csgUri
     * @return
     */
    public List<String> getCsgControlSetsUris(String csgUri)   throws RuntimeException {
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
    public List<ControlSetDB> getCsgControlSets(String csgUri)   throws RuntimeException {
        try {
            List<ControlSetDB> csList = new ArrayList<ControlSetDB>();
            for (String csUri : controlStrategies.get(csgUri).getMandatoryCS()) {
                csList.add(controlSets.get(csgUri));
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
    public List<String> getCsgInactiveControlSets(String csgUri)   throws RuntimeException {

        try {
            List<String> csList = new ArrayList<>();
            for (String csUri : this.controlStrategies.get(csgUri).getMandatoryCS()) {
                //TODO needs revisiting, CS object should be accessed directly
                for (ControlSetDB cs : controlSets.values()){
                    if(cs.getUri().equals(csUri) && (!cs.isProposed())) {
                        csList.add(csUri);
                    }
                }
            }
            return csList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * get threat inactive CSGs
     *
     * @param threatUri
     * @return
     */
    public List<String> getThreatInactiveCSGs(String threatUri, boolean future)  throws RuntimeException {
        try {
            List<String> csgUriList = new ArrayList<String>();
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

    //TODO filtering LevelValue should be a parameter
    public List<String> filterMisbehaviours()  throws RuntimeException {
        /* compare MS by risk then likelihood, and return MS with
         * likelihood or risk >= MEDIUM
         */

        List<String> msUris = new ArrayList<>();

        try {
            logger.debug("filtering misbehaviour sets...");

            List<MisbehaviourSetDB> msSorted = new ArrayList<>(misbehaviourSets.values());

            Comparator<MisbehaviourSetDB> comparator = Comparator.
                comparing(MisbehaviourSetDB::getRisk)
                .thenComparing(MisbehaviourSetDB::getPrior);

            msSorted.sort(comparator);

            List<MisbehaviourSetDB> msFiltered = msSorted.stream()
                .filter(ms -> riLevels.get(ms.getRisk()).getLevelValue() >= 3)
                .collect(Collectors.toList());

            for (MisbehaviourSetDB ms : msFiltered){
                AssetDB asset = assets.get(ms.getLocatedAt());
                logger.debug("filtered MS:   {} \t-> risk {} prior {} at {}",
                        ms.getUri().substring(7), ms.getRisk().substring(7),
                        ms.getPrior().substring(7), asset.getLabel());
                msUris.add(ms.getUri());
            }

            logger.debug("filtered MS sets size: {}/{}", msUris.size(), misbehaviourSets.size());


        } catch (Exception e) {
            logger.error("got an error filtering misbehaviours: {}", e.getMessage());
            throw new RuntimeException("got an error filtering misbehavours", e);
        }

        return msUris;
    }

    public boolean isExternalCause(String uri){
        boolean retVal = false;
        // TODO: no need to check MS for external causes any more?
        if (misbehaviourSets.containsKey(uri)) {
            MisbehaviourSetDB ms = querier.getMisbehaviourSet(uri, "system-inf");
            if (ms != null) {
                retVal = ms.isExternalCause();
            }
        } else if (trustworthinessAttributeSets.containsKey(uri)){
            TrustworthinessAttributeSetDB twa = trustworthinessAttributeSets.get(uri);
            if(twa != null) {
                retVal = twa.isExternalCause();
            }
        }

        return retVal;
    }


    /**
     * check URI is a normal operation
     *
     * @param uri string of a threat or misbehaviour
     * @rerutn boolean
     */
    public boolean isNormalOp(String uri){
        boolean retVal = false;

        // check if we have to deal with a threat URI
        if (this.threats.containsKey(uri)) {
            ThreatDB threat = this.querier.getThreat(uri, "system-inf");
            if (threat != null) {
                retVal = threat.isNormalOperation();
            }
        } else if (misbehaviourSets.containsKey(uri)) {
            MisbehaviourSetDB ms = querier.getMisbehaviourSet(uri, "system-inf");
            if (ms != null) {
                retVal = ms.isNormalOpEffect();
            }
        } else if (trustworthinessAttributeSets.containsKey(uri)) {
            retVal = false;
        } else {
            logger.warn("Not sure what is this: {}", uri);
        }

        return retVal;
    }

    // describes if the URI refers to an initial cause misbehaviour
    public boolean isInitialCause(String uri){
        if (this.threats.keySet().contains(uri)){
            return threats.get(uri).isInitialCause();
        } else {
            return false;
        }
    }

    public boolean isThreatSimple(String uri){
        if (this.threats.keySet().contains(uri)){
            return true;
        } else {
            return false;
        }
    }

    public boolean isMisbehaviourSet(String uri){
        if (this.misbehaviourSets.keySet().contains(uri)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isTrustworthinessAttributeSets(String uri) {
        if (this.trustworthinessAttributeSets.keySet().contains(uri)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isThreat(String uri){
        if (this.threats.keySet().contains(uri)){
            return true;
        } else {
            return false;
        }
    }

    public ThreatDB getThreat(String uri) {
        return threats.get(uri);
    }

    public void printThreatUris() {
        for(String uri : this.threats.keySet()){
            logger.debug("Threat: {}", uri);
        }
    }

    public boolean isSecondaryThreat(String uri){
        if(threats.keySet().contains(uri) && 
                (threats.get(uri).getSecondaryEffectConditions().size() > 0)) {
                return true;
        }
        return false;
    }

    public boolean isRootCause(String uri) {
        if(threats.keySet().contains(uri)){
            ThreatDB threat = this.querier.getThreat(uri, "system-inf");
            if (threat != null){
                return threat.isRootCause();
            }
        }
        return false;
    }

    public String getLikelihood(String uri){
        if(this.likelihoods.keySet().contains(uri)){
            return this.likelihoods.get(uri);
        }
        return "";
    }

    // check MS list exists, no point going futher
    public boolean checkMisbehaviourList(List<String> misbehaviours){
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


    /**
     * capitilise string
     *
     * @param str
     * @return
     */
    private String capitaliseString(String str) {
            return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

}

