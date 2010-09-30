/*******************************************************************************
 * Copyright (c) 2010 JVM Monitor project. All rights reserved. 
 * 
 * This code is distributed under the terms of the Eclipse Public License v1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jvmmonitor.internal.ui.properties.timeline;

import org.eclipse.osgi.util.NLS;

/**
 * The messages.
 */
public final class Messages extends NLS {

    /** The bundle name. */
    private static final String BUNDLE_NAME = "org.jvmmonitor.internal.ui.properties.timeline.messages";//$NON-NLS-1$

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    /**
     * The constructor.
     */
    private Messages() {
        // do not instantiate
    }

    // columns

    /** */
    public static String objectNameColumnLabel;

    /** */
    public static String attributeColumnLabel;

    /** */
    public static String objectNameColumnToolTip;

    /** */
    public static String attributeColumnToolTip;

    // confirm delete dialog

    /** */
    public static String confirmDeleteChartTitle;

    /** */
    public static String confirmDeleteChartMsg;

    // new chart dialog

    /** */
    public static String newChartDialogTitle;

    // configure chart dialog

    /** */
    public static String configureChartDialogTitle;

    /** */
    public static String attributeSelectionDialogTitle;

    /** */
    public static String attributesToAddOnChartLabel;

    /** */
    public static String colorLabel;

    /** */
    public static String chartTitleDuplicatedMsg;

    /** */
    public static String chartTitleLabel;

    /** */
    public static String yAxisUnitLabel;

    /** */
    public static String monitoredAttributesLabel;

    /** */
    public static String addButtonLabel;

    /** */
    public static String removeButtonLabel;

    // actions

    /** */
    public static String clearTimelineDataLabel;

    /** */
    public static String restoreDefaultsLabel;

    /** */
    public static String configureChartLabel;

    /** */
    public static String newChartLabel;

    /** */
    public static String deleteChartLabel;

    // tooltip

    /** */
    public static String timeLabel;

    // job name

    /** */
    public static String refreshChartJobLabel;

    // error log messages

    /** */
    public static String restoreDefaultChartsFailedMsg;

    /** */
    public static String configureMonitoredAttributesFailedMsg;

    /** */
    public static String getMBeanNamesFailedMsg;

    /** */
    public static String getMBeanInfoFailedMsg;

    /** */
    public static String getMBeanAttributeFailedMsg;

    /** */
    public static String addAttributeFailedMsg;
}