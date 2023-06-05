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

import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.TreeJsonDoc;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.Graph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import java.util.stream.Collectors;

public class AttackTree {
    private static final Logger logger = LoggerFactory.getLogger(AttackTree.class);

    private AttackPathDataset apd;
    private Map<String, AttackNode> nodeByUri = new HashMap<>();
    private List<String> targetUris = new ArrayList<>();
    private boolean isFutureRisk = false;
    private Set<String> boundingUriRefs = new HashSet<>();

    private int nodeCounter = 0;

    private Map<String, Set<Integer>> rankByUri = new HashMap<>();

    private Map<String, Map<String, Integer>> pathNodes = new HashMap<>();

    private class InnerLink {
        AttackNode node = null;
        String predicate = "";
        AttackNode effectNode = null;

        public AttackNode getNode() { return this.node; }
        public void setNode(AttackNode node) { this.node = node; }
        public String getPredicate() { return this.predicate; }
        public void setPredicate(String predicate) { this.predicate = predicate; }
        public AttackNode getEffectNode() { return this.effectNode; }
        public void setEffectNode(AttackNode node) { this.effectNode = node; }

        public String toString() {
            return node.getUri().substring(7) + " " + predicate + " " + effectNode.getUri().substring(7);
        }
    }

    /**
    * Creates a new AttackTree object.
    *
    * @param targetUris A list of target URIs.
    * @param futureRisk A boolean flag indicating whether future risk should be considered*.
    * @param shortestPath A boolean flag indicating whether the shortest path should be calculated.
    * @param apDataset An AttackPathDataset object containing attack path data.
    */
    public AttackTree(List<String> targetUris, boolean futureRisk, boolean shortestPath, AttackPathDataset apDataset) {

        final long startTime = System.currentTimeMillis();

        logger.info("*******************************************************");
        logger.info("Starting ThreatTree with {} target MS, futureRisk {}, and shortestPath {} flags.",
                targetUris, futureRisk, shortestPath);

        this.apd = apDataset;

        this.targetUris = targetUris;

        this.isFutureRisk = futureRisk;

        if (!shortestPath){
            logger.info("***********************");
            logger.info("Running backtrace");
            logger.info("***********************");
            this.backtrace(true);
        } else {
            /*
             * If the shortest path is required then we get the URIRefs of the
             * shortest path nodes from the first pass at the ThreatTree then
             * discard all TreeNodes and create a new ThreatTree which is
             * bounded by the shortest path URIRefs.
             */
            logger.info("***********************");
            logger.info("RUNNING FIRST backtrace");
            logger.info("***********************");

            this.backtrace(false);

            this.boundingUriRefs =  new HashSet<String>();
            for(AttackNode node : this.shortestPathNodes()) {
                this.boundingUriRefs.add(node.getUri());
            }

            this.nodeByUri = new HashMap<>();
            this.nodeCounter = 0;

            logger.info("***********************");
            logger.info("RUNNING SECOND backtrace, bounded by {} nodes", this.boundingUriRefs.size());
            logger.info("***********************");

            this.backtrace(true);
        }

        logger.info("Adding distance for each node from each target MS");
        for(String targetMSUri : this.targetUris) {
            this.addMaxDistanceFromTarget(targetMSUri, null);
            Map<String, Integer> pNodes = new HashMap<>();
            this.followPath(targetMSUri, null, pNodes);
            this.pathNodes.put(targetMSUri, pNodes);
        }
        logger.info("Final ThreatTree now has {} nodes", this.nodes().size());

        final long endTime = System.currentTimeMillis();
        logger.info("AttackTree.AttackTree(IQuerierDB querier): execution time {} ms", endTime - startTime);
    }

    public boolean getIsFutureRisk() {
        return this.isFutureRisk;
    }

    public Set<String> getBoundingUriRefs() {
        return this.boundingUriRefs;
    }

    public AttackNode getNode(String uri) {
        return this.nodeByUri.get(uri);
    }

    /**
    * Gets or creates a new AttackNode object with the specified URI.
    *
    * @param uri The URI of the AttackNode to get or create.
    * @return The AttackNode object with the specified URI.
    */
    public AttackNode getOrCreateNode(String uri) {

        if (!this.nodeByUri.containsKey(uri)) {
            AttackNode treeNode = new AttackNode(uri, this.apd, this, ++nodeCounter);
            this.nodeByUri.put(uri, treeNode);
        }

        return this.nodeByUri.get(uri);
    }

