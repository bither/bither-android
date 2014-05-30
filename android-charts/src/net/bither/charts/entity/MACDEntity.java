/*
 * MACDEntity.java
 * Android-Charts
 *
 * Created by limc on 2014.
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

public class MACDEntity implements IStickEntity {

    private static final long serialVersionUID = 1L;

    private double dea;
    private double diff;
    private double macd;
    private long date;
    private String title;

    public MACDEntity() {
        super();
    }

    public MACDEntity(double dea, double diff, double macd, int date) {
        super();
        this.dea = dea;
        this.diff = diff;
        this.macd = macd;
        this.date = date;
    }

    public double getDea() {
        return dea;
    }

    public void setDea(double dea) {
        this.dea = dea;
    }

    public double getDiff() {
        return diff;
    }

    public void setDiff(double diff) {
        this.diff = diff;
    }

    public double getMacd() {
        return macd;
    }

    public void setMacd(double macd) {
        this.macd = macd;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public double getHigh() {
        return Math.max(Math.max(getDea(), getDiff()), getMacd());
    }

    public double getLow() {
        return Math.min(Math.min(getDea(), getDiff()), getMacd());
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
