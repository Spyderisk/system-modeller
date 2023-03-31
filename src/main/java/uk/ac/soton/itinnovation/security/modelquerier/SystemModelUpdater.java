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
//      Created Date :          2017-02-16
//      Created for Project :   5G-ENSURE
//      Modified By:            Mike Surridge
//      Modified for Project:   FOGPROTECT
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.modelquerier;

import java.text.SimpleDateFormat;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.model.SemanticEntity;
import uk.ac.soton.itinnovation.security.model.system.Asset;
import uk.ac.soton.itinnovation.security.model.system.AssetGroup;
import uk.ac.soton.itinnovation.security.model.system.ControlSet;
import uk.ac.soton.itinnovation.security.model.system.MisbehaviourSet;
import uk.ac.soton.itinnovation.security.model.system.MetadataPair;
import uk.ac.soton.itinnovation.security.model.system.Model;
import uk.ac.soton.itinnovation.security.model.system.Relation;
import uk.ac.soton.itinnovation.security.model.system.TrustworthinessAttributeSet;
import uk.ac.soton.itinnovation.security.modelquerier.util.ModelStack;
import uk.ac.soton.itinnovation.security.semanticstore.AStoreWrapper;
import uk.ac.soton.itinnovation.security.semanticstore.util.SparqlHelper;

public class SystemModelUpdater {

	private static final Logger logger = LoggerFactory.getLogger(SystemModelUpdater.class);

	private final ModelStack model;

	public SystemModelUpdater(ModelStack model) {
		this.model = model;
	}

	public void updateModelInfo(AStoreWrapper store, Model m) {
		
		logger.debug("updateModelInfo: risksValid = {}", m.isRisksValid());

		//delete first - will get duplicates otherwise
		String sparql = "WITH <" + model.getGraph("system") + "> DELETE {\n" +
		"	?m a owl:Ontology .\n" +
		"	?m rdfs:label ?label .\n" +
		"	?m rdfs:comment ?desc .\n" +
		"	?m core:domainGraph ?import .\n" +
		"	?m core:isValid ?valid .\n" +
		"	?m core:risksValid ?risksValid .\n" +
		"	?m core:isValidating ?validating .\n" +
		"	?m core:isCalculatingRisk ?calculatingRisk .\n" +
		"	?m core:riskCalculationMode ?riskCalculationMode .\n" +
		//"	?m core:hasRisk ?risk .\n" +
		"} INSERT {\n" +
		"	?m a owl:Ontology .\n" +
		"	?m rdfs:label \"" + SparqlHelper.escapeLiteral(m.getLabel())+ "\"^^xsd:string .\n" +
		"	?m rdfs:comment \"" + SparqlHelper.escapeLiteral(m.getDescription())+ "\"^^xsd:string .\n" +
		"	?m core:domainGraph <" + SparqlHelper.escapeURI(m.getDomain()) + "> .\n" +
		"	?m core:isValid \"" + SparqlHelper.escapeLiteral(String.valueOf(m.getValid())) + "\"^^xsd:boolean .\n" +
		"	?m core:risksValid \"" + SparqlHelper.escapeLiteral(String.valueOf(m.isRisksValid())) + "\"^^xsd:boolean .\n" +
		"	?m core:isValidating \"" + SparqlHelper.escapeLiteral(String.valueOf(m.isValidating())) + "\"^^xsd:boolean .\n" +
		"	?m core:isCalculatingRisk \"" + SparqlHelper.escapeLiteral(String.valueOf(m.isCalculatingRisk())) + "\"^^xsd:boolean .\n" +
		(m.getRiskCalculationMode() != null ? "	?m core:riskCalculationMode \"" + SparqlHelper.escapeLiteral(m.getRiskCalculationMode().name()) + "\" .\n" : "") +
		//"	?m core:hasRisk <" + SparqlHelper.escapeURI(m.getRisk().getUri()) + "> .\n" +
		"} WHERE {\n" +
		"	BIND (<" + SparqlHelper.escapeURI(m.getUri()) + "> AS ?m)\n" +
		"	OPTIONAL {?m a owl:Ontology}\n" +
		"	OPTIONAL {?m rdfs:label ?label}\n" +
		"	OPTIONAL {?m rdfs:comment ?desc}\n" +
		"	OPTIONAL {?m core:domainGraph ?import}\n" +
		"	OPTIONAL {?m core:isValid ?valid}\n" +
		"	OPTIONAL {?m core:risksValid ?risksValid}\n" +
		"	OPTIONAL {?m core:isValidating ?validating}\n" +
		"	OPTIONAL {?m core:isCalculatingRisk ?calculatingRisk}\n" +
		"	OPTIONAL {?m core:riskCalculationMode ?riskCalculationMode}\n" +
		//"	OPTIONAL {?m core:hasRisk ?risk}\n" +
		"}";
		store.update(sparql);

		logger.debug("Stored {} in graph <{}>", model, model.getGraph("system"));
	}


	/**
	 * This method is used specifically to transfer metadata entries from a source model to a new (copied) model.
	 * These specific triples need to be handled carefully so that the resulting copies refer to the
	 * new model URI, rather than the old one.
	 */
	public void updateModelInfoInCopiedModel(AStoreWrapper store, Model m1, String m2Uri) {
		
		logger.debug("updateModelInfoInCopiedModel: source URI = {}", m1.getUri());
		logger.debug("updateModelInfoInCopiedModel: dest   URI = {}", m2Uri);

		//delete first - will get duplicates otherwise
		//N.B. copied model triples still refer to the old model URI, so we must bind m1 to source model URI
		//in order to delete the old entries. We then insert using the new m2 URI
		String sparql = "WITH <" + model.getGraph("system") + "> DELETE {\n" +
		"	?m1 a owl:Ontology .\n" +
		"	?m1 rdfs:label ?label .\n" +
		"	?m1 rdfs:comment ?desc .\n" +
		"	?m1 core:domainGraph ?import .\n" +
		"	?m1 core:isValid ?valid .\n" +
		"	?m1 core:risksValid ?risksValid .\n" +
		"	?m1 core:isValidating ?validating .\n" +
		"	?m1 core:isCalculatingRisk ?calculatingRisk .\n" +
		"	?m1 core:riskCalculationMode ?riskCalculationMode .\n" +
		//"	?m1 core:hasRisk ?risk .\n" +
		"} INSERT {\n" +
		"	?m2 a owl:Ontology .\n" +
		"	?m2 rdfs:label \"" + SparqlHelper.escapeLiteral(m1.getLabel())+ "\"^^xsd:string .\n" +
		"	?m2 rdfs:comment \"" + SparqlHelper.escapeLiteral(m1.getDescription())+ "\"^^xsd:string .\n" +
		"	?m2 core:domainGraph <" + SparqlHelper.escapeURI(m1.getDomain()) + "> .\n" +
		"	?m2 core:isValid \"" + SparqlHelper.escapeLiteral(String.valueOf(m1.getValid())) + "\"^^xsd:boolean .\n" +
		"	?m2 core:risksValid \"" + SparqlHelper.escapeLiteral(String.valueOf(m1.isRisksValid())) + "\"^^xsd:boolean .\n" +
		"	?m2 core:isValidating \"" + SparqlHelper.escapeLiteral(String.valueOf(m1.isValidating())) + "\"^^xsd:boolean .\n" +
		"	?m2 core:isCalculatingRisk \"" + SparqlHelper.escapeLiteral(String.valueOf(m1.isCalculatingRisk())) + "\"^^xsd:boolean .\n" +
		(m1.getRiskCalculationMode() != null ? "	?m2 core:riskCalculationMode \"" + SparqlHelper.escapeLiteral(m1.getRiskCalculationMode().name()) + "\" .\n" : "") +
		//"	?m2 core:hasRisk <" + SparqlHelper.escapeURI(m1.getRisk().getUri()) + "> .\n" +
		"} WHERE {\n" +
		"	BIND (<" + SparqlHelper.escapeURI(m1.getUri()) + "> AS ?m1)\n" +
		"	BIND (<" + SparqlHelper.escapeURI(m2Uri) + "> AS ?m2)\n" +
		"	OPTIONAL {?m1 a owl:Ontology}\n" +
		"	OPTIONAL {?m1 rdfs:label ?label}\n" +
		"	OPTIONAL {?m1 rdfs:comment ?desc}\n" +
		"	OPTIONAL {?m1 core:domainGraph ?import}\n" +
		"	OPTIONAL {?m1 core:isValid ?valid}\n" +
		"	OPTIONAL {?m1 core:risksValid ?risksValid}\n" +
		"	OPTIONAL {?m1 core:isValidating ?validating}\n" +
		"	OPTIONAL {?m1 core:isCalculatingRisk ?calculatingRisk}\n" +
		"	OPTIONAL {?m1 core:riskCalculationMode ?riskCalculationMode}\n" +
		//"	OPTIONAL {?m1 core:hasRisk ?risk}\n" +
		"}";
		store.update(sparql);

		logger.debug("Stored {} in graph <{}>", model, model.getGraph("system"));
	}

