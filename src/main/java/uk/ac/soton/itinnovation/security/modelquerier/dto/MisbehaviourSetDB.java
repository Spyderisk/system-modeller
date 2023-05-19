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

import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class MisbehaviourSetDB extends EntityDB {
	public MisbehaviourSetDB(){
		this.defaultLevel = null;
		this.causedThreats = new ArrayList<>();
		this.causedBy = new HashSet<>();
	}

	/*
	 * No need for parent, label or description fields in either domain or system model variants.
	 */	
	
	// Properties set during validation
	@SerializedName("hasMisbehaviour")
	private String misbehaviour;						// The type of misbehaviour
	private String locatedAt;							// The asset (or in the domain model, role) where it occurs
	@SerializedName("hasImpactLevel")
	private String impactLevel;							// Impact of this misbehaviour located at this asset, should it occur
	private String defaultLevel;						// Default impact level - currently used to temporarily store this level during the risk calculation

	// Properties set during risk calculation
	@SerializedName("hasPrior")
    @JsonProperty("likelihood")
	private String prior;								// Synonym for 'likelihood'
	@SerializedName("hasRisk")
	private String risk;								// Risk level, calculated from the MisbehaviourSet likleihood and impact level
	@SerializedName("isExternalCause")
	private Boolean externalCause;						// True if the MS has non-zero likelihood caused by a trustworthiness assertion and not by any threat
	@SerializedName("isNormalOpEffect")
	private Boolean normalOpEffect;						// True if the MS has non-zero likelihood caused by normal operational threats and their secondary effects
	@SerializedName("causesThreat")
	private Collection<String> causedThreats;			// Threats caused by the MisbehaviourSet, including by undermining the related TWAS 
	private Collection<String> causedBy;				// Threats that cause the MisbehaviourSet, i.e., are responsible for its likelihood level

	private String minOf;								// Pointer from lowest likelihood MisbehaviourSet to the average likelihood MisbehaviourSet
	private String maxOf;								// Pointer from highest likelihood MisbehaviourSet to the average likelihood MisbehaviourSet
	private String hasMin;								// Pointer from average likelihood MisbehaviourSet to the lowest likelihood MisbehaviourSet
	private String hasMax;								// Pointer from average likelihood MisbehaviourSet to the highest likelihood MisbehaviourSet	

	public boolean isExternalCause() {
		return externalCause != null ? externalCause : false;
	}
	public void setExternalCause(Boolean value){
		if(value == null || !value)
			this.externalCause = null;
		else
			this.externalCause = value;
	}
	
	public boolean isNormalOpEffect() {
		return normalOpEffect != null ? normalOpEffect : false;
	}
	public void setNormalOpEffect(Boolean value){
		if(value == null || !value)
			this.normalOpEffect = null;
		else
			this.normalOpEffect = value;
	}	
}
