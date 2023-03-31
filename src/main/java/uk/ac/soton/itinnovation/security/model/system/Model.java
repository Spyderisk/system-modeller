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
//      Created By :            Ken Meacham
//		Modified By :	        Stefanie Cox
//      Created Date :          2016-08-17
//      Created for Project :   ASSURED
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.model.system;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.soton.itinnovation.security.model.Level;

public class Model {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private String uri;

	private String id;

	private String label;

	private String description;

	private String domain;

	private String domainVersion; //current domain version

	private String validatedDomainVersion; //last validated domain version

	private boolean valid;

	private boolean risksValid;

	private RiskCalculationMode riskCalculationMode;

	private boolean validating;

	private boolean calculatingRisk;

	private String user;

	private Date created;

	private Date modified;

	private Map<String, Asset> assets;

	private Set<Relation> relations;

	private Level risk;

	public Model() {
		//an empty model should be valid by default
		valid = true;
		risksValid = true;
		//no calculations are currently in progress
		validating = false;
		calculatingRisk = false;
	}

	public Model(String name, String description, String domain, String domainVersion, String validatedDomainVersion, boolean valid, String user, Date created) {
		this();
		this.label = name;
		this.description = description;
		this.domain = domain;
		this.domainVersion = domainVersion;
		this.validatedDomainVersion = validatedDomainVersion;
		this.valid = valid;
		this.user = user;
		this.created = created;
		this.modified = created;
	}

	public Model(String name, String description, String domain, String domainVersion, String validatedDomainVersion, boolean valid, String user, Date created, Date modified) {
		this(name, description, domain, domainVersion, validatedDomainVersion, valid, user, created);
		this.modified = modified;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getDomainVersion() {
		return domainVersion;
	}

	public void setDomainVersion(String domainVersion) {
		this.domainVersion = domainVersion;
	}

	public String getValidatedDomainVersion() {
		return validatedDomainVersion;
	}

	public void setValidatedDomainVersion(String validatedDomainVersion) {
		//logger.debug("setValidatedDomainVersion: {}, ", validatedDomainVersion);
		this.validatedDomainVersion = validatedDomainVersion;
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

	public boolean getValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public boolean isRisksValid() {
		return risksValid;
	}

	public void setRisksValid(boolean risksValid) {
		this.risksValid = risksValid;
	}

	public boolean isValidating() {
		return validating;
	}

	public void setValidating(boolean validating) {
		this.validating = validating;
	}

	public boolean isCalculatingRisk() {
		return calculatingRisk;
	}

	public void setCalculatingRisk(boolean calculatingRisk) {
		this.calculatingRisk = calculatingRisk;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public Map<String, Asset> getAssets() {
		return assets;
	}

	public Set<Relation> getRelations() {
		return relations;
	}

	public Level getRisk() {
		return risk;
	}

	public void setRisk(Level risk) {
		this.risk = risk;
	}

	public RiskCalculationMode getRiskCalculationMode() {
		return riskCalculationMode;
	}

	public void setRiskCalculationMode(RiskCalculationMode riskCalculationMode) {
		this.riskCalculationMode = riskCalculationMode;
	}

	@Override
	public String toString() {
		return "Model " + label + " <" + uri + ">:\n" +
				"Description: " + description + "\n" +
				"Domain:      " + domain + 
				"Domain version: " + domainVersion +
				"Validated domain version: " + validatedDomainVersion;
	}

}
