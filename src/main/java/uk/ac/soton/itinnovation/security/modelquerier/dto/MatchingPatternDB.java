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
package uk.ac.soton.itinnovation.security.modelquerier.dto;

import java.util.ArrayList;
import java.util.Collection;

import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class MatchingPatternDB extends EntityDB {
	public MatchingPatternDB() {
		// Defaults
		this.uniqueNodes = new ArrayList<>();
		this.mandatoryNodes = new ArrayList<>();
		this.necessaryNodes = new ArrayList<>();
		this.sufficientNodes = new ArrayList<>();
		this.prohibitedNodes = new ArrayList<>();
		this.optionalNodes = new ArrayList<>();
		this.prohibitedLinks = new ArrayList<>();
		this.distinctNodeGroups = new ArrayList<>();
		this.links = new ArrayList<>();
		this.nodes = new ArrayList<>();
	}
	
	/*
	 * Both domain and system model variants have a label. The system model variant has
	 * a parent field. The domain model variant has a description but this is used only
	 * by the domain model editor.
	 */
	@SerializedName("rdfs#label")
	protected String label;
	protected String parent;
	
	@SerializedName("hasRootPattern")
	private String rootPattern;							// URI of the associated root pattern in domain/system model patterns
	private Collection<String> uniqueNodes;				// URIs of unique nodes in system model patterns
	@SerializedName("hasMandatoryNode")
	private Collection<String> mandatoryNodes;			// URIs of all non-unique, mandatory nodes in domain model patterns (will be deprecated)
	@SerializedName("hasNecessaryNode")
	private Collection<String> necessaryNodes;			// URIs of non-unique, mandatory and necessary nodes in domain/system model patterns
	@SerializedName("hasSufficientNode")
	private Collection<String> sufficientNodes;			// URIs of non-unique, mandatory but sufficient nodes in domain/system model patterns
	@SerializedName("hasProhibitedNode")
	private Collection<String> prohibitedNodes;			// URIs of prohibited nodes in domain model patterns (there will be none in system model patterns)
	@SerializedName("hasOptionalNode")
	private Collection<String> optionalNodes;			// URIs of non-unique, optional nodes in domain/system model patterns
	@SerializedName("hasProhibitedLink")
	private Collection<String> prohibitedLinks;			// URIs of prohibited links in domain model patterns (there will be none in system model patterns)
	@SerializedName("hasDistinctNodeGroup")
	private Collection<String> distinctNodeGroups;		// URIs of distinct node groups in domain model patterns
	@SerializedName("hasLink")
	private Collection<String> links;					// URIs of mandatory links in domain/system model patterns
	@SerializedName("hasNode")
	private Collection<String> nodes;					// URIs of all nodes in system model patterns

	// Cardinality represented by a reference to a level in the new PopulationLevel scale
	private String population;
	
}
