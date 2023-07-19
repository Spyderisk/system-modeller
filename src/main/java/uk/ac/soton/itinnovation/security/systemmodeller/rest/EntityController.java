/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2023
//
// Copyright in this software belongs to University of Southampton
// IT Innovation Centre, Highfield Campus, Southampton, SO17 1BJ, UK.
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
//    Created By :          Panos Melas
//    Created Date :        12/05/2023
//    Modified by:          
//    Created for Project : Cyberkit4SME
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
import java.net.URL;
import java.rmi.UnexpectedException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.jena.query.Dataset;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
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

import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;
import uk.ac.soton.itinnovation.security.model.system.RiskLevelCount;
import uk.ac.soton.itinnovation.security.model.system.RiskVector;
import uk.ac.soton.itinnovation.security.modelquerier.dto.RiskCalcResultsDB;
import uk.ac.soton.itinnovation.security.modelvalidator.ModelValidator;
import uk.ac.soton.itinnovation.security.modelvalidator.Progress;
import uk.ac.soton.itinnovation.security.semanticstore.AStoreWrapper;
import uk.ac.soton.itinnovation.security.semanticstore.IStoreWrapper;
import uk.ac.soton.itinnovation.security.semanticstore.util.SparqlHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.auth.KeycloakAdminClient;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.model.ModelFactory;
import uk.ac.soton.itinnovation.security.systemmodeller.model.WebKeyRole;
import uk.ac.soton.itinnovation.security.systemmodeller.mongodb.IModelRepository;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.LoadingProgress;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.LoadingProgressResponse;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.ModelDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.ThreatDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.UpdateModelResponse;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.BadRequestErrorException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.InternalServerErrorException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.ModelException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.ModelInvalidException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.NotAcceptableErrorException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.NotFoundErrorException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.UserForbiddenFromDomainException;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.StoreModelManager;
import uk.ac.soton.itinnovation.security.systemmodeller.util.PaletteGenerator;
import uk.ac.soton.itinnovation.security.systemmodeller.util.ReportGenerator;
import uk.ac.soton.itinnovation.security.systemmodeller.util.SecureUrlHelper;

import uk.ac.soton.itinnovation.security.modelquerier.util.ModelStack;
import uk.ac.soton.itinnovation.security.modelquerier.JenaQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ModelExportDB;
import uk.ac.soton.itinnovation.security.semanticstore.JenaTDBStoreWrapper;

import uk.ac.soton.itinnovation.security.modelquerier.dto.AssetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.CASettingDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.CardinalityConstraintDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ControlDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ControlSetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ControlStrategyDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.LevelDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.MADefaultSettingDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.MatchingPatternDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.MisbehaviourDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.MisbehaviourInhibitionSetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.MisbehaviourSetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ModelDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ModelFeatureDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.NodeDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.RiskCalcResultsDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.RootPatternDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.TWAADefaultSettingDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ThreatDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.TrustworthinessAttributeDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.TrustworthinessAttributeSetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.TrustworthinessImpactSetDB;

import java.util.function.Function;

/**
 * Includes all operations of the Entity Controller Service.
 */
@RestController
public class EntityController {

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
	 * This REST method ...
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @param uri threat URI
	 * @return A JSON representation of a threat object
     * @throws InternalServerErrorException   if an error occurs during report generation
	 */
	@RequestMapping(value = "/models/{modelId}/entity/system/threats/{uri}", method = RequestMethod.GET)
	public ResponseEntity<ThreatDB> getEntitySystemThreat(
            @PathVariable String modelId,
            @PathVariable String uri) {

        logger.info("get system threat for model {} with URI: {}", modelId, uri);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting threat");

            ThreatDB threat = querierDB.getThreat(uri, "system-inf");

            if (threat == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(threat);

        } catch (Exception e) {
            logger.error("Simple API get threat failed due to an error", e);
            throw new InternalServerErrorException(
                    "Threat fetch failed. Please contact support for further assistance.");
        }
    }

