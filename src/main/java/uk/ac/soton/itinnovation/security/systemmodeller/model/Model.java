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
//      Modified By :	        Stefanie Wiegand
//      Created Date :          2016-08-17
//      Created for Project :   ASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.model;

import java.util.Collection;
import java.util.Date;
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
import uk.ac.soton.itinnovation.security.model.system.Relation;
import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;
import uk.ac.soton.itinnovation.security.model.system.RiskVector;
import uk.ac.soton.itinnovation.security.model.system.Threat;
import uk.ac.soton.itinnovation.security.model.system.TrustworthinessAttributeSet;
import uk.ac.soton.itinnovation.security.modelquerier.SystemModelQuerier;
import uk.ac.soton.itinnovation.security.modelquerier.SystemModelUpdater;
import uk.ac.soton.itinnovation.security.modelquerier.util.ModelStack;
import uk.ac.soton.itinnovation.security.semanticstore.AStoreWrapper;
import uk.ac.soton.itinnovation.security.systemmodeller.mongodb.IModelRepository;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.LoadingProgress;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.StoreModelManager;

public class Model {

	private static final Logger logger = LoggerFactory.getLogger(Model.class);

	private IModelRepository modelRepository;

	private StoreModelManager storeModelManager;

	private AStoreWrapper store;

	//Stored in Mongo
	private ModelACL modelACL;

	//Stored in Jena
	private ModelInfo modelInfo;

	//Queried from domain graph (not stored in system model)
	private String domainVersion;

	//Stored in Jena
	private ModelData modelData;
	
	private boolean stateUpdating = false;

	private ModelStack stack;

	private SystemModelQuerier querier;

	private SystemModelUpdater updater;

	//Not public
	Model(
		ModelACL modelACL,
		IModelRepository modelRepository,
		StoreModelManager storeModelManager
	) {
		//Ideally would test storeModelManager wasn't null.
		//Will have to make do with a NullPointerException instead.
		this(modelACL, modelRepository, storeModelManager, storeModelManager.getStore());
	}

	//This is only intended for use by the unit tests.
	//Should try to remove this if possible.
	Model(
		ModelACL modelACL,
		IModelRepository modelRepository,
		AStoreWrapper store
	) {
		this(modelACL, modelRepository, null, store);
	}

	//TODO: why did this need to be private?
	//private Model(
		Model(
		ModelACL modelACL,
		IModelRepository modelRepository,
		StoreModelManager storeModelManager,
		AStoreWrapper store
	) {
		if (modelACL == null) {
			throw new IllegalArgumentException("Attempting to create Model with null modelACL");
		}
		if (modelRepository == null) {
			throw new IllegalArgumentException("Attempting to create Model with null modelRepository");
		}
		if (storeModelManager == null) {
			//This is null when called directly from the unit tests (including REST controller tests),
			//though not when called from the code under test.
			//Need to change how the tests are structured before we can enable this.
			//throw new IllegalArgumentException("Attempting to create Model with null storeModelManager");
		}
		if (store == null) {
			throw new IllegalArgumentException("Attempting to create Model with null store");
		}

		this.modelACL = modelACL;
		this.modelInfo = null;

		this.modelRepository = modelRepository;
		this.storeModelManager = storeModelManager;
		this.store = store;
	}

/////////////////////////////////////////////////////////////////////////
//
// ModelACL - stored in Mongo
//
/////////////////////////////////////////////////////////////////////////

	//Not public
	void saveModelACL() {
		setModified(new Date());
		modelRepository.save(modelACL);
	}

	//Not public
	void deleteModelACL() {
		modelRepository.delete(modelACL);
	}

	public ModelACL getModelACL() {
		return modelACL;
	}

	public String getId() {
		return modelACL.getId();
	}

	public String getUri() {
		return modelACL.getUri();
	}

	//Not public
	void setUri(String uri) {
		modelACL.setUri(uri);
	}

