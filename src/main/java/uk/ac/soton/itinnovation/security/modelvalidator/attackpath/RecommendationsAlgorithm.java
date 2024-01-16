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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;
import uk.ac.soton.itinnovation.security.model.system.RiskVector;
import uk.ac.soton.itinnovation.security.modelquerier.IQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ControlSetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ModelDB;
import uk.ac.soton.itinnovation.security.modelvalidator.Progress;
import uk.ac.soton.itinnovation.security.modelvalidator.RiskCalculator;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.LogicalExpression;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.AdditionalPropertyDTO;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.AssetDTO;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.ConsequenceDTO;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.ControlDTO;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.ControlStrategyDTO;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.RecommendationDTO;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.RecommendationReportDTO;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.StateDTO;
import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;
import uk.ac.soton.itinnovation.security.model.system.RiskVector;
import uk.ac.soton.itinnovation.security.modelquerier.IQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ModelDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ControlDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ControlSetDB;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.LogicalExpression;
import com.bpodgursky.jbool_expressions.And;
import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.Not;
import com.bpodgursky.jbool_expressions.Or;
import com.bpodgursky.jbool_expressions.Variable;
import com.bpodgursky.jbool_expressions.parsers.ExprParser;
import com.bpodgursky.jbool_expressions.rules.RuleSet;

