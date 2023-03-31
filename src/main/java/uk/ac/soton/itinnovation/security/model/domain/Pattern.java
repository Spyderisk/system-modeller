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
//      Created By :            Stefanie Cox
//      Created Date :          2017-11-29
//      Created for Project :   SHiELD
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.model.domain;

import java.util.HashSet;
import java.util.Set;
import uk.ac.soton.itinnovation.security.model.SemanticEntity;

public abstract class Pattern extends SemanticEntity {

	protected Set<Node> nodes;

	protected Set<Link> links;

	public Pattern() {
		this.nodes = new HashSet<>();
		this.links = new HashSet<>();
	}

	@Override
	public String toString() {
		String s = getLabel() + " <" + getUri() + ">";
		s += "\n\t\tNodes: ";
		s = nodes.stream().map((n) -> n.toString()).reduce(s, String::concat);
		s += "\n\t\tLinks: ";
		s = links.stream().map((l) -> l.toString()).reduce(s, String::concat);
		return s;
	}

	public Set<Node> getNodes() {
		return nodes;
	}

	public void setNodes(Set<Node> nodes) {
		this.nodes = nodes;
	}

	public Set<Link> getLinks() {
		return links;
	}

	public void setLinks(Set<Link> links) {
		this.links = links;
	}
}
