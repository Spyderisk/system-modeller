/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2017
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
//      Created By :            Stefanie Cox
//      Created Date :          2017-11-23
//      Created for Project :   SHiELD
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.modelquerier.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ModelTree is like a model stack but extends it by the concept of a "main model".
 * This means that a model is the main anchor for queries/updates and that the graphs in the tree are somehow linked.
 * The assumption is that every graph has a /ui graph to go with it. If no such graph is found during the import,
 * a new, empty /ui graph will be created automatically.
 */
public class ModelTree extends ModelStack {

	private static final Logger logger = LoggerFactory.getLogger(ModelTree.class);

	private String mainModel;

	/**
	 * Creates a new model tree.
	 */
	public ModelTree() {
		super();
	}

	/**
	 * Add a graph to the list of graphs. If the identifier already exists, the graph URI will be overridden.
	 *
	 * @param id a short identifier for the graph which can also be used in SPARQL queries
	 * @param graphURI the URI of the graph in the store
	 */
	@Override
	public void addGraph(String id, String graphURI) {

		super.addGraph(id, graphURI);
		if (!id.endsWith(UI_ID_SUFFIX) && !graphURI.endsWith(UI_GRAPH_SUFFIX)) {
			logger.debug("Adding UI graph {} <{}> for graph {} <{}>",
					id + UI_ID_SUFFIX, graphURI + UI_GRAPH_SUFFIX, id, graphURI);
			graphs.put(id + UI_ID_SUFFIX, graphURI + UI_GRAPH_SUFFIX);
		} else {
			logger.debug("Skipping addition of UI graph for {} <{}>", id, graphURI);
		}
	}

	/**
	 * Removes a graph from the list of graphs.
	 *
	 * @param id a short identifier for the graph which can also be used in SPARQL queries
	 */
	public void removeGraph(String id) {

		graphs.remove(id);
		graphs.remove(id + UI_ID_SUFFIX);
	}

	// Getters/Setters ////////////////////////////////////////////////////////////////////////////

	/**
	 * Get the main model in this model tree
	 *
	 * @return the graph URI of the main model, i.e. the model which is the root for this model tree
	 */
	public String getMainModel() {
		return mainModel;
	}

	/**
	 * Get the main model in this model tree
	 * 
	 * @param id
	 */
	public void setMainModel(String id) {

		if (graphs.containsKey(id)) {
			logger.info("Setting {} as the main model of this model tree", id);
			this.mainModel = id;
		} else {
			logger.warn("Could not set main model {}: no graph with this ID exists in the store", id);
		}
	}
}
