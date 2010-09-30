/*******************************************************************************
 * Copyright (c) 2010 JVM Monitor project. All rights reserved. 
 * 
 * This code is distributed under the terms of the Eclipse Public License v1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jvmmonitor.internal.ui.views;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.jvmmonitor.core.IActiveJvm;
import org.jvmmonitor.core.mbean.IMBeanServer;
import org.jvmmonitor.core.mbean.IMBeanServerChangeListener;
import org.jvmmonitor.core.mbean.IMonitoredMXBeanAttribute;
import org.jvmmonitor.core.mbean.MBeanServerEvent;
import org.jvmmonitor.core.mbean.MBeanServerEvent.MBeanServerState;
import org.jvmmonitor.internal.ui.actions.PreferencesAction;

/**
 * The JVM explorer view.
 */
public class JvmExplorer extends ViewPart implements
        ITabbedPropertySheetPageContributor {

    /** The tree viewer. */
    private JvmTreeViewer treeViewer;

    /*
     * @see WorkbenchPart#createPartControl(Composite)
     */
    @Override
    public void createPartControl(Composite parent) {
        treeViewer = new JvmTreeViewer(parent, SWT.MULTI | SWT.H_SCROLL
                | SWT.V_SCROLL, getViewSite().getActionBars());

        getSite().setSelectionProvider(treeViewer);
        createLocalToolBar();
        createLocalMenus();
    }

    /*
     * @see WorkbenchPart#setFocus()
     */
    @Override
    public void setFocus() {
        treeViewer.getControl().setFocus();
    }

    /*
     * @see WorkbenchPart#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
        if (treeViewer != null) {
            treeViewer.dispose();
        }
    }

    /*
     * @see WorkbenchPart#getAdapter(Class)
     */
    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
        if (adapter == IPropertySheetPage.class) {
            return new MyTabbedPropertySheetPage(this);
        }
        return super.getAdapter(adapter);
    }

    /*
     * @see ITabbedPropertySheetPageContributor#getContributorId()
     */
    @Override
    public String getContributorId() {
        return getSite().getId();
    }

    /**
     * Gets the selection.
     * 
     * @return The selection
     */
    protected ISelection getSelection() {
        return treeViewer.getSelection();
    }

    /**
     * Creates the local tool bar.
     */
    private void createLocalToolBar() {
        IToolBarManager manager = getViewSite().getActionBars()
                .getToolBarManager();
        manager.add(new NewJvmConnectionAction(treeViewer));
        manager.update(false);
    }

    /**
     * Creates the local menus.
     */
    private void createLocalMenus() {
        IMenuManager manager = getViewSite().getActionBars().getMenuManager();
        manager.add(new PreferencesAction());
        manager.update(false);
    }

    /**
     * Tabbed property sheet page.
     */
    private static class MyTabbedPropertySheetPage extends TabbedPropertySheetPage {

        /**
         * The constructor.
         * 
         * @param contributor
         *            The tabbed property sheet page contributor
         */
        public MyTabbedPropertySheetPage(
                ITabbedPropertySheetPageContributor contributor) {
            super(contributor);
        }

        /** The MBean server. */
        private IMBeanServer server;

        /** The MBean server change listener. */
        private MBeanServerChangeListener listener;

        /*
         * @see TabbedPropertySheetPage#selectionChanged(IWorkbenchPart,
         * ISelection)
         */
        @Override
        public void selectionChanged(IWorkbenchPart part, ISelection selection) {
            super.selectionChanged(part, selection);
            if (!(selection instanceof IStructuredSelection)) {
                return;
            }
            Object element = ((IStructuredSelection) selection)
                    .getFirstElement();
            if (!(element instanceof IActiveJvm)) {
                return;
            }

            server = ((IActiveJvm) element).getMBeanServer();
            if (server == null) {
                return;
            }

            if (listener != null) {
                server.removeServerChangeListener(listener);
            }
            listener = new MBeanServerChangeListener(this);
            server.addServerChangeListener(listener);
        }

        /*
         * @see TabbedPropertySheetPage#resizeScrolledComposite()
         */
        @Override
        public void resizeScrolledComposite() {
            // no scroll bar except for section itself
        }

        /*
         * @see TabbedPropertySheetPage#dispose()
         */
        @Override
        public void dispose() {
            super.dispose();
            if (server != null) {
                server.removeServerChangeListener(listener);
            }
        }
    }

    /**
     * The MBean server change listener.
     */
    private static class MBeanServerChangeListener implements
            IMBeanServerChangeListener {

        /** The timeline tab ID. */
        private static final String TIMELINE_TAB_ID = "org.jvmmonitor.ui.timelineTab"; //$NON-NLS-1$

        /** The tabbed property sheet page. */
        TabbedPropertySheetPage page;

        /**
         * The constructor.
         * 
         * @param page
         *            The tabbed property sheet page
         */
        public MBeanServerChangeListener(TabbedPropertySheetPage page) {
            this.page = page;
        }

        /*
         * @see IMBeanServerChangeListener#serverChanged(MBeanServerEvent)
         */
        @Override
        public void serverChanged(MBeanServerEvent event) {
            Object source = event.source;
            if (!(source instanceof IMonitoredMXBeanAttribute)
                    || event.state != MBeanServerState.MonitoredAttributeAdded) {
                return;
            }

            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    page.setSelectedTab(TIMELINE_TAB_ID);
                }
            });
        }
    }
}