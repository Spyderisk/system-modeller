/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2016
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
//      Created By :            Gianluca Correndo
//      Created Date :          3 Feb 2017
//      Created for Project :   5g-Ensure
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.systemmodeller.semantics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.ac.soton.itinnovation.security.semanticstore.AStoreWrapper;
import uk.ac.soton.itinnovation.security.semanticstore.JenaTDBStoreWrapper;

/**
 * Implements a factory pattern to create triple stores wrappers.
 * @author gc
 */
@Component
public class StoreFactory {

	private final static Logger logger = LoggerFactory.getLogger(StoreFactory.class);

	@Value("${triplestore}")
	private String triplestore;

	@Value("${jena.tdb.folder}")
	private String folder;

	public StoreFactory(){

	}

	/**
	 * Return an instance of a store wrapper based on the application properties
	 * @return
	 */
	public AStoreWrapper getInstance(){

		AStoreWrapper store = null;

		switch (triplestore) {
			case "jena.tdb":
				if (folder == null){
					logger.error("'folder' property not set!");
				}
				store = new JenaTDBStoreWrapper(folder);
				//KEM: moved into StoreModelManager.init()
				//store.getPrefixURIMap().put("acl", "http://www.w3.org/ns/auth/acl#");
				//store.getPrefixURIMap().put("v", "http://www.w3.org/2006/vcard/ns#");
		}
		return store;
	}

	public String getFolder() {
		return folder;
	}
}
