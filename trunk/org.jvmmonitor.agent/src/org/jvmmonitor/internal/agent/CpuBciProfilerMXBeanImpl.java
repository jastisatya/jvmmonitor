/*******************************************************************************
 * Copyright (c) 2010 JVM Monitor project. All rights reserved. 
 * 
 * This code is distributed under the terms of the Eclipse Public License v1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jvmmonitor.internal.agent;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Set;

/**
 * The profiler controller.
 */
@SuppressWarnings("nls")
public class CpuBciProfilerMXBeanImpl implements CpuBciProfilerMXBean {

    /** The instrumentation. */
    private Instrumentation inst;

    /** The state indicating if transformation is needed. */
    private boolean transformNeeded;

    /**
     * The constructor.
     * 
     * @param inst
     *            The instrumentation
     * @throws IOException
     */
    public CpuBciProfilerMXBeanImpl(Instrumentation inst) throws IOException {
        this.inst = inst;
        transformNeeded = true;
        CpuBciProfiler.initialize();
        if (Config.getInstance().isProfilerEnabled()) {
            setRunning(true);
        }
    }

    /*
     * @see CpuProfilerMXBean#setRunning(boolean)
     */
    @Override
    public void setRunning(boolean run) {
        if (run) {
            try {
                if (transformNeeded) {
                    inst.addTransformer(new ClassFileTransformerImpl(), true);
                    retransformClasses();
                    transformNeeded = false;
                }
                Config.getInstance().setProfilerEnabled(true);
            } catch (Throwable t) {
                Agent.logError(t, Messages.CANNOT_RESUME);
            }
        } else {
            try {
                Config.getInstance().setProfilerEnabled(false);
            } catch (Throwable t) {
                Agent.logError(t, Messages.CANNOT_SUSPEND);
            }
        }
    }

    /*
     * @see ProfilerMXBean#isRunning()
     */
    @Override
    public boolean isRunning() {
        try {
            return Config.getInstance().isProfilerEnabled();
        } catch (Throwable t) {
            Agent.logError(t, Messages.CANNOT_GET_RUNNING_STATE);
            return false;
        }
    }

    /*
     * @see ProfilerMXBean#clear()
     */
    @Override
    public void clear() {
        try {
            CpuBciProfiler.getModel().clear();
        } catch (Throwable t) {
            Agent.logError(t, Messages.CANNOT_CLEAR);
        }
    }

    /*
     * @see ProfilerMXBean#dump()
     */
    @Override
    public String dump() {
        try {
            return CpuBciProfiler.getModel().dump();
        } catch (Throwable t) {
            Agent.logError(t, Messages.CANNOT_GET_DUMP);
            return "";
        }
    }

    /*
     * @see ProfilerMXBean#dumpToFile()
     */
    @Override
    public void dumpToFile() {
        try {
            CpuBciProfiler.getModel().dumpToFile();
        } catch (Throwable t) {
            Agent.logError(t, Messages.CANNOT_DUMP_TO_FILE);
        }
    }

    /*
     * @see CpuProfilerMXBean#getDumpDir()
     */
    @Override
    public String getDumpDir() {
        return Config.getInstance().getDumpDir();
    }

    /*
     * @see CpuProfilerMXBean#setDumpDir(String)
     */
    @Override
    public void setDumpDir(String dir) {
        Config.getInstance().setDumpDir(dir);
    }

    /*
     * @see ProfilerMXBean#getVersion()
     */
    @Override
    public String getVersion() {
        return Constants.VERSION;
    }

    /*
     * @see CpuProfilerMXBean#setFilter(String, String)
     */
    @Override
    public void setFilter(String key, String value) {
        if (Constants.PROFILED_PACKAGES_PROP_KEY.equals(key)) {
            Config.getInstance().profiledPackages.clear();
            Config.getInstance().addElements(
                    Config.getInstance().profiledPackages, value);
        }
    }

    /*
     * @see CpuProfilerMXBean#getProfiledClassloaders()
     */
    @Override
    public String[] getProfiledClassloaders() {
        Set<String> list = Config.getInstance().profiledClassLoaders;
        return list.toArray(new String[list.size()]);
    }

    /*
     * @see CpuProfilerMXBean#getProfiledPackages()
     */
    @Override
    public String[] getProfiledPackages() {
        Set<String> list = Config.getInstance().profiledPackages;
        return list.toArray(new String[list.size()]);
    }

    /*
     * @see CpuProfilerMXBean#getIgnoredPackages()
     */
    @Override
    public String[] getIgnoredPackages() {
        Set<String> list = Config.getInstance().ignoredPackages;
        return list.toArray(new String[list.size()]);
    }

    /**
     * Re-transforms the loaded classes.
     */
    private void retransformClasses() {
        for (@SuppressWarnings("rawtypes")
        Class clazz : inst.getAllLoadedClasses()) {
            String className = clazz.getName();
            if (!isMatch(className, Config.getInstance().profiledPackages)
                    || isMatch(className, Config.getInstance().ignoredPackages)
                    || className.startsWith("[")) {
                continue;
            }

            Agent.logInfo(Messages.RETRANSFORMED_CLASS, clazz);

            try {
                inst.retransformClasses(clazz);
            } catch (UnmodifiableClassException e) {
                Agent.logError(e, Messages.CANNOT_RETRANSFORM_CLASS,
                        clazz.getCanonicalName());
            }
        }
    }

    /**
     * Checks if the given class belongs to one of the packages list.
     * 
     * @param className
     *            the class name (e.g. java.lang.String)
     * @param packages
     *            the list of packages
     * @return true if the given class belongs to one of the packages list
     */
    private boolean isMatch(String className, Set<String> packages) {
        if (packages.isEmpty()) {
            return false;
        }

        String packageName;
        if (className.contains(".")) {
            packageName = className.substring(0, className.lastIndexOf('.'));
        } else {
            packageName = Constants.DEFAULT_PACKAGE;
        }
        for (String pkg : packages) {
            if (pkg.endsWith("*")) {
                if (packageName.startsWith(pkg.substring(0, pkg.length() - 2))) {
                    return true;
                }
            } else {
                if (packageName.equals(pkg)) {
                    return true;
                }
            }
        }
        return false;
    }
}
