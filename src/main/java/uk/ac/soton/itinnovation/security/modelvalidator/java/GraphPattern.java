/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2019
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
//      Created By :            Lee Mason
//      Created Date :          05/08/2019
//      Created for Project :   RESTASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelvalidator.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GraphPattern {
	private String uri;
	private List<String> roles = new ArrayList<>();
	private List<String> nodes = new ArrayList<>();
	private Map<String, Map<String, Set<String>>> adjacent = new HashMap<>();
	private List<PatternLink> prohibitedLinks = new ArrayList<>();
	private List<String> prohibitedNodes = new ArrayList<>();
	private List<PatternLink> matchLinks = new ArrayList<>();
	private List<String> optionalNodes = new ArrayList<>();
	private List<String> mandatoryNodes = new ArrayList<>();
	private List<String> necessaryNodes = new ArrayList<>();
	private List<String> sufficientNodes = new ArrayList<>();
	private List<String> distinctNodeGroups = new ArrayList<>();
	
	// TODO: Secondary nodes
	
	public GraphPattern(String uri) {
		this.uri = uri;
	}
	
	public String getUri() {
		return uri;
	}
	
	public void addProhibitedLink(String fromNode, String toNode, String linkType) {
		// TODO: Check fromNode and toNode present
		prohibitedLinks.add(new PatternLink(fromNode, toNode, linkType));
	}
	
	public void addProhibitedNode(String node) {
		prohibitedNodes.add(node);
	}
	
	public void addOptionalNode(String node) {
		optionalNodes.add(node);
	}
	
	public List<String> getOptionalNodes() {
		return optionalNodes;
	}
	
	public void addMandatoryNode(String node) {
		mandatoryNodes.add(node);
	}
	
	public void addNecessaryNode(String node) {
		necessaryNodes.add(node);
	}
	
	public void addSufficientNode(String node) {
		sufficientNodes.add(node);
	}

	public List<String> getNecessaryNodes() {
		return necessaryNodes;
	}
	
	public List<String> getSufficientNodes() {
		return sufficientNodes;
	}
	
	public List<String> getMandatoryNodes() {
		return mandatoryNodes;
	}
	
	public List<String> getNodes() {
		return nodes;
	}

	public void addLink(String fromNode, String toNode, String linkType) {
		addRole(fromNode);
		addRole(toNode);
		
		Map<String, Set<String>> typeToNodes = adjacent.get(fromNode);
		if (typeToNodes == null) {
			typeToNodes = new HashMap<>();
			adjacent.put(fromNode, typeToNodes);
		}
		
		Set<String> toNodes = typeToNodes.get(linkType);
		if (toNodes == null) {
			toNodes = new HashSet<>();
			typeToNodes.put(linkType, toNodes);
		}
		toNodes.add(toNode);
	}
	
	public Set<String> getAdjacentFrom(String fromNode, String linkType) {
		Map<String, Set<String>> typeToNodes = adjacent.get(fromNode);
		if (typeToNodes == null) {
			return new HashSet<>();
		}
		Set<String> toNodes = typeToNodes.get(linkType);
		return toNodes != null ? toNodes : new HashSet<>();
	}
	
	public List<PatternLink> getLinksFrom(String fromNode) {
		List<PatternLink> links = new ArrayList<>();
		Map<String, Set<String>> typeToNodes = adjacent.get(fromNode);
		if (typeToNodes != null) {
			for (Map.Entry<String,Set<String>> toEntry : typeToNodes.entrySet()) {
				for (String nodeTo : toEntry.getValue()) {
					links.add(new PatternLink(fromNode, nodeTo, toEntry.getKey()));
				}
			}
		}
		return links;
	}
	
	public List<String> getRoles() {
		return roles;
	}
	
	public void addRole(String role) {
		if (!roles.contains(role)) {
			roles.add(role);
		}
	}
	
	public void addNode(String node) {
		nodes.add(node);
	}

	public List<PatternLink> getProhibitedLinks() {
		return prohibitedLinks;
	}
	
	public List<String> getProhibitedNodes() {
		return prohibitedNodes;
	}
	
	public void addMatchLink(String linksFrom, String linksTo, String linkType) {
		this.matchLinks.add(new PatternLink(linksFrom, linksTo, linkType));
	}
	
	public List<PatternLink> getMatchLinks() {
		return matchLinks;
	}
	
	public void addDistinctNodeGroup(String distinctNodeGroup) {
		this.distinctNodeGroups.add(distinctNodeGroup);
	}
	
	public List<String> getDistinctNodeGroups() {
		return this.distinctNodeGroups;
	}
}
