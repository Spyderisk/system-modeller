/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2020
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
//      Created Date :          2020-04-04
//      Created for Project :   PROTEGO
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.model.system;

import java.util.Objects;
import uk.ac.soton.itinnovation.security.model.SemanticEntity;

public final class MetadataPair extends SemanticEntity {
    private String key;
    private String value;

    public MetadataPair() {};

    public MetadataPair(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String toString() {
        return "MetadataPair(" + key + ":" + value + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof MetadataPair)) {
            return false;
        }

        MetadataPair pair = (MetadataPair) other;

        return (key == null ? pair.getKey() == null : key.equals(pair.getKey()))
            && (value == null ? pair.getValue() == null : value.equals(pair.getValue()));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + Objects.hashCode(key);
        hash = 13 * hash + Objects.hashCode(value);
        return hash;
    }
}
