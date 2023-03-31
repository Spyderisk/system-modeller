/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2017
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
//      Created By :            Stefanie Cox
//      Created Date :          2017-11-29
//      Created for Project :   SHiELD
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.model.domain;

import uk.ac.soton.itinnovation.security.model.Level;
import uk.ac.soton.itinnovation.security.model.SemanticEntity;

public class MisbehaviourSet extends SemanticEntity {

	private String misbehaviour;

	private String role;

	private Level impactLevel;

	public MisbehaviourSet() {}

	public MisbehaviourSet(String misbehaviour, String role) {

		this.misbehaviour = misbehaviour;
		this.role = role;
	}

	@Override
	public String toString() {
		return "(" + role + ") " + misbehaviour;
	}

	public String getMisbehaviour() {
		return misbehaviour;
	}

	public void setMisbehaviour(String misbehaviour) {
		this.misbehaviour = misbehaviour;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public Level getImpactLevel() {
		return impactLevel;
	}

	public void setImpactLevel(Level impactLevel) {
		this.impactLevel = impactLevel;
	}
}
