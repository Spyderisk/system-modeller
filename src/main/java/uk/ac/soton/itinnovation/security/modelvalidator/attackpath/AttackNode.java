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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.Variable;

public class AttackNode {
    private static final Logger logger = LoggerFactory.getLogger(AttackNode.class);

    private static int instanceCount = 0; // Static counter variable

    private AttackPathDataset apd;
    private String uri = "";
    private AttackTree nodes;
    private boolean isTargetMS = false;
    private int id = -1;

    private LogicalExpression controlStrategies = null;
    private LogicalExpression controls = null;

    private Expression<String> uriSymbol = null;

    private List<String> allDirectCauseUris = null;

    private Set<String> directCauseUris = new HashSet<>();
    private Set<String> directEffectUris = new HashSet<>();

    private Map<LogicalExpression, Integer> maxDistanceFromRootByCause = new HashMap<>();
    private Map<LogicalExpression, Integer> minDistanceFromRootByCause = new HashMap<>();
    private Map<String, Integer> maxDistanceFromTargetByTarget = new HashMap<>();

    private boolean cannotBeCaused = false; // Flag for nodes that cannot be caused because they cause themselves

    private boolean notACause = true; // Assume the node is not a cause no matter what path taken, unless we find otherwise

    private LogicalExpression rootCause;

    // cached results:
    private List<InnerResult> causeResults = new ArrayList<>();
    private List<InnerResult> noCauseResults = new ArrayList<>();

    // counters to see what's going on with the caching
    private int visits = 0;
    private int noCauseVisits = 0;
    private int causeVisits = 0;
    private int cacheHitVisits = 0;

    // LogicalExpressions to record the tree data:
    private LogicalExpression attackTreeMitigationCS = null;
    private LogicalExpression threatTreeMitigationCS = null;
    private LogicalExpression attackTreeMitigationCSG = null;
    private LogicalExpression threatTreeMitigationCSG = null;
    private LogicalExpression attackTree = null;
    private LogicalExpression threatTree = null;

    private static final String ROOT_CAUSE = "root_cause";
    private static final String THREAT_MITIGATION_CS = "threat_mitigation_cs";
    private static final String THREAT_MITIGATION_CSG = "threat_mitigation_csg";
    private static final String THREAT_TREE = "threat_tree";
    private static final String ATTACK_TREE = "attack_tree";
    private static final String ATTACK_MITIGATION_CS = "attack_mitigation_cs";
    private static final String ATTACK_MITIGATION_CSG = "attack_mitigation_csg";

    private class InnerResult {
        Set<String> loopbackNodeUris = new HashSet<>();
        Set<String> allCauseUris = new HashSet<>();
        int minDistance = 0;
        int maxDistance = 0;
        Map<String, LogicalExpression> data = new HashMap<>();

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{{{");
            sb.append(loopbackNodeUris);
            sb.append(", ");
            sb.append(allCauseUris);
            sb.append(", ");
            sb.append("data:");
            sb.append(data);
            sb.append("}}}");
            return sb.toString();
        }

        public LogicalExpression getData(String key) {
            return this.data.get(key);
        }

        public void putData(String key, LogicalExpression value) {
            this.data.put(key, value);
        }

        public void setLoopbackNodeUris(Set<String> nodeUris) {
            this.loopbackNodeUris = nodeUris;
        }

        public Set<String> getLoopbackNodeUris() {
            return this.loopbackNodeUris;
        }

        public void setAllCauseUris(Set<String> allCauseUris) {
            this.allCauseUris = allCauseUris;
        }

        public Set<String> getAllCauseUris() {
            return this.allCauseUris;
        }

        public void setMinDistance(int distance) {
            this.minDistance = distance;
        }

        public int getMinDistance() {
            return this.minDistance;
        }

        public void setMaxDistance(int distance) {
            this.maxDistance = distance;
        }

        public int getMaxDistance() {
            return this.maxDistance;
        }
    };

    public AttackNode(String uri, AttackPathDataset apd, AttackTree nodes, int id) {

        instanceCount++;

        this.apd = apd;
        this.uri = uri;
        this.nodes = nodes;
        this.isTargetMS = false;
        this.id = id;

        /*
         * if the containing AttackTree defines a bound on the nodes to explore then we apply it here by discarding parent not in the bounding_urirefs set
         */
        this.allDirectCauseUris = this.getAllDirectCauseUris();

        if (!this.nodes.getBoundingUriRefs().isEmpty()) {
            this.allDirectCauseUris.retainAll(this.nodes.getBoundingUriRefs());
        }

        this.controlStrategies = this.getControlStrategies();
        this.controls = this.getControls();
        this.uriSymbol = this.makeSymbol(uri);
    }

    public LogicalExpression getAttackTreeMitigationCSG() {
        return this.attackTreeMitigationCSG;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uri);
    }

