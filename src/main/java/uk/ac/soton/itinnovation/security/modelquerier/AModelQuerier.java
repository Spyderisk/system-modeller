/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2015
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
//      Created By:				Stefanie Cox, Matthew Jones
//      Created Date:			2015-02-06
//      Created for Project:		OPTET
//		Modified for Project:	5G-ENSURE, ASSURED, SHiELD
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelquerier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.model.Level;
import uk.ac.soton.itinnovation.security.modelquerier.util.ModelStack;
import uk.ac.soton.itinnovation.security.modelquerier.util.TemplateLoader;
import uk.ac.soton.itinnovation.security.semanticstore.AStoreWrapper;
import uk.ac.soton.itinnovation.security.semanticstore.util.SparqlHelper;

/**
 * A class to query the Model
 */
public abstract class AModelQuerier {

	private static final Logger logger = LoggerFactory.getLogger(AModelQuerier.class);

	protected final ModelStack model;
	protected final Map<String, String> templateMap;

	public AModelQuerier(ModelStack stack) {

		this.model = stack;
		String dir = AModelQuerier.class.getClassLoader().getResource("./sparql").getPath();
		templateMap = TemplateLoader.loadTemplateMap(dir);
	}

	/**
	 * Get all graphs in the store
	 *
	 * @param store the store in which to find the graphs
	 * @return a set of all graph URIs in teh store
	 */
	public Set<String> getAllGraphs(AStoreWrapper store){

		Set<String> result = new HashSet<>();
		store.translateSelectResult(store.querySelect(templateMap.get("GetAllGraphs"))).forEach(
			row -> result.add(row.get("g")
		));
		return result;
	}

	/**
	 * Get the description of the given class
	 *
	 * @param store the store to query
	 * @param classURI the class
	 * @return the description as a string. If it doesn't exist try to get the parent class' description.
	 */
	public String getDescriptionOfClass(AStoreWrapper store, String classURI) {

		//use the own description if it exists and the parent class' description otherwise
		String sparql = "SELECT DISTINCT ?desc WHERE {\n"
				+ "	BIND (<" + SparqlHelper.escapeURI(classURI) + "> as ?c) .\n"
				+ "	OPTIONAL { ?c rdfs:comment ?l1 . }\n"
				+ "	OPTIONAL {\n"
				+ "		?c rdfs:subClassOf ?sc .\n"
				+ "		?sc a owl:Class .\n"
				+ "		OPTIONAL { ?sc rdfs:comment ?l2 . }\n"
				+ "	}\n"
				+ "	BIND (IF(BOUND(?l1), ?l1, ?l2) AS ?desc) .\n"
				+ "}";

		for (Map<String, String> row : store.translateSelectResult(store.querySelect(sparql,
				model.getGraph("core"),
				model.getGraph("domain"),
				model.getGraph("system"),
				model.getGraph("system-inf")
		))) {
			//TODO: what if the superclass hasn't got a description either? surely we want to return null in that case!
			return row.get("desc");
		}
		return null;
	}

	/**
	 * Get all superclasses of the given class
	 *
	 * @param store the store to query
	 * @param classURI the class
	 * @return the superclass URIs
	 */
	public Set<String> getDirectSuperclassesOfClass(AStoreWrapper store, String classURI) {

		String sparql = "SELECT ?c\n" +
		"WHERE {\n" +
		"	<" + SparqlHelper.escapeURI(classURI) + "> rdfs:subClassOf ?c .\n" +
		"}";

		Set<String> result = new HashSet<>();

		for (Map<String, String> row : store.translateSelectResult(store.querySelect(sparql,
			model.getGraph("core"),
			model.getGraph("domain"),
			model.getGraph("system"),
			model.getGraph("system-inf")
		))) {
			result.add(row.get("c"));
		}

		return result;
	}

