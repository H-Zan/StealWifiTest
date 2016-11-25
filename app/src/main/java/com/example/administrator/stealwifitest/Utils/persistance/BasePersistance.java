package com.example.administrator.stealwifitest.Utils.persistance;

import android.content.Context;

import java.util.List;
import java.util.Map;

public abstract class BasePersistance {

    public abstract boolean save(Context context, String fileName, List<?> source, Object... otherParams);

    public abstract boolean save(Context context, String fileName, List<Map<String, Object>> source);

    public abstract <T> List<T> restore(Context context, String fileName, Class<T> beanClass, Object... otherParams);

    public abstract List<Map<String, Object>> restore(Context context, String fileName, Object... otherParams);

    public abstract boolean isFileExist(Context context, String fileName);
}
