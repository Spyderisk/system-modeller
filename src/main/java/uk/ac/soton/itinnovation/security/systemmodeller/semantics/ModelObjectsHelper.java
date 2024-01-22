/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2017
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
//  Created By :			Maxim Bashevoy
//  Created Date :		  2017-02-21
//  Updated By :			Stefanie Wiegand
//  Created for Project :   5G-ENSURE, ASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.semantics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.cli.MissingArgumentException;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import uk.ac.soton.itinnovation.security.domainreasoner.IDomainReasoner;
import uk.ac.soton.itinnovation.security.model.Level;
import uk.ac.soton.itinnovation.security.model.domain.Control;
import uk.ac.soton.itinnovation.security.model.system.Asset;
import uk.ac.soton.itinnovation.security.model.system.AssetGroup;
import uk.ac.soton.itinnovation.security.model.system.ComplianceSet;
import uk.ac.soton.itinnovation.security.model.system.ComplianceThreat;
import uk.ac.soton.itinnovation.security.model.system.ControlSet;
import uk.ac.soton.itinnovation.security.model.system.ControlStrategy;
import uk.ac.soton.itinnovation.security.model.system.MisbehaviourSet;
import uk.ac.soton.itinnovation.security.model.system.Pattern;
import uk.ac.soton.itinnovation.security.model.system.Relation;
import uk.ac.soton.itinnovation.security.model.system.RiskVector;
import uk.ac.soton.itinnovation.security.model.system.Threat;
import uk.ac.soton.itinnovation.security.model.system.TrustworthinessAttributeSet;
import uk.ac.soton.itinnovation.security.modelquerier.util.ModelStack;
import uk.ac.soton.itinnovation.security.modelquerier.util.TemplateLoader;
import uk.ac.soton.itinnovation.security.modelvalidator.ModelValidator;
import uk.ac.soton.itinnovation.security.modelvalidator.Progress;
import uk.ac.soton.itinnovation.security.semanticstore.AStoreWrapper;
import uk.ac.soton.itinnovation.security.semanticstore.util.SparqlHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.auth.KeycloakAdminClient;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.LoadingProgress;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.ModelException;

@Component
public class ModelObjectsHelper {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static final String SYSTEM_PREFIX = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/system#";

	@Autowired
	private StoreModelManager storeManager;

	@Autowired
	private KeycloakAdminClient keycloakAdminClient;

	@Autowired
	private ModelObjectsHelper modelHelper;

	private Map<String, String> queries;
	
	private Map<String, Map<String, String>> modelAssetIDs;
	private Map<String, Map<String, String>> modelAssetUris;
	private Map<String, Set<Threat>> modelThreats;

	private Map<String, Progress> modelValidationProgress;
	private Map<String, ScheduledFuture<?>> validationFutures;
	private HashMap<String, ScheduledFuture<?>> loadingFutures;

	private Map<String, LoadingProgress> modelLoadingProgress;

	private Map<String, Object> modelLocks;

	@Value("${set.accepted.threats.as.resolved}")
	private boolean setAcceptedThreatsAsResolved;
	
	private List<String> defaultUserDomainModels; //TODO: persist in TDB instead

	/**
	 * Initialises this component.
	 */
	@PostConstruct
	public void init() throws IOException {
		logger.debug("Initialising Model Objects Helper");
		modelAssetIDs = new HashMap<>();
		modelAssetUris = new HashMap<>();
		modelThreats = new HashMap<>();
		modelValidationProgress = new HashMap<>();
		validationFutures = new HashMap<>();
		loadingFutures = new HashMap<>();
		modelLoadingProgress = new HashMap<>();
		modelLocks = new HashMap<>();
		queries = loadQueries();
		logger.debug("Finished initialising Model Objects Helper");
	}

	// Non-querying utility methods ///////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates unique URI for a Model Asset.
	 *
	 * @return
	 */
	public String createNewAssetUri() {
		return SYSTEM_PREFIX + getRandomString();
	}

	/**
	 * Creates unique URI for a Model Asset Group.
	 *
	 * @return
	 */
	public String createNewAssetGroupUri() {
		return SYSTEM_PREFIX + "Group-" + getRandomString();
	}

	private String getRandomString() {
		//generate a random hexadecimal string
		Random r = new Random();
		StringBuilder sb = new StringBuilder();

		while(sb.length() < 8){
			sb.append(Integer.toHexString(r.nextInt()));
		}

		return sb.toString();
	}

	/**
	 * Returns the fully qualified reasoner class name for the domain model
	 * @param model the model for which to return the reasoner class
	 * @return the reasoner class name
	 */
	public String getModelReasonerClassForModel(Model model){
		logger.debug("Getting the reasoner class for domain model: {}", model.getDomainGraph());
		return this.getReasonerClass(model.getModelStack(), storeManager.getStore());
	}

	/**
	 * N.B. This code snippet taken from DomainModelQuerier, to avoid dependency on this class
	 * Gets the fully qualified reasoner class from the store, returns null if cannot find one
	 * @param store the store that is to be queried
	 * @return the fully qualified java class
	 */
	private String getReasonerClass(ModelStack model, AStoreWrapper store){
		String sparql = "SELECT DISTINCT ?class" +
				"WHERE{" +
				"	?s core:reasonerClass ?class ." +
				"}";

		for(Map<String, String> row: store.translateSelectResult(store.querySelect(
				sparql, model.getGraph("domain")))){
			return row.get("class");
		}

		return null;
	}

