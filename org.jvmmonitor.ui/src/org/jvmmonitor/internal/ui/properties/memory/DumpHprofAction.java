/*******************************************************************************
 * Copyright (c) 2010 JVM Monitor project. All rights reserved. 
 * 
 * This code is distributed under the terms of the Eclipse Public License v1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jvmmonitor.internal.ui.properties.memory;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Date;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.jvmmonitor.core.IActiveJvm;
import org.jvmmonitor.core.ISnapshot.SnapshotType;
import org.jvmmonitor.core.JvmCoreException;
import org.jvmmonitor.internal.ui.properties.AbstractJvmPropertySection;
import org.jvmmonitor.internal.ui.views.OpenSnapshotAction;
import org.jvmmonitor.ui.Activator;
import org.jvmmonitor.ui.ISharedImages;

/**
 * The action to dump heap as hprof file.
 */
public class DumpHprofAction extends Action {

    /** The property section. */
    AbstractJvmPropertySection section;

    /**
     * The constructor.
     * 
     * @param section
     *            The property section
     */
    public DumpHprofAction(AbstractJvmPropertySection section) {
        setText(Messages.dumpHprofLabel);
        setImageDescriptor(Activator
                .getImageDescriptor(ISharedImages.TAKE_HPROF_DUMP_IMG_PATH));
        setDisabledImageDescriptor(Activator
                .getImageDescriptor(ISharedImages.DISABLED_TAKE_HPROF_DUMP_IMG_PATH));
        setId(getClass().getName());

        this.section = section;
    }

    /*
     * @see Action#run()
     */
    @Override
    public void run() {
        IActiveJvm jvm = section.getJvm();
        if (jvm == null) {
            return;
        }

        // get file name for remote host
        final String fileName[] = new String[1];
        final boolean transfer[] = new boolean[] { false };

        try {
            if (jvm.isRemote()) {
                final FileNameInputDialog dialog = new FileNameInputDialog(
                        getInitialFileName(jvm));

                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (dialog.open() == Window.OK) {
                            fileName[0] = dialog.getValue();
                            transfer[0] = dialog.isFileTransfered();
                        }
                    }
                });

                if (fileName[0] == null) {
                    return;
                }
            }
        } catch (JvmCoreException e) {
            Activator.log(Messages.dumpHeapDataFailedMsg, e);
            return;
        }

        dumpHprof(fileName[0], transfer[0]);
    }

    /**
     * Dumps the heap data as hprof file.
     * 
     * @param fileName
     *            The file name
     * @param transfer
     *            <tt>true</tt> to transfer file to local host
     */
    private void dumpHprof(final String fileName, final boolean transfer) {
        new Job(Messages.dumpHprofDataJobLabel) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                IActiveJvm activeJvm = section.getJvm();
                if (activeJvm == null) {
                    return Status.CANCEL_STATUS;
                }

                IFileStore fileStore = null;
                try {
                    fileStore = activeJvm.getMBeanServer().dumpHprof(fileName,
                            transfer, monitor);
                } catch (JvmCoreException e) {
                    Activator.log(Messages.dumpHeapDataFailedMsg, e);
                    return Status.CANCEL_STATUS;
                }

                if (isMemoryAnalyzerInstalled() && fileStore != null) {
                    section.setPinned(true);
                    OpenSnapshotAction.openEditor(fileStore);
                }

                return Status.OK_STATUS;
            }
        }.schedule();
    }

    /**
     * Gets the state indicating if Memory Analyzer is installed.
     * 
     * @return <tt>true</tt> if Memory Analyzer is installed
     */
    boolean isMemoryAnalyzerInstalled() {
        return Platform.getBundle("org.eclipse.mat.ui") != null; //$NON-NLS-1$
    }

    /**
     * Gets the initial file name.
     * 
     * @param jvm
     *            The active JVM
     * @return The file name, or <tt>null</tt> if file name is specified
     * @throws JvmCoreException
     */
    String getInitialFileName(IActiveJvm jvm) throws JvmCoreException {

        ObjectName objectName;
        try {
            objectName = new ObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME);
        } catch (MalformedObjectNameException e) {
            throw new JvmCoreException(IStatus.ERROR, e.getMessage(), e);
        } catch (NullPointerException e) {
            throw new JvmCoreException(IStatus.ERROR, e.getMessage(), e);
        }

        TabularData initialName = (TabularData) jvm.getMBeanServer()
                .getAttribute(objectName, "SystemProperties"); //$NON-NLS-1$
        CompositeData compisiteData = initialName
                .get(new Object[] { "user.home" }); //$NON-NLS-1$
        String home = compisiteData.values().toArray(new String[0])[1];
        StringBuffer initialFileName = new StringBuffer(home);
        initialFileName.append(File.separator).append(new Date().getTime())
                .append('.').append(SnapshotType.Hprof.getExtension());
        return initialFileName.toString();
    }

    /**
     * The input validator.
     */
    private static class InputValidator implements IInputValidator {

        /** The invalid characters. */
        private static final char[] INVALID_CHARACTERS = new char[] { '*', '?',
                '"', '<', '>', '|' };

        /**
         * The constructor.
         */
        public InputValidator() {
            // do nothing
        }

        /*
         * @see IInputValidator#isValid(String)
         */
        @Override
        public String isValid(String newText) {

            // check if text is empty
            if (newText.isEmpty()) {
                return ""; //$NON-NLS-1$
            }

            // check if invalid characters are contained
            for (char c : INVALID_CHARACTERS) {
                if (newText.indexOf(c) != -1) {
                    return Messages.pathContainsInvalidCharactersMsg;
                }
            }

            return null;
        }
    }

    /**
     * The dialog to input file name.
     */
    private class FileNameInputDialog extends InputDialog {

        /** The dialog settings key for transfer. */
        private static final String TRANSFER_KEY = "transfer";

        /** <tt>true</tt> to transfer hprof file to local host. */
        boolean isFileTransfered;

        /**
         * The constructor.
         * 
         * @param initialFileName
         *            The initial file name
         */
        public FileNameInputDialog(String initialFileName) {
            super(section.getPart().getSite().getShell(),
                    Messages.dumpHprofTitle, Messages.hprofFileLabel,
                    initialFileName, new InputValidator());
            isFileTransfered = Activator.getDefault()
                    .getDialogSettings(getClass().getName())
                    .getBoolean(TRANSFER_KEY);
        }

        /*
         * @see InputDialog#createDialogArea(Composite)
         */
        @Override
        protected Control createDialogArea(Composite parent) {
            Composite control = (Composite) super.createDialogArea(parent);
            final Button button = new Button(control, SWT.CHECK);
            button.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    isFileTransfered = button.getSelection();
                }
            });
            button.setText(Messages.transferHprofFileLabel);
            button.setSelection(isFileTransfered);
            return control;
        }

        /*
         * @see Dialog#okPressed()
         */
        @Override
        protected void okPressed() {
            Activator.getDefault().getDialogSettings(getClass().getName())
                    .put(TRANSFER_KEY, isFileTransfered);
            super.okPressed();
        }

        /**
         * Gets the state indicating if file is transfered to local host.
         * 
         * @return <tt>true</tt> if file is transfered to local host
         */
        protected boolean isFileTransfered() {
            return isFileTransfered;
        }
    }
}
