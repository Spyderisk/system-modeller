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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GraphNode {
	private String asset;
	private Set<String> roles;
	private Map<String, Set<GraphNode>> forwardLinks = new HashMap<>();
	private Map<String, Set<GraphNode>> backwardLinks = new HashMap<>();
	private String type;

	public GraphNode(String asset, Set<String> roles) {
		this.asset = asset;
		this.roles = roles;
	}
	
	public boolean addForwardLink(String linkType, GraphNode nodeTo) {
		Set<GraphNode> linkNodes = forwardLinks.get(linkType);
		if (linkNodes == null) {
			linkNodes = new HashSet<>();
			forwardLinks.put(linkType, linkNodes);
		}
		return linkNodes.add(nodeTo);
	}
	
	public boolean delForwardLink(String linkType, GraphNode nodeTo) {
		Set<GraphNode> linkNodes = forwardLinks.get(linkType);
		if (linkNodes != null) {
			linkNodes.remove(nodeTo);
			return true;
		}
		return false;
	}

	public boolean addBackwardLink(String linkType, GraphNode nodeFrom) {
		Set<GraphNode> linkNodes = backwardLinks.get(linkType);
		if (linkNodes == null) {
			linkNodes = new HashSet<>();
			backwardLinks.put(linkType, linkNodes);
		}
		return linkNodes.add(nodeFrom);
	}
	
	public boolean delBackwardLink(String linkType, GraphNode nodeFrom) {
		Set<GraphNode> linkNodes = backwardLinks.get(linkType);
		if (linkNodes != null) {
			linkNodes.remove(nodeFrom);
			return true;
		}
		return false;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public Set<String> getRoles() {
		return roles;
	}
	
	public String getAsset() {
		return asset;
	}
	
	public String getType() {
		return type;
	}

	public Set<GraphNode> getNodesFromLinks(Collection<String> types) {
		Set<GraphNode> nodes = new HashSet<>();
		for (String subType : types) {
			Set<GraphNode> graphNodes = forwardLinks.get(subType);
			if (graphNodes != null) {
				nodes.addAll(graphNodes);
			}
		}
		return nodes != null ? nodes : new HashSet<>();
	}

	public Set<GraphNode> getNodesToLinks(Collection<String> types) {
		Set<GraphNode> nodes = new HashSet<>();
		for (String subType : types) {
			Set<GraphNode> graphNodes = backwardLinks.get(subType);
			if (graphNodes != null) {
				nodes.addAll(graphNodes);
			}
		}
		return nodes != null ? nodes : new HashSet<>();
	}
	
	@Override
	public String toString() {
		String string = asset + "(" + roles + ")";
		string += "\n\tforwardLinks:"; 
		
		for (Map.Entry<String, Set<GraphNode>> toEntry : forwardLinks.entrySet()) {
			string += "\n\t\t" + toEntry.getKey() + " -> ";
			for (GraphNode toNode : toEntry.getValue()) {
				string += "," + toNode.getAsset();
			}
		}
		string += "\n\tbackwardLinks:";
		for (Map.Entry<String, Set<GraphNode>> backEntry : backwardLinks.entrySet()) {
			string += "\n\t\t" + backEntry.getKey()  + " <- ";
			for (GraphNode backNode : backEntry.getValue()) {
				string += backNode.getAsset();
			}
		}

		return string;
	}
}
