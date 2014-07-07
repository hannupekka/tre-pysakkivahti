package fi.hpheinajarvi.tamperepysakkivahti;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import fi.hpheinajarvi.tamperepysakkivahti.db.DatabaseHandler;

/**
 * Shows users stops.
 * @author Hannu-Pekka Heinajarvi <hannupekka@gmail.com>
 */
public class StopListFragment extends Fragment {
	private static final String TAG = "Tampere-Pysakkivahti";
	private DatabaseHandler mDb;
	private Context mAppContext;
	private SelectionListener mListItemCallback;
	private StopAdapter mAdapter;
	private ListView mStops;
	private TextView mEmptyView;
	private Button mEmptyViewButton;
	
	/**
	 * 
	 * @author hp
	 *
	 */
	public interface SelectionListener {
		/**
		 * 
		 * @param position
		 * @param id
		 */
		public void onItemSelected(String stopNumber, String stopName);
	}
	
	/**
	 * (non-Javadoc)
	 * @see android.app.Fragment#onAttach(android.app.Activity)
	 */
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		try {
			mListItemCallback = (SelectionListener) activity;
		} catch (ClassCastException ce) {
			throw new ClassCastException(activity.toString()
					+ " must implement SelectionListener");
		}
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Get context and create adapter.
		mAppContext = getActivity().getApplicationContext();
		mAdapter = new StopAdapter(getActivity(), R.layout.stoplist_item);
		
		// This fragment has options menu
		setHasOptionsMenu(true);
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {		
		return inflater.inflate(R.layout.fragment_stoplist, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Get empty view and button
		mEmptyView = (TextView) getActivity().findViewById(R.id.empty_stoplist);
		mEmptyViewButton = (Button) getActivity().findViewById(R.id.stoplist_empty_button);
		mEmptyViewButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), ManageActivity.class));
			}
		});
		
		// Get listview and set adapter
		mStops = (ListView) getActivity().findViewById(R.id.stoplist);
		mStops.setAdapter(mAdapter);
		
		// Set list item click listener
		mStops.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// Get relative layout from list item
				RelativeLayout item = (RelativeLayout) arg1;
				
				// Get stop number and name
				TextView tvStopNumber = (TextView) item.findViewById(R.id.stop_number);
				TextView tvStopName = (TextView) item.findViewById(R.id.stop_name);
				
				// Call callback 
				mListItemCallback.onItemSelected(tvStopNumber.getText().toString(), tvStopName.getText().toString());
			}
		});
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// Clear adapter and add all stops to it
		mDb = new DatabaseHandler(mAppContext);
		mAdapter.clear();
		mAdapter.addAll(mDb.getStops());
		mDb.close();
		
		// Dirty hack to show or hide empty view.
		// Should use setEmptyView rather than this.
		if (mAdapter.isEmpty()) {
			mEmptyView.setVisibility(View.VISIBLE);
			mEmptyViewButton.setVisibility(View.VISIBLE);
			mAdapter.notifyDataSetInvalidated();
		} else {
			mEmptyView.setVisibility(View.GONE);
			mEmptyViewButton.setVisibility(View.GONE);
			mAdapter.notifyDataSetChanged();
		}
				
		// Enable ActionBar app icon to work as navigational back button
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_stoplist, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_manage:
			startActivity(new Intent(getActivity(), ManageActivity.class));
			break;
		case R.id.action_settings:
			startActivity(new Intent(getActivity(), SettingsActivity.class));
			break;
		case R.id.action_about:
			startActivity(new Intent(getActivity(), AboutActivity.class));
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}
