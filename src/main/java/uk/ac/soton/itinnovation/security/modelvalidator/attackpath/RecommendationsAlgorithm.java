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
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;
import uk.ac.soton.itinnovation.security.model.system.RiskVector;
import uk.ac.soton.itinnovation.security.modelquerier.IQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ModelDB;
import uk.ac.soton.itinnovation.security.modelvalidator.Progress;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.recommendations.CSGNode;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.recommendations.ControlDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.recommendations.ControlStrategyDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.recommendations.RecommendationDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.recommendations.RecommendationReportDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.recommendations.StateDTO;

import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.Variable;

@Component
public class RecommendationsAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationsAlgorithm.class);

    private AttackPathDataset apd;
    private IQuerierDB querier;
    private String modelId;
    private int recCounter = 0;
    private RecommendationReportDTO report;
    private String riskMode = "CURRENT";

    // allPaths flag for single or double backtrace
    private boolean allPaths = true;

    public RecommendationsAlgorithm(RecommendationsAlgorithmConfig config) {
        this.querier = config.getQuerier();
        this.modelId = config.getModelId();
        this.riskMode = config.getRiskMode();
        this.report = new RecommendationReportDTO();

        initializeAttackPathDataset();
    }

    private void initializeAttackPathDataset() {
        logger.debug("Preparing datasets ...");

        apd = new AttackPathDataset(querier);
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
     * Calculate the attack path graph using an MS threshold
     * @param thresholdLevel
     * @return 
     */
    public AttackTree calcAttackTree(String thresholdLevel) {
        return calculateAttackTree(apd.filterMisbehaviours(thresholdLevel), riskMode, this.allPaths);
    }

    /**
     * Calculate the attack path graph
     * @return 
     */
    public AttackTree calcAttackTree() {
        return calculateAttackTree(apd.filterMisbehaviours(), riskMode, this.allPaths);
    }

    /**
     * Calculate the attack path graph
     * @param targetUris
     * @param riskCalculationMode
     * @param allPaths
     * @return
     * @throws RuntimeException 
     */
    public AttackTree calculateAttackTree(List<String> targetUris, String riskCalculationMode, boolean allPaths) throws RuntimeException {
        logger.debug("calculate attack tree with isFUTURE: {}, allPaths: {}", riskCalculationMode, allPaths);
        logger.debug("target URIs: {}", targetUris);

        checkRequestedRiskCalculationMode(riskCalculationMode);

        return calculateAttackTreeInternal(targetUris, riskCalculationMode, allPaths);
    }

    private AttackTree calculateAttackTreeInternal(List<String> targetUris, String riskCalculationMode, boolean singlePath)
            throws RuntimeException {

        boolean isFutureRisk = apd.isFutureRisk(riskCalculationMode);
        AttackTree attackTree = null;

        try {
            final long startTime = System.currentTimeMillis();
            attackTree = new AttackTree(targetUris, isFutureRisk, singlePath, apd);
            attackTree.stats();
            final long endTime = System.currentTimeMillis();
            logger.info("AttackPathAlgorithm.calculateAttackTree: execution time {} ms", endTime - startTime);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return attackTree;
    }

    private CSGNode applyCSGs(LogicalExpression le) {
        CSGNode node = new CSGNode();
        return applyCSGs(le, node, "");
    }

    /**
     * Build CSG recommendations tree
     * @param le
     * @param myNode
     * @param parentStep - for logging
     * @return 
     */
    private CSGNode applyCSGs(LogicalExpression le, CSGNode myNode, String parentStep) {
        logger.debug("applyCSGs() recursive method with parentStep: {}", parentStep);

        // convert LE to DNF
        le.applyDNF(300);

        // convert from CSG logical expression to list of CSG options
        List<Expression> csgOptions = le.getListFromOr();

        logger.debug("Derived DNF (OR) CSG expressions: {} (options).", csgOptions.size());
        for (Expression csgOption : csgOptions) {
            logger.debug("   └──> {}", csgOption);
        }

        int csgOptionCounter = 0;

        // examine CSG options
        for (Expression csgOption : csgOptions) {

            csgOptionCounter++;
            String myStep = String.format("%s%d/%d", parentStep.equals("") ? "" : parentStep + "-", csgOptionCounter, csgOptions.size());
            logger.debug("examining CSG LE option {}: {}", myStep, csgOption);

            List<Variable> options = LogicalExpression.getListFromAnd(csgOption);

            List<String> csgList = new ArrayList<>();

            for (Variable va : options) {
                csgList.add(va.toString());
            }
            logger.debug("CSG flattened list ({}): {}", csgList.size(), csgList);

            // logger.debug("adding a child CSGNode with {} csgs", csgList.size());
            CSGNode childNode = new CSGNode(csgList);
            myNode.addChild(childNode);

            Set<String> csSet = new HashSet<>();
            for (String csg : csgList) {
                for (String cs : apd.getCsgInactiveControlSets(csg)) {
                    csSet.add(cs);
                }
            }

            // store CS set in the node to reconstruct the final CS list
            // correctly in the Recommendation report for nested iterations.
            childNode.setCsList(csSet);

            logger.debug("CS set for LE CSG_option {}", csgOption);
            logger.debug("  └──> {}", csSet);

            // apply all CS in the CS_set
            if (csSet.isEmpty()) {
                logger.warn("EMPTY csSet is found, skipping iteration");
                continue;
            }
            apd.changeCS(csSet, true);

            // Re-calculate risk now and create a potential recommendation
            RiskVector riskResponse = null;
            RecommendationDTO recommendation = null;
            try {
                riskResponse = apd.calculateRisk(this.modelId, RiskCalculationMode.valueOf(riskMode));
                logger.debug("Risk calculation response: {}", riskResponse);
                logger.debug("Overall model risk: {}", riskResponse.getOverall());
                StateDTO state = apd.getState();

                recommendation = createRecommendation(csgList, csSet, state);

                if (this.report.getRecommendations() == null) {
                    report.setRecommendations(new ArrayList<>());
                }
                childNode.setRecommendation(recommendation);

            } catch (Exception e) {
                logger.error("failed to get risk calculation, restore model");

                // restore model ...
                apd.changeCS(csSet, false);

                // raise exception since failed to run risk calculation
                throw new RuntimeException(e);
            }

            // check if risk has improved or teminate iteration?
            logger.debug("check for a termination condition for ID {}", recommendation.getIdentifier());
            if ((riskResponse != null) & (apd.compareOverallRiskToMedium(riskResponse.getOverall()))) {
                logger.info("Termination condition reached for {}", myStep);
            } else {
                logger.debug("Risk is still higher than Medium");
                logger.info("Recalculating nested attack path tree");
                AttackTree nestedAttackTree = calcAttackTree("domain#RiskLevelMedium");
                LogicalExpression nestedLogicalExpression = nestedAttackTree.attackMitigationCSG();
                applyCSGs(nestedLogicalExpression, childNode, myStep);
            }

            // undo CS changes in CS_set
            logger.debug("Undo CS controls ({})", csSet.size());
            apd.changeCS(csSet, false);
            apd.calculateRisk(this.modelId, RiskCalculationMode.valueOf(riskMode));

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
        recommendation.setIdentifier(this.recCounter++);
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
            if (node.getRecommendation() != null) {
                Set<String> csgSet = reconstructCSGs(currentPath);
                Set<String> csSet = reconstructCSs(currentPath);
                updateRecommendation(node.getRecommendation(), new ArrayList<>(csgSet), csSet);
                report.getRecommendations().add(node.getRecommendation());
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
     * @throws RuntimeException 
     */
    public RecommendationReportDTO recommendations(Progress progress) throws RuntimeException {

        logger.info("Recommendations core part (risk mode: {})", riskMode);

        try {
            progress.updateProgress(0.1, "Getting initial risk state");
            // get initial risk state
            RiskVector riskResponse = apd.calculateRisk(this.modelId, RiskCalculationMode.valueOf(riskMode));

            StateDTO state = apd.getState();
            report.setCurrent(state);

            progress.updateProgress(0.2, "Calculating attack tree");
            AttackTree threatTree = calcAttackTree();

            // step: attackMitigationCSG?
            LogicalExpression attackMitigationCSG = threatTree.attackMitigationCSG();
            attackMitigationCSG.displayExpression();

            // step: rootNode?
            progress.updateProgress(0.3, "Applying control strategies");
            CSGNode rootNode = applyCSGs(attackMitigationCSG);

            // step: makeRecommendations on rootNode?
            logger.debug("MAKE RECOMMENDATIONS");
            progress.updateProgress(0.4, "Making recommendations");
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

