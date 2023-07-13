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
//      Created By :            Lee Mason
//      Created Date :          28/12/2019
//      Created for Project :   RESTASSURED
//      Modified By:            Mike Surridge
//      Modified for Project :  FOGPROTECT
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelquerier.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.util.ArrayList;
import java.util.Collection;

@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class ModelDB extends EntityDB {
    public ModelDB () {
    }

    /*
     * Domain and system model variants have label and description fields, but
     * there is no need for a parent field.
     */
	@SerializedName("rdfs#label")
	protected String label;
	@SerializedName("rdfs#comment")
	protected String description;

    @SerializedName("hasRisk")
    private String risk;

    private boolean risksValid;

    private String riskCalculationMode;

    //@SerializedName("http://www.w3.org/2002/07/owl#versionInfo")
    private String versionInfo;

    private String domainVersion;

    // TODO: Get working
    @SerializedName("http://purl.org/dc/terms/created")
    private String created;

    @SerializedName("http://purl.org/dc/terms/modified")
    private String modified;
}
