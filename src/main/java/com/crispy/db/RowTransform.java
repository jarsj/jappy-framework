package com.crispy.db;

import org.json.JSONObject;

public interface RowTransform {
	public JSONObject transform(JSONObject o);
}
