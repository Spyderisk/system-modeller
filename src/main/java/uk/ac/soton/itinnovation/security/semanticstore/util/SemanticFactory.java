/////////////////////////////////////////////////////////////////////////
//
// (c) University of Southampton IT Innovation Centre, 2014
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
//      Created By :          Stefanie Wiegand
//      Created Date :        2014
//      Created for Project:  OPTET
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.semanticstore.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.semanticstore.util.Triple.TripleType;

/**
 * The SemanticFactory is a singleton factory which helps create, covert and analyse triples with
 * the correct prefixes.
 */
public final class SemanticFactory {

	private static final Logger logger = LoggerFactory.getLogger(SemanticFactory.class);

	//the prefix of this baseURI if it is defined (normally "")
	private String prefix;
	//the base URI of this ontology
	private String baseURI;

	private Map<String, String> namespaces;

	/**
	 * Creates a new, empty semantic factory. Both prefix and URI will be "". This menthod was
	 * created for use in the JenaOntologyManager class and is not recommended to be used for other
	 * purposes. Use one of the overloaded methods instead.
	 */
	public SemanticFactory() {

		clear();
		init();
	}

	/**
	 * Creates a SemanticFactory using the base namespace and prefix from a model as well as
	 * prefixes defined in that model.
	 *
	 * @param model the input model
	 */
	public SemanticFactory(Model model) {

		this("", model.getNsPrefixMap().get(""));
		addMappingsFromModel(model);
	}

	/**
	 * Creates a SemanticFactory using a custom base namespace and prefix
	 *
	 * @param prefix the short prefix to use
	 * @param uri the base namespace to use
	 */
	public SemanticFactory(String prefix, String uri) {

		this();
		//set the base namespace/prefix
		if (prefix == null) {
			this.prefix = "";
		} else {
			this.prefix = prefix;
		}
		this.baseURI = uri;
	}

