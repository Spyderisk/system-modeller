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

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.AdditionalPropertyDTO;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.AssetDTO;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.ConsequenceDTO;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.ControlDTO;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.ControlStrategyDTO;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.RecommendationDTO;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.RecommendationReportDTO;
//import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.RiskVectorDTO;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.StateDTO;

import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;
import uk.ac.soton.itinnovation.security.model.system.RiskVector;
import uk.ac.soton.itinnovation.security.modelquerier.IQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ModelDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ControlSetDB;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.LogicalExpression;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;

import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.And;
import com.bpodgursky.jbool_expressions.Or;
import com.bpodgursky.jbool_expressions.Not;
import com.bpodgursky.jbool_expressions.Variable;
import com.bpodgursky.jbool_expressions.parsers.ExprParser;
import com.bpodgursky.jbool_expressions.rules.RuleSet;

import uk.ac.soton.itinnovation.security.modelvalidator.Progress;
import uk.ac.soton.itinnovation.security.modelvalidator.RiskCalculator;

@Component
public class RecommendationsAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationsAlgorithm.class);

    private AttackPathDataset apd;
    private IQuerierDB querier;
    private String modelId;
    private int recCounter = 0;
    private RecommendationReportDTO report;
    private String riskMode = "FUTURE";

    private List<String> nodes = new ArrayList<>();
    private List<String> links = new ArrayList<>();

    @Autowired
    private ModelObjectsHelper modelObjectsHelper;

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
        logger.debug("model info: {}", model);

        RiskCalculationMode modelRiskCalculationMode;
        RiskCalculationMode requestedMode;

        try {
            modelRiskCalculationMode = RiskCalculationMode.valueOf(model.getRiskCalculationMode());
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
        boolean retVal = true;
        logger.debug("Checking submitted list of target URIs: {}", targetUris);
        if (!apd.checkMisbehaviourList(targetUris)) {
            logger.error("shortest path, target MS URI not valid");
            retVal = false;
        }
        return retVal;
    }

    public AttackTree calcAttackTree() {
        List<String> targetMSUris = apd.filterMisbehaviours();
        logger.info("TARGET MS: {}", targetMSUris);

        return calculateAttackTree(targetMSUris, riskMode, true, true);
    }

    public AttackTree calculateAttackTree(List<String> targetUris, String riskCalculationMode, boolean allPaths,
                                        boolean normalOperations) throws RuntimeException {
        logger.debug("calculate attack tree with isFUTURE: {}, allPaths: {}, normalOperations: {}", riskCalculationMode,
                allPaths, normalOperations);
        logger.debug("target URIs: {}", targetUris);

        checkRequestedRiskCalculationMode(riskCalculationMode);

        return calculateAttackTreeInternal(targetUris, riskCalculationMode, !allPaths);
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

   // TODO: remove this method
   private CSGNode applyCSGsOUT(LogicalExpression le, CSGNode myNode) {
        logger.debug("applyCSGs");
        if (myNode == null) {
            myNode = new CSGNode();
        }

        // convert LE to DNF
        le.applyDNF(100);

        // convert from CSG logical expression to list of CSG options
        List<Expression> csgOptions = le.getListFromOr();
        logger.debug("list of OR CSG options: {}", csgOptions.size());
        for (Expression csgOption : csgOptions) {
            logger.debug("└──> {}", csgOption);
        }
        logger.debug("eXit");
        return null;
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
            logger.debug("└──> {}", csSet);

            // apply all CS in the CS_set
            // TODO: I need to keep track of CS changes or roll them back later
            // csSet is used for that.
            apd.applyCS(csSet, true);

            // Re-calculate risk now and create a recommendation
            RiskVector riskResponse = null;
            try {
                riskResponse = apd.calculateRisk(this.modelId);
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
                logger.debug("undo CS set");
                apd.applyCS(csSet, false);
                // raise exception since failed to run risk calculation
                throw new RuntimeException(e);
            }

            // check if risk has improved or teminate loop
            logger.debug("check for termination condition ({})", recCounter);
            if ((riskResponse != null) & (apd.compareOverallRiskToMedium(riskResponse.getOverall()))) {
                logger.info("Termination condition");
            } else {
                logger.info("Recalculate threat tree ...");
                AttackTree tt = calcAttackTree();
                LogicalExpression nle = tt.attackMitigationCSG();
                this.applyCSGs(nle, childNode);
            }

            // undo CS changes in CS_set
            logger.debug("undo CS set");
            apd.applyCS(csSet, false);
        }

        logger.debug("return from iteration");

        return myNode;
    }

    private RecommendationDTO createRecommendation(List<String> csgList, Set<String> csSet, StateDTO state) {
        RecommendationDTO recommendation = new RecommendationDTO();
        recommendation.setIdentifier(this.recCounter++);
        recommendation.setCategory("unknown");

        List<ControlStrategyDTO> recCSGList = new ArrayList<>();
        for (String csgUri : csgList) {
            ControlStrategyDTO csgDto = new ControlStrategyDTO();
            csgDto.setUri(csgUri);
            csgDto.setDescription(apd.getCSGDescription(csgUri));
            recCSGList.add(csgDto);
        }
        recommendation.setControlStrategies(recCSGList);

        List<ControlDTO> recControlList = new ArrayList<>();
        for (String ctrlUri : csSet) {
            ControlDTO ctrl = new ControlDTO();
            ctrl.setUri(ctrlUri);
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

            logger.debug("adding cached path recommendation {}", node.getRecommendation().getIdentifier());

            if (node.getRecommendation() != null) {
                report.getRecommendations().add(node.getRecommendation());
            }
        }
    }

    public void recommendations(boolean allPaths, boolean normalOperations) throws RuntimeException {

        logger.debug("Recommendations core part (risk mode: {})", riskMode);

        try {

            // get initial risk state
            RiskVector riskResponse = apd.calculateRisk(this.modelId);
            apd.getState();

            StateDTO state = new StateDTO();
            state.setRisk(riskResponse.toString());
            report.setCurrent(state);

            //AttackTree threatTree = calculateAttackTree(targetUris, riskCalculationMode, allPaths, normalOperations);
            AttackTree threatTree = calcAttackTree();

            // step: attackMitigationCSG?
            LogicalExpression attackMitigationCSG = threatTree.attackMitigationCSG();

            // step: rootNode?
            CSGNode rootNode = applyCSGs(attackMitigationCSG, new CSGNode());

            // step: makeRecommendations on rootNode?
            makeRecommendations(rootNode);

            //logger.debug("RECOMMENDATIONS REPORT: {}", report);
            logger.debug("REPORT has: {} recommendations", report.getRecommendations().size());
            for (RecommendationDTO rec : report.getRecommendations()) {
                logger.debug("  recommendation: {}", rec.getState().getRisk());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

