/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2020
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
//      Created By :          Toby Wilkinson
//      Created Date :        16/12/2020
//      Created for Project : ZDMP
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.model.Level;
import uk.ac.soton.itinnovation.security.model.system.Asset;
import uk.ac.soton.itinnovation.security.model.system.AssetGroup;
import uk.ac.soton.itinnovation.security.model.system.ComplianceSet;
import uk.ac.soton.itinnovation.security.model.system.ComplianceThreat;
import uk.ac.soton.itinnovation.security.model.system.ControlSet;
import uk.ac.soton.itinnovation.security.model.system.ControlStrategy;
import uk.ac.soton.itinnovation.security.model.system.MisbehaviourSet;
import uk.ac.soton.itinnovation.security.model.system.Pattern;
import uk.ac.soton.itinnovation.security.model.system.Relation;
import uk.ac.soton.itinnovation.security.model.system.RiskLevelCount;
import uk.ac.soton.itinnovation.security.model.system.RiskVector;
import uk.ac.soton.itinnovation.security.model.system.Threat;
import uk.ac.soton.itinnovation.security.model.system.TrustworthinessAttributeSet;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.LoadingProgress;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;

//Not public
class ModelData {

	private static final Logger logger = LoggerFactory.getLogger(ModelData.class);

	private final Set<Asset> assets;

	private final Set<Relation> relations;

	private final Map<String, MisbehaviourSet> misbehaviourSets;

	private final Map<String, TrustworthinessAttributeSet> twas;

	private final Set<ControlSet> controlSets;

	private final Map<String, ControlStrategy> controlStrategies;

	private final Set<Threat> threats;

	private final Set<ComplianceThreat> complianceThreats;

	private final Set<ComplianceSet> complianceSets;

	private final Map<String, Collection<Level>> levels;
	
	private final Set<AssetGroup> assetGroups;

	private final RiskVector riskVector;

	public ModelData(Model model, ModelObjectsHelper modelObjectsHelper, LoadingProgress loadingProgress) {
		loadingProgress.updateProgress(0.0, "Loading model properties");
		levels = modelObjectsHelper.getLevelsForModel(model);

		loadingProgress.updateProgress(0.1, "Loading control sets");
		Map<String, ControlSet> controlSetsMap = modelObjectsHelper.getControlSetsForModel(model);
		controlSets = new HashSet<>(controlSetsMap.values());

		loadingProgress.updateProgress(0.2, "Loading misbehaviour sets");
		boolean includeCauseAndEffects = true;
		//Get misbehaviour sets including cause and effects
		misbehaviourSets = modelObjectsHelper.getMisbehavioursForModel(model, includeCauseAndEffects);

		loadingProgress.updateProgress(0.3, "Loading trustworthiness attributes");
		twas = modelObjectsHelper.getTWASForModel(model);

		loadingProgress.updateProgress(0.4, "Loading assets");
		assets = modelObjectsHelper.getAssetsForModel(model, true, controlSetsMap, misbehaviourSets, twas); // refresh cache

		// loadingProgress.updateProgress(0.45, "Loading groups");
		assetGroups = modelObjectsHelper.getAssetGroups(model);
			
		if (model.riskLevelsValid()) {
			logger.debug("Getting model risk vector");
			riskVector = modelObjectsHelper.getModelRiskVector(model, levels.get("RiskLevel"), misbehaviourSets);
		}
		else {
			logger.debug("Risk levels not valid: returning null risk vector");
			riskVector = null;
		}
		
		// only load relations, threats, etc, if there are any assets!
		if (assets.size() > 0) {
			loadingProgress.updateProgress(0.5, "Loading relations");
			relations = new HashSet<>(modelObjectsHelper.getRelationsForModel(model));

			loadingProgress.updateProgress(0.6, "Loading threats");
			controlStrategies = modelObjectsHelper.getControlStrategiesForModel(model, controlSetsMap);

			Map<String, Pattern> patterns = modelObjectsHelper.getPatternsForModel(model);
			threats = modelObjectsHelper.getThreatsForModel(model, true, patterns, misbehaviourSets, controlStrategies, twas);

			loadingProgress.updateProgress(0.7, "Loading compliance threats");
			Map<String, ComplianceThreat> complianceThreatsMap = modelObjectsHelper.getComplianceThreatsForModel(model, patterns, controlStrategies);
			complianceThreats = new HashSet<>(complianceThreatsMap.values());

			loadingProgress.updateProgress(0.8, "Loading compliance sets");
			complianceSets = modelObjectsHelper.getComplianceSetsForModel(model, complianceThreatsMap);
		} else {
			relations = Collections.emptySet();
			controlStrategies = Collections.emptyMap();
			threats = Collections.emptySet();
			complianceThreats = Collections.emptySet();
			complianceSets = Collections.emptySet();
		}
	}

