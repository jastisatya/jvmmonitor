/*******************************************************************************
 * Copyright (c) 2010 JVM Monitor project. All rights reserved. 
 * 
 * This code is distributed under the terms of the Eclipse Public License v1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jvmmonitor.internal.ui.properties.memory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jdt.ui.actions.JdtActionConstants;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IActionBars;
import org.jvmmonitor.internal.ui.IConfigurableColumns;
import org.jvmmonitor.internal.ui.actions.ConfigureColumnsAction;
import org.jvmmonitor.internal.ui.actions.CopyAction;
import org.jvmmonitor.internal.ui.actions.OpenDeclarationAction;
import org.jvmmonitor.ui.Activator;

/**
 * The heap composite.
 */
public class HeapComposite extends Composite implements IConfigurableColumns,
        IPropertyChangeListener {

    /** The heap viewer. */
    private TreeViewer heapViewer;

    /** the open action */
    OpenDeclarationAction openAction;

    /** The action to configure columns. */
    ConfigureColumnsAction configureColumnsAction;

    /** The copy action. */
    CopyAction copyAction;

    /** The columns with visibility state. */
    private LinkedHashMap<String, Boolean> columns;

    /**
     * The constructor.
     * 
     * @param parent
     *            The parent composite
     * @param actionBars
     *            The action bars
     */
    public HeapComposite(Composite parent, IActionBars actionBars) {
        super(parent, SWT.NONE);

        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        setLayout(layout);

        heapViewer = new HeapFilteredTree(this).getViewer();
        heapViewer.setContentProvider(new HeapContentProvider(heapViewer));
        heapViewer.setLabelProvider(new HeapLabelProvider(heapViewer));

        loadColumnsPreference();
        configureTree();
        createContextMenu(actionBars);

        Activator.getDefault().getPreferenceStore()
                .addPropertyChangeListener(this);
    }

    /*
     * @see IConfigurableColumn#getColumns()
     */
    @Override
    public List<String> getColumns() {
        ArrayList<String> columnLabels = new ArrayList<String>();
        HeapColumn[] values = HeapColumn.values();
        for (HeapColumn value : values) {
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
        return true;
    }

    /*
     * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (!event.getProperty().equals(getId())
                || heapViewer.getTree().isDisposed()) {
            return;
        }

        String columnsString = (String) event.getNewValue();
        if (columnsString == null || columnsString.isEmpty()) {
            return;
        }

        setColumns(columnsString);
        configureTree();
        refresh();
    }

    /*
     * @see AbstractJvmPropertySection#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
        Activator.getDefault().getPreferenceStore()
                .removePropertyChangeListener(this);
    }

    /**
     * Sets the heap input.
     * 
     * @param heapInput
     *            The heap input
     */
    public void setInput(IHeapInput heapInput) {
        heapViewer.setInput(heapInput);
    }

    /**
     * Refreshes the appearance.
     */
    public void refresh() {
        if (!heapViewer.getControl().isDisposed()) {
            heapViewer.refresh();
        }
    }

    /**
     * Creates the context menu.
     * 
     * @param actionBars
     *            The action bars
     */
    private void createContextMenu(IActionBars actionBars) {
        openAction = new OpenDeclarationAction();
        copyAction = new CopyAction();
        configureColumnsAction = new ConfigureColumnsAction(this);
        heapViewer.addSelectionChangedListener(openAction);
        heapViewer.addSelectionChangedListener(copyAction);

        actionBars.setGlobalActionHandler(JdtActionConstants.OPEN, openAction);
        heapViewer.addOpenListener(new IOpenListener() {
            @Override
            public void open(OpenEvent event) {
                openAction.run();
            }
        });

        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                manager.add(openAction);
                manager.add(copyAction);
                manager.add(new Separator());
                manager.add(configureColumnsAction);
            }
        });

        Menu menu = menuMgr.createContextMenu(heapViewer.getControl());
        heapViewer.getControl().setMenu(menu);
    }

    /**
     * Loads the columns preference.
     */
    private void loadColumnsPreference() {
        columns = new LinkedHashMap<String, Boolean>();
        String value = Activator.getDefault().getPreferenceStore()
                .getString(getId());
        if (value.isEmpty()) {
            for (HeapColumn column : HeapColumn.values()) {
                columns.put(column.label, true);
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
        Tree tree = heapViewer.getTree();
        if (tree.isDisposed()) {
            return;
        }

        for (TreeColumn column : tree.getColumns()) {
            column.dispose();
        }

        tree.setLinesVisible(true);
        tree.setHeaderVisible(true);

        for (Entry<String, Boolean> entry : columns.entrySet()) {
            HeapColumn column = HeapColumn.getColumn(entry.getKey());
            if (!columns.get(column.label)) {
                continue;
            }

            TreeColumn treeColumn = new TreeColumn(tree, SWT.NONE);
            treeColumn.setText(column.label);
            treeColumn.setWidth(column.defalutWidth);
            treeColumn.setAlignment(column.alignment);
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
        Tree tree = heapViewer.getTree();
        int columnIndex = tree.indexOf(treeColumn);
        HeapComparator sorter = (HeapComparator) heapViewer.getComparator();

        if (sorter != null && columnIndex == sorter.getColumnIndex()) {
            sorter.reverseSortDirection();
        } else {
            sorter = new HeapComparator(columnIndex);
            heapViewer.setComparator(sorter);
        }
        tree.setSortColumn(treeColumn);
        tree.setSortDirection(sorter.getSortDirection());
        refresh();
    }
}