	/**
	 * Adds a collection of standard prefixes
	 */
	private void init() {

		//add primary ontology
		addPrefixURIMapping(prefix, baseURI);
		//add standard set on ontologies to use:
		addPrefixURIMapping("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		addPrefixURIMapping("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		addPrefixURIMapping("owl", "http://www.w3.org/2002/07/owl#");
		addPrefixURIMapping("xsd", "http://www.w3.org/2001/XMLSchema#");
		addPrefixURIMapping("sp", "http://spinrdf.org/sp#");
		addPrefixURIMapping("spin", "http://spinrdf.org/spin#");
		addPrefixURIMapping("spl", "http://spinrdf.org/spl#");
		addPrefixURIMapping("fn", "http://www.w3.org/2005/xpath-functions#");
		addPrefixURIMapping("dct", "http://purl.org/dc/terms/");
		addPrefixURIMapping("dc", "http://purl.org/dc/elements/1.1/");
		//addPrefixURIMapping("jr", "urn:x-hp-jena:rubrik/");
	}

	// Maintain mapping ///////////////////////////////////////////////////////////////////////////
	/**
	 * Adds an ontology to the internal list of namespaces. If the short prefix already exists, it
	 * will warn and overwrite its matching namespace with the provided namespace.
	 *
	 * @param prefix the short prefix
	 * @param uri the URI
	 */
	public void addPrefixURIMapping(String prefix, String uri) {
		if (uri != null && !uri.isEmpty()) {

			if ("".equals(prefix)) {
				//logger.debug("Setting baseURI to {}", uri);
				baseURI = uri;
			}

			if (!namespaces.containsKey(prefix)) {
				logger.debug("Added prefix {}: for URI {}", prefix, uri);
				namespaces.put(prefix, uri);
				//only write valid URIs
			} else {
				if (!namespaces.get(prefix).equals(uri)) {
					logger.debug("Prefix {}: changed. Old URI: {} New URI: {}", prefix, namespaces.get(prefix), uri);
					namespaces.put(prefix, uri);
				}
			}
		}
	}

	/**
	 * Removes an ontology from the internal list of namespaces if it exists.
	 *
	 * @param prefix the short prefix
	 */
	public void removePrefixURIMapping(String prefix) {
		if (namespaces.containsKey(prefix)) {
			namespaces.remove(prefix);
		}
	}

	/**
	 * Removes a mapping where only the URI is known
	 *
	 * @param uri the URI
	 */
	public void removePrefixURIMappingForURI(String uri) {

		if (namespaces.containsValue(uri)) {
			//get prefix
			String p = null;
			for (Map.Entry<String, String> mapping : namespaces.entrySet()) {
				if (mapping.getValue().equals(uri)) {
					p = mapping.getKey();
					break;
				}
			}
			if (prefix != null) {
				removePrefixURIMapping(p);
			}
		}
	}

	/**
	 * Adds a map of namespaces to the one already contained. Will overwrite if necessary.
	 *
	 * @param mappings the new mappings to add.
	 */
	public void addPrefixURIMap(Map<String, String> mappings) {
		mappings.entrySet().stream().forEach(mapping ->
			addPrefixURIMapping(mapping.getKey(), mapping.getValue())
		);
	}

	/**
	 * Adds all the mappings from the given model to the map of prefixes, overriding if applicable.
	 *
	 * @param model the model to load the prefixes from
	 */
	public void addMappingsFromModel(Model model) {

		if (model!=null && model.getNsPrefixMap()!=null) {
			//set baseURI if it exists
			if (model.getNsPrefixMap().containsKey("")) {
				baseURI = model.getNsPrefixURI("");
			}

			//set prefixes
			model.getNsPrefixMap().entrySet().stream().forEach(entry ->
				addPrefixURIMapping(entry.getKey(), entry.getValue())
			);
		} else {
			logger.warn("The model is null or it doesn't contain a NsPrefixMap");
		}
	}

	// URI stuff //////////////////////////////////////////////////////////////////////////////////
	/**
	 * Is this URI short (i.e. is there a mapping for it in the namespaces map)?
	 *
	 * @param uri the URI to test
	 * @return true if it is short, false if not or don't know
	 */
	public boolean isShortURI(String uri) {
		//TODO: can be improved
		return namespaces.containsKey(Triple.splitURI(uri, 0, false));
	}

	/**
	 * Is this URI long (i.e. is there a mapping for it in the namespaces map)?
	 *
	 * @param uri the URI to test
	 * @return true if it is long, false if not or don't know
	 */
	public boolean isLongURI(String uri) {

		//assume it's long if it can't be determined
		boolean isLong = true;
		//TODO: use this variable for something?
		//String t = Triple.splitURI(uri, 0, true);
		//short URI for local namespace or found in map of prefixes
		if (":".equals(prefix) || namespaces.containsKey(Triple.splitURI(uri, 0, false))) {
			isLong = false;
		}
		return isLong;
	}

	/**
	 * Translate a short URI to a long URI using the current known namespaces.
	 *
	 * @param shortURI the short URI to start with
	 * @return the full URI or null if the prefix is unknown
	 */
	public String toFullURI(String shortURI) {

		if (isLongURI(shortURI)) {
			return shortURI;
		} else {
			//logger.debug("{} is not a long URI", shortURI);
		}
		String shortprefix = Triple.splitURI(shortURI, 0, false);
		String longURI = null;
		if (namespaces.containsKey(shortprefix)) {
			if (!shortprefix.isEmpty()) {
				longURI = shortURI.replace(shortprefix + ":", namespaces.get(shortprefix));
			} else {
				longURI = namespaces.get("") + shortURI.substring(1);
			}
		} else {
			logger.error("Error translating {} to full URI, prefix {} is unknown", shortURI, shortprefix);
		}
		//logger.debug("Translating short URI {} to full URI {}", shortURI, longURI);
		return longURI;
	}

	/**
	 * Translate a short URI to a long URI using the current known namespaces.
	 *
	 * @param fullURI
	 * @return the full URI or null if the prefix is unknown
	 */
	public String toShortURI(String fullURI) {

		if (isShortURI(fullURI)) {
			return fullURI;
		}
		String fullprefix = Triple.splitURI(fullURI, 0, true);
		String shortURI = "";

		if (namespaces.containsValue(fullprefix)) {
			String key = null;
			for (Map.Entry<String, String> e : namespaces.entrySet()) {
				if (e.getValue().equals(fullprefix)) {
					key = e.getKey();
					break;
				}
			}
			//TODO: safety
			shortURI = fullURI.replace(fullprefix, key + ":");
		} else {
			logger.error("Error translating {} to short URI, prefix {} is unknown", fullURI, fullprefix);
		}
		return shortURI;
	}

	// Other stuff ////////////////////////////////////////////////////////////////////////////////
	/**
	 * Create a triple from the given arguments translating them to long URIs
	 *
	 * @param subject
	 * @param predicate
	 * @param object
	 * @param type
	 * @return the finished triple
	 */
	public Triple createTriple(String subject, String predicate, String object, TripleType type) {

		String s = subject;
		String p = predicate;
		String o = object;

		//translate to full URIs
		if (Triple.isShort(s)) {
			s = toFullURI(s);
		}
		if (Triple.isShort(p)) {
			p = toFullURI(p);
		}
		switch (type) {
			//only create full uri for actual URIs, not data values
			case OBJECT_PROPERTY:
			case CLASS_ASSERTION:
				if (Triple.isShort(o)) {
					o = toFullURI(o);
				}
				break;
			default:
			//do nothing
		}
		return new Triple(s, p, o, type);
	}

	/**
	 * Creates a statement from the given triple. If it's invalid, it will return null. Note that
	 * this is only to be used for concrete nodes, i.e. you cannot add blank nodes. To add blank
	 * nodes, you need to execute a SPARQL CONSTRUCT query on the model.
	 *
	 * @param subject
	 * @param predicate
	 * @param object
	 * @param type (class assertion, object/data/annotation property)
	 * @return the new statement
	 */
	public Statement createStatement(String subject, String predicate, String object, TripleType type) {

		//logger.debug("{} {} {}", subject, predicate, object);
		Statement s;
		Resource subj = ResourceFactory.createResource(toFullURI(subject));
		Property pred = ResourceFactory.createProperty(toFullURI(predicate));
		Resource obj = ResourceFactory.createResource(toFullURI(object));

		switch (type) {
			case CLASS_ASSERTION:
				pred = ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
				s = ResourceFactory.createStatement(subj, pred, obj);
				break;

			case OBJECT_PROPERTY:
				s = ResourceFactory.createStatement(subj, pred, obj);
				break;

			case DATA_PROPERTY:
			case ANNOTATION_PROPERTY:
				//find datatypes other than string (standard)
				RDFDatatype xsdtype = XSDDatatype.XSDstring;
				String value = object;
				if (object.contains("^^")) {
					value = object.substring(0, object.indexOf("^^"));
					String lowerobject = object.toLowerCase();
					//TODO: add more xsd datatypes
					if (lowerobject.lastIndexOf("boolean") > lowerobject.lastIndexOf("^^")) {
						xsdtype = XSDDatatype.XSDboolean;
					} else if (lowerobject.lastIndexOf("integer") > lowerobject.lastIndexOf("^^")) {
						xsdtype = XSDDatatype.XSDinteger;
					} else if (lowerobject.lastIndexOf("double") > lowerobject.lastIndexOf("^^")) {
						xsdtype = XSDDatatype.XSDdouble;
					} else if (lowerobject.lastIndexOf("datetime") > lowerobject.lastIndexOf("^^")) {
						xsdtype = XSDDatatype.XSDdateTime;
					} else if (lowerobject.lastIndexOf("date") > lowerobject.lastIndexOf("^^")) {
						xsdtype = XSDDatatype.XSDdate;
					} else if (lowerobject.lastIndexOf("time") > lowerobject.lastIndexOf("^^")) {
						xsdtype = XSDDatatype.XSDtime;
					} else if (lowerobject.lastIndexOf("byte") > lowerobject.lastIndexOf("^^")) {
						xsdtype = XSDDatatype.XSDbyte;
					} else if (lowerobject.lastIndexOf("decimal") > lowerobject.lastIndexOf("^^")) {
						xsdtype = XSDDatatype.XSDdecimal;
					} else if (lowerobject.lastIndexOf("duration") > lowerobject.lastIndexOf("^^")) {
						xsdtype = XSDDatatype.XSDduration;
					} else if (lowerobject.lastIndexOf("float") > lowerobject.lastIndexOf("^^")) {
						xsdtype = XSDDatatype.XSDfloat;
					} else if (lowerobject.lastIndexOf("long") > lowerobject.lastIndexOf("^^")) {
						xsdtype = XSDDatatype.XSDlong;
					}
				}
				s = ResourceFactory.createStatement(subj, pred, ResourceFactory.createTypedLiteral(value, xsdtype));
				break;

			default:
				logger.error("Error adding triple {} {} {}, unknown triple type {}", subject, predicate, object, type);
				s = null;
		}

		return s;
	}

	/**
	 * Reset the SemanticFactory to a clean state (i.e. clear all namespaces and prefixes)
	 */
	public void clear() {
		namespaces = new ConcurrentHashMap<>();
		prefix = "";
		baseURI = "";
	}

	//GETTERS/SETTERS//////////////////////////////////////////////////////////////////////////////
	/**
	 * Returns the factory's base URI, including the separator at the end (/ or #)
	 *
	 * @return the (long) prefix
	 */
	public String getBaseURI() {
		return baseURI;
	}

	/**
	 * Sets the baseURI of the ontology for this semanticFactory, including the separator at the end (/ or #)
	 *
	 * @param baseURI the new baseURI
	 */
	public void setBaseURI(String baseURI) {
		this.baseURI = baseURI;
		namespaces.put("", baseURI);
	}

	/**
	 * Translates a short prefix into a long one.
	 *
	 * @param prefix the short prefix
	 * @return the long prefix or null if it isn't defined
	 */
	public String getNamespaceForPrefix(String prefix) {
		if (namespaces.containsKey(prefix)) {
			return namespaces.get(prefix);
		} else {
			return null;
		}
	}

	/**
	 * Finds out whether this prefix is contained in the currently held map of namespaces
	 *
	 * @param prefix the prefix to look for
	 * @return true if it's contained, false if not
	 */
	public boolean containsPrefix(String prefix) {
		return namespaces.containsKey(prefix);
	}

	/**
	 * Finds out whether this uri is contained in the currently held map of namespaces
	 *
	 * @param uri the uri to look for
	 * @return true if it's contained, false if not
	 */
	public boolean containsURI(String uri) {
		return namespaces.containsValue(uri);
	}

	/**
	 * Get a snapshot of all the currently maintained prefixes and their full URIs
	 * @return
	 */
	public Map<String, String> getNamespaces() {
		return namespaces;
	}

	/**
	 * Set a new namespace map
	 *
	 * @param namespaces  the new map
	 */
	public void setNamespaces(Map<String, String> namespaces) {
		this.namespaces = namespaces;
	}
}
