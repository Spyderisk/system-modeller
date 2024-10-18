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
//      Modified By :	        Josh Harris
//      Created Date :          2017-02-16
//      Created for Project :   5G-ENSURE
//      Modified By:            Mike Surridge
//      Modified for Project:   FOGPROTECT
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelquerier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.soton.itinnovation.security.model.Level;
import uk.ac.soton.itinnovation.security.model.SemanticEntity;
import uk.ac.soton.itinnovation.security.model.domain.Control;
import uk.ac.soton.itinnovation.security.model.domain.ControlStrategy.ControlStrategyType;
import uk.ac.soton.itinnovation.security.model.system.Asset;
import uk.ac.soton.itinnovation.security.model.system.AssetGroup;
import uk.ac.soton.itinnovation.security.model.system.ComplianceSet;
import uk.ac.soton.itinnovation.security.model.system.ComplianceThreat;
import uk.ac.soton.itinnovation.security.model.system.ControlSet;
import uk.ac.soton.itinnovation.security.model.system.ControlStrategy;
import uk.ac.soton.itinnovation.security.model.system.Link;
import uk.ac.soton.itinnovation.security.model.system.MetadataPair;
import uk.ac.soton.itinnovation.security.model.system.MisbehaviourSet;
import uk.ac.soton.itinnovation.security.model.system.Model;
import uk.ac.soton.itinnovation.security.model.system.Node;
import uk.ac.soton.itinnovation.security.model.system.Pattern;
import uk.ac.soton.itinnovation.security.model.system.Relation;
import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;
import uk.ac.soton.itinnovation.security.model.system.SecondaryEffectStep;
import uk.ac.soton.itinnovation.security.model.system.Threat;
import uk.ac.soton.itinnovation.security.model.system.TrustworthinessAttributeSet;
import uk.ac.soton.itinnovation.security.modelquerier.util.ModelStack;
import uk.ac.soton.itinnovation.security.semanticstore.AStoreWrapper;
import uk.ac.soton.itinnovation.security.semanticstore.util.SparqlHelper;

/**
 * Analogous to the system model querier, this class retrieves entities from a system model (model). It returns POJOs as
 * defined in the data model.
 */
public class SystemModelQuerier extends AModelQuerier {

	private static final Logger logger = LoggerFactory.getLogger(SystemModelQuerier.class);

	public static final String PREFIX = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/";

	public SystemModelQuerier(ModelStack model) {
		super(model);
	}

	// General model actions //////////////////////////////////////////////////////////////////////////////////////////
	public Model getModelInfo(AStoreWrapper store) {

		//logger.debug("Getting system model info");

		String sparql = "SELECT DISTINCT ?m ?domain ?validatedDomainVersion ?label ?desc (STR(?v) AS ?valid) ?rcm " +
		"(STR(?risksVal) AS ?risksValid) (STR(?val) AS ?validating) (STR(?cR) AS ?calculatingRisk) ?r WHERE {\n" +
		"	GRAPH <" + model.getGraph("system") + "> {\n" +
		"	BIND (<" + model.getGraph("system") + "> AS ?m)" +
		"	OPTIONAL { ?m a owl:Ontology }\n" +
		"	OPTIONAL { ?m core:domainGraph ?domain }\n" +
		"	OPTIONAL { ?m rdfs:label ?label }\n" +
		"	OPTIONAL { ?m rdfs:comment ?desc }\n" +
		"	OPTIONAL { ?m core:isValid ?v }\n" +
		"	OPTIONAL { ?m core:risksValid ?risksVal }\n" +
		"	OPTIONAL { ?m core:isValidating ?val }\n" +
		"	OPTIONAL { ?m core:isCalculatingRisk ?cR }\n" +
		"	OPTIONAL { ?m core:riskCalculationMode ?rcm }\n" +
		"	}\n" +
		"	GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"	BIND (<" + model.getGraph("system") + "> AS ?m)" +
		"	OPTIONAL { ?m core:hasRisk ?r }\n" +
		"	OPTIONAL { ?system core:domainVersion ?validatedDomainVersion }\n" +
		"	}\n" +
		"}";

		List<Map<String, String>> result = store.translateSelectResult(store.querySelect(sparql,
			model.getGraph("system"), model.getGraph("system-inf")
		));

		Model m = new Model();
		Iterator<Map<String, String>> it = result.iterator();
		while (it.hasNext()) {
			Map<String, String> row = it.next();

			if (m.getUri() == null) {
				m.setUri(row.get("m"));
			} else {
				logger.warn("Ignoring duplicate model URI <{}>", row.get("m"));
			}
			if (m.getDomain() == null) {
				m.setDomain(row.get("domain"));
			} else {
				logger.warn("Ignoring duplicate domain <{}>", row.get("domain"));
			}
			if (m.getValidatedDomainVersion() == null) {
				//logger.info("getModelInfo: validatedDomainVersion = {}", row.get("validatedDomainVersion"));
				m.setValidatedDomainVersion(row.get("validatedDomainVersion"));
			} else {
				logger.warn("Ignoring duplicate validatedDomainVersion <{}>", row.get("validatedDomainVersion"));
			}
			if (m.getLabel() == null) {
				m.setLabel(row.get("label"));
			} else {
				logger.warn("Ignoring duplicate model label \"{}\"", row.get("label"));
			}
			if (m.getDescription() == null) {
				m.setDescription(row.get("desc"));
			} else {
				logger.warn("Ignoring duplicate model description \"{}\"", row.get("desc"));
			}
			if (row.containsKey("valid") && row.get("valid")!=null) {
				m.setValid(Boolean.valueOf(row.get("valid")));
			}
			if (row.containsKey("risksValid") && row.get("risksValid")!=null) {
				m.setRisksValid(Boolean.valueOf(row.get("risksValid")));
			}
			if (row.containsKey("validating") && row.get("validating")!=null) {
				m.setValidating(Boolean.valueOf(row.get("validating")));
			}
			if (row.containsKey("calculatingRisk") && row.get("calculatingRisk")!=null) {
				m.setCalculatingRisk(Boolean.valueOf(row.get("calculatingRisk")));
			}
			if (row.containsKey("rcm") && row.get("rcm") != null) {
				m.setRiskCalculationMode(RiskCalculationMode.valueOf(row.get("rcm")));
			}
			if (row.containsKey("r") && row.get("r")!=null) {
				Map<String, Level> riskLevels = getLevels(store, "RiskLevel");
				if (riskLevels.containsKey(row.get("r"))) {
					m.setRisk(riskLevels.get(row.get("r")));
				} else {
					logger.warn("Could not find risk level \"{}\" for model", row.get("r"));
				}
			}
		}
		return m;
	}

	public String getDomainEntityType(AStoreWrapper store, String uri) {
		String query = String.format("\r\nSELECT DISTINCT * WHERE {\r\n" + 
				"    GRAPH <%s> {\r\n" + 
				(uri != null ? "    BIND (<" + SparqlHelper.escapeURI(uri) + "> as ?uri) .\n" : "") +
				"    ?uri rdf:type ?type .\r\n" + 
				"    }\r\n" +
				"}", model.getGraph("domain"));

		logger.debug(query);

		List<Map<String, String>> rows = store.translateSelectResult(store.querySelect(query,
			model.getGraph("domain")
		));

		logger.debug("rows: {}", rows.size());

		if (rows.size() > 1) {
			throw new RuntimeException("Duplicate entries found for uri: " + uri);
		}
		else if (rows.size() == 1) {
			Map<String, String> row = rows.get(0);

			logger.debug("uri: {}", row.get("uri"));
			logger.debug("type: {}", row.get("type"));
			return row.get("type");
		}

		return null;
	}

	// Assets /////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Get all system-specific assets
	 *
	 * @param store the store to query
	 * @return map of all assets
	 */
	public Map<String, Asset> getSystemAssets(AStoreWrapper store) {
		return getSystemAssets(store, getControlSets(store), getMisbehaviourSets(store, false), getTrustworthinessAttributeSets(store), true, true);
	}

	/**
	 * Get all system-specific assets with only basic information (URI, label, type and ID)
	 * 
	 * @param store the store to query
	 * @return map of all assets
	 */
	public Map<String, Asset> getBasicSystemAssets(AStoreWrapper store) {
		return getSystemAssets(store, null, null, null, false, false);
	}
	
	/**
	 * Get system model assets
	 *
	 * @param store the store to query
	 * @param controlSets map of ControlSets (can be null); if defined then the asset ControlSets are queried and objects in the map are attached to the result
	 * @param misbehaviourSets map of MisbehaviourSets (can be null); if defined then the asset MisbehaviourSets are queried and objects in the map are attached to the result
	 * @param trustworthinessAttributeSets map of TrustworthinessAttributeSets; if defined then the asset TrustworthinessAttributeSets are queried and objects in the map are attached to the result
	 * @param getInferredAssets whether to return the inferred assets or not
	 * @param getAssetPosition whether to return the asset UI positions or not
	 * @return map of all assets
	 */
	public Map<String, Asset> getSystemAssets (AStoreWrapper store,
			Map<String, ControlSet> controlSets,
			Map<String, MisbehaviourSet> misbehaviourSets,
			Map<String, TrustworthinessAttributeSet> trustworthinessAttributeSets,
			boolean getInferredAssets,
			boolean getAssetPosition) {
		return getSystemAssets(store, null, null, controlSets, misbehaviourSets, trustworthinessAttributeSets, getInferredAssets, getAssetPosition);
	}

	/**
	 * Get a system model asset
	 *
	 * @param store the store to query
	 * @param assetURI the URI of the asset
	 * @return the asset or null if it doesn't exist
	 */
	public Asset getSystemAsset(AStoreWrapper store, String assetURI) {

		Map<String, Asset> assets = getSystemAssets(store, assetURI, null, getControlSets(store), getMisbehaviourSets(store, true), getTrustworthinessAttributeSets(store), true, true);	
		return assets.get(assetURI);
	}

	/**
	 * Get a system model asset
	 *
	 * @param store the store to query
	 * @param assetID the URI of the asset
	 * @return the asset or null if it doesn't exist
	 */
	public Asset getSystemAssetById(AStoreWrapper store, String assetID) {

		Map<String, Asset> assets = getSystemAssets(store, null, assetID, getControlSets(store), getMisbehaviourSets(store, true), getTrustworthinessAttributeSets(store), true, true);	
		return assets.isEmpty() ? null : assets.values().iterator().next();
	}
	
	/**
	 * General method to query for asset(s)
	 * 
	 * @param store the store to query
	 * @param assetURI the asset URI, can be null
	 * @param assetId the asset ID, can be null
	 * @param controlSets map of ControlSets (can be null); if defined then the asset ControlSets are queried and objects in the map are attached to the result
	 * @param misbehaviourSets map of MisbehaviourSets (can be null); if defined then the asset MisbehaviourSets are queried and objects in the map are attached to the result
	 * @param trustworthinessAttributeSets map of TrustworthinessAttributeSets; if defined then the asset TrustworthinessAttributeSets are queried and objects in the map are attached to the result
	 * @param getInferredAssets whether to return the inferred assets or not
	 * @param getAssetPosition whether to return the asset UI positions or not
	 * @return map of assets
	 */
	protected Map<String, Asset> getSystemAssets(AStoreWrapper store,
			String assetURI, String assetId,
			Map<String, ControlSet> controlSets,
			Map<String, MisbehaviourSet> misbehaviourSets,
			Map<String, TrustworthinessAttributeSet> trustworthinessAttributeSets,
			boolean getInferredAssets,
			boolean getAssetPosition) {

		logger.info("Getting system assets");
						
		Set<Asset> setVisible = new HashSet<>();
		Map<String, Asset> assets = new HashMap<>();
		
		// ----- Get base asset information -----
		
		String sparql = String.format("SELECT * WHERE {\r\n" + 
				(assetURI != null ? "	BIND(<" + SparqlHelper.escapeURI(assetURI) + "> AS ?a) .\n" : "") +
				"	GRAPH <%s> {\r\n" + 
				" 		?type rdfs:subClassOf* core:Asset .\r\n" + 
				"       OPTIONAL {?type core:isVisible ?vis}" + 
				"	}\r\n" + 
				"	?a a ?type .\r\n" + 
				(assetId != null ? "	?a core:hasID \"" + SparqlHelper.escapeLiteral(assetId) + "\" .\n" : "") +
				"	?a core:hasID ?aid .\r\n" + 
				" 	OPTIONAL {?a rdfs:label ?label}\r\n" + 
				"	OPTIONAL {?a core:minCardinality ?min}\r\n" + 
				"	OPTIONAL {?a core:maxCardinality ?max}\r\n" + 
				"	OPTIONAL {\n" +
				"		GRAPH <%s> {\n" +
				"			?a core:population ?apop .\n" +
				"		}\n" +
				"	}\n" +
				"	OPTIONAL {\n" +
				"		GRAPH <%s> {\n" +
				"			?a core:population ?dpop .\n" +
				"		}\n" +
				"	}\n" +
				"}", model.getGraph("domain"), model.getGraph("system"), model.getGraph("system-inf"));

		List<Map<String, String>> rows = store.translateSelectResult(store.querySelect(sparql,
			model.getGraph("core"),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf"),
			model.getGraph("system-ui")
		));
		
		for (Map<String, String> row : rows) {
			Asset asset = new Asset();
			asset.setUri(row.get("a"));
			String label = row.get("label");
			asset.setLabel(label == null? "" : label);

			String min = row.get("min");
			String max = row.get("max");
			min = min != null ? min.replace("^^http://www.w3.org/2001/XMLSchema#integer", "") : min;
			max = max != null ? max.replace("^^http://www.w3.org/2001/XMLSchema#integer", "") : max;	
			asset.setMinCardinality(min != null ? Integer.valueOf(min) : -1);
			asset.setMaxCardinality(min != null ? Integer.valueOf(max) : -1);

			String assertedPop = row.get("apop");
			String defaultPop = row.get("dpop");
			String population = assertedPop != null ? assertedPop : defaultPop;
			asset.setPopulation(population);

			String vis = row.get("vis");
			if (vis != null) {
				asset.setVisible(StringUtils.containsIgnoreCase(vis, "true")); // Just in case of type prefix.
			} else {
				setVisible.add(asset);
			}
			asset.setType(row.get("type"));
			asset.setIconPosition(-1, -1);
			
			if (assets.put(asset.getUri(), asset) != null) {
				throw new RuntimeException("Duplicate Asset with URI " + asset.getUri() + " found.");
			}
		}
		
		
		// ----- Get inferred assets -----
		if (getInferredAssets) {
		
			sparql = String.format("SELECT * WHERE {\r\n" + 
					(assetURI != null ? "	BIND(<" + SparqlHelper.escapeURI(assetURI) + "> AS ?a) .\n" : "") +
					"	GRAPH <%s> {\r\n" + 
					"        ?ia core:displayedAtAsset ?a \r\n" + 
					"    }\r\n" + 
					"}", model.getGraph("system-inf"));
			rows = store.translateSelectResult(store.querySelect(sparql,
					model.getGraph("system-inf")
				));
			
			for (Map<String, String> row : rows) {
				String assetUri = row.get("a");
				Asset asset = assets.get(assetUri);
				if (asset != null) {
					String ia = row.get("ia");
					if (ia != null) {
						asset.getInferredAssets().add(ia);
					}
				}
			}
		}
		
		// ----- Get if asset asserted and visible -----
		
		sparql = String.format("SELECT * WHERE {\r\n" + 
				(assetURI != null ? "	BIND(<" + SparqlHelper.escapeURI(assetURI) + "> AS ?a) .\n" : "") +
				"	GRAPH <%s> {\r\n" + 
				"        ?a core:createdByPattern ?createdBy\r\n" + 
				"    }\r\n" + 
				"}", model.getGraph("system-inf"));
		rows = store.translateSelectResult(store.querySelect(sparql,
				model.getGraph("system-inf")
			));
		
		for (Map<String, String> row : rows) {
			String assetUri = row.get("a");
			Asset asset = assets.get(assetUri);
			if (asset != null) {
				asset.setAsserted(false);
			}
		}
		
		for (Asset asset : setVisible) {
			asset.setVisible(asset.isAsserted());
		}
		
		// ----- Get asset position -----
		if (getAssetPosition) {
		
			sparql = String.format("SELECT * WHERE {\r\n" + 
					(assetURI != null ? "	BIND(<" + SparqlHelper.escapeURI(assetURI) + "> AS ?a) .\n" : "") +
					"	GRAPH <%s> {\r\n" + 
					"        ?a core:positionX ?x .\r\n" + 
					"        ?a core:positionY ?y .\r\n" + 
					"    }\r\n" + 
					"}", model.getGraph("system-ui"));
			rows = store.translateSelectResult(store.querySelect(sparql,
					model.getGraph("system-ui")
				));
			
			for (Map<String, String> row : rows) {
				String assetUri = row.get("a");
				Asset asset = assets.get(assetUri);
				if (asset != null) {
					String x = row.get("x");
					String y = row.get("y");
					x = x.replace("^^http://www.w3.org/2001/XMLSchema#integer", "");
					y = y.replace("^^http://www.w3.org/2001/XMLSchema#integer", "");
					asset.setIconPosition(Integer.valueOf(x), Integer.valueOf(y));
				}
			}
		}
		
		// ----- Get misbehaviours ----
		if (misbehaviourSets != null) {
		
			sparql = String.format("SELECT * WHERE {\r\n" + 
					(assetURI != null ? "	BIND(<" + SparqlHelper.escapeURI(assetURI) + "> AS ?a) .\n" : "") +
					"	GRAPH <%s> {\r\n" + 
					"        ?ms core:locatedAt ?a .\r\n" + 
					"        ?ms a core:MisbehaviourSet .\r\n" + 
					"    }\r\n" + 
					"}", model.getGraph("system-inf"));
			rows = store.translateSelectResult(store.querySelect(sparql,
					model.getGraph("system-inf")));
			
			for (Map<String, String> row : rows) {
				String assetUri = row.get("a");
				Asset asset = assets.get(assetUri);
				if (asset != null) {
					MisbehaviourSet misbs = misbehaviourSets.get( row.get("ms"));
					if (misbs != null) {
						if (asset.getMisbehaviourSets().put(misbs.getUri(), misbs) != null) {
							 throw new RuntimeException("Duplicate MisbehaviourSet with URI " + misbs.getUri() + 
									 " added to Asset with URI " + asset.getUri());
						}
					}
				}
			}
		}
		
		// ----- Get trustworthiness attribute sets -----
		if (trustworthinessAttributeSets != null) {

			sparql = String.format("SELECT * WHERE {\r\n" + 
					(assetURI != null ? "	BIND(<" + SparqlHelper.escapeURI(assetURI) + "> AS ?a) .\n" : "") +
					"    GRAPH <%s> {\r\n" + 
					"        ?twas a core:TrustworthinessAttributeSet .\r\n" + 
					"        ?twas core:locatedAt ?a .\r\n" + 
					"    }\r\n" + 
					"}", model.getGraph("system-inf"));
			rows = store.translateSelectResult(store.querySelect(sparql,
					model.getGraph("system-inf")));
			
			for (Map<String, String> row : rows) {
				String assetUri = row.get("a");
				Asset asset = assets.get(assetUri);
				if (asset != null) {
					TrustworthinessAttributeSet twas = trustworthinessAttributeSets.get(row.get("twas"));
					if (twas != null) {
						if (asset.getTrustworthinessAttributeSets().put(twas.getUri(), twas) != null) {
							throw new RuntimeException("Duplicate TrustworthinessAttributeSet with URI " + twas.getUri() + 
									 " added to Asset with URI " + asset.getUri());
						}
					}
				}
			}
		}
		
		// ----- Get control sets -----
		if (controlSets != null) {
		
			sparql = String.format("SELECT * WHERE {\r\n" + 
					(assetURI != null ? "	BIND(<" + SparqlHelper.escapeURI(assetURI) + "> AS ?a) .\n" : "") +
					"    GRAPH <%s> {\r\n" + 
					"        ?cs core:locatedAt ?a .\r\n" + 
					"        ?cs a core:ControlSet .\r\n" + 
					"    }\r\n" + 
					"}", model.getGraph("system-inf"));
			rows = store.translateSelectResult(store.querySelect(sparql,
					model.getGraph("system-inf")));
			
			for (Map<String, String> row : rows) {
				String assetUri = row.get("a");
				Asset asset = assets.get(assetUri);
				if (asset != null) {
					ControlSet cs = controlSets.get(row.get("cs"));
					if (cs != null) {
						if (asset.getControlSets().put(cs.getUri(), cs) != null) {
							throw new RuntimeException("Duplicate ControlSet with URI " + cs.getUri() + 
									 " added to Asset with URI " + asset.getUri());
						}
					}
				}
			}
		}

		return assets;
	}

