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
//      Created By :            Stefanie Wiegand
//      Created Date :          2017-01-23
//      Created for Project :   5G-ENSURE
//		Modified By:            Gianluca Correndo, Stefanie Cox
//		Modified for Project :	ASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.semanticstore;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFWriter;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.resultset.ResultSetMem;
import org.apache.jena.tdb.TDB;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.semanticstore.util.SparqlHelper;

/**
 * A store which is based on Apache Jena TDB using a native (file-based) persistence method
 *
 * @author Stefanie Cox
 */
public class JenaTDBStoreWrapper extends AStoreWrapper {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(JenaTDBStoreWrapper.class);

	private final Dataset dataset;
	
	public static final String DEFAULT_GRAPH = "DEFAULT_GRAPH";

	public JenaTDBStoreWrapper(String repodir) {

		super();

		//Creating Dataset, using existing data if it exists
		File dir = new File(repodir);
		if (!dir.exists()) {
			logger.debug("Creating store at {}", repodir);
			dir.mkdirs();
		} else {
			logger.debug("Using store at {}", repodir);
		}
		dataset = TDBFactory.createDataset(dir.getPath());
	}

	// Graph management ///////////////////////////////////////////////////////////////////////////

	@Override
	public boolean graphExists(String graph) {
		RuntimeException exceptionToThrow = null;
		boolean result = false; 
		
		openTransaction(ReadWrite.READ);
		try {
			result = dataset.containsNamedModel(graph);
		} catch (RuntimeException e) {
			String message = String.format("Failed to check if graph %s exists", graph);		
			logger.error(message, e);
			exceptionToThrow = new RuntimeException(message);
			exceptionToThrow.initCause(e);
			dataset.abort();
		} finally {
			closeTransaction();
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
		}
		
		return result;
	}

	@Override
	public void copyGraph(String from, String to) {
		RuntimeException exceptionToThrow = null;
		
		openTransaction(ReadWrite.WRITE);
		try {
			if (!graphExists(from)) {
				String message = String.format("Graph does not exist: <%s>", from);
				//a warning is sufficient here, as some graphs may not exist legitimately,
				//e.g. /inf might not exist if the source model had not been validated
				logger.warn(message);
			}
			long toCount = 0;
			if (graphExists(to)) {
				toCount = getCount(to);
				logger.warn("Graph <{}> already exists and contains {} triples.", to, toCount);
			}
			dataset.addNamedModel(to, dataset.getNamedModel(from));
			commitTransaction();
			
			logger.info("Copied {} triples from graph <{}> into graph <{}>, old size: {}, new size: {}", getCount(from),
					from, to, toCount, getCount(to));
		} catch (RuntimeException e) {
			String message = String.format("Failed to copy triples from graph <%s> into graph <%s>", from, to);		
			logger.error(message, e);
			exceptionToThrow = new RuntimeException(message);
			exceptionToThrow.initCause(e);
			dataset.abort();
		} finally {
			closeTransaction();
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
		}
	}

	@Override
	public void createGraph(String graph) {
		RuntimeException exceptionToThrow = null;
		
		openTransaction(ReadWrite.WRITE);
		try {
			dataset.addNamedModel(graph, ModelFactory.createDefaultModel());
			commitTransaction();
		} catch (RuntimeException e) {
			String message = String.format("Failed to create graph %s", graph);		
			logger.error(message, e);
			exceptionToThrow = new RuntimeException(message);
			exceptionToThrow.initCause(e);
			dataset.abort();
		} finally {
			closeTransaction();
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
		}
	}

	@Override
	public void clearGraph(String graph) {
		if (graph == null) {
			throw new IllegalArgumentException("'null' not a valid graph.");
		}
		
		boolean clearDefault = false;
		if (graph.equals(DEFAULT_GRAPH)) {
			clearDefault = true;
		}
		
		RuntimeException exceptionToThrow = null;
		
		openTransaction(ReadWrite.WRITE);
		try {
			if (!graphExists(graph)) {
				logger.warn("Attempting to clear non-existent graph {}", graph);
			}
			Model clearModel = clearDefault ? dataset.getDefaultModel() : 
				dataset.getNamedModel(graph);
			clearModel.removeAll();
			commitTransaction();
		} catch (RuntimeException e) {
			String message = clearDefault ? "Default graph cannot be cleared" : 
				String.format("Graph <%s> cannot be cleared", graph);		
			logger.error(message, graph, e);
			exceptionToThrow = new RuntimeException(message);
			exceptionToThrow.initCause(e);
			dataset.abort();
		} finally {
			closeTransaction();
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
		}
	}
	
