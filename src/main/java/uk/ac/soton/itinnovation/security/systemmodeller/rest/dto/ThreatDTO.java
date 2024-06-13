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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.model.Level;
import uk.ac.soton.itinnovation.security.model.domain.ControlStrategy.ControlStrategyType;
import uk.ac.soton.itinnovation.security.model.system.ComplianceThreat;
import uk.ac.soton.itinnovation.security.model.system.ControlSet;
import uk.ac.soton.itinnovation.security.model.system.ControlStrategy;
import uk.ac.soton.itinnovation.security.model.system.Pattern;
import uk.ac.soton.itinnovation.security.model.system.Threat;
import uk.ac.soton.itinnovation.security.model.system.TrustworthinessAttributeSet;

public class ThreatDTO extends SemanticEntityDTO {

	private final static Logger logger = LoggerFactory.getLogger(ThreatDTO.class);

	private Pattern pattern;

	private String threatensAssets;

	private String type;

	private boolean resolved;

	private String acceptanceJustification;

	//all misbehaviours that could possibly be caused by this threat
	private final Set<String> misbehaviours;

	//these are all effects, including but not limited to direct effects
	private final Set<String> indirectEffects;

	//Flag to indicate if secondary threat, otherwise primary
	private boolean secondaryThreat;

	private boolean normalOperation;

	private final Set<String> secondaryEffectConditions;

	//Map of control strategies to their type
	private final Map<String, ControlStrategyType> controlStrategies;

	private Map<String, ControlStrategy> csgs; //this is only used internally

	private final Set<String> entryPoints;

	private Level likelihood;

	private Level riskLevel;

	private boolean rootCause;
	
