package com.crispy.template;

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by harsh on 1/8/16.
 */
public class JSONWrapper extends DefaultObjectWrapper {

    public static final JSONWrapper INSTANCE = new JSONWrapper();

    @Override
    public TemplateModel wrap(Object obj) throws TemplateModelException {
        if (obj instanceof JSONObject) {
            return new JSONObjectModel((JSONObject) obj, this);
        }
        if (obj instanceof JSONArray) {
            return new JSONArrayModel((JSONArray) obj, this);
        }
        return super.wrap(obj);
    }
}
