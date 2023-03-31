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
public class ConstructionPatternDB extends EntityDB {
	public ConstructionPatternDB() {
		// Defaults
		this.inferredNodes = new ArrayList<>();
		this.inferredLinks = new ArrayList<>();
		this.inferredNodeSettings = new ArrayList<>();
		this.priority = Integer.MAX_VALUE;
		this.iterate = false;
		this.maxIterations = 1;
	}
	
	/*
	 * This is a domain model entity only, so doesn't need a parent field. The label and
	 * description fields are only used by the domain model editor.
	 */

	@SerializedName("hasMatchingPattern")
	private String matchingPattern;
	@SerializedName("hasInferredNode")
	private Collection<String> inferredNodes;
	@SerializedName("hasInferredLink")
	private Collection<String> inferredLinks;
	@SerializedName("hasPriority")
	private int priority;
	private Boolean iterate;
	@SerializedName("hasInferredNodeSetting")
	private Collection<String> inferredNodeSettings;
	private int maxIterations;
}
