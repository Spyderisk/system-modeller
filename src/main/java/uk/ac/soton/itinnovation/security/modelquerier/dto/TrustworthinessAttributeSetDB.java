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

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collection;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class TrustworthinessAttributeSetDB extends EntityDB {
	public TrustworthinessAttributeSetDB(){
		this.causedThreats = new ArrayList<>();
	}
	/*
	 * No need for parent, label or description fields in either domain or system model variants.
	 */	
	
	@SerializedName("hasTrustworthinessAttribute")
	private String trustworthinessAttribute;			// Type of trustworthiness attribute
	private String locatedAt;							// Asset with which it is associated
	@SerializedName("hasAssertedLevel")
	private String assertedLevel;						// Assumed TW level - user specified or default assumption
	@SerializedName("hasInferredLevel")
	private String inferredLevel;						// Calculated TW level - including the effects of threats
	@SerializedName("isExternalCause")
	private Boolean externalCause;						// True if the TWAS has a less than perfect assumed TW not undermined further by any threat
	@SerializedName("causesThreat")
	private Collection<String> causedThreats;			// Primary threats caused by the TWAS, where it is an external cause

	private String minOf;			// Pointer from lowest level TrustworthinessAttributeSet to the average level TrustworthinessAttributeSet
	private String maxOf;			// Pointer from highest level TrustworthinessAttributeSet to the average level TrustworthinessAttributeSet	
	private String hasMin;			// Pointer from average level TrustworthinessAttributeSet to the lowest level TrustworthinessAttributeSet
	private String hasMax;			// Pointer from average level TrustworthinessAttributeSet to the highest level TrustworthinessAttributeSet	

	public boolean isExternalCause() {
		return externalCause != null ? externalCause : false;
	}
	public void setExternalCause(Boolean value){
		if(value == null || !value)
			this.externalCause = null;
		else
			this.externalCause = value;
	}
}
