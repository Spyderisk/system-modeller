/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2014
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
//      Created By :            Stefanie Wiegand
//      Created Date :          2014-12-12
//      Created for Project :   REVEAL
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.semanticstore;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.jena.sparql.resultset.ResultSetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.semanticstore.util.SemanticFactory;

/**
 * This class provides an interface to a triple store's SPARQL HTTP endpoint.
 * Modifications have been made to work with the RDF4J SPARQL endpoint, which defines its own
 */
public class JenaHttpStoreWrapper extends AHttpStoreWrapper {

	private static final Logger logger = LoggerFactory.getLogger(JenaHttpStoreWrapper.class);

	private SemanticFactory semFac;

	/**
	 * Creates a HTTP store wrapper to access a triple store via its SPARQL endpoint.
	 *
	 * @param props the required properties containing the endpoint's URL(s)
	 */
	public JenaHttpStoreWrapper(Properties props) {
		super(props);

		setUpdatePostvar("query");
		semFac = new SemanticFactory("", "");
	}

	// General actions ////////////////////////////////////////////////////////////////////////////

	@Override
	public long getCount(String ... graph) {

		//TODO: use more than one graph:
		String g = graph!=null?graph[0]:null;

		//Different store implementations might support one way or another of determining the amount of triples
		//These two variables collect all information with only the reasonable result being used eventually
		List<Map<String, String>> result1;
		List<Map<String, String>> result2 = null;

		//for stores that don't support named graphs...
		result1 = translateSelectResult(querySelect("SELECT (COUNT(*) as ?size) WHERE {?s ?p ?o}"));
		if (g != null) {
			//...and for those that do
			result2 = translateSelectResult(querySelect(
				"SELECT (COUNT(*) as ?size) WHERE {GRAPH <" + g + "> {?s ?p ?o}}"
			));
		}

		//evaluate the results
		final String count = "size";
		long count1 = 0;
		if (result1 != null && !result1.isEmpty() && result1.get(0).containsKey(count)) {
			String s = result1.get(0).get(count);
			//remove ^^
			if (s.contains("^^")) {
				s = s.substring(1, s.indexOf("^^"));
			}
			//remove quotes
			s = s.replace("\"", "");
			//cast to int
			if (!s.isEmpty()) {
				count1 = Integer.valueOf(s);
			}
		}
		long count2 = 0;
		if (result2 != null && !result2.isEmpty() && result2.get(0).containsKey(count)) {
			String s = result2.get(0).get(count);
			//remove ^^
			if (s.contains("^^")) {
				s = s.substring(1, s.indexOf("^^"));
			}
			//remove quotes
			s = s.replace("\"", "");
			//cast to int
			if (!s.isEmpty()) {
				count2 = Integer.valueOf(s);
			}
		}

		//use the bigger number since it might be possible that one of the queries wrongly returns 0
		return Long.max(count1, count2);
	}

	@Override
	public ResultSet querySelect(String sparql, String ... graph) {

		ResultSet rs = null;
		String sparqlG = addGraphsToSparql(sparql, SparqlType.QUERY, graph);

		try {
			//set the POST parameters:
			Map<String, String> params = new HashMap<>();
			//only accept SPARQL results
			params.put("Accept", "application/sparql-results+xml");
			params.put("Content-Type", "application/x-www-form-urlencoded");

			String response = doRequest(sparqlSelectEndpoint, "POST", selectPostVar + "="
					+ URLEncoder.encode(sparqlG, "UTF-8"), params);

			if (response != null) {
				//logger.debug("RESPONSE: {}", response);
				//deserialise XML
				rs = ResultSetFactory.fromXML(response);
			}
		} catch (ResultSetException e) {
			//Empty result retrieved from endpoint: create empty result set from new empty model
			rs = ResultSetFactory.makeResults(ModelFactory.createDefaultModel());
			logger.warn("Exception encountered during HTTP SPARQL query {}", sparqlG, e);
		} catch (UnsupportedEncodingException e) {
			logger.error("Could not HTTP encode SPARQL query {}", sparqlG, sparqlSelectEndpoint, e);
			//not throwing here; failes query will be logged and return an empty result set
		}

		return rs;
	}

	@Override
	public Model queryConstruct(String sparql, String ... graph) {

		String sparqlG = addGraphsToSparql(sparql, SparqlType.QUERY, graph);

		Query query = QueryFactory.create(sparqlG);
		Model result;

		try (QueryEngineHTTP httpQuery = new QueryEngineHTTP(sparqlUpdateEndpoint, query)) {
			result = null;
			if (query.getQueryType() == 222) {
				logger.debug("Executing CONSTRUCT query");
				result = httpQuery.execConstruct();
			} else {
				logger.debug("Invalid CONSTRUCT query:\n{}", sparqlG);
			}
		}

		return result;
	}