    /**
     * Update the position for this asset. The position is taken from the asset object itself.
     *
	 * @param asset the asset. Note that only some properties are writable!
	 * @param store the store
     */
    public void updateAssetPosition(AStoreWrapper store, Asset asset) {
        updateAssetsPositions(store, Arrays.asList(asset));
		logger.debug("Updated position for asset {}<{}>", asset.getLabel(), asset.getUri());
    }
    
    public void updateAssetsPositions(AStoreWrapper store, List<Asset> assets) {
        
        String sparql = "";

        for (Asset asset : assets) {
            sparql += "DELETE {\n" +
            "   GRAPH <" + model.getGraph("system-ui") + "> {\n" +
            "       ?a core:positionX ?x .\n" +
            "       ?a core:positionY ?y .\n" +
            "   }\n" +
            "} INSERT {\n" +
            "   GRAPH <" + model.getGraph("system-ui") + "> {\n" +
            "       ?a core:positionX \"" + SparqlHelper.escapeLiteral(String.valueOf(asset.getIconX())) + "\"^^xsd:int .\n" +
            "       ?a core:positionY \"" + SparqlHelper.escapeLiteral(String.valueOf(asset.getIconY())) + "\"^^xsd:int .\n" +
            "   }\n" +
            "}\n" +
            "WHERE {\n" +
            "   BIND (<" + SparqlHelper.escapeURI(asset.getUri()) + "> AS ?a)\n" +
            "   GRAPH <" + model.getGraph("system-ui") + "> {\n" +
            "       OPTIONAL {?a core:positionX ?x}\n" +
            "       OPTIONAL {?a core:positionY ?y}\n" +
            "   }\n" +
            "};\n";
        }
        store.update(sparql);

        logger.debug("Updated {} asset positions", assets.size());
    }

	/**
     * Update the position for this asset. The position is taken from the asset object itself.
     *
	 * @param assetURI the asset URI
	 * @param store the store
	 * @param x the new x coordinate
	 * @param y the new y coordinate
     */
    public void updateAssetPosition(AStoreWrapper store, String assetURI, int x, int y) {

		String sparql = "DELETE {\n" +
		"	GRAPH <" + model.getGraph("system-ui") + "> {\n" +
		"		?a core:positionX ?x .\n" +
		"		?a core:positionY ?y .\n" +
		"	}\n" +
		"} INSERT {\n" +
		"	GRAPH <" + model.getGraph("system-ui") + "> {\n" +
		"		?a core:positionX \"" + SparqlHelper.escapeLiteral(String.valueOf(x)) + "\"^^xsd:int .\n" +
		"		?a core:positionY \"" + SparqlHelper.escapeLiteral(String.valueOf(y)) + "\"^^xsd:int .\n" +
		"	}\n" +
		"}\n" +
		"WHERE {\n" +
		"	BIND (<" + SparqlHelper.escapeURI(assetURI) + "> AS ?a)\n" +
		"	GRAPH <" + model.getGraph("system-ui") + "> {\n" +
		"		OPTIONAL {?a core:positionX ?x}\n" +
		"		OPTIONAL {?a core:positionY ?y}\n" +
		"	}\n" +
		"}";
		store.update(sparql);

		logger.debug("Updated position for asset <{}>, new coordinhates: ({}/{})", assetURI, x, y);
    }

	/**
     * Update the label for this asset.
     *
	 * @param assetURI the asset URI
	 * @param store the store
	 * @param assetLabel the new label
     */
    public void updateAssetLabel(AStoreWrapper store, String assetURI, String assetLabel) {

		String sparql = "DELETE {\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?a rdfs:label ?l .\n" +
		"	}\n" +
		"} INSERT {\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?a rdfs:label \"" + SparqlHelper.escapeLiteral(assetLabel) + "\"^^xsd:string .\n" +
		"	}\n" +
		"}\n" +
		"WHERE {\n" +
		"	BIND (<" + SparqlHelper.escapeURI(assetURI) + "> AS ?a)\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		OPTIONAL {?a rdfs:label ?l}\n" +
		"	}\n" +
		"}";
		store.update(sparql);

		logger.debug("Updated position for asset {} <{}>", assetLabel, assetURI);
    }

    public void updateAssetType(AStoreWrapper store, String assetURI, String assetType) {
		//check number of triples in graphs, prior to query
		logger.debug("current triples count (asserted) = {}", store.getCount(model.getGraph("system")));
		logger.debug("current triples count (inferred) = {}", store.getCount(model.getGraph("system-inf")));

		String sparql = "DELETE {\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?a rdf:type ?type .\n" +
		"		?a ?prop3 ?b1 .\n" +
		"		?c1 ?prop4 ?a .\n" +
		"	}\n" +
		"} INSERT {\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?a rdf:type <" + SparqlHelper.escapeURI(assetType) + "> .\n" +
		"		?a ?prop1 ?b2 .\n" +
		"		?c2 ?prop2 ?a .\n" +
		"	}\n" +
		"}\n" +
		"WHERE {\n" +
		"	BIND (<" + SparqlHelper.escapeURI(assetURI) + "> AS ?a)\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		OPTIONAL {?a rdf:type ?type}\n" +
		"		OPTIONAL {?a ?prop3 ?b1 .\n" +
		"			GRAPH<" + model.getGraph("domain") + ">{\n" +
		"				?prop3 rdf:type owl:ObjectProperty .\n" +
		"			}\n" +
		"		}\n" +
		"		OPTIONAL {?c1 ?prop4 ?a .\n" +
		"			GRAPH<" + model.getGraph("domain") + ">{\n" +
		"				?prop4 rdf:type owl:ObjectProperty .\n" +
		"			}\n" +
		"		}\n" +
		"		OPTIONAL {?a ?prop1 ?b2 .\n" +
		"			GRAPH<" + model.getGraph("domain") + ">{\n" +
		"				?prop1 rdf:type owl:ObjectProperty .\n" +
		"				<" + SparqlHelper.escapeURI(assetType) + "> rdfs:subClassOf* ?superDomain .\n" +
		"				?prop1 rdfs:domain ?superDomain .\n" +
		"			}\n" +
		"		}\n" +
		"		OPTIONAL {?c2 ?prop2 ?a .\n" +
		"			GRAPH<" + model.getGraph("domain") + ">{\n" +
		"				?prop2 rdf:type owl:ObjectProperty .\n" +
		"				<" + SparqlHelper.escapeURI(assetType) + "> rdfs:subClassOf* ?superRange .\n" +
		"				?prop2 rdfs:range ?superRange .\n" +
		"			}\n" +
		"		}\n" +
		"	}\n" +
		"}";
		store.update(sparql);

		logger.debug("Updated type for asset <{}> to {}", assetURI, assetType);
		
		//check number of triples in graphs, after query
		logger.debug("new triples count (asserted) = {}", store.getCount(model.getGraph("system")));
		logger.debug("new triples count (inferred) = {}", store.getCount(model.getGraph("system-inf")));
    }

	/**
	 * Updates an asset in the store. If the asset doesn't exist, it will be created.
	 *
	 * @param assetURI the asset URI
	 * @param store the store
	 * @param min the new minimum cardinality for this asset
	 * @param max the new maximum cardinality for this asset
	 */
	public void updateAssetCardinality(AStoreWrapper store, String assetURI, int min, int max) {

		String sparql = "DELETE {\n" +
		//delete from both possible graphs just in case the asserted property has changed (although it shouldn't)
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?a core:minCardinality ?min .\n" +
		"		?a core:maxCardinality ?max .\n" +
		"	}\n" +
		"	GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"		?a core:minCardinality ?min .\n" +
		"		?a core:maxCardinality ?max .\n" +
		"	}\n" +
		"} INSERT {\n" +
		"	GRAPH ?g {\n" +
		"		?a core:minCardinality \"" + SparqlHelper.escapeLiteral(String.valueOf(min)) + "\"^^xsd:int .\n" +
		"		?a core:maxCardinality \"" + SparqlHelper.escapeLiteral(String.valueOf(max)) + "\"^^xsd:int .\n" +
		"	}\n" +
		"} WHERE {\n" +
		"	BIND (<" + SparqlHelper.escapeURI(assetURI) + "> AS ?a)\n" +
		"	GRAPH ?g {?a a ?asset}\n" +
		"	OPTIONAL {?a core:minCardinality ?min}\n" +
		"	OPTIONAL {?a core:maxCardinality ?max}\n" +
		"}";
		store.update(sparql, model.getGraph("system"), model.getGraph("system-inf"));

		logger.debug("Stored <{}>'s new cardinality constraints: {}, {}", assetURI, min, max);
	}

