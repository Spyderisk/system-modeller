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
//      Created Date:			2023-09-01
//      Created for Project :   Cyberkit4SME
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto;

import java.util.List;
import java.util.Map;

public class RiskVectorDB implements Comparable<RiskVectorDB> {
    private int veryHigh;
    private int high;
    private int medium;
    private int low;
    private int veryLow;

    public RiskVectorDB() {
        veryHigh = 0;
        high = 0;
        medium = 0;
        low = 0;
        veryLow = 0;
    }

    public RiskVectorDB(int vHigh, int high, int med, int low, int vLow) {
        this.veryHigh = vHigh;
        this.high = high;
        this.medium = med;
        this.low = low;
        this.veryLow = vLow;
    }

    @Override
    public int compareTo(RiskVectorDB other) {
        if (this.equals(other)) {
            return 0;
        } else if (this.greaterThan(other)) {
            return 1;
        } else if (this.lessThan(other)) {
            return -1;
        } else {
            // it should not happen?
            return -2;
        }
    }


    public boolean equals(RiskVectorDB other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        return low == other.low &&
                veryLow == other.veryLow &&
                medium == other.medium &&
                high == other.high &&
                veryHigh == other.veryHigh;
    }

    public boolean greaterThan(RiskVectorDB other) {
        if (veryHigh - other.veryHigh > 0) {
            return true;
        } else if (veryHigh - other.veryHigh < 0) {
            return false;
        } else if (high - other.high > 0) {
            return true;
        } else if (high - other.high < 0) {
            return false;
        } else if (medium - other.medium > 0) {
            return true;
        } else if (medium - other.medium < 0) {
            return false;
        } else if (low - other.low > 0) {
            return true;
        } else if (low - other.low < 0) {
            return false;
        } else {
            return veryLow - other.veryLow > 0;
        }
    }

    public boolean lessThan(RiskVectorDB other) {
        if (veryHigh - other.veryHigh < 0) {
            return true;
        } else if (veryHigh - other.veryHigh > 0) {
            return false;
        } else if (high - other.high < 0) {
            return true;
        } else if (high - other.high > 0) {
            return false;
        } else if (medium - other.medium < 0) {
            return true;
        } else if (medium - other.medium > 0) {
            return false;
        } else if (low - other.low < 0) {
            return true;
        } else if (low - other.low > 0) {
            return false;
        } else {
            return veryLow - other.veryLow < 0;
        }
    }

}
