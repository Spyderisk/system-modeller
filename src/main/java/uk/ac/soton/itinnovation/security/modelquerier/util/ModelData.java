/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2019
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
//      Created By :            Lee Mason
//      Created Date :          05/08/2019
//      Created for Project :   RESTASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelquerier.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import uk.ac.soton.itinnovation.security.semanticstore.util.SparqlHelper;

public class ModelData {
	
	public final static String PREFIX = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/";
	public final static String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns";
	public final static String RDFS = "http://www.w3.org/2000/01/rdf-schema";
	public final static String SUBCLASS = "rdfs#subClassOf";
	public final static String SUBPROPERTY = "rdfs#subPropertyOf";
	public final static String LABEL = "rdfs#label";
	public final static String COMMENT = "rdfs#comment";
	
	private final String STRING_ENCODE = "^^xsd:string";
	private final String INT_ENCODE = "^^xsd:integer";
	
	private final String STRING_DECODE = "^^http://www.w3.org/2001/XMLSchema#string";
	private final String INT_DECODE = "^^http://www.w3.org/2001/XMLSchema#integer";
	private final String BOOL_DECODE = "^^http://www.w3.org/2001/XMLSchema#boolean";
	
	private final String TYPE_ENTRY = "rdf#type";
	
	private String uri;
	private String type;
	
	private Map<String, Set<String>> uriProperties = new HashMap<>();
	private Map<String, Set<String>> stringProperties = new HashMap<>();
	private Map<String, Set<String>> intProperties = new HashMap<>();
	private Map<String, Set<String>> boolProperties = new HashMap<>();
	
	// TODO: Change to more memory efficient tree structure
	// TODO: Remove from this class entirely?
	private Set<String> superClasses = new HashSet<>();
	
	public ModelData(String uri, String type) {
		this.uri = uri;
		this.type = type;
	}
	
	public ModelData(String uri) {
		this.uri = uri;
	}
	
	public ModelData() {
		
	}
	
	public Set<Integer> getIntProperties(String predicate) {
		Set<String> properties = intProperties.get(predicate);
		if (properties != null) {
			Set<Integer> propsParsed = new HashSet<>();
			for (String property : properties) {
				propsParsed.add(Integer.parseInt(property));
			}
			return propsParsed;
		} else {
			return new HashSet<Integer>();
		}
	}
	
	public Integer getIntProperty(String predicate) {
		Iterator<Integer> it = getIntProperties(predicate).iterator();
		return it.hasNext() ? it.next() : null;
	}
	
	public Boolean getBooleanProperty(String predicate) {
		Iterator<Boolean> it = getBooleanProperties(predicate).iterator();
		return it.hasNext() ? it.next() : null;
	}
	
	public Set<Boolean> getBooleanProperties(String predicate) {
		Set<String> properties = boolProperties.get(predicate);
		
		if (properties != null) {
			Set<Boolean> propsParsed = new HashSet<>();
			for (String property : properties) {
				propsParsed.add(Boolean.parseBoolean(property));
			}
			return propsParsed;
		} else {
			return new HashSet<Boolean>();
		}
	}
	
	public Map<String, Set<String>> getUriProperties() {
		return uriProperties;
	}
	
	public Set<String> getUriProperties(String predicate) {
		Set<String> properties = uriProperties.get(predicate);
		return properties != null ? properties : new HashSet<String>();
	}
	
	public String getUriProperty(String predicate) {
		Iterator<String> it = getUriProperties(predicate).iterator();
		return it.hasNext() ? it.next() : null;
	}
	
	public Set<String> getStringProperties(String predicate) {
		Set<String> properties = stringProperties.get(predicate);
		return properties != null ? properties : new HashSet<String>();
	}
	
	public String getStringProperty(String predicate) {
		Iterator<String> it = getStringProperties(predicate).iterator();
		return it.hasNext() ? it.next() : null;
	}
	
	public Set<String> getProperties(String predicate) {
		return getUriProperties(predicate);
	}
	