    /**
     * Backtraces the attack graph from each target node, and computes the logic values
     * of the nodes if the computeLogic parameter is set to true.
     *
     * @param computeLogic if true, the logic values of the nodes are computed during backtracing.
     */
    private void backtrace(boolean computeLogic) {

        for (String targetUri : this.targetUris) {

            AttackNode node = this.getOrCreateNode(targetUri);
            node.setIsTargetMS(true);

            try {
                node.backtrace(new HashSet<String>(), computeLogic);
            } catch (TreeTraversalException e) {
                logger.error("Tree traversal error: " + e.getMessage(), e);
            } catch (Exception e) {
                logger.error("Exception error: " + e.getMessage(), e);
            }
        }
    }

    /**
    * Finds the set of AttackNodes that make up the shortest attack path in the AttackTree.
    *
    * @return The set of AttackNodes that make up the shortest attack path.
    */
    private Set<AttackNode> shortestPathNodes() {
        // Return the set of nodes that are on the shortest path(s)

        /* The strategy is to remove nodes where all children are NOT further
         * away from the root cause, or where there are no children.
         * We define "good nodes" to be ones which have at least one child
         * further away than the node, remove the others and iterate until no
         * change.
         */

        // TODO: review this as it looks liek it's not quite working

        Set<AttackNode> spn = this.nodes().stream().collect(Collectors.toSet());
        while (true) {
            Set<AttackNode> goodNodes = new HashSet<>();
            for (String targetMsUri : this.targetUris) {
                goodNodes.add(this.nodeByUri.get(targetMsUri));
            }

            for (AttackNode node : spn) {
                for (String causeUri : node.getDirectCauseUris()) {
                    AttackNode causeNode = this.nodeByUri.get(causeUri);

                    // Special logic here to still include normal-ops as their
                    // root cause distances are measured from a normal-op
                    // initial cause so at the boundary the normal logic fails.

                    if((causeNode.minDistanceFromRoot() < node.minDistanceFromRoot())
                            || (causeNode.isNormalOp() && !node.isNormalOp())){
                        goodNodes.add(causeNode);
                    }
                }
            }

            goodNodes.retainAll(spn);
            if (goodNodes.size() < spn.size()){
                spn = goodNodes;
            } else {
                break;
            }
        }
        return spn;
    }

    /**
    * Gets a list of all the AttackNodes in the AttackTree that are not in the
    * error state, i.e. not not-a-cause.
    *
    * @return A list of all the AttackNodes in the AttackTree.
    */
    private Set<AttackNode> nodes() {
        //Don't return the nodes that are the error state
        Set<AttackNode> filteredSet;
        filteredSet = this.nodeByUri.values().stream()
            .filter(node -> !node.getNotACause())
            .collect(Collectors.toSet());
        return filteredSet;
    }

    /**
    * Gets a list of all the AttackNodes in the AttackTree that are not in the
    * error state, i.e. not not-a-cause.
    *
    * @return A list of all the AttackNodes in the AttackTree.
    */
    private List<AttackNode> excludedNodes() {
        //Don't return the nodes that are the error state
        List<AttackNode> filteredList;
        filteredList = this.nodeByUri.values().stream()
            .filter(node -> node.getNotACause())
            .collect(Collectors.toList());
        return filteredList;
    }

    private void addMaxDistanceFromTarget(String uriRef, List<String>currentPath) {
        if (currentPath == null) {
            currentPath = new ArrayList<String>();
        }

        List<String> copyCP = new ArrayList<>();
        copyCP.addAll(currentPath);

        // Using a tuple for current_path to ensure that when we change it we
        // make a copy so that the addition is undone when the recursion
        // unwinds
        copyCP.add(uriRef);
        AttackNode currentNode = this.nodeByUri.get(uriRef);
        String targetUriRef = copyCP.get(0);

        int currentDistance = currentNode.getMaxDistanceFromTargetByTarget(targetUriRef);
        int maxDistance = Math.max(currentDistance, (copyCP.size() - 1));
        currentNode.setMaxDistanceFromTargetByTarget(targetUriRef, maxDistance);

        for(String causeUriRef : currentNode.getDirectCauseUris()) {
            // there can be loops int eh "tree" so have to make sure we don't
            // follow one
            if (!copyCP.contains(causeUriRef)) {
                this.addMaxDistanceFromTarget(causeUriRef, copyCP);
            }
        }
    }

