/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2023
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
//      Created By :            Mike Surridge
//      Created Date :          19/01/2023
//      Created for Project :   FOGPROTECT
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelquerier.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
@Data
@ToString(callSuper=true)
public class ModelExportDB {
	public ModelExportDB(){
	}

    /*
     * Used only for system models, and does not need label, description or parent fields.
     */
	
	// System model entities, including outputs of the risk calculation
	private ModelDB model;
	private Map<String, AssetDB> assets;
	private Map<String, CardinalityConstraintDB> relationships;
	private Map<String, MisbehaviourSetDB> misbehaviourSets;
	private Map<String, TrustworthinessAttributeSetDB> trustworthinessAttributeSets;
	private Map<String, ControlSetDB> controlSets;
	private Map<String, ThreatDB> threats;
	private Map<String, ControlStrategyDB> controlStrategies;

}
