/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2024
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
//      Created Date :          24/06/2024
//      Created for Project :   Cyberkit4SME
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.AboutDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.AboutLinkDTO;

@RestController
public class AboutController {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Value("${spyderisk.website}")
	private String spyderiskWebsite;

	@Value("${spyderisk.license.link}")
	private String spyderiskLicenseLink;

	@Value("${spyderisk.license.text}")
	private String spyderiskLicenseText;

	@Value("${spyderisk.contributors.link}")
	private String spyderiskContributorsLink;

	@Value("${spyderisk.contributors.text}")
	private String spyderiskContributorsText;

	/**
	 * REST method to GET about info for Spyderisk installation (versions, etc)
	 *
	 * @return an AboutDTO object containing info about Spyderisk installation
	 */
	@GetMapping(value = "/about")
	public ResponseEntity<AboutDTO> getAboutInfo() {

		logger.info("Called REST method to GET about info");

		String spyderiskVersion = System.getenv("SPYDERISK_VERSION");
		String spyderiskCommitSha = System.getenv("SPYDERISK_COMMIT_SHA");
		String spyderiskCommitTimestamp = System.getenv("SPYDERISK_COMMIT_TIMESTAMP");
		String spyderiskAdaptorVersion= System.getenv("SPYDERISK_ADAPTOR_VERSION");

		logger.debug("SPYDERISK_VERSION: {}", spyderiskVersion);
		logger.debug("SPYDERISK_COMMIT_SHA: {}", spyderiskCommitSha);
		logger.debug("SPYDERISK_COMMIT_TIMESTAMP: {}", spyderiskCommitTimestamp);
		logger.debug("SPYDERISK_ADAPTOR_VERSION: {}", spyderiskAdaptorVersion);

		AboutDTO aboutDTO = new AboutDTO();

		aboutDTO.setSpyderiskVersion(spyderiskVersion);
		aboutDTO.setSpyderiskCommitSha(spyderiskCommitSha);
		aboutDTO.setSpyderiskCommitTimestamp(spyderiskCommitTimestamp);
		aboutDTO.setSpyderiskAdaptorVersion(spyderiskAdaptorVersion);

		AboutLinkDTO website = new AboutLinkDTO(spyderiskWebsite);
		AboutLinkDTO license = new AboutLinkDTO(spyderiskLicenseLink, spyderiskLicenseText);
		AboutLinkDTO contributors = new AboutLinkDTO(spyderiskContributorsLink, spyderiskContributorsText);

		aboutDTO.setWebsite(website);
		aboutDTO.setLicense(license);
		aboutDTO.setContributors(contributors);

		return ResponseEntity.status(HttpStatus.OK).body(aboutDTO);
	}

}
