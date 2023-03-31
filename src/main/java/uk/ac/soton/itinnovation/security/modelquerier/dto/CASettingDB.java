/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2022
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
//      Created By :            Mike Surridge
//      Created Date :          09/11/2022
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
public class CASettingDB extends EntityDB {
	/*
	 * This is a domain model entity, no need for parent, label or description fields
	 */	

	 @SerializedName("hasControl")
	private String control;
	private String metaLocatedAt;
	@SerializedName("hasLevel")
	private String level;
	@SerializedName("isAssertable")
	private Boolean assertible;
	private Boolean independentLevels;

	public boolean getIndependentLevels(){
		// if independentLevels is not set, assume it is false
		return independentLevels != null ? independentLevels : false;
	}
}
