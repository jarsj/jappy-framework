package com.crispy.template;

import freemarker.template.*;
import org.json.JSONArray;

/**
 * Created by harsh on 1/1/16.
 */
public class JSONArrayModel extends WrappingTemplateModel implements TemplateSequenceModel {

    private JSONArray a;

    public JSONArrayModel(JSONArray a, ObjectWrapper wrapper) {
        super(wrapper);
        this.a = a;
    }


    @Override
    public TemplateModel get(int index) throws TemplateModelException {
        return wrap(a.get(index));
    }

    @Override
    public int size() throws TemplateModelException {
        return a.length();
    }

    public JSONArray getAsJSONArray() {
        return a;
    }
}
