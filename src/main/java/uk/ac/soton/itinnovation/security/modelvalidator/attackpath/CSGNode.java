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
//      Created Date:           2023-07-25
//      Created for Project :   Cyberkit4SME
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelvalidator.attackpath;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.RecommendationDTO;

public class CSGNode {
    private List<String> csgList;
    private Set<String> csList;
    private List<CSGNode> children;
    private RecommendationDTO recommendation;

    public CSGNode() {
        this(new ArrayList<>());
    }

    public CSGNode(List<String> csgList) {
        if (csgList == null) {
            csgList = new ArrayList<>();
        }
        this.csgList = csgList;
        this.children = new ArrayList<>();
        this.recommendation = null;
        this.csList = new HashSet<>();
    }

    public void addChild(CSGNode child) {
        children.add(child);
    }

    public Set<String> getCsList() {
        return this.csList;
    }

    public List<String> getCsgList() {
        return this.csgList;
    }

    public void setCsList(Set<String> csList) {
        this.csList = csList;
    }

    public List<CSGNode> getChildren() {
        return this.children;
    }

    public RecommendationDTO getRecommendation() {
        return this.recommendation;
    }

    public void setRecommendation(RecommendationDTO rec) {
        this.recommendation = rec;
    }
}
