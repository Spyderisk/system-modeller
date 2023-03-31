/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2019
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
//      Created By :          Ken Meacham
//      Created Date :        2019-01-17
//      Created for Project : SHIELD
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest.dto;

import java.util.Set;

public class UpdateControlsRequest {
	private Set<String> controls;
	private boolean proposed;
	private boolean workInProgress;

	public Set<String> getControls() {
		return controls;
	}

	public void setControls(Set<String> controls) {
		this.controls = controls;
	}

	public boolean isProposed() {
		return proposed;
	}

	public void setProposed(boolean proposed) {
		this.proposed = proposed;
	}

	public boolean isWorkInProgress() {
    		return workInProgress;
    	}

    public void setWorkInProgress(boolean workInProgress) {
    		this.workInProgress = workInProgress;
    }
}
