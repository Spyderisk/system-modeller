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

import java.util.HashSet;
import java.util.Set;
import uk.ac.soton.itinnovation.security.model.Level;
import uk.ac.soton.itinnovation.security.model.SemanticEntity;

public class MisbehaviourSet extends SemanticEntity {

	private String misbehaviour;

	private String misbehaviourLabel;

	private String asset;

	private String assetLabel;
	
	private boolean visible = true; //visible by default

	private Level impactLevel;

	private Level likelihood;

	private Level riskLevel;

	private boolean impactLevelAsserted; //is the impact level asserted by the user

	private boolean normalOpEffect; // True if the MS has non-zero likelihood caused by normal operational threats and their secondary effects

	private final Set<String> directCauses;

	private final Set<String> indirectCauses;
	
	private final Set<String> rootCauses;

	private final Set<String> directEffects;
	
	public MisbehaviourSet() {
		directCauses = new HashSet<>();
		indirectCauses = new HashSet<>();
		rootCauses = new HashSet<>();
		directEffects = new HashSet<>();
	}

	/**
	 * Create a new MisbehaviourSet
	 *
	 * @param uri the URI of the misbehaviour set
	 * @param misbehaviour the misbehaviour caused
	 * @param misbehaviourLabel
	 * @param asset the asset on which the misbehaviour is/would be observed
	 * @param assetLabel
	 */
	public MisbehaviourSet(String uri, String misbehaviour, String misbehaviourLabel, String asset, String assetLabel) {

		this();
		setUri(uri);
		this.misbehaviour = misbehaviour;
		this.misbehaviourLabel = misbehaviourLabel;
		this.asset = asset;
		this.assetLabel = assetLabel;
	}

	@Override
	public String toString() {
		String impactStr = impactLevel != null ? impactLevel.getLabel()!=null?impactLevel.getLabel():impactLevel.getUri() : "null";
		String likelihoodStr = likelihood != null ? likelihood.getLabel()!=null?likelihood.getLabel():likelihood.getUri() : "null";
		String riskStr = riskLevel != null ? riskLevel.getLabel()!=null?riskLevel.getLabel():riskLevel.getUri() : "null";

		return misbehaviourLabel + " at " + assetLabel + " (impact level: " + impactStr
										+ ", likelihood: " + likelihoodStr
										+ ", risk level: " + riskStr + ")"
				;
	}

	/**
	 * @return the misbehaviour
	 */
	public String getMisbehaviour() {
		return misbehaviour;
	}

	/**
	 * @param misbehaviour the misbehaviour to set
	 */
	public void setMisbehaviour(String misbehaviour) {
		this.misbehaviour = misbehaviour;
	}

	/**
	 * @return the asset
	 */
	public String getAsset() {
		return asset;
	}

	/**
	 * @param asset the asset to set
	 */
	public void setAsset(String asset) {
		this.asset = asset;
	}

	public String getMisbehaviourLabel() {
		return misbehaviourLabel;
	}

	public void setMisbehaviourLabel(String misbehaviourLabel) {
		this.misbehaviourLabel = misbehaviourLabel;
	}

	public String getAssetLabel() {
		return assetLabel;
	}

	public void setAssetLabel(String assetLabel) {
		this.assetLabel = assetLabel;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public Level getImpactLevel() {
		return impactLevel;
	}

	public void setImpactLevel(Level impactLevel) {
		this.impactLevel = impactLevel;
	}

	public Level getLikelihood() {
		return likelihood;
	}

	public void setLikelihood(Level likelihood) {
		this.likelihood = likelihood;
	}

	public Level getRiskLevel() {
		return riskLevel;
	}

	public void setRiskLevel(Level riskLevel) {
		this.riskLevel = riskLevel;
	}

	public boolean isImpactLevelAsserted() {
		return impactLevelAsserted;
	}

	public void setImpactLevelAsserted(boolean impactLevelAsserted) {
		this.impactLevelAsserted = impactLevelAsserted;
	}

	public boolean isNormalOpEffect() {
		return normalOpEffect;
	}

	public void setNormalOpEffect(boolean normalOpEffect) {
		this.normalOpEffect = normalOpEffect;
	}

	public Set<String> getDirectCauses() {
		return directCauses;
	}

	public Set<String> getIndirectCauses() {
		return indirectCauses;
	}

	public Set<String> getRootCauses() {
		return rootCauses;
	}

	public Set<String> getDirectEffects() {
		return directEffects;
	}
	
}
