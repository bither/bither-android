/*
 *
 *  * Copyright 2014 http://Bither.net
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package net.bither.enums;

import net.bither.BitherApplication;
import net.bither.R;

/**
 * Created by songchenwen on 15/8/17.
 */
public enum TotalBalanceHide {

    ShowAll(0), ShowChart(1), HideAll(2);

    private int value;

    TotalBalanceHide(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public int nameRes() {
        switch (this) {
            case ShowAll:
                return R.string.total_balance_hide_show_all;
            case ShowChart:
                return R.string.total_balance_hide_show_chart;
            case HideAll:
                return R.string.total_balance_hide_hide_all;
        }
        return R.string.total_balance_hide_show_all;
    }

    public String displayName() {
        return BitherApplication.mContext.getString(nameRes());
    }

    public boolean shouldShowBalance() {
        return value < ShowChart.value;
    }

    public boolean shouldShowChart() {
        return value < HideAll.value;
    }

    public static TotalBalanceHide totalBalanceHide(int value) {
        for (TotalBalanceHide h : TotalBalanceHide.values()) {
            if (h.value == value) {
                return h;
            }
        }
        return ShowAll;
    }
}