	/**
	 * Updates an asset population in the store.
	 *
	 * @param assetURI the asset URI
	 * @param store the store
	 * @param population the new or updated population level for the asset
	 */
	public void updateAssetPopulation(AStoreWrapper store, String assetURI, String population) {

		String sparql = "DELETE {\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?a core:population ?apop .\n" + // asserted population
		"	}\n" +
		"} INSERT {\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?a core:population <" + SparqlHelper.escapeURI(population) + ">\n" +
		"	}\n" +
		"} WHERE {\n" +
		"	BIND (<" + SparqlHelper.escapeURI(assetURI) + "> AS ?a)\n" +
		"	GRAPH ?g {?a a ?asset}\n" +
		"	OPTIONAL {\n" +
		"		GRAPH <" + model.getGraph("system") + "> {\n" +
		"			?a core:population ?apop .\n" +
		"		}\n" +
		"	}\n" +
		"}";
		store.update(sparql, model.getGraph("system"), model.getGraph("system-inf"));

		logger.debug("Stored <{}>'s new population: {}, {}", assetURI, population);
	}

	/**
	 * Updates an asset in the store. If the asset doesn't exist, it will be created.
	 *
	 * @param asset the asset. Note that only some properties are writable!
	 * @param store the store
	 */
	public void storeAsset(AStoreWrapper store, Asset asset) {

		//depending on whether the asset is asserted or inferred it is stored in a different graph
		String assetGraph = asset.isAsserted()?model.getGraph("system"):model.getGraph("system-inf");

		String sparql = "DELETE {\n" +
		//delete from both possible graphs just in case the asserted property has changed (although it shouldn't)
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?a rdfs:label ?label .\n" +
		"		?a rdf:type ?type .\n" +
		"		?a core:hasID ?id .\n" +
		"		?a core:minCardinality ?min .\n" + //delete old cardinality
		"		?a core:maxCardinality ?max .\n" + //delete old cardinality
		"		?a core:population ?apop .\n" +
		"		?a ?prop3 ?b1 .\n" +
		"		?c1 ?prop4 ?a .\n" +
		"	}\n" +
		"	GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"		?a rdfs:label ?label .\n" +
		"		?a rdf:type ?type .\n" +
		"		?a core:hasID ?id .\n" +
		"		?a core:minCardinality ?min .\n" +  //delete old cardinality
		"		?a core:maxCardinality ?max .\n" +  //delete old cardinality
		"		?a core:population ?apop .\n" +
		"		?a ?prop3 ?b1 .\n" +
		"		?c1 ?prop4 ?a .\n" +
		"	}\n" +
		"	GRAPH <" + model.getGraph("system-ui") + "> {\n" +
		"		?a core:positionX ?x .\n" +
		"		?a core:positionY ?y .\n" +
		"		?a core:isVisible ?visible .\n" +
		"	}\n" +
		"} INSERT {\n" +
		"	GRAPH <" + assetGraph + "> {\n" +
		"		?a rdfs:label \"" + SparqlHelper.escapeLiteral(asset.getLabel())+ "\"^^xsd:string .\n" +
		"		?a rdf:type <" + SparqlHelper.escapeURI(asset.getType()) + "> .\n" +
		"		?a core:hasID \"" + asset.getID() + "\" .\n" +
		//"		?a core:minCardinality \"" + SparqlHelper.escapeLiteral(String.valueOf(asset.getMinCardinality())) + "\"^^xsd:int .\n" + // no longer store cardinality
		//"		?a core:maxCardinality \"" + SparqlHelper.escapeLiteral(String.valueOf(asset.getMaxCardinality())) + "\"^^xsd:int .\n" + // no longer store cardinality
		"		?a core:population <" + SparqlHelper.escapeURI(asset.getPopulation()) + "> .\n" +
		"		?a ?prop1 ?b2 .\n" +
		"		?c2 ?prop2 ?a .\n" +
		"	}\n" +
		"	GRAPH <" + model.getGraph("system-ui") + "> {\n" +
		"		?a core:positionX \"" + SparqlHelper.escapeLiteral(String.valueOf(asset.getIconX())) + "\"^^xsd:int .\n" +
		"		?a core:positionY \"" + SparqlHelper.escapeLiteral(String.valueOf(asset.getIconY())) + "\"^^xsd:int .\n" +
		"		?a core:isVisible \"" + SparqlHelper.escapeLiteral(String.valueOf(asset.isVisible())) + "\"^^xsd:boolean .\n" +
		"	}\n" +
		"}\n" +
		"WHERE {\n" +
		"	BIND (<" + SparqlHelper.escapeURI(asset.getUri()) + "> AS ?a)\n" +
		"	OPTIONAL {?a core:hasID ?id}\n" +
		"	OPTIONAL {?a rdf:type ?type}\n" +
		"	OPTIONAL {?a core:positionX ?x}\n" +
		"	OPTIONAL {?a core:positionY ?y}\n" +
		"	OPTIONAL {?a core:minCardinality ?min}\n" +
		"	OPTIONAL {?a core:maxCardinality ?max}\n" +
		"	OPTIONAL {?a core:population ?apop}\n" +
		"	OPTIONAL {?a rdfs:label ?label}\n" +
		"	OPTIONAL {?a core:isVisible ?visible}\n" +
		"	OPTIONAL {?a ?prop3 ?b1 .\n" +
		"		GRAPH<" + model.getGraph("domain") + ">{\n" +
		"				?prop3 rdf:type owl:ObjectProperty}}\n" +
		"	OPTIONAL {?c1 ?prop4 ?a .\n" +
		"		GRAPH<" + model.getGraph("domain") + ">{\n" +
		"				?prop4 rdf:type owl:ObjectProperty}}\n" +
		"	OPTIONAL {?a ?prop1 ?b2 .\n" +
		"		GRAPH<" + model.getGraph("domain") + ">{\n" +
		"			?prop1 rdf:type owl:ObjectProperty .\n" +
		"			<" + SparqlHelper.escapeURI(asset.getType()) + "> rdfs:subClassOf* ?superDomain .\n" +
		"			?prop1 rdfs:domain ?superDomain .\n" +
		"		}\n" +
		"	}\n" +
		"	OPTIONAL {?c2 ?prop2 ?a .\n" +
		"		GRAPH<" + model.getGraph("domain") + ">{\n" +
		"			?prop2 rdf:type owl:ObjectProperty .\n" +
		"			<" + SparqlHelper.escapeURI(asset.getType()) + "> rdfs:subClassOf* ?superRange .\n" +
		"			?prop2 rdfs:range ?superRange .\n" +
		"		}\n" +
		"	}\n" +
		"}";
		store.update(sparql, model.getGraph("system"), model.getGraph("system-inf"), model.getGraph("system-ui"), model.getGraph("domain"));

		logger.debug("Stored {} in graph <{}> and UI components in graph <{}>", asset, assetGraph, model.getGraph("system-ui"));
	}

	/**
	 * Deletes the given asset from the store
	 *
	 * @param asset the asset to delete
	 * @param store the store
	 */
	public void deleteAsset(AStoreWrapper store, Asset asset) {
		deleteControlSet(store, asset.getUri());
		deleteTWAS(store, asset.getUri());
		deleteImpactLevelsForAsset(store, asset.getUri());
		deleteMetadataOnEntity(store, asset);
		deleteAssetRelationsAndCardinalityConstraints(store, asset.getUri());
	}

	/**
	 * Deletes the given asset from the store
	 *
	 * @param id the ID of the asset to delete
	 * @param store the store
	 */
	public void deleteAsset(AStoreWrapper store, String id) {
		//TODO: STW
		//Should also call deleteMetadataOnEntity() but we don't have the asset only the ID.
		//This appears to be deadcode though.
		//Will remove shortly.
		deleteThingByID(store, id);
	}

	/**
	 * Adds a relation to the store.
	 *
	 * @param relation the relation. Note that you need to remove the old relation in case this is an update operation
	 * @param store the store
	 */
	public void storeRelation(AStoreWrapper store, Relation relation) {

		logger.debug("Storing <{}> in graph <{}>", relation, model.getGraph("system"));

		/* All "manually" added relations will be treated as asserted. Only the validator can add inferred
		 * relations.
		 * 
		 * However, the client/user should not be able to set source and target cardinality, as they are
		 * constrained by the population of the source and target assets. Even if the client/user specifies
		 * relationship cardinality that satisfies the constraints (which they may not), asset populations
		 * can change, at which point relationship cardinality should be recalculated.
		 * The only reasonable option is to keep relationship cardinality in the inferred graph so it can
		 * be updated by the validator as necessary. This means the client/user should not be able to set
		 * relationship cardinality at all. Hence those properties are not updated by this query.
		 */
		String cc = "<" + SparqlHelper.escapeLiteral(model.getNS("system")) +
				relation.getFromID() + "-" + relation.getLabel() + "-" + relation.getToID() + ">";

		String sparql = "INSERT DATA {\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		<" + SparqlHelper.escapeURI(relation.getFrom()) + "> <" + SparqlHelper.escapeURI(relation.getType()) +
				"> <" + SparqlHelper.escapeURI(relation.getTo()) + "> .\n" +
		"		" + cc + " a core:CardinalityConstraint .\n" +
		"		" + cc + " core:linksFrom <" + SparqlHelper.escapeURI(relation.getFrom()) + "> .\n" +
		"		" + cc + " core:linksTo <" + SparqlHelper.escapeURI(relation.getTo()) + "> .\n" +
		"		" + cc + " core:linkType <" + SparqlHelper.escapeURI(relation.getType()) + "> .\n" +
		"	}\n" +
		"}";

		store.update(sparql);
	}

