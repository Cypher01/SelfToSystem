package com.cypher.selftosystem;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.cypher.selftosystem.SystemAppUtilities.SystemAppManagementException;

public class MainActivity extends Activity {
	public static final String TAG = "SelfToSystem";

	private TextView tv_rootAvailable;
	private TextView tv_userApp;
	private TextView tv_systemApp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		tv_rootAvailable = (TextView) findViewById(R.id.tv_rootAvailable);
		tv_userApp = (TextView) findViewById(R.id.tv_userApp);
		tv_systemApp = (TextView) findViewById(R.id.tv_systemApp);
		Button btn_refreshData = (Button) findViewById(R.id.btn_refreshData);
		Button btn_gainRootAccess = (Button) findViewById(R.id.btn_gainRootAccess);
		Button btn_installAsSystemApp = (Button) findViewById(R.id.btn_installAsSystemApp);
		Button btn_uninstallSystemApp = (Button) findViewById(R.id.btn_uninstallSystemApp);
		Button btn_uninstallUserApp = (Button) findViewById(R.id.btn_uninstallUserApp);
		Button btn_clearAppData = (Button) findViewById(R.id.btn_clearAppData);

		btn_refreshData.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					refreshData();
				} catch (SystemAppManagementException e) {
					errorDialog(e.getMessage());
				}
			}
		});

		btn_gainRootAccess.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					SystemAppUtilities.gainRootAccess();
				} catch (SystemAppManagementException e) {
					errorDialog(e.getMessage());
				}
			}
		});

		btn_installAsSystemApp.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					SystemAppUtilities.installAsSystemApp(MainActivity.this, true);
				} catch (SystemAppManagementException e) {
					errorDialog(e.getMessage());
				}
			}
		});

		btn_uninstallSystemApp.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					SystemAppUtilities.uninstallSystemApp(MainActivity.this, true);
				} catch (SystemAppManagementException e) {
					errorDialog(e.getMessage());
				}
			}
		});

		btn_uninstallUserApp.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					SystemAppUtilities.uninstallUserApp(MainActivity.this);
				} catch (SystemAppManagementException e) {
					errorDialog(e.getMessage());
				}
			}
		});

		btn_clearAppData.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					SystemAppUtilities.clearAppData(MainActivity.this);
				} catch (SystemAppManagementException e) {
					errorDialog(e.getMessage());
				}
			}
		});

		if (!SystemAppUtilities.isRootAvailable()) {
			btn_gainRootAccess.setEnabled(false);
			btn_installAsSystemApp.setEnabled(false);
			btn_uninstallSystemApp.setEnabled(false);

			errorDialog("This App works only on a rooted device. It won't help without root access.");
		}
	}

	@Override
	protected void onResume() {
		try {
			refreshData();
		} catch (SystemAppManagementException e) {
			e.printStackTrace();
		}
	}

	private void refreshData() throws SystemAppManagementException {
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

	private void errorDialog(String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle("Error")
				.setMessage(message)
				.setNeutralButton(R.string.button_close, null)
				.show();
	}

	private boolean warningDialog(String message) {
		final boolean[] answer = new boolean[1]; // for whatever reason, IntelliJ suggested to use an array
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle("Warning")
				.setMessage(message)
				.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						answer[0] = true;
					}
				})
				.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						answer[0] = false;
					}
				})
				.show();
		return answer[0];
	}
}