	/**
	 * Query system model assets by their metadata. Each asset returned must have AT LEAST ONE of the specified values
	 * for EACH given key.
	 *
	 * @param store The store to query
	 * @param queryMetadata A list of (key, value) pairs used to query the system assets
	 * @return Map of all assets
	 */
	public Map<String, Asset> getSystemAssetsByMetadata(AStoreWrapper store, List<MetadataPair> queryMetadata) {
		if (queryMetadata == null) {
			throw new IllegalArgumentException("MetadataPair list cannot be null");
		}

		// Get ALL system assets with only basic information (URI, label, type and ID)
		Map<String, Asset> assets = getBasicSystemAssets(store);

		if (queryMetadata.isEmpty()) {
			return assets;
		}

		// Get URIs of assets matching the metadata query and then use the relevant assets found previously
		Set<String> foundUris = getEntitiesByMetadata(store, queryMetadata);
		Map<String, Asset> foundAssets = new HashMap<>();
		for (String foundUri : foundUris) {
			Asset asset = assets.get(foundUri);
			List<MetadataPair> assetMetadata = getMetadataOnEntity(store, asset);
			asset.setMetadata(assetMetadata);
			foundAssets.put(foundUri, asset);
		}
		return foundAssets;
	}

	// Relations //////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Get all system-specific relations
	 *
	 * @param store the store to query
	 * @return all relations
	 */
	public Set<Relation> getSystemRelations(AStoreWrapper store) {
		return getSystemRelations(store, null, null, null);
	}
	
	/**
	 * Get all system-specific relation ids
	 *
	 * @param store the store to query
	 * @return all relation ids
	 */
	public Set<Relation> getSystemRelationIDs(AStoreWrapper store) {
		return getSystemRelationIDs(store, null, null, null);
	}

	/**
	 * Get one system-specific relation
	 *
	 * @param store the store to query
	 * @param fromURI the URI of the source
	 * @param type the URI of the type
	 * @param toURI the URI of the target
	 * @return all relations
	 */
	public Relation getSystemRelation(AStoreWrapper store, String fromURI, String type, String toURI) {

		Set<Relation> rels =  getSystemRelations(store, fromURI, type, toURI);
		return rels.isEmpty() ? null : rels.iterator().next();
	}

	/**
	 * Get one system-specific relation
	 *
	 * @param store the store to query
	 * @param relId the ID of the relation
	 * @return all relations
	 */
	public Relation getSystemRelationById(AStoreWrapper store, String relId) {

		//need to iterate over all relations since the IDs don't get stored (relations are not things)
		for (Relation rel : getSystemRelations(store)) {
			if (rel.getID().equals(relId)) {
				return rel;
			}
		}
		return null;
	}

	/**
	 * Get the IDs of all asserted relations in and out of an asset.
	 * 
	 * @param store the store to query
	 * @param assetURI the URI of the asset
	 * @return a Set of relation IDs
	 */
	public Set<String> getAssetRelations(AStoreWrapper store, String assetURI) {
		Set<String> relsToAsset = getRelationIDsBetweenAssets(store, null, null, assetURI);
		Set<String> relsFromAsset = getRelationIDsBetweenAssets(store, assetURI, null, null);
		// Take the union of relationships to and from the asset
		Set<String> relations = new HashSet<>(relsToAsset);
		relations.addAll(relsFromAsset);
		return relations;
	}

	protected Set<Relation> getSystemRelations(AStoreWrapper store, String fromURI, String type, String toURI) {
		Set<Relation> relations = new HashSet<>();
		
		List<Map<String, String>> inferredAssets = getInferredAssets(store);
		
		// ----- Get created by patterns -----
		
		String sparql = "SELECT * WHERE {\r\n" + 
				"    ?n core:createdByPattern ?p \r\n" + 
				"}";
		List<Map<String, String>> rows = store.translateSelectResult(store.querySelect(sparql,
				model.getGraph("domain"),
				model.getGraph("system"),
				model.getGraph("system-inf")
			));
		
		Map<String, String> createdBy = new HashMap<>();
		for (Map<String, String> row : rows) {
			createdBy.put(row.get("n"), row.get("p"));
		}
		
		// ----- Get relations -----
			
		sparql = //String.format("SELECT ?from ?type ?to ?supertype ?label ?immutableB ?hiddenB ?scard ?tcard ?g WHERE {\r\n" + 
				String.format("SELECT * WHERE {\r\n" + 
				(fromURI != null ? "	BIND(<" + SparqlHelper.escapeURI(fromURI) + "> AS ?from)\n" : "") +
				(toURI != null ? "	BIND(<" + SparqlHelper.escapeURI(toURI) + "> AS ?to)\n" : "") +
				"    ?fromClass rdfs:subClassOf* core:Asset .\r\n" + 
				"    ?toClass rdfs:subClassOf* core:Asset .\r\n" + 
				"    ?from a ?fromClass .\r\n" + 
				"    ?to a ?toClass .\r\n" + 
				"    ?from ?type ?to .\r\n" + 
				"    GRAPH ?g {\r\n" + 
				"		?from ?type ?to .\r\n" + 
				"	}\r\n" + 
				"    GRAPH <%s> {\r\n" + 
				"		?type a owl:ObjectProperty\r\n" + 
				"	}\r\n" + 
				"    ?type rdfs:subPropertyOf* ?supertype .\r\n" + 
				"	OPTIONAL { ?type core:hidden ?hiddenB }\r\n" + 
				"	OPTIONAL { ?type core:immutable ?immutableB }\r\n" + 
				(type != null ? "	FILTER(?type=<" + SparqlHelper.escapeURI(type) + ">)\n" : "") +
				"    OPTIONAL {?type rdfs:label ?label}\r\n" + 
				"    OPTIONAL {\r\n" + 
				"		?cc a core:CardinalityConstraint .\r\n" +
				"		?cc core:linksTo ?to .\r\n" + 
				"		?cc core:linksFrom ?from .\r\n" + 
				"		?cc core:linkType ?type .\r\n" + 
				"		OPTIONAL {\r\n" +
				"			GRAPH <%s> {\r\n" +
				"				?cc core:sourceCardinality ?scarda .\r\n" + 
				"				?cc core:targetCardinality ?tcarda .\r\n" + 
				"			}\r\n" + 
				"		}\r\n" +
				"		OPTIONAL {\r\n" +
				"			GRAPH <%s> {\r\n" +
				"				?cc core:sourceCardinality ?scardi .\r\n" + 
				"				?cc core:targetCardinality ?tcardi .\r\n" + 
				"			}\r\n" + 
				"		}\r\n" +
				//"		BIND(IF(BOUND(?scardi), ?scardi, ?scarda) AS ?scard) .\r\n" +
				//"		BIND(IF(BOUND(?tcardi), ?tcardi, ?tcarda) AS ?tcard) .\r\n" +
				"	}\r\n" + 
				"}", model.getGraph("domain"), model.getGraph("system"), model.getGraph("system-inf"));
		rows = store.translateSelectResult(store.querySelect(sparql,
				model.getGraph("domain"),
				model.getGraph("system"),
				model.getGraph("system-inf")
			));
		
		// Get supertypes of assets first.
		Map<Integer, Set<String>> supertypes = new HashMap<>();
		for (Map<String, String> row : rows) {
			String from = row.get("from");
			String relType = row.get("type");
			String to = row.get("to");
		    
		    Relation relation = new Relation(from, null, to, null, relType, null);
		    relations.add(relation);
		    int relationHash = relation.hashCode();
		    
			Set<String> types = supertypes.get(relationHash);
			if (types == null) {
				types = new HashSet<String>();
				supertypes.put(relationHash, types);
			}
			types.add(row.get("supertype"));
		}
		
		for (Map<String, String> row : rows) {
			String from = row.get("from");
		    String relType = row.get("type");
		    String to = row.get("to");
		    
		    if (from ==  null || relType == null || to == null) {
		        continue;
		    }
		    
		    String label = row.get("label");
		    label = label != null? label : "";
		    Relation relation = new Relation(from, null, to, null, relType, label);

		    // If relation already seen, replace with this instance.
		    relations.remove(relation);
		    
		    relation.setInferredAssets(new HashSet<String>());
		    String immutable = row.get("immutableB");
		    relation.setImmutable(immutable != null ? Boolean.valueOf(immutable) : false);
		    String hidden = row.get("hiddenB");
		    relation.setHidden(hidden != null ? Boolean.valueOf(hidden) : false);
		    
		    /*String scard = row.get("scard");
		    relation.setSourceCardinality(scard != null ? 
		    		Integer.valueOf(scard.replace("^^http://www.w3.org/2001/XMLSchema#integer", "")) : 0);
		    String tcard = row.get("tcard");
		    relation.setTargetCardinality(tcard != null ? Integer.valueOf(
		    		tcard.replace("^^http://www.w3.org/2001/XMLSchema#integer", "")) : 0);*/
		    String scarda = row.get("scarda");
		    String scardi = row.get("scardi");
		    String tcarda = row.get("tcarda");
		    String tcardi = row.get("tcardi");
		    relation.setSourceCardinality(scardi != null ? 
		    		Integer.valueOf(scardi.replace("^^http://www.w3.org/2001/XMLSchema#integer", "")) :
		    			(scarda != null ? Integer.valueOf(scarda.replace("^^http://www.w3.org/2001/XMLSchema#integer", "")) : 0));
		    relation.setTargetCardinality(tcardi != null ? 
		    		Integer.valueOf(tcardi.replace("^^http://www.w3.org/2001/XMLSchema#integer", "")) :
		    			(tcarda != null ? Integer.valueOf(tcarda.replace("^^http://www.w3.org/2001/XMLSchema#integer", "")) : 0));
		    
		    String graph = row.get("g");
		    relation.setAsserted(graph.equals(model.getGraph("system")));
		    
		    if (!createdBy.containsKey(from) && !createdBy.containsKey(to)) {
		    	relation.setVisible(true);
		    } else {
		    	relation.setVisible(false);
		    }
		    
		    Set<String> stypes = supertypes.get(relation.hashCode());
		    for (Map<String, String> inferredAsset : inferredAssets) {
		        if (inferredAsset.get("from").equals(relation.getFrom())
		                && inferredAsset.get("to").equals(relation.getTo())
		                && stypes.contains(inferredAsset.get("type"))) {
		            relation.getInferredAssets().add(inferredAsset.get("ia"));
		        }
		    }
		    
		    relations.add(relation);
		}

		return relations;
	}

	/**
	 * Get the IDs of asserted and inferred relations from one asset to another asset.
	 * 
	 * @param store the store to query
	 * @param fromURI the URI of the asset at the start of the relation (can be null)
	 * @param type the relation type
	 * @param toURI the URI of the asset at the end of the relation (can be null)
	 * @return the relation object
	 */
	public Relation getRelationFromCC(AStoreWrapper store, String fromURI, String typeIn, String toURI) {
		List<Map<String, String>> rows;

		String ccIDIn = PREFIX + "system#" + fromURI + "-" + typeIn + "-" + toURI;
		if (typeIn != null) typeIn = PREFIX + "domain#" + typeIn;

		String sparql = "SELECT ?linksToResult ?linksFromResult WHERE {\r\n" + 
				(ccIDIn != null ? "	BIND(<" + SparqlHelper.escapeURI(ccIDIn) + "> AS ?ccID)\n" : "") +
				"?ccID core:linksTo ?linksToResult .\n" +
				"?ccID core:linksFrom ?linksFromResult .\n" +
				"}";

		rows = store.translateSelectResult(store.querySelect(sparql,
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf")
		));

	    // There should be only one row	
		Relation relation = new Relation();
		for (Map<String, String> row : rows) {
			String from = row.get("linksFromResult");
			String to = row.get("linksToResult");
			
			if (from ==  null || typeIn == null || to == null) {
				continue;
			}
			relation = new Relation(from, null, to, null, typeIn, "");
		}
		return relation;
	}

	/**
	 * Get the IDs of asserted relations from one asset to another asset.
	 * 
	 * @param store the store to query
	 * @param fromURI the URI of the asset at the start of the relation (can be null)
	 * @param type the relation type
	 * @param toURI the URI of the asset at the end of the relation (can be null)
	 * @return the Set of relation IDs
	 */
	protected Set<String> getRelationIDsBetweenAssets(AStoreWrapper store, String fromURI, String type, String toURI) {
		Set<String> relationIDs = new HashSet<>();
		List<Map<String, String>> rows;
		
		String sparql = String.format("SELECT ?from ?type ?to WHERE {\r\n" + 
				(fromURI != null ? "	BIND(<" + SparqlHelper.escapeURI(fromURI) + "> AS ?from)\n" : "") +
				(toURI != null ? "	BIND(<" + SparqlHelper.escapeURI(toURI) + "> AS ?to)\n" : "") +
				"    ?fromClass rdfs:subClassOf* core:Asset .\n" + 
				"    ?toClass rdfs:subClassOf* core:Asset .\n" + 
				"    ?from a ?fromClass .\n" + 
				"    ?to a ?toClass .\n" + 
				"    ?from ?type ?to .\n" + 
				"    GRAPH ?g {\n" + 
				"        ?from ?type ?to .\n" + 
				"    }\n" + 
				"    GRAPH <%s> {\n" + 
				"        ?type a owl:ObjectProperty\n" + 
				"    }\n" + 
				"    ?type rdfs:subPropertyOf* ?supertype .\n" + 
				"}", model.getGraph("domain"));
		
		rows = store.translateSelectResult(store.querySelect(sparql,
			model.getGraph("domain"),
			model.getGraph("system")
		));
		
		for (Map<String, String> row : rows) {
			String from = row.get("from");
			String relType = row.get("type");
			String to = row.get("to");
			
			if (from ==  null || relType == null || to == null) {
				continue;
			}
			
			Relation relation = new Relation(from, null, to, null, relType, "");
			relationIDs.add(relation.getID());
		}
		return relationIDs;
	}

