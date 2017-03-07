package fr.pchab.androidrtc;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by damo on 7/19/16.
 *
 * This is the touch event in the DrawingView
 */
public class ThirdEyeDrawEvent {
    public static final String ACTION_DOWN = "ACTION_DOWN";
    public static final String ACTION_MOVE = "ACTION_MOVE";
    public static final String ACTION_UP = "ACTION_UP";

    public String drawMode = ACTION_DOWN;
    public float eventX = 0;
    public float eventY = 0;


    public interface ThirdEyeEventListener{
        public void onThirdEyeEvent(ThirdEyeDrawEvent event);
    }

    public String toString(){
        return "["+drawMode+","+eventX+","+eventY+"]";
    }

    public static ThirdEyeDrawEvent fromString(String string){
        ThirdEyeDrawEvent ret = new ThirdEyeDrawEvent();
        string = string
                .replace("[","")
                .replace("]","");
        String[] split = string.split(",");

        assert (split.length == 3);

        ret.drawMode = split[0];
        ret.eventX = Float.valueOf(split[1]);
        ret.eventY = Float.valueOf(split[2]);

        return ret;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("drawMode",drawMode);
        object.put("x",(double)eventX);
        object.put("y",(double)eventY);
        return object;
    }
    public static ThirdEyeDrawEvent fromJson(JSONObject json) throws JSONException {
        ThirdEyeDrawEvent ret = new ThirdEyeDrawEvent();
        ret.drawMode = json.getString("drawMode");
        ret.eventX = (float) json.getDouble("x");
        ret.eventY = (float) json.getDouble("y");
        return ret;
    }


}
