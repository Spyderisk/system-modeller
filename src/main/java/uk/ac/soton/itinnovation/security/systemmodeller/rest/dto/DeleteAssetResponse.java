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
//      Modified By :           Stefanie Wiegand
//      Created Date :          2016-08-18
//      Created for Project :   ASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest.dto;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeleteAssetResponse {

	private Set<String> assets;

	private Set<String> relations;

	private String assetGroup;

	private boolean valid;

	public DeleteAssetResponse() {
		this.assets = new HashSet<>();
		this.relations = new HashSet<>();
		this.valid = false;
	}
	
	public Set<String> getAssets() {
		return assets;
	}

	public void setAssets(Set<String> assets) {
		this.assets = assets;
	}

	public void addAssets(Set<String> assetIDs) {

		if (assetIDs == null) {
			return;
		}

		if (this.assets == null) {
			this.assets = new HashSet<>();
		}

		assetIDs.stream().filter((assetID) -> (!this.assets.contains(assetID))).forEach((assetID) -> {
			if (assetID.contains("#")) {
				try {
					throw new Exception("Bad asset ID (contains #): " + assetID);
				} catch (Exception ex) {
					Logger.getLogger(DeleteRelationResponse.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
			else {
				this.assets.add(assetID);
			}
		});
	}

	public Set<String> getRelations() {
		return relations;
	}

	public void setRelations(Set<String> relations) {
		this.relations = relations;
	}

	public void addRelations(Set<String> relationIDs) {
		if (relationIDs == null) {
			return;
		}

		if (this.relations == null) {
			this.relations = new HashSet<>();
		}

		relationIDs.stream().filter((relationID) -> (!this.relations.contains(relationID))).forEach((relationID) -> {
			if (relationID.contains("#")) {
				try {
					throw new Exception("Bad relation ID (contains #): " + relationID);
				} catch (Exception ex) {
					Logger.getLogger(DeleteRelationResponse.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
			else {
				this.relations.add(relationID);
			}
		});
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public void setAssetGroup(String assetGroup) {
		this.assetGroup = assetGroup;
	}

	public String getAssetGroup() {
		return assetGroup;
	}

}
