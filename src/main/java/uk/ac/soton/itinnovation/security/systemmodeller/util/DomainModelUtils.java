/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2023
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
//      Created Date :          20 Apr 2023
//      Created for Project :   CyberKit4SME
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.systemmodeller.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.lingala.zip4j.ZipFile;

public class DomainModelUtils {
	private final static Logger logger = LoggerFactory.getLogger(DomainModelUtils.class);	
    
	public Map<String, String> extractDomainBundle(File f, boolean newDomain, String domainUri, String domainModelName) throws IOException {
		logger.info("Extracting zipfile: {}", f.getAbsolutePath());

		String tmpdir = Files.createTempDirectory("domain").toFile().getAbsolutePath();
		logger.debug("Created tmp folder: {}", tmpdir);

		String source = f.getAbsolutePath();
		String destination = tmpdir;   
		
		ZipFile zipFile = new ZipFile(source);
		zipFile.extractAll(destination);
		zipFile.close();

		String nqFilename = "domain.nq";
		String nqFilepath = destination + File.separator + nqFilename;

		//set domain model file (to be loaded later)
		f = new File(nqFilepath);

		//Check for domain model file
		if (!f.exists()) throw new IOException("Cannot locate domain model file: " + nqFilename);
		logger.debug("Located domain model file: {}", nqFilepath);

		//Check for icon mapping file
		String iconMappingFilename = "icon-mapping.json";
		File iconMappingFile = new File(destination + File.separator + iconMappingFilename);
		if (!iconMappingFile.exists()) throw new IOException("Cannot locate icon mapping file: " + iconMappingFilename);
		logger.debug("Located icon mapping file: {}", iconMappingFile.getAbsolutePath());

		//Check for icons folder
		String iconsFoldername = "icons";
		File iconsFolder = new File(destination + File.separator + iconsFoldername);
		if (!iconsFolder.exists() || !iconsFolder.isDirectory()) throw new IOException("Cannot locate icons folder");
		logger.debug("Located icons folder: {}", iconsFolder.getAbsolutePath());

		if (newDomain) {
			//Determine domain URI from icons mapping file 
			domainUri = PaletteGenerator.getDomainUri(new FileInputStream(iconMappingFile));
			domainModelName = domainUri.substring(domainUri.lastIndexOf('/') + 1);
			logger.debug("domainUri: {}", domainUri);
			logger.debug("domainModelName: {}", domainModelName);
		}

		String imagesDir = this.getClass().getResource("/static/images/").getPath();
		String domainImagesPath = imagesDir + domainModelName;

		//extract icons folder from zipfile into /images folder
		zipFile.extractFile(iconsFoldername + "/", imagesDir);

		//locate extracted icons folder
		File iconsDir = new File(imagesDir + "icons");

		//define domain images folder and delete if exists
		File domainImagesDir = new File(domainImagesPath);
		FileUtils.deleteDirectory(domainImagesDir);

		//rename icons folder to domain images folder
		iconsDir.renameTo(domainImagesDir);

		if (!domainImagesDir.exists() || !domainImagesDir.isDirectory()) throw new IOException("Cannot locate icons folder: " +
			domainImagesDir.getAbsolutePath());

		logger.debug("Created domain icons folder: {}", domainImagesDir.getAbsolutePath());

		HashMap<String, String> result = new HashMap<>();
		result.put("domainUri", domainUri);
		result.put("domainModelName", domainModelName);
		result.put("nqFilepath", f.getAbsolutePath());
		result.put("iconMappingFile", iconMappingFile.getAbsolutePath());

		return result;
	}
}
