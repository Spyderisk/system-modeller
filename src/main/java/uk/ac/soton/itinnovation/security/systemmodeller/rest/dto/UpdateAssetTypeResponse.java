/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2016
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
//      Created By :            Ken Meacham
//      Modified By :           Ken Meacham
//      Created Date :          2017-08-01
//      Created for Project :   ASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest.dto;

import java.util.Set;

public class UpdateAssetTypeResponse extends UpdateAssetResponse {
	
	private Set<String> relations; // any deleted relations

	public UpdateAssetTypeResponse(UpdateAsset asset, Set<String> relations, boolean valid) {
		super(asset, valid);
		this.relations = relations;
	}

	public Set<String> getRelations() {
		return relations;
	}

	public void setRelations(Set<String> relations) {
		this.relations = relations;
	}
	
}
