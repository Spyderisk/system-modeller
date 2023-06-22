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
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.TreeJsonDoc;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.Graph;

import java.util.List;

public class AttackPathAlgorithm {
    private static final Logger logger = LoggerFactory.getLogger(AttackPathAlgorithm.class);

    private AttackPathDataset apd;

    public AttackPathAlgorithm(IQuerierDB querier) {

        final long startTime = System.currentTimeMillis();

        logger.debug("STARTING Shortest Path Attack algortithm ...");

        apd = new AttackPathDataset(querier);

        final long endTime = System.currentTimeMillis();
        logger.info("AttackPathAlgorithm.AttackPathAlgorithm(IQuerierDB querier): execution time {} ms", endTime - startTime);

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

    public AttackTree calculateAttack(List<String> targetUris,
            boolean allPaths, boolean normalOperations) throws RuntimeException {

        logger.debug("calculate attack tree with allPaths: {}, normalOperations: {}",
                allPaths, normalOperations);
        logger.debug("target URIs: {}", targetUris);

        AttackTree attackTree;

        try {
            final long startTime = System.currentTimeMillis();

            // calculate attack tree, allPath dictates one or two backtrace
            // AttackTree is initialised with FUTURE risk mode enabled
            attackTree = new AttackTree(targetUris, true, !allPaths, apd);

            final long endTime = System.currentTimeMillis();
            logger.info("AttackPathAlgorithm.calculateAttackTree: execution time {} ms", endTime - startTime);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return attackTree;
    }


    public TreeJsonDoc calculateAttackTreeDoc(List<String> targetUris,
            boolean allPaths, boolean normalOperations) throws RuntimeException {

        logger.debug("calculate attack tree with allPaths: {}, normalOperations: {}",
                allPaths, normalOperations);
        logger.debug("target URIs: {}", targetUris);

        TreeJsonDoc doc = null;
        try {
            final long startTime = System.currentTimeMillis();

            // calculate attack tree, allPath dictates one or two backtrace
            // AttackTree is initialised with FUTURE risk mode enabled
            AttackTree attackTree = new AttackTree(targetUris, true, !allPaths, apd);

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

        for(String targetMS : tree.getGraphs().keySet()) {
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

