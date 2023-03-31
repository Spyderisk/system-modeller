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

public class MatchingPattern extends Pattern {
	
	private String rootPattern;
	private final Set<Node> excludedFromLabel;
	private final Set<Node> excludedFromURI;
	private final Set<Node> excludedFromMatching;

	public MatchingPattern() {
		super();
		excludedFromLabel = new HashSet<>();
		excludedFromURI = new HashSet<>();
		excludedFromMatching = new HashSet<>();
	}

	@Override
	public String toString() {
		String s = getLabel() + " <" + getUri() + ">";
		s += "\n\t\tNodes: ";
		s = nodes.stream().map(n -> n.toString()).reduce(s, String::concat);
		s += "\n\t\tLinks: ";
		s = links.stream().map(l -> l.toString()).reduce(s, String::concat);
		return s;
	}

	public String getRootPattern() {
		return rootPattern;
	}

	public void setRootPattern(String rootPattern) {
		this.rootPattern = rootPattern;
	}

	public Set<Node> getExcludedFromLabel() {
		return excludedFromLabel;
	}

	public Set<Node> getExcludedFromURI() {
		return excludedFromURI;
	}

	public Set<Node> getExcludedFromMatching() {
		return excludedFromMatching;
	}

}
