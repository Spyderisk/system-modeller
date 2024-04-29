/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2014
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
//      Created Date :          2014-02-20
//      Created for Project :   OPTET
//      Modified for Project:   5G-ENSURE, ASSURED
//      Modified By:            Mike Surridge
//      Modified for Project:   FOGPROTECT
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelvalidator;

import java.util.Date;

import org.apache.commons.cli.MissingArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.soton.itinnovation.security.domainreasoner.IDomainReasoner;
import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;
import uk.ac.soton.itinnovation.security.modelquerier.IQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.JenaQuerierDB;
import uk.ac.soton.itinnovation.security.modelquerier.dto.RiskCalcResultsDB;
import uk.ac.soton.itinnovation.security.modelquerier.util.ModelStack;
import uk.ac.soton.itinnovation.security.semanticstore.AStoreWrapper;
import uk.ac.soton.itinnovation.security.semanticstore.JenaTDBStoreWrapper;

/**
 * This class loads, modifies and saves a system model by using SPIN/SPARQL contained in
 * the model itself or its imports.
 * It also check whether a model is actually valid.
 * It creates a deep copy of its input models to return them but does not maintain any models itself.
 */
public class ModelValidator extends AValidator {

	private static final Logger logger = LoggerFactory.getLogger(ModelValidator.class);

	private final DesigntimeValidator designtimeValidator;

	/**
	 * Creates a ModelValidator using the given store
	 *
	 *	@param store the store with which to work
	 *	@param model the model of models which needs to be validated
	 *	@param reasoner the domain-specific reasoner or null if there is none
	 * @throws org.apache.commons.cli.MissingArgumentException
	 */
	public ModelValidator(AStoreWrapper store, ModelStack model, IDomainReasoner reasoner) throws MissingArgumentException {
		super(store, model, reasoner);
		designtimeValidator = new DesigntimeValidator(store, model, reasoner);
	}

	// API methods ////////////////////////////////////////////////////////////////////////////////

	/**
	 * Validates a design-time model.
	 *
	 * @param progress the validation progress object, used by UI
	 * @return true if the validation was executed, false if the model was already validating at the time of calling
	 */
	public boolean validateDesigntimeModel(Progress progress) throws MissingArgumentException {

		return designtimeValidator.validateDesigntimeModel(progress);

	}

	/**
	 * Recalculate risk levels across the system
	 *
	 * @param mode the risk calculation mode to use
     * @param saveResults indicates whether results should be stored
	 * @param progress the progress object, used by UI. Will be ignored if null
	 * @return the risk calculation results
	 */
	public RiskCalcResultsDB calculateRiskLevels(RiskCalculationMode mode, boolean saveResults, Progress progress) throws RuntimeException {

		//check if model is already being processed
		if (smq.isValidating(store)) {
			throw new RuntimeException("The model is already validating!");
		}
		if (smq.isCalculatingRisk(store)) {
			throw new RuntimeException("The model is already validated and risks are being calculated!");
		}

		//check if model is valid
		if (!smq.isValid(store)) {
			throw new RuntimeException("The model is flagged as not valid!");
		}

		String createDateString = smq.getCreateDate(store);  // we need to save this and restore it later below for some reason
		smu.setIsCalculatingRisk(store, true);
		
		try {
			final long startTime = System.currentTimeMillis();
		
			IQuerierDB querier = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(), model);
			//TODO: check when this should be run, as it may also be done elseqhere
			querier.initForRiskCalculation();
			RiskCalculator rc = new RiskCalculator(querier);
			boolean success = rc.calculateRiskLevels(mode, saveResults, progress);
			RiskCalcResultsDB riskCalcResults = rc.getRiskCalcResults();

			//write metadata information
			smu.setRisksValid(store, success);
			if (success) {
				smu.setModifiedDate(store, new Date());
			}
			//return success;
			final long endTime = System.currentTimeMillis();
			logger.info("ModelValidator.calculateRiskLevels(): execution time {} ms", endTime - startTime);
			return riskCalcResults;

		} catch (Exception e) {
			String message = "Error occurred when calculating risks: " + e.getMessage();
			logger.error(message);
			throw new RuntimeException(message, e);
		} finally {
			// remove isCalculatingRisk flag
			smu.setIsCalculatingRisk(store, false);
			// restore the creation date, saved above
			smu.setCreateDate(store, createDateString);
		}
	}

	/**
	 * Recalculate inferred controls across the system
	 *
	 * @param progress the progress object, used by UI. Will be ignored if null
	 * @return true if the calculation was executed, false if the model was already calculating at the time of calling
	 */
	public boolean runControlInference(Progress progress) throws MissingArgumentException {
		return designtimeValidator.runControlInference(progress);
	}

}
