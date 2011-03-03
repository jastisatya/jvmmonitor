/*******************************************************************************
 * Copyright (c) 2010 JVM Monitor project. All rights reserved. 
 * 
 * This code is distributed under the terms of the Eclipse Public License v1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jvmmonitor.internal.ui.properties.cpu;

import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.jvmmonitor.core.IActiveJvm;
import org.jvmmonitor.core.JvmCoreException;
import org.jvmmonitor.core.JvmModelEvent;
import org.jvmmonitor.core.JvmModelEvent.State;
import org.jvmmonitor.core.cpu.CpuModelEvent;
import org.jvmmonitor.core.cpu.ICpuModelChangeListener;
import org.jvmmonitor.core.cpu.ICpuProfiler;
import org.jvmmonitor.core.cpu.ICpuProfiler.ProfilerState;
import org.jvmmonitor.core.cpu.ICpuProfiler.ProfilerType;
import org.jvmmonitor.internal.ui.IConstants;
import org.jvmmonitor.internal.ui.IHelpContextIds;
import org.jvmmonitor.internal.ui.RefreshJob;
import org.jvmmonitor.internal.ui.actions.CopyAction;
import org.jvmmonitor.internal.ui.actions.OpenDeclarationAction;
import org.jvmmonitor.internal.ui.properties.AbstractJvmPropertySection;
import org.jvmmonitor.internal.ui.properties.cpu.actions.ClearCpuProfilingDataAction;
import org.jvmmonitor.internal.ui.properties.cpu.actions.DumpCpuProfilingDataAction;
import org.jvmmonitor.internal.ui.properties.cpu.actions.FindAction;
import org.jvmmonitor.internal.ui.properties.cpu.actions.ResumeCpuProfilingAction;
import org.jvmmonitor.internal.ui.properties.cpu.actions.SuspendCpuProfilingAction;
import org.jvmmonitor.ui.Activator;

/**
 * The CPU section.
 */
public class CpuSection extends AbstractJvmPropertySection {

    /** The default profiler sampling period. */
    private static final Integer DEFAULT_SAMPLING_PERIOD = 50;

    /** The default profiler type. */
    private static final ProfilerType DEFAULT_PROFILER_TYPE = ProfilerType.SAMPLING;

    /** The call tree. */
    CallTreeTabPage callTree;

    /** The hot spots. */
    HotSpotsTabPage hotSpots;

    /** The caller and callee. */
    CallerCalleeTabPage callerCallee;

    /** The action to resume CPU profiler. */
    ResumeCpuProfilingAction resumeCpuProfilingAction;

    /** The action to suspend CPU profiler. */
    SuspendCpuProfilingAction suspendCpuProfilingAction;

    /** The action to clear CPU profiling data. */
    ClearCpuProfilingDataAction clearCpuProfilingDataAction;

    /** The action to dump CPU profiling data. */
    DumpCpuProfilingDataAction dumpCpuProfilingDataAction;

    /** The separator. */
    private Separator separator;

    /** The CPU model change listener. */
    private ICpuModelChangeListener cpuModelChangeListener;

