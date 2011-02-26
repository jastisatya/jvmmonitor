/*******************************************************************************
 * Copyright (c) 2010 JVM Monitor project. All rights reserved. 
 * 
 * This code is distributed under the terms of the Eclipse Public License v1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jvmmonitor.internal.ui.properties.memory;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.PageBook;
import org.jvmmonitor.core.IActiveJvm;
import org.jvmmonitor.core.IHeapElement;
import org.jvmmonitor.core.ISWTResourceElement;
import org.jvmmonitor.internal.ui.IHelpContextIds;
import org.jvmmonitor.internal.ui.properties.AbstractJvmPropertySection;

/**
 * The memory section.
 */
public class MemorySection extends AbstractJvmPropertySection {

    /** The default tab height. */
    private static int defaultTabHeight;

    /** The heap histogram page. */
    HeapHistogramPage heapHistogramPage;

    /** The SWT resource page. */
    SWTResourcesPage swtResourcePage;

    /** The tab folder. */
    private CTabFolder tabFolder;

    private PageBook heapHistogramPageBook;

    private Label heapHistogramMessageLabel;

    /*
     * @see AbstractPropertySection#createControls(Composite,
     * TabbedPropertySheetPage)
     */
    @Override
    public void createControls(Composite parent) {
        tabFolder = getWidgetFactory().createTabFolder(parent,
                SWT.BOTTOM | SWT.FLAT);

        heapHistogramPageBook = new PageBook(tabFolder, SWT.NONE);
        heapHistogramMessageLabel = new Label(heapHistogramPageBook, SWT.NONE);
        heapHistogramPage = new HeapHistogramPage(this, heapHistogramPageBook,
                tabFolder, getActionBars());

        swtResourcePage = new SWTResourcesPage(this, tabFolder, getActionBars());

        defaultTabHeight = tabFolder.getTabHeight();
        tabFolder.setTabHeight(0);

        PlatformUI.getWorkbench().getHelpSystem()
                .setHelp(parent, IHelpContextIds.MEMORY_PAGE);
    }

    /*
     * @see AbstractPropertySection#refresh()
     */
    @Override
    public void refresh() {
        if (getJvm() == null) {
            return;
        }
        if (!heapHistogramPage.isDisposed() && heapHistogramPage.isVisible()) {
            heapHistogramPage.refresh();
        }
        if (!swtResourcePage.isDisposed() && swtResourcePage.isVisible()) {
            swtResourcePage.refresh(false);
        }
    }

    /*
     * @see AbstractJvmPropertySection#setInput(IWorkbenchPart, ISelection,
     * IActiveJvm, IActiveJvm)
     */
    @Override
    protected void setInput(IWorkbenchPart part, ISelection selection,
            final IActiveJvm newJvm, IActiveJvm oldJvm) {
        int tabHeight;
        if (newJvm.getSWTResourceMonitor().isSupported()) {
            tabHeight = defaultTabHeight;
        } else {
            tabHeight = 0;
            tabFolder.setSelection(0);
        }
        tabFolder.setTabHeight(tabHeight);
        tabFolder.layout();

        heapHistogramPage.setInput(new IHeapInput() {
            @Override
            public IHeapElement[] getHeapListElements() {
                return newJvm.getMBeanServer().getHeapCache();
            }
        });

        swtResourcePage.setInput(new ISWTResorceInput() {
            @Override
            public ISWTResourceElement[] getSWTResourceElements() {
                return newJvm.getSWTResourceMonitor().getResources();
            }
        });
    }

    /*
     * @see AbstractJvmPropertySection#addToolBarActions(IToolBarManager)
     */
    @Override
    protected void addToolBarActions(IToolBarManager manager) {
        if (tabFolder.getSelectionIndex() == 0) {
            heapHistogramPage.addToolBarActions(manager);
        } else {
            swtResourcePage.addToolBarActions(manager);
        }
    }

    /*
     * @see AbstractJvmPropertySection#removeToolBarActions(IToolBarManager)
     */
    @Override
    protected void removeToolBarActions(IToolBarManager manager) {
        if (tabFolder.getSelectionIndex() == 0) {
            heapHistogramPage.removeToolBarActions(manager);
        } else {
            swtResourcePage.removeToolBarActions(manager);
        }
    }

    /*
     * @see AbstractJvmPropertySection#addLocalMenus(IMenuManager)
     */
    @Override
    protected void addLocalMenus(IMenuManager manager) {
        if (tabFolder.getSelectionIndex() == 1) {
            swtResourcePage.addLocalMenus(manager);
        }
    }

    /*
     * @see AbstractJvmPropertySection#removeLocalMenus(IMenuManager)
     */
    @Override
    protected void removeLocalMenus(IMenuManager manager) {
        if (tabFolder.getSelectionIndex() == 1) {
            swtResourcePage.removeLocalMenus(manager);
        }
    }

    @Override
    protected void activateSection() {
        heapHistogramPage
                .updateLocalToolBar(tabFolder.getSelectionIndex() == 0);
        swtResourcePage.updateLocalToolBar(tabFolder.getSelectionIndex() == 1);
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
                setMessageLabel(Messages.notSupportedOnRemoteHostMsg);
            } else if (!heapHistogramPage.isSupported()) {
                setMessageLabel(Messages.notSupportedForEclipseItselfOn64bitOS);
            } else {
                setMessageLabel("");//$NON-NLS-1$
            }
            heapHistogramPageBook.showPage(heapHistogramMessageLabel.getText()
                    .isEmpty() ? heapHistogramPage : heapHistogramMessageLabel);
        }
    }

    /**
     * Sets the message label inside the heap histogram page.
     * 
     * @param message
     *            The message
     */
    private void setMessageLabel(String message) {
        if (!heapHistogramMessageLabel.isDisposed()
                && !heapHistogramPageBook.isDisposed()) {
            heapHistogramMessageLabel.setText(message);
            heapHistogramPageBook.showPage(heapHistogramMessageLabel);
        }
    }
}
