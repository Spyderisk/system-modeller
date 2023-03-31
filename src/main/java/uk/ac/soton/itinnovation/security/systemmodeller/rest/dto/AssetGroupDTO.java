/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2020
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
//      Created Date :          15 Jul 2020
//      Modified By :           
//      Created for Project :   ZDMP
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.ac.soton.itinnovation.security.model.system.Asset;
import uk.ac.soton.itinnovation.security.model.system.AssetGroup;

public class AssetGroupDTO extends SemanticEntityDTO {
	
	public AssetGroupDTO() {}
	
	public AssetGroupDTO(AssetGroup assetGroup) {
		setUri(assetGroup.getUri());
		setLabel(assetGroup.getLabel());
		setDescription(assetGroup.getDescription());
		left = assetGroup.getX() + "px";
		top = assetGroup.getY() + "px";
		width = assetGroup.getWidth();
		height = assetGroup.getHeight();
		expanded = assetGroup.isExpanded();
		assetIds = new ArrayList(); 
		Collection<Asset> assets = assetGroup.getAssets().values();
		for (Asset asset : assets) {
			assetIds.add(asset.getID());
		}
	}
	
	private String left;
	private String top;
	private int width;
	private int height;
	private boolean expanded;
	private List assetIds;

	public AssetGroup toAssetGroup() {
		String uri = this.getUri();
		String label = this.getName();
		String left = this.getLeft();
		String top = this.getTop();
		int x = -1;
		int y = -1;
		if (left != null) {
			x = Integer.parseInt(left.replace("px", ""));
		}
		if (top != null) {
			y = Integer.parseInt(top.replace("px", ""));
		}
		int width = this.getWidth();
		int height = this.getHeight();
		boolean expanded = this.isExpanded();
		
		Map<String, Asset> assets = new HashMap<>();
		
		AssetGroup assetGroup = new AssetGroup();
		
		assetGroup.setUri(uri);
		assetGroup.setLabel(label);
		assetGroup.setAssets(assets);
		assetGroup.setX(x);
		assetGroup.setY(y);
		assetGroup.setWidth(width);
		assetGroup.setHeight(height);
		assetGroup.setExpanded(expanded);
		
		return assetGroup;
	}
	
	public String getName() {
		return getLabel();
	}

	public void setName(String name) {
		setLabel(name);
	}

	public String getLeft() {
		return left;
	}

	public void setLeft(String left) {
		this.left = left;
	}

	public String getTop() {
		return top;
	}

	public void setTop(String top) {
		this.top = top;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public boolean isExpanded() {
		return expanded;
	}

	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}

	public List getAssetIds() {
		return assetIds;
	}

	public void setAssetIds(List assetIds) {
		this.assetIds = assetIds;
	}
}
