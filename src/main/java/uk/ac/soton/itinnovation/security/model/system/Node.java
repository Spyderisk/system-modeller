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
//		Modified By :	        Stefanie Cox
//      Created Date :          2016-08-23
//      Created for Project :   ASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.model.system;

import java.util.Objects;
import uk.ac.soton.itinnovation.security.model.SemanticEntity;

public class Node extends SemanticEntity {

	private String asset;

	private String role;

	private String assetLabel;

	private String roleLabel;

	public Node() {}

	public Node(String asset, String assetLabel, String role, String roleLabel) {

		this.asset = asset;
		this.assetLabel = assetLabel;
		this.role = role;
		this.roleLabel = roleLabel;
	}

	@Override
	public String toString() {
		return "(" + (roleLabel!=null?roleLabel:role) + ") " + (assetLabel!=null?assetLabel:asset);
	}

	@Override
	public boolean equals(Object other) {

		if (other!=null && other.getClass().equals(Node.class)) {
			return asset.equals(((Node) other).getAsset()) && role.equals(((Node) other).getRole());
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 71 * hash + Objects.hashCode(this.asset);
		hash = 71 * hash + Objects.hashCode(this.role);
		return hash;
	}

	public String getAsset() {
		return asset;
	}

	public void setAsset(String asset) {
		this.asset = asset;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getAssetLabel() {
		return assetLabel;
	}

	public void setAssetLabel(String assetLabel) {
		this.assetLabel = assetLabel;
	}

	public String getRoleLabel() {
		return roleLabel;
	}

	public void setRoleLabel(String roleLabel) {
		this.roleLabel = roleLabel;
	}
}
