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
//      Created By :            Ken Meacham
//		Modified By :	        Stefanie Cox
//      Created Date :          2018-03-29
//      Created for Project :   SHiELD
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.model.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComplianceThreat extends AThreat {

	private final static Logger logger = LoggerFactory.getLogger(ComplianceThreat.class);

	public ComplianceThreat() {
		super();
	}

	@Override
	public final void setAcceptanceJustification(String acceptanceJustification) {
		logger.warn("Cannot accept compliance threat! Ignoring \"{}\"", acceptanceJustification);
	}
}