	/**
	 * Updates a relation type in the store.
	 *
	 * @param store the store
	 * @param oldRelation
	 * @param updatedRelation
	 */
	public void updateRelationType(AStoreWrapper store, Relation oldRelation, Relation updatedRelation) {
		/*
		 * Note that this must be done by deleting the old relation and inserting the new one.
		 * 
		 * The URI of a CardinalityConstraint depends on the relationship type. If we only have the
		 * new relation specification, we can't figure out the URI of the relation to be updated. It
		 * is possible that there may be more then one relation between the source and target assets,
		 * so it can't be found unambiguously just by searching for links between the two assets.
		 * 
		 * That means the only solution is for the old value of the link type (at least) to be an
		 * argument, so the method can find and delete the old relation and add the new one with a
		 * completely new CardinalityConstraint having a differernt URI from the old one. It isn't
		 * possible to improve or optimise, any further than that.
		 */
		logger.debug("Updating relation type for <{}> in graph <{}>", oldRelation, model.getGraph("system"));
		
		//For now, just use the existing delete relation, as there are many fields to update
		this.deleteRelation(store, oldRelation);
		
		//For now, use existing store (create) relation method
		this.storeRelation(store, updatedRelation);
		
	}

	/**
	 * No need for a general purpose update method. The only update that makes sense is an update in
	 * the relation type.
	 */	
	
	/**
	 * Deletes the given relation from the store
	 *
	 * @param store the store
	 * @param relation the relation to delete
	 */
	public void deleteRelation(AStoreWrapper store, Relation relation) {
	
		logger.debug("Deleting relation {}", relation);
		String sparql = "DELETE {\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?from ?type ?to .\n" +
		"		?cc a core:CardinalityConstraint .\n" +
		"		?cc core:linksTo ?to .\n" +
		"		?cc core:linksFrom ?from .\n" +
		"		?cc core:linkType ?type .\n" +
		"		?cc core:sourceCardinality ?scard .\n" +
		"		?cc core:targetCardinality ?tcard .\n" +
		"	}\n" +
		"	GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"		?from ?type ?to .\n" +
		"		?cc a core:CardinalityConstraint .\n" +
		"		?cc core:linksTo ?to .\n" +
		"		?cc core:linksFrom ?from .\n" +
		"		?cc core:linkType ?type .\n" +
		"		?cc core:sourceCardinality ?scard .\n" +
		"		?cc core:targetCardinality ?tcard .\n" +
		"	}\n" +
		"}\n" +
		"WHERE {\n" +
		"	BIND (<" + SparqlHelper.escapeURI(relation.getFrom()) + "> AS ?from)\n" +
		"	BIND (<" + SparqlHelper.escapeURI(relation.getType()) + "> AS ?type)\n" +
		"	BIND (<" + SparqlHelper.escapeURI(relation.getTo()) + "> AS ?to)\n" +
		"	OPTIONAL { GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?from ?type ?to .\n" +
		"		?cc a core:CardinalityConstraint .\n" +
		"		?cc core:linksTo ?to .\n" +
		"		?cc core:linksFrom ?from .\n" +
		"		?cc core:linkType ?type .\n" +
		"	}}\n" +
		"}";

		store.update(sparql, model.getGraph("system"), model.getGraph("system-inf"), model.getGraph("system-ui"));
	}

	/**
	 * Toggle the proposed status of a control set and its associated min/max (populations)
	 *
	 * @param store the store
	 * @param cs the control set to be toggled
	 * @return set of updated control set uris
	 */
	public Set<String> updateControlSet(AStoreWrapper store, ControlSet cs) {

		logger.debug("Updating control set {}, proposed: {}, workInProgress: {}, coverage: {}", cs.toString(), cs.isProposed(), cs.isWorkInProgress(), cs.getCoverageLevel());

		String csuri = cs.getUri();
		if (csuri == null) {
			throw new RuntimeException("Control set URI is null");
		}

		logger.debug("Update CS: {}", csuri);
		Set<String> controlSets = new HashSet<>(Arrays.asList(csuri));

		if (cs.getCoverageLevel() != null) {
			logger.debug("Setting coverage: {}", cs.getCoverageLevel());
			String sparql = createUpdateControlSetSparql(cs.getUri(), cs.getAssetUri(), cs.getControl(), cs.isProposed(), cs.isWorkInProgress(), cs.getCoverageLevel());
			store.update(sparql, model.getGraph("domain"), model.getGraph("system"), model.getGraph("system-inf"));
		}
		else {
			controlSets = this.updateControlSets(store, controlSets, cs.isProposed(), cs.isWorkInProgress());
		}

		return controlSets;
	}

	private Set<String> getExpandedControlSets(Set<String> controlSets) {
		Set<String> expandedControlSets = new HashSet<>();

		for (String cs : controlSets) {
			Set<String> expCs = getControlTriplet(cs);
			expandedControlSets.addAll(expCs);
		}

		return expandedControlSets;
	}

	private Set<String> getControlTriplet(String csuri) {
		String[] uriFrags = csuri.split("#");
		String uriPrefix = uriFrags[0];
		String shortUri = uriFrags[1];

		String [] shortUriFrags = shortUri.split("-");
		String control = shortUriFrags[0] + "-" + shortUriFrags[1];
		control = control.replace("_Min", "").replace("_Max", "");
		String assetId = shortUriFrags[2];

		//logger.debug("control: {}", control);
		//logger.debug("assetId: {}", assetId);

		String csAvg = uriPrefix + "#" + control + "-" + assetId;
		String csMin = uriPrefix + "#" + control + "_Min" + "-" + assetId;
		String csMax = uriPrefix + "#" + control + "_Max" + "-" + assetId;

		//logger.debug("csAvg: {}", csAvg);
		//logger.debug("csMin: {}", csMin);
		//logger.debug("csMax: {}", csMax);

		Set<String> controlSets = new HashSet<>(Arrays.asList(csAvg, csMin, csMax));
		return controlSets;
	}

	/**
	 * Toggle the proposed status of multiple control sets
	 *
	 * @param store the store
	 * @param controlSets the control set URIs to be toggled
	 * @param proposed the updated value for all control sets
	 * @param workInProgress the updated value for all control sets
	 */
	public Set<String> updateControlSets(AStoreWrapper store, Set<String> controlSets, boolean proposed, boolean workInProgress) {

		logger.debug("Updating multiple control sets, proposed: {}, workInProgress: {}", proposed, workInProgress);

		if (workInProgress && !proposed) {
			throw new IllegalArgumentException("Controls cannot be work in progress but not proposed");
		}

		Set<String> expandedControlSets = getExpandedControlSets(controlSets);

		for (String cs : expandedControlSets) {
			logger.debug("control set {}, proposed: {}", cs, proposed);

			String sparql = createUpdateControlSetSparql(cs, null, null, proposed, workInProgress, null);
			store.update(sparql, model.getGraph("domain"), model.getGraph("system"), model.getGraph("system-inf"));
		}

		return expandedControlSets;
	}

	/**
	 * Delete the coverage level assertion for a control set
	 *
	 * @param store the store
	 * @param cs the control set to be reverted
	 */
	public void deleteCoverageForControlSet(AStoreWrapper store, ControlSet cs) {

		logger.debug("Deleting control set coverage {}", cs.toString());

		String sparql = createRevertControlSetCoverageSparql(cs.getUri());
		//logger.info(sparql);

		store.update(sparql, model.getGraph("system"), model.getGraph("system-inf"));
	}

	private String createRevertControlSetCoverageSparql(String uri) {
		String sparql =  "DELETE {\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?cs core:hasCoverageLevel ?coverageLevel .\n" +
		"	}\n" +
		"} WHERE {\n" +
		"	BIND(<" + SparqlHelper.escapeURI(uri) + "> AS ?cs)\n" +
		"	GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"		?cs a core:ControlSet .\n" +
		"	}\n" +
		"	OPTIONAL {\n" +
		"		?cs core:hasCoverageLevel ?coverageLevel .\n" +
		"	}\n" +
		"}";

		return sparql;
	}

