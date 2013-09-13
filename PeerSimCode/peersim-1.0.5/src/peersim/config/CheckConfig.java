/*
 * Copyright (c) 2003-2005 The BISON Project
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package peersim.config;

import java.util.*;

import peersim.cdsim.*;
import peersim.edsim.*;
import peersim.util.*;


/**
 * This is utility tool that checks whether a config file can be loaded
 * or not, without actually performing the simulation. All the error
 * messages generated by controls and protocols when initialized are
 * reported.  This is useful to check all the configuration files in a 
 * directory.
 */
public class CheckConfig {

//========================== parameters ================================
//======================================================================

/**
 * This is the prefix of the config properties whose value vary during
 * a set of experiments.
 * @config
 */
private static final String PAR_RANGE = "range";


// ========================== static constants ==========================
// ======================================================================

/** {@link CDSimulator} */
protected static final int CDSIM = 0;

/** {@link EDSimulator} */
protected static final int EDSIM = 1;

protected static final int UNKNOWN = -1;

/** the class names of simulators used */
protected static final String[] simName = {
	CDSimulator.class.getCanonicalName(),
	EDSimulator.class.getCanonicalName(),
};



	
// ========================== methods ===================================
// ======================================================================

/**
* Returns the numeric id of the simulator to invoke. At the moment this can
* be {@link #CDSIM}, {@link #EDSIM} or {@link #UNKNOWN}.
*/
protected static int getSimID() {
	
	if( CDSimulator.isConfigurationCycleDriven())
	{
		return CDSIM;
	}
	else if( EDSimulator.isConfigurationEventDriven() )
	{	
		return EDSIM;
	}
	else	return UNKNOWN;
}

// ----------------------------------------------------------------------

/**
* Loads the configuration and checks the configuration files against
* simple configuration errors, such as missing classes, missing 
* parameters or syntax errors.
* <p>
* The goal is to provide a mechanism to test a configuration file,
* without having to perform the actual simulations (that could be
* time-consuming) and without necessarily blocking after the first
* error encountered. It may be useful, for example, when a major 
* refactoring of your code requires a thorough check on all your 
* configuration files.
* <p>
* Loading the configuration is currently done with the help of 
* constructing an instance of {@link ParsedProperties} using the 
* constructor {@link ParsedProperties#ParsedProperties(String[])},
* in the same way as the normal simulator.
* <p>
* After loading the configuration, the collection of nodes forming a 
* Network is instantiated, together with all protocols forming a
* node. Initialization controls are executed, and then the simulation
* stops. 
* <p>
* For each error encountered, a message is printed ons standard error,
* and the initialization keeps going without interruption. If multiple 
* errors are present, an error message for each of them is printed.
* Apart from errors, default choices are also printed as warnings, to 
* allow developers to spot subtle configuration errors such as missing
* parameters that defaults to standard values.
* 
* @param args passed on to
* {@link ParsedProperties#ParsedProperties(String[])}
*/
public static void main(String[] args)
  throws Exception
{
	System.setErr(new NullPrintStream());
	Properties prop = new ParsedProperties(args);
	Configuration.setConfig( prop, true );
	parseRanges(prop);
	
	final int SIMID = getSimID();
	if( SIMID == UNKNOWN )
	{
		System.err.println(
		    "Simulator: unable to identify configuration, exiting.");
		return;
	}
	
	try {
	
		// XXX could be done through reflection, but
		// this is easier to read.
		switch(SIMID)
		{
		case CDSIM:
			// Set cycles to 0, so no simulation is ever performed.
			prop.setProperty(CDSimulator.PAR_CYCLES, "0");
			// Look onto this
			CDSimulator.nextExperiment(1);
			break;
		case EDSIM:
			// Set endtime to 0, so no simulation is ever performed.
			prop.setProperty(EDSimulator.PAR_ENDTIME, "0");
			EDSimulator.nextExperiment();
			break;
		}
	
	} catch (MissingParameterException e) {
		System.out.println(e.getMessage());
		System.exit(1);
	} catch (IllegalParameterException e) {
		System.out.println(e.getMessage());
		System.exit(1);
	}	
}

/**
 * Parses a collection of range specifications, identifies the set
 * of parameters that will change during the simulation and
 * instantiates them with the first value of their ranges.
 */
private static void parseRanges(Properties prop)
{
	// Get ranges
	String[] ranges = Configuration.getNames(PAR_RANGE);

	for (int i = 0; i < ranges.length; i++) {
		String[] array = Configuration.getString(ranges[i]).split(";");
		if (array.length != 2) {
			throw new IllegalParameterException(ranges[i],
					" should be formatted as <parameter>;<value list>");
		}
		String[] values = StringListParser.parseList(array[1]);
		prop.setProperty(array[0], values[0]);
	}
}

}
