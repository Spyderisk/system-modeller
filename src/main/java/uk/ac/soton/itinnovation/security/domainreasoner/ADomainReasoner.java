/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2017
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
//      Created By :            Stefanie Cox
//      Created Date :          2017-02-10
//      Created for Project :   5G-ENSURE
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.domainreasoner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.modelquerier.IQuerierDB;

public abstract class ADomainReasoner implements IDomainReasoner {

	private static final Logger logger = LoggerFactory.getLogger(ADomainReasoner.class);

	private boolean repeatValidation = false;

	@Override
	public void beforeValidation(IQuerierDB mq) {
		logger.debug("No domain-specific reasoning has been implemented to run before the validation");
	}

	@Override
	public void beforeSystemPatterns(IQuerierDB mq) {
		logger.debug("No domain-specific reasoning has been implemented to run before the system pattern generation");
	}

	@Override
	public void beforeControlSets(IQuerierDB mq) {
		logger.debug("No domain-specific reasoning has been implemented to run before the control set generation");
	}

	@Override
	public void beforeThreats(IQuerierDB mq) {
		logger.debug("No domain-specific reasoning has been implemented to run before the threat generation");
	}

	@Override
	public void afterValidation(IQuerierDB mq) {
		logger.debug("No domain-specific reasoning has been implemented to run after the validation");
	}

	@Override
	public void beforeCalculatingRisk(IQuerierDB mq) {
		logger.debug("No domain-specific reasoning has been implemented to run before calculating risk");
	}

	@Override
	public void afterCalculatingRisk(IQuerierDB mq) {
		logger.debug("No domain-specific reasoning has been implemented to run after calculating risk");
	}

	@Override
	public void calculateControls(IQuerierDB mq) {
		logger.debug("No domain-specific reasoning has been implemented to infer controls");
	}

	@Override
	public boolean repeatValidation() {
		return repeatValidation;
	}

	@Override
	public void setRepeatValidation(boolean doit) {
		repeatValidation = doit;
	}
}
