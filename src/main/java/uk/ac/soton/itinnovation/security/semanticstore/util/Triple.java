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

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a semantic triple including the type (object property, data property, class assertion,...))
 */
public class Triple extends Object {

	private static final Logger logger = LoggerFactory.getLogger(Triple.class);

	protected static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

	private String subject;
	private String predicate;
	private String object;
	private TripleType type;

	/**
	 * Create a triple of a specific type.
	 *
	 * @param subject
	 * @param predicate
	 * @param object
	 * @param type
	 */
	public Triple(String subject, String predicate, String object, TripleType type) {

		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
		this.type = type;

		//attach predicate from type
		if (this.type==TripleType.CLASS_ASSERTION &&  (this.predicate==null || "".equals(this.predicate))) {
			this.predicate = RDF_TYPE;
		//attach type from predicate
		} else if (this.predicate.equals(RDF_TYPE)) {
			this.type = TripleType.CLASS_ASSERTION;
		}
	}

	/**
	 * Test whether a URI is short or long
	 *
	 * @param uri
	 * @return true if it's short, false otherwise
	 */
	public static boolean isShort(String uri) {
		//form either prefix:localName or is not a URI
		//only : and no other special characters
		boolean probablyShort = uri.contains(":") && !uri.contains("#") && !uri.contains("/");
		// no special characters at all - lazy form of local name
		boolean lazyShort = !uri.contains(":") && !uri.contains("#") && !uri.contains("/");
		return probablyShort || lazyShort;
	}

	/**
	 * Split a URI and return just one part of it.
	 *
	 * @param uriOriginal the full URI
	 * @param part which part to return. 0 for the namespace, 1 for the local name
	 * @param includeSeparator whether to include the separator (: for short, # or / for long prefixes)
	 * @return the part to be returned
	 */
	public static String splitURI(String uriOriginal, int part, boolean includeSeparator) {

		String uri = uriOriginal;
		String result = "";
		int splitIndex = -1;

		if (uri.indexOf("#")>0) {
			splitIndex = uri.indexOf("#");
		} else if (uri.lastIndexOf("/")>0) {
			splitIndex = uri.lastIndexOf("/");
		} else if (uri.lastIndexOf(":")>0) {
			splitIndex = uri.lastIndexOf(":");
		} else {
			//plain local name, assume no (home) namespace
			uri = ":" + uri;
			splitIndex = 0;
		}

		if (splitIndex>=0) {
			//get prefix
			if (part==0) {
				if (includeSeparator) {
					result = uri.substring(0, splitIndex + 1).trim();
				} else {
					result = uri.substring(0, splitIndex).trim();
				}
			//get local name
			} else {
				result = uri.substring(splitIndex+1).trim();
			}
		}
		if (result==null) {
			result = "";
		}
		return result;
	}

	/**
	 * Compare two triples. They are equal if they have the same subject, predicate and object.
	 *
	 * @param triple The triple to compare to this one
	 * @return whether the triples are equal
	 */
	@Override
	public boolean equals(Object triple) {
		//TODO: special cases: full prefix is case insensitive but individual name is case sensitive.
		//also try to convert to full URI (using SemanticFactory?) before comparing

		if (triple == null || !triple.getClass().equals(Triple.class)) {
			return false;
		}

		Triple t = (Triple) triple;
		return this.subject.equals(t.getSubject())
				&& this.predicate.equals(t.getPredicate())
				&& this.object.equals(t.getObject());

	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.subject)
				+ Objects.hashCode(this.predicate)
				+ Objects.hashCode(this.object)
				+ Objects.hashCode(this.type);
	}

	/**
	 * Check whether the triple has the given predicate.
	 *
	 * @param pred
	 * @return
	 */
	public boolean hasPredicate(String pred) {
		if (pred == null || predicate == null) {
			return false;
		}
		return predicate.equals(pred);
	}

	/**
	 * Provides a human-readable form of a triple for printing/debugging purposes
	 *
	 * @return the formatted triple
	 */
	@Override
	public String toString() {
		return "[" + getType() + "] " + getSubject() + " " + getPredicate() + " " + getObject();
	}

	//GETTERS/SETTERS//////////////////////////////////////////////////////////////////////////////

	/**
	 * Get the triple's subject
	 *
	 * @return the subject
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * Set the triple's subject
	 *
	 * @param subject the new subject
	 */
	public void setSubject(String subject) {
		this.subject = subject;
	}

	/**
	 * Get the triple's predicate
	 *
	 * @return the predicate
	 */
	public String getPredicate() {
		return predicate;
	}

	/**
	 * Set the triple's predicate
	 *
	 * @param predicate the new predicate
	 */
	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}

	/**
	 * Get the triple's object
	 *
	 * @return the object
	 */
	public String getObject() {
		return object;
	}

	/**
	 * Set the triple's object
	 *
	 * @param object the new object
	 */
	public void setObject(String object) {
		this.object = object;
	}

	/**
	 * Get the triple's object
	 *
	 * @return the object
	 */
	public TripleType getType() {
		return type;
	}

	/**
	 * Set a type for this triple. Possible types are:
	 *
	 * CLASS_ASSERTION:	subject rdf:type Class
	 * OBJECT_PROPERTY:	subject relatedTo object
	 * DATA_PROPERTY: subject hasData "data"^^xsd:type
	 * ANNOTATION_PROPERTY:	subject rdfs:comment "Some description"
	 * UNKNOWN: fallback type. This will make most operations on te triple fail.
	 *
	 * @see TripleType
	 * @param type the type to set
	 */
	public void setType(TripleType type) {
		this.type = type;
	}

	// ENUM ///////////////////////////////////////////////////////////////////////////////////////

	/**
	 * This enum contains the different types of triples that are relevant for EasyJena.
	 */
	public enum TripleType {

		// object rdf:type Class
		CLASS_ASSERTION,
		// object relatedTo otherObject
		OBJECT_PROPERTY,
		// object hasData "data"^^xsd:type
		DATA_PROPERTY,
		// object rdfs:comment "Some description"
		ANNOTATION_PROPERTY,
		// unknown (=invalid)
		UNKNOWN
	}
}
