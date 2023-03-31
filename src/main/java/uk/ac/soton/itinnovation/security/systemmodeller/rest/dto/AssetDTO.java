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
//      Created By :            Ken Meacham
//      Modified By :           Ken Meacham
//      Created Date :          2018-03-09
//      Created for Project :   SHIELD
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest.dto;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import uk.ac.soton.itinnovation.security.model.system.Asset;
import uk.ac.soton.itinnovation.security.model.system.ControlSet;

public final class AssetDTO extends SemanticEntityDTO {

	private String type;

	//an asset is asserted if it hasn't been created by a pattern (core:createdByPattern)
	//it is also asserted if it's in the system model graph, not the inferred graph
	private boolean asserted;

	private boolean visible;

	private int iconX;

	private int iconY;

	private int minCardinality;
	private int maxCardinality;

	private String population;

	private Map<String, ControlSet> controlSets;

	private Set<String> misbehaviourSets;

	//these (invisible) assets are displayed on this asset
	private Set<String> inferredAssets;

	private Set<String> trustworthinessAttributeSets;

	public AssetDTO() {
		controlSets = new HashMap<>();
		misbehaviourSets = new HashSet<>();
		inferredAssets = new HashSet<>();
		trustworthinessAttributeSets = new HashSet<>();

		//no default cardinality restrictions
		minCardinality = -1;
		maxCardinality = -1;
	}

	public AssetDTO(Asset asset) {
		setUri(asset.getUri());
		setLabel(asset.getLabel());
		setDescription(asset.getDescription());
		this.type = asset.getType();
		this.asserted = asset.isAsserted();
		this.visible = asset.isVisible();
		this.iconX = asset.getIconX();
		this.iconY = asset.getIconY();
		this.maxCardinality = asset.getMaxCardinality();
		this.minCardinality = asset.getMinCardinality();
		this.population = asset.getPopulation();
		this.controlSets = asset.getControlSets();
		this.misbehaviourSets = asset.getMisbehaviourSets().keySet(); // get misbehaviour set URIs
		this.inferredAssets = asset.getInferredAssets();
		this.trustworthinessAttributeSets = asset.getTrustworthinessAttributeSets().keySet(); //get twas URIs
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
	public AssetDTO(String uri, String label, String type, int iconX, int iconY) {

		this();
		setUri(uri);
		setLabel(label);
		this.type = type;
		this.iconX = iconX;
		this.iconY = iconY;
		this.asserted = true;
		this.visible = true;
	}

	@Override
	public String toString() {
		return "Asset <" + getUri() + ">:\n" +
			"ID:            " + getID() + "\n" +
			"Label:         " + getLabel() + "\n" +
			"Type:          " + type + "\n" +
			"X/Y:           " + iconX + "/" + iconY + "\n" +
			"Cardinality:   " + minCardinality + "/" + maxCardinality + "\n" +
			"Population:    " + population + "\n" +
			"Asserted?:     " + asserted + "\n" +
			"Visible?:      " + visible + "\n" +
			"Controls:      " + Arrays.toString(controlSets.values().toArray()) + "\n" +
			"Misbehaviours: " + Arrays.toString(misbehaviourSets.toArray()) + "\n" +
			"TWAS:          " + Arrays.toString(trustworthinessAttributeSets.toArray()) + "\n" +
			"Inf.assets:    " + Arrays.toString(inferredAssets.toArray()) + "\n";
	}

	public String getType() {
		return type;
	}

	public void setType(String typename) {
		this.type = typename;
	}

	public boolean isAsserted() {
		return asserted;
	}

	public void setAsserted(boolean asserted) {
		this.asserted = asserted;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
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

	public int getMinCardinality() {
		return minCardinality;
	}

	public void setMinCardinality(int minCardinality) {
		this.minCardinality = minCardinality;
	}

	public int getMaxCardinality() {
		return maxCardinality;
	}

	public void setMaxCardinality(int maxCardinality) {
		this.maxCardinality = maxCardinality;
	}

	public String getPopulation() {
		return population;
	}

	public void setPopulation(String population) {
		this.population = population;
	}

	public Map<String, ControlSet> getControlSets() {
		return controlSets;
	}

	public void setControlSets(Map<String, ControlSet> controlSets) {
		this.controlSets = controlSets;
	}

	public Set<String> getMisbehaviourSets() {
		return misbehaviourSets;
	}

	public void setMisbehaviourSets(Set<String> misbehaviourSets) {
		this.misbehaviourSets = misbehaviourSets;
	}

	public Set<String> getInferredAssets() {
		return inferredAssets;
	}

	public void setInferredAssets(Set<String> inferredAssets) {
		this.inferredAssets = inferredAssets;
	}

	public Set<String> getTrustworthinessAttributeSets() {
		return trustworthinessAttributeSets;
	}

	public void setTrustworthinessAttributeSets(Set<String> trustworthinessAttributeSets) {
		this.trustworthinessAttributeSets = trustworthinessAttributeSets;
	}
}
