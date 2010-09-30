/*******************************************************************************
 * Copyright (c) 2010 JVM Monitor project. All rights reserved. 
 * 
 * This code is distributed under the terms of the Eclipse Public License v1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jvmmonitor.internal.ui.properties.cpu.actions;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.jvmmonitor.core.Activator;
import org.jvmmonitor.internal.ui.properties.cpu.PackageLabelProvider;

/**
 * The package selection dialog.
 */
public class PackageSelectionDialog extends ElementListSelectionDialog {

    /** The resource name for external plug-in libraries. */
    private static final String EXTERNAL_PLUGIN_LIBRARIES = "External Plug-in Libraries"; //$NON-NLS-1$

    /** The filtering packages. */
    private Object[] filteringPackages;

    /**
     * The constructor.
     * 
     * @param parent
     *            The parent shell
     * @param filteringPackages
     *            The packages to be filtered out
     */
    public PackageSelectionDialog(Shell parent, Object[] filteringPackages) {
        super(parent, new PackageLabelProvider());
        this.filteringPackages = filteringPackages;

        IJavaModel javaModel = JavaCore.create(ResourcesPlugin.getWorkspace()
                .getRoot());
        setElements(getPackageFragments(javaModel));
        setMultipleSelection(true);
        setMessage(Messages.packageSelectionDialogMessage);
    }

    /*
     * @see ElementListSelectionDialog#createDialogArea(Composite)
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Control control = super.createDialogArea(parent);
        getShell().setText(Messages.packageSelectionDialogTitle);
        return control;
    }

    /**
     * Gets the package fragments.
     * 
     * @param javaModel
     *            The java model
     * @return The package fragments
     */
    private Object[] getPackageFragments(IJavaModel javaModel) {

        Set<String> packageElements = new HashSet<String>();
        IJavaProject[] projects;
        try {
            projects = javaModel.getJavaProjects();
        } catch (JavaModelException e) {
            Activator.log(IStatus.ERROR, Messages.getJavaModelFailedMsg, e);
            return new Object[0];
        }

        for (IJavaProject project : projects) {
            if (EXTERNAL_PLUGIN_LIBRARIES.equals(project.getResource()
                    .getName())) {
                continue;
            }

            IPackageFragmentRoot[] packageFragmentRoots;
            try {
                packageFragmentRoots = project.getPackageFragmentRoots();
            } catch (JavaModelException e) {
                continue;
            }

            for (IPackageFragmentRoot packageFragment : packageFragmentRoots) {
                try {
                    addPackage(packageElements, packageFragment);
                } catch (JavaModelException e) {
                    // do nothing
                }
            }
        }

        for (Object packageName : filteringPackages) {
            packageElements.remove(packageName);
        }

        return packageElements.toArray(new String[0]);
    }

    /**
     * Adds the package.
     * 
     * @param packageElements
     *            The package elements into which package is added
     * @param element
     *            The element which contains package element to be added
     * @throws JavaModelException
     */
    private void addPackage(Set<String> packageElements, IJavaElement element)
            throws JavaModelException {

        // java source folder
        if (element instanceof IPackageFragmentRoot) {
            int kind = ((IPackageFragmentRoot) element).getKind();
            if (kind == IPackageFragmentRoot.K_SOURCE) {
                IJavaElement[] children = ((IPackageFragmentRoot) element)
                        .getChildren();
                for (IJavaElement child : children) {
                    addPackage(packageElements, child);
                }
            }
        }

        // java package
        if (element instanceof IPackageFragment) {
            IJavaElement[] children = ((IPackageFragment) element)
                    .getChildren();
            for (IJavaElement child : children) {
                if (IJavaElement.COMPILATION_UNIT == child.getElementType()) {
                    packageElements.add(element.getElementName());
                    break;
                }
            }
        }
    }
}
