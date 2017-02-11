package com.cypher.selftosystem;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.util.Log;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * SystemAppUtilities, created by Cypher01
 *
 * This class offers various tools to make an app capable of making itself a system app
 *
 * This class can be used for any app to extend its capabilities and make it resistant to factory resets
 *
 * The main concept of this class is based on GSam Battery Monitor Root Companion [1]
 * The original class has been heavily adopted and extended
 *
 * [1] https://github.com/GSamLabs/GSamBatteryMonitor-RootCompanion
 */
public class SystemAppUtilities {
	private static final String TAG = SystemAppUtilities.class.getSimpleName();

	private static final String systemAppFile;

	static {
		String systemAppsPath = "/system/priv-app/";
		String subfolder = "";
		String apkName = BuildConfig.APPLICATION_ID + ".apk";

		if (android.os.Build.VERSION.SDK_INT < 18) { // Android 4.2: API 17, Android 4.3: API 18 (first with priv-app)
			systemAppsPath = "/system/app/";
		}

		if (android.os.Build.VERSION.SDK_INT < 20) { // Android 4.4: API 19, Android 5.0: API 21 (first with subfolders for apps)
			// TODO: Create subfolder for system app? Not necessary, but would be nice.
			//String appName = ; // TODO: We should use the app name, but how to get it without the context?
			//subfolder = appName + "/";
			//apkName = appName + ".apk";
		}

		systemAppFile = systemAppsPath + subfolder + apkName;
	}

	/**
	 * Check if root is available using RootTools
	 * In general, this doesn't actually call SU, but may in certain cases
	 *
	 * @return root is available or not
	 */
	public static boolean isRootAvailable() {
		return RootTools.isRootAvailable();
	}

	/**
	 * Gain root access using RootTools
	 * This leads to a root confirmation popup
	 *
	 * @throws SystemAppManagementException in case of an error, check message
	 */
	public static void gainRootAccess() throws SystemAppManagementException {
		if (!RootTools.isAccessGiven()) {
			throw new SystemAppManagementException("Unable to obtain root access. Please make sure you grant this app root authority.");
		}
	}

	/**
	 * Get APK infos in form of a string
	 * This can be used preferably for debugging, not really for production
	 *
	 * @param context app context
	 * @param systemApp true for the system app (if it exists), false for the user app (if it exists)
	 * @return APK path, version code and version name, or not-available-message
	 * @throws SystemAppManagementException in case of an error, check message
	 */
	public static String getApkInfos(final Context context, boolean systemApp) throws SystemAppManagementException {
		String currentFile = getApkName(context, true);

		if (systemApp) {
			if (isSystemApp(context)) {
				currentFile = systemAppFile;
			} else {
				return "No system app available";
			}
		} else {
			if (!currentFile.startsWith("/data/app/")) {
				return "No user app available";
			}
		}

		PackageInfo info = context.getPackageManager().getPackageArchiveInfo(currentFile, 0);

		return "Apk file: " + currentFile + "\nVersion code: " + info.versionCode + "\nVersion name: " + info.versionName;
	}

	/**
	 * Get APK version code
	 * This can be used to check if the user app has been updated and the system app should also get updated
	 *
	 * @param context app context
	 * @param systemApp true for the system app (if it exists), false for the user app (if it exists)
	 * @return APKs version code
	 * @throws SystemAppManagementException in case of an error, check message
	 */
	public static int getApkVersionCode(final Context context, boolean systemApp) throws SystemAppManagementException {
		String currentFile = getApkName(context, true);

		if (systemApp) {
			if (isSystemApp(context)) {
				currentFile = systemAppFile;
			} else {
				throw new SystemAppManagementException("No system app available.");
			}
		} else {
			if (!currentFile.startsWith("/data/app/")) {
				throw new SystemAppManagementException("No user app available.");
			}
		}

		PackageInfo info = context.getPackageManager().getPackageArchiveInfo(currentFile, 0);

		return info.versionCode;
	}

