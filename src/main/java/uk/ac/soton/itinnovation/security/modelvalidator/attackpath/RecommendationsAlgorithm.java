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

import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;
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

public class RecommendationsAlgorithm {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationsAlgorithm.class);

    private AttackPathDataset apd;
    private IQuerierDB querier;
    private AttackTree threatTree;

    @Autowired
    private ModelObjectsHelper modelObjectsHelper;

    public RecommendationsAlgorithm(IQuerierDB querier) {

        this.querier = querier;

        final long startTime = System.currentTimeMillis();

        logger.debug("STARTING recommendations algortithm ...");

        // TODO might have to delay initialisation of the dataset until risk
        // mode is checked.
        apd = new AttackPathDataset(querier);

        List<String> msList = apd.filterMisbehaviours();
        logger.debug("MS LIST: {}", msList);

        final long endTime = System.currentTimeMillis();
        logger.info("RecommendationsAlgorithm.RecommendationsAlgorithm(IQuerierDB querier): execution time {} ms",
                endTime - startTime);

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

    public AttackTree calculateAttackTree(List<String> targetUris, String riskCalculationMode, boolean allPaths,
            boolean normalOperations) throws RuntimeException {

        logger.debug("calculate attack tree with isFUTURE: {}, allPaths: {}, normalOperations: {}", riskCalculationMode,
                allPaths, normalOperations);
        logger.debug("target URIs: {}", targetUris);

        checkRequestedRiskCalculationMode(riskCalculationMode);
                boolean isFutureRisk = apd.isFutureRisk(riskCalculationMode);

        AttackTree attackTree = null;

        try {
            final long startTime = System.currentTimeMillis();

            // calculate attack tree, allPath dictates one or two backtrace
            // runs which is represented in AttackTree as boolean shortestPath
            attackTree = new AttackTree(targetUris, isFutureRisk, !allPaths, apd);

            attackTree.stats();

            //attackTree.logicalExpressions();

            final long endTime = System.currentTimeMillis();
            logger.info("AttackPathAlgorithm.calculateAttackTree: execution time {} ms", endTime - startTime);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return attackTree;
    }

    /*
    private void calculateRisk() {
		try {
		    Progress validationProgress = modelObjectsHelper.getValidationProgressOfModel(model);
			querier.initForRiskCalculation();
			RiskCalculator rc = new RiskCalculator(querier);
			rc.calculateRiskLevels(RiskCalculationMode.FUTURE, false, new Progress(tester.getGraph("system")));
		} catch (Exception e) {
			logger.error("Exception thrown by risk level calculator", e);
		}
    }
    */

    private CSGNode applyCSGs(LogicalExpression le, CSGNode myNode) {
        logger.debug("applyCSGs");
        if (myNode == null) {
            myNode = new CSGNode();
        }

        // convert LE to DNF
        le.applyDNF(100);

        // convert from CSG logical expression to list of CSG options
        List<Expression> csgOptions = le.getListFromOr();
        logger.debug("list of options: {}", csgOptions.size());

        for (Expression csgOption : csgOptions) {
            logger.debug("examining CSG option {}", csgOption.getClass().getName());
            //logger.debug("children: {}", csgOption.getChildren().size());

            List<Variable> options = le.getListFromAnd(csgOption);
            List<String> csgList = new ArrayList<>();
            for (Variable va : options) {
                logger.debug("va -> {}, {}", va, va.getClass().getName());
                csgList.add(va.toString());
            }

            CSGNode childNode = new CSGNode(csgList);
            myNode.addChild(childNode);

            Set<String> csSet = new HashSet<>();
            for (String csg : csgList) {
                for (String cs : apd.getCsgInactiveControlSets(csg)) {
                    csSet.add(cs);
                }
            }
            logger.debug("CS set for CSG_option {}", csgOption);
            logger.debug(" CS set: {}", csSet);

            // apply all CS in the CS_set
            // TODO: I need to keep track of CS changes or roll them back later
            apd.applyCS(csSet);

            // Re-calculate risk now and 


        }

        return new CSGNode();
    }

    public void recommendations(List<String> targetUris, String riskCalculationMode, boolean allPaths,
            boolean normalOperations) throws RuntimeException {
        logger.debug("Recommendations core part");
        try {
            threatTree = calculateAttackTree(targetUris,
                    riskCalculationMode, allPaths, normalOperations);

            // step: attackMitigationCSG?
            LogicalExpression attackMitigationCSG = threatTree.attackMitigationCSG();

            // step: rootNode?
            CSGNode rootNode = applyCSGs(attackMitigationCSG, new CSGNode());

            // step: makeRecommendations?

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

