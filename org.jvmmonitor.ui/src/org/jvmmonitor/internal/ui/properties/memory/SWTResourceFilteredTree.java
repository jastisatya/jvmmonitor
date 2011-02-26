/*******************************************************************************
 * Copyright (c) 2011 JVM Monitor project. All rights reserved. 
 * 
 * This code is distributed under the terms of the Eclipse Public License v1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jvmmonitor.internal.ui.properties.memory;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.jvmmonitor.internal.ui.actions.CopyAction;

/**
 * The thread filtered tree.
 */
public class SWTResourceFilteredTree extends FilteredTree {

    /** The copy action. */
    CopyAction copyAction;

    /**
     * The constructor.
     * 
     * @param parent
     *            The parent composite
     */
    protected SWTResourceFilteredTree(Composite parent) {
        super(parent, SWT.MULTI | SWT.FULL_SELECTION, new PatternFilter(), true);

        configureTree();
        createContextMenu();
        setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
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

    /**
     * Configure the tree adding columns.
     */
    private void configureTree() {
        for (TreeColumn column : getViewer().getTree().getColumns()) {
            column.dispose();
        }

        getViewer().getTree().setLinesVisible(true);
        getViewer().getTree().setHeaderVisible(true);

        TreeColumn treeColumn = new TreeColumn(getViewer().getTree(), SWT.NONE);
        treeColumn.setText(Messages.nameColumnLabel);
        treeColumn.setWidth(500);
        treeColumn.setAlignment(SWT.LEFT);
        treeColumn.setToolTipText(Messages.nameColumnToolTip);
    }

    /**
     * Creates the context menu.
     */
    private void createContextMenu() {
        copyAction = new CopyAction();
        getViewer().addSelectionChangedListener(copyAction);

        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                manager.add(copyAction);
            }
        });

        Menu menu = menuMgr.createContextMenu(getViewer().getControl());
        getViewer().getControl().setMenu(menu);
    }
}
