package fi.hpheinajarvi.tamperepysakkivahti.model;

/**
 * Class to hold information about departure.
 */
public class Departure {
	private String mLine;	// Line, eg. 26
	private String mDest;	// Destination, eg. Linnainmaa
	private String mTime;	// Time the bus leaves from the stop, eg. 13:37
	private int mMins;		// Minutes until departure, eg. 14
	
	/**
	 * Constructs departure
	 * @param line
	 * @param dest
	 * @param time
	 * @param mins
	 */
	public Departure(String line, String dest, String time, int mins) {
		mLine = line;
		mDest = dest;
		mTime = time;
		mMins = mins;
	}
	
	public Departure() {
		mLine = "";
		mDest = "";
		mTime = "";
		mMins = 0;
	}
	
	/**
	 * @return
	 */
	public String getLine() {
		return mLine;
	}
	
	/**
	 * @param line
	 */
	public void setLine(String line) {
		mLine = line;
	}
	
	/**
	 * @return
	 */
	public String getDest() {
		return mDest;
	}
	
	/**
	 * @param dest
	 */
	public void setDest(String dest) {
		mDest = dest;
	}
	
	/**
	 * @return
	 */
	public String getTime() {
		return mTime;
	}
	
	/**
	 * @param time
	 */
	public void setTime(String time) {
		mTime = time;
	}
	
	/**
	 * @return
	 */
	public int getMins() {
		return mMins;
	}
	
	/**
	 * @param mins
	 */
	public void setMins(int mins) {
		mMins = mins;
	}
}