	/**
	 * Get the name, description and superclass of the given class
	 *
	 * @param store the store to query
	 * @param uri the class
	 * @return a map of information with the following keys: - name: the label or the local part of its URI if it
	 * doesn't have a human-readable label - superclass: the superclass of this class (assuming it only has one - random
	 * one if it has more) - description: the description or the description of its superclass if it doesn't have one
	 */
	public Map<String, String> getClassInfo(AStoreWrapper store, String uri) {

		Map<String, String> res = new HashMap<>();

		String sparql = "SELECT DISTINCT ?name ?super ?desc WHERE {\n"
				+ "	BIND (<" + SparqlHelper.escapeURI(uri) + "> as ?x) .\n"
				+ "	{\n"
				+ "		?x a ?super .\n"
				+ "	} UNION {\n"
				+ "		?x core:parent ?super .\n"
				+ "	} UNION {\n"
				+ "		?x rdfs:subClassOf ?super .\n"
				+ "		?super a owl:Class .\n"
				+ "	}\n"
				+ "	OPTIONAL { ?x rdfs:label ?label . }\n"
				+ "	OPTIONAL { ?x rdfs:comment ?l1 . }\n"
				+ "	OPTIONAL { ?super rdfs:comment ?l2 . }\n"
				+ "	BIND (IF(BOUND(?l1), ?l1, ?l2) AS ?desc) .\n"
				+ "	BIND (IF(BOUND(?label), ?label, STRAFTER(STR(?x),\"#\")) AS ?name) .\n"
				+ "}";

		String[] graphs = new String[4];
		graphs[0] = model.getGraph("core");
		graphs[1] = model.getGraph("domain");
		if (this.getClass().equals(SystemModelQuerier.class)) {
			graphs[2] = model.getGraph("system");
			graphs[3] = model.getGraph("system-inf");
		}

		for (Map<String, String> row : store.translateSelectResult(store.querySelect(sparql, graphs))) {
			res.put("name", row.get("name"));
			if (row.containsKey("desc")) {
				res.put("description", row.get("desc"));
			} else {
				res.put("description", "No description available");
			}
			res.put("super", row.get("super"));
		}

		return res;
	}

	/**
	 * Get the name, description and superproperty of the given property
	 *
	 * @param store the store to query
	 * @param uri the property
	 * @return a map of information with the following keys: - name: the label or the local part of its URI if it
	 * doesn't have a human-readable label - superclass: the superproperty of this property - description: the
	 * description of the description of its superproperty if it doesn't have one
	 */
	public Map<String, String> getPropertyInfo(AStoreWrapper store, String uri) {

		Map<String, String> res = new HashMap<>();

		String sparql = "SELECT DISTINCT ?name ?super ?desc WHERE {\n"
				+ "	BIND (<" + SparqlHelper.escapeURI(uri) + "> as ?x) .\n"
				+ "	{\n"
				+ "		?x rdfs:subPropertyOf ?super .\n"
				+ "		?super rdf:type ?prop .\n"
				+ "		?prop rdfs:subPropertyOf* rdf:Property .\n"
				+ "	} UNION {\n"
				+ "		?x rdf:type ?super .\n"
				+ "		?super rdfs:subClassOf* rdf:Property .\n"
				+ "	}\n"
				+ "	OPTIONAL { ?x rdfs:label ?label . }\n"
				+ "	OPTIONAL { ?x rdfs:comment ?l1 . }\n"
				+ "	OPTIONAL { ?super rdfs:comment ?l2 . }\n"
				+ "	BIND (IF(BOUND(?l1), ?l1, ?l2) AS ?desc) .\n"
				+ "	BIND (IF(BOUND(?label), ?label, STRAFTER(STR(?x),\"#\")) AS ?name) .\n"
				+ "}";

		for (Map<String, String> row : store.translateSelectResult(store.querySelect(sparql,
				model.getGraph("core"),
				model.getGraph("domain"),
				model.getGraph("system"),
				model.getGraph("system-inf")
		))) {
			res.put("name", row.get("name"));
			if (row.containsKey("desc")) {
				res.put("description", row.get("desc"));
			} else {
				res.put("description", "No description available");
			}
			res.put("superproperty", row.get("super"));
		}

		return res;
	}

	/**
	 * Get basic information about any ontology in this model
	 *
	 * @param store the store to query
	 * @param uri the URI of the model for which to get the information
	 * @return a map of information with the following keys: - uri: the base URI of the ontology - name: the label
	 * containing the name of the ontology if it has one or the last part of the URI - version: the ontology's version -
	 * description: the description of the ontology
	 */
	public Map<String, String> getOntologyInfo(AStoreWrapper store, String uri) {

		Map<String, String> res = new HashMap<>();

		if (uri == null || uri.length() < 1) {
			logger.warn("Core URI <{}> is invalid, could not execute query.", uri);
			return res;
		}

		String sparql = "SELECT DISTINCT ?ont ?version ?label ?desc WHERE {\n"
				+ "	BIND(<" + SparqlHelper.escapeURI(uri.substring(0, uri.length() - 1)) + "> as ?ont) .\n"
				+ "	OPTIONAL { ?ont owl:versionInfo ?ver . }\n"
				+ "	OPTIONAL { ?ont rdfs:label ?lab . }\n"
				+ "	OPTIONAL { ?ont rdfs:comment ?comm . }\n"
				+ "	BIND (IF(BOUND(?ver), ?ver, \"Not specified\") AS ?version) .\n"
				+ "	BIND (IF(BOUND(?lab), ?lab, \"Unknown domain model\") AS ?label) .\n"
				+ "	BIND (IF(BOUND(?comm), ?comm, \"No description available\") AS ?desc) .\n"
				+ "}";

		for (Map<String, String> row : store.translateSelectResult(store.querySelect(sparql,
				model.getGraph("core"),
				model.getGraph("domain"),
				model.getGraph("system"),
				model.getGraph("system-inf")
		))) {
			res.put("uri", row.get("ont"));
			res.put("name", row.get("label"));
			res.put("version", row.get("version"));
			res.put("description", row.get("desc"));
		}

		return res;
	}

