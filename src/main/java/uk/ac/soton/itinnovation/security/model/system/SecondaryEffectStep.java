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
//      Created Date :          10/03/2017
//      Created for Project :   5G-ENSURE
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.model.system;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.model.SemanticEntity;

/**
 * This class is a data structure for a step in a secondary effect chain. It consists of the threat URI,
 * the misbehaviour sets causing this threat to be a secondary effect and the misbheviour sets
 * triggered by it (the effect).
 * A threat that isn't a secondary effect (i.e. a primary threat) can also be a secondary effect step. It would
 * appear as a root cause in a secondary effect chain, i.e. a certain type of leaf. It can be distinguished from a
 * true secondary effect because its "cause" set will be empty.
 */
public class SecondaryEffectStep extends SemanticEntity {

	public static final Logger logger = LoggerFactory.getLogger(SecondaryEffectStep.class);

	private final Set<MisbehaviourSet> cause;

	private final Set<MisbehaviourSet> effect;

	private boolean active;

	public SecondaryEffectStep() {
		this.cause = new HashSet<>();
		this.effect = new HashSet<>();
		this.active = true;
	}

	/**
	 * Create a new secondary effect step. This is active by default.
	 *
	 * @param uri the threat URI
	 */
	public SecondaryEffectStep(String uri) {
		this();
		setUri(uri);
	}

	@Override
	public String toString() {
		return "[" + (isPrimaryThreat()?"PRIMARY THREAT":"SECONDARY EFFECT") + "] "
				+ getUri() + ": (" + cause.toString() + ") -> (" + effect.toString() + ")";
	}

	@Override
	public boolean equals(Object other) {

		if (other!=null && other.getClass().equals(SecondaryEffectStep.class)) {
			return getUri().equals(((SemanticEntity) other).getUri())
				&& cause.equals(((SecondaryEffectStep) other).getCause())
				&& effect.equals(((SecondaryEffectStep) other).getEffect());
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 89 * hash + Objects.hashCode(getUri());
		hash = 89 * hash + Objects.hashCode(this.cause);
		hash = 89 * hash + Objects.hashCode(this.effect);
		return hash;
	}

	public Set<MisbehaviourSet> getCause() {
		return cause;
	}

	public Set<MisbehaviourSet> getEffect() {
		return effect;
	}

	public void addCause(MisbehaviourSet ms) {
		cause.add(ms);
	}

	public void addEffect(MisbehaviourSet ms) {
		effect.add(ms);
	}

	/**
	 * Find out whether a secondary effect step is actually a primary threat
	 *
	 * @return true if it's a primary threat, false if it's a genuine secondary effect
	 */
	public boolean isPrimaryThreat() {
		return cause.isEmpty();
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
