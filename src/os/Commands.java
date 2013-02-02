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

package os;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Commands {
	
	private static final Logger LOG = LogManager.getLogger(Commands.class);
	
	// FIXME define your own OS specific paths and use them!
	public static final OperatingSystem OS = new ExampleOS();
	
	private static final String DEFAULT_ADB_PATH = OS.getAdbPath();
	
	public static String getPackageInfo(Path apkPath) {
		// dump short package info for APK %s
		String aaptCmd = OS.getAaptPath() + " dump badging %s";
		Results aaptResults = execAndGetResults(String.format(aaptCmd, apkPath));
		return aaptResults.getOutput();
	}
	
	public static void sign(Path apkPath) {
		// sign APK %s with key release_me from debug.keystore
		String jarsignerCmd = OS.getJarsignerPath() + " -storepass debugNotWork -sigalg MD5withRSA -digestalg SHA1 -keystore ." + File.separator + "debug.keystore %s release_me";
		execAndGetResults(String.format(jarsignerCmd, apkPath));
	}

	public static void align(Path apkPath) {
		// align APK %s to 4 bytes, saving it in file aligned-%s
		String apkName = apkPath.getFileName().toString();
		Path alignedApkPath = apkPath.resolveSibling("aligned-" + apkName);
		String zipalignCmd = OS.getZipalignPath() + " -v 4 %s %s";
		execAndGetResults(String.format(zipalignCmd, apkPath, alignedApkPath));
		// move aligned APK over original APK
		try {
			Files.move(alignedApkPath, apkPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException("IOException while moving aligned APK over original APK", e);
		}
	}
	
	public static String getEmulatorTime() {
		// get epoch time
		String getTimeCmd = DEFAULT_ADB_PATH + " shell date +\"%s\"";
		Results timeResults = execAndGetResults(getTimeCmd);
		return timeResults.getOutput();
	}
	
	public static Results install(Path path) {
		// (re)install file %s
		String installCmd = DEFAULT_ADB_PATH + " install -r %s";
		return execAndGetResults(String.format(installCmd, path.toString()));
	}
	
	public static void uninstall(String appPackage) {
		// uninstall package %s
		String uninstallCmd = DEFAULT_ADB_PATH + " uninstall %s";
		execAndGetResults(String.format(uninstallCmd, appPackage));
	}
	
	public static String getDevices() {
		// list known devices
		String devicesCmd = DEFAULT_ADB_PATH + " devices";
		Results adbDevices = execAndGetResults(devicesCmd);
		return adbDevices.getOutput();
	}
	
	public static void clearLogcat() {
		// clear logs
		String clearLogcatCmd = DEFAULT_ADB_PATH + " logcat -c";
		execAndGetResults(clearLogcatCmd);
	}
	
	public static void startActivity(String appPackage, String mainActivity) {
		// tell ActivityManager to start the "main intent" of activity %s/%s
		String startCmd = DEFAULT_ADB_PATH + " shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n %s/%s";
		execAndGetResults(String.format(startCmd, appPackage, mainActivity));
	}
	
	public static String getBriefLog() {
		// get brief log, terminating logcat thereafter
		String briefLogCmd = DEFAULT_ADB_PATH + " logcat -v brief -d";
		Results briefLog = execAndGetResults(briefLogCmd);
		return briefLog.getOutput();
	}
	
	public static String getProcessActivityLog() {
		// dump and exit logcat logger, use short "process" format, filter for entries with tag "ActivityManager" which level is >= info
		String processLogCmd = DEFAULT_ADB_PATH + " logcat -v process -d ActivityManager:I *:S";
		Results processLog = execAndGetResults(processLogCmd);
		return processLog.getOutput();
	}
	
	private static void waitForProcess(Process proc) {
		LOG.debug("waiting for process to terminate");
		int exitValue;
		try {
			exitValue = proc.waitFor();
		} catch (InterruptedException e) {
			throw new RuntimeException("InterruptedException while waiting for the process to terminate", e);
		}
		if (exitValue != 0) {
			throw new RuntimeException("process terminated abnormal with code " + exitValue);
		}
	}

	private static Results getResults(Process proc) {
		LOG.debug("getting results from process");
		String output = getLines(proc.getInputStream());
		String errors = getLines(proc.getErrorStream());
		waitForProcess(proc);
		return new Results(output, errors);
	}
	
	private static String getLines(InputStream in) {
		StringBuilder lines = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		try {
			String line = reader.readLine();
			while (line != null) {
				lines.append(line);
				lines.append('\n');
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			throw new RuntimeException("IOException while reading input lines", e);
		}
		return lines.toString();
	}
	
	private static Results execAndGetResults(String command) {
		LOG.debug("executing command {}", command);
		Process proc;
		try {
			proc = Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			throw new RuntimeException("IOException while executing command " + command, e);
		}
		return getResults(proc);
	}
}