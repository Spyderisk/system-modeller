/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2017
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
//		Created By :			Ken Meacham
//		Modified By:
//		Created Date :			2017-06-20
//		Created for Project :	ASSURED
//		Modified for Project:
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelvalidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class to track validation progress, ultimately to be displayed in a UI
 */
public class Progress {

	private final Logger logger = LoggerFactory.getLogger(Progress.class);

	private String modelId;

	private double progress;

	private String message;

	private String status;

	private String error;

	/**
	 * Create a new ValidationProgress for the given model
	 *
	 * @param modelId the ID of the model that's being validated
	 */
	public Progress(String modelId) {

		setModelId(modelId);
		setProgress(0d);
		setMessage("Not currently running");
		setStatus("inactive");
		setError("");
	}

	/**
	 * Create a new ValidationProgress for the given model
	 *
	 * @param modelId the ID of the model that's being validated
	 * @param progress the initial progress
	 * @param message an initial message to display
	 */
	public Progress(String modelId, double progress, String... message) {
		this(modelId);
		setProgress(progress);
		if (message != null && message.length > 0) {
			setMessage(message[0]);
		}
		setStatus("inactive");
		setError("");
	}

	@Override
	public String toString() {
		return "Current progress for model " + modelId + ": " + progress + ", " + message;
	}

	/**
	 * Update the progress
	 *
	 * @param progress the new progress
	 * @param message a new message to display
	 */
	public void updateProgress(double progress, String message) {
		String newStatus = "running"; // default status must be running if making progress
		String newError = "";
		this.updateProgress(progress, message, newStatus, newError);
	}

	/**
	 * Update the progress
	 *
	 * @param progress the new progress
	 * @param message a new message to display
	 * @param status the new status
	 */
	public void updateProgress(double progress, String message, String status) {
		this.updateProgress(progress, message, status, "");
	}

	/**
	 * Update the progress and display an error
	 *
	 * @param progress the new progress
	 * @param message a new message to display
	 * @param status the new status
	 * @param error the error to display
	 */
	public void updateProgress(double progress, String message, String status, String error) {

		setProgress(progress);
		setMessage(message);
		setStatus(status);
		setError(error);
		if ("failed".equals(status)) {
			logger.error("Process failed: {}", error);
		} else {
			logger.debug("Updated progress: {}, {} ({})", getProgress(), message, status);
		}
	}

	// Getters/Setters ////////////////////////////////////////////////////////////////////////////////////////////////
	public String getModelId() {
		return modelId;
	}

	public final void setModelId(String modelId) {
		this.modelId = modelId;
	}

	public double getProgress() {
		return progress;
	}

	public final void setProgress(double progress) {
		//we don't round here - that's the job of whatever piece of code renders this in a UI
		this.progress = progress;
	}

	public String getMessage() {
		return message;
	}

	public final void setMessage(String message) {
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
}
