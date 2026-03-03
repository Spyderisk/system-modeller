/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2026
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
//      Created By:				Ken Meacham
//      Created Date:			2026-02-6
//      Created for Project :   THEMIS
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelvalidator.attackpath;

public class TimeoutException extends RuntimeException {

    public TimeoutException(String message) {
		super(message);
	}

    public TimeoutException(String message, Exception e) {
		super(message, e);
	}
}
