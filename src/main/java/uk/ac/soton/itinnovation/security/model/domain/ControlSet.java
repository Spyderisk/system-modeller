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

import java.util.Objects;
import uk.ac.soton.itinnovation.security.model.SemanticEntity;

public class ControlSet extends SemanticEntity {

	private String control;

	private String role;

	public ControlSet(String control, String role) {

		this.control = control;
		this.role = role;
	}

	@Override
	public String toString() {
		return "(" + role + ") " + control;
	}

	@Override
	public boolean equals(Object other) {

		if (other!=null && other.getClass().equals(ControlSet.class)) {
			return control.equals(((ControlSet) other).getControl()) && role.equals(((ControlSet) other).getRole());
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 71 * hash + Objects.hashCode(this.control);
		hash = 71 * hash + Objects.hashCode(this.role);
		return hash;
	}

	public String getControl() {
		return control;
	}

	public void setControl(String control) {
		this.control = control;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}
}
