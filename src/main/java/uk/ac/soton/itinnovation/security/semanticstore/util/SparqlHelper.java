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
//      Created Date :          2017-03-27
//      Created for Project :   ASSURED
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.semanticstore.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparqlHelper {

	private static final Logger logger = LoggerFactory.getLogger(SparqlHelper.class);
	private static final String REPLACEMENT = "x";

	private SparqlHelper() {}

	/**
	 * Escape a SPARQL variable name as defined at https://www.w3.org/TR/sparql11-query/#rVARNAME
	 * VARNAME	  ::=  	( PN_CHARS_U | [0-9] ) ( PN_CHARS_U | [0-9] | #x00B7 | [#x0300-#x036F] | [#x203F-#x2040] )*
	 *
	 * @param var the variable
	 * @return the escaped variable
	 */
	public static String escapeVar(String var) {

		//PN_CHARS_BASE	  ::=  	[A-Z] | [a-z] | [#x00C0-#x00D6] | [#x00D8-#x00F6] | [#x00F8-#x02FF] | [#x0370-#x037D] |
		//						[#x037F-#x1FFF] | [#x200C-#x200D] | [#x2070-#x218F] | [#x2C00-#x2FEF] | [#x3001-#xD7FF]
		//						| [#xF900-#xFDCF] | [#xFDF0-#xFFFD] | [#x10000-#xEFFFF]
		//PN_CHARS_U	  ::=  	PN_CHARS_BASE | '_'

		//first character must be letter, number or underscore, following letters can be the same plus
		//various other unicode characters
		Pattern p = Pattern.compile("[\\w][\\w'\\u00C0'-'\\u00D6''\\u00D8'-'\\u00F6''\\u00F8'-'\\u02FF'" +
			"'\\u0370'-'\\u037D''\\u037F'-'\\u1FFF''\\u200C'-'\\u200D''\\u2070'-'\\u218F''\\u2FEF'-'\\u00D6'" +
			"'\\u3001'-'\\uD7FF''\\uF900'-'\\uFDCF''\\uFDF0'-'\\uFFFD''\\u10000'-'\\uEFFFF']*");

		//input is clean
		if (p.matcher(var).matches()) {
			return var;
		} else {
			//input needs to be cleaned
			String clean;

			//check first character is valid
			p = Pattern.compile("[\\w].*");
			if (!p.matcher(var).matches()) {
				logger.debug("A variable name should start with a letter, number or underscore. Prepending '{}'",
					REPLACEMENT);
				clean = REPLACEMENT + var;
			} else {
				logger.debug("Disallowed character(s) detected in input. Replacing any of them with '{}'.", REPLACEMENT);
				//if the problem is that disallowed characters are contained just replace them
				p = Pattern.compile("[^\\w'\\u00C0'-'\\u00D6''\\u00D8'-'\\u00F6''\\u00F8'-'\\u02FF'" +
				"'\\u0370'-'\\u037D''\\u037F'-'\\u1FFF''\\u200C'-'\\u200D''\\u2070'-'\\u218F''\\u2FEF'-'\\u00D6'" +
				"'\\u3001'-'\\uD7FF''\\uF900'-'\\uFDCF''\\uFDF0'-'\\uFFFD''\\u10000'-'\\uEFFFF']");
				clean = p.matcher(var).replaceAll(REPLACEMENT);
			}

			//tell the user
			logger.warn("{} is not a valid variable name. It will be changed to {} but this might break your SPARQL.",
				var, clean);

			return clean;
		}
	}

	/**
	 * Escape a URI to be used as a SPARQL IRI as defined at https://www.w3.org/TR/sparql11-query/#rIRIREF
	 * IRIREF	  ::=  	'<' ([^<>"{}|^`\]-[#x00-#x20])* '>'
	 * Other way of defining this: [^<>"{}|^`#x00-#x20]
	 *
	 * @param uri the URI
	 * @return the escaped URI
	 */
	public static String escapeURI(String uri) {
		
		if (uri==null) {
			logger.warn("<{}> is not a valid URI", uri);
			return "";
		}

		//check for disallowed characters: <>\"{}|^` plus various non-printable characters
		Pattern p = Pattern.compile("[<>\\\\\\\"{}|^`'\u0000'-'\u0020']");
		Matcher m = p.matcher(uri);

		String clean = uri;

		if(m.find()) {
			clean = clean.replaceAll(p.pattern(), REPLACEMENT);

			//tell the user
			logger.warn("<{}> is not a valid URI. It will be changed to <{}> but this might break your SPARQL.",
				uri, clean);
		}

		return clean;
	}

	/**
	 * Escape a string used as a data property or for string comparison.
	 * See www.morelab.deusto.es/code_injection/ for more information or find a sample implementation at
	 * https://github.com/apache/jena/blob/master/jena-arq/src/main/java/org/apache/jena/query/ParameterizedSparqlString.java
	 *
	 * @param s the string to escape
	 * @return the escaped string
	 */
	public static String escapeLiteral(String s) {

		if (s==null || s.isEmpty()) {
			return "";
		}
		
		String safe = s;

		//check for unescaped disallowed characters: currently only " and ', not \ for now ('\\u000A')
		//see https://www.w3.org/TR/n-quads/#sec-literals
		Pattern p = Pattern.compile("[^'\\u005C']['\\u0022''\\u0027']");
		Matcher m = p.matcher(s);

		while(m.find()) {
			String match = m.group();
			//random valid character precedes offending character:

			//retain original preceding character but insert escaping backslash before the second
			//(=offending) character in the match
			safe = safe.replace(match, match.substring(0,1) + "\\" + match.substring(1,2));
		}

		//offending character is the first character in the string
		if (safe.startsWith("\"")) {
			safe = "\\" + safe;
		}
		if (safe.startsWith("'")) {
			safe = "\\" + safe;
		}

		//let the user know
		if (s.length()!=safe.length()) {
			logger.warn("Escaping unescaped disallowed character in input.\nOriginal: {}\nEscaped:  {}", s, safe);
		}

		return safe;
	}

	/**
	 * Unescapes characters escaped by escapeLiteral(). For now, only handles double quote character (").
	 * @param s
	 * @return
	 */
	public static String unescapeLiteral(String s) {
		if (s==null || s.isEmpty()) {
			return "";
		}

		return s.replace("\\\"", "\"");
	}
}
