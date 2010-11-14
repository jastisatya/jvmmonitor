/*******************************************************************************
 * Copyright (c) 2010 JVM Monitor project. All rights reserved. 
 * 
 * This code is distributed under the terms of the Eclipse Public License v1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jvmmonitor.internal.agent;

/**
 * The messages.
 */
@SuppressWarnings("nls")
public class Messages {

    /** The error message that registering CpuProfilerMXBean failed. */
    static final String CANNOT_REGISTER_CPU_PROFILER_MXBEAN = "Cannot not register CpuBciProfilerMXBean.";

    /** The error message that opening configuration file failed. */
    static final String CANNOT_OPEN_CONFIG_FILE = "Cannot open the specified configuration file:\n\t%s\n";

    /** The error message that getting dump failed. */
    static final String CANNOT_GET_DUMP = "Cannot get the CPU profiling data.";

    /** The error message that clearing profile data failed. */
    static final String CANNOT_CLEAR = "Cannot clear the CPU profiling data.";

    /** The error message that getting profiler state failed. */
    static final String CANNOT_GET_RUNNING_STATE = "Cannot get the profiler running state.";

    /** The error message that suspending profiler failed. */
    static final String CANNOT_SUSPEND = "Cannot suspend the CPU profiler.";

    /** The error message that resuming profiler failed. */
    static final String CANNOT_RESUME = "Cannot resume the CPU profiler.";

    /** The error message that dumping to file failed. */
    static final String CANNOT_DUMP_TO_FILE = "Cannot dump the CPU profiling data to file.";

    /** the error message that creating dump file failed. */
    static final String CANNOT_CREATE_DUMP_FILE = "Writing into a dump file failed.\n"
            + "Please check the output directory \"%s\" specified in configuration file.\n";

    /** The error message that re-transforming class failed. */
    static final String CANNOT_RETRANSFORM_CLASS = "Cannot retransform class: %s";

    /** The error message that reading file failed. */
    static final String CANNOT_READ_FILE = "Cannot read file: %s";

    /** The info message that agent got loaded. */
    static final String AGENT_LOADED = "Agent has been loaded.";

    /** The info message that agent has been already loaded. */
    static final String AGENT_ALREADY_LOADED = "Agent has been already loaded.";

    /** The info message that class has been instrumented. */
    static final String INSTRUMENTED_CLASS = "Instrumented class: %s";

    /** The info message that class has been re-transformed. */
    static final String RETRANSFORMED_CLASS = "Retransformed class: %s";
}
