package info.fshi.oppnetdemo1.utils;

import java.util.HashMap;

public class Devices {

	public static int DEVICE_TYPE_SENSOR = 1;
	public static int DEVICE_TYPE_RELAY = 2;
	public static int DEVICE_TYPE_SINK = 3;
	
	public static final HashMap<String, Integer> PARTICIPATING_DEVICES;
    static
    {
    	PARTICIPATING_DEVICES = new HashMap<String, Integer>();
    	PARTICIPATING_DEVICES.put("BC:EE:7B:B0:7E:5A", DEVICE_TYPE_RELAY);
    	PARTICIPATING_DEVICES.put("D8:50:E6:34:15:4D", DEVICE_TYPE_RELAY);
    	PARTICIPATING_DEVICES.put("BC:EE:7B:B0:7D:E4", DEVICE_TYPE_RELAY);
    	PARTICIPATING_DEVICES.put("BC:EE:7B:B0:7C:38", DEVICE_TYPE_SINK);
    	PARTICIPATING_DEVICES.put("98:4F:EE:03:37:7A", DEVICE_TYPE_SENSOR);
    	PARTICIPATING_DEVICES.put("58:3F:54:74:63:6A", DEVICE_TYPE_SENSOR);
    	
    }
}