	private String createUpdateControlSetSparql(String uri, String assetUri, String controlUri, boolean proposed, boolean workInProgress, String coverageLevel) {
		String sparql =  "DELETE {\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?cs core:isProposed ?proposed .\n" +
		"		?cs core:isWorkInProgress ?workInProgress .\n" +
		"	}\n" +
		"	GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"		?cs core:isProposed ?proposed .\n" +
		"		?cs core:isWorkInProgress ?workInProgress .\n" +
		"	}\n";
		if (coverageLevel != null) {
			sparql +=
			"	GRAPH <" + model.getGraph("system") + "> {\n" +
			"		?cs core:hasCoverageLevel ?coverageLevel .\n" +
			"	}\n";
		}
		if (proposed || coverageLevel != null) {
			sparql += "} INSERT {\n";
		}
		if (proposed) {
			sparql +=
			"	GRAPH <" + model.getGraph("system") + "> {\n" +
			"		?cs core:isProposed \"" + SparqlHelper.escapeLiteral(String.valueOf(proposed)) + "\"^^xsd:boolean .\n" +
			"	}\n" +
			"	GRAPH <" + model.getGraph("system-inf") + "> {\n" +
			"		?cs core:isProposed \"" + SparqlHelper.escapeLiteral(String.valueOf(proposed)) + "\"^^xsd:boolean .\n" +
			"	}\n";
		}
		if (workInProgress) {
			sparql +=
			"	GRAPH <" + model.getGraph("system") + "> {\n" +
			"		?cs core:isWorkInProgress \"" + SparqlHelper.escapeLiteral(String.valueOf(workInProgress)) + "\"^^xsd:boolean .\n" +
			"	}\n" +
			"	GRAPH <" + model.getGraph("system-inf") + "> {\n" +
			"		?cs core:isWorkInProgress \"" + SparqlHelper.escapeLiteral(String.valueOf(workInProgress)) + "\"^^xsd:boolean .\n" +
			"	}\n";
		}
		if (coverageLevel != null) {
			sparql +=
			"	GRAPH <" + model.getGraph("system") + "> {\n" +
			"		?cs core:hasCoverageLevel <" + SparqlHelper.escapeURI(coverageLevel) + ">\n" +
			"	}\n";
		}
		sparql += "}" +
		"WHERE {\n";
		if (uri != null) {
			sparql += "	BIND(<" + SparqlHelper.escapeURI(uri) + "> AS ?cs)\n";
		} else {
			sparql += "	BIND(<" + SparqlHelper.escapeURI(assetUri) + "> AS ?a)\n" +
			"	BIND(<" + SparqlHelper.escapeURI(controlUri) + "> AS ?c)\n";
		}
		sparql += "	GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"		?cs a core:ControlSet .\n" +
		"		?cs core:locatedAt ?a .\n" +
		"		?cs core:hasControl ?c .\n" +
		"	}\n" +
		"	OPTIONAL {\n" +
		//not filtering by graph here means we can manually override inferred controls
		//"		GRAPH <" + model.getGraph("system") + "> {\n" +
		"			?cs core:isProposed ?proposed .\n" +
		//"		}\n" +
		"	}\n" +
		"	OPTIONAL {\n" +
		//not filtering by graph here means we can manually override inferred controls
		//"		GRAPH <" + model.getGraph("system") + "> {\n" +
		"			?cs core:isWorkInProgress ?workInProgress .\n" +
		//"		}\n" +
		"	}\n" +
		"	OPTIONAL {\n" +
		//not filtering by graph here means we can manually override inferred controls
		//"		GRAPH <" + model.getGraph("system") + "> {\n" +
		"			?cs core:hasCoverageLevel ?coverageLevel .\n" +
		//"		}\n" +
		"	}\n" +
		"	OPTIONAL {\n" +
		"		GRAPH <" + model.getGraph("system") + "> {\n" +
		"			?cs core:isAssertable ?assertable\n" +
		"		}\n" +
		"	}\n" +
		"}";

		return sparql;
	}

	/**
	 * Toggle the proposed status of a control set
	 *
	 * @param store the store
	 * @param threatUri the URI of the threat to accept
	 * @param justification the explanation why the threat is acceptable
	 */
	public void acceptThreat(AStoreWrapper store, String threatUri, String justification) {

		String sparql;
		if (justification!=null) {
			logger.debug("Accepting threat <{}>, justification: {}", threatUri, justification);

			sparql = "DELETE {\n" +
			"	GRAPH <" + model.getGraph("system") + "> {\n" +
			"		?t core:acceptanceJustification ?aj\n" +
			"	}\n" +
			"} INSERT {\n" +
			"	GRAPH <" + model.getGraph("system") + "> {\n" +
			"		?t core:acceptanceJustification \"" + SparqlHelper.escapeLiteral(justification) + "\"^^xsd:string\n" +
			"	}\n" +
			"}" +
			"WHERE {\n" +
			"	BIND(<" + SparqlHelper.escapeURI(threatUri) + "> AS ?t)\n" +
			"	?t a core:Threat .\n" +
			"	OPTIONAL {?t core:acceptanceJustification ?aj}\n" +
			"}";

		} else {
			logger.debug("Un-accepting threat <{}>", threatUri);

			sparql = "DELETE {\n" +
			"	GRAPH <" + model.getGraph("system") + "> {\n" +
			"		?t core:acceptanceJustification ?aj\n" +
			"	}\n" +
			"}" +
			"WHERE {\n" +
			"	BIND(<" + SparqlHelper.escapeURI(threatUri) + "> AS ?t)\n" +
			"	?t a core:Threat .\n" +
			"	OPTIONAL {?t core:acceptanceJustification ?aj}\n" +
			"}";
		}
		store.update(sparql, model.getGraph("domain"), model.getGraph("system"), model.getGraph("system-inf"));
	}

	/**
	 * Writes the updated TWAS level back to the store. Note that only the level will be written and the update is only
	 * executed if the TWAS already exists in the store.
	 *
	 * @param store the store
	 * @param twas the TrustworthinessAttributeSet
	 */
	public void updateTWAS(AStoreWrapper store, TrustworthinessAttributeSet twas) {

		//the default level gets deleted but never inserted. whenever a twas is saved, the asserted level (which may
		//come from the domain model as a default level) is counted as user asserted.

		String sparql = "DELETE {\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?twas core:hasAssertedLevel ?twaslAsserted\n" +
		"	}\n" +
		"	GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"		?twas core:hasAssertedLevel ?twaslDefault\n" +
		"	}\n" +
		"} INSERT {\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?twas core:hasAssertedLevel <" + SparqlHelper.escapeURI(twas.getAssertedTWLevel().getUri()) + ">\n" +
		"	}\n" +
		"} WHERE {\n" +
		"	BIND (<" + SparqlHelper.escapeURI(twas.getUri()) + "> as ?twas)\n" +
		"	OPTIONAL {\n" +
		"		GRAPH <" + model.getGraph("system") + "> {\n" +
		"			?twas core:hasAssertedLevel ?twaslAsserted .\n" +
		"		}\n" +
		"	}\n" +
		"	OPTIONAL {\n" +
		"		GRAPH <" + model.getGraph("system") + "> {\n" +
		"			?twas core:hasAssertedLevel ?twaslDefault .\n" +
		"		}\n" +
		"	}\n" +
		"}";
		store.update(sparql, model.getGraph("system"), model.getGraph("system-inf"));
	}

	/**
	 * Delete asserted TWAS level
	 *
	 * @param store the store
	 * @param twas the TWAS to be reverted
	 */
	public void deleteAssertedTwLevel(AStoreWrapper store, TrustworthinessAttributeSet twas) {

		logger.debug("Deleting asserted TW level {}", twas.getUri());

		String sparql = createRevertTwasSparql(twas.getUri());

		store.update(sparql, model.getGraph("system"), model.getGraph("system-inf"));
	}

	private String createRevertTwasSparql(String uri) {
		String sparql =  "DELETE {\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?twas core:hasAssertedLevel ?twaslAsserted .\n" +
		"	}\n" +
		"} WHERE {\n" +
		"	BIND(<" + SparqlHelper.escapeURI(uri) + "> AS ?twas)\n" +
		"	OPTIONAL {\n" +
		"		GRAPH <" + model.getGraph("system") + "> {\n" +
		"			?twas core:hasAssertedLevel ?twaslAsserted .\n" +
		"		}\n" +
		"	}\n" +
		"}";

		return sparql;
	}

