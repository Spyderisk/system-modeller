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
//      Created By :            Gianluca Correndo
//      Created Date :          19 Sep 2016
//		Modified By :	        Stefanie Cox, Josh Harris
//      Created for Project :   5G-Ensure
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.model.system;

import java.lang.IllegalArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.model.SemanticEntity;

public class ControlSet extends SemanticEntity {

	public static final Logger logger = LoggerFactory.getLogger(ControlSet.class);

	private String control;

	private String assetUri;

	private String assetId;

	private boolean proposed;

	private boolean assertable;

	private boolean workInProgress;

	private String coverageLevel;

	private boolean coverageAsserted;

	public ControlSet() {}

	/**
	 * Create a new control set
	 *
	 * @param uri the URI of the control set
	 * @param control the control
	 * @param controlLabel the label of the control (note that we don't use the ControlSet label here!)
	 * @param assetUri the assetUri at which the control is located
	 * @param assetId the ID of the asset at which the control is located
	 * @param proposed whether the control set is proposed
	 * @param workInProgress whether the control set is proposed but not yet implemented (if true then control must be proposed)
	 */
	public ControlSet(String uri, String control, String controlLabel, String assetUri, String assetId, boolean proposed, boolean workInProgress) {

		this();
		setUri(uri);
		this.control = control;
		setLabel(controlLabel);
		this.assetUri = assetUri;
		this.assetId = assetId;

		if (workInProgress && !proposed) {
			throw new IllegalArgumentException("Control cannot be work in progress but not proposed");
		}
		this.proposed = proposed;
		this.workInProgress = workInProgress;
	}

	/**
	 * Create a new control set with asserted flag
	 *
	 * @param uri the URI of the control set
	 * @param control the control
	 * @param controlLabel the label of the control (note that we don't use the ControlSet label here!)
	 * @param assetUri the assetUri at which the control is located
	 * @param assetId the ID of the asset at which the control is located
	 * @param proposed whether the control set is proposed
	 * @param workInProgress whether the control set is proposed but not yet implemented (if true then control must be proposed)
	 * @param assertable whether the control is assertable
	 * @param coverageLevel coverage level for the control (population support)
	 * @param coverageAsserted whether the coverage level is asserted by the user (population support)
	 */
	public ControlSet(String uri, String control, String controlLabel, String assetUri, String assetId, boolean proposed, boolean workInProgress, boolean assertable, String coverageLevel, boolean coverageAsserted) {

		this(uri, control, controlLabel, assetUri, assetId, proposed, workInProgress);
		this.assertable = assertable;
		this.coverageLevel = coverageLevel;
		this.coverageAsserted = coverageAsserted;
	}

	@Override
	public String toString() {
		return (proposed?"[proposed]":"[not proposed]") + " "
			+ (workInProgress?"[work in progress]":"[not work in progress]") + " "
			+ (assertable?"[assertable]":"[not assertable]") + " "
			+ "coverage: " + coverageLevel + " "
			+ getLabel() + " on " + assetUri + ", uri <" + getUri() + ">";
	}

	/**
	 * @return the control
	 */
	public String getControl() {
		return control;
	}

	/**
	 * @param control
	 */
	public void setControl(String control) {
		this.control = control;
	}

	/**
	 * @return the assetUri
	 */
	public String getAssetUri() {
		return assetUri;
	}

	/**
	 * @param assetUri
	 */
	public void setAssetUri(String assetUri) {
		this.assetUri = assetUri;
	}

	public String getAssetId() {
		return assetId;
	}

	public void setAssetId(String assetId) {
		this.assetId = assetId;
	}

	/**
	 * @return whether this control set is proposed
	 */
	public boolean isProposed() {
		return proposed;
	}

	/**
	 * @param proposed
	 */
	public void setProposed(boolean proposed) {
		if (workInProgress && !proposed) {
			throw new IllegalStateException("Cannot unset proposed: control is work in progress");
		}
		this.proposed = proposed;
	}

	/**
	 * @return whether this control set is assertable
	 */
	public boolean isAssertable() {
		return assertable;
	}

	/**
	 * @param assertable
	 */
	public void setAssertable(boolean assertable){
		this.assertable = assertable;
	}

	/**
	 * @return whether this control set is assertable
	 */
	public boolean isWorkInProgress() {
		return workInProgress;
	}

	/**
	 * @param workInProgress
	 */
	public void setWorkInProgress(boolean workInProgress) {
		if (workInProgress && !proposed) {
			throw new IllegalStateException("Cannot set work in progress: control is not proposed");
		}
		this.workInProgress = workInProgress;
	}

	public String getCoverageLevel() {
		return coverageLevel;
	}

	/**
	 * @param coverageLevel
	 */
	public void setCoverageLevel(String coverageLevel) {
		this.coverageLevel = coverageLevel;
	}

	public boolean isCoverageAsserted() {
		return coverageAsserted;
	}

	/**
	 * @param coverageAsserted
	 */
	public void setCoverageAsserted(boolean coverageAsserted) {
		this.coverageAsserted = coverageAsserted;
	}

	public static enum ControlSetType {
		MANDATORY,
		OPTIONAL
	}

}
