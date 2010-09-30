/*******************************************************************************
 * Copyright (c) 2010 JVM Monitor project. All rights reserved. 
 * 
 * This code is distributed under the terms of the Eclipse Public License v1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jvmmonitor.internal.ui.properties.timeline;

import org.eclipse.jface.action.Action;
import org.jvmmonitor.core.IActiveJvm;
import org.jvmmonitor.core.JvmCoreException;
import org.jvmmonitor.internal.ui.properties.AbstractJvmPropertySection;
import org.jvmmonitor.ui.Activator;
import org.jvmmonitor.ui.ISharedImages;

/**
 * The action to restore the default charts.
 */
public class RestoreDefaultsAction extends Action {

    /** The property section. */
    AbstractJvmPropertySection section;

    /**
     * The constructor.
     * 
     * @param section
     *            The property section
     */
    public RestoreDefaultsAction(AbstractJvmPropertySection section) {
        setText(Messages.restoreDefaultsLabel);
        setImageDescriptor(Activator
                .getImageDescriptor(ISharedImages.RESTORE_DEFAULT_CHARTS_IMG_PATH));
        setDisabledImageDescriptor(Activator
                .getImageDescriptor(ISharedImages.DISABLED_RESTORE_DEFAULT_CHARTS_IMG_PATH));
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

        try {
            jvm.getMBeanServer().restoreDefaultMonitoredAttributeGroup();
        } catch (JvmCoreException e) {
            Activator.log(Messages.restoreDefaultChartsFailedMsg, e);
        }
    }
}
