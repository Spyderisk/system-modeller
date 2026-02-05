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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;
import uk.ac.soton.itinnovation.security.modelquerier.IQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ModelDB;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.Graph;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.TreeJsonDoc;

public class AttackPathAlgorithm {
    private static final Logger logger = LoggerFactory.getLogger(AttackPathAlgorithm.class);

    private AttackPathDataset apd;
    private IQuerierDB querier;

	private Integer attackPathTimeoutSecs;

    public AttackPathAlgorithm(IQuerierDB querier, Integer attackPathTimeoutSecs) {

        this.querier = querier;
        this.attackPathTimeoutSecs = attackPathTimeoutSecs;

        final long startTime = System.currentTimeMillis();

        logger.info("STARTING Shortest Path Attack algortithm ...");
        
        apd = new AttackPathDataset(querier);

        final long endTime = System.currentTimeMillis();
        logger.info("AttackPathAlgorithm.AttackPathAlgorithm(IQuerierDB querier): execution time {} ms",
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
        logger.debug("Checking submitted list of target URIs: {}", targetUris);

        // Check if the list is null or empty
        if (targetUris == null || targetUris.isEmpty()) {
            logger.warn("The list of target URIs is null or empty.");
            return false;
        }

        if (!apd.checkMisbehaviourList(targetUris)) {
            logger.error("shortest path, target MS URI not valid");
            return false;
        }
        return true;
    }

    public TreeJsonDoc calculateAttackTreeDoc(List<String> targetUris, String riskCalculationMode, boolean allPaths,
            boolean normalOperations) throws RuntimeException {

        logger.debug("calculate attack tree with isFUTURE: {}, allPaths: {}, normalOperations: {}", riskCalculationMode,
                allPaths, normalOperations);
        logger.debug("target URIs: {}", targetUris);

        checkRequestedRiskCalculationMode(riskCalculationMode);

        boolean isFutureRisk = apd.isFutureRisk(riskCalculationMode);

        TreeJsonDoc doc = null;
        try {
            final long startTime = System.currentTimeMillis();
            Integer maxSecs = this.attackPathTimeoutSecs;
            long maxEndTime;

            // Determine end time for attack path (i.e. after which no further iterations will be completed)
            if (maxSecs != null) {
                maxEndTime = startTime + maxSecs * 1000;
            }
            else {
                logger.warn("No attackpath.timeout.secs property set. Not setting timeout...");
                maxEndTime = Long.MAX_VALUE;
            }

            // calculate attack tree, allPath dictates one or two backtrace
            // runs which is represented in AttackTree as boolean shortestPath
            AttackTree attackTree = new AttackTree(targetUris, isFutureRisk, !allPaths, apd, maxEndTime);

            doc = attackTree.calculateTreeJsonDoc(allPaths, normalOperations);

            this.printJsonDoc(doc);

            attackTree.stats();

            final long endTime = System.currentTimeMillis();
            logger.info("AttackPathAlgorithm.calculateAttackTreeDoc: execution time {} ms", endTime - startTime);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return doc;
    }

    public void printJsonDoc(TreeJsonDoc tree) {

        logger.debug("*****************************************");
        logger.debug("P r i n t   J S O N   D o c   N o d e s :");
        logger.debug("*****************************************");

        for (String targetMS : tree.getGraphs().keySet()) {
            logger.debug("TARGET GRAPH for MS: {}", targetMS.substring(7));

            Graph graph = tree.getGraphs().get(targetMS);

            logger.debug("GRAPH SUMMARY for {}", targetMS.substring(7));
            logger.debug("   ├──> threats......: {}", graph.getThreats().size());
            logger.debug("   ├──> misbehaviours: {}", graph.getMisbehaviours().size());
            logger.debug("   ├──> twas.........: {}", graph.getTwas().size());
            logger.debug("   └──> links........: {}", graph.getLinks().size());
        }
    }

}