   	/**
	 * This REST method ...
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @return A JSON representation of threat objects map
     * @throws InternalServerErrorException   if an error occurs during report generation
	 */
	@RequestMapping(value = "/models/{modelId}/entity/system/threats", method = RequestMethod.GET)
	public ResponseEntity<Map<String, ThreatDB>> getEntitySystemThreats(
            @PathVariable String modelId) {

        logger.info("get system threats for model {}", modelId);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting threat");

            Map<String, ThreatDB> threats = querierDB.getThreats("system-inf");

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(threats);

        } catch (Exception e) {
            logger.error("Simple API get threats failed due to an error", e);
            throw new InternalServerErrorException(
                    "Threats fetch failed. Please contact support for further assistance.");
        }
    }

   	/**
	 * This REST method ...
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @param uri misbehaviour set URI
	 * @return A JSON representation of a misbehaviour set object
     * @throws InternalServerErrorException   if an error occurs during report generation
	 */
	@RequestMapping(value = "/models/{modelId}/entity/system/misbehaviourSet/{uri}", method = RequestMethod.GET)
	public ResponseEntity<MisbehaviourSetDB> getEntitySystemMisbehaviourSet(
            @PathVariable String modelId,
            @PathVariable String uri) {

        logger.info("get system misbihaviour for model {} with URI: {}", modelId, uri);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting misbehaviour");

            MisbehaviourSetDB ms = querierDB.getMisbehaviourSet(uri, "system-inf");

            if (ms == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(ms);

        } catch (Exception e) {
            logger.error("Simple API get misbehaviour failed due to an error", e);
            throw new InternalServerErrorException(
                    "Misbehaviour fetch failed. Please contact support for further assistance.");
        }
    }

   	/**
	 * This REST method ...
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @return A JSON representation of misbehaviour set objects map
     * @throws InternalServerErrorException   if an error occurs during report generation
	 */
	@RequestMapping(value = "/models/{modelId}/entity/system/misbehaviourSet", method = RequestMethod.GET)
	public ResponseEntity<Map<String, MisbehaviourSetDB>> getEntitySystemMisbehaviourSets(
            @PathVariable String modelId) {

        logger.info("get system misbihaviours for model {}", modelId);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting misbehaviours");

            Map<String, MisbehaviourSetDB> msMap = querierDB.getMisbehaviourSets("system-inf");

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(msMap);

        } catch (Exception e) {
            logger.error("Simple API get misbehaviours failed due to an error", e);
            throw new InternalServerErrorException(
                    "Misbehaviours fetch failed. Please contact support for further assistance.");
        }
    }

   	/**
	 * This REST method ...
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @param uri control strategies (CSG) URI
	 * @return A JSON representation of a control strategies object
     * @throws InternalServerErrorException   if an error occurs during report generation
	 */
	@RequestMapping(value = "/models/{modelId}/entity/system/controlStrategies/{uri}", method = RequestMethod.GET)
	public ResponseEntity<ControlStrategyDB> getEntitySystemControlStrategy(
            @PathVariable String modelId,
            @PathVariable String uri) {

        logger.info("get system ControlStrategy for model {} with URI: {}", modelId, uri);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting control strategy");

            ControlStrategyDB csg = querierDB.getControlStrategy(uri, "system-inf");

            if (csg == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(csg);

        } catch (Exception e) {
            logger.error("Simple API get control strategy failed due to an error", e);
            throw new InternalServerErrorException(
                    "ControlStrategy fetch failed. Please contact support for further assistance.");
        }
    }

   	/**
	 * This REST method ...
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @return A JSON representation of control strategies object map
     * @throws InternalServerErrorException   if an error occurs during report generation
	 */
	@RequestMapping(value = "/models/{modelId}/entity/system/controlStrategies", method = RequestMethod.GET)
	public ResponseEntity<Map<String, ControlStrategyDB>> getEntitySystemControlStrategies(
            @PathVariable String modelId) {

        logger.info("get system ControlStrategies for model {}", modelId);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting control strategies");

            Map<String, ControlStrategyDB> csgs = querierDB.getControlStrategies("system-inf");

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(csgs);

        } catch (Exception e) {
            logger.error("Simple API get control strategies failed due to an error", e);
            throw new InternalServerErrorException(
                    "ControlStrategies fetch failed. Please contact support for further assistance.");
        }
    }

   	/**
	 * This REST method ...
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @param uri control set URI
	 * @return A JSON representation of a control set object
     * @throws InternalServerErrorException   if an error occurs during report generation
	 */
	@RequestMapping(value = "/models/{modelId}/entity/system/controlSets/{uri}", method = RequestMethod.GET)
	public ResponseEntity<ControlSetDB> getEntitySystemControlSet(
            @PathVariable String modelId,
            @PathVariable String uri) {

        logger.info("get system ControlSet for model {} with URI: {}", modelId, uri);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting control set");

            ControlSetDB cs = querierDB.getControlSet(uri, "system-inf");

            if (cs == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(cs);

        } catch (Exception e) {
            logger.error("Simple API get control set failed due to an error", e);
            throw new InternalServerErrorException(
                    "ControlSet fetch failed. Please contact support for further assistance.");
        }
    }


   	/**
	 * This REST method ...
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @param controlsetURI control set URI
	 * @return A JSON representation of a map of system model control sets
     * @throws InternalServerErrorException   if an error occurs during report generation
	 */
	@RequestMapping(value = "/models/{modelId}/entity/system/controlSets", method = RequestMethod.GET)
	public ResponseEntity<Map<String, ControlSetDB>> getEntitySystemControlSets(
            @PathVariable String modelId) {

        logger.info("get system ControlSet for model {}", modelId);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting control sets");

            Map<String, ControlSetDB> css = querierDB.getControlSets("system-inf");

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(css);

        } catch (Exception e) {
            logger.error("Simple API get control sets failed due to an error", e);
            throw new InternalServerErrorException(
                    "ControlSets fetch failed. Please contact support for further assistance.");
        }
    }

   	/**
	 * This REST method ...
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @param uri asset URI
	 * @return A JSON representation of an asset object
     * @throws InternalServerErrorException   if an error occurs during report generation
	 */
	@RequestMapping(value = "/models/{modelId}/entity/system/assets/{uri}", method = RequestMethod.GET)
	public ResponseEntity<AssetDB> getEntitySystemAsset(
            @PathVariable String modelId,
            @PathVariable String uri) {

        logger.info("get system Asset for model {} with URI: {}", modelId, uri);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting asset");

            AssetDB asset = querierDB.getAsset(uri, "system-inf");

            if (asset == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(asset);

        } catch (Exception e) {
            logger.error("Simple API get asset failed due to an error", e);
            throw new InternalServerErrorException(
                    "Asset fetch failed. Please contact support for further assistance.");
        }
    }

   	/**
	 * This REST method ...
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @return A JSON representation of an asset object map
     * @throws InternalServerErrorException   if an error occurs during report generation
	 */
	@RequestMapping(value = "/models/{modelId}/entity/system/assets", method = RequestMethod.GET)
	public ResponseEntity<Map<String, AssetDB>> getEntitySystemAssets(
            @PathVariable String modelId) {

        logger.info("get system Assets for model {}", modelId);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting assets");

            Map<String, AssetDB> assets = querierDB.getAssets("system-inf");

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(assets);

        } catch (Exception e) {
            logger.error("Simple API get assets failed due to an error", e);
            throw new InternalServerErrorException(
                    "Assets fetch failed. Please contact support for further assistance.");
        }
    }

   	/**
	 * This REST method ...
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @param uri trustworthiness attribute set (TWAS) URI
	 * @return A JSON representation of a trustworthiness attribute set object
     * @throws InternalServerErrorException   if an error occurs during report generation
	 */
	@RequestMapping(value = "/models/{modelId}/entity/system/trustworthinessAttributeSet/{uri}", method = RequestMethod.GET)
	public ResponseEntity<TrustworthinessAttributeSetDB> getEntitySystemTWA(
            @PathVariable String modelId,
            @PathVariable String uri) {

        logger.info("get system TWA for model {} with URI: {}", modelId, uri);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting TWA");

            TrustworthinessAttributeSetDB twa = querierDB.getTrustworthinessAttributeSet(uri, "system-inf");

            if (twa == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(twa);

        } catch (Exception e) {
            logger.error("Simple API get TWA failed due to an error", e);
            throw new InternalServerErrorException(
                    "TWA fetch failed. Please contact support for further assistance.");
        }
    }

   	/**
	 * This REST method ...
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @return A JSON representation of a map of system model trustworthiness attribute set (TWAS)
     * @throws InternalServerErrorException   if an error occurs during report generation
	 */
	@RequestMapping(value = "/models/{modelId}/entity/system/trustworthinessAttributeSet", method = RequestMethod.GET)
	public ResponseEntity<Map<String, TrustworthinessAttributeSetDB>> getEntitySystemTWAs(
            @PathVariable String modelId) {

        logger.info("get system TWA for model {}", modelId);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting TWAs");

            Map<String, TrustworthinessAttributeSetDB> twas = querierDB.getTrustworthinessAttributeSets("system-inf");

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(twas);

        } catch (Exception e) {
            logger.error("Simple API get TWAs failed due to an error", e);
            throw new InternalServerErrorException(
                    "TWAs fetch failed. Please contact support for further assistance.");
        }
    }

   	/**
	 * This REST method ...
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @param uri control strategy URI
	 * @return A JSON representation of a control object
     * @throws InternalServerErrorException   if an error occurs during report generation
	 */
	@RequestMapping(value = "/models/{modelId}/entity/domain/controls/{uri}", method = RequestMethod.GET)
	public ResponseEntity<ControlDB> getEntityDomainControl(
            @PathVariable String modelId,
            @PathVariable String uri) {

        logger.info("get domain Control for model {} with URI: {}", modelId, uri);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting control");

            ControlDB control = querierDB.getControl(uri, "domain");

            if (control == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(control);

        } catch (Exception e) {
            logger.error("Simple API get control failed due to an error", e);
            throw new InternalServerErrorException(
                    "Control fetch failed. Please contact support for further assistance.");
        }
    }

   	/**
	 * This REST method ...
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @return A JSON representation of a control object map
     * @throws InternalServerErrorException   if an error occurs during report generation
	 */
	@RequestMapping(value = "/models/{modelId}/entity/domain/controls", method = RequestMethod.GET)
	public ResponseEntity<Map<String, ControlDB>> getEntityDomainControls(
            @PathVariable String modelId) {

        logger.info("get domain Control for model {}", modelId);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting domain controls");

            Map<String, ControlDB> controls = querierDB.getControls("domain");

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(controls);

        } catch (Exception e) {
            logger.error("Simple API get controls failed due to an error", e);
            throw new InternalServerErrorException(
                    "Controls fetch failed. Please contact support for further assistance.");
        }
    }

   	/**
	 * This REST method ...
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @param uri consequence URI
	 * @return A JSON representation of a consequence object
     * @throws InternalServerErrorException   if an error occurs during report generation
	 */
	@RequestMapping(value = "/models/{modelId}/entity/domain/misbehaviour/{uri}", method = RequestMethod.GET)
	public ResponseEntity<MisbehaviourDB> getEntityDomainMisbehaviour(
            @PathVariable String modelId,
            @PathVariable String uri) {

        logger.info("get domain Misbehaviour for model {} with URI: {}", modelId, uri);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting misbehaviour set");

            MisbehaviourDB misbehaviour = querierDB.getMisbehaviour(uri, "domain");

            if (misbehaviour == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(misbehaviour);

        } catch (Exception e) {
            logger.error("Simple API get misbehaviour failed due to an error", e);
            throw new InternalServerErrorException(
                    "Misbehaviour fetch failed. Please contact support for further assistance.");
        }
    }

   	/**
	 * This REST method ...
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @return A JSON representation of a consequence object map
     * @throws InternalServerErrorException   if an error occurs during report generation
	 */
	@RequestMapping(value = "/models/{modelId}/entity/domain/misbehaviour", method = RequestMethod.GET)
	public ResponseEntity<Map<String, MisbehaviourDB>> getEntityDomainMisbehaviours(
            @PathVariable String modelId) {

        logger.info("get domain Misbehaviour for model {}", modelId);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting misbehaviours");

            Map<String, MisbehaviourDB> misbehaviours = querierDB.getMisbehaviours("domain");

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(misbehaviours);

        } catch (Exception e) {
            logger.error("Simple API get misbehaviours failed due to an error", e);
            throw new InternalServerErrorException(
                    "Misbehaviours fetch failed. Please contact support for further assistance.");
        }
    }

   	/**
	 * This REST method ...
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @param metric metric name
	 * @param uri level URI
	 * @return A JSON representation of a level object
     * @throws InternalServerErrorException   if an error occurs during report generation
     */
	@RequestMapping(value = "/models/{modelId}/entity/domain/{metric}/{uri}", method = RequestMethod.GET)
	public ResponseEntity<LevelDB> getEntityDomainLevel(
            @PathVariable String modelId,
            @PathVariable String metric,
            @PathVariable String uri) {

        logger.info("get domain impact level for metric: {}, uri: {}", metric, uri);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting impact level");

            LevelDB level = null;

            switch (metric) {
                case "impact":
                    level = querierDB.getImpactLevel(uri);
                    break;
                case "population":
                    level = querierDB.getPopulationLevel(uri);
                    break;
                case "likelihood":
                    level = querierDB.getLikelihoodLevel(uri);
                    break;
                case "trustworthiness":
                    level = querierDB.getTrustworthinessLevel(uri);
                    break;
                case "risk":
                    level = querierDB.getRiskLevel(uri);
                    break;
                default:
                    logger.error("Cannot understand metric: {}, level {}", metric, uri);
                    return ResponseEntity.notFound().build();
            }

            if (level == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(level);

        } catch (Exception e) {
            logger.error("Simple API get impact level failed due to an error", e);
            throw new InternalServerErrorException(
                    "Metric level fetch failed. Please contact support for further assistance.");
        }
    }

   	/**
	 * This REST method ...
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @param metric metric name
	 * @return A JSON representation of a level object map
     * @throws InternalServerErrorException   if an error occurs during report generation
     */
	@RequestMapping(value = "/models/{modelId}/entity/domain/{metric}", method = RequestMethod.GET)
	public ResponseEntity<Map<String, LevelDB>> getEntityDomainLevels(
            @PathVariable String modelId,
            @PathVariable String metric) {

        logger.info("get domain impact levels for model {}, metric: {}", modelId, metric);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting impact level");

            Map<String, LevelDB> levels = new HashMap<>();

            switch (metric) {
                case "impact":
                    levels = querierDB.getImpactLevels();
                    break;
                case "population":
                    levels = querierDB.getPopulationLevels();
                    break;
                case "likelihood":
                    levels = querierDB.getLikelihoodLevels();
                    break;
                case "trustworthiness":
                    levels = querierDB.getTrustworthinessLevels();
                    break;
                case "risk":
                    levels = querierDB.getRiskLevels();
                    break;
                default:
                    logger.error("Cannot understand metric: {}", metric);
                    return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(levels);

        } catch (Exception e) {
            logger.error("Simple API get impact levels failed due to an error", e);
            throw new InternalServerErrorException(
                    "Impact levels fetch failed. Please contact support for further assistance.");
        }
    }

}
