package net.sf.openrocket.android.thrustcurve;

import net.sf.openrocket.R;
import net.sf.openrocket.android.db.DbAdapter;
import net.sf.openrocket.android.motor.ExtendedThrustCurveMotor;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class TCQueryActivity extends Activity
implements TCQueryAction.OnComplete
{

	private static final String TAG = "ThrustCurveQueryActivity";

	private TCQueryAction queryAction;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tcqueryform);

		queryAction = new TCQueryAction(this);

		final Spinner manufacturerField = (Spinner) findViewById(R.id.TCMotorSearchFormManufacturerField);
		final Spinner impulseField = (Spinner) findViewById(R.id.TCMotorSearchFormImpulseField);
		final Spinner diameterField = (Spinner) findViewById(R.id.TCMotorSearchFormDiameterField);
		final EditText commonNameField = (EditText) findViewById(R.id.TCMotorSearchFormCommonNameField);

		Button submitButton = (Button) findViewById(R.id.TCMotorSearchFromSubmitButton);
		submitButton.setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick( View v ) {
						Log.d(TAG,"submit button clicked");

						String commonName = commonNameField.getText().toString();

						SearchRequest r = new SearchRequest();
						if ( manufacturerField.getSelectedItemPosition() != 0) {
							String m = (String) manufacturerField.getSelectedItem();
							Log.d(TAG,"manufacturer = " + m);
							r.setManufacturer(m);
						}
						if ( impulseField.getSelectedItemPosition() != 0  ) {
							String impulse = (String) impulseField.getSelectedItem();
							Log.d(TAG,"impulse = " + impulse);
							r.setImpulse_class(impulse);
						}
						if ( diameterField.getSelectedItemPosition() != 0 ) {
							String diameter = (String)diameterField.getSelectedItem();
							Log.d(TAG,"diameter = " + diameter);
							r.setDiameter(diameter);
						}
						r.setCommon_name(commonName);

						queryAction.start(r);
					}
				}
				);
	}

	@Override
	public void onComplete() {
		finish();
	}

	/*
	 * TODO - ??
	@Override
	public Object onRetainNonConfigurationInstance() {
		return downloadThread;
	}
	 */
	@Override
	protected void onDestroy() {
		queryAction.dismiss();
		super.onDestroy();
	}

}
