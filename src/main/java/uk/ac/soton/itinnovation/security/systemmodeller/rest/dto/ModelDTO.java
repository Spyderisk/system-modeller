/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2018
//
// Copyright in this library belongs to the University of Southampton
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
//      Created By :          Oliver Hayes
//      Created Date :        2017-08-21
//      Created for Project : 5G-ENSURE
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uk.ac.soton.itinnovation.security.model.Level;
import uk.ac.soton.itinnovation.security.model.system.Asset;
import uk.ac.soton.itinnovation.security.model.system.AssetGroup;
import uk.ac.soton.itinnovation.security.model.system.ComplianceSet;
import uk.ac.soton.itinnovation.security.model.system.ComplianceThreat;
import uk.ac.soton.itinnovation.security.model.system.ControlSet;
import uk.ac.soton.itinnovation.security.model.system.ControlStrategy;
import uk.ac.soton.itinnovation.security.model.system.MisbehaviourSet;
import uk.ac.soton.itinnovation.security.model.system.Relation;
import uk.ac.soton.itinnovation.security.model.system.RiskLevelCount;
import uk.ac.soton.itinnovation.security.model.system.RiskVector;
import uk.ac.soton.itinnovation.security.model.system.Threat;
import uk.ac.soton.itinnovation.security.model.system.TrustworthinessAttributeSet;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;

public class ModelDTO {

	private String name;
	private String id;
	private String description;
	private String domainGraph;
	private String domainVersion;
	private String validatedDomainVersion;
	private boolean valid;
	private boolean isValidating;
	private boolean riskLevelsValid;
	private boolean calculatingRisks;
	private boolean canBeEdited;
	private boolean canBeShared;
	private Level risk;
	private Set<ThreatDTO> threats;
	private Map<String, ControlStrategyDTO> controlStrategies;
	private Set<ComplianceThreatDTO> complianceThreats;
	private Set<ComplianceSetDTO> complianceSets;
	private Map<String, MisbehaviourSet> misbehaviourSets;
	private Map<String, TrustworthinessAttributeSet> twas;
	private Set<ControlSet> controlSets;
	private String userId;
	private String editorId;
	private Date created;
	private Date modified;
	private String modifiedBy;
	private Map<String, Collection<Level>> levels;
	private Map<String, RiskLevelCount> riskVector;
	private Set<AssetDTO> assets;
	private Set<Relation> relations;
	private Set<AssetGroupDTO> groups;
	private String loadingId;

	// Required by Jackson
	public ModelDTO() {}

	public ModelDTO(Model model) {
		this(model, false, false);
	}

	// Initialise a modelDTO with access rights set for a particular user
	public ModelDTO(Model model, boolean canBeEdited, boolean canBeShared){
		this.name = model.getName();
		this.id = model.getNoRoleUrl();
		this.description = model.getDescription();
		this.domainGraph = model.getDomainGraph();
		this.domainVersion = model.getDomainVersion();
		this.validatedDomainVersion = model.getValidatedDomainVersion();
		this.valid = model.isValid();
		this.isValidating = model.isValidating();
		this.riskLevelsValid = model.riskLevelsValid();
		this.calculatingRisks = model.isCalculatingRisks();
		this.risk = (model.riskLevelsValid() ? model.getRiskLevel() : null);
		// TODO -- this needs to be a username for display on the GUI. 
		// Should update from this.userId to this.userName but that will require changes on the front end
		this.userId = model.getUserName();
		// TODO -- this needs to be a username for display on the GUI. 
		// Should update from this.editorId to this.editorName but that will require changes on the front end
		this.editorId = model.getEditorName();
		this.created = model.getCreated();
		this.modified = model.getModified();
		// TODO -- this needs to be a username for display on the GUI. 
		// Should update from this.modifiedBy to this.modifiedByName but that will require changes on the front end
		this.modifiedBy = model.getModifiedByName();
		this.canBeEdited = canBeEdited;
		this.canBeShared = canBeShared;

		if (model.hasModelData()) {
			this.controlStrategies = getControlStrategyDTOs(model.getControlStrategies());
			this.threats = getThreatDTOs(model.getThreats());
			this.complianceThreats = getComplianceThreatDTOs(model.getComplianceThreats());
			this.complianceSets = getComplianceSetDTOs(model.getComplianceSets());
			this.misbehaviourSets = model.getMisbehaviourSets();
			this.twas = model.getTwas();
			this.controlSets = model.getControlSets();
			this.levels = model.getLevels();
			RiskVector rv = model.getRiskVector();
			this.riskVector = rv != null ? rv.getRiskVector() : null;
			this.assets = getAssetDTOs(model.getAssets());
			this.relations = model.getRelations();
			this.groups = getAssetGroupDTOs(model.getAssetGroups());
		} else {
			this.controlStrategies = Collections.emptyMap();
			this.threats = Collections.emptySet();
			this.complianceThreats = Collections.emptySet();
			this.complianceSets = Collections.emptySet();
			this.misbehaviourSets = Collections.emptyMap();
			this.twas = Collections.emptyMap();
			this.controlSets = Collections.emptySet();
			this.levels = Collections.emptyMap();
			this.riskVector = null;
			this.assets = Collections.emptySet();
			this.relations = Collections.emptySet();
			this.groups = Collections.emptySet();
		}
	}
	