	@Override
	public void clearDefaultGraph() {
		//clearGraph("urn:x-arq:DefaultGraph"); TODO: Why doesn't this work?	
		clearGraph(DEFAULT_GRAPH);
	}

	@Override
	public void deleteGraph(String graph) {
		if (graph == null) {
			throw new IllegalArgumentException("'null' not a valid graph.");
		}
		
		RuntimeException exceptionToThrow = null;
		
		openTransaction(ReadWrite.WRITE);
		try {
			dataset.removeNamedModel(graph);
			commitTransaction();
		} catch (Exception e) {
			String message = String.format("Could not delete graph <%s>.", graph);
			logger.warn(message, e);
			exceptionToThrow = new RuntimeException(message);
			exceptionToThrow.initCause(e);
			dataset.abort();
		} finally {
			closeTransaction();
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
		}
	}

	@Override
	public long getCount(String ... graph) {
		if (graph == null) {
			throw new IllegalArgumentException("'null' not a valid graph.");
		}
		
		RuntimeException exceptionToThrow = null;
		long result = 0l;
		
		openTransaction(ReadWrite.READ);
		try {
			if (graph.length<1) {
				result = countTriples(dataset);
			} else {
				for (String g: graph) {
					if (g == null) {
						throw new IllegalArgumentException("'null' not a valid graph.");
					}
					
					result += dataset.getNamedModel(g).size();
				}
			}
		} catch (RuntimeException e) {
			String message = String.format("Failed to count triples in graph(s)");		
			logger.error(message, e);
			exceptionToThrow = new RuntimeException(message);
			exceptionToThrow.initCause(e);
			dataset.abort();
		} finally {
			closeTransaction();
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
		}
	
		return result;
	}
	
	@Override
	public long getCountDefault() {
		//return getCount("urn:x-arq:DefaultGraph"); // TODO: Why doesn't this work?
		
		openTransaction(ReadWrite.READ);
		long result = dataset.getDefaultModel().size();
		closeTransaction();
		return result;
	}

	@Override
	public Set<String> getGraphs() {
		Set<String> graphs = new HashSet<>();	
		RuntimeException exceptionToThrow = null;

		openTransaction(ReadWrite.READ);
		try {
			Iterator<String> it = dataset.listNames();
			while (it.hasNext()) {
				graphs.add(it.next());
			}
		} catch (RuntimeException e) {
			String message = String.format("Failed to get graphs.");		
			logger.error(message, e);
			exceptionToThrow = new RuntimeException(message);
			exceptionToThrow.initCause(e);
			dataset.abort();
		} finally {
			closeTransaction();
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
		}

		return graphs;
	}

	// General actions ////////////////////////////////////////////////////////////////////////////

	@Override
	public void connect() {
		//not required
	}

	@Override
	public void disconnect() {
		//not required
		
		//TDBFactory.release(dataset);
		dataset.close();
	}

	@Override
	public String getSizes() {
		RuntimeException exceptionToThrow = null;
		String info = null;
		
		openTransaction(ReadWrite.READ);
		try {
			info = "Store sizes: overall (" + getCount() + "), default graph (" + getCountDefault() + ")";
			Iterator<String> it = dataset.listNames();
			while (it.hasNext()) {
				String g = it.next();
				info += ", <" + g + "> (" + getCount(g) + ")";
			}
		} catch (RuntimeException e) {
			String message = String.format("Failed to get sizes.");		
			logger.error(message, e);
			exceptionToThrow = new RuntimeException(message);
			exceptionToThrow.initCause(e);
			dataset.abort();
		} finally {
			closeTransaction();
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
		}
		
		return info;
	}

