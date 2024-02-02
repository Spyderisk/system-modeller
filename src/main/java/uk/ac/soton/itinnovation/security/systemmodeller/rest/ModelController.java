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
//    Created Date :        ?
//    Modified by:          Ken Meacham, Stefanie Wiegand
//    Created for Project : 5g-Ensure
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.UnexpectedException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.naming.SizeLimitExceededException;
import javax.servlet.http.HttpServletRequest;

import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;
import uk.ac.soton.itinnovation.security.model.system.RiskLevelCount;
import uk.ac.soton.itinnovation.security.model.system.RiskVector;
import uk.ac.soton.itinnovation.security.modelquerier.JenaQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.RiskCalcResultsDB;
import uk.ac.soton.itinnovation.security.modelvalidator.ModelValidator;
import uk.ac.soton.itinnovation.security.modelvalidator.Progress;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.AttackPathAlgorithm;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.RecommendationsAlgorithm;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.RecommendationsAlgorithmConfig;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.RecommendationReportDTO;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.TreeJsonDoc;
import uk.ac.soton.itinnovation.security.semanticstore.AStoreWrapper;
import uk.ac.soton.itinnovation.security.semanticstore.IStoreWrapper;
import uk.ac.soton.itinnovation.security.semanticstore.JenaTDBStoreWrapper;
import uk.ac.soton.itinnovation.security.semanticstore.util.SparqlHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.auth.KeycloakAdminClient;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.model.ModelFactory;
import uk.ac.soton.itinnovation.security.systemmodeller.model.WebKeyRole;
import uk.ac.soton.itinnovation.security.systemmodeller.mongodb.IModelRepository;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.AsyncController.JobResponseDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.LoadingProgress;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.LoadingProgressResponse;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.ModelDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.UpdateModelResponse;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.BadRequestErrorException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.InternalServerErrorException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.MisbehaviourSetInvalidException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.ModelException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.ModelInvalidException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.NotAcceptableErrorException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.NotFoundErrorException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.UnprocessableEntityException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.UserForbiddenFromDomainException;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.StoreModelManager;
import uk.ac.soton.itinnovation.security.systemmodeller.util.ReportGenerator;
import uk.ac.soton.itinnovation.security.systemmodeller.util.SecureUrlHelper;

import uk.ac.soton.itinnovation.security.systemmodeller.model.RecommendationEntity;
import uk.ac.soton.itinnovation.security.systemmodeller.mongodb.RecommendationRepository;
import uk.ac.soton.itinnovation.security.systemmodeller.attackpath.AsyncService;
import uk.ac.soton.itinnovation.security.systemmodeller.attackpath.AsyncService.RecStatus;


/**
 * Includes all operations of the Model Controller Service.
 */
@RestController
public class ModelController {

	private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RecommendationRepository recRepository;

	@Autowired
	private IModelRepository modelRepository;

	@Autowired
	private StoreModelManager storeModelManager;

	@Autowired
	private KeycloakAdminClient keycloakAdminClient;

	@Autowired
	private ModelObjectsHelper modelObjectsHelper;

	@Autowired
	private ModelFactory modelFactory;

	@Autowired
	private SecureUrlHelper secureUrlHelper;

    @Autowired
    private AsyncService asyncService;

	@Value("${admin-role}")
	public String adminRole;

	@Value("${knowledgebases.install.folder}")
	private String kbInstallFolder;

	/**
	 * Take the user IDs of the model owner, editor and modifier and look up the current username for them
	 */
	private void setModelNamesFromIds(Model m) {
		UserRepresentation keycloakUser;

		if (m.getUserId() != null) {
			m.setUserName(keycloakAdminClient.getUserById(m.getUserId()).getUsername());
		}

		if (m.getEditorId() != null) {
			// TODO: we should not have to check with keycloak to see if the editorId was actually a webkey
			keycloakUser = keycloakAdminClient.getUserById(m.getEditorId());
			if (keycloakUser == null) {
				m.setEditorId("anonymous");  // TODO: ideally we would let the client choose what to display
				m.setEditorName("anonymous"); // N.B. editorId is set to value of editorName in ModelDTO!
			} else {
				m.setEditorName(keycloakUser.getUsername());
			}
		}
		if (m.getModifiedBy() != null) {
			keycloakUser = keycloakAdminClient.getUserById(m.getModifiedBy());
			if (keycloakUser == null) {
				m.setModifiedByName("anonymous");
			} else {
				m.setModifiedByName(keycloakUser.getUsername());
			}
		}
	}

	private Set<Object> getModelObjectsForUser(UserRepresentation user){
		int numModels, counter = 0;
		Set<Model> dbOwnModels = modelFactory.getModels(modelRepository.findByUserId(user.getId()));
		Set<Object> models = new HashSet<>();

		Set<Model> dbSharedOwnerModels = modelFactory.getModels(modelRepository.findByOwnerUsernamesContains(user.getUsername()));
		Set<Model> dbWriteModels = modelFactory.getModels(modelRepository.findByWriteUsernamesContains(user.getUsername()));
		Set<Model> dbReadModels = modelFactory.getModels(modelRepository.findByReadUsernamesContains(user.getUsername()));

		List<String> dbOwnModelIds = dbOwnModels.stream().map(Model::getId).collect(Collectors.toList());
		List<String> dbSharedOwnerModelIds = dbSharedOwnerModels.stream().map(Model::getId).collect(Collectors.toList());
		List<String> dbWriteModelIds = dbWriteModels.stream().map(Model::getId).collect(Collectors.toList());

		numModels = dbOwnModels.size() + dbWriteModels.size() + dbReadModels.size();

		logger.debug("Listing {} DB models for user {}", numModels, user.getUsername());
		for (Model m : dbOwnModels) {
			logger.debug("DB {}: [{}] {} ({})", counter, m.getId(), m.getName(), m.getUri());

			setModelNamesFromIds(m);
			
			models.add(new ModelDTO(m, true, true));
			counter++;
		}

		for (Model m : dbSharedOwnerModels) {
			// prevent showing duplicates in case a user somehow shares a model with themselves
			if (dbOwnModelIds.contains(m.getId())) {
				continue;
			}

			logger.debug("DB {}: [{}] {} ({})", counter, m.getId(), m.getName(), m.getUri());
			setModelNamesFromIds(m);
			
			models.add(new ModelDTO(m, true, true));
			counter++;
		}

		for (Model m : dbWriteModels) {
			// prevent showing duplicates in case a user ends up with both write and owner permissions
			if (dbOwnModelIds.contains(m.getId()) || 
					dbSharedOwnerModelIds.contains(m.getId())) {
				continue;
			}

			logger.debug("DB {}: [{}] {} ({})", counter, m.getId(), m.getName(), m.getUri());
			setModelNamesFromIds(m);
			
			models.add(new ModelDTO(m, true, false));
			counter++;
		}

		for (Model m : dbReadModels) {
			// prevent showing duplicates in case a user ends up with read, write and owner permissions
			if (dbOwnModelIds.contains(m.getId()) || 
					dbSharedOwnerModelIds.contains(m.getId()) ||
					dbWriteModelIds.contains(m.getId())) {
				continue;
			}

			logger.debug("DB {}: [{}] {} ({})", counter, m.getId(), m.getName(), m.getUri());
			setModelNamesFromIds(m);
			
			models.add(new ModelDTO(m, false, false));
			counter++;
		}
		return models;
	}