	//This method is used when changing type, to determing which relations have been deleted. TODO: tidy this up and probably just return list of IDs
	protected Set<Relation> getSystemRelationIDs(AStoreWrapper store, String fromURI, String type, String toURI) {
		Set<Relation> relations = new HashSet<>();
		List<Map<String, String>> rows;
		
		//List<Map<String, String>> inferredAssets = getInferredAssets(store);
		
		/*
		// ----- Get created by patterns -----
		
		String sparql = "SELECT * WHERE {\r\n" + 
				"    ?n core:createdByPattern ?p \r\n" + 
				"}";
		List<Map<String, String>> rows = store.translateSelectResult(store.querySelect(sparql,
				model.getGraph("domain"),
				model.getGraph("system"),
				model.getGraph("system-inf")
			));
		
		Map<String, String> createdBy = new HashMap<>();
		for (Map<String, String> row : rows) {
			createdBy.put(row.get("n"), row.get("p"));
		}
		*/
		
		// ----- Get relations -----
		
		String sparql = String.format("SELECT * WHERE {\r\n" + 
				//(fromURI != null ? "	BIND(<" + SparqlHelper.escapeURI(fromURI) + "> AS ?from)\n" : "") +
				//(toURI != null ? "	BIND(<" + SparqlHelper.escapeURI(toURI) + "> AS ?to)\n" : "") +
				"    ?fromClass rdfs:subClassOf* core:Asset .\r\n" + 
				"    ?toClass rdfs:subClassOf* core:Asset .\r\n" + 
				"    ?from a ?fromClass .\r\n" + 
				"    ?to a ?toClass .\r\n" + 
				"    ?from ?type ?to .\r\n" + 
				"    GRAPH <%s> {\r\n" + 
				"		?from ?type ?to .\r\n" + 
				"	}\r\n" + 
				"    GRAPH <%s> {\r\n" + 
				"		?type a owl:ObjectProperty\r\n" + 
				"	}\r\n" + 
				"    ?type rdfs:subPropertyOf* ?supertype .\r\n" + 
				//"	OPTIONAL { ?type core:hidden ?hiddenB }\r\n" + 
				//"	OPTIONAL { ?type core:immutable ?immutableB }\r\n" + 
				//(type != null ? "	FILTER(?type=<" + SparqlHelper.escapeURI(type) + ">)\n" : "") +
				"    OPTIONAL {?type rdfs:label ?label}\r\n" + 
				//"    OPTIONAL {\r\n" + 
				//"		?cc core:linksTo ?to .\r\n" + 
				//"		?cc core:linksFrom ?from .\r\n" + 
				//"		?cc core:linkType ?type .\r\n" + 
				//"		?cc a core:CardinalityConstraint .\r\n" + 
				//"		?cc core:sourceCardinality ?scard .\r\n" + 
				//"		?cc core:targetCardinality ?tcard .\r\n" + 
				//"	}\r\n" + 
				"}", model.getGraph("system"), model.getGraph("domain"));
		rows = store.translateSelectResult(store.querySelect(sparql,
				model.getGraph("domain"),
				model.getGraph("system")
				//model.getGraph("system-inf")
			));
		
		/*
		// Get supertypes of assets first.
		Map<Integer, Set<String>> supertypes = new HashMap<>();
		for (Map<String, String> row : rows) {
			String from = row.get("from");
			String relType = row.get("type");
			String to = row.get("to");
		    
		    Relation relation = new Relation(from, null, to, null, relType, null);
		    relations.add(relation);
		    int relationHash = relation.hashCode();
		    
			Set<String> types = supertypes.get(relationHash);
			if (types == null) {
				types = new HashSet<String>();
				supertypes.put(relationHash, types);
			}
			types.add(row.get("supertype"));
		}
		*/
		
		//logger.info("Found {} rows", rows.size());
		
		for (Map<String, String> row : rows) {
			String from = row.get("from");
		    String relType = row.get("type");
		    String to = row.get("to");
		    
		    if (from ==  null || relType == null || to == null) {
		        continue;
		    }
		    
		    String label = row.get("label");
		    label = label != null? label : "";
		    Relation relation = new Relation(from, null, to, null, relType, label);
			//logger.info("relation: {}", relation);

		    // If relation already seen, replace with this instance.
		    relations.remove(relation);
		    
			/*
		    relation.setInferredAssets(new HashSet<String>());
		    String immutable = row.get("immutableB");
		    relation.setImmutable(immutable != null ? Boolean.valueOf(immutable) : false);
		    String hidden = row.get("hiddenB");
		    relation.setHidden(hidden != null ? Boolean.valueOf(hidden) : false);
		    String scard = row.get("scard");
		    relation.setSourceCardinality(scard != null ? 
		    		Integer.valueOf(scard.replace("^^http://www.w3.org/2001/XMLSchema#integer", "")) : 0);
		    String tcard = row.get("tcard");
		    relation.setTargetCardinality(tcard != null ? Integer.valueOf(
		    		tcard.replace("^^http://www.w3.org/2001/XMLSchema#integer", "")) : 0);
		    String graph = row.get("g");
		    relation.setAsserted(graph.equals(model.getGraph("system")));
		    
		    if (!createdBy.containsKey(from) && !createdBy.containsKey(to)) {
		    	relation.setVisible(true);
		    } else {
		    	relation.setVisible(false);
		    }
			*/
		    
			/*
		    Set<String> stypes = supertypes.get(relation.hashCode());
		    for (Map<String, String> inferredAsset : inferredAssets) {
		        if (inferredAsset.get("from").equals(relation.getFrom())
		                && inferredAsset.get("to").equals(relation.getTo())
		                && stypes.contains(inferredAsset.get("type"))) {
		            relation.getInferredAssets().add(inferredAsset.get("ia"));
		        }
		    }
			*/
		    
		    relations.add(relation);
		}

		
		return relations;
	}

	/**
	 * Get all inferred assets from the store
	 *
	 * @param store the store to query
	 * @return all inferred assets
	 */
	private List<Map<String, String>> getInferredAssets(AStoreWrapper store) {

		String sparql = "SELECT DISTINCT ?ia ?in ?from ?to ?type ?da WHERE {\n" +
		"	GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"		?ia a ?iac .\n" +
		"		?ia core:displayedAtRelationFrom ?from .\n" +
		"		?ia core:displayedAtRelationTo ?to .\n" +
		"		?ia core:displayedAtRelationType ?type .\n" +
		"	}\n" +
		"	GRAPH <" + model.getGraph("domain") + "> {\n" +
		"		?iac rdfs:subClassOf* core:Asset .\n" +
		"	}\n" +
		"} ORDER BY ?ia ?from ?type ?to";

		return store.translateSelectResult(store.querySelect(sparql,
			model.getGraph("core"),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf")
		));
	}
	

	// Root Patterns //////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Get system-specific root patterns
	 *
	 * @param store the store to query
	 * @return the pattern
	 */
	public Map<String, Pattern> getRootPatterns(AStoreWrapper store) {

		logger.info("Getting system root patterns");

		List<Map<String, String>> patterns = store.translateSelectResult(store.querySelect(
			createRootPatternSparql(null, null),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf")
		));
		return createRootPatterns(patterns);
	}

	/**
	 * Get system-specific root patterns
	 *
	 * @param store the store to query
	 * @param rootPatternParent the parent root pattern: only find children of this generic root pattern
	 * @return the pattern
	 */
	public Map<String, Pattern> getRootPatterns(AStoreWrapper store, String rootPatternParent) {

		logger.info("Getting system root patterns for parent {}", rootPatternParent);

		List<Map<String, String>> patterns = store.translateSelectResult(store.querySelect(
			createRootPatternSparql(null, rootPatternParent),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf")
		));
		return createRootPatterns(patterns);
	}

	/**
	 * Get a system-specific root pattern
	 *
	 * @param store the store to query
	 * @param patternURI the URI of the pattern to find
	 * @return the patterns
	 */
	public Pattern getRootPattern(AStoreWrapper store, String patternURI) {

		logger.info("Getting system root pattern {}", patternURI);

		List<Map<String, String>> result = store.translateSelectResult(store.querySelect(
			createRootPatternSparql(patternURI, null),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf")
		));
		return createRootPatterns(result).get(patternURI);
	}

	/**
	 * Creates the SPARQL query to retrieve root patterns using various filtering options
	 *
	 * @param patternURI the pattern URI to filter by or null for all patterns
	 * @param patternParentURI the pattern's parent URI to filter by or null for all patterns
	 * @return SPARQL to retrieve a map of patterns that match the filtering criteria
	 */
	private String createRootPatternSparql(String patternURI, String patternParentURI) {

		return "SELECT DISTINCT * WHERE {\n" +
		(patternURI != null ? ("	BIND (<" + patternURI + "> AS ?rp)\n") : "") +
		(patternParentURI != null ? ("	BIND (<" + patternParentURI + "> AS ?rpp)\n") : "") +
		"	GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"		?rp a core:RootPattern .\n" +
		"		?rp core:parent ?rpp .\n" +
		"		?rp rdfs:label ?rpl .\n" +
		"		?rp core:hasNode ?rpn .\n" +
		"		?rpn core:hasAsset ?rpa .\n" +
		"		?rpn core:hasRole ?rpr .\n" +
		"	}\n" +
		//this can be in the system or system-inf graph
		"	?rpa rdf:type ?rpat .\n" +
		"	?rpa rdfs:label ?rpal .\n" +
		"	GRAPH <" + model.getGraph("domain") + "> {\n" +
		"		OPTIONAL { ?rpp rdfs:comment ?rppc }\n" +
		"		?rpp rdfs:label ?rppl .\n" +
		"		?rpr rdfs:label ?rprl .\n" +
		"	}\n" +
		"} ORDER BY ?rpp ?rp ?rpr";
	}

	/**
	 * Build root patterns from the SPARQL results
	 *
	 * @param patterns the results as retrieved from the store
	 * @return a map of patterns
	 */
	private Map<String, Pattern> createRootPatterns(List<Map<String, String>> patterns) {

		Map<String, Pattern> map = new HashMap<>();
		for (Map<String, String> row : patterns) {

			String rp = row.get("rp");

			//add the pattern if it doesn't exist yet
			if (!map.containsKey(rp)) {
				Pattern pattern = new Pattern();
				pattern.setUri(rp);
				pattern.setLabel(row.get("rpl") != null ? row.get("rpl") : "");
				pattern.setDescription(row.get("rppc") != null ? row.get("rppc") : "");
				pattern.setParent(row.get("rpp") != null ? row.get("rpp") : null);
				pattern.setParentLabel(row.get("rppl") != null ? row.get("rppl") : null);

				map.put(rp, pattern);
			}

			//add pattern nodes
			if (row.containsKey("rpn") && row.get("rpn") != null) {
				Node n = new Node(row.get("rpa"), row.get("rpal"), row.get("rpr"), row.get("rprl"));
				n.setUri(row.get("rpn"));
				if (!map.get(rp).getNodes().contains(n)) {
					map.get(rp).getNodes().add(n);
				}
			}

			//leaving out links - not required as tey can be read from the domain model
		}

		return map;
	}

	// System Patterns //////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Get all system-specific patterns
	 *
	 * @param store the store to query
	 * @return r ms of all patterns
	 */
	public Map<String, Pattern> getSystemPatterns(AStoreWrapper store) {
		return getSystemPatterns(store, null, null, false);
	}
	
	/**
	 * Get all system-specific patterns
	 *
	 * @param store the store to query
	 * @param assetURI only include patterns that contain this asset
	 * @return r ms of all patterns
	 */
	public Map<String, Pattern> getSystemPatterns(AStoreWrapper store, String assetURI) {

		logger.info("Getting system patterns for asset {}", assetURI);
		
		return getSystemPatterns(store, null, assetURI, false);
	}

	/**
	 * Get all system-specific patterns
	 *
	 * @param store the store to query
	 * @return r ms of all patterns
	 */
	public Map<String, Pattern> getSystemPatternsForValidation(AStoreWrapper store) {

		logger.info("Getting system patterns");
		
		return getSystemPatterns(store, null, null, true);
	}

	/**
	 * Get a system-specific pattern
	 *
	 * @param store the store to query
	 * @param patternURI the URI of the pattern
	 * @return the pattern or null if it doesn't exist
	 */
	public Pattern getSystemPattern(AStoreWrapper store, String patternURI) {

		logger.info("Getting system pattern {}", patternURI);

		return getSystemPatterns(store, patternURI, null, false).get(patternURI);
	}
	
	protected Map<String, Pattern> getSystemPatterns(AStoreWrapper store, String patternURI, String assetURI, boolean validationOnly) {
		
		Map<String, Pattern> patterns = new HashMap<>();
		Map<String, List<Pattern>> children = new HashMap<>();
		Map<String, List<Pattern>> stems = new HashMap<>();
		
		// ----- Get patterns -----
		
		String query = String.format("SELECT DISTINCT * WHERE {\r\n" + 
				"	GRAPH <%s> {\r\n" + 
				"		?genP core:hasRootPattern ?rp .\r\n" + 
				"		?patternClass rdfs:subClassOf* core:MatchingPattern .\r\n" + 
				"		?genP a ?patternClass .\r\n" + 
				(validationOnly? "		?vp a core:ValidationPattern .\n		?vp core:hasMatchingPattern ?genP .\n" : "") +
				"	}\r\n" + 
				"    GRAPH <%s> {\r\n" + 
				"		?p core:parent ?genP .\r\n" + 
				"		OPTIONAL { ?p rdfs:label ?pl }\r\n" + 
				"		OPTIONAL { ?p rdfs:comment ?pDesc }\r\n" + 
				"	}\r\n" + 
				"}", model.getGraph("domain"), model.getGraph("system-inf"));
		
		List<Map<String, String>> rows = store.translateSelectResult(store.querySelect(query,
				model.getGraph("domain"), model.getGraph("system-inf")));
		for (Map<String, String> row : rows) {			
			String patternUri = row.get("p");
			
			if (!patterns.containsKey(patternUri)) {
				Pattern pattern = new Pattern();
				pattern.setUri(patternUri);
				pattern.setLabel(row.get("pl") != null ? row.get("pl") : "");
				pattern.setDescription(row.get("pDesc") != null ? row.get("pDesc") : "");
				pattern.setParent(row.get("genP"));
				patterns.put(patternUri, pattern);
				
				// Add pattern to list of children of its generating pattern.
				String generatingUri = row.get("genP");
				List<Pattern> patternChildren = children.get(generatingUri);
				if (patternChildren == null) {
					patternChildren = new ArrayList<Pattern>();
					children.put(generatingUri, patternChildren);
				}
				patternChildren.add(pattern);
				
				
				// Add pattern to list of stems of its root pattern.
				String rootUri = row.get("rp");
				List<Pattern> patternStems = stems.get(rootUri);
				if (patternStems == null) {
					patternStems = new ArrayList<Pattern>();
					stems.put(rootUri, patternStems);
				}
				patternStems.add(pattern);
			}
		}
		
		// ----- Get nodes for patterns -----
		
		query = String.format("SELECT DISTINCT * WHERE {\r\n" + 
				(patternURI != null ? "	BIND(<" + SparqlHelper.escapeURI(patternURI) + "> AS ?p) .\n" : "") +
				"	GRAPH <%s> {\r\n" + 
				"        ?p core:hasNode ?pn .\r\n" + 
				"        ?pn core:hasRole ?pnr .\r\n" + 
				"        ?pn core:hasAsset ?pna .\r\n" + 
				"    }\r\n" + 
				(assetURI != null ? "		FILTER(?pna=<" + SparqlHelper.escapeURI(assetURI) + ">) .\n" : "") +
				"    OPTIONAL { ?pna rdfs:label ?pnal }\r\n" + 
				"    OPTIONAL {\r\n" + 
				"			GRAPH <%s> {\r\n" + 
				"				?pnr rdfs:label ?pnrl\r\n" + 
				"			}\r\n" + 
				"		}" + 
				"}", model.getGraph("system-inf"), model.getGraph("domain"));
		rows = store.translateSelectResult(store.querySelect(query,
				model.getGraph("system-inf"), model.getGraph("domain"), model.getGraph("system")));
		
		for (Map<String, String> row : rows) {
			String nodeUri = row.get("pn");
			String patternUri = row.get("p");
			Node node = new Node(row.get("pna"), row.get("pnal"), row.get("pnr"), row.get("pnrl"));
			node.setUri(nodeUri);

			Pattern pattern = patterns.get(patternUri);
			if (pattern != null) {
				pattern.getNodes().add(node);
			}
		}
	
		
		// ---- Add pattern links -----

		query = String.format("SELECT DISTINCT * WHERE {\r\n" + 
				"    GRAPH <%s> {\r\n" + 
				"        ?p ?hasLink ?pli .\r\n" + 
				"        ?pli core:linksFrom ?plifr .\r\n" + 
				"        ?pli core:linksTo ?plitr .\r\n" + 
				"        ?pli core:linkType ?plist .\r\n" + 
				"    }\r\n" + 
				"    ?plit rdfs:subPropertyOf* ?plist .\r\n" + 
				"    ?plifa ?plit ?plita .\r\n" + 
				"    OPTIONAL {\r\n" + 
				"        GRAPH <%s> {\r\n" + 
				"            ?plit rdfs:label ?plitlabel\r\n" + 
				"        }\r\n" + 
				"    }\r\n" + 
				"}", model.getGraph("domain") , model.getGraph("domain"));
		rows = store.translateSelectResult(store.querySelect(query,
				model.getGraph("domain"), model.getGraph("system-inf"), model.getGraph("system")));
		
		for (Map<String, String> row : rows) {
			
			String patternUri = row.get("p");
			String fromRoleUri = row.get("plifr");
			String toRoleUri = row.get("plitr");
			String fromAssetUri = row.get("plifa");
			String toAssetUri = row.get("plita");
			
			// Combine all the children (if generating pattern) or stems (if root pattern) of this pattern.
			List<Pattern> allPatterns = new ArrayList<Pattern>();
			List<Pattern> patternChildren = children.get(patternUri);
			if (patternChildren != null) {
				allPatterns.addAll(patternChildren);
			}
			List<Pattern> patternStems = stems.get(patternUri);
			if (patternStems != null) {
				allPatterns.addAll(patternStems);
			}

			for (Pattern p : allPatterns) {
				boolean containsFrom = false;
				boolean containsTo = false;		
				Node fromNode = null;
				Node toNode = null;
				
				// Add the link to the pattern only if the pattern contains a node with the from role and from 
				// asset of the link, AND another node with the to role and to asset of the link.
				for (Node node : p.getNodes()) {
					if (node.getRole().equals(fromRoleUri) && node.getAsset().equals(fromAssetUri)) {
						containsFrom = true;
						fromNode = node;
					} else if (node.getRole().equals(toRoleUri) && node.getAsset().equals(toAssetUri)) {
						containsTo = true;
						toNode = node;
					}
				}		
				
				if (containsFrom && containsTo) {
					Link link = new Link(fromAssetUri, fromNode.getAssetLabel(), 
							fromRoleUri, fromNode.getRoleLabel(), toAssetUri, 
							toNode.getAssetLabel(), toRoleUri, toNode.getRoleLabel(), 
							row.get("plit"), row.get("plitlabel"));
					p.getLinks().add(link);
				}
			}
		}
		
		if (patternURI != null) {
			Pattern pattern = patterns.get(patternURI);
			patterns.clear();
			patterns.put(pattern.getUri(), pattern);
		}
		
		// Filter patterns.
		if (assetURI != null) {
			Map<String, Pattern> filteredPatterns = new HashMap<>();
			
			for (Pattern p : patterns.values()) {
				if (p.getNodes().size() > 0) {
					filteredPatterns.put(p.getUri(), p);
				} 
			}
			
			return filteredPatterns;
		} else {
			return patterns;
		}
		
	}
	

