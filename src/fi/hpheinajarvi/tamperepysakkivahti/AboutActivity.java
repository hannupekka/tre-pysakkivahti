package fi.hpheinajarvi.tamperepysakkivahti;

import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.widget.TextView;

public class AboutActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		
		TextView version = (TextView) findViewById(R.id.app_version_code);
		try {
			String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			version.setText(versionName);			
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}
}
