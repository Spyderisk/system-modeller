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
//      Created Date :          2017-08-01
//      Created for Project :   SHiELD
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.model.domain;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Link {

	public static final Logger logger = LoggerFactory.getLogger(Link.class);

	private String fromRole;
	private String toRole;
	private String type;

	public Link() {
		//default empty constructor
	}

	public Link(String fromRole, String toRole, String type) {

		this.fromRole = fromRole;
		this.toRole = toRole;
		this.type = type;
	}

	@Override
	public String toString() {
		return "(" + fromRole + ")-[" + type + "]-(" + toRole + ")";
	}

	@Override
	public boolean equals(Object other) {

		if (other!=null && other.getClass().equals(Link.class)) {
			return fromRole.equals(((Link) other).getFromRole())
					&& toRole.equals(((Link) other).getToRole())
					&& type.equals(((Link) other).getType());
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 13 * hash + Objects.hashCode(this.fromRole);
		hash = 13 * hash + Objects.hashCode(this.toRole);
		hash = 13 * hash + Objects.hashCode(this.type);
		return hash;
	}

	public String getFromRole() {
		return fromRole;
	}

	public void setFromRole(String fromRole) {
		this.fromRole = fromRole;
	}

	public String getToRole() {
		return toRole;
	}

	public void setToRole(String toRole) {
		this.toRole = toRole;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