	/**
	 * Get basic model details plus (optionally) risks data (i.e. misbehaviours, assets and riskVector),
	 * depending on the value of mode
	 * @param model
	 * @param modelObjectsHelper
	 * @param mode indicates model data to load, e.g. Mode.INFO or Mode.RISKS
	 */
	public ModelData(Model model, ModelObjectsHelper modelObjectsHelper, Mode mode) {
		if (mode == Mode.INFO) {
			levels = new HashMap<>();
			controlSets = Collections.emptySet();
			twas = new HashMap<>();
			relations = Collections.emptySet();
			controlStrategies = Collections.emptyMap();
			threats = Collections.emptySet();
			complianceThreats = Collections.emptySet();
			complianceSets = Collections.emptySet();
			misbehaviourSets = new HashMap<>();
			assets = Collections.emptySet();
			assetGroups = Collections.emptySet();
			riskVector = null;
		}
		else if (mode == Mode.RISKS) {
			boolean includeCauseAndEffects = false; //Don't include cause and effects in misbehaviours
			levels = modelObjectsHelper.getLevelsForModel(model);
			controlSets = Collections.emptySet();
			twas = new HashMap<>();
			relations = Collections.emptySet();
			controlStrategies = Collections.emptyMap();
			threats = Collections.emptySet();
			complianceThreats = Collections.emptySet();
			complianceSets = Collections.emptySet();
			misbehaviourSets = modelObjectsHelper.getMisbehavioursForModel(model, includeCauseAndEffects);
			assets = modelObjectsHelper.getAssetsForModel(model, true, new HashMap<>(), misbehaviourSets, twas);
			assetGroups = Collections.emptySet();

			if (model.riskLevelsValid()) {
				logger.debug("Getting model risk vector");
				riskVector = modelObjectsHelper.getModelRiskVector(model, levels.get("RiskLevel"), misbehaviourSets);
			} else {
				logger.debug("Risk levels not valid: returning null risk vector");
				riskVector = null;
			}
		}
		else {
			throw new RuntimeException("Unsupported mode: " + mode);
		}
	}

	public Set<Asset> getAssets() {
		return assets;
	}

	public Set<Relation> getRelations() {
		return relations;
	}

	public Map<String, MisbehaviourSet> getMisbehaviourSets() {
		return misbehaviourSets;
	}

	public Map<String, TrustworthinessAttributeSet> getTwas() {
		return twas;
	}

	public Set<ControlSet> getControlSets() {
		return controlSets;
	}

	public Map<String, ControlStrategy> getControlStrategies() {
		return controlStrategies;
	}

	public Set<Threat> getThreats() {
		return threats;
	}

	public Set<ComplianceThreat> getComplianceThreats() {
		return complianceThreats;
	}

	public Set<ComplianceSet> getComplianceSets() {
		return complianceSets;
	}

	public Map<String, Collection<Level>> getLevels() {
		return levels;
	}
	
	public Set<AssetGroup> getAssetGroups() {
		return assetGroups;
	}

	public RiskVector getRiskVector() {
		return riskVector;
	}
	
	@Override
	public String toString() {
		return
			"\nriskVector: " + (riskVector != null ? riskVector.toString() : "null");
	}
	
	public static enum Mode {
		INFO,
		RISKS
	}
}
