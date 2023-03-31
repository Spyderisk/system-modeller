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
//      Created By :            Stefanie Cox
//      Created Date :          2017-03-24
//      Created for Project :   ASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.model.system;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Link {

	public static final Logger logger = LoggerFactory.getLogger(Link.class);

	private String fromAsset;
	private String fromAssetLabel;
	private String fromRole;
	private String fromRoleLabel;
	private String toAsset;
	private String toAssetLabel;
	private String toRole;
	private String toRoleLabel;
	private String type;
	private String typeLabel;

	public Link() {
		//default empty constructor
	}

	public Link(String fromAsset, String fromAssetLabel, String fromRole, String fromRoleLabel, String toAsset,
		String toAssetLabel, String toRole, String toRoleLabel, String type, String typeLabel) {

		this.fromAsset = fromAsset;
		this.fromAssetLabel = fromAssetLabel!=null?fromAssetLabel:fromAsset;
		this.fromRole = fromRole;
		this.fromRoleLabel = fromRoleLabel;
		this.toAsset = toAsset;
		this.toAssetLabel = toAssetLabel!=null?toAssetLabel:toAsset;
		this.toRole = toRole;
		this.toRoleLabel = toRoleLabel;
		this.type = type;
		this.typeLabel = typeLabel;
	}

	@Override
	public String toString() {
		return "(" + (fromAssetLabel!=null?fromAssetLabel:fromAsset) + ")-[" + (typeLabel!=null?typeLabel:type) + "]-("
			+ (toAssetLabel!=null?toAssetLabel:toAsset) + ")";
	}

	@Override
	public boolean equals(Object other) {

		if (other!=null && other.getClass().equals(Link.class)) {
			return (fromAsset.equals(((Link) other).getFromAsset()) && toAsset.equals(((Link) other).getToAsset())
					&& fromRole.equals(((Link) other).getFromRole()) && toRole.equals(((Link) other).getToRole())
					&& type.equals(((Link) other).getType()));
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 13 * hash + Objects.hashCode(this.fromAsset);
		hash = 13 * hash + Objects.hashCode(this.fromRole);
		hash = 13 * hash + Objects.hashCode(this.toAsset);
		hash = 13 * hash + Objects.hashCode(this.toRole);
		hash = 13 * hash + Objects.hashCode(this.type);
		return hash;
	}

	public String getFromAsset() {
		return fromAsset;
	}

	public void setFromAsset(String fromAsset) {
		this.fromAsset = fromAsset;
	}

	public String getFromAssetLabel() {
		return fromAssetLabel;
	}

	public void setFromAssetLabel(String fromAssetLabel) {
		this.fromAssetLabel = fromAssetLabel;
	}

	public String getFromRole() {
		return fromRole;
	}

	public void setFromRole(String fromRole) {
		this.fromRole = fromRole;
	}

	public String getFromRoleLabel() {
		return fromRoleLabel;
	}

	public void setFromRoleLabel(String fromRoleLabel) {
		this.fromRoleLabel = fromRoleLabel;
	}

	public String getToAsset() {
		return toAsset;
	}

	public void setToAsset(String toAsset) {
		this.toAsset = toAsset;
	}

	public String getToAssetLabel() {
		return toAssetLabel;
	}

	public void setToAssetLabel(String toAssetLabel) {
		this.toAssetLabel = toAssetLabel;
	}

	public String getToRole() {
		return toRole;
	}

	public void setToRole(String toRole) {
		this.toRole = toRole;
	}

	public String getToRoleLabel() {
		return toRoleLabel;
	}

	public void setToRoleLabel(String toRoleLabel) {
		this.toRoleLabel = toRoleLabel;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTypeLabel() {
		return typeLabel;
	}

	public void setTypeLabel(String typeLabel) {
		this.typeLabel = typeLabel;
	}

}
