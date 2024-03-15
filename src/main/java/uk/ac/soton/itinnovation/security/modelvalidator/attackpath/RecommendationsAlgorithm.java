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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;
import uk.ac.soton.itinnovation.security.model.system.RiskVector;
import uk.ac.soton.itinnovation.security.modelquerier.IQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ModelDB;
import uk.ac.soton.itinnovation.security.modelvalidator.Progress;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.recommendations.ControlDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.recommendations.ControlStrategyDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.recommendations.RecommendationDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.recommendations.RecommendationReportDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.recommendations.StateDTO;

import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.Variable;

import uk.ac.soton.itinnovation.security.systemmodeller.mongodb.RecommendationRepository;
import uk.ac.soton.itinnovation.security.systemmodeller.attackpath.RecommendationsService.RecommendationJobState;
import uk.ac.soton.itinnovation.security.systemmodeller.model.RecommendationEntity;

import uk.ac.soton.itinnovation.security.systemmodeller.model.RecommendationEntity;

@Component
public class RecommendationsAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationsAlgorithm.class);

    private AttackPathDataset apd;
    private IQuerierDB querier;
    private String modelId;
    private int recCounter = 0;
    private RecommendationReportDTO report;
    private String riskMode = "CURRENT";
    private String acceptableRiskLevel;
    private List<String> targetMS;
    private RiskVector initialRiskVector;
    private boolean localSearch;
    private boolean abortFlag = false;
    private RecommendationRepository recRepository;
    private String jobId;

    // allPaths flag for single or double backtrace
    private boolean shortestPath = true;

    public RecommendationsAlgorithm(RecommendationsAlgorithmConfig config) {
        this.querier = config.getQuerier();
        this.modelId = config.getModelId();
        this.riskMode = config.getRiskMode();
        this.acceptableRiskLevel = config.getAcceptableRiskLevel();
        this.targetMS = config.getTargetMS();
        this.report = new RecommendationReportDTO();
        this.localSearch = config.getLocalSearch();

        initializeAttackPathDataset();
    }

    private void initializeAttackPathDataset() {
        logger.debug("Preparing datasets ...");

        apd = new AttackPathDataset(querier);
    }

    public void setRecRepository(RecommendationRepository recRepository, String job) {
        this.recRepository = recRepository;
        this.jobId = job;
    }

    public void setAbortFlag() {
        this.abortFlag = true;
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

    /**
     * wrapper method for check existing risk calculation mode
     * @param requestedRiskMode 
     */
    public void checkRequestedRiskCalculationMode(String requestedRiskMode) {
        if (!checkRiskCalculationMode(requestedRiskMode)) {
            logger.debug("mismatch between the stored risk calculation mode and the requested one");
            throw new RuntimeException("mismatch between the stored risk calculation mode and the requested one");
        }
    }

    /**
     * Calculate the attack tree
     * @return the attack graph
     */
    private AttackTree calculateAttackTree() {
        if (!targetMS.isEmpty()) {
            logger.debug("caclulate attack tree using MS list: {}", targetMS);
            return calculateAttackTree(targetMS);
        } else {
            logger.debug("caclulate attack tree using acceptable risk level: {}", acceptableRiskLevel);
            return calculateAttackTree(apd.filterMisbehavioursByRiskLevel(acceptableRiskLevel));
        }
    }

    /**
     * Calculate the attack tree
     * @param targetUris
     * @return the attack graph
     * @throws RuntimeException 
     */
    private AttackTree calculateAttackTree(List<String> targetUris) throws RuntimeException {
        logger.debug("calculate attack tree with isFUTURE: {}, shortestPath: {}", riskMode, shortestPath);
        logger.debug("target URIs: {}", targetUris);

        boolean isFutureRisk = apd.isFutureRisk(riskMode);
        AttackTree attackTree = null;

        try {
            final long startTime = System.currentTimeMillis();
            attackTree = new AttackTree(targetUris, isFutureRisk, shortestPath, apd);
            attackTree.stats();
            final long endTime = System.currentTimeMillis();
            logger.info("AttackPathAlgorithm.calculateAttackTree: execution time {} ms", endTime - startTime);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return attackTree;
    }

    private RiskVector processOption(List<String> csgList, Set<String> csSet, CSGNode childNode) {

        // Calculate risk, and create a potential recommendation
        RiskVector riskResponse = null;
        RecommendationDTO recommendation = null;
        try {
            riskResponse = apd.calculateRisk(modelId, RiskCalculationMode.valueOf(riskMode));
            logger.debug("Risk calculation response: {}", riskResponse);
            logger.debug("Overall model risk: {}", riskResponse.getOverall());
            StateDTO state = apd.getState();

            recommendation = createRecommendation(csgList, csSet, state);

            if (report.getRecommendations() == null) {
                report.setRecommendations(new ArrayList<>());
            }

            // store recommendation to node
            childNode.setRecommendation(recommendation);

            // flag this recommendation if there is risk reduction
            childNode.setGreaterEqualLess(initialRiskVector.compareTo(riskResponse));

        } catch (Exception e) {
            logger.error("failed to get risk calculation, restore model");

            // restore model ...
            apd.changeCS(csSet, false);

            // raise exception since failed to run risk calculation
            throw new RuntimeException(e);
        }

        return riskResponse;
    }

    private Set<String> extractCS(List<String> csgList) {

        Set<String> csSet = new HashSet<>();
        for (String csg : csgList) {
            for (String cs : apd.getCsgInactiveControlSets(csg)) {
                csSet.add(cs);
            }
        }

        logger.debug("CS set for LE CSG_option {}", csgList);
        logger.debug("  └──> {}", csSet);

        return csSet;
    }

    private void updateJobState(RecommendationJobState newState) {
        // get job status:
        Optional<RecommendationEntity> optionalRec = recRepository.findById(jobId);
        logger.debug("updating job status: {}", optionalRec);
        optionalRec.ifPresent(rec -> {
            rec.setState(newState);
            rec.setModifiedAt(LocalDateTime.now());
            recRepository.save(rec);
        });
    }

    private boolean checkJobAborted(){
        // get job status:
        Optional<RecommendationJobState> jobState = recRepository.findById(jobId).map(RecommendationEntity::getState);
        logger.debug("APPLY CSG: check task status: {}", jobState);
        if (jobState.isPresent() && jobState.get() == RecommendationJobState.ABORTED) {
            logger.debug("APPLY CSG: Got job status, cancelling this task");
            setAbortFlag();
        }
        return abortFlag;
    }

    private CSGNode applyCSGs(LogicalExpression le) {
        CSGNode node = new CSGNode();
        return applyCSGs(le, node, "", apd.getRiskVector());
    }

    /**
     * Build CSG recommendations tree.
     * The method is recursive and will create a tree of CSG options.
     * @param le
     * @param myNode
     * @param parentStep
     * @param parentRiskVector
     * @return
     */
    private CSGNode applyCSGs(LogicalExpression le, CSGNode myNode, String parentStep, RiskVector parentRiskVector) {
        logger.debug("applyCSGs() with parentStep: {}", parentStep);

        // convert LE to DNF
        le.applyDNF(300);

        // convert from CSG logical expression to list of CSG options
        List<Expression> csgOptions = le.getListFromOr();

        logger.debug("Derived DNF (OR) CSG expressions: {} (options).", csgOptions.size());
        for (Expression csgOption : csgOptions) {
            logger.debug("   └──> {}", csgOption);
        }

        // examine CSG options
        int csgOptionCounter = 0;
        for (Expression csgOption : csgOptions) {

            // check if job is aborted:
            if (checkJobAborted()) {
                break;
            } else {
                updateJobState(RecommendationJobState.RUNNING);
            }

            csgOptionCounter += 1;
            String myStep = String.format("%s%d/%d", parentStep.equals("") ? "" : parentStep + "-", csgOptionCounter, csgOptions.size());
            logger.debug("examining CSG LE option {}: {}", myStep, csgOption);

            List<Variable> options = LogicalExpression.getListFromAnd(csgOption);

            List<String> csgList = new ArrayList<>();

            for (Variable va : options) {
                csgList.add(va.toString());
            }
            logger.debug("CSG flattened list ({}): {}", csgList.size(), csgList);

            CSGNode childNode = new CSGNode(csgList);
            myNode.addChild(childNode);

            // get available CS
            Set<String> csSet = extractCS(csgList);

            // store CS set in the node to reconstruct the final CS list
            // correctly in the Recommendation report for nested iterations.
            childNode.setCsList(csSet);

            logger.debug("CS set for LE CSG_option {}", csgOption);
            logger.debug("  └──> {}", csSet);

            // apply all CS in the CS_set
            if (csSet.isEmpty()) {
                logger.warn("EMPTY csSet is found, skipping this CSG option");
                continue;
            }
            apd.changeCS(csSet, true);

            // Re-calculate risk now and create a potential recommendation
            RiskVector riskResponse = processOption(csgList, csSet, childNode);

            // Check for success
            // Finish if the maximum risk is below or equal to the acceptable risk level
            // If we are constrained to some target MS, then we should only check the
            // risk levels of the targets (otherwise it is likely it will never finish)

            boolean globalRiskAcceptable = targetMS.isEmpty() && apd.compareRiskLevelURIs(riskResponse.getOverall(), acceptableRiskLevel) <= 0;
            boolean targetedRiskAcceptable = !targetMS.isEmpty() && apd.compareMSListRiskLevel(targetMS, acceptableRiskLevel) <= 0;

            if (globalRiskAcceptable || targetedRiskAcceptable) {
                logger.debug("Success termination condition reached for {}", myStep);
            } else {
                logger.debug("Risk is still higher than {}", acceptableRiskLevel);

                // Check if we should abort
                // If doing localSearch then stop searching (fail) if the risk vector is higher than the parent
                // In this way we do not let the risk vector increase. We could make this softer by comparing
                // the "overall risk level", i.e. the highest risk level of the current and parent vector

                if (localSearch && (riskResponse.compareTo(parentRiskVector) == 1)) {
                    logger.debug("Risk level has increased. Abort branch {}", myStep);
                } else {

                    // Carry on searching by recursing into the next level

                    logger.debug("Recalculate nested attack path tree");
                    AttackTree nestedAttackTree = calculateAttackTree();
                    LogicalExpression nestedLogicalExpression = nestedAttackTree.attackMitigationCSG();
                    applyCSGs(nestedLogicalExpression, childNode, myStep, riskResponse);
                }
            }

            // undo CS changes in CS_set
            logger.debug("Undo CS controls ({})", csSet.size());
            apd.changeCS(csSet, false);
            logger.debug("Re-run risk calculation after CS changes have been revoked");
            // TODO: optimise this
            // This does more work than is necessary as we are going to run the risk calculation again in the next iteration.
            // The reason it is here is because of the side effect of the calculateRisk method which updates the various cached data.
            apd.calculateRisk(modelId, RiskCalculationMode.valueOf(riskMode));

            logger.debug("Finished examining CSG LE option {}: {}", myStep, csgOption);
        }

        logger.debug("return from applyCSGs() iteration with parentStep: {}", parentStep);

        return myNode;
    }

    /**
     * Create control strategy DTO object
     * @param csgUri
     * @return
     */
    private ControlStrategyDTO createControlStrategyDTO(String csgUri) {
        ControlStrategyDTO csgDto = new ControlStrategyDTO();
        csgDto.setUri(csgUri);
        csgDto.setDescription(apd.getCSGDescription(csgUri));
        csgDto.setCategory(csgUri.contains("-Runtime") ? "Applicable" : "Conditional");
        return csgDto;
    }

    /**
     * Crete CSG DTO object
     * @param csgList
     * @return 
     */
    private Set<ControlStrategyDTO> createCSGDTO(List<String> csgList) {
        Set<ControlStrategyDTO> recCSGSet = new HashSet<>();
        for (String csgUri : csgList) {
            recCSGSet.add(createControlStrategyDTO(csgUri));
        }
        return recCSGSet;
    }

    /**
     * create control DTO
     * @param ctrlUri
     * @return 
     */
    private ControlDTO createControlDTO(String ctrlUri) {
        return apd.fillControlDTO(ctrlUri);
    }

    /**
     * Crete control set DTO
     * @param csSet
     * @return 
     */
    private Set<ControlDTO> createCSDTO(Set<String> csSet) {
        Set<ControlDTO> recControlSet = new HashSet<>();
        for (String ctrlUri : csSet) {
            recControlSet.add(createControlDTO(ctrlUri));
        }
        return recControlSet;
    }

    /**
     * Create recommendation DTO object
     * @param csgList
     * @param csSet
     * @param state
     * @return 
     */
    private RecommendationDTO createRecommendation(List<String> csgList, Set<String> csSet, StateDTO state) {
        RecommendationDTO recommendation = new RecommendationDTO();
        recommendation.setIdentifier(recCounter++);
        recommendation.setState(state);
        logger.debug("Creating a potential recommendation ID: {}", recommendation.getIdentifier());

        Set<ControlStrategyDTO> csgDTOs = createCSGDTO(csgList);
        Set<ControlDTO> controlDTOs = createCSDTO(csSet);

        recommendation.setControlStrategies(csgDTOs);
        recommendation.setControls(controlDTOs);

        // N.B. the list of CSG and CS will be updated later if the recommendation is nested.
        return recommendation;
    }

    /**
     * Update recommendation DTO object
     * @param recommendation
     * @param csgList
     * @param csSet 
     */
    private void updateRecommendation(RecommendationDTO recommendation, List<String> csgList, Set<String> csSet) {
        Set<ControlStrategyDTO> csgDTOs = createCSGDTO(csgList);
        Set<ControlDTO> controlDTOs = createCSDTO(csSet);

        recommendation.setControlStrategies(csgDTOs);
        recommendation.setControls(controlDTOs);
    }

    /**
     * Parse the CSGNode tree and find recommendations
     * @param node 
     */
    private void makeRecommendations(CSGNode node) {
        List<CSGNode> path = new ArrayList<>();
        makeRecommendations(node, path);
    }

    /**
     * Parse the CSGNode tree and find recommendations
     * @param node
     * @param path 
     */
    private void makeRecommendations(CSGNode node, List<CSGNode> path) {

        // if path is undefined, initalise it as empty list
        if (path == null) {
            path = new ArrayList<>();
        }

        // Create a new instance of the path list for the current recursive call
        List<CSGNode> currentPath = new ArrayList<>(path);
        currentPath.add(node);

        if (node.getChildren().isEmpty()) {
            if ((node.getRecommendation() != null) & (node.getGreaterEqualLess() > 0)){
                Set<String> csgSet = reconstructCSGs(currentPath);
                Set<String> csSet = reconstructCSs(currentPath);
                updateRecommendation(node.getRecommendation(), new ArrayList<>(csgSet), csSet);
                report.getRecommendations().add(node.getRecommendation());
            } else {
                logger.debug("skipping recommendation: {}", node.getRecommendation());
            }
        } else {
            for (CSGNode child : node.getChildren()){
                makeRecommendations(child, currentPath);
            }
        }
    }

    /**
     * Reconstruct CSGs for nested recommendations
     * @param nodeList
     * @return 
     */
    private Set<String> reconstructCSGs(List<CSGNode> nodeList) {
        Set<String> csgSet = new HashSet<>();
        for (CSGNode node : nodeList) {
            for (String csg : node.getCsgList()) {
                csgSet.add(csg);
            }
        }
        return csgSet;
    }

    /**
     * Reconstruct CS for nested recommendations
     * @param nodeList
     * @return 
     */
    private Set<String> reconstructCSs(List<CSGNode> nodeList) {
        Set<String> csSet = new HashSet<>();
        for (CSGNode node : nodeList) {
            for (String cs : node.getCsList()) {
                csSet.add(cs);
            }
        }
        return csSet;
    }

    /**
     * Start recommendations algorithm
     * @param progress
     * @return
     */
    public RecommendationReportDTO recommendations(Progress progress) {

        logger.info("Recommendations core part (risk mode: {})", riskMode);

        try {
            progress.updateProgress(0.1, "Getting initial risk state");
            // get initial risk state
            initialRiskVector = apd.calculateRisk(modelId, RiskCalculationMode.valueOf(riskMode));

            StateDTO state = apd.getState();
            report.setCurrent(state);

            progress.updateProgress(0.2, "Calculating attack tree");
            AttackTree threatTree = calculateAttackTree();

            // step: attackMitigationCSG?
            LogicalExpression attackMitigationCSG = threatTree.attackMitigationCSG();
            attackMitigationCSG.displayExpression();

            // step: rootNode?
            progress.updateProgress(0.3, "Trying different control strategy options");
            CSGNode rootNode = applyCSGs(attackMitigationCSG);

            // step: makeRecommendations on rootNode?
            logger.debug("MAKE RECOMMENDATIONS");
            progress.updateProgress(0.8, "Making recommendations");
            makeRecommendations(rootNode);

            progress.updateProgress(0.9, "Preparing report");
            List<RecommendationDTO> recommendations = report.getRecommendations() != null ? report.getRecommendations() : Collections.emptyList();
            logger.info("The Recommendations Report has: {} recommendations", recommendations.size());
            for (RecommendationDTO rec : recommendations) {
                logger.debug("  recommendation: {}", rec.getState().getRisk());
                for (ControlStrategyDTO csgDTO : rec.getControlStrategies()) {
                    logger.debug("  └──> csgs: {}", csgDTO.getUri().substring(7));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return report;
    }

}

