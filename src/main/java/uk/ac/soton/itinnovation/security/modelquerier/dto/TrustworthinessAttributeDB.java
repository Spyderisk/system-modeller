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

import java.util.Collection;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class TrustworthinessAttributeDB extends EntityDB {
	/*
	 * This is a domain model entity, no need for parent field.
	 */
	@SerializedName("rdfs#label")
	protected String label;
	@SerializedName("rdfs#comment")
	protected String description;
	
	private Collection<String> metaLocatedAt;
	private String minOf;			// Pointer from lowest level TrustworthinessAttribute to the average level TrustworthinessAttribute
	private String maxOf;			// Pointer from highest level TrustworthinessAttribute to the average level TrustworthinessAttribute
	private String hasMin;			// Pointer from average level TrustworthinessAttribute to the lowest level TrustworthinessAttribute
	private String hasMax;			// Pointer from average level TrustworthinessAttribute to the highest level TrustworthinessAttribute	
	@SerializedName("isVisible")
	private Boolean visible;	
}
