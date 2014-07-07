package fi.hpheinajarvi.tamperepysakkivahti;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import fi.hpheinajarvi.tamperepysakkivahti.model.Stop;

/**
 * Custom adapter for stop list.
 */
public class StopAdapter extends ArrayAdapter<Stop> {
	private boolean mShowNumber;

	/**
	 * ViewHolder
	 */
	private class ViewHolder {
		// TextViews for departure info
		public final TextView tvStop;
		public final TextView tvName;

		/**
		 * Constructs ViewHolder
		 * @param line
		 * @param dest
		 * @param time
		 * @param mins
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
	public StopAdapter(Context context, int resource) {
		super(context, resource);
		// Get preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		mShowNumber = prefs.getBoolean("pref_show_line_number", true);
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
			view = li.inflate(R.layout.stoplist_item, null);

			// Get references to TextViews
			tvStop = (TextView) view.findViewById(R.id.stop_number);
			tvName = (TextView) view.findViewById(R.id.stop_name);

			// Hide number if user does not want it
			if (!mShowNumber) {
				tvStop.setVisibility(View.GONE);
			} else {
				tvStop.setVisibility(View.VISIBLE);
			}

			// Set new ViewHolder with references to TextViews as tag
			view.setTag(new ViewHolder(tvStop, tvName));
		}
		// Use existing view
		else {
			ViewHolder holder = (ViewHolder) view.getTag();
			tvStop = holder.tvStop;
			tvName = holder.tvName;

			// Hide number if user does not want it
			if (!mShowNumber) {
				tvStop.setVisibility(View.GONE);
			} else {
				tvStop.setVisibility(View.VISIBLE);
			}
		}

		// Get current departure
		Stop stop = getItem(position);

		// Set texts
		// Show line number only if user wants
		if (mShowNumber) {
			tvStop.setText(stop.getStop());
		}

		tvName.setText(stop.getAlias());

		return view;
	}
}	