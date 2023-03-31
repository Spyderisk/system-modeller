/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2018
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
//      Modified By :           Ken Meacham
//      Created Date :          2018-03-29
//      Created for Project :   SHIELD
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest.dto;

import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.model.system.ComplianceSet;

public class ComplianceSetDTO extends SemanticEntityDTO {

	public static final Logger logger = LoggerFactory.getLogger(ComplianceSetDTO.class);

	private final Set<String> systemThreats;
	
	private final boolean compliant;

	public ComplianceSetDTO() {
		systemThreats = new HashSet<>();
		compliant = false;
	}

	/**
	 * Creates a new compliance set with description, label and uri
	 * Instantiates the threat sets
	 * @param uri
	 * @param label
	 * @param description
	 */
	public ComplianceSetDTO(String uri, String label, String description) {
		this();
		setUri(uri);
		setLabel(label);
		setDescription(description);
	}

	public ComplianceSetDTO(ComplianceSet complianceSet) {
		setUri(complianceSet.getUri());
		setLabel(complianceSet.getLabel());
		setDescription(complianceSet.getDescription());
		this.compliant = complianceSet.isCompliant();
		this.systemThreats = complianceSet.getThreats().keySet();
	}

	/**
	 * Returns true if this compliance passes
	 * @return true if compliant
	 */
	public boolean isCompliant(){
		return compliant;
	}

	/**
	 * Gets the set of system level threat URIs
	 * @return set of uris of the system level threats
	 */
	public Set<String> getSystemThreats(){
		return systemThreats;
	}

}