	/**
	 * This method retrieves all object properties within a specified namespace
	 *
	 * @param store the store to query
	 * @param uri the URI in which to search for properties
	 * @return a set of property URIs
	 */
	public Set<String> getObjectProperties(AStoreWrapper store, String uri) {

		Set<String> properties = new HashSet<>();

		//TODO: this should really use graphs instead of strings for filtering
		String sparql = "SELECT ?p WHERE {\n"
				+ "	?p a owl:ObjectProperty .\n"
				+ "	FILTER(STRSTARTS(STR(?p), \"" + SparqlHelper.escapeLiteral(uri) + "\")) .\n"
				+ "}";

		for (Map<String, String> row : store.translateSelectResult(store.querySelect(sparql,
				model.getGraph("core"),
				model.getGraph("domain"),
				model.getGraph("system"),
				model.getGraph("system-inf")
		))) {
			properties.add(row.get("p"));
		}
		return properties;
	}

	/**
	 * retrieve the domain and range assets of a given property class
	 *
	 * @param store the store to query
	 * @param propertyClass the property class
	 * @return a map containing on set of strings for the domain and one set of strings for the range
	 */
	public Map<String, Set<String>> getDomainRangeFromProperty(AStoreWrapper store, String propertyClass) {

		Map<String, Set<String>> domainRange = new HashMap<>();
		domainRange.put("domain", new HashSet<>());
		domainRange.put("range", new HashSet<>());

		String[] graphs = new String[4];
		graphs[0] = model.getGraph("core");
		graphs[1] = model.getGraph("domain");
		if (this.getClass().equals(SystemModelQuerier.class)) {
			graphs[2] = model.getGraph("system");
			graphs[3] = model.getGraph("system-inf");
		}

		//domain
		String sparql = "SELECT DISTINCT ?domain WHERE {\n" +
		"	BIND(<" + SparqlHelper.escapeURI(propertyClass) + "> as ?property)\n" +
		"	?property rdfs:domain ?d .\n" +
		"	{\n" +
		"		?domain rdfs:subClassOf* ?d .\n" +
		"		?d rdfs:subClassOf* core:Asset .\n" +
		"	} UNION {\n" +
		"		?d owl:unionOf ?u .\n" +
		"		?u rdf:rest*/rdf:first ?c .\n" +
		"		?domain rdfs:subClassOf* ?c .\n" +
		"	}\n" +
		"}";
		for (Map<String, String> row : store.translateSelectResult(store.querySelect(sparql, graphs))) {
			domainRange.get("domain").add(row.get("domain"));
		}

		//range
		sparql = "SELECT DISTINCT ?range WHERE {\n" +
		"	BIND(<" + SparqlHelper.escapeURI(propertyClass) + "> as ?property)\n" +
		"	?property rdfs:range ?r .\n" +
		"	{\n" +
		"		?range rdfs:subClassOf* ?r .\n" +
		"		?r rdfs:subClassOf* core:Asset .\n" +
		"	} UNION {\n" +
		"		?r owl:unionOf ?u .\n" +
		"		?u rdf:rest*/rdf:first ?c .\n" +
		"		?range rdfs:subClassOf* ?c .\n" +
		"	}\n" +
		"}";
		for (Map<String, String> row : store.translateSelectResult(store.querySelect(sparql, graphs))) {
			domainRange.get("range").add(row.get("range"));
		}

		return domainRange;
	}

	/**
	 * A query to get the domain or range of a property
	 *
	 * @param store
	 * @param uri
	 * @param prop
	 * @return
	 */
	public Set<String> getInfoOfProperty(AStoreWrapper store, String uri, String prop){

		Set<String> result = new HashSet<>();
		for (Map<String, String> row: store.translateSelectResult(store.querySelect(
				TemplateLoader.formatTemplate(templateMap.get("GetInfoOfProperty"), SparqlHelper.escapeURI(uri), prop),
				model.getGraph("domain")))){
			result.add(row.get("t"));
		}
		return result;
	}

