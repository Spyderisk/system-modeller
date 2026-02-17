/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2026
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
//    Created By :          Ken Meacham 
//    Created Date :        09-02-2026
//    Created for Project : THEMIS
//
////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** 
 * SERVICE_UNAVAILABLE exception thrown to indicate that a service has timed out
 * 
 * @param message The error message to display
 */
@ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
public class ServiceTimeoutException extends RuntimeException {
    public ServiceTimeoutException(String message) {
        super(message);
    }

    public ServiceTimeoutException(String message, Exception e) {
        super(message, e);
    }
} 
