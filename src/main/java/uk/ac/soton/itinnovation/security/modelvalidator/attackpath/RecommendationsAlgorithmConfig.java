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
//      Created By:             Panos Melas
//      Created Date:           2023-07-25
//      Created for Project :   Cyberkit4SME
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelvalidator.attackpath;

import java.util.ArrayList;
import java.util.List;

import uk.ac.soton.itinnovation.security.modelquerier.IQuerierDB;

public class RecommendationsAlgorithmConfig {
    private IQuerierDB querier;
    private String modelId;
    private String riskMode;
    private String acceptableRiskLevel;
    private List<String> targetMS;
    private boolean localSearch;

    public RecommendationsAlgorithmConfig(IQuerierDB querier, String modelId, String riskMode, boolean localSearch, String level, List<String> targets) {
        this.querier = querier;
        this.modelId = modelId;
        this.riskMode = riskMode;
        this.acceptableRiskLevel = level;
        if (targets == null) {
            this.targetMS = new ArrayList<>();
        } else {
            this.targetMS = targets;
        }
        this.localSearch = localSearch;
    }

    public IQuerierDB getQuerier() {
        return querier;
    }

    public String getModelId() {
        return modelId;
    }

    public String getRiskMode() {
        return riskMode;
    }

    public void setQuerier(IQuerierDB querier) {
        this.querier = querier;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public void setRiskMode(String riskMode) {
        this.riskMode = riskMode;
    }

    public String getAcceptableRiskLevel() {
        return this.acceptableRiskLevel;
    }

    public void setAcceptableRiskLevel(String level) {
        acceptableRiskLevel = level;
    }

    public List<String> getTargetMS() {
        return this.targetMS;
    }

    public void setTargetMS(List<String> targets) {
        this.targetMS = targets;
    }

    public Boolean getLocalSearch() {
        return this.localSearch;
    }

    public void setLocalSearch(Boolean flag) {
        this.localSearch = flag;
    }
}
