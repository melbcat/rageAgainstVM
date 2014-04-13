package com.example.Net;

import java.util.HashMap;
import java.util.Map;

public class Data {

	private Map<String, String> mInputMap;

	public Data() {
		mInputMap = new HashMap<String, String>();
	}

	public void setHeuristicData(String key, String value) {
		mInputMap.put(key, value);
	}

	public Map<String, String> getHeuristicData() {
		return (mInputMap);
	}
}