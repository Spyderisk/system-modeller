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
//      Created Date :          05/08/2019
//      Created for Project :   RESTASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelquerier.dto;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;

import com.google.gson.annotations.SerializedName;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class ThreatDB extends EntityDB {
	public ThreatDB() {
		this.misbehaviours = new ArrayList<>();
		this.secondaryEffectConditions = new ArrayList<>();
		this.entryPoints = new ArrayList<>();
		this.directMisbehaviours = new ArrayList<>();
		this.indirectMisbehaviours = new ArrayList<>();
		this.indirectThreats = new ArrayList<>();
		this.blockedByCSG = new HashSet<>();
		this.mitigatedByCSG = new HashSet<>();
		this.triggeredByCSG = new HashSet<>();
		this.causedBy = new HashSet<>();
	}

	/*
	 * Domain and system model variants use label and description fields. The system model variant
	 * also has a parent field.
	 */	
	@SerializedName("rdfs#label")
	protected String label;
	@SerializedName("rdfs#comment")
	protected String description;
	protected String parent;
	
	// Properties set in system model threats during validation, based on domain model equivalents
	private String threatens;								// Reference to a threatened asset, for which the threat is listed by the SSM GUI (it should use any causesMisbehaviour asset)
	private String appliesTo;								// Reference to the threat matching pattern
	@SerializedName("hasFrequency")
	private String frequency;								// Limit on threat likelihood, reflecting how difficult it is to carry out and/or how unlikely the pre-requisite conditions 
	@SerializedName("hasEntryPoint")
	private Collection<String> entryPoints;					// TWAS representing causes of a primary threat (although secondary threats can now also have causes represented by TWAS)
	@SerializedName("hasSecondaryEffectCondition")
	private Collection<String> secondaryEffectConditions;	// MS representing causes of a secondary threat (although primary threats can now also have causes represented by MS)
	@SerializedName("causesMisbehaviour")
	private Collection<String> misbehaviours;				// MS representing potential effects of a threat 
	@SerializedName("blockedBy")
	private Collection<String> blockedByCSG;				// CSGs that block the threat
	@SerializedName("mitigatedBy")
	private Collection<String> mitigatedByCSG;				// CSGs that mitigate [sic] the threat
	@SerializedName("triggeredBy")
	private Collection<String> triggeredByCSG;				// CSGs that trigger the threat
	@SerializedName("isTriggered")
	private Boolean triggered;								// True if the threat represents a side effect triggered by a CSG

	private String minOf;									// Pointer from lowest likelihood Threat to the average likelihood Threat
	private String maxOf;									// Pointer from highest likelihood Threat to the average likelihood Threat
	private String hasMin;									// Pointer from average likelihood Threat to the lowest likelihood Threat
	private String hasMax;									// Pointer from average likelihood Threat to the highest likelihood Threat

	@SerializedName("isCurrentRisk")
	private Boolean currentRisk;							// True if the threat is relevant in current risk calculations
	@SerializedName("isFutureRisk")
	private Boolean futureRisk;								// True if the threat is relevant in future risk calculations
	@SerializedName("isSecondaryThreat")
	private Boolean secondaryThreat;						// True if the threat represents an involuntary threat effect propagation mechanism
	@SerializedName("isNormalOp")
	private Boolean normalOperation;						// True if the threat represents a normal operational process

	// Properties set during risk calculation
	@SerializedName("hasPrior")
    @JsonProperty("likelihood")
	private String prior;									// Synonym for threat likelihood
	@SerializedName("hasRisk")
	private String risk;									// Risk level, calculated from the likleihood and impact of threat effects
	@SerializedName("causesDirectMisbehaviour")
	private Collection<String> directMisbehaviours;			// MS representing direct effects attributable to this threat 
	@SerializedName("causesIndirectMisbehaviour")
	private Collection<String> indirectMisbehaviours;		// MS representing all effects attributable (at least in part) to this threat (if a root/initial causes)
	@SerializedName("causesIndirectThreat")
	private Collection<String> indirectThreats;				// Threats attributable (at least in part) to this threat (if a root/initial cause)
	private Collection<String> causedBy;					// MS or TWAS that cause this threat, i.e., contribute to it having the likelihood it does

	@SerializedName("isInitialCause")
	private Boolean initialCause;							// True if the threat is an initial cause (depends only on external causes)
	@SerializedName("isRootCause")
	private Boolean rootCause;								// True if the threat is a root cause (depends only on external causes or normal operational effects)

	/**
	 * Returns true if this threat represents normal operation, i.e. its consequences may
	 * erode trustworthiness but in themselves they are normally desirable.
	 */
	public Boolean isNormalOperation(){
		return normalOperation != null ? normalOperation : false;
	}
	public void setNormalOperation(Boolean value){
		if(value == null || !value)
			this.normalOperation = null;
		else
			this.normalOperation = value;
	}

	/**
	 * Returns true if this threat represents an involuntary effect propagation mechanism.
	 */
	public Boolean isSecondaryThreat(){
		return secondaryThreat != null ? secondaryThreat : false;
	}
	public void setSecondaryThreat(Boolean value){
		if(value == null || !value)
			this.secondaryThreat = null;
		else
			this.secondaryThreat = value;
	}

	/**
	 * Returns true if the presence of security controls is a pre-requisite for this threat
	 */
	public Boolean isTriggered() {
		return triggered != null ? triggered : false;
	}
	public void setTriggered(Boolean value){
		if(value == null || !value)
			this.triggered = null;
		else
			this.triggered = value;
	}

	/**
	 * Returns true if this threat is an initial cause (start of a normal operation threat path)
	 */
	public Boolean isInitialCause() {
		return initialCause != null ? initialCause : false;
	}
	public void setInitialCause(Boolean value){
		if(value == null || !value)
			this.initialCause = null;
		else
			this.initialCause = value;
	}

	/**
	 * Returns true if this threat is a root cause (start of an attack path)
	 */
	public Boolean isRootCause() {
		return rootCause != null ? rootCause : false;
	}
	public void setRootCause(Boolean value){
		if(value == null || !value)
			this.rootCause = null;
		else
			this.rootCause = value;
	}

	/**
	 * Returns true if this threat is relevant in current risk calculations
	 */
	public boolean isCurrentRisk() {
		// If the property doesn't exist, default to true
		return currentRisk != null ? currentRisk : true;
	}
	public void setCurrentRisk(Boolean value){
		if(value == null || value) {
			// If the property doesn't exist, it is equivalent to true
			this.currentRisk = null;
		} else {
			// So it only needs to be stored if false
			this.currentRisk = value;
		}
	}

	/**
	 * Returns true if this threat is relevant in future risk calculations
	 */
	public boolean isFutureRisk() {
		// If the property doesn't exist, default to true
		return futureRisk != null ? futureRisk : true;
	}
	public void setFutureRisk(Boolean value){
		if(value == null || value) {
			// If the property doesn't exist, it is equivalent to true
			this.currentRisk = null;
		} else {
			// So it only needs to be stored if false
			this.currentRisk = value;
		}
	}
	
}