	/**
	 * Writes the updated MS back to the store. Note that only the impact level will be written and the update is only
	 * executed if the MS already exists in the store.
	 *
	 * @param store the store
	 * @param ms the MisbehaviourSet
	 */
	public void updateMS(AStoreWrapper store, MisbehaviourSet ms) {

		String sparql = "DELETE {\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?ms core:hasImpactLevel ?mslAsserted\n" +
		"	}\n" +
		//KEM - don't delete impact level in inferred graph (this should be kept as the default value)
		//"	GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		//"		?ms core:hasImpactLevel ?mslInferred\n" +
		//"	}\n" +
		"} INSERT {\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?ms core:hasImpactLevel <" + SparqlHelper.escapeURI(ms.getImpactLevel().getUri()) + ">\n" +
		"	}\n" +
		"} WHERE {\n" +
		"	BIND (<" + SparqlHelper.escapeURI(ms.getUri()) + "> as ?ms)\n" +
		"	OPTIONAL {\n" +
		"		GRAPH <" + model.getGraph("system") + "> {\n" +
		"			?ms core:hasImpactLevel ?mslAsserted .\n" +
		"		}\n" +
		"	}\n" +
		"	OPTIONAL {\n" +
		"		GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"			?ms core:hasImpactLevel ?mslInferred .\n" +
		"		}\n" +
		"	}\n" +
		"}";
		store.update(sparql, model.getGraph("system"), model.getGraph("system-inf"));
	}

	/**
	 * Delete asserted impact level for misbehaviour
	 *
	 * @param store the store
	 * @param ms the MisbehaviourSet to be reverted
	 */
	public void deleteAssertedImpactLevel(AStoreWrapper store, MisbehaviourSet ms) {

		logger.debug("Deleting asserted impact level {}", ms.getUri());

		String sparql = createRevertImpactSparql(ms.getUri());
		logger.info(sparql);

		store.update(sparql, model.getGraph("system"), model.getGraph("system-inf"));
	}

	private String createRevertImpactSparql(String uri) {
		String sparql =  "DELETE {\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?ms core:hasImpactLevel ?mslAsserted .\n" +
		"	}\n" +
		"} WHERE {\n" +
		"	BIND(<" + SparqlHelper.escapeURI(uri) + "> AS ?ms)\n" +
		"	OPTIONAL {\n" +
		"		GRAPH <" + model.getGraph("system") + "> {\n" +
		"			?ms core:hasImpactLevel ?mslAsserted .\n" +
		"		}\n" +
		"	}\n" +
		"}";

		return sparql;
	}

	/**
	 * Remove all references to inferred assets from the asserted graph
	 *
	 * @param store the store
	 */
	public void cleanAssertedModel(AStoreWrapper store) {

		//get all triples from the (asserted) system graph that have a reference an inferred asset
		String sparql = "SELECT DISTINCT ?x ?p ?a ?y WHERE {\n" +
		"	GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"		?a a ?asset .\n" +
		"	}\n" +
		"	GRAPH <" + model.getGraph("domain") + "> {\n" +
		"		?asset rdfs:subClassOf* core:Asset .\n" +
		"		?p a owl:ObjectProperty .\n" +
		"	}\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		{ ?x ?p ?a . } UNION { ?a ?p ?y }\n" +
		"	}\n" +
		"}";
		List<Map<String, String>> result = store.translateSelectResult(store.querySelect(
			AStoreWrapper.addGraphsToSparql(sparql, AStoreWrapper.SparqlType.QUERY,
				model.getGraph("domain"),
				model.getGraph("system"),
				model.getGraph("system-inf")
		)));

		//remove triples
		sparql = "DELETE DATA { GRAPH <" + SparqlHelper.escapeURI(model.getGraph("system")) + "> {\n";
		for (Map<String, String> t: result) {
			if (t.containsKey("x")) {
				sparql += "	<" + t.get("x") + "> <" + t.get("p") + "> <" + t.get("a") + "> .\n";
			} else if (t.containsKey("y")) {
				sparql += "	<" + t.get("a") + "> <" + t.get("p") + "> <" + t.get("y") + "> .\n";
			}
		}
		sparql += "}}";
		store.update(sparql);
	}

	/**
	 * Deletes the asset with the given URI and all its relations (incoming and outgoing) and
	 * cardinality constraints
	 *
	 * @param store the store
	 * @param uri the URI of the thing
	 */
	private boolean deleteAssetRelationsAndCardinalityConstraints(AStoreWrapper store, String uri) {

		logger.debug("Deleting <{}>", uri);

		String deleteString = "GRAPH <%s> {\n" +
		"		?thing ?p1 ?o .\n" +
		"		?s ?p2 ?thing .\n" +
		"		?cc1 a core:CardinalityConstraint .\n" +
		"		?cc1 core:sourceCardinality ?scard .\n" +
		"		?cc1 core:targetCardinality ?tcard .\n" +
		"		?cc1 core:linksTo ?thing .\n" +
		"		?cc1 core:linksFrom ?from .\n" +
		"		?cc1 core:linkType ?type .\n" +
		"		?cc2 a core:CardinalityConstraint .\n" +
		"		?cc2 core:sourceCardinality ?scard .\n" +
		"		?cc2 core:targetCardinality ?tcard .\n" +
		"		?cc2 core:linksFrom ?thing .\n" +
		"		?cc2 core:linksTo ?to .\n" +
		"		?cc2 core:linkType ?type .\n" +
		"	}\n";
		String whereString = 
		"WHERE {\n" +
		"	BIND (<" + SparqlHelper.escapeURI(uri) + "> AS ?thing)\n" +
		"	OPTIONAL {?thing ?p1 ?o}\n" +
		"	OPTIONAL {?s ?p2 ?thing}\n" +
		"	OPTIONAL {?cc1 core:linksTo ?thing .\n" +
		"	?cc1 core:linksFrom ?from .\n" +
		"	?cc1 core:targetCardinality ?tcard .\n" +
		"	?cc1 core:sourceCardinality ?scard .\n" +
		"	?cc1 core:linkType ?type}\n" +
		"	OPTIONAL {?cc2 core:linksFrom ?thing .\n" +
		"	?cc2 core:linksTo ?to .\n" +
		"	?cc2 core:targetCardinality ?tcard .\n" +
		"	?cc2 core:sourceCardinality ?scard .\n" +
		"	?cc2 core:linkType ?type}\n" +
		"}";

		String sparql = "DELETE {\n" + 
			//Have removed reference to system-inf below to optimise, i.e. only specifically delete asserted triples
			//TODO: however, is this actually correct below? Why do we use the same deleteString for the UI graph?
			String.format(deleteString, model.getGraph("system")) +
			String.format(deleteString, model.getGraph("system-ui")) +
			"}\n" + 
			whereString;

		return store.update(sparql, model.getGraph("system"), model.getGraph("system-ui"));
	}

	/**
	 * Delete all control sets belonging to a particular asset
	 * @param uri the URI of the asset that has the control set to delete
	 */
	private boolean deleteControlSet(AStoreWrapper store, String assetUri) {
		String sparql =  "DELETE {\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?cs core:isProposed ?proposed .\n" +
		"		?cs core:isWorkInProgress ?workInProgress .\n" +
		"	}\n" +
		"   } WHERE {\n" + 
		"	    BIND(<" + SparqlHelper.escapeURI(assetUri) + "> AS ?asset)\n" + 
		"       ?cs core:locatedAt ?asset .\n" + 
		"		OPTIONAL { ?cs core:isProposed ?proposed }\n" +
		"		OPTIONAL { ?cs core:isWorkInProgress ?workInProgress }\n" +
		"   }";

		return store.update(sparql, model.getGraph("system"), model.getGraph("system-inf"));
	}

	/**
	 * Delete all trustworthiness attribute sets belonging to a particular asset
	 * @param uri the URI of the asset 
	 */
	private boolean deleteTWAS(AStoreWrapper store, String assetUri) {
		String sparql =  "DELETE {\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?twas core:hasAssertedLevel ?level .\n" +
		"	}\n" +
		"   } WHERE {\n" + 
		"	    BIND(<" + SparqlHelper.escapeURI(assetUri) + "> AS ?asset)\n" + 
		"       ?twas core:locatedAt ?asset .\n" + 
		"		OPTIONAL { ?twas core:hasAssertedLevel ?level }\n" +
		"   }";

		return store.update(sparql, model.getGraph("system"), model.getGraph("system-inf"));
	}

	/**
	 * Delete all asserted impact levels for misbehaviour sets related to a particular asset
	 * @param uri the URI of the asset 
	 */
	private boolean deleteImpactLevelsForAsset(AStoreWrapper store, String assetUri) {
		String sparql =  "DELETE {\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?ms core:hasImpactLevel ?level .\n" +
		"	}\n" +
		"   } WHERE {\n" + 
		"	    BIND(<" + SparqlHelper.escapeURI(assetUri) + "> AS ?asset)\n" + 
		"       ?ms core:locatedAt ?asset .\n" + 
		"		OPTIONAL { ?ms core:hasImpactLevel ?level }\n" +
		"   }";

		return store.update(sparql, model.getGraph("system"), model.getGraph("system-inf"));
	}

