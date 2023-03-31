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
//      Created By :            Oliver Hayes
//      Created Date :          2017-08-21
//      Created for Project :   5G-ENSURE
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest.dto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;

public class LoadingProgress {
	private final Logger logger = LoggerFactory.getLogger(LoadingProgress.class);
	private String modelId;
	private double progress;
	private String message;
	private String status;
	private String error;
	private Model model;

	public LoadingProgress(String modelId) {
		this.modelId = modelId;
		this.progress = 0.0D;
		this.message = "Not currently validating";
		this.model = null;
		setStatus("inactive");
		setError("");
	}

	public void updateProgress(double progress, String message) {
		//this.logger.info("Updating loading progress: " + progress);
		String status = "loading"; // default status must be loading if making progress
		String error = "";
		this.updateProgress(progress, message, status, error, null);
	}

	public void updateProgress(double progress, String message, String status) {
		String error = "";
		this.updateProgress(progress, message, status, error, null);
	}

	public void updateProgress(double progress, String message, Model model) {
		String error = "";
		this.updateProgress(progress, message, status, error, model);
	}

	public void updateProgress(double progress, String message, String status, String error, Model model) {
		this.progress = progress;
		this.message = message;
		this.status = status;
		this.error = error;
		this.model = model;
		if ("failed".equals(status)) {
			logger.error("Loading failed: {}", error);
		}
		else {
			logger.debug("Updated loading progress: {}, {} ({})", getProgress(), message, status);
		}
	}

	public String getModelId() {
		return this.modelId;
	}

	public void setModelId(String modelId) {
		this.modelId = modelId;
	}

	public double getProgress() {
		return this.progress;
	}

	public void setProgress(double progress) {
		this.progress = progress;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getStatus() {
		return status;
	}

	public final void setStatus(String status) {
		this.status = status;
	}

	public String getError() {
		return error;
	}

	public final void setError(String error) {
		this.error = error;
	}

	public Model getModel() {
		return model;
	}

	public void setModel(Model model) {
		this.model = model;
	}
	
}
