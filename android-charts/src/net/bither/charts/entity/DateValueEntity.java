/*
 * DateValueEntity.java
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

import java.io.Serializable;

public class DateValueEntity implements IHasDate, IHasXTitle, Serializable {

    private static final long serialVersionUID = 1L;

    private String title;
    private long date;
    private float value;

    public DateValueEntity(float value, long date) {
        super();
        this.value = value;
        this.date = date;
        this.title = "";
    }

    public DateValueEntity(float value, String title, long date) {
        super();
        this.value = value;
        this.title = title;
        this.date = date;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
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