	/**
	 * Returns the domain specific reasoner for the given model
	 * @param model the model for which to return the reasoner object
	 * @return the reasoner object
	 */
	public IDomainReasoner getDomainSpecificReasoner(Model model) {
		logger.info("Getting domain specific reasoner class for model: {}", model != null ? model.getUri() : "unknown");
		String domainSpecificReasonerClass = getModelReasonerClassForModel(model);
		logger.info("domainSpecificReasonerClass = " + domainSpecificReasonerClass);

		if (model!=null && domainSpecificReasonerClass == null) {
			logger.warn("Could not get domain specific reasoner from domain model: {}", model.getDomainGraph());
			return null;
		}

		try {
			Constructor<?> constructor = Class.forName(domainSpecificReasonerClass).getConstructor();
			Object domainReasoner = constructor.newInstance();
			return (IDomainReasoner) domainReasoner;
		} catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
			logger.warn("Could create domain specific reasoner for class: {}", domainSpecificReasonerClass, ex);
			return null;
		}
	}

	/**
	 * Creates ModelValidator for a model.
	 *
	 * @param model
	 * @return
	 */
	public ModelValidator getModelValidatorForModel(Model model) {

		ModelValidator validator;

		try {
			IDomainReasoner domainReasoner = getDomainSpecificReasoner(model);
			validator = new ModelValidator(storeManager.getStore(), model.getModelStack(), domainReasoner);
		} catch (MissingArgumentException ex) {
			logger.error("Could not create model validator", ex);
			validator = null;
		}

		return validator;
	}

	public void clearInferredGraph(Model model) {
		logger.info("Clearing inferred graph...");
		storeManager.getStore().clearGraph(model.getModelStack().getGraph("system-inf"));
		logger.info("Done");
	}

	public synchronized Object getModelLock(Model model) {
		String modelId = model.getId();
		//logger.debug("Getting lock object for model: " + modelId);
		Object modelLock = modelLocks.get(modelId);
		if (modelLock == null) {
			modelLock = new Object();
			modelLocks.put(modelId, modelLock);
		}
		return modelLock;
	}
	
	private Map<String, String> getAssetIDsForModel(Model model) {
		Map<String, String> assetIDs = this.modelAssetIDs.get(model.getId());
		Map<String, String> assetUris = this.modelAssetUris.get(model.getId());

		if ( (assetIDs == null) || (assetUris == null) ) {
			logger.warn("No asset URIs map for model: {} - creating now...", model.getId());
			assetIDs = new HashMap<>();
			assetUris = new HashMap<>();
			this.modelAssetIDs.put(model.getId(), assetIDs);
			this.modelAssetUris.put(model.getId(), assetUris);
		}

		return assetIDs;
	}

	private Map<String, String> getAssetURIsForModel(Model model) {
		Map<String, String> assetIDs = this.modelAssetIDs.get(model.getId());
		Map<String, String> assetUris = this.modelAssetUris.get(model.getId());

		if ( (assetIDs == null) || (assetUris == null) ) {
			logger.warn("No asset URIs map for model: {} - creating now...", model.getId());
			assetIDs = new HashMap<>();
			assetUris = new HashMap<>();
			this.modelAssetIDs.put(model.getId(), assetIDs);
			this.modelAssetUris.put(model.getId(), assetUris);
		}

		return assetUris;
	}

	public Set<Threat> getCachedThreatsForModel(Model model) {
		Set<Threat> threats = this.modelThreats.get(model.getId());

		if ( (threats == null) ) {
			logger.warn("No threats map for model: {} - creating now...", model.getId());
			threats = new HashSet<>();
			this.modelThreats.put(model.getId(), threats);
		}

		return threats;
	}

	private void updateCachedThreat(Threat threat, Model model) {
		//TODO: this does nothing! @kem please investigate!
		Set<Threat> cachedThreats = this.getCachedThreatsForModel(model);
		cachedThreats.add(threat);
	}

	public void addAssetToCache(Asset asset, Model model) {
		String id = asset.getID();
		String uri = asset.getUri();
		logger.debug("Adding asset to cache: {}, {}", id, uri);
		Map<String, String> assetIDs = getAssetIDsForModel(model);
		Map<String, String> assetUris = getAssetURIsForModel(model);
		assetIDs.put(uri, id);
		assetUris.put(id, uri);
	}

	public void deleteAssetFromCache(Asset asset, Model model) {
		String id = asset.getID();
		String uri = asset.getUri();
		logger.debug("Deleting asset from cache: {}, {}", id, uri);
		Map<String, String> assetIDs = getAssetIDsForModel(model);
		Map<String, String> assetUris = getAssetURIsForModel(model);
		assetIDs.remove(uri);
		assetUris.remove(id);
	}
	
	public boolean registerValidationExecution(String modelId, ScheduledFuture<?> future) {

		if (validationFutures.containsKey(modelId)){
			ScheduledFuture<?> validationExecution = validationFutures.get(modelId);
			if (validationExecution.isDone()) {
				logger.debug("Clearing previous validation execution");
				//TODO: tidy up previous execution
			} else {
				logger.warn("Validation execution already registered (still running)");
				return false;
			}
		}

		logger.debug("Registering validation execution for model: {}", modelId);
		validationFutures.put(modelId, future);
		return true;
	}

	public boolean registerLoadingExecution(String modelId, ScheduledFuture<?> future) {

		if (loadingFutures.containsKey(modelId)){
			ScheduledFuture<?> loadingExecution = loadingFutures.get(modelId);
			if (loadingExecution.isDone()) {
				logger.debug("Clearing previous loading execution");
				//TODO: tidy up previous execution
			} else {
				logger.warn("Validation execution already registered (still running)");
				return false;
			}
		}

		logger.debug("Registering loading execution for model: {}", modelId);
		loadingFutures.put(modelId, future);
		return true;
	}

	public Progress getValidationProgressOfModel(Model model){
		String modelId = model.getId();
		Progress validationProgress;
		if (modelValidationProgress.containsKey(modelId)){
			validationProgress = modelValidationProgress.get(modelId);
		} else {
			validationProgress = new Progress(modelId);
			modelValidationProgress.put(modelId, validationProgress);
		}

		//TODO: remove this logging, once recommendations are working
		logger.info("Validation progress status: {}", validationProgress.getStatus());
		
		// No need to check execution if not yet running
		if (! "running".equals(validationProgress.getStatus())) {
			logger.info("Validation not running - not checking execution status");
			return validationProgress;
		}

		if (validationFutures.containsKey(modelId)) {
			ScheduledFuture<?> validationExecution = validationFutures.get(modelId);
			if (validationExecution.isDone()) {
				Object result;

				try {
					result = validationExecution.get();
					logger.debug("Validation result: {}", result != null ? result.toString() : "null");
					if ( (result == null) || (result.equals(false)) ) {
						validationProgress.updateProgress(1.0, "Validation failed", "failed", "Unknown error");
					}
					else {
						validationProgress.updateProgress(1.0, "Validation complete", "completed");
					}
				} catch (InterruptedException ex) {
					logger.error("Could not get validation progress", ex);
					validationProgress.updateProgress(1.0, "Validation cancelled", "cancelled");
				} catch (ExecutionException ex) {
					logger.error("Could not get validation progress", ex);
					validationProgress.updateProgress(1.0, "Validation failed", "failed", ex.getMessage());
				}
				
				// Finally, remove the execution from the list
				logger.debug("Unregistering validation execution for model: {}", modelId);
				validationFutures.remove(modelId);
				//KEM - don't remove the progress object here, as others requests still need access to this
				//(e.g. another user may monitor validation progress)
				//modelValidationProgress.remove(modelId);
			}
		}
		else {
			logger.warn("No registered execution for model validation: {}", modelId);
		}

		//logger.info("Validation progress: {}", validationProgress);
		return validationProgress;
	}
	
	public LoadingProgress createLoadingProgressOfModel(Model model, String loadingProgressID){

		if (modelLoadingProgress.containsKey(loadingProgressID)) {
			String message = "Loading ID " + loadingProgressID + " already exists for model " + model.getId();
			logger.error(message);
			throw new IllegalArgumentException(message);
		}

		LoadingProgress loadingProgress = new LoadingProgress(model.getId());
		modelLoadingProgress.put(loadingProgressID, loadingProgress);

		return loadingProgress;
	}

	public LoadingProgress getLoadingProgressOfModel(String loadingProgressID){

		if (!modelLoadingProgress.containsKey(loadingProgressID)) {
			return null;
		}

		LoadingProgress loadingProgress = modelLoadingProgress.get(loadingProgressID);

		// No need to check execution if not yet running
		if (! "loading".equals(loadingProgress.getStatus())) {
			return loadingProgress;
		}

		if (loadingFutures.containsKey(loadingProgressID)) {
			ScheduledFuture<?> loadingExecution = loadingFutures.get(loadingProgressID);
			if (loadingExecution.isDone()) {
				Object result;

				try {
					result = loadingExecution.get();
					logger.debug("Loading result: {}", result != null ? result.toString() : "null");
					if ( (result == null) || (! (result instanceof Model)) ) {
						loadingProgress.updateProgress(1.0, "Loading failed", "failed", "Unknown error", null);
					}
					else {
						loadingProgress.updateProgress(1.0, "Loading complete", "completed", "", (Model)result);
					}
				} catch (InterruptedException ex) {
					logger.error("Could not get loading progress", ex);
					loadingProgress.updateProgress(1.0, "Loading cancelled", "cancelled");
				} catch (ExecutionException ex) {
					Throwable cause = ex.getCause();
					if (cause instanceof ModelException) {
						ModelException me = (ModelException)cause;
						logger.error("Model exception", me);
						loadingProgress.updateProgress(1.0, "Model error", "failed", me.getMessage(), me.getModel());
					}
					else {
						logger.error("Could not get loading progress", ex);
						loadingProgress.updateProgress(1.0, "Loading failed", "failed", ex.getMessage(), null);
					}
				}

				// Finally, remove the execution from the list
				logger.debug("Unregistering loading execution for id: {}", loadingProgressID);
				loadingFutures.remove(loadingProgressID);
				//KEM - don't remove the progress object here, as others requests still need access to this
				//(e.g. another user may monitor loading progress)
				//KEM 2/3/2018 reinstating this, as it causes memory problems without it (i.e. grogress objects hold onto loaded model object)
				//TODO: investigate if there really are any multi-user consequences of removing the loading progress object
				modelLoadingProgress.remove(loadingProgressID);
			}
		} else {
			logger.warn("No registered execution for model loading id: {}", loadingProgressID);
		}

		return loadingProgress;
	}

	public String getAssetUri(String id, Model model) {
		//logger.debug("Looking up assetUri for ID: {}", id);
		Map<String, String> assetIDs = getAssetIDsForModel(model);
		Map<String, String> assetUris = getAssetURIsForModel(model);
		String assetUri = assetUris.get(id);

		if (assetUri == null) {
			logger.warn("No asset uri cached for id: {} - checking store... ", id);
			/*
			Asset asset = getAssetById(id, model, true);
			if(asset == null){
				logger.warn("Could not find asset for id: {}, returning null", id);
				return null;
			}
			assetUri = asset.getUri();
			*/
			assetUri = this.getSystemAssetURI(id, model);
			logger.warn("Located asset uri: {} - adding to cache... ", assetUri);
			assetIDs.put(assetUri, id);
			assetUris.put(id, assetUri);
		}

		//logger.debug("assetUri for ID: {} = {}", id, assetUri);
		return assetUri;
	}
	
	public String getSystemAssetURI(String assetId, Model model) {
		// ----- Get asset URI -----
		//logger.debug("Getting URI for asset: {}", assetId);
		
		String assetURI = null;
		
		String sparql = "SELECT * WHERE {\r\n" + 
				"	?a core:hasID \"" + SparqlHelper.escapeLiteral(assetId) + "\" .\n" +
				"}";
		
		AStoreWrapper store = storeManager.getStore();
		ModelStack stack = model.getModelStack();
		
		List<Map<String, String>> rows = store.translateSelectResult(store.querySelect(sparql, stack.getGraph("system")));
		
		if (rows.size() > 1) {
			logger.warn("multiple URIs found for asset: {}", assetId);
		} 
		
		if (rows.size() > 0) {
			Map<String, String> row = rows.iterator().next();
			assetURI = row.get("a");
			//logger.debug("asset {} has URI {}", assetId, assetURI);
		}
		else {
			logger.warn("could not locate URI for asset: {}", assetId);
		}
		
		return assetURI;
	}

	public String getAssetId(String uri, Model model) {
		logger.debug("Looking up asset ID for URI: {}", uri);
		Map<String, String> assetIDs = getAssetIDsForModel(model);
		Map<String, String> assetUris = getAssetURIsForModel(model);

		String assetId = assetIDs.get(uri);

		if (assetId == null) {
			logger.warn("No asset id cached for uri: {} - checking store... ", uri);
			Asset asset = getAssetForModel(uri, model);
			assetId = asset.getID();
			logger.warn("Located asset id: {} - adding to cache... ", assetId);
			assetIDs.put(uri, assetId);
			assetUris.put(assetId, asset.getUri());
		}

		logger.debug("asset ID for URI: {} = {}", uri, assetId);
		return assetId;
	}
	
	private Map<String, String> loadQueries() throws IOException {
		Map<String, String> queries = new HashMap<>();
		
		//TODO don't hardcode this stuff
		List<File> directories = new ArrayList<>();
		directories.add(new File(ModelObjectsHelper.class.getClassLoader().getResource("sparql/palette").getPath()));
		directories.add(new File(ModelObjectsHelper.class.getClassLoader().getResource("sparql/management").getPath()));
		
		BufferedReader br;
		for(File d : directories){
			for(File file : d.listFiles()){
				String contents = "";
				br = new BufferedReader(new FileReader(file));
				while(br.ready()){
					contents += br.readLine();
				}
				queries.put(file.getName().replace(".sparql", ""), contents);
				br.close();
			}
		}
		
		return queries;
	}

	// Get from store /////////////////////////////////////////////////////////////////////////////////////////////////
	// These should be called from the REST controllers to get things from the store

	public Asset getAssetById(String assetId, Model model, boolean fullDetails) {

		if (fullDetails) {
			return model.getQuerier().getSystemAssetById(storeManager.getStore(), assetId);
		}
		else {
			//For basic details, get the asset URI from the cache
			String uri = this.getAssetUri(assetId, model);
			if (uri == null) {
				logger.warn("Asset URI not found for id: {} - returning null.", assetId);
				return null;
			}
			Asset asset = new Asset();
			asset.setUri(uri);
			return asset;
		}
		
	}

	/**
	 * Attempts to verify and return valid Asset UI from an asset store URI.
	 *
	 * @param assetUri
	 * @param model
	 * @return
	 * @throws UnexpectedException
	 */
	public Asset getAssetForModel(String assetUri, Model model) {

		logger.info("Returning asset <{}> for model <{}>", assetUri, model.getUri());
		return model.getQuerier().getSystemAsset(storeManager.getStore(), assetUri);
	}

	public Map<String, Collection<Level>> getLevelsForModel(Model model) {
		HashMap<String, Collection<Level>> levelsMap = new HashMap<>();

		levelsMap.put("TrustworthinessLevel", model.getQuerier().getLevels(storeManager.getStore(), "TrustworthinessLevel").values());
		levelsMap.put("ImpactLevel", model.getQuerier().getLevels(storeManager.getStore(), "ImpactLevel").values());
		levelsMap.put("Likelihood", model.getQuerier().getLevels(storeManager.getStore(), "Likelihood").values());
		levelsMap.put("RiskLevel", model.getQuerier().getLevels(storeManager.getStore(), "RiskLevel").values());
		levelsMap.put("PopulationLevel", model.getQuerier().getLevels(storeManager.getStore(), "PopulationLevel").values());

		return levelsMap;
	}

	/**
	 * Returns TWAS for an asset from a model
	 * 
	 * @param asset The asset object
	 * @param model The model object
	 * @return A map of the TWAS
	 */
	public Map<String, TrustworthinessAttributeSet> getAssetTwas(Asset asset, Model model) {
		Map<String, TrustworthinessAttributeSet> twas = model.getQuerier().getTrustworthinessAttributeSetsForAssetURI(storeManager.getStore(), asset.getUri());
		return twas;
	}

	/**
	 * Returns control sets for an asset from a model
	 * 
	 * @param asset The asset object
	 * @param model The model object
	 * @return A map of the control sets
	 */
	public Map<String, ControlSet> getAssetControlSets(Asset asset, Model model) {
		Map<String, ControlSet> controlSets = model.getQuerier().getControlSets(storeManager.getStore(), asset.getUri(), null);
		return controlSets;
	}

	public Map<String, Control> getControls(Model model) {
		Map<String, Control> controls = model.getQuerier().getControls(storeManager.getStore());
		return controls;
	}

	/**
	 * Returns semantic assets for model.
	 *
	 * @param model
	 * @param refreshCache
	 * @return
	 */
	public Set<Asset> getAssetsForModel(Model model, boolean refreshCache) {
		return getAssetsForModel(model, refreshCache,
			getControlSetsForModel(model), getMisbehavioursForModel(model, false), getTWASForModel(model));
	}

	public Set<Asset> getAssetsForModel(Model model, boolean refreshCache, Map<String, ControlSet> controlSets,
			Map<String, MisbehaviourSet> misbehaviourSets, Map<String, TrustworthinessAttributeSet> twas) {

		logger.debug("Getting asset(s) for model <{}> ({})", model.getUri(), model.getId());
		Map<String, Asset> assets = model.getQuerier().getSystemAssets(storeManager.getStore(), controlSets, misbehaviourSets, twas, true, true);
		Collection<Asset> assetsSet = assets.values();

		// Generate map of id => uri, uri => id, for all assets in this model
		Map<String, String> assetIDs = this.modelAssetIDs.get(model.getId());
		Map<String, String> assetUris = this.modelAssetUris.get(model.getId());

		if ( (assetIDs == null) || (assetUris == null) || refreshCache ) {
			logger.debug("Generating asset IDs/URIs map for model <{}> ({})", model.getUri(), model.getId());
			assetIDs = new HashMap<>();
			assetUris = new HashMap<>();

			for (Asset asset : assetsSet) {
				String assetId = asset.getID();
				assetIDs.put(asset.getUri(), assetId);
				assetUris.put(assetId, asset.getUri());
			}

			this.modelAssetIDs.put(model.getId(), assetIDs);
			this.modelAssetUris.put(model.getId(), assetUris);
		}

		logger.info("Returning {} asset(s) for model <{}>", assets.size(), model.getUri());

		Set<Asset> ass = new HashSet<>();
		ass.addAll(assetsSet);
		return ass;
	}

	public Set<Asset> getAssetsForModel(Model model) {
		return getAssetsForModel(model, false); //don't refresh cache by default
	}

	public Set<Asset> getBasicAssetsForModel(Model model) {
		logger.debug("Getting basic assets for model <{}> ({})", model.getUri(), model.getId());
		Map<String, Asset> assets = model.getQuerier().getBasicSystemAssets(storeManager.getStore());
		Collection<Asset> assetsSet = assets.values();

		Set<Asset> ass = new HashSet<>();
		ass.addAll(assetsSet);
		return ass;
	}

	/**
	 * Gets all cached asset URIs for a model.
	 *
	 * @param model
	 * @return map of asset IDs to URIs
	 */
	public Map<String, String> getAssetUrisForModel(Model model) {
		Map<String, String> assetUris = this.getAssetURIsForModel(model);
		return assetUris;
	}
	
	/**
	 * Gets all cached assets for a model.
	 * N.B. This ONLY returns the basic asset objects with their URI defined,
	 * so is useful for methods such as querier.getAssetGroupById, which require
	 * a list of assets, but in fact only use the URI from each.
	 *
	 * @param model
	 * @return map of asset URIs to Asset
	 */
	public Map<String, Asset> getBasicAssetsMap(Model model) {
		Map<String, String> assetUris = getAssetUrisForModel(model);
		
		HashMap<String, Asset> assets = new HashMap<>();
		for (String assetUri : assetUris.values()) {
			Asset asset = new Asset();
			asset.setUri(assetUri);
			assets.put(assetUri, asset);
		}
		
		return assets;
	}

	/**
	 * Get a single asset group for a model.
	 *
	 * @param model
	 * @param groupId
	 * @return asset group
	 */
	public AssetGroup getAssetGroupById(Model model, String groupId) {
		Map<String, Asset> assets = getBasicAssetsMap(model);
		AssetGroup assetGroup = model.getQuerier().getAssetGroupById(storeManager.getStore(), groupId, assets);
		
		return assetGroup;
	}
	
	public Set<AssetGroup> getAssetGroups(Model model) {
		Map<String, Asset> assets = getBasicAssetsMap(model);
		Map<String, AssetGroup> assetGroupsMap = model.getQuerier().getAssetGroups(storeManager.getStore(), assets);
		
		Set<AssetGroup> assetGroups = new HashSet<>(assetGroupsMap.values());
		return assetGroups;
	}
	
	public Threat getThreat(String id, Model model) {

		logger.debug("Looking up threat for ID: {}", id);
		Set<Threat> threats = getCachedThreatsForModel(model);
		Threat threat = null;
		for (Threat t: threats) {
			if (t.getID().equals(id)) {
				threat = t;
				break;
			}
		}

		if (threat == null) {
			logger.warn("No threat cached for id: {} - checking store... ", id);
			threat = getThreatById(id, model, true, false); //full details, not cached
			if(threat == null) {
				logger.warn("Could not find threat for id: {} - returning null.", id);
				return null;
			}
			logger.warn("Located threat: {} - adding to cache... ", threat.getUri());
			threats.add(threat);
		}

		logger.debug("threatUri for ID: {} = {}", id, threat.getUri());
		return threat;
	}

	public Threat getThreatById(String threatId, Model model, boolean fullDetails, boolean cached) {

		if (! cached) {
			return model.getQuerier().getSystemThreatById(storeManager.getStore(), threatId);
		} else {
			return this.getThreat(threatId, model);
		}
	}

	/**
	 * Get all misbehaviour sets from the store
	 *
	 * @param model the model for which to get the misbehaviour sets
	 * @param includeCauseAndEffects flag to indicate whether cause and effect data should be included
	 * @return a map of misbehaviour sets by uri
	 */
	public Map<String, MisbehaviourSet> getMisbehavioursForModel(Model model, boolean includeCauseAndEffects) {

		Map<String, MisbehaviourSet> ms = model.getQuerier().getMisbehaviourSets(storeManager.getStore(), includeCauseAndEffects);
		logger.info("Returning {} misbehaviour sets(s) for model <{}>", ms.size(), model.getUri());
		return ms;
	}

	public MisbehaviourSet getMisbehaviourById(String misbehaviourId, Model model) {

		logger.info("getMisbehaviourById: " + misbehaviourId);
		return model.getQuerier().getMisbehaviourSetByID(storeManager.getStore(), misbehaviourId, true);
	}

	public MisbehaviourSet getMisbehaviourByUri(String misbehaviourUri, Model model) {

		logger.info("getMisbehaviourByUri: " + misbehaviourUri);
		return model.getQuerier().getMisbehaviourSet(storeManager.getStore(), misbehaviourUri, true);
	}

	public RiskVector getModelRiskVector(Model model) {
		if (! model.riskLevelsValid()) {
			logger.warn("Risk levels not valid: returning null risk vector");
			return null;
		}
		
		//Get all defined levels for model
		Map<String, Collection<Level>> levels = this.getLevelsForModel(model);
		
		//Get risk levels
		Collection<Level> riskLevels = levels.get("RiskLevel");
		
		//Get all misbehaviours for model (don't include causse and effects data)
		Map<String, MisbehaviourSet> misbehaviourSets = this.getMisbehavioursForModel(model, false);
		//logger.debug("getModelRiskVector: located {} misbehaviours", misbehaviourSets.keySet().size());
		
		return getModelRiskVector(model, riskLevels, misbehaviourSets);
	}
	
	public RiskVector getModelRiskVector(Model model, Collection<Level> riskLevels, Map<String, MisbehaviourSet> misbehaviourSets) {
		if (! model.riskLevelsValid()) {
			logger.warn("Risk levels not valid: returning null risk vector");
			return null;
		}
		
		//Initialise counts for each risk level URI
		Map<String, Integer> riskLevelCounts = new HashMap<>();
		for (Level level : riskLevels) {
			riskLevelCounts.put(level.getUri(), 0);
		}
	
		//Loop though misbehaviours, get its risk level and increment the risk count
		for (MisbehaviourSet ms : misbehaviourSets.values()) {
			//Only include "visible" misbehaviours
			if (ms.isVisible()) {
				Level riskLevel = ms.getRiskLevel();
				//N.B. If model has been validated but no risk calculation run then riskLevels will not be set
				if (null != riskLevel) {
					Integer riskLevelCount = riskLevelCounts.get(riskLevel.getUri());
					int count = riskLevelCount;
					count++;
					riskLevelCounts.put(riskLevel.getUri(), count);
				}
			}
		}
		
		//Create riskVector from riskLevels and riskLevelCounts
		RiskVector riskVector = new RiskVector(riskLevels, riskLevelCounts);
		
		return riskVector;
	}

	/**
	 * Get all twas from the store
	 *
	 * @param model the model for which to get the twas
	 * @return a map of twas
	 */
	public Map<String, TrustworthinessAttributeSet> getTWASForModel(Model model) {

		Map<String, TrustworthinessAttributeSet> twas = model.getQuerier().getTrustworthinessAttributeSets(storeManager.getStore());
		logger.info("Returning {} TWAS for model <{}>", twas.size(), model.getUri());
		return twas;
	}

	/**
	 * Get all patterns from the store
	 *
	 * @param model the model for which to get the patterns
	 * @return a map of patterns by uri
	 */
	public Map<String, Pattern> getPatternsForModel(Model model) {

		Map<String, Pattern> patterns = model.getQuerier().getSystemPatterns(storeManager.getStore());
		logger.info("Returning {} patterns for model <{}>", patterns.size(), model.getUri());
		return patterns;
	}

	/**
	 * Retrieves one threat from the model
	 *
	 * @param model the model for which to return the threat
	 * @param threatUri
	 * @return the threat
	 */
	public Threat getThreatForModel(Model model, String threatUri) {

		Threat threat = model.getQuerier().getSystemThreat(storeManager.getStore(), threatUri);
		this.updateCachedThreat(threat, model);
		return threat;
	}

	/**
	 * Retrieves all threats from the model
	 *
	 * @param model the model for which to return the threats
	 * @param refreshCache
	 * @return a set of threats
	 */
	public Set<Threat> getThreatsForModel(Model model, boolean refreshCache) {

		return getThreatsForModel(model, refreshCache, getPatternsForModel(model), getMisbehavioursForModel(model, false),
				getControlStrategiesForModel(model, getControlSetsForModel(model)), getTWASForModel(model));
	}

	public Set<Threat> getThreatsForModel(Model model, boolean refreshCache,
			Map<String, Pattern> patterns,
			Map<String, MisbehaviourSet> misbehaviourSets,
			Map<String, ControlStrategy> controlStrategies,
			Map<String, TrustworthinessAttributeSet> twas) {

		Set<Threat> threats;

		Set<Threat> cachedThreats = this.modelThreats.get(model.getId());

		logger.debug("refreshCache = {}", refreshCache);
		logger.debug("cachedThreats = {}", cachedThreats == null ? "null" : cachedThreats.size());

		if (refreshCache || (cachedThreats == null)) {
			threats = new HashSet<>();
			logger.debug("Calling getSystemThreats");
			threats.addAll(model.getQuerier().getSystemThreats(storeManager.getStore(),
					patterns, misbehaviourSets, controlStrategies, twas).values());
		} else {
			logger.debug("Using cached threats");
			threats = cachedThreats;
		}

		if (refreshCache || setAcceptedThreatsAsResolved) {
			logger.debug("Generating threat IDs/URIs map for model <{}>", model.getUri());
			cachedThreats = new HashSet<>();

			for (Threat threat : threats) {
				if (setAcceptedThreatsAsResolved && threat.getAcceptanceJustification() != null) {
					logger.warn("Setting accepted threat {} as resolved", threat.getLabel());
					threat.isResolved();
				}
				cachedThreats.add(threat);
			}

			this.modelThreats.put(model.getId(), cachedThreats);
		}

		logger.info("Returning {} threat(s) for model <{}>", threats.size(), model.getUri());
		return threats;
	}

	/**
	 * Retrieves all updated threats from the model (for example after control set has been changed)
	 *
	 * @param model the model for which to return the updated threats
	 * @return a set of threats
	 */
	/* KEM: does not appear to be used
	public Set<Threat> getUpdatedThreatsForModel(Model model) {

		Set<Threat> threats;
		Set<Threat> cachedThreats = this.modelThreats.get(model.getId());

		if (cachedThreats == null) {
			threats = new HashSet<>();
			threats.addAll(model.getQuerier().getSystemThreats(storeManager.getStore()).values());
			// no need to call getUpdatedThreatStatus here, as all updated information should be available
		} else {
			threats = cachedThreats; // load cached threats
			Map<String, Threat> threatMap = new HashMap<>();
			threats.forEach(t -> threatMap.put(t.getUri(), t));
			// get updated threats (including updated controls and threat status)
			threats.addAll(model.getQuerier().getUpdatedThreatStatus(storeManager.getStore(), threatMap).values());
		}

		logger.debug("Generating threat IDs/URIs map for model <{}>", model.getUri());
		cachedThreats = new HashSet<>();

		for (Threat threat : threats) {
			if (setAcceptedThreatsAsResolved && threat.getAcceptanceJustification() != null) {
				logger.warn("Setting accepted threat {} as resolved", threat.getLabel());
				threat.isResolved();
			}
			cachedThreats.add(threat);
		}

		this.modelThreats.put(model.getId(), cachedThreats);

		logger.info("Returning {} threat(s) for model <{}>", threats.size(), model.getUri());
		return threats;
	}
	*/

	public Set<Threat> getThreatsForModel(Model model) {
		return getThreatsForModel(model, false); //don't refresh cache by default
	}

	/**
	 * Retrieves all threats from the model that threaten a particular asset
	 *
	 * @param model the model for which to return the threats
	 * @param assetURI the URI of the asset
	 * @return a set of threats
	 */
	public Set<Threat> getThreatsForAsset(Model model, String assetURI) {
		Map<String, Threat> threats = model.getQuerier().getSystemThreats(storeManager.getStore(), assetURI);
		logger.info("Returning {} threat(s) for asset <{}> model <{}>", threats.size(), assetURI, model.getUri());
		Set<Threat> t = new HashSet<>();
		t.addAll(threats.values());
		return t;
	}

	public Set<ComplianceSet> getComplianceSetsForModel(Model model) {
		logger.info("Getting compliance sets");
		Map<String, ComplianceSet> complianceSets = model.getQuerier().getComplianceSets(storeManager.getStore());
		Set<ComplianceSet> cs = new HashSet<>();
		cs.addAll(complianceSets.values());
		return cs;
	}

	public Set<ComplianceSet> getComplianceSetsForModel(Model model, Map<String, ComplianceThreat> threats) {
		logger.info("Getting compliance sets");
		Map<String, ComplianceSet> complianceSets = model.getQuerier().getComplianceSets(storeManager.getStore(), threats);
		Set<ComplianceSet> cs = new HashSet<>();
		cs.addAll(complianceSets.values());
		return cs;
	}

	public Map<String, ComplianceThreat> getComplianceThreatsForModel(Model model) {
		logger.info("Getting compliance threats");
		Map<String, ComplianceThreat> complianceThreats = model.getQuerier().getComplianceThreats(storeManager.getStore());
		return complianceThreats;
	}

	public Map<String, ComplianceThreat> getComplianceThreatsForModel(Model model, Map<String, Pattern> patterns, Map<String, ControlStrategy> controlStrategies) {
		logger.info("Getting compliance threats");
		Map<String, ComplianceThreat> complianceThreats = model.getQuerier().getComplianceThreats(storeManager.getStore(), patterns, controlStrategies);
		return complianceThreats;
	}

	/**
	 * Returns relation for a model.
	 *
	 * @param model
	 * @param id
	 * @return
	 */
	public Relation getRelationForModel(Model model, String id) {
		if (id.contains("-")){
			// from, type and to info has been sent through in the id -- we do not need to calculate it
			String[] components = id.split("-");
			logger.info("Returning relation {} - {} - {}", components[0], components[1], components[2]);
			return model.getQuerier().getRelationFromCC(storeManager.getStore(), components[0], components[1], components[2]);
		}

		// from, type and to info is not included in id
		Relation rel = model.getQuerier().getSystemRelationById(storeManager.getStore(), id);
		if(rel == null) {
			logger.warn("could not find relation with id: {} - returning null", id);
			return null;
		}
		logger.info("Returning relation {} for model <{}>", rel, model.getUri());
		return model.getQuerier().getSystemRelation(storeManager.getStore(), rel.getFrom(), rel.getType(), rel.getTo());
	}

	/**
	 * Returns IDs of inferred and asserted relations to and from an asset.
	 *
	 * @param model
	 * @param asset
	 * @return Set of relation IDs
	 */
	public Set<String> getRelationIDsForAsset(Model model, Asset asset) {

		Set<String> relationIDs = model.getQuerier().getAssetRelations(storeManager.getStore(), asset.getUri());
		logger.info("Returning {} relation(s) for asset <{}>", relationIDs.size(), asset.getUri());
		return relationIDs;
	}

	/**
	 * Returns relations for a model.
	 *
	 * @param model
	 * @return
	 */
	public Set<Relation> getRelationsForModel(Model model) {

		Set<Relation> relations = model.getQuerier().getSystemRelations(storeManager.getStore());
		logger.info("Returning {} relation(s) for model <{}>", relations.size(), model.getUri());
		return relations;
	}

	/**
	 * Returns relation ids for a model.
	 *
	 * @param model
	 * @return
	 */
	public Set<Relation> getRelationIDsForModel(Model model) {

		Set<Relation> relations = model.getQuerier().getSystemRelationIDs(storeManager.getStore());
		logger.info("Returning {} relation ids for model <{}>", relations.size(), model.getUri());
		return relations;
	}

	/**
	 * Get all control sets from the store
	 *
	 * @param model the model for which to get the control sets
	 * @return a map of control sets
	 */
	public Map<String, ControlSet> getControlSetsForModel(Model model) {

		Map<String, ControlSet> cs = model.getQuerier().getControlSets(storeManager.getStore());
		logger.info("Returning {} control sets(s) for model <{}>", cs.size(), model.getUri());
		return cs;
	}

	public ControlSet getControlSet(String uri, Model model) {

		logger.info("Getting control set {}", uri);
		ControlSet cs = model.getQuerier().getControlSet(storeManager.getStore(), uri);
		logger.info("Returning {} control set for model <{}>", cs, model.getUri());
		return cs;
	}

	public Map<String, ControlStrategy> getControlStrategiesForModel(Model model, Map<String, ControlSet> controlSets) {
		return model.getQuerier().getControlStrategies(storeManager.getStore(), controlSets);
	}

	// Palette Generation ///////////////////////////////////////////////////////////////////////////////////////////////////#
	
	/**
	* Gets the assets required to build the palette for a domain model
	* @param domainURI the uri of the domain model
	* @return assets required to build the palette for a domain model
	*/
	public List<Map<String, String>> getPaletteAssets(String domainURI) {


	String query = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n" +
			"PREFIX core:<http://it-innovation.soton.ac.uk/ontologies/trustworthiness/core#>\n" +
			"SELECT DISTINCT ?asset ?al (GROUP_CONCAT(?c;separator=\",\") AS ?type) ?category ?cl ?description ?a (STR(?minCardinality) AS ?min) (STR(?maxCardinality) AS ?max)\n" +
			"WHERE {\n" +
			//"  GRAPH <http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain/> {\n" +
			"	?c rdfs:subClassOf+ core:Asset .\n" +
			"	?asset rdfs:subClassOf* ?c .\n" +
			"	?asset rdfs:label ?al .\n" +
			"	?asset rdfs:subClassOf* ?category .\n" +
			"	?category rdfs:subClassOf core:Asset .\n" +
			"	?category rdfs:label ?cl .\n" +
			"	OPTIONAL { ?asset rdfs:comment ?description }\n" +
			"	OPTIONAL { ?asset core:isAssertable ?assertable }\n" +
			"	OPTIONAL { ?asset core:minCardinality ?minCardinality }\n" +
			"	OPTIONAL { ?asset core:maxCardinality ?maxCardinality }\n" +
			"	BIND(IF(BOUND(?assertable),STR(?assertable),\"false\") AS ?a)\n" +
			//"  }\n" +
			"  FILTER(!isBlank(?asset))\n" +
			"  FILTER(!isBlank(?category))\n" +
			"} GROUP BY ?asset ?al ?category ?cl ?description ?a ?minCardinality ?maxCardinality ORDER BY ?type ?asset";
	
	AStoreWrapper store = storeManager.getStore();
		
	return store.translateSelectResult(store.querySelect(query, domainURI));

	}
	
	/**
	 * Gets the relation required to build the palette for a domain model
	 * @param domainURI the uri of the domain model
	 * @return relation required to build the palette for a domain model
	 */
	public List<Map<String, String>> getPaletteRelations(String domainURI) {

	
	String query = "#Retrieve all relations and their domain and range\n" +
			"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n" +
			"PREFIX core:<http://it-innovation.soton.ac.uk/ontologies/trustworthiness/core#>\n" +
			"PREFIX owl:<http://www.w3.org/2002/07/owl#>\n" +
			"SELECT DISTINCT ?from ?prop ?label ?comment ?to (STR(?hidden) AS ?isHidden) WHERE {\n" +
			//"GRAPH <http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain/> {\n" +
			"	?prop a owl:ObjectProperty .\n" +
			"	?prop rdfs:domain ?domain .\n" +
			"	?prop rdfs:range ?range .\n" +
			"	?prop rdfs:label ?label .\n" +
			"	?prop rdfs:comment ?comment .\n" +
			"	OPTIONAL {?prop core:hidden ?hidden }\n" +
			"	{\n" +
			"		?dasset rdfs:subClassOf* core:Asset .\n" +
			"		?domain rdfs:subClassOf* ?dasset .\n" +
			"		?from rdfs:subClassOf* ?domain .\n" +
			"	} UNION {\n" +
			"		?domain owl:unionOf ?du .\n" +
			"		?du (rdf:rest)*/rdf:first ?da .\n" +
			"		?from rdfs:subClassOf* ?da .\n" +
			"	}\n" +
			"	{\n" +
			"		?rasset rdfs:subClassOf* core:Asset .\n" +
			"		?range rdfs:subClassOf* ?rasset .\n" +
			"		?to rdfs:subClassOf* ?range .\n" +
			"	} UNION {\n" +
			"		?range owl:unionOf ?ru .\n" +
			"		?ru (rdf:rest)*/rdf:first ?ra .\n" +
			"		?to rdfs:subClassOf* ?ra .\n" +
			"	}\n" +
			//"  }\n" +
			"} ORDER BY ?prop ?from ?to";
	
	AStoreWrapper store = storeManager.getStore();

	return store.translateSelectResult(store.querySelect(query, domainURI));
	}

	 // User Management ////////////////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * Sets the default domain models that new users should have access to
	 * @param defaultDomainModels the list of domain models
	 */
	public void setDefaultUserDomainModels(List<String> defaultDomainModels) {
		//TODO: store this in TDB instead of defaultUserOntologies field
		logger.info("setDefaultUserDomainModels: {}", defaultDomainModels);
		this.defaultUserDomainModels = defaultDomainModels;
	}
	
	/**
	 * Gets the default domain models that new users should have access to
	 * @return the default list of domain models for new users
	 */
	public List<String> getDefaultUserDomainModels() {
		//TODO: get this from TDB instead of defaultUserOntologies field
		return this.defaultUserDomainModels;
	}
	
	/**
	 * Adds user access to the default domain models
	 */
	public void addAccessToDefaultDomainModels(List<String> userNames) {
		// get default list of domain models
		List<String> defaultDomainModels = this.defaultUserDomainModels; //TODO: get from TDB
		logger.info("defaultDomainModels: {}", defaultDomainModels);
		
		if (defaultDomainModels == null) {
			logger.warn("No default user demain models defined");
			return;
		}
		
		logger.info("Setting default domain models for: " + userNames);
		for (String domainTitle : defaultDomainModels) {
			List<String> users = this.getUsersForDomainModel(domainTitle);
			users.addAll(userNames);
			this.setUsersForDomainModel(domainTitle, users);
			logger.info(domainTitle + ": " + users);
		}
	}
	
	/**
	 * Gets the list of domain models that a user has access to
	 * @param userName the username of the user
	 * @return the list of domain models
	 */
	public Map<String, Map<String, Object>> getDomainModelsForUser(String userName) {
	
		String query = TemplateLoader.formatTemplate(queries.get("GetDomainModelsForUser"), storeManager.getManagementGraph(), userName);

		AStoreWrapper store = storeManager.getStore();
		List<Map<String, String>> result = store.translateSelectResult(store.querySelect(query));

		Map<String, Map<String, Object>> response = new HashMap<>();
			result.forEach(solution -> {
				Map<String, Object> domain = new HashMap<>();
				domain.put("label", solution.get("label"));
				domain.put("title", solution.get("title"));
				domain.put("comment", solution.get("comment"));
				domain.put("version", solution.get("version"));
				domain.put("loaded", true);
				response.put(solution.get("g"), domain);
			});

		return response;
	}
	
	/**
	 * Gets a list of all users who can access a specified domain model
	 * @param domainTitle the specified domain model (name not URI)
	 * @return all users who can access the domain model
	 */
	public List<String> getUsersForDomainModel(String domainTitle) {
		   	
		String query = queries.get("GetUsersForDomainModel");
		AStoreWrapper store = storeManager.getStore();
		List<Map<String, String>> result = store.translateSelectResult(store.querySelect(TemplateLoader.formatTemplate(query, domainTitle), storeManager.getManagementGraph()));
		List<String> response = new ArrayList<>();
		result.forEach((t) -> {
			response.add(t.get("user"));
		});

		return response;
	}
	
	/**
	 * Sets the list of users who can access a domain model
	 * @param domainTitle the name of the domain model
	 * @param users the list of users to be assigned access to the domain model
	 * @return true if successful
	 */
	public boolean setUsersForDomainModel(String domainTitle, List<String> users) {
	
		AStoreWrapper store = storeManager.getStore();

		String deleteQuery = TemplateLoader.formatTemplate(queries.get("DeleteUsersForDomainModel"),
			storeManager.getManagementGraph(), storeManager.getManagementGraph(), SparqlHelper.escapeLiteral(domainTitle));

		store.update(deleteQuery);

		StringBuilder sb = new StringBuilder();

		users.forEach((user) -> {
			sb.append("		<" + storeManager.getManagementGraph() + "#" + SparqlHelper.escapeLiteral(user) + "> acl:accessTo ?domain .\n");
		});


		String insertQuery = TemplateLoader.formatTemplate(queries.get("InsertUsersForDomainModel"),
			storeManager.getManagementGraph(), sb.toString(), storeManager.getManagementGraph(),  SparqlHelper.escapeLiteral(domainTitle));

		store.update(insertQuery);

		return true;
	}
	
	/**
	 * Asks the store of a user can access a domain model
	 * @param domainURI the uri of the domain model
	 * @param user the username of the user
	 * @return true/false if they are able to access the domain model
	 */
	public boolean canUserAccessDomain(String domainURI, String user){
		// If this is the user's first login we need to add them to
		// the management graph and set their default domain model
		// access.
		syncUsers();

		AStoreWrapper store = storeManager.getStore();
		return store.queryAsk(TemplateLoader.formatTemplate(queries.get("CanUserAccessDomain"), storeManager.getManagementGraph(), user, domainURI));
	}
	
	/**
	 * Adds a new users to the management graph in the store
	 * @param label username of the user (used to create URI)
	 * @param email email of the user
	 * @return true if successful
	 */
	public boolean createNewUser(String label, String email) {
		logger.info("Adding user: {}", label);
		String uri = SparqlHelper.escapeURI(storeManager.getManagementGraph() + "#" + label);

		AStoreWrapper store = storeManager.getStore();
		store.update(TemplateLoader.formatTemplate(queries.get("CreateNewUser"), SparqlHelper.escapeLiteral(label),
			SparqlHelper.escapeLiteral(email), uri), storeManager.getManagementGraph());

		return true;
	}
	
	/**
	 * Gets a list of all users in the management graph from the store
	 * @return all users
	 */
	public List<String> getUsers() {
		AStoreWrapper store = storeManager.getStore();

		return store
			.translateSelectResult(store.querySelect(queries.get("GetUsers"), storeManager.getManagementGraph()))
			.stream()
			.map(it -> it.get("user"))
			.collect(Collectors.toList());
	}
	
	static private Object syncUsersLock = new Object();

	/**
	 * Synchronise the users in the management graph with those in Keycloak.
	 * Keycloak is the reference and new users from Keycloak need to be added
	 * to the management graph. We also need to set their default domain
	 * model access too.
	 */
	public void syncUsers() {
		// Probably need to synchronise all this.
		// Don't want to use the ModelObjectHelper instance as the lock though,
		// as this will introduce contention with getModelLock().
		synchronized (syncUsersLock) {
			logger.info("Syncing users with Keycloak");

			List<String> localUserNames = getUsers();
			logger.debug("Current users {}", localUserNames);

			// We only add missing users.
			// We should delete users that have been deleted from Keycloak.
			// We don't do this.
			// This is consistant with the old deleteUser() REST controller.
			// That only deleted the user from Mongo.
			// It made no attempt to remove them from the management graph.
			List<String> newUsers = new ArrayList<>();

			for (UserRepresentation user: keycloakAdminClient.getAllUsers()) {
				String userName = user.getUsername();
				String email = user.getEmail();

				if (!localUserNames.contains(userName)) {
					newUsers.add(userName);
					createNewUser(userName, email);
				}
			}

			if (!newUsers.isEmpty()) {
				addAccessToDefaultDomainModels(newUsers);
			}
		}
	}
}
