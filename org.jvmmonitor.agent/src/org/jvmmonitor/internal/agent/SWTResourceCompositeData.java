package org.jvmmonitor.internal.agent;

import java.beans.ConstructorProperties;
import java.util.Set;

/**
 * The SWT resource that is converted into <tt>CompositeData</tt>.
 */
public class SWTResourceCompositeData {

    /** The resource name that is given by Resource.toString(). */
    private String name;

    /** The stack traces to show how the resource was created. */
    private Set<StackTraceElementCompositeData> stackTrace;

    /**
     * The constructor.
     * 
     * @param name
     *            The resource name
     * @param stackTrace
     *            The stack trace
     */
    @ConstructorProperties({ "name", "stackTrace" })
    public SWTResourceCompositeData(String name, Set<StackTraceElementCompositeData> stackTrace) {
        this.name = name;
        this.stackTrace = stackTrace;
    }

    /**
     * Gets the resource name.
     * 
     * @return The resource name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the stack trace.
     * 
     * @return The stack trace
     */
    public Set<StackTraceElementCompositeData> getStackTrace() {
        return stackTrace;
    }
}
