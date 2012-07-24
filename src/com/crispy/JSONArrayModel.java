package com.crispy;

import org.json.JSONArray;
import org.json.JSONException;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.WrappingTemplateModel;

public class JSONArrayModel extends WrappingTemplateModel implements
		TemplateSequenceModel{
	private JSONArray mA;

	public JSONArrayModel(JSONArray a) {
		setObjectWrapper(UWrapper.INSTANCE);
		mA = a;
	}

	public TemplateModel get(int index) throws TemplateModelException {
		try {
			return wrap(mA.get(index));
		} catch (JSONException e) {
			throw new TemplateModelException(e);
		}
	}

	public int size() throws TemplateModelException {
		return mA.length();
	}

	JSONArray source() {
		return mA;
	}


}
