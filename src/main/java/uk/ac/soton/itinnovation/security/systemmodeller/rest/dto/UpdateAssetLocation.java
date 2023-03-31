/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2018
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
//      Created By :          Ken Meacham
//      Modified By :         Ken Meacham
//      Created Date :        2018-03-21
//      Created for Project : SHIELD
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest.dto;

import uk.ac.soton.itinnovation.security.model.system.Asset;

public final class UpdateAssetLocation extends UpdateAsset {

	private int iconX;

	private int iconY;

	public UpdateAssetLocation() {
	}

	public UpdateAssetLocation(Asset asset) {
		super(asset);
		this.iconX = asset.getIconX();
		this.iconY = asset.getIconY();
	}
	
	/**
	 * Create a new asserted Asset, normally using values from the UI
	 *
	 * @param uri the URI
	 * @param label the label
	 * @param type the asset class
	 * @param iconX the x coordinate on canvas
	 * @param iconY the y coordinate on canvas
	 */
	public UpdateAssetLocation(String uri, String label, String type, int iconX, int iconY) {
		super(uri, label);
		this.iconX = iconX;
		this.iconY = iconY;
	}

	@Override
	public String toString() {
		return "Asset <" + getUri() + ">:\n" +
			"ID:            " + getID() + "\n" +
			"Label:         " + getLabel() + "\n" +
			"X/Y:           " + iconX + "/" + iconY + "\n";
	}

	public void setIconPosition(int x, int y) {
		iconX = x;
		iconY = y;
	}

	public int getIconX() {
		return iconX;
	}

	public int getIconY() {
		return iconY;
	}

}
