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
//      Created Date:			2023-01-24
//      Created for Project :   Cyberkit4SME
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelvalidator.attackpath;

import java.util.Set;
import java.util.HashSet;

class TreeTraversalException extends Exception {
    private Set<String> loopbackNodeUris;

    TreeTraversalException(Set<String> loopbackNodeUris) {
        if (loopbackNodeUris.isEmpty()) {
            this.loopbackNodeUris = new HashSet<>();
        } else {
            this.loopbackNodeUris = loopbackNodeUris;
        }
    }

    public Set<String> getLoopbackNodeUris() {
        return this.loopbackNodeUris;
    }
}
