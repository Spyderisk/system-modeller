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
//      Created Date :          2017-09-29
//      Created for Project :   5G-ENSURE
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.model.domain;

import java.util.HashSet;
import java.util.Set;

public class ValidationPattern extends Pattern {

	private String matchingPattern;
	private final Set<Node> assertedNodes;
	private final Set<Link> assertedLinks;

	public ValidationPattern() {
		super();
		assertedNodes = new HashSet<>();
		assertedLinks = new HashSet<>();
	}

	@Override
	public String toString() {
		String s = getLabel() + " <" + getUri() + ">";
		s += "\n\t\tNodes: ";
		s = nodes.stream().map(n -> n.toString()).reduce(s, String::concat);
		s += "\n\t\tAsserted Nodes: ";
		s = assertedNodes.stream().map(n -> n.toString()).reduce(s, String::concat);
		s += "\n\t\tLinks: ";
		s = links.stream().map(l -> l.toString()).reduce(s, String::concat);
		s += "\n\t\tAsserted Links: ";
		s = assertedLinks.stream().map(l -> l.toString()).reduce(s, String::concat);
		return s;
	}

	public String getMatchingPattern() {
		return matchingPattern;
	}

	public void setMatchingPattern(String matchingPattern) {
		this.matchingPattern = matchingPattern;
	}

	public Set<Node> getAssertedNodes() {
		return assertedNodes;
	}

	public Set<Link> getAssertedLinks() {
		return assertedLinks;
	}

}
