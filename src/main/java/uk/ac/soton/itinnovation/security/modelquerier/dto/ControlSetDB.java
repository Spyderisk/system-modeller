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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class ControlSetDB extends EntityDB {
	public ControlSetDB (){
	}

	/*
	 * No need for parent, label or description fields in either domain or system model variants.
	 */	
		
	private String locatedAt;					// Asset with which the control is associated
	@SerializedName("hasControl")
	private String control;						// Control type
	@SerializedName("isProposed")
	private Boolean proposed;					// Control status
	@SerializedName("hasCoverageLevel")
	private String coverageLevel;				// Control coverage level

	private String minOf;						// Pointer from lowest coverage ControlSet to the average coverage ControlSet
	private String maxOf;						// Pointer from highest coverage ControlSet to the average coverage ControlSet
	private String hasMin;						// Pointer from average coverage ControlSet to the lowest coverage ControlSet
	private String hasMax;						// Pointer from average coverage ControlSet to the highest coverage ControlSet
	
	public boolean isProposed() {
		return proposed != null ? proposed : false;
	}

	public Boolean getProposed() {
		return isProposed();
	}

	public void setProposed(Boolean status){
		if (status) {
			proposed = status;
		}
		else {
			proposed = null;
		}
	}
}
