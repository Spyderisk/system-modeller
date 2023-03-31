/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2022
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
//      Created Date :          21/01/2022
//      Created for Project :   FOGPROTECT
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelquerier.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
@Data
@ToString(callSuper=true)
public class RiskCalcResultsDB {
	public RiskCalcResultsDB(){
	}

    /*
     * Used only for system models, and does not need label, description or parent fields.
     */
	
	// System model entities, including outputs of the risk calculation
	private ModelDB model;
	private Map<String, ThreatDB> threats;
	private Map<String, MisbehaviourSetDB> misbehaviourSets;
	private Map<String, TrustworthinessAttributeSetDB> twas;
	private Map<String, ControlSetDB> cs;

	// Domain model entities, included so run-time clients can find 'static' types and levels plus their labels and descriptions
	private Map<String, Map<String, LevelDB>> levels;
	private Map<String, MisbehaviourDB> misbehaviours;
	private Map<String, ControlDB> controls;

}
