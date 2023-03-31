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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides an interface to a triple store's SPARQL HTTP endpoint.
 * Modifications have been made to work with the RDF4J SPARQL endpoint, which defines its own
 */
public abstract class AHttpStoreWrapper extends AStoreWrapper {

	private static final Logger logger = LoggerFactory.getLogger(AHttpStoreWrapper.class);

	//a HTTP SPARQL endpoint might have different GET/POST variables for the actual SPARQL statement submitted
	//depending on whether it is a query (read-only) or an update (read/write)
	protected String selectPostVar = "query";
	protected String updatePostvar = "update";

	//a HTTP SPARQL endpoint might have different endpoints to execute different actions.
	protected String sparqlSelectEndpoint;
	protected String sparqlUpdateEndpoint;
	protected String sparqlClearEndpoint;

	/**
	 * Creates a HTTP store wrapper to access a triple store via its SPARQL endpoint.
	 *
	 * @param props the required properties containing the endpoint's URL(s)
	 */
	public AHttpStoreWrapper(Properties props) {
		super();

		//sanity check of the given properties
		this.props = props;
		if (!props.containsKey("sparqlendpoint.select")) {
			throw new RuntimeException("Could not create HttpStoreWrapper, please specify the sparqlendpoint.select"
					+ " property in the properties file.");
		} else {
			sparqlSelectEndpoint = props.getProperty("sparqlendpoint.select");
		}
		//optional, i.e. can be null
		if (props.containsKey("sparqlendpoint.update") && !props.get("sparqlendpoint.update").equals("")) {
			sparqlUpdateEndpoint = props.getProperty("sparqlendpoint.update");
		} else {
			sparqlUpdateEndpoint = sparqlSelectEndpoint;
		}
		if (props.containsKey("sparqlendpoint.clear") && !props.get("sparqlendpoint.clear").equals("")) {
			sparqlClearEndpoint = props.getProperty("sparqlendpoint.clear");
		} else {
			sparqlClearEndpoint = sparqlUpdateEndpoint;
		}
		//auth
		if (props.contains("sparqlendpoint.user") || props.containsKey("sparqlendpoint.password")) {
			//TODO: implement
//			HttpAuthenticator auth = new HttpAuthenticator();
		}
	}

	// Graph management ///////////////////////////////////////////////////////////////////////////
	@Override
	public boolean graphExists(String graph) {
		logger.debug("Checking for the existence of graph {} on server {}", graph, sparqlSelectEndpoint);
		//stores are not required to record existence of empty graphs
		//see http://www.w3.org/TR/sparql11-update/#graphManagement

		//assuming true because the notion of empty graphs is optional, see above
		boolean result = true;
		try {
			result = queryAsk("ASK { GRAPH <" + graph + "> {?s ?p ?o} }");
		} catch (Exception e) {
			logger.warn("Could not check for graph existence: {}. Assuming it exists but might be empty.", e);
		}

		return result;
	}

	@Override
	public void createGraph(String graph) {
		//stores are not required to record existence of empty graphs
		//see http://www.w3.org/TR/sparql11-update/#graphManagement
		//this will create an empty graph for stores who do and have no effect otherwise.
		logger.debug("Creating graph <{}>", graph);
		update("CREATE GRAPH <" + graph + ">");
	}

	@Override
	public void clearGraph(String graph) {
		//stores are not required to record existence of empty graphs
		//see http://www.w3.org/TR/sparql11-update/#graphManagement
		//This means it is the same as deleting a graph for some stores.
		logger.debug("Clearing graph <{}>", graph);
		update("CLEAR GRAPH <" + graph + ">");

		//check if repo is empty and delete all triples if not
		if (getCount(graph) > 0) {
			logger.warn("Clearing the graph failed, deleting all triples instead");
			//for stores that don't support named graphs...
			update("DELETE WHERE {?s ?p ?o}");
			//...and for those that do
			update("DELETE {?s ?p ?o} WHERE {GRAPH <" + graph + "> {?s ?p ?o}}");
		}
	}

	@Override
	public void deleteGraph(String graph) {
		//stores are not required to record existence of empty graphs
		//see http://www.w3.org/TR/sparql11-update/#graphManagement
		logger.debug("Deleting graph <{}>", graph);
		update("DROP GRAPH <" + graph + ">");

		if (getCount(graph) > 0) {
			logger.warn("Deleting graph {} failed, clearing graph instead", graph);
			clearGraph(graph);
		}
	}