    @Override
    public boolean equals(Object obj) {
        AttackNode an = (AttackNode) obj;
        if (this.uri.equals(an.getUri())) {
            return true;
        } else {
            return false;
        }
    }

    public void setMaxDistanceFromTargetByTarget(String uri, int value) {
        this.maxDistanceFromTargetByTarget.put(uri, value);
    }

    public int getMaxDistanceFromTargetByTarget(String uri) {
        return this.maxDistanceFromTargetByTarget.getOrDefault(uri, -1);
    }

    public int maxDistanceFromTarget() {
        return Collections.max(this.maxDistanceFromTargetByTarget.values());
    }

    public int maxDistanceFromRoot() {
        return Collections.max(this.maxDistanceFromRootByCause.values());
    }

    public int minDistanceFromRoot() {
        return Collections.min(this.maxDistanceFromRootByCause.values());
    }

    public int getVisits() {
        return this.visits;
    }

    public String getVisitsStats() {
        StringBuffer sb = new StringBuffer();
        sb.append(" Visits: " + this.visits);
        sb.append(" noCauseV: " + this.noCauseVisits);
        sb.append(" causeV: " + this.causeVisits);
        sb.append(" cacheHitV: " + this.cacheHitVisits);
        return sb.toString();
    }

    public String toString(String pad) {
        StringBuffer sb = new StringBuffer();
        sb.append(pad + " ID(");
        sb.append(this.id);
        sb.append(") --> ");
        sb.append(this.uri.substring(7));
        sb.append(" distance: ");
        sb.append(this.minDistanceFromRoot());

        return sb.toString();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("\nNode (");
        sb.append(this.id);
        sb.append(") URI: ");
        sb.append(this.uri);
        sb.append("\t is Threat: ");
        sb.append(this.isThreat());
        sb.append("\n  is Normal Op: ");
        sb.append(this.isNormalOp());
        sb.append("\n  distance from root: ");
        sb.append(this.minDistanceFromRoot());
        sb.append("\n  direct causes: ");
        sb.append(this.allDirectCauseUris);
        sb.append("\n  csgs logical expressions: ");
        sb.append(this.controlStrategies);
        sb.append("\n  controls logical expressions: ");
        sb.append(this.controls);

        return sb.toString();
    }

    public LogicalExpression getControlStrategies() {
        if (!this.isThreat()) {
            return null;
        }

        List<String> csgs = this.apd.getThreatInactiveCSGs(this.uri, this.nodes.getIsFutureRisk());

        List<Object> csgSymbols = new ArrayList<>();
        for (String csgUri : csgs) {
            csgSymbols.add(this.makeSymbol(csgUri));
        }

        LogicalExpression leCSG = new LogicalExpression(this.apd, csgSymbols, false);

        return leCSG;
    }

    public LogicalExpression getControls() {
        if (!this.isThreat()) {
            return null;
        }

        /*
         * The LogicalExpression for the controls that will mitigate a threat is: OR(the control strategy expressions)
         *
         * The LogicalExpression for a control strategy is: AND(the control strategy's inactive controls)
         *
         * So we will end up with something like: OR(AND(c1, c1), AND(c3), AND(c1, c4))
         */

        Set<String> csgUris = this.apd.getThreatControlStrategyUris(this.uri, this.nodes.getIsFutureRisk());

        List<LogicalExpression> leCSGs = new ArrayList<>();

        for (String csgUri : csgUris) {
            List<String> csUris = this.apd.getCsgInactiveControlSets(csgUri);
            List<Object> csSymbols = new ArrayList<>();
            for (String csUri : csUris) {
                csSymbols.add(this.makeSymbol(csUri));
            }
            leCSGs.add(new LogicalExpression(this.apd, csSymbols, true));
        }
        return new LogicalExpression(this.apd, new ArrayList<Object>(leCSGs), false);
    }