	// Threats ////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Gets a map of threats that have an updated value for the resolved field
	 *
	 * @param store
	 * @param threats the existing threats
	 * @return the updated threats
	 */
	public Map<String, Threat> getUpdatedThreatStatus(AStoreWrapper store, Map<String, Threat> threats) {
		return getUpdatedThreatStatus(store, threats, getControlSets(store));
	}

	/**
	 * Gets a map of threats that have an updated value for the resolved field
	 *
	 * @param store
	 * @param threats the existing threats
	 * @param controlSets all control sets
	 * @return the updated threats
	 */
	public Map<String, Threat> getUpdatedThreatStatus(AStoreWrapper store,
			Map<String, Threat> threats,
			Map<String, ControlSet> controlSets) {

		logger.info("Getting updated threat status");

		Map<String, Threat> result = new HashMap<>();

		for (Threat t : threats.values()) {

			Threat updatedThreat = t;

			//check all control strategies in a threat
			for (ControlStrategy csg: t.getControlStrategies().values()) {
				//check all control sets in the csg against the new ones
				for (ControlSet cs: csg.getMandatoryControlSets().values()) {
					//if this control set is in the updated control sets, replace the old one with it
					if (controlSets.containsKey(cs.getUri())) {
						updatedThreat.getControlStrategies().get(csg.getUri()).getMandatoryControlSets().put(cs.getUri(), controlSets.get(cs.getUri()));
					}
				}
			}
			//update the threat's status
			updatedThreat.isResolved();

			result.put(updatedThreat.getUri(), updatedThreat);
		}

		return result;
	}

	/**
	 * Get all system-specific threats
	 *
	 * @param store the store to query
	 * @return all threats
	 */
	public Map<String, Threat> getSystemThreats(AStoreWrapper store) {
		return getSystemThreats(store,
			getSystemPatterns(store),
			getMisbehaviourSets(store, true),
			getControlStrategies(store),
			getTrustworthinessAttributeSets(store));
	}
	
	/**
	 * Get all system-specific threats
	 *
	 * @param store the store to query
	 * @param patterns all patterns
	 * @param misbehaviourSets all misbehaviour sets
	 * @param controlStrategies all control strategies
	 * @param trustworthinessAttributeSets all TWAS
	 * @return all threats
	 */
	public Map<String, Threat> getSystemThreats(AStoreWrapper store,
			Map<String, Pattern> patterns,
			Map<String, MisbehaviourSet> misbehaviourSets,
			Map<String, ControlStrategy> controlStrategies,
			Map<String, TrustworthinessAttributeSet> trustworthinessAttributeSets) {

		logger.info("Getting system threats");

		return createThreats(store, null, null, null,
				patterns, misbehaviourSets, controlStrategies, trustworthinessAttributeSets);
	}

	/**
	 * Get all system-specific threats for a given asset
	 *
	 * @param store the store to query
	 * @param assetURI the asset for which to get the threats
	 * @return r ms of all threats
	 */
	public Map<String, Threat> getSystemThreats(AStoreWrapper store, String assetURI) {

		logger.info("Getting system threats for asset {}", assetURI);

		return createThreats(store, assetURI, null, null,
			getSystemPatterns(store, assetURI),
			getMisbehaviourSets(store, false),
			getControlStrategies(store),
			getTrustworthinessAttributeSets(store)
		);
	}

	/**
	 * Get r system-specific threat
	 *
	 * @param store the store to query
	 * @param threatURI the URI of the threat
	 * @return the threat or null if it doesn'r exist
	 */
	public Threat getSystemThreat(AStoreWrapper store, String threatURI) {

		logger.info("Getting system threat {}", threatURI);

		Map<String, Threat> threats = createThreats(store, null, threatURI, null,
			getSystemPatterns(store),
			getMisbehaviourSets(store, true),
			getControlStrategiesForThreat(store, threatURI),
			//TODO: filter by threat/ep?
			getTrustworthinessAttributeSets(store)
		);

		return threats.get(threatURI);
	}

	/**
	 * Get a system-specific threat
	 *
	 * @param store the store to query
	 * @param threatId the ID of the threat
	 * @return the threat or null if it doesn'r exist
	 */
	public Threat getSystemThreatById(AStoreWrapper store, String threatId) {

		logger.info("Getting system threat with ID {}", threatId);

		Collection<Threat> results  = createThreats(store, null, null, threatId,
			getSystemPatterns(store),
			getMisbehaviourSets(store, false),
			getControlStrategies(store),
			getTrustworthinessAttributeSets(store)
		).values();
		
		if(results.isEmpty()) {
			return null;
		} else{
			return results.iterator().next();
		}
	}
	
	private Map<String, Threat> createThreats(AStoreWrapper store, 
			String assetURI, String threatURI, String threatId,
			Map<String, Pattern> patterns,
			Map<String, MisbehaviourSet> misbehaviourSets,
			Map<String, ControlStrategy> controlStrategies,
			Map<String, TrustworthinessAttributeSet> trustworthinessAttributeSets) {
				
		// ----- Get threats -----
		
		String query = String.format("SELECT DISTINCT * WHERE {\r\n" + 
				(threatURI != null ? "    BIND (<" + SparqlHelper.escapeURI(threatURI) + "> AS ?t) .\n" : "") +
				"    GRAPH <%s> {\r\n" + 
				"        ?t core:parent ?genThreat .\r\n" + 
				"        OPTIONAL { ?t core:isSecondaryThreat ?isSecondaryThreat }\r\n" + 
				"        BIND(IF(BOUND(?isSecondaryThreat),STR(?isSecondaryThreat),\"false\") AS ?isST)\n" +
				"        OPTIONAL { ?t core:isNormalOp ?isNormalOp }\r\n" + 
				"        BIND(IF(BOUND(?isNormalOp),STR(?isNormalOp),\"false\") AS ?isNO)\n" +
				(threatId != null ? "    ?t core:hasID \"" + SparqlHelper.escapeLiteral(threatId) + "\" . \n" : "") +
				"        ?t core:threatens ?a .\r\n" + 
				"        ?t core:appliesTo ?matchingPattern .\r\n" + 
				"        OPTIONAL { ?t rdfs:comment ?tDesc }\r\n" + 
				"        OPTIONAL { ?t rdfs:label ?l }\r\n" + 
				"        OPTIONAL { ?t core:isRootCause ?isRootCause }\r\n" + 
				"        BIND(IF(BOUND(?isRootCause),STR(?isRootCause),\"false\") AS ?isRC)\n" +
				"    }\r\n" +
				(assetURI != null ? "    FILTER (?a=<" + SparqlHelper.escapeURI(assetURI) + ">) .\n" : "") +
				"    GRAPH <%s> {\r\n" + 
				"        ?genThreat a ?threat .\r\n" + 
				"        ?threat rdfs:subClassOf* core:Threat .\r\n" + 
				"        OPTIONAL { ?genThreat rdfs:label ?gtl }\r\n" + 
				"        OPTIONAL { ?genThreat rdfs:comment ?genDesc }\r\n" + 
				"    }" +
				"}", model.getGraph("system-inf"), 
				model.getGraph("domain"));
		List<Map<String, String>> rows = store.translateSelectResult(store.querySelect(query,
				model.getGraph("domain"),
				model.getGraph("system-inf")));
		
		Map<String, Threat> threats = new HashMap<String, Threat>();
		
		if (rows.isEmpty()) {
			logger.debug("No threats found");
			return threats;
		}
		
		for (Map<String, String> row : rows) {
			String threatUri = row.get("t");

			if (!row.containsKey("isSecondaryThreat")) {
				logger.debug("isSecondaryThreat missing for threat: {}", threatUri);
			}

			//is threat a secondary threat (otherwise primary)
			boolean secondaryThreat = Boolean.parseBoolean(row.get("isST"));

			//is threat a normal operation
			boolean normalOperation = Boolean.parseBoolean(row.get("isNO"));

			Threat threat = new Threat(threatUri,
					row.get("l"),
					row.get("tDesc")!=null?row.get("tDesc"):(row.get("genDesc")!=null?row.get("genDesc"):"No description available"),
					patterns.get(row.get("matchingPattern")),
					row.get("a"),
					row.get("genThreat"),
					secondaryThreat,
					null, 
					new HashMap<>(), new HashMap<>(), new HashMap<>());

			threat.setNormalOperation(normalOperation);

			//is threat a root cause of a misbehaviour set?
			threat.setRootCause(Boolean.valueOf(row.get("isRC")));
			
			if (threats.put(threatUri, threat) != null) {
				throw new RuntimeException("Duplicate Threat with URI " + threatUri + " found.");
			}
		}
				
		// ----- Get indirect misbehaviours -----
		
		query = String.format("SELECT DISTINCT * WHERE {\r\n" +
				(threatURI != null ? "	BIND (<" + SparqlHelper.escapeURI(threatURI) + "> AS ?t) .\n" : "") +
				"    GRAPH <%s> {\r\n" + 
				(threatId != null ? "	?t core:hasID \"" + SparqlHelper.escapeLiteral(threatId) + "\" . \n" : "") +
				"        ?t core:causesIndirectMisbehaviour ?ms\r\n" + 
				"    }\r\n" + 
				"}", model.getGraph("system-inf"));
		rows = store.translateSelectResult(store.querySelect(query, 
				model.getGraph("system-inf")));
		
		for (Map<String, String> row : rows) {
			String threatUri = row.get("t");
			String effectUri = row.get("ms");
			
			MisbehaviourSet misbs = misbehaviourSets.get(effectUri);
			if (misbs != null && threats.containsKey(threatUri)) {
				if (threats.get(threatUri).getIndirectEffects().put(effectUri, misbs) != null) {
					throw new RuntimeException("Duplicate MisbehaviourSet with URI " + misbs.getUri() + 
							 " associated with IndirectEffect with URI " + effectUri + " for Threat with URI " + threatUri);
				}
			}
		}
			
		
		// ----- Get misbehaviours -----
		
		query = String.format("SELECT DISTINCT * WHERE {\r\n" + 
				(threatURI != null ? "	BIND (<" + SparqlHelper.escapeURI(threatURI) + "> AS ?t) .\n" : "") +
				"	GRAPH <%s> {\r\n" + 
				(threatId != null ? "	?t core:hasID \"" + SparqlHelper.escapeLiteral(threatId) + "\" . \n" : "") +
				"        ?t core:causesMisbehaviour ?cmisb .\r\n" + 
				"    }\r\n" + 
				"}", model.getGraph("system-inf"));
		rows = store.translateSelectResult(store.querySelect(query,
				model.getGraph("system-inf")));
		
		for (Map<String, String> row : rows) {
			String threatUri = row.get("t");
			MisbehaviourSet cm = misbehaviourSets.get(row.get("cmisb"));
			if ( threats.containsKey(threatUri)) {
				if (cm != null) {
					if (!threats.get(threatUri).getMisbehaviours().containsKey(cm.getUri())) {
						threats.get(threatUri).getMisbehaviours().put(cm.getUri(), cm);
					}
				} else {
					logger.warn("Could not find misbehaviour set {} for threat {}", 
							row.get("cmisb"), threats.get(threatUri).getLabel());
				}
			}
		}
		
		// -----  Get acceptance justifications -----

		query = String.format("SELECT DISTINCT * WHERE {\r\n" + 
				(threatURI != null ? "	BIND (<" + SparqlHelper.escapeURI(threatURI) + "> AS ?t) .\n" : "") +
				"    GRAPH <%s> {\r\n" + 
				(threatId != null ? "	?t core:hasID \"" + SparqlHelper.escapeLiteral(threatId) + "\" . \n" : "") +
				"        ?t core:acceptanceJustification ?aj .\r\n" + 
				"    }\r\n" + 
				"}", model.getGraph("system"));
		rows = store.translateSelectResult(store.querySelect(query,
				model.getGraph("system")));
		
		for (Map<String, String> row : rows) {
			String threatUri = row.get("t");
			Threat threat = threats.get(threatUri);
			if (threat != null) {
				threat.setAcceptanceJustification(row.get("aj"));
			}
			else {
				logger.warn("accepted threat does not exist: {}", threatUri);
			}
		}
		
		// ----- Get secondary effect conditions -----
		
		query = String.format("SELECT DISTINCT * WHERE {\r\n" + 
				(threatURI != null ? "	BIND (<" + SparqlHelper.escapeURI(threatURI) + "> AS ?t) .\n" : "") +
				"    GRAPH <%s> {\r\n" + 
				(threatId != null ? "	?t core:hasID \"" + SparqlHelper.escapeLiteral(threatId) + "\" . \n" : "") +
				"        ?t core:hasSecondaryEffectCondition ?seceff .\r\n" + 
				"    }\r\n" + 
				"}", model.getGraph("system-inf"));
		rows = store.translateSelectResult(store.querySelect(query,
				model.getGraph("system-inf")));
		
		for (Map<String, String> row : rows) {
			String threatUri = row.get("t");
			String seceffUri = row.get("seceff");
			Threat threat = threats.get(threatUri);
			if (threat != null) {
				if (threat.getSecondaryEffectConditions().put(seceffUri, misbehaviourSets.get(seceffUri)) != null) {
					throw new RuntimeException("Duplicate MisbehaviourSet with URI " + misbehaviourSets.get(seceffUri).getUri() + 
							 " associated with SecondaryEffectCondition with URI " + seceffUri + " at Threat " + threatUri);
				}
			}
		}
		
		// ----- Get entry points -----
		
		query = String.format("SELECT DISTINCT * WHERE {\r\n" + 
				(threatURI != null ? "	BIND (<" + SparqlHelper.escapeURI(threatURI) + "> AS ?t) .\n" : "") +
				"    GRAPH <%s> {\r\n" + 
				(threatId != null ? "	?t core:hasID \"" + SparqlHelper.escapeLiteral(threatId) + "\" . \n" : "") +
				"        ?t core:hasEntryPoint ?twas .\r\n" + 
				"    }\r\n" + 
				"}", model.getGraph("system-inf"));
		rows = store.translateSelectResult(store.querySelect(query,
				model.getGraph("system-inf")));
		
		for (Map<String, String> row : rows) {
			String threatUri = row.get("t");
			TrustworthinessAttributeSet twas = trustworthinessAttributeSets.get(row.get("twas"));
			if (twas != null && threats.containsKey(threatUri)) {
				if (!threats.get(threatUri).getEntryPoints().containsKey(twas.getUri())) {
					threats.get(threatUri).getEntryPoints().put(twas.getUri(), twas);
				} else {
					logger.warn("Could not find twas {} in the model", row.get("twas"));
				}
			}
		}
		
		// ----- Get likelihoods -----
		
		query = String.format("SELECT DISTINCT * WHERE {\r\n" + 
				(threatURI != null ? "	BIND (<" + SparqlHelper.escapeURI(threatURI) + "> AS ?t) .\n" : "") +
				"    GRAPH <%s> {\r\n" + 
				(threatId != null ? "	?t core:hasID \"" + SparqlHelper.escapeLiteral(threatId) + "\" . \n" : "") +
				"			?t core:hasPrior ?likelihood .\r\n" + 
				"		}\r\n" + 
				"    GRAPH <%s> {\r\n" + 
				"        ?likelihood rdfs:label ?likelihoodl .\r\n" + 
				"        OPTIONAL { ?likelihood rdfs:comment ?likelihoodc }\r\n" + 
				"        ?likelihood core:levelValue ?likelihoodv .\r\n" + 
				"        BIND(STR(?likelihoodv) AS ?likelihoodval)\r\n" + 
				"    }\r\n" + 
				"}", model.getGraph("system-inf"), model.getGraph("domain"));
		rows = store.translateSelectResult(store.querySelect(query,
				model.getGraph("system-inf"), model.getGraph("domain")));
		
		for (Map<String, String> row : rows) {
			String threatUri = row.get("t");
			Level likelihood = new Level();
			likelihood.setUri(row.get("likelihood"));	
			String value = row.get("likelihoodl");
			if (value != null) {
				likelihood.setLabel(value);
			}
			value = row.get("likelihoodc");
			if (value != null) {
				likelihood.setDescription(value);
			}
			value = row.get("likelihoodval");
			if (value != null) {
				likelihood.setValue(Integer.valueOf(value));
			}
			
			Threat threat = threats.get(threatUri);
			if (threat != null) {
				threat.setLikelihood(likelihood);
			}
		}
		
		// ----- Get risks -----
		
		query = String.format("SELECT DISTINCT * WHERE {\r\n" + 
				(threatURI != null ? "	BIND (<" + SparqlHelper.escapeURI(threatURI) + "> AS ?t) .\n" : "") +
				"    GRAPH <%s> {\r\n" + 
				(threatId != null ? "	?t core:hasID \"" + SparqlHelper.escapeLiteral(threatId) + "\" . \n" : "") +
				"			?t core:hasRisk ?risk .\r\n" + 
				"		}\r\n" + 
				"    GRAPH <%s> {\r\n" + 
				"        ?risk rdfs:label ?riskl .\r\n" + 
				"        OPTIONAL { ?risk rdfs:comment ?riskc }\r\n" + 
				"        ?risk core:levelValue ?riskv .\r\n" + 
				"        BIND(STR(?riskv) AS ?riskval)\r\n" + 
				"    }\r\n" + 
				"}", model.getGraph("system-inf"), model.getGraph("domain"));
		rows = store.translateSelectResult(store.querySelect(query,
				model.getGraph("system-inf"), model.getGraph("domain")));
		
		for (Map<String, String> row : rows) {
			String threatUri = row.get("t");
			Level risk = new Level();
			risk.setUri(row.get("risk"));	
			String value = row.get("riskl");
			if (value != null) {
				risk.setLabel(value);
			}
			value = row.get("riskc");
			if (value != null) {
				risk.setDescription(value);
			}
			value = row.get("riskval");
			if (value != null) {
				risk.setValue(Integer.valueOf(value));
			}
			
			Threat threat = threats.get(threatUri);
			if (threat != null) {
				threat.setRiskLevel(risk);
			}
		}
		
		logger.info("Total control strategies: {}", controlStrategies.size());

		for (ControlStrategy csg : controlStrategies.values()) {
			for (String csgThreat : csg.getThreatCsgTypes().keySet()) {
				Threat threat = threats.get(csgThreat);
				if (threat != null) {
					//logger.debug("  Adding CSG {} to threat {}", csg.getUri(), threat.getUri());
					if (threat.getControlStrategies().put(csg.getUri(), csg) != null) {
						throw new RuntimeException("Duplicate ControlStrategy with URI " + csg.getUri() + 
								 " added to Threat with URI " + threat.getUri());
					}
				}
				else {
					logger.warn("Coud not locate threat: {}", csgThreat);
				}
			}
		}
		
		for (Threat threat : threats.values()) {
			// TODO: Getter shouldn't have side effects.
			threat.isResolved();
		}
		
		 Map<String, Threat> filteredThreats = new HashMap<String, Threat>();
		 for (Threat threat : threats.values()) {
			 if (!threat.getMisbehaviours().isEmpty() || !threat.getEntryPoints().isEmpty() || !threat.getSecondaryEffectConditions().isEmpty()) {
				 filteredThreats.put(threat.getUri(), threat);
			 }
		 }
		
		 return filteredThreats;
	}

