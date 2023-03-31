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
//      Created By :          Toby Wilkinson
//      Created Date :        06/12/2020
//      Created for Project : ZDMP
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.model;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.soton.itinnovation.security.modelquerier.util.ModelStack;
import uk.ac.soton.itinnovation.security.semanticstore.AStoreWrapper;
import uk.ac.soton.itinnovation.security.systemmodeller.mongodb.IModelRepository;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.StoreModelManager;
import uk.ac.soton.itinnovation.security.systemmodeller.util.SecureUrlHelper;

@Component
public class ModelFactory {

	private static final Logger logger = LoggerFactory.getLogger(ModelFactory.class);

	@Autowired
	private IModelRepository modelRepository;

	@Autowired
	private StoreModelManager storeModelManager;

	@Autowired
	private SecureUrlHelper secureUrlHelper;

	public Model createModel(String domainGraph, String userId) {
		logger.info("Creating model for user '{}' with ontology <{}>", userId, domainGraph);

		Model model = createModelWithoutModelInfo(null, null, domainGraph, userId);

		//Load default ModelInfo from Jena
		model.loadModelInfo();

		//Set flags to sensible values
		model.invalidate();

		return model;
	}

	public Model createModelForImport(String uri, String domainGraph, String userId) {
		logger.info("Creating model <{}> for user '{}' with ontology <{}>", uri, userId, domainGraph);

		if (uri == null) {
			throw new IllegalArgumentException("Attempting to create Model with null uri");
		}
		if (uri.isEmpty()) {
			throw new IllegalArgumentException("Attempting to create Model with empty uri");
		}

		return createModelWithoutModelInfo(uri, null, domainGraph, userId);
	}

	public Model createModelForCopy(String fromUri, String domainGraph, String userId) {
		logger.info("Creating copy of model <{}> for user '{}' with ontology <{}>", fromUri, userId, domainGraph);

		if (fromUri == null) {
			throw new IllegalArgumentException("Attempting to copy from Model with null uri");
		}
		if (fromUri.isEmpty()) {
			throw new IllegalArgumentException("Attempting to copy from Model with empty uri");
		}

		return createModelWithoutModelInfo(null, fromUri, domainGraph, userId);
	}

	private Model createModelWithoutModelInfo(String toUri, String fromUri, String domainGraph, String userId) {
		logger.debug("createModelWithoutModelInfo fromUri:{}, toUri:{}, domainGraph: {}", fromUri, toUri, domainGraph);
		if (domainGraph == null) {
			throw new IllegalArgumentException("Attempting to create Model with null domainGraph");
		}
		if (domainGraph.isEmpty()) {
			throw new IllegalArgumentException("Attempting to create Model with empty domainGraph");
		}
		if (userId == null) {
			throw new IllegalArgumentException("Attempting to create Model with null userId");
		}
		if (userId.isEmpty()) {
			throw new IllegalArgumentException("Attempting to create Model with empty userId");
		}

		Date created = new Date();

		ModelACL modelACL = new ModelACL();
		modelACL.setUserId(userId);
		modelACL.setCreated(created);
		modelACL.setModified(created);
		modelACL.setModifiedBy(userId);

		Model model = new Model(modelACL, modelRepository, storeModelManager);

		//Save in Mongo to generate the model ID.
		model.saveModelACL();

		String newUri;
		try {
			if (toUri == null || storeModelManager.systemModelExists(toUri)) {
				//Create new model URI.
				newUri = storeModelManager.createSystemModel(model.getId(), fromUri, userId, domainGraph);
				logger.debug("Generated new URI <{}>", newUri);
			} else {
				//Use old model URI.
				logger.debug("Reusing URI <{}>", toUri);
				newUri = storeModelManager.createSystemModel(toUri, fromUri, model.getId(), userId, domainGraph);
				logger.debug("newUri <{}>", newUri);
			}
		} catch (Exception ex) {
			model.deleteModelACL();
			throw new RuntimeException("Failed to create system model", ex);
		}

		//Finish populating ModelACL
		model.setUri(newUri);
		model.setDomainGraph(domainGraph);
		model.setNoRoleUrl(secureUrlHelper.generateHardToGuessUrl());
		model.setWriteUrl(secureUrlHelper.generateHardToGuessUrl());
		model.setReadUrl(secureUrlHelper.generateHardToGuessUrl());
		model.setOwnerUrl(secureUrlHelper.generateHardToGuessUrl());
		model.saveModelACL();

		logger.debug("createModelWithoutModelInfo: populated model: {}", model);
		
		return model;
	}

	public Model getModel(ModelACL modelACL) {
		if (modelACL == null) {
			throw new IllegalArgumentException("Attempting to get Model for null modelACL");
		}

		return getModelFromStore(modelACL);
	}

	public Model getModelOrNull(ModelACL modelACL) {
		if (modelACL == null) {
			return null;
		}

		return getModelFromStore(modelACL);
	}

	private Model getModelFromStore(ModelACL modelACL) {
		Model model = new Model(modelACL, modelRepository, storeModelManager);

		//Load ModelInfo from Jena
		model.loadModelInfo();

		return model;
	}

	public Set<Model> getModels(List<ModelACL> list) {
		if (list == null) {
			throw new IllegalArgumentException("Attempting to get Models for null list");
		}

		return list
			.stream()
			.map(this::getModel)
			.collect(Collectors.toSet());
	}

	//This is only intended for use by the unit tests
	public Model getModel(ModelStack modelStack, AStoreWrapper store) {
		Model model = getModelWithoutModelInfo(modelStack, store);

		//Load ModelInfo from Jena
		model.loadModelInfo();

		return model;
	}

	//This is only intended for use by the unit tests
	public Model getModelWithoutModelInfo(ModelStack modelStack, AStoreWrapper store) {
		if (modelStack == null) {
			throw new IllegalArgumentException("Attempting to get Model for null modelStack");
		}
		if (store == null) {
			throw new IllegalArgumentException("Attempting to get Model for null store");
		}

		//Create a "dummy" ModelACL
		ModelACL modelACL = new ModelACL();
		modelACL.setUri(modelStack.getGraph("system"));
		modelACL.setDomainGraph(modelStack.getGraph("domain"));

		return new Model(modelACL, modelRepository, this.storeModelManager, store);
	}
}
