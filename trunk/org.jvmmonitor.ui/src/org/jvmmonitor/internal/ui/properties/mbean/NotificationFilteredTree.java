/*******************************************************************************
 * Copyright (c) 2010 JVM Monitor project. All rights reserved. 
 * 
 * This code is distributed under the terms of the Eclipse Public License v1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jvmmonitor.internal.ui.properties.mbean;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.management.Notification;
import javax.management.ObjectName;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.jvmmonitor.core.IActiveJvm;
import org.jvmmonitor.internal.ui.IConfigurableColumns;
import org.jvmmonitor.internal.ui.actions.ConfigureColumnsAction;
import org.jvmmonitor.internal.ui.actions.CopyAction;
import org.jvmmonitor.internal.ui.properties.AbstractJvmPropertySection;
import org.jvmmonitor.ui.Activator;

/**
 * The notification filtered tree.
 */
public class NotificationFilteredTree extends FilteredTree implements
        IConfigurableColumns, IPropertyChangeListener, IDoubleClickListener {

    /** The date format. */
    private static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss.SSS"; //$NON-NLS-1$

    /** The columns with visibility state. */
    private LinkedHashMap<String, Boolean> columns;

    /** The copy action. */
    CopyAction copyAction;

    /** The configure columns action. */
    ConfigureColumnsAction configureColumnsAction;

    /** The action to clear. */
    Action clearAction;

    /** The action to open details dialog. */
    NotificationDetailsDialogAction detailsAction;

    /** The notifications tab. */
    NotificationsTab notificationsTab;

    /** The property section. */
    private AbstractJvmPropertySection section;

    /**
     * The constructor.
     * 
     * @param notificationsTab
     *            The notifications tab
     * @param section
     *            The property section
     */
    protected NotificationFilteredTree(NotificationsTab notificationsTab,
            AbstractJvmPropertySection section) {
        super(notificationsTab, SWT.MULTI | SWT.FULL_SELECTION,
                new PatternFilter(), true);

        this.notificationsTab = notificationsTab;
        this.section = section;
        treeViewer.setLabelProvider(new NotificationsLabelProvider(treeViewer));
        treeViewer.setContentProvider(new NotificationsContentProvider());
        treeViewer.addDoubleClickListener(this);

        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalIndent = 0;
        setLayoutData(gridData);

        loadColumnsPreference();
        configureTree();
        createContextMenu();
        setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        Activator.getDefault().getPreferenceStore()
                .addPropertyChangeListener(this);
    }

    /*
     * @see FilteredTree#createControl(Composite, int)
     */
    @Override
    protected void createControl(Composite composite, int treeStyle) {
        super.createControl(composite, treeStyle);

        // adjust the indentation of filter composite
        GridData data = (GridData) filterComposite.getLayoutData();
        data.horizontalIndent = 2;
        data.verticalIndent = 2;
        filterComposite.setLayoutData(data);
    }

    /*
     * @see IConfigurableColumn#getColumns()
     */
    @Override
    public List<String> getColumns() {
        ArrayList<String> columnLabels = new ArrayList<String>();
        NotificationColumn[] values = NotificationColumn.values();
        for (NotificationColumn value : values) {
            columnLabels.add(value.label);
        }
        return columnLabels;
    }

    /*
     * @see IConfigurableColumn#getId()
     */
    @Override
    public String getId() {
        return getClass().getName();
    }

    /*
     * @see IConfigurableColumn#getDefaultVisibility(String)
     */
    @Override
    public boolean getDefaultVisibility(String column) {
        return NotificationColumn.getColumn(column).initialVisibility;
    }

    /*
     * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (!event.getProperty().equals(getId())
                || getViewer().getTree().isDisposed()) {
            return;
        }

        String columnsString = (String) event.getNewValue();
        if (columnsString == null || columnsString.isEmpty()) {
            return;
        }

        setColumns(columnsString);
        configureTree();
        getViewer().refresh();
    }

    /*
     * @see IDoubleClickListener#doubleClick(Double@Override ClickEvent)
     */
    @Override
    public void doubleClick(DoubleClickEvent event) {
        detailsAction.run();
    }

    /*
     * @see Widget#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
        Activator.getDefault().getPreferenceStore()
                .removePropertyChangeListener(this);
    }

    /**
     * Sets the input.
     * 
     * @param objectName
     *            The object name
     */
    public void setInput(ObjectName objectName) {
        IActiveJvm jvm = section.getJvm();
        if (objectName == null || jvm == null) {
            treeViewer.setInput(null);
            return;
        }

        treeViewer.setInput(jvm.getMBeanServer().getMBeanNotification()
                .getNotifications(objectName));
    }

    /**
     * Gets the previous item.
     * 
     * @return The previous item
     */
    protected Notification getPrevItem() {
        Object selectedItem = ((StructuredSelection) getViewer().getSelection())
                .getFirstElement();
        TreeItem[] items = getViewer().getTree().getItems();
        for (int i = 0; i < items.length; i++) {
            if (items[i].getData().equals(selectedItem) && i > 0) {
                return (Notification) items[i - 1].getData();
            }
        }
        return null;
    }

    /**
     * Gets the next item.
     * 
     * @return The next item
     */
    protected Notification getNextItem() {
        Object selectedItem = ((StructuredSelection) getViewer().getSelection())
                .getFirstElement();
        TreeItem[] items = getViewer().getTree().getItems();
        for (int i = 0; i < items.length; i++) {
            if (items[i].getData().equals(selectedItem) && i < items.length - 1) {
                return (Notification) items[i + 1].getData();
            }
        }
        return null;
    }

    /**
     * Selects the previous item.
     */
    protected void selectPrevItem() {
        Notification prevItem = getPrevItem();
        if (prevItem != null) {
            getViewer().setSelection(new StructuredSelection(prevItem), true);
        }
    }

    /**
     * Selects the next item.
     */
    protected void selectNextItem() {
        Notification nextItem = getNextItem();
        if (nextItem != null) {
            getViewer().setSelection(new StructuredSelection(nextItem), true);
        }
    }

    /**
     * Loads the columns preference.
     */
    private void loadColumnsPreference() {
        columns = new LinkedHashMap<String, Boolean>();
        String value = Activator.getDefault().getPreferenceStore()
                .getString(getId());
        if (value.isEmpty()) {
            for (NotificationColumn column : NotificationColumn.values()) {
                columns.put(column.label, column.initialVisibility);
            }
        } else {
            setColumns(value);
        }
    }

    /**
     * Sets the columns with given column order and visibility.
     * 
     * @param columnData
     *            The column order and visibility
     */
    private void setColumns(String columnData) {
        columns.clear();
        for (String column : columnData.split(",")) { //$NON-NLS-1$
            String[] elemnets = column.split("="); //$NON-NLS-1$
            String columnName = elemnets[0];
            boolean columnVisibility = Boolean.valueOf(elemnets[1]);
            columns.put(columnName, columnVisibility);
        }
    }

    /**
     * Configure the tree adding columns.
     */
    private void configureTree() {
        Tree tree = getViewer().getTree();
        for (TreeColumn column : tree.getColumns()) {
            column.dispose();
        }

        tree.setLinesVisible(true);
        tree.setHeaderVisible(true);
        for (Entry<String, Boolean> entry : columns.entrySet()) {
            NotificationColumn column = NotificationColumn.getColumn(entry
                    .getKey());
            if (!columns.get(column.label)) {
                continue;
            }

            TreeColumn treeColumn = new TreeColumn(getViewer().getTree(),
                    SWT.NONE);
            treeColumn.setText(column.label);
            treeColumn.setWidth(column.defalutWidth);
            treeColumn.setAlignment(column.initialAlignment);
            treeColumn.setToolTipText(column.toolTip);
            treeColumn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (e.widget instanceof TreeColumn) {
                        sortColumn((TreeColumn) e.widget);
                    }
                }
            });
        }
    }

    /**
     * Sorts the tree with given column.
     * 
     * @param treeColumn
     *            the tree column
     */
    void sortColumn(TreeColumn treeColumn) {
        int columnIndex = getViewer().getTree().indexOf(treeColumn);
        NotificationComparator sorter = (NotificationComparator) getViewer()
                .getComparator();

        if (sorter != null && columnIndex == sorter.getColumnIndex()) {
            sorter.reverseSortDirection();
        } else {
            sorter = new NotificationComparator(columnIndex);
            getViewer().setComparator(sorter);
        }
        getViewer().getTree().setSortColumn(treeColumn);
        getViewer().getTree().setSortDirection(sorter.getSortDirection());
        getViewer().refresh();
    }

    /**
     * Creates the context menu.
     */
    private void createContextMenu() {
        copyAction = new CopyAction() {
            @Override
            protected String getString(Object element) {
                if (element instanceof Notification) {
                    return getNotificationString((Notification) element);
                }
                return ""; //$NON-NLS-1$
            }
        };
        clearAction = new Action(Messages.clearLabel) {
            @Override
            public void run() {
                notificationsTab.clear();
            }
        };
        detailsAction = new NotificationDetailsDialogAction(this);
        configureColumnsAction = new ConfigureColumnsAction(this);
        getViewer().addSelectionChangedListener(copyAction);
        getViewer().addSelectionChangedListener(detailsAction);

        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                manager.add(copyAction);
                manager.add(new Separator());
                manager.add(clearAction);
                manager.add(new Separator());
                manager.add(configureColumnsAction);
                manager.add(new Separator());
                manager.add(detailsAction);
            }
        });

        Menu menu = menuMgr.createContextMenu(getViewer().getControl());
        getViewer().getControl().setMenu(menu);
    }

    /**
     * Gets the notification string.
     * 
     * @param notification
     *            The notification
     * @return The notification string
     */
    String getNotificationString(Notification notification) {
        StringBuffer buffer = new StringBuffer();

        buffer.append(
                new SimpleDateFormat(DATE_FORMAT).format(new Date(notification
                        .getTimeStamp()))).append('\n');
        buffer.append(Messages.sequenceNumberLabel).append(' ')
                .append(notification.getSequenceNumber()).append('\n');
        buffer.append(Messages.sourceLabel).append(' ')
                .append(notification.getSource()).append('\n');
        buffer.append(Messages.typeLabel).append(' ')
                .append(notification.getType()).append("\n\n"); //$NON-NLS-1$
        buffer.append(notification.getMessage()).append('\n');

        new AttributeParser()
                .parseObject(buffer, notification.getUserData(), 0);

        return buffer.toString();
    }
}
