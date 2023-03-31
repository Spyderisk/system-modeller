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

import com.google.gson.annotations.SerializedName;

import lombok.Data;

import java.util.Objects;

@Data
public class EntityDB {
	// TODO: Remove "rdf#"/"rdfs#" prefixes (requires changes to JenaQuerierDB code handling labels, comments, and types)

	private String uri;
	@SerializedName("rdf#type")
	protected String type;
	@SerializedName("hasID")
	protected String id;
	
	public String generateID() {
		return Integer.toHexString(oldHash());
	}

	// To ensure consistency with old version (for tests)
	private int oldHash() {
		int hash = 7;
		hash = 13 * hash + Objects.hashCode("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/" + this.uri);
		return hash;
	}
}
