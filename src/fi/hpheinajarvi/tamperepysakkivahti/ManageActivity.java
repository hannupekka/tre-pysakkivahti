package fi.hpheinajarvi.tamperepysakkivahti;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.DragSortListView.DropListener;

import fi.hpheinajarvi.tamperepysakkivahti.db.DatabaseHandler;
import fi.hpheinajarvi.tamperepysakkivahti.model.Stop;

/**
 * Allows user to manage saved stops.
 * @author Hannu-Pekka Heinajarvi <hannupekka@gmail.com>
 */
public class ManageActivity extends Activity {
	private static final String TAG = "Tampere-Pysakkivahti";	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_manage);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		getActionBar().setHomeButtonEnabled(true);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    // Respond to the action bar's Up/Home button
	    case android.R.id.home:
	        NavUtils.navigateUpFromSameTask(this);
	        return true;
	    }
	    return super.onOptionsItemSelected(item);
	}
	
	public void showTabs(View v) {
		startActivity(new Intent(this, AddStopTabsActivity.class));
	}

	/**
	 * Holds the actual UI for stop management.
	 * @author Hannu-Pekka Heinajarvi <hannupekka@gmail.com>
	 */
	public static class PlaceholderFragment extends Fragment {
		private Context mAppContext;
		private ManageStopAdapter mAdapter;
		private DragSortListView mStops;
		private TextView mEmptyView;
		private Button mEmptyViewButton;
		private Stop mStop;
		
		public PlaceholderFragment() {
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			// Get context
			mAppContext = getActivity().getApplicationContext();
			
			// Create adapter and set currently selected item if there is one
			mAdapter = new ManageActivity().new ManageStopAdapter(mAppContext, R.layout.manage_stoplist_item);
			
			// This fragment has options menu
			setHasOptionsMenu(true);
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_manage,
					container, false);
			return rootView;
		}
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			
			// Get empty view and button
			mEmptyView = (TextView) getActivity().findViewById(R.id.empty_manage_stoplist);
			mEmptyViewButton = (Button) getActivity().findViewById(R.id.manage_empty_button);
			
			// Get list view and add adapter
			mStops = (DragSortListView) getActivity().findViewById(R.id.manage_stops_list);
			mStops.setAdapter(mAdapter);
		}
		
		@Override
		public void onResume() {
			super.onResume();
			
			// Get new DB handler, clear adapter and add all stops to it and close DB connection
			DatabaseHandler db = new DatabaseHandler(mAppContext);
			mAdapter.clear();
			mAdapter.addAll(db.getStops());
			db.close();

			// Dirty hack to show TextView if adapter is empty
			if (mAdapter.isEmpty()) {
				mEmptyView.setVisibility(View.VISIBLE);
				mAdapter.notifyDataSetInvalidated();
			} else {
				mEmptyView.setVisibility(View.GONE);
				mEmptyViewButton.setVisibility(View.GONE);
				mAdapter.notifyDataSetChanged();
			}
			
			// Register for context menu
			registerForContextMenu(mStops);
		}
					
		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			inflater.inflate(R.menu.manage, menu);
			super.onCreateOptionsMenu(menu, inflater);
		}
		
		@Override
		public void onCreateContextMenu(ContextMenu menu, View v,
		                                ContextMenuInfo menuInfo) {
		    super.onCreateContextMenu(menu, v, menuInfo);
		    MenuInflater inflater = getActivity().getMenuInflater();
		    inflater.inflate(R.menu.manage_actionmode, menu);
		}		
		
		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			// Only one menu item, no need to loop anything
			startActivity(new Intent(getActivity(), AddStopTabsActivity.class));
			return super.onOptionsItemSelected(item);
		}
				
		@Override
		public boolean onContextItemSelected(MenuItem item) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	    	mStop = (Stop) mStops.getItemAtPosition(info.position);
	        switch (item.getItemId()) {
	            case R.id.action_edit:
	            	showEditDialog();
	                return true;
	            case R.id.action_delete:
	            	deleteStop();
	            	return true;
	            default:
	                return false;
	        }
		}
		
		public void deleteStop() {
        	// Get DB handler, remove stop from DB and close connection
        	DatabaseHandler db = new DatabaseHandler(mAppContext);
        	db.deleteStop(mStop.getStop());
        	db.close();
        	
        	// Remove stop from adapter
        	mAdapter.remove(mStop);
        	mAdapter.notifyDataSetChanged();
        	
        	if (mAdapter.isEmpty()) {
        		mEmptyViewButton.setVisibility(View.VISIBLE);
        	}
        	
        	// Notify user
        	Toast.makeText(mAppContext, getResources().getString(R.string.stop_deleted), Toast.LENGTH_SHORT).show();
        	
        	// Get rid of stop object
        	mStop = null;			
		}
		
		public void showEditDialog() {
			Resources r = getActivity().getResources();
			final EditText edit = new EditText(getActivity());
			edit.setText(mStop.getAlias());
			new AlertDialog.Builder(getActivity())
				.setTitle(r.getString(R.string.action_edit_rename))
				.setMessage(r.getString(R.string.action_edit_rename_msg))
				.setView(edit)
				.setPositiveButton(r.getString(R.string.ok), new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String alias = edit.getText().toString();
						DatabaseHandler db = new DatabaseHandler(getActivity());
						db.renameStop(mStop, alias);
			        	
						mAdapter.clear();
						mAdapter.addAll(db.getStops());
						db.close();
						
			        	// Notify user
			        	Toast.makeText(mAppContext, getResources().getString(R.string.stop_renamed), Toast.LENGTH_SHORT).show();
			        	
			        	// Get rid of stop object
			        	mStop = null;			        	
					}
				})
				.setNegativeButton(r.getString(R.string.cancel), new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {						
					}
				})
				.create()
				.show();
		}
	}
	
	/**
	 * Adapter for stop list.
	 * @author Hannu-Pekka Heinajarvi <hannupekka@gmail.com>
	 */
	private class ManageStopAdapter extends ArrayAdapter<Stop> implements DropListener{
		private static final String TAG = "Tampere-Pysakkivahti";
		private Context mContext;

		/**
		 * ViewHolder
		 * @author Hannu-Pekka Heinajarvi <hannupekka@gmail.com>
		 */
		private class ViewHolder {
			// TextViews for stop info
			public final TextView tvStop;
			public final TextView tvName;

			/**
			 * Constructs ViewHolder
			 * @param line
			 * @param dest
			 */
			public ViewHolder(TextView stop, TextView name) {
				tvStop = stop;
				tvName = name;
			}
		}
				
		/**
		 * Constructs adapter
		 * @param context
		 * @param resource
		 */
		public ManageStopAdapter(Context context, int resource) {
			super(context, resource);
			mContext = context;
		}
						
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			
			// Init TextViews
			TextView tvStop;
			TextView tvName;
			
			// Inflate view if it's null
			if (view == null) {
				// Inflate list item layout
				LayoutInflater li = (LayoutInflater) LayoutInflater.from(getContext());
				view = li.inflate(R.layout.manage_stoplist_item, null);
				
				// Get references to TextViews
				tvStop = (TextView) view.findViewById(R.id.manage_stop_number);
				tvName = (TextView) view.findViewById(R.id.manage_stop_name);

				// Set new ViewHolder with references to TextViews as tag
				view.setTag(new ViewHolder(tvStop, tvName));
			}
			// Use existing view
			else {
				ViewHolder holder = (ViewHolder) view.getTag();
				tvStop = holder.tvStop;
				tvName = holder.tvName;
			}
			
			// Get current departure
			Stop stop = getItem(position);
			
			// Set texts
			tvStop.setText(stop.getStop());
			tvName.setText(stop.getAlias());
			
			return view;
		}

		@Override
		public void drop(int from, int to) {
			Stop stop = getItem(from);
			remove(stop);
			insert(stop, to);
			notifyDataSetChanged();
			DatabaseHandler db = new DatabaseHandler(mContext);
			int moveFrom, moveTo = 0;
			if (from < to) {
				moveFrom = from;
				moveTo = to;
			} else {
				moveFrom = to;
				moveTo = from;
			}

			for (int idx = moveFrom; idx <= moveTo; idx++) {
				Stop moveStop = getItem(idx);
				db.updateWeight(moveStop.getId(), idx);
			}				
			db = null;
		}
	}
}
