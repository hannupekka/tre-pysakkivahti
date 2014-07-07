package fi.hpheinajarvi.tamperepysakkivahti;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import fi.hpheinajarvi.tamperepysakkivahti.db.DatabaseHandler;
import fi.hpheinajarvi.tamperepysakkivahti.model.Departure;
import fi.hpheinajarvi.tamperepysakkivahti.model.StopCache;


/**
 * Shows departures from a bus stop
 */
public class StopFragment extends Fragment {

	private DepartureAdapter mAdapter; // Adapter for ListView that shows departures from stop
	private PullToRefreshListView mDepartureList;
	private Bundle mStopInfo;
	private TextView mHeader;
	private LinearLayout mEmptyView;
	private Context mAppContext;
	private AsyncTask<String, Void, String> mDownloader;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAppContext = getActivity().getApplicationContext();

		// New adapter instance
		mAdapter = new DepartureAdapter(getActivity(), R.layout.departurelist_item);
		mEmptyView = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.empty, null);

		// Arguments should contain stop info
		Bundle arguments = getArguments();
		if (arguments != null) {
			mStopInfo = arguments;
		}

		// Retain this fragment and also tell that it has options
		setRetainInstance(true);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_stop, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Get reference to view
		mDepartureList = (PullToRefreshListView) getActivity().findViewById(R.id.stop_departures);

		// Set adapter
		mDepartureList.setAdapter(mAdapter);

		// Set refresh listener
		mDepartureList.setOnRefreshListener(new OnRefreshListener<ListView>() {
		    @Override
		    public void onRefresh(PullToRefreshBase<ListView> refreshView) {
		    	showDepartures(mStopInfo);
		    }
		});

		mHeader = (TextView) getActivity().findViewById(R.id.stop_header);

		// Set header text
		if (mStopInfo != null) {
			mHeader.setText(mStopInfo.getString("stopNumber") + " " + mStopInfo.getString("stopName"));
		} else {
			mHeader.setText(getResources().getString(R.string.main_header));
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		if (mStopInfo != null) {
			// Add empty departure to list, set it as being refreshed and remove empty departure
			// Dirty hack to set refreshing view visible on empty list
			mAdapter.add(new Departure());
			// This also eventually calls showDepartures
			mDepartureList.setRefreshing(true);
			mAdapter.clear();
		}

		// Enable ActionBar app icon to work as navigational back button
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_stop, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_refresh:
			// Refresh clicked, so refresh
			mDepartureList.setRefreshing();
			break;
		case android.R.id.home:
			// App icon - go back
	        getActivity().onBackPressed();
	        return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Updates departure list
	 * @param id
	 */
	public void showDepartures(Bundle stopInfo) {
		if (stopInfo != null) {
			mStopInfo = stopInfo;
			mHeader.setText(stopInfo.getString("stopNumber") + " " + stopInfo.getString("stopName"));
			mDownloader = new DepartureDownloader(StopFragment.this).execute(stopInfo.getString("stopNumber"));
		}
	}

	/**
	 * Loops trough list of departures and adds them to adapter
	 * @param data
	 */
	public void updateDepartures(List<Departure> data) {
		// Only loop if there is data
		if (data.size() > 0) {
			// Clear current adapter and add departures
			mAdapter.clear();
			for (int i = 0; i < data.size(); i++) {
				mAdapter.add(data.get(i));
			}
			// Notify adapter that data has changed
			mAdapter.notifyDataSetChanged();
		} else {
			// Set empty view and invalidate adapter
			mDepartureList.setEmptyView(mEmptyView);
			mAdapter.notifyDataSetInvalidated();
		}
	}

	/**
	 * Custom adapter for list view
	 * @author Hannu-Pekka Heinajarvi <hannupekka@gmail.com>
	 */
	private class DepartureAdapter extends ArrayAdapter<Departure> {
		/**
		 * ViewHolder
		 * @author Hannu-Pekka Heinajarvi <hannupekka@gmail.com>
		 */
		private class ViewHolder {
			// TextViews for departure info
			public final TextView tvLine;
			public final TextView tvDest;
			public final TextView tvTime;
			public final TextView tvMins;

			/**
			 * Constructs ViewHolder
			 * @param line
			 * @param dest
			 * @param time
			 * @param mins
			 */
			public ViewHolder(TextView line, TextView dest, TextView time, TextView mins) {
				tvLine = line;
				tvDest = dest;
				tvTime = time;
				tvMins = mins;
			}
		}

		/**
		 * Constructs adapter
		 * @param context
		 * @param resource
		 */
		public DepartureAdapter(Context context, int resource) {
			super(context, resource);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;

			// Init TextViews
			TextView tvLine;
			TextView tvDest;
			TextView tvTime;
			TextView tvMins;

			// Inflate view if it's null
			if (view == null) {
				// Inflate list item layout
				LayoutInflater li = (LayoutInflater) LayoutInflater.from(getContext());
				view = li.inflate(R.layout.departurelist_item, null);

				// Get references to TextViews
				tvLine = (TextView) view.findViewById(R.id.line);
				tvDest = (TextView) view.findViewById(R.id.destination);
				tvTime = (TextView) view.findViewById(R.id.time);
				tvMins = (TextView) view.findViewById(R.id.minutes);

				// Set new ViewHolder with references to TextViews as tag
				view.setTag(new ViewHolder(tvLine, tvDest, tvTime, tvMins));
			}
			// Use existing view
			else {
				ViewHolder holder = (ViewHolder) view.getTag();
				tvLine = holder.tvLine;
				tvDest = holder.tvDest;
				tvTime = holder.tvTime;
				tvMins = holder.tvMins;
			}

			// Get current departure
			Departure departure = getItem(position);

			// S)et texts
			tvLine.setText(departure.getLine());
			tvDest.setText(departure.getDest());
			tvTime.setText(departure.getTime());
			tvMins.setText(Integer.toString(departure.getMins()) + "min");

			view.setEnabled(false);

			return view;
		}
	}

	/**
	 * Downloads and parses stop departure data
	 * @author Hannu-Pekka Heinajarvi <hannupekka@gmail.com>
	 */
	private class DepartureDownloader extends AsyncTask<String, Void, String>{
		private static final String TAG = "Tampere-Pysakkivahti";
		private static final String URL = "http://api.publictransport.tampere.fi/prod/?user=<YOUR_USER>&pass=<YOUR_PASS>&request=stop&format=json&dep_limit=20&time_limit=360&code=";
		private String mStop;
		private boolean mCached = false;
		private StopFragment mParentFragment;

		public DepartureDownloader(StopFragment parent) {
			super();

			mParentFragment = (StopFragment) parent;
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
		}

		@Override
		protected String doInBackground(String... params) {
			return download(params);
		}

		private String download(String params[]) {
			String departures = new String();
			if (isCancelled()) {
				return departures;
			}
			mStop = params[0];
			DatabaseHandler db = new DatabaseHandler(mAppContext);
			String cacheData = db.getCache(mStop);
			db.close();
			// Check if cached data is not empty
			if (!cacheData.equalsIgnoreCase("")) {
				departures = cacheData;
				mCached = true;
			}
			// Download data
			else if (departures.isEmpty() || !mCached) {
				Log.i(TAG, params[0]+ " download");
				try {
					URL stopUrl = new URL(URL + mStop);

					InputStream inStream;
					BufferedReader reader;

					inStream = stopUrl.openStream();
					reader = new BufferedReader(new InputStreamReader(inStream));

					String line;
					StringBuffer buffer = new StringBuffer();

					while ((line = reader.readLine()) != null) {
						if (isCancelled()) {
							break;
						}
						buffer.append(line);
					}

					departures = buffer.toString();

					if (reader != null) {
						reader.close();
					}
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}

			return departures;
		}

		protected void onPostExecute(String data) {
			if (data != null) {
				// Yo dawg, do not re-cache cached data
				if (!mCached) {
					DatabaseHandler db = new DatabaseHandler(mAppContext);
					StopCache cache = new StopCache(mStop, data, System.currentTimeMillis() / 1000);
					db.addCache(cache);
					db.close();
				}

				List<Departure> dataJSON = parseData(data);
				Log.i(TAG, mStop + " departurelist update");
				mParentFragment.updateDepartures(dataJSON);
			}

			mDepartureList.onRefreshComplete();
			super.onPostExecute(data);
		}

		private List<Departure> parseData(String data) {
			mAdapter.clear();
			Log.i(TAG, mStop + " JSON parse");
			List<Departure> departures = new ArrayList<Departure>();
			try {
				JSONObject json = new JSONArray(data).getJSONObject(0);
				String departureData = json.getString("departures");
				if (!departureData.equalsIgnoreCase("")) {
					JSONArray jsonDepartures = new JSONArray(departureData);

					String rawTime;
					String rawDate;
					Calendar now = Calendar.getInstance();
					Calendar then;
					SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
					for (int i = 0; i < jsonDepartures.length(); i++) {
						JSONObject jsonDeparture = jsonDepartures.getJSONObject(i);


						// Departure time
						rawTime = jsonDeparture.getString("time");

						// Minutes until departure
						rawDate = jsonDeparture.getString("date");
						then = Calendar.getInstance();
						then.set(Calendar.YEAR, Integer.parseInt(rawDate.substring(0, 4)));
						then.set(Calendar.MONTH, Integer.parseInt(rawDate.substring(4, 6)) - 1);
						then.set(Calendar.DAY_OF_MONTH, Integer.parseInt(rawDate.substring(6, 8)));
						then.set(Calendar.HOUR_OF_DAY, Integer.parseInt(rawTime.substring(0, 2)));
						then.set(Calendar.MINUTE, Integer.parseInt(rawTime.substring(2, 4)));
						if (now.getTimeInMillis() > then.getTimeInMillis()) {
							continue;
						}

						Departure departure = new Departure(
								jsonDeparture.getString("code"),	// Line
								jsonDeparture.getString("name1").split(" - ")[1].trim(), // Destination
								dateFormat.format(then.getTime()),	// Time
								(int) (then.getTimeInMillis() - now.getTimeInMillis()) / (1000 * 60)); // Minutes
						departures.add(departure);
					}
				}
			} catch (JSONException jsone) {
				jsone.printStackTrace();
			}

			return departures;
		}
	}
}
