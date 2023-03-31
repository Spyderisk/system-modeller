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
//      Created By :            Stefanie Cox
//      Created Date :          2018-03-29
//      Created for Project :   SHiELD
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.model.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.model.SemanticEntity;
import uk.ac.soton.itinnovation.security.model.domain.ControlStrategy.ControlStrategyType;

public abstract class AThreat extends SemanticEntity {

	private final static Logger logger = LoggerFactory.getLogger(AThreat.class);

	protected Pattern pattern;

	protected String threatensAssets;

	protected String type;

	protected String acceptanceJustification;

	protected final Map<String, ControlStrategy> controlStrategies;

	//need to cache this here to that the JS UI can read it in the serialised object
	boolean resolved;

	public AThreat() {

		this.resolved = false;
		this.acceptanceJustification = null;
		this.controlStrategies = new HashMap<>();
	}

	@Override
	public String toString() {

		String t = getLabel() + " <" + getUri() + ">:\n" +
			"\t- threatens " + threatensAssets + "\n" +
			"\t- pattern   " + pattern + "\n" +
			"\t- type      " + type + "\n" +
			"\t- desc      " + getDescription() + "\n" +
			"\t- resolved? " + isResolved() + "\n" +
			"\t- accepted? " + (acceptanceJustification!=null?acceptanceJustification:"not accepted") + "\n";

		t += "\t- control strategies\n";
		if (!controlStrategies.isEmpty()) {
			t = controlStrategies.values().stream().map(m -> "\t\t* " + m.toString() + "\n").reduce(t, String::concat);
		} else {
			t += "\t\tnone\n";
		}
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
		return combinations;
	}
	*/

	/**
	 * Check whether a threat is resolved. 
	 * A threat is resolved:
	 *    (a) if there is an acceptance justification noted, or
	 *    (b) if there is a mitigating or blocking control strategy enabled
	 *
	 * @return true if it is resolved, false if not
	 */
	public boolean isResolved() {
		resolved = acceptanceJustification != null;

		if (!resolved) {
			for (ControlStrategy csg: controlStrategies.values()) {
				ControlStrategyType csgType = csg.getType(this.getUri());
				//Only check CSGs of type BLOCK or MITIGATE (i.e. not TRIGGER)
				if (csgType != ControlStrategyType.TRIGGER && csg.isEnabled()) {
					resolved = true;
					break;
				}
			}
		}

		return resolved;
	}

	public String getAcceptanceJustification() {
		return acceptanceJustification;
	}

	public void setAcceptanceJustification(String acceptanceJustification) {
		this.acceptanceJustification = acceptanceJustification;
		//this may change the resolved status
		isResolved();
	}

	public Map<String, ControlStrategy> getControlStrategies() {
		return controlStrategies;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public final void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

	public String getThreatensAssets() {
		return threatensAssets;
	}

	public final void setThreatensAssets(String threatensAssets) {
		this.threatensAssets = threatensAssets;
	}

	public String getType() {
		return type;
	}

	public final void setType(String type) {
		this.type = type;
	}
}
