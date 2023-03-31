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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.soton.itinnovation.security.model.domain.ControlStrategy.ControlStrategyType;
import uk.ac.soton.itinnovation.security.model.system.ComplianceThreat;
import uk.ac.soton.itinnovation.security.model.system.ControlSet;
import uk.ac.soton.itinnovation.security.model.system.ControlStrategy;
import uk.ac.soton.itinnovation.security.model.system.Pattern;

public class ComplianceThreatDTO extends SemanticEntityDTO {

	private final static Logger logger = LoggerFactory.getLogger(ComplianceThreatDTO.class);

	private Pattern pattern;

	private String threatensAssets;

	private String type;

	private boolean resolved;

	private String acceptanceJustification;

	//Map of control strategies to their type
	private final Map<String, ControlStrategyType> controlStrategies;

	public ComplianceThreatDTO() {
		controlStrategies = new HashMap<>();
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
	 * @param acceptanceJustification
	 * @param resolved whether the threat is treated
	 * @param misbehaviours the misbehaviour sets caused by this threat
	 * @param secondaryEffectConditions the conditions under which this threat could be a secondary effect
	 * @param controlStrategies the threat's control strategies
	 */
	public ComplianceThreatDTO(String uri, String label, String description, Pattern pattern,
			String threatensAsset, String type, boolean resolved, String acceptanceJustification,
			Set<String> misbehaviours, Set<String> secondaryEffectConditions,
			Map<String, ControlStrategy> controlStrategies) {

		this();
		setUri(uri);
		setLabel(label);
		setDescription(description);
		this.pattern = pattern;
		this.threatensAssets = threatensAsset;
		this.type = type;
		this.resolved = resolved;
		this.acceptanceJustification = acceptanceJustification;
		extractControlStrategiesMap(controlStrategies);
	}

	public ComplianceThreatDTO(ComplianceThreat threat) {
		setUri(threat.getUri());
		setLabel(threat.getLabel());
		setDescription(threat.getDescription());
		
		this.pattern = threat.getPattern();
		this.threatensAssets = threat.getThreatensAssets();
		this.type = threat.getType();
		this.resolved = threat.isResolved();
		this.acceptanceJustification = threat.getAcceptanceJustification();
		this.controlStrategies = new HashMap<>();
		extractControlStrategiesMap(threat.getControlStrategies());
	}

	private void extractControlStrategiesMap(Map<String, ControlStrategy> csgs) {
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
			"\t- threatens " + threatensAssets + "\n" +
			"\t- pattern   " + pattern + "\n" +
			"\t- type      " + type + "\n" +
			"\t- desc      " + getDescription() + "\n" +
			"\t- resolved? " + resolved + "\n" +
			"\t- accepted? " + (acceptanceJustification!=null?acceptanceJustification:"not accepted") + "\n";

		t += "\t- control strategies\n";
		if (!controlStrategies.isEmpty()) {
			t = controlStrategies.values().stream().map(m -> "\t\t* " + m.toString() + "\n").reduce(t, String::concat);
		} else {
			t += "\t\tnone\n";
		}
		t += "\t- entry points\n";
		return t;
	}

	/**
	 * Get all combinations of control sets that would block the threat
	 *
	 * @return a set of control set combinations
	 */
	/* KEM - does not appear to be used
	public List<SortedSet<ControlSet>> getAllControlCombinations() {

		//make sure the order of the CSGs is consistent. Use URI. We don't care what the order is.
		SortedMap<String, ControlStrategy> csgsSorted = new TreeMap<>();
		csgsSorted.putAll(controlStrategies);
		List<SortedSet<ControlSet>> combinations = new ArrayList<>();
		for (ControlStrategy csg: csgsSorted.values()) {
			SortedSet<ControlSet> oneCombination = new TreeSet<>();
			oneCombination.addAll(csg.getControlSets().values());
			combinations.add(oneCombination);
		}
//		if (combinations.isEmpty()) {
//			logger.debug("No control strategies for threat {}", getLabel());
//			SortedSet<ControlSet> fakeControlSet = new TreeSet<>();
//			ControlSet cs = new ControlSet();
//			cs.setUri(getUri());
//			cs.setAssetUri(getThreatensAssets());
//			cs.setControl("AcceptThreat");
//			cs.setLabel("Accept threat " + getLabel());
//			fakeControlSet.add(cs);
//			combinations.add(fakeControlSet);
//		}
		return combinations;
	}
	*/

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

	public Map<String, ControlStrategyType> getControlStrategies() {
		return controlStrategies;
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

}
