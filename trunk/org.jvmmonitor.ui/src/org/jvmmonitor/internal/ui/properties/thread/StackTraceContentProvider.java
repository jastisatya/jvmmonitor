/*******************************************************************************
 * Copyright (c) 2010 JVM Monitor project. All rights reserved. 
 * 
 * This code is distributed under the terms of the Eclipse Public License v1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jvmmonitor.internal.ui.properties.thread;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.jvmmonitor.core.IThreadElement;

/**
 * The content provider for stack trace list.
 */
public class StackTraceContentProvider implements IStructuredContentProvider {

    /** The thread list element. */
    private IThreadElement element;

    /*
     * @see IContentProvider#dispose()
     */
    @Override
    public void dispose() {
        // do nothing
    }

    /*
     * @see IContentProvider#inputChanged(Viewer, Object, Object)
     */
    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if (!(newInput instanceof IStructuredSelection)) {
            return;
        }
        IStructuredSelection selection = (IStructuredSelection) newInput;
        Object fisrtElement = selection.getFirstElement();

        if (fisrtElement instanceof IThreadElement) {
            element = (IThreadElement) fisrtElement;
        }
    }

    /*
     * @see IStructuredContentProvider#getElements(Object)
     */
    @Override
    public Object[] getElements(Object inputElement) {
        if (element == null) {
            return null;
        }

        return element.getStackTraceElements();
    }
}