	/**
	 * Returns a list of models for the current user. 
	 *
	 * @return a list of ModelSummary objects, for models owned by the user
	 */
	@RequestMapping(value = "/models", method = RequestMethod.GET)
	public ResponseEntity<Set<Object>> listModels() {

		logger.info("Called REST method to GET models");

		UserRepresentation user = keycloakAdminClient.getCurrentUser();
		Set<Object> models = getModelObjectsForUser(user);

		return ResponseEntity.status(HttpStatus.OK).body(models);
	}

	/**
	 * Returns a list of models for a given user. (N.B. admin function only)
	 *
	 * @param userId
	 * @return a list of ModelSummary objects, for specified user (N.B. admin function only)
	 */
	@RequestMapping(value = "/usermodels/{userId}", method = RequestMethod.GET)
	public ResponseEntity<Set<Object>> listModelsForUser(@PathVariable String userId) {

		logger.info("Called REST method to GET models for user ID: {}", userId);

		UserRepresentation user = keycloakAdminClient.getUserById(userId);
		if (user == null) {
			throw new NotFoundErrorException("Unknown user: " + userId);
		}
		Set<Object> models = getModelObjectsForUser(user);
	
		return ResponseEntity.status(HttpStatus.OK).body(models);
	}

	/**
	 *  Send a request to create a new blank model for the user
	 *
	 * @param input the initial model used to create a persistent model in the backend.
	 * @return the persisted model object (internal id set), as a ModelSummary
	 */
	@RequestMapping(value = "/models", method = RequestMethod.POST)
	public ResponseEntity<ModelDTO> createModel(@RequestBody ModelDTO input) {

		logger.info("Called REST method to create model \"{}\"", input.getName());

		UserRepresentation user = keycloakAdminClient.getCurrentUser();

		String modelName = input.getName();
		String modelDescription = input.getDescription();
		String domainGraph = input.getDomainGraph();

		if (!keycloakAdminClient.currentUserHasRole(adminRole) && !modelObjectsHelper.canUserAccessDomain(domainGraph, user.getUsername())) {
			throw new UserForbiddenFromDomainException();
		}

		Model model = modelFactory.createModel(domainGraph, user.getId());

		if (model == null) {
			throw new InternalServerErrorException("Failed to save model in RDF store");
		}
		
		model.setName(modelName);
		model.setDescription(modelDescription);
		model.save();

		logger.info("Created model: {}", model);
		
		setModelNamesFromIds(model);
		ModelDTO modelContainer = new ModelDTO(model, true, true);

		return ResponseEntity.status(HttpStatus.CREATED).body(modelContainer);
	}

	/**
	 * Copy a source model to a new copy of that model
	 * 
	 * @param modelId the webkey of the model to copy
	 * @param input the initial model used to create a persistent model in the backend (just the name and description fields are used).
	 * @return the persisted model object (internal id set), as a ModelSummary
	 */
	@RequestMapping(value = "/models/{modelId}/copyModel", method = RequestMethod.POST)
	public ResponseEntity<ModelDTO> copyModel(@PathVariable String modelId, @RequestBody ModelDTO input) {

		logger.info("Called REST method to copy model to new name: \"{}\"", input.getName());

		UserRepresentation user = keycloakAdminClient.getCurrentUser();
		final Model sourceModel = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

		// Load model info (metadata) for the source model
		sourceModel.loadModelInfo();

		// Get new name and description from the request
		String modelName = input.getName();
		String modelDescription = input.getDescription();

		// Override source model name and description
		sourceModel.setName(modelName);
		sourceModel.setDescription(modelDescription);

		// The copied model should be in the same domain graph as the original
		String domainGraph = sourceModel.getDomainGraph();

		if (!keycloakAdminClient.currentUserHasRole(adminRole) && !modelObjectsHelper.canUserAccessDomain(domainGraph, user.getUsername())) {
			throw new UserForbiddenFromDomainException();
		}

		// Copy triples from the source graph(s) into the new copy
		Model model = modelFactory.createModelForCopy(sourceModel.getUri(), domainGraph, user.getId());

		if (model == null) {
			throw new InternalServerErrorException("Failed to save model in RDF store");
		}
		
		// Update the new model metadata from the source metadata (redundant triples will be deleted)
		model.updateCopiedModelInfo(sourceModel.getModelInfo());

		// Re-load the updated metadata into the new model
		model.loadModelInfo();

		logger.info("Created model: {}", model);
		
		setModelNamesFromIds(model);

		ModelDTO modelContainer = new ModelDTO(model, true, true);

		return ResponseEntity.status(HttpStatus.CREATED).body(modelContainer);
	}

	/**
	 * Return the model info for the given ID.
	 *
	 * @param modelId the String representation of the model object to fetch
	 * @param servletRequest
	 * @return the FullModel instance
	 * @throws java.rmi.UnexpectedException
	 */
	@RequestMapping(value = "/models/{modelId}", method = RequestMethod.GET)
	public ResponseEntity<ModelDTO> getModel(@PathVariable String modelId, HttpServletRequest servletRequest) throws UnexpectedException {

		logger.info("Called REST method to GET model {}", modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);
		logger.info("Located model: {}, <{}>", model.getId(), model.getUri());

		UUID loadingUUID = UUID.randomUUID();
		String loadingID = loadingUUID.toString();

		LoadingProgress loadingProgress = modelObjectsHelper.createLoadingProgressOfModel(model, loadingID);
		loadingProgress.updateProgress(0.0, "Loading model");

		ScheduledFuture<?> future = Executors.newScheduledThreadPool(1).schedule(() -> {

			model.loadModelData(modelObjectsHelper, loadingProgress);

			if (model.getAssets().size() > 0) {
				// finally, check if misbahaviours are missing
				if (!model.getThreats().isEmpty() && model.getMisbehaviourSets().isEmpty()) {
					throw new ModelException("Model has missing misbehaviour sets. This may be due to importing an older system model which is not compatible with the latest software. You may still view the model, however certain functions may not work. Please contact support for further assistance.", model);
				}
			}

			setModelNamesFromIds(model);

			return model;
		}, 0, TimeUnit.SECONDS);

		modelObjectsHelper.registerLoadingExecution(loadingUUID.toString(), future);

		ModelDTO responseModel = new ModelDTO(model);
		responseModel.setLoadingId(loadingID); // return the loading id to the client, for subsequent progress checks
		logger.debug("Set loadingID: {}", loadingID);

		// return basic model details, while assets, etc are still being loaded
		return ResponseEntity.status(HttpStatus.OK).body(responseModel);
	}

