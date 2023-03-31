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
//      Created By :            Josh Harris
//      Created Date :          18/07/2017
//      Created for Project :   SHIELD
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelquerier.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jh17
 */
public class TemplateLoader {

	private static final Logger logger = LoggerFactory.getLogger(TemplateLoader.class);

	private TemplateLoader() {}

	/**
	 * Takes the name of a file and returns the contents of that sparql file with the
	 * variables (%s in the file) replaced with those in the args list
	 * @param templateName name of the file
	 * @param args list of arguments to change in the template
	 * @return the sparql query from the file
	 */
	public static String loadTemplate(String templateName, String ... args){
		return loadTemplate(templateName, TemplateLoader.class, args);
	}

	/**
	 * Takes the name of a file and returns the contents of that sparql file with the
	 * variables (%s in the file) replaced with those in the args list
	 * @param templateName name of the file
	 * @param c the class of anything to find the class loader to locate the files
	 * @param args list of arguments to change in the template
	 * @return the sparql query from the file
	 */
	public static String loadTemplate(String templateName, Class<? extends Object> c, String ... args){
		String sparql = null;

		try{
			//Reads in the file into a string
			String tmp = IOUtils.toString(c.getClass().getResourceAsStream("/sparql/" + templateName + ".sparql"));

			sparql = formatTemplate(tmp, args);

		} catch (IOException | NullPointerException ex) {
			logger.error("Template {} not found", templateName, ex);
		}

		return sparql;
	}

	/**
	 * Formats a sparql query by replacing the %s markers in the template with the
	 * list of arguments passed to it
	 * @param template the template filled with %s symbols
	 * @param args the list of arguments used
	 * @return the sparql query that is ready to be fired at the store
	 */
	public static String formatTemplate(String template, String ... args){

		String formatted = template;
		//Replace the placeholders in the file with the given arguments
		if (args!=null && args.length>0) {
			formatted = String.format(formatted, (Object[]) args);
		}

		//Trims white space off of the query
		return formatted.trim();
	}

	/**
	 *
	 * @param directory
	 * @return
	 */
	public static Map<String, String> loadTemplateMap(String directory){
		return loadTemplateMap(directory, TemplateLoader.class);
	}

	/**
	 *
	 * @param directory
	 * @param c
	 * @return
	 */
	public static Map<String, String> loadTemplateMap(String directory, Class<? extends Object> c){
		Map<String, String> templates = new HashMap<>();

		File dir = new File(directory);
		File[] fileList = dir.listFiles();
		if (fileList!=null) {
			for (File file : fileList) {
				if (!file.isDirectory()) {
					try {
						try (FileReader fr = new FileReader(directory + "/" + file.getName())) {
							String query = IOUtils.toString(fr);
							templates.put(file.getName().replace(".sparql", ""), query);
						}
					} catch(IOException e) {
						logger.error("Could not find {}", file.getName(), e);
					}
				}
			}

			return templates;
		} else {
			logger.error("{} is not a directory", directory);
			return null;
		}
	}
}
