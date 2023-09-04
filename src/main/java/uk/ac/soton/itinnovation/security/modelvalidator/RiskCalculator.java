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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.core.joran.conditional.ElseAction;
import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;
import uk.ac.soton.itinnovation.security.modelquerier.IQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.AssetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.CASettingDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.CardinalityConstraintDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ControlDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ControlSetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ControlStrategyDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.LevelDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.MADefaultSettingDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.MatchingPatternDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.MisbehaviourDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.MisbehaviourInhibitionSetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.MisbehaviourSetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ModelDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ModelFeatureDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.NodeDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.RiskCalcResultsDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.RootPatternDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.TWAADefaultSettingDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ThreatDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.TrustworthinessAttributeDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.TrustworthinessAttributeSetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.TrustworthinessImpactSetDB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RiskCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RiskCalculator.class);

    private IQuerierDB querier;

    private Map<String, ModelFeatureDB> domainFeatures = new HashMap<>();   // Map of domain model features

    private Map<String, LevelDB> poLevels = new HashMap<>();                // Map of domain model population levels indexed by URI
    private Map<String, LevelDB> liLevels = new HashMap<>();                // Map of domain model likelihood levels indexed by URI
    private Map<String, LevelDB> twLevels = new HashMap<>();                // Map of domain model trustworthiness levels indexed by URI
    private Map<String, LevelDB> imLevels = new HashMap<>();                // Map of domain model impact levels indexed by URI
    private Map<String, LevelDB> riLevels = new HashMap<>();                // Map of domain model risk levels indexed by URI

    private List<LevelDB> populationLevels = new ArrayList<>();             // List of population levels, ordered by level value (increasing population)
    private List<LevelDB> likelihoodLevels = new ArrayList<>();             // List of likelihood levels, ordered by level value (increasing likelihood)
    private List<LevelDB> trustworthinessLevels = new ArrayList<>();        // List of trustworthiness levels, ordered by level value (increasing TW)
    private List<LevelDB> impactLevels = new ArrayList<>();                 // List of impact levels, ordered by level value (increasing impact)
    private List<LevelDB> riskLevels = new ArrayList<>();                   // List of risk levels, ordered by level value (increasing risk)

    private Map<String, ThreatDB> dthreats = new HashMap<>();                       // Map of domain model threats, indexed by URI
    private Map<String, ControlStrategyDB> dcsgs = new HashMap<>();                 // Map of domain model CSGs, indexed by URI
    private Map<String, MisbehaviourDB> dmisbehaviours = new HashMap<>();           // Map of domain model misbehaviours, indexed by URI
    private Map<String, ControlDB> dcontrols = new HashMap<>();                     // Map of domain model controls, indexed by URI

    private Map<String, List<String>> dsuppressedBy = new HashMap<>();              // Map of domain model controls that suppress misbehaviours as threat causes, indexed by misbehaviour URI
    private Map<String, List<String>> dsuppressed = new HashMap<>();                // Map of domain model misbehaviours suppressed as a threat cause by controls, indexed by control URI
    private Map<String, List<String>> dtriggers = new HashMap<>();                  // Map of domain model misbehaviours suppressed by controls leading to side-effect triggering, indexed by control URI
    private Map<String, String> deffector = new HashMap<>();                        // Map of domain model misbehaviours that undermine trustworthiness attributes, indexed by TWA URI

    private Map<String, List<String>> uniqueRolesByThreat = new HashMap<>();        // Map of unique roles indexed by domain model threat URI
    private Map<String, List<String>> necessaryRolesByThreat = new HashMap<>();     // Map of necessary roles indexed by domain model threat URI
    private Map<String, List<String>> sufficientRolesByThreat = new HashMap<>();    // Map of sufficient roles indexed by domain model threat URI
    private Map<String, List<String>> optionalRolesByThreat = new HashMap<>();      // Map of optional roles indexed by domain model threat URI

    private ModelDB dmodel;                                                         // Basic details of the domain model
    private ModelDB model;                                                          // Basic details of the system model, including overall risk

    private Map<String, AssetDB> assets = new HashMap<>();                                              // Map of system model assets, indexed by asset URI
    private Map<String, ThreatDB> threats = new HashMap<>();                                            // Map of system model threats, indexed by threat URI
    private Map<String, MatchingPatternDB> matchingPatterns = new HashMap<>();                          // Map of system model matching patterns, indexed by matching pattern URI
    private Map<String, NodeDB> nodes = new HashMap<>();                                                // Map of system model nodes, indexed by node URI
    private Map<String, Map<String, List<String>>> assetsByThreatByRole = new HashMap<>();              // Map of system model asset URI, indexed by URI of the threat and role within the threat
    private Map<String, Map<String, List<String>>> rolesByThreatByAsset = new HashMap<>();              // Map of system model role  URI, indexed by URI of the threat and asset within the threat

    private Map<String, MisbehaviourSetDB> misbehaviourSets = new HashMap<>();                          // Map of system model misbehaviour sets (MS), indexed by URI
    private Map<String, TrustworthinessAttributeSetDB> trustworthinessAttributeSets = new HashMap<>();  // Map of system model trustworthiness attribute sets (TWAS), indexed by URI
    private Map<String, ControlSetDB> controlSets = new HashMap<>();                                    // Map of system model control sets (CS), indexed by URI
    private Map<String, ControlStrategyDB> controlStrategies = new HashMap<>();                         // Map of system model control strategies (CSG), indexed by URI

    private Map<String, List<ThreatDB>> threatsByEffect = new HashMap<>();                              // Map of system threats that affect each MS, indexed by the URI of the MS
    private Map<String, List<String>> causesByThreat = new HashMap<>();                                 // Map of system model MS URI associated with a threat cause, indexed by threat URI

    private Map<String, MisbehaviourSetDB> entryPointMisbehaviour = new HashMap<>();                    // Map of system model MS associated with a TWAS, indexed by the TWAS URI
    private Map<String, String> misbehaviourTWAS = new HashMap<>();                                     // Map of system model TWAS URI associated with an MS, indexed by the MS URI

    private Map<String, Set<String>> suppressedMisbehaviours = new HashMap<>();                         // Map of system model MS URIs suppressed by each CS, indexed by the CS URI
    private Map<String, Set<String>> triggeringMisbehaviours = new HashMap<>();                         // Map of system model MSs suppressed by each CS leading to side-effect triggering, indexed by the CS URI

    private Map<String, Set<String>> suppressingControls = new HashMap<>();                             // Map of system model CS URIs that suppress each MS, indexed by the MS URI
    private Map<String, Map<String, Set<Integer>>> suppressingControlsByThreat = new HashMap<>();       // Map of system model CS that suppress causes of each threat, plus the related CSGs for that CS

    Map<String, Map<String, Integer>> causeSuppressionByThreat = new HashMap<>();                       // Map of threat cause optional control suppression factors, indexed by threat URI and then by cause URI

    public RiskCalculator(IQuerierDB querier) {

        final long startTime = System.currentTimeMillis();

        // Save the querier reference for use in other methods
        this.querier = querier;
        
        /*
         * TODO: at this stage the constructor loads up everything that seems like it might be useful, but this may be
         *       inefficient. At some point we should remove anything that isn't needed.
         */
        // Load domain model basic data
        dmodel = querier.getModelInfo("domain");
        domainFeatures = querier.getModelFeatures("domain");

         // Load domain model poulation, impact, trustworthiness, risk and likelihood scales as maps keyed on their URI
        poLevels = querier.getPopulationLevels();
        imLevels = querier.getImpactLevels();
        liLevels = querier.getLikelihoodLevels();
        twLevels = querier.getTrustworthinessLevels();
        riLevels = querier.getRiskLevels();

        // Load domain model impact, trustworthiness, risk, and likelihood scales as lists sorted by their level value
        populationLevels.addAll(poLevels.values());
        populationLevels.sort(Comparator.comparingInt(LevelDB::getLevelValue));
        impactLevels.addAll(imLevels.values());
        impactLevels.sort(Comparator.comparingInt(LevelDB::getLevelValue));
        likelihoodLevels.addAll(liLevels.values());
        likelihoodLevels.sort(Comparator.comparingInt(LevelDB::getLevelValue));
        trustworthinessLevels.addAll(twLevels.values());
        trustworthinessLevels.sort(Comparator.comparingInt(LevelDB::getLevelValue));
        riskLevels.addAll(riLevels.values());
        riskLevels.sort(Comparator.comparingInt(LevelDB::getLevelValue));

        // Load domain model threats and control strategies
        dthreats = querier.getThreats("domain");
        dcsgs = querier.getControlStrategies("domain");

        // Load system model basic data
        model = querier.getModelInfo("system");

        // Load system model assets, matching patterns and nodes
        assets = querier.getAssets("system", "system-inf");
        logger.info("Found {} system assets", assets.size());
        matchingPatterns = querier.getMatchingPatterns("system-inf");
        nodes = querier.getNodes("system-inf");

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
        logger.info("RiskCalculator.RiskCalculator(IQuerierDB querier): execution time {} ms", endTime - startTime);

    }

    /* Create maps required by the risk calculation to find TWAS, MS and their relationship to roles
    */
    private void createMaps(){

        final long startTime = System.currentTimeMillis();

        // Create domain model maps

        // Load domain model control types and construct their triplets
        dcontrols = querier.getControls("domain");
        Map<String, String> dcavg = new HashMap<>();
        Map<String, String> dcmin = new HashMap<>();
        Map<String, String> dcmax = new HashMap<>();
        for (ControlDB s : dcontrols.values()){
            if((s.getMinOf() != null) && (s.getMaxOf() == null)){
                // This is the lowest likelihood misbehaviour 
                dcmin.put(s.getMinOf(), s.getUri());
            } else if((s.getMinOf() == null) && (s.getMaxOf() != null)){
                // This is the highest likelihood misbehaviour 
                dcmax.put(s.getMaxOf(), s.getUri());
            } else {
                // This must be the average likelihood misbehaviour
                dcavg.put(s.getUri(), s.getUri());
                if((s.getMinOf() != null) && (s.getMaxOf() != null)){
                    // If it is both highest and lowest, treat it as average but log an error
                    logger.warn("Control {} is both a highest and lowest coverage control, which should be impossible", s.getUri());
                }    
            }
        }

        // Get the domain model misbehaviour types, and construct their triplets
        dmisbehaviours = querier.getMisbehaviours("domain");
        Map<String, String> dmavg = new HashMap<>();
        Map<String, String> dmmin = new HashMap<>();
        Map<String, String> dmmax = new HashMap<>();
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

        // Load domain model Control Sets, indexed by their own URI
        Map<String, ControlSetDB> dcss = querier.getControlSets("domain");

        // Load domain model patterns and nodes, indexed by URI
        Map<String, MatchingPatternDB> dmps = querier.getMatchingPatterns("domain");
        Map<String, RootPatternDB> drps = querier.getRootPatterns("domain");
        Map<String, NodeDB> dnodes = querier.getNodes("domain");
    
        // Domain model map used locally
        Map<String, Map<String, List<String>>> controlByThreatByRole = new HashMap<>();

        // Load domain model MIS and create maps of controls affecting threat causation linked with misbehaviours
        Map<String, MisbehaviourInhibitionSetDB> dmiss = querier.getMisbehaviourInhibitionSets("domain");
        for(MisbehaviourInhibitionSetDB dmis : dmiss.values()) {
            String misbehaviourURI = dmis.getInhibited();       // Only average misbehaviour types should be in a MIS
            String controlURI = dmis.getInhibitedBy();          // Only average control types should be in a MIS

            // Add the relationship between control and misbehaviour to the maps
            List<String> dsuppressedByThisControl = dsuppressed.computeIfAbsent(controlURI, k -> new ArrayList<>());
            dsuppressedByThisControl.add(misbehaviourURI);

            List<String> dsuppressesThisMisbehaviour = dsuppressedBy.computeIfAbsent(misbehaviourURI, k -> new ArrayList<>());
            dsuppressesThisMisbehaviour.add(controlURI);

            List<String> dtriggersThisControl = dtriggers.computeIfAbsent(controlURI, k -> new ArrayList<>());
            dtriggersThisControl.add(misbehaviourURI);

            // If this is part of a control triplet, so add the others to these maps
            if(dcmin.containsKey(controlURI) && dcmax.containsKey(controlURI)) {
                String cminURI =  dcmin.get(controlURI);
                String cmaxURI =  dcmax.get(controlURI);
                String mminURI =  dmmin.get(misbehaviourURI);
                String mmaxURI =  dmmax.get(misbehaviourURI);

                // Add the relationship from control to misbehaviour to the maps
                dsuppressedByThisControl = dsuppressed.computeIfAbsent(cminURI, k -> new ArrayList<>());
                dsuppressedByThisControl.add(mmaxURI);

                dsuppressedByThisControl = dsuppressed.computeIfAbsent(cmaxURI, k -> new ArrayList<>());
                dsuppressedByThisControl.add(mminURI);

                dsuppressesThisMisbehaviour = dsuppressedBy.computeIfAbsent(mminURI, k -> new ArrayList<>());
                dsuppressesThisMisbehaviour.add(cmaxURI);

                dsuppressesThisMisbehaviour = dsuppressedBy.computeIfAbsent(mmaxURI, k -> new ArrayList<>());
                dsuppressesThisMisbehaviour.add(cminURI);

                dtriggersThisControl = dtriggers.computeIfAbsent(cminURI, k -> new ArrayList<>());
                dtriggersThisControl.add(mminURI);

                dtriggersThisControl = dtriggers.computeIfAbsent(cmaxURI, k -> new ArrayList<>());
                dtriggersThisControl.add(mmaxURI);

            }

        }

        // Load domain model TWIS and create maps of misbehaviours affecting trustworthiness attributes
        Map<String, TrustworthinessImpactSetDB> dtwiss = querier.getTrustworthinessImpactSets("domain");
        for(TrustworthinessImpactSetDB dtwis : dtwiss.values()) {
            String misbehaviourURI = dtwis.getAffectedBy();
            String twaURI = dtwis.getAffects();
            deffector.put(twaURI, misbehaviourURI);
        }

        // Infill domain Threat -> CSG links (which will not be saved)
        for(ControlStrategyDB dcsg : dcsgs.values()) {
            for(String dthreatURI : dcsg.getTriggers()){
                ThreatDB dthreat = dthreats.get(dthreatURI);
                dthreat.getTriggeredByCSG().add(dcsg.getUri());
            }    
            for(String dthreatURI : dcsg.getBlocks()){
                ThreatDB dthreat = dthreats.get(dthreatURI);
                dthreat.getBlockedByCSG().add(dcsg.getUri());
            }    
            for(String dthreatURI : dcsg.getMitigates()){
                ThreatDB dthreat = dthreats.get(dthreatURI);
                dthreat.getMitigatedByCSG().add(dcsg.getUri());
            }    
        }

        // Create maps per domain model threat
        for(ThreatDB dthreat : dthreats.values()) {
            // Create maps of roles of each type
            List<String> uniqueRoles = uniqueRolesByThreat.computeIfAbsent(dthreat.getUri(), k -> new ArrayList<>());
            List<String> necessaryRoles = necessaryRolesByThreat.computeIfAbsent(dthreat.getUri(), k -> new ArrayList<>());
            List<String> sufficientRoles = sufficientRolesByThreat.computeIfAbsent(dthreat.getUri(), k -> new ArrayList<>());
            List<String> optionalRoles = optionalRolesByThreat.computeIfAbsent(dthreat.getUri(), k -> new ArrayList<>());
            String dmpUri = dthreat.getAppliesTo();
            MatchingPatternDB dmp = dmps.get(dmpUri);
            String drpUri = dmp.getRootPattern();
            RootPatternDB drp = drps.get(drpUri);
            for(String dn : drp.getKeyNodes()) {
                NodeDB dnode = dnodes.get(dn);
                uniqueRoles.add(dnode.getRole());
            }
            for(String dn : drp.getRootNodes()) {
                NodeDB dnode = dnodes.get(dn);
                uniqueRoles.add(dnode.getRole());
            }
            for(String dn : dmp.getNecessaryNodes()) {
                NodeDB dnode = dnodes.get(dn);
                necessaryRoles.add(dnode.getRole());
            }
            for(String dn : dmp.getSufficientNodes()) {
                NodeDB dnode = dnodes.get(dn);
                sufficientRoles.add(dnode.getRole());
            }
            for(String dn : dmp.getOptionalNodes()) {
                NodeDB dnode = dnodes.get(dn);
                optionalRoles.add(dnode.getRole());
            }

            // Create a map of optional Controls by role associated with control strategies addressing this threat
            Map<String, List<String>> controlByRole = controlByThreatByRole.computeIfAbsent(dthreat.getUri(), k -> new HashMap<>());
            for(String dcsgURI : dthreat.getBlockedByCSG()) {
                ControlStrategyDB dcsg = dcsgs.get(dcsgURI);
                for(String dcsURI : dcsg.getOptionalCS()) {
                    ControlSetDB dcs = dcss.get(dcsURI);
                    String roleURI = dcs.getLocatedAt();
                    String controlUri = dcs.getControl();
                    List<String> controlThisRole = controlByRole.computeIfAbsent(roleURI, k -> new ArrayList<>());
                    controlThisRole.add(controlUri);        
                }
            }

        }

        // Populate system model maps

        // Create maps between system model TWAS and the associated MS (using domain model TWIS information)
        for(TrustworthinessAttributeSetDB twas : trustworthinessAttributeSets.values()){
            AssetDB asset = assets.get(twas.getLocatedAt());
            String twaURI = twas.getTrustworthinessAttribute();
            String misbehaviourURI = deffector.get(twaURI);
            String msURI = querier.generateMisbehaviourSetUri(misbehaviourURI, asset);
            entryPointMisbehaviour.put(twas.getUri(), misbehaviourSets.get(msURI));
            misbehaviourTWAS.put(msURI, twas.getUri());
        }

        // Create maps from system model CS to inhibited and triggering MS (using data extracted from the domain model MIS)
        for(ControlSetDB cs : controlSets.values()){
            String controlURI = cs.getControl();
            AssetDB asset = assets.get(cs.getLocatedAt());

            Set<String> suppressedByCS = suppressedMisbehaviours.computeIfAbsent(cs.getUri(), k -> new HashSet<>());
            for(String misbehaviourURI : dsuppressed.getOrDefault(controlURI, new ArrayList<>())){
                String msURI = querier.generateMisbehaviourSetUri(misbehaviourURI, asset);
                suppressedByCS.add(msURI);
            }

            Set<String> triggersCS = triggeringMisbehaviours.computeIfAbsent(cs.getUri(), k -> new HashSet<>());
            for(String misbehaviourURI : dtriggers.getOrDefault(controlURI, new ArrayList<>())){
                String msURI = querier.generateMisbehaviourSetUri(misbehaviourURI, asset);
                triggersCS.add(msURI);
            }

        }

        // Create a map of threats that could directly cause each misbehaviour set
        for (ThreatDB threat : threats.values()) {
            for (String msURI : threat.getMisbehaviours()) {
                List<ThreatDB> threatsCausingThisMS = threatsByEffect.computeIfAbsent(msURI, k -> new ArrayList<>());
                threatsCausingThisMS.add(threat);
            }
        }

        // Create a map of MS that directly (or via an associated TWAS) cause each threat
        for (ThreatDB threat : threats.values()) {
            List<String> causesThisThreat = new ArrayList<>();
            for(String twasURI : threat.getEntryPoints()){
                if(twasURI == null){
                    logger.info("Found null entry point for threat = {}", threat.getUri());
                }
                String msURI;
                MisbehaviourSetDB ms = entryPointMisbehaviour.get(twasURI);
                if(ms != null){
                    msURI = entryPointMisbehaviour.get(twasURI).getUri();
                    if(!causesThisThreat.contains(msURI))
                        causesThisThreat.add(msURI);
                } else {
                    logger.info("Found null MS in entryPointMisbehaviour for TWAS = {}, threat {}", twasURI, threat.getUri());
                    throw new RuntimeException(String.format("Found null MS in entryPointMisbehaviour for TWAS = {}", twasURI));
                }
            }
            for(String msURI : threat.getSecondaryEffectConditions()){
                if(!causesThisThreat.contains(msURI))
                    causesThisThreat.add(msURI);
            }
            causesByThreat.put(threat.getUri(), causesThisThreat);
        }

        // Create a map of involved assets by threat and role
        for(ThreatDB threat : threats.values()) {
            // Get the map for this threat
            Map<String, List<String>> assetsByRole = assetsByThreatByRole.computeIfAbsent(threat.getUri(), k -> new HashMap<>());
            Map<String, List<String>> rolesByAsset = rolesByThreatByAsset.computeIfAbsent(threat.getUri(), k -> new HashMap<>());

            // Get the matching pattern for this threat
            MatchingPatternDB mp = matchingPatterns.get(threat.getAppliesTo());

            // Get the nodes and add the assets to the lists
            for (String nodeUri : mp.getNodes()) {
                NodeDB node = nodes.get(nodeUri);
                String assetURI = node.getSystemAsset();
                String roleURI = node.getRole();
                List<String> assetsThisRole = assetsByRole.computeIfAbsent(roleURI, k -> new ArrayList<>());
                List<String> rolesThisAsset = rolesByAsset.computeIfAbsent(assetURI, k -> new ArrayList<>());
                assetsThisRole.add(assetURI);
                rolesThisAsset.add(roleURI);
            }

        }

        final long endTime = System.currentTimeMillis();
        logger.info("RiskCalculator.CreateMaps(): execution time {} ms", endTime - startTime);

    }

    /**
     * Calculate risk levels on a validated system graph.
     * 
     * @param mode the risk calculation mode to use
     * @param saveResults indicates whether results should be stored
     * @param progress
     * @return
     */
    public boolean calculateRiskLevels(RiskCalculationMode mode, boolean saveResults, Progress progress) throws RuntimeException {
        try {
            // TODO: Fix!
            /**
             * Using the current querier implementation (JenaQuerierDB), if this line isn't here, the RC doesn't work on
             * re-run (e.g. testCurrentOrFutureRiskCalculation() fails).
             * This likely has something to do with the EntityCache, but needs further investigation.
             */
            //querier.getLinks("system", "system-inf");
            /**
             * Changes in Oct-21 mean we no longer store any links, so it is probable this line no longer has the same
             * effect on the re-run test. Unclear what is going on here...but testCurrentOrFutureRiskCalculation() did
             * indeed stop working, even though running the test sequence manually yielded correct outputs.
             * Given that, development of the risk calculator was not put on hold, but SCP started an investigation on
             * why the unit test fails, which will run in parallel with the risk calculator developments. 
             */

            // First, store the risk calc mode (FUTURE/CURRENT), to ensure this is returned in the results
            model.setRiskCalculationMode(mode.toString());

            // TODO: Remove prints
            progress.updateProgress(0.1, "Creating maps");
            createMaps();
            progress.updateProgress(0.2, "Initialising risk levels");
            initialiseLevels(mode);
            progress.updateProgress(0.3, "Calculating misbehaviour threat likelihoods");
            calculateThreatLikelihood(mode);
            progress.updateProgress(0.4, "Calculating causation relationships");
            calculateCausationLinks();
            progress.updateProgress(0.5, "Calculating misbehaviour risk levels");
            calculateMisbehaviourRiskLevels();
            progress.updateProgress(0.6, "Calculating threat risk levels");
            calculateThreatRiskLevels();
            progress.updateProgress(0.7, "Calculating normal operational effects");
            calculateNormalOperationPaths();
            progress.updateProgress(0.8, "Calculating attack paths and effects");
            calculateAttackPaths();

            // Restore default levels to the inferred graph
            restoreDefaultLevels();

            // Save to the triple store if the user/client asked for that to be done
            if (saveResults) {
                progress.updateProgress(0.9, "Saving risk calculation results");
                logger.info("Saving risk calculation results");
                querier.sync("system-inf");
            }
            else {
                logger.info("NOT saving risk calculation results");
            }

            return true;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gathers up the results from the risk calculation into a single object that can be serialised and sent 
     * back to clients via the system modeller API.
     */
    public RiskCalcResultsDB getRiskCalcResults() {
        RiskCalcResultsDB results = new RiskCalcResultsDB();
        results.setModel(model);
        results.setThreats(threats);
        results.setMisbehaviourSets(misbehaviourSets);
        results.setTwas(trustworthinessAttributeSets);
        results.setCs(controlSets);

        Map<String, Map<String, LevelDB>> levels = new HashMap<>();
        levels.put("liLevels", liLevels);
        levels.put("twLevels", twLevels);
        levels.put("imLevels", imLevels);
        levels.put("riLevels", riLevels);
        results.setLevels(levels);

        results.setMisbehaviours(dmisbehaviours);
        results.setControls(dcontrols);

        return results;
    }

    /**
     * Initialise inferred values, mainly by using default values from the domain model but preserving any asserted
     * values from the user if they exist.
     * Note that previously calculated inferred levels should have been flushed by the constructor method.
     */
    private void initialiseLevels(RiskCalculationMode mode) {
        // TODO: IMPORTANT Only store entities if necessary

        final long startTime = System.currentTimeMillis();

        /* Reinitialise asserted TWL per TWAS in the inferred graph, taking account of any changes in the asserted graph.
         * No need to enforce consistency in user/client supplied levels because the plan is to make
         * the API do that, and meanwhile the user/client is responsible. However, we probably should
         * log a warning message if there is inconsistency.
         * In that case, the calculated TWL per TWAS can be adjusted accordingly.
        */
        for (TrustworthinessAttributeSetDB twasavg : trustworthinessAttributeSets.values()){
            // Make sure we only start from an average level TWAS
            if((twasavg.getMinOf() == null) && (twasavg.getMaxOf() == null)) {
                // Get the associated asset info
                AssetDB asset = assets.get(twasavg.getLocatedAt());
                LevelDB popLevel = poLevels.get(asset.getPopulation());
                
                // Get the associated default settings
                Boolean independentLevels = true;
                Integer defaultLevel = twLevels.size() - 1;
                TWAADefaultSettingDB setting = querier.getTWAADefaultSetting(asset, twasavg.getTrustworthinessAttribute());
                if(setting != null) {
                    if(setting != null) {
                        // Old domain models have no levels, so check for that
                        if(setting.getLevel() != null) {
                            defaultLevel = twLevels.get(setting.getLevel()).getLevelValue();
                            independentLevels = setting.getIndependentLevels();
                        } else {
                            independentLevels = false;
                        }
                    }
                }
    
                // Get data from the asserted graph for this TWAS
                TrustworthinessAttributeSetDB twasavgInput = querier.getTrustworthinessAttributeSet(twasavg.getUri(), "system");

                if(twasavg.getHasMin() != null && twasavg.getHasMax() != null){     // In a population triplet both min and max exist
                    // Get the lowest TW member of the triplet, plus any properties from the asserted graph 
                    TrustworthinessAttributeSetDB twasmin = trustworthinessAttributeSets.get(twasavg.getHasMin());
                    TrustworthinessAttributeSetDB twasminInput = querier.getTrustworthinessAttributeSet(twasmin.getUri(), "system");

                    // Get the highest TW member of the triplet, plus any properties from the asserted graph 
                    TrustworthinessAttributeSetDB twasmax = trustworthinessAttributeSets.get(twasavg.getHasMax());
                    TrustworthinessAttributeSetDB twasmaxInput = querier.getTrustworthinessAttributeSet(twasmax.getUri(), "system");

                    // Assemble asserted graph levels and proposed status into triplets of values
                    Integer[] assertedValues = {null, null, null};
                    if (twasminInput != null && twasminInput.getAssertedLevel() != null){
                        assertedValues[0] = twLevels.get(twasminInput.getAssertedLevel()).getLevelValue();
                    }
                    if (twasavgInput != null && twasavgInput.getAssertedLevel() != null) {
                        assertedValues[1] = twLevels.get(twasavgInput.getAssertedLevel()).getLevelValue();
                    }
                    if (twasmaxInput != null && twasmaxInput.getAssertedLevel() != null) {
                        assertedValues[2] = twLevels.get(twasmaxInput.getAssertedLevel()).getLevelValue();
                    }

                    // Find a consistent set of levels obtained by adjusting the asserted graph levels
                    Integer[] adjustedValues = querier.getAdjustedLevels(asset, defaultLevel, assertedValues);

                    // Log any inconsistencies
                    if( (assertedValues[0] != null && assertedValues[0] != adjustedValues[0]) ||
                        (assertedValues[1] != null && assertedValues[1] != adjustedValues[1]) ||
                        (assertedValues[2] != null && assertedValues[2] != adjustedValues[2]) ) {
                        logger.warn("Client/User input TW levels for TW attribute {} at asset {} needed adjustment",
                                    twasavg.getTrustworthinessAttribute(), twasavg.getLocatedAt());
                        logger.warn("...asserted graph levels (min, avg, max) were ({},{},{}), adjusted to ({},{},{})",
                                    assertedValues[0], assertedValues[1], assertedValues[2],
                                    adjustedValues[0], adjustedValues[1], adjustedValues[2]);
                    }

                    // If there is no lowest or highest level assumed TW, calculate it from the average
                    LevelDB averageLevel = trustworthinessLevels.get(adjustedValues[1]); 
                    if(adjustedValues[0] == null)
                        adjustedValues[0] = querier.lookupLowestTWLevel(averageLevel, popLevel, independentLevels).getLevelValue();
                    if(adjustedValues[2] == null)
                        adjustedValues[2] = querier.lookupHighestTWLevel(averageLevel, popLevel, independentLevels).getLevelValue();

                    // Now set the inferred graph asserted levels based on this
                    twasmin.setAssertedLevel(trustworthinessLevels.get(adjustedValues[0]).getUri());
                    twasavg.setAssertedLevel(averageLevel.getUri());
                    twasmax.setAssertedLevel(trustworthinessLevels.get(adjustedValues[2]).getUri());

                    // Now set the inferred levels based on that
                    twasmin.setInferredLevel(twasmin.getAssertedLevel());
                    twasavg.setInferredLevel(twasavg.getAssertedLevel());
                    twasmax.setInferredLevel(twasmax.getAssertedLevel());

                    // Store the reinitialised TWAS in the inferred graph
                    querier.store(twasmin,"system-inf");
                    querier.store(twasavg,"system-inf");
                    querier.store(twasmax,"system-inf");

                } else {                                                            // With an old domain model neither min nor max exists
                    // This is an old domain model, just initialise from the asserted graph if required
                    if(twasavgInput != null && twasavgInput.getAssertedLevel() != null)
                        twasavg.setAssertedLevel(twasavgInput.getAssertedLevel());
                    twasavg.setInferredLevel(twasavg.getAssertedLevel());

                    // Store the reinitialised TWAS in the inferred graph
                    querier.store(twasavg,"system-inf");
                }

            }

        }

        /* Reinitialise coverage level per CS in the inferred graph, taking account of any changes in the asserted graph.
         * No need to enforce consistency in user/client supplied levels because the plan is to make
         * the API do that, and meanwhile the user/client is responsible. However, we probably should
         * log a warning message if there is inconsistency.
        */
        for (ControlSetDB csavg : controlSets.values()){
            // Make sure we only start from an average level CS
            if((csavg.getMinOf() == null) && (csavg.getMaxOf() == null)) {
                // Get the associated asset info
                AssetDB asset = assets.get(csavg.getLocatedAt());
                LevelDB popLevel = poLevels.get(asset.getPopulation());
                
                // Get the associated default settings
                Boolean independentLevels = true;
                Integer defaultLevel = twLevels.size() - 1;
                CASettingDB setting = querier.getCASetting(asset, csavg.getControl());
                if(setting != null) {
                    // Old domain models have no levels, so check for that
                    if(setting.getLevel() != null) {
                        defaultLevel = twLevels.get(setting.getLevel()).getLevelValue();
                        independentLevels = setting.getIndependentLevels();
                    } else {
                        independentLevels = false;
                    }
                }

                // Get data from the asserted graph for this CS
                ControlSetDB csavgInput = querier.getControlSet(csavg.getUri(), "system");

                if(csavg.getHasMin() != null && csavg.getHasMax() != null){         // In a population triplet both min and max exist
                    // Get the lowest coverage member of the triplet, plus any properties from the asserted graph 
                    ControlSetDB csmin = controlSets.get(csavg.getHasMin());
                    ControlSetDB csminInput = querier.getControlSet(csmin.getUri(), "system");

                    // This is a population, find the other members of the CS triplet
                    ControlSetDB csmax = controlSets.get(csavg.getHasMax());
                    ControlSetDB csmaxInput = querier.getControlSet(csmax.getUri(), "system");

                    // Assemble a triplet of asserted graph proposed status values
                    Boolean[] proposedValues = {false, false, false};
                    if(csminInput != null)
                        proposedValues[0] = csminInput.isProposed();
                    if(csavgInput != null)
                        proposedValues[1] = csavgInput.isProposed();
                    if(csmaxInput != null)
                        proposedValues[2] = csmaxInput.isProposed();

                    // Log any inconsistencies in control status
                    if( (proposedValues[0] && proposedValues[1] && proposedValues[2]) !=
                        (proposedValues[0] || proposedValues[1] || proposedValues[2])) {
                            logger.warn("Client/User input status for control {} at asset {} was inconsistent",
                                    csavg.getControl(), csavg.getLocatedAt());
                            logger.warn("...asserted graph levels (min, avg, max) were ({},{},{}), used {}",
                                    proposedValues[0], proposedValues[1], proposedValues[2],
                                    proposedValues[0] || proposedValues[1] || proposedValues[2]);
                    }

                    // Now set the inferred graph control status
                    csmin.setProposed(proposedValues[0] || proposedValues[1] || proposedValues[2]);
                    csavg.setProposed(proposedValues[0] || proposedValues[1] || proposedValues[2]);
                    csmax.setProposed(proposedValues[0] || proposedValues[1] || proposedValues[2]);

                    // Assemble a triplet of asserted graph coverage levels
                    Integer[] assertedValues = {null, null, null};
                    if(csminInput != null && csminInput.getCoverageLevel() != null)
                        assertedValues[0] = twLevels.get(csminInput.getCoverageLevel()).getLevelValue();
                    if(csavgInput != null && csavgInput.getCoverageLevel() != null)
                        assertedValues[1] = twLevels.get(csavgInput.getCoverageLevel()).getLevelValue();
                    if(csmaxInput != null && csmaxInput.getCoverageLevel() != null)
                        assertedValues[2] = twLevels.get(csmaxInput.getCoverageLevel()).getLevelValue();

                    // Find a consistent set of levels obtained by adjusting the asserted graph levels
                    Integer[] adjustedValues = querier.getAdjustedLevels(asset, defaultLevel, assertedValues);

                    // Log any inconsistencies in coverage levels
                    if( (assertedValues[0] != null && assertedValues[0] != adjustedValues[0]) ||
                        (assertedValues[1] != null && assertedValues[1] != adjustedValues[1]) ||
                        (assertedValues[2] != null && assertedValues[2] != adjustedValues[2]) ) {
                        logger.warn("Client/User input coverage levels for control {} at asset {} needed adjustment",
                                    csavg.getControl(), csavg.getLocatedAt());
                        logger.warn("...asserted graph levels (min, avg, max) were ({},{},{}), adjusted to ({},{},{})",
                                    assertedValues[0], assertedValues[1], assertedValues[2],
                                    adjustedValues[0], adjustedValues[1], adjustedValues[2]);
                    }

                    // If there is no lowest or highest level, calculate them from the average
                    LevelDB averageLevel = trustworthinessLevels.get(adjustedValues[1]); 
                    if(adjustedValues[0] == null)
                        adjustedValues[0] = querier.lookupLowestTWLevel(averageLevel, popLevel, independentLevels).getLevelValue();
                    if(adjustedValues[2] == null)
                        adjustedValues[2] = querier.lookupHighestTWLevel(averageLevel, popLevel, independentLevels).getLevelValue();

                    // Now set the inferred graph asserted levels based on this
                    csmin.setCoverageLevel(trustworthinessLevels.get(adjustedValues[0]).getUri());
                    csavg.setCoverageLevel(trustworthinessLevels.get(adjustedValues[1]).getUri());
                    csmax.setCoverageLevel(trustworthinessLevels.get(adjustedValues[2]).getUri());

                    // Store the reinitialised CS in the inferred graph
                    querier.store(csmin, "system-inf");
                    querier.store(csavg, "system-inf");
                    querier.store(csmax, "system-inf");

                } else {                                                            // With an old domain model neither min nor max exists
                    // This is an old domain model, just initialise from the asserted graph if required
                    if(csavgInput != null) {
                        csavg.setProposed(csavgInput.isProposed());                    
                        if(csavgInput.getCoverageLevel() != null)
                            csavg.setCoverageLevel(csavgInput.getCoverageLevel());
                    }

                    // Store the reinitialised CS in the inferred graph
                    querier.store(csavg, "system-inf");

                }

            }

        }

        // Determine status and coverage level of each CSG based on its mandatory CS
        for (ControlStrategyDB csg : controlStrategies.values()) {
            ControlStrategyDB dcsg = dcsgs.get(csg.getParent());
            Integer csgCoverage = twLevels.get(dcsg.getBlockingEffect()).getLevelValue();
            boolean enabled = true;
            switch(mode) {
                case CURRENT:
                    enabled = dcsg.isCurrentRisk();
                    break;
                case FUTURE:
                    enabled = dcsg.isFutureRisk();
                    break;
            }
            if(enabled) {
                Collection<String> css = csg.getMandatoryCS();
                for (String csURI : css) {
                    ControlSetDB cs = controlSets.get(csURI);
                    if(!cs.isProposed()) {
                        // CSG cannot be enabled, so skip the remaining mandatory CS
                        enabled = false;
                        break;
                    } else if(cs.getCoverageLevel() != null) {
                        Integer csCoverage = twLevels.get(cs.getCoverageLevel()).getLevelValue();
                        if(csgCoverage > csCoverage)
                            csgCoverage = csCoverage;
                    }
                }
                if(!enabled) csgCoverage = 0;
            }

            csg.setEnabled(enabled);
            csg.setCoverageLevel(trustworthinessLevels.get(csgCoverage).getUri());

            querier.store(csg, "system-inf");

        }

        // Find effect of optional control sets that suppress causation of each threat
        for(ThreatDB threat : threats.values()){
            // Start by setting each cause to have a minimum TW level set to the lowest level
            Map<String, Integer> causeSuppressionThisThreat = new HashMap<>();
            for(String epURI : threat.getEntryPoints()){
                causeSuppressionThisThreat.put(epURI, 0);
            }
            for(String secURI : threat.getSecondaryEffectConditions()){
                causeSuppressionThisThreat.put(secURI, 0);
            }
  
            // Find enabled CSGs with optional controls related to each cause
            Set<String> allCSGs = new HashSet<>();
            allCSGs.addAll(threat.getBlockedByCSG());
            if(mode == RiskCalculationMode.FUTURE) allCSGs.addAll(threat.getMitigatedByCSG());
            for(String csgURI : allCSGs){
                ControlStrategyDB csg = controlStrategies.get(csgURI);
                if(csg.isEnabled()){
                    Integer csgCoverageLevel = twLevels.get(csg.getCoverageLevel()).getLevelValue();
                    for(String csURI : csg.getOptionalCS()){
                        // Get the optional CS and find its effective coverage level
                        ControlSetDB cs = controlSets.get(csURI);
                        Integer effectiveLevel = twLevels.get(cs.getCoverageLevel()).getLevelValue();
                        if(effectiveLevel > csgCoverageLevel)
                            effectiveLevel = csgCoverageLevel;

                        // Apply this to any threat causes suppressed by the optional CS
                        Set<String> suppressedByCS = suppressedMisbehaviours.getOrDefault(csURI, new HashSet<>());
                        for(String msURI : suppressedByCS) {
                            // Check if this MS is a secondary threat cause
                            if(causeSuppressionThisThreat.containsKey(msURI)){
                                // If yes, impose the CS level
                                Integer oldLevel = causeSuppressionThisThreat.get(msURI);
                                if(oldLevel < effectiveLevel)
                                    causeSuppressionThisThreat.replace(msURI, oldLevel, effectiveLevel);
                            }

                            // Check if the associated TWAS is a primary threat cause
                            String twasURI = misbehaviourTWAS.get(msURI);
                            if(twasURI != null && causeSuppressionThisThreat.containsKey(twasURI)){
                                // If yes, impose the CS level
                                Integer oldLevel = causeSuppressionThisThreat.get(twasURI);
                                if(oldLevel < effectiveLevel)
                                    causeSuppressionThisThreat.replace(twasURI, oldLevel, effectiveLevel);
                            }
                        }
                    }
                }
            }
            causeSuppressionByThreat.put(threat.getUri(), causeSuppressionThisThreat);
        }

        // Reset MS likelihoods to the lowest possible level, and empty their lists of caused threats
        String lowestLikelihood = likelihoodLevels.get(0).getUri();
        String lowestImpact = impactLevels.get(0).getUri();
        for (MisbehaviourSetDB msavg : misbehaviourSets.values()) {
            // Make sure we only start from an average level CS
            if((msavg.getMinOf() == null) && (msavg.getMaxOf() == null)) {
                // Get the associated asset and default setting
                AssetDB asset = assets.get(msavg.getLocatedAt());
                MADefaultSettingDB setting = querier.getMADefaultSetting(asset, msavg.getMisbehaviour());

                // Temporarily save the inferred impact level
                if(msavg.getImpactLevel() != null) {
                    // Save the previous inferred graph value if there is one
                    msavg.setDefaultLevel(msavg.getImpactLevel());
                }
                else if(setting != null) {
                    // Save the default setting if there is one
                    msavg.setDefaultLevel(setting.getLevel());
                }
                else {
                    // Save the lowest impact level as a last resort
                    msavg.setDefaultLevel(lowestImpact);
                }

                // Check if there is an impact level in the asserted graph, and if so, use it 
                MisbehaviourSetDB msavgInput = querier.getMisbehaviourSet(msavg.getUri(), "system");
                if(msavgInput != null && msavgInput.getImpactLevel() != null){
                    msavg.setImpactLevel(msavgInput.getImpactLevel());
                } else {
                    // If there is no level in the asserted graph, restore the saved level
                    msavg.setImpactLevel(msavg.getDefaultLevel());
                    msavg.setDefaultLevel(null);
                }

                // Initialise the live object and save to the inferred graph
                msavg.setPrior(lowestLikelihood);
                msavg.setExternalCause(null);
                msavg.getCausedThreats().clear();
                querier.store(msavg, "system-inf");
        
                // Check for other members of the same triplet
                if(msavg.getHasMin() != null && msavg.getHasMax() != null){         // In a population triplet both min and max exist
                    // Get the lowest likelihood member of the triplet
                    MisbehaviourSetDB msmin = misbehaviourSets.get(msavg.getHasMin());

                    // Temporarily save the inferred impact level
                    if(msmin.getImpactLevel() != null) {
                        // Save the previous inferred graph value if there is one
                        msmin.setDefaultLevel(msmin.getImpactLevel());
                    }
                    else {
                        // Save the lowest impact level otherwise
                        msmin.setDefaultLevel(lowestImpact);
                    }

                    // Check if there is an impact level in the asserted graph, and if so, use it 
                    MisbehaviourSetDB msminInput = querier.getMisbehaviourSet(msmin.getUri(), "system");

                    // Get the impact level from the asserted graph, or using a default value
                    if(msminInput != null && msminInput.getImpactLevel() != null) {
                        msmin.setImpactLevel(msminInput.getImpactLevel());
                    }
                    else {
                        // If there is no level in the asserted graph, restore the saved level
                        msmin.setImpactLevel(msmin.getDefaultLevel());
                        msmin.setDefaultLevel(null);
                    }

                    // Initialise the live object and save to the inferred graph
                    msmin.setPrior(lowestLikelihood);
                    msmin.setExternalCause(null);
                    msmin.getCausedThreats().clear();
                    querier.store(msmin, "system-inf");

                    // Get the highest likelihood member of the triplet
                    MisbehaviourSetDB msmax = misbehaviourSets.get(msavg.getHasMax());

                    // Temporarily save the inferred impact level
                    if(msmax.getImpactLevel() != null) {
                        // Save the previous inferred graph value if there is one
                        msmax.setDefaultLevel(msmax.getImpactLevel());
                    }
                    else {
                        // Save the lowest impact level otherwise
                        msmax.setDefaultLevel(lowestImpact);
                    }
                    
                    // Check if there is an impact level in the asserted graph, and if so, use it 
                    MisbehaviourSetDB msmaxInput = querier.getMisbehaviourSet(msmax.getUri(), "system");
                    if(msmaxInput != null && msmaxInput.getImpactLevel() != null) {
                        msmax.setImpactLevel(msmaxInput.getImpactLevel());
                    }
                    else {
                        // If there is no level in the asserted graph, restore the saved level
                        msmax.setImpactLevel(msmax.getDefaultLevel());
                        msmax.setDefaultLevel(null);
                    }

                    // Initialise the live object and save to the inferred graph
                    msmax.setPrior(lowestLikelihood);
                    msmax.setExternalCause(null);
                    msmax.getCausedThreats().clear();
                    querier.store(msmax, "system-inf");
                }

            }

        }

        /* If this is an old domain model, we may also need to raise the initial MS likelihood to a level
         * equivalent to the TW level of the associated TWAS (if any). This was the case before SSM could
         * handle secondary threats with primary causes (and vice versa).
         * 
         * We can find this out by checking if the domain model has a feature string "MixedThreatCauses".
         */
        Boolean mixedThreatCauses = domainFeatures.containsKey("domain#Feature-MixedThreatCauses");
        if(mixedThreatCauses) {
            logger.info("Domain model supports mixed threat causes, so MS likelihood initialised to zero");
        } else {
            logger.info("Domain model does not support mixed threat causes, so MS likelihood initialised from TWAS levels");
            for(TrustworthinessAttributeSetDB twas : trustworthinessAttributeSets.values()){
                String twasURI = twas.getUri();
                String twasLevelURI = twas.getInferredLevel();
                if (twasLevelURI == null) {
                    logger.warn("Found a TWAS with no inferred TWL");
                }
                MisbehaviourSetDB ms = entryPointMisbehaviour.get(twasURI);
                LevelDB twasLevel = twLevels.get(twasLevelURI);
                LevelDB msLevel = invertToLikelihood(twasLevel);
                String msLevelUri = msLevel.getUri();
                if(!msLevelUri.equals(lowestLikelihood)){
                    ms.setPrior(msLevelUri);
                    querier.store(ms, "system-inf"); 
                }    
            }
        }

        // Reset threat likelihood and risk level, and reset other properties to null / empty lists
        for (ThreatDB threat : threats.values()){
            threat.setPrior(null);
            threat.setRisk(null);
            threat.setRootCause(null);
            threat.setInitialCause(null);
            threat.getDirectMisbehaviours().clear();
            threat.getIndirectMisbehaviours().clear();
            threat.getIndirectThreats().clear();
            threat.getCausedBy().clear();
            querier.store(threat, "system-inf");
        }
        for(TrustworthinessAttributeSetDB twas : trustworthinessAttributeSets.values()){
            twas.getCausedThreats().clear();
        }
        for(MisbehaviourSetDB ms : misbehaviourSets.values()){
            ms.getCausedThreats().clear();
            ms.getCausedBy().clear();
        }

        final long endTime = System.currentTimeMillis();
        logger.info("RiskCalculator.initialiseLevels(): execution time {} ms", endTime - startTime);

    }

    /**
     * Restore default levels to the inferred graph, where they had to be overridden in initialiseLevels.
     */
    private void restoreDefaultLevels(){
        for(MisbehaviourSetDB ms : misbehaviourSets.values()){
            if(ms.getDefaultLevel() != null) {
                ms.setImpactLevel(ms.getDefaultLevel());
                ms.setDefaultLevel(null);
            }
        }
    }

    private void calculateThreatLikelihood(RiskCalculationMode mode) {
        final long startTime = System.currentTimeMillis();

        boolean finished = false;

        while (!finished) {
            // we assume this is the last iteration until a MS has to be changed
            calculateThreatLikelihoods(mode);
            finished = updateMSLikelihoods();
            updateTWASLevels();
        }

        final long endTime = System.currentTimeMillis();
        logger.info("RiskCalculator.calculateThreatLikelihood(): execution time {} ms", endTime - startTime);
        
    }

    /**
     * Set each triggered threat's likelihood set to either
     *    a) the inverse of the most trustworthy TWAS among its entry points, if it is a primary threat
     *    b) the least likely MS among its secondary effect conditions, if it is a secondary threat
     * up to a maximum equal to the inverse of the most trustworthy fully-asserted CSG.
     * If a threat has a frequency level, then it's likelihood level cannot exceed the frequency level.
     * If a threat is untriggered (i.e. has one or more triggering CSGs which are unasserted) then its
     * likelihood is set to the minimum level.
     *
     * @param mode the risk calculation mode to use
     */
    private void calculateThreatLikelihoods(RiskCalculationMode mode) {

        // Calculate the likelihood of each threat, given the likelihood of each cause and the status of CSGs.
        /* TODO: change this code to implement the algorithms needed to support populations. The main changes are:
         * 
         * - Threat causes should be computed per asset, then combined per node (depending on node type), and
         *   only then should contributions from each node be combined to get the threat causation likelihood.
         * - The effect of optional control sets in enabled CSGs should be inserted per asset during the threat
         *   causation calculation. Only CSGs with no optional control sets should work in the current manner.
         */
        for (ThreatDB threat : threats.values()) {
            // Get the threat URI and Label, not used but helps with debugging
            String threatURI = threat.getUri();
            String threatLabel = threat.getLabel();
            String dthreatURI = threat.getParent();
            if(dthreatURI == null) {
                // Threat type cannot be determined, which is an error
                String message = String.format("Threat %s has no domain model parent type", threatURI);
                throw new RuntimeException(message);
            }
            ThreatDB dthreat = dthreats.get(dthreatURI);
            if(dthreat == null){
                // Domain model parent threat cannot be found, which is an error
                String message = String.format("Threat %s has parent type %s, but this cannot be found", threatURI, dthreatURI);
                throw new RuntimeException(message);
            }

            // STAGE 0: Check whether we need to calculate a likelihood for this threat

            // If this is a compliance threat with no causes, we skip it
            if (threat.getEntryPoints().isEmpty() && threat.getSecondaryEffectConditions().isEmpty()){
                continue;
            }

            // If this threat should not be considered in this type of risk calculation, we skip it 
            boolean enabled = false;
            switch(mode) {
                case CURRENT:
                    enabled = dthreat.isCurrentRisk();
                    break;
                case FUTURE:
                    enabled = dthreat.isFutureRisk();
                    break;
            }
            if(!enabled){
                continue;
            }

            // If this is a triggered threat with no triggers, we log it (a possible error) and skip it
            if(threat.isTriggered() && threat.getTriggeredByCSG().isEmpty()){
                logger.warn("Threat {} is a triggered threat with no triggering CSGs", threat.getUri());
                continue;
            }

            // If this is a triggered threat with no enabled triggers (not an error), we skip it
            if(threat.isTriggered()){
                boolean untriggered = true;
                for(String triggerURI : threat.getTriggeredByCSG()) {
                    ControlStrategyDB trigger = controlStrategies.get(triggerURI);
                    if(trigger.isEnabled()) {
                        // The threat is triggered, so we can skip the remaining triggers
                        untriggered = false;
                        break;
                    }
                }
                if(untriggered){
                    continue;
                }
            }

            // The threat is either non-triggered (needs no trigger) or triggered, so find its likelihood

            // Start by grabbing some data concerning roles and assets and the effect of optional controls
            MatchingPatternDB matchingPattern = querier.getMatchingPattern(threat.getAppliesTo(), "system-inf");
            List<String> pseudorootAssets = querier.getPseudorootAssets(matchingPattern);
            List<String> sufficientRoles = sufficientRolesByThreat.get(dthreatURI);
            List<String> necessaryRoles = necessaryRolesByThreat.get(dthreatURI);
            Map<String, List<String>> assetsByRole = assetsByThreatByRole.get(threat.getUri());
            Map<String, List<String>> rolesByAsset = rolesByThreatByAsset.get(threat.getUri());
            Map<String, Integer> causeSuppressionThisThreat = causeSuppressionByThreat.get(threat.getUri());

            // STAGE 1: find the effective threat causation TW level for each asset
            Map<String, Integer> assetLevels = new HashMap<>();

            /* Set an initial TW level per asset based on the coverage levels of any optional CS that
             * is part of an enabled triggering CSG? See Issue #1340.
             * 
             * This is the first step, covered by Issue #1350, which uses suppressed threat cause
             * likelihood but not yet control coverage levels.
             * 
             * TODO : modify the 'effective level' so it also depends on control coverage level.
             */
            if(threat.isTriggered()) {
                for(String triggerURI : threat.getTriggeredByCSG()) {
                    ControlStrategyDB trigger = controlStrategies.get(triggerURI);
                    if(trigger.isEnabled()){
                        Integer csgCoverageLevel = twLevels.get(trigger.getCoverageLevel()).getLevelValue();
                        Map<String, Integer> csgLevels = new HashMap<>();
                        for(String csURI : trigger.getOptionalCS()){
                            // Initialise the effective triggering level
                            Integer effectiveLevel = twLevels.size() - 1;

                            // Get the triggering optional control set and its asset
                            ControlSetDB cs = controlSets.get(csURI);
                            String assetURI = cs.getLocatedAt();

                            /* Flip between min and max coverage CS at necessary/sufficient nodes. See
                             * 
                             * https://iglab.it-innovation.soton.ac.uk/Security/system-modeller/-/issues/1360#note_32462
                             */
                            // This is a max coverage control set, should flip if it is at a necessary node
                            if(cs.getMaxOf() != null) {
                                Boolean necessary = !pseudorootAssets.contains(assetURI);
                                if(necessary) for(String roleURI : rolesByAsset.get(assetURI)){
                                    necessary = necessary || necessaryRoles.contains(roleURI);
                                }
                                if(necessary) {
                                    cs = controlSets.get(controlSets.get(cs.getMaxOf()).getHasMin());
                                }
                            }
                            else if(cs.getMinOf() != null) {
                                // This is a min coverage control set, should flip if it is at a sufficient node
                                Boolean sufficient = !pseudorootAssets.contains(assetURI);
                                if(sufficient) for(String roleURI : rolesByAsset.get(assetURI)){
                                    sufficient = sufficient || sufficientRoles.contains(roleURI);
                                }
                                if(sufficient) {
                                    cs = controlSets.get(controlSets.get(cs.getMinOf()).getHasMax());
                                }
                            }

                            if(cs.isProposed()){
                                // If the CS is present and enabled, get its effective coverage level
                                Integer coverageLevel = twLevels.get(cs.getCoverageLevel()).getLevelValue();
                                if(coverageLevel > csgCoverageLevel) coverageLevel = csgCoverageLevel;

                                // Find the highest likelihood of any MS suppressed as a threat cause by this CS
                                Integer misbehaviourLevel = twLevels.size() - 1;
                                for (String msURI : triggeringMisbehaviours.getOrDefault(cs.getUri(), new HashSet<>())){
                                    MisbehaviourSetDB ms = misbehaviourSets.get(msURI);
                                    if(ms == null) {
                                        logger.warn("Optional control {} in CSG {} does not suppress any behaviour", cs.getControl(), triggerURI);
                                    }
                                    Integer level = invertToTrustworthiness(liLevels.get(ms.getPrior())).getLevelValue();
                                    if (misbehaviourLevel > level) misbehaviourLevel = level;

                                }

                                /* Find the side effect causation likelihood (as a TW level)
                                 * Here we're assuming that if misbehaviour M has likelihood L_m at asset A, and control C
                                 * has coverage level T_c at asset A, in which case:
                                 * - likelihood of control failing to cover A is L_c = N - 1 - T_c
                                 * - likelihood of threat causation at A due to M is L_t = min(L_m, L_c)
                                 * - likelihood of side effect causation at A due to C is L_s = L_m - L_t
                                 * The corresponding TW level T_s = N - 1 - L_s, which after rearrangement turns out to be
                                 * T_s = N - 1 + T_m - max(T_m, T_c), where N is the number of levels in the likelihood/TW scales
                                 */
                                if(coverageLevel > misbehaviourLevel){
                                    effectiveLevel = effectiveLevel + misbehaviourLevel - coverageLevel;
                                }
    
                            }
                            
                            // Side effect causation at this asset from this CSG is from the asset's least likely CS
                            if(!csgLevels.containsKey(assetURI)){
                                csgLevels.put(assetURI, effectiveLevel);
                            } else if(csgLevels.get(assetURI) < effectiveLevel) {
                                csgLevels.replace(assetURI, effectiveLevel);
                            }

                        }

                        // Side effect trigger causation at this asset is from the most likely CSG
                        for(String assetURI : csgLevels.keySet()){
                            Integer effectiveLevel = csgLevels.get(assetURI);
                            if(assetLevels.containsKey(assetURI)){
                                if(effectiveLevel < assetLevels.get(assetURI)) {
                                    assetLevels.replace(assetURI, effectiveLevel);
                                }            
                            } else {
                                assetLevels.put(assetURI, effectiveLevel);
                            }
                        }
                    }
                }
            }

            // Next, calculate the effective TW level of each asset based on any primary threat causes
            for(String twasURI : threat.getEntryPoints()){
                // Find the entry point TWAS and get its TW level
                TrustworthinessAttributeSetDB twas = trustworthinessAttributeSets.get(twasURI);
                Integer effectiveLevel = twLevels.get(twas.getInferredLevel()).getLevelValue();

                // Apply the limit coming from optional CS
                Integer lowerLimit = causeSuppressionThisThreat.get(twasURI);
                if(effectiveLevel < lowerLimit)
                    effectiveLevel = lowerLimit;

                // Apply this level to the causation TW level of the asset
                String assetURI = twas.getLocatedAt();
                if(assetLevels.containsKey(assetURI)) {
                    // There is already a cause at this asset
                    if(assetLevels.get(assetURI) < effectiveLevel) {
                        // Replace it if this cause is less likely (more trustworthy)
                        assetLevels.replace(assetURI, effectiveLevel);
                    }
                } else {
                    // First cause at this asset, so just save it
                    assetLevels.put(assetURI, effectiveLevel);
                }
            }

            /* Next, include any secondary effect causes.
             *
             * NOTE : previously there was an 'else' here, so threats with primary causes could not
             * also have secondary causes. I removed the 'else', so we can now have primary threats
             * that also depend on the presence of misbehaviours, although until now we don't have 
             * any domain models that use this.
             */
            for(String msURI : threat.getSecondaryEffectConditions()){
                // Find the secondary effect cause MS and get its TW level
                MisbehaviourSetDB ms = misbehaviourSets.get(msURI);
                Integer effectiveLevel = invertToTrustworthiness(liLevels.get(ms.getPrior())).getLevelValue();

                // Apply the limit coming from optional CS
                Integer lowerLimit = causeSuppressionThisThreat.get(msURI);
                if(effectiveLevel < lowerLimit)
                    effectiveLevel = lowerLimit;

                // Apply this level to the causation TW level of the asset
                String assetURI = ms.getLocatedAt();
                if(assetLevels.containsKey(assetURI)) {
                    // There is already a cause at this asset
                    if(assetLevels.get(assetURI) < effectiveLevel) {
                        // Replace it if this cause is less likely (more trustworthy)
                        assetLevels.replace(assetURI, effectiveLevel);
                    }
                } else {
                    // First cause at this asset, so just save it
                    assetLevels.put(assetURI, effectiveLevel);
                }
            }

            // STAGE 2: find the effective threat causation TW level for each role, based on node type
            Map<String, Integer> roleLevels = new HashMap<>();
            for(String roleURI : assetsByRole.keySet()) {
                List<String> assetsThisRole = assetsByRole.get(roleURI);
                for(String assetURI : assetsThisRole) {
                    if(assetLevels.containsKey(assetURI)) {
                        Integer effectiveLevel = assetLevels.get(assetURI);
                        if(roleLevels.containsKey(roleURI)) {
                            // Update depending whether we're looking for the highest/lowest TW asset
                            if(sufficientRoles != null && sufficientRoles.contains(roleURI)) {
                                // We need the lowest TW asset, so update if this level is lower
                                if(effectiveLevel < roleLevels.get(roleURI)) {
                                    roleLevels.replace(roleURI, effectiveLevel);
                                }
                            } else {
                                // We need the highest TW asset, so update if this level is higher
                                if(effectiveLevel > roleLevels.get(roleURI)) {
                                    roleLevels.replace(roleURI, effectiveLevel);
                                }
                            }
                        } else {
                            roleLevels.put(roleURI, effectiveLevel);
                        }                        
                    }
                }
            }

            // STAGE 3: assemble causation levels from different roles
            Integer maxLevel = 0;
            for(String roleURI : roleLevels.keySet()) {
                Integer effectiveLevel = roleLevels.get(roleURI);
                if(maxLevel < effectiveLevel) {
                    maxLevel = effectiveLevel;
                }
            }

            // STAGE 4: constrain based on the most effective enabled CSG that has no optional CS
            Integer maxCsgLevel = 0;
            Set<String> allCSGs = new HashSet<>();
            allCSGs.addAll(threat.getBlockedByCSG());
            if(mode == RiskCalculationMode.FUTURE) allCSGs.addAll(threat.getMitigatedByCSG());

            for(String csgURI : allCSGs) {
                ControlStrategyDB csg = controlStrategies.get(csgURI);
                if(csg.isEnabled() && csg.getOptionalCS().isEmpty()){
                    ControlStrategyDB domainCSG = dcsgs.get(csg.getParent());
                    Integer domainCsgLevel = twLevels.get(domainCSG.getBlockingEffect()).getLevelValue();
                    Integer csgCoverageLevel = twLevels.get(csg.getCoverageLevel()).getLevelValue();
                    if (domainCsgLevel > maxCsgLevel) {
                        maxCsgLevel = domainCsgLevel;
                    }
                }
            }
            maxLevel = maxLevel > maxCsgLevel ? maxLevel : maxCsgLevel;

            // Convert to likelihood, and impose threat frequency limit if still above that limit
            LevelDB likelihood =  invertToLikelihood(trustworthinessLevels.get(maxLevel));
            if (threat.getFrequency() != null) {
                LevelDB frequency = liLevels.get(threat.getFrequency());
                likelihood = frequency.getLevelValue() < likelihood.getLevelValue() ? frequency : likelihood;
            }

            threat.setPrior(likelihood.getUri());

            // TODO: Only store at end of whole threat calculation (is this possible?)
            querier.store(threat, "system-inf");

        }

    }

    /** Set the likelihood of each MS to the likelihood of the most likely among the Threats that are 
     *  potential causes of that MS.
     * @return True, if any MS likelihoods were changed. Otherwise false.
     */
    private boolean updateMSLikelihoods() {
        boolean finished = true;

        // Update MS likelihoods based on those of causing threats
        for (MisbehaviourSetDB ms : misbehaviourSets.values()) {
            LevelDB maxLevel = liLevels.get(ms.getPrior());
            for (ThreatDB threat: threatsByEffect.getOrDefault(ms.getUri(), new ArrayList<>())) {
                if (threat.getPrior() != null) {
                    LevelDB level = liLevels.get(threat.getPrior());
                    if (level.getLevelValue() > maxLevel.getLevelValue()) {
                        maxLevel = level;
                    }
                }
            }

            if (!maxLevel.getUri().equals(ms.getPrior())) {
                ms.setPrior(maxLevel.getUri());
                querier.store(ms, "system-inf");
                finished = false;
            }
            
        }

        return finished;
    }

    /** Lower the TW of each TWAS to the inverse of the associated MS likelihood (from 
     *  the TWIS) if the TW is higher than the inverse MS likelihood.
     */
    private void updateTWASLevels() {

        for(String twasURI : entryPointMisbehaviour.keySet()){
            MisbehaviourSetDB ms = entryPointMisbehaviour.get(twasURI);
            LevelDB msLikelihoodInvert = invertToTrustworthiness(liLevels.get(ms.getPrior()));
            TrustworthinessAttributeSetDB twas = trustworthinessAttributeSets.get(twasURI);
            LevelDB twLevel = twLevels.get(twas.getInferredLevel());
            if (msLikelihoodInvert.getLevelValue() < twLevel.getLevelValue()) {
                twas.setInferredLevel(msLikelihoodInvert.getUri());
                querier.store(twas, "system-inf");
            }
        }
    }

    /** Calculate the risk level of each MS using a look-up table on its likelihood and impact levels
     *  Also fill in misbehaviour set label and description
     */
    private void calculateMisbehaviourRiskLevels() {
        final long startTime = System.currentTimeMillis();

        LevelDB worstRiskLevel = null;

        for (MisbehaviourSetDB ms : misbehaviourSets.values()) {
            // Get misbehaviour URI for this misbehaviour set
            String mUri = ms.getMisbehaviour();

            if (ms.getPrior() != null && ms.getImpactLevel() != null) {
                LevelDB newLevel = lookupRiskLevel(imLevels.get(ms.getImpactLevel()), liLevels.get(ms.getPrior()));
                if (worstRiskLevel == null || newLevel.getLevelValue() > worstRiskLevel.getLevelValue()) {
                    worstRiskLevel = newLevel;
                }
                ms.setRisk(newLevel.getUri());
                querier.store(ms, "system-inf");
            }
        }

        if (worstRiskLevel != null) {
            model.setRisk(worstRiskLevel.getUri());
            model.setRisksValid(true); //ensure that flag is set for returning in the response
            querier.store(model, "system-inf");
        }

        final long endTime = System.currentTimeMillis();
        logger.info("RiskCalculator.calculateMisbehaviourRiskLevels(): execution time {} ms", endTime - startTime);
                
    }

    /** Calculate forward causation properties and relationships
     *  - external cause status for TWAS with reduced TW not caused by any threat
     *  - from external cause TWAS to threats
     *  - from MS to secondary threats caused by the MS
     *  - from MS to threats caused by the MS undermining a related TWAS
     *  - from MS to threats caused by side effects of controls preventing other
     *    threat causation by a related MS or TWAS 
     */
    private void calculateCausationLinks(){
        final long startTime = System.currentTimeMillis();

        // Find out whether mixed threats are supported by this domain model
        Boolean mixedThreatCauses = domainFeatures.containsKey("domain#Feature-MixedThreatCauses");

        // Find external cause TWAS (add twas.core#isExternalCause -> true)
        for(TrustworthinessAttributeSetDB twas : trustworthinessAttributeSets.values()){
            LevelDB inferredLevel = twLevels.get(twas.getInferredLevel());
            LevelDB assertedLevel = twLevels.get(twas.getAssertedLevel());
            if (assertedLevel != null && inferredLevel != null && inferredLevel.getLevelValue() < (trustworthinessLevels.size() - 1)) {
                if (inferredLevel.getLevelValue() == assertedLevel.getLevelValue()){
                    if(mixedThreatCauses) {
                        // We support mixed threat causes, so the TWAS is considered to be the external cause
                        twas.setExternalCause(true);
                    } else {
                        // We don't support mixed threat causes, so the associated MS is considered to be the external cause
                        MisbehaviourSetDB ms = entryPointMisbehaviour.get(twas.getUri());
                        ms.setExternalCause(true);
                    }
                }
            }
        }
        
        // Find threat direct cause (core#causesThreat) and effect (core#causesDirectMisbehaviour) relationships
        for (ThreatDB threat : threats.values()) {
            // Get the threat URI - used later and helpful for debugging
            String threatURI = threat.getUri();

            // Is this a compliance threat or untriggered threat with no likelihood?
            if(threat.getPrior() == null) {
                // If yes, it has no causes or effects
                continue;
            }
            
            // Is this a triggered or non-triggered threat that isn't caused or is blocked from happening?
            Integer threatLikelihoodValue = liLevels.get(threat.getPrior()).getLevelValue();
            if(threatLikelihoodValue == 0){
                // If yes, it has no causes or effects
                continue;
            }

            /* TODO : decide whether side effect threats should be root causes
             *
             * - if yes, there is nothing to do here
             * - if no, we need to link from upstream MS suppressed by each CS that
             *   lead to side effects from the CS
             */

            // Get the effect of optional controls to suppress causes of this threat
            Map<String, Integer> causeSuppressionThisThreat = causeSuppressionByThreat.get(threat.getUri());

             // Find TWAS that cause the threat, taking account of inhibition by optional CS
            for(String twasURI : threat.getEntryPoints()){
                TrustworthinessAttributeSetDB twas = trustworthinessAttributeSets.get(twasURI);
                if(twas.getInferredLevel() == null){
                    // This TWAS cannot be causing the threat, so skip to the next cause
                    continue;
                }

                // Get the TW level of the TWAS, subject to cause inhibition by optional CS
                Integer causeTW = twLevels.get(twas.getInferredLevel()).getLevelValue();
                Integer coverageLevel = causeSuppressionThisThreat.get(twasURI);
                if(causeTW < coverageLevel)
                    causeTW = coverageLevel;

                // Invert to get a likelihood level
                Integer causeLikelihood = liLevels.size() - causeTW;

                // Now check if the TWAS causation likelihood is high enough to be a threat cause
                if(causeLikelihood >= threatLikelihoodValue) {
                    // This is a threat cause, but is it the TWAS or the upstream MS?
                    if(twas.isExternalCause()){
                        // The TWAS causes the threat based on its assumed TW level
                        twas.getCausedThreats().add(threatURI);
                        threat.getCausedBy().add(twasURI);
                    } else {
                        // The TWAS causes the threat but its TW level is caused by an upstream MS
                        MisbehaviourSetDB ms = entryPointMisbehaviour.get(twasURI);
                        ms.getCausedThreats().add(threatURI);
                        threat.getCausedBy().add(ms.getUri());
                    }
                }
            }

            // Find MS that cause the threat, taking account of inhibition by optional CS
            for(String msURI : threat.getSecondaryEffectConditions()){
                MisbehaviourSetDB ms = misbehaviourSets.get(msURI);
                if(ms.getPrior() == null){
                    continue;
                }

                // Get the likelihood of threat causation by the MS
                Integer causeLikelihood = liLevels.get(ms.getPrior()).getLevelValue();
                Integer coverageLikelihood = liLevels.size() - causeSuppressionThisThreat.get(msURI);
                if(causeLikelihood > coverageLikelihood)
                    causeLikelihood = coverageLikelihood;


                // Now check if the MS causation likelihood is high enough to be a threat cause
                if(causeLikelihood >= threatLikelihoodValue) {
                    // This is a threat cause
                    ms.getCausedThreats().add(threatURI);
                    threat.getCausedBy().add(msURI);
                }

            }

            // Find direct effects attributable to the threat (add threat.causesDirectMisbehaviour -> ms)
            for(String msURI : threat.getMisbehaviours()){
                MisbehaviourSetDB ms = misbehaviourSets.get(msURI);
                if(ms != null && ms.getPrior() != null) {
                    Integer msLikelihoodValue = liLevels.get(ms.getPrior()).getLevelValue();
                    if(msLikelihoodValue == threatLikelihoodValue) {
                        threat.getDirectMisbehaviours().add(msURI);
                        ms.getCausedBy().add(threatURI);
                    }
                }
            }

        }

        // Now store the results
        for(TrustworthinessAttributeSetDB twas : trustworthinessAttributeSets.values()){
            querier.store(twas, "system-inf");
        }
        for(MisbehaviourSetDB ms : misbehaviourSets.values()){
            querier.store(ms, "system-inf");
        }
        for(ThreatDB fromThreat : threats.values()) {
            querier.store(fromThreat, "system-inf");
        }

        final long endTime = System.currentTimeMillis();
        logger.info("RiskCalculator.calculateCausationLinks(): execution time {} ms", endTime - startTime);

    }

    /** Calculate threat paths, starting from a list of effects (MS) and causes (Threats).
     * 
     * @param nop: is a flag which determines whether this is restricted to normal operation threats
     * @param threatPath: the threats at the start of the threat paths that will be found (one branched path per threat)
     * @param effectPath: contains causes of the initial set of threats, plus any external causes that should also be used
     */
    private void calculateThreatPath(Boolean nop, Map<String, Set<String>> effectPath, Map<String, Set<String>> threatPath){
        // Useful constant
        String zeroLikelihood = likelihoodLevels.get(0).getUri();

        /* TODO : use this information to avoid unnecessary checks in the following */
        // This uses an iterative procedure, which terminates when no new threats or effects are found
        // At the start this is not the case as the initial effectPath and threatPath have just been added
        Boolean finished = false;

        // Keep track of which threats have just been added, so we don't keep repeating the same checks every iteration
        Set<String> liveEffects = new HashSet<>();
        Set<String> liveThreats = new HashSet<>();

        // On the first pass, all threats in the threat path are live
        for(String threatURI : threatPath.keySet()){
            liveThreats.add(threatURI);
        }
        int pass = 0;
        while(!finished){
            // Set finished to true and change it if we find something new
            finished = true;
            pass++;

            // Reset the list of live effects so it can be recreated with only new threat effects
            liveEffects.clear();

            // First add effects of the threats just added
            for(String threatURI : liveThreats) {
                // Get the threat object and its initial/root causes within the threat path
                ThreatDB threat = threats.get(threatURI);
                Set<String> threatCauses = threatPath.get(threatURI);

                // Check the direct effects attributable to this threat
                for(String msURI : threat.getDirectMisbehaviours()){
                    // Get a list of causes for this effect
                    MisbehaviourSetDB ms = misbehaviourSets.get(msURI);
                    Set<String> msCauses;
                    if(effectPath.containsKey(msURI)){
                        /* It is possible for a new threat to cause a previously found effect, in which
                         * case we already have some causes for this effect
                         */
                        msCauses = effectPath.get(msURI);
                    } else {
                        /* If this is a new effect, add it to the effectPath with an empty set of causes
                         */
                        msCauses = new HashSet<>();
                        effectPath.put(msURI, msCauses);
                    }

                    // Now add the causes of this threat to the causes of the effect it caused 
                    Integer nCauses = msCauses.size();
                    msCauses.addAll(threatCauses);

                    // If new root/initial causes were added
                    if(nCauses < msCauses.size()){
                        // This effect should become live again
                        liveEffects.add(msURI);

                        // And the search should continue at least one more iteration
                        finished = false;
                    }

                }

            }

            // Reset the list of live threats so it can be recreated from new effects
            liveThreats.clear();

            // Look for threats caused by the new effects just found
            for(String msURI : liveEffects){
                // Get the MS object and its causes
                MisbehaviourSetDB ms = misbehaviourSets.get(msURI);
                Set<String> msCauses = effectPath.get(msURI);

                // Check threats caused by this effect
                for(String threatURI : ms.getCausedThreats()){
                    // Get the threat
                    ThreatDB threat = threats.get(threatURI);

                    // Ignore the threat if it has no likelihood or zero likelihood
                    if(threat.getPrior() == null || threat.getPrior().equals(zeroLikelihood)){
                        // Skip to the next threat (though this shouldn't arise for a threat with causes)
                        continue;
                    }

                    // If this is the normal operational path, and the threat is a primary, adverse threat
                    if(nop && !(threat.isNormalOperation() || threat.isSecondaryThreat())){
                        // Skip to the next threat (though this shouldn't arise for a threat with causes)
                        continue;
                    }

                    // If this is the attack path, and the threat is a normal operational threat
                    if(!nop && threat.isNormalOperation()){
                        // Skip to the next threat
                        continue;
                    }

                    // Create an empty set of threat causes, appropriate if this is a new threat
                    Set<String> threatCauses = new HashSet<>();

                    // Recalling that the threat may have been found before if the MS was reactivated
                    if(threatPath.containsKey(threatURI)){
                        // It already has a list of root causes, but it may need updating
                        threatCauses = threatPath.get(threatURI);

                        // Now add the causes of this MS to the causes of the threat it caused 
                        Integer nCauses = threatCauses.size();
                        threatCauses.addAll(msCauses);

                        // If any new causes were added, make the threat live
                        if(threatCauses.size() > nCauses){
                            liveThreats.add(threatURI);
                            finished = false;
                        }

                    } else {
                        // If it is a new threat, we must check that all its causes have been found
                        Boolean gotAllCauses = true;

                        // Look for threat causes that haven't been found yet: first the entry points
                        if(gotAllCauses) for(String epURI : threat.getEntryPoints()) {
                            // Get the TWAS that could be causing the threat, and the associated MS
                            TrustworthinessAttributeSetDB causeTWAS = trustworthinessAttributeSets.get(epURI);

                            // If this is an external cause that really causes the threat, it should have been found
                            if(causeTWAS.isExternalCause()){
                                // Check it really did cause the threat (it might not if at a sufficient node)
                                if(causeTWAS.getCausedThreats().contains(threatURI)){
                                    // If it did, the cause should have been found already
                                    if(effectPath.containsKey(epURI)){
                                        // If found, an external cause has no causes, so go to the next threat cause
                                        continue;
                                    } else {
                                        // Set the status to not good and skip the rest of the threat causes
                                        gotAllCauses = false;
                                        break;
                                    }
                                }
                            } else {
                                // The MS would be considered to be causing the threat
                                MisbehaviourSetDB causeMS = entryPointMisbehaviour.get(epURI);

                                // Check it really did cause the threat (it might not if at a sufficient node)
                                if(causeMS.getCausedThreats().contains(threatURI)){
                                    // If it did, the cause should have been found already
                                    if(effectPath.containsKey(causeMS.getUri())){
                                        // Add the root/initial causes of this cause to the threat causes
                                        threatCauses.addAll(effectPath.get(causeMS.getUri()));
                                    } else {
                                        // Set the status to not good and skip the rest of the threat causes
                                        gotAllCauses = false;
                                        break;
                                    }
                                }
                            }
                        }

                        // Look for threat causes that haven't been found yet: now the secondary effect causes
                        if(gotAllCauses) for(String secURI : threat.getSecondaryEffectConditions()){
                            // Get the MS that could be causing the threat
                            MisbehaviourSetDB causeMS = misbehaviourSets.get(secURI);

                            // Check it really did cause the threat (it might not if at a sufficient node)
                            if(causeMS.getCausedThreats().contains(threatURI)){
                                // If it did, the cause should have been found already
                                if(effectPath.containsKey(causeMS.getUri())){
                                    // Add the root/initial causes of this cause to the threat causes
                                    threatCauses.addAll(effectPath.get(causeMS.getUri()));
                                } else {
                                    // Set the status to not good and skip the rest of the threat causes
                                    gotAllCauses = false;
                                    break;
                                }
                            }
                        }

                        // If all the causes were been found
                        if(gotAllCauses) {
                            // Add the new threat to the threat path
                            threatPath.put(threatURI, threatCauses);

                            // Make it a live threat for the next iteration
                            liveThreats.add(threatURI);

                            // And make sure there will be at least one more iteration
                            finished = false;
                        }

                    }
                    
                }

            }

        }

    }

    /** Calculate normal operation paths
     */
    private void calculateNormalOperationPaths(){
        final long startTime = System.currentTimeMillis();

        // Start with empty sets of threats and effects: the key is a threat or effect URI, the value is a list of initial/root causes
        Map<String, Set<String>> normalOpThreats = new HashMap<>();
        Map<String, Set<String>> normalOpEffects = new HashMap<>();

        // Useful constant
        String zeroLikelihood = likelihoodLevels.get(0).getUri();

        // Check the TWAS, find external causes, and add those that cause normal operational threats to the map
        for(TrustworthinessAttributeSetDB twas : trustworthinessAttributeSets.values()){
            if(twas.isExternalCause()){
                for(String threatURI : twas.getCausedThreats()){
                    ThreatDB threat = threats.get(threatURI);
                    // Normal operations can include secondary threats
                    if(threat.isNormalOperation() || threat.isSecondaryThreat()) {
                        // If it qualifies add it to the map with an empty list of initial causes
                        Set<String> icauses = new HashSet<>();
                        normalOpEffects.put(twas.getUri(), icauses);
                    }
                }    
            }
        }

        // If the domain model doesn't support mixed cause threats, some MS may be external causes
        for(MisbehaviourSetDB ms : misbehaviourSets.values()){
            if(ms.isExternalCause()){
                // This is an external cause
                for(String threatURI : ms.getCausedThreats()){
                    ThreatDB threat = threats.get(threatURI);
                    // Normal operations can include secondary threats
                    if(threat.isNormalOperation() || threat.isSecondaryThreat()) {
                        // If it qualifies add it to the map with an empty list of initial causes
                        Set<String> icauses = new HashSet<>();
                        normalOpEffects.put(ms.getUri(), icauses);
                    }
                }    
            }
        }

        // Find initial cause threats caused only by external causes
        for(ThreatDB threat : threats.values()){
            if(threat.getPrior() == null || threat.getPrior().equals(zeroLikelihood)){
                // Can't be an initial cause of anything, so we can skip this threat
                continue;
            }

            if(threat.getDirectMisbehaviours().isEmpty()){
                // Not an initial cause of any effects, so we can skip this threat
                continue;
            }

            // If the threat is a NOP or a secondary threat then it could be an initial cause
            if(threat.isNormalOperation() || threat.isSecondaryThreat()) {
                Boolean initialCause = true;
                if(initialCause) for(String twasURI : threat.getEntryPoints()){
                    TrustworthinessAttributeSetDB twas = trustworthinessAttributeSets.get(twasURI);
                    MisbehaviourSetDB ms = entryPointMisbehaviour.get(twasURI);
                    if(!twas.isExternalCause() && !ms.isExternalCause()) {
                        // Threat is not an initial cause, so we can skip the remaining causes
                        initialCause = false;
                        break;
                    }
                }
                if(initialCause) for(String msURI : threat.getSecondaryEffectConditions()){
                    MisbehaviourSetDB ms = misbehaviourSets.get(msURI);
                    if(!ms.isExternalCause()){
                        // Threat is not an initial cause, so we can skip the remaining causes
                        initialCause = false;
                        break;
                    }
                }

                if(initialCause){
                    // If this is an initial cause, add it to the list of threats, with itself as the cause
                    Set<String> icauses = new HashSet<>();
                    icauses.add(threat.getUri());
                    normalOpThreats.put(threat.getUri(), icauses);

                    // And set the property indicating that it is an initial cause
                    threat.setInitialCause(true);
                }
            }
        }

        // Now generate the rest of the normal operation paths from these starting points
        calculateThreatPath(true, normalOpEffects, normalOpThreats);

        // Now set properties indicating which initial cause threats cause which other normal operation threats and effects
        for(String threatURI : normalOpThreats.keySet()){
            // For each cause, create the link to the caused threat
            for(String causeURI : normalOpThreats.get(threatURI)){
                ThreatDB cause = threats.get(causeURI);
                cause.getIndirectThreats().add(threatURI);
            }
        }
        for(String msURI : normalOpEffects.keySet()){
            // For each cause, create the link to the caused threat
            for(String causeURI : normalOpEffects.get(msURI)){
                ThreatDB cause = threats.get(causeURI);
                cause.getIndirectMisbehaviours().add(msURI);
            }
            
            // Set the normal operaional effect flag (if this is an MS not a TWAS)
            MisbehaviourSetDB ms = misbehaviourSets.get(msURI);
            if(ms != null){
                ms.setNormalOpEffect(true);    
                querier.store(ms, "system-inf");
            }
        }

        // Save the updated threats to the querier
        for(ThreatDB threat : threats.values()){
            querier.store(threat, "system-inf");
        }

        final long endTime = System.currentTimeMillis();
        logger.info("RiskCalculator.calculateNormalOperationPaths(): execution time {} ms", endTime - startTime);
                
    }

    /** Calculate attack paths
     */
    private void calculateAttackPaths(){        
        final long startTime = System.currentTimeMillis();

        // Start with empty sets of threats and effects: the key is a threat or effect URI, the value is a list of initial/root causes
        Map<String, Set<String>> attackThreats = new HashMap<>();
        Map<String, Set<String>> attackEffects = new HashMap<>();

        // Useful constant
        String zeroLikelihood = likelihoodLevels.get(0).getUri();

        // The starting point for attack paths are the external causes, so check the TWAS
        for(TrustworthinessAttributeSetDB twas : trustworthinessAttributeSets.values()){
            if(twas.isExternalCause() && !twas.getCausedThreats().isEmpty()){
                // Add them to the attack effects with an empty set of root causes
                Set<String> rcauses = new HashSet<>();
                attackEffects.put(twas.getUri(), rcauses);    
            }
        }

        // If the domain model doesn't support mixed cause threats, some MS may be external causes
        for(MisbehaviourSetDB ms : misbehaviourSets.values()){
            if(ms.isExternalCause() && !ms.getCausedThreats().isEmpty()){
                // Add them to the attack effects with an empty set of root causes
                Set<String> rcauses = new HashSet<>();
                attackEffects.put(ms.getUri(), rcauses);
            }
        }

        // Then add all normal operation effects (all direct and indirect effects of initial causes)
        for(ThreatDB threat : threats.values()){
            if(threat.isInitialCause()){
                // Add their indirect effects
                for(String msURI : threat.getIndirectMisbehaviours()){
                    // Add them to the attack effects with an empty set of root causes
                    Set<String> rcauses = new HashSet<>();
                    attackEffects.put(msURI, rcauses);    
                }
            }
        }

        // Root causes are abnormal primary threats caused only by external causes plus normal operational effects
        for(ThreatDB threat : threats.values()){
            if(threat.getPrior() == null || threat.getPrior().equals(zeroLikelihood)){
                // Ignore threats that have no likelihood
                continue;
            }

            if(threat.isNormalOperation()){
                // Ignore normal operational process threats
                continue;
            }

            Boolean rootCause = true;
            if(rootCause) for(String twasURI : threat.getEntryPoints()){
                MisbehaviourSetDB ms = entryPointMisbehaviour.get(twasURI);
                if(!attackEffects.containsKey(ms.getUri()) && !attackEffects.containsKey(twasURI)){
                    // This is not a root cause, so skip the rest of its causes
                    rootCause = false;
                    break;
                }
            }
            if(rootCause) for(String msURI : threat.getSecondaryEffectConditions()){
                if(!attackEffects.containsKey(msURI)){
                    // This is not a root cause, so skip the rest of its causes
                    rootCause = false;
                    break;
                }
            }

            if(rootCause){
                // If this is a root cause, add it to the list with itself as the cause
                Set<String> rcauses = new HashSet<>();
                rcauses.add(threat.getUri());
                attackThreats.put(threat.getUri(), rcauses);

                // And set the property indicating it is a root cause
                threat.setRootCause(true);
            }

        }

        // Now generate the rest of the attack paths from these starting points
        calculateThreatPath(false, attackEffects, attackThreats);

        // Now set properties indicating which initial cause threats cause which other normal operation threats and effects
        for(String threatURI : attackThreats.keySet()){
            // For each cause, create the link to the caused threat
            for(String causeURI : attackThreats.get(threatURI)){
                ThreatDB cause = threats.get(causeURI);
                cause.getIndirectThreats().add(threatURI);
            }
        }
        for(String msURI : attackEffects.keySet()){
            // For each cause, create the link to the caused threat
            for(String causeURI : attackEffects.get(msURI)){
                ThreatDB cause = threats.get(causeURI);
                cause.getIndirectMisbehaviours().add(msURI);
            }
        }

        // Save the results to the querier
        for(ThreatDB threat : threats.values()){
            querier.store(threat, "system-inf");
        }

        final long endTime = System.currentTimeMillis();
        logger.info("RiskCalculator.calculateAttackPaths(): execution time {} ms", endTime - startTime);
                
    }

    /** Calculate threat risk levels based on secondary effect cascade
     */
    private void calculateThreatRiskLevels(){
        // This no longer calculates threat paths or root causes, just risk levels for caused misbehaviours

        final long startTime = System.currentTimeMillis();

        /**
         * Start by calculating a 'local' risk value per threat, based on potential effects x threat likelihood
         * Note that this should be applied even if some effects are not attributed to this threat. They will
         * have likelihood higher than the threat, but the threat likelihood is significant as it represents a
         * lower limit on effect likelihood, i.e. the effect likelihood were other threats not present.
         * Thus it makes sense to define the threat risk level as that reached if the likelihood of all direct
         * potential effects were equal to the threat likelihood (not the effect likelihood).
         */
        for(ThreatDB threat : threats.values()){
            // If the threat doesn't have a likelihood, then it doesn't have a risk level
            if (threat.getPrior() == null) {
                continue;
            }

            // If the threat does have a likelihood then initialise the risk level to the lowest possible level
            LevelDB threatRisk = riskLevels.get(0);

            // Now find the likelihood of the threat and impact of the effects and increase risk if necessary
            LevelDB threatLikelihood = liLevels.get(threat.getPrior());
            for(String msURI : threat.getMisbehaviours()){
                MisbehaviourSetDB ms = misbehaviourSets.get(msURI);
                LevelDB msImpact = imLevels.get(ms.getImpactLevel());
                if(threatLikelihood == null) {
                    logger.warn("Trying to find risk for threat {}, which has a non-existent likelihood", threat.getUri());
                }
                if(msImpact == null) {
                    logger.warn("Trying to find risk of threat causing MS {}, which has a non-existent impact level", ms.getUri());
                }
                LevelDB msRisk = lookupRiskLevel(msImpact, threatLikelihood);
                if(msRisk.getLevelValue() > threatRisk.getLevelValue()){
                    threatRisk = msRisk;
                }
            }

            // Now set the risk level for this threat
            threat.setRisk(threatRisk.getUri());

        }

        /**
         * Now for each threat, check which other threats are caused by it (directly or indirectly), and further
         * increase the risk level of the first threat based on the risk level of threats it causes.
         * In this calculation, only consider effects attributable to the threat (i.e. effect likelihood equals
         * threat likelihood) when deciding whether to attribute subsequent threats to the first threat.
         * Unlike the previous version, here we put the while loop outside the loop over threats, calculating
         * risk levels due to indirect effects for all threats together instead of doing them one at a time.
         */
        boolean finished = false;
        while (!finished) {
            finished = true;
            for (ThreatDB fromThreat : threats.values()) {
                // Ignore threats with no likelihood
                if (fromThreat.getPrior() == null) {
                    continue;
                }

                // Get the current risk level of this threat
                LevelDB threatRisk = riLevels.get(fromThreat.getRisk());
                
                // Find the effects attributable to this threat
                for(String msURI : fromThreat.getDirectMisbehaviours()){
                    // Get the misbehaviour set for the effect
                    MisbehaviourSetDB ms = misbehaviourSets.get(msURI);

                    // Find threats caused by this misbehaviour set
                    for(String toThreatURI : ms.getCausedThreats()){
                        // Find the threat object
                        ThreatDB toThreat = threats.get(toThreatURI);
                        
                        // Ignore the downstream threats that have no risk level
                        if(toThreat.getRisk() == null){
                            continue;
                        }

                        LevelDB downstreamRisk = riLevels.get(toThreat.getRisk());
                        
                        // Upgrade the risk of this threat if the downstream threat has a higher risk level
                        if(downstreamRisk.getLevelValue() > threatRisk.getLevelValue()){
                            threatRisk = downstreamRisk;
                            finished = false;
                        }

                    }

                }

                // Set the threat risk level to the result of this calculation
                fromThreat.setRisk(threatRisk.getUri());

            }

        }

        // Now save the results to the querier
        for(ThreatDB threat : threats.values()){
            if(threat.getRisk() != null) {
                querier.store(threat, "system-inf");
            }
        }

        final long endTime = System.currentTimeMillis();
        logger.info("RiskCalculator.calculateThreatRiskLevels(): execution time {} ms", endTime - startTime);
                
    }

    /**
     * Obtains risk levels using the lookup table from Mike's slides.
     *
     * @param impact the input impact level
     * @param likelihood the input likelihood
     * @return the risk level based on the inputs
     */
    private LevelDB lookupRiskLevel(LevelDB impact, LevelDB likelihood) {
        // Lookup tables for different impact, likelihood and risk level scales
        int lut5x5x5[][] = {
            { 0, 0, 0, 1, 1},
            { 0, 0, 1, 1, 2}, 
            { 0, 1, 2, 3, 3}, 
            { 1, 2, 3, 4, 4}, 
            { 1, 2, 3, 4, 4}};

        int lut5x6x5[][] = {
            { 0, 0, 0, 0, 1, 1},
            { 0, 0, 0, 1, 1, 2},
            { 0, 0, 1, 2, 3, 3},
            { 0, 1, 2, 3, 4, 4},
            { 0, 1, 2, 3, 4, 4}};
    
        int lut6x6x5[][] = {
            { 0, 0, 0, 0, 0, 0},
            { 0, 0, 0, 0, 1, 1},
            { 0, 0, 0, 1, 1, 2},
            { 0, 0, 1, 2, 3, 3},
            { 0, 1, 2, 3, 4, 4},
            { 0, 1, 2, 3, 4, 4}};
        
        int lut6x6x6[][] = {
            { 0, 0, 0, 0, 0, 0},
            { 0, 0, 0, 1, 1, 2},
            { 0, 0, 1, 2, 2, 3},
            { 0, 1, 2, 3, 4, 4},
            { 0, 2, 3, 4, 5, 5},
            { 0, 2, 3, 5, 5, 5}};
        
        int riskValue;

        int i = impact.getLevelValue();
        int l = likelihood.getLevelValue();

        // Ideally we would specify a lookup table label in the domain model.
        // For now, we'll assume that no two lookup tables have the same dimensions.
        // That means we can figure out which one to use based on the number of levels
        // in each scale.
        int sizei = imLevels.size();
        int sizel = liLevels.size();
        int sizer = riLevels.size();
        if(sizei == 5 && sizel == 5 && sizer == 5){
            riskValue = lut5x5x5[i][l];
        } else if (sizei == 5 && sizel == 6 && sizer == 5){
            riskValue = lut5x6x5[i][l];
        } else if (sizei == 6 && sizel == 6 && sizer == 5){
            riskValue = lut6x6x5[i][l];
        } else if (sizei == 6 && sizel == 6 && sizer == 6){
            riskValue = lut6x6x6[i][l];
        } else {
            // Should throw error, for now just return zero risk level
            riskValue=0;
        }

        return riskLevels.get(riskValue);

    }
    
    private LevelDB invertToLikelihood(LevelDB level) {
        if(level == null) {
            logger.warn("Trying to invert a null TW level to a likelihood level");
        }
        return likelihoodLevels.get(likelihoodLevels.size() - level.getLevelValue() - 1);
    }

    private LevelDB invertToTrustworthiness(LevelDB level) {
        if(level == null) {
            logger.warn("Trying to invert a null likelihood level to a TW level");
        }
        return trustworthinessLevels.get(trustworthinessLevels.size() - level.getLevelValue() - 1);
    }

}

