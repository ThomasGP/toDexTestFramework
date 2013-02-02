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

import java.nio.file.Path;

import os.Commands;

public class Apk {
	
	private static final String PACKAGE_NAME_HEADER = "package: name='";
	
	private static final String MAIN_ACTIVITY_HEADER = "launchable-activity: name='";
	
	private final Path path;
	
	private final String appPackage;
	
	private final String mainActivity;
	
	public Apk(Path path) {
		this.path = path;
		String packageInfo = Commands.getPackageInfo(path);
		this.appPackage = extractAppPackage(packageInfo);
		this.mainActivity = extractMainActivity(packageInfo);
	}
	
	private Apk(Path path, String appPackage, String mainActivity) {
		this.path = path;
		this.appPackage = appPackage;
		this.mainActivity = mainActivity;
	}
	
	public Apk withNewPath(Path newPath) {
		return new Apk(newPath, appPackage, mainActivity);
	}
	
	private String extractAppPackage(String packageInfo) {
		int packageStart = PACKAGE_NAME_HEADER.length();
		int packageEnd = packageInfo.indexOf('\'', packageStart);
		return packageInfo.substring(packageStart, packageEnd);
	}
	
	private String extractMainActivity(String packageInfo) {
		int mainActivityStart = packageInfo.indexOf(MAIN_ACTIVITY_HEADER);
		if (mainActivityStart == -1) {
			return ""; // leave main activity empty
		}
		mainActivityStart += MAIN_ACTIVITY_HEADER.length();
		int mainActivityEnd = packageInfo.indexOf('\'', mainActivityStart);
		String mainActivity = packageInfo.substring(mainActivityStart, mainActivityEnd);
		return mainActivity.replaceAll("\\$", "\\\\\\$"); // escape '$' for inner classes
	}

	public Path getPath() {
		return path;
	}

	public String getAppPackage() {
		return appPackage;
	}

	public String getMainActivity() {
		return mainActivity;
	}

	public String getName() {
		return path.getFileName().toString();
	}
	
	public String getActivityWithPackage() {
		// combine appPackage and mainActivity with a "/"
		if (mainActivity.startsWith(appPackage)) {
			/* 
			 * mainActivity is already combined but with wrong connector,
			 * so cut of the package part, leaving something like ".ActivityName"
			 */
			String dotActivity = mainActivity.substring(appPackage.length());
			return appPackage + "/" + dotActivity;
		} else {
			return appPackage + "/" + mainActivity;
		}
	}
}