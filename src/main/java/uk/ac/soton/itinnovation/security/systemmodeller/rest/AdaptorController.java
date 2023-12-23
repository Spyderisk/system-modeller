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

//import org.apache.jena.query.Dataset;

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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Includes all operations of the Model Controller Service.
 */

@RestController
public class AdaptorController {

	private final Logger logger = LoggerFactory.getLogger(getClass());

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
	@RequestMapping(value = "/models/{modelId}/threatgraph1", method = RequestMethod.GET)
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
	 * This REST method generates a recommendation report 
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @param riskMode string indicating the prefered risk calculation mode
	 * @return A JSON report containing recommendations 
     * @throws InternalServerErrorException   if an error occurs during report generation
	 */
	@RequestMapping(value = "/models/{modelId}/recommendations1", method = RequestMethod.GET)
	public ResponseEntity<RecommendationReportDTO> calculateRecommendations(
            @PathVariable String modelId,
            @RequestParam(defaultValue = "FUTURE") String riskMode) {

        logger.info("Calculating threat graph for model {}", modelId);
        logger.info(" riskMode: {}",riskMode);

		try {
            RiskCalculationMode.valueOf(riskMode);
		} catch (IllegalArgumentException e) {
			logger.error("Found unexpected riskCalculationMode parameter value {}, valid values are: {}.",
					riskMode, RiskCalculationMode.values());
			throw new BadRequestErrorException("Invalid 'riskMode' parameter value " + riskMode +
                        ", valid values are: " + Arrays.toString(RiskCalculationMode.values()));
		}

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

		String mId = model.getId();

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("Calculating Recommendations");

			RecommendationsAlgorithmConfig recaConfig = new RecommendationsAlgorithmConfig(querierDB, mId, riskMode);
			RecommendationsAlgorithm reca = new RecommendationsAlgorithm(recaConfig);

            if (!reca.checkRiskCalculationMode(riskMode)) {
                logger.error("mismatch in risk calculation mode found");
                throw new BadRequestErrorException("mismatch between the stored and requested risk calculation mode, please run the risk calculation");
            }

            RecommendationReportDTO report = reca.recommendations(true, false);

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(report);

        } catch (BadRequestErrorException e) {
            logger.error("mismatch between the stored and requested risk calculation mode, please run the risk calculation");
            throw e;
        } catch (Exception e) {
            logger.error("Threat path failed due to an error", e);
            throw new InternalServerErrorException(
                    "Finding recommendations failed. Please contact support for further assistance.");
        }
    }

}