    /**
     * The constructor.
     */
    public CpuSection() {
        suspendCpuProfilingAction = new SuspendCpuProfilingAction(this);
        resumeCpuProfilingAction = new ResumeCpuProfilingAction(this);
        clearCpuProfilingDataAction = new ClearCpuProfilingDataAction(this);
        dumpCpuProfilingDataAction = new DumpCpuProfilingDataAction(this);
        separator = new Separator();

        cpuModelChangeListener = new ICpuModelChangeListener() {
            @Override
            public void modelChanged(CpuModelEvent event) {
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        refreshUI();
                    }
                });
            }
        };
    }

    /*
     * @see AbstractPropertySection#createControls(Composite,
     * TabbedPropertySheetPage)
     */
    @Override
    public void createControls(Composite parent) {
        contributeToActionBars();

        // hide the highlight margin with SWT.FLAT
        final CTabFolder tabFolder = getWidgetFactory().createTabFolder(parent,
                SWT.BOTTOM | SWT.FLAT);

        tabFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                tabSelectionChanged(tabFolder.getSelection());
            }
        });

        callTree = new CallTreeTabPage(this, tabFolder);
        hotSpots = new HotSpotsTabPage(this, tabFolder);
        callerCallee = new CallerCalleeTabPage(this, tabFolder);

        setProfiledPackages();

        PlatformUI.getWorkbench().getHelpSystem()
                .setHelp(parent, IHelpContextIds.CPU_PAGE);
    }

    /*
     * @see AbstractPropertySection#refresh()
     */
    @Override
    public void refresh() {
        if (getJvm() == null || !isVisible()) {
            return;
        }

        refreshJob = new RefreshJob(NLS.bind(
                Messages.refeshCpuProfileDataJobLabel, getJvm().getPid()),
                getId() + "Tree") { //$NON-NLS-1$
            @Override
            protected void refreshModel(IProgressMonitor monitor) {
                IActiveJvm jvm = getJvm();
                if (jvm == null
                        || !jvm.isConnected()
                        || jvm.getCpuProfiler().getState() != ICpuProfiler.ProfilerState.RUNNING
                        || isRefreshSuspended()) {
                    return;
                }

                try {
                    if (jvm.isConnected()) {
                        jvm.getCpuProfiler().refreshBciProfileCache(monitor);
                        jvm.getCpuProfiler().getCpuModel().refreshMaxValues();
                    }
                } catch (JvmCoreException e) {
                    Activator.log(Messages.refreshCpuProfileDataFailedMsg, e);
                }
            }

            @Override
            protected void refreshUI() {
                if (callTree.isDisposed() || hotSpots.isDisposed()
                        || callerCallee.isDisposed()) {
                    return;
                }

                IActiveJvm jvm = getJvm();
                boolean isConnected = jvm != null && jvm.isConnected();
                refreshBackground(callTree.getChildren(), isConnected);
                refreshBackground(hotSpots.getChildren(), isConnected);
                refreshBackground(callerCallee.getChildren(), isConnected);
                CpuSection.this.refreshUI();
            }
        };
        refreshJob.schedule();
    }

    /*
     * @see AbstractJvmPropertySection#jvmModelChanged(JvmModelEvent)
     */
    @Override
    public void jvmModelChanged(JvmModelEvent event) {
        super.jvmModelChanged(event);

        IActiveJvm jvm = getJvm();
        if (jvm == null || event.jvm == null
                || event.jvm.getPid() != jvm.getPid()
                || !(event.jvm instanceof IActiveJvm)) {
            return;
        }

        if (event.state == State.JvmConnected) {
            setProfilerType();
            setProfiledPackages();
            setProfilerSamplingPeriod();
        }

        if (event.state != State.CpuProfilerConfigChanged) {
            return;
        }

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                updatePage();
            }
        });
    }

    /*
     * @see AbstractJvmPropertySection#setInput(IWorkbenchPart, ISelection,
     * IActiveJvm, IActiveJvm)
     */
    @Override
    protected void setInput(IWorkbenchPart part, ISelection selection,
            IActiveJvm newJvm, IActiveJvm oldJvm) {
        if (oldJvm != null) {
            oldJvm.getCpuProfiler().getCpuModel()
                    .removeModelChangeListener(cpuModelChangeListener);
        }
        newJvm.getCpuProfiler().getCpuModel()
                .addModelChangeListener(cpuModelChangeListener);

        if (newJvm.isConnected()) {
            setProfilerType();
            setProfiledPackages();
            setProfilerSamplingPeriod();
        }

        callTree.setInput(newJvm);
        hotSpots.setInput(newJvm);
        callerCallee.setInput(newJvm);
    }

    /*
     * @see AbstractJvmPropertySection#addToolBarActions(IToolBarManager)
     */
    @Override
    protected void addToolBarActions(IToolBarManager manager) {
        suspendCpuProfilingAction.setEnabled(false);
        resumeCpuProfilingAction.setEnabled(false);
        clearCpuProfilingDataAction.setEnabled(false);
        manager.insertAfter("defaults", separator); //$NON-NLS-1$
        if (manager.find(clearCpuProfilingDataAction.getId()) == null) {
            manager.insertAfter("defaults", clearCpuProfilingDataAction); //$NON-NLS-1$
        }
        if (manager.find(suspendCpuProfilingAction.getId()) == null) {
            manager.insertAfter("defaults", suspendCpuProfilingAction); //$NON-NLS-1$
        }
        if (manager.find(resumeCpuProfilingAction.getId()) == null) {
            manager.insertAfter("defaults", resumeCpuProfilingAction); //$NON-NLS-1$
        }
        if (manager.find(dumpCpuProfilingDataAction.getId()) == null) {
            manager.insertAfter("defaults", dumpCpuProfilingDataAction); //$NON-NLS-1$
        }
    }

    /*
     * @see AbstractJvmPropertySection#removeToolBarActions(IToolBarManager)
     */
    @Override
    protected void removeToolBarActions(IToolBarManager manager) {
        manager.remove(suspendCpuProfilingAction.getId());
        manager.remove(resumeCpuProfilingAction.getId());
        manager.remove(clearCpuProfilingDataAction.getId());
        manager.remove(dumpCpuProfilingDataAction.getId());
        manager.remove(separator);
    }

    /*
     * @see AbstractJvmPropertySection#updateActions()
     */
    @Override
    protected void updateActions() {
        if (getJvm() == null || !isVisible()) {
            return;
        }

        refreshJob = new RefreshJob(NLS.bind(
                Messages.refreshLocalToolbarJobLabel, getJvm().getPid()),
                getId() + "Actions") { //$NON-NLS-1$
            private boolean isCpuProfilerReady;
            private boolean isPackageSpecified;
            private boolean enableSuspend;

            @Override
            protected void refreshModel(IProgressMonitor monitor) {
                /*
                 * The state can be changed not only by this plug-in but also by
                 * any other applications via MXBean.
                 */
                isCpuProfilerReady = isCpuProfilerReady();
                isPackageSpecified = isPackageSpecified();
                enableSuspend = isCpuProfilerRunning();
            }

            @Override
            protected void refreshUI() {
                boolean enableResume = !enableSuspend;
                IActiveJvm jvm = getJvm();
                boolean isConnected = jvm != null && jvm.isConnected();

                suspendCpuProfilingAction.setEnabled(enableSuspend
                        && isCpuProfilerReady && isConnected);
                resumeCpuProfilingAction.setEnabled(enableResume
                        && isCpuProfilerReady && isPackageSpecified
                        && isConnected);
                clearCpuProfilingDataAction.setEnabled(isCpuProfilerReady
                        && isPackageSpecified && isConnected);
                dumpCpuProfilingDataAction.setEnabled(!hasErrorMessage());
            }
        };

        refreshJob.schedule();
    }

    /*
     * @see AbstractJvmPropertySection#updatePage()
     */
    @Override
    protected void updatePage() {
        super.updatePage();

        IActiveJvm jvm = getJvm();
        if (jvm == null || !jvm.isConnected()) {
            return;
        }

        refreshJob = new RefreshJob(NLS.bind(Messages.refeshCpuSectionJobLabel,
                jvm.getPid()), getId() + "Page") { //$NON-NLS-1$

            /** The state indicating if packages are specified. */
            private boolean isPackageSpecified;

            @Override
            protected void refreshModel(IProgressMonitor monitor) {
                isPackageSpecified = isPackageSpecified();
            }

            @Override
            protected void refreshUI() {
                updatePage(isPackageSpecified);
            }
        };

        refreshJob.schedule();
    }

    /**
     * Clears the CPU profile data.
     */
    public void clear() {
        IActiveJvm jvm = getJvm();
        if (jvm != null) {
            try {
                jvm.getCpuProfiler().clear();
            } catch (JvmCoreException e) {
                Activator.log(Messages.clearCpuProfileDataFailedMsg, e);
            }
        }
    }

    /**
     * Notifies that tab selection has been changed.
     * 
     * @param tabItem
     *            The tab item
     */
    public void tabSelectionChanged(CTabItem tabItem) {
        clearStatusLine();

        AbstractTabPage page = (AbstractTabPage) tabItem.getControl();
        AbstractFilteredTree filteredTree = page.getFilteredTrees().get(0);
        FindAction findAction = (FindAction) getActionBars()
                .getGlobalActionHandler(ActionFactory.FIND.getId());
        if (findAction != null) {
            findAction.setViewer(filteredTree.getViewer(),
                    filteredTree.getViewerType());
        }
    }

    /**
     * Update the page.
     * 
     * @param isPackageSpecified
     *            True if packages are specified
     */
    void updatePage(boolean isPackageSpecified) {
        if (!callTree.isDisposed() && !hotSpots.isDisposed()
                && !callerCallee.isDisposed()) {
            callTree.updatePage(isPackageSpecified);
            hotSpots.updatePage(isPackageSpecified);
            callerCallee.updatePage(isPackageSpecified);
        }
    }

    /**
     * Refreshes the UI.
     */
    void refreshUI() {
        if (hotSpots.isDisposed() || callTree.isDisposed()
                || callerCallee.isDisposed()) {
            return;
        }
        if (callTree.isVisible()) {
            callTree.refresh();
        }
        if (hotSpots.isVisible()) {
            hotSpots.refresh();
        }
        if (callerCallee.isVisible()) {
            callerCallee.refresh();
        }
    }

    /**
     * Gets the state indicating if section is visible.
     * 
     * @return <tt>true</tt> if section is visible
     */
    private boolean isVisible() {
        final boolean[] visible = new boolean[] { true };
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                visible[0] = callTree != null
                        && hotSpots != null
                        && callerCallee != null
                        && !callTree.isDisposed()
                        && !hotSpots.isDisposed()
                        && !callerCallee.isDisposed()
                        && (callTree.isVisible() || hotSpots.isVisible() || callerCallee
                                .isVisible());
            }
        });
        return visible[0];
    }

    /**
     * Contributes to action bars.
     */
    private void contributeToActionBars() {
        IActionBars actionBars = getActionBars();

        OpenDeclarationAction.createOpenDeclarationAction(actionBars);
        actionBars.setGlobalActionHandler(ActionFactory.FIND.getId(),
                new FindAction());
        CopyAction.createCopyAction(actionBars);
    }

    /**
     * Sets the profiled packages to CPU profiler.
     */
    private void setProfiledPackages() {
        new Job(Messages.setProfiledPackagesJobLabel) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    IActiveJvm jvm = getJvm();
                    if (jvm == null) {
                        return Status.CANCEL_STATUS;
                    }
                    Set<String> packages = jvm.getCpuProfiler()
                            .getProfiledPackages();
                    if (packages.isEmpty()) {
                        String packagesString = Activator.getDefault()
                                .getDialogSettings(CpuSection.class.getName())
                                .get(IConstants.PACKAGES_KEY);
                        if (packagesString != null) {
                            if (packagesString.contains(",")) { //$NON-NLS-1$
                                for (String item : packagesString.split(",")) { //$NON-NLS-1$
                                    packages.add(item);
                                }
                            } else if (!packagesString.isEmpty()) {
                                packages.add(packagesString);
                            }

                            jvm.getCpuProfiler().setProfiledPackages(packages);
                        }
                    }
                } catch (JvmCoreException e) {
                    Activator.log(Messages.setProfiledPackagesFailedMsg, e);
                }
                return Status.OK_STATUS;
            }

        }.schedule();
    }

    /**
     * Sets the profiler sampling period.
     */
    private void setProfilerSamplingPeriod() {
        IActiveJvm jvm = getJvm();
        if (jvm == null) {
            return;
        }
        Integer period = jvm.getCpuProfiler().getSamplingPeriod();
        if (period != null) {
            return;
        }

        String periodString = Activator.getDefault()
                .getDialogSettings(CpuSection.class.getName())
                .get(IConstants.PROFILER_SAMPLING_PERIOD_KEY);
        if (periodString != null) {
            try {
                period = Integer.valueOf(periodString);
            } catch (NumberFormatException e) {
                // do nothing
            }
        }
        if (period == null) {
            period = DEFAULT_SAMPLING_PERIOD;
        }

        jvm.getCpuProfiler().setSamplingPeriod(period);
    }

    /**
     * Sets the profiler type.
     */
    private void setProfilerType() {
        IActiveJvm jvm = getJvm();
        if (jvm == null) {
            return;
        }

        ProfilerType type = jvm.getCpuProfiler().getProfilerType();
        if (type != null) {
            return;
        }

        String typeString = Activator.getDefault()
                .getDialogSettings(CpuSection.class.getName())
                .get(IConstants.PROFILER_TYPE_KEY);
        if (typeString != null) {
            for (ProfilerType profilerType : ProfilerType.values()) {
                if (profilerType.name().equals(typeString)) {
                    type = profilerType;
                    break;
                }
            }
        }
        if (type == null
                || jvm.getCpuProfiler().getState() == ProfilerState.AGENT_NOT_LOADED) {
            type = DEFAULT_PROFILER_TYPE;
        }

        jvm.getCpuProfiler().setProfilerType(type);
    }

    /**
     * Gets the state indicating if CPU profiler is ready.
     * 
     * @return True if CPU profiler is ready
     */
    boolean isCpuProfilerReady() {
        IActiveJvm jvm = getJvm();
        return jvm != null
                && jvm.isConnected()
                && (jvm.getCpuProfiler().getState() == ProfilerState.READY || jvm
                        .getCpuProfiler().getState() == ProfilerState.RUNNING);
    }

    /**
     * Gets the state indicating if CPU profiler is running.
     * 
     * @return True if CPU profiler is running
     */
    boolean isCpuProfilerRunning() {
        IActiveJvm jvm = getJvm();
        return jvm != null && jvm.isConnected()
                && jvm.getCpuProfiler().getState() == ProfilerState.RUNNING;
    }

    /**
     * Gets the state indicating if the profiled packages are specified.
     * 
     * @return True if the profiled packages are specified
     */
    boolean isPackageSpecified() {
        IActiveJvm jvm = getJvm();
        if (jvm == null
                || jvm.getCpuProfiler().getState() == ProfilerState.AGENT_NOT_LOADED) {
            return false;
        }

        try {
            Set<String> packages = jvm.getCpuProfiler().getProfiledPackages();
            return packages != null && !packages.isEmpty();
        } catch (JvmCoreException e) {
            Activator.log(Messages.getProfiledPackagesFailedMsg, e);
            return false;
        }
    }
}
