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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.Data;

@Data
public class GraphMatchedPattern {
	private GraphPattern parent;
	
	private Map<String, Set<String>> allFeasible = new HashMap<>();
	
	public GraphMatchedPattern(GraphPattern parent) {
		this.parent = parent;
	}
	
	public void removeFeasible(String role, String asset) {
		allFeasible.get(role).remove(asset);
	}
	
	public Set<String> getFeasibleFrom(String fromNode) {
		Set<String> feasible = allFeasible.get(fromNode);
		if (feasible == null) {
			feasible = new HashSet<>();
			allFeasible.put(fromNode, feasible);
		}
		return feasible;
	}
	
	public void addFeasibleFrom(String fromRole, String asset) {
		Set<String> assets = allFeasible.get(fromRole);
		if (assets == null) {
			assets = new HashSet<>();
			allFeasible.put(fromRole, assets);
		}
		assets.add(asset);
	}
	
	public GraphMatchedPattern clone() {
		GraphMatchedPattern clone = new GraphMatchedPattern(parent);
		
		for (Map.Entry<String, Set<String>> entry : allFeasible.entrySet()) {
			Set<String> copySet = new HashSet<>();
			copySet.addAll(entry.getValue());
			clone.getAllFeasible().put(entry.getKey(), copySet);
		}
		
		return clone;
	}
	
	public String getRoleAsset(String role) {
		Set<String> assets = allFeasible.get(role);
		return assets != null ? assets.iterator().next() : null;
	}
	
	public Set<String> getRoleAssets(String role) {
		Set<String> assets = allFeasible.get(role);
		return assets != null ? assets : new HashSet<>();
	}

	public void addRoleAsset(String role, String asset) {
		Set<String> assets = allFeasible.computeIfAbsent(role, k -> new HashSet<>());
		assets.add(asset);
	}
	
	public boolean checkInvalidMatch() {
		for (Set<String> feasible : allFeasible.values()) {
			if (feasible.isEmpty()) {
				return true;
			}
		}
		return false;
	}
	
	public String toString() {
		return parent.getUri().toString() + ":" + allFeasible.toString();
	}
}
