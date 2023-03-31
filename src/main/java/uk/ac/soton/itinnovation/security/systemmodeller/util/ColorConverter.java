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
//  Created By :            ?
//  Created Date :          ?
//  Created for Project :   ?
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.systemmodeller.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiElement;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Logback {@link CompositeConverter} colors output using the {@link AnsiOutput} class. A
 * single 'color' option can be provided to the converter, or if not specified color will
 * be picked based on the logging level.
 *
 * @author Phillip Webb
 */
public class ColorConverter extends CompositeConverter<ILoggingEvent> {

	private static final Map<String, AnsiElement> ELEMENTS;

	static {
		Map<String, AnsiElement> elements = new HashMap<String, AnsiElement>();
		elements.put("faint", AnsiStyle.FAINT);
		elements.put("red", AnsiColor.RED);
		elements.put("green", AnsiColor.GREEN);
		elements.put("yellow", AnsiColor.YELLOW);
		elements.put("blue", AnsiColor.BLUE);
		elements.put("magenta", AnsiColor.MAGENTA);
		elements.put("cyan", AnsiColor.CYAN);
		ELEMENTS = Collections.unmodifiableMap(elements);
	}

	private static final Map<Integer, AnsiElement> LEVELS;

	static {
		Map<Integer, AnsiElement> levels = new HashMap<Integer, AnsiElement>();
		levels.put(Level.ERROR_INTEGER, AnsiColor.RED);
		levels.put(Level.WARN_INTEGER, AnsiColor.YELLOW);
		levels.put(Level.DEBUG_INTEGER, AnsiColor.MAGENTA);
		LEVELS = Collections.unmodifiableMap(levels);
	}

	static {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
	}

	@Override
	protected String transform(ILoggingEvent event, String in) {
		AnsiElement element = ELEMENTS.get(getFirstOption());
		if (element == null) {
			// Assume highlighting
			element = LEVELS.get(event.getLevel().toInteger());
			element = (element == null ? AnsiColor.GREEN : element);
		}
		return toAnsiString(in, element);
	}

	protected String toAnsiString(String in, AnsiElement element) {
		return AnsiOutput.toString(element, in);
	}

}
