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
//      Created Date :          2016-11-21
//      Created for Project :   ASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest.dto;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeleteRelationResponse {

	private Set<String> relations;

	private boolean valid;

	public DeleteRelationResponse() {
		this.relations = new HashSet<>();
		this.valid = false;
	}

	public DeleteRelationResponse(Set<String> relations, boolean valid) {
		this.relations = relations;
		this.valid = valid;
	}

	public Set<String> getRelations() {
		return relations;
	}

	public void setRelations(Set<String> relations) {
		this.relations = relations;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
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

	public boolean isEmpty() {
		return (relations.isEmpty());
	}

}
