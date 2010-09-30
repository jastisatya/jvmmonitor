/*******************************************************************************
 * Copyright (c) 2010 JVM Monitor project. All rights reserved. 
 * 
 * This code is distributed under the terms of the Eclipse Public License v1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jvmmonitor.internal.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.jvmmonitor.core.JvmCoreException;
import org.jvmmonitor.core.JvmModel;
import org.jvmmonitor.core.JvmModelEvent;
import org.jvmmonitor.core.JvmModelEvent.State;
import org.jvmmonitor.core.mbean.IMBeanNotification;

/**
 * The MBean notification.
 */
public class MBeanNotification implements IMBeanNotification {

    /** The active JVM. */
    private ActiveJvm activeJvm;

    /** The notification listeners. */
    private Map<ObjectName, NotificationListener> listeners;

    /** The notifications. */
    protected Map<ObjectName, List<Notification>> notifications;

    /**
     * The constructor.
     * 
     * @param activeJvm
     *            The active JVM
     */
    public MBeanNotification(ActiveJvm activeJvm) {
        this.activeJvm = activeJvm;
        listeners = new HashMap<ObjectName, NotificationListener>();
        notifications = new LinkedHashMap<ObjectName, List<Notification>>();
    }

    /*
     * @see IMBeanNotification#subscribe(ObjectName)
     */
    @Override
    public void subscribe(final ObjectName objectName) throws JvmCoreException {
        NotificationListener listener = new NotificationListener() {
            @Override
            public void handleNotification(Notification notification,
                    Object handback) {
                List<Notification> list = notifications.get(objectName);
                if (list == null) {
                    list = new ArrayList<Notification>();
                }
                list.add(notification);
                notifications.put(objectName, list);
            }
        };
        activeJvm.getMBeanServer()
                .addNotificationListener(objectName, listener);
        listeners.put(objectName, listener);
        JvmModel.getInstance().fireJvmModelChangeEvent(
                new JvmModelEvent(State.JvmModified, activeJvm));
    }

    /*
     * @see IMBeanNotification#unsubscribe(ObjectName)
     */
    @Override
    public void unsubscribe(ObjectName objectName) throws JvmCoreException {
        NotificationListener listener = listeners.get(objectName);
        activeJvm.getMBeanServer().removeNotificationListener(objectName,
                listener);
        listeners.remove(objectName);
        notifications.remove(objectName);
        JvmModel.getInstance().fireJvmModelChangeEvent(
                new JvmModelEvent(State.JvmModified, activeJvm));
    }

    /*
     * @see IMBeanNotification#getNotifications(ObjectName)
     */
    @Override
    public Notification[] getNotifications(ObjectName objectName) {
        List<Notification> list = notifications.get(objectName);
        if (list == null) {
            return new Notification[0];
        }
        return list.toArray(new Notification[list.size()]);
    }

    /*
     * @see IMBeanNotification#clear(ObjectName)
     */
    @Override
    public void clear(ObjectName objectName) {
        List<Notification> notification = notifications.get(objectName);
        if (notification == null) {
            return;
        }
        notification.clear();
        JvmModel.getInstance().fireJvmModelChangeEvent(
                new JvmModelEvent(State.JvmModified, activeJvm));
    }

    /*
     * @see IMBeanNotification#isSubscribed(javax.management.ObjectName)
     */
    @Override
    public boolean isSubscribed(ObjectName objectName) {
        return listeners.containsKey(objectName);
    }

    /*
     * @see IMBeanNotification#isSupported(javax.management.ObjectName)
     */
    @Override
    public boolean isSupported(ObjectName objectName) {
        MBeanInfo info = null;
        try {
            info = activeJvm.getMBeanServer().getMBeanInfo(objectName);
        } catch (JvmCoreException e) {
            return false;
        }

        if (info == null) {
            return false;
        }

        MBeanNotificationInfo[] notificationInfoArray = info.getNotifications();
        if (notificationInfoArray == null || notificationInfoArray.length == 0) {
            return false;
        }

        return true;
    }

    /**
     * Disposes the resources.
     */
    public void dispose() {
        for (Entry<ObjectName, NotificationListener> entry : listeners
                .entrySet()) {
            try {
                unsubscribe(entry.getKey());
            } catch (JvmCoreException e) {
                // do nothing
            }
        }
        listeners.clear();
        notifications.clear();
    }
}