	@Override
	public void printSizes() {
		logger.info(getSizes());
	}

	@Override
	public void clear() {

		clearDefaultGraph();
		for (String g: getGraphs()) {
			//clearGraph(g);
			deleteGraph(g);
		}
	}

	// Querying ///////////////////////////////////////////////////////////////////////////////////
	@Override
	public ResultSet querySelect(String sparql, String... graph) {
		RuntimeException exceptionToThrow = null;
		ResultSet result = null;
		QueryExecution qexec = null;

		openTransaction(ReadWrite.READ);
		try {
			qexec = createQueryExecution(sparql, graph);
			result = qexec.execSelect();
			result = new ResultSetMem(ResultSetFactory.copyResults(result));
		} catch (Exception e) {
			String queryString = qexec != null? qexec.getQuery().serialize(Syntax.syntaxSPARQL) : sparql;
			String message = String.format("Could not execute query\n%s\n against graph(s) %s", queryString, graph);
			logger.error(message, e);
			exceptionToThrow = new RuntimeException(message);
			exceptionToThrow.initCause(e);
			dataset.abort();
		} finally {
			TDB.getContext().set(TDB.symUnionDefaultGraph, false);
			closeTransaction();
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
		}
		return result;
	}

	@Override
	public Model queryConstruct(String sparql, String... graph) {
		RuntimeException exceptionToThrow = null;
		Model inf = null;
		QueryExecution qexec = null;

		openTransaction(ReadWrite.READ);
		try {
			qexec = createQueryExecution(sparql, graph);
			inf = qexec.execConstruct();
			//logger.debug("Created {} triples in CONSTRUCT query:\n{}", (inf!=null?inf.size():"no"), getSPARQLPrefixes() + sparqlG);
		} catch (Exception e) {
			String queryString = qexec != null? qexec.getQuery().serialize(Syntax.syntaxSPARQL) : sparql;
			String message = String.format("Could not execute query\n%s\n against graph(s) %s", queryString, graph);
			logger.error(message, e);
			exceptionToThrow = new RuntimeException(message);
			exceptionToThrow.initCause(e);
			dataset.abort();
		} finally {
			TDB.getContext().set(TDB.symUnionDefaultGraph, false);
			closeTransaction();
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
		}
		return inf;
	}

	@Override
	public boolean queryAsk(String sparql, String... graph) {
		RuntimeException exceptionToThrow = null;
		boolean result = false;
		QueryExecution qexec = null;

		openTransaction(ReadWrite.READ);
		try {
			qexec = createQueryExecution(sparql, graph);
			result = qexec.execAsk();
		} catch (Exception e) {			
			String message = String.format("Could not execute query\n%s\n against graph(s) %s", 
					qexec != null? qexec.getQuery().serialize(Syntax.syntaxSPARQL) : sparql, graph);
			logger.error(message, e);
			exceptionToThrow = new RuntimeException(message);
			exceptionToThrow.initCause(e);
			dataset.abort();
		} finally {
			TDB.getContext().set(TDB.symUnionDefaultGraph, false);
			closeTransaction();
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
		}
		return result;
	}