	/**
	 * Deletes the asset group with the given URI and all its relations (incoming and outgoing)
	 * // TODO -- it's not clear that an asset group has any relations
	 *
	 * @param store the store
	 * @param uri the URI of the asset group
	 */
	private boolean deleteAssetGroup(AStoreWrapper store, String uri) {

		logger.debug("Deleting <{}>", uri);

		String sparql = "DELETE {\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?thing ?p1 ?o .\n" +
		"		?s ?p2 ?thing .\n" +
		"	}\n" +
		"	GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"		?thing ?p1 ?o .\n" +
		"		?s ?p2 ?thing .\n" +
		"	}\n" +
		"	GRAPH <" + model.getGraph("system-ui") + "> {\n" +
		"		?thing ?p1 ?o .\n" +
		"		?s ?p2 ?thing .\n" +
		"	}\n" +
		"}\n" +
		"WHERE {\n" +
		"	BIND (<" + SparqlHelper.escapeURI(uri) + "> AS ?thing)\n" +
		"	OPTIONAL {?thing ?p1 ?o}\n" +
		"	OPTIONAL {?s ?p2 ?thing}\n" +
		"}";
		return store.update(sparql, model.getGraph("system"), model.getGraph("system-inf"), model.getGraph("system-ui"));
	}


	/**
	 * Deletes the thing with the given URI and all its relations (incoming and outgoing)
	 *
	 * @param store the store
	 */
	private boolean deleteThingByID(AStoreWrapper store, String id) {

		logger.debug("Deleting thing with ID {}", id);

		String sparql = "DELETE {\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"		?thing ?p1 ?o .\n" +
		"		?s ?p2 ?thing .\n" +
		"	}\n" +
		"	GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"		?thing ?p1 ?o .\n" +
		"		?s ?p2 ?thing .\n" +
		"	}\n" +
		"	GRAPH <" + model.getGraph("system-ui") + "> {\n" +
		"		?thing ?p1 ?o .\n" +
		"		?s ?p2 ?thing .\n" +
		"	}\n" +
		"}\n" +
		"WHERE {\n" +
		"	?thing core:hasID \"" + id + "\"^^xsd:string .\n" +
		"	OPTIONAL {?thing ?p1 ?o}\n" +
		"	OPTIONAL {?s ?p2 ?thing}\n" +
		"}";
		return store.update(sparql, model.getGraph("system"), model.getGraph("system-inf"), model.getGraph("system-ui"));
	}

	public void setIsValidating(AStoreWrapper store, boolean validating) {
		setModelFlag(store, "core:isValidating", validating);
	}

	public void setIsValid(AStoreWrapper store, boolean valid) {
		setModelFlag(store, "core:isValid", valid);
	}

	public void setRisksValid(AStoreWrapper store, boolean valid) {
		setModelFlag(store, "core:risksValid", valid);
	}

	public void setIsCalculatingRisk(AStoreWrapper store, boolean calculating) {
		setModelFlag(store, "core:isCalculatingRisk", calculating);
	}

	public void setIsCalculatingControls(AStoreWrapper store, boolean calculatingControls) {
		setModelFlag(store, "core:isCalculatingControls", calculatingControls);
	}

	private void setModelFlag(AStoreWrapper store, String flag, boolean value) {
		String modelGraph = model.getGraph("system");
		store.update("DELETE {\n" +
				"	GRAPH <" + modelGraph + "> { ?model " + flag + " ?v .}\n" +
				"} INSERT {\n" +
				"	GRAPH <" + modelGraph + "> { ?model " + flag + " \"" + (value?"true":"false") + "\"^^xsd:boolean .}\n" +
				"} WHERE {\n" +
				"	GRAPH <" + modelGraph + "> {\n" +
				"		BIND (<" + modelGraph + "> AS ?model)\n" +
				"		OPTIONAL { ?model " + flag + " ?v }\n" +
				"	}\n" +
				"}");
	}