	public String getDomainGraph() {
		return modelACL.getDomainGraph();
	}

	//Not public
	void setDomainGraph(String domainGraph) {
		modelACL.setDomainGraph(domainGraph);
	}

	public Date getCreated() {
		return modelACL.getCreated();
	}

	public void setCreated(Date created) {
		modelACL.setCreated(created);
	}

	public String getUserId() {
		return modelACL.getUserId();
	}

	public void setUserId(String userId) {
		modelACL.setUserId(userId);
	}

	public String getUserName() {
		return modelACL.getUserName();
	}

	public void setUserName(String userName) {
		modelACL.setUserName(userName);
	}

	public Date getModified() {
		return modelACL.getModified();
	}

	public void setModified(Date modified) {
		modelACL.setModified(modified);
	}

	public String getModifiedBy() {
		return modelACL.getModifiedBy();
	}

	public void setModifiedBy(String modifiedBy) {
		modelACL.setModifiedBy(modifiedBy);
	}

	public String getModifiedByName() {
		return modelACL.getModifiedByName();
	}

	public void setModifiedByName(String modifiedByName) {
		modelACL.setModifiedByName(modifiedByName);
	}

	public String getNoRoleUrl() {
		return modelACL.getNoRoleUrl();
	}

	public void setNoRoleUrl(String noRoleUrl) {
		modelACL.setNoRoleUrl(noRoleUrl);
	}

	public String getReadUrl() {
		return modelACL.getReadUrl();
	}

	public void setReadUrl(String readUrl) {
		modelACL.setReadUrl(readUrl);
	}

	public Set<String> getReadUsernames() {
		return modelACL.getReadUsernames();
	}

	public void setReadUsernames(Set<String> readUsernames) {
		modelACL.setReadUsernames(readUsernames);
	}

	public String getWriteUrl() {
		return modelACL.getWriteUrl();
	}

	public void setWriteUrl(String writeUrl) {
		modelACL.setWriteUrl(writeUrl);
	}

	public Set<String> getWriteUsernames() {
		return modelACL.getWriteUsernames();
	}

	public void setWriteUsernames(Set<String> writeUsernames) {
		modelACL.setWriteUsernames(writeUsernames);
	}

	public Set<String> getOwnerUsernames() {
		return modelACL.getOwnerUsernames();
	}

	public void setOwnerUsernames(Set<String> ownerUsernames) {
		modelACL.setOwnerUsernames(ownerUsernames);
	}

	public String getOwnerUrl() {
		return modelACL.getOwnerUrl();
	}

	public void setOwnerUrl(String ownerUrl) {
		modelACL.setOwnerUrl(ownerUrl);
	}

	public String getEditorId() {
		return modelACL.getEditorId();
	}

	public void setEditorId(String editorId) {
		modelACL.setEditorId(editorId);
	}

	public String getEditorName() {
		return modelACL.getEditorName();
	}

	public void setEditorName(String editorName) {
		modelACL.setEditorName(editorName);
	}

	public boolean isValidating() {
		return modelACL.isValidating();
	}

	//Not public
	void setValidating(boolean validating) {
		modelACL.setValidating(validating);
	}

	public boolean isCalculatingRisks() {
		return modelACL.isCalculatingRisk();
	}

	public boolean isStateUpdating() {
		return stateUpdating;
	}

	public void setStateUpdating(boolean stateUpdating) {
		this.stateUpdating = stateUpdating;
	}

	//Not public
	void setCalculatingRisks(boolean calculatingRisks) {
		modelACL.setCalculatingRisk(calculatingRisks);
	}

/////////////////////////////////////////////////////////////////////////
//
// ModelInfo - stored in Jena
//
/////////////////////////////////////////////////////////////////////////

	public boolean hasModelInfo() {
		return modelInfo != null;
	}

