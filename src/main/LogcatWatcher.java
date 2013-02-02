/*
 * Copyright 2013 Thomas Pilot
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

package main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import os.Commands;

public class LogcatWatcher implements Runnable {
	
	private static final Logger LOG = LogManager.getLogger(LogcatWatcher.class);
	
	private static final String DEFAULT_PID = "-1";
	
	private static final int MAX_SECS_TO_WATCH_LOG = 10;
	
	private boolean foundError = false;
	
	private final String activityName;

	public LogcatWatcher(String activityName) {
		this.activityName = activityName;
	}

	@Override
	public void run() {
		LOG.info("started watching activity {}", activityName);
		String activityPid = getPidInTime();
		if (activityPid.isEmpty()) {
			LOG.error("could not get activity process ID in time!");
			return;
		}
		getErrorInTime(activityPid);
		if (foundError) {
			LOG.info("found error in log, stopping log watching");
		} else {
			LOG.info("no errors found, stopping log watching");
		}
	}

	private void getErrorInTime(String activityPid) {
		LOG.info("doing actual log watching, looking for error messages with process ID {}", activityPid);
		boolean enoughTimeLeft = true;
		int maxTimeToWatch = getEmulatorTime() + MAX_SECS_TO_WATCH_LOG;
		do {
			searchForErrorLogLines(activityPid);
			enoughTimeLeft = getEmulatorTime() < maxTimeToWatch;
		} while (enoughTimeLeft && !foundError);
	}

	private String getPidInTime() {
		LOG.info("getting process ID of the started activity to filter log messages");
		String activityPid = "";
		boolean enoughTimeLeft = true;
		int maxTimeToWatch = getEmulatorTime() + MAX_SECS_TO_WATCH_LOG;
		do {
			activityPid = getActivityPidInLog();
			enoughTimeLeft = getEmulatorTime() < maxTimeToWatch;
		} while (enoughTimeLeft && activityPid.isEmpty());
		return activityPid;
	}
	
	private int getEmulatorTime() {
		String getTimeResult = Commands.getEmulatorTime();
		String time = getTimeResult.trim();
		return Integer.parseInt(time);
	}

	private void searchForErrorLogLines(String activityPid) {
		String briefLog = Commands.getBriefLog();
		if (briefLog.isEmpty()) {
			return;
		}
		StringBuilder localLogLines = new StringBuilder();
		String[] logLines = briefLog.split("\n");
		for (String logLine : logLines) {
			String logPid = extractPid(logLine);
			if (activityPid.equals(logPid)) {
				localLogLines.append(logLine);
				localLogLines.append('\n');
				if (isErrorMessage(logLine)) {
					foundError = true;
				}
			}
		}
		if (foundError) {
			LOG.error("found error log lines:\n{}", localLogLines.toString());
		}
	}
	
	private boolean isErrorMessage(String logMessage) {
		// threat messages other than warnings, errors and fatals as "not an error"
		if (!logMessage.startsWith("W/") && !logMessage.startsWith("E/") && !logMessage.startsWith("F/")) {
			return false;
		}
		// filter "harmless" warning / error messages
		if (logMessage.endsWith("Unable to open stack trace file '/data/anr/traces.txt': Permission denied")) {
			// not being able to write stack traces is OK, particularly if the "Permission denied" comes and goes for some reason
			return false;
		}
		if (logMessage.contains("Converting to string: TypedValue")) {
			// harmless warning of unknown source, something like "W/Resources(17336): Converting to string: TypedValue{t=0x10/d=0x6 a=-1}"
			return false;
		}
		if (logMessage.contains("No known package when getting value for resource number")) {
			return false;
		}
		// if adding new definitions for harmless errors: do not forget the reason for harmlessness...
		return true;
	}

	private String getActivityPidInLog() {
		String processActivityLog = Commands.getProcessActivityLog();
		String[] logEntries = processActivityLog.split("\n");
		for (String logEntry : logEntries) {
			int activityIndex = logEntry.indexOf("for activity " + activityName);
			int pidIdx = logEntry.indexOf("pid=");
			int uidIdx = logEntry.indexOf(" uid=");
			if (activityIndex == -1 || pidIdx == -1 || uidIdx == -1) {
				continue;
			}
			return logEntry.substring(pidIdx + 4, uidIdx);
		}
		return ""; // log was not ready or message format changed, we don't know...
	}
	
	private String extractPid(String briefLogEntry) {
		int pidBegin = briefLogEntry.indexOf('(');
		int pidEnd = briefLogEntry.indexOf(')', pidBegin);
		if (pidBegin == -1 || pidEnd == -1) {
			return DEFAULT_PID;
		}
		return briefLogEntry.substring(pidBegin + 1, pidEnd).trim();
	}

	public boolean foundError() {
		return foundError;
	}
}