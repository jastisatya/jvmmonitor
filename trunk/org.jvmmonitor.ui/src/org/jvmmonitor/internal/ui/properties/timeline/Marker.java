package org.jvmmonitor.internal.ui.properties.timeline;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.jvmmonitor.core.mbean.IMonitoredMXBeanGroup.AxisUnit;
import org.swtchart.ICustomPaintListener;
import org.swtchart.IPlotArea;
import org.swtchart.ISeries;

/**
 * The marker.
 */
public class Marker {

    /** The marker color. */
    private static final Color MARKER_COLOR = Display.getDefault()
            .getSystemColor(SWT.COLOR_DARK_GRAY);

    /** The chart. */
    private TimelineChart chart;

    /** The tooltip text. */
    private String toolTipText;

    /** The nearest x position. */
    private Integer nearestX;

    /** The value corresponding to nearest time. */
    private double nearestValue;

    /**
     * The constructor.
     * 
     * @param chart
     *            The chart
     */
    public Marker(TimelineChart chart) {
        this.chart = chart;

        IPlotArea protArea = (IPlotArea) chart.getPlotArea();
        protArea.addCustomPaintListener(new ICustomPaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                paint(e);
            }

            @Override
            public boolean drawBehindSeries() {
                return false;
            }
        });
    }

    /**
     * Check if marker is disposed.
     * 
     * @return <tt>true</tt> if marker is disposed.
     */
    protected boolean isDisposed() {
        return nearestX == null;
    }

    /**
     * Disposes the resource.
     */
    protected void dispose() {
        nearestX = null;
        chart.getPlotArea().setToolTipText(null);
        chart.redraw();
    }

    /**
     * Sets the position.
     * 
     * @param x
     */
    protected void setPosition(int x) {
        this.nearestX = getNearestPosition(x);
        chart.getPlotArea().setToolTipText(toolTipText);
        chart.redraw();
    }

    /**
     * Paints the marker.
     * 
     * @param e
     *            The paint event
     */
    void paint(PaintEvent e) {
        if (nearestX == null) {
            return;
        }

        int y = chart.getAxisSet().getYAxes()[0]
                .getPixelCoordinate(nearestValue);

        e.gc.setForeground(MARKER_COLOR);
        e.gc.setBackground(MARKER_COLOR);
        e.gc.setAntialias(SWT.ON);
        e.gc.drawLine(nearestX, e.y, nearestX, y - 12);
        e.gc.drawLine(nearestX, y + 5, nearestX, e.y + e.height);

        int[] points = new int[] { nearestX, y, nearestX + 5, y - 9,
                nearestX - 5, y - 9 };
        e.gc.fillPolygon(points);
    }

    /**
     * Gets the nearest position.
     * 
     * @param desiredX
     *            The x coordinate
     * @return The nearest x coordinate
     */
    private int getNearestPosition(int desiredX) {
        long desiredTime = (long) chart.getAxisSet().getAxes()[0]
                .getDataCoordinate(desiredX);

        long candidateTime = -1;
        long nearestTime = -1;
        StringBuffer buffer = new StringBuffer();
        for (ISeries series : chart.getSeriesSet().getSeries()) {
            Date[] dates = series.getXDateSeries();
            for (int i = 0; i < dates.length; i++) {
                if (dates[i].getTime() < desiredTime && i != dates.length - 1) {
                    continue;
                }

                int nearestIndex;
                if (i > 0
                        && dates[i].getTime() - desiredTime > desiredTime
                                - dates[i - 1].getTime()) {
                    nearestIndex = i - 1;
                } else {
                    nearestIndex = i;
                }
                candidateTime = dates[nearestIndex].getTime();

                if (nearestTime == -1
                        || Math.abs(nearestTime - desiredTime) < Math
                                .abs(nearestTime - candidateTime)) {
                    nearestValue = series.getYSeries()[nearestIndex];
                    if (nearestTime != desiredTime) {
                        buffer = new StringBuffer();
                    }
                    if (buffer.length() > 0) {
                        buffer.append('\n');
                    }
                    buffer.append(Messages.timeLabel).append(' ')
                            .append(new SimpleDateFormat("HH:mm:ss") //$NON-NLS-1$
                                    .format(dates[nearestIndex])).append('\n');
                    buffer.append(series.getId()).append(": ") //$NON-NLS-1$
                            .append(getFormattedValue(series, nearestValue));
                    nearestTime = candidateTime;
                }
                break;
            }
        }

        toolTipText = buffer.toString();
        return chart.getAxisSet().getAxes()[0].getPixelCoordinate(nearestTime);
    }

    /**
     * Shows the tool tip.
     * 
     * @param series
     *            The series
     * @param value
     *            The value
     * @return The formatted value
     */
    private String getFormattedValue(ISeries series, Object value) {
        AxisUnit axisUnit = chart.getAttributeGroup().getAxisUnit();
        if (axisUnit == AxisUnit.Percent) {
            return new DecimalFormat("###%").format(value); //$NON-NLS-1$
        } else if (axisUnit == AxisUnit.MBytes) {
            return new DecimalFormat("#####.#M").format(value); //$NON-NLS-1$
        } else if (axisUnit == AxisUnit.Count) {
            return NumberFormat.getIntegerInstance().format(value);
        }
        return value.toString();
    }
}
