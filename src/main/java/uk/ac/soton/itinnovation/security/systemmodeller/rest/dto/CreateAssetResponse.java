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
//      Created Date :          2016-11-09
//      Created for Project :   ASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest.dto;

import uk.ac.soton.itinnovation.security.model.system.Asset;

public class CreateAssetResponse {

	protected AssetDTO asset;
	protected boolean valid;

	public CreateAssetResponse(AssetDTO asset, boolean valid) {
		this.asset = asset;
		this.valid = valid;
	}
	
	public CreateAssetResponse(Asset asset, boolean valid) {
		this.asset = new AssetDTO(asset);
		this.valid = valid;
	}

	public AssetDTO getAsset() {
		return asset;
	}

	public void setAsset(AssetDTO asset) {
		this.asset = asset;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

}
