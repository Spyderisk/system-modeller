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

import uk.ac.soton.itinnovation.security.modelquerier.IQuerierDB;

public interface IDomainReasoner {
	/**
	 * Run before the validation
	 *
	 * @param mq the querier for the model the reasoner works on
	 */
	void beforeValidation(IQuerierDB mq);

	/**
	 * Run after the implicit assets have been created but before the system-specific patterns are created
	 *
	 * @param mq the querier for the model the reasoner works on
	 */
	void beforeSystemPatterns(IQuerierDB mq);

	/**
	 * Run after the system-specific patterns are created but before the control sets are generated
	 *
	 * @param mq the querier for the model the reasoner works on
	 */
	void beforeControlSets(IQuerierDB mq);

	/**
	 * Run after the control sets are generated but before the threats are generated
	 *
	 * @param mq the querier for the model the reasoner works on
	 */
	void beforeThreats(IQuerierDB mq);

	/**
	 * Run after the threats are generated
	 *
	 * @param mq the querier for the model the reasoner works on
	 */
	void afterValidation(IQuerierDB mq);

	/**
	 * Run before the risk calculation starts
	 *
	 * @param mq the querier for the model the reasoner works on
	 */
	void beforeCalculatingRisk(IQuerierDB mq);

	/**
	 * Run directly after the risk calculation has finished
	 *
	 * @param mq the querier for the model the reasoner works on
	 */
	void afterCalculatingRisk(IQuerierDB mq);

	/**
	 * A method in which a domain reasoner can calculate inferred controls (normally based on asserted controls).
	 *
	 * @param mq the querier for the model the reasoner works on
	 */
	void calculateControls(IQuerierDB mq);

	/**
	 * Check whether the validation should be repeated
	 *
	 * @return true to repeat, false to leave it there
	 */
	boolean repeatValidation();

	/**
	 * Set the flag to repeat the validation. This is useful in case the domain-specific reasoning creates new things
	 * that could cause new treats to appear. Remember to set it back so that we don't get trapped in a loop!
	 *
	 * @param doit whether to repeat the validation
	 */
	void setRepeatValidation(boolean doit);
}
