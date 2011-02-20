package org.jvmmonitor.internal.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The MXBean to monitor SWT resources.
 */
@SuppressWarnings("nls")
public class SWTResourceMonitorMXBeanImpl implements SWTResourceMonitorMXBean {

    /** The display object. */
    private Object displayObject;

    /** The display class. */
    private Class<?> displayClass;

    /** The device class. */
    private Class<?> deviceClass;

    private SWTResourceCompositeData[] resources;

    /**
     * The constructor.
     * 
     * @param inst
     *            The instrumentation
     */
    public SWTResourceMonitorMXBeanImpl(Instrumentation inst) {
        for (@SuppressWarnings("rawtypes")
        Class clazz : inst.getAllLoadedClasses()) {
            String className = clazz.getName();
            if ("org.eclipse.swt.graphics.Device".equals(className)) {
                deviceClass = clazz;
            } else if ("org.eclipse.swt.widgets.Display".equals(className)) {
                displayClass = clazz;
            }
        }

        try {
            Field field = deviceClass.getDeclaredField("trackingLock");
            field.setAccessible(true);
            field.set(getDisplayObject(), new Object());
        } catch (Throwable t) {
            deviceClass = null;
            displayClass = null;
        }
    }

    /*
     * @see SWTResourceMonitorMXBean#setTracking(boolean)
     */
    @Override
    public void setTracking(boolean tracking) {
        try {
            if (tracking && !isTracking()) {
                clear();
            }

            Field field = deviceClass.getDeclaredField("tracking");
            field.setAccessible(true);
            field.set(getDisplayObject(), tracking);
        } catch (Throwable t) {
            Agent.logError(t, Messages.CANNOT_SET_RESOURCE_TRACKING_STATE);
        }
    }

    /*
     * @see SWTResourceMonitorMXBean#isTracking()
     */
    @Override
    public boolean isTracking() {
        try {
            Field field = deviceClass.getDeclaredField("tracking");
            field.setAccessible(true);
            return (Boolean) field.get(getDisplayObject());
        } catch (Throwable t) {
            Agent.logError(t, Messages.CANNOT_GET_RESOURCE_TRACKING_STATE);
            return false;
        }
    }

    /*
     * @see SWTResourceMonitorMXBean#getResources()
     */
    @Override
    public SWTResourceCompositeData[] getResources() {
        try {
            refresh();
        } catch (Throwable t) {
            Agent.logError(t, Messages.CANNOT_GET_RESOURCES);
        }
        return resources;
    }

    /*
     * @see SWTResourceMonitorMXBean#clear()
     */
    @Override
    public void clear() {
        try {
            Field field = deviceClass.getDeclaredField("errors");
            field.setAccessible(true);
            field.set(getDisplayObject(), new Error[127]);

            field = deviceClass.getDeclaredField("objects");
            field.setAccessible(true);
            field.set(getDisplayObject(), new Object[127]);
        } catch (Throwable t) {
            Agent.logError(t, Messages.CANNOT_CLEAR_RESOURCE_TRACKING_DATA);
        }
    }

    /**
     * Gets the state indicating if monitoring SWT resources is supported.
     * 
     * @return <tt>true</tt> if monitoring SWT resources is supported
     */
    public boolean isSuppoted() {
        return deviceClass != null && displayClass != null;
    }

    /**
     * Refreshes the resources stored in this class.
     * 
     * @throws Throwable
     */
    private void refresh() throws SecurityException, Throwable {
        Field field = deviceClass.getDeclaredField("objects");
        field.setAccessible(true);
        Object[] objects = (Object[]) field.get(getDisplayObject());

        field = deviceClass.getDeclaredField("errors");
        field.setAccessible(true);
        Error[] errors = (Error[]) field.get(getDisplayObject());

        if (objects == null || errors == null
                || objects.length != errors.length) {
            resources = new SWTResourceCompositeData[0];
            return;
        }

        List<SWTResourceCompositeData> resourcesList = new ArrayList<SWTResourceCompositeData>();
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] == null) {
                continue;
            }

            SWTResourceCompositeData resource = new SWTResourceCompositeData(
                    objects[i].toString(), getStackTrace(errors[i]));
            resourcesList.add(resource);
        }
        resources = resourcesList
                .toArray(new SWTResourceCompositeData[resourcesList.size()]);
    }

    /**
     * Gets the stack trace as an array of composite data.
     * 
     * @param error
     *            The error
     * @return The stack trace
     */
    private Set<StackTraceElementCompositeData> getStackTrace(Error error) {
        Set<StackTraceElementCompositeData> set = new HashSet<StackTraceElementCompositeData>();
        if (error != null) {
            for (StackTraceElement element : error.getStackTrace()) {
                set.add(new StackTraceElementCompositeData(element));
            }
        }
        return set;
    }

    /**
     * Gets the display object.
     * 
     * @return The display object
     * @throws Throwable
     */
    private Object getDisplayObject() throws Throwable {
        if (displayObject == null) {
            Method method = displayClass.getDeclaredMethod("getDefault");
            displayObject = method.invoke(null);
        }
        return displayObject;
    }
}
