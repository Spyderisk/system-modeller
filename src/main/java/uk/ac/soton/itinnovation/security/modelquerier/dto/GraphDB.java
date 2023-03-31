/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2019
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
//      Created By :            Lee Mason
//      Created Date :          05/08/2019
//      Created for Project :   RESTASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelquerier.dto;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class GraphDB {
	public GraphDB() {
		// Defaults
		this.assets = new HashMap<>();
		this.threats = new HashMap<>();
		this.roles = new HashMap<>();
		this.links = new HashMap<>();
		this.constructionPatterns = new HashMap<>();
		this.matchingPatterns = new HashMap<>();
		this.nodes = new HashMap<>();
		this.rootPatterns = new HashMap<>();
		this.inferredNodeSettings = new HashMap<>();
		this.misbehaviourInhibitionSets = new HashMap<>();
		this.misbehaviourSets = new HashMap<>();
		this.maDefaultSettings = new HashMap<>();
		this.twaaDefaultSettings = new HashMap<>();
		this.impactLevels = new HashMap<>();
		this.trustworthinessLevels = new HashMap<>();
		this.likelihoodLevels = new HashMap<>();
		this.trustworthinessAttributeSets = new HashMap<>();
		this.trustworthinessImpactSets = new HashMap<>();
		this.trustworthinessAttributes = new HashMap<>();
		this.misbehaviours = new HashMap<>();
		this.controlStrategies = new HashMap<>();
		this.controlSets = new HashMap<>();
		this.distinctNodeGroups = new HashMap<>();
		this.cardinalityConstraints = new HashMap<>();
		this.model = new HashMap<>();
	}
	
	@SerializedName("Threat")
	private Map<String, ThreatDB> threats;
	@SerializedName("Asset")
	private Map<String, AssetDB> assets;
	@SerializedName("Role")
	private Map<String, RoleDB> roles;
	@SerializedName("RoleLink")
	private Map<String, RoleLinkDB> links;
	@SerializedName("ConstructionPattern")
	private Map<String, ConstructionPatternDB> constructionPatterns;
	@SerializedName("MatchingPattern")
	private Map<String, MatchingPatternDB> matchingPatterns;
	@SerializedName("Node")
	private Map<String, NodeDB> nodes;
	@SerializedName("RootPattern")
	private Map<String, RootPatternDB> rootPatterns;
	@SerializedName("InferredNodeSetting")
	private Map<String, InferredNodeSettingDB> inferredNodeSettings;
	@SerializedName("MisbehaviourInhibitionSet")
	private Map<String, MisbehaviourInhibitionSetDB> misbehaviourInhibitionSets;
	@SerializedName("MisbehaviourSet")
	private Map<String, MisbehaviourSetDB> misbehaviourSets;
	@SerializedName("MADefaultSetting")
	private Map<String, MADefaultSettingDB> maDefaultSettings;
	@SerializedName("TWAADefaultSetting")
	private Map<String, TWAADefaultSettingDB> twaaDefaultSettings;
	@SerializedName("ImpactLevel")
	private Map<String, LevelDB> impactLevels;
	@SerializedName("TrustworthinessLevel")
	private Map<String, LevelDB> trustworthinessLevels;
	@SerializedName("Likelihood")
	private Map<String, LevelDB> likelihoodLevels;
	@SerializedName("RiskLevel")
	private Map<String, LevelDB> riskLevels;
	@SerializedName("TrustworthinessAttributeSet")
	private Map<String, TrustworthinessAttributeSetDB> trustworthinessAttributeSets;
	@SerializedName("TrustworthinessImpactSet")
	private Map<String, TrustworthinessImpactSetDB> trustworthinessImpactSets;
	@SerializedName("TrustworthinessAttribute")
	private Map<String, TrustworthinessAttributeDB> trustworthinessAttributes;
	@SerializedName("Misbehaviour")
	private Map<String, MisbehaviourDB> misbehaviours;
	@SerializedName("ControlStrategy")
	private Map<String, ControlStrategyDB> controlStrategies;
	@SerializedName("ControlSet")
	private Map<String, ControlSetDB> controlSets;
	@SerializedName("DistinctNodeGroup")
	private Map<String, DistinctNodeGroupDB> distinctNodeGroups;
	@SerializedName("CardinalityConstraint")
	private Map<String, CardinalityConstraintDB> cardinalityConstraints;
	@SerializedName("http://www.w3.org/2002/07/owl#Ontology")
	private Map<String, ModelDB> model;
}
