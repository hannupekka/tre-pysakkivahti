package fi.hpheinajarvi.tamperepysakkivahti.model;

public class StopCache {
	private int mId;
	private String mStop;
	private String mData;
	private long mTimestamp;
	
	public StopCache() {
		
	}
	
	public StopCache(int id, String stop, String data, long timestamp) {
		mId = id;
		mStop = stop;
		mData = data;
		mTimestamp = timestamp;
	}
	
	public StopCache(String stop, String data, long timestamp) {
		mStop = stop;
		mData = data;
		mTimestamp = timestamp;
	}
	
	public int getId() {
		return mId;
	}
	
	public void setId(int id) {
		mId = id;
	}
	
	public String getStop() {
		return mStop;
	}
	
	public void setStop(String stop) {
		mStop = stop;
	}
	
	public String getData() {
		return mData;
	}
	
	public void setData(String data) {
		mData = data;
	}
	
	public long getTimestamp() {
		return mTimestamp;
	}
	
	public void setTimestamp(long timestamp) {
		mTimestamp = timestamp;
	}
}