	/**
	 * Takes a link property and returns the domain of the property
	 *
	 * @param store
	 * @param property
	 * @return the domain of the link property
	 */
	public Set<String> getDomainOfProperty(AStoreWrapper store, String property){

		Set<String> result = new HashSet<>();

		for (Map<String, String> row: store.translateSelectResult(store.querySelect(
				TemplateLoader.formatTemplate(templateMap.get("GetInfoOfProperty"), property, "rdfs:domain"),
				model.getGraph("domain")))) {
			result.add(row.get("t"));
		}
        return result;
	}

	/**
	 * Takes a link property and returns the domain of the property
	 *
	 * @param store
	 * @param property
	 * @return the domain of the link property
	 */
	public Set<String> getRangeOfProperty(AStoreWrapper store, String property){

		Set<String> result = new HashSet<>();

		for (Map<String, String> row: store.translateSelectResult(store.querySelect(
				TemplateLoader.formatTemplate(templateMap.get("GetInfoOfProperty"), property, "rdfs:range"),
				model.getGraph("domain")))) {
			result.add(row.get("t"));
		}
        return result;
	}

	/**
     * Gets a boolean of whether a specified uri exists in the store
     *
     * @param store the store on which to execute the query
	 * @param uri the uri to be searched for
	 * @return returns a bool of whether the item exists in store or not
	 */
	public boolean containsURI(AStoreWrapper store, String uri){
		return store.queryAsk(TemplateLoader.formatTemplate(templateMap.get("ContainsUri"), uri), model.getGraph("domain"));
	}

	/**
	 * Returns all data from any given uri that has the given tag
	 *
	 * @param store the store on which to execute the query
	 * @param uri the uri of the item that the description is being found in
	 * @param property the property tag of the data that would like to be found
	 * @return the description found in the store for that uri, if none found then will return an empty string
	 */
	@Deprecated
	public Set<String> getDataSetFromURI(AStoreWrapper store, String uri, String property){

		Set<String> result = new HashSet<>();

		for (Map<String, String> row: store.translateSelectResult(store.querySelect(
			TemplateLoader.formatTemplate(templateMap.get("GetDataFromURI"), uri, property), model.getGraph("domain")))){
			if (row.containsKey("o") && row.get("o")!=null && !row.get("o").isEmpty()) {
				result.add(row.get("o"));
			}
		}

		return result;
	}

	/**
	 * Returns all possible objects for a property/subject combination
	 *
	 * @param store the store on which to execute the query
	 * @param subjectURI the uri of the subject
	 * @param property the property short URI
	 * @return all subjects, no duplicates
	 */
	public Set<String> getObjectsForSubjectAndProperty(AStoreWrapper store, String subjectURI, String property){

		Set<String> result = new HashSet<>();

		for (Map<String, String> row: store.translateSelectResult(store.querySelect(
			TemplateLoader.formatTemplate(templateMap.get("GetObjects"), subjectURI, property), model.getGraph("domain")))){
			if (row.containsKey("o") && row.get("o")!=null && !row.get("o").isEmpty()) {
				result.add(row.get("o"));
			}
		}

		return result;
	}

	/**
	 * Returns whether there exists and object for a property/subject combination
	 *
	 * @param store the store on which to execute the query
	 * @param subjectURI the uri of the subject
	 * @param property the property short URI
	 * @return true if there is an object, false otherwise
	 */
	public boolean hasObjectForSubjectAndProperty(AStoreWrapper store, String subjectURI, String property){
		return !getObjectsForSubjectAndProperty(store, subjectURI, property).isEmpty();
	}

	/**
	 * Returns all possible subjects for a property/object combination
	 *
	 * @param store the store on which to execute the query
	 * @param objectURI the uri of the object
	 * @param property the property short URI
	 * @return all subjects, no duplicates
	 */
	public Set<String> getSubjectsForPropertyAndObject(AStoreWrapper store, String property, String objectURI){

		Set<String> result = new HashSet<>();

		for (Map<String, String> row: store.translateSelectResult(store.querySelect(
			TemplateLoader.formatTemplate(templateMap.get("GetSubjects"), property, objectURI), model.getGraph("domain")))){
			if (row.containsKey("s") && row.get("s")!=null && !row.get("s").isEmpty()) {
				result.add(row.get("s"));
			}
		}

		return result;
	}

