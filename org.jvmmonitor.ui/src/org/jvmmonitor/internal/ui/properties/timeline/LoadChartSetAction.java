/*******************************************************************************
 * Copyright (c) 2010 JVM Monitor project. All rights reserved. 
 * 
 * This code is distributed under the terms of the Eclipse Public License v1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jvmmonitor.internal.ui.properties.timeline;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.jvmmonitor.core.JvmCoreException;
import org.jvmmonitor.core.mbean.IMBeanServer;
import org.jvmmonitor.core.mbean.IMonitoredMXBeanGroup;
import org.jvmmonitor.core.mbean.IMonitoredMXBeanGroup.AxisUnit;
import org.jvmmonitor.internal.ui.properties.AbstractJvmPropertySection;
import org.jvmmonitor.ui.Activator;

/**
 * The action to select chart set.
 */
public class LoadChartSetAction extends AbstractChartSetAction {

    /**
     * The constructor.
     * 
     * @param section
     *            The property section
     */
    public LoadChartSetAction(AbstractJvmPropertySection section) {
        super(section);
        setText(Messages.loadChartSetLabel);
    }

    /*
     * @see Action#run()
     */
    @Override
    public void run() {
        LoadChartSetDialog dialog;
        try {
            dialog = new LoadChartSetDialog(section.getPart().getSite()
                    .getShell(), getChartSets(), getDefaultChartSet(),
                    getPredefinedChartSets());
        } catch (WorkbenchException e) {
            Activator.log(IStatus.ERROR,
                    Messages.openSaveChartSetAsDialogFailedMsg, e);
            return;
        } catch (IOException e) {
            Activator.log(IStatus.ERROR,
                    Messages.openSaveChartSetAsDialogFailedMsg, e);
            return;
        }

        if (dialog.open() == Window.OK) {
            try {
                saveChartSets(dialog.getChartSets());
                loadChartSet(dialog.getChartSet());
            } catch (WorkbenchException e) {
                Activator.log(IStatus.ERROR, Messages.loadChartSetFailedMsg, e);
            } catch (IOException e) {
                Activator.log(IStatus.ERROR, Messages.loadChartSetFailedMsg, e);
            } catch (JvmCoreException e) {
                Activator.log(IStatus.ERROR, Messages.loadChartSetFailedMsg, e);
            }
            storeDefaultChartSet(dialog.getDefaultChartSet());
        }
    }

    /**
     * Loads the default chart set.
     * 
     * @throws JvmCoreException
     */
    protected void loadDefaultChartSet() throws JvmCoreException {
        try {
            loadChartSet(getDefaultChartSet());
        } catch (WorkbenchException e) {
            Activator.log(IStatus.ERROR, Messages.loadChartSetFailedMsg, e);
        } catch (IOException e) {
            Activator.log(IStatus.ERROR, Messages.loadChartSetFailedMsg, e);
        }
    }

    /**
     * Stores the default chart set.
     * 
     * @param defaultChartSet
     *            The default chart set
     */
    private void storeDefaultChartSet(String defaultChartSet) {
        Activator.getDefault().getPreferenceStore()
                .setValue(DEFAULT_CHART_SET, defaultChartSet);
    }

    /**
     * Gets the default chart set.
     * 
     * @return The default chart set
     * @throws IOException
     * @throws WorkbenchException
     */
    private String getDefaultChartSet() throws WorkbenchException, IOException {
        String defaultChartSet = Activator.getDefault().getPreferenceStore()
                .getString(DEFAULT_CHART_SET);
        List<String> chartSets = getChartSets();
        if (defaultChartSet.isEmpty() || !chartSets.contains(defaultChartSet)) {
            Activator.getDefault().getPreferenceStore()
                    .setDefault(DEFAULT_CHART_SET, OVERVIEW_CHART_SET);
            defaultChartSet = OVERVIEW_CHART_SET;
        }
        return defaultChartSet;
    }

    /**
     * Gets the chart sets including predefined chart sets.
     * 
     * @return The chart sets
     * @throws WorkbenchException
     * @throws IOException
     */
    @Override
    List<String> getChartSets() throws WorkbenchException, IOException {
        List<String> elements = super.getChartSets();
        elements.add(0, OVERVIEW_CHART_SET);
        elements.add(1, MEMORY_CHART_SET);
        return elements;
    }