    public int getId() {
        return this.id;
    }

    public String getUri() {
        return this.uri;
    }

    public void setIsTargetMS(boolean value) {
        this.isTargetMS = value;
    }

    public boolean getIsTargetMS() {
        return this.isTargetMS;
    }

    public void setNotACause(boolean value) {
        this.notACause = value;
    }

    public boolean getNotACause() {
        return this.notACause;
    }

    public List<String> getAllDirectCauseUris() {
        if (this.isThreat()) {
            return this.apd.getThreatDirectCauseUris(this.uri);
        }
        return this.apd.getMisbehaviourDirectCauseUris(this.uri);
    }

    private Expression<String> makeSymbol(String uri) {
        // TODO need to find equivalent of symbol->algebra.definition
        return Variable.of(uri);
    }

    /**
     * Performs a backtrace from the current AttackNode to its ancestors
     *
     * @param cPath        the current path to the AttackNode being traced
     * @param computeLogic compute the logical result of the backtrace
     * @return an object containing the results of the backtrace
     * @throws TreeTraversalException if an error occurs during traversal of the AttackNode tree
     * @throws Exception              if an unexpected error occurs
     */
    public InnerResult backtrace(Set<String> cPath, boolean computeLogic) throws TreeTraversalException, Exception {

        Set<String> currentPath = new HashSet<>();

        if (!cPath.isEmpty()) {
            currentPath = new HashSet<>(cPath);
        }

        // TODO debug only:
        //if (this.uri.substring(7).equals("MS-InService-a0826156")) {
        //    logger.debug("DDD: currentPath: {}, self all: {}", currentPath, this.allDirectCauseUris);
        //}

        currentPath.add(this.uri);
        logger.debug(String.format("%1$"+ currentPath.size() +"s", "") +
                " BACKTRACE for: " + this.uri.substring(7) + " (nodeID:" + this.id + ") "+
                " current path length: " + (currentPath.size()-1) +
                " all direct cause uris: " + this.allDirectCauseUris.size());

        this.visits += 1;

        // check the cached results
        if (this.cannotBeCaused) {
            // if this node is unreachable regardless of path taken to it
            // TODO: this can just be donw with zero-length loopback_node_uris
            // results in no_cause_results
            this.cacheHitVisits += 1;
            throw new TreeTraversalException(new HashSet<String>());
        }

        for (InnerResult result : this.noCauseResults) {
            // if all of the loopback nodes in this node's causation tree are
            // on the current path
            Set<String> intersection = new HashSet<>(result.getLoopbackNodeUris());
            intersection.retainAll(currentPath);
            if (intersection.size() == result.getLoopbackNodeUris().size()) {
                this.cacheHitVisits += 1;
                logger.debug(String.format("%1$"+ currentPath.size() +"s", "") +
                        " Cache hit, no cause");
                throw new TreeTraversalException(result.getLoopbackNodeUris());
            }
        }

        List<InnerResult> validCaches = new ArrayList<>();

        for (InnerResult result : this.causeResults) {
            // if none of the causes of this node are on the path to the node
            Set<String> intersection = new HashSet<>(result.getAllCauseUris());
            intersection.retainAll(currentPath);
            if (intersection.isEmpty()) {
                // then the cached cause will still work
                validCaches.add(result);
            }
        }

        if (!validCaches.isEmpty()) {
            boolean useCache = true;
            InnerResult res = null;
            for (InnerResult result : validCaches) {
                res = result;
                Set<String> intersection = new HashSet<>(result.getLoopbackNodeUris());
                intersection.retainAll(currentPath);
                if (intersection.size() == result.getLoopbackNodeUris().size()) {
                    // then the current path has all the loopback_nodes of the
                    // cahced result so would behave the same
                    continue;
                } else {
                    // then in this case there is more to explore
                    logger.debug(String.format("%1$"+ currentPath.size() +"s", "") +
                            " Cache hit: node can be cause, but more to explore");
                    useCache = false;
                    break;
                }
            }

            if (useCache) {
                this.cacheHitVisits += 1;
                logger.debug(String.format("%1$"+ currentPath.size() +"s", "") +
                        " Cache hit, node can be caused, cache can be used");
                return res;
            }
        }

        // store data from this visit to the node
        List<Integer> parentMinDistancesFromRoot = new ArrayList<>();
        List<Integer> parentMaxDistancesFromRoot = new ArrayList<>();

        List<Object> parentRootCauses = new ArrayList<>();
        List<LogicalExpression> parentAttackMitigationsCS = new ArrayList<>();
        List<LogicalExpression> parentThreatMitigationsCS = new ArrayList<>();
        List<LogicalExpression> parentAttackMitigationsCSG = new ArrayList<>();
        List<LogicalExpression> parentThreatMitigationsCSG = new ArrayList<>();
        List<Object> parentAttackTrees = new ArrayList<>();
        List<Object> parentThreatTrees = new ArrayList<>();
        Set<String> validParentUris = new HashSet<>();
        Set<String> loopbackNodeUris = new HashSet<>(); // nodes that cause a failure because they are the current path
        Set<String> allCauseUris = new HashSet<>();

        int tmpMinDistanceFromRoot = -1;
        int tmpMaxDistanceFromRoot = -1;
        LogicalExpression tmpRootCause = null;

        LogicalExpression attackMitigatedByCS = null;
        LogicalExpression threatMitigatedByCS = null;
        LogicalExpression attackMitigatedByCSG = null;
        LogicalExpression threatMitigatedByCSG = null;
        LogicalExpression bsAttackTree = null;
        LogicalExpression bsThreatTree = null;

        boolean outerSuccess = true; // need this for try->except->ELSE* python equivalent
        try {

            if (this.allDirectCauseUris.isEmpty()) {
                logger.debug(String.format("%1$"+ currentPath.size() +"s", "") +
                        " No direct causes");

                // This will be top of tree misbehaviours (normal-op, external
                // cause). Not root causes as they have parents in normal-ops.
                // TODO: can this just move to the end of the function?

                tmpMinDistanceFromRoot = -1;
                tmpMaxDistanceFromRoot = -1;

                List<Object> tmpObjList = new ArrayList<Object>();
                tmpObjList.add(this.makeSymbol(this.uri));

                tmpRootCause = new LogicalExpression(this.apd, tmpObjList, true);

                if (this.isThreat()) {
                    String err = "There should not be a threat with no parents: " + this.uri;
                    for (String dcUri : getAllDirectCauseUris()) {
                        logger.warn("Direct cause URI --> {}", dcUri);
                    }
                    logger.error(err);
                    throw new Exception(err); // TODO: put error in exception and choose a better Exception class
                } else {

                    attackMitigatedByCS = null;
                    threatMitigatedByCS = null;
                    attackMitigatedByCSG = null;
                    threatMitigatedByCSG = null;
                    bsAttackTree = null;
                    bsThreatTree = null;
                }

            } else if (this.isThreat()) {

                Set<String> intersection = new HashSet<>(this.allDirectCauseUris);
                intersection.retainAll(currentPath);
                if (intersection.size() > 0) {
                    // For a threat we require all parents.
                    // If even one is on the current path then the threat is triggered by its own consequence which is useless.
                    List<String> consequence = new ArrayList<>();
                    for (String item : intersection) {
                        consequence.add(item.substring(7));
                    }
                    logger.debug(String.format("%1$"+ currentPath.size() +"s", "") +
                            " threat is dependent on its own consequence: " + consequence);
                    throw new TreeTraversalException(intersection);
                }

                List<String> sortedCauses = new ArrayList<>(this.allDirectCauseUris);
                Collections.sort(sortedCauses);
                logger.debug(String.format("%1$"+ currentPath.size() +"s", "") +
                        " " + sortedCauses.size() + " direct causes of threat");

                logger.debug(String.format("%1$"+ currentPath.size() +"s", "") +
                        " └─>" + sortedCauses);

                for (String parentUri : sortedCauses) {
                    AttackNode parent = this.nodes.getOrCreateNode(parentUri);

                    boolean success = true; // need this for try->except->ELSE* python equivalent

                    InnerResult pResult = new InnerResult();
                    try {
                        pResult = parent.backtrace(currentPath, computeLogic);
                    } catch (TreeTraversalException error) {
                        success = false;
                        loopbackNodeUris.addAll(error.getLoopbackNodeUris());
                        throw new TreeTraversalException(loopbackNodeUris);
                    }
                    if (success == true) { // *equivalent to try->catch->ELSE
                        // TODO: this clause only needs executing (for each
                        // parent) if allthe threat's parents are valid.

                        // We could collect all the p_results from the try
                        // block and then iterate through them instead of
                        // executing immediately.

                        validParentUris.add(parentUri);
                        loopbackNodeUris.addAll(pResult.getLoopbackNodeUris());
                        allCauseUris.addAll(pResult.getAllCauseUris());

                        // if (this.isNormalOp() == parent.isNormalOp()) {
                        if (Objects.equals(this.isNormalOp(), parent.isNormalOp()) && !parent.isExternalCause()) {
                            parentMinDistancesFromRoot.add(pResult.getMinDistance());
                            parentMaxDistancesFromRoot.add(pResult.getMaxDistance());
                            parentRootCauses.add(pResult.getData(ROOT_CAUSE));
                        }

                        if (computeLogic) {
                            parentThreatMitigationsCS.add(pResult.getData(THREAT_MITIGATION_CS)); // Entire path
                            parentThreatMitigationsCSG.add(pResult.getData(THREAT_MITIGATION_CSG)); // Entire path
                            parentThreatTrees.add(pResult.getData(THREAT_TREE));
                            if (!parent.isNormalOp() && !parent.isExternalCause()) {
                                parentAttackMitigationsCS.add(pResult.getData(ATTACK_MITIGATION_CS));
                                parentAttackMitigationsCSG.add(pResult.getData(ATTACK_MITIGATION_CSG));
                                parentAttackTrees.add(pResult.getData(ATTACK_TREE));
                            }
                        }
                    }
                }

                if (parentRootCauses.isEmpty()) {
                    // then this is a root cause threat
                    parentMinDistancesFromRoot = new ArrayList<>();
                    parentMinDistancesFromRoot.add(-1);

                    parentMaxDistancesFromRoot = new ArrayList<>();
                    parentMaxDistancesFromRoot.add(-1);

                    List<Object> tmpObjList = new ArrayList<Object>();
                    tmpObjList.add(this.makeSymbol(this.uri));
                    parentRootCauses.add(new LogicalExpression(this.apd, tmpObjList, true));
                }

                // The root cause of a threat is all (AND) of the rout
                // causes of its parents
                tmpRootCause = new LogicalExpression(this.apd, parentRootCauses, true);

                // The distance from a root cause therefore is the maximum
                // of the parent distances +1
                tmpMinDistanceFromRoot = Collections.max(parentMinDistancesFromRoot) + 1;
                tmpMaxDistanceFromRoot = Collections.max(parentMaxDistancesFromRoot) + 1;

                logger.debug(String.format("%1$"+ currentPath.size() +"s", "") +
                        " Finished looking at threat causes (nodeID:" + this.id + ")");

                if (computeLogic == true) {
                    // The attack/threat tree is
                    // AND(
                    // the threat itself
                    // all the parent threat tree
                    // )
                    if (!this.isNormalOp()) {
                        // if this threat (self) is on the attack path then
                        // it can inself be a mitigation on the attack_path
                        parentAttackTrees.add(this.uriSymbol);
                    }

                    bsAttackTree = new LogicalExpression(this.apd, parentAttackTrees, true);

                    // All threats are on the threat path
                    parentThreatTrees.add(this.uriSymbol);
                    threatTree = new LogicalExpression(this.apd, parentThreatTrees, true);

                    /*
                     * A threat can be mitigated by OR( inactive control strategies located at itself mitigations of any of its parents )
                     */
                    if (!this.isNormalOp()) {
                        // If this threat is on the attackpath then it can
                        // itself be a mitigation on the attack_path
                        parentAttackMitigationsCS.add(this.controls);
                        parentAttackMitigationsCSG.add(this.controlStrategies);
                    }

                    // All threats are a mitigation of the complete threat
                    // path
                    parentThreatMitigationsCS.add(this.controls);
                    parentThreatMitigationsCSG.add(this.controlStrategies);
                    //logger.debug("PTM_CSG1: {}", this.controlStrategies);

                    attackMitigatedByCS = new LogicalExpression(this.apd,
                            new ArrayList<Object>(parentAttackMitigationsCS), false);
                    threatMitigatedByCS = new LogicalExpression(this.apd,
                            new ArrayList<Object>(parentThreatMitigationsCS), false);
                    attackMitigatedByCSG = new LogicalExpression(this.apd,
                            new ArrayList<Object>(parentAttackMitigationsCSG), false);
                    threatMitigatedByCSG = new LogicalExpression(this.apd,
                            new ArrayList<Object>(parentThreatMitigationsCSG), false);
                }
            } else {
                // we are a misbehaviour with direct causes
                loopbackNodeUris = new HashSet<>(this.allDirectCauseUris);
                //logger.debug("allDirectCauseURIs: {}", loopbackNodeUris);
                //logger.debug("current path: {}", currentPath);
                loopbackNodeUris.retainAll(currentPath);
                //logger.debug("allDirectCauseURIsB: {}", loopbackNodeUris);

                List<String> sortedCauses = new ArrayList<>(allDirectCauseUris);
                //logger.debug("sortedCauses: {}", sortedCauses);
                sortedCauses.removeAll(currentPath);
                //logger.debug("sortedCausesA: {}", sortedCauses);
                Collections.sort(sortedCauses);
                //logger.debug("sortedCausesB: {}", sortedCauses);

                logger.debug(String.format("%1$"+ currentPath.size() +"s", "") +
                        " " + sortedCauses.size() + " direct causes of MS");

                logger.debug(String.format("%1$"+ currentPath.size() +"s", "") +
                        " └─>" + sortedCauses);

                for (String parentUri : sortedCauses) {
                    AttackNode parent = this.nodes.getOrCreateNode(parentUri);

                    boolean success = true; // need this for try->except->else python equivalent

                    InnerResult pResult = new InnerResult();

                    try {
                        pResult = parent.backtrace(currentPath, computeLogic);
                        //logger.debug("pResult({}): {}", this.instanceCount, pResult);
                    } catch (TreeTraversalException error) {
                        success = false;
                        loopbackNodeUris.addAll(error.getLoopbackNodeUris());
                    }
                    if (success == true) {
                        validParentUris.add(parentUri);
                        loopbackNodeUris.addAll(pResult.getLoopbackNodeUris());
                        allCauseUris.addAll(pResult.getAllCauseUris());
                        parentMinDistancesFromRoot.add(pResult.getMinDistance());
                        parentMaxDistancesFromRoot.add(pResult.getMaxDistance());
                        parentRootCauses.add(pResult.getData(ROOT_CAUSE));

                        if (computeLogic) {
                            parentThreatMitigationsCS.add(pResult.getData(THREAT_MITIGATION_CS)); // Entire path
                            parentThreatMitigationsCSG.add(pResult.getData(THREAT_MITIGATION_CSG)); // Entire path
                            parentThreatTrees.add(pResult.getData(THREAT_TREE));
                            if (!parent.isNormalOp()) {
                                parentAttackMitigationsCS.add(pResult.getData(ATTACK_MITIGATION_CS));
                                parentAttackMitigationsCSG.add(pResult.getData(ATTACK_MITIGATION_CSG));
                                parentAttackTrees.add(pResult.getData(ATTACK_TREE));
                                //logger.debug("SSS({}): p_attack_tree {}", this.instanceCount, pResult.getData(ATTACK_TREE));
                            }
                        }
                    }
                }

                if (validParentUris.isEmpty()) {
                    // Then all parents have thrown exceptions or were on the
                    // current path
                    logger.debug(String.format("%1$"+ currentPath.size() +"s", "") +
                            " misbehaviour with all parents invalid: " + this.uri + " (nodeID:" + this.id + ")");
                    throw new TreeTraversalException(loopbackNodeUris);
                }

                // The rootCause of a misbehaviour is any (OR) of the root
                // cause of its parents
                rootCause = new LogicalExpression(this.apd, parentRootCauses, false);

                // The distance from a root cause is therefore the minimum of
                // the parent distances
                tmpMinDistanceFromRoot = Collections.min(parentMinDistancesFromRoot) + 1;
                tmpMaxDistanceFromRoot = Collections.min(parentMaxDistancesFromRoot) + 1;

                logger.debug(String.format("%1$"+ currentPath.size() +"s", "") +
                 " Finished looking at MS causes (nodeID:" + this.id + ") distance: " +
                 tmpMinDistanceFromRoot + " " + tmpMaxDistanceFromRoot);

                if (computeLogic) {
                    bsAttackTree = new LogicalExpression(this.apd, parentAttackTrees, false);
                    bsThreatTree = new LogicalExpression(this.apd, parentThreatTrees, false);
                    // Misbehaviours can be miticated by
                    // AND(
                    // mitigations of their parents
                    // )
                    attackMitigatedByCS = new LogicalExpression(this.apd,
                            new ArrayList<Object>(parentAttackMitigationsCS), true);
                    threatMitigatedByCS = new LogicalExpression(this.apd,
                            new ArrayList<Object>(parentThreatMitigationsCS), true);
                    attackMitigatedByCSG = new LogicalExpression(this.apd,
                            new ArrayList<Object>(parentAttackMitigationsCSG), true);
                    threatMitigatedByCSG = new LogicalExpression(this.apd,
                            new ArrayList<Object>(parentThreatMitigationsCSG), true);
                }
            }

        } catch (TreeTraversalException error) {
            outerSuccess = false;

            //logger.error(String.format("%1$"+ currentPath.size() +"s", "") +
            //        " Error " + this.uri + " (nodeID:" + this.id + ")");

            loopbackNodeUris = error.getLoopbackNodeUris();

            Set<String> loopbackNodeUrisOnPath = new HashSet<String>(currentPath);
            loopbackNodeUrisOnPath.retainAll(loopbackNodeUris);
            loopbackNodeUrisOnPath.remove(this.uri);

            InnerResult result = new InnerResult();
            if (loopbackNodeUrisOnPath.isEmpty()) {
                this.cannotBeCaused = true;
                logger.error(String.format("%1$"+ currentPath.size() +"s", "") +
                        " Error " + this.uri + " can never be caused (nodeID:" + this.id + ")");
            } else {
                result.setLoopbackNodeUris(loopbackNodeUrisOnPath);
                logger.error(String.format("%1$"+ currentPath.size() +"s", "") +
                        " Error " + this.uri + " caused by node on path: (nodeID:" + this.id + ")");
            }

            this.noCauseResults.add(result);
            this.noCauseVisits += 1;

            throw new TreeTraversalException(loopbackNodeUrisOnPath);
        }
        if (outerSuccess == true) { // TODO this is an try->catch->ELSE
            // If we've got this far then the node is on a workable path

            this.notACause = false; // set "True" on initialisation but not elsewhere, so this means that the node is on *at least one* workable path

            // keep track of which direct cause Nodes enabled this Node (also
            // adds this node as an effect of the cause)
            this.addDirectCauseUris(validParentUris);

            // Add the direct causes to the accumulated direct causes' causes
            allCauseUris.addAll(validParentUris);

            loopbackNodeUris.remove(this.uri);

            /*
             * At this point we have a distance_from root, root_cause and mitigation for the current path. We want those to be used in the child that called this method on
             * this node, but before that we need to merge the results with any others that have previously been found from other paths to this node. Interestingly, when
             * combining cause over different paths, the logic is reversed.
             */

            List<Object> tmpObjList = new ArrayList<>(Arrays.asList(this.rootCause, tmpRootCause));
            this.rootCause = new LogicalExpression(this.apd, tmpObjList, false);

            // Save the max and min distance from this root_cause
            // The max is useful to spread things out for display
            // The min is useful to find shortest paths
            this.maxDistanceFromRootByCause.put(this.rootCause,
                    Math.max(this.maxDistanceFromRootByCause.getOrDefault(this.rootCause, -1), tmpMaxDistanceFromRoot));
            this.minDistanceFromRootByCause.put(this.rootCause, Math
                    .min(this.maxDistanceFromRootByCause.getOrDefault(this.rootCause, 99999), tmpMinDistanceFromRoot));

            // although tempting to calculate the distance from target here, we
            // can't because we don't know if the current tree is going to be
            // successful all the way back to the target.

            if (computeLogic) {
                List<LogicalExpression> aCsList = new ArrayList<>(
                        Arrays.asList(this.attackTreeMitigationCS, attackMitigatedByCS));
                this.attackTreeMitigationCS = new LogicalExpression(this.apd, new ArrayList<Object>(aCsList), true);

                List<LogicalExpression> tCsList = new ArrayList<>(
                        Arrays.asList(this.threatTreeMitigationCS, threatMitigatedByCS));
                this.threatTreeMitigationCS = new LogicalExpression(this.apd, new ArrayList<Object>(tCsList), true);

                List<LogicalExpression> aCsgList = new ArrayList<>(
                        Arrays.asList(this.attackTreeMitigationCSG, attackMitigatedByCSG));
                this.attackTreeMitigationCSG = new LogicalExpression(this.apd, new ArrayList<Object>(aCsgList), true);

                List<LogicalExpression> tCsgList = new ArrayList<>(
                        Arrays.asList(this.threatTreeMitigationCSG, threatMitigatedByCSG));
                this.threatTreeMitigationCSG = new LogicalExpression(this.apd, new ArrayList<Object>(tCsgList), true);

                List<LogicalExpression> atList = new ArrayList<>(Arrays.asList(this.attackTree, bsAttackTree));
                this.attackTree = new LogicalExpression(this.apd, new ArrayList<Object>(atList), false);

                List<LogicalExpression> ttList = new ArrayList<>(Arrays.asList(this.threatTree, bsThreatTree));
                this.threatTree = new LogicalExpression(this.apd, new ArrayList<Object>(ttList), false);
            }

            InnerResult iResult = new InnerResult();
            iResult.setLoopbackNodeUris(loopbackNodeUris);

            iResult.setAllCauseUris(allCauseUris);
            iResult.setMinDistance(tmpMinDistanceFromRoot);
            iResult.setMaxDistance(tmpMaxDistanceFromRoot);
            iResult.putData(ROOT_CAUSE, rootCause);

            if (computeLogic) {
                iResult.putData(ATTACK_MITIGATION_CS, attackMitigatedByCS);
                iResult.putData(ATTACK_MITIGATION_CSG, attackMitigatedByCSG);
                iResult.putData(THREAT_MITIGATION_CS, threatMitigatedByCS);
                iResult.putData(THREAT_MITIGATION_CSG, threatMitigatedByCSG);
                iResult.putData(ATTACK_TREE, bsAttackTree);
                iResult.putData(THREAT_TREE, bsThreatTree);
            }

            this.causeResults.add(iResult);

            this.causeVisits += 1;

            return iResult;
        }

        return new InnerResult();
    }

