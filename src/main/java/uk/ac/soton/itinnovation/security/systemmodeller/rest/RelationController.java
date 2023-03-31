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
//    Created By :          Gianluca Correndo
//    Created Date :        27/07/2016
//    Modified by:          Ken Meacham
//    Created for Project : 5G-Ensure
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import java.rmi.UnexpectedException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.soton.itinnovation.security.model.system.Asset;
import uk.ac.soton.itinnovation.security.model.system.Relation;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.model.WebKeyRole;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.CreateRelationResponse;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.DeleteRelationResponse;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.ModelDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.AssetInvalidException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.RelationInvalidException;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.StoreModelManager;
import uk.ac.soton.itinnovation.security.systemmodeller.util.SecureUrlHelper;


@RestController
public class RelationController {

	private static final Logger logger = LoggerFactory.getLogger(RelationController.class);

	@Autowired
	private ModelObjectsHelper modelHelper;

	@Autowired
	private StoreModelManager storeManager;

	@Autowired
	private SecureUrlHelper secureUrlHelper;

	/**
	 *  Get a list of Relation present in the model.
	 *
	 * @param modelId
	 * @return a list of relations contained in the model
	 * @throws java.rmi.UnexpectedException
	 */
	@RequestMapping(value = "/models/{modelId}/relations", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Set<Relation>> listModelRelations(@PathVariable String modelId) throws UnexpectedException {

		logger.info("Called REST method to GET relations for model {}", modelId);

		// check if model valid
		Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

		return ResponseEntity.status(HttpStatus.OK).body(modelHelper.getRelationsForModel(model));
	}

	/**
	 *  Submit a new relation to be created.
	 *
	 * @param modelId
	 * @param newRel
	 * @return the persisted relation, error otherwise
	 * @throws java.rmi.UnexpectedException
	 */
	@RequestMapping(value = "/models/{modelId}/relations", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<CreateRelationResponse> createRelation(@PathVariable String modelId, @RequestBody Relation newRel) throws UnexpectedException {

		logger.info("Called REST method to POST relation {} for model {}", newRel, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		// look up store assets by ID
		logger.debug("Looking up asset with ID: {}", newRel.getFromID());
		Asset from = modelHelper.getAssetById(newRel.getFromID(), model, false); //only basic details required here
		if (from == null) {
			logger.error("Unknown asset '{}' for model [{}] {}", newRel.getFromID(), model.getId(), model.getName());
			throw new AssetInvalidException();
		}
		String fromUri = from.getUri();

		logger.debug("Looking up asset with ID: {}", newRel.getToID());
		Asset to = modelHelper.getAssetById(newRel.getToID(), model, false); //only basic details required here
		if (to == null) {
			logger.error("Unknown asset '{}' for model [{}] {}", newRel.getToID(), model.getId(), model.getName());
			throw new AssetInvalidException();
		}
		String toUri = to.getUri();

		logger.debug("Resolved assets to URIs: from <{}> to <{}>", fromUri, toUri);

		// create relation
		newRel.setFrom(fromUri);
		newRel.setTo(toUri);
		model.getUpdater().storeRelation(storeManager.getStore(), newRel);

		// invalidate model
		model.invalidate();

		return ResponseEntity.status(HttpStatus.CREATED).body(new CreateRelationResponse(newRel, new ModelDTO(model)));
	}

	/**
	 *  Get information about a relation.
	 *
	 * @param modelId
	 * @param relationId
	 * @return the relation contained in the model
	 */
	@RequestMapping(value = "/models/{modelId}/relations/{relationId}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Relation> getRelation(@PathVariable String modelId, @PathVariable String relationId) {

		logger.info("Called REST method to GET relation {} for model {}", relationId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

		// check if relation exists in the database
		Relation relation = modelHelper.getRelationForModel(model, relationId);
		if (relation == null) {
			throw new RelationInvalidException();
		}

		return ResponseEntity.status(HttpStatus.OK).body(relation);
	}

	/**
	 *  Update information about a relation.
	 *
	 * @param modelId
	 * @param relationId
	 * @param relationUpdate
	 * @return the updated relation contained in the model
	 * @throws java.rmi.UnexpectedException
	 */
	@RequestMapping(value = "/models/{modelId}/relations/{relationId}", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<Relation> updateRelation(@PathVariable String modelId, @PathVariable String relationId, @RequestBody Relation relationUpdate) throws UnexpectedException {

		logger.info("Called REST method to PUT relation {}:{} for model {}", relationId, relationUpdate, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		// check if relation exists in the database
		Relation oldRelation = modelHelper.getRelationForModel(model, relationId);
		if (oldRelation == null) {
			logger.error("Failed to find relation [{}]", relationId);
			throw new RelationInvalidException();
		}
		
		//TODO: could throw exception here if relation is not assertable
		
		Relation updatedRelation = new Relation(oldRelation.getFrom(),
												oldRelation.getFromID(),
												oldRelation.getTo(),
												oldRelation.getToID(),
												oldRelation.getType(),
												oldRelation.getLabel());
		
		//ensure that other fields are copied across
		updatedRelation.setSourceCardinality(oldRelation.getSourceCardinality());
		updatedRelation.setTargetCardinality(oldRelation.getTargetCardinality());
		
		boolean updateType = false;
		
		// update relation fields
		if (relationUpdate.getLabel() != null && !oldRelation.getLabel().equals(relationUpdate.getLabel())) {
			updatedRelation.setLabel(relationUpdate.getLabel());
		}
		
		if (relationUpdate.getType() != null && !oldRelation.getType().equals(relationUpdate.getType())) {
			logger.debug("will update relation type to {}", relationUpdate.getType());
			updatedRelation.setType(relationUpdate.getType());
			updateType = true;
		}

		if (relationUpdate.getFromID() != null && !oldRelation.getFromID().equals(relationUpdate.getFromID())) {
			// look up asset by ID
			logger.debug("Looking up asset with ID: {}", relationUpdate.getFromID());
			Asset asset = modelHelper.getAssetById(relationUpdate.getFromID(), model, false); //only basic details required here
			if (asset == null) {
				logger.error("Unknown asset '{}' for model [{}] {}", relationUpdate.getFromID(), model.getId(), model.getName());
				throw new AssetInvalidException();
			}
			updatedRelation.setFromID(relationUpdate.getFromID());
			updatedRelation.setFrom(asset.getUri());
		}
		if (relationUpdate.getToID() != null && !oldRelation.getToID().equals(relationUpdate.getToID())) {
			// look up asset by ID
			logger.debug("Looking up asset with ID: {}", relationUpdate.getToID());
			Asset asset = modelHelper.getAssetById(relationUpdate.getToID(), model, false); //only basic details required here
			if (asset == null) {
				logger.error("Unknown asset '{}' for model [{}] {}", relationUpdate.getToID(), model.getId(), model.getName());
				throw new AssetInvalidException();
			}
			updatedRelation.setToID(relationUpdate.getToID());
			updatedRelation.setTo(asset.getUri());
		}

		int newMinCardinality = relationUpdate.getSourceCardinality();
		if (newMinCardinality > -2) {
			int oldMinCardinality = oldRelation.getSourceCardinality();
			if (oldMinCardinality != newMinCardinality) {
				logger.debug("Will update sourceCardinality to {} for relation with sourceCardinality '{}' {}", newMinCardinality, oldMinCardinality, oldRelation.getID());
				updatedRelation.setSourceCardinality(newMinCardinality);
			}
		}
		int newMaxCardinality = relationUpdate.getTargetCardinality();
		if (newMaxCardinality > -2) {
			int oldMaxCardinality = oldRelation.getTargetCardinality();
			if (oldMaxCardinality != newMaxCardinality) {
				logger.debug("Will update targetCardinality to {} for relation with targetCardinality '{}' {}", newMaxCardinality, oldMaxCardinality, oldRelation.getID());
				updatedRelation.setTargetCardinality(newMaxCardinality);
			}
		}

		logger.debug("Updating relation {} to {}", oldRelation.toString(), updatedRelation);

		// save updated relation
		if (updateType) {
			logger.debug("Calling updateRelationType");
			model.getUpdater().updateRelationType(storeManager.getStore(), oldRelation, updatedRelation);
		}
		else {
			logger.warn("Calling storeRelation (should probably use updateRelation here!)");
			model.getUpdater().storeRelation(storeManager.getStore(), oldRelation);
		}

		// invalidate model
		model.invalidate();

		return ResponseEntity.status(HttpStatus.OK).body(updatedRelation);
	}

	/**
	 *  Delete a relation given the model and relation IDs.
	 *
	 * @param modelId
	 * @param relationId
	 * @return a JSON object describing the RelationUi with the given relation id
	 */
	@RequestMapping(value = "/models/{modelId}/relations/{relationId}", method = RequestMethod.DELETE)
	@ResponseBody
	public ResponseEntity<DeleteRelationResponse> deleteRelationInModel(@PathVariable String modelId, @PathVariable String relationId) {

		logger.info("Called REST method to DELETE relation {} for model {}", relationId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		// find relation ui in db
		Relation relation = modelHelper.getRelationForModel(model, relationId);
		if (relation == null) {
			logger.error("Unknown relation [{}]", relationId);
			throw new RelationInvalidException();
		}

		String fromUri = relation.getFrom();
		String type = relation.getType();
		String toUri = relation.getTo();

		//delete relation
		logger.debug("Deleting relation [{}] from {} to {} of type {}", relationId, fromUri, toUri, type);
		model.getUpdater().deleteRelation(storeManager.getStore(), relation);

		// invalidate model, don't remove inferred assets as this will happen during revalidation
		model.invalidate();

		DeleteRelationResponse response = new DeleteRelationResponse();
		Set<String> removedRelations = new HashSet<>();
		removedRelations.add(relation.getID());
		response.addRelations(removedRelations);
		response.setValid(model.isValid()); //should always be false, as we have invalidated above

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
}