	// Get ControlStrategies as DTOs
	private Map<String, ControlStrategyDTO> getControlStrategyDTOs(Map<String, ControlStrategy> controlStrategies) {
		Map<String, ControlStrategyDTO> controlStrategyDTOs = new HashMap<>();

		for (String csgUri : controlStrategies.keySet()) {
			ControlStrategy csg = controlStrategies.get(csgUri);
			ControlStrategyDTO csgDto = new ControlStrategyDTO(csg);
			controlStrategyDTOs.put(csgUri, csgDto);
		}

		return controlStrategyDTOs;
	}

	// Get Assets as DTOs
	private Set<AssetDTO> getAssetDTOs(Set<Asset> assets) {
		Set<AssetDTO> assetsSet = new HashSet<>();
		for (Asset asset : assets) {
			AssetDTO assetDto = new AssetDTO(asset);
			assetsSet.add(assetDto);
		}
		return assetsSet;
	}

	// Get AssetGroups as DTOs
	private Set<AssetGroupDTO> getAssetGroupDTOs(Set<AssetGroup> assetGroups) {
		Set<AssetGroupDTO> assetGroupsSet = new HashSet<>();
		for (AssetGroup assetGroup : assetGroups) {
			AssetGroupDTO assetDto = new AssetGroupDTO(assetGroup);
			assetGroupsSet.add(assetDto);
		}
		return assetGroupsSet;
	}

	// Get Threats as DTOs
	private Set<ThreatDTO> getThreatDTOs(Set<Threat> threats) {
		Set<ThreatDTO> threatsSet = new HashSet<>();
		for (Threat threat : threats) {
			ThreatDTO threatDto = new ThreatDTO(threat);
			threatsSet.add(threatDto);
		}
		return threatsSet;
	}

	// Get Compliance Threats as DTOs
	private Set<ComplianceThreatDTO> getComplianceThreatDTOs(Set<ComplianceThreat> threats) {
		Set<ComplianceThreatDTO> threatsSet = new HashSet<>();
		if (threats != null) {
			for (ComplianceThreat threat : threats) {
				ComplianceThreatDTO threatDto = new ComplianceThreatDTO(threat);
				threatsSet.add(threatDto);
			}
		}
		return threatsSet;
	}
	
