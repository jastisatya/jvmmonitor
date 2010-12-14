/*******************************************************************************
 * Copyright (c) 2010 JVM Monitor project. All rights reserved. 
 * 
 * This code is distributed under the terms of the Eclipse Public License v1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jvmmonitor.internal.ui.properties.memory;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.jvmmonitor.core.IActiveJvm;
import org.jvmmonitor.core.IHeapElement;
import org.jvmmonitor.core.JvmCoreException;
import org.jvmmonitor.internal.ui.IHelpContextIds;
import org.jvmmonitor.internal.ui.RefreshJob;
import org.jvmmonitor.internal.ui.actions.RefreshAction;
import org.jvmmonitor.internal.ui.properties.AbstractJvmPropertySection;
import org.jvmmonitor.ui.Activator;

/**
 * The memory section.
 */
public class MemorySection extends AbstractJvmPropertySection {

    /** The 64 bit OS architecture. */
    private static final String ARCH_64BIT = "64"; //$NON-NLS-1$

    /** The action to refresh section. */
    RefreshAction refreshAction;

    /** The action to run garbage collector. */
    GarbageCollectorAction garbageCollectorAction;

    /** The action to clear heap delta. */
    ClearHeapDeltaAction clearHeapDeltaAction;

    /** The action to dump heap. */
    DumpHeapAction dumpHeapAction;

    /** The action to dump hprof. */
    DumpHprofAction dumpHprofAction;

    /** The separator. */
    private Separator separator;

    /** The heap composite. */
    HeapComposite heapComposite;

    /*
     * @see AbstractPropertySection#createControls(Composite,
     * TabbedPropertySheetPage)
     */
    @Override
    public void createControls(Composite parent) {
        heapComposite = new HeapComposite(parent, getActionBars());
        createActions();

        PlatformUI.getWorkbench().getHelpSystem()
                .setHelp(parent, IHelpContextIds.MEMORY_PAGE);
    }

    /*
     * @see AbstractPropertySection#refresh()
     */
    @Override
    public void refresh() {
        if (getJvm() == null || !isSupported()) {
            return;
        }

        refreshJob = new RefreshJob(NLS.bind(
                Messages.refreshMemorySectionJobLabel, getJvm().getPid()),
                getId()) {
            @Override
            protected void refreshModel(IProgressMonitor monitor) {
                try {
                    IActiveJvm jvm = getJvm();
                    if (jvm != null && jvm.isConnected() && !jvm.isRemote()
                            && !isRefreshSuspended()) {
                        jvm.getMBeanServer().refreshHeapCache();
                    }
                } catch (JvmCoreException e) {
                    Activator.log(Messages.refreshHeapDataFailedMsg, e);
                }
            }

            @Override
            protected void refreshUI() {
                if (heapComposite.isDisposed()) {
                    return;
                }

                IActiveJvm jvm = getJvm();
                boolean isConnected = jvm != null && jvm.isConnected();
                boolean isRemote = jvm != null && jvm.isRemote();
                refreshBackground(heapComposite.getChildren(), isConnected
                        && !isRemote);
                dumpHprofAction.setEnabled(isConnected);
                dumpHeapAction.setEnabled(!hasErrorMessage());
                refreshAction.setEnabled(isConnected && !isRemote);
                garbageCollectorAction.setEnabled(isConnected);
                clearHeapDeltaAction.setEnabled(isConnected && !isRemote);

                if (!heapComposite.isDisposed()) {
                    heapComposite.refresh();
                }
            }
        };

        refreshJob.schedule();
    }

    /*
     * @see AbstractJvmPropertySection#setInput(IWorkbenchPart, ISelection,
     * IActiveJvm, IActiveJvm)
     */
    @Override
    protected void setInput(IWorkbenchPart part, ISelection selection,
            final IActiveJvm newJvm, IActiveJvm oldJvm) {
        heapComposite.setInput(new IHeapInput() {
            @Override
            public IHeapElement[] getHeapListElements() {
                return newJvm.getMBeanServer().getHeapCache();
            }
        });
    }

    /*
     * @see AbstractJvmPropertySection#addToolBarActions(IToolBarManager)
     */
    @Override
    protected void addToolBarActions(IToolBarManager manager) {
        manager.insertAfter("defaults", separator); //$NON-NLS-1$
        if (manager.find(refreshAction.getId()) == null) {
            manager.insertAfter("defaults", refreshAction); //$NON-NLS-1$
        }
        if (manager.find(garbageCollectorAction.getId()) == null) {
            manager.insertAfter("defaults", garbageCollectorAction); //$NON-NLS-1$
        }
        if (manager.find(clearHeapDeltaAction.getId()) == null) {
            manager.insertAfter("defaults", clearHeapDeltaAction); //$NON-NLS-1$
        }
        if (manager.find(dumpHeapAction.getId()) == null) {
            manager.insertAfter("defaults", dumpHeapAction); //$NON-NLS-1$
        }
        if (manager.find(dumpHprofAction.getId()) == null) {
            manager.insertAfter("defaults", dumpHprofAction); //$NON-NLS-1$
        }
    }

    /*
     * @see AbstractJvmPropertySection#removeToolBarActions(IToolBarManager)
     */
    @Override
    protected void removeToolBarActions(IToolBarManager manager) {
        manager.remove(separator);
        manager.remove(refreshAction.getId());
        manager.remove(garbageCollectorAction.getId());
        manager.remove(clearHeapDeltaAction.getId());
        manager.remove(dumpHeapAction.getId());
        manager.remove(dumpHprofAction.getId());
    }

    /*
     * @see AbstractJvmPropertySection#deactivateSection()
     */
    @Override
    protected void deactivateSection() {
        super.deactivateSection();
        if (refreshJob != null) {
            refreshJob.cancel();
        }
    }

    /*
     * @see AbstractJvmPropertySection#updatePage()
     */
    @Override
    protected void updatePage() {
        super.updatePage();

        IActiveJvm jvm = getJvm();
        if (jvm != null && jvm.isConnected()) {
            if (jvm.isRemote()) {
                setErrorMessageLabel(Messages.notSupportedOnRemoteHostMsg);
            } else if (!isSupported()) {
                setErrorMessageLabel(Messages.notSupportedForEclipseItselfOn64bitOS);
            }
        }
    }

    /**
     * Creates the actions.
     */
    private void createActions() {
        refreshAction = new RefreshAction(this);
        garbageCollectorAction = new GarbageCollectorAction(this);
        clearHeapDeltaAction = new ClearHeapDeltaAction(heapComposite, this);
        dumpHeapAction = new DumpHeapAction(this);
        dumpHprofAction = new DumpHprofAction(this);
        separator = new Separator();
    }

    /**
     * Gets the state indicating if heap histogram is supported.
     * <p>
     * WORKAROUND: Heap histogram is disabled on 64bit OS when monitoring
     * eclipse itself, due to the issue that the method heapHisto() of the class
     * HotSpotVirtualMachine causes continuously increasing the committed heap
     * memory.
     * 
     * @return <tt>true</tt> if heap histogram is supported
     */
    private boolean isSupported() {
        IActiveJvm jvm = getJvm();
        if (jvm == null) {
            return false;
        }

        OperatingSystemMXBean osMBean = ManagementFactory
                .getOperatingSystemMXBean();
        RuntimeMXBean runtimeMBean = ManagementFactory.getRuntimeMXBean();
        if (osMBean.getArch().contains(ARCH_64BIT)
                && runtimeMBean.getName()
                        .contains(String.valueOf(jvm.getPid()))) {
            return false;
        }

        return true;
    }
}
