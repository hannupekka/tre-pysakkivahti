package fi.hpheinajarvi.tamperepysakkivahti;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.MarkerManager.Collection;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.ClusterManager.OnClusterClickListener;
import com.google.maps.android.clustering.ClusterManager.OnClusterItemInfoWindowClickListener;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import fi.hpheinajarvi.tamperepysakkivahti.db.DatabaseHandler;
import fi.hpheinajarvi.tamperepysakkivahti.model.StopSuggestion;


/**
 * Shows bus stops on a map
 * @author Hannu-Pekka Heinajarvi <hannupekka@gmail.com>
 */
public class AddStopFromMapFragment extends Fragment {
	private static final String TAG = "Tampere-Pysakkivahti";

	private static final int DEFAULT_ZOOM = 14;	// Default zoom level for map
	private static final LatLng TAMPERE = new LatLng(61.500138, 23.766495);	// Static coordinates for Tampere
	private static final String CACHE_FILE = "tampere-pysakkivahti-stoplist";	// File to save stop data to

	private AsyncTask<Boolean, Void, String> mDownloader;	// Downloads stop data
	private GoogleMap mMap;	// Map
	private ClusterManager<MapStopItem> mClusterManager;	// Combines markers that are close to each other
	private StopRenderer mStopRenderer;
	private boolean mCached = false;
	private ProgressDialog mLoading;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null && savedInstanceState.containsKey("CACHE")) {
			mCached = savedInstanceState.getBoolean("CACHE");
		}
		setHasOptionsMenu(true);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_add_stop_from_map, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.i(TAG, "onActivityCreated");

		boolean resume = mMap != null;

		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
		mClusterManager = new ClusterManager<MapStopItem>(getActivity(), mMap);
		mStopRenderer = new StopRenderer();
		mClusterManager.setRenderer(mStopRenderer);

		// Cluster click listener
		mClusterManager.setOnClusterClickListener(new OnClusterClickListener<MapStopItem>() {
			@Override
			public boolean onClusterClick(Cluster<MapStopItem> cluster) {
				// Zoom into cluster when clicked
				mMap.moveCamera(CameraUpdateFactory.newLatLng(cluster.getPosition()));
				mMap.moveCamera(CameraUpdateFactory.zoomIn());
				return true;
			}
		});

		// Cluster item click listener
		mClusterManager.setOnClusterItemInfoWindowClickListener(new OnClusterItemInfoWindowClickListener<MapStopItem>() {
			@Override
			public void onClusterItemInfoWindowClick(final MapStopItem item) {
				// Get clicked stop
				final StopSuggestion stop = new StopSuggestion(item.getCode(), item.getName());

				// Connect to DB
				final DatabaseHandler db = new DatabaseHandler(getActivity());

				// Initialize AlertDialog
				AlertDialog.Builder confirm = new AlertDialog.Builder(getActivity());

				// Set dialog title
				if (!db.stopExists(stop)) {
					confirm.setTitle(getResources().getText(R.string.add_stop));
				} else {
					confirm.setTitle(getResources().getText(R.string.remove_stop));
				}

				// Set dialog message
				confirm.setMessage(item.getCode() + " " + item.getName());

				// Set dialog "OK"-button
				confirm.setPositiveButton(R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// Add stop to DB only if it does not already exist, otherwise delete it.
						if (!db.stopExists(stop)) {
							db.addStop(stop);
							Toast.makeText(getActivity(), R.string.stop_added, Toast.LENGTH_SHORT).show();
						} else {
							db.deleteStop(stop.getCode());
							Toast.makeText(getActivity(), R.string.stop_deleted, Toast.LENGTH_SHORT).show();
						}
						// Disconnect from DB
						db.close();
					}
				});

				// Dialog 'Cancel'-button
				confirm.setNegativeButton(R.string.cancel, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// Does nothing.
					}
				});

				// Show dialog
				confirm.show();
			}
		});

		// Set listeners for map
		mMap.setOnCameraChangeListener(mClusterManager);
		mMap.setOnMarkerClickListener(mClusterManager);
		mMap.setOnInfoWindowClickListener(mClusterManager);

		// Enable user location
		mMap.setMyLocationEnabled(true);

		if (resume) {
			Thread updater = new Thread() {
				@Override
			    public void run() {
					parseData(readCache());
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mClusterManager.cluster();
						}
					});
			    }
			};
			updater.start();
		} else {
			mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(TAMPERE, DEFAULT_ZOOM));
			mDownloader = new StopDownloader(AddStopFromMapFragment.this).execute(false);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_map_stop, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_refresh_stops:
			// Force download stop data
			mDownloader = new StopDownloader(AddStopFromMapFragment.this).execute(true);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Write stop data JSON to file
	 */
	private void writeCache(String data) {
		FileOutputStream outputStream;

		try {
			Log.d(TAG, "Write cache to " + CACHE_FILE);
			outputStream = getActivity().openFileOutput(CACHE_FILE, Context.MODE_PRIVATE);
			outputStream.write(data.getBytes());
			outputStream.close();
		} catch (Exception e) {
		  e.printStackTrace();
		}
	}

	/**
	 * Read stop data JSON from file
	 * @return
	 */
	private String readCache() {
		String data = "";
		FileInputStream iStream;
		BufferedInputStream bStream;
		StringBuffer sBuffer = new StringBuffer();

		try {
			Log.d(TAG, "Open cache file " + CACHE_FILE);
			iStream = getActivity().openFileInput(CACHE_FILE);
			bStream = new BufferedInputStream(iStream);

			byte[] buffer = new byte[1024];
			int bRead = 0;

			// Read file
			while ((bRead = bStream.read(buffer)) != -1) {
				sBuffer.append(new String(buffer, 0, bRead));
			}

			if (iStream != null) {
				iStream.close();
			}

			if (bStream != null) {
				bStream.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return "";
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}

		// Combine buffered data
		data = sBuffer.toString();
		return data;
	}

	/**
	 * Parses JSON and adds stops to cluster
	 * @param data
	 */
	private void parseData(String data) {
		Log.i(TAG, "Parsing stops");
		try {
			JSONArray json = new JSONArray(data);
			Log.i(TAG, json.length() + " stops found");

			for (int i = 0; i < json.length(); i++) {
				JSONObject stopData = json.getJSONObject(i);
				Double lat = Double.parseDouble(stopData.getString("lat"));
				Double lng = Double.parseDouble(stopData.getString("lng"));
				String code = stopData.getString("code");
				String name = stopData.getString("name");
				MapStopItem stop = new MapStopItem(lat, lng, code, name);
				mClusterManager.addItem(stop);
			}
		} catch (JSONException jsone) {
			jsone.printStackTrace();
		}
	}

	/**
	 * Downloads and parses stop data
	 * @author Hannu-Pekka Heinajarvi <hannupekka@gmail.com>
	 */
	private class StopDownloader extends AsyncTask<Boolean, Void, String>{
		private static final String TAG = "Tampere-Pysakkivahti";
		private static final String URL = "YOUR_API_ENDPOINT"; // Should print contents of stops.txt from http://data.itsfactory.fi/files/tamperefeed_xxxxx.zip in JSON format
		private AddStopFromMapFragment mParentFragment;

		public StopDownloader(AddStopFromMapFragment parent) {
			super();

			mParentFragment = (AddStopFromMapFragment) parent;
		}

		protected void onCancelled() {
			super.onCancelled();
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// Initialize and show ProgressDialog
			mLoading = new ProgressDialog(mParentFragment.getActivity());
			mLoading.setTitle(mParentFragment.getResources().getText(R.string.please_wait));
			mLoading.setMessage(mParentFragment.getResources().getText(R.string.loading));
			mLoading.setCancelable(true);
			mLoading.show();
		}

		@Override
		protected String doInBackground(Boolean... params) {
			return download(params);
		}

		private String download(Boolean params[]) {
			String data = new String();
			// Force downloading data
			if (params[0] == true) {
				try {
					// Create URL
					URL dataUrl = new URL(URL);

					InputStream inStream;
					BufferedReader reader;

					inStream = dataUrl.openStream();
					reader = new BufferedReader(new InputStreamReader(inStream));

					String stop;
					StringBuffer buffer = new StringBuffer();

					// Read data
					while ((stop = reader.readLine()) != null) {
						if (isCancelled()) {
							break;
						}
						buffer.append(stop);
					}

					// Combine data
					data = buffer.toString();

					if (reader != null) {
						reader.close();
					}
				} catch (UnknownHostException uhe) {
					// Show error
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// Initialize AlertDialog
							AlertDialog.Builder error = new AlertDialog.Builder(getActivity());

							// Set dialog title and message
							error.setTitle(getResources().getText(R.string.error));
							error.setMessage(getResources().getText(R.string.error_message));

							// Set dialog "OK"-button
							error.setPositiveButton(R.string.ok, new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
								}
							});

							error.show();
						}
					});
					uhe.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				// Save data to cache file
				if (!data.equalsIgnoreCase("")) {
					writeCache(data);
				}
			} else {
				// Read cache file
				data = readCache();
				// Cache empty, force download stop data
				if (data.equalsIgnoreCase("")) {
					data = download(new Boolean[]{true});
				}
			}

			return data;
		}

		protected void onPostExecute(String data) {
			super.onPostExecute(data);

			if (data != null) {
				parseData(data);
			}

			// Hide ProgressDialog
			mClusterManager.cluster();
			try {
				mLoading.dismiss();
			} catch (IllegalArgumentException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	/**
	 * Implementation of ClusterRenderer
	 * @author Hannu-Pekka Heinajarvi <hannupekka@gmail.com>
	 */
    private class StopRenderer extends DefaultClusterRenderer<MapStopItem> {
        public StopRenderer() {
            super(getActivity(), mMap, mClusterManager);
        }

        @Override
        protected void onBeforeClusterItemRendered(MapStopItem stop, MarkerOptions markerOptions) {
        	if (!isAdded()) {
        		return;
        	}

        	// Get marker size
        	int size = getResources().getDimensionPixelSize(R.dimen.map_marker_size);

        	// Create correct size bitmap
        	Bitmap marker = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        	// Create canvas out of marker
        	Canvas canvas = new Canvas(marker);

        	// Get drawable, set bounds and draw
        	Drawable shape = getResources().getDrawable(R.drawable.bus_stop);
        	shape.setBounds(0, 0, marker.getWidth(), marker.getHeight());
        	shape.draw(canvas);

        	// Create icon
        	BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(marker);

        	// Set icon and marker texts
            markerOptions.icon(icon).title(stop.getCode() + " " + stop.getName()).snippet(getResources().getString(R.string.stop_add_delete));
        }
    }
}