	// ComplianceThreats //////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Get all system-specific compliance threats
	 *
	 * @param store the store to query
	 * @return all threats
	 */
	public Map<String, ComplianceThreat> getComplianceThreats(AStoreWrapper store) {
		return getComplianceThreats(store,
			getSystemPatterns(store),
			getControlStrategies(store));
	}

	/**
	 * Get all system-specific compliance  threats
	 *
	 * @param store the store to query
	 * @param patterns all patterns
	 * @param controlStrategies all control strategies
	 * @return all threats
	 */
	public Map<String, ComplianceThreat> getComplianceThreats(AStoreWrapper store,
			Map<String, Pattern> patterns,
			Map<String, ControlStrategy> controlStrategies) {

		logger.info("Getting compliance threats");

		return createComplianceThreats(store.translateSelectResult(store.querySelect(
			createComplianceThreatSparql(),
			model.getGraph("core"),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf")
		)), patterns, controlStrategies);
	}

	/**
	 * Creates the SPARQL query to retrieve threats using various filtering options
	 *
	 * @return SPARQL to retrieve a map of threats that match the filtering criteria
	 */
	private String createComplianceThreatSparql() {

		return "SELECT DISTINCT * WHERE {\n" +
		"	GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"		?t core:parent ?genT .\n" +
		"		?t core:threatens ?a .\n" +
		"		?t core:appliesTo ?p .\n" +
		"		OPTIONAL { ?t rdfs:comment ?tDesc }\n" +
		"		OPTIONAL { ?t rdfs:label ?l }\n" +
		"	}\n" +
		"	GRAPH <" + model.getGraph("domain") + "> {\n" +
		"		?genT a ?threat .\n" +
		"		?threat rdfs:subClassOf* core:Threat .\n" +
		"		OPTIONAL { ?genT rdfs:label ?gtl }\n" +
		"		OPTIONAL { ?genT rdfs:comment ?genDesc }\n" +
		"	}\n" +
		"	FILTER NOT EXISTS {\n" +
		"		GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"			?t core:causesMisbehaviour ?cmisb .\n" +
		"		}\n" +
		"	}\n" +
		"	FILTER NOT EXISTS {\n" +
		"		GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"			?t core:hasSecondaryEffectCondition ?seceff .\n" +
		"		}\n" +
		"	}\n" +
		"	FILTER NOT EXISTS {\n" +
		"		GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"			?t core:hasEntryPoint ?twas .\n" +
		"		}\n" +
		"	}\n" +
		"	BIND(IF(BOUND(?tDesc), ?tDesc, IF(BOUND(?genDesc), ?genDesc, \"No description available\")) AS ?desc)\n" +
		"}";
	}

	/**
	 * Build threats from the SPARQL results
	 *
	 * @param sparqlResults the results as retrieved from the store
	 * @return a map of threats
	 */
	private Map<String, ComplianceThreat> createComplianceThreats(List<Map<String, String>> sparqlResults,
			Map<String, Pattern> patterns,
			Map<String, ControlStrategy> controlStrategies) {

		Map<String, ComplianceThreat> map = new HashMap<>();
		for (Map<String, String> row : sparqlResults) {

			String t = row.get("t");

			//add the threat if it doesn't exist yet
			if (!map.containsKey(t)) {
				ComplianceThreat threat = new ComplianceThreat();
				threat.setUri(t);
				threat.setLabel(row.get("l"));
				threat.setDescription(row.get("desc"));
				threat.setPattern(new Pattern(row.get("p"), null));
				threat.setThreatensAssets(row.get("a"));
				threat.setType(row.get("genT"));
				map.put(t, threat);
			}
		}

		//add control strategies (CSGs may now refer to multiple threats, each with a potentially different type)
		for (ControlStrategy csg : controlStrategies.values()) {
			for (String csgThreat : csg.getThreatCsgTypes().keySet()) {
				if (map.containsKey(csgThreat)) {
					ComplianceThreat threat = map.get(csgThreat);
					threat.getControlStrategies().put(csg.getUri(), csg);
				}
			}
		}

		for (ComplianceThreat t : map.values()) {
			//add the pattern
			t.setPattern(patterns.get(t.getPattern().getUri()));

			//set threat resolved if any CSG is enabled.
			t.isResolved();
		}

		return map;
	}