    private List<String> uris() {
        // Don't return the nodes that are in the error state
        List<String> filteredList = new ArrayList<>();
        filteredList = this.nodeByUri.keySet().stream()
            .filter(uri -> !this.nodeByUri.get(uri).getNotACause())
            .collect(Collectors.toList());
        return filteredList;
    }

    private Set<String> rootCauses() {
        Set<String> uriSet = new HashSet<>();
        for (AttackNode an : this.nodes()) {
            if (an.isRootCause()) {
                uriSet.add(an.getUri());
            }
        }
        return uriSet;
    }

    private Set<String> externalCauses() {
        Set<String> uriSet = new HashSet<>();
        for (AttackNode an : this.nodes()) {
            if (an.isExternalCause()) {
                uriSet.add(an.getUri());
            }
        }
        return uriSet;
    }

    private Set<String> normalOperations() {
        Set<String> uriSet = new HashSet<>();
        for (AttackNode an : this.nodes()) {
            if (an.isNormalOp()) {
                uriSet.add(an.getUri());
            }
        }
        return uriSet;
    }

    /**
    * Applies a filter of normal operations on a given set of AttackNodes.
    *
    * @param nodes the set of AttackNodes to perform normal operations on
    * @return a Set of AttackNodes with normal operations nodes
    */
    private Set<AttackNode> normalOperations(Set<AttackNode> nodes) {
        Set<AttackNode> noOpSet = new HashSet<>();
        for (AttackNode an : nodes) {
            if (an.isNormalOp()) {
                noOpSet.add(an);
            }
        }
        return noOpSet;
    }

    Set<String> initialCauses() {
        Set<String> uris = new HashSet<>();

        for (AttackNode node : this.nodes()) {
            if (node.isInitialCause(node.getUri())) {
                uris.add(node.getUri());
            }
        }
        return uris;
    }

    private void followPath(String uri, List<String> cPath, Map<String, Integer> pathNodes) {
        if (cPath == null ) {
            cPath = new ArrayList<String>();
        }
        cPath.add(uri);
        AttackNode cNode = this.nodeByUri.get(uri);
        pathNodes.put(uri, cNode.maxDistanceFromTarget());
        for (String causeUri : cNode.getDirectCauseUris()) {
            if (!cPath.contains(causeUri)) {
                this.followPath(causeUri, cPath, pathNodes);
            }
        }
    }