	private void assertHasModelInfo() {
		if (!hasModelInfo()) {
			throw new IllegalStateException("modelInfo is null: loadModelInfo() must be called first");
		}
	}

	public void loadModelInfo() {
		modelInfo = new ModelInfo(getQuerier().getModelInfo(store));

		if (!modelACL.getUri().equals(modelInfo.getUri())) {
			throw new RuntimeException(
				"Inconsistent URIs in modelACL <" +
					modelACL.getUri() +
				"> and modelInfo <" +
					modelInfo.getUri() +
				">"
			);
		}

		if (!modelACL.getDomainGraph().equals(modelInfo.getDomainGraph())) {
			throw new RuntimeException(
				"Inconsistent domain graph URIs in modelACL <" +
					modelACL.getDomainGraph() +
				"> and modelInfo <" +
					modelInfo.getDomainGraph() +
				">"
			);
		}

		/*
		 * TODO : work out why this section is needed. It seems to load a set of domain model
		 * object purely in order to write some messages to the logger.
		 * 
		 * These messages do nothing to improve the readability of the log, because this code
		 * is called even for 'trivial' aspects such as status polling by SSM clients. Instead
		 * of one useful heading indicating what domain models are available and what is used,
		 * we get many repeats of the same paragraph that adds no value and obscures other log
		 * content.
		 * 
		 * This has caused difficulties when debugging the population code changes, so at least
		 * temporarily we should suppress most of the logger statements.
		 */
		if (storeModelManager != null) {
			Map<String, Map<String, Object>> domainModels = storeModelManager.getDomainModels();

			if (domainModels.size() > 0) {
				String domainGraph = modelInfo.getDomainGraph();
				String domainVersion = null;
				Map<String, Object> domainModel = domainModels.get(domainGraph);

				if (domainModel == null) {
					logger.error("Could not locate domain model for graph: " + domainGraph);
				}
				else {
					domainVersion = (String) domainModel.get("version");
				}

				this.setDomainVersion(domainVersion);
			}
			else {
				logger.warn("loadModelInfo: no domain models currently loaded");
			}
		}
		else {
			logger.warn("loadModelInfo: storeModelManager is null");
		}
	}

	public uk.ac.soton.itinnovation.security.model.system.Model getModelInfo() {
		return modelInfo.getModel();
	}

	public void updateCopiedModelInfo(uk.ac.soton.itinnovation.security.model.system.Model sourceModel) {
		getUpdater().updateModelInfoInCopiedModel(store, sourceModel, this.getUri());
	}

	private void saveModelInfo() {
		assertHasModelInfo();
		getUpdater().updateModelInfo(store, modelInfo.getModel());
	}

	public String getName() {
		assertHasModelInfo();
		return modelInfo.getName();
	}

	public void setName(String name) {
		assertHasModelInfo();
		modelInfo.setName(name);
	}

	public String getDescription() {
		assertHasModelInfo();
		return modelInfo.getDescription();
	}

	public void setDescription(String description) {
		assertHasModelInfo();
		modelInfo.setDescription(description);
	}

	public String getValidatedDomainVersion() {
		assertHasModelInfo();
		return modelInfo.getValidatedDomainVersion();
	}

	public boolean isValid() {
		assertHasModelInfo();
		return modelInfo.isValid();
	}

	public void setValid(boolean valid) {
		assertHasModelInfo();

		if (isValidating() && !this.stateUpdating) {
			throw new IllegalStateException("Cannot set valid: model is validating");
		}
		if (isCalculatingRisks() && !this.stateUpdating) {
			throw new IllegalStateException("Cannot set valid: model is calculating risks");
		}

		modelInfo.setValid(valid);
	}

	public boolean riskLevelsValid() {
		assertHasModelInfo();
		return modelInfo.riskLevelsValid();
	}

