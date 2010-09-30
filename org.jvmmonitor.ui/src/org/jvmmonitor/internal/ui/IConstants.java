/*******************************************************************************
 * Copyright (c) 2010 JVM Monitor project. All rights reserved. 
 * 
 * This code is distributed under the terms of the Eclipse Public License v1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jvmmonitor.internal.ui;

import org.jvmmonitor.ui.Activator;

/**
 * The constants.
 */
@SuppressWarnings("nls")
public interface IConstants {

    /** The preference key for legend visibility. */
    static final String LEGEND_VISIBILITY = "org.jvmmonitor.ui.legend.visibility";
    
    /** The preference key for update period. */
    static final String UPDATE_PERIOD = "org.jvmmonitor.ui.update.period";

    /** The dialog settings key for profiled packages. */
    static final String PACKAGES_KEY = Activator.getDefault().getBundle()
            .getSymbolicName()
            + ".packages";

    /** The dialog settings key for profiler sampling period. */
    static final String PROFILER_SAMPLING_PERIOD_KEY = Activator.getDefault()
            .getBundle().getSymbolicName()
            + ".profilerSampingPeriod";

    /** The dialog settings key for profiler type (BCI or sampling). */
    static final String PROFILER_TYPE_KEY = Activator.getDefault().getBundle()
            .getSymbolicName()
            + ".profilerType";
}
