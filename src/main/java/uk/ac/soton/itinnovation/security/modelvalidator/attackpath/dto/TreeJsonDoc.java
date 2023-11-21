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

public class TreeJsonDoc {
    // TODO: should pass it as a parameter
    private static final String uriPrefix = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/";
    private Map<String, Graph> graphs;

    public TreeJsonDoc(Map<String, Graph> graphs) {
        this.graphs = graphs;
    }

    public String getUriPrefix() {
        return uriPrefix;
    }

    public Map<String, Graph> getGraphs() {
        return graphs;
    }

    public void setGraphs(Map<String, Graph> graphs) {
        this.graphs = graphs;
    }
}
