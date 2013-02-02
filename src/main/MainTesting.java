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

import static fdroid.KnownFailures.hasKnownFailures;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import os.Commands;
import soot.Dexpler;
import soot.G;
import soot.Main;
import soot.SourceLocator;

public class MainTesting {
	
	private static final Logger LOG = LogManager.getLogger(MainTesting.class);
	
	private static final Marker SUMMARY_MARKER = MarkerManager.getMarker("SUMMARY");

	// usage: <optional list of APKs to test>
	// if the list is empty, the APKs in the folder "./fdroid" will be used
	public static void main(String[] args) throws Throwable {
		try {
			if (args.length == 0) {
				testFdroidApks();
			} else {
				testCommandLineApks(args);
			}
		} catch (Throwable t) {
			// try to log any throwable that goes up to the environment
			LOG.fatal(SUMMARY_MARKER, "top-level failure", t);
			// this log-and-throw antipattern is only "allowed" at the top-level...
			throw t;
		}
	}

	private static void testFdroidApks() {
		Set<Path> fdroidApks = new HashSet<Path>();
		Path fdroidDir = Paths.get("./fdroid");
		DirectoryStream<Path> apks;
		try {
			apks = Files.newDirectoryStream(fdroidDir, "*.{apk}");
		} catch (IOException e) {
			throw new RuntimeException("IOException while getting list of fdroid APKs", e);
		}
		for (Path apk : apks) {
			if (hasKnownFailures(apk)) {
				// skip whole APK if it has known failures not related to our implementation
				LOG.info(SUMMARY_MARKER, "skipping fdroid APK due to known failures: {}", apk);
				continue;
			}
			fdroidApks.add(apk.toAbsolutePath());
		}
		testApks(fdroidApks);
	}
	
	private static void testCommandLineApks(String[] args) {
		Set<Path> commandLineApks = new HashSet<Path>();
		for (String arg : args) {
			Path apkPath = Paths.get(arg).toAbsolutePath();
			commandLineApks.add(apkPath);
		}
		testApks(commandLineApks);
	}

	private static void testApks(Set<Path> apks) {
		int numApksToTest = apks.size();
		LOG.info("testing {} APK(s)", numApksToTest);
		int apkCounter = 0;
		for (Path apk : apks) {
			apkCounter++;
			LOG.info("testing APK {} of {}: {}", apkCounter, numApksToTest, apk);
			try {
				boolean success = testApk(apk);
				LOG.info(SUMMARY_MARKER, "success: {} for {}", success, apk);
			} catch (RuntimeException e) {
				LOG.warn("RuntimeException while testing APK", e);
				LOG.info(SUMMARY_MARKER, "RuntimeException, see detailed log for {}", apk);
			}
		}
	}
	
	private static boolean testApk(Path apk) {
		Emulator emulator = new Emulator();
		LOG.info("testing part 1/2: original APK");
		Apk originalApk = new Apk(apk);
		boolean originalSucceeded = emulator.run(originalApk);
		if (!originalSucceeded) {
			return false;
		}
		LOG.info("testing part 2/2: converted APK");
		Apk convertedApk = convertApk(originalApk);
		return emulator.run(convertedApk);
		/*
		 * TODO re-add testing part 3/3: fuzz original APK with an AbstractFuzzer and Soot, running it on the emulator:
		 * change the method runThroughSoot(String) to conditionally include this after the resetting and before calling Soot
		 * PackManager.v( ).getPack("jtp").add(new Transform("jtp.fuzzing", fuzzer)); // fuzzer should be an AbstractFuzzer
		 */
	}
	
	private static Apk convertApk(Apk oldApk) {
		LOG.info("converting APK");
		Path newApk = getNewApkPath(oldApk);
		try {
			Files.deleteIfExists(newApk);
		} catch (IOException e) {
			throw new RuntimeException("IOException while deleting old converted APK", e);
		}
		runThroughSoot(oldApk.getPath().toString());
		signAndAlign(newApk);
		return oldApk.withNewPath(newApk);
	}

	private static Path getNewApkPath(Apk oldApk) {
		String sootOutput = SourceLocator.v().getOutputDir();
		Path outputPath = Paths.get(sootOutput).toAbsolutePath();
		return outputPath.resolve(oldApk.getName());
	}

	private static void runThroughSoot(String apkPath) {
		LOG.info("running APK through Soot");
		G.reset(); // reset globals from previous Soot run in the same JVM (including the (fuzzing) transformer, if one was added!)
		Dexpler.reset(); //  reset dex class cache from previous Soot run
		
		String[] sootArgs = new String[]{
				// "-p", "jb.tr", "use-older-type-assigner:true",			// use old type assigner to prevent some stack overflow happening (commented out, since the APKs are excluded)
				"-p", "jb.lp", "enabled:true",							// enable local packer (to reduce number of locals to cope with)
				"-p", "jb.ne", "enabled:false",							// disable nop eliminator (deliberately, if they are in the original APK...)
				"-p", "jb.uce", "enabled:false",						// disable unreachable code eliminator (for the same reason)
				"-p", "jb.dae", "enabled:false",						// disable dead assignment eliminator (for the same reason)
				"-allow-phantom-refs",									// allow these refs (useful for libs with missing, but unused classes referenced)
				"-src-prec", "apk",										// assume input is an apk
				"-output-format", "dex",								// output to dex/apk
				"-force-android-jar", Commands.OS.getAndroidJarPath(),	// use this android.jar
				"-soot-classpath", apkPath,								// use given APK as soot-classpath
				"-prepend-classpath",									// add default soot-classpath after given one
				"-process-dir", apkPath,								// use all classes in the given APK as application classes
				/* 
				 * include these harmless packages, which are by default excluded (see Scene.excludedPackages, those are for a JRE).
				 * still not included are: java., javax., org.xml., org.w3c. and sun., since they are "harmfull" in the android world, too
				 */
				"-include", "com.sun.", "-include", "com.ibm.", "-include", "apple.awt.", "-include", "com.apple."
		};
		Main.main(sootArgs);
	}
	
	private static void signAndAlign(Path apk) {
		LOG.info("signing and aligning APK");
		Commands.sign(apk);
		Commands.align(apk);
	}
}