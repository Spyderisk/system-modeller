/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2023
//
// Copyright in this library belongs to the University of Southampton
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
//    Created By :          Panos Melas
//    Created Date :        10-03-2023
//    Created for Project : Cyberkit$SME
//
////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
* Exception thrown when a rest function tries to access an invalid misbehaviour
* Will present as an HTTP response.
*/
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Unknown target misbehaviour")
public class MisbehaviourSetInvalidException extends RuntimeException {
    public MisbehaviourSetInvalidException(String message) {
        super(message);
    }
}
