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
//      Created By :            Lee Mason
//      Created Date :          06 Jul 2020
//      Modified By :           Ken Meacham
//      Created for Project :   ZDMP
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.model.system;

import uk.ac.soton.itinnovation.security.model.SemanticEntity;

import java.util.HashMap;
import java.util.Map;

public class AssetGroup extends SemanticEntity {

    private int x;
    private int y;
    private int width;
    private int height;
	private boolean expanded;
	
    private Map<String, Asset> assets = new HashMap<>();

    public AssetGroup() {
        x = -1;
        y = -1;
        width = 400;
        height = 400;
		expanded = true;
    }

    public AssetGroup(String uri, String label, Map<String, Asset> assets, int x, int y, int width, int height, boolean expanded) {
        setUri(uri);
        setLabel(label);
        setAssets(assets);
        setX(x);
        setY(y);
		setWidth(width);
		setHeight(height);
		setExpanded(expanded);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
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

    public Map<String, Asset> getAssets() {
        return assets;
    }

    public void setAssets(Map<String, Asset> assets) {
        this.assets = assets;
    }

    public boolean addAsset(Asset asset) {
        return (assets.put(asset.getUri(), asset) == null);
    }

    public boolean removeAsset(Asset asset) {
        boolean present = assets.containsKey(asset.getUri());
        if (present) {
            assets.remove(asset.getUri());
        }
        return present;
    }
}
