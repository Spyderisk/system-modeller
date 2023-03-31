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
//      Created Date :          11/11/2022
//      Created for Project :   FOGPROTECT
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelquerier.dto;

import java.util.Collection;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class ControlDB extends EntityDB {
	/*
	 * This is a domain model entity, no need for parent field.
	 */
	@SerializedName("rdfs#label")
	protected String label;
	@SerializedName("rdfs#comment")
	protected String description;
	
	private Collection<String> metaLocatedAt;
	private String minOf;			// Pointer from lowest coverage Control to the average coverage Control
	private String maxOf;			// Pointer from highest coverage Control to the average coverage Control
	private String hasMin;			// Pointer from average coverage Control to the lowest coverage Control
	private String hasMax;			// Pointer from average coverage Control to the highest coverage Control	
	@SerializedName("isVisible")
	private Boolean visible;
}
