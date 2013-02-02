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

package fdroid;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class KnownFailures {
	
	private static final Set<String> apks;
	
	static {
		// TODO use reliable dexfile signature for apk identification instead of just the apk filename
		/*
		 * something like:
		 * DexFile df = new DexFile("Foo.apk");
		 * df.HeaderItem.getSignature(), available in smali rev. 1.3.3..., see:
		 * http://code.google.com/p/smali/source/detail?r=94abcd3332bbc12b4f1099bdd5d93ea8b6fb9c89
		 * unfortunatelly, soot uses rev. 1.3.2...
		 */
		apks = new HashSet<String>();
		apks.addAll(getEnvironmentFailApks());
		apks.addAll(getSootShortcomingsFailApks());
		apks.addAll(getUnknownFailApks());
		// NOTE: if adding new APKs: do not forget to comment on the failure
	}

	private static Set<String> getEnvironmentFailApks() {
		Set<String> environmentFailApks = new HashSet<String>();
		/*
		 * errors due to the environment
		 */
		// "Failure [INSTALL_FAILED_MISSING_SHARED_LIBRARY]" / "Package X requires unavailable shared library com.google.android.maps; failing!"
		environmentFailApks.add("net.codechunk.speedofsound_8.apk");
		environmentFailApks.add("to.networld.android.divedroid_1.apk");
		environmentFailApks.add("OpenGPSTracker_1.3.2-osmupdate.apk");
		environmentFailApks.add("org.mixare_20.apk");
		environmentFailApks.add("com.showmehills_4.apk");
		// "java.lang.UnsatisfiedLinkError: Couldn't load X: findLibrary returned null"
		environmentFailApks.add("com.jecelyin.editor_32.apk");
		environmentFailApks.add("eu.domob.anacam_10100.apk");
		environmentFailApks.add("net.tedstein.AndroSS_17.apk");
		environmentFailApks.add("com.dozingcatsoftware.bouncy_11.apk");
		environmentFailApks.add("org.coolreader_509.apk");
		environmentFailApks.add("org.eehouse.android.xw4_31.apk");
		// "java.io.FileNotFoundException: /mnt/sdcard/zoffcc/applications/aagtl/config/cookie.txt: open failed: ENOENT (No such file or directory)"
		environmentFailApks.add("com.zoffcc.applications.aagtl_31.apk");
		// "java.io.FileNotFoundException: /proc/net/xt_qtaguid/stats: open failed: ENOENT (No such file or directory)"
		environmentFailApks.add("aarddict.android_13.apk");
		// "D/MediaPlayer( 8080): Couldn't open file on client side, trying server side"
		// "E/MediaPlayer( 8080): Unable to to create media player"
		// "E/RingtoneManager( 8080): Failed to open ringtone content://settings/system/alarm_alert"
		environmentFailApks.add("com.angrydoughnuts.android.alarmclock_8.apk");
		// java.net.SocketTimeoutException, Caused by: libcore.io.ErrnoException: recvfrom failed: EAGAIN (Try again)
		environmentFailApks.add("net.rocrail.androc_362.apk");
		// "E/Error(11369): result false", because some directory could not be created with File.mkdir()
		environmentFailApks.add("org.liberty.android.fantastischmemo_135.apk");
		/*
		 * E/SQLiteDatabase( 5783): android.database.sqlite.SQLiteCantOpenDatabaseException: unable to open database file
		 * os_unix.c: open() at line 27701 - "" errno=2 path=/data/data/org.droidseries/databases/droidseries.db, db=/data/data/org.droidseries/databases/droidseries.db
		 */
		environmentFailApks.add("org.droidseries_13.apk");
		return environmentFailApks;
	}
	
	private static Set<String> getSootShortcomingsFailApks() {
		Set<String> sootShortcomingsFailApks = new HashSet<String>();
		/*
		 * errors due to soot shortcomings
		 */
		// these use API level 16 (like the field android.content.pm.ActivityInfo.parentActivityName), which we do not have an android.jar for yet
		sootShortcomingsFailApks.add("org.andstatus.app_61.apk");
		sootShortcomingsFailApks.add("CSipSimple-0.04-01.apk");
		// leads to an InternalTypingException, see http://www.sable.mcgill.ca/pipermail/soot-list/2012-October/004916.html
		sootShortcomingsFailApks.add("com.drismo_17.apk");
		/*
		 * "java.lang.IllegalArgumentException: Class ActionBarSherlockCompat is not annotated with @Implementation":
		 * at least the class com.actionbarsherlock.ActionBarSherlock has (class) annotations, which are currently not supported
		 */
		sootShortcomingsFailApks.add("org.adaway_38.apk");
		sootShortcomingsFailApks.add("eu.prismsw.lampshade_117.apk");
		// "java.lang.OutOfMemoryError: Java heap space" soot seems to need more than 1 GB for these
		sootShortcomingsFailApks.add("net.bible.android.activity_79.apk");
		sootShortcomingsFailApks.add("APG-1.0.8-release.apk");
		sootShortcomingsFailApks.add("es.cesar.quitesleep_13.apk");
		// due to a bug in the TypeResolver, the APK leads to a "java.lang.ClassCastException: soot.RefType cannot be cast to soot.ArrayType"
		// in soot.toDex.StmtVisitor.buildArrayGetInsn(), with the RefType being java.io.Serializable, which should be something like InetAddress[]
		sootShortcomingsFailApks.add("com.beem.project.beem_11.apk");
		// another bug in soot.jimple.toolkits.typing.fast.TypeResolver, inserts a cast from byte[] to int[]...
		sootShortcomingsFailApks.add("arity.calculator_27.apk");
		return sootShortcomingsFailApks;
	}
	
	private static Set<String> getUnknownFailApks() {
		Set<String> unknownFailApks = new HashSet<String>();
		/*
		 * errors with unknown sources, carefully considered harmless
		 */
		// "E/A2DP_Volume( 1551): errornull", logged from class a2dp.Vol.main without a crash
		unknownFailApks.add("a2dp.Vol_93.apk");
		// "E/VoiceP  (20711): minsize 1520 bufsize 16384", logged as error from constructor of class com.ihunda.android.binauralbeat.VoicesPlayer
		unknownFailApks.add("com.ihunda.android.binauralbeat_24.apk");
		// spams the error log with too many unnecessary entries
		unknownFailApks.add("Audalyzer-1.15.apk");
		// "W/webcore (21312): java.lang.Throwable: EventHub.removeMessages(int what = 107) is not supported before the WebViewCore is set up.", seems to be a programming error
		unknownFailApks.add("BarcodeScanner4.2.apk");
		// original APK fails with NPE in NetInfoAdapter.java:109
		unknownFailApks.add("com.eddyspace.networkmonitor_2.apk");
		return unknownFailApks;
	}
	
	public static boolean hasKnownFailures(Path apkPath) {
		String apkName = apkPath.getFileName().toString();
		return apks.contains(apkName);
	}
}