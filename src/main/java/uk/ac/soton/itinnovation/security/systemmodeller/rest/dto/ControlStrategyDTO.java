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
//      Created Date :          2023-02-03
//      Created for Project :   Fogprotect
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest.dto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.model.Level;
import uk.ac.soton.itinnovation.security.model.SemanticEntity;
import uk.ac.soton.itinnovation.security.model.domain.ControlStrategy.ControlStrategyType;
import uk.ac.soton.itinnovation.security.model.system.ControlSet;
import uk.ac.soton.itinnovation.security.model.system.ControlStrategy;
import uk.ac.soton.itinnovation.security.model.system.ControlSet.ControlSetType;

public class ControlStrategyDTO extends SemanticEntityDTO {

	public static final Logger logger = LoggerFactory.getLogger(ControlStrategyDTO.class);

	private Level blockingEffect;
	private boolean enabled;

	//Mandatory control set URIs
	private final Set<String> mandatoryControlSets;

	//Optional control set URIs
	private final Set<String> optionalControlSets;

	//Map of associated threat to its CSG type
	private final Map<String, ControlStrategyType> threatCsgTypes;

	public ControlStrategyDTO() {
		this.mandatoryControlSets = new HashSet<>();
		this.optionalControlSets = new HashSet<>();
		this.threatCsgTypes = new HashMap<>();
	}

	/**
	 * Create a new control strategy
	 *
	 * @param uri the URI of the control strategy
	 * @param mandatoryControlSets the mandatory control sets for this control strategy
	 * @param optionalControlSets the optional control sets for this control strategy
	 */
	public ControlStrategyDTO(String uri, Map<String, ControlSet> mandatoryControlSets, Map<String, ControlSet> optionalControlSets) {
		this();
		setUri(uri);
		this.mandatoryControlSets.addAll(mandatoryControlSets.keySet());
		this.optionalControlSets.addAll(optionalControlSets.keySet());
	}

	public ControlStrategyDTO(ControlStrategy csg) {
		setUri(csg.getUri());
		setLabel(csg.getLabel());
		setDescription(csg.getDescription());
		this.setBlockingEffect(csg.getBlockingEffect());
		this.setEnabled(csg.isEnabled());
		this.mandatoryControlSets = new HashSet<>();
		this.mandatoryControlSets.addAll(csg.getMandatoryControlSets().keySet());
		this.optionalControlSets = new HashSet<>();
		this.optionalControlSets.addAll(csg.getOptionalControlSets().keySet());
		this.threatCsgTypes = csg.getThreatCsgTypes();
	}

	@Override
	public String toString() {
		String csg = "[" + isEnabled() + "] <" + getUri() + ">:\n";
		csg += "Mandatory control sets:\n";
		csg += mandatoryControlSets.stream().map(cs -> "\t\t\t- " + cs.toString() + "\n").reduce(csg, String::concat);
		csg += "Optional control sets:\n";
		csg += optionalControlSets.stream().map(cs -> "\t\t\t- " + cs.toString() + "\n").reduce(csg, String::concat);
		return csg;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Set<String> getMandatoryControlSets() {
		return mandatoryControlSets;
	}

	public Set<String> getOptionalControlSets() {
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