	public void setRiskLevelsValid(boolean riskLevelsValid) {
		assertHasModelInfo();

		if (isValidating() && !this.stateUpdating) {
			throw new IllegalStateException("Cannot set riskLevelsValid: model is validating");
		}
		if (isCalculatingRisks() && !this.stateUpdating) {
			throw new IllegalStateException("Cannot set riskLevelsValid: model is calculating risks");
		}

		modelInfo.setRiskLevelsValid(riskLevelsValid);
	}

	public Level getRiskLevel() {
		assertHasModelInfo();
		return modelInfo.getRiskLevel();
	}

	public RiskCalculationMode getRiskCalculationMode() {
		assertHasModelInfo();
		return modelInfo.getRiskCalculationMode();
	}

	public void setRiskCalculationMode(RiskCalculationMode riskCalculationMode) {
		assertHasModelInfo();
		modelInfo.setRiskCalculationMode(riskCalculationMode);
	}


/////////////////////////////////////////////////////////////////////////
//
// Queried from domain model
//
/////////////////////////////////////////////////////////////////////////

	public String getDomainVersion() {
		return this.domainVersion;
	}

	public void setDomainVersion(String domainVersion) {
		this.domainVersion = domainVersion;
	}


/////////////////////////////////////////////////////////////////////////
//
// ModelData - stored in Jena
//
/////////////////////////////////////////////////////////////////////////

	public boolean hasModelData() {
		return modelData != null;
	}

	private void assertHasModelData() {
		if (!hasModelData()) {
			throw new IllegalStateException("modelData is null: loadModelData() must be called first");
		}
	}

	public void loadModelData(
		ModelObjectsHelper modelObjectsHelper,
		LoadingProgress loadingProgress
	) {
		this.modelData = new ModelData(this, modelObjectsHelper, loadingProgress);
	}

	/**
	 * Get basic model details only
	 * @param modelObjectsHelper
	 */
	public void loadModelInfo(
		ModelObjectsHelper modelObjectsHelper
	) {
		this.modelData = new ModelData(this, modelObjectsHelper, ModelData.Mode.INFO);
	}

	/**
	 * Get basic model details plus risks data (i.e. misbehaviours and assets)
	 * @param modelObjectsHelper
	 */
	public void loadModelAndRisksData(
		ModelObjectsHelper modelObjectsHelper
	) {
		this.modelData = new ModelData(this, modelObjectsHelper, ModelData.Mode.RISKS);
	}

	public Set<Asset> getAssets() {
		assertHasModelData();
		return modelData.getAssets();
	}

	public Set<Relation> getRelations() {
		assertHasModelData();
		return modelData.getRelations();
	}

	public Map<String, MisbehaviourSet> getMisbehaviourSets() {
		assertHasModelData();
		return modelData.getMisbehaviourSets();
	}

	public Map<String, TrustworthinessAttributeSet> getTwas() {
		assertHasModelData();
		return modelData.getTwas();
	}

	public Set<ControlSet> getControlSets() {
		assertHasModelData();
		return modelData.getControlSets();
	}

	public Map<String, ControlStrategy> getControlStrategies() {
		assertHasModelData();
		return modelData.getControlStrategies();
	}

	public Set<Threat> getThreats() {
		assertHasModelData();
		return modelData.getThreats();
	}

	public Set<ComplianceThreat> getComplianceThreats() {
		assertHasModelData();
		return modelData.getComplianceThreats();
	}

	public Set<ComplianceSet> getComplianceSets() {
		assertHasModelData();
		return modelData.getComplianceSets();
	}

	public Map<String, Collection<Level>> getLevels() {
		assertHasModelData();
		return modelData.getLevels();
	}

	public Set<AssetGroup> getAssetGroups() {
		assertHasModelData();
		return modelData.getAssetGroups();
	}

	public RiskVector getRiskVector() {
		assertHasModelData();
		return modelData.getRiskVector();
	}

/////////////////////////////////////////////////////////////////////////
//
// Validation and risk calculation state machine
//
/////////////////////////////////////////////////////////////////////////

