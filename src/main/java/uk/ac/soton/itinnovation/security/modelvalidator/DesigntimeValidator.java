/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2018
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
//      Created By :            Stefanie Wiegand
//      Created Date :          2018-04-09
//      Created for Project :   RESTASSURED
//      Modified By:            Mike Surridge
//      Modified for Project:   FOGPROTECT
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.modelvalidator;

import java.util.*;

import org.apache.commons.cli.MissingArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.soton.itinnovation.security.domainreasoner.IDomainReasoner;
import uk.ac.soton.itinnovation.security.modelquerier.IQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.LevelDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.AssetDB;
import uk.ac.soton.itinnovation.security.modelquerier.JenaQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.util.ModelStack;
import uk.ac.soton.itinnovation.security.semanticstore.AStoreWrapper;
import uk.ac.soton.itinnovation.security.semanticstore.JenaTDBStoreWrapper;

/**
 * This class provides methods to validate a design-time system model.
 */
public class DesigntimeValidator extends AValidator {

	private static final Logger logger = LoggerFactory.getLogger(DesigntimeValidator.class);

	/**
	 * Creates a DesigntimeValidator using the given store
	 *
	 *	@param store the store with which to work
	 *	@param model the model of models which needs to be validated
	 *	@param reasoner the domain-specific reasoner or null if there is none
	 * @throws org.apache.commons.cli.MissingArgumentException
	 */
	public DesigntimeValidator(AStoreWrapper store, ModelStack model, IDomainReasoner reasoner) throws MissingArgumentException {
		super(store, model, reasoner);
	}

	// Design-time validation /////////////////////////////////////////////////////////////////////////////////////////

	public boolean validateDesigntimeModel(Progress progress) throws MissingArgumentException {
		try{
			//check if model is already being processed
			if (smq.isValidating(store)) {
				logger.error("The model is already validating!");
				return false;
			}
			if (smq.isCalculatingRisk(store)) {
				logger.error("The model is already validated and risks are being calculated!");
				return false;
			}

			logger.info("Validating design-time system model...");

			//re-run?
			boolean repeat;

			//add isValidating flag
			smu.setIsValidating(store, true);

			final long startTime = System.currentTimeMillis();

			//drop the inference graph
			store.clearGraph(model.getGraph("system-inf"));

			// Get the Jena store
			JenaTDBStoreWrapper jenaStore = (JenaTDBStoreWrapper) store;

			// Then create the caching querier and validator and get started
			IQuerierDB querierDB = new JenaQuerierDB(jenaStore.getDataset(), model, true);
			querierDB.initForValidation();

			// We no longer have a domain specific 'reasoner', so we can do this in one call
			Validator validator = new Validator(querierDB);
			validator.validate(progress);

			//write metadata information
			logger.info("Setting created date");
			smu.setCreateDate(store, new Date());

			//remove isValidating flag
			logger.info("Removing isValidating flag");
			smu.setIsValidating(store, false);

			logger.info("Finished compiling system model");

			final long endTime = System.currentTimeMillis();
			logger.info("DesignTimeValidator.validateDesigntimeModel(): execution time {} ms", endTime - startTime);
				
			return true;
		} catch (Exception e) {
			smu.setIsValidating(store, false);
			throw e;
		}
	}
	
	/**
	 * Recalculate inferred controls
	 *
	 * @param progress the progress object, used by UI. Will be ignored if null
	 * @return true if the calculation was executed, false if the model was already calculating at the time of calling
	 */
	public boolean runControlInference(Progress progress) throws MissingArgumentException {

		if (progress==null) {
			progress = new Progress(model.getGraph("system"));
		}

		if (smq.isCalculatingControls(store)) {
			logger.error("The control calculation is already running!");
			return false;
		}

		logger.info("Calculating inferred controls");

		smu.setIsCalculatingControls(store, true);

		progress.updateProgress(0.5, "Calculating inferred controls");
		JenaTDBStoreWrapper jenaStore = (JenaTDBStoreWrapper) store;
		IQuerierDB querierDB = new JenaQuerierDB(jenaStore.getDataset(), model, true);
		querierDB.init();
		if (reasoner!=null) { reasoner.calculateControls(querierDB); }

		progress.updateProgress(1.0, "Finished control inference");
		logger.info("Finished control inference");

		smu.setIsCalculatingControls(store, false);

		return true;
	}

}
