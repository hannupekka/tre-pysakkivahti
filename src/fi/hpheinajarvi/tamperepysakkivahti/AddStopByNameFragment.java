package fi.hpheinajarvi.tamperepysakkivahti;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import fi.hpheinajarvi.tamperepysakkivahti.db.DatabaseHandler;
import fi.hpheinajarvi.tamperepysakkivahti.model.StopSuggestion;

/**
 * Class used to display activity where user can add new stops.
 * @author Hannu-Pekka Heinajarvi <hannupekka@gmail.com>
 */
public class AddStopByNameFragment extends Fragment {
	private AutoCompleteTextView mNewStop;	// AutoCompleteTextView user uses to search for stops
	private ProgressBar mLoading;
	private SuggestionAdapter mAdapter;		// Adapter for stop suggestions
	private StopSuggestion mSuggestedStop;	// Stop user selects amongst suggestions
	private Button mAddStop;
	private Button mClearStop;
	private Activity mHost;					// Hosting activity
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHost = getActivity();
	}
	
	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.activity_add_stop, container, false);
	}
	
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		// Create adapter for suggestions
		mAdapter = new SuggestionAdapter(mHost, R.layout.stoplist_item);

		// Get reference to search box
		mNewStop = (AutoCompleteTextView) mHost.findViewById(R.id.new_stop);
		
		// Get reference to progressbar
		mLoading = (ProgressBar) mHost.findViewById(R.id.new_stop_loading);
		mLoading.setVisibility(View.GONE);
		
		// Set minimum char count for search and set adapter
		mNewStop.setThreshold(1);
		mNewStop.setAdapter(mAdapter);
		
		// Set onClick listener for stop suggestions
		mNewStop.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// Disable search box so stop name can't be modified before saving
				mNewStop.setEnabled(false);
				
				// Enable buttons
				mAddStop.setEnabled(true);
				mClearStop.setEnabled(true);
				
				// Get suggestion at click position
				mSuggestedStop = mAdapter.getItem(position);				
			}
		});
		
		// Add text change listener
		mNewStop.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() == 0) {
					mClearStop.setEnabled(false); 
				} else {
					mClearStop.setEnabled(true);
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				
			}
		});
				
		// Get references to buttons and disable them
		mAddStop = (Button) mHost.findViewById(R.id.save_new_stop);
		mAddStop.setEnabled(false);
		mClearStop = (Button) mHost.findViewById(R.id.clear_new_stop);
		mClearStop.setEnabled(false);
		
		// Add onClick listener for button
		mAddStop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Don't do anything if no stop is selected
				if (mSuggestedStop != null) {
					// Connect to DB
					DatabaseHandler db = new DatabaseHandler(mHost.getApplicationContext());
					
					// Add stop to DB only if it does not already exist
					if (!db.stopExists(mSuggestedStop)) {
						db.addStop(mSuggestedStop);
						Toast.makeText(mHost.getApplicationContext(), R.string.stop_added, Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(mHost.getApplicationContext(), R.string.stop_exists, Toast.LENGTH_SHORT).show();
					}
					// Disconnect from DB
					db.close();
					
					// Disable buttons
					mAddStop.setEnabled(false);
					mClearStop.setEnabled(false);
					
					// Clear search box and enable it and clear current stop suggestion
					mNewStop.setText("");
					mNewStop.setEnabled(true);
					mSuggestedStop = null;
				}
			}
		});
		
		// Set onClick listener for reset button
		mClearStop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Disable buttons
				mAddStop.setEnabled(false);
				mClearStop.setEnabled(false);
				
				// Clear search box and enable it and clear current stop suggestion
				mNewStop.setText("");
				mNewStop.setEnabled(true);
				mSuggestedStop = null;
			}
		});
		
		// Restore selected suggestion
		if (savedInstanceState != null && savedInstanceState.containsKey("stopCode")) {
			mSuggestedStop = new StopSuggestion(savedInstanceState.getString("stopCode"), savedInstanceState.getString("stopName"));
			mNewStop.setEnabled(false);
			mNewStop.setText(mSuggestedStop.toString());
			
			// Enable buttons
			mAddStop.setEnabled(true);
			mClearStop.setEnabled(true);			
		}		
	}
		
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// Save selected suggestion
		if (mSuggestedStop != null) {
			outState.putString("stopCode", mSuggestedStop.getCode());
			outState.putString("stopName", mSuggestedStop.getName());
		}
	}
	
	/**
	 * Class used to download stop suggestions based on users search terms.
	 * @author Hannu-Pekka Heinajarvi <hannupekka@gmail.com>
	 */
	public class SuggestionAdapter extends ArrayAdapter<StopSuggestion> implements Filterable {
		// URL for stop suggestions
		private static final String URL = "http://lissu.tampere.fi/ajax_servers/geocoder.php?term=";
		
		/**
		 * ViewHolder for optimizing ListView rendering
		 * @author Hannu-Pekka Heinajarvi <hannupekka@gmail.com>
		 */
		private class ViewHolder {
			// TextViews for suggestion info
			public final TextView tvStop;
			public final TextView tvName;
			
			/**
			 * Constructs ViewHolder
			 * @param stop
			 * @param name
			 */
			public ViewHolder(TextView stop, TextView name) {
				tvStop = stop;
				tvName = name;
			}
		}
		
		/**
		 * Downloads and parses stops based on users search terms.
		 * @author Hannu-Pekka Heinajarvi <hannupekka@gmail.com>
		 */
		final class SuggestionFilter extends Filter {
	        @Override
	        protected FilterResults performFiltering(
	                CharSequence constraint) {
	        	// For results
	            FilterResults result = new FilterResults();
	            
	            // Don't do anything if there is nothing to do with
	            if (constraint != null) {
					try {
						// Show progress bar
						mHost.runOnUiThread(new Runnable() {
							@Override
						    public void run() {
								mLoading.setVisibility(View.VISIBLE);
						    }
						});						
						// Append search term to base URL
						URL suggestionUrl = new URL(URL + URLEncoder.encode(constraint.toString(), "UTF-8"));

						// Open InputStream from URL and prepare reader
						InputStream inStream = suggestionUrl.openStream(); 
						BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
						
						// Read buffered stream into string buffer line by line
						String line;
						StringBuffer buffer = new StringBuffer();
						
						while ((line = reader.readLine()) != null) {
							buffer.append(line);
						}
						
						// List for suggestions
						List<StopSuggestion> suggestions = new ArrayList<StopSuggestion>();
						try {
							// Get JSONArray from JSON data
							JSONArray json = new JSONArray(buffer.toString());
							// Loop through data
							for (int idx = 0; idx < json.length(); idx++) {
								JSONObject stopData = json.getJSONObject(idx);
								String stopId = stopData.getString("id");
								// Stops are not zero-padded so pad them.
								if (stopId.length() == 1) {
									stopId = "000" + stopId;
								} else if (stopId.length() == 2) {
									stopId = "00" + stopId;
								} else if (stopId.length() == 3) {
									stopId = "0" + stopId;
								}
								// Add suggestion
								suggestions.add(new StopSuggestion(stopId, stopData.getString("value")));
							}
						} catch (JSONException jsone) {
							jsone.printStackTrace();
						}
						
						// Format results
						result.values = suggestions;
						result.count = suggestions.size();
						
						// Close reader
						if (reader != null) {
							reader.close();
						}			
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}
	            }
		
	            // Return results
	            return result;
	        }

	        @SuppressWarnings("unchecked")
			@Override
	        protected void publishResults(CharSequence constraint, FilterResults results) {
	        	// Clear adapter from old results
	            clear();
	            
	            // If there are results, add them to adapter 
	            if (results.count > 0) {
	                for (StopSuggestion suggestion : (ArrayList<StopSuggestion>) results.values) {
	                    add(suggestion);
	                }
	            }
	            
	            // Hide progress bar
				mHost.runOnUiThread(new Runnable() {
					@Override
				    public void run() {
						mLoading.setVisibility(View.GONE);
				    }
				});
				
				// Notify adapter that data has changed
	            notifyDataSetChanged();
	        }
	    }
		
		/**
		 * Returns suggestion filter
		 */
		public Filter getFilter() {
	        return new SuggestionFilter();
	    }		
		
		/**
		 * Constructs adapter
		 * @param context
		 * @param resource
		 */
		public SuggestionAdapter(Context context, int resource) {
			super(context, resource);
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
				view = li.inflate(R.layout.suggestion_item, null);
				
				// Get references to TextViews
				tvStop = (TextView) view.findViewById(R.id.suggestion_stop_number);
				tvName = (TextView) view.findViewById(R.id.suggestion_stop_name);

				
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
			StopSuggestion suggestion = getItem(position);
			
			// Set texts
			tvStop.setText(suggestion.getCode());
			tvName.setText(suggestion.getName());
			
			return view;
		}	
	}	
}
