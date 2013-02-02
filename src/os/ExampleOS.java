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

public class ExampleOS implements OperatingSystem {

	private static final String sdkPath = "/Users/thomas/ma/sdk/android-sdk-macosx/";

	private static final String jdkBinPath = "/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/bin/";

	@Override
	public String getAndroidJarPath() {
		return "/Users/thomas/ma/soot/android-platforms/android-15/android.jar";
	}

	@Override
	public String getZipalignPath() {
		return sdkPath + "tools/zipalign";
	}

	@Override
	public String getAdbPath() {
		return sdkPath + "platform-tools/adb";
	}

	@Override
	public String getAaptPath() {
		return sdkPath + "platform-tools/aapt";
	}

	@Override
	public String getJarsignerPath() {
		return jdkBinPath + "/jarsigner";
	}

}
