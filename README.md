toDexTestFramework
==================

This framework is for testing the *toDex* part of [Soot](https://github.com/sable/soot/tree/develop (develop branch). For a detailed description (in german) see the master thesis [Generating and testing Dalvik bytecode for Android](http://www.ec-spride.tu-darmstadt.de/csf/sse/teaching_sse/theses_sse/theses_sse.en.jsp . In short, the framework reads Android apps (as .apk files), converts them, and uses the android emulator to test the converted app. To eliminate non-working untouched apps, the .apk file is run on the emulator beforehand. See [this blog post](http://www.bodden.de/2013/01/08/soot-android-instrumentation/ for the general usage of Soot's Android capabilities.

How to use
==========

The repo is a ready-to-use eclipse framework (which expects Soot with the toDex part in the workspace). First, you have to implement the interface os.OperatingSystem and use it in the os.Commands.OS variable. See os.ExampleOS for details - it defines certain OS specific paths for external tools form the JDK or Android SDK.

Running a test
--------------

The framework expects exactly one working android emulator running. You can check if this is the case with "adb devices" on your command line. Given that, you should supply the main method in main.MainTesting with the path to the .apk you want to test. The framework produces messages on the Console to keep you informed. These messages are also saved in a log file at logs/messages.log. You can also run multiple APKs by providing multiple paths to the main method (no spaces, sorry). To see a summary of all the tests you ran, see logs/summary.log for a log file with one line per test.

Testing f-droid.org APKs
------------------------

If you do not provide any arguments to the main method, the framework will test the APKs in the "fdroid" folder of the project. These should originate from the [fdroid repository](http://f-droid.org and were used during the framework's initial development. Note that some APKs are excluded from testing due to known failures (see fdroid.KnownFailures for details). To get all the current APKs in the repo, you could utilize the class fdroid.IndexXmlParser. Call its main method to get an URL list of all the latest apps in that repository.

Misc
====

Some minor things to note.

Fuzzing
-------

In the package fuzzing, you will find a prototypical IntConstantFuzzer. This is to show how one could use the framework for [fuzzing](http://en.wikipedia.org/wiki/Fuzz_testing the "toDex" part or Android's virtual machine with Soot's output. The simple IntConstantFuzzer changes at most one integer constant in the original APK to zero. To enable fuzzing as a third step in the test framework, see the TODO in main.MainTesting.

The file debug.keystore
-----------------------

To run an android app, its APK has to be [signed](http://developer.android.com/tools/publishing/app-signing.html . The test framework uses the file debug.keystore as a source for the private key needed for signing. The file includes the private key "release_me", which is protected by the keystore's password "debugNotWork", which you can deliberately see in the source code of the method os.Commands.sign(Path). So, if anybody [finds](http://www.h-online.com/open/news/item/GitHub-search-exposes-uploaded-credentials-1791252.html that key here on github: I'm OK with that ;)