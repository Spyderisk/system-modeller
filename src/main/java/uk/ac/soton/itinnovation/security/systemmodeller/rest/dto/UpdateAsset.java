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

public class UpdateAsset extends SemanticEntityDTO {

	public UpdateAsset() {
	}

	public UpdateAsset(Asset asset) {
		setUri(asset.getUri());
		setLabel(asset.getLabel());
		setDescription(asset.getDescription());
	}
	
	/**
	 * Create a new asserted Asset, normally using values from the UI
	 *
	 * @param uri the URI
	 * @param label the label
	 */
	public UpdateAsset(String uri, String label) {
		this();
		setUri(uri);
		setLabel(label);
	}

	@Override
	public String toString() {
		return "Asset <" + getUri() + ">:\n" +
			"ID:            " + getID() + "\n" +
			"Label:         " + getLabel() + "\n";
	}

}
