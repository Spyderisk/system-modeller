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
public class AssetDB extends EntityDB {
	public AssetDB() {
	}

	/*
	 * Domain and system model variants have a label field.
	 * 
	 * The domain model variant also has a description, which is used in the palette but
	 * not in the system model itself. It may make sense to define a separate AssetTypeDB
	 * entity with a description field, and drop it from the system model version.
	 * 
	 * Assets are also odd in that the system model variant uses the 'type' field to refer
	 * to its domain model parent type. All other entities use the 'parent' field for this,
	 * but Assets do no require a 'parent' field.
	 */	
	@SerializedName("rdfs#label")
	protected String label;
	@SerializedName("rdfs#comment")
	protected String description;
	
	// Cardinality represented by min and max values (with -1 meaning 'many'), to be deprecated
	private Integer minCardinality;
	private Integer maxCardinality;

	// Cardinality represented by a reference to a level in the new PopulationLevel scale
	private String population;

}
