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

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.model.Level;

public class Threat extends AThreat {

	private final static Logger logger = LoggerFactory.getLogger(Threat.class);

	//all misbehaviours that could possibly be caused by this threat
	private final Map<String, MisbehaviourSet> misbehaviours;

	//these are direct effects, as found to be caused by this threat through the secondary effect calculations
	//private final Map<String, MisbehaviourSet> directEffects;

	//these are all effects, including but not limited to direct effects
	private final Map<String, MisbehaviourSet> indirectEffects;

	//Flag to indicate if secondary threat, otherwise primary
	private boolean secondaryThreat;

	private final Map<String, MisbehaviourSet> secondaryEffectConditions;

	private final Map<String, TrustworthinessAttributeSet> entryPoints;

	private Level frequency;

	private Level likelihood;

	private Level riskLevel;
	
	private boolean rootCause;

	public Threat() {

		super();

		misbehaviours = new HashMap<>();
		//directEffects = new HashMap<>();
		indirectEffects = new HashMap<>();
		secondaryEffectConditions = new HashMap<>();
		entryPoints = new HashMap<>();
	}

	/**
	 * This constructor is used when retrieving a threat from the store. No empty constructor is needed
	 * since threats are never created by the UI.
	 *
	 * @param uri the threat URI
	 * @param label the label
	 * @param description the system-specific description
	 * @param pattern the pattern it applies to
	 * @param threatensAsset the asset which is threatened
	 * @param type the (parent) type of the threat	 
	 * @param secondaryThreat whether the threat is secondary (otherwise primary)
	 * @param acceptanceJustification
	 * @param misbehaviours the misbehaviour sets caused by this threat
	 * @param secondaryEffectConditions the conditions under which this threat could be a secondary effect
	 * @param controlStrategies the threat's control strategies
	 */
	public Threat(String uri, String label, String description, Pattern pattern,
			String threatensAsset, String type, boolean secondaryThreat, String acceptanceJustification, 
			Map<String, MisbehaviourSet> misbehaviours, Map<String, MisbehaviourSet> secondaryEffectConditions,
			Map<String, ControlStrategy> controlStrategies) {

		this();
		setUri(uri);
		setLabel(label);
		setDescription(description);
		super.setPattern(pattern);
		super.setThreatensAssets(threatensAsset);
		super.setType(type);
		super.setAcceptanceJustification(acceptanceJustification);
		this.misbehaviours.putAll(misbehaviours);
		this.secondaryThreat = secondaryThreat;
		this.secondaryEffectConditions.putAll(secondaryEffectConditions);
		super.getControlStrategies().putAll(controlStrategies);
	}

	@Override
	public String toString() {

		String t = super.toString();
		if (!misbehaviours.isEmpty()) {
			t += "\t- potential misbehaviours\n";
			t = misbehaviours.values().stream().map(m -> "\t\t* " + m.toString() + "\n").reduce(t, String::concat);
		}
		t += "\t- " + (this.secondaryThreat ? "secondary" : "primary") + " threat\n";
		if (!secondaryEffectConditions.isEmpty()) {
			t += "\t- secondary effect conditions\n";
			t = secondaryEffectConditions.values().stream().map(m -> "\t\t* " + m.toString() + "\n").reduce(t, String::concat);
		}
		t += "\t- entry points\n";
		if (!entryPoints.isEmpty()) {
			t = entryPoints.values().stream().map(ep -> "\t\t* " + ep.toString() + "\n").reduce(t, String::concat);
		} else {
			t += "\t\tnone\n";
		}
		//if (!directEffects.isEmpty()) {
		//	t += "\t- direct effects\n";
		//	t = directEffects.values().stream().map(m -> "\t\t* " + m.toString() + "\n").reduce(t, String::concat);
		//}
		if (!indirectEffects.isEmpty()) {
			t += "\t- indirect effects\n";
			t = indirectEffects.values().stream().map(m -> "\t\t* " + m.toString() + "\n").reduce(t, String::concat);
		}
		if (likelihood!=null) {
			t += "\t- likelihood: " + likelihood + "\n";
		}
		if (frequency!=null) {
			t += "\t- frequency: " + frequency + "\n";
		}
		if (riskLevel!=null) {
			t += "\t- risk: " + riskLevel + "\n";
		}

		return t;
	}

	public Map<String, MisbehaviourSet> getMisbehaviours() {
		return misbehaviours;
	}

	/*
	public Map<String, MisbehaviourSet> getDirectEffects() {
		return directEffects;
	}
	*/

	public Map<String, MisbehaviourSet> getIndirectEffects() {
		return indirectEffects;
	}

	public boolean isSecondaryThreat() {
		return secondaryThreat;
	}

	public void setSecondaryThreat(boolean secondaryThreat) {
		this.secondaryThreat = secondaryThreat;
	}

	public Map<String, MisbehaviourSet> getSecondaryEffectConditions() {
		return secondaryEffectConditions;
	}

	public Map<String, TrustworthinessAttributeSet> getEntryPoints() {
		return entryPoints;
	}

	public Level getLikelihood() {
		return likelihood;
	}

	public void setLikelihood(Level likelihood) {
		this.likelihood = likelihood;
	}

	public Level getFrequency() {
		return frequency;
	}

	public void setFrequency(Level frequency) {
		this.frequency = frequency;
	}

	public Level getRiskLevel() {
		return riskLevel;
	}

	public void setRiskLevel(Level riskLevel) {
		this.riskLevel = riskLevel;
	}
	
	public boolean isRootCause() {
		return rootCause;
	}
	
	public void setRootCause(boolean rootCause) {
		this.rootCause = rootCause;
	}
}
