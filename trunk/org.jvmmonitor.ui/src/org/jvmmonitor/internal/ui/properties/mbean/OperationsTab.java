/*******************************************************************************
 * Copyright (c) 2010 JVM Monitor project. All rights reserved. 
 * 
 * This code is distributed under the terms of the Eclipse Public License v1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jvmmonitor.internal.ui.properties.mbean;

import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ObjectName;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.jvmmonitor.core.IActiveJvm;
import org.jvmmonitor.core.JvmCoreException;
import org.jvmmonitor.internal.ui.RefreshJob;
import org.jvmmonitor.internal.ui.properties.AbstractJvmPropertySection;
import org.jvmmonitor.ui.Activator;
import org.jvmmonitor.ui.ISharedImages;

/**
 * The operations tab.
 */
public class OperationsTab extends Composite {

    /** The table viewer. */
    TableViewer tableViewer;

    /** The action to invoke MBean operation. */
    InvokeAction invokeAction;

    /** The tab item. */
    CTabItem tabItem;

    /** The tab folder. */
    private CTabFolder tabFolder;

    /** The content provider. */
    OperationsContentProvider contentProvider;

    /** The object name. */
    ObjectName objectName;

    /** The property section. */
    AbstractJvmPropertySection section;

    /** The method image. */
    private Image methodImage;

    /**
     * The constructor.
     * 
     * @param tabFolder
     *            The tab folder
     * @param section
     *            The property section
     */
    public OperationsTab(CTabFolder tabFolder,
            AbstractJvmPropertySection section) {
        super(tabFolder, SWT.NONE);

        this.tabFolder = tabFolder;
        this.section = section;
        addTabItem();

        setLayout(new FillLayout());

        tableViewer = new TableViewer(this, SWT.NONE);
        tableViewer.setLabelProvider(new OperationsLabelProvider());
        contentProvider = new OperationsContentProvider();
        tableViewer.setContentProvider(contentProvider);

        createContextMenu();
        configureTable();
    }

    /*
     * @see Widget#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
        if (methodImage != null) {
            methodImage.dispose();
        }
    }

    /**
     * Notifies that selection has been changed.
     * 
     * @param selection
     *            The selection
     */
    public void selectionChanged(ISelection selection) {
        if (!(selection instanceof StructuredSelection)) {
            return;
        }

        objectName = getObjectName((StructuredSelection) selection);
        if (objectName == null) {
            return;
        }

        tableViewer.setInput(objectName);
        invokeAction.selectionChanged(objectName);
        contentProvider.refresh(null);
        tableViewer.refresh();

        refresh();
    }

    /**
     * Refreshes.
     */
    public void refresh() {
        RefreshJob refreshJob = new RefreshJob(
                Messages.refreshOperationsTabJobLabel, section.getId()
                        + OperationsTab.class.getName()) {

            /** The MBean operations. */
            private MBeanOperationInfo[] operations;

            @Override
            protected void refreshModel(IProgressMonitor monitor) {
                IActiveJvm jvm = section.getJvm();
                if (jvm == null || !jvm.isConnected()) {
                    return;
                }

                MBeanInfo info = null;
                if (objectName != null) {
                    try {
                        info = jvm.getMBeanServer().getMBeanInfo(objectName);
                    } catch (JvmCoreException e) {
                        Activator.log(Messages.getMBeanInfoFailedMsg, e);
                        return;
                    }
                }

                if (info != null) {
                    operations = info.getOperations();
                    contentProvider.refresh(operations);
                }
            }

            @Override
            protected void refreshUI() {
                if (operations == null || operations.length == 0) {
                    tabItem.dispose();
                    return;
                }

                if (tabItem.isDisposed()) {
                    addTabItem();
                }

                if (!tableViewer.getControl().isDisposed()) {
                    tableViewer.refresh();
                }
            }
        };
        refreshJob.schedule();
    }

    /**
     * Adds the tab item.
     */
    void addTabItem() {
        tabItem = new CTabItem(tabFolder, SWT.NONE);
        tabItem.setText(Messages.operationsTabLabel);
        tabItem.setImage(getMethodImage());
        tabItem.setControl(this);
    }

    /**
     * Gets the object name.
     * 
     * @param selection
     *            The selection
     * @return The object name
     */
    private ObjectName getObjectName(StructuredSelection selection) {
        Object element = selection.getFirstElement();
        if (element instanceof MBeanType) {
            MBeanName[] mBeanName = ((MBeanType) element).getMBeanNames();
            if (mBeanName != null && mBeanName.length == 1) {
                return mBeanName[0].getObjectName();
            }
        } else if (element instanceof MBeanName) {
            return ((MBeanName) element).getObjectName();
        }

        return null;
    }

    /**
     * Configure the table adding columns.
     */
    private void configureTable() {
        Table table = tableViewer.getTable();
        if (table.isDisposed()) {
            return;
        }

        table.setLinesVisible(true);
        table.setHeaderVisible(true);

        TableColumn column = new TableColumn(table, SWT.NONE);
        column.setText(Messages.operationColumnLabel);
        column.setWidth(300);
        column.setToolTipText(Messages.operationColumnToolTip);
    }

    /**
     * Creates the context menu.
     */
    private void createContextMenu() {

        // create actions
        invokeAction = new InvokeAction(tableViewer, section);
        tableViewer.addSelectionChangedListener(invokeAction);

        // create menu manager
        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                manager.add(invokeAction);
            }
        });

        // create context menu
        Menu menu = menuMgr.createContextMenu(tableViewer.getControl());
        tableViewer.getControl().setMenu(menu);
    }

    /**
     * Gets the method image.
     * 
     * @return The method image
     */
    private Image getMethodImage() {
        if (methodImage == null || methodImage.isDisposed()) {
            methodImage = Activator.getImageDescriptor(
                    ISharedImages.METHOD_IMG_PATH).createImage();
        }
        return methodImage;
    }
}