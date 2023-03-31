/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2017
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
//      Created Date :          2017-12-27
//      Created for Project :   SHiELD
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.model.system;
import uk.ac.soton.itinnovation.security.model.Level;
import uk.ac.soton.itinnovation.security.model.SemanticEntity;

public class TrustworthinessAttributeSet extends SemanticEntity {

	private String asset;

	private SemanticEntity attribute;

	private String causingMisbehaviourSet;

	private Level assertedTWLevel; //TODO: should be "assumed"

	private Level inferredTWLevel;

	private boolean twLevelAsserted; //is the TW level asserted by the user
	
	private boolean visible = true; //visible by default

	public TrustworthinessAttributeSet() {}

	@Override
	public String toString() {
		return (attribute!=null?attribute.toString():"null") +
			", caused by " + causingMisbehaviourSet + ", " +
			(assertedTWLevel!=null?assertedTWLevel.toString():"null") + " (asserted), " +
			(inferredTWLevel!=null?inferredTWLevel.toString():"null") + " (inferred) on " + asset;
	}

	public String getAsset() {
		return asset;
	}

	public void setAsset(String asset) {
		this.asset = asset;
	}

	public SemanticEntity getAttribute() {
		return attribute;
	}

	public void setAttribute(SemanticEntity attribute) {
		this.attribute = attribute;
	}

	public String getCausingMisbehaviourSet() {
		return causingMisbehaviourSet;
	}

	public void setCausingMisbehaviourSet(String causingMisbehaviourSet) {
		this.causingMisbehaviourSet = causingMisbehaviourSet;
	}

	public Level getAssertedTWLevel() {
		return assertedTWLevel;
	}

	public void setAssertedTWLevel(Level assertedTWLevel) {
		this.assertedTWLevel = assertedTWLevel;
	}

	public Level getInferredTWLevel() {
		return inferredTWLevel;
	}

	public void setInferredTWLevel(Level inferredTWLevel) {
		this.inferredTWLevel = inferredTWLevel;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public boolean isTwLevelAsserted() {
		return twLevelAsserted;
	}

	public void setTwLevelAsserted(boolean twLevelAsserted) {
		this.twLevelAsserted = twLevelAsserted;
	}

}