    /**
     * Loads the given chart set.
     * 
     * @param chartSet
     *            The chart set
     * @throws JvmCoreException
     */
    private void loadChartSet(String chartSet) throws JvmCoreException {
        loadPredefinedChartSet(chartSet);

        IMemento chartSetsMemento;
        try {
            chartSetsMemento = getChartSetsMemento();
        } catch (WorkbenchException e) {
            throw new JvmCoreException(IStatus.ERROR,
                    Messages.loadChartSetFailedMsg, e);
        } catch (IOException e) {
            throw new JvmCoreException(IStatus.ERROR,
                    Messages.loadChartSetFailedMsg, e);
        }

        if (chartSetsMemento == null) {
            return;
        }
        IMemento[] mementos = chartSetsMemento.getChildren(CHART_SET);
        for (IMemento memento : mementos) {
            if (chartSet.equals(memento.getID())) {
                loadChartSet(memento);
                return;
            }
        }
    }

    /**
     * Loads the given memento of chart set.
     * 
     * @param memento
     *            The memento
     * @throws JvmCoreException
     */
    private void loadChartSet(IMemento memento) throws JvmCoreException {
        IMBeanServer server = section.getJvm().getMBeanServer();
        server.getMonitoredAttributeGroups().clear();

        for (IMemento groupMemento : memento.getChildren(GROUP)) {
            IMonitoredMXBeanGroup group = server.addMonitoredAttributeGroup(
                    groupMemento.getID(),
                    AxisUnit.valueOf(groupMemento.getString(UNIT)));
            for (IMemento attributeMemento : groupMemento
                    .getChildren(ATTRIBUTE)) {
                group.addAttribute(attributeMemento.getString(OBJECT_NAME),
                        attributeMemento.getID(),
                        getRGB(attributeMemento.getString(COLOR)));
            }
        }
    }

    /**
     * Loads the given predefined chat set. If the given chart set is not
     * predefined one, does nothing.
     * 
     * @param chartSet
     *            The chart set
     * @throws JvmCoreException
     */
    private void loadPredefinedChartSet(String chartSet)
            throws JvmCoreException {
        if (OVERVIEW_CHART_SET.equals(chartSet)) {
            loadOverviewChartSet();
        } else if (MEMORY_CHART_SET.equals(chartSet)) {
            loadMemoryChartSet();
        }
    }

    /**
     * Loads the overview chart set.
     * 
     * @throws JvmCoreException
     */
    private void loadOverviewChartSet() throws JvmCoreException {
        final int[] blue = new int[] { 0, 0, 255 };
        IMBeanServer server = section.getJvm().getMBeanServer();
        server.getMonitoredAttributeGroups().clear();

        IMonitoredMXBeanGroup group = server.addMonitoredAttributeGroup(
                "Used Heap Memory", AxisUnit.MBytes); //$NON-NLS-1$
        group.addAttribute(ManagementFactory.MEMORY_MXBEAN_NAME,
                "HeapMemoryUsage.used", blue); //$NON-NLS-1$

        group = server.addMonitoredAttributeGroup(
                "Loaded Class Count", AxisUnit.Count); //$NON-NLS-1$
        group.addAttribute(ManagementFactory.CLASS_LOADING_MXBEAN_NAME,
                "LoadedClassCount", blue); //$NON-NLS-1$

        group = server.addMonitoredAttributeGroup(
                "Thread Count", AxisUnit.Count); //$NON-NLS-1$
        group.addAttribute(ManagementFactory.THREAD_MXBEAN_NAME, "ThreadCount", //$NON-NLS-1$
                blue);

        group = server
                .addMonitoredAttributeGroup("CPU Usage", AxisUnit.Percent); //$NON-NLS-1$
        group.addAttribute(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME,
                "ProcessCpuTime", blue); //$NON-NLS-1$
    }

