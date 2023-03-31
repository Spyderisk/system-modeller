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
//      Created By :            Ken Meacham
//      Created Date :          11 Jun 2020
//      Modified By :           
//      Created for Project :   Protego
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import uk.ac.soton.itinnovation.security.model.system.Relation;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.model.WebKeyRole;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.AssetGroupDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.DeleteGroupResponse;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.AssetGroupInvalidException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.AssetInvalidException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.BadRequestErrorException;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.StoreModelManager;
import uk.ac.soton.itinnovation.security.systemmodeller.util.SecureUrlHelper;

@RestController
public class GroupController {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private StoreModelManager storeManager;
	
	@Autowired
	private ModelObjectsHelper modelHelper;

	@Autowired
	private SecureUrlHelper secureUrlHelper;
	
	/**
	 * Add an AssetGroup object to persist in the model. 
	 *
	 * @param modelId Webkey of the model
	 * @param assetGroupDTO AssetGroup object to add.
	 * @return the AssetGroup instance
	 */
	@RequestMapping(value = "/models/{modelId}/assetGroups", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public ResponseEntity<AssetGroupDTO> addAssetGroupToModel(@PathVariable String modelId, @RequestBody AssetGroupDTO assetGroupDTO) {

		logger.info("Called REST method to POST new asset group for model {}", modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		// create asset group
		assetGroupDTO.setUri(modelHelper.createNewAssetGroupUri());
		assetGroupDTO.setExpanded(true); //expanded by default
		int width = assetGroupDTO.getWidth();
		assetGroupDTO.setWidth(width > 0 ? width : 400); //ensure min width
		int height = assetGroupDTO.getHeight();
		assetGroupDTO.setHeight(height > 0 ? height : 400); //ensure min height
		if (assetGroupDTO.getAssetIds() == null) {
			assetGroupDTO.setAssetIds(new ArrayList<>());
		}

		// save to store
		AssetGroup assetGroup = assetGroupDTO.toAssetGroup();
		model.getUpdater().storeAssetGroup(storeManager.getStore(), assetGroup);

		return ResponseEntity.status(HttpStatus.CREATED).body(assetGroupDTO);
	}

	/**
	 * Get all AssetGroups
	 *
	 * @param modelId Webkey of the model
	 * @return a list of model's AssetGroups
	 */
	@RequestMapping(value = "/models/{modelId}/assetGroups", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Set<AssetGroupDTO>> getAssetGroups(@PathVariable String modelId) {

		logger.info("Called REST method to GET groups for model {}", modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

		Map<String, AssetGroup> assetGroupsMap = model.getQuerier().getAssetGroups(storeManager.getStore());
		Collection<AssetGroup> assetGroups = assetGroupsMap.values();

		Set<AssetGroupDTO> assetGroupsSet = new HashSet<>();
		for (AssetGroup assetGroup : assetGroups) {
			AssetGroupDTO assetDto = new AssetGroupDTO(assetGroup);
			assetGroupsSet.add(assetDto);
		}
		
		return ResponseEntity.status(HttpStatus.OK).body(assetGroupsSet);
	}

	/**
	 * Add an Asset to an AssetGroup object. 
	 *
	 * @param modelId Webkey of the model
	 * @param groupId the id of the AssetGroup
	 * @param assetId the id of the Asset to add
	 * @param updatedAsset the Asset to add, including updated location within group
	 * @return the updated AssetGroupDTO instance
	 */
	@RequestMapping(value = "/models/{modelId}/assetGroups/{groupId}/addAsset/{assetId}", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<AssetGroupDTO> addAssetToGroup(@PathVariable String modelId, @PathVariable String groupId, @PathVariable String assetId, @RequestBody Asset updatedAsset) {

		logger.info("Called REST method to add asset {} at [{},{}] to asset group {} for model {}", assetId, updatedAsset.getIconX(), updatedAsset.getIconY(), groupId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		logger.debug("Getting asset group {}", groupId);
		AssetGroup assetGroup = modelHelper.getAssetGroupById(model, groupId);
		if (assetGroup == null) {
			logger.error("Unknown asset group '{}' for model [{}] {}", groupId, model.getId(), model.getName());
			throw new AssetGroupInvalidException();
		}

		logger.debug("Getting asset {}", assetId);
		Asset asset = modelHelper.getAssetById(assetId, model, false); //just do basic lookup
		if (asset == null) {
			logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
			throw new AssetInvalidException();
		}

		asset.setIconPosition(updatedAsset.getIconX(), updatedAsset.getIconY());
		logger.debug("addAssetToGroup: Updating asset {} to location [{},{}]", asset.getID(), asset.getIconX(), asset.getIconY());
		model.getUpdater().updateAssetPosition(storeManager.getStore(), asset);
		logger.debug("addAssetToGroup: Updated asset {} to location [{},{}]", asset.getID(), asset.getIconX(), asset.getIconY());

		Set<Asset> addAssets = new HashSet<>();
		addAssets.add(asset);

		boolean success = model.getUpdater().addAssetsToAssetGroup(storeManager.getStore(), assetGroup, addAssets);

		if (success) {
			return new ResponseEntity<>(new AssetGroupDTO(assetGroup), HttpStatus.OK);
		} else {
			throw new BadRequestErrorException(String.format("An error occurred when attempting " +
							"to add asset %s to asset group %s, the asset may already be in the group or may belong to " +
							"another group", assetId, groupId));
		}
	}

	/**
	 * Remove an Asset from an AssetGroup object (placing back onto canvas)
	 *
	 * @param modelId Webkey of the model
	 * @param groupId the id of the AssetGroup
	 * @param assetId the id of the Asset to remove
	 * @param updatedAsset the Asset to remove, including new position on canvas
	 * @return the updated AssetGroupDTO instance
	 */
	@RequestMapping(value = "/models/{modelId}/assetGroups/{groupId}/removeAsset/{assetId}", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<AssetGroupDTO> removeAssetFromGroup(@PathVariable String modelId, @PathVariable String groupId, @PathVariable String assetId, @RequestBody Asset updatedAsset) {

		logger.info("Called REST method to remove asset {} at [{},{}] from group {} for model {}", assetId, updatedAsset.getIconX(), updatedAsset.getIconY(), groupId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		logger.debug("Getting asset group {}", groupId);
		AssetGroup assetGroup = modelHelper.getAssetGroupById(model, groupId);
		if (assetGroup == null) {
			logger.error("Unknown asset group '{}' for model [{}] {}", groupId, model.getId(), model.getName());
			throw new AssetGroupInvalidException();
		}

		logger.debug("Getting asset {}", assetId);
		Asset asset = modelHelper.getAssetById(assetId, model, false); //just do basic lookup
		if (asset == null) {
			logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
			throw new AssetInvalidException();
		}
		
		asset.setIconPosition(updatedAsset.getIconX(), updatedAsset.getIconY());
		logger.debug("removeAssetFromGroup: Updating asset {} to location [{},{}]", asset.getID(), asset.getIconX(), asset.getIconY());
		model.getUpdater().updateAssetPosition(storeManager.getStore(), asset);
		logger.debug("removeAssetFromGroup: Updated asset {} to location [{},{}]", asset.getID(), asset.getIconX(), asset.getIconY());

		Set<Asset> removeAssets = new HashSet<>();
		removeAssets.add(asset);

		boolean success = model.getUpdater().removeAssetsFromAssetGroup(storeManager.getStore(), assetGroup, removeAssets);

		if (success) {
			return new ResponseEntity<>(new AssetGroupDTO(assetGroup), HttpStatus.OK);
		} else {
			throw new BadRequestErrorException(String.format("An error occurred when attempting " +
					"to remove asset %s from asset group %s, the asset may not be in the group", assetId, groupId));
		}
	}

	/**
	 * Move an Asset from one AssetGroup to another
	 *
	 * @param modelId Webkey of the model
	 * @param groupId the id of the source AssetGroup
	 * @param assetId the id of the Asset to move
	 * @param targetGroupId the id of the target AssetGroup
	 * @param updatedAsset the Asset to move, including its new position in target AssetGroup
	 * @return the updated AssetGroupDTO instance
	 */
	@RequestMapping(value = "/models/{modelId}/assetGroups/{groupId}/moveAsset/{assetId}/toGroup/{targetGroupId}", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Map<String, AssetGroupDTO>> moveAssetGroup(@PathVariable String modelId, @PathVariable String groupId, @PathVariable String assetId, @PathVariable String targetGroupId, @RequestBody Asset updatedAsset) {

		logger.info("Called REST method to move asset {} at [{},{}] from group {} to group {} for model {}", assetId, updatedAsset.getIconX(), updatedAsset.getIconY(), groupId, targetGroupId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		logger.debug("Getting asset source group {}", groupId);
		AssetGroup assetGroup = modelHelper.getAssetGroupById(model, groupId);
		if (assetGroup == null) {
			logger.error("Unknown asset group '{}' for model [{}] {}", groupId, model.getId(), model.getName());
			throw new AssetGroupInvalidException();
		}

		logger.debug("Getting asset target group {}", groupId);
		AssetGroup targetAssetGroup = modelHelper.getAssetGroupById(model, targetGroupId);
		if (targetAssetGroup == null) {
			logger.error("Unknown asset group '{}' for model [{}] {}", targetGroupId, model.getId(), model.getName());
			throw new AssetGroupInvalidException();
		}
		
		logger.debug("Getting asset {}", assetId);
		Asset asset = modelHelper.getAssetById(assetId, model, false); //just do basic lookup
		if (asset == null) {
			logger.error("Unknown asset '{}' for model [{}] {}", assetId, model.getId(), model.getName());
			throw new AssetInvalidException();
		}
		
		asset.setIconPosition(updatedAsset.getIconX(), updatedAsset.getIconY());
		logger.debug("moveAssetGroup: Updating asset {} to location [{},{}]", asset.getID(), asset.getIconX(), asset.getIconY());
		model.getUpdater().updateAssetPosition(storeManager.getStore(), asset);
		logger.debug("moveAssetGroup: Updated asset {} to location [{},{}]", asset.getID(), asset.getIconX(), asset.getIconY());
		
		Set<Asset> removeAssets = new HashSet<>();
		removeAssets.add(asset);

		logger.debug("Removing asset {} from group {}", assetId, groupId);
		boolean success = model.getUpdater().removeAssetsFromAssetGroup(storeManager.getStore(), assetGroup, removeAssets);

		if (success) {
			Set<Asset> addAssets = new HashSet<>();
			addAssets.add(asset);

			logger.debug("Adding asset {} to group {}", assetId, targetGroupId);
			success = model.getUpdater().addAssetsToAssetGroup(storeManager.getStore(), targetAssetGroup, addAssets);
			
			if (success) {
				Map<String, AssetGroupDTO> response = new HashMap<>();
				response.put("sourceGroup", new AssetGroupDTO(assetGroup));
				response.put("targetGroup", new AssetGroupDTO(targetAssetGroup));
				logger.debug("Done");
				return new ResponseEntity<>(response, HttpStatus.OK);
			}
			else {
				throw new BadRequestErrorException(String.format("An error occurred when attempting " +
								"to add asset %s to asset group %s, the asset may already be in the group or may belong to " +
								"another group", assetId, targetGroupId));
			}
		
		} else {
			throw new BadRequestErrorException(String.format("An error occurred when attempting " +
					"to remove asset %s from asset group %s, the asset may not be in the group", assetId, groupId));
		}
	}

	/**
	 * Update an AssetGroup location on the canvas. 
	 *
	 * @param modelId Webkey of the model
	 * @param groupId the id of the AssetGroup
	 * @param assetGroupDTO the AssetGroupDTO with updated left (x) and top (y) values
	 * @return the updated AssetGroupDTO instance
	 */
	@RequestMapping(value = "/models/{modelId}/assetGroups/{groupId}/location", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<AssetGroupDTO> updateGroupLocation(@PathVariable String modelId, @PathVariable String groupId, @RequestBody AssetGroupDTO updatedGroupDTO) {

		logger.info("Called REST method to update location for group {}: for model {}", groupId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		AssetGroup assetGroup = updatedGroupDTO.toAssetGroup();
		model.getUpdater().updateAssetGroupLocation(storeManager.getStore(), assetGroup, assetGroup.getX(), assetGroup.getY());

		logger.debug("Updated group location {} <{}>", assetGroup.getID(), assetGroup.getUri());
		return ResponseEntity.status(HttpStatus.OK).body(updatedGroupDTO);
	}

	/**
	 * Update an AssetGroup size on the canvas. 
	 *
	 * @param modelId Webkey of the model
	 * @param groupId the id of the AssetGroup
	 * @param assetGroupDTO the AssetGroupDTO with updated size (width, height)
	 * @return the updated AssetGroupDTO instance
	 */
	@RequestMapping(value = "/models/{modelId}/assetGroups/{groupId}/size", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<AssetGroupDTO> updateGroupSize(@PathVariable String modelId, @PathVariable String groupId, @RequestBody AssetGroupDTO updatedGroupDTO) {

		logger.info("Called REST method to update size to [{},{}] for group {}: for model {}", updatedGroupDTO.getWidth(), updatedGroupDTO.getHeight(), groupId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		AssetGroup assetGroup = updatedGroupDTO.toAssetGroup();
		model.getUpdater().updateAssetGroupSize(storeManager.getStore(), assetGroup, assetGroup.getWidth(), assetGroup.getHeight());

		logger.debug("Updated group size {} <{}>", assetGroup.getID(), assetGroup.getUri());
		return ResponseEntity.status(HttpStatus.OK).body(updatedGroupDTO);
	}

	/**
	 * Expand/contract an AssetGroup on the canvas, depending on the value of "expanded" field 
	 *
	 * @param modelId Webkey of the model
	 * @param groupId the id of the AssetGroup
	 * @param assetGroupDTO the AssetGroupDTO, with updated "expanded" field (true or false)
	 * @return the updated AssetGroupDTO instance
	 */
	@RequestMapping(value = "/models/{modelId}/assetGroups/{groupId}/expanded", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<AssetGroupDTO> updateGroupExpanded(@PathVariable String modelId, @PathVariable String groupId, @RequestBody AssetGroupDTO updatedGroupDTO) {

		logger.info("Called REST method to update expanded to {} for group {}: for model {}", updatedGroupDTO.isExpanded(), groupId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		AssetGroup assetGroup = updatedGroupDTO.toAssetGroup();
		model.getUpdater().updateAssetGroupExpanded(storeManager.getStore(), assetGroup, assetGroup.isExpanded());

		logger.debug("Updated group expanded {} <{}>", assetGroup.getID(), assetGroup.getUri());
		return ResponseEntity.status(HttpStatus.OK).body(updatedGroupDTO);
	}

	/**
	 * Update an AssetGroup label. 
	 *
	 * @param modelId Webkey of the model
	 * @param groupId the id of the AssetGroup
	 * @param assetGroupDTO the AssetGroupDTO with updated label field
	 * @return the updated AssetGroupDTO instance
	 */
	@RequestMapping(value = "/models/{modelId}/assetGroups/{groupId}/label", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<AssetGroupDTO> updateGroupLabel(@PathVariable String modelId, @PathVariable String groupId, @RequestBody AssetGroupDTO updatedGroupDTO) {

		logger.info("Called REST method to update label to {} for group {}: for model {}", updatedGroupDTO.getLabel(), groupId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		AssetGroup assetGroup = updatedGroupDTO.toAssetGroup();
		model.getUpdater().updateAssetGroupLabel(storeManager.getStore(), assetGroup, assetGroup.getLabel());

		logger.debug("Updated group label {} <{}>", assetGroup.getID(), assetGroup.getUri());
		return ResponseEntity.status(HttpStatus.OK).body(updatedGroupDTO);
	}

	/**
	 * Delete an AssetGroup, optionally deleting the grouped assets.
	 *
	 * @param modelId Webkey of the model
	 * @param groupId the id of the AssetGroup
	 * @param deleteAssets flag to indicate whether grouped assets should also be deleted, or returned to the canvas
	 * @return DeleteGroupResponse includes group id deleted and any assets, relations that were also deleted
	 */
	@RequestMapping(value = "/models/{modelId}/assetGroups/{groupId}", method = RequestMethod.DELETE)
	@ResponseBody
	public ResponseEntity<DeleteGroupResponse> deleteGroupInModel(@PathVariable String modelId, @PathVariable String groupId,
												@RequestParam(defaultValue = "false") boolean deleteAssets) {

		logger.info("Called REST method to DELETE group {} from model {} (delete child assets: {})", groupId, modelId,
				deleteAssets);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		// get lock for model and synchronize on this
		Object modelLock = modelHelper.getModelLock(model);
		logger.debug("deleteGroupInModel: waiting for lock on model: {}...", model.getId());
		synchronized(modelLock) {
			logger.debug("deleteGroupInModel: aquired lock on model: {}", model.getId());

			logger.debug("Getting asset group {}", groupId);
			AssetGroup assetGroup = modelHelper.getAssetGroupById(model, groupId);
			if (assetGroup == null) {
				logger.error("Unknown asset group '{}' for model [{}] {}", groupId, model.getId(), model.getName());
				throw new AssetGroupInvalidException();
			}

			Set<Relation> relations = new HashSet<>();
			if (deleteAssets) {
				relations = modelHelper.getRelationsForModel(model);
			}

			logger.debug("Deleting asset group {}", groupId);

			DeleteGroupResponse response = new DeleteGroupResponse();

			response.setAssetGroup(assetGroup.getID());
			model.getUpdater().deleteAssetGroup(storeManager.getStore(), assetGroup, deleteAssets);

			if (deleteAssets) {
				Map<String, Asset> groupAssets = assetGroup.getAssets();
				Set<String> assetIDs = new HashSet<>(); //deleted asset IDs
				for (Asset asset : groupAssets.values()) {
					modelHelper.deleteAssetFromCache(asset, model);
					assetIDs.add(asset.getID());
				}
				
				response.addAssets(assetIDs);
				logger.debug("Deleted {} assets", assetIDs.size());

				relations.removeAll(modelHelper.getRelationsForModel(model));

				// add deleted relations to list
				Set<String> relationIDs = new HashSet<>();
				relations.forEach(r -> relationIDs.add(r.getID()));
				response.addRelations(relationIDs);
				logger.debug("Deleted {} relations", relationIDs.size());
				
				if ((assetIDs.size() + relationIDs.size()) > 0) {
					//invalidate the model
					model.invalidate();
				}
			}

			//Check if model is valid (should be invalidated if assets are deleted)
			logger.debug("valid = {}", model.isValid());
			response.setValid(model.isValid());
			
			logger.debug("Done");

			logger.debug("deleteGroupInModel: releasing lock on model: {}", model.getId());
			return ResponseEntity.status(HttpStatus.OK).body(response);
		}
	}
}
