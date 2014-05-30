/*
 * LineEntity.java
 * Android-Charts
 *
 * Created by limc on 2011/05/29.
 *
 * Copyright 2011 limc.cn All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.charts.entity;

import java.util.ArrayList;
import java.util.List;

public class LineEntity<T> {

    private List<T> lineData;
    private String title;
    private int lineColor;
    private boolean display = true;

    public LineEntity() {
        super();
    }

    public LineEntity(List<T> lineData, String title, int lineColor) {
        super();
        this.lineData = lineData;
        this.title = title;
        this.lineColor = lineColor;
    }

    public void put(T value) {
        if (null == lineData) {
            lineData = new ArrayList<T>();
        }
        lineData.add(value);
    }

    public List<T> getLineData() {
        return lineData;
    }

    public void setLineData(List<T> lineData) {
        this.lineData = lineData;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getLineColor() {
        return lineColor;
    }

    public void setLineColor(int lineColor) {
        this.lineColor = lineColor;
    }

    public boolean isDisplay() {
        return display;
    }

    public void setDisplay(boolean display) {
        this.display = display;
    }
}
