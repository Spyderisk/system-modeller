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

import java.util.ArrayList;
import java.util.Collection;

import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class RootPatternDB extends EntityDB {
	public RootPatternDB() {
		// Defaults
		this.keyNodes = new ArrayList<>();
		this.rootNodes = new ArrayList<>();
		this.links = new ArrayList<>();
	}
	
	/*
	 * This is a domain model entity, no need for parent field. The label and description
	 * fields do exist in the domain model, but are used only by the domain model editor.
	 */
	
	@SerializedName("hasRootPattern")
	private String rootPattern;
	@SerializedName("hasKeyNode")
	private Collection<String> keyNodes;
	@SerializedName("hasRootNode")
	private Collection<String> rootNodes;
	@SerializedName("hasLink")
	private Collection<String> links;

}