	@Override
	public Model queryDescribe(String sparql, String ... graph) {

		String sparqlG = addGraphsToSparql(sparql, SparqlType.QUERY, graph);

		Query query = QueryFactory.create(sparqlG);
		Model result;

		try ( //execute DESCRIBE query on read-only endpoint
				QueryEngineHTTP httpQuery = new QueryEngineHTTP(sparqlSelectEndpoint, query)) {
			result = null;
			if (query.getQueryType() == 222) {
				logger.debug("Executing DESCRIBE query");
				//TODO: add graph to SPARQL query
				result = httpQuery.execConstruct();
			} else {
				logger.error("Invalid DESCRIBE query:\n{}", sparqlG);
				//don't throw here; only return empty result set and log error
			}
		}
		return result;
	}

	@Override
	public boolean queryAsk(String sparql, String ... graph) {

		String sparqlG = addGraphsToSparql(sparql, SparqlType.QUERY, graph);

		Query query = QueryFactory.create(sparqlG);
		boolean result;

		try ( //execute ASK query on read-only endpoint
				QueryEngineHTTP httpQuery = new QueryEngineHTTP(sparqlSelectEndpoint, query)) {
			result = false;
			if (query.getQueryType() == 444) {
				logger.debug("Executing ASK query");
				//TODO: add graph to SPARQL query
				result = httpQuery.execAsk();
			} else {
				logger.error("Invalid ASK query:\n{}", sparqlG);
				//don't throw here; return "false" log error
			}
		}
		return result;
	}

	@Override
	public List<Map<String, String>> translateSelectResult(Object results) {

		ResultSet rs = (ResultSet) results;
		List<Map<String, String>> result = new LinkedList<>();

		//prepare hashmap
		if (rs != null) {
			while (rs.hasNext()) {
				//for each solution create a hashmap
				QuerySolution row = rs.next();
				Map<String, String> r = new HashMap<>();
				//add all the variables found in the solution
				rs.getResultVars().forEach(var -> r.put(var, row.get(var).toString()));
				result.add(r);
			}
		}
		return result;
	}

	// Actions that might be executed on a particular graph ///////////////////////////////////////

	@Override
	public void storeModel(Object m, String graph) {

		Model model = (Model) m;

		//get all triples from the given model
		StmtIterator it = model.listStatements();
		StringBuilder sparql = new StringBuilder("INSERT DATA {\n");
		if (graph != null) {
			sparql.append(String.join(" GRAPH <", graph, "> {\n"));
		}

		//build giant insert query
		//TODO: split from certain size onwards?
		int i = 0;
		while (it.hasNext()) {
			Statement stmt = it.next();
			String subj;
			String obj;

			//sort out subject
			if (stmt.getSubject().isURIResource()) {
				subj = "<" + stmt.getSubject().getURI() + ">";
			} else if (stmt.getSubject().isAnon()) {
				subj = stmt.getSubject().toString();
			} else {
				logger.warn("UNKNOWN subject type!");
				subj = "<null>";
			}

			//TODO: annotation properties?!
			//wrap URIs in angle brackets
			if (stmt.getObject().isURIResource()) {
				obj = "<" + stmt.getObject().toString() + ">";
			} else if (stmt.getObject().isLiteral()) {
				obj = stmt.getObject().asLiteral().getString();
				String xsdtype = stmt.getObject().asLiteral().getDatatypeURI();
				//remove quotes if they exist
				if (obj.startsWith("\"") && obj.endsWith("\"")) {
					obj = obj.substring(1, obj.length() - 1);
				}
				//add triple quotes and datatype if applicable
				final String tripleQuotes = "\"\"\"";
				if (xsdtype != null) {
					obj = tripleQuotes + obj + tripleQuotes + "^^" + semFac.toShortURI(xsdtype);
				} else {
					obj = tripleQuotes + obj + tripleQuotes;
				}
			} else if (stmt.getObject().isAnon()) {
				obj = stmt.getObject().toString();
			} else {
				logger.warn("UNKNOWN object type!");
				obj = "<null>";
			}
			//build triple
			sparql.append(String.join("", "\t\t", subj, " <", stmt.getPredicate().getURI(), "> ", obj, " .\n"));

			//9000 seems to be a good size for queries
			if (i<9000) {
				i++;
			} else {
				i = 0;
				if (graph != null) {
					sparql.append("\t}\n");
				}
				sparql.append("}");

				//execute
				update(sparql.toString(), graph);

				sparql = new StringBuilder("INSERT DATA {\n");
				if (graph != null) {
					sparql.append(String.join("\tGRAPH <", graph, "> {\n"));
				}
			}
		}
		if (graph != null) {
			sparql.append("\t}\n");
		}
		sparql.append("}");

		//execute
		update(sparql.toString());

		logger.info("Added model (size {}) to graph <{}>", model.size(), graph);
	}

	// Getters/Setters ////////////////////////////////////////////////////////////////////////////
	public SemanticFactory getSemanticFactory() {
		return semFac;
	}

	@Override
	public void clearDefaultGraph() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getCountDefault() {
		// TODO Auto-generated method stub
		return 0;
	}
}
