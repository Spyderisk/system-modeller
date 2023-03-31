/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2018
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
//      Created By :          Ken Meacham
//      Modified By :         Ken Meacham
//      Created Date :        2018-03-09
//      Created for Project : SHIELD
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest.dto;

import java.util.Objects;

/**
 * This is a base class for all semantic classes in the security object model
 */
public abstract class SemanticEntityDTO implements Comparable<SemanticEntityDTO> {

	private String uri;

	private String label;

	private String description;

	public SemanticEntityDTO() {
	}

	/**
	 * Get the ID for this semantic entity. The ID is a hexadecimal string generated from the hash code for this entity.
	 * While each semantic entity may choose to implement their own hashCode, the default is to compare by URI: if the
	 * URI matches, it is the same entity. This is not only useful but needed as a unique identifier that can (as
	 * opposed to a URI) be used *in* a URI. It's much shorter (8 characters) but still sufficiently unique.
	 *
	 * @return the ID
	 */
	public String getID() {
		return Integer.toHexString(hashCode());
	}

	/**
	 * Returns the entity's fully qualified URI
	 *
	 * @return the URI
	 */
	public final String getUri() {
		return uri;
	}

	/**
	 * Sets the entity's fully qualified URI.
	 *
	 * @param uri the new URI for this entity
	 */
	public final void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * Get the entity's rdfs:label
	 *
	 * @return the actual label string without the XSD datatype if it was attached - it's always xsd:string
	 */
	public final String getLabel() {

		/*
		//if there is no URI or there is a label return the label
		if (label != null || uri == null) {
			return label;
			//fallback: use the "local" part of the URI
		} else {
			if (uri.contains("#")) {
				return uri.substring(uri.lastIndexOf("#") + 1);
			} else {
				return uri.substring(uri.lastIndexOf("/") + 1);
			}
		}
		*/
		return label;
	}

	/**
	 * Set the entity's rdfs:label
	 *
	 * @param label the actual label string without the XSD datatype - it's always xsd:string
	 */
	public final void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Get the entity's rdfs:comment, i.e. the description
	 *
	 * @return the actual description String without the XSD datatype if it was attached - it's always xsd:string
	 */
	public final String getDescription() {
		return description;
	}

	/**
	 * Set the entity's rdfs:comment, i.e. the description
	 *
	 * @param description the actual description string without the XSD datatype - it's always xsd:string
	 */
	public final void setDescription(String description) {
		this.description = description;
	}

	@Override
	public boolean equals(Object other) {

		if (other == null) {
			return false;
		}

		if (this.getClass() != other.getClass()) {
			return false;
		}

		if (SemanticEntityDTO.class.isAssignableFrom(other.getClass())) {
			return uri.equals(((SemanticEntityDTO) other).getUri());
		}

		return false;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 13 * hash + Objects.hashCode(this.uri);
		return hash;
	}

	@Override
	public int compareTo(SemanticEntityDTO other) {
		return other!=null?uri.compareTo(other.getUri()):1;
	}

	@Override
	public String toString() {
		return (label!=null?label + " ":"") + (uri!=null?"<" + uri + ">":"<undefined URI>");
	}
}