    /**
     * Loads the memory chart set.
     * 
     * @throws JvmCoreException
     */
    private void loadMemoryChartSet() throws JvmCoreException {
        final int[] blue = new int[] { 0, 0, 255 };
        final int[] green = new int[] { 0, 255, 0 };
        IMBeanServer server = section.getJvm().getMBeanServer();
        server.getMonitoredAttributeGroups().clear();

        IMonitoredMXBeanGroup group = server.addMonitoredAttributeGroup(
                "Heap Memory", AxisUnit.MBytes); //$NON-NLS-1$
        group.addAttribute(ManagementFactory.MEMORY_MXBEAN_NAME,
                "HeapMemoryUsage.used", blue); //$NON-NLS-1$
        group.addAttribute(ManagementFactory.MEMORY_MXBEAN_NAME,
                "HeapMemoryUsage.committed", green); //$NON-NLS-1$

        group = server.addMonitoredAttributeGroup(
                "Non-Heap Memory", AxisUnit.MBytes); //$NON-NLS-1$
        group.addAttribute(ManagementFactory.MEMORY_MXBEAN_NAME,
                "NonHeapMemoryUsage.used", blue); //$NON-NLS-1$
        group.addAttribute(ManagementFactory.MEMORY_MXBEAN_NAME,
                "NonHeapMemoryUsage.committed", green); //$NON-NLS-1$

        group = server.addMonitoredAttributeGroup(
                "Heap Memory Pool", AxisUnit.MBytes); //$NON-NLS-1$
        for (String objectName : getHeapMemoryPoolObjectNames()) {
            group.addAttribute(objectName, "Usage.used", blue); //$NON-NLS-1$
        }

        group = server.addMonitoredAttributeGroup(
                "Non-Heap Memory Pool", AxisUnit.MBytes); //$NON-NLS-1$
        for (String objectName : getNonHeapMemoryPoolObjectNames()) {
            group.addAttribute(objectName, "Usage.used", blue); //$NON-NLS-1$
        }
    }

    /**
     * Gets the heap memory pool object names.
     * 
     * @return The heap memory pool object names
     * @throws JvmCoreException
     */
    private List<String> getHeapMemoryPoolObjectNames() throws JvmCoreException {
        return Arrays.asList(new String[] {
                "java.lang:type=MemoryPool,name=Eden Space", //$NON-NLS-1$
                "java.lang:type=MemoryPool,name=Survivor Space", //$NON-NLS-1$
                "java.lang:type=MemoryPool,name=Tenured Gen" }); //$NON-NLS-1$
    }

    /**
     * Gets the non-heap memory pool object names.
     * 
     * @return The non-heap memory pool object names
     * @throws JvmCoreException
     */
    private List<String> getNonHeapMemoryPoolObjectNames()
            throws JvmCoreException {
        List<String> heapMemoryPoolObjectNames = new ArrayList<String>();
        heapMemoryPoolObjectNames
                .add("java.lang:type=MemoryPool,name=Code Cache"); //$NON-NLS-1$

        try {
            for (ObjectName objectName : section
                    .getJvm()
                    .getMBeanServer()
                    .queryNames(
                            ObjectName
                                    .getInstance("java.lang:type=MemoryPool,name=*"))) { //$NON-NLS-1$
                String canonicalName = objectName.getCanonicalName();
                if (canonicalName.contains("name=Perm Gen")) { //$NON-NLS-1$
                    heapMemoryPoolObjectNames.add(canonicalName);
                }
            }
        } catch (MalformedObjectNameException e) {
            Activator.log(IStatus.ERROR,
                    Messages.getMemoryPoolAttributeFailedMsg, e);
        } catch (NullPointerException e) {
            Activator.log(IStatus.ERROR,
                    Messages.getMemoryPoolAttributeFailedMsg, e);
        }
        return heapMemoryPoolObjectNames;
    }

    /**
     * Saves the given chart sets that can be subset of previous chart sets.
     * 
     * @param chartSets
     *            The chart sets
     * @throws WorkbenchException
     * @throws IOException
     */
    private void saveChartSets(List<String> chartSets)
            throws WorkbenchException, IOException {
        IMemento oldChartSetsMemento = getChartSetsMemento();
        IMemento[] oldMementos;
        if (oldChartSetsMemento == null) {
            oldMementos = new IMemento[0];
        } else {
            oldMementos = oldChartSetsMemento.getChildren(CHART_SET);
        }
        
        XMLMemento chartSetsMemento = XMLMemento.createWriteRoot(CHART_SETS);
        for (String chartSet : chartSets) {
            for (IMemento memento : oldMementos) {
                if (chartSet.equals(memento.getID())) {
                    chartSetsMemento.createChild(CHART_SET).putMemento(memento);
                    break;
                }
            }
        }

        StringWriter writer = new StringWriter();
        chartSetsMemento.save(writer);
        Activator.getDefault().getPreferenceStore()
                .setValue(CHART_SETS, writer.getBuffer().toString());
    }

    /**
     * Gets the RGB integer array corresponding to the given RGB string.
     * 
     * @param rgbString
     *            The RGB string "r,g,b" (e.g. "225,225,0")
     * @return The RGB integer array
     */
    private int[] getRGB(String rgbString) {
        String[] elements = rgbString.split(","); //$NON-NLS-1$
        int[] rgb = new int[3];

        for (int i = 0; i < rgb.length; i++) {
            rgb[i] = Integer.valueOf(elements[i]);
        }

        return rgb;
    }
}
