package com.cypher.selftosystem;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.cypher.selftosystem.SystemAppUtilities.SystemAppUtilitiesException;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class MainActivity extends Activity {
	public static final String TAG = "SelfToSystem";

	public static final String PREF = TAG + "_Pref";
	public static final String DESCRIPTION = TAG + "_Pref";

	private TextView tv_rootAvailable;
	private TextView tv_userApp;
	private TextView tv_systemApp;

	private SharedPreferences pref;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		pref = getSharedPreferences(PREF, MODE_PRIVATE);

		tv_rootAvailable = (TextView) findViewById(R.id.tv_rootAvailable);
		tv_userApp = (TextView) findViewById(R.id.tv_userApp);
		tv_systemApp = (TextView) findViewById(R.id.tv_systemApp);
		Button btn_refreshData = (Button) findViewById(R.id.btn_refreshData);
		Button btn_gainRootAccess = (Button) findViewById(R.id.btn_gainRootAccess);
		Button btn_installAsSystemApp = (Button) findViewById(R.id.btn_installAsSystemApp);
		Button btn_uninstallSystemApp = (Button) findViewById(R.id.btn_uninstallSystemApp);
		Button btn_uninstallUserApp = (Button) findViewById(R.id.btn_uninstallUserApp);
		Button btn_clearAppData = (Button) findViewById(R.id.btn_clearAppData);
		Button btn_softReboot = (Button) findViewById(R.id.btn_softReboot);
		Button btn_reboot = (Button) findViewById(R.id.btn_reboot);

		btn_refreshData.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (blockingOkCancelDialog(getString(R.string.msg_header_info), getString(R.string.msg_refreshData))) {
					try {
						refreshData();
					} catch (SystemAppUtilitiesException e) {
						errorDialog(e.getMessage());
					}
				}
			}
		});

		btn_gainRootAccess.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (blockingOkCancelDialog(getString(R.string.msg_header_info), getString(R.string.msg_gainRootAccess))) {
					try {
						SystemAppUtilities.gainRootAccess();
					} catch (SystemAppUtilitiesException e) {
						errorDialog(e.getMessage());
					}
				}
			}
		});

		btn_installAsSystemApp.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (blockingOkCancelDialog(getString(R.string.msg_header_warning), getString(R.string.msg_installAsSystemApp))) {
					try {
						SystemAppUtilities.installAsSystemApp(MainActivity.this, true);
					} catch (SystemAppUtilitiesException e) {
						errorDialog(e.getMessage());
					}
				}
			}
		});

		btn_uninstallSystemApp.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (blockingOkCancelDialog(getString(R.string.msg_header_warning), getString(R.string.msg_uninstallSystemApp))) {
					try {
						SystemAppUtilities.uninstallSystemApp(MainActivity.this, true);
					} catch (SystemAppUtilitiesException e) {
						errorDialog(e.getMessage());
					}
				}
			}
		});

		btn_uninstallUserApp.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (blockingOkCancelDialog(getString(R.string.msg_header_warning), getString(R.string.msg_uninstallUserApp))) {
					try {
						SystemAppUtilities.uninstallUserApp(MainActivity.this);
					} catch (SystemAppUtilitiesException e) {
						errorDialog(e.getMessage());
					}
				}
			}
		});

		btn_clearAppData.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (blockingOkCancelDialog(getString(R.string.msg_header_warning), getString(R.string.msg_clearAppData))) {
					try {
						SystemAppUtilities.clearAppData(MainActivity.this);
					} catch (SystemAppUtilitiesException e) {
						errorDialog(e.getMessage());
					}
				}
			}
		});

		btn_softReboot.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (blockingOkCancelDialog(getString(R.string.msg_header_info), getString(R.string.msg_reboot))) {
					SystemAppUtilities.rebootDevice(MainActivity.this, true);
				}
			}
		});

		btn_reboot.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (blockingOkCancelDialog(getString(R.string.msg_header_info), getString(R.string.msg_reboot))) {
					SystemAppUtilities.rebootDevice(MainActivity.this, false);
				}
			}
		});

		if (pref.getBoolean(DESCRIPTION, true)) {
			pref.edit().putBoolean(DESCRIPTION, false).apply();

			if (!blockingTwoButtonDialog(getString(R.string.app_name), getString(R.string.msg_first_start), getString(R.string.button_close), getString(R.string.button_github))) {
				Uri uri = Uri.parse("https://github.com/Cypher01/SelfToSystem");
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(intent);
			}
		}

		if (!SystemAppUtilities.isRootAvailable()) {
			btn_gainRootAccess.setEnabled(false);
			btn_installAsSystemApp.setEnabled(false);
			btn_uninstallSystemApp.setEnabled(false);
			btn_uninstallUserApp.setEnabled(false);
			btn_clearAppData.setEnabled(false);

			errorDialog(getString(R.string.msg_root_unavailable));
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		try {
			refreshData();
		} catch (SystemAppUtilitiesException e) {
			e.printStackTrace();
		}
	}

	private void refreshData() throws SystemAppUtilitiesException {
		if (SystemAppUtilities.isRootAvailable()) {
			tv_rootAvailable.setText(getString(R.string.root_available));
			tv_rootAvailable.setTextColor(0xff99cc00); // @android:color/holo_green_light
		} else {
			tv_rootAvailable.setText(getString(R.string.root_unavailable));
			tv_rootAvailable.setTextColor(0xffff4444); // @android:color/holo_red_light
		}

		tv_userApp.setText(SystemAppUtilities.getApkInfos(MainActivity.this, false));
		tv_systemApp.setText(SystemAppUtilities.getApkInfos(MainActivity.this, true));
	}

	public boolean blockingOkCancelDialog(String title, String message) {
		return blockingTwoButtonDialog(title, message, getString(R.string.button_ok), getString(R.string.button_cancel));
	}

	public boolean blockingTwoButtonDialog(String title, String message, String button1, String button2) {
		final boolean[] mResult = new boolean[1]; // for whatever reason, IntelliJ suggested to use an array

		final Handler handler = new Handler() {
			@Override
			public void handleMessage(Message mesg) {
				throw new RuntimeException();
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle(title)
				.setMessage(message)
				.setPositiveButton(button1, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						mResult[0] = true;
						handler.sendMessage(handler.obtainMessage());
					}
				})
						.setNegativeButton(button2, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						mResult[0] = false;
						handler.sendMessage(handler.obtainMessage());
					}
				})
				.show();

		try { Looper.loop(); } catch (RuntimeException ignored) { }

		return mResult[0];
	}

	public boolean blockingOneButtonDialog(String button, String title, String message) {
		final boolean[] mResult = new boolean[1]; // for whatever reason, IntelliJ suggested to use an array

		final Handler handler = new Handler() {
			@Override
			public void handleMessage(Message mesg) {
				throw new RuntimeException();
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle(title)
				.setMessage(message)
				.setNeutralButton(button, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						mResult[0] = true;
						handler.sendMessage(handler.obtainMessage());
					}
				}).show();

		try { Looper.loop(); } catch (RuntimeException ignored) { }

		return mResult[0];
	}

	private void errorDialog(String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle(getString(R.string.msg_header_error))
				.setMessage(message)
				.setNeutralButton(R.string.button_close, null)
				.show();
	}
}
