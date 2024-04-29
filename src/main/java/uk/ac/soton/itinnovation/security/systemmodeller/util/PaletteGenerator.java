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
//      Created Date :          12 Sep 2016
//      Modified By :           Stefanie Wiegand
//      Created for Project :   5G-Ensure
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.systemmodeller.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.json.JsonBuilder;
import org.apache.jena.atlas.json.JsonValue;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;

/**
 * This class can generate a palette using a domain model ontology as its input
 *
 * @author Stefanie Cox
 */
public class PaletteGenerator {

	private static final String FALLBACK_ICON = "fallback.svg";

	public static Logger logger = LoggerFactory.getLogger(PaletteGenerator.class);


	private final String domainModelGraph;
	private JSONObject icons;
	private boolean defaultUserAccess;
	private boolean paletteCreated = false;

	private final JsonBuilder palettebuilder = new JsonBuilder();

	private final HashMap<String, HashMap<String, ArrayList<String>>> incoming = new HashMap<>();
	private final HashMap<String, HashMap<String, ArrayList<String>>> outgoing = new HashMap<>();

	private ModelObjectsHelper modelObjectsHelper;

	private String domainModelFolder;

	public PaletteGenerator(String domainModelFolder, String domainModelGraph, ModelObjectsHelper modelObjectsHelper, InputStream iconMap) {
		this.modelObjectsHelper = modelObjectsHelper;
		this.domainModelGraph = domainModelGraph;
		this.domainModelFolder = domainModelFolder;

		//Ensures the InputStream is closed when this method returns
		try (InputStream input = iconMap) {
			String jsonStr = IOUtils.toString(input);
			JSONArray json;

			if (jsonStr.startsWith("[")) {
				json = new JSONArray(jsonStr);
			}
			else {
				json = new JSONArray();
				JSONObject jsonObj = new JSONObject(jsonStr);
				json.put(jsonObj);
			}

			//find this ontology in the icon mapping file
			for (int i=0; i<json.length(); i++) {

				//parse the JSON file to get the ontology label
				JSONObject ont = (JSONObject) json.get(i);
				String graph = ont.getString("graph");

				//get icons
				if (graph.equals(domainModelGraph)) {
					logger.debug("Getting icons for {}", graph);
					this.icons = (JSONObject) ont.get("icons");
				}

				boolean defaultUserAccess = ont.getBoolean("defaultUserAccess");
				this.defaultUserAccess = defaultUserAccess;
				logger.debug("Setting defaultUserAccess = {}", this.defaultUserAccess);
			}

			if (this.icons == null) {
				logger.warn("Could not find domain model definition in icon mapping for: {}", domainModelGraph);
			}

		} catch (IOException ex) {
			logger.warn("Could not load icon definitions from icon mapping file", ex);
		}
		finally {
			if (this.icons == null) {
				this.icons = new JSONObject();
			}
		}
	}

