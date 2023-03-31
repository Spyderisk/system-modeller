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
//      Created By :          Ken Meacham
//      Created Date :        30/08/2016
//      Modified by:          Stefanie Wiegand
//      Created for Project : ASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.soton.itinnovation.security.model.domain.Control;
import uk.ac.soton.itinnovation.security.model.system.ControlSet;
import uk.ac.soton.itinnovation.security.model.system.MisbehaviourSet;
import uk.ac.soton.itinnovation.security.model.system.Threat;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.model.WebKeyRole;
import uk.ac.soton.itinnovation.security.systemmodeller.mongodb.IModelRepository;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.ThreatDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.ThreatInvalidException;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.StoreModelManager;
import uk.ac.soton.itinnovation.security.systemmodeller.util.SecureUrlHelper;

@RestController
public class ThreatController {
	private static final Logger logger = LoggerFactory.getLogger(ThreatController.class);

	@Autowired
	IModelRepository modelRepository;

	@Autowired
	private StoreModelManager storeManager;

	@Autowired
	private ModelObjectsHelper modelHelper;

	@Autowired
	private SecureUrlHelper secureUrlHelper;

	/**
	 *  Find the list of threats available for the model.
	 *
	 * @param modelId
	 * @param cached boolean to optionally request for cached threats
	 * @return a list of models' threats owned by the user
	 */
	@RequestMapping(value = "/models/{modelId}/threats", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Set<ThreatDTO>> getThreats(@PathVariable String modelId, @RequestParam(required = false) boolean cached) {

		logger.info("Called REST method to GET threats for model {}", modelId);
		logger.debug("cached = {}", cached);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

		Set<Threat> threats = Collections.emptySet();

		if (cached) {
			logger.info("Getting cached threats..");
			threats = modelHelper.getCachedThreatsForModel(model); //attempt to get cached threats
			if (threats.isEmpty()) {
				logger.info("No cached threats - getting from store..");
				threats = modelHelper.getThreatsForModel(model, true);
			}
		}
		else {
			logger.info("Getting threats from store..");
			threats = modelHelper.getThreatsForModel(model, true);
		}

		logger.info("Returning {} threats", threats.size());

		Set<ThreatDTO> threatsSet = new HashSet<>();
		threats.stream().map(threat -> new ThreatDTO(threat)).forEachOrdered(threatDto -> threatsSet.add(threatDto));

		return ResponseEntity.status(HttpStatus.OK).body(threatsSet);
	}

	/**
	 * Get info about a particular threat in a model
	 *
	 * @param modelId
	 * @param threatId
	 * @return a JSON object describing the Threat with the given threat id
	 */
	@RequestMapping(value = "/models/{modelId}/threats/{threatId}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<ThreatDTO> getThreatInModel(@PathVariable String modelId, @PathVariable String threatId) {

		logger.info("Called REST method to GET threat {} for model {}", threatId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

		// look up threat by ID
		logger.debug("Looking up threat with ID: {}", threatId);
		Threat threat = modelHelper.getThreatById(threatId, model, true, true);
		if (threat == null) {
			logger.error("Unknown threat '{}' for model [{}] {}", threatId, model.getId(), model.getName());
			throw new ThreatInvalidException();
		}

		return ResponseEntity.status(HttpStatus.OK).body(new ThreatDTO(threat));
	}

	/**
	 * Accept a particular threat
	 *
	 * @param modelId
	 * @param threatId
	 * @param updatedThreat
	 * @param assetId
	 * @param updatedControlUi
	 * @return a JSON object describing the updated controls and threats for a given asset
	 */
	@RequestMapping(value = "/models/{modelId}/threats/{threatId}/accept", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<ThreatDTO> acceptThreat(@PathVariable String modelId, @PathVariable String threatId, @RequestBody Threat updatedThreat) {

		logger.info("Called REST method to POST acceptance justification {} on threat <{}> for model {}",
				updatedThreat.getAcceptanceJustification(), threatId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		// look up threat by ID
		logger.debug("Looking up threat with ID: {}", threatId);
		Threat threat = modelHelper.getThreatById(threatId, model, false, true);
		if (threat == null) {
			logger.error("Unknown threat '{}' for model [{}] {}", threatId, model.getId(), model.getName());
			throw new ThreatInvalidException();
		}

		if (updatedThreat.getAcceptanceJustification()==null || updatedThreat.getAcceptanceJustification().isEmpty()) {
			updatedThreat.setAcceptanceJustification(null);
		}

		//do it
		logger.info("Updating acceptance justification \"{}\" for threat <{}>",
				updatedThreat.getAcceptanceJustification(), threat.getUri());
		model.getUpdater().acceptThreat(storeManager.getStore(), threat.getUri(), updatedThreat.getAcceptanceJustification());

		//TODO: check if we really need to retrieve the threat again here (we do at least need to update the cached threat)
		Threat returnedThreat = modelHelper.getThreatForModel(model, threat.getUri());

		return ResponseEntity.status(HttpStatus.OK).body(new ThreatDTO(returnedThreat));
	}

	/**
	 *  Update impact information about a misbehaviour associated with a threat.
	 *
	 * @param modelId
	 * @param misbehaviourId
	 * @param updatedMisbehaviour
	 * @return a JSON object describing updated misbehaviour impact
	 */
	@RequestMapping(value = "/models/{modelId}/misbehaviours/{misbehaviourId}/impact", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<String> updateMisbehaviourImpact(@PathVariable String modelId, @PathVariable String misbehaviourId,
			@RequestBody MisbehaviourSet updatedMisbehaviour) {

		logger.info("Called REST method to PUT misbehaviour impact {} in model {}", misbehaviourId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		//update the misbehaviour
		logger.debug("Calling updateMS");
		model.getUpdater().updateMS(storeManager.getStore(), updatedMisbehaviour);

		//set a flag to show the user they have to re-run the risk level calculation
		model.invalidateRiskLevels();
		
		logger.debug("Sending response from updateMisbehaviourImpact");
		return ResponseEntity.status(HttpStatus.OK).body("completed");
	}

	/**
	 *  Revert asserted impact level for a misbehaviour
	 *
	 * @param modelId
	 * @param misbehaviourId
	 * @param misbehaviour Misbehaviour id/uri
	 * @return updated misbehaviour containing reverted impact level
	 */
	@RequestMapping(value = "/models/{modelId}/misbehaviours/{misbehaviourId}/revert-impact", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<MisbehaviourSet> revertMisbehaviourImpact(@PathVariable String modelId, @PathVariable String misbehaviourId,
			@RequestBody MisbehaviourSet misbehaviour) {

		logger.info("Called REST method to revert misbehaviour impact {} in model {}", misbehaviourId, modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.WRITE);

		logger.info("Deleting asserted impact level for misbehaviour {}", misbehaviour.getUri());
		model.getUpdater().deleteAssertedImpactLevel(storeManager.getStore(), misbehaviour);

		logger.info("Getting updated misbehaviour: {}", misbehaviour.getUri());
		MisbehaviourSet updatedMS = model.getQuerier().getMisbehaviourSet(storeManager.getStore(), misbehaviour.getUri(), false);

		logger.debug("Sending response from revertMisbehaviourImpact");
		logger.debug("Updated MS: {}", updatedMS);

		//set a flag to show the user they have to re-run the risk level calculation
		model.invalidateRiskLevels();

		return ResponseEntity.status(HttpStatus.OK).body(updatedMS);
	}

	/**
	 *  Find the control sets for the model.
	 *
	 * @param modelId
	 * @return map of control sets
	 */
	@RequestMapping(value = "/models/{modelId}/controlsets", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Map<String, ControlSet>> getControlSets(@PathVariable String modelId) {

		logger.info("Called REST method to GET control sets for model {}", modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

		Map<String, ControlSet> controlSets = modelHelper.getControlSetsForModel(model);

		return ResponseEntity.status(HttpStatus.OK).body(controlSets);
	}

	/**
	 *  Find the list of controls for the model.
	 *
	 * @param modelId
	 * @return a list of controls
	 */
	@RequestMapping(value = "/models/{modelId}/controls", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Map<String, Control>> getControls(@PathVariable String modelId) {

		logger.info("Called REST method to GET controls for model {}", modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

		Map<String, Control> controls = modelHelper.getControls(model);

		return ResponseEntity.status(HttpStatus.OK).body(controls);
	}

}