    private Map<String, Integer> sortedMap(Map<String, Integer> inMap) {

        List<Map.Entry<String, Integer>> list = new ArrayList<>(inMap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>(){
            public int compare(Map.Entry<String, Integer> entry1, Map.Entry<String, Integer> entry2) {
                return entry1.getValue().compareTo(entry2.getValue());
            }
        });

        Map<String, Integer> sortedMap = new LinkedHashMap<>();

        for (Map.Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public Graph createGraphDoc(Map<String, Integer> fNodes, Set<InnerLink> links) {
        //return a response json object

        Map<String, Integer> threats = new HashMap<>();
        Map<String, Integer> misbehaviours = new HashMap<>();
        Map<String, Integer> twas = new HashMap<>();
        List<List<String>> treeLinks = new ArrayList<>();

        // create nodes lists
        for (String nodeUri : fNodes.keySet()){
            AttackNode node = this.nodeByUri.get(nodeUri);
            if (node.isThreat()) {
                threats.put(nodeUri, fNodes.get(nodeUri));
            } else if (node.isMisbehaviourSet()) {
                misbehaviours.put(nodeUri, fNodes.get(nodeUri));
            } else if (node.isTrustworthinessAttributeSets()) {
                twas.put(nodeUri, fNodes.get(nodeUri));
            } else {
                logger.error("unknown type of node: {}", node.getUri());
            }
        }

        // crete links list
        for (InnerLink iLink : links) {
            List<String> link = new ArrayList<>();
            if(fNodes.containsKey(iLink.getNode().getUri()) &&
                    fNodes.containsKey(iLink.getEffectNode().getUri())) {
                link.add(iLink.getNode().getUri());
                link.add(iLink.getEffectNode().getUri());
                treeLinks.add(link);
            }
        }

        Graph graph = new Graph(this.sortedMap(threats), this.sortedMap(misbehaviours),
                this.sortedMap(twas), treeLinks);

        return graph;
    }

    /**
    * Calculates a TreeJsonDoc object representing the tree of nodes,
    * optionally including all paths and/or applying normal operations.
    *
    * @param allPaths if true, include all paths in the tree; if false, only include paths to
    *                 shortest path.
    * @param normalOp if true, apply normal operations to filter AttackNodes
    * @return a TreeJsonDoc object representing the tree nodes.
    */
    public TreeJsonDoc calculateTreeJsonDoc(boolean allPaths, boolean normalOp) {

        Set<AttackNode> nodes = null;

        if (allPaths) {
            nodes = this.nodes().stream().collect(Collectors.toSet());
        } else {
            nodes = this.shortestPathNodes();
        }

        if (!normalOp) {
            nodes = this.normalOperations(nodes);
        }

        Set<InnerLink> links = new HashSet<>();

        for (InnerLink il : this.createLinks(nodes)) {
            if (nodes.contains(il.getNode()) && nodes.contains(il.getEffectNode())) {
                links.add(il);
            } else {
                logger.debug("link rejected: {} -> {}", il.getNode().getUri(), il.getEffectNode().getUri());
            }
        }

        logger.debug("Selected nodes: {}", nodes.size());
        logger.debug("Created links: {}", links.size());

        Map<String, Graph> graphs = new HashMap<>();

        for(String targetMS : this.targetUris) {
            Graph graph = this.createGraphDoc(this.pathNodes.get(targetMS), links);
            graphs.put(targetMS, graph);
        }

        TreeJsonDoc treeJsonDoc = new TreeJsonDoc(graphs);

        return treeJsonDoc;
    }

    private LogicalExpression attackMitigationCSG() {
        List<LogicalExpression> leList = new ArrayList<>();
        for (String uri : this.targetUris){
            leList.add(this.nodeByUri.get(uri).getControlStrategies());
        }
        logger.debug("attackMitigationCSG LE size: {}", leList.size());
        return new LogicalExpression(this.apd, new ArrayList<Object>(leList), true);
    }

    private LogicalExpression attackMitigationCS() {
        List<LogicalExpression> leList = new ArrayList<>();
        for (String uri : this.targetUris){
            leList.add(this.nodeByUri.get(uri).getControls());
        }
        return new LogicalExpression(this.apd, new ArrayList<Object>(leList), true);
    }

    private Set<InnerLink> createLinks(Set<AttackNode> nodes) {
        Set<InnerLink> links = new HashSet<>();
        for (AttackNode node : nodes) {
            for (String effectUri : node.getDirectEffectUris()) {
                AttackNode effectNode = this.nodeByUri.get(effectUri);
                InnerLink iLink = new InnerLink();
                iLink.setNode(node);
                iLink.setEffectNode(effectNode);
                if (node.isThreat() || effectNode.isSecondaryThreat()) {
                    iLink.setPredicate("causes");
                } else {
                    iLink.setPredicate("enables");
                }
                links.add(iLink);
            }
        }
        logger.debug("Links found: {}", links.size());
        return links;
    }

    private void setRank(String nodeUri, int rank) {
        Set<Integer> ranks;
        if (this.rankByUri.containsKey(nodeUri)) {
            ranks = this.rankByUri.get(nodeUri);
        } else {
            ranks = new HashSet<>();
            this.rankByUri.put(nodeUri, ranks);
        }

        if (ranks.contains(rank)) {
            return;
        } else {
            ranks.add(rank);
            for (String causeUri : this.nodeByUri.get(nodeUri).getDirectCauseUris()) {
                this.setRank(causeUri, rank + 1);
            }
        }
    }

    //////////////////////////////
    /* other auxiliary methods */
    public void stats() {
        // calculte tree stats
        logger.info("#################################");
        logger.info("#### Attack Tree stats ####");
        logger.info("Tree nodes_by_uri..: {}", this.nodeByUri.size());
        logger.info(" @property nodes...: {}", this.uris().size());

        Set<String> csgs = new HashSet<>();
        Set<String> controls = new HashSet<>();
        int spnCounter = 0;
        for (AttackNode an : this.shortestPathNodes()) {
            if (an.isThreat()) {
                spnCounter += 1;
                for (String csgUri : this.apd.getThreatControlStrategyUris(an.getUri(), true)) {
                    csgs.add(csgUri);
                    for (String csUri : this.apd.getCsgControlSetsUris(csgUri)){
                        controls.add(csUri);
                    }
                }
            }
        }
        logger.debug("Shortest path nodes: {}", spnCounter);
        logger.debug("CSGs...............: {}", csgs.size());
        logger.debug("CS.................: {}", controls.size());
        logger.info("#################################");
    }

}