	/**
	 * Retrieve assets from the domain model
	 */
	private void getAssets() {
        String[] parts = domainModelGraph.split("/");
        String domainModelName = parts[parts.length - 1];
		logger.debug("Getting assets for domain: {}", domainModelName);

		Path domainIconsFolderPath = Paths.get(this.domainModelFolder, "icons");

		List<Map<String, String>> assets = modelObjectsHelper.getPaletteAssets(domainModelGraph);

		palettebuilder.key("assets");
		palettebuilder.startArray();

		logger.info("Located {} assets", assets.size());

		assets.forEach((asset) -> {
		if (asset != null){
			palettebuilder.startObject();
			palettebuilder.key("id").value(asset.get("asset"));
			palettebuilder.key("label").value(asset.get("al"));
			palettebuilder.key("category").value(asset.get("cl")); //category label
			palettebuilder.key("assertable").value(Boolean.valueOf(asset.get("a")));

			String type = asset.get("type");
			String[] types;
			List<String> typeIcons = new ArrayList<>();
			//multiple types
			if (type.contains(",")) {
				types = type.split(",");
				//for some reason the ordering is the wrong way round. need more specific types first
				for (int i = types.length-1; i>=0; i--) {
					typeIcons.add(types[i]);
				}
			//one type only
			} else {
				types = new String[1];
				types[0] = type;
				typeIcons.add(type);
			}

			//record types
			palettebuilder.key("type");
			palettebuilder.startArray();
			for (String t : types) {
				palettebuilder.value(t);
			}
			palettebuilder.finishArray();

			//logger.debug("Icon {} found for asset {}", icons.getString(asset.get("asset").toString()), asset.get("al"));
			String icon;
			try{
			icon = icons.getString(asset.get("asset"));
			} catch (JSONException e) {
			icon = FALLBACK_ICON;
			}
			asset.put("icon", icon!=null?icon:FALLBACK_ICON);

			//pick the best icon available
			String actualIcon = asset.get("icon");
			for (String t: typeIcons) {

				//map back to asset
				String potentialIcon = null;
				if (asset.containsKey("icon") && asset.get("icon")!=null) {
					potentialIcon = asset.get("icon");
				}

				if (potentialIcon!=null && !potentialIcon.equals(FALLBACK_ICON)) {
					URL resource = PaletteGenerator.class.getClassLoader().getResource("static/images/" + potentialIcon);
					if (resource!=null && (new File(resource.getPath()).isFile())) {
						actualIcon = potentialIcon;
						break;
					}
				}
			}

			//logger.debug("Locating icon {} for asset type {}", actualIcon, asset.get("al"));

			//First, try to locate image file in domain specific folder
			URL resource = null;

			Path iconPath = domainIconsFolderPath.resolve(actualIcon);

			if (iconPath.toFile().isFile()) {
				palettebuilder.key("icon").value(domainModelName + "/" + actualIcon);
			}
			else {
				//If not found, try the FALLBACK_ICON in the domain specific folder
				iconPath = domainIconsFolderPath.resolve(FALLBACK_ICON);

				if ((iconPath.toFile().isFile())) {
					logger.warn("Could not find icon {} for asset {}: using fallback icon ({})",
						actualIcon, asset.get("al"), domainModelName + File.separator + FALLBACK_ICON);
					palettebuilder.key("icon").value(domainModelName + "/" + FALLBACK_ICON);
				}
				else {
					//If not found, try the FALLBACK_ICON in the main images folder
					resource = PaletteGenerator.class.getClassLoader().getResource("static/images/" + FALLBACK_ICON);

					if (resource != null && (new File(resource.getPath()).isFile())) {
						logger.warn("Could not find icon {} for asset {}: using fallback icon ({})",
							actualIcon, asset.get("al"), "static/images/" + FALLBACK_ICON);
						palettebuilder.key("icon").value(FALLBACK_ICON);
					}
					else {
						logger.warn("Could not find icon {} or fallback icon ({}), skipping icon field for asset {}",
							actualIcon, FALLBACK_ICON, asset.get("al"));
					}
				}
			}

			if (asset.containsKey("description")) {
				palettebuilder.key("description").value(asset.get("description"));
			}
			if(asset.containsKey("min")) {
				String cardinality = asset.get("min");
				palettebuilder.key("minCardinality").value(Integer.parseInt(cardinality.substring(0, cardinality.indexOf("^"))));
			} else {
				palettebuilder.key("minCardinality").value(-1);
			}
			if(asset.containsKey("max")) {
				String cardinality = asset.get("max");
				palettebuilder.key("maxCardinality").value(Integer.parseInt(cardinality.substring(0, cardinality.indexOf("^"))));
			} else {
				palettebuilder.key("maxCardinality").value(-1);
			}
			palettebuilder.finishObject();
		}
		});
		palettebuilder.finishArray();

	}