	/**
	 * Gets the basic model details
	 *
	 * @param modelId the String representation of the model object to fetch
	 * @param servletRequest
	 * @return the FullModel instance
	 * @throws java.rmi.UnexpectedException
	 */
	@RequestMapping(value = "/models/{modelId}/info", method = RequestMethod.GET)
	public ResponseEntity<ModelDTO> getModelInfo(@PathVariable String modelId, HttpServletRequest servletRequest) throws UnexpectedException {

		logger.info("Called REST method to GET model info {}", modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

		//Get basic model details only
		model.loadModelInfo(modelObjectsHelper);

		logger.debug("Returning basic model info: {}", model);
					
		setModelNamesFromIds(model);

		ModelDTO responseModel = new ModelDTO(model);

		return ResponseEntity.status(HttpStatus.OK).body(responseModel);
	}


	/**
	 * Gets the basic model details and risks data (only)
	 *
	 * @param modelId the String representation of the model object to fetch
	 * @param servletRequest
	 * @return the FullModel instance
	 * @throws java.rmi.UnexpectedException
	 */
	@RequestMapping(value = "/models/{modelId}/risks", method = RequestMethod.GET)
	public ResponseEntity<ModelDTO> getModelAndRisks(@PathVariable String modelId, HttpServletRequest servletRequest) throws UnexpectedException {

		logger.info("Called REST method to GET model risks {}", modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

		//Get basic model details plus risks data (i.e. misbehaviours and assets)
		model.loadModelAndRisksData(modelObjectsHelper);

		logger.debug("Returning model and risks data: {}", model);
					
		setModelNamesFromIds(model);

		ModelDTO responseModel = new ModelDTO(model);

		return ResponseEntity.status(HttpStatus.OK).body(responseModel);
	}

	/**
	 * Get risk vector for model (breakdown of numbers of misbehaviours at each risk level)
	 *
	 * @param modelId the String representation of the model object to fetch
	 * @param servletRequest
	 * @return the RiskVector instance (map of risk URI to RiskLevelCount objects)
	 * @throws java.rmi.UnexpectedException
	 */
	@RequestMapping(value = "/models/{modelId}/riskvector", method = RequestMethod.GET)
	public ResponseEntity<Map<String, RiskLevelCount>> getModelRiskVector(@PathVariable String modelId, HttpServletRequest servletRequest) throws UnexpectedException {

		logger.info("Called REST method to GET model risk vector {}", modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

		RiskVector riskVector = modelObjectsHelper.getModelRiskVector(model);
		logger.info("Returning risk vector: {}", riskVector != null ? riskVector.toString() : null);
		
		Map<String, RiskLevelCount> riskVectorResponse = riskVector != null ? riskVector.getRiskVector() : null;

		return ResponseEntity.status(HttpStatus.OK).body(riskVectorResponse);
	}

	/**
	 * // TODO -- javadoc
	 *
	 * @param objId the String representation of the model object to fetch
	 */
	@RequestMapping(value = "/models/{objid}/palette", method = RequestMethod.GET)
	// TODO convert all but the successful return statement to exceptions, then set explicit return type
	public ResponseEntity<Map<?, ?>> getPalette(@PathVariable String objid) {

		ObjectMapper objectMapper = new ObjectMapper();
		Map<?, ?> map;
		
		final Model model = secureUrlHelper.getModelFromUrlThrowingException(objid, WebKeyRole.READ);
		
		String ontology = model.getDomainGraph().substring(model.getDomainGraph().lastIndexOf("/")+1);
		
		try {
			String paletteFile = "palette.json";
			Path palettePath = Paths.get(kbInstallFolder, ontology, paletteFile);
			File palette = palettePath.toFile();
			logger.info("Loading palette file: {}", palette.getAbsolutePath());

			map = objectMapper.readValue(palette, Map.class);
		} catch (IOException e) {
			logger.error("Could not read palette", e);
			throw new NotFoundErrorException("Could not load palette");
		}
		return ResponseEntity.ok().body(map);
	}

	@RequestMapping(value = "/images/{domainModel}/{image}" , method = RequestMethod.GET) 
    public ResponseEntity<FileSystemResource> getImage(@PathVariable String domainModel, @PathVariable String image) throws IOException {
		Path imagePath = Paths.get(kbInstallFolder, domainModel, "icons", image);
		FileSystemResource resource = new FileSystemResource(imagePath);

		return ResponseEntity.ok()
		.contentType(MediaType.parseMediaType(Files.probeContentType(imagePath)))
		.body(resource);
    }

	/**
	 * This method forces a checkout even if another user is currently editing a model, as for example
	 * when a user chooses the option to take over editing of a model. 
	 *
	 * @param objid the String representation of the model object to check out
	 * @return OK/not OK in header, body with the checked out model object
	 */
	@RequestMapping(value = "/models/{objid}/checkout", method = RequestMethod.POST)
	public ResponseEntity<ModelDTO> checkoutModel(@PathVariable String objid) {

		logger.info("Called REST method to checkout model {}", objid);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(objid, WebKeyRole.WRITE, true, false);

		logger.debug("checkout objid = {}", objid);
		setModelNamesFromIds(model);
		ModelDTO responseModel = new ModelDTO(model);
		return ResponseEntity.status(HttpStatus.OK).body(responseModel);
	}

	/**
	 * This method checks in a model if the user is the current editor for that model 
	 *
	 * @param objid the String representation of the model object to check in
	 * @return OK/not OK in header, body with the checked in model object
	 */
	@RequestMapping(value = "/models/{objid}/checkin", method = RequestMethod.POST)
	public ResponseEntity<ModelDTO> checkinModel(@PathVariable String objid) {

		logger.info("Called REST method to checkin model {}", objid);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(objid, WebKeyRole.WRITE, false, true);

		model.setEditorId(null);
		model.save();

		logger.debug("checkin objid = {}", objid);
		setModelNamesFromIds(model);
		ModelDTO responseModel = new ModelDTO(model);
		return ResponseEntity.status(HttpStatus.OK).body(responseModel);
	}

	/** 
	 *  Update a model given the model parameters and ID.
	 *
	 * @param modelid
	 * @param updated the model to update in the persistence layer in the backend.
	 * @return the persisted model object
	 */
	@RequestMapping(value = "/models/{modelid}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<UpdateModelResponse> updateModel(@PathVariable String modelid, @RequestBody ModelDTO updated) {

		logger.info("Called REST method to PUT model {}", modelid);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelid, WebKeyRole.WRITE);

		//update model in mongo (N.B. these fields are technically redundant as also stored in model info below)
		model.setName(updated.getName() != null ? updated.getName() : model.getName());
		model.setDescription(updated.getDescription() != null ? updated.getDescription() : model.getDescription());

		model.save();
		
		//update model in its own graph
		storeModelManager.getStore().update("DELETE { GRAPH <" + model.getUri() + "> {\n" +
		"	<" + model.getUri() + "> rdfs:label ?l ." +
		"}} INSERT { GRAPH <" + model.getUri() + "> {\n" +
		"	<" + model.getUri() + "> rdfs:label \"" + SparqlHelper.escapeLiteral(model.getName()) + "\" ." +
		"}} WHERE { GRAPH <" + model.getUri() + "> {\n" +
		"	<" + model.getUri() + "> rdfs:label ?l ." +
		"}}");
		
		return ResponseEntity.status(HttpStatus.OK).body(new UpdateModelResponse(model));
	}

	/**
	 *  Initiate a validation operation for the model given the ID of the model.
	 * 
	 * @param modelWriteId write id for the validated model object to fetch
	 * @return the FullModel instance, including any added inferred assets, etc
	 * @throws java.rmi.UnexpectedException
	 */
	@RequestMapping(value = "/models/{modelWriteId}/validated", method = RequestMethod.GET)
	public ResponseEntity<String> validateModel(@PathVariable String modelWriteId) throws UnexpectedException {

		logger.info("Called REST method to GET validated model {}", modelWriteId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelWriteId, WebKeyRole.WRITE);

		String modelId = model.getId();

		if (model.isValidating()) {
			logger.warn("Model {} is already validating - ignoring request {}", modelId, modelWriteId);
			return new ResponseEntity<>(HttpStatus.ACCEPTED);
		}

		if (model.isCalculatingRisks()) {
			logger.warn("Model {} is already calculating risks - ignoring request {}", modelId, modelWriteId);
			return new ResponseEntity<>(HttpStatus.ACCEPTED);
		}

		Progress validationProgress = modelObjectsHelper.getValidationProgressOfModel(model);
		validationProgress.updateProgress(0d, "Validation starting");

		logger.debug("Marking as validating model [{}] {}", modelId, model.getName());
		model.markAsValidating();

		ScheduledFuture<?> future = Executors.newScheduledThreadPool(1).schedule(() -> {
			boolean valid = false;

			try {
				//drop previous inference model:
				//required because it might contain references to assets/relations that no longer exist
				logger.debug("Deleting inferred model model");
				validationProgress.updateProgress(0.1, "Deleting inferred model");
				storeModelManager.deleteInferredModel(model.getUri());

				//run validation
				logger.debug("Calling model validator");
				validationProgress.updateProgress(0.2, "Validating design time model");
				ModelValidator validator = modelObjectsHelper.getModelValidatorForModel(model);
				validator.validateDesigntimeModel(validationProgress);

				valid = true;
			} catch(Throwable t) {
				logger.error("Validation failed:", t);
				throw new Exception("Validation failed. Please contact support for further assistance.");
			} finally {
				//always reset the flags even if validation crashes
				model.finishedValidating(valid);
				validationProgress.updateProgress(1.0, "Validation complete");
			}

			return true;
		}, 0, TimeUnit.SECONDS);

		modelObjectsHelper.registerValidationExecution(modelId, future);

		return new ResponseEntity<>(HttpStatus.ACCEPTED);
	}

	/**
	 *  Clear inferred graph for a given model.
	 * 
	 * @param modelWriteId write id for the model
	 */
	@RequestMapping(value = "/models/{modelWriteId}/clear_inferred_graph", method = RequestMethod.POST)
	public ResponseEntity<String>  clearInferredGraphForModel(@PathVariable String modelWriteId) {

		logger.info("Called REST method to clear inferred graph {}", modelWriteId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelWriteId, WebKeyRole.WRITE);

		//clear inferred graph
		modelObjectsHelper.clearInferredGraph(model);

		//clear validity flags
		model.invalidate();

		return ResponseEntity.status(HttpStatus.OK).build();
	}

	/**
	 *  Initiate a risk calculation operation for the given model, as a background thread.
	 *
	 * @param modelWriteId write id for the model
	 * @param mode the risk calculation mode to use ("CURRENT" or "FUTURE")
	 * @return
	 * @throws java.rmi.UnexpectedException
	 */
	@RequestMapping(value = "/models/{modelWriteId}/calc_risks", method = RequestMethod.GET)
	public ResponseEntity<String> calculateRisks(@PathVariable String modelWriteId, @RequestParam() String mode) throws UnexpectedException {

		logger.info("Called REST method to calculate {} risks for model {}", mode, modelWriteId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelWriteId, WebKeyRole.WRITE);

		String modelId = model.getId();

		if (model.isValidating()) {
			logger.warn("Model {} is currently validating - ignoring calc risks request {}", modelId, modelWriteId);
			return new ResponseEntity<>(HttpStatus.ACCEPTED);
		}

		if (model.isCalculatingRisks()) {
			logger.warn("Model {} is already calculating risks - ignoring request {}", modelId, modelWriteId);
			return new ResponseEntity<>(HttpStatus.ACCEPTED);
		}

		RiskCalculationMode rcMode;
		mode = mode != null ? mode.toUpperCase() : "FUTURE";
		try {
			rcMode = RiskCalculationMode.valueOf(mode);
		} catch (IllegalArgumentException e) {
			logger.error("Found unexpected riskCalculationMode parameter value {}, valid values are: {}.",
					mode, RiskCalculationMode.values());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
					"Invalid 'mode' parameter value " + mode + " , valid values are: " +
							RiskCalculationMode.values());
		}
		
        Progress validationProgress = modelObjectsHelper.getTaskProgressOfModel("Risk calculation", model);
		validationProgress.updateProgress(0d, "Risk calculation starting");
		
		logger.debug("Marking as calculating risks [{}] {}", modelId, model.getName());
		model.markAsCalculatingRisks(rcMode, true);

		ScheduledFuture<?> future = Executors.newScheduledThreadPool(1).schedule(() -> {
			//boolean valid = false;
			RiskCalcResultsDB results = null;

			try {
				ModelValidator validator = modelObjectsHelper.getModelValidatorForModel(model);
				logger.info("Calculating risks [{}] {}", modelId, model.getName());
				results = validator.calculateRiskLevels(rcMode, true, validationProgress); //save results
			} catch(Throwable t) {
				logger.error("Risk calculation failed:", t);
				throw new Exception("Risk calculation failed. Please contact support for further assistance.");
			} finally {
				//always reset the flags even if the risk calculation crashes
				model.finishedCalculatingRisks(results != null, rcMode, true);
				validationProgress.updateProgress(1.0, "Risk calculation complete");
			}

			return true;
		}, 0, TimeUnit.SECONDS);

		modelObjectsHelper.registerValidationExecution(modelId, future);

		return new ResponseEntity<>(HttpStatus.ACCEPTED);
	}

	/**
	 * Run a risk calculation for a model, as a blocking call, 
	 * returning results in the response.
	 * 
	 * @param modelWriteId write id for the model
	 * @param mode the risk calculation mode to use ("CURRENT" or "FUTURE")
	 * @param save whether the results should be saved before returning
	 * @return risk calculation results
	 * @throws java.rmi.UnexpectedException
	 */
	@RequestMapping(value = "/models/{modelWriteId}/calc_risks_blocking", method = RequestMethod.GET)
	public ResponseEntity<RiskCalcResultsDB> calculateRisksBlocking(@PathVariable String modelWriteId, @RequestParam() String mode, @RequestParam() boolean save) throws UnexpectedException {

		logger.info("Called REST method to calculate {} risks for model {}", mode, modelWriteId);
		logger.info("Save = {}", save);

		RiskCalculationMode rcMode;
		mode = mode != null ? mode.toUpperCase() : "FUTURE";
		
		try {
			rcMode = RiskCalculationMode.valueOf(mode);
		} catch (IllegalArgumentException e) {
			logger.error("Found unexpected riskCalculationMode parameter value {}, valid values are: {}.",
					mode, RiskCalculationMode.values());
			throw new BadRequestErrorException(
					"Invalid 'mode' parameter value " + mode + " , valid values are: " +
							RiskCalculationMode.values());
							
		}

		final Model model;
		Progress validationProgress;

		synchronized(this) {
			if (save) {
				model = secureUrlHelper.getModelFromUrlThrowingException(modelWriteId, WebKeyRole.WRITE);
			} else {
				// If we are not saving the risk calculation then we permit it with read-only AuthZ
				// It's not completely clear that this is correct as there may be some side-effect that we've overlooked but it will do for now.
				model = secureUrlHelper.getModelFromUrlThrowingException(modelWriteId, WebKeyRole.READ);
			}

			String modelId = model.getId();

			if (model.isValidating()) {
				logger.warn("Model {} is currently validating - ignoring calc risks request {}", modelId, modelWriteId);
				return ResponseEntity.status(HttpStatus.OK).body(new RiskCalcResultsDB()); //TODO: may need to improve this
			}

			if (model.isCalculatingRisks()) {
				logger.warn("Model {} is already calculating risks - ignoring request {}", modelId, modelWriteId);
				return ResponseEntity.status(HttpStatus.OK).body(new RiskCalcResultsDB()); //TODO: may need to improve this
			}

			validationProgress = modelObjectsHelper.getTaskProgressOfModel("Risk calculation", model);
			validationProgress.updateProgress(0d, "Risk calculation starting");
			
			logger.debug("Marking as calculating risks [{}] {}", modelId, model.getName());
			model.markAsCalculatingRisks(rcMode, save);
		} //synchronized block

		RiskCalcResultsDB results = null;

		try {
			ModelValidator validator = modelObjectsHelper.getModelValidatorForModel(model);
			logger.info("Calculating risks [{}] {}", model.getId(), model.getName());
			results = validator.calculateRiskLevels(rcMode, save, validationProgress);
		} catch(Throwable t) {
			logger.error("Risk calculation failed:", t);
			throw new InternalServerErrorException("Risk calculation failed. Please contact support for further assistance.");
		} finally {
			//always reset the flags even if the risk calculation crashes
			model.finishedCalculatingRisks(results != null, rcMode, save);
			validationProgress.updateProgress(1.0, "Risk calculation complete");
		}
		
		return ResponseEntity.status(HttpStatus.OK).body(results);
	}

	/**
	 * Delete the model given the ID. 
	 *
	 * @param modelid
	 * @return an OK response
	 */
	@RequestMapping(value = "/models/{modelid}", method = RequestMethod.DELETE)
	@ResponseBody
	public ResponseEntity<String> deleteModel(@PathVariable String modelid) {

		logger.info("Called REST method to DELETE model {}", modelid);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelid, WebKeyRole.OWNER);

		logger.warn("Deleting model [{}] with uri <{}>", modelid, model.getUri());

		model.delete();

		return ResponseEntity.status(HttpStatus.OK).build();
	}

