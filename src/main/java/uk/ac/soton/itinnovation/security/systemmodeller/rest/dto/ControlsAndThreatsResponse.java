/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2016
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
//      Modified By :           Stefanie Wiegand
//      Created Date :          2016-08-23
//      Created for Project :   ASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest.dto;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import uk.ac.soton.itinnovation.security.model.system.ComplianceSet;
import uk.ac.soton.itinnovation.security.model.system.ComplianceThreat;
import uk.ac.soton.itinnovation.security.model.system.ControlSet;
import uk.ac.soton.itinnovation.security.model.system.Threat;

public class ControlsAndThreatsResponse {

	private Set<ThreatDTO> threats;
	private Set<ComplianceThreatDTO> complianceThreats;
	private Set<ComplianceSetDTO> complianceSets;
	//this is needed for displaying control strategies on an asset which have control sets that
	//refer to another asset than the currently selected one
	private Collection<ControlSet> controlSets;

	public ControlsAndThreatsResponse() {
		this.controlSets = new HashSet<>();
		this.threats = new HashSet<>();
		this.complianceThreats = new HashSet<>();
		this.complianceSets = new HashSet<>();
	}

	public ControlsAndThreatsResponse(Set<ControlSet> controls, Set<Threat> threats) {
		this.controlSets = controls;
		this.threats = getThreatDTOs(threats);
	}

	// Get Threats as DTOs
	private Set<ThreatDTO> getThreatDTOs(Set<Threat> threats) {
		Set<ThreatDTO> threatsSet = new HashSet<>();
		for (Threat threat : threats) {
			ThreatDTO threatDto = new ThreatDTO(threat);
			threatsSet.add(threatDto);
		}
		return threatsSet;
	}

	// Get ComplianceThreats as DTOs
	private Set<ComplianceThreatDTO> getComplianceThreatDTOs(Set<ComplianceThreat> threats) {
		Set<ComplianceThreatDTO> threatsSet = new HashSet<>();
		for (ComplianceThreat threat : threats) {
			ComplianceThreatDTO threatDto = new ComplianceThreatDTO(threat);
			threatsSet.add(threatDto);
		}
		return threatsSet;
	}

	// Get ComplianceSets as DTOs
	private Set<ComplianceSetDTO> getComplianceSetDTOs(Set<ComplianceSet> complianceSets) {
		Set<ComplianceSetDTO> complianceSetsSet = new HashSet<>();
		for (ComplianceSet complianceSet : complianceSets) {
			ComplianceSetDTO complianceSetDTO = new ComplianceSetDTO(complianceSet);
			complianceSetsSet.add(complianceSetDTO);
		}
		return complianceSetsSet;
	}

	public Collection<ControlSet> getControlSets() {
		return controlSets;
	}

	public void setControlSets(Collection<ControlSet> controls) {
		this.controlSets = controls;
	}

	public Set<ThreatDTO> getThreats() {
		return threats;
	}

	public void setThreats(Set<Threat> threats) {
		this.threats = getThreatDTOs(threats);
	}

	public Set<ComplianceThreatDTO> getComplianceThreats() {
		return complianceThreats;
	}

	public void setComplianceThreats(Map<String, ComplianceThreat> complianceThreatsMap) {
		Set<ComplianceThreat> threats = new HashSet<>();
		threats.addAll(complianceThreatsMap.values());
		this.complianceThreats = getComplianceThreatDTOs(threats);
	}
	
	public void setComplianceThreats(Set<ComplianceThreat> complianceThreats) {
		this.complianceThreats = getComplianceThreatDTOs(complianceThreats);
	}

	public Set<ComplianceSetDTO> getComplianceSets() {
		return complianceSets;
	}

	public void setComplianceSets(Set<ComplianceSet> complianceSets) {
		this.complianceSets = getComplianceSetDTOs(complianceSets);
	}

}
