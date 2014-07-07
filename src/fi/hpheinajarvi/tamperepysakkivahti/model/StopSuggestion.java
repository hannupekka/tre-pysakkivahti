package fi.hpheinajarvi.tamperepysakkivahti.model;

public class StopSuggestion {
	private String mCode;
	private String mName;
	
	public StopSuggestion() {
		
	}
	
	public StopSuggestion(String code, String name) {
		mCode = code;
		mName = name;
	}
	
	public String getCode() {
		return mCode;
	}
	
	public void setCode(String code) {
		mCode = code;
	}
	
	public String getName() {
		return mName;
	}
	
	public void setName(String name) {
		mName = name;
	}
	
	@Override
	public String toString() {
		return mCode + " " + mName;
	}
}