	public void invalidate() {
		logger.debug("Invalidating model: {}", getUri());
		setValid(false);
		save();
	}

	public void invalidateRiskLevels() {
		logger.debug("Invalidating risk levels: {}", getUri());
		setRiskLevelsValid(false);
		save();
	}

	public void markAsValidating() {
		logger.info("Marking as validating: {}", getUri());
		setStateUpdating(true);
		setValidating(true);
		save();
		
		logger.info("Invalidating model: {}", getUri());
		setValid(false);
		setRiskLevelsValid(false);
		setRiskCalculationMode(null); //clear any previous risk calc mode
		setStateUpdating(false);
	}

	public void finishedValidating(boolean result) {
		logger.debug("Setting validation result ({}): {}", result, getUri());
		setValidating(false);
		setValid(result);
		save();
	}

	public void markAsCalculatingRisks(RiskCalculationMode rcMode, boolean save) {
		logger.info("Marking as calculating risks: {}", getUri());
		setStateUpdating(true);
		setCalculatingRisks(true);
		save();
		
		if (save) {
			logger.info("Invalidating risk levels: {}", getUri());
			setRiskLevelsValid(false);
			setRiskCalculationMode(rcMode);
		}

		setStateUpdating(false);
	}

	public void finishedCalculatingRisks(boolean result, RiskCalculationMode rcMode, boolean save) {
		logger.debug("Setting risk calculation result ({}): {}", result, getUri());
		setCalculatingRisks(false);
		if (save) {
			//If saving risk calc results, store the risksValid value prior to saving
			setRiskLevelsValid(result);
			setRiskCalculationMode(rcMode);
			save();
		}
		else {
			//Otherwise save the other params and set the risksValid flag only for the response
			save();
			setRiskLevelsValid(result);
			setRiskCalculationMode(rcMode);
		}
	}

/////////////////////////////////////////////////////////////////////////
//
// Other methods
//
/////////////////////////////////////////////////////////////////////////

	@Override
	public String toString() {
		String modelDetails = modelACL.toString();

		if (hasModelInfo()) {
			modelDetails += modelInfo.toString();
		}
		
		modelDetails += "\ndomainVersion: " + getDomainVersion();
		
		if (hasModelData()) {
			modelDetails += modelData.toString();
		}

		return modelDetails;
	}

	//Saves data to MongoDB and JenaTDB
	public void save() {
		logger.debug("Saving model: {}", getUri());
		saveModelACL();
		saveModelInfo();
	}

	//Only saves data to MongoDB
	//Used for when we know no triples have been changed
	public void saveModelACLonly() {
		logger.debug("Saving model: {}", getUri());
		saveModelACL();
	}

	//Should not be called directly by unit tests
	public void delete() {
		logger.debug("Deleting model: {}", getUri());
		deleteModelACL();

		//In unit tests storeModelManager can be null
		storeModelManager.deleteSystemModel(getUri());
	}

	public ModelStack getModelStack() {
		if (stack == null) {
			if (getUri() == null || getDomainGraph() == null) {
				throw new RuntimeException("Missing URI or domainGraph from model");
			}

			stack = new ModelStack();

			stack.addGraph("core", "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/core");
			stack.addGraph("domain", getDomainGraph());
			stack.addGraph("system", getUri());
			stack.addGraph("system-ui", getUri() + "/ui");
			stack.addGraph("system-inf", getUri() + "/inf");
			stack.addGraph("system-meta", getUri() + "/meta");
		}

		return stack;
	}

	public SystemModelQuerier getQuerier() {
		if (querier == null) {
			querier = new SystemModelQuerier(getModelStack());
		}

		return querier;
	}

	public SystemModelUpdater getUpdater() {
		if (updater == null) {
			updater = new SystemModelUpdater(getModelStack());
		}

		return updater;
	}
}
