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
//      Created By:				Panos Melas
//      Created Date:			2023-03-01
//      Created for Project :   Cyberkit4SME
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto;

import java.util.Map;
import java.util.List;

public class Graph {
    private Map<String, Integer> threats;
    private Map<String, Integer> misbehaviours;
    private Map<String, Integer> twas;
    private List<List<String>> links;

    public Graph (Map<String, Integer> threats,
            Map<String, Integer> misbehaviours,
            Map<String, Integer> twas,
            List<List<String>> links) {

        this.threats = threats;
        this.misbehaviours = misbehaviours;
        this.twas = twas;
        this.links = links;
    }

    public Map<String, Integer> getThreats() {
        return threats;
    }

    public void setThreats(Map<String, Integer> threats) {
        this.threats = threats;
    }

    public Map<String, Integer> getMisbehaviours() {
        return misbehaviours;
    }

    public void setMisbehaviours(Map<String, Integer> misbehaviours) {
        this.misbehaviours = misbehaviours;
    }

    public Map<String, Integer> getTwas() {
        return twas;
    }

    public void setTwas(Map<String, Integer> twas) {
        this.twas = twas;
    }

    public List<List<String>> getLinks() {
        return links;
    }

    public void setLinks(List<List<String>> links) {
        this.links = links;
    }
}
