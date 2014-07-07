package fi.hpheinajarvi.tamperepysakkivahti;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;


/**
 * Shows list of saved bus stops.
 * @author Hannu-Pekka Heinajarvi <hannupekka@gmail.com>
 */
public class MainActivity extends Activity implements StopListFragment.SelectionListener {
	@SuppressWarnings("unused")
	private static final String TAG = "Tampere-Pysakkivahti";
	private Bundle mStopInfo;	// Info about selected stop
	private StopListFragment mStopListFragment;
	private StopFragment mStopFragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
						
		// Restore selected stop if such exists
		if (savedInstanceState != null && savedInstanceState.containsKey("stopInfo")) {
			mStopInfo = (Bundle) savedInstanceState.getBundle("stopInfo");
		}
		
		if (!isInTwoPaneMode()) {
			FragmentManager fm = getFragmentManager();
			
			// Try to get fragments
			mStopListFragment = (StopListFragment) fm.findFragmentByTag("stops");
			mStopFragment = (StopFragment) fm.findFragmentByTag("stop");
					
			// If either of fragments exist, create StopList fragment and add it to container
			if (mStopListFragment == null && mStopFragment == null) {
				mStopListFragment = new StopListFragment();
				fm.beginTransaction().add(R.id.container, mStopListFragment, "stops").commit();
			}
			// If StopFragment exists, replace StopList with it
			else if (mStopFragment != null) {
				fm.beginTransaction().replace(R.id.container, mStopFragment, "stop").commit();
			}
		} else {
			mStopFragment = (StopFragment) getFragmentManager().findFragmentById(R.id.frag_stop);
		}
	}
	
	private boolean isInTwoPaneMode() {
		return findViewById(R.id.container) == null;
	}
		
	@Override
	public void onItemSelected(String stopNumber, String stopName) {		
		// If StopFragment is not created yet, do it now
		if (mStopFragment == null) {
			mStopFragment = new StopFragment();
		}
		
		// Show departures
		Bundle stopInfo = new Bundle();
		stopInfo.putString("stopNumber", stopNumber);
		stopInfo.putString("stopName", stopName);
		mStopInfo = stopInfo;
		if (!isInTwoPaneMode()) {
			try {
				mStopFragment.setArguments(stopInfo);
			
				// Replace any other fragment with StopFragment
				FragmentManager fm = getFragmentManager();
				fm.beginTransaction().replace(R.id.container, mStopFragment, "stop").addToBackStack(null).commit();
				fm.executePendingTransactions();
			} catch (IllegalStateException ise) {
				ise.printStackTrace();
			}
		} else {
			mStopFragment.showDepartures(stopInfo);
		}
	}
	
	/**
	 * Class to hold info about bus stops
	 * @author Hannu-Pekka Heinajarvi <hannupekka@gmail.com>
	 */
	public class StopInfo {
		private String mStopNumber;	// Stop number, eg. 0045
		private String mStopName;	// Stop name, eg. Keskustori C
		
		/**
		 * Constructs StopInfo
		 * @param stopNumber
		 * @param stopName
		 */
		public StopInfo(String stopNumber, String stopName) {
			mStopNumber = stopNumber;
			mStopName = stopName;
		}

		/**
		 * @return
		 */
		public String getStopNumber() {
			return mStopNumber;
		}

		/**
		 * @param stopNumber
		 */
		public void setStopNumber(String stopNumber) {
			mStopNumber = stopNumber;
		}


		/**
		 * @return
		 */
		public String getStopName() {
			return mStopName;
		}

		/**
		 * @param stopName
		 */
		public void setStopName(String stopName) {
			mStopName = stopName;
		}
	}
}
