/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.util;

import android.content.Intent;

import net.bither.BitherApplication;
import net.bither.bitherj.utils.Utils;
import net.bither.service.BlockchainService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ServiceUtil {

    private ServiceUtil() {

    }

    private static List<Long> peerTimeList = new ArrayList<Long>();
    private static final long ALLOWED_TIME_DRIFT = 2 * 24 * 60 * 60;

    public static void addPeerTime(long peerTime) {
        peerTimeList.add(peerTime);
    }

    public static boolean localTimeIsWrong() {
        if (peerTimeList.size() < 3) {
            return false;
        }
        // LogUtil.d("localTimeIsWrong","timeList:"+peerTimeList.size());
        Collections.sort(peerTimeList);
        List<Long> timeList = new ArrayList<Long>();
        timeList.addAll(peerTimeList);
        while (timeList.size() > 2) {
            timeList = trimTimeList(timeList);
            //   LogUtil.d("localTimeIsWrong","timeList:"+timeList.size());
        }
        long average = getAverage(timeList);
        long currentTime = Utils.currentTimeMillis() / 1000;
        //LogUtil.d("localTimeIsWrong","average:"+average+",currentTime:"+currentTime);
        return average > currentTime + ALLOWED_TIME_DRIFT;
    }

    public static List<Long> trimTimeList(List<Long> timeList) {
        if (timeList.size() > 2) {
            long average = getAverage(timeList);
            long firstDiff = Math.abs(timeList.get(0) - average);
            long lastDiff = Math.abs(timeList.get(timeList.size() - 1) - average);
            if (firstDiff > lastDiff) {
                timeList.remove(0);
            } else {
                timeList.remove(timeList.size() - 1);
            }

        }
        return timeList;
    }

    public static long getAverage(List<Long> list) {
        long result = 0;
        for (long item : list) {
            result = result + item;
        }
        long average = result / list.size();
        return average;
    }

    private static boolean getIsNoSoundWithoutWeekend(int hour) {
        return hour == 23 || hour < 8;
    }

    public static boolean isNoPrompt(long timeMillis) {
        int week = DateTimeUtil.getDayOfWeek(timeMillis) - 1;

        int hour = DateTimeUtil.getHour(timeMillis);
        boolean isNoSound = false;
        switch (week) {
            case 0:
                isNoSound = (hour >= 1 && hour < 10) || (hour == 23);
                break;
            case 5:
                isNoSound = hour < 8;
                break;
            case 6:
                isNoSound = hour >= 1 && hour < 10;
                break;
            default:
                isNoSound = getIsNoSoundWithoutWeekend(hour);
                break;
        }
        if (isNoSound) {
            LogUtil.d(
                    "NoSound",
                    String.format("week:%d,hour:%d", week, hour)
                            + "  time:"
                            + DateTimeUtil.getDateTimeString(new Date(
                            timeMillis))
            );
            LogUtil.d("NoSound", "result:" + Boolean.toString(isNoSound));

        } else {
            LogUtil.d(
                    "Sound",
                    String.format("week:%d,hour:%d", week, hour)
                            + "  time:"
                            + DateTimeUtil.getDateTimeString(new Date(
                            timeMillis))
            );
            LogUtil.d("Sound", "result:" + Boolean.toString(isNoSound));
        }
        return isNoSound;
    }

}