	public ThreatDTO() {

		misbehaviours = new HashSet<>();
		indirectEffects = new HashSet<>();
		secondaryEffectConditions = new HashSet<>();
		controlStrategies = new HashMap<>();
		entryPoints = new HashSet<>();
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
	 * @param resolved whether the threat is treated
	 * @param misbehaviours the misbehaviour sets caused by this threat
	 * @param secondaryEffectConditions the conditions under which this threat could be a secondary effect
	 * @param controlStrategies the threat's control strategies
	 */
	public ThreatDTO(String uri, String label, String description, Pattern pattern,
			String threatensAsset, String type, boolean secondaryThreat, boolean resolved, String acceptanceJustification, 
			Set<String> misbehaviours, Set<String> secondaryEffectConditions,
			Map<String, ControlStrategy> controlStrategies) {

		this();
		setUri(uri);
		setLabel(label);
		setDescription(description);
		this.pattern = pattern;
		this.threatensAssets = threatensAsset;
		this.type = type;
		this.secondaryThreat = secondaryThreat;
		this.resolved = resolved;
		this.acceptanceJustification = acceptanceJustification;
		this.misbehaviours.addAll(misbehaviours);
		this.secondaryEffectConditions.addAll(secondaryEffectConditions);
		extractControlStrategiesMap(controlStrategies);
	}

	public ThreatDTO(Threat threat) {
		setUri(threat.getUri());
		setLabel(threat.getLabel());
		setDescription(threat.getDescription());
		this.pattern = threat.getPattern();
		this.threatensAssets = threat.getThreatensAssets();
		this.type = threat.getType();
		this.resolved = threat.isResolved();
		this.acceptanceJustification = threat.getAcceptanceJustification();
		this.misbehaviours = threat.getMisbehaviours().keySet();
		this.indirectEffects = threat.getIndirectEffects().keySet();
		this.secondaryThreat = threat.isSecondaryThreat();
		this.secondaryEffectConditions = threat.getSecondaryEffectConditions().keySet();
		this.normalOperation = threat.isNormalOperation();
		this.controlStrategies = new HashMap<>();
		extractControlStrategiesMap(threat.getControlStrategies());
		this.entryPoints = threat.getEntryPoints().keySet();
		this.likelihood = threat.getLikelihood();
		this.riskLevel = threat.getRiskLevel();
		this.rootCause = threat.isRootCause();
	}

	public ThreatDTO(ComplianceThreat threat) {
		setUri(threat.getUri());
		setLabel(threat.getLabel());
		setDescription(threat.getDescription());
		
		this.misbehaviours = new HashSet<>();
		this.indirectEffects = new HashSet<>();
		this.secondaryThreat = false; //cannot be a secondary threat
		this.secondaryEffectConditions = new HashSet<>();
		this.normalOperation = false; //cannot be a normal operation
		this.entryPoints = new HashSet<>();

		this.pattern = threat.getPattern();
		this.threatensAssets = threat.getThreatensAssets();
		this.type = threat.getType();
		this.resolved = threat.isResolved();
		this.acceptanceJustification = threat.getAcceptanceJustification();
		this.controlStrategies = new HashMap<>();
		extractControlStrategiesMap(threat.getControlStrategies());
	}

	private void extractControlStrategiesMap(Map<String, ControlStrategy> csgs) {
		this.csgs = csgs;

		String threatURI = this.getUri();

		for (ControlStrategy csg : csgs.values()) {
			//Get the specific CSG type for this threat
			ControlStrategyType type = csg.getThreatCsgTypes().get(threatURI);
			this.controlStrategies.put(csg.getUri(), type);
		}
	}

	@Override
	public String toString() {
		String t = getLabel() + " <" + getUri() + ">:\n" +
			"\t- secondary " + secondaryThreat + "\n" +
			"\t- threatens " + threatensAssets + "\n" +
			"\t- pattern   " + pattern + "\n" +
			"\t- type      " + type + "\n" +
			"\t- desc      " + getDescription() + "\n" +
			"\t- rootCause? " + rootCause + "\n" +
			"\t- resolved? " + resolved + "\n" +
			"\t- accepted? " + (acceptanceJustification!=null?acceptanceJustification:"not accepted") + "\n";

		if (!misbehaviours.isEmpty()) {
			t += "\t- potential misbehaviours\n";
			t = misbehaviours.stream().map(m -> "\t\t* " + m + "\n").reduce(t, String::concat);
		}
		if (!secondaryEffectConditions.isEmpty()) {
			t += "\t- secondary effect conditions\n";
			t = secondaryEffectConditions.stream().map(m -> "\t\t* " + m + "\n").reduce(t, String::concat);
		}
		if (!indirectEffects.isEmpty()) {
			t += "\t- indirect effects\n";
			t = indirectEffects.stream().map(m -> "\t\t* " + m + "\n").reduce(t, String::concat);
		}
		t += "\t- control strategies\n";
		if (!controlStrategies.isEmpty()) {
			t = controlStrategies.values().stream().map(m -> "\t\t* " + m.toString() + "\n").reduce(t, String::concat);
		} else {
			t += "\t\tnone\n";
		}
		t += "\t- entry points\n";
		if (!entryPoints.isEmpty()) {
			t = entryPoints.stream().map(ep -> "\t\t* " + ep.toString() + "\n").reduce(t, String::concat);
		} else {
			t += "\t\tnone\n";
		}
		if (likelihood!=null) {
			t += "\t- likelihood: " + likelihood + "\n";
		}
		if (riskLevel!=null) {
			t += "\t- risk: " + riskLevel + "\n";
		}

		return t;
	}

	/**
	 * Get all combinations of control sets that would block the threat
	 *
	 * @return a set of control set combinations
	 */
	public List<SortedSet<String>> getAllControlCombinations() {
		String threatURI = this.getUri();

		//make sure the order of the CSGs is consistent. Use URI. We don't care what the order is.
		SortedMap<String, ControlStrategy> csgsSorted = new TreeMap<>();
		csgsSorted.putAll(this.csgs);

		Collection<ControlStrategy> csgValues = csgsSorted.values();
		csgValues.removeIf(csg -> csg.getType(threatURI).equals(ControlStrategyType.TRIGGER)); //filter out triggering CSGs

		List<SortedSet<String>> combinations = new ArrayList<>();
		for (ControlStrategy csg: csgValues) {
			SortedSet<String> oneCombination = new TreeSet<>();
			oneCombination.addAll(csg.getMandatoryControlSets().keySet());
			oneCombination.addAll(csg.getOptionalControlSets().keySet());
			combinations.add(oneCombination);
		}

		return combinations;
	}

	public boolean getResolved() {
		return resolved;
	}

	public void setResolved(boolean resolved) {
		this.resolved = resolved;
	}

	public String getAcceptanceJustification() {
		return acceptanceJustification;
	}

	public void setAcceptanceJustification(String acceptanceJustification) {
		this.acceptanceJustification = acceptanceJustification;
	}

	public Set<String> getMisbehaviours() {
		return misbehaviours;
	}

	public Set<String> getIndirectEffects() {
		return indirectEffects;
	}

	public boolean isSecondaryThreat() {
		return secondaryThreat;
	}

	public void setSecondaryThreat(boolean secondaryThreat) {
		this.secondaryThreat = secondaryThreat;
	}

	public Set<String> getSecondaryEffectConditions() {
		return secondaryEffectConditions;
	}

	public boolean isNormalOperation() {
		return normalOperation;
	}

	public void setNormalOperation(boolean normalOperation) {
		this.normalOperation = normalOperation;
	}

	public Map<String, ControlStrategyType> getControlStrategies() {
		return controlStrategies;
	}

	public Set<String> getEntryPoints() {
		return entryPoints;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

	public String getThreatensAssets() {
		return threatensAssets;
	}

	public void setThreatensAssets(String threatensAssets) {
		this.threatensAssets = threatensAssets;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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
	
	public boolean isRootCause() {
		return rootCause;
	}
	
	public void setRootCause(boolean rootCause) {
		this.rootCause = rootCause;
	}
}
