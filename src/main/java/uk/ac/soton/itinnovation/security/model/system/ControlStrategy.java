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
import uk.ac.soton.itinnovation.security.model.SemanticEntity;
import uk.ac.soton.itinnovation.security.model.domain.ControlStrategy.ControlStrategyType;
import uk.ac.soton.itinnovation.security.model.system.ControlSet.ControlSetType;

public class ControlStrategy extends SemanticEntity {

	public static final Logger logger = LoggerFactory.getLogger(ControlStrategy.class);

	private Level blockingEffect;

	//Mandatory control sets
	private final Map<String, ControlSet> mandatoryControlSets;

	//Optional control sets
	private final Map<String, ControlSet> optionalControlSets;

	//Map of associated threat to its CSG type
	private final Map<String, ControlStrategyType> threatCsgTypes;

	public ControlStrategy() {
		this.mandatoryControlSets = new HashMap<>();
		this.optionalControlSets = new HashMap<>();
		this.threatCsgTypes = new HashMap<>();
	}

	/**
	 * Create a new control strategy
	 *
	 * @param uri the URI of the control strategy
	 * @param mandatoryControlSets the mandatory control sets for this control strategy
	 * @param optionalControlSets the optional control sets for this control strategy
	 */
	public ControlStrategy(String uri, Map<String, ControlSet> mandatoryControlSets, Map<String, ControlSet> optionalControlSets) {
		this();
		setUri(uri);
		this.mandatoryControlSets.putAll(mandatoryControlSets);
		this.optionalControlSets.putAll(optionalControlSets);
	}

	@Override
	public String toString() {
		String csg = "[" + isEnabled() + "] <" + getUri() + ">:\n";
		csg += "Mandatory control sets:\n";
		csg = mandatoryControlSets.values().stream().map(cs -> "\t\t\t- " + cs.toString() + "\n").reduce(csg, String::concat);
		csg += "Optional control sets:\n";
		csg = optionalControlSets.values().stream().map(cs -> "\t\t\t- " + cs.toString() + "\n").reduce(csg, String::concat);
		return csg;
	}

	/**
	 * A control strategy is enabled if 
	 * 1) There are NO mandatory control sets, or
	 * 2) all of its mandatory control sets are proposed
	 *
	 * @return true if it's enabled, false if not
	 */
	public boolean isEnabled() {
		if (mandatoryControlSets.size() == 0) {
			return true;
		}
		return mandatoryControlSets.values().stream().noneMatch(cs -> !cs.isProposed());
	}

	/**
	 * Setting this to true will enable all control sets in this control strategy.
	 * Setting it to false won't do anything.
	 *
	 * @param enabled whether to enable the entire control strategy
	 */
	public void setEnabled(boolean enabled) {
		if (enabled) {
			mandatoryControlSets.values().forEach(cs -> cs.setProposed(enabled));
			optionalControlSets.values().forEach(cs -> cs.setProposed(enabled));
		}
	}

	public Map<String, ControlSet> getMandatoryControlSets() {
		return mandatoryControlSets;
	}

	public Map<String, ControlSet> getOptionalControlSets() {
		return optionalControlSets;
	}

	/**
	 * @return the type for a specified threat
	 */
	public ControlStrategyType getType(String threatUri) {
		return threatCsgTypes.get(threatUri);
	}

	public Level getBlockingEffect() {
		return blockingEffect;
	}

	public Map<String, ControlStrategyType> getThreatCsgTypes() {
		return threatCsgTypes;
	}

	public void setBlockingEffect(Level blockingEffect) {
		this.blockingEffect = blockingEffect;
	}
}