	@Override
	public long getCount(String ... graph) {

		//TODO: use more than one graph:
		String g = graph!=null?graph[0]:null;

		//Different store implementations might support one way or another of determining the amount of triples
		//These two variables collect all information with only the reasonable result being used eventually
		List<Map<String, String>> result1;
		List<Map<String, String>> result2 = null;

		//for stores that don't support named graphs...
		result1 = translateSelectResult(querySelect("SELECT (COUNT(*) as ?count) WHERE {?s ?p ?o}"));
		if (g != null) {
			//...and for those that do
			result2 = translateSelectResult(querySelect(
				"SELECT (COUNT(*) as ?count) WHERE {GRAPH <" + g + "> {?s ?p ?o}}"
			));
		}

		//evaluate the results
		final String count = "count";
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

	// General actions ////////////////////////////////////////////////////////////////////////////
	@Override
	public void connect() {
		//not needed; this method is only overridden here because other store connectors require this
		logger.debug("Explicitly connecting to a store is unnecessary when using a SPARQL endpoint");
	}

	@Override
	public void disconnect() {
		//not needed; this method is only overridden here because other store connectors require this
		logger.debug("Explicitly disconnecting from a store is unnecessary when using a SPARQL endpoint");
	}

	@Override
	public boolean update(String sparql, String ... graph) {

		try {
			Map<String, String> params = new HashMap<>();
			//accept server response only as rdf/xml. More options might be implemented in the future
			params.put("Accept", "application/rdf+xml");
			params.put("Content-Type", "application/sparql-query");
			//TODO: use graph URI(s) in SPARQL
			doRequest(sparqlUpdateEndpoint, "POST", updatePostvar + "=" + URLEncoder.encode(sparql, "UTF-8"), params);
		} catch (UnsupportedEncodingException e) {
			//don't throw here, only notify user
			logger.error("Could not HTTP encode SPARQL update {} for execution on endpoint {}",
					sparql, sparqlUpdateEndpoint, e);
		}
		
		return false;
	}

	// Actions that might be executed on a particular graph ///////////////////////////////////////

	// Protected Methods ////////////////////////////////////////////////////////////////////////////
	/**
	 * Execute a HTTP POST request on the store
	 *
	 * @param url the SPARQL HTTP endpoint url
	 * @param payload the actual payload of the query
	 * @param parameters the HTTP parameters
	 * @return the result retrieved back from the url
	 */
	protected String doRequest(String url, String method, String payload, Map<String, String> parameters) {

		String r = null;

		try {
			//open connection to the url
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			//set request properties
			con.setRequestMethod(method);
			if (parameters != null) {
				parameters.entrySet().stream().forEach(param
					-> con.setRequestProperty(param.getKey(), param.getValue())
				);
			}

			// Send request
			if ("GET".equals(method)) {
				con.setDoOutput(true);
				//no extra action required
			} else if ("POST".equals(method) || "PUT".equals(method)) {
				con.setDoOutput(true);
				try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
					if (payload!=null) {
						wr.writeBytes(payload);
						wr.flush();
					}
				}
			} else if ("DELETE".equals(method)) {
				//TODO: file not found exception on non-existing graph
			} else {
				logger.error("Method {} is either unknown or not (yet) implemented", method);
			}

			//get response
			InputStream responseStream = con.getInputStream();
			//int responseCode = con.getResponseCode();
			//logger.debug("Response code {} for {} query on endpoint {}, parameters {} ({})", responseCode, method, url, parameters, payload);
			StringBuilder response;
			try (BufferedReader in = new BufferedReader(new InputStreamReader(responseStream))) {
				response = new StringBuilder();
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
			}
			r = response.toString();

		} catch (IOException e) {
			logger.error("Could not send HTTP {} request to endpoint {}.\nHave you started your web server?", method, url, e);
			logger.debug("Parameters {} ({})", parameters, payload);
		}
		return r;
	}

	// Getters/Setters ////////////////////////////////////////////////////////////////////////////
	public String getSelectPostVar() {
		return selectPostVar;
	}

	public void setSelectPostVar(String selectPostVar) {
		this.selectPostVar = selectPostVar;
	}

	public String getUpdatePostvar() {
		return updatePostvar;
	}

	public void setUpdatePostvar(String updatePostvar) {
		this.updatePostvar = updatePostvar;
	}

	public String getSparqlSelectEndpoint() {
		return sparqlSelectEndpoint;
	}

	public void setSparqlSelectEndpoint(String sparqlSelectEndpoint) {
		this.sparqlSelectEndpoint = sparqlSelectEndpoint;
	}

	public String getSparqlUpdateEndpoint() {
		return sparqlUpdateEndpoint;
	}

	public void setSparqlUpdateEndpoint(String sparqlUpdateEndpoint) {
		this.sparqlUpdateEndpoint = sparqlUpdateEndpoint;
	}

	public String getSparqlClearEndpoint() {
		return sparqlClearEndpoint;
	}

	public void setSparqlClearEndpoint(String sparqlClearEndpoint) {
		this.sparqlClearEndpoint = sparqlClearEndpoint;
	}

}