	public void setCreateDate(AStoreWrapper store, Date date) {
		String modelGraph = model.getGraph("system");
		String modelInfGraph = model.getGraph("system-inf");

		String dateString = (new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")).format(date);
		setCreateDate(store, dateString);
	}

	public void setCreateDate(AStoreWrapper store, String dateString) {
		String modelGraph = model.getGraph("system");
		String modelInfGraph = model.getGraph("system-inf");

		String predicate = "<http://purl.org/dc/terms/created>";

		store.update("DELETE {\n" +
				"	GRAPH <" + modelInfGraph + "> { ?model " + predicate + " ?createDate .}\n" +
				"} INSERT {\n" +
				"	GRAPH <" + modelInfGraph + "> { ?model " + predicate + " \"" + dateString + "\"^^xsd:string .}\n" +
				"} WHERE {\n" +
				"	GRAPH <" + modelInfGraph + "> {\n" +
				"		BIND (<" + modelGraph + "> AS ?model)\n" +
				"		OPTIONAL { ?model " + predicate + " ?createDate }\n" +
				"	}\n" +
				"}");
	}

	public void setModifiedDate(AStoreWrapper store, Date date) {
		String modelGraph = model.getGraph("system");
		String modelInfGraph = model.getGraph("system-inf");

		String dateString = (new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")).format(date);
		String predicate = "<http://purl.org/dc/terms/modified>";

		store.update("DELETE {\n" +
				"	GRAPH <" + modelInfGraph + "> { ?model " + predicate + " ?createDate .}\n" +
				"} INSERT {\n" +
				"	GRAPH <" + modelInfGraph + "> { ?model " + predicate + " \"" + dateString + "\"^^xsd:string .}\n" +
				"} WHERE {\n" +
				"	GRAPH <" + modelInfGraph + "> {\n" +
				"		BIND (<" + modelGraph + "> AS ?model)\n" +
				"		OPTIONAL { ?model " + predicate + " ?createDate }\n" +
				"	}\n" +
				"}");
	}

	/**
	 * Adds a new metadata pair (key, value) to the entity. If the entity already has a metadata pair with the same
	 * key and value then this method will skip and return false. Otherwise, and if the metadata pair is added
	 * successfully, this method will return true.
	 *
	 * @param store
	 * @param entity Entity which the metadata pair will be added to
	 * @param metadata metadata to be added
	 * @return whether the metadata pair was successfully added
	 */
	public boolean addMetadataPairToEntity(AStoreWrapper store, SemanticEntity entity, MetadataPair metadata) {
		if (metadata == null) {
			throw new IllegalArgumentException("MetadataPair cannot be null");
		}
		if (metadata.getKey() == null) {
			throw new IllegalArgumentException("MetadataPair key cannot be null");
		}
		if (metadata.getValue() == null) {
			throw new IllegalArgumentException("MetadataPair value cannot be null");
		}

		String key = SparqlHelper.escapeLiteral(metadata.getKey());
		String value = SparqlHelper.escapeLiteral(metadata.getValue());

		String metaUri =  model.getNS("system") + "Meta-" + Integer.toHexString(key.hashCode()) +
				"-" + Integer.toHexString(value.hashCode());

		// Check no metadata pairs with the same key and value already exist for this entity
		String query = String.format("SELECT ?metaUri WHERE {\n" +
				"  GRAPH <%s> {\n" +
				"    <%s> core:hasMetadata ?metaUri .\n" +
				"    ?metaUri core:hasKey '%s' .\n" +
				"    ?metaUri core:hasValue '%s' .\n" +
				"  }\n" +
				"}", model.getGraph("system-meta"), entity.getUri(), key, value);
		List<?> results = store.translateSelectResult(store.querySelect(query));
		if (results.size() > 0) {
			logger.warn(
				"Attempted to add duplicate metadata ({}:{}) to entity <{}>.",
				metadata.getKey(),
				metadata.getValue(),
				entity.getUri()
			);
			return false;
		}

		query = String.format("INSERT DATA {\n" +
			"  GRAPH <%s> {\n" +
			"    <%s> core:hasKey '%s' .\n" +
			"    <%s> core:hasValue '%s' .\n" +
			"    <%s> core:hasMetadata <%s> \n" +
			"  }\n" +
			"}",
			model.getGraph("system-meta"), metaUri, key, metaUri, value, entity.getUri(), metaUri);

		return store.update(query);
	}

	/**
	 * Delete all metadata associated with the given entity
	 * @param store
	 * @param entity
	 */
	public void deleteMetadataOnEntity(AStoreWrapper store, SemanticEntity entity) {
		String query = String.format("DELETE {\n" +
			"  GRAPH <%s> {\n" +
			"    ?uri core:hasMetadata ?metaUri .\n" +
			"    ?metaUri ?p ?o .\n" +
			"  }\n" +
			"} WHERE {\n" +
			"  GRAPH <%s> {\n" +
			"    BIND (<%s> as ?uri) .\n" +
			"    ?uri core:hasMetadata ?metaUri .\n" +
			"    OPTIONAL {\n" +
			"      FILTER NOT EXISTS {\n" +
			"        ?other core:hasMetadata ?metaUri .\n" +
			"        FILTER (?other != ?uri) .\n" +
			"      }\n" +
			"      ?metaUri ?p ?o .\n" +
			"    }\n" +
			"  }\n" +
			"}", model.getGraph("system-meta"), model.getGraph("system-meta"),  entity.getUri());
		store.update(query);
	}

	/**
	 * Stores a new asset group or updates an existing asset group (if an asset group with an identical URI is already
	 * present)
	 * @param store
	 * @param assetGroup
	 * @return
	 */
	public boolean storeAssetGroup(AStoreWrapper store, AssetGroup assetGroup) {
		String escapedUri = SparqlHelper.escapeURI(assetGroup.getUri());

		boolean updated = store.update(String.format("WITH <%s>\n" +
				"DELETE {\n" +
				"  ?assetGroup ?p ?o\n" +
				"} WHERE {\n" +
				"  BIND (<%s> as ?assetGroup)\n" +
				"  ?assetGroup ?p ?o\n" +
				"}", model.getGraph("system-ui"), assetGroup.getUri()));
		if (updated) {
			logger.info("Found existing asset group with URI {}, updating", assetGroup.getUri());
		}

		SystemModelQuerier querier = new SystemModelQuerier(model);
		for (Asset asset : assetGroup.getAssets().values()) {
			AssetGroup existingAssetGroup = querier.getAssetGroupOfAsset(store, asset);
			if (existingAssetGroup != null) {
				logger.error("Error when creating new asset group: asset {} is already in group {}",
						asset.getUri(), existingAssetGroup);
				return false;
			}
		}

		StringBuilder sb = new StringBuilder();
		sb.append("INSERT DATA {\n");
		sb.append("  GRAPH <").append(model.getGraph("system-ui")).append("> {\n");
		sb.append("    <").append(escapedUri).append("> a core:AssetGroup ;\n");
		sb.append("      rdfs:label \"").append(
				SparqlHelper.escapeLiteral(assetGroup.getLabel())).append("\" ;\n");
		sb.append("      core:positionX \"").append(assetGroup.getX()).append("\"^^xsd:int ;\n");
		sb.append("      core:positionY \"").append(assetGroup.getY()).append("\"^^xsd:int ;\n");
		sb.append("      core:width \"").append(assetGroup.getWidth()).append("\"^^xsd:int ;\n");
		sb.append("      core:height \"").append(assetGroup.getHeight()).append("\"^^xsd:int ;\n");
		sb.append("      core:isExpanded \"").append(assetGroup.isExpanded()).append("\"^^xsd:boolean ;\n");
        sb.append("      core:hasID \"").append(assetGroup.getID()).append("\" ;\n");
		for (String assetUri : assetGroup.getAssets().keySet()) {
			sb.append("      core:hasAsset <").append(
					SparqlHelper.escapeURI(assetUri)).append("> ;\n");
		}
		sb.append("  }\n");
		sb.append("}\n");

		return store.update(sb.toString());
	}

	public boolean addAssetsToAssetGroup(AStoreWrapper store, AssetGroup assetGroup, Set<Asset> assets) {
		SystemModelQuerier querier = new SystemModelQuerier(model);

		for (Asset asset : assets) {
			AssetGroup existingAssetGroup = querier.getAssetGroupOfAsset(store, asset);
			if (existingAssetGroup != null && existingAssetGroup.getUri() != assetGroup.getUri()) {
				logger.error("Error when adding assets to asset group {}: asset {} is already in group {}",
						assetGroup.getUri(), asset.getUri(), existingAssetGroup);
				return false;
			}
		}

		StringBuilder sb = new StringBuilder();
		sb.append("INSERT DATA {\n");
		sb.append("  GRAPH <").append(model.getGraph("system-ui")).append("> {\n");
		for (Asset asset : assets) {
			if (assetGroup.addAsset(asset)) {
				sb.append("    <").append(assetGroup.getUri()).append("> core:hasAsset <").append(asset.getUri()).append("> .\n");
			} else {
				logger.warn("Attempted to add asset {} to a group {} it is already in", asset.getUri(),
						assetGroup.getUri());
			}
		}
		sb.append("  }\n");
		sb.append("}\n");
		return store.update(sb.toString());
	}

	public boolean removeAssetsFromAssetGroup(AStoreWrapper store, AssetGroup assetGroup, Set<Asset> assets) {
		StringBuilder sb = new StringBuilder();
		sb.append("DELETE DATA {\n");
		sb.append("  GRAPH <").append(model.getGraph("system-ui")).append("> {\n");
		for (Asset asset : assets) {
			if (assetGroup.removeAsset(asset)) {
				sb.append("    <").append(assetGroup.getUri()).append("> core:hasAsset <").append(asset.getUri()).append("> .\n");
			} else {
				logger.warn("Attempted to delete asset {} from a group {} it is not in", asset.getUri(),
						assetGroup.getUri());
			}
		}
		sb.append("  }\n");
		sb.append("}\n");
		return store.update(sb.toString());
	}

	public boolean deleteAssetGroup(AStoreWrapper store, AssetGroup assetGroup, boolean deleteAssets) {
		if (deleteAssets) {
			for (Asset asset : assetGroup.getAssets().values()) {
				deleteAsset(store, asset);
			}
		}
		return deleteAssetGroup(store, assetGroup.getUri());
	}

	public void updateAssetGroupLocation(AStoreWrapper store, AssetGroup assetGroup, int x, int y) {
		String query = String.format(
				"WITH <%s>\n" +
				"DELETE {\n" +
				"  ?assetGroup core:positionX ?x .\n" +
				"  ?assetGroup core:positionY ?y .\n" +
				"} INSERT {\n" +
				"  ?assetGroup core:positionX \"%d\"^^xsd:int .\n" +
				"  ?assetGroup core:positionY \"%d\"^^xsd:int .\n" +
				"} WHERE {\n" +
				"  BIND (<%s> as ?assetGroup)\n" +
				"  ?assetGroup core:positionX ?x .\n" +
				"  ?assetGroup core:positionY ?y .\n" +
				"}", model.getGraph("system-ui"), x, y, assetGroup.getUri());

		store.update(query);
		assetGroup.setX(x);
		assetGroup.setY(y);
	}

	public void updateAssetGroupSize(AStoreWrapper store, AssetGroup assetGroup, int width, int height) {
		String query = String.format(
				"WITH <%s>\n" +
				"DELETE {\n" +
				"  ?assetGroup core:width ?w .\n" +
				"  ?assetGroup core:height ?h .\n" +
				"} INSERT {\n" +
				"  ?assetGroup core:width \"%d\"^^xsd:int .\n" +
				"  ?assetGroup core:height \"%d\"^^xsd:int .\n" +
				"} WHERE {\n" +
				"  BIND (<%s> as ?assetGroup)\n" +
				"  OPTIONAL { ?assetGroup core:width ?w }\n" +
				"  OPTIONAL { ?assetGroup core:height ?h }\n" +
				"}", model.getGraph("system-ui"), width, height, assetGroup.getUri());

		store.update(query);
		assetGroup.setWidth(width);
		assetGroup.setHeight(height);
	}

	public void updateAssetGroupExpanded(AStoreWrapper store, AssetGroup assetGroup, boolean expanded) {
		String query = String.format(
				"WITH <%s>\n" +
				"DELETE {\n" +
				"  ?assetGroup core:isExpanded ?expanded .\n" +
				"} INSERT {\n" +
				"  ?assetGroup core:isExpanded \"%s\"^^xsd:boolean .\n" +
				"} WHERE {\n" +
				"  BIND (<%s> as ?assetGroup)\n" +
				"  OPTIONAL { ?assetGroup core:isExpanded ?expanded }\n" +
				"}", model.getGraph("system-ui"), expanded, assetGroup.getUri());

		store.update(query);
		assetGroup.setExpanded(expanded);
	}

	public void updateAssetGroupLabel(AStoreWrapper store, AssetGroup assetGroup, String label) {
		String query = String.format(
				"WITH <%s>\n" +
				"DELETE {\n" +
				"  ?assetGroup rdfs:label ?label .\n" +
				"} INSERT {\n" +
				"  ?assetGroup rdfs:label \"%s\" .\n" +
				"} WHERE {\n" +
				"  BIND (<%s> as ?assetGroup)\n" +
				"  ?assetGroup rdfs:label ?label .\n" +
				"}", model.getGraph("system-ui"), label, assetGroup.getUri());

		store.update(query);
		assetGroup.setLabel(label);
	}

	/**
	 * Get the current model for this model updater
	 *
	 * @return the model
	 */
	public ModelStack getModel() {
		return model;
	}
}
