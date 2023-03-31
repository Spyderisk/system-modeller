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
//      Created By :            Stefanie Cox
//      Created Date :          2017-11-29
//      Created for Project :   SHiELD
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an enumeration instance. Enumerations can exist on core- and domain level where domain-level classifiers
 * inherit core-level enumerations but can choose to extend them by adding more instances.
 */
public class Level extends SemanticEntity implements Comparable<SemanticEntity> {

	public static final Logger logger = LoggerFactory.getLogger(Level.class);

	private int value;

	public Level() {}

	@Override
	public String toString() {
		return super.toString() + ", value: " + String.valueOf(value);
	}

	@Override
	public int compareTo(SemanticEntity other) {

		//compare levels by value
		if (other!=null && other.getClass().equals(Level.class)) {
			int returnValue =  Double.compare(getValue(), ((Level) other).getValue());
			return returnValue!=0?returnValue:super.compareTo(other);
		//otherwise compare URIs
		} else {
			return super.compareTo(other);
		}
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

}
