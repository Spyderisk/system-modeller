/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2017
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
//      Created By :            Josh Harris
//      Modified By :           Stefanie Cox
//      Created Date :          11 Sep 2017
//      Created for Project :   5G-Ensure, SHiELD
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.model.system;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.model.SemanticEntity;

public class ComplianceSet extends SemanticEntity {

	public static final Logger logger = LoggerFactory.getLogger(ComplianceSet.class);

	private final Map<String, ComplianceThreat> threats;

	public ComplianceSet() {
		threats = new HashMap<>();
	}

	/**
	 * Creates a new compliance with description, lavel and uri
	 * Instansiates the threat sets
	 * @param uri
	 * @param label
	 * @param description
	 */
	public ComplianceSet(String uri, String label, String description){

		this();
		setUri(uri);
		setLabel(label);
		setDescription(description);
	}

	/**
	 * Returns true if this compliance passes
	 * @return true if compliant
	 */
	public boolean isCompliant(){

		boolean compliant = true;
		for (ComplianceThreat t: threats.values()) {
			if (!t.isResolved()) {
				compliant = false;
				break;
			}
		}
		return compliant;
	}

	/**
	 * Gets the set of system level threats
	 * @return set of uris of the system level threats
	 */
	public Map<String, ComplianceThreat> getThreats(){
		return threats;
	}

}
