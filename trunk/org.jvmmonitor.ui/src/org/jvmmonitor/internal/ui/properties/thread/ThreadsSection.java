/*******************************************************************************
 * Copyright (c) 2010 JVM Monitor project. All rights reserved. 
 * 
 * This code is distributed under the terms of the Eclipse Public License v1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jvmmonitor.internal.ui.properties.thread;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.jvmmonitor.core.IActiveJvm;
import org.jvmmonitor.core.IThreadElement;
import org.jvmmonitor.core.JvmCoreException;
import org.jvmmonitor.internal.ui.IHelpContextIds;
import org.jvmmonitor.internal.ui.RefreshJob;
import org.jvmmonitor.internal.ui.actions.RefreshAction;
import org.jvmmonitor.internal.ui.actions.ToggleOrientationAction;
import org.jvmmonitor.internal.ui.properties.AbstractJvmPropertySection;
import org.jvmmonitor.ui.Activator;

/**
 * The thread section.
 */
public class ThreadsSection extends AbstractJvmPropertySection {

    /** The sash form. */
    ThreadSashForm sashForm;

    /** The action to dump threads. */
    DumpThreadsAction dumpThreadsAction;

    /** The action to refresh section. */
    RefreshAction refreshAction;

    /** The separator. */
    private Separator separator;

    /** The layout menu. */
    private MenuManager layoutMenu;

    /**
     * The constructor.
     */
    public ThreadsSection() {
        createActions();
    }

    /*
     * @see AbstractPropertySection#refresh()
     */
    @Override
    public void refresh() {
        if (getJvm() == null) {
            return;
        }

        refreshJob = new RefreshJob(NLS.bind(
                Messages.refreshThreadsSectionJobLabel, getJvm().getPid()),
                getId()) {

            @Override
            protected void refreshModel(IProgressMonitor monitor) {
                IActiveJvm jvm = getJvm();
                if (jvm != null && jvm.isConnected() && !isRefreshSuspended()) {
                    try {
                        jvm.getMBeanServer().refreshThreadCache();
                    } catch (JvmCoreException e) {
                        Activator.log(null, e);
                    }
                }
            }

            @Override
            protected void refreshUI() {
                IActiveJvm jvm = getJvm();
                boolean isConnected = jvm != null && jvm.isConnected();
                dumpThreadsAction.setEnabled(!hasErrorMessage());
                refreshAction.setEnabled(isConnected);

                if (!sashForm.isDisposed()) {
                    refreshBackground(sashForm.getChildren(), isConnected);
                    sashForm.refresh();
                }
            }
        };

        refreshJob.schedule();
    }

    /*
     * @see AbstractJvmPropertySection#createControls(Composite)
     */
    @Override
    protected void createControls(Composite parent) {
        sashForm = new ThreadSashForm(parent, getActionBars());

        PlatformUI.getWorkbench().getHelpSystem()
                .setHelp(parent, IHelpContextIds.THREADS_PAGE);
    }

    /*
     * @see AbstractJvmPropertySection#setInput(IWorkbenchPart, ISelection,
     * IActiveJvm, IActiveJvm)
     */
    @Override
    protected void setInput(IWorkbenchPart part, ISelection selection,
            final IActiveJvm newJvm, IActiveJvm oldJvm) {
        sashForm.setInput(new IThreadInput() {
            @Override
            public IThreadElement[] getThreadListElements() {
                return newJvm.getMBeanServer().getThreadCache();
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
        if (manager.find(dumpThreadsAction.getId()) == null) {
            manager.insertAfter("defaults", dumpThreadsAction); //$NON-NLS-1$
        }
    }

    /*
     * @see AbstractJvmPropertySection#removeToolBarActions(IToolBarManager)
     */
    @Override
    protected void removeToolBarActions(IToolBarManager manager) {
        manager.remove(separator);
        manager.remove(refreshAction.getId());
        manager.remove(dumpThreadsAction.getId());
    }

    /*
     * @see AbstractJvmPropertySection#addLocalMenus(IMenuManager)
     */
    @Override
    protected void addLocalMenus(IMenuManager manager) {
        layoutMenu = new MenuManager(Messages.layoutLabel);
        manager.add(layoutMenu);
        for (ToggleOrientationAction action : sashForm.getOrientationActions()) {
            layoutMenu.add(action);
        }
    }

    /*
     * @see AbstractJvmPropertySection#removeLocalMenus(IMenuManager)
     */
    @Override
    protected void removeLocalMenus(IMenuManager manager) {
        manager.remove(layoutMenu);
    }

    /**
     * Creates the actions.
     */
    private void createActions() {
        dumpThreadsAction = new DumpThreadsAction(this);
        refreshAction = new RefreshAction(this);
        separator = new Separator();
    }
}
