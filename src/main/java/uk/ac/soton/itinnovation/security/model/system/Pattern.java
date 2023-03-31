/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2016
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
//      Created By :            Gianluca Correndo
//      Created Date :          20 Sep 2016
//		Modified By :	        Stefanie Cox
//      Created for Project :   5G-Ensure
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.model.system;

import java.util.HashSet;
import java.util.Set;
import uk.ac.soton.itinnovation.security.model.SemanticEntity;

public class Pattern extends SemanticEntity {

	protected String parent;

	protected String parentLabel;

	protected final Set<Node> nodes;

	protected final Set<Link> links;

	public Pattern() {
		this.nodes = new HashSet<>();
		this.links = new HashSet<>();
	}

	public Pattern(String uri, String label) {

		this();
		setUri(uri);
		setLabel(label);
	}

	@Override
	public String toString() {
		String s = getLabel() + " <" + getUri() + ">, parent <" + parent + ">\n\t\tnodes:";
		s = nodes.stream().map(n -> "\n\t\t\t" + n).reduce(s, String::concat);
		s += "\n\t\tlinks:";
		s = links.stream().map(l -> "\n\t\t\t" + l).reduce(s, String::concat);
		return s;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public String getParentLabel() {
		return parentLabel;
	}

	public void setParentLabel(String parentLabel) {
		this.parentLabel = parentLabel;
	}

	public Set<Node> getNodes() {
		return nodes;
	}

	public Set<Link> getLinks() {
		return links;
	}
}
