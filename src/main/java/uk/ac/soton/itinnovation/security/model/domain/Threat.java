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
//      Created Date :          2017-11-29
//      Created for Project :   SHiELD
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.model.domain;

import java.util.HashMap;
import java.util.Map;
import uk.ac.soton.itinnovation.security.model.Level;
import uk.ac.soton.itinnovation.security.model.SemanticEntity;

public class Threat extends SemanticEntity {

	private String threatensRole;

	private String appliesTo;

	private EntryPoint entryPoint;

	private Level priorLikelihood;

	private final Map<String, MisbehaviourSet> causesMisbehaviours;

	private final Map<String, MisbehaviourSet> secondaryEffectConditions;

	private String category;

	public Threat(Map<String, MisbehaviourSet> causesMisbehaviours, Map<String, MisbehaviourSet> secondaryEffectConditions) {
		this.causesMisbehaviours = causesMisbehaviours;
		this.secondaryEffectConditions = secondaryEffectConditions;
	}

	public Threat() {
		causesMisbehaviours = new HashMap<>();
		secondaryEffectConditions = new HashMap<>();
	}

	public String getThreatensRole() {
		return threatensRole;
	}

	public String getAppliesTo() {
		return appliesTo;
	}

	public Map<String, MisbehaviourSet> getCausesMisbehaviours() {
		return causesMisbehaviours;
	}

	public Map<String, MisbehaviourSet> getSecondaryEffectConditions() {
		return secondaryEffectConditions;
	}

	public String getCategory() {
		return category;
	}

	public void setThreatensRole(String threatensRole) {
		this.threatensRole = threatensRole;
	}

	public void setAppliesTo(String appliesTo) {
		this.appliesTo = appliesTo;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public EntryPoint getEntryPoint() {
		return entryPoint;
	}

	public void setEntryPoint(EntryPoint entryPoint) {
		this.entryPoint = entryPoint;
	}

	public Level getPriorLikelihood() {
		return priorLikelihood;
	}

	public void setPriorLikelihood(Level priorLikelihood) {
		this.priorLikelihood = priorLikelihood;
	}
}
