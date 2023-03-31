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
//      Created By :            Gianluca Correndo
//      Created Date :          26 Jul 2016
//      Modified By :           Ken Meacham, Stefanie Wiegand
//      Created for Project :   5g-Ensure
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.soton.itinnovation.security.model.system.Asset;
import uk.ac.soton.itinnovation.security.model.system.AssetGroup;
import uk.ac.soton.itinnovation.security.model.system.ComplianceSet;
import uk.ac.soton.itinnovation.security.model.system.ComplianceThreat;
import uk.ac.soton.itinnovation.security.model.system.ControlSet;
import uk.ac.soton.itinnovation.security.model.system.MetadataPair;
import uk.ac.soton.itinnovation.security.model.system.Relation;
import uk.ac.soton.itinnovation.security.model.system.TrustworthinessAttributeSet;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.model.WebKeyRole;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.AssetArrayDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.ControlsAndThreatsResponse;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.CreateAssetResponse;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.DeleteAssetResponse;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.DeleteRelationResponse;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.UpdateAsset;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.UpdateAssetCardinality;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.UpdateAssetResponse;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.UpdateAssetType;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.UpdateAssetTypeResponse;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.UpdateControlsRequest;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.UpdateControlsResponse;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.AssetInvalidException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.BadRequestErrorException;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.StoreModelManager;
import uk.ac.soton.itinnovation.security.systemmodeller.util.SecureUrlHelper;

@RestController
public class AssetController {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final int maxAssetNameLength = 50;

	@Autowired
	private StoreModelManager storeManager;

	@Autowired
	private ModelObjectsHelper modelHelper;

	@Autowired
	private SecureUrlHelper secureUrlHelper;

