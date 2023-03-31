/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2021
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
//      Created By :            Ken Meacham
//      Created Date :          2021-06-25
//      Created for Project :   Protego
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.model.system;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import uk.ac.soton.itinnovation.security.model.Level;

public class RiskVector {
	
	private Map<String, RiskLevelCount> riskVector;

	public RiskVector(Collection<Level> riskLevels, Map<String, Integer> riskLevelCounts) {
		riskVector = new HashMap<>();
		
		//For each defined risk level, get the count of misbehaviours at this level
		for (Level riskLevel : riskLevels) {
			RiskLevelCount riskLevelCount = new RiskLevelCount();
			riskLevelCount.setLevel(riskLevel);
			Integer count = riskLevelCounts.get(riskLevel.getUri());
			riskLevelCount.setCount(count);
			riskVector.put(riskLevel.getUri(), riskLevelCount);
		}
	}

	public Map<String, RiskLevelCount> getRiskVector() {
		return riskVector;
	}

	public void setRiskVector(Map<String, RiskLevelCount> riskVector) {
		this.riskVector = riskVector;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("(");
		
		Collection<RiskLevelCount> riskLevelCounts = riskVector.values();
		
		for (RiskLevelCount riskLevelCount : riskLevelCounts) {
			sb.append(riskLevelCount.getLevel().getLabel());
			sb.append(": ");
			sb.append(riskLevelCount.getCount());
			sb.append(", ");
		}
		
		sb.setLength(sb.length() -2); //remove last comma
		
		sb.append(")");
		
		return sb.toString();
	}

}
