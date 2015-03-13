/*
 * Copyright 2012-2015 Tobi29
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tobi29.scapes.server.controlpanel.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

public class ProfilerGraph extends Composite {
    public final Canvas canvas;
    private final Label label;
    private final BorderNameFilter borderNameFilter;
    private int i;
    @SuppressWarnings("FieldMayBeFinal")
    private double[] data;

    public ProfilerGraph(Composite parent, double max, double scale,
            BorderNameFilter borderNameFilter) {
        super(parent, SWT.NONE);
        this.borderNameFilter = borderNameFilter;
        setLayout(new GridLayout(1, false));

        label = new Label(this, SWT.NONE);
        label.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        canvas = new Canvas(this, SWT.BORDER);
        canvas.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        canvas.addListener(SWT.Resize, event -> {
            Point size = canvas.getSize();
            if (data.length != size.x) {
                i = 0;
                data = new double[size.x];
            }
        });
        Display display = getDisplay();
        Color colorGreen = display.getSystemColor(SWT.COLOR_GREEN);
        Color colorYellow = display.getSystemColor(SWT.COLOR_YELLOW);
        Color colorRed = display.getSystemColor(SWT.COLOR_RED);
        canvas.addPaintListener(event -> {
            Point size = canvas.getSize();
            for (int i = 1; i < data.length; i++) {
                int x = i - this.i;
                if (x < 1) {
                    x += data.length;
                }
                double percentageLow = data[i - 1] / max;
                double percentageHigh = data[i] / max;
                if (percentageLow < 1.0 && percentageHigh < 1.0) {
                    event.gc.setForeground(colorGreen);
                } else if (percentageLow < 1.1 && percentageHigh < 1.1) {
                    event.gc.setForeground(colorYellow);
                } else {
                    event.gc.setForeground(colorRed);
                }
                int heightLow = (int) (percentageLow * scale * size.y);
                int heightHigh = (int) (percentageHigh * scale * size.y);
                event.gc.drawLine(x, size.y - heightLow, x + 1,
                        size.y - heightHigh);
            }
        });
        Point size = canvas.getSize();
        data = new double[size.x];
        label.setText("Loading...");
    }

    public void addStamp(double value) {
        if (i < data.length) {
            data[i++] = value;
        }
        if (i >= data.length) {
            i = 0;
        }
        canvas.redraw();
        label.setText(borderNameFilter.process(value));
    }

    @Override
    protected void checkSubclass() {
    }

    @FunctionalInterface
    public interface BorderNameFilter {
        String process(double value);
    }
}