	public String getProperty(String predicate) {
		return getUriProperty(predicate);
	}
	
	public void addUriProperty(String predicate, String object) {
		addProperty(uriProperties, predicate, object);
	}
	
	public void addStringProperty(String predicate, String object) {
		addProperty(stringProperties, predicate, object);
	}
	
	public void addIntegerProperty(String predicate, Integer object) {
		addProperty(intProperties, predicate, object.toString());
	}
	
	public void addBooleanProperty(String predicate, Boolean object) {
		addProperty(boolProperties, predicate, object.toString());
	}
	
	public void loadProperty(String predicate, String object) {
		if (object.contains(INT_DECODE)) {
			// TODO: Exception handling
			addIntegerProperty(predicate, Integer.parseInt(object.replace(INT_DECODE, "")));
		} else if (object.contains(BOOL_DECODE)) {
			// TODO: Exception handling
			addBooleanProperty(predicate, Boolean.parseBoolean(object.replace(BOOL_DECODE, "")));
		} else if (predicate.equals(TYPE_ENTRY)) {
			this.type = object;
		} else if (predicate.equals(SUBCLASS)) {
			superClasses.add(object);
		} else if (object.contains("#")) { // TODO: Better way of determining this.
			addUriProperty(predicate, object);
		}   else {
			addStringProperty(predicate, object);
		}
	}
	
	public void updateData(ModelData modelData) {
		uriProperties.putAll(modelData.uriProperties);
		stringProperties.putAll(modelData.stringProperties);
		intProperties.putAll(modelData.intProperties);
		boolProperties.putAll(modelData.boolProperties);
	}
	
	public void setUri(String uri) {
		this.uri = uri;
	}
	
	public String getUri() {
		return uri;
	}
	
	public String getType() {
		return type;
	}
	
	public void getType(String type) {
		this.type = type;
	}
	
	public Set<String> getSuperClasses() {
		return superClasses;
	}
	
	public String toNQ() {
		String nqUri = uri(uri);
		
		// TODO: Tidy with lambda expressions.
		String nq = "";
		for (Map.Entry<String, Set<String>> entry : uriProperties.entrySet()) {
			for (String object : entry.getValue()) {
				nq += String.format("<%s> %s <%s> .\n", nqUri, entry.getKey().replace("#", ":"),
						uri(object));
			}
		}
		for (Map.Entry<String, Set<String>> entry : stringProperties.entrySet()) {
			for (String object : entry.getValue()) {
				nq += String.format("<%s> %s %s .\n", nqUri, entry.getKey().replace("#", ":"),
						encodeString(object));
			}
		}
		for (Map.Entry<String, Set<String>> entry : intProperties.entrySet()) {
			for (String object : entry.getValue()) {
				nq += String.format("<%s> %s %s .\n", nqUri, entry.getKey().replace("#", ":"), 
						encodeInteger(object));
			}
		}
		
		if (type != null) {
			nq += String.format("<%s> rdf:type %s .\n", nqUri, type.replace("#", ":"));
		}
		
		return nq;
	}
	
	private void addProperty(Map<String, Set<String>> map, String predicate, String object) {
		predicate = predicate.replace(":", "#");
		Set<String> properties = map.get(predicate);
		if (properties == null) {
			properties = new HashSet<>();
			map.put(predicate, properties);
		}
		properties.add(object);
	}
	
	private String encodeString(String string) {
		//return String.format("\"%s\"%s", SparqlHelper.escapeLiteral(string), STRING_ENCODE);
		return String.format("\"%s\"", SparqlHelper.escapeLiteral(string));
	}
	
	private String encodeInteger(String integer) {
		return String.format("\"%s\"%s", SparqlHelper.escapeLiteral(integer), INT_ENCODE);
	}
	
	public static String uri(String element) {
		return PREFIX + element;
	}
	
	public static String trim(String string) {
		return string.replace(PREFIX, "").replace(RDFS, "rdfs").replace(RDF, "rdf");
	}
}