	/**
	 * Check if the app is a system app, independent of whether there is also a user app or not
	 *
	 * @param context app context
	 * @return true if app is a system app, false otherwise
	 */
	public static boolean isSystemApp(final Context context) {
		return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_SYSTEM) != 0;
	}

	/**
	 * Starts an AsyncTask to copy the user app, if existing, to the system partition
	 *
	 * @param context app context
	 * @param overwriteIfExists decides if an existing system app should be overwritten or not
	 * @throws SystemAppManagementException in case of an error, check message
	 */
	public static void installAsSystemApp(final Context context, final boolean overwriteIfExists) throws SystemAppManagementException {
		AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
			SystemAppManagementException error = null;
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				progress = ProgressDialog.show(context, context.getText(R.string.progress_title), context.getText(R.string.progress_copy_to_system));
			}

			@Override
			protected Boolean doInBackground(Void... params) {
				try {
					copyAppToSystem(context, overwriteIfExists);
				} catch (SystemAppManagementException e) {
					error = e;
					return false;
				}

				return true;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				progress.dismiss();
				checkResult(result, context, error);
			}
		};

		task.execute((Void) null);
	}

	/**
	 * Starts an AsyncTask to delete the system app, if existing, from the system partition
	 *
	 * @param context app context
	 * @param keepUserApp decides if an existing user app should be kept or deleted
	 * @throws SystemAppManagementException in case of an error, check message
	 */
	public static void uninstallSystemApp(final Context context, final boolean keepUserApp) throws SystemAppManagementException {
		AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
			SystemAppManagementException error = null;
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				progress = ProgressDialog.show(context, context.getText(R.string.progress_title), context.getText(R.string.progress_uninstalling));
			}

			@Override
			protected Boolean doInBackground(Void... params) {
				try {
					deleteSystemApp(context, keepUserApp);
				} catch (SystemAppManagementException e) {
					error = e;
					return false;
				}

				return true;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				progress.dismiss();

				checkResult(result, context, error);
			}
		};

		task.execute((Void) null);
	}

	/**
	 * Starts an AsyncTask to delete the user app, if existing, from the data partition
	 * This doesn't care about if there is a system app, so it maybe lead to a complete uninstall
	 *
	 * CAUTION! This could lead to problems on various Android versions!
	 *
	 * @param context app context
	 * @throws SystemAppManagementException in case of an error, check message
	 */
	public static void uninstallUserApp(final Context context) throws SystemAppManagementException {
		AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
			SystemAppManagementException error = null;
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				progress = ProgressDialog.show(context, context.getText(R.string.progress_title), context.getText(R.string.progress_uninstalling));
			}

			@Override
			protected Boolean doInBackground(Void... params) {
				try {
					deleteUserApp(context);
				} catch (SystemAppManagementException e) {
					error = e;
					return false;
				}

				return true;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				progress.dismiss();

				checkResult(result, context, error);
			}
		};

		task.execute((Void) null);
	}

	/**
	 * Starts an AsyncTask to delete the app data, like doing so in the Android settings
	 *
	 * CAUTION! This could lead to problems on various Android versions!
	 *
	 * @param context app context
	 * @throws SystemAppManagementException in case of an error, check message
	 */
	public static void clearAppData(final Context context) throws SystemAppManagementException {
		AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
			SystemAppManagementException error = null;
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				progress = ProgressDialog.show(context, context.getText(R.string.progress_title), context.getText(R.string.progress_deleting));
			}

			@Override
			protected Boolean doInBackground(Void... params) {
				try {
					deleteAppData(context);
				} catch (SystemAppManagementException e) {
					error = e;
					return false;
				}

				return true;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				progress.dismiss();

				checkResult(result, context, error);
			}
		};

		task.execute((Void) null);
	}

	/**
	 * Returns the APK file name, by request including its full path
	 *
	 * @param context app context
	 * @param includeFullPath decides if the full path should be included or not
	 * @return APK file name, including its full path or not
	 * @throws SystemAppManagementException in case of an error, check message
	 */
	private static String getApkName(final Context context, boolean includeFullPath) throws SystemAppManagementException {
		String fullPath = context.getApplicationInfo().sourceDir;

		if (fullPath.isEmpty() || (fullPath.lastIndexOf('/') == -1)) {
			throw new SystemAppManagementException("Unable to find the path to the APK. Is it already uninstalled? Did you remember to reboot after uninstalling? Current location appears to be " + fullPath + ".");
		}

		if (!includeFullPath) {
			fullPath = fullPath.substring(fullPath.lastIndexOf('/') + 1);
		}

		return fullPath;
	}

	/**
	 * Copies the user app, if existing, to the system partition using RootTools
	 *
	 * @param context app context
	 * @param overwriteIfExists decides if an existing system app should be overwritten or not
	 * @throws SystemAppManagementException in case of an error, check message
	 */
	private static void copyAppToSystem(final Context context, final boolean overwriteIfExists) throws SystemAppManagementException {
		// Verify we do have root
		gainRootAccess();

		// Copy the file to system partition
		String currentFile = getApkName(context, true);

		if (currentFile.startsWith("/system/")) {
			throw new SystemAppManagementException("Only a system app is available but no user app.");
		}

		if (RootTools.exists(systemAppFile, false)) {
			if (overwriteIfExists) {
				boolean deletedApp = RootTools.deleteFileOrDirectory(systemAppFile, true);

				if (!deletedApp) {
					throw new SystemAppManagementException("Unable to delete the file " + systemAppFile + ".");
				}
			} else {
				// RootTools.copyFile(...) below overwrites existing files, so throw an exception if the file exists
				throw new SystemAppManagementException("The file " + systemAppFile + " already exists.");
			}
		}

		Log.d(TAG, "Using RootTools to copy app from " + currentFile + " to " + systemAppFile);

		// RootTools.copyFile(...) overwrites existing files, so we check if the file exists above
		boolean copiedApp = RootTools.copyFile(currentFile, systemAppFile, true, true);

		if (!copiedApp) {
			throw new SystemAppManagementException("Unable to copy the file " + currentFile + " to " + systemAppFile + ".");
		}
	}

	/**
	 * Copies the system app, if existing, to the data partition using RootTools
	 *
	 * No overwriteIfExists parameter is used here, existing user apps are deleted regardless
	 * First, it is too annoying to discover an existing app
	 * Second, this shouldn't be used anyway if there is an existing user app
	 *
	 * The purpose of this method is to restore a user app, if it got lost by a factory reset.
	 *
	 * @param context app context
	 * @throws SystemAppManagementException in case of an error, check message
	 */
	private static void copyAppToData(final Context context) throws SystemAppManagementException {
		String currentFile = getApkName(context, true);

		// It shouldn't be necessary to test this, because we tested it before calling this method
		// Let's do it anyway, if this method is going to be used somewhere else in the future
		// We don't use isSystemApp(...) here, because, although this should not happen, the app or device probably haven't been restarted
		if (!RootTools.exists(systemAppFile)) {
			throw new SystemAppManagementException("System app doesn't exist, nothing to do.");
		}

		// Verify we do have root
		gainRootAccess();

		String targetPath;
		boolean copiedApp = true;

		if (android.os.Build.VERSION.SDK_INT < 20) { // Android 4.4: API 19, Android 5.0: API 21 (first with subfolders for apps)
			// Although this should never happen, delete possibly existing user app files or folders using a wildcard
			boolean deletedUserApp = RootTools.deleteFileOrDirectory("/data/app/" + context.getPackageName() + "*", true);
			// It doesn't really matter if this worked, so don't check deletedUserApp

			// Set the target file name
			targetPath = "/data/app/" + context.getPackageName() + "-1.apk";
		} else {
			String userAppDir;

			// Although this should never happen, delete possibly existing user app files or folders using a wildcard
			boolean deletedUserApp = RootTools.deleteFileOrDirectory("/data/app/" + context.getPackageName() + "*", true);
			// It doesn't really matter if this worked, so don't check deletedUserApp

			// Set the target file name
			targetPath = "/data/app/" + context.getPackageName() + "-1/base.apk";
			// Set the target folder name
			userAppDir = "/data/app/" + context.getPackageName() + "-1";

			// Create the target folder
			copiedApp &= executeCommand("mkdir " + userAppDir);
			copiedApp &= executeCommand("chmod 755 " + userAppDir);
			copiedApp &= executeCommand("chown system:system " + userAppDir);
		}

		Log.d(TAG, "Using RootTools to copy app from " + currentFile + " to " + targetPath);

		copiedApp &= RootTools.copyFile(currentFile, targetPath, false, true);
		copiedApp &= executeCommand("chown system:system " + targetPath);

		if (!copiedApp) {
			// Revert eventually successful parts by deleting the folder
			boolean deletedUserAppDir = RootTools.deleteFileOrDirectory(targetPath.substring(0, targetPath.lastIndexOf('/')), true);

			throw new SystemAppManagementException("Unable to copy the file " + currentFile + " to " + targetPath + ".");
		}
	}

	/**
	 * Deletes the system app, if existing, from the system partition using RootTools
	 *
	 * @param context app context
	 * @param keepUserApp decides if an existing user app should be kept or deleted
	 * @throws SystemAppManagementException in case of an error, check message
	 */
	private static void deleteSystemApp(final Context context, final boolean keepUserApp) throws SystemAppManagementException {
		// We don't use isSystemApp(...) here, because, although this should not happen, the app or device probably haven't been restarted
		if (!RootTools.exists(systemAppFile)) {
			throw new SystemAppManagementException("System app doesn't exist, nothing to do.");
		}

		// Verify we do have root
		gainRootAccess();

		// First, evaluate if the user app is still there
		String currentFile = getApkName(context, true);

		if (currentFile.startsWith("/system/")) {
			// Only the system app is left, most likely because of a factory reset
			if (keepUserApp) {
				copyAppToData(context);
			}
		}

		Log.d(TAG, "Using RootTools to delete app from " + systemAppFile);

		// Delete app on system partition
		boolean deletedSystemApp = RootTools.deleteFileOrDirectory(systemAppFile, true);

		if (!deletedSystemApp) {
			throw new SystemAppManagementException("Unable to delete the file " + systemAppFile + ".");
		}
	}

	/**
	 * Deletes the user app, if existing, from the data partition using RootTools
	 * This doesn't care about if there is a system app, so it maybe lead to a complete uninstall
	 *
	 * CAUTION! This could lead to problems on various Android versions!
	 *
	 * @param context app context
	 * @throws SystemAppManagementException in case of an error, check message
	 */
	private static void deleteUserApp(final Context context) throws SystemAppManagementException {
		// Verify we do have root
		gainRootAccess();

		// First, evaluate if the user app is still there
		String currentFile = getApkName(context, true);

		if (currentFile.startsWith("/system/")) {
			throw new SystemAppManagementException("User app doesn't exist, only system app is left.");
		}

		String userAppDir;

		if (android.os.Build.VERSION.SDK_INT < 20) { // Android 4.4: API 19, Android 5.0: API 21 (first with subfolders for apps)
			userAppDir = currentFile;
		} else {
			userAppDir = currentFile.substring(0, currentFile.lastIndexOf('/'));
		}

		if (userAppDir.startsWith("/data/app/") && (userAppDir.length() > "/data/app/".length())) {
			Log.d(TAG, "Using RootTools to delete app from " + userAppDir);

			boolean deletedDataApp = RootTools.deleteFileOrDirectory(userAppDir, false);

			if (!deletedDataApp) {
				throw new SystemAppManagementException("Unable to delete the file " + userAppDir + ".");
			}
		}
	}

	/**
	 * Deletes the app data from the data partition, like doing so in the Android settings
	 *
	 * CAUTION! This could lead to problems on various Android versions!
	 *
	 * @param context app context
	 * @throws SystemAppManagementException in case of an error, check message
	 */
	private static void deleteAppData(final Context context) throws SystemAppManagementException {
		// Verify we do have root
		gainRootAccess();

		String dataDir = context.getApplicationInfo().dataDir;

		if (dataDir.contains(context.getPackageName())) { // TODO: Is this a good idea? Or is it better to just search for app name?
			Log.d(TAG, "Using RootTools to delete data from " + dataDir);

			boolean deletedDataDir = RootTools.deleteFileOrDirectory(dataDir, false);

			if (!deletedDataDir) {
				throw new SystemAppManagementException("Unable to delete the directory " + dataDir + ".");
			}
		}
	}

	/**
	 * Checks the result of any AsyncTask
	 * In case of a success, it shows a dialog asking you to reboot your device
	 * In case of an error, it shows a dialog containing the error message
	 *
	 * @param result true if successful, false if an error occurred
	 * @param context app context
	 * @param error error exception, null if successful
	 */
	private static void checkResult(Boolean result, final Context context, SystemAppManagementException error) {
		if (result) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(R.string.complete_title)
					.setMessage(R.string.complete_reboot)
					.setNegativeButton(R.string.button_soft_reboot, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							rebootDevice(((Dialog) dialog).getContext(), true);
						}
					})
					.setPositiveButton(R.string.button_reboot, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							rebootDevice(((Dialog) dialog).getContext(), false);
						}
					})
					.setNeutralButton(R.string.button_no, null)
					.show();
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			String message;

			if (error != null) {
				message = error.getMessage();
			} else {
				message = "Unknown Error";
			}

			builder.setMessage(message)
					.setNeutralButton(android.R.string.ok, null)
					.show();
		}
	}

	/**
	 * Starts an AsyncTask to reboot the device
	 * This can be done by a soft reboot, that's just restarting zygote, or doing a complete Unix like reboot
	 *
	 * @param context app context
	 * @param softReboot decides whether doing a soft or complete reboot
	 */
	private static void rebootDevice(final Context context, final boolean softReboot) {
		AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				progress = ProgressDialog.show(context, context.getText(R.string.progress_title), context.getText(R.string.progress_title));
			}

			@Override
			protected Boolean doInBackground(Void... params) {
				String command = "reboot";

				if (softReboot) {
					// Using RootTools.restartAndroid() simply kills the zygote process and lets Android restart it, but allegedly this doesn't work on all devices
					// This command is used by the Xposed Installer as well as GravityBox and should work more reliable
					command = "setprop ctl.restart surfaceflinger; setprop ctl.restart zygote";
				}

				return executeCommand(command);
			}

			@Override
			protected void onPostExecute(Boolean result) {
				progress.dismiss();

				if (!result) {
					Log.d(TAG, "Reboot via RootTools failed");
					AlertDialog.Builder builder = new AlertDialog.Builder(context);
					builder.setMessage("Unable to reboot automatically. Please reboot your phone manually.")
							.setNeutralButton(R.string.button_ok, null)
							.show();
				}
			}
		};

		task.execute((Void) null);
	}

	/**
	 * Executes a command on the command line
	 *
	 * Possible exceptions are only logged via Android logging, because they most probably don't even occur
	 *
	 * @param command the command string
	 * @return true if execution was successful, false otherwise
	 */
	private static boolean executeCommand(String command) {
		Command cmd = new Command(100, command);

		try {
			RootTools.getShell(true).add(cmd);
		} catch (IOException e) {
			Log.d(TAG, "IOException on executeCommand");
			return false;
		} catch (RootDeniedException e) {
			Log.d(TAG, "RootDeniedException on executeCommand");
			return false;
		} catch (TimeoutException e) {
			Log.d(TAG, "TimeoutException on executeCommand");
			return false;
		}

		int count = 0;

		while (!cmd.isFinished() && (count < 100)) {
			Log.d(TAG, "sleeping 100");
			try { Thread.sleep(100); } catch (InterruptedException ignored) { }
			count++;
		}

		return cmd.getExitCode() == 0; // true if OK
	}

	/**
	 * Simple exception used for various error messages
	 */
	public static class SystemAppManagementException extends Exception {
		private static final long serialVersionUID = 5745558614933336197L;

		public SystemAppManagementException(String msg) {
			super(msg);
		}

		public SystemAppManagementException(String msg, Throwable e) {
			super(msg, e);
		}
	}
}
