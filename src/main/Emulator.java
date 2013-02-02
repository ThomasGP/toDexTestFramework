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
import os.Results;

public class Emulator {
	
	private static final Logger LOG = LogManager.getLogger(Emulator.class);

	public boolean run(Apk apk) {
		LOG.info("running APK on emulator");
		assertRunning();
		Commands.clearLogcat();
		Results installResults = install(apk);
		if (installResults.getOutput().contains("Failure")) {
			handleInstallFailure(installResults);
			return false;
		} else if (apk.getMainActivity().isEmpty()) {
			LOG.info("no main activity found in APK, skipping start after installation");
			return true;
		} else {
			return startAndWatchForErrors(apk);
		}
	}

	private void assertRunning() {
		String devicesResult = Commands.getDevices();
		if (devicesResult.isEmpty() || devicesResult.equals("List of devices attached \n\n")) {
			throw new Error("no running emulator found");
		}
	}
	
	private Results install(Apk apk) {
		LOG.info("uninstalling old APK, if there");
		Commands.uninstall(apk.getAppPackage());
		LOG.info("installing APK");
		return Commands.install(apk.getPath());
	}
	
	private void handleInstallFailure(Results installResults) {
		/*
		 * use every log line from the brief log for installation. Unfortunately the 'dalvikvm' process id
		 * could differ from the PackageManager's, so we cannot filter by PID like during execution.
		 */
		String installFailureLog = Commands.getBriefLog(); // can be empty
		StringBuilder sb = new StringBuilder("installation of APK failed.\n");
		
		sb.append("output from install command was '");
		sb.append(installResults.getOutput());
		sb.append("', error output was '");
		sb.append(installResults.getErrors());
		sb.append("'.\n");
		
		sb.append("log output was '");
		sb.append(installFailureLog);
		sb.append("'");
		
		LOG.error(sb.toString());
	}
	
	private boolean startAndWatchForErrors(Apk apk) {
		LOG.info("starting separate log watcher thread for APK");
		LogcatWatcher logcatWatcher = new LogcatWatcher(apk.getActivityWithPackage());
		Thread watcherThread = new Thread(logcatWatcher, "logcatWatcher");
		watcherThread.start();
		String mainActivity = apk.getMainActivity();
		LOG.info("starting main activity in APK: {}", mainActivity);
		Commands.startActivity(apk.getAppPackage(), mainActivity);
		LOG.info("waiting for log watcher thread to terminate");
		try {
			watcherThread.join();
			LOG.info("log watcher thread for APK terminated");
		} catch (InterruptedException e) {
			LOG.warn("InterruptedException waiting for watcher thread", e);
			// try at least to stop the other thread, if waiting does not work
			watcherThread.interrupt();
		}
		return !logcatWatcher.foundError();
	}
}