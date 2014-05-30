/*
 * OHLCEntity.java
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

public class OHLCEntity implements IStickEntity {

    private static final long serialVersionUID = 1L;

    private double open;
    private double high;
    private double low;
    private double close;
    private long date;
    private String title;

    public OHLCEntity(double open, double high, double low, double close,
                      long date) {
        super();
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.date = date;
    }

    public OHLCEntity(double open, double high, double low, double close,
                      String title, long date) {
        super();
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.title = title;
        this.date = date;
    }

    public OHLCEntity() {
        super();
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    @Override
    public String getTitle() {

        return this.title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;

    }
}
