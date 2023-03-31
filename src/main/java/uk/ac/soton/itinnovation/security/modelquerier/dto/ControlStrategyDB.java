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
//      Created Date :          08/08/2019
//      Created for Project :   RESTASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelquerier.dto;
import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class ControlStrategyDB extends EntityDB {
	public ControlStrategyDB() {
		// Defaults
		controlSets = new ArrayList<>();
		mandatoryCS = new ArrayList<>();
		optionalCS = new ArrayList<>();
		blocks = new HashSet<>();
		mitigates = new HashSet<>();
		triggers = new HashSet<>();
	}
	
	/*
	 * Domain and system model variants use a description fields. The system model variant
	 * also has a parent field, and the domain model variant has a label field.
	 */	
	@SerializedName("rdfs#label")
	protected String label;
	@SerializedName("rdfs#comment")
	protected String description;
	protected String parent;
	
	@SerializedName("hasBlockingEffect")
	private String blockingEffect;
	@SerializedName("hasCoverageLevel")
	private String coverageLevel;
	@SerializedName("hasControlSet")
	private List<String> controlSets;
	@SerializedName("hasMandatoryCS")
	private List<String> mandatoryCS;
	@SerializedName("hasOptionalCS")
	private List<String> optionalCS;
	private Collection<String> blocks;
	private Collection<String> mitigates;
	private Collection<String> triggers;

	@SerializedName("isCurrentRisk")
	private Boolean currentRisk;			// The CSG is relevant in current risk calculations
	@SerializedName("isFutureRisk")
	private Boolean futureRisk;				// The CSG is relevant in future risk calculations
	@SerializedName("isEnabled")
	private Boolean enabled;				// The CSG status (calculated from its CS)

	private String minOf;					// Pointer from CSG for a lowest likelihood Threat to the CSG for the average likelihood Threat
	private String maxOf;					// Pointer from CSG for a highest likelihood Threat to the CSG for the average likelihood Threat
	private String hasMin;					// Pointer from CSG for an average likelihood Threat to the CSG for the lowest likelihood Threat
	private String hasMax;					// Pointer from CSG for an average likelihood Threat to the CSG for the highest likelihood Threat

	public boolean isCurrentRisk() {
		// If the property doesn't exist, default to true
		return currentRisk != null ? currentRisk : true;
	}

	public boolean isFutureRisk() {
		// If the property doesn't exist, default to true
		return futureRisk != null ? futureRisk : true;
	}

	public boolean isEnabled() {
		// If the property doesn't exist, default to false
		return enabled != null ? enabled : false;
	}

	public void setEnabled(Boolean status){
		// If the property should be false, remove it to save space
		if(status) {
			enabled = status;
		}
		else{
			enabled = null;
		}
	}	
}
