/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2014
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
//      Created By :          Maxim Bashevoy
//      Created Date :        2014-07-28
//      Created for Project : EXPERIMEDIA
//      Modified By:          Vadims Krivcovs, Oliver Hayes
//      Modified for Project: 5G-ENSURE
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.mongodb;

import com.mongodb.client.MongoDatabase;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.StoreModelManager;

/**
 * Handles all configuration (executed on start)
 */
@Service
public class ConfigurationService {

	private final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private StoreModelManager storeModelManager;

	@Value("${reset.on.start}")
	private boolean resetOnStart;

	/**
	 * Initialises the service
	 */
	@PostConstruct
	public void init() {

		logger.info("Initialising Configuration Service...");

		// check if we want to drop users collection
		if (resetOnStart || storeModelManager.storeIsEmpty()) {
			resetService();
		}

		logger.info("Initialising Configuration Service...[done]");
	}

	/**
	 * Resets the service by dropping the database and clearing management graph.
	 */
	private void resetService() {

		MongoDatabase currentDatabase = mongoTemplate.getDb();
		logger.warn("Dropping mongoDB database: {}", currentDatabase.getName());

		try {
			currentDatabase.drop();
		} catch (Exception e) {
			logger.error("Failed to drop mongoDB database {}", currentDatabase.getName(), e);
		}
	}

	/**
	 * Ensures the service is shut down properly.
	 */
	@PreDestroy
	public void shutdown() {}
}