    public Set<String> getDirectCauseUris() {
        return this.directCauseUris;
    }

    public void addDirectCauseUris(Set<String> uris) {
        this.directCauseUris.addAll(uris);
        for (String causeUri : uris) {
            this.nodes.getNode(causeUri).addDirectEffectUri(this.uri);
        }
    }

    private void addDirectEffectUri(String uri) {
        this.directEffectUris.add(uri);
    }

    public boolean isNormalOp() {
        return this.apd.isNormalOp(this.uri);
    }

    public boolean isExternalCause() {
        return this.apd.isExternalCause(this.uri);
    }

    public Set<String> getDirectEffectUris() {
        return this.directEffectUris;
    }

    public boolean isThreat() {
        return this.apd.isThreat(this.uri);
    }

    public boolean isMisbehaviourSet() {
        return this.apd.isMisbehaviourSet(this.uri);
    }

    public boolean isTrustworthinessAttributeSets() {
        return this.apd.isTrustworthinessAttributeSets(this.uri);
    }

    public boolean isSecondaryThreat() {
        return apd.isSecondaryThreat(this.uri);
    }

    public boolean isRootCause() {
        return apd.isRootCause(this.uri);
    }

    public Expression<String> uriSymbol() {
        return this.makeSymbol(this.uri);
    }

    public boolean isInitialCause(String uri) {
        return this.apd.isInitialCause(uri);
    }

    public boolean isMisbehaviourSet(String uri) {
        return this.apd.isMisbehaviourSet(uri);
    }

}
