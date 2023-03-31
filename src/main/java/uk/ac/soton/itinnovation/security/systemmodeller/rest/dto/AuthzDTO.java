/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2017
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
//  Created By :            Anna Brown
//  Created Date :          13/07/2021
//  Created for Project :   Spyderisk Accelerator
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.systemmodeller.rest.dto;
import uk.ac.soton.itinnovation.security.systemmodeller.model.ModelACL;
import java.util.Set;

public class AuthzDTO {
    private Set<String> readUsernames;
    private Set<String> writeUsernames;
    private Set<String> ownerUsernames;
	private String noRoleUrl;
	private String readUrl;
	private String writeUrl;
	private String ownerUrl;

	public AuthzDTO() {}

	public AuthzDTO(ModelACL modelACL){
		this.readUsernames = modelACL.getReadUsernames();
		this.writeUsernames = modelACL.getWriteUsernames();
		this.ownerUsernames = modelACL.getOwnerUsernames();
		this.noRoleUrl = modelACL.getNoRoleUrl();
		this.readUrl = modelACL.getReadUrl();
		this.writeUrl = modelACL.getWriteUrl();
		this.ownerUrl = modelACL.getOwnerUrl();
	}

	public String getNoRoleUrl() {
		return noRoleUrl;
	}

	public void setNoRoleUrl(String noRoleUrl) {
		this.noRoleUrl = noRoleUrl;
	}

	public String getReadUrl() {
		return readUrl;
	}

	public void setReadUrl(String readUrl) {
		this.readUrl = readUrl;
	}

	public Set<String> getReadUsernames() {
		return readUsernames;
	}

	public void addReadUsername(String readUsername){
		this.readUsernames.add(readUsername);
	}

	public void setReadUsernames(Set<String> readUsernames) {
		this.readUsernames = readUsernames;
	}

	public String getWriteUrl() {
		return writeUrl;
	}

	public void setWriteUrl(String writeUrl) {
		this.writeUrl = writeUrl;
	}

	public Set<String> getWriteUsernames() {
		return writeUsernames;
	}

	public void addWriteUsername(String writeUsername){
		this.writeUsernames.add(writeUsername);
	}

	public void setWriteUsernames(Set<String> writeUsernames) {
		this.writeUsernames = writeUsernames;
	}

	public Set<String> getOwnerUsernames() {
		return ownerUsernames;
	}

	public void addOwnerUsername(String ownerUsername){
		this.ownerUsernames.add(ownerUsername);
	}

	public void setOwnerUsernames(Set<String> ownerUsernames) {
		this.ownerUsernames = ownerUsernames;
	}

	public String getOwnerUrl() {
		return ownerUrl;
	}

	public void setOwnerUrl(String ownerUrl) {
		this.ownerUrl = ownerUrl;
	}

}
