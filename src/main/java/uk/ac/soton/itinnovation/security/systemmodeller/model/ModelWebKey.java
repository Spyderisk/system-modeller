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
//      Created By :          Anna Brown
//      Created Date :        30/06/2021
//      Created for Project : SPYDERISK Accelerator
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.model;
import org.springframework.stereotype.Component;
import uk.ac.soton.itinnovation.security.systemmodeller.model.WebKeyRole;


@Component
public class ModelWebKey {

	private String url;
    private WebKeyRole role;

	public ModelWebKey(){

	}

	public ModelWebKey(WebKeyRole role) {
		this.role = role;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public WebKeyRole getRole() {
		return role;
	}

	public void setRole(WebKeyRole role) {
		this.role = role;
	}

	@Override
    public String toString() {
		if (url == null || role == null) {
			return null;
		}
        return String.format("Role: " + role + ", URL: " + url);
    }
}