@Component
public class RecommendationsAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationsAlgorithm.class);

    private AttackPathDataset apd;
    private IQuerierDB querier;
    private String modelId;
    private int recCounter = 0;
    private RecommendationReportDTO report;
    private String riskMode = "CURRENT";

    private List<String> nodes = new ArrayList<>();
    private List<String> links = new ArrayList<>();

    // allPaths flag for single or double backtrace
    private boolean allPaths = false;

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

    public void checkRequestedRiskCalculationMode(String requestedRiskMode) {
        if (!checkRiskCalculationMode(requestedRiskMode)) {
            logger.debug("mismatch between the stored risk calculation mode and the requested one");
            throw new RuntimeException("mismatch between the stored risk calculation mode and the requested one");
        }
    }

    public boolean checkTargetUris(List<String> targetUris) {
        logger.debug("Checking submitted list of target URIs: {}", targetUris);

        if (!apd.checkMisbehaviourList(targetUris)) {
            logger.error("shortest path, target MS URI not valid");
            return false;
        }
        return true;
    }

    public AttackTree calcAttackTree(String thresholdLevel) {
        return calculateAttackTree(apd.filterMisbehaviours(thresholdLevel), riskMode, this.allPaths);
    }

    public AttackTree calcAttackTree() {
        return calculateAttackTree(apd.filterMisbehaviours(), riskMode, this.allPaths);
    }

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

    private CSGNode applyCSGs(LogicalExpression le, CSGNode myNode) {
        logger.debug("applyCSGs()");
        if (myNode == null) {
            myNode = new CSGNode();
        }

        // convert LE to DNF
        le.applyDNF(100);

        // convert from CSG logical expression to list of CSG options
        List<Expression> csgOptions = le.getListFromOr();
        logger.debug("List of OR CSG options: {}", csgOptions.size());
        for (Expression csgOption : csgOptions) {
            logger.debug("└──> {}", csgOption);
        }

        logger.debug("list of OR CSG options: {}", csgOptions.size());
        // examine CSG options
        for (Expression csgOption : csgOptions) {
            logger.debug("examining CSG LE option: {}", csgOption);
            logger.debug("CSG LE option type: {}", csgOption.getClass().getName());

            List<Variable> options = le.getListFromAnd(csgOption);

            List<String> csgList = new ArrayList<>();

            for (Variable va : options) {
                logger.debug("variable -> {}, {}", va, va.getClass().getName());
                csgList.add(va.toString());
            }
            logger.debug("LE csgList: {}", csgList);

            CSGNode childNode = new CSGNode(csgList);
            myNode.addChild(childNode);

            Set<String> csSet = new HashSet<>();
            for (String csg : csgList) {
                for (String cs : apd.getCsgInactiveControlSets(csg)) {
                    csSet.add(cs);
                }
            }
            logger.debug("CS set for LE CSG_option {}", csgOption);
            logger.debug("  └──> {}", csSet);

            // apply all CS in the CS_set
            // TODO: I need to keep track of CS changes or roll them back later
            // csSet is used for that.
            if (csSet.isEmpty()) {
                logger.debug("EMPTY csSet is found, skipping iteration");
                continue;
            }
            apd.changeCS(csSet, true);

            // Re-calculate risk now and create a recommendation
            RiskVector riskResponse = null;
            try {
                riskResponse = apd.calculateRisk(this.modelId, RiskCalculationMode.valueOf(riskMode));
                logger.debug("RiskResponse: {}", riskResponse);
                logger.debug("Overall risk: {}", riskResponse.getOverall());
                StateDTO state = apd.getState();

                RecommendationDTO recommendation = createRecommendation(csgList, csSet, state);

                if (this.report.getRecommendations() == null) {
                    report.setRecommendations(new ArrayList<>());
                }
                childNode.setRecommendation(recommendation);

            } catch (Exception e) {
                logger.warn("failed to get risk calculation, restore model");
                // restore model ...
                // TODO: restore model controls
                apd.changeCS(csSet, false);
                // raise exception since failed to run risk calculation
                throw new RuntimeException(e);
            }

            // check if risk has improved or teminate loop
            logger.debug("check for termination condition ({})", recCounter);
            if ((riskResponse != null) & (apd.compareOverallRiskToMedium(riskResponse.getOverall()))) {
                logger.info("Termination condition");
            } else {
                logger.debug("Risk is still higher than Medium");
                logger.info("Recalculate threat tree for a lower level ...");
                AttackTree tt = calcAttackTree("domain#RiskLevelLow");
                LogicalExpression nle = tt.attackMitigationCSG();
                this.applyCSGs(nle, childNode);
            }

            // undo CS changes in CS_set
            logger.debug("Undoing CS controls ({})", csSet.size());
            apd.changeCS(csSet, false);
            apd.calculateRisk(this.modelId, RiskCalculationMode.valueOf(riskMode));
        }

        logger.debug("return from iteration");

        return myNode;
    }

    private RecommendationDTO createRecommendation(List<String> csgList, Set<String> csSet, StateDTO state) {
        RecommendationDTO recommendation = new RecommendationDTO();
        recommendation.setIdentifier(this.recCounter++);

        List<ControlStrategyDTO> recCSGList = new ArrayList<>();
        for (String csgUri : csgList) {
            ControlStrategyDTO csgDto = new ControlStrategyDTO();
            csgDto.setUri(csgUri);
            csgDto.setDescription(apd.getCSGDescription(csgUri));
            recCSGList.add(csgDto);
            csgDto.setCategory(apd.hasExternalDependencies(csgUri) ? "Applicable" : "Conditional");
        }
        recommendation.setControlStrategies(recCSGList);

        List<ControlDTO> recControlList = new ArrayList<>();
        for (String ctrlUri : csSet) {
            ControlDTO ctrl = apd.fillControlDTO(ctrlUri);
            recControlList.add(ctrl);
        }
        recommendation.setControls(recControlList);
        recommendation.setState(state);

        logger.debug("Potential recommendation: {}", recommendation);

        return recommendation;
    }

    private void makeRecommendations(CSGNode node) {
        List<CSGNode> path = new ArrayList<CSGNode>();
        makeRecommendations(node, path);
    }

    private void makeRecommendations(CSGNode node, List<CSGNode> path) {
        // This method should not run more risk calculations, instead it will
        // try to use recommendations stored in nodes

        String rootCsgList;

        if (node.getCsgList().isEmpty()) {
            rootCsgList = "root";
        } else {
            rootCsgList = String.join(", ", node.getCsgList());
        }
        nodes.add(rootCsgList);

        logger.debug("MAKE RECOMMENDATIONS TREE: {}", node.getCsgList());

        path.add(node);

        if (!node.getChildren().isEmpty()) {
            for(CSGNode child : node.getChildren()) {
                links.add(String.join(", ", child.getCsgList()));
                makeRecommendations(child, path);
            }
        } else {
            List<String> csgList = new ArrayList<>();
            for (CSGNode pNode : path) {
                for (String csgUri : pNode.getCsgList()) {
                    csgList.add(csgUri);
                }
            }

            if (node.getRecommendation() != null) {
                logger.debug("adding cached path recommendation {}", node.getRecommendation().getIdentifier());
                report.getRecommendations().add(node.getRecommendation());
            }
        }
    }

    public RecommendationReportDTO recommendations() throws RuntimeException {

        logger.info("Recommendations core part (risk mode: {})", riskMode);

        try {

            // get initial risk state
            RiskVector riskResponse = apd.calculateRisk(this.modelId, RiskCalculationMode.valueOf(riskMode));
            apd.getState();

            StateDTO state = new StateDTO();
            state.setRisk(riskResponse.toString());
            report.setCurrent(state);

            AttackTree threatTree = calcAttackTree();

            // step: attackMitigationCSG?
            LogicalExpression attackMitigationCSG = threatTree.attackMitigationCSG();

            // step: rootNode?
            CSGNode rootNode = applyCSGs(attackMitigationCSG, new CSGNode());

            // step: makeRecommendations on rootNode?
            makeRecommendations(rootNode);

            List<RecommendationDTO> recommendations = report.getRecommendations() != null ? report.getRecommendations() : Collections.emptyList();
            logger.info("The Recommendations Report has: {} recommendations", recommendations.size());
            for (RecommendationDTO rec : recommendations) {
                logger.debug("  recommendation: {}", rec.getState().getRisk());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return report;
    }

}