	/**
	 * This REST method exports all graphs for a model as n-quads (.nq.gz format)
	 *
	 * @param modelId the String representation of the validated model object to fetch
	 * @return the serialised model
	 * @throws java.rmi.UnexpectedException
	 */
	@RequestMapping(value = "/models/{modelId}/export", method = RequestMethod.GET)
	public ResponseEntity<ByteArrayResource> export(@PathVariable String modelId) throws UnexpectedException {

		logger.info("Called REST method to GET serialised model {}", modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

		//get quads from store
		String quads = storeModelManager.getStore().export(
				IStoreWrapper.Format.NQ,
				null,
				model.getUri(),
				storeModelManager.getInferredModel(model.getUri()),
				storeModelManager.getUIModel(model.getUri()),
				storeModelManager.getMetaModel(model.getUri())
		);

		ByteArrayResource resource = null;

		//ZIP the quads file into gzipped data
		try{
			ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
			GZIPOutputStream zippedOutput = new GZIPOutputStream(byteOutput);

			zippedOutput.write(quads.getBytes("UTF-8"));
			zippedOutput.close();

			//prepare response (zipped quads in textfile to download)
			resource = new ByteArrayResource(byteOutput.toByteArray());

		} catch (IOException e){

			logger.error(e.getMessage());

		}

		HttpHeaders headers = new HttpHeaders();
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");
		headers.add("Content-disposition", "attachment;filename=" + model.getName() + " " +
				(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm")).format(new Date()) + ".nq.gz");

		return ResponseEntity.status(HttpStatus.OK).headers(headers).contentType(MediaType.TEXT_PLAIN).body(resource);
	}

	/**
	 * This REST method exports all asserted graphs for a model as n-quads (.nq.gz format)
	 *
	 * @param modelId the String representation of the validated model object to fetch
	 * @return the serialised model
	 * @throws java.rmi.UnexpectedException
	 */
	@RequestMapping(value = "/models/{modelId}/exportAsserted", method = RequestMethod.GET)
	public ResponseEntity<ByteArrayResource> exportAsserted(@PathVariable String modelId) throws UnexpectedException {

		logger.info("Called REST method to GET serialised model {} (asserted facts only)", modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);
		
		//As the exported model will not have inferred data, we need to set the valid flag to false
		//therefore, we set this temporarily (if necessaey), then set it back again after the export

		logger.debug("Model valid: {}", model.isValid());
		
		boolean resetValidFlag = false;
		boolean resetRiskLevelsFlag = false;
		
		if (model.isValid()) {
			logger.debug("Model is valid (temporarily invalidating for export)");
			resetValidFlag = true;
			if (model.riskLevelsValid()) {
				resetRiskLevelsFlag = true;
			}
			model.invalidate();
		}
		else {
			logger.debug("Model is already invalid");
		}

		//get quads from store
		String quads = storeModelManager.getStore().export(
				IStoreWrapper.Format.NQ,
				null,
				model.getUri(),
				storeModelManager.getUIModel(model.getUri())
		);

		ByteArrayResource resource = null;

		//ZIP the quads file into gzipped data
		try{
			ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
			GZIPOutputStream zippedOutput = new GZIPOutputStream(byteOutput);

			zippedOutput.write(quads.getBytes("UTF-8"));
			zippedOutput.close();
			//prepare response (quads in textfile to download)
			resource = new ByteArrayResource(byteOutput.toByteArray());

		} catch (IOException e){
			logger.error(e.getMessage());
		}

		HttpHeaders headers = new HttpHeaders();
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");
		headers.add("Content-disposition", "attachment;filename=" + model.getName() + " asserted " +
				(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm")).format(new Date()) + ".nq.gz");
		
		//If we have temporarily invalidated the model above, reset the flag to true
		if (resetValidFlag) {
			model.setValid(true);
			if (resetRiskLevelsFlag) {
				model.setRiskLevelsValid(true);
			}
			model.save();
		}

		return ResponseEntity.status(HttpStatus.OK).headers(headers).contentType(MediaType.TEXT_PLAIN).body(resource);
	}

	/*
	@RequestMapping(value = "/models/{modelId}/db/export", method = RequestMethod.GET)
	public ResponseEntity<ModelExportDB> exportDB(@PathVariable String modelId) throws UnexpectedException {

		logger.info("Called REST method to GET serialised model DB {}", modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);
		AStoreWrapper store = storeModelManager.getStore();
		
		ModelExportDB results = new ModelExportDB();
		try{
			// Disposable non-caching querier, used for set up that should be persisted before anything else
			JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(), model.getModelStack(), false);
			querierDB.init();

			// Populate the return object
			results.setModel(querierDB.getModelInfo("system"));
			results.setAssets(querierDB.getAssets("system","system-inf"));
			results.setRelationships(querierDB.getCardinalityConstraints("system","system-inf"));
			results.setTrustworthinessAttributeSets(querierDB.getTrustworthinessAttributeSets("system","system-inf"));
			results.setMisbehaviourSets(querierDB.getMisbehaviourSets("system","system-inf"));
			results.setControlSets(querierDB.getControlSets("system","system-inf"));
			results.setThreats(querierDB.getThreats("system","system-inf"));
			results.setControlStrategies(querierDB.getControlStrategies("system","system-inf"));

		} catch(Throwable t) {
			logger.error("ModelExportDB failed:", t);
			throw new InternalServerErrorException("ModelExportDB failed. Please contact support for further assistance.");
		}

		return ResponseEntity.status(HttpStatus.OK).body(results);

	}
	*/
	
	/**
	 *  Import a model model in .nq.gz or .nq format. returns list of models for the user
	 *
	 * @param file
	 * @param asserted
	 * @param overwrite
	 * @param newName
	 * @param redirectAttributes
	 * @return SUCCESS/ERROR
	 * @throws org.apache.tomcat.util.http.fileupload.impl.SizeLimitExceededException
	 */
	@RequestMapping(value = "/models/import", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Set<Object>> importModel(@RequestParam("file") MultipartFile file, @RequestParam("asserted") boolean asserted,
										 @RequestParam("overwrite") boolean overwrite,
										 @RequestParam(value = "newName", required = false) String newName,
										 RedirectAttributes redirectAttributes) throws SizeLimitExceededException {

		logger.info("Called REST method to POST serialised model {}, size {}", file.getOriginalFilename(), file.getSize());

		UserRepresentation user = keycloakAdminClient.getCurrentUserThrowingException();

		String domainGraph = null;

		if (!file.isEmpty()) {
			logger.debug("Uploaded file {}, size {}", file.getOriginalFilename(), file.getSize());

			//parse quads to find URIs for system, ui and inf graphs
			String oldGraphURI = null;
			try {
				InputStream inputStream;
				BufferedReader bufferedReader;

				//need to decompress before parsing!
				if (file.getOriginalFilename().endsWith("gz")) {
					inputStream = new GZIPInputStream(file.getInputStream());
					Reader decoder = new InputStreamReader(inputStream, "UTF-8");
					bufferedReader = new BufferedReader(decoder);
				} else {
					inputStream = file.getInputStream();
					bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
				}

				//every line line contains one of the system model graphs at the end
				String line = bufferedReader.readLine();

				boolean validDomain = false;
				boolean canAccessAllDomains = keycloakAdminClient.currentUserHasRole(adminRole);

				if (!line.startsWith("<http://it-innovation.soton.ac.uk")) {
					throw new NotAcceptableErrorException("Incompatible file uploaded");
				}
				do {
					if (oldGraphURI==null) {
						oldGraphURI = line.substring(line.lastIndexOf("<") + 1).replace("> .", "");
						if (oldGraphURI.endsWith(StoreModelManager.UI_GRAPH)) {
							oldGraphURI = oldGraphURI.replace(StoreModelManager.UI_GRAPH, "");
						}
						if (oldGraphURI.endsWith(StoreModelManager.INF_GRAPH)) {
							oldGraphURI = oldGraphURI.replace(StoreModelManager.INF_GRAPH, "");
						}
						if (oldGraphURI.endsWith(StoreModelManager.META_GRAPH)) {
							oldGraphURI = oldGraphURI.replace(StoreModelManager.META_GRAPH, "");
						}
					}
					
					if (!validDomain && line.contains("<http://it-innovation.soton.ac.uk/ontologies/trustworthiness/core#domainGraph>")) {
						String domainURI = line.substring(0, line.lastIndexOf("<")-1);
						domainURI = domainURI.substring(domainURI.lastIndexOf("<") + 1, domainURI.lastIndexOf(">"));
						logger.debug("Model domain: {}", domainURI);

						boolean domainModelExists = storeModelManager.domainModelExists(domainURI);

						if (!domainModelExists) {
							logger.error("The system model attempted to use non-existent domain: {}", domainURI);
							throw new UnprocessableEntityException("The system model requires a knowledgebase that is not installed: " + domainURI);
						}

						if (!canAccessAllDomains && !modelObjectsHelper.canUserAccessDomain(domainURI, user.getUsername())) {
							logger.info("User: {} blocked from importing model with domain {}", user.getUsername(), domainURI);
							throw new UserForbiddenFromDomainException();
						}

						validDomain = true;
						domainGraph = domainURI;
					}

					if (oldGraphURI!=null && validDomain){
						break;
					}
				} while ((line = bufferedReader.readLine()) != null);
			} catch (IOException e) {
				logger.error("Could not parse uploaded file", e);
				throw new NotAcceptableErrorException("Incompatible file uploaded");
			}

			if (oldGraphURI==null) {
				logger.error("Could not find graph URI in uploaded file");
				throw new NotAcceptableErrorException("Incompatible graph type");
			}

			Model model = modelFactory.createModelForImport(oldGraphURI, domainGraph, user.getId());

			String newGraphURI = model.getUri();

			//create temporary file to load model into triple store from
			try {
				//write string to file
				File toImport = File.createTempFile("system-modeller-system-", file.getOriginalFilename().endsWith(".gz")?".nq.gz":".nq");
				try {
					file.transferTo(toImport);
				} catch (IOException e) {
					logger.error("Could not upload domain model", e);
				}

				//call import method
				logger.debug("Loading model into triple store");
				if (oldGraphURI.equals(newGraphURI)) {
					storeModelManager.getStore().load(toImport.getAbsolutePath());
				} else {
					storeModelManager.getStore().load(toImport.getAbsolutePath(), oldGraphURI, newGraphURI);
				}
				logger.debug("Done loading model into triple store");
				//delete temporary file
				toImport.delete();

				//update graph URI if it has changed
				if (!oldGraphURI.equals(newGraphURI)) {
					logger.info("Changing imported model URI from {} to {}", oldGraphURI, newGraphURI);
					storeModelManager.changeSystemModelURI(newGraphURI, oldGraphURI, newGraphURI);
				}
			} catch (IOException | IllegalStateException ex) {
				logger.error("Could not load file contents into store", ex);

				//tidy up
				storeModelManager.deleteSystemModel(newGraphURI);

				throw new InternalServerErrorException("Failed to load model into RDF store");
			}

			//now that the model graphs have been stored in Jena read the model info (status flags etc) from Jena
			model.loadModelInfo();

			//update the model info with any new model name
			if (newName != null) {
				logger.info("Storing model name: {}", newName);
				model.setName(newName);
				model.save();
			}

			logger.info("Cleaning asserted model: " + asserted);
			if(asserted) {
				//remove references to inferred assets from asserted graph
				model.getUpdater().cleanAssertedModel(storeModelManager.getStore());
				//drop inference graph
				storeModelManager.deleteInferredModel(newGraphURI);

				//clear validity flags
				model.invalidate();
			}

			logger.info("Imported model: {}", model);

			//tell the user
			redirectAttributes.addFlashAttribute("message",
				file.getOriginalFilename() + " (" + file.getSize() + ") was uploaded successfully");

		} else {
			redirectAttributes.addFlashAttribute("message",
				"Failed to upload " + file.getOriginalFilename() + " because it was empty");
		}

		return listModels();
	}

	/**
	 *  Get an update on the validation operation running the model given the ID of the model.
	 *
	 * @param modelId
	 * @return validation progress
	 * @throws java.rmi.UnexpectedException
	 */
	@RequestMapping(value = "/models/{modelId}/validationprogress", method = RequestMethod.GET)
	public ResponseEntity<Progress> getValidationProgress(@PathVariable String modelId) throws UnexpectedException {
		logger.info("Called REST method to GET validation progress for model {}", modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);
		return ResponseEntity.status(HttpStatus.OK).body(modelObjectsHelper.getValidationProgressOfModel(model));
	}

	/**
	 *  Get an update on the risk calculation operation running the model given the ID of the model.
	 *
	 * @param modelId
	 * @return risk calculation progress
	 * @throws java.rmi.UnexpectedException
	 */
	@RequestMapping(value = "/models/{modelId}/riskcalcprogress", method = RequestMethod.GET)
	public ResponseEntity<Progress> getRiskCalcProgress(@PathVariable String modelId) throws UnexpectedException {
		logger.info("Called REST method to GET risk calculation progress for model {}", modelId);

		synchronized(this) {
			final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);
			return ResponseEntity.status(HttpStatus.OK).body(modelObjectsHelper.getTaskProgressOfModel("Risk calculation", model));
		}
	}

	/**
	 *  Get an update on the progress of the recommendations operation, given the ID of the model.
	 *
	 * @param modelId
	 * @return recommendations progress
	 * @throws java.rmi.UnexpectedException
	 */
	@GetMapping(value = "/models/{modelId}/recommendationsprogress")
	public ResponseEntity<Progress> getRecommendationsProgress(@PathVariable String modelId) throws UnexpectedException {
		logger.info("Called REST method to GET recommendations progress for model {}", modelId);

		synchronized(this) {
			final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);
			Progress progress = modelObjectsHelper.getTaskProgressOfModel("Recommendations", model);
			logger.info("{}", progress);
			return ResponseEntity.status(HttpStatus.OK).body(progress);
		}
	}

	/**
	 *  Get an update on loading the model given the ID and loadingID of the model.
	 *
	 * @param modelId
	 * @param loadingID unique id of the model loading task
	 * @param servletRequest
	 * @return loading progress
	 * @throws java.rmi.UnexpectedException
	 */
	@RequestMapping(value = "/models/{modelId}/{loadingID}/loadingprogress", method = RequestMethod.GET)
	public ResponseEntity<LoadingProgressResponse> getLoadingProgress(@PathVariable String modelId, @PathVariable String loadingID, HttpServletRequest servletRequest) throws UnexpectedException {

		logger.info("Called REST method to GET loading progress for model");

		secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

		if (loadingID.equals("undefined")) {
			logger.error("Undefined loadingID in URL");
			throw new BadRequestErrorException("Undefined loadingID in URL");
		}

		LoadingProgress loadingProgress = modelObjectsHelper.getLoadingProgressOfModel(loadingID);

		if (loadingProgress == null) {
			logger.error("Cannot get loading progress for id: " + loadingID);
			throw new ModelInvalidException("Unknown model loading id: " + loadingID);
		}

		ModelDTO responseModel = null;

		// get updated model (if available)
		Model model = loadingProgress.getModel();

		if (model != null) {
			responseModel = new ModelDTO(model);
		}

		LoadingProgressResponse loadingProgressResponse = new LoadingProgressResponse(loadingProgress, responseModel);

		return ResponseEntity.status(HttpStatus.OK).body(loadingProgressResponse);
	}

	/**
	 * This REST method generates a JSON report from the given model
	 *
	 * @param modelId
	 * @return A JSON report
	 */
	@RequestMapping(value = "/models/{modelId}/report", method = RequestMethod.GET)
	public ResponseEntity<Object> generateReport(@PathVariable String modelId) {
		logger.info("Called REST method to GET report for model {}", modelId);

		final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

		ReportGenerator reportGenerator = new ReportGenerator();
		String reportJson = reportGenerator.generate(modelObjectsHelper, model);
		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(reportJson);
	}

	/**
	 * This REST method generates a JSON representation of the shortest attack
     * path for the given model and target URIs
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @param riskMode string indicating the prefered risk calculation mode
	 * @param allPaths flag indicating whether to calculate all paths
	 * @param normalOperations flag indicationg whether to include normal operations
	 * @param targetURIs list of target misbehaviour sets
	 * @return A JSON report containing the attack tree
     * @throws MisbehaviourSetInvalidException if an invalid target URIs set is provided
     * @throws InternalServerErrorException   if an error occurs during report generation
	 */
	@RequestMapping(value = "/models/{modelId}/threatgraph", method = RequestMethod.GET)
	public ResponseEntity<TreeJsonDoc> calculateThreatGraph(
            @PathVariable String modelId,
            @RequestParam(defaultValue = "FUTURE") String riskMode,
            @RequestParam(defaultValue = "false") boolean allPaths,
            @RequestParam(defaultValue = "false") boolean normalOperations,
            @RequestParam List<String> targetURIs) {

        logger.info("Calculating threat graph for model {}", modelId);
        logger.info(" with target URIs: {}, all-paths: {}, normal-operations: {} riskMode: {}",
                targetURIs, allPaths, normalOperations, riskMode);

		try {
            RiskCalculationMode.valueOf(riskMode);
		} catch (IllegalArgumentException e) {
			logger.error("Found unexpected riskCalculationMode parameter value {}, valid values are: {}.",
					riskMode, RiskCalculationMode.values());
			throw new BadRequestErrorException("Invalid 'riskMode' parameter value " + riskMode +
                        ", valid values are: " + Arrays.toString(RiskCalculationMode.values()));
		}

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("Calculating attack tree");

            AttackPathAlgorithm apa = new AttackPathAlgorithm(querierDB);

            if (!apa.checkTargetUris(targetURIs)) {
                logger.error("Invalid target URIs set");
                throw new MisbehaviourSetInvalidException("Invalid misbehaviour set");
            }

            if (!apa.checkRiskCalculationMode(riskMode)) {
                logger.error("mismatch in risk calculation mode found");
                throw new BadRequestErrorException("mismatch between the stored and requested risk calculation mode, please run the risk calculation");
            }

            TreeJsonDoc treeDoc = apa.calculateAttackTreeDoc(targetURIs, riskMode, allPaths, normalOperations);

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(treeDoc);

        } catch (MisbehaviourSetInvalidException e) {
            logger.error("Threat graph calculation failed due to invalid misbehaviour set", e);
            throw e;
        } catch (BadRequestErrorException e) {
            logger.error("mismatch between the stored and requested risk calculation mode, please run the risk calculation");
            throw e;
        } catch (Exception e) {
            logger.error("Threat path failed due to an error", e);
            throw new InternalServerErrorException(
                    "Threat graph calculation failed. Please contact support for further assistance.");
        }
    }


	/**
	 * This REST method generates a recommendation report, as a blocking call
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @param riskMode string indicating the prefered risk calculation mode
	 * @return A JSON report containing recommendations 
     * @throws InternalServerErrorException   if an error occurs during report generation
	 */
	@GetMapping(value = "/models/{modelId}/recommendations_blocking")
	public ResponseEntity<RecommendationReportDTO> calculateRecommendationsBlocking(
            @PathVariable String modelId,
            @RequestParam(defaultValue = "CURRENT") String riskMode) {

        logger.info("Calculating recommendations for model {}", modelId);
		riskMode = riskMode.replaceAll("[\n\r]", "_");
        logger.info("riskMode: {}",riskMode);

		RiskCalculationMode rcMode;

		try {
            rcMode = RiskCalculationMode.valueOf(riskMode);
		} catch (IllegalArgumentException e) {
			logger.error("Found unexpected riskCalculationMode parameter value {}, valid values are: {}.",
					riskMode, RiskCalculationMode.values());
			throw new BadRequestErrorException("Invalid 'riskMode' parameter value " + riskMode +
                        ", valid values are: " + Arrays.toString(RiskCalculationMode.values()));
		}

		final Model model;
		Progress progress;

		synchronized(this) {
			model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);
			String mId = model.getId();
			
			if (model.isValidating()) {
				logger.warn("Model {} is currently validating - ignoring calc risks request {}", mId, modelId);
				return ResponseEntity.status(HttpStatus.OK).body(new RecommendationReportDTO());
			}

			if (model.isCalculatingRisks()) {
				logger.warn("Model {} is already calculating risks - ignoring request {}", mId, modelId);
				return ResponseEntity.status(HttpStatus.OK).body(new RecommendationReportDTO());
			}

			progress = modelObjectsHelper.getTaskProgressOfModel("Recommendations", model);
			progress.updateProgress(0d, "Recommendations starting");

			logger.debug("Marking as calculating risks [{}] {}", modelId, model.getName());
			model.markAsCalculatingRisks(rcMode, false);
		} //synchronized block

		AStoreWrapper store = storeModelManager.getStore();
		RecommendationReportDTO report = null;

		try {
			logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), true);

            querierDB.initForRiskCalculation();

            logger.info("Calculating recommendations");

            String jobId = UUID.randomUUID().toString();
            logger.info("Submitting synchronous job with id: {}", jobId);
			String mId = model.getId();

			RecommendationsAlgorithmConfig recaConfig = new RecommendationsAlgorithmConfig(querierDB, mId, riskMode);
			RecommendationsAlgorithm reca = new RecommendationsAlgorithm(recaConfig);

            if (!reca.checkRiskCalculationMode(riskMode)) {
                logger.error("mismatch in risk calculation mode found");
                throw new BadRequestErrorException("mismatch between the stored and requested risk calculation mode, please run the risk calculation");
            }

            report = reca.recommendations(progress);

            // create recEntry and save it to mongo db
            RecommendationEntity recEntity = new RecommendationEntity();
            recEntity.setId(jobId);
            recEntity.setModelId(mId);
            recEntity.setStatus(RecStatus.STARTED);
            recEntity.setReport(report);
            recRepository.save(recEntity);
            logger.debug("rec entity saved for {}", recEntity.getId());

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(report);

        } catch (BadRequestErrorException e) {
            logger.error("mismatch between the stored and requested risk calculation mode, please run the risk calculation");
            throw e;
        } catch (Exception e) {
            logger.error("Recommendations failed due to an error", e);
            throw new InternalServerErrorException(
                    "Finding recommendations failed. Please contact support for further assistance.");
		} finally {
			//always reset the flags even if the risk calculation crashes
			model.finishedCalculatingRisks(report != null, rcMode, false);
		}
	}

