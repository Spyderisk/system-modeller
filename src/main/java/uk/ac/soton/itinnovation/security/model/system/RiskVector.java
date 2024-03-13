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
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import uk.ac.soton.itinnovation.security.model.Level;

public class RiskVector implements Comparable<RiskVector> {

    private Map<String, RiskLevelCount> riskV;
    private Map<Integer, String> levelValueMap;  // aux map for comparison

    public RiskVector(Collection<Level> riskLevels, Map<String, Integer> riskLevelCounts) {
        this.riskV = new HashMap<>();
        this.levelValueMap = new HashMap<>();

        //For each defined risk level, get the count of misbehaviours at this level
        for (Level riskLevel : riskLevels) {
            RiskLevelCount riskLevelCount = new RiskLevelCount();
            riskLevelCount.setLevel(riskLevel);
            Integer count = riskLevelCounts.get(riskLevel.getUri());
            riskLevelCount.setCount(count);
            riskV.put(riskLevel.getUri(), riskLevelCount);
            levelValueMap.put(riskLevel.getValue(), riskLevel.getUri());
        }
    }

    public Map<String, RiskLevelCount> getRiskVector() {
        return riskV;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("(");

        // put the items from riskLevelCounts in a list
        List<RiskLevelCount> riskLevelCounts = new ArrayList<>(riskV.values());

        // sort the riskLevelCounts entries by the RiskLevelCount object's default sort
        Collections.sort(riskLevelCounts);

        for (RiskLevelCount riskLevelCount : riskLevelCounts) {
            sb.append(riskLevelCount.getLevel().getLabel());
            sb.append(": ");
            sb.append(riskLevelCount.getCount());
            sb.append(", ");
        }

        sb.setLength(sb.length() - 2); //remove last comma

        sb.append(")");

        return sb.toString();
    }

    public String getOverall() {
        int overall = 0;
        String uri = "";
        for (Map.Entry<String, RiskLevelCount> entry : riskV.entrySet()) {
            String riskLevelUri = entry.getValue().getLevel().getUri();
            int riskLevelValue = entry.getValue().getLevel().getValue();
            int riskCount = entry.getValue().getCount();
            if (riskCount > 0 && riskLevelValue >= overall) {
                overall = riskLevelValue;
                uri = riskLevelUri;
            }
        }
        return uri;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        RiskVector other = (RiskVector) obj;
        return Objects.equals(riskV, other.riskV);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(riskV);
    }

    @Override
    public int compareTo(RiskVector other) {

        List<Integer> sortedKeys = new ArrayList<>(levelValueMap.keySet());
        Collections.sort(sortedKeys, Collections.reverseOrder());

        // iterate based on the sorted keys
        for (Integer key : sortedKeys) {
            String riskLevelUri = levelValueMap.get(key);
            RiskLevelCount thisRiskLevelCount = riskV.get(riskLevelUri);
            RiskLevelCount otherRiskLevelCount = other.riskV.get(riskLevelUri);

            if (thisRiskLevelCount == null && otherRiskLevelCount == null) {
                continue; // Both are missing
            }
            if (thisRiskLevelCount == null) {
                return -1; // This object is considered "less"
            }
            if (otherRiskLevelCount == null) {
                return 1;  // This object is considered "greater"
            }

            // Compare RiskLevelCount objects
            int result = thisRiskLevelCount.compareTo(otherRiskLevelCount);
            if (result != 0) {
                return result;
            }
        }

        // If all compared RiskLevelCount objects are equal, consider the RiskVectors equal
        return 0;
    }
}
