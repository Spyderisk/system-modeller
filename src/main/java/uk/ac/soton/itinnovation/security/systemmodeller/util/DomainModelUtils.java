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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.lingala.zip4j.ZipFile;

public class DomainModelUtils {
	private final static Logger logger = LoggerFactory.getLogger(DomainModelUtils.class);	
	private final static String DEFAULT_DOMAIN_FILE = "domain.nq";
    
	public Map<String, String> extractDomainBundle(String kbInstallFolder, File zipfile, boolean newDomain, String domainUri, String domainModelName) throws IOException {
		logger.info("");
		logger.info("Extracting zipfile: {}", zipfile.getAbsolutePath());

		String tmpdir = Files.createTempDirectory("domain").toFile().getAbsolutePath();
		logger.debug("Created tmp folder: {}", tmpdir);

		String zipfilePath = zipfile.getAbsolutePath();
		String destination = tmpdir;   
		
		ZipFile zipFile = new ZipFile(zipfilePath);
		zipFile.extractAll(destination);
		zipFile.close();

		//List of domain model (.nq) files extracted from zipfile
		ArrayList<String> nqFiles = new ArrayList<>();

		File destinationDir = new File(destination);
		File[] fileList = destinationDir.listFiles();
		if (fileList != null) {
			logger.info("Located nq files:");
			for (File file : fileList) {
				if (!file.isDirectory()) {
					String filename = file.getName();
					if (filename.endsWith(".nq")) {
						logger.info(filename);
						nqFiles.add(filename);
					}
				}
			}
		}

		if (nqFiles.size() == 0) {
			throw new IOException("No domain files (*.nq) located in zipfile: " + zipfilePath);
		}

		String nqFilename;

		//Check for "domain.nq" first, otherwise use first available .nq file
		if (nqFiles.contains(DEFAULT_DOMAIN_FILE)) {
			logger.info("Using default domain file: {}", DEFAULT_DOMAIN_FILE);
			nqFilename = DEFAULT_DOMAIN_FILE;
		}
		else {
			nqFilename = nqFiles.get(0);
			if (nqFiles.size() > 1) {
				logger.warn("Multiple domain nq files found. Using first: {}", nqFilename);
			}
			else {
				logger.info("Using domain file: {}", nqFilename);
			}
		}

		String nqTmpFilepath = destination + File.separator + nqFilename;

		//set domain model file (to be loaded later)
		File domainTmpFile = new File(nqTmpFilepath);

		//Check for domain model file
		if (!domainTmpFile.exists()) throw new IOException("Cannot locate domain model file: " + nqFilename);
		logger.debug("Located domain model file: {}", nqTmpFilepath);

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

		//Define domain model folder path
		String domainModelFolderPath = kbInstallFolder + File.separator + domainModelName;
		logger.info("Installing knowledgebase to folder: {}", domainModelFolderPath);

		//Create domain model folder and delete if exists
		File domainModelFolder = new File(domainModelFolderPath);
		FileUtils.deleteDirectory(domainModelFolder);
		domainModelFolder.mkdirs();

		//extract domain model (domain.nq) from zipfile into domain model folder
		zipFile.extractFile(nqFilename, domainModelFolderPath);

		//extract icon mapping file from zipfile into domain model folder
		zipFile.extractFile(iconMappingFilename, domainModelFolderPath);

		//extract icons folder from zipfile into domain model folder
		zipFile.extractFile(iconsFoldername + "/", domainModelFolderPath);
		zipFile.close();

		File domainFile = new File(domainModelFolderPath, nqFilename);
		if (!domainFile.exists()) throw new IOException("Cannot locate domain model file: " + domainFile.getAbsolutePath());

		iconMappingFile = new File(domainModelFolderPath, iconMappingFilename);
		if (!iconMappingFile.exists()) throw new IOException("Cannot locate icon mapping file: " + iconMappingFile.getAbsolutePath());

		String domainImagesPath = domainModelFolderPath + File.separator + iconsFoldername;

		//define domain images folder
		File domainImagesDir = new File(domainImagesPath);

		if (!domainImagesDir.exists() || !domainImagesDir.isDirectory()) throw new IOException("Cannot locate icons folder: " +
			domainImagesDir.getAbsolutePath());

		logger.debug("Created domain icons folder: {}", domainImagesDir.getAbsolutePath());

		logger.debug("Deleting tmp folder: {}", tmpdir);
		FileUtils.deleteDirectory(new File(tmpdir));

		HashMap<String, String> result = new HashMap<>();
		result.put("domainUri", domainUri);
		result.put("domainModelName", domainModelName);
		result.put("domainModelFolder", domainModelFolderPath);
		result.put("nqFilepath", domainFile.getAbsolutePath());
		result.put("iconMappingFile", iconMappingFile.getAbsolutePath());

		return result;
	}
}
