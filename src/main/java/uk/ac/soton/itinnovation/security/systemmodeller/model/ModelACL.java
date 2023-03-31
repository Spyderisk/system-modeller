/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2020
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
//      Created By :          Toby Wilkinson
//      Created Date :        27/11/2020
//      Created for Project : ZDMP
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import uk.ac.soton.itinnovation.security.systemmodeller.mongodb.AbstractDocument;

@Document(collection = "ModelACL")
public class ModelACL extends AbstractDocument {

	private String uri;

	private String domainGraph;

	// TODO -- could use annotations but the current implementation updates these values
	// by hand -- need to determine if there is any reason for this 
	//@CreatedDate
	private Date created;

	// TODO -- could use annotations but the current implementation updates these values
	// by hand -- need to determine if there is any reason for this 
	//@CreatedBy
	private String userId;

	private String userName;

	// TODO -- could use annotations but the current implementation updates these values
	// by hand -- need to determine if there is any reason for this 
	//@LastModifiedDate
	private Date modified;

	// Spring Data auditing enabled in ModelAuditor and MongoConfig
	@LastModifiedBy
	private String modifiedBy;

	private String modifiedByName;

	@Indexed
	private ModelWebKey noRoleUrl;

	@Indexed
	private ModelWebKey readUrl;

	private Set<String> readUsernames;

	@Indexed
	private ModelWebKey writeUrl;

	private Set<String> writeUsernames;

	@Indexed
	private ModelWebKey ownerUrl;
	
	private Set<String> ownerUsernames;

	private String editorId;

	private String editorName;

	// validating and calculatingRisk flags:
	//
	// There are equivalent flags stored within the model itself.
	// That is a mistake, as these flags control what actions can
	// be perfomed on the model, rather than being part of the
	// model, so we ignore them and store them in Mongo.
	private Boolean validating;

	private Boolean calculatingRisk;

	public ModelACL() {
		this.readUsernames = new HashSet<>();
		this.writeUsernames = new HashSet<>();
		this.ownerUsernames = new HashSet<>();
		this.noRoleUrl = new ModelWebKey(WebKeyRole.NONE);
		this.readUrl = new ModelWebKey(WebKeyRole.READ);
		this.writeUrl = new ModelWebKey(WebKeyRole.WRITE);
		this.ownerUrl = new ModelWebKey(WebKeyRole.OWNER);

		this.validating = false;
		this.calculatingRisk = false;
		this.userName = null;
		this.editorName = null;
		this.modifiedByName = null;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
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

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public Date getModified() {
		return modified;
	}

	public void setModified(Date modified) {
		this.modified = modified;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public String getModifiedByName() {
		return modifiedByName;
	}

	public void setModifiedByName(String modifiedByName) {
		this.modifiedByName = modifiedByName;
	}

	public String getNoRoleUrl() {
		return noRoleUrl.getUrl();
	}

	public void setNoRoleUrl(String noRoleUrl) {
		this.noRoleUrl.setUrl(noRoleUrl);
	}

	public String getReadUrl() {
		return readUrl.getUrl();
	}

	public void setReadUrl(String readUrl) {
		this.readUrl.setUrl(readUrl);
	}

	public Set<String> getReadUsernames() {
		return readUsernames;
	}

	public void setReadUsernames(Set<String> readUsernames) {
		this.readUsernames = readUsernames;
	}

	public String getWriteUrl() {
		return writeUrl.getUrl();
	}

	public void setWriteUrl(String writeUrl) {
		this.writeUrl.setUrl(writeUrl);
	}

	public Set<String> getWriteUsernames() {
		return writeUsernames;
	}

	public void setWriteUsernames(Set<String> writeUsernames) {
		this.writeUsernames = writeUsernames;
	}

	public Set<String> getOwnerUsernames() {
		return ownerUsernames;
	}

	public void setOwnerUsernames(Set<String> ownerUsernames) {
		this.ownerUsernames = ownerUsernames;
	}

	public String getOwnerUrl() {
		return ownerUrl.getUrl();
	}

	public void setOwnerUrl(String ownerUrl) {
		this.ownerUrl.setUrl(ownerUrl);
	}

	public String getEditorId() {
		return editorId;
	}

	public void setEditorId(String editorId) {
		this.editorId = editorId;
	}

	public String getEditorName() {
		return editorName;
	}

	public void setEditorName(String editorName) {
		this.editorName = editorName;
	}

	public boolean isValidating() {
		return validating;
	}

	public void setValidating(boolean validating) {
		if (validating && isValidating()) {
			throw new IllegalStateException("Cannot set validating: already validating");
		}
		if (!validating && !isValidating()) {
			throw new IllegalStateException("Cannot clear validating: not validating");
		}
		if (isCalculatingRisk()) {
			throw new IllegalStateException("Cannot set validating: already calculating risks");
		}

		this.validating = validating;
	}

	public boolean isCalculatingRisk() {
		return calculatingRisk;
	}

	public void setCalculatingRisk(boolean calculatingRisk) {
		if (calculatingRisk && isCalculatingRisk()) {
			throw new IllegalStateException("Cannot set calculatingRisk: already calculating risks");
		}
		if (!calculatingRisk && !isCalculatingRisk()) {
			throw new IllegalStateException("Cannot clear calculatingRisk: not calculating risks");
		}
		if (isValidating()) {
			throw new IllegalStateException("Cannot set calculatingRisk: already validating");
		}

		this.calculatingRisk = calculatingRisk;
	}

	@Override
	public String toString() {
		return
			"\nid: " + getId() +
			"\nuri: " + uri +
			"\ndomainGraph: " + domainGraph +
			"\ncreated: " + created +
			"\nuserId: " + userId +
			"\nmodified: " + modified +
			"\nmodifiedBy: " + modifiedBy +
			"\nnoRoleUrl: " + noRoleUrl +
			"\nreadUrl: " + readUrl +
			"\nreadUsernames: " + readUsernames +
			"\nwriteUrl: " + writeUrl +
			"\nwriteUsernames: " + writeUsernames +
			"\nownerUrl: " + ownerUrl +
			"\nownerUsernames: " + ownerUsernames +
			"\neditorId: " + editorId +
			"\nvalidating: " + validating +
			"\ncalculatingRisk: " + calculatingRisk;
	}
}
