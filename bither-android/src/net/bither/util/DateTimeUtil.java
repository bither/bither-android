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

import android.content.Context;
import android.text.format.DateUtils;

import net.bither.bitherj.BitherjSettings.KlineTimeType;
import net.bither.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateTimeUtil {

	private DateTimeUtil() {

	};

	public static final String SHORT_DATE_TIME_FORMAT = "MM-dd HH:mm";
	public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
	private static final String DATE_TIME_FORMAT_DCIM_FilENAME = "yyyyMMdd_HHmmss";
	private static final SimpleDateFormat AccurateTimeFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm");
	private static final long RelativeDateMinResolution = DateUtils.MINUTE_IN_MILLIS * 5;
	private static final String DEFAULT_TIMEZONE = "GMT+0";

	private static final String DATE_TIME_FORMAT_OF_MINUTE = "HH:mm";
	private static final String DATE_TIME_FORMAT_OF_DAY = "MM/dd";


    public static final String getNameForDcim(long time) {
        Date date = new Date(time);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                DATE_TIME_FORMAT_DCIM_FilENAME);
        return dateFormat.format(date);
    }


    public static final String getNameForFile(long time) {
		Date date = new Date(time);
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				DATE_TIME_FORMAT_DCIM_FilENAME);
		return dateFormat.format(date);
	}

	public static final String getXTitle(KlineTimeType klineTimeType, Date date) {
		SimpleDateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT_OF_MINUTE);
		switch (klineTimeType) {
		case ONE_MINUTE:
		case FIVE_MINUTES:
			break;
		case ONE_HOUR:
		case ONE_DAY:
			df = new SimpleDateFormat(DATE_TIME_FORMAT_OF_DAY);
			break;

		default:
			break;
		}
		return df.format(date);

	}

	public static final String getDateTimeString(Date date) {
		SimpleDateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT);
		String result = df.format(date);
		return result;
	}

	public static final String getShortDateTimeString(Date date) {
		SimpleDateFormat df = new SimpleDateFormat(SHORT_DATE_TIME_FORMAT);
		return df.format(date);

	}

	public static final Date getDateTimeForTimeZone(String str)
			throws ParseException {
		SimpleDateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT);
		Long time = new Date(df.parse(str).getTime()).getTime();
		return getDateTimeForTimeZone(time);
	}

	public static final Date getDateTimeForTimeZone(Long time) {
		Long sourceRelativelyGMT = time
				- TimeZone.getTimeZone(DEFAULT_TIMEZONE).getRawOffset();
		Long targetTime = sourceRelativelyGMT
				+ TimeZone.getDefault().getRawOffset();

		Date date = new Date(targetTime);
		return date;

	}

	public static final String getAccurateDate(Date date) {
		String result = AccurateTimeFormat.format(date);
		return result;
	}

	public static int getDayOfWeek(long timeMillis) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(timeMillis);
		return calendar.get(Calendar.DAY_OF_WEEK);
	}

	public static int getHour(long timeMillis) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(timeMillis);
		return calendar.get(Calendar.HOUR_OF_DAY);
	}

	/**
	 * Displays a user-friendly date difference string
	 * 
	 * @param date
	 *            - Date to format as date difference from now
	 * @return Friendly-formatted date diff string
	 */

	public static CharSequence getRelativeDate(Context context, Date date) {
		CharSequence result;
		if (System.currentTimeMillis() - date.getTime() <= RelativeDateMinResolution) {
			result = context.getResources().getString(R.string.just_now);
		} else {
			try {
				result = DateUtils.getRelativeTimeSpanString(date.getTime(),
						System.currentTimeMillis(), RelativeDateMinResolution,
						DateUtils.FORMAT_12HOUR
								| DateUtils.FORMAT_ABBREV_RELATIVE);
				// on some phones this will cause NotFoundException: Plural
				// resource
			} catch (Exception e) {
				result = DateTimeUtil.getAccurateDate(date);
			}
		}
		return result;
	}
}