	private Set<ComplianceSetDTO> getComplianceSetDTOs(Set<ComplianceSet> complianceSets) {
		Set<ComplianceSetDTO> complianceSetDTOs = new HashSet<>();
		if (complianceSets != null) {
			for (ComplianceSet complianceSet : complianceSets) {
				ComplianceSetDTO complianceSetDto = new ComplianceSetDTO(complianceSet);
				complianceSetDTOs.add(complianceSetDto);
			}
		}
		return complianceSetDTOs;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDomainGraph() {
		return domainGraph;
	}

	public void setDomainGraph(String domainGraph) {
		this.domainGraph = domainGraph;
	}

	public String getDomainVersion() {
		return domainVersion;
	}

	public void setDomainVersion(String domainVersion) {
		this.domainVersion = domainVersion;
	}

	public String getValidatedDomainVersion() {
		return validatedDomainVersion;
	}

	public void setValidatedDomainVersion(String validatedDomainVersion) {
		this.validatedDomainVersion = validatedDomainVersion;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public boolean isValidating() {
		return isValidating;
	}

	public void setValidating(boolean validating) {
		isValidating = validating;
	}

	public boolean isRiskLevelsValid() {
		return riskLevelsValid;
	}

	public void setRiskLevelsValid(boolean riskLevelsValid) {
		this.riskLevelsValid = riskLevelsValid;
	}

	public boolean isCalculatingRisks() {
		return calculatingRisks;
	}

	public void setCalculatingRisks(boolean calculatingRisks) {
		this.calculatingRisks = calculatingRisks;
	}

	public Level getRisk() {
		return risk;
	}

	public void setRisk(Level risk) {
		this.risk = risk;
	}
	
	public Set<ThreatDTO> getThreats() {
		return threats;
	}

	public void setThreats(Set<ThreatDTO> threats) {
		this.threats = threats;
	}

	public Set<ComplianceThreatDTO> getComplianceThreats() {
		return complianceThreats;
	}

	public void setComplianceThreats(Set<ComplianceThreatDTO> complianceThreats) {
		this.complianceThreats = complianceThreats;
	}

	public Set<ComplianceSetDTO> getComplianceSets() {
		return complianceSets;
	}

	public void setComplianceSets(Set<ComplianceSetDTO> complianceSets) {
		this.complianceSets = complianceSets;
	}

	public Map<String, MisbehaviourSet> getMisbehaviourSets() {
		return misbehaviourSets;
	}

	public void setMisbehaviourSets(Map<String, MisbehaviourSet> misbehaviourSets) {
		this.misbehaviourSets = misbehaviourSets;
	}

	public Map<String, TrustworthinessAttributeSet> getTwas() {
		return twas;
	}

	public void setTwas(Map<String, TrustworthinessAttributeSet> twas) {
		this.twas = twas;
	}

	public Map<String, ControlStrategyDTO> getControlStrategies() {
		return controlStrategies;
	}

	public void setControlStrategies(Map<String, ControlStrategyDTO> controlStrategies) {
		this.controlStrategies = controlStrategies;
	}
	
	public Set<ControlSet> getControlSets() {
		return controlSets;
	}

	public void setControlSets(Set<ControlSet> controlSets) {
		this.controlSets = controlSets;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getEditorId() {
		return editorId;
	}

	public void setEditorId(String editorId) {
		this.editorId = editorId;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getModified() {
		return modified;
	}

	public void setModified(Date modified) {
		this.modified = modified;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public Map<String, Collection<Level>> getLevels() {
		return levels;
	}

	public void setLevels(Map<String, Collection<Level>> levels) {
		this.levels = levels;
	}

	public Map<String, RiskLevelCount> getRiskVector() {
		return riskVector;
	}

	public void setRiskVector(Map<String, RiskLevelCount> riskVector) {
		this.riskVector = riskVector;
	}

	public Set<AssetDTO> getAssets() {
		return assets;
	}

	public void setAssets(Set<AssetDTO> assets) {
		this.assets = assets;
	}

	public Set<Relation> getRelations() {
		return relations;
	}

	public Set<AssetGroupDTO> getGroups() {
		return groups;
	}

	public void setGroups(Set<AssetGroupDTO> groups) {
		this.groups = groups;
	}

	public void setRelations(Set<Relation> relations) {
		this.relations = relations;
	}

	public String getLoadingId() {
		return loadingId;
	}

	public void setLoadingId(String loadingId) {
		this.loadingId = loadingId;
	}

	public boolean getCanBeShared() {
		return canBeShared;
	}

	public boolean setCanBeShared(boolean canBeShared) {
		return this.canBeShared = canBeShared;
	}

	public boolean getCanBeEdited() {
		return canBeEdited;
	}

	public boolean setCanBeEdited(boolean canBeEdited) {
		return this.canBeEdited = canBeEdited;
	}
}
