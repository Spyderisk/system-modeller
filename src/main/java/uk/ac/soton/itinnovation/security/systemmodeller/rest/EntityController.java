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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.soton.itinnovation.security.modelquerier.JenaQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.AssetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ControlDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ControlSetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ControlStrategyDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.LevelDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.MisbehaviourDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.MisbehaviourSetDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.ThreatDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.TrustworthinessAttributeDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.TrustworthinessAttributeSetDB;
import uk.ac.soton.itinnovation.security.semanticstore.AStoreWrapper;
import uk.ac.soton.itinnovation.security.semanticstore.JenaTDBStoreWrapper;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.model.WebKeyRole;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.InternalServerErrorException;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.StoreModelManager;
import uk.ac.soton.itinnovation.security.systemmodeller.util.SecureUrlHelper;

/**
 * Includes all operations of the Entity Controller Service.
 */
@RestController
public class EntityController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private StoreModelManager storeModelManager;

    @Autowired
    private SecureUrlHelper secureUrlHelper;

    @Value("${admin-role}")
    public String adminRole;

    /**
    * Retrieves a JSON document describing a specific system model threat.
    *
    * @param modelId The String representation of the model object to search.
    * @param uri Threat URI (of the short form "system#ThreatName_ID"),
    *            e.g., "system#123". The URI should be properly encoded.
    * @return A JSON representation of a threat object.
    * @throws InternalServerErrorException if an error occurs during report generation.
    */
    @RequestMapping(value = "/models/{modelId}/entity/system/threats/{uri}", method = RequestMethod.GET)
    public ResponseEntity<ThreatDB> getEntitySystemThreat(@PathVariable String modelId, @PathVariable String uri) {

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
     * Retrieves a list of JSON documents describing all threats associated with
     * a specific system model.
      *
     * @param modelId the String representation of the model object to seacrh
     * @return A JSON representation of threat objects map
     * @throws InternalServerErrorException if an error occurs during report generation
     */
    @RequestMapping(value = "/models/{modelId}/entity/system/threats", method = RequestMethod.GET)
    public ResponseEntity<Map<String, ThreatDB>> getEntitySystemThreats(@PathVariable String modelId) {

        logger.info("get system threats for model {}", modelId);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting system threats");

            Map<String, ThreatDB> threats = querierDB.getThreats("system-inf");

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(threats);

        } catch (Exception e) {
            logger.error("Simple API get threats failed due to an error", e);
            throw new InternalServerErrorException(
                    "Threats fetch failed. Please contact support for further assistance.");
        }
    }

    /**
     * This REST method retrieves a misbehavour set (MS).
     *
     * @param modelId the String representation of the model object to seacrh
     * @param uri MisbehaviourSet URI (of the short form "system#MisbehaviourSetName_ID"),
     *            e.g., "system#123". The URI should be properly encoded.
     * @return A JSON representation of a misbehaviour set object
     * @throws InternalServerErrorException if an error occurs during report generation
     */
    @RequestMapping(value = "/models/{modelId}/entity/system/misbehaviourSets/{uri}", method = RequestMethod.GET)
    public ResponseEntity<MisbehaviourSetDB> getEntitySystemMisbehaviourSet(@PathVariable String modelId,
            @PathVariable String uri) {

        logger.info("Get system misbehaviour set for model {} with URI: {}", modelId, uri);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("Getting misbehaviour set: {}", uri);

            // Get misbehaviour set mainly from inferred graph, but include any impact level from asserted graph
            MisbehaviourSetDB ms = querierDB.getMisbehaviourSet(uri, "system-inf", "system");

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
     * This REST method retrives all misbehavour sets (MS) from the specified
     * system model.
     *
     * @param modelId the String representation of the model object to seacrh
     * @return A JSON representation of misbehaviour set objects map
     * @throws InternalServerErrorException if an error occurs during report generation
     */
    @RequestMapping(value = "/models/{modelId}/entity/system/misbehaviourSets", method = RequestMethod.GET)
    public ResponseEntity<Map<String, MisbehaviourSetDB>> getEntitySystemMisbehaviourSets(
            @PathVariable String modelId) {

        logger.info("Get system misbehaviour sets for model {}", modelId);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("Getting misbehaviour sets");

            // Get misbehaviour sets mainly from inferred graph, but include any impact levels from asserted graph
            Map<String, MisbehaviourSetDB> msMap = querierDB.getMisbehaviourSets("system-inf", "system");
            logger.info("Got {} misbehaviourSets", msMap.size());

            logger.info("Non-negligible impact levels:");
            for (MisbehaviourSetDB ms : msMap.values()) {
                if (!ms.getImpactLevel().contains("ImpactLevelNegligible")) {
                    logger.info("{}: {}", ms.getUri(), ms.getImpactLevel());
                }
            }

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(msMap);

        } catch (Exception e) {
            logger.error("Simple API get misbehaviours failed due to an error", e);
            throw new InternalServerErrorException(
                    "Misbehaviours fetch failed. Please contact support for further assistance.");
        }
    }

    /**
    * Retrieves control strategies (CSG) for a specific CSG URI.
    *
    * @param modelId The String representation of the model object to search.
    * @param uri Control strategies (CSG) short form URI to retrieve data from,
    *            e.g., "system#123". The URI should be properly encoded.
    * @return A JSON representation of a control strategies object.
    * @throws InternalServerErrorException if an error occurs during report generation.
    */
    @RequestMapping(value = "/models/{modelId}/entity/system/controlStrategies/{uri}", method = RequestMethod.GET)
    public ResponseEntity<ControlStrategyDB> getEntitySystemControlStrategy(@PathVariable String modelId,
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
     * Retrieves all control strategies (CSG) for a specific system model.
     *
     * @param modelId the String representation of the model object to seacrh
     * @return A JSON representation of control strategies object map
     * @throws InternalServerErrorException if an error occurs during report generation
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
     * Retrieves control set (CS) for a specific CS URI.
     *
     * @param modelId the String representation of the model object to seacrh
     * @param uri Control set (CSG) short form URI to retrieve data from,
     *            e.g., "system#123". The URI should be properly encoded.
     * @return A JSON representation of a control set object
     * @throws InternalServerErrorException if an error occurs during report generation
     */
    @RequestMapping(value = "/models/{modelId}/entity/system/controlSets/{uri}", method = RequestMethod.GET)
    public ResponseEntity<ControlSetDB> getEntitySystemControlSet(@PathVariable String modelId,
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
     * Retrieves all control sets (CS) from a specific system model.
     *
     * @param modelId the String representation of the model object to seacrh
     * @return A JSON representation of a map of system model control sets
     * @throws InternalServerErrorException if an error occurs during report generation
     */
    @RequestMapping(value = "/models/{modelId}/entity/system/controlSets", method = RequestMethod.GET)
    public ResponseEntity<Map<String, ControlSetDB>> getEntitySystemControlSets(@PathVariable String modelId) {

        logger.info("get system ControlSets for model {}", modelId);

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
     * Retrieves an asset for a specific Asset URI.
     *
     * @param modelId the String representation of the model object to seacrh
     * @param uri asset short form URI to retrieve data from,
     *            e.g., "system#123". The URI should be properly encoded.
     * @return A JSON representation of an asset object
     * @throws InternalServerErrorException if an error occurs during report generation
     */
    @RequestMapping(value = "/models/{modelId}/entity/system/assets/{uri}", method = RequestMethod.GET)
    public ResponseEntity<AssetDB> getEntitySystemAsset(@PathVariable String modelId, @PathVariable String uri) {

        logger.info("get system Asset for model {} with URI: {}", modelId, uri);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting asset");

            AssetDB asset = querierDB.getAsset(uri, "system", "system-inf");

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
     * Retrieves all assets for a specific system model.
     *
     * @param modelId the String representation of the model object to seacrh
     * @return A JSON representation of an asset object map
     * @throws InternalServerErrorException if an error occurs during report generation
     */
    @RequestMapping(value = "/models/{modelId}/entity/system/assets", method = RequestMethod.GET)
    public ResponseEntity<Map<String, AssetDB>> getEntitySystemAssets(@PathVariable String modelId) {

        logger.info("get system Assets for model {}", modelId);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting assets");

            Map<String, AssetDB> assets = querierDB.getAssets("system", "system-inf");

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(assets);

        } catch (Exception e) {
            logger.error("Simple API get assets failed due to an error", e);
            throw new InternalServerErrorException(
                    "Assets fetch failed. Please contact support for further assistance.");
        }
    }

    /**
     * Retrieves a trustworthiness attribute set (TWAS) for a specific URI.
     *
     * @param modelId the String representation of the model object to seacrh
     * @param uri     trustworthiness attribute set (TWAS) short form URI,
     *                e.g., "system#123". The URI should be properly encoded.
     * @return A JSON representation of a trustworthiness attribute set object
     * @throws InternalServerErrorException if an error occurs during report generation
     */
    @RequestMapping(value = "/models/{modelId}/entity/system/trustworthinessAttributeSets/{uri}", method = RequestMethod.GET)
    public ResponseEntity<TrustworthinessAttributeSetDB> getEntitySystemTWAS(@PathVariable String modelId,
            @PathVariable String uri) {

        logger.info("Get system TWAS for model {} with URI: {}", modelId, uri);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("Getting TWAS: {}", uri);

            // Get TWAS mainly from inferred graph, but include any asserted tw level from asserted graph
            TrustworthinessAttributeSetDB twas = querierDB.getTrustworthinessAttributeSet(uri, "system-inf", "system");

            if (twas == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(twas);

        } catch (Exception e) {
            logger.error("Simple API get TWAS failed due to an error", e);
            throw new InternalServerErrorException("TWAS fetch failed. Please contact support for further assistance.");
        }
    }

    /**
     * Retrieves all trustworthiness attribute sets (TWAS) from a system model.
     *
     * @param modelId the String representation of the model object to seacrh
     * @return A JSON representation of a map of system model trustworthiness attribute set (TWAS)
     * @throws InternalServerErrorException if an error occurs during report generation
     */
    @RequestMapping(value = "/models/{modelId}/entity/system/trustworthinessAttributeSets", method = RequestMethod.GET)
    public ResponseEntity<Map<String, TrustworthinessAttributeSetDB>> getEntitySystemTWAs(
            @PathVariable String modelId) {

        logger.info("Get system TWAS for model {}", modelId);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("Getting TWAS");

            Map<String, TrustworthinessAttributeSetDB> twas = querierDB.getTrustworthinessAttributeSets("system-inf", "system");

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(twas);

        } catch (Exception e) {
            logger.error("Simple API get TWAs failed due to an error", e);
            throw new InternalServerErrorException("TWAs fetch failed. Please contact support for further assistance.");
        }
    }

    /**
     * Retrieves a trustworthiness attribute (TWA) for a specific URI, from the domain model.
     *
     * @param modelId the String representation of the model object to seacrh
     * @param uri     trustworthiness attribute (TWA) short form URI,
     *                e.g., "system#123". The URI should be properly encoded.
     * @return A JSON representation of a trustworthiness attribute object
     * @throws InternalServerErrorException if an error occurs during report generation
     */
    @RequestMapping(value = "/models/{modelId}/entity/domain/trustworthinessAttributes/{uri}", method = RequestMethod.GET)
    public ResponseEntity<TrustworthinessAttributeDB> getEntityDomainTWA(@PathVariable String modelId,
            @PathVariable String uri) {

        logger.info("get domain TWA for model {} with URI: {}", modelId, uri);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting TWAS");

            TrustworthinessAttributeDB twa = querierDB.getTrustworthinessAttribute(uri, "domain");

            if (twa == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(twa);

        } catch (Exception e) {
            logger.error("Simple API get TWAS failed due to an error", e);
            throw new InternalServerErrorException("TWAS fetch failed. Please contact support for further assistance.");
        }
    }

    /**
     * Retrieves all trustworthiness attributes (TWA) from the domain model.
     *
     * @param modelId the String representation of the model object to seacrh
     * @return A JSON representation of a map of system model trustworthiness attribute (TWA)
     * @throws InternalServerErrorException if an error occurs during report generation
     */
    @RequestMapping(value = "/models/{modelId}/entity/domain/trustworthinessAttributes", method = RequestMethod.GET)
    public ResponseEntity<Map<String, TrustworthinessAttributeDB>> getEntityDomainTWAs(
            @PathVariable String modelId) {

        logger.info("get domain TWAs for model {}", modelId);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting TWAs");

            Map<String, TrustworthinessAttributeDB> twas = querierDB.getTrustworthinessAttributes("domain");

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(twas);

        } catch (Exception e) {
            logger.error("Simple API get TWAs failed due to an error", e);
            throw new InternalServerErrorException("TWAs fetch failed. Please contact support for further assistance.");
        }
    }


    /**
     * Retrieves domain model control for a specific control URI.
     *
     * @param modelId the String representation of the model object to seacrh
     * @param uri Control short form URI to retrieve data from,
     *            e.g., "system#123". The URI should be properly encoded.
     * @return A JSON representation of a control object
     * @throws InternalServerErrorException if an error occurs during report generation
     */
    @RequestMapping(value = "/models/{modelId}/entity/domain/controls/{uri}", method = RequestMethod.GET)
    public ResponseEntity<ControlDB> getEntityDomainControl(@PathVariable String modelId, @PathVariable String uri) {

        logger.info("get domain Control for model {} with URI: {}", modelId, uri);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting domain control");

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
     * Retrieves all domain model controls for a specific system model.
     *
     * @param modelId the String representation of the model object to seacrh
     * @return A JSON representation of a control object map
     * @throws InternalServerErrorException if an error occurs during report generation
     */
    @RequestMapping(value = "/models/{modelId}/entity/domain/controls", method = RequestMethod.GET)
    public ResponseEntity<Map<String, ControlDB>> getEntityDomainControls(@PathVariable String modelId) {

        logger.info("get domain Controls for model {}", modelId);

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
     * This REST method retrieves domain misbehaviour for URI.
     *
     * @param modelId the String representation of the model object to seacrh
     * @param uri Misbehaviour URI (of the short form "system#MisbehaviourName_ID"),
     *            e.g., "system#123". The URI should be properly encoded.
     * @return A JSON representation of a misbehaviour object
     * @throws InternalServerErrorException if an error occurs during report generation
     */
    @RequestMapping(value = "/models/{modelId}/entity/domain/misbehaviours/{uri}", method = RequestMethod.GET)
    public ResponseEntity<MisbehaviourDB> getEntityDomainMisbehaviour(@PathVariable String modelId,
            @PathVariable String uri) {

        logger.info("get domain Misbehaviour for model {} with URI: {}", modelId, uri);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting domain misbehaviour");

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
     * This REST method retrieves all domain misbehaviours for the specific
     * system model.
     *
     * @param modelId the String representation of the model object to seacrh
     * @return A JSON representation of a misbehaviours object map
     * @throws InternalServerErrorException if an error occurs during report generation
     */
    @RequestMapping(value = "/models/{modelId}/entity/domain/misbehaviours", method = RequestMethod.GET)
    public ResponseEntity<Map<String, MisbehaviourDB>> getEntityDomainMisbehaviours(@PathVariable String modelId) {

        logger.info("get domain Misbehaviours for model {}", modelId);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            logger.info("getting domain misbehaviours");

            Map<String, MisbehaviourDB> misbehaviours = querierDB.getMisbehaviours("domain");

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(misbehaviours);

        } catch (Exception e) {
            logger.error("Simple API get misbehaviours failed due to an error", e);
            throw new InternalServerErrorException(
                    "Misbehaviours fetch failed. Please contact support for further assistance.");
        }
    }

    /**
     * Retrieves a domain level metric for a specific URI.
     *
     * @param modelId the String representation of the model object to seacrh
     * @param metric  metric name
     * @param uri level short form URI, e.g., "system#123". The URI should be properly encoded.
     * @return A JSON representation of a level object
     * @throws InternalServerErrorException if an error occurs during report generation
     */
    @RequestMapping(value = "/models/{modelId}/entity/domain/levels/{metric}/{uri}", method = RequestMethod.GET)
    public ResponseEntity<LevelDB> getEntityDomainLevel(@PathVariable String modelId, @PathVariable String metric,
            @PathVariable String uri) {

        logger.info("get {} level for model {}, uri: {}", metric, modelId, uri);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

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
     * Retrieves all domain level metric for a specific system model.
     *
     * @param modelId the String representation of the model object to seacrh
     * @param metric  metric name
     * @return A JSON representation of a level object map
     * @throws InternalServerErrorException if an error occurs during report generation
     */
    @RequestMapping(value = "/models/{modelId}/entity/domain/levels/{metric}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, LevelDB>> getEntityDomainLevels(@PathVariable String modelId,
            @PathVariable String metric) {

        logger.info("get {} levels for model {}", metric, modelId);

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);

        AStoreWrapper store = storeModelManager.getStore();

        try {
            logger.info("Initialising JenaQuerierDB");

            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), false);

            querierDB.init();

            Map<String, LevelDB> levels;

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