	/**
	 * This REST method generates a recommendation report, as an asynchronous call.
	 * Results may be downloaded once this task has completed.
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @param riskMode string indicating the prefered risk calculation mode
	 * @return ACCEPTED status and jobId for the background task
     * @throws InternalServerErrorException   if an error occurs during report generation
	 */
	@GetMapping(value = "/models/{modelId}/recommendations")
    public ResponseEntity<JobResponseDTO> calculateRecommendations(
            @PathVariable String modelId,
            @RequestParam (defaultValue = "CURRENT") String riskMode) {

        logger.info("Calculating recommendations for model {}", modelId);
		riskMode = riskMode.replaceAll("[\n\r]", "_");
        logger.info(" riskMode: {}",riskMode);

        RiskCalculationMode rcMode;

		try {
            rcMode = RiskCalculationMode.valueOf(riskMode);
		} catch (IllegalArgumentException e) {
			logger.error("Found unexpected riskCalculationMode parameter value {}, valid values are: {}.",
					riskMode, RiskCalculationMode.values());
			throw new BadRequestErrorException("Invalid 'riskMode' parameter value " + riskMode +
                        ", valid values are: " + Arrays.toString(RiskCalculationMode.values()));
		}

        final String rm = riskMode;

		final Model model;
		Progress progress;

		synchronized(this) {
			model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);
            String mId = model.getId();

			if (model.isValidating()) {
				logger.warn("Model {} is currently validating - ignoring request {}", mId, modelId);
				return new ResponseEntity<>(HttpStatus.ACCEPTED);
			}

			if (model.isCalculatingRisks()) {
				logger.warn("Model {} is already calculating risks - ignoring request {}", mId, modelId);
				return new ResponseEntity<>(HttpStatus.ACCEPTED);
			}

			progress = modelObjectsHelper.getTaskProgressOfModel("Recommendations", model);
			progress.updateProgress(0d, "Recommendations starting");

			logger.debug("Marking as calculating risks [{}] {}", mId, model.getName());
			model.markAsCalculatingRisks(rcMode, false);
		} //synchronized block

