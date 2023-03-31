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
//      Created Date :          2016-08-17
//      Created for Project :   ASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.model.system;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Relation {

	public static final Logger logger = LoggerFactory.getLogger(Relation.class);

	private String from;

	private String fromID;

	private String to;

	private String toID;

	private String type;

	private String label;

	private int sourceCardinality;

	private int targetCardinality;

	//a relation is asserted if it's in the system model graph, not the inferred graph
	private boolean asserted;

	//a relation is visible iff both its source and target are visible
	private boolean visible;

	//an immutable relation is read-only to a user
	private boolean immutable;

	//a hidden relation is never displayed anywhere: neither on the canvas, nor in any other place
	private boolean hidden;

	//these (invisible) assets are displayed on this relation
	private Set<String> inferredAssets;

	public Relation() {
		inferredAssets = new HashSet<>();

		//no default cardinality restrictions; -1 to -1 is assumed.
		//a client should ensure that the restrictions are compatible with the source/target cardinality restrictions
		sourceCardinality = -1;
		targetCardinality = -1;
	}

	/**
	 * Create an asserted relation. visible and asserted are true by default, which might not be true.
	 * It's the responsibility of the creator of the relation to make sure to set the correct values
	 * for these properties after creating the relation.
	 *
	 * @param from the source
	 * @param fromID
	 * @param to the target
	 * @param toID
	 * @param type the relationship type
	 * @param label
	 */
	public Relation(String from, String fromID, String to, String toID, String type, String label) {

		this();
		this.from = from;
		this.to = to;
		this.type = type;
		this.label = label;
		//note that this might not be the case. these are default values only
		this.asserted = true;
		this.visible = true;
		this.immutable = false;
		this.hidden = false;
	}

	@Override
	public String toString() {
		return (asserted?"asserted":"inferred") + "," + (visible?"visible":"invisible") +
			"," + (hidden?"hidden":"not hidden")+ "," + (immutable?"immutable":"mutable") + ":" +
			"(" + (from!=null?from:fromID) + ")-[" + (label!=null?label:type) + "]->(" + (to!=null?to:toID) + ")" +
			(inferredAssets.isEmpty()?"":", inferred assets: "+Arrays.toString(inferredAssets.toArray()));
	}

	/**
	 * Returns the domain of this relation.
	 *
	 * @return the from
	 */
	public String getFrom() {
		return from;
	}

	/**
	 * Sets the domain of this relation.
	 *
	 * @param from the domain
	 */
	public void setFrom(String from) {
		this.from = from;
	}

	public String getFromID() {

		if (fromID!=null) {
			return fromID;
		} else {
			Asset a  = new Asset();
			a.setUri(from);
			return a.getID();
		}
	}

	public void setFromID(String fromID) {
		this.fromID = fromID;
	}

	/**
	 * Returns the range of this relation.
	 *
	 * @return the to
	 */
	public String getTo() {
		return to;
	}

	/**
	 * Sets the range of this relation.
	 *
	 * @param to the range
	 */
	public void setTo(String to) {
		this.to = to;
	}

	public String getToID() {

		if (toID!=null) {
			return toID;
		} else {
			Asset a  = new Asset();
			a.setUri(to);
			return a.getID();
		}
	}

	public void setToID(String toID) {
		this.toID = toID;
	}

	/**
	 * Returns the type of this relation.
	 *
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the type of this relation.
	 *
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public int getSourceCardinality() {
		return sourceCardinality;
	}

	public void setSourceCardinality(int sourceCardinality) {
		this.sourceCardinality = sourceCardinality;
	}

	public int getTargetCardinality() {
		return targetCardinality;
	}

	public void setTargetCardinality(int targetCardinality) {
		this.targetCardinality = targetCardinality;
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

	public boolean isImmutable() {
		return immutable;
	}

	public void setImmutable(boolean immutable) {
		this.immutable = immutable;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public Set<String> getInferredAssets() {
		return inferredAssets;
	}

	public void setInferredAssets(Set<String> inferredAssets) {
		this.inferredAssets = inferredAssets;
	}

	@Override
	public boolean equals(Object other) {

		if (other!=null && other.getClass().equals(Relation.class)) {
			return from.equals(((Relation) other).getFrom())
				&& to.equals(((Relation) other).getTo())
				&& type.equals(((Relation) other).getType());
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 59 * hash + Objects.hashCode(this.from);
		hash = 59 * hash + Objects.hashCode(this.to);
		hash = 59 * hash + Objects.hashCode(this.type);
		return hash;
	}

	/**
	 * Get the ID for this relation. The ID is a hexadecimal string generated from the hash code.
	 *
	 * @return the ID
	 */
	public String getID() {
		return Integer.toHexString(hashCode());
	}
}