	@Override
	public boolean update(String sparql, String... graph) {
		if (graph == null) {
			throw new IllegalArgumentException("'null' not a valid graph.");
		}
		if (sparql == null) {
			throw new IllegalArgumentException("'null' not a valid sparql query.");
		}
		
		RuntimeException exceptionToThrow = null;
		
		//take graphs into account if any are given
		String sparqlG = addGraphsToSparql(sparql, SparqlType.UPDATE, graph);

		openTransaction(ReadWrite.WRITE);
		long diff = 0;
		try {
			long countBefore;

			//ARQ syntax is required for "complex" property paths, see https://jena.apache.org/documentation/query/property_paths.html
			UpdateRequest update = UpdateFactory.create(getSPARQLPrefixes() + sparqlG, Syntax.syntaxARQ);
			logger.debug("{}", update.toString());

			//no originalGraph specified: run on the entire dataset
			if (graph.length < 1) {
				countBefore = getCount();
				UpdateAction.execute(update, dataset);
				diff = getCount() - countBefore;
				//execute on one originalGraph only
			} else if (graph.length == 1) {
				countBefore = dataset.getNamedModel(graph[0]).size();
				UpdateAction.execute(update, dataset.getNamedModel(graph[0]));
				diff = dataset.getNamedModel(graph[0]).size() - countBefore;
				//TODO: investigate if multiple graphs can be addressed in a more efficient way
			} else {
				countBefore = countTriples(dataset);
				UpdateAction.execute(update, dataset);
				diff = countTriples(dataset) - countBefore;
			}

			if (diff == 0) {
				logger.info("Update executed, the number of triples is unchanged");
			} else {
				logger.info("Update executed, {} triple{} {} graph(s) {}",
					Math.abs(diff), Math.abs(diff)>1?"s":"", diff>0?"added to":"deleted from", graph);
			}
			commitTransaction();
		} catch (Exception e) {
			dataset.abort();
			String message = String.format("Could not execute update %s against graph %s", getSPARQLPrefixes() + sparql, graph);
			logger.error(message, e);
			exceptionToThrow = new RuntimeException(message);
			exceptionToThrow.initCause(e);
		} finally {
			closeTransaction();
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
		}
		
		return diff != 0;
	}

	@Override
	public List<Map<String, String>> translateSelectResult(Object results) {

		List<Map<String, String>> res = new ArrayList<>();
		ResultSetRewindable rs = (ResultSetRewindable) results;

		if (rs != null) {
			while (rs.hasNext()) {
				QuerySolution row = rs.nextSolution();
				Map<String, String> map = new HashMap<>();
				for (String var: rs.getResultVars()) {
					if (var != null && row.contains(var)) {
						map.put(var, row.get(var) != null ? SparqlHelper.unescapeLiteral(row.get(var).toString()) : null);
					}
				}
				res.add(map);
			}
		}

		return res;
	}

	/**
	 * Use Jena pre-filtering for the query execution to optimise query speed.
	 * It is assumed that when this method is called, a transaction is already open.
	 *
	 * @param query the query to execute
	 * @param graph the originalGraph(s) to execute the query on, null or no arguments = execute on the entire dataset
	 * @return the query execution object
	 */
	private QueryExecution createQueryExecution(String sparql, String... graphs) {
		if (sparql == null) {
			throw new IllegalArgumentException("'null' not a valid sparql.");
		}
		if (graphs == null) {
			throw new IllegalArgumentException("'null' not a valid graph.");
		}
		
		String sparqlG = sparql;		
		if (graphs.length < 1) {
			TDB.getContext().set(TDB.symUnionDefaultGraph, true);
		} else {
			sparqlG = addGraphsToSparql(sparql, SparqlType.QUERY, graphs);
		}
		
		Query query = QueryFactory.create(getSPARQLPrefixes() + sparqlG, Syntax.syntaxARQ);
		logger.debug("{}", query.toString());
		QueryExecution queryExecution = QueryExecutionFactory.create(query, dataset);
		
		return queryExecution;
	}

	
	
	/**
	 * Since a union's size method returns the amount of graphs, not the the amount of triples, this method
	 * provides said number
	 *
	 * @return the amount of triples in the dataset
	 */
	private long countTriples(Dataset ds) {

		long amount = 0;

		//triples in the default model
		amount += ds.getDefaultModel().size();

		//triples in the graphs
		Iterator<String> it = ds.listNames();
		while (it.hasNext()) {
			amount += ds.getNamedModel(it.next()).size();
		}

		return amount;
	}

	// Other //////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Decompress a (gz) compressed file into a temporary file to facilitate loading
	 *
	 * @param compressed the path of the compressed file
	 * @return the path of the decompressed, temporary file
	 */
	public String decompressGZ(String compressed) {

		String decompressed = compressed;

		try {
			byte[] buffer = new byte[1024];
			try (GZIPInputStream zis = new GZIPInputStream(new FileInputStream(compressed))) {
				File newFile = new File(decompressed.replace(".gz", ""));
				try (FileOutputStream fos = new FileOutputStream(newFile)) {
					int len;
					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}
				}
			}
		} catch (IOException ex) {
			String message = String.format("Could not decompress %s", compressed);
			RuntimeException exception = new RuntimeException(message);
			logger.error(message, ex);
			exception.initCause(ex);
			throw exception;
		}
		