	/**
	 * Retrieve relations from domain model
	 */
	private void getLinks() {
		List<Map<String, String>> relations = modelObjectsHelper.getPaletteRelations(domainModelGraph);

		Map<String, String> typeLabels = new HashMap<>();
		Map<String, String> typeComments = new HashMap<>();
		Map<String, Boolean> typeIsHidden = new HashMap<>();

		relations.forEach(solution -> {
			String prop = solution.get("prop");
			String label = solution.get("label");
			String comment = solution.get("comment");
			Boolean isHidden = Boolean.valueOf(solution.get("isHidden"));
			String from = solution.get("from");
			String to = solution.get("to");

			if (isHidden) {
				//logger.debug("skipping {} {} {}", from, label, to);
				return;
			}

			typeLabels.put(prop, label);
			typeComments.put(prop, comment);
			typeIsHidden.put(prop, isHidden);

			HashMap<String,ArrayList<String>> linksTo =  incoming.getOrDefault(to, new HashMap<>());
			ArrayList<String> targets = linksTo.getOrDefault(prop, new ArrayList<>());
			targets.add(from);
			linksTo.put(prop, targets);
			incoming.put(to, linksTo);

			HashMap<String,ArrayList<String>> linksFrom =  outgoing.getOrDefault(from, new HashMap<>());
			ArrayList<String> sources = linksFrom.getOrDefault(prop, new ArrayList<>());
			sources.add(to);
			linksFrom.put(prop, sources);
			outgoing.put(from, linksFrom);
		});

		HashSet<String> assets = new HashSet<>(incoming.keySet());
		assets.addAll(outgoing.keySet());
		palettebuilder.key("links");

		palettebuilder.startObject();
		assets.stream().forEach(asset -> {

			palettebuilder.key(asset);
			palettebuilder.startObject();

			if (outgoing.get(asset) != null){
				palettebuilder.key("linksFrom");
				palettebuilder.startArray();
				outgoing.get(asset).forEach((property,targets)->{
					palettebuilder.startObject();

					palettebuilder.key("type").value(property)
						.key("label").value(typeLabels.get(property))
						.key("comment").value(typeComments.get(property))
						.key("inferred").value(false)
						.key("options");
					palettebuilder.startArray();
					targets.forEach(target->{
						palettebuilder.value(target);
					});
					palettebuilder.finishArray();
					palettebuilder.finishObject();
				});
				palettebuilder.finishArray();
			}

			if (incoming.get(asset) != null){
				palettebuilder.key("linksTo");
				palettebuilder.startArray();
				incoming.get(asset).forEach((property,targets) -> {
					palettebuilder.startObject();

					palettebuilder
						.key("type").value(property)
						.key("label").value(typeLabels.get(property))
						.key("comment").value(typeComments.get(property))
						.key("inferred").value(false)
						.key("options").startArray();
					targets.forEach(target->{
						palettebuilder.value(target);
					});
					palettebuilder.finishArray();
					palettebuilder.finishObject();
				});
				palettebuilder.finishArray();
			}

			palettebuilder.finishObject();
			//System.out.println(asset + "\n\tincoming:" + incoming.get(asset) + "\n\toutgoing:" + outgoing.get(asset));
		});
		palettebuilder.finishObject();
	}

	/**
	 * Build a palette
	 *
	 * @return the palette, JSON encoded
	 */
	private JsonValue build() {
		logger.info("build started");
		palettebuilder.startObject();
		logger.info("build getAssets");
		getAssets();
		logger.info("build getLinks");
		getLinks();
		palettebuilder.finishObject();
		return palettebuilder.build();
	}

	public boolean createPalette(String ontology) {
		logger.info("createPalette: calling build()");
		JsonValue j = build();
		String palette = j.toString();
		Path filePath = Paths.get(this.domainModelFolder, "palette.json");

		try (PrintWriter out = new PrintWriter(filePath.toFile())) {
			out.println(palette);
		} catch (FileNotFoundException ex) {
			logger.error("Could not create palette for ontology: " + ontology, ex);
			return false;
		}

		logger.info("Created palette for domain: {}", ontology);
		logger.info("Location: {}", filePath);

		this.paletteCreated = true;

		return this.paletteCreated;
	}

	public static PaletteGenerator createPalette(String domainModelFolder, String ontology, ModelObjectsHelper modelObjectsHelper, InputStream iconMap) {
		PaletteGenerator pg = new PaletteGenerator(domainModelFolder, ontology, modelObjectsHelper, iconMap);
		pg.createPalette(ontology);
		return pg;
	}

	//Determine domain/graph URI from icon map
	public static String getDomainUri(InputStream iconMap) throws IOException {
		JSONObject json = new JSONObject(IOUtils.toString(iconMap));
		String graph = json.getString("graph");
		logger.info("Domain graph: " + graph);
		return graph;
	}

	public boolean isDefaultUserAccess() {
		return defaultUserAccess;
	}

	public boolean isPaletteCreated() {
		return paletteCreated;
	}

}
