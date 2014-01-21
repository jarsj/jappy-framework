package com.crispy.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleCollection;
import freemarker.template.SimpleDate;
import freemarker.template.SimpleHash;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleObjectWrapper;
import freemarker.template.SimpleScalar;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class UWrapper implements ObjectWrapper {

	public static final UWrapper INSTANCE = new UWrapper();

	public Object unwrap(TemplateModel t) throws TemplateModelException {
		if (t == null)
			return null;
		if (t instanceof JSONObjectModel)
			return ((JSONObjectModel) t).source();
		if (t instanceof JSONArrayModel)
			return ((JSONArrayModel) t).source();
		return ((SimpleObjectWrapper) SIMPLE_WRAPPER).unwrap(t);
	}

	@SuppressWarnings("unchecked")
	public TemplateModel wrap(Object obj) throws TemplateModelException {
		if (obj == null) {
			return null;
		}
		if (obj.equals(null))
			return null;
		if (obj instanceof TemplateModel) {
			return (TemplateModel) obj;
		}
		if (obj instanceof String) {
			return new SimpleScalar((String) obj);
		}
		if (obj instanceof JSONObject)
			return new JSONObjectModel((JSONObject) obj);
		if (obj instanceof JSONArray)
			return new JSONArrayModel((JSONArray) obj);
		if (obj instanceof Number) {
			return new SimpleNumber((Number) obj);
		}
		if (obj instanceof java.util.Date) {
			if (obj instanceof java.sql.Date) {
				return new SimpleDate((java.sql.Date) obj);
			}
			if (obj instanceof java.sql.Time) {
				return new SimpleDate((java.sql.Time) obj);
			}
			if (obj instanceof java.sql.Timestamp) {
				return new SimpleDate((java.sql.Timestamp) obj);
			}
			return new SimpleDate((java.util.Date) obj, TemplateDateModel.DATE);
		}
		if (obj.getClass().isArray()) {
			obj = convertArray(obj);
		}
		if (obj instanceof Collection) {
			return new SimpleSequence((Collection) obj, this);
		}
		if (obj instanceof Map) {
			return new SimpleHash((Map) obj, this);
		}
		if (obj instanceof Boolean) {
			return obj.equals(Boolean.TRUE) ? TemplateBooleanModel.TRUE
					: TemplateBooleanModel.FALSE;
		}
		if (obj instanceof Iterator) {
			return new SimpleCollection((Iterator) obj, this);
		}
		throw new TemplateModelException("Unknown Type " + obj.getClass());
	}

	@SuppressWarnings("unchecked")
	protected Object convertArray(Object arr) {
		final int size = Array.getLength(arr);
		ArrayList list = new ArrayList(size);
		for (int i = 0; i < size; i++) {
			list.add(Array.get(arr, i));
		}
		return list;
	}
}
