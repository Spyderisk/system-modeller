/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2021
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
//      Created Date :          2021-06-29
//      Created for Project :   Protego
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.model.system;

import uk.ac.soton.itinnovation.security.model.Level;

public class RiskLevelCount {
	
	private Level level;
			
	private int count;
	
	public RiskLevelCount() {
	}

	public Level getLevel() {
		return level;
	}

	public void setLevel(Level level) {
		this.level = level;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
	
	
}
