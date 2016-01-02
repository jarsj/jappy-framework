package com.crispy.template;

import freemarker.template.*;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by harsh on 1/1/16.
 */
public class JSONObjectModel extends WrappingTemplateModel implements TemplateHashModelEx {

    private JSONObject o;

    public JSONObjectModel(JSONObject o, ObjectWrapper wrapper) {
        super(wrapper);
        this.o = o;
    }

    @Override
    public int size() throws TemplateModelException {
        return o.length();
    }

    @Override
    public TemplateCollectionModel keys() throws TemplateModelException {
        return new SimpleCollection(o.keys(), getObjectWrapper());
    }

    @Override
    public TemplateCollectionModel values() throws TemplateModelException {
        List<Object> values = new ArrayList<>();
        Iterator<String> keysIter = o.keys();
        while (keysIter.hasNext()) {
            values.add(o.get(keysIter.next()));
        }
        return new SimpleCollection(values, getObjectWrapper());
    }

    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        return wrap(o.opt(key));
    }

    @Override
    public boolean isEmpty() throws TemplateModelException {
        return o.length() == 0;
    }
}
