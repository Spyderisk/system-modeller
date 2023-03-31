/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2018
//
// Copyright in this library belongs to the University of Southampton
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
//      Created Date :        2018-10-23
//      Created for Project : SHIELD
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest.dto;

import java.util.Date;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;

public class UpdateModelResponse {
	private String id;
	private String name;
	private String description;
	private String domainGraph;
	private Date created;
	private Date modified;
	
	public UpdateModelResponse(Model model) {
		this.id = model.getNoRoleUrl();
		this.name = model.getName();
		this.description = model.getDescription();
		this.domainGraph = model.getDomainGraph();
		this.created = model.getCreated();
		this.modified = model.getModified();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDomainGraph() {
		return domainGraph;
	}

	public void setDomainGraph(String domainGraph) {
		this.domainGraph = domainGraph;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getModified() {
		return modified;
	}

	public void setModified(Date modified) {
		this.modified = modified;
	}

}