	/**
	 * Returns a random object for the given subject and the given
	 *
	 * @param store the store on which to execute the query
	 * @param uri the uri of the item that the description is being found in
	 * @param property the property tag of the data that would like to be found
	 * @return the property found in the store for that uri or a random one if more than one is set
	 */
	public String getDataFromURI(AStoreWrapper store, String uri, String property){

		Set<String> data = getDataSetFromURI(store, uri, property);

		if (data.isEmpty()) {
			return "Property " + property + " missing for <" + uri + ">";
		} else {
			//TODO: fix this. Which one do we want?
			return data.iterator().next();
		}
	}

	public Set<String> getAllDirectSubclasses(AStoreWrapper store, String superClass){
		Set<String> result = new HashSet<>();
		for (Map<String, String> row: store.translateSelectResult(store.querySelect(
				TemplateLoader.formatTemplate(templateMap.get("GetAllDirectSubclasses"), SparqlHelper.escapeURI(superClass)),
				model.getGraph("domain")))){
			result.add(row.get("sub"));
		}
		return result;
	}

	/**
	 * Get all instances of this class or any of its subclasses
	 *
	 * @param store the store to query
	 * @param superClass the class of which the individual needs to be an instance
	 * @return a set of all instances
	 */
	public Set<String> getAllInstances(AStoreWrapper store, String superClass){
		Set<String> result = new HashSet<>();
		store.translateSelectResult(store.querySelect(TemplateLoader.formatTemplate(
			templateMap.get("GetAllInstances"), SparqlHelper.escapeURI(superClass)))).forEach(row ->
				result.add(row.get("i"))
		);
		return result;
	}

	public Set<String> getSuperClassList(AStoreWrapper store, String subClass){

		Set<String> result = new HashSet<>();
		for (Map<String, String> row: store.translateSelectResult(store.querySelect(
				TemplateLoader.formatTemplate(templateMap.get("GetSuperClassList"), SparqlHelper.escapeURI(subClass)),
				model.getGraph("domain")))){
			result.add(row.get("super"));
		}
		return result;
	}

	/**
	 * Get the graph for an ontology URI
	 *
	 * @param store
	 * @return
	 */
	public String getBaseURI(AStoreWrapper store){

		for (Map<String, String> row: store.translateSelectResult(store.querySelect(
				TemplateLoader.formatTemplate(templateMap.get("GetBaseURI"), model.getGraph("domain"))))){
			return row.get("uri");
		}
		return null;
	}

	/**
	 * Get all instances of this Level
	 *
	 * @param store the store
	 * @param levelClassLocalName the local name of the Level class (i.e. the URI without the core prefix)
	 * @return a set of the instances
	 */
	public Map<String, Level> getLevels(AStoreWrapper store, String ... levelClassLocalName) {

		Map<String, Level> result = new HashMap<>();

		if (levelClassLocalName.length<=0) {
			return result;
		}

		String sparql = "SELECT * WHERE {\n" +
		"	?level a ?levelClass .\n" +
		"	FILTER (?levelClass IN (";
		for (String lc: levelClassLocalName) {
			sparql += "core:" + SparqlHelper.escapeURI(lc) + ", ";
		}
		sparql = sparql.substring(0, sparql.length()-2);
		sparql += "))\n" +
		"	?level rdfs:label ?levell .\n" +
		"	OPTIONAL {\n" +
		"		?level core:levelValue ?levelv .\n" +
		"		BIND(STR(?levelv) AS ?levelval) .\n" +
		"	}\n" +
		"}";
		for (Map<String, String> row: store.translateSelectResult(store.querySelect(sparql,
				model.getGraph("core"), model.getGraph("domain")
		))){
			Level l = new Level();
			if (row.containsKey("level") && row.get("level")!=null) {
				l.setUri(row.get("level"));
			}
			if (row.containsKey("levell") && row.get("levell")!=null) {
				l.setLabel(row.get("levell"));
			}
			if (row.containsKey("levelClass") && row.get("levelClass")!=null) {
				l.getParents().add(row.get("levelClass"));
			}
			if (row.containsKey("levelval") && row.get("levelval")!=null) {
				l.setValue(Integer.valueOf(row.get("levelval")));
			}
			result.put(l.getUri(), l);
		}

		return result;
	}

	// Getters/Setters ////////////////////////////////////////////////////////////////////////////

	/**
	 * Get the current stack for this model querier
	 *
	 * @return the stack
	 */
	public ModelStack getModel() {
		return model;
	}
}
