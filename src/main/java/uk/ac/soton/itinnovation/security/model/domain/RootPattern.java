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
//      Created Date :          2017-08-01
//      Created for Project :   SHiELD
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.model.domain;

import java.util.HashSet;
import java.util.Set;
import uk.ac.soton.itinnovation.security.model.SemanticEntity;

public class RootPattern extends SemanticEntity {

	private Set<Node> rootNodes;
	
	private Set<Node> keyNodes;

	private Set<Link> links;

	public RootPattern() {
		this.rootNodes = new HashSet<>();
		this.keyNodes = new HashSet<>();
		this.links = new HashSet<>();
	}

	@Override
	public String toString() {
		String s = getLabel() + " <" + getUri() + ">";
		s = keyNodes.stream().map(n -> "\n\t\tKey:  " + n).reduce(s, String::concat);
		s = rootNodes.stream().map(n -> "\n\t\tRoot: " + n).reduce(s, String::concat);
		s = links.stream().map(l -> "\n\t\t" + l).reduce(s, String::concat);
		return s;
	}

	public Set<Node> getRootNodes() {
		return rootNodes;
	}

	public void setRootNodes(Set<Node> rootNodes) {
		this.rootNodes = rootNodes;
	}

	public Set<Node> getKeyNodes() {
		return keyNodes;
	}

	public void setKeyNodes(Set<Node> keyNodes) {
		this.keyNodes = keyNodes;
	}

	public Set<Link> getLinks() {
		return links;
	}

	public void setLinks(Set<Link> links) {
		this.links = links;
	}
}