		return decompressed;
	}

	/**
	 * Checks whether a file is compressed
	 *
	 * @param path the path to the file
	 * @return true if it's compressed, false if not
	 */
	public boolean isCompressed(String path) {
		String mimeType = null;
		
		try {
			mimeType = Files.probeContentType(new File(path).toPath());
		} catch (IOException e) {
			logger.error("Could not determine if file at path {} was compressed.", path, e);
		}
		
		
		return mimeType == null? false : (mimeType.contains("gzip"));
	}

	/**
	 * Get the path of an uncompressed file. If the file is already uncompressed, this simply returns the path.
	 * Otherwise, the file will be decompressed. It's the responsibility of the calling method to take care of the
	 * decompressed file (keep it? delete it?)
	 *
	 * @param path the path of the file
	 * @return the path of the uncompressed file
	 */
	public String getUncompressedPath(String path) {

		String uncompressed = path;

		if (isCompressed(path)) {
			uncompressed = decompressGZ(path);
		}

		return uncompressed;
	}

	@Override
	public Dataset loadDataset(String path) {
		logger.info("Loading file {} into dataset", path);
		String uncompressed = getUncompressedPath(path);
		logger.debug("Uncompressed path: {}", path);
		Dataset toLoad = RDFDataMgr.loadDataset(uncompressed);
		return toLoad;
	}

	@Override
	public void loadIntoGraph(String path, String graph, Format format) {
		logger.info("Loading {} from {} into graph <{}>", format, path, graph);

		loadModelHandler(graph, format)
			.loadFromPath(path);
	}

	@Override
	public void loadIntoGraph(InputStream input, String graph, Format format) {
		logger.info("Loading {} from input stream into graph <{}>", format, graph);

		loadModelHandler(graph, format)
			.loadFromStream(input);
	}

	@Override
	public void load(String path) {
		logger.info("Loading file {} into store", path);

		loadDatasetHandler()
			.loadFromPath(path);
	}

	@Override
	public void load(InputStream input) {
		logger.info("Loading from input stream into store");

		loadDatasetHandler()
			.loadFromStream(input);
	}

	@Override
	public void load(String path, String oldURI, String newURI) {
		logger.info("Loading file {} into store ( {}  ->  {} )", path, oldURI, newURI);

		loadDatasetAndRenameHandler(oldURI, newURI)
			.loadFromPath(path);
	}

	@Override
	public void load(InputStream input, String oldURI, String newURI) {
		logger.info("Loading from input stream into store ( {}  ->  {} )", oldURI, newURI);

		loadDatasetAndRenameHandler(oldURI, newURI)
			.loadFromStream(input);
	}

	private LoadHandler loadModelHandler(String graph, Format format) {
		return new LoadHandler(
			stream -> {
				if (graph == null) {
					throw new IllegalArgumentException("Graph null");
				}
				if (format == null) {
					throw new IllegalArgumentException("Format null");
				}

				Lang lang;

				switch (format) {
					case RDF:
						lang = Lang.RDFXML;
						break;
					case TTL:
						lang = Lang.TTL;
						break;
					default:
						throw new IllegalArgumentException("Invalid format: " + format);
				}

				loadModelTransaction(stream, graph, lang);
			}
		);
	}

	private LoadHandler loadDatasetHandler() {
		return new LoadHandler(
			stream -> loadDatasetTransaction(
				stream,
				graph -> new GraphAction() {
					public String getNewGraphName() {
						return graph;
					}

					public void doAction() {}
				}
			)
		);
	}

	private LoadHandler loadDatasetAndRenameHandler(String oldURI, String newURI) {
		return new LoadHandler(
			stream -> {
				if (oldURI == null || oldURI.isEmpty()) {
					throw new IllegalArgumentException("Old URI null or empty");
				}
				if (newURI == null || newURI.isEmpty()) {
					throw new IllegalArgumentException("New URI null or empty");
				}

				loadDatasetTransaction(
					stream,
					graph -> new GraphAction() {
						public String getNewGraphName() {
							return graph.replace(oldURI, newURI);
						}

						public void doAction() {
							String newGraph = getNewGraphName();
							renameURIsInGraph(newGraph, oldURI, newURI);
							logger.debug("Updated references to old URI in graph {}", newGraph);
						}
					}
				);
			}
		);
	}

	private void renameURIsInGraph(String graph, String oldURI, String newURI) {
		update(String.format("DELETE { \n" +
			"  GRAPH <%s> {\n" +
			"    ?old ?ps ?o .\n" +
			"    ?s ?po ?old \n" +
			"  }\n" +
			"}\n" +
			"INSERT { \n" +
			"  GRAPH <%s> {\n" +
			"    ?new ?ps ?o .\n" +
			"    ?s ?po ?new \n" +
			"  }\n" +
			"} WHERE {\n" +
			"  GRAPH <%s> {\n" +
			"    OPTIONAL { ?old ?ps ?o } . \n" +
			"    OPTIONAL { ?s ?po ?old } . \n" +
			"    FILTER CONTAINS(str(?old), '%s') .\n" +
			"    BIND(URI(REPLACE(str(?old), '%s', '%s')) as ?new) . \n" +
			"  }\n" +
			"}", graph, graph, graph, oldURI, oldURI, newURI));
	}

	private interface LoadAction {
		void load(InputStream input);
	}

	private interface GraphAction {
		String getNewGraphName();

		void doAction();
	}

	private class LoadHandler {
		private LoadAction action;

		public LoadHandler(LoadAction action) {
			this.action = action;
		}

		public void loadFromPath(String path) {
			if (path == null || path.isEmpty()) {
				throw new IllegalArgumentException("Path null or empty");
			}

			try {
				if (isCompressed(path)) {
					loadFromStream(new GZIPInputStream(new FileInputStream(path)));
				} else {
					loadFromStream(new FileInputStream(path));
				}
			} catch (IOException e) {
				String message = String.format("Could not load <%s> into store", path);
				logger.error(message, e);
				throw new RuntimeException(message, e);
			}
		}

		public void loadFromStream(InputStream input) {
			if (input == null) {
				throw new IllegalArgumentException("Input stream null");
			}

			//Don't need to alias input with stream in Java 9 onwards
			try (InputStream stream = input) {
				action.load(stream);
			} catch (IOException e) {
				String message = "Failed to close input stream";
				logger.error(message, e);
				throw new RuntimeException(message, e);
			}
		}

	}

	private void loadModelTransaction(InputStream input, String graph, Lang lang) {
		RuntimeException exceptionToThrow = null;

		openTransaction(ReadWrite.WRITE);
		try {
			Model model = ModelFactory.createDefaultModel();
			RDFDataMgr.read(model, input, lang);
			dataset.addNamedModel(graph, model);
			commitTransaction();
			logger.debug("Loaded {} triples into graph {}", model.size(), graph);
		} catch (Exception e) {
			String message = String.format("Could not load input stream into graph <%s>", graph);
			logger.error(message, e);
			exceptionToThrow = new RuntimeException(message, e);
			dataset.abort();
		} finally {
			closeTransaction();
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
		}
	}

	private void loadDatasetTransaction(InputStream input, Function<String, GraphAction> generator) {
		RuntimeException exceptionToThrow = null;

		List<GraphAction> actions = new ArrayList<>();

		openTransaction(ReadWrite.WRITE);
		try {
			Dataset toLoad = DatasetFactory.create();
			RDFDataMgr.read(toLoad, input, Lang.NQUADS);
			Iterator<String> it = toLoad.listNames();
			while (it.hasNext()) {
				String graph = it.next();
				Model model = toLoad.getNamedModel(graph);

				GraphAction graphAction = generator.apply(graph);
				actions.add(graphAction);

				String newGraph = graphAction.getNewGraphName();
				dataset.addNamedModel(newGraph, model);
				logger.info("Loaded {} triples into graph {}", model.size(), newGraph);
			}
			commitTransaction();
		} catch (Exception e) {
			String message = String.format("Could not load input stream into store");
			logger.error(message, e);
			exceptionToThrow = new RuntimeException(message, e);
			dataset.abort();
		} finally {
			closeTransaction();
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
		}

		//perform actions afterwards to avoid nesting transactions
		actions.forEach(action -> action.doAction());
	}

	@Override
	public String export(Format format, String xmlBase, String... graph) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Dataset union;
		RuntimeException exceptionToThrow = null;
		openTransaction(ReadWrite.READ);
		
		try {
			//find out what to export
			if (graph.length == 0) {
				union = dataset;
				logger.info("Exported {} triples from the dataset", dataset.asDatasetGraph().size());
			} else {
				union = DatasetFactory.create();
				long size = 0;
				for (String g : graph) {
					Model next = dataset.getNamedModel(g);
					size += next.size();
					union.addNamedModel(g, next);
					logger.debug("Found {} triples in graph <{}>", next.size(), g);
				}

				logger.info("Exported {} triples from {} graphs", size, graph.length);
			}

			//export it, depending on the format
			if (format.equals(Format.NQ)) {
				//export quads directly
				RDFDataMgr.write(os, union, Lang.NQUADS);
			} else {
				//for triples we need to get rid of the originalGraph part of the quad
				Model tmp = ModelFactory.createDefaultModel();
				Iterator<String> it = union.listNames();
				while (it.hasNext()) {
					tmp.add(union.getNamedModel(it.next()));
				}
				tmp.setNsPrefixes(getPrefixURIMap());
				if (format.equals(Format.RDF)) {

					RDFWriter writer = tmp.getWriter("RDF/XML-ABBREV");
					writer.setProperty("allowBadURIs", "false");
					writer.setProperty("xmlbase", xmlBase);
					//relative URIs seem to be a problem for import statements
					writer.setProperty("relativeURIs", "");
					writer.setProperty("showDoctypeDeclaration", "false");
					writer.setProperty("showXmlDeclaration", "false");
					//set baseURI to null for absolute URIs throughout
					writer.write(tmp, os, null);
				} else if (format.equals(Format.TTL)) {
					RDFDataMgr.write(os, tmp, Lang.TTL);
				} else {
					logger.error("Invalid export format. Please select one of: {}",
							Arrays.asList(IStoreWrapper.Format.values()));
				}
			}
		} catch (RuntimeException e) {
			String message = String.format("Failed to export graphs to format %s", format);		
			logger.error(message, e);
			exceptionToThrow = new RuntimeException(message);
			exceptionToThrow.initCause(e);
			dataset.abort();
		} finally {
			closeTransaction();
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
		}

		return os.toString();
	}

	@Override
	public void save(String filename, Format format, String xmlBase, boolean compressed, String... graph) {
		RuntimeException exceptionToThrow = null;
		File file = new File(filename + "." + format.toString().toLowerCase() + (compressed?".gz":""));
		String content = export(format, xmlBase, graph);

		BufferedWriter writer = null;
		try {
			if (compressed) {
				GZIPOutputStream stream = new GZIPOutputStream(new FileOutputStream(file, false));
				writer = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"));
				writer.append(content);
			} else {
				(new FileOutputStream(file, false)).write(content.getBytes(Charset.forName("UTF-8")));
			}
			logger.info("Saved data in {}compressed file {}", compressed?"":"un", file.getAbsolutePath());
		} catch (IOException e) {	
			String message = String.format("Could not write data to %scompressed file %s", compressed?"":"un", file.getAbsolutePath());
			logger.error(message, e);
			exceptionToThrow = new RuntimeException(message);
			exceptionToThrow.initCause(e);
		} finally {
			if(writer != null){
				try {
					writer.close();
				} catch (IOException ex) {
					logger.error("Could not close stream", ex);
				}
			}
			
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
		}
	}

	@Override
	public void storeModel(Object m, String graph) {
		if (m == null) {
			throw new IllegalArgumentException("'null' not a valid model.");
		}
		if (!(m instanceof Model)) {
			throw new IllegalArgumentException(
					String.format("Argument 'm' must be instance of Model, instead %s was found", m.getClass().toString()));
		}
		if (graph == null) {
			throw new IllegalArgumentException("'null' not a valid graph.");
		}
		
		RuntimeException exceptionToThrow = null;
		Model model = (Model) m;

		logger.debug("Store sizes before storing model: {}", getSizes());

		openTransaction(ReadWrite.WRITE);
		try {
			dataset.addNamedModel(graph, model);
			logger.info("Stored model (size {}) in graph <{}>", model.size(), graph);
			commitTransaction();
		} catch (Exception e) {
			String message = String.format("Could not add model to graph <%s>", graph);
			logger.error(message, e);
			exceptionToThrow = new RuntimeException(message);
			exceptionToThrow.initCause(e);
			dataset.abort();
		} finally {
			closeTransaction();
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
		}

		logger.debug("Store sizes after storing model: {}", getSizes());
	}

	@Override
	public void storeModels(Set<Object> models, String graph) {
		//create a new model and add all the models to it
		Model toStore = ModelFactory.createDefaultModel();
		models.stream().map(o -> (Model) o).forEachOrdered(m -> toStore.add(m));

		//only save it once
		storeModel(toStore, graph);
	}

	@Override
	public void removeModel(Object m, String graph) {
		RuntimeException exceptionToThrow = null;
		Model model = (Model) m;
		long modelSize = model.size();

		openTransaction(ReadWrite.WRITE);
		try {
			long oldSize = dataset.getNamedModel(graph).size();
			dataset.getNamedModel(graph).remove(model);
			logger.info("Removed model (size {}) from graph <{}>, old size: {}, new size: {}",
					modelSize, graph, oldSize, dataset.getNamedModel(graph).size());
			commitTransaction();
		} catch (Exception e) {
			String message = String.format("Could not add model (size %s) to graph <%s>", modelSize, graph);
			logger.error(message, e);
			exceptionToThrow = new RuntimeException(message);
			exceptionToThrow.initCause(e);
			dataset.abort();
		} finally {
			closeTransaction();
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
		}
	}

	public Dataset getDataset() {
		return dataset;
	}
	
	// Transaction handling methods //////////////////////////////////////////////////////////////////////////////////////
	
	private ThreadLocal<Integer> transactionOpenAttempts = new ThreadLocal<Integer>() {
		@Override 
		protected Integer initialValue() {
			return 0;
		}
	};
	
	/**
	 * Opens a transaction if not currently in one. This method should be called to open all
	 * transactions to ensure that an attempt to open a nested transaction (currently unsupported
	 * by Jena) is not made. Variable is ThreadLocal to account for possible case where two threads
	 * have same instance of this class.
	 * 
	 * @param readWrite The type of transaction to open.
	 */
	private void openTransaction(ReadWrite readWrite) {
		if (transactionOpenAttempts.get() == 0) {
			dataset.begin(readWrite);
		} 
		
		transactionOpenAttempts.set(transactionOpenAttempts.get() + 1);
	}
	
	/**
	 * Closes a transaction if it is the highest level transaction (therefore calling this 
	 * operation from a method call within a transaction will not close the transaction).
	 * This ensures that transactions are not closed too early. Variable is ThreadLocal to account 
	 * for possible case where two threads have same instance of this class.
	 */
	private void closeTransaction() {	
		if (transactionOpenAttempts.get() >= 1) {
			if (transactionOpenAttempts.get() == 1) {
				dataset.end();
			}
			transactionOpenAttempts.set(transactionOpenAttempts.get() - 1);
		}
	}
	
	/**
	 * Commits a transaction if it is the highest level transaction (therefore calling this 
	 * operation from a method call within a transaction will not commit the transaction).
	 * This is necessary as the Jena commit operation closes the transaction, which can lead
	 * to transactions being closed too early. 
	 */
	private void commitTransaction() {
		if (transactionOpenAttempts.get() >= 1) {
			if (transactionOpenAttempts.get() == 1) {
				dataset.commit();
			}
			transactionOpenAttempts.set(transactionOpenAttempts.get() - 1);
		}
	}

}