		AStoreWrapper store = storeModelManager.getStore();

        logger.info("Creating async job for {}", modelId);
        String jobId = UUID.randomUUID().toString();
        logger.info("Submitting async job with id: {}", jobId);

        ScheduledFuture<?> future = Executors.newScheduledThreadPool(1).schedule(() -> {
            boolean success = false;

            try {
                logger.info("Initialising JenaQuerierDB");

                JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                        model.getModelStack(), true);

                querierDB.initForRiskCalculation();

                logger.info("Calculating recommendations");

                RecommendationsAlgorithmConfig recaConfig = new RecommendationsAlgorithmConfig(querierDB, model.getId(), rm);
                asyncService.startRecommendationTask(jobId, recaConfig, progress);

                success = true;
            } catch (BadRequestErrorException e) {
                logger.error("mismatch between the stored and requested risk calculation mode, please run the risk calculation");
                throw e;
            } catch (Exception e) {
                logger.error("Recommendations failed due to an error", e);
                throw new InternalServerErrorException(
                        "Finding recommendations failed. Please contact support for further assistance.");
            } finally {
                //always reset the flags even if the risk calculation crashes
                model.finishedCalculatingRisks(success, rcMode, false);
                progress.updateProgress(1.0, "Recommendations complete");
            }
			return true;
		}, 0, TimeUnit.SECONDS);
        
		modelObjectsHelper.registerValidationExecution(model.getId(), future);

        // Build the Location URI for the job status
        URI locationUri = URI.create("/models/" + modelId + "/recommendations/status/" + jobId);

        // Return 202 Accepted with a Location header
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(locationUri);

        JobResponseDTO response = new JobResponseDTO(jobId, "CREATED");

        return ResponseEntity.accepted().headers(headers).body(response);
    }

}
