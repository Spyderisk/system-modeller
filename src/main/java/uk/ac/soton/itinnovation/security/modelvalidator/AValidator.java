/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2018
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
//		Created By :				Stefanie Wiegand
//		Created Date :			2018-04-09
//		Created for Project :	RESTASSURED
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.modelvalidator;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.domainreasoner.IDomainReasoner;
import uk.ac.soton.itinnovation.security.modelquerier.SystemModelQuerier;
import uk.ac.soton.itinnovation.security.modelquerier.SystemModelUpdater;
import uk.ac.soton.itinnovation.security.modelquerier.util.ModelStack;
import uk.ac.soton.itinnovation.security.semanticstore.AStoreWrapper;

/**
 * An abstract class that provides common methods required for validation of models.
 */
public abstract class AValidator {

	private static final Logger logger = LoggerFactory.getLogger(AValidator.class);

	public static final int SPARQL_MAX_BATCH_SIZE = 3000;

	protected final ModelStack model;
	protected final AStoreWrapper store;
	protected final SystemModelQuerier smq;
	protected final SystemModelUpdater smu;
	protected final IDomainReasoner reasoner;

	public AValidator(AStoreWrapper store, ModelStack model, IDomainReasoner reasoner) throws MissingArgumentException {

		this.model = model;

		if (model.getNS("core")==null || model.getGraph("core")==null) {
			throw new MissingArgumentException("A model model needs to define a core namespace and graph!");
		}

		if (model.getNS("domain")==null || model.getGraph("domain")==null) {
			throw new MissingArgumentException("A model model needs to define a domain namespace and graph!");
		}

		if (model.getNS("system")==null || model.getGraph("system")==null) {
			logger.warn("A model model should define a system namespace and graph unless you're validating a domain model.");
		}

		this.store = store;
		this.reasoner = reasoner;
		this.smq = new SystemModelQuerier(model);
		this.smu = new SystemModelUpdater(model);
	}

	// Utility ////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Load a template from the resources into memory. No escaping is done!
	 *
	 * @param template the name of the template (in the resources/sparql dir, without the file type)
	 * @param variables any spin:arguments to the template. may be null or empty
	 * @param args strings that should be used in the SPARQL, added via a String formatter using %s. Need to be
	 *			in the order in which they appear in the template
	 * @return the actual template
	 */
	public static String loadTemplate(String template, Map<String, String> variables, String ... args) {

		logger.debug("Loading template <{}>", template);
		try {
			String tmp = FileUtils.readFileToString(new File(
					ModelValidator.class.getClassLoader().getResource("sparql/" + template + ".sparql").getPath()
			), "UTF-8");

			//use spin arguments
			if (variables!=null && !variables.isEmpty()) {
				logger.debug("Using template arguments: {}", variables);
				for (Map.Entry<String, String> arg: variables.entrySet()) {
					tmp = tmp.replace("?" + arg.getKey(), arg.getValue());
				}
			}

			//use strings
			if (args!=null && args.length>0) {
				tmp = String.format(tmp, (Object[]) args);
			}

			return tmp.trim();
		} catch (IOException ex) {
			logger.error("Template {} not found", template, ex);
		}
		return null;
	}

	// GETTERS / SETTERS //////////////////////////////////////////////////////////////////////////

	public AStoreWrapper getStore() {
		return store;
	}

	public ModelStack getModel() {
		return model;
	}

	public IDomainReasoner getReasoner() {
		return reasoner;
	}
}
