package com.crispy;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import freemarker.template.SimpleCollection;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.WrappingTemplateModel;

public class JSONObjectModel extends WrappingTemplateModel implements
		TemplateHashModelEx {
	private JSONObject mO;

	public JSONObjectModel(JSONObject o) {
		setObjectWrapper(UWrapper.INSTANCE);
		mO = o;
	}

	@SuppressWarnings("unchecked")
	public TemplateCollectionModel keys() throws TemplateModelException {
		ArrayList<String> keys = new ArrayList<String>();
		Iterator<String> iter = mO.keys();
		while (iter.hasNext()) {
			keys.add(iter.next());
		}
		return new SimpleCollection(keys, getObjectWrapper());
	}

	public int size() throws TemplateModelException {
		return mO.length();
	}

	@SuppressWarnings("unchecked")
	public TemplateCollectionModel values() throws TemplateModelException {
		try {
			ArrayList<Object> values = new ArrayList<Object>();
			Iterator<String> iter = mO.keys();
			while (iter.hasNext()) {
				values.add(mO.get(iter.next()));
			}
			return new SimpleCollection(values, getObjectWrapper());
		} catch (JSONException e) {
			throw new TemplateModelException(e);
		}
	}

	public TemplateModel get(String key) throws TemplateModelException {
		try {
			if (!mO.has(key)) {
				return null;
			}
			return UWrapper.INSTANCE.wrap(mO.get(key));
		} catch (JSONException e) {
			throw new TemplateModelException(e);
		}
	}

	public boolean isEmpty() throws TemplateModelException {
		return mO.length() > 0;
	}

	JSONObject source() {
		return mO;
	}
}
