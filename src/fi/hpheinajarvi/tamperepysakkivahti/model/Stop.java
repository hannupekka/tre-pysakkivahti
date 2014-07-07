package fi.hpheinajarvi.tamperepysakkivahti.model;

public class Stop {
	private int mId;
	private String mStop;
	private String mName;
	private String mAlias;
	private int mWeight;
	
	public Stop() {
		
	}
	
	public Stop(int id, String stop, String name, String alias, int weight) {
		mId = id;
		mStop = stop;
		mName = name;
		mAlias = alias;
		mWeight = weight;
	}
	
	public Stop(String stop, String name, String alias, int weight) {
		mStop = stop;
		mName = name;
		mAlias = alias;
		mWeight = weight;
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
	
	public String getName() {
		return mName;
	}
	
	public void setName(String name) {
		mName = name;
	}
	
	public String getAlias() {
		return mAlias;
	}
	
	public void setAlias(String alias) {
		mAlias = alias;
	}
	
	public int getWeight() {
		return mWeight;
	}
	
	public void setWeight(int weight) {
		mWeight = weight;
	}
}
