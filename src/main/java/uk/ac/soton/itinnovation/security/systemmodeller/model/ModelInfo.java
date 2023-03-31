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
//      Created By :          Toby Wilkinson
//      Created Date :        09/12/2020
//      Created for Project : ZDMP
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.model;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.model.Level;
import uk.ac.soton.itinnovation.security.model.system.Model;
import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;

//Not public
class ModelInfo {

	private static final Logger logger = LoggerFactory.getLogger(ModelInfo.class);

	private final Model model;

	public ModelInfo(Model model) {
		this.model = model;
	}

	public Model getModel() {
		return model;
	}

	public String getUri() {
		return model.getUri();
	}

	public String getDomainGraph() {
		return model.getDomain();
	}

	public String getValidatedDomainVersion() {
		return model.getValidatedDomainVersion();
	}

	public String getName() {
		return model.getLabel();
	}

	public void setName(String name) {
		model.setLabel(name);
	}

	public String getDescription() {
		return model.getDescription();
	}

	public void setDescription(String description) {
		model.setDescription(description);
	}

	public boolean isValid() {
		return model.getValid();
	}

	public void setValid(boolean valid) {
		model.setValid(valid);

		if (!valid) {
			setRiskLevelsValid(false);
		}
	}

	public boolean riskLevelsValid() {
		return model.isRisksValid();
	}

	public void setRiskLevelsValid(boolean riskLevelsValid) {
		if (riskLevelsValid && !isValid()) {
			throw new IllegalStateException("Cannot set riskLevelsValid: model is invalid");
		}

		model.setRisksValid(riskLevelsValid);
	}

	public Level getRiskLevel() {
		if (!riskLevelsValid()) {
			throw new IllegalStateException("Cannot get riskLevel: risk levels are invalid");
		}

		return model.getRisk();
	}

	public RiskCalculationMode getRiskCalculationMode() {
		return model.getRiskCalculationMode();
	}

	public void setRiskCalculationMode(RiskCalculationMode mode) {
		model.setRiskCalculationMode(mode);
	}

	@Override
	public String toString() {
		return
			"\nname: " + getName() +
			"\ndescription: " + getDescription() +
			"\nvalidatedDomainVersion: " + getValidatedDomainVersion() +
			"\nvalid: " + isValid() +
			"\nriskLevelsValid: " + riskLevelsValid() +
			(riskLevelsValid() ? "\nriskLevel: " + getRiskLevel() : "") +
			"\nriskCalculationMode: " + getRiskCalculationMode();
	}
}
