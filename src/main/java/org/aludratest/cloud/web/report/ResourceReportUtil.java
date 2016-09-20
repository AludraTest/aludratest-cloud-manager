/*
 * Copyright (C) 2015 Hamburg Sud and the contributors.
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
package org.aludratest.cloud.web.report;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sql.RowSet;

import org.aludratest.cloud.impl.app.CloudManagerApplicationHolder;
import org.aludratest.cloud.resource.ResourceType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class ResourceReportUtil {

	private static final String DB_DATE_FORMAT = "YYYY-MM-dd HH:mm:ss.SSS";

	public static final long DEFAULT_TIME_EQUALITY_TOLERANCE_MS = 2000;

	public static JavaScriptArray getResourceUsageData(ResourceType resourceType, DateTime startTime, DateTime endTime,
			long tolerance)
			throws SQLException {
		startTime = startTime.toDateTime(DateTimeZone.UTC);
		endTime = endTime.toDateTime(DateTimeZone.UTC);
		String tp = resourceType.getName();

		// get active count at start time
		String startTimeStr = startTime.toString(DB_DATE_FORMAT);
		String sql = "SELECT count(*) FROM app.acm_request WHERE start_work_time_utc <= '" + startTimeStr
				+ "' AND end_work_time_utc > '" + startTimeStr + "' AND resource_type = '" + tp + "'";

		RowSet rs = CloudManagerApplicationHolder.getInstance().getDatabase().populateQuery(sql);
		int initialCount = 0;
		if (rs.next()) {
			initialCount = rs.getInt(1);
		}

		// iterate over all entries starting or ending in this interval
		sql = "SELECT start_work_time_utc, end_work_time_utc FROM acm_request WHERE resource_type = '" + tp
				+ "' AND (start_work_time_utc >= '" + startTimeStr + "' AND end_work_time_utc < '"
				+ endTime.toString(DB_DATE_FORMAT) + "') OR (start_work_time_utc <= '" + startTimeStr
				+ "' AND end_work_time_utc > '" + startTimeStr + "') ORDER BY start_work_time_utc";

		rs = CloudManagerApplicationHolder.getInstance().getDatabase().populateQuery(sql);

		JavaScriptArray data = new JavaScriptArray();

		int activeResources = initialCount;

		// first entry
		data.add(createTimeEntry(startTime, activeResources));

		// build a full list of "delta" events
		List<ActiveResourcesDeltaEvent> events = new ArrayList<ActiveResourcesDeltaEvent>(2000);

		while (rs.next()) {
			DateTime dtStart = new DateTime(rs.getTimestamp(1).getTime()).withZoneRetainFields(DateTimeZone.UTC);
			DateTime dtEnd = new DateTime(rs.getTimestamp(2).getTime()).withZoneRetainFields(DateTimeZone.UTC);

			if (!dtStart.isBefore(startTime)) {
				events.add(new ActiveResourcesDeltaEvent(dtStart, 1));
			}
			events.add(new ActiveResourcesDeltaEvent(dtEnd, -1));
		}

		// compress event list to data series
		Collections.sort(events);

		DateTime currentTime = null;
		int currentDelta = 0;

		for (ActiveResourcesDeltaEvent event : events) {
			if (!sameTimePoint(currentTime, event.dt, tolerance)) {
				if (currentTime != null && currentDelta != 0) {
					// also add a helper point
					data.add(createTimeEntry(new DateTime(currentTime.getMillis() - 1), activeResources));
					activeResources += currentDelta;
					data.add(createTimeEntry(currentTime, activeResources));
				}
				currentTime = event.dt;
				currentDelta = event.resourcesDelta;
			}
			else {
				currentDelta += event.resourcesDelta;
			}
		}

		if (currentTime != null && currentDelta != 0) {
			if (activeResources == 0) {
				data.add(createTimeEntry(new DateTime(currentTime.getMillis() - 1), 0));
			}
			activeResources += currentDelta;
			data.add(createTimeEntry(currentTime, activeResources));
		}

		if (activeResources == 0) {
			// add end point
			data.add(createTimeEntry(endTime, 0));
		}

		return data;
	}

	private static JavaScriptObject createTimeEntry(DateTime time, int activeResources) {
		JavaScriptObject result = new JavaScriptObject();
		time = time.toDateTime(DateTimeZone.UTC);

		// convert time to JavaScript UTC time
		StringBuilder sbDateTime = new StringBuilder();
		sbDateTime.append("Date.UTC(");
		sbDateTime.append(time.getYear()).append(", ");
		sbDateTime.append(time.getMonthOfYear() - 1).append(", ");
		sbDateTime.append(time.getDayOfMonth()).append(", ");
		sbDateTime.append(time.getHourOfDay()).append(", ");
		sbDateTime.append(time.getMinuteOfHour()).append(", ");
		sbDateTime.append(time.getSecondOfMinute()).append(", ");
		sbDateTime.append(time.getMillisOfSecond()).append(")");

		result.set("x", new JavaScriptCodeFragment(sbDateTime.toString()));
		result.set("y", new JavaScriptCodeFragment("" + activeResources));

		return result;
	}

	private static boolean sameTimePoint(DateTime dt1, DateTime dt2, long tolerance) {
		if (dt1 == null || dt2 == null) {
			return false;
		}

		return dt1.getMillis() + tolerance > dt2.getMillis();
	}

	private static class ActiveResourcesDeltaEvent implements Comparable<ActiveResourcesDeltaEvent> {

		private DateTime dt;

		private int resourcesDelta;

		public ActiveResourcesDeltaEvent(DateTime dt, int resourcesDelta) {
			this.dt = dt;
			this.resourcesDelta = resourcesDelta;
		}

		@Override
		public int compareTo(ActiveResourcesDeltaEvent o) {
			return dt.compareTo(o.dt);
		}
	}

}