	/**
	 * /models/{modelId}/assets 
	 *
	 * @param modelId Model ID that can be used to access the model. Found in ModelInfo definition and any response that returns ModelInfo type.
	 * 
	 * @return a list of models' assets owned by the user
	 */
	@RequestMapping(value = "/models/{modelId}/assets", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Set<Asset>> getAssets(@PathVariable String modelId) {

		logger.info("Called REST method to GET assets for model {}", modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

		//get assets 
		Set<Asset> assets = modelHelper.getBasicAssetsForModel(model);

		//add asset metadata
		for (Asset asset : assets) {
			List<MetadataPair> assetMetadata = model.getQuerier().getMetadataOnEntity(storeManager.getStore(), asset);
			asset.setMetadata(assetMetadata);
		}

		return ResponseEntity.status(HttpStatus.OK).body(assets);
	}

	/**
	 * Add an Asset object to persist in the model. 
	 *
	 * @param modelId Webkey of the model
	 * @param asset Asset object that can be used with the request. Found in Relation definition and any response that returns Relation type.
	 * @return the Asset instance
	 */
	@RequestMapping(value = "/models/{modelId}/assets", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public ResponseEntity<CreateAssetResponse> addAssetToModel(@PathVariable String modelId, @RequestBody Asset asset) {

		logger.info("Called REST method to POST new asset for model {}", modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		// create asset
		asset.setUri(modelHelper.createNewAssetUri());

		model.invalidate();

		// save to store
		model.getUpdater().storeAsset(storeManager.getStore(), asset);
		modelHelper.addAssetToCache(asset, model);

		CreateAssetResponse response = new CreateAssetResponse(asset, model.isValid());

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Get extended information belonging to an asset, given its ID
	 *
	 * @param modelId Webkey of the model
	 * @param assetId ID of the asset
	 * @return an Asset object (as JSON) describing the asset
	 */
	@RequestMapping(value = "/models/{modelId}/assets/{assetId}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Asset> getAssetInModel(@PathVariable String modelId, @PathVariable String assetId) {

		logger.info("Called REST method to GET asset {} for model {}", assetId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

		// look up asset by ID
		logger.debug("Looking up asset with ID: {}", assetId);
		Asset asset = modelHelper.getAssetById(assetId, model, true); //get full details
		if (asset == null) {
			logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
			throw new AssetInvalidException();
		}

		// build UI asset
		return ResponseEntity.status(HttpStatus.OK).body(asset);
	}

	/**
	 * Get TWAS for a given asset
	 *
	 * @param modelId Webkey of the model
	 * @param assetId ID of the asset
	 * @return a map of the TWAS data
	 */
	@RequestMapping(value = "/models/{modelId}/assets/{assetId}/twas", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Map<String, TrustworthinessAttributeSet>> getAssetTwas(@PathVariable String modelId, @PathVariable String assetId) {

		logger.info("Called REST method to GET asset twas {} for model {}", assetId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

		// look up asset by ID
		Asset asset = modelHelper.getAssetById(assetId, model, false); //no need to get full details here
		if (asset == null) {
			logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
			throw new AssetInvalidException();
		}

		if (asset == null) {
			logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
			throw new AssetInvalidException();
		}

		Map<String, TrustworthinessAttributeSet> twas = modelHelper.getAssetTwas(asset, model);
		logger.info("Returning {} TWAS", twas.size());

		return ResponseEntity.status(HttpStatus.OK).body(twas);
	}

	/**
	 * Get control sets for a given asset
	 *
	 * @param modelId Webkey of the model
	 * @param assetId ID of the asset
	 * @return A map of the control sets
	 */
	@RequestMapping(value = "/models/{modelId}/assets/{assetId}/controlsets", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Map<String, ControlSet>> getAssetControlSets(@PathVariable String modelId, @PathVariable String assetId) {

		logger.info("Called REST method to GET asset control sets {} for model {}", assetId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

		// look up asset by ID
		Asset asset = modelHelper.getAssetById(assetId, model, false); //no need to get full details here
		if (asset == null) {
			logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
			throw new AssetInvalidException();
		}

		if (asset == null) {
			logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
			throw new AssetInvalidException();
		}

		Map<String, ControlSet> controlSets = modelHelper.getAssetControlSets(asset, model);
		logger.info("Returning {} control sets", controlSets.size());

		return ResponseEntity.status(HttpStatus.OK).body(controlSets);
	}

	/**
	 * Update location for an asset.
	 *
	 * @param modelId Webkey of the model
	 * @param assetId ID of the asset
	 * @param updatedAsset Asset (in the request body)
	 * @return status message response object
	 */
	@RequestMapping(value = "/models/{modelId}/assets/{assetId}/location", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<String> updateAssetLocation(@PathVariable String modelId, @PathVariable String assetId, @RequestBody Asset updatedAsset) {

		logger.info("Called REST method to update location for asset {}: for model {}", assetId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		// look up asset by ID
		//logger.debug("Looking up asset with ID: {}", assetId);
		Asset asset = modelHelper.getAssetById(assetId, model, false); //just do basic lookup
		if (asset == null) {
			logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
			throw new AssetInvalidException();
		}
		
		asset.setIconPosition(updatedAsset.getIconX(), updatedAsset.getIconY());
		
		// logger.debug("updateAssetLocation: Updating asset {} to location [{},{}]", asset.getID(), asset.getIconX(), asset.getIconY());
		model.getUpdater().updateAssetPosition(storeManager.getStore(), asset);
		// logger.debug("updateAssetLocation: Updated asset {} to location [{},{}]", asset.getID(), asset.getIconX(), asset.getIconY());

		//UpdateAssetResponse response = new UpdateAssetResponse(new UpdateAssetLocation(updatedAsset), model.isValid());

		//return ResponseEntity.status(HttpStatus.OK).body(response);
		return ResponseEntity.status(HttpStatus.OK).body("completed"); //no need to return any data
	}

	/**
	 * Update location for a set of assets. Used mainly by the Canvas user operations.
	 *
	 * @param modelId Webkey of the model
	 * @param assetArrayDTO AssetArrayDTO in request body
	 * @return status message response object
	 */
	@RequestMapping(value = "/models/{modelId}/assets/updateLocations", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<String> updateAssetsLocations(@PathVariable String modelId,
												   @RequestBody AssetArrayDTO assetArrayDTO) {

		logger.info("Called REST method to update multiple asset locations for model {}", modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		List<Asset> assets = assetArrayDTO.getAssets();
		model.getUpdater().updateAssetsPositions(storeManager.getStore(), assets);

		logger.debug("Updated multiple asset locations");
		return ResponseEntity.status(HttpStatus.OK).body("completed"); //no need to return any data
	}

	/**
	 * Update an asset label.
	 *
	 * @param modelId Webkey of the model
	 * @param assetId ID of the asset
	 * @param assetUpdateRequest Asset (in the request body)
	 * @return an UpdateAsset JSON object describing the updated Asset
	 */
	@RequestMapping(value = "/models/{modelId}/assets/{assetId}/label", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<UpdateAsset> updateAssetLabel(@PathVariable String modelId, @PathVariable String assetId, @RequestBody Asset assetUpdateRequest) {

		logger.info("Called REST method to PUT field for asset {} <{}> for model {}", assetId, assetUpdateRequest.getUri(), modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		// get lock for model and synchronize on this
		Object modelLock = modelHelper.getModelLock(model);
		//logger.debug("updateAssetInModel: waiting for lock on model: {}...", model.getId());
		synchronized(modelLock) {
			//logger.debug("updateAssetInModel: aquired lock on model: {}", model.getId());

			// look up asset by ID
			logger.debug("Looking up asset with ID: {}", assetId);
			Asset updatedAsset = modelHelper.getAssetById(assetId, model, false); //just do basic lookup
			if (updatedAsset == null) {
				logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
				throw new AssetInvalidException();
			}

			logger.debug("Updating asset: {}", updatedAsset.getUri());

			// update non-null and non-zero values that we don't have the same values set already
			String assetURI = updatedAsset.getUri();

			UpdateAsset updateAsset = new UpdateAsset(updatedAsset); // return in case nothing updated

			String newLabel = assetUpdateRequest.getLabel().trim();
			String newName = (newLabel.length() > maxAssetNameLength ? newLabel.substring(0, maxAssetNameLength) : newLabel);
			String assetDefaultLabel = "";
			
			if (assetURI.contains("#")) { // It really should do!
				assetDefaultLabel = assetURI.substring(assetURI.lastIndexOf("#") + 1);
			}

			if (newName.equals(assetDefaultLabel)) {
				//if this occurs, the original asset label must have been null
				logger.error("Invalid label: {}", newName);
				throw new BadRequestErrorException("Invalid asset label");
			}
			else {
				logger.debug("Will update name to {} for asset {}", newName, updatedAsset.getUri());
				updatedAsset.setLabel(newName);
				updateAsset = new UpdateAsset(updatedAsset);
				model.getUpdater().updateAssetLabel(storeManager.getStore(), updatedAsset.getUri(), newName);
				return ResponseEntity.status(HttpStatus.OK).body(updateAsset);
			}
		}
	}

	/**
	 * Update an asset type.
	 *
	 * @param modelId Webkey of the model
	 * @param assetId ID of the asset
	 * @param assetUpdateRequest Asset (in the request body)
	 * @return a JSON object describing the AssetUi with the given asset id
	 */
	@RequestMapping(value = "/models/{modelId}/assets/{assetId}/type", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<UpdateAssetResponse> updateAssetType(@PathVariable String modelId, @PathVariable String assetId, @RequestBody Asset assetUpdateRequest) {

		logger.info("Called REST method to PUT type for asset {} <{}> for model {}", assetId, assetUpdateRequest.getUri(), modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		// get lock for model and synchronize on this
		Object modelLock = modelHelper.getModelLock(model);
		synchronized(modelLock) {

			// look up asset by ID
			logger.debug("Looking up asset with ID: {}", assetId);
			Asset updatedAsset = modelHelper.getAssetById(assetId, model, false); //just do basic lookup
			if (updatedAsset == null) {
				logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
				throw new AssetInvalidException();
			}

			logger.debug("Updating asset: {}", updatedAsset.getUri());

			// update non-null and non-zero values that we don't have the same values set already
			String assetURI = updatedAsset.getUri();

			UpdateAsset updateAsset = new UpdateAsset(updatedAsset); // return in case nothing updated

			String newType = assetUpdateRequest.getType();
			if (newType != null) {
				logger.debug("Will update type to {} for asset {}", newType, updatedAsset.getUri());
				updatedAsset.setType(newType);
				updateAsset = new UpdateAssetType(updatedAsset);
			}
				
			Set<Relation> relations = new HashSet<>();
			Set<String> deletedRelations = new HashSet<>();

			//save current relations, prior to updating asset type
			
			logger.debug("updateAssetInModel: getting current relation ids");
			relations = modelHelper.getRelationIDsForModel(model);
			logger.debug("updateAssetInModel: got current relation ids");
			
			logger.debug("updateAssetInModel: updating asset type to {}", updatedAsset.getType());
			model.getUpdater().updateAssetType(storeManager.getStore(), updatedAsset.getUri(), updatedAsset.getType());
			logger.debug("updateAssetInModel: asset updated");

			model.invalidate();

			UpdateAssetResponse response;

			logger.debug("updateAssetInModel: determining deleted relations");

			//TODO: this needs to come from the request above - for a quick test delete all relations
			relations.removeAll(modelHelper.getRelationIDsForModel(model));
			relations.stream().forEachOrdered(relation -> deletedRelations.add(relation.getID()));
			//logger.debug("Deleted relations:");
			//deletedRelations.forEach(relation -> logger.debug(relation));

			response = new UpdateAssetTypeResponse(updateAsset, deletedRelations, model.isValid());
			logger.debug("updateAssetInModel: done");

			logger.debug("updateAssetInModel: releasing lock on model: {}", model.getId());
			return ResponseEntity.status(HttpStatus.OK).body(response);
		}
	}

	/**
	 * Update an asset cardinality constraint (N.B. should not longer be required, as replaced by population)
	 *
	 * @param modelId Webkey of the model
	 * @param assetId ID of the asset
	 * @param assetUpdateRequest Asset (in the request body)
	 * @return a JSON object describing the AssetUi with the given asset id
	 */
	@RequestMapping(value = "/models/{modelId}/assets/{assetId}/cardinality", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<UpdateAssetResponse> updateAssetInModel(@PathVariable String modelId, @PathVariable String assetId, @RequestBody Asset assetUpdateRequest) {

		logger.info("Called REST method to PUT cardinality for asset {} <{}> for model {}", assetId, assetUpdateRequest.getUri(), modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		// get lock for model and synchronize on this
		Object modelLock = modelHelper.getModelLock(model);
		//logger.debug("updateAssetInModel: waiting for lock on model: {}...", model.getId());
		synchronized(modelLock) {
			//logger.debug("updateAssetInModel: aquired lock on model: {}", model.getId());

			// look up asset by ID
			logger.debug("Looking up asset with ID: {}", assetId);
			Asset updatedAsset = modelHelper.getAssetById(assetId, model, false); //just do basic lookup
			if (updatedAsset == null) {
				logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
				throw new AssetInvalidException();
			}

			logger.debug("Updating asset: {}", updatedAsset.getUri());

			// update non-null and non-zero values that we don't have the same values set already
			String assetURI = updatedAsset.getUri();

			UpdateAsset updateAsset = new UpdateAsset(updatedAsset); // return in case nothing updated

			int newMinCardinality = assetUpdateRequest.getMinCardinality();
			int newMaxCardinality = assetUpdateRequest.getMaxCardinality();

			if (newMinCardinality <= -2 || newMaxCardinality <= -2) {
				throw new BadRequestErrorException("Minimum or maximum cardinality negative");
			}

			if (newMaxCardinality < newMinCardinality) {
				throw new BadRequestErrorException("Minimum cardinality larger than maximum");
			}

			logger.debug("Updating minCardinality to {} for asset <{}>", newMinCardinality, updatedAsset.getUri());
			updatedAsset.setMinCardinality(newMinCardinality);

			logger.debug("Updating maxCardinality to {} for asset <{}>", newMaxCardinality, updatedAsset.getUri());
			updatedAsset.setMaxCardinality(newMaxCardinality);

			updateAsset = new UpdateAssetCardinality(updatedAsset);
			model.getUpdater().updateAssetCardinality(storeManager.getStore(), updatedAsset.getUri(), updatedAsset.getMinCardinality(), updatedAsset.getMaxCardinality());
			
			logger.debug("invalidating model");
			model.invalidate();
			
			UpdateAssetResponse response = new UpdateAssetResponse(updateAsset, model.isValid());
			return ResponseEntity.status(HttpStatus.OK).body(response);
		}
	}

	/**
	 * Update an asset population level.
	 *
	 * @param modelId Webkey of the model
	 * @param assetId ID of the asset
	 * @param assetUpdateRequest Asset (in the request body)
	 * @return status message response object
	 */
	@RequestMapping(value = "/models/{modelId}/assets/{assetId}/population", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<String> updateAssetPopulation(@PathVariable String modelId, @PathVariable String assetId, @RequestBody Asset assetUpdateRequest) {

		logger.info("Called REST method to PUT population {} for asset {} <{}> for model {}", assetUpdateRequest.getPopulation(), assetId, assetUpdateRequest.getUri(), modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		// get lock for model and synchronize on this
		Object modelLock = modelHelper.getModelLock(model);
		synchronized(modelLock) {
			// look up asset by ID
			logger.debug("Looking up asset with ID: {}", assetId);
			Asset updatedAsset = modelHelper.getAssetById(assetId, model, false); //just do basic lookup
			if (updatedAsset == null) {
				logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
				throw new AssetInvalidException();
			}

			logger.debug("Updating asset: {}", updatedAsset.getUri());

			updatedAsset.setPopulation(assetUpdateRequest.getPopulation());

			logger.debug("population: {}", updatedAsset.getPopulation());

			model.getUpdater().updateAssetPopulation(storeManager.getStore(), updatedAsset.getUri(), updatedAsset.getPopulation());
			
			logger.debug("invalidating model");
			model.invalidate();
			
			return ResponseEntity.status(HttpStatus.OK).body("completed"); //no need to return any data
		}
	}

	/**
	 * Delete an asset from the model. 
	 *
	 * @param modelId Webkey of the model
	 * @param assetId ID of the asset
	 * @return a JSON object describing the Asset with the given asset ID
	 */
	@RequestMapping(value = "/models/{modelId}/assets/{assetId}", method = RequestMethod.DELETE)
	@ResponseBody
	public ResponseEntity<DeleteAssetResponse> deleteAssetInModel(@PathVariable String modelId, @PathVariable String assetId) {

		logger.info("Called REST method to DELETE asset {} from model {}", assetId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		// get lock for model and synchronize on this
		Object modelLock = modelHelper.getModelLock(model);
		logger.debug("deleteAssetInModel: waiting for lock on model: {}...", model.getId());
		synchronized(modelLock) {
			logger.debug("deleteAssetInModel: aquired lock on model: {}", model.getId());

			// look up asset by ID
			logger.debug("Looking up asset with ID: {}", assetId);
			Asset asset = modelHelper.getAssetById(assetId, model, false); // just look up ID and URI
			if (asset == null) {
				logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
				throw new AssetInvalidException();
			}

			AssetGroup assetGroup = model.getQuerier().getAssetGroupOfAsset(storeManager.getStore(), asset);

    		// Get relations to be deleted when the asset is deleted
			Set<String> relationIDs = modelHelper.getRelationIDsForAsset(model, asset);

			model.getUpdater().deleteAsset(storeManager.getStore(), asset);
			modelHelper.deleteAssetFromCache(asset, model);

			//invalidate the model
			model.invalidate();

			DeleteAssetResponse response = new DeleteAssetResponse();

			// add asset to list of asset IDs deleted
			Set<String> assetIDs = new HashSet<>();
			assetIDs.add(asset.getID());
			response.addAssets(assetIDs);
			response.addRelations(relationIDs);
			response.setValid(model.isValid());

			if (assetGroup != null) {
				response.setAssetGroup(assetGroup.getID());
			}

			logger.debug("deleteAssetInModel: releasing lock on model: {}", model.getId());
			return ResponseEntity.status(HttpStatus.OK).body(response);
		}
	}

	/**
	 * Get all controls and threats for a single asset. Can be used for more compact model information retrieval.
	 *
	 * @param modelId Webkey of the model
	 * @param assetId ID of the asset
	 * @return a JSON object describing the controls and threats for a given asset
	 */
	@RequestMapping(value = "/models/{modelId}/assets/{assetId}/controls_and_threats", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<ControlsAndThreatsResponse> getControlsAndThreatsForAsset(@PathVariable String modelId, @PathVariable String assetId) {

		logger.info("Called REST method to GET controls and threats for asset <{}> in model {}", assetId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

		// look up asset by ID
		logger.debug("Looking up asset with ID: {}", assetId);
		Asset asset = modelHelper.getAssetById(assetId, model, true); //get full details
		if (asset == null) {
			logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
			throw new AssetInvalidException();
		}

		ControlsAndThreatsResponse controlsAndThreats = new ControlsAndThreatsResponse();

		//Get controls for asset
		Set<ControlSet> css = new HashSet<>();
		css.addAll(asset.getControlSets().values());
		controlsAndThreats.setControlSets(css);

		//Get threats for asset
		controlsAndThreats.setThreats(modelHelper.getThreatsForAsset(model, asset.getUri()));

		//Get all controls for model
		controlsAndThreats.setControlSets(modelHelper.getControlSetsForModel(model).values());

		return ResponseEntity.status(HttpStatus.OK).body(controlsAndThreats);
	}

	/**
	 * Update a control for an asset. 
	 * For population support, this automatically updates the associated min/max control sets
	 * @param modelId Webkey of the model
	 * @param assetId ID of the asset
	 * @param updatedControl ControlSet in the request body
	 * @return updated control URIs
	 */
	@RequestMapping(value = "/models/{modelId}/assets/{assetId}/control", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<UpdateControlsResponse> updateControlForAsset(@PathVariable String modelId, @PathVariable String assetId, @RequestBody ControlSet updatedControl) {

		logger.info("Called REST method to PUT control {} on asset {} for model {}", updatedControl, assetId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		// look up asset by ID
		logger.debug("Looking up asset with ID: {}", assetId);
		Asset asset = modelHelper.getAssetById(assetId, model, false); //no need to get full details here
		if (asset == null) {
			logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
			throw new AssetInvalidException();
		}

		//do it
		logger.info("Updating control {} for asset {}", updatedControl, asset);
		Set<String> updatedControls = model.getUpdater().updateControlSet(storeManager.getStore(), updatedControl);

		//set a flag to show the user they have to re-run the risk level calculation
		model.invalidateRiskLevels();

		UpdateControlsResponse response = new UpdateControlsResponse();
		response.setControls(updatedControls);
		response.setProposed(updatedControl.isProposed());
		response.setWorkInProgress(updatedControl.isWorkInProgress());
		response.setCoverageLevel(updatedControl.getCoverageLevel());
				
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/**
	 * Revert coverage level for control set on an asset.
	 *
	 * @param modelId Webkey of the model
	 * @param assetId ID of the asset
	 * @param controlSet ControlSet in the request body
	 * @return updated ControlSet
	 */
	@RequestMapping(value = "/models/{modelId}/assets/{assetId}/revert-control-coverage", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<ControlSet> revertCoverageForControl(@PathVariable String modelId, @PathVariable String assetId, @RequestBody ControlSet controlSet) {

		logger.info("Called REST method to revert coverage for control {} on asset {} for model {}", controlSet, assetId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		// look up asset by ID
		logger.debug("Looking up asset with ID: {}", assetId);
		Asset asset = modelHelper.getAssetById(assetId, model, false); //no need to get full details here
		if (asset == null) {
			logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
			throw new AssetInvalidException();
		}

		//do it
		logger.info("Deleting coverage assertion for control {} for asset {}", controlSet, asset);
		model.getUpdater().deleteCoverageForControlSet(storeManager.getStore(), controlSet);

		logger.info("Getting reverted control set: {}", controlSet.getUri());
		ControlSet revertedCS = model.getQuerier().getControlSet(storeManager.getStore(), controlSet.getUri());

		//set a flag to show the user they have to re-run the risk level calculation
		model.invalidateRiskLevels();
				
		return ResponseEntity.status(HttpStatus.OK).body(revertedCS);
	}

	/**
	 * Update multiple controls for assets. Used in the control strategy operations and Control Explorer.
	 *
	 * @param modelId Webkey of the model
	 * @param updateControlsRequest UpdateControlsRequest object in the request body
	 * @return status message response object
	 */
	@RequestMapping(value = "/models/{modelId}/assets/controls", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<UpdateControlsResponse> updateControls(@PathVariable String modelId, @RequestBody UpdateControlsRequest updateControlsRequest) {

		boolean proposed = updateControlsRequest.isProposed();
		boolean workInProgress = updateControlsRequest.isWorkInProgress();
		logger.info("Called REST method to PUT controls proposed: {} workInProgress: {} for model {}", proposed, workInProgress, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		//do it
		logger.info("Updating multiple controls");
		Set<String> updatedControls = model.getUpdater().updateControlSets(storeManager.getStore(), updateControlsRequest.getControls(), proposed, workInProgress);

		//set a flag to show the user they have to re-run the risk level calculation
		model.invalidateRiskLevels();
		
		UpdateControlsResponse response = new UpdateControlsResponse();
		response.setControls(updatedControls);
		response.setProposed(updateControlsRequest.isProposed());
		response.setWorkInProgress(updateControlsRequest.isWorkInProgress());
		response.setCoverageLevel(null); //we don't updated multiple coverage levels here
				
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	/**
	 * Update Trustworthiness Assignment for an asset. Used mainly in the Trustworthiness assignment operations.
	 *
	 * @param modelId Webkey of the model
	 * @param assetId ID of the asset
	 * @param updatedTWAS Trustworthiness Attribute Set object in the request body
	 * @return updated TWAS
	 */
	@RequestMapping(value = "/models/{modelId}/assets/{assetId}/twas", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<String> updateTwasForAsset(@PathVariable String modelId, @PathVariable String assetId, @RequestBody TrustworthinessAttributeSet updatedTWAS) {

		logger.info("Called REST method to PUT TWAS {} on asset {} for model {}", updatedTWAS, assetId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		// look up asset by ID
		logger.debug("Looking up asset with ID: {}", assetId);
		Asset asset = modelHelper.getAssetById(assetId, model, false); //no need to get full details here
		if (asset == null) {
			logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
			throw new AssetInvalidException();
		}

		//update the TWAS
		model.getUpdater().updateTWAS(storeManager.getStore(), updatedTWAS);

		//set a flag to show the user they have to re-run the risk level calculation
		model.invalidateRiskLevels();

		return new ResponseEntity<>("completed", HttpStatus.OK);
	}

	/**
	 * Revert asserted TWAS for an asset
	 *
	 * @param modelId Webkey of the model
	 * @param assetId ID of the asset
	 * @param twas Trustworthiness Attribute Set object in the request body
	 * @return reverted TWAS
	 */
	@RequestMapping(value = "/models/{modelId}/assets/{assetId}/revert-twas", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<TrustworthinessAttributeSet> revertTwasForAsset(@PathVariable String modelId, @PathVariable String assetId, @RequestBody TrustworthinessAttributeSet twas) {

		logger.info("Called REST method to revert TWAS {} on asset {} for model {}", twas, assetId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		// look up asset by ID
		logger.debug("Looking up asset with ID: {}", assetId);
		Asset asset = modelHelper.getAssetById(assetId, model, false); //no need to get full details here
		if (asset == null) {
			logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
			throw new AssetInvalidException();
		}

		//do it
		logger.info("Deleting asserted TW level for twas {} for asset {}", twas, asset);
		model.getUpdater().deleteAssertedTwLevel(storeManager.getStore(), twas);

		logger.info("Getting reverted TWAS: {}", twas.getUri());
		TrustworthinessAttributeSet revertedTWAS = model.getQuerier().getTrustworthinessAttributeSet(storeManager.getStore(), twas.getUri());

		//set a flag to show the user they have to re-run the risk level calculation
		model.invalidateRiskLevels();

		return ResponseEntity.status(HttpStatus.OK).body(revertedTWAS);
	}

	/**
	 * Get all metadata pairs associated with a single asset in a model.
	 *
	 * @param modelId Webkey of the model
	 * @param assetId ID of the asset
	 * @return Request response with a body containing a list of metadata pairs
	 */
	@RequestMapping(value = "/models/{modelId}/assets/{assetId}/meta", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<List<MetadataPair>> getMetadataOnAsset(@PathVariable String modelId, @PathVariable String assetId) {
		logger.info("Called REST method to get metadata pairs on asset {} for model {}", assetId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

		// look up asset by ID
		logger.debug("Looking up asset with ID: {}", assetId);
		Asset asset = modelHelper.getAssetById(assetId, model, false); //no need to get full details here
		if (asset == null) {
			logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
			throw new AssetInvalidException();
		}

		List<MetadataPair> assetMetadata = model.getQuerier().getMetadataOnEntity(storeManager.getStore(), asset);
		return new ResponseEntity<>(assetMetadata, HttpStatus.OK);
	}

	/**
	 * Query assets by their metadata. The query is constructed from a list of metadata pairs.
	 *
	 * @param modelId Webkey of the model
	 * @param metadataJson A JSON array, the request body, specifying a list of metadata pairs (e.g. "[{"key":"k1", "value":"v1"}]")
	 * @return Request response with a body containing a list of assets
	 */
	@RequestMapping(value = "/models/{modelId}/assets/meta", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Set<Asset>> getAssetsByMetadata(@PathVariable String modelId, @RequestParam String metadataJson) {

		// NOTE: Unfortunately, we couldn't use the request body to specify the metadata pairs for the query as it is
		// considered bad practice to include a request body in a GET request, and therefore wasn't supported by openAPI
		// (at the time). As an alternative, we decided to pass the metadata pairs as JSON into a request parameter. I
		// couldn't get this to automatically map to a POJO as can be done for request body, so the parameter is a String
		// which is deserialized 'manually' using Jackson

		logger.info("Called REST method to get assets with metadata {} in model {}", metadataJson.toString(),  modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

		List<MetadataPair> queryMetadata;

		try {
			queryMetadata = new ObjectMapper().readValue(metadataJson, new TypeReference<List<MetadataPair>>() {});
		} catch (IOException ex) {
			logger.error("Error parsing JSON passed in query parameter for model [{}] {}", model.getId(), model.getName());
			throw new BadRequestErrorException("Error parsing JSON passed in query parameter");
		}

		Set<Asset> assets = new HashSet<>();
		assets.addAll(model.getQuerier().getSystemAssetsByMetadata(storeManager.getStore(), queryMetadata).values());
		logger.info("Located assets:");
		assets.forEach(asset -> {
			logger.info(asset.getLabel());
			logger.info("getting metadata");
			List<MetadataPair> assetMetadata = model.getQuerier().getMetadataOnEntity(storeManager.getStore(), asset);
			logger.info("metadata: {}", assetMetadata);
		});

		logger.info("Get assets by metadata done");

		return ResponseEntity.status(HttpStatus.OK).body(assets);
	}

	/**
	 * Delete all metadata associated with a single asset.
	 *
	 * @param modelId Webkey of the model
	 * @param assetId ID of the asset
	 * @return Request response with a body containing a list of assets
	 */
	@RequestMapping(value = "/models/{modelId}/assets/{assetId}/meta", method = RequestMethod.DELETE)
	@ResponseBody
	public ResponseEntity<List<MetadataPair>> deleteMetadataOnAsset(@PathVariable String modelId, @PathVariable String assetId) {
		logger.info("Called REST method to delete metadata on asset {} in model {}", assetId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		// look up asset by ID
		logger.debug("Looking up asset with ID: {}", assetId);
		Asset asset = modelHelper.getAssetById(assetId, model, false); //no need to get full details here
		if (asset == null) {
			logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
			throw new AssetInvalidException();
		}

		model.getUpdater().deleteMetadataOnEntity(storeManager.getStore(), asset);

		List<MetadataPair> assetMetadata = model.getQuerier().getMetadataOnEntity(storeManager.getStore(), asset);
		return new ResponseEntity<>(assetMetadata, HttpStatus.OK);
	}


	/**
	 * Replace all metadata on an asset.
	 *
	 * @param modelId Webkey of the model
	 * @param assetId ID of the asset
	 * @param metadataPairs A list of metadata pairs (in the request body)
	 * @return Request response with a body containing the resulting list of metadata pairs on the asset
	 */
	@RequestMapping(value = "/models/{modelId}/assets/{assetId}/meta", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<List<MetadataPair>> replaceMetadataOnAsset(@PathVariable String modelId, @PathVariable String assetId,
												@RequestBody List<MetadataPair> metadataPairs) {
		logger.info("Called REST method to replace metadata on asset {} for model {}", assetId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		// look up asset by ID
		logger.debug("Looking up asset with ID: {}", assetId);
		Asset asset = modelHelper.getAssetById(assetId, model, false); //no need to get full details here
		if (asset == null) {
			logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
			throw new AssetInvalidException();
		}

		model.getUpdater().deleteMetadataOnEntity(storeManager.getStore(), asset);
		for (MetadataPair metadataPair : metadataPairs) {
			model.getUpdater().addMetadataPairToEntity(storeManager.getStore(), asset, metadataPair);
		}

		List<MetadataPair> assetMetadata = model.getQuerier().getMetadataOnEntity(storeManager.getStore(), asset);
		return new ResponseEntity<>(assetMetadata, HttpStatus.OK);
	}

	/**
	 * Add metadata to an asset.
	 *
	 * @param modelId Webkey of the model
	 * @param assetId ID of the asset
	 * @param metadataPairs A list of metadata pairs (in the request body).
	 * @return Request response with a body containing the resulting list of metadata pairs on the asset
	 */
	@RequestMapping(value = "/models/{modelId}/assets/{assetId}/meta", method = RequestMethod.PATCH)
	@ResponseBody
	public ResponseEntity<List<MetadataPair>> addMetadataOnAsset(@PathVariable String modelId, @PathVariable String assetId,
													@RequestBody List<MetadataPair> metadataPairs) {
		logger.info("Called REST method to add metadata on asset {} for model {}", assetId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		// look up asset by ID
		logger.debug("Looking up asset with ID: {}", assetId);
		Asset asset = modelHelper.getAssetById(assetId, model, false); //no need to get full details here
		if (asset == null) {
			logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
			throw new AssetInvalidException();
		}

		for (MetadataPair metadataPair : metadataPairs) {
			model.getUpdater().addMetadataPairToEntity(storeManager.getStore(), asset, metadataPair);
		}

		List<MetadataPair> assetMetadata = model.getQuerier().getMetadataOnEntity(storeManager.getStore(), asset);
		return new ResponseEntity<>(assetMetadata, HttpStatus.OK);
	}
}
