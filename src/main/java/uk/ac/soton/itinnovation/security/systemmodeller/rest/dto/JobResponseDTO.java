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
//      Created By:				Panos Melas
//      Created Date:			2023-12-23
//      Created for Project :   Cyberkit4SME
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest.dto;

public class JobResponseDTO {

    private String jobId;
    private String state;
    private String message;

    public JobResponseDTO(String jobId, String stateName) {
        this.jobId = jobId;
        this.state = stateName;
        this.message = "";
    }

    public JobResponseDTO(String jobId, String stateName, String msg) {
        this.jobId = jobId;
        this.state = stateName;
        this.message = msg;
    }

    public String getJobId() { return this.jobId; }

    public void setJobId(String jobid) { this.jobId = jobid; }

    public String getMessage() { return this.message; }

    public void setMessage(String msg) { this.message = msg; }

    public String getState() { return this.state; }

    public void setState(String stateName) { this.state = stateName; }


}