	// Controls ///////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Get all system-specific control sets
	 *
	 * @param store the store to query
	 * @return the control sets
	 */
	public Map<String, ControlSet> getControlSets(AStoreWrapper store) {

		logger.info("Getting system control sets");

		return createControlSets(store.translateSelectResult(
			store.querySelect(createControlSetSparql(null, null, null, null, null),
			model.getGraph("core"),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf"))),
			this.getControls(store)
		);
	}

	/**
	 * Get a system-specific control ms
	 *
	 * @param store the store to query
	 * @param csURI the URI of the control ms
	 * @return the control ms or null if it doesn't exist
	 */
	public ControlSet getControlSet(AStoreWrapper store, String csURI) {

		logger.info("Getting system control set {}", csURI);

		Map<String, ControlSet> result = createControlSets(store.translateSelectResult(
			store.querySelect(createControlSetSparql(csURI, null, null, null, null),
			model.getGraph("core"),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf"))),
			this.getControls(store)
		);
		return result.get(csURI);
	}

	/**
	 * Get a system-specific control ms
	 *
	 * @param store the store to query
	 * @param csId the ID of the control ms
	 * @return the control ms or null if it doesn't exist
	 */
	public ControlSet getControlSetById(AStoreWrapper store, String csId) {

		logger.info("Getting system control set with ID {}", csId);

		Map<String, ControlSet> result = createControlSets(store.translateSelectResult(
			store.querySelect(createControlSetSparql(null, csId, null, null, null),
			model.getGraph("core"),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf"))),
			this.getControls(store)
		);
		return (result!=null && !result.isEmpty())?result.values().iterator().next():null;
	}

	/**
	 * Get control sets for a specific asset
	 *
	 * @param store the store to query
	 * @param assetURI the URI of the assetURI or null for no filtering
	 * @param assetId the ID of the asset or null for no filtering
	 * @return the control set or null if it doesn't exist
	 */
	public Map<String, ControlSet> getControlSets(AStoreWrapper store, String assetURI, String assetId) {

		logger.info("Getting system control sets for asset {} with ID {}", assetURI, assetId);

		return createControlSets(store.translateSelectResult(store.querySelect(
			createControlSetSparql(null, null, null, assetURI, assetId),
			model.getGraph("core"),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf")
		)), this.getControls(store));
	}

	public Map<String, Control> getControls(AStoreWrapper store) {

		logger.info("Getting system controls");

		return createControls(store.translateSelectResult(store.querySelect(
			createControlSparql(),
			model.getGraph("core"),
			model.getGraph("domain")
		)));
	}

	/**
	 * Get all system-specific control sets for a given control strategy
	 *
	 * @param store the store to query
	 * @param csg the URI of the control strategy in which this control ms is contained
	 * @return the control sets
	 */
	public Map<String, ControlSet> getControlSetsForControlStrategy(AStoreWrapper store, String csg) {

		logger.info("Getting system control sets for control strategy {}", csg);

		return createControlSets(store.translateSelectResult(store.querySelect(
			createControlSetSparql(null, null, csg, null, null),
			model.getGraph("core"),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf")
		)), this.getControls(store));
	}

	/**
	 * Creates the SPARQL query to retrieve control sets using various filtering options
	 *
	 * @param csURI the control set URI to filter by or null for all control sets, can be null
	 * @param csId the control set ID to filter by or null for all control sets, can be null
	 * @param csg the URI of the control strategy in which this control set is contained, can be null
	 * @param asssetURI the URI of the asset that the control set is located at, can be null
	 * @param assetId the ID of the asset that the control set is located at, can be null
	 * @return SPARQL to retrieve a map of control sets that match the filtering criteria
	 */
	private String createControlSetSparql(String csURI, String csId, String csg, String assetURI, String assetId) {

		return "SELECT DISTINCT * WHERE {\n" +
		"	GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		(csg != null ? "		BIND (<" + SparqlHelper.escapeURI(csg) + "> as ?csg) .\n" +
		"		?csg core:hasControlSet ?cs .\n" : "") +
		(csURI != null ? "		BIND (<" + SparqlHelper.escapeURI(csURI) + "> as ?cs) .\n" : "") +
		"		?cs a core:ControlSet .\n" +
		(csId != null ? "		?cs core:hasID \"" + SparqlHelper.escapeLiteral(csId) + "\" .\n" : "") +
		(assetURI != null ? "		BIND (<" + SparqlHelper.escapeURI(assetURI) + "> as ?a) .\n" : "") +
		"		?cs core:hasControl ?c .\n" +
		"		?cs core:locatedAt ?a .\n" +
		"	}\n" +
		//the asset may be in the system or system-inf graph
		"	{\n" +
		"		GRAPH <" + model.getGraph("system") + "> {\n" +
		"			?a a ?asset .\n" +
		"			?a core:hasID ?aid .\n" +
		"		}\n" +
		"	} UNION {\n" +
		"		GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"			?a a ?asset .\n" +
		"			?a core:hasID ?aid .\n" +
		"		}\n" +
		"	}\n" +
		(assetId != null ? "	FILTER (?aid IN (\"" + SparqlHelper.escapeLiteral(assetId) + "\"))\n" : "") +
		"	OPTIONAL {\n" +
		"		?cs core:isProposed ?proposed .\n" +
		"	}\n" +
		"	OPTIONAL {\n" +
		"		?cs core:isWorkInProgress ?workInProgress .\n" +
		"	}\n" +
		"	OPTIONAL {\n" +
		"		GRAPH <" + model.getGraph("system") + "> {\n" +
		"			?cs core:hasCoverageLevel ?acl .\n" +
		"		}\n" +
		"	}\n" +
		"	OPTIONAL {\n" +
		"		GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"			?cs core:hasCoverageLevel ?dcl .\n" +
		"		}\n" +
		"	}\n" +
		"	OPTIONAL {\n" +
		"		GRAPH <" + model.getGraph("domain") + "> {\n" +
		"			?csp a core:CASetting .\n" +
		"			?csp core:metaLocatedAt ?asset .\n" +
		"			?csp core:hasControl ?c .\n" +
		"			?csp core:isAssertable ?assertable .\n" +
		"		}\n" +
		"	}\n" +
		"	BIND(IF(BOUND(?proposed),STR(?proposed),\"false\") AS ?prop)\n" +
		"	BIND(IF(BOUND(?workInProgress),STR(?workInProgress),\"false\") AS ?wip)\n" +
		"	BIND(IF(BOUND(?assertable),STR(?assertable),\"false\") AS ?assert)\n" +
		"}";
	}

	private String createControlSparql() {
		return "SELECT DISTINCT * WHERE {\n" +
		"	GRAPH <" + model.getGraph("domain") + "> {\n" +
		"		?c a core:Control .\n" +
		"		OPTIONAL { ?c rdfs:label ?cl }\n" +
		"		OPTIONAL { ?c rdfs:comment ?cd }\n" +
		//"		?asset rdfs:subClassOf* core:Asset .\n" +
		"	}\n" +
		"}";
	}

	/**
	 * Build ControlSets from the SPARQL results and controls map
	 *
	 * @return a map of all ControlSets
	 */
	private Map<String, ControlSet> createControlSets(List<Map<String, String>> result, Map<String, Control> controls) {

		Map<String, ControlSet> sets = new HashMap<>();

		for (Map<String, String> row : result) {

			String assertedCoverageLevel = row.get("acl");
			String defaultCoverageLevel = row.get("dcl");
			boolean coverageAsserted = assertedCoverageLevel != null;
			String coverageLevel = coverageAsserted ? assertedCoverageLevel : defaultCoverageLevel;

			ControlSet cs = new ControlSet(
				row.get("cs"),
				row.get("c"),
				row.containsKey("cl")?row.get("cl"):row.get("c"),
				row.get("a"),
				row.get("aid"),
				Boolean.valueOf(row.get("prop")),
				Boolean.valueOf(row.get("wip")),
				Boolean.valueOf(row.get("assert")),
				coverageLevel,
				coverageAsserted
			);

			cs.setDescription(row.get("cd"));
			
			if (sets.put(cs.getUri(), cs) != null) {
				 throw new RuntimeException("Duplicate ControlSet with URI " + cs.getUri() + " found.");
			}
		}

		// Add in label and description from controls data
		for (ControlSet controlSet : sets.values()) {
			String controlUri = controlSet.getControl();
			Control control = controls.get(controlUri);
			if (control != null) {
				controlSet.setLabel(control.getLabel());
				controlSet.setDescription(control.getDescription());
			}
			else {
				logger.warn("Could not locate control for controlSet: {}", controlSet.getUri());
			}
		}

		return sets;
	}

	private Map<String, Control> createControls(List<Map<String, String>> result) {

		Map<String, Control> controls = new HashMap<>();

		for (Map<String, String> row : result) {

			Control c = new Control(
				row.get("c"),
				row.get("cl"),
				row.get("cd")
			);

			if (controls.put(c.getUri(), c) != null) {
				throw new RuntimeException("Duplicate Control with URI " + c.getUri() + " found.");
			}
		}

   		return controls;
	}

	// Control Strategies /////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Get all system-specific control strategies
	 *
	 * @param store the store to query
	 * @return the control strategies
	 */
	public Map<String, ControlStrategy> getControlStrategies(AStoreWrapper store) {

		logger.info("Getting system control strategies");
		
		return createControlStrategies(store, store.translateSelectResult(store.querySelect(
				createControlStrategySparql(null, null),
				model.getGraph("core"),
				model.getGraph("domain"),
				model.getGraph("system"),
				model.getGraph("system-inf")
			)), getControlSets(store), getLevels(store, "TrustworthinessLevel"));
	}

	/**
	 * Get all system-specific control strategies
	 *
	 * @param store the store to query
	 * @param controlSets the control sets
	 * @return the control strategies
	 */
	public Map<String, ControlStrategy> getControlStrategies(AStoreWrapper store, Map<String, ControlSet> controlSets) {

		logger.info("Getting system control strategies");

		return createControlStrategies(store, store.translateSelectResult(store.querySelect(
			createControlStrategySparql(null, null),
			model.getGraph("core"),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf")
		)), controlSets, getLevels(store, "TrustworthinessLevel"));
	}

	/**
	 * Get a system-specific control strategy
	 *
	 * @param store the store to query
	 * @param csgURI the URI of the control strategy
	 * @return the misbehaviour ms or null if it doesn't exist
	 */
	public ControlStrategy getControlStrategy(AStoreWrapper store, String csgURI) {

		logger.info("Getting system control strategy {}", csgURI);

		Map<String, ControlStrategy> csgs = createControlStrategies(store, store.translateSelectResult(store.querySelect(
			createControlStrategySparql(csgURI, null),
			model.getGraph("core"),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf")
		)), getControlSets(store), getLevels(store, "TrustworthinessLevel"));
		return csgs.get(csgURI);
	}

	/**
	 * Get the system-specific control strategies for a threat
	 *
	 * @param store the store to query
	 * @param threat the URI of the threat
	 * @return the control strategy or null if it doesn't exist
	 */
	public Map<String, ControlStrategy> getControlStrategiesForThreat(AStoreWrapper store, String threat) {

		logger.info("Getting system control strategies for threat {}", threat);

		return createControlStrategies(store, store.translateSelectResult(store.querySelect(
			createControlStrategySparql(null, threat),
			model.getGraph("core"),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf")
		)), getControlSets(store), getLevels(store, "TrustworthinessLevel"));
	}

	/**
	 * Creates the SPARQL query to retrieve control strategies using various filtering options
	 *
	 * @param csgURI the control set URI to filter by or null for all control strategies
	 * @param threat the threat URI to filter by or null for any threat
	 * @return SPARQL to retrieve a map of control strategies that match the filtering criteria
	 */
	private String createControlStrategySparql(String csgURI, String threat) {

		return "SELECT DISTINCT * WHERE {\n" +
		(threat != null ? "		BIND (<" + SparqlHelper.escapeURI(threat) + "> as ?t) .\n" : "") +
		(csgURI != null ? "		BIND (<" + SparqlHelper.escapeURI(csgURI) + "> as ?csg) .\n" : "") +
		"	GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"		?csg a core:ControlStrategy .\n" +
		" 		OPTIONAL{?csg rdfs:comment ?desc .} \n" +
		"		OPTIONAL{?csg core:hasControlSet ?cs .}\n" +
		"		OPTIONAL{?csg core:hasMandatoryCS ?mcs .}\n" +
		"		OPTIONAL{?csg core:hasOptionalCS ?ocs .}\n" +
		"		?csg core:parent ?parent .\n" +
		"	}\n" +
		"	OPTIONAL {\n" +
		"		GRAPH <" + model.getGraph("domain") + "> {\n" +
		"			?parent rdfs:label ?csgl .\n" +
		"			?parent core:hasBlockingEffect ?be .\n" +
		"		}\n" +
		"	}\n" +
		"}";
	}

	/**
	 * Build control strategies from the SPARQL results
	 *
	 * @return a map of control strategies
	 */
	private Map<String, ControlStrategy> createControlStrategies(AStoreWrapper store, List<Map<String, String>> result,
			Map<String, ControlSet> controlSets, Map<String, Level> blockingEffects) {

		Map<String, ControlStrategy> csgs = new HashMap<>();

		ControlStrategy newCSG = new ControlStrategy();
		for (Map<String, String> row : result) {

			String csg = row.get("csg");

			//new CSG
			if (!csg.equals(newCSG.getUri())) {
				newCSG = new ControlStrategy();
				newCSG.setUri(csg);
				
				if(row.containsKey("desc")) {
					newCSG.setDescription(row.get("desc"));
				}

				if (row.containsKey("csgl")) {
					newCSG.setLabel(row.get("csgl"));
				}
			}

			//Add mandatory control set
			if (row.containsKey("mcs") && row.get("mcs")!=null) {
				if (controlSets.containsKey(row.get("mcs")) && controlSets.get(row.get("mcs"))!=null) {
					if (newCSG.getOptionalControlSets().containsKey(row.get("mcs")))
						throw new RuntimeException("Control set already in optional list: " + row.get("mcs"));
					logger.debug("Adding mandatory control set: {}", row.get("mcs"));
					newCSG.getMandatoryControlSets().put(row.get("mcs"), controlSets.get(row.get("mcs")));
				} else {
					logger.warn("Could not find control set <{}> for control strategy <{}>", row.get("mcs"), newCSG.getUri());
				}
			}

			//Add optional control set
			if (row.containsKey("ocs") && row.get("ocs")!=null) {
				if (controlSets.containsKey(row.get("ocs")) && controlSets.get(row.get("ocs"))!=null) {
					if (newCSG.getMandatoryControlSets().containsKey(row.get("ocs")))
						throw new RuntimeException("Control set already in mandatory list: " + row.get("ocs"));
					logger.debug("Adding optional control set: {}", row.get("ocs"));
						newCSG.getOptionalControlSets().put(row.get("ocs"), controlSets.get(row.get("ocs")));
				} else {
					logger.warn("Could not find control set <{}> for control strategy <{}>", row.get("ocs"), newCSG.getUri());
				}
			}

			//No generally required, as each CS should now be listed in mandatory ot optional lists
			//However, older system models may still contain "hasControlSet" triples
			if (row.containsKey("cs") && row.get("cs")!=null) {
				if (controlSets.containsKey(row.get("cs")) && controlSets.get(row.get("cs"))!=null) {
					//logger.warn("Control set not defined as mandatory or optional - adding to mandatory list: {}", row.get("cs"));
					newCSG.getMandatoryControlSets().put(row.get("cs"), controlSets.get(row.get("cs")));
				} else {
					logger.warn("Could not find control set <{}> for control strategy <{}>", row.get("cs"), newCSG.getUri());
				}
			}

			if (row.containsKey("be") && row.get("be")!=null) {
				if (blockingEffects.containsKey(row.get("be")) && blockingEffects.get(row.get("be"))!=null) {
					newCSG.setBlockingEffect(blockingEffects.get(row.get("be")));
				} else {
					logger.warn("Could not find blocking effect <{}> for control strategy <{}>", row.get("be"), newCSG.getUri());
				}
			}

			//this overrides older versions of the control strategy
			// TODO: Is this what we want?
			csgs.put(csg, newCSG);
		}

		this.addCsgThreats(store, csgs);

		return csgs;
	}

	private void addCsgThreats(AStoreWrapper store, Map<String, ControlStrategy> csgs) {
		logger.info("Adding threats to CSGs");

		String query = String.format("SELECT DISTINCT * WHERE {\r\n" +
				"	GRAPH <" + model.getGraph("core") + "> {\n" +
				"		?treats rdfs:label ?type .\n" +
				"	}\n" +
				"    GRAPH <%s> {\r\n" + 
				"		?csg ?treats ?t .\n" +
				"		?csg a core:ControlStrategy .\n" +
				"		?t a core:Threat .\n" +
				"    }\r\n" + 
				"}", model.getGraph("system-inf"));

		List<Map<String, String>> rows = store.translateSelectResult(store.querySelect(query, 
			model.getGraph("core"), model.getGraph("system-inf")));
		
		logger.debug("Result rows: {}", rows.size());

		for (Map<String, String> row : rows) {
			String csgURI = row.get("csg");
			String threatURI = row.get("t");
			String type = row.get("type");

			//logger.debug("{} {} {}", csgURI, type, threatURI);
			
			if ((csgURI != null) && (threatURI != null)) {
				ControlStrategy csg = csgs.get(csgURI);
				if (csg != null) {
					ControlStrategyType csgType;
					if ("mitigates".equals(type)) {
						csgType = ControlStrategyType.MITIGATE;
					} else if ("blocks".equals(type)) {
						csgType = ControlStrategyType.BLOCK;
					} else {
						csgType = ControlStrategyType.TRIGGER;
					}
					csg.getThreatCsgTypes().put(threatURI, csgType);
				}
				else {
					logger.warn("Could not locate CSG: {}", csgURI);
				}
			}
		}
	}

	// Misbehaviours //////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Get all system-specific misbehaviour sets
	 *
	 * @param store the store to query
	 * @param includeCauseAndEffects flag to indicate if causes and effects should be returned for each misbehaviour
	 * @return the misbehaviour sets
	 */
	public Map<String, MisbehaviourSet> getMisbehaviourSets(AStoreWrapper store, boolean includeCauseAndEffects) {

		logger.info("Getting system misbehaviour sets: includeCauseAndEffects = {}", includeCauseAndEffects);

		return createMisbehaviourSets(store, store.translateSelectResult(store.querySelect(
			createMisbehaviourSetSparql(null, null, null, null),
			model.getGraph("core"),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf")
		)), getLevels(store, "ImpactLevel", "Likelihood", "RiskLevel"), includeCauseAndEffects);
	}

	/**
	 * Get a system-specific misbehaviour set
	 *
	 * @param store the store to query
	 * @param msURI the URI of the misbehaviour set
	 * @param includeCauseAndEffects flag to indicate if causes and effects should be returned for misbehaviour
	 * @return the misbehaviour set or null if it doesn't exist
	 */
	public MisbehaviourSet getMisbehaviourSet(AStoreWrapper store, String msURI, boolean includeCauseAndEffects) {

		logger.info("Getting system misbehaviour set {}", msURI);

		Map<String, MisbehaviourSet> result = createMisbehaviourSets(store,
				store.translateSelectResult(store.querySelect(createMisbehaviourSetSparql(msURI, null, null, null),
			model.getGraph("core"),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf")
		)), getLevels(store, "ImpactLevel", "Likelihood", "RiskLevel"), includeCauseAndEffects);
		return result.get(msURI);
	}

	/**
	 * Get a system-specific misbehaviour set
	 *
	 * @param store the store to query
	 * @param msID the ID of the misbehaviour set
	 * @return the misbehaviour set or null if it doesn't exist
	 */
	public MisbehaviourSet getMisbehaviourSetByID(AStoreWrapper store, String msID, boolean includeCauseAndEffects) {

		logger.info("Getting system misbehaviour set with ID {}", msID);

		Map<String, MisbehaviourSet> result = createMisbehaviourSets(store, store.translateSelectResult(store.querySelect(
			createMisbehaviourSetSparql(null, msID, null, null),
			model.getGraph("core"),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf")
		)), getLevels(store, "ImpactLevel", "Likelihood", "RiskLevel"), includeCauseAndEffects);
		return (result!=null && !result.isEmpty())?result.values().iterator().next():null;
	}

	/**
	 * Get misbehaviour sets for a specific asset
	 *
	 * @param store the store to query
	 * @param assetURI the URI of the assetURI or null for no filtering
	 * @param assetId the ID of the asset or null for no filtering
	 * @return the misbehaviour set or null if it doesn't exist
	 */
	public Map<String, MisbehaviourSet> getMisbehaviourSets(AStoreWrapper store, String assetURI, String assetId, boolean includeCauseAndEffects) {

		logger.info("Getting system misbehaviour sets for asset {}, ID {}", assetURI, assetId);

		return createMisbehaviourSets(store, store.translateSelectResult(store.querySelect(
			createMisbehaviourSetSparql(null, null, assetURI, assetId),
			model.getGraph("core"),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf")
		)), getLevels(store, "ImpactLevel", "Likelihood", "RiskLevel"), includeCauseAndEffects);
	}

	/**
	 * Creates the SPARQL query to retrieve misbehaviour sets using various filtering options
	 *
	 * @param msURI the misbehaviour set URI to filter by or null for all misbehaviour sets
	 * @param assetURI the URI of the assetURI on which the misbehaviour is located
	 * @return SPARQL to retrieve a set of misbehaviour sets that match the filtering criteria
	 */
	private String createMisbehaviourSetSparql(String msURI, String msID, String assetURI, String assetId) {
		return "SELECT DISTINCT * WHERE {\n" +
		"	GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		(msURI != null ? "		BIND (<" + SparqlHelper.escapeURI(msURI) + "> as ?ms) .\n" : "") +
		(assetURI != null ? "		BIND (<" + SparqlHelper.escapeURI(assetURI) + "> as ?a) .\n" : "") +
		"		?ms core:hasMisbehaviour ?m .\n" +
		(msID != null ? "		?ms core:hasID \"" + SparqlHelper.escapeLiteral(msID) + "\" .\n" : "") +
		"		?ms core:locatedAt ?a .\n" +
		"		?ms a core:MisbehaviourSet .\n" +
		"       OPTIONAL { ?ms core:isNormalOpEffect ?isNormalOpEffect } \n" +
		"	}\n" +
		//the assetURI may be in the system or system-inf graph
		"	{\n" +
		"		GRAPH <" + model.getGraph("system") + "> {\n" +
		"			?a a ?asset .\n" +
		"			?a rdfs:label ?al .\n" +
		(assetId != null ? "			?a core:hasID \"" + SparqlHelper.escapeLiteral(assetId) + "\" .\n" : "") +
		"		}\n" +
		"	} UNION {\n" +
		"		GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"			?a a ?asset .\n" +
		"			?a rdfs:label ?al .\n" +
		(assetId != null ? "			?a core:hasID \"" + SparqlHelper.escapeLiteral(assetId) + "\" .\n" : "") +
		"		}\n" +
		"	}\n" +
		//the impact level - prefer asserted ones to inferred ones
		"	OPTIONAL {\n" +
		"		GRAPH <" + model.getGraph("system") + "> {\n" +
		"			?ms core:hasImpactLevel ?impA .\n" +
		"		}\n" +
		"	}\n" +
		"	OPTIONAL {\n" +
		"		GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"			?ms core:hasImpactLevel ?impI .\n" +
		"		}\n" +
		"	}\n" +
		"	BIND(IF(BOUND(?impA),?impA,?impI) AS ?imp)" +
		//the likelihood (inferred)
		"	OPTIONAL {\n" +
		"		GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"			?ms core:hasPrior ?pl .\n" +
		"		}\n" +
		"	}\n" +
		//the risk level (inferred)
		"	OPTIONAL {\n" +
		"		GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"			?ms core:hasRisk ?rl .\n" +
		"		}\n" +
		"	}\n" +
		"	GRAPH <" + model.getGraph("domain") + "> {\n" +
		"		?m rdfs:label ?ml .\n" +
		"		OPTIONAL { ?m rdfs:comment ?md } \n" +
		"		OPTIONAL { ?m core:isVisible ?isVisible } \n" +
		"	}\n" +
		"   BIND(IF(BOUND(?isVisible),STR(?isVisible),\"true\") AS ?vis)\n" +
		"   BIND(IF(BOUND(?isNormalOpEffect),STR(?isNormalOpEffect),\"false\") AS ?nop)\n" +
		"}";
	}

	/**
	 * Build MisbehaviourSets from the SPARQL results
	 *
	 * @param sparqlResults the SPARQL result "table"
	 * @return a map of all MisbehaviourSets
	 */
	private Map<String, MisbehaviourSet> createMisbehaviourSets(AStoreWrapper store,
			List<Map<String, String>> sparqlResults, Map<String, Level> levels, boolean includeCauseAndEffects) {

		logger.debug("createMisbehaviourSets: includeCauseAndEffects = {}", includeCauseAndEffects);

		Map<String, Set<String>> directCauses = new HashMap<>();
		Map<String, Set<String>> indirectCauses = new HashMap<>();
		Map<String, Set<String>> directEffects = new HashMap<>();
		Map<String, Set<String>> rootCauses = new HashMap<>();

		if (includeCauseAndEffects) {
			//get caused misbehaviours
			//logger.debug("Get caused misbehaviours");
			String sparql = "SELECT DISTINCT ?t ?dc ?ic ?de ?twas ?ms WHERE {\n"
					+ "	?t a ?threat .\n"
					+ "	?t core:parent ?parent .\n"
					+ "	?threat rdfs:subClassOf* core:Threat .\n"
					+ "	OPTIONAL {\n"
					+ "		?t core:hasEntryPoint ?twas .\n"
					+ "		?twis core:affects ?twas .\n"
					+ "		?twis core:affectedBy ?ms .\n"
					+ "	}\n"
					+ "	{\n"
					+ "		?t core:causesMisbehaviour ?dc .\n"
					+ "	} UNION {\n"
					+ "		?t core:causesIndirectMisbehaviour ?ic .\n"
					+ "	}\n"
					+ "	OPTIONAL { ?t core:hasSecondaryEffectCondition ?de }\n"
					+ "}";

			List<Map<String, String>> result = store.translateSelectResult(store.querySelect(sparql,
					model.getGraph("domain"),
					model.getGraph("system"),
					model.getGraph("system-inf")
			));

			for (Map<String, String> row : result) {
				if (row.containsKey("dc") && row.get("dc") != null) {
					if (!directCauses.containsKey(row.get("dc"))) {
						directCauses.put(row.get("dc"), new HashSet<>());
					}
					directCauses.get(row.get("dc")).add(row.get("t"));
				}

				if (row.containsKey("ic") && row.get("ic") != null) {
					if (!indirectCauses.containsKey(row.get("ic"))) {
						indirectCauses.put(row.get("ic"), new HashSet<>());
					}
					indirectCauses.get(row.get("ic")).add(row.get("t"));
				}

				if (row.containsKey("de") && row.get("de") != null) {
					if (!directEffects.containsKey(row.get("de"))) {
						directEffects.put(row.get("de"), new HashSet<>());
					}
					directEffects.get(row.get("de")).add(row.get("t"));
				}

				if (row.containsKey("ms") && row.get("ms") != null) {
					if (!directEffects.containsKey(row.get("ms"))) {
						directEffects.put(row.get("ms"), new HashSet<>());
					}
					directEffects.get(row.get("ms")).add(row.get("t"));
				}
			}

			//get root causes for misbehaviour sets
			//logger.debug("Get root causes for misbehaviour sets");
			sparql = "SELECT DISTINCT ?t ?ms WHERE {\n"
					+ "	?t a ?threat .\n"
					+ "	?t core:parent ?parent .\n"
					+ "	?threat rdfs:subClassOf* core:Threat .\n"
					+ "	?ms core:hasRootCause ?t .\n"
					+ "}";

			List<Map<String, String>> rootCauseResults = store.translateSelectResult(store.querySelect(sparql,
					model.getGraph("domain"),
					model.getGraph("system"),
					model.getGraph("system-inf")
			));

			for (Map<String, String> row : rootCauseResults) {
				String msURI = row.get("ms");
				String threatURI = row.get("t");
				if (!rootCauses.containsKey(msURI)) {
					rootCauses.put(msURI, new HashSet<>());
				}
				rootCauses.get(msURI).add(threatURI);
			}

		}
		
		//create misbehaviour sets
		//logger.debug("Create misbehaviour sets");
		Map<String, MisbehaviourSet> sets = new HashMap<>();

		for (Map<String, String> row : sparqlResults) {
			MisbehaviourSet ms = new MisbehaviourSet(
				row.get("ms"), row.get("m"), row.get("ml"), row.get("a"), row.get("al")
			);
			
			//check for misbehaviour set visibility (via core#isVisible on the misbehaviour in domain model)
			boolean visible = Boolean.parseBoolean(row.get("vis"));
			ms.setVisible(visible);

			//is MS a normal operation effect?
			boolean nop = Boolean.parseBoolean(row.get("nop"));
			ms.setNormalOpEffect(nop);
			
			if (row.containsKey("md") && row.get("md")!=null) {
				ms.setDescription(row.get("md"));
			}

			if (includeCauseAndEffects) {
				if (directCauses.containsKey(ms.getUri())) {
					ms.getDirectCauses().addAll(directCauses.get(ms.getUri()));
				}

				if (indirectCauses.containsKey(ms.getUri())) {
					ms.getIndirectCauses().addAll(indirectCauses.get(ms.getUri()));
				}

				if (directEffects.containsKey(ms.getUri())) {
					ms.getDirectEffects().addAll(directEffects.get(ms.getUri()));
				}

				if (rootCauses.containsKey(ms.getUri())) {
					ms.getRootCauses().addAll(rootCauses.get(ms.getUri()));
				}

				if (row.containsKey("rc") && row.get("rc")!=null) {
					//logger.debug("Adding root cause: {}", row.get("rc"));
					ms.getRootCauses().add(row.get("rc"));
				}
			}
			
			if (row.containsKey("imp") && row.get("imp")!=null) {
				if (levels.containsKey(row.get("imp")) && levels.get(row.get("imp"))!=null) {
					ms.setImpactLevel(levels.get(row.get("imp")));

					//If core:hasImpactLevel is set in system graph then user has asserted this
					ms.setImpactLevelAsserted(row.containsKey("impA"));
				} else {
					logger.warn("Could not find impact level <{}> for misbehaviour set <{}> in model", row.get("imp"), ms);
				}
			}

			if (row.containsKey("pl") && row.get("pl")!=null) {
				if (levels.containsKey(row.get("pl")) && levels.get(row.get("pl"))!=null) {
					ms.setLikelihood(levels.get(row.get("pl")));
				} else {
					logger.warn("Could not find prior likelihood <{}> for misbehaviour set <{}> in model", row.get("pl"), ms);
				}
			}

			if (row.containsKey("rl") && row.get("rl")!=null) {
				if (levels.containsKey(row.get("rl")) && levels.get(row.get("rl"))!=null) {
					ms.setRiskLevel(levels.get(row.get("rl")));
				} else {
					logger.warn("Could not find risk level <{}> for misbehaviour set <{}> in model", row.get("rl"), ms);
				}
			}

			MisbehaviourSet existing = sets.get(ms.getUri());
			if (existing != null) {
				if (existing.equals(ms)) {
					logger.warn("Two MS rows returned from query with URI " + ms.getUri() + ", but with identical details so no discrepancy.");
				} else {
					throw new RuntimeException("Duplicate MisbehaviourSet with URI " + ms.getUri() + " found.");
				}
			}
			sets.put(ms.getUri(), ms);
		}
		
		//logger.debug("Created misbehaviour sets");

		return sets;
	}

	// Compliance Sets ////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Gets a map of all of the compliance sets in the store filled in with their respective threat lists
	 *
	 * @param store the store to query
	 * @return a map of all of the compliance sets
	 */
	public Map<String, ComplianceSet> getComplianceSets(AStoreWrapper store) {

		logger.info("Getting compliance sets");
		return createComplianceSets(store.translateSelectResult(store.querySelect(createComplianceSetSparql(null))), getComplianceThreats(store));
	}

	/**
	 * Gets a map of all of the compliance sets in the store filled in with their respective threat lists
	 *
	 * @param store the store to query
	 * @param threats a map of all threats in the system
	 * @return a map of all of the compliance sets
	 */
	public Map<String, ComplianceSet> getComplianceSets(AStoreWrapper store, Map<String, ComplianceThreat> threats) {

		logger.info("Getting compliance sets");
		return createComplianceSets(store.translateSelectResult(store.querySelect(createComplianceSetSparql(null))), threats);
	}

	/**
	 * Gets a specific complianceSet from the store with the given uri
	 *
	 * @param store the store to query
	 * @param complianceSetURI the uri of the specific complianceSet that is needed
	 * @return the complianceSet object that is requested
	 */
	public ComplianceSet getComplianceSet(AStoreWrapper store, String complianceSetURI) {

		logger.info("Getting compliance set with uri {}", complianceSetURI);
		Map<String, ComplianceSet> result = createComplianceSets(store.translateSelectResult(store.querySelect(createComplianceSetSparql(complianceSetURI))), getComplianceThreats(store));
		return result.get(complianceSetURI);
	}

	/**
	 * Gets a specific complianceSet from the store with the given uri
	 *
	 * @param store the store to query
	 * @param complianceSetURI the uri of the specific complianceSet that is needed
	 * @param threats a map of all threats in the system
	 * @return the complianceSet object that is requested
	 */
	public ComplianceSet getComplianceSet(AStoreWrapper store, String complianceSetURI, Map<String, ComplianceThreat> threats) {

		logger.info("Getting compliance set with uri {}", complianceSetURI);
		Map<String, ComplianceSet> result = createComplianceSets(store.translateSelectResult(store.querySelect(createComplianceSetSparql(complianceSetURI))), threats);
		return result.get(complianceSetURI);
	}

	/**
	 * Creates the SPARQL query to retrieve compliance sets from the store
	 *
	 * @param complianceSet the compliance set's URI to filter by or null for all compliance sets
	 * @return SPARQL to retrieve a map of compliance sets that match the filtering criteria
	 */
	private String createComplianceSetSparql(String complianceSet) {

		return "SELECT ?compUri ?compLabel ?compDesc ?threat WHERE{\n" +
		"	GRAPH <" + model.getGraph("domain") + ">{\n" +
		(complianceSet != null ? "		BIND (<" + SparqlHelper.escapeURI(complianceSet) + "> as ?compUri) .\n" : "") +
		"		?compUri rdf:type core:ComplianceSet .\n" +
		"		OPTIONAL{\n" +
		"			?compUri rdfs:label ?compLabel .\n" +
		"		}\n" +
		"		OPTIONAL{\n" +
		"			?compUri rdfs:comment ?compDesc .\n" +
		"		}\n" +
		"		OPTIONAL{\n" +
		"			?compUri core:requiresTreatmentOf ?threat .\n" +
		"		}\n" +
		"	}\n" +
		"}\n";
	}

	/**
	 * Build ComplianceSets from the SPARQL results
	 *
	 * @param sparqlResults the SPARQL result "table"
	 * @return a map of all ComplianceSets
	 */
	private Map<String, ComplianceSet> createComplianceSets(List<Map<String, String>> sparqlResults, Map<String, ComplianceThreat> threats) {

		Map<String, ComplianceSet> complianceSets = new HashMap<>();

		for (Map<String, String> row : sparqlResults) {

			String uri = row.get("compUri");

			if (!complianceSets.containsKey(uri)) {
				ComplianceSet comp = new ComplianceSet();
				comp.setUri(uri);
				complianceSets.put(comp.getUri(), comp);
			}
			if (row.containsKey("compLabel")) {
				complianceSets.get(uri).setLabel(row.get("compLabel"));
			}
			if (row.containsKey("compDesc")) {
				complianceSets.get(uri).setDescription(row.get("compDesc"));
			}
			if (row.containsKey("threat")) {
				for (ComplianceThreat t: threats.values()) {
					if (t.getType().equals(row.get("threat"))) {
						if (complianceSets.get(uri).getThreats().put(t.getUri(), t) != null) {
							throw new RuntimeException("Duplicate Threat with URI " + t.getUri() + 
									 " added to ComplianceSet with URI " + uri);
						}
					}
				}
			}
		}

		return complianceSets;
	}

	// TWAS ///////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Get all system-specific twas
	 *
	 * @param store the store to query
	 * @return the twas
	 */
	public Map<String, TrustworthinessAttributeSet> getTrustworthinessAttributeSets(AStoreWrapper store) {

		logger.info("Getting system trustworthiness attribute sets");
				
		Map<String, TrustworthinessAttributeSet> sets = createTrustworthinessAttributeSets(store.translateSelectResult(store.querySelect(
				createTrustworthinessAttributeSetSparql(null, null, null, null),
				model.getGraph("core"),
				model.getGraph("domain"),
				model.getGraph("system"),
				model.getGraph("system-inf")
			)));
		
		return sets;
	}

	/**
	 * Get system-specific twas
	 *
	 * @param store the store to query
	 * @param threatURI the URI of the entry point or null for no filtering
	 * @return a map of twas
	 */
	public Map<String, TrustworthinessAttributeSet> getTrustworthinessAttributeSets(AStoreWrapper store, String threatURI) {

		logger.info("Getting system trustworthiness attribute sets for threat {}", threatURI);

		return createTrustworthinessAttributeSets(store.translateSelectResult(store.querySelect(
			createTrustworthinessAttributeSetSparql(null, threatURI, null, null),
			model.getGraph("core"),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf")
		)));
	}

	/**
	 * Get system-specific twas
	 *
	 * @param store the store to query
	 * @param assetURI the URI of the asset of the TWAS or null for no filtering
	 * @return a map of twas
	 */
	public Map<String, TrustworthinessAttributeSet> getTrustworthinessAttributeSetsForAssetURI(AStoreWrapper store, String assetURI) {

		logger.info("Getting system trustworthiness attribute sets for asset {}", assetURI);

		return createTrustworthinessAttributeSets(store.translateSelectResult(store.querySelect(
			createTrustworthinessAttributeSetSparql(null, null, assetURI, null),
			model.getGraph("core"),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf")
		)));
	}

	/**
	 * Get system-specific twas
	 *
	 * @param store the store to query
	 * @param assetID the ID of the asset of the TWAS or null for no filtering
	 * @return a map of twas
	 */
	public Map<String, TrustworthinessAttributeSet> getTrustworthinessAttributeSetsForAssetID(AStoreWrapper store, String assetID) {

		logger.info("Getting system trustworthiness attribute sets for asset with ID {}", assetID);

		return createTrustworthinessAttributeSets(store.translateSelectResult(store.querySelect(
			createTrustworthinessAttributeSetSparql(null, null, null, assetID),
			model.getGraph("core"),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf")
		)));
	}

	/**
	 * Get a system-specific twas
	 *
	 * @param store the store to query
	 * @param twasURI the URI of the twas
	 * @return the twas or null if it doesn't exist
	 */
	public TrustworthinessAttributeSet getTrustworthinessAttributeSet(AStoreWrapper store, String twasURI) {

		logger.info("Getting system trustworthiness attribute set {}", twasURI);

		Map<String, TrustworthinessAttributeSet> result = createTrustworthinessAttributeSets(store.translateSelectResult(store.querySelect(
			createTrustworthinessAttributeSetSparql(twasURI, null, null, null),
			model.getGraph("core"),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf")
		)));
		return result.get(twasURI);
	}

	/**
	 * Creates the SPARQL query to retrieve twas using various filtering options
	 *
	 * @param twasURI the twasURI URI to filter by or null for all twas
	 * @param threatURI the URI of the threat to filter by or null for all twas
	 * @param assetURI the URI of the asset for which to get TWAS or null for all twas
	 * @param assetID the ID of the asset for which to get TWAS or null for all twas
	 * @return SPARQL to retrieve a map of twas that match the filtering criteria
	 */
	private String createTrustworthinessAttributeSetSparql(String twasURI, String threatURI, String assetURI, String assetID) {

		return "SELECT * WHERE {\n" +
		(twasURI != null ? "	BIND (<" + SparqlHelper.escapeURI(twasURI) + "> as ?twas) .\n" : "") +
		(assetURI != null ? "	BIND (<" + SparqlHelper.escapeURI(assetURI) + "> as ?a) .\n" : "") +
		(assetID != null ? ("	?a a ?asset .\n" +
		"	GRAPH <" + model.getGraph("domain") + "> { ?asset rdfs:subClassOf* core:Asset }\n" +
		"	?a core:hasID \"" + SparqlHelper.escapeLiteral(assetID) + "\" .\n"): "") +
		"	GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"		?twas a core:TrustworthinessAttributeSet .\n" +
		(threatURI != null ? "		<" + SparqlHelper.escapeURI(threatURI) + "> core:hasEntryPoint ?twas .\n" : "") +
		"		?twas core:locatedAt ?a .\n" +
		"		?twas core:hasTrustworthinessAttribute ?twa .\n" +
		"		OPTIONAL {\n" +
		"			?twis core:affects ?twas .\n" +
		"			?twis core:affectedBy ?ms .\n" +
		"			?twis a core:TrustworthinessImpactSet .\n" +
		"		}\n" +
		"		OPTIONAL {\n" +
		"			?twas rdfs:label ?twasl ." +
		"		}" +
		"	}\n" +
		"	GRAPH <" + model.getGraph("domain") + "> {\n" +
		"		?twa rdfs:label ?twal .\n" +
		"		OPTIONAL { ?twa rdfs:comment ?twac }\n" +
		"		OPTIONAL { ?twa core:isVisible ?isVisible } \n" +
		"	}\n" +
		"	BIND(IF(BOUND(?isVisible),STR(?isVisible),\"true\") AS ?vis)\n" +
		"	OPTIONAL {\n" +
		"		GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"			?twas core:hasInferredLevel ?il .\n" +
		"		}\n" +
		"		?il a core:TrustworthinessLevel .\n" +
		"		?il rdfs:label ?ill .\n" +
		"		OPTIONAL {\n" +
		"			?il core:levelValue ?ilv .\n" +
		"			BIND(STR(?ilv) AS ?ilval)\n" +
		"		}\n" +
		"	}\n" +
		//"asserted" level can be in inf (default value) or system (actually asserted by the user)
		//this query gets them both but gives precedence to the user-asserted value
		"	OPTIONAL {\n" +
		"		GRAPH <" + model.getGraph("system") + "> {\n" +
		"			?twas core:hasAssertedLevel ?al1 .\n" +
		"		}\n" +
		"		?al1 a core:TrustworthinessLevel .\n" +
		"		?al1 rdfs:label ?all1 .\n" +
		"		OPTIONAL {\n" +
		"			?al1 core:levelValue ?alv1 .\n" +
		"			BIND(STR(?alv1) AS ?alval1)\n" +
		"		}\n" +
		"	}\n" +
		"	OPTIONAL {\n" +
		"		GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"			?twas core:hasAssertedLevel ?al2 .\n" +
		"		}\n" +
		"		?al2 a core:TrustworthinessLevel .\n" +
		"		?al2 rdfs:label ?all2 .\n" +
		"		OPTIONAL {\n" +
		"			?al2 core:levelValue ?alv2 .\n" +
		"			BIND(STR(?alv2) AS ?alval2)\n" +
		"		}\n" +
		"	}\n" +
		"	BIND(IF(BOUND(?al1),?al1,?al2) AS ?al)\n" +
		"	BIND(IF(BOUND(?all1),?all1,?all2) AS ?all)\n" +
		"	BIND(IF(BOUND(?alv1),?alv1,?alv2) AS ?alv)\n" +
		"	BIND(IF(BOUND(?alval1),?alval1,?alval2) AS ?alval)\n" +
		"}";
	}

	/**
	 * Build twas from the SPARQL results
	 *
	 * @param sparqlResults the SPARQL result "table"
	 * @return a map of all twas
	 */
	private Map<String, TrustworthinessAttributeSet> createTrustworthinessAttributeSets(List<Map<String, String>> sparqlResults) {

		Map<String, TrustworthinessAttributeSet> sets = new HashMap<>();

		for (Map<String, String> row : sparqlResults) {

			TrustworthinessAttributeSet twas = new TrustworthinessAttributeSet();
			twas.setUri(row.get("twas"));
			twas.setAsset(row.get("a"));
			twas.setLabel(row.get("twasl"));

			if (row.containsKey("twa") && row.get("twa")!=null) {
				SemanticEntity twa = new SemanticEntity();
				twa.setUri(row.get("twa"));
				twas.setAttribute(twa);
				if (row.containsKey("twal") && row.get("twal")!=null) {
					twa.setLabel(row.get("twal"));
				}
				if (row.containsKey("twac") && row.get("twac")!=null) {
					twa.setDescription(row.get("twac"));
				}
			}
			
			//check for TWAS visibility (via core#isVisible on the TWA in domain model)
			boolean visible = Boolean.valueOf(row.get("vis"));
			//logger.debug("<{}> visible = {}", twas.getUri(), visible);
			twas.setVisible(visible);

			if (row.containsKey("ms") && row.get("ms")!=null) {
				twas.setCausingMisbehaviourSet(row.get("ms"));
			}

			if (row.containsKey("al") && row.get("al")!=null) {
				Level l = new Level();
				l.setUri(row.get("al"));
				if (row.containsKey("all") && row.get("all")!=null) {
					l.setLabel(row.get("all"));
				}
				if (row.containsKey("alval") && row.get("alval")!=null) {
					l.setValue(Integer.valueOf(row.get("alval")));
				}
				twas.setAssertedTWLevel(l);

				//If core:hasAssertedLevel is set in system graph then user has asserted this
				twas.setTwLevelAsserted(row.containsKey("al1"));
			}

			if (row.containsKey("il") && row.get("il")!=null) {
				Level l = new Level();
				l.setUri(row.get("il"));
				if (row.containsKey("ill") && row.get("ill")!=null) {
					l.setLabel(row.get("ill"));
				}
				if (row.containsKey("ilval") && row.get("ilval")!=null) {
					l.setValue(Integer.valueOf(row.get("ilval")));
				}
				twas.setInferredTWLevel(l);
			}

			if (sets.put(twas.getUri(), twas) != null) {
				throw new RuntimeException("Duplicate TrustworthinessAttributeSet with URI " + twas.getUri() + " found.");
			}
		}

		return sets;
	}

	// Other //////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Get all possible steps that can be used to create a secondary effect chain.
	 * Each step contains an input misbehaviour ms and a (secondary) threat triggered by it.
	 *
	 * @param store the store to query
	 * @param secondaryEffectsOnly whether to only get actual secondary indirectEffects
	 * @return a map of steps that can be used to build a secondary effect chain, model or other data structure
	 */
	//TODO: this may now be redundant
	public Set<SecondaryEffectStep> getSecondaryEffectSteps(AStoreWrapper store, boolean secondaryEffectsOnly) {

		Map<String, MisbehaviourSet> misbehaviours = getMisbehaviourSets(store, true);

		String sparql = "SELECT * WHERE {\n" +
		"	GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"		?t a core:Threat .\n" +
		"		?t core:parent ?genT .\n" +
		"	}\n" +
		(!secondaryEffectsOnly ? "	OPTIONAL {\n" : "") +
		"		GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"			?t core:hasSecondaryEffectCondition ?inms .\n" +
		"		}\n" +
		(!secondaryEffectsOnly ? "	}\n" : "") +
		"	OPTIONAL {\n" +
		"		GRAPH <" + model.getGraph("system-inf") + "> {\n" +
		"			?t core:causesMisbehaviour ?outms .\n" +
		"		}\n" +
		"	}\n" +
		"}";

		//get all secondary indirectEffects
		Map<String, SecondaryEffectStep> steps = new HashMap<>();
		for (Map<String, String> row : store.translateSelectResult(store.querySelect(sparql,
				model.getGraph("domain"),
				model.getGraph("system"),
				model.getGraph("system-inf")))) {

			String t = row.get("t");

			//add it if it's a new one
			if (!steps.containsKey(t)) {
				SecondaryEffectStep se = new SecondaryEffectStep(t);
				steps.put(se.getUri(), se);
			}

			//cause
			if (row.containsKey("inms") && row.get("inms")!=null) {
				steps.get(t).addCause(misbehaviours.get(row.get("inms")));
			}
			//effect
			if (row.containsKey("outms") && row.get("outms")!=null) {
				steps.get(t).addEffect(misbehaviours.get(row.get("outms")));
			}
		}

		return new HashSet<>(steps.values());
	}

	public boolean isValidating(AStoreWrapper store) {
		return checkModelFlag(store, "core:isValidating");
	}

	public boolean isValid(AStoreWrapper store) {
		return checkModelFlag(store, "core:isValid");
	}

	public boolean isCalculatingRisk(AStoreWrapper store) {
		return checkModelFlag(store, "core:isCalculatingRisk");
	}

	public boolean isCalculatingControls(AStoreWrapper store) {
		return checkModelFlag(store, "core:isCalculatingControls");
	}

	private boolean checkModelFlag(AStoreWrapper store, String flag) {
		String modelGraph = model.getGraph("system");
		return store.queryAsk("ASK WHERE {\n" +
				"	GRAPH <" + modelGraph + "> {\n" +
				"		<" + modelGraph + "> " + flag + " \"true\"^^xsd:boolean\n" +
				"	}\n" +
				"}", modelGraph);
	}

	public String getCreateDate(AStoreWrapper store) {
		String sparql = "SELECT ?createDate WHERE {\n"
				+ "<" + model.getGraph("system") + "> <http://purl.org/dc/terms/created> ?createDate .\n"
                                + "}";

		List<Map<String, String>> rows = store.translateSelectResult(store.querySelect(sparql,
					model.getGraph("system-inf")));

		if (rows.size() > 1) {
			throw new RuntimeException("Duplicate create date found.");
		}

		for (Map<String, String> row : rows) {
			if (row.containsKey("createDate")) {
				return row.get("createDate");
			}
		}

		return null;
	}

	/**
	 * Returns a list of all metadata pairs associated with the given entity.
	 *
	 * @param store The store to query
	 * @param entity The entity
	 * @return List of all metadata pairs
	 */
	public List<MetadataPair> getMetadataOnEntity(AStoreWrapper store, SemanticEntity entity) {
		String query = String.format(
				"SELECT ?metaUri ?key ?val WHERE {\n" +
				"  GRAPH <%s> {\n" +
				"    <%s> core:hasMetadata ?metaUri .\n" +
				"    ?metaUri core:hasKey ?key.\n" +
				"    ?metaUri core:hasValue ?val.\n" +
				"  }}", model.getGraph("system-meta"), entity.getUri());

		List<MetadataPair> metadataPairs = new ArrayList<>();
		for (Map<String, String> result : store.translateSelectResult(store.querySelect(query))) {
			MetadataPair pair = new MetadataPair(result.get("key"), result.get("val"));
			pair.setUri(result.get("metaUri"));
			metadataPairs.add(pair);
		}

		return metadataPairs;
	}

	/**
	 * Takes a list of metadata (key, value) pairs and uses it to query the store for entities with matching metadata.
	 * A single key can be present in multiple pairs, but these pairs should have unique values.
	 * Duplicate (key, value) pairs will be ignored.
	 * Each entity returned must have AT LEAST ONE of the specified values for EACH given key.
	 *
	 * @param store The store to query
	 * @param queryMetadata A list of (key, value) pairs used to query the system assets
	 * @return A Set of URIs
	 */
	public Set<String> getEntitiesByMetadata(AStoreWrapper store, List<MetadataPair> queryMetadata) {
		assert queryMetadata != null    : "queryMetadata cannot be null";
		assert !queryMetadata.isEmpty() : "queryMetadata cannot be empty";

		// Construct map from queryMetadata: each unique key is associated with a set of all its values
		Map<String, Set<String>> metadataValuesByKey = new HashMap<>();
		for (MetadataPair metadataPair : queryMetadata) {
			Set<String> values = metadataValuesByKey.get(metadataPair.getKey());
			if (values == null) {
				values = new HashSet<>();
				metadataValuesByKey.put(metadataPair.getKey(), values);
			}
			values.add(metadataPair.getValue());
		}

		// Construct the SPARQL query
		StringBuilder sb = new StringBuilder("SELECT ?entity WHERE { GRAPH <" +  model.getGraph("system-meta") + "> {\n");
		int i = 1;
		for (Map.Entry<String, Set<String>> entry : metadataValuesByKey.entrySet()) {
			sb.append("?entity core:hasMetadata ?metaUri").append(i).append(" .\n");
			sb.append("?metaUri").append(i).append(" core:hasKey '").append(entry.getKey()).append("' .\n");
			sb.append("?metaUri").append(i).append(" core:hasValue ").append("?value").append(i).append(" .\n");
			sb.append("VALUES ?value").append(i).append("{ ");
			for (String value : entry.getValue()) {
				sb.append("'").append(value).append("' ");
			}
			sb.append("}\n");

			i++;
		}
		sb.append("}}");
		String query = sb.toString();

		Set<String> entities = new HashSet<>();
		for (Map<String, String> result : store.translateSelectResult(store.querySelect(query))) {
			entities.add(result.get("entity"));
		}

		return entities;
	}

	public Map<String, AssetGroup> getAssetGroups(AStoreWrapper store) {
		return getAssetGroups(store, getSystemAssets(store));
	}

	public Map<String, AssetGroup> getAssetGroups(AStoreWrapper store, Map<String, Asset> assets) {
		return getAssetGroups(store, null, assets);
	}

	public AssetGroup getAssetGroupById(AStoreWrapper store, String id, Map<String, Asset> assets) {
		Map<String, AssetGroup> assetGroups = getAssetGroups(store, id, assets); // Should be single asset group
		if (assetGroups.isEmpty()) {
			logger.info("No asset group with ID {} found", id);
			return null;
		}
		else if (assetGroups.size() > 1) {
			throw new RuntimeException("Located duplicate asset groups with ID: " + id);
		}
		//KEM - why call getAssetGroups again?
		//return getAssetGroups(store, id, assets).values().iterator().next();
		return assetGroups.values().iterator().next();
	}

	public AssetGroup getAssetGroupById(AStoreWrapper store, String id) {
		return getAssetGroupById(store, id, getSystemAssets(store));
	}

	public AssetGroup getAssetGroupOfAsset(AStoreWrapper store, Asset asset) {
		String query = String.format(
				"SELECT * WHERE {\n" +
						"  GRAPH <%s> {" +
						"    ?assetGroup a core:AssetGroup .\n" +
						"    ?assetGroup core:hasAsset <%s> .\n" +
						"    ?assetGroup core:hasID ?id .\n" +
						"  }" +
						"}", model.getGraph("system-ui"), asset.getUri());
		List<Map<String, String>> results = store.translateSelectResult(store.querySelect(query));
		if (results.size() > 0) {
			String assetGroupID = results.iterator().next().get("id");
			return getAssetGroupById(store, assetGroupID);
		} else {
			return null;
		}
	}

	private Map<String, AssetGroup> getAssetGroups(AStoreWrapper store, String id, Map<String, Asset> assets) {
		String query = String.format("SELECT * WHERE {\n" +
				"  GRAPH <%s> {\n" +
				"    ?assetGroup a core:AssetGroup .\n" +
				"    ?assetGroup core:positionX ?x .\n" +
				"    ?assetGroup core:positionY ?y .\n" +
				"    OPTIONAL { ?assetGroup core:width ?w } \n" +
				"    OPTIONAL { ?assetGroup core:height ?h } \n" +
				"    OPTIONAL { ?assetGroup core:isExpanded ?expanded } \n" +
				(id != null ? "?assetGroup core:hasID \"" + id + "\" .\n" : "") +
				"    OPTIONAL { ?assetGroup rdfs:label ?label } \n" +
				"  }\n" +
				"}", model.getGraph("system-ui"));
		List<Map<String, String>> results = store.translateSelectResult(store.querySelect(query));

		Map<String, AssetGroup> assetGroups = new HashMap<>();
		for (Map<String, String> result : results) {
			AssetGroup assetGroup = new AssetGroup();
			assetGroup.setUri(result.get("assetGroup"));
			assetGroup.setLabel(result.get("label"));
			assetGroup.setX(Integer.parseInt(result.get("x").replace("^^http://www.w3.org/2001/XMLSchema#integer", "")));
			assetGroup.setY(Integer.parseInt(result.get("y").replace("^^http://www.w3.org/2001/XMLSchema#integer", "")));
			
			String widthStr = result.get("w");
			if (widthStr != null) assetGroup.setWidth(Integer.parseInt(widthStr.replace("^^http://www.w3.org/2001/XMLSchema#integer", "")));
			String heightStr = result.get("h");
			if (heightStr != null) assetGroup.setHeight(Integer.parseInt(heightStr.replace("^^http://www.w3.org/2001/XMLSchema#integer", "")));
			String expandedStr = result.get("expanded");
			if (expandedStr != null) assetGroup.setExpanded(Boolean.parseBoolean(expandedStr.replace("^^http://www.w3.org/2001/XMLSchema#boolean", "")));
			
			assetGroups.put(assetGroup.getUri(), assetGroup);
		}

		query = String.format("SELECT * WHERE {\n" +
				"  GRAPH <%s> {\n" +
				"    ?assetGroup core:hasAsset ?asset .\n" +
				"  }" +
				"}", model.getGraph("system-ui"));
		results = store.translateSelectResult(store.querySelect(query));
		for (Map<String, String> result : results) {
			AssetGroup assetGroup = assetGroups.get(result.get("assetGroup"));
			Asset asset = assets.get(result.get("asset"));
			if (assetGroup != null && asset != null) {
				assetGroup.getAssets().put(asset.getUri(), asset);
			}
			else if (asset == null) {
				logger.warn("Cannot locate grouped asset: {}", result.get("asset"));
			}
		}

		return assetGroups;
	}
}
