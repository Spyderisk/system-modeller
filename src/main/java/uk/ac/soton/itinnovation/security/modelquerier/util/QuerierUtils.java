/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2024
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
//      Created Date :          18/01/2024
//      Created for Project :   Cyberkit4SME
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelquerier.util;

import java.util.*;

public class QuerierUtils {
    
    private QuerierUtils() {
        throw new IllegalStateException("QuerierUtils is a Utility class");
    }

    /**
     * Given a set of ControlSet URIs, return all related URIs (see getControlTriplet)
     * @param controlSets set of ControlSet URIs
     * @return expanded set of ControlSet URIs
     */
    public static Set<String> getExpandedControlSets(Set<String> controlSets) {
		Set<String> expandedControlSets = new HashSet<>();

		for (String cs : controlSets) {
			Set<String> expCs = getControlTriplet(cs);
			expandedControlSets.addAll(expCs);
		}

		return expandedControlSets;
	}

    /**
     * Given a ControlSet URI, return the set of related URIs: csAvg, csMin, csMax
     * @param csuri ControlSet URI (could be avg, min or max)
     * @return set of related URIs: csAvg, csMin, csMax
     */
	public static Set<String> getControlTriplet(String csuri) {
		String[] uriFrags = csuri.split("#");
		String uriPrefix = uriFrags[0];
		String shortUri = uriFrags[1];

		String [] shortUriFrags = shortUri.split("-");
		String control = shortUriFrags[0] + "-" + shortUriFrags[1];
		control = control.replace("_Min", "").replace("_Max", "");
		String assetId = shortUriFrags[2];

		String csAvg = uriPrefix + "#" + control + "-" + assetId;
		String csMin = uriPrefix + "#" + control + "_Min" + "-" + assetId;
		String csMax = uriPrefix + "#" + control + "_Max" + "-" + assetId;

		return new HashSet<>(Arrays.asList(csAvg, csMin, csMax));
	}

}
