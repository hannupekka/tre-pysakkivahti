package fi.hpheinajarvi.tamperepysakkivahti;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

/**
 * Marker item
 * @author Hannu-Pekka Heinajarvi <hannupekka@gmail.com>
 */
public class MapStopItem implements ClusterItem {
    private final LatLng mPosition;
    private final String mCode;
    private final String mName;

    public MapStopItem(double lat, double lng, String code, String name) {
        mPosition = new LatLng(lat, lng);
        mCode = code;
        mName = name;
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }
    
    public String getCode() {
    	return mCode;
    }
    
    public String getName() {
    	return mName;
    }
}