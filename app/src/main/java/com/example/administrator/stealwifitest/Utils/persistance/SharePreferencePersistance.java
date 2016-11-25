package com.example.administrator.stealwifitest.Utils.persistance;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SharePreferencePersistance extends BasePersistance {

    private static final String TAG = SharePreferencePersistance.class.getSimpleName();

    public SharePreferencePersistance() {
    }

    @Override
    public boolean isFileExist(Context context, String fileName) {
        File file = new File("data/data/" + context.getPackageName()
                                 + "/shared_prefs/" + fileName + ".xml");
        return file.isFile();
    }

    public boolean saveStringArray(Context context, String fileName, List<String> source) {
        SharedPreferences preferences = context.getSharedPreferences(fileName,
                                                                     Context.MODE_PRIVATE);
        Editor editor = preferences.edit();
        editor.clear();
        for (int i = 0; i < source.size(); i++) {
            editor.putString(String.valueOf(i), source.get(i));
        }
        return editor.commit();
    }

    public List<String> restoreStringArray(Context context, String fileName) {
        List<String> list = new ArrayList<String>();
        String[] listArray = null;
        SharedPreferences preferences = context.getSharedPreferences(fileName,
                                                                     Context.MODE_PRIVATE);
        Map<String, ?> map = preferences.getAll();
        if (map != null && !map.isEmpty()) {
            listArray = new String[map.size()];
            for (Entry<String, ?> entry : map.entrySet()) {
                try {
                    listArray[Integer.parseInt(entry.getKey())] = (String) entry
                                                                               .getValue();
                } catch (Exception e) {
                   
                        e.printStackTrace();
                    
                    continue;
                }
            }
        }
        if (listArray != null && listArray.length > 0) {
            list = new ArrayList<String>(Arrays.asList(listArray));
        }
        return list;
    }

    @Override
    public boolean save(Context context, String fileName, List<?> source, Object... otherParams) {
        SharedPreferences preferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        Editor editor = preferences.edit();
        editor.clear();
        for (Object bean : source) {
            Map<String, Object> map = parseBean(bean);
            if (map != null) {
                for (String key : map.keySet()) {
                    Object value = map.get(key);
                    if (value == null) {
                        continue;
                    }
                    Class<?> cls = value.getClass();
                    // 这里为key添加一个hashcode以便于识别不同的bean对象
                    key = key + ":" + bean.hashCode();
                    // SharePreference只支持保存5种基本类型
                    if (cls.isAssignableFrom(Long.class)) {
                        editor.putLong(key, (Long) value);
                    } else if (cls.isAssignableFrom(String.class)) {
                        editor.putString(key, (String) value);
                    } else if (cls.isAssignableFrom(Integer.class)) {
                        editor.putInt(key, (Integer) value);
                    } else if (cls.isAssignableFrom(Boolean.class)) {
                        editor.putBoolean(key, (Boolean) value);
                    } else if (cls.isAssignableFrom(Float.class)) {
                        editor.putFloat(key, (Float) value);
                    } else {
                        Log.w(TAG, "无法识别的类型：key=" + key + "，class=" + value.getClass().getSimpleName());
                    }
                }
            }
        }
        return editor.commit();
    }

    @Override
    public boolean save(Context context, String fileName, List<Map<String, Object>> source) {
        SharedPreferences preferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        Editor editor = preferences.edit();
        editor.clear();
        for (Map<String, Object> map : source) {
            for (String key : map.keySet()) {
                Object value = map.get(key);
                if (value == null) {
                    continue;
                }
                key = key + ":" + map.hashCode();
                if (value instanceof Long) {
                    editor.putLong(key, (Long) value);
                } else if (value instanceof String) {
                    editor.putString(key, (String) value);
                } else if (value instanceof Integer) {
                    editor.putInt(key, (Integer) value);
                } else if (value instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) value);
                } else if (value instanceof Float) {
                    editor.putFloat(key, (Float) value);
                } else {
                    Log.w(TAG, "无法识别的类型：key=" + key + "，class="
                                   + value.getClass().getSimpleName());
                }
            }
        }
        return editor.commit();
    }

    @Override
    public <T> List<T> restore(Context context, String fileName, Class<T> beanClass, Object... otherParams) {
        List<T> list = new ArrayList<T>();
        SharedPreferences preferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        Map<String, ?> map = preferences.getAll();
        HashMap<String, HashMap<String, Object>> parseMap = new HashMap<String, HashMap<String, Object>>();
        for (String key : map.keySet()) {
            String[] keyInfos = key.split(":");
            if (keyInfos == null || keyInfos.length != 2) {
                continue;
            }
            String realKey = keyInfos[0];
            String ident = keyInfos[1];
            Object value = map.get(key);
            HashMap<String, Object> beanMap = parseMap.get(ident);
            // 没有对象则新建，否则追加数据
            if (beanMap == null) {
                beanMap = new HashMap<String, Object>();
                parseMap.put(ident, beanMap);
            }
            beanMap.put(realKey, value);
        }
        // 这里一个beanmap代表了一个bean对象
        for (HashMap<String, Object> beanMap : parseMap.values()) {
            try {
                T bean = beanClass.newInstance();
                for (String fieldName : beanMap.keySet()) {
                    Object fieldValue = beanMap.get(fieldName);
                    Field field = bean.getClass().getDeclaredField(fieldName);
                    if (field != null) {
                        field.setAccessible(true);
                        field.set(bean, fieldValue);
                    }
                }
                list.add(bean);
            } catch (IllegalAccessException e) {
                continue;
            } catch (InstantiationException e) {
                continue;
            } catch (SecurityException e) {
                continue;
            } catch (NoSuchFieldException e) {
                continue;
            }
        }
        return list;
    }

    @Override
    public List<Map<String, Object>> restore(Context context, String fileName, Object... otherParams) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        SharedPreferences preferences = context.getSharedPreferences(fileName,
                                                                     Context.MODE_PRIVATE);
        HashMap<String, HashMap<String, Object>> parseMap = new HashMap<String, HashMap<String, Object>>();
        Map<String, ?> map = preferences.getAll();
        for (String key : map.keySet()) {
            String[] keyInfos = key.split(":");
            if (keyInfos == null || keyInfos.length != 2) {
                continue;
            }
            String realKey = keyInfos[0];
            String ident = keyInfos[1];
            Object value = map.get(key);
            HashMap<String, Object> beanMap = parseMap.get(ident);
            // 没有对象则新建，否则追加数据
            if (beanMap == null) {
                beanMap = new HashMap<String, Object>();
                parseMap.put(ident, beanMap);
            }
            beanMap.put(realKey, value);
        }
        // 这里一个beanmap代表了一个bean对象
        for (HashMap<String, Object> beanMap : parseMap.values()) {
            result.add(beanMap);
        }
        return result;
    }

    public static Map<String, Object> parseBean(Object bean) {
        if (bean == null) {
            return null;
        }
        Map<String, Object> map = null;
        Field[] fields = bean.getClass().getDeclaredFields();
        if (fields != null) {
            map = new HashMap<String, Object>();
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    map.put(field.getName(), field.get(bean));
                } catch (IllegalArgumentException e) {
                   
                        e.printStackTrace();
                    
                } catch (IllegalAccessException e) {
                   
                        e.printStackTrace();
                    
                }
            }
        }
        return map;
    }
    
    public boolean putString(Context context,String key, String value) {
        SharedPreferences preferences = context.getSharedPreferences("syssdk_setting", Context.MODE_PRIVATE);
        Editor editor = preferences.edit(); 
        editor.clear();
        editor.putString(key, value).apply();
        return editor.commit();
    }
    
    public String getString(Context context,String key, String defValue) {
        SharedPreferences preferences = context.getSharedPreferences("syssdk_setting", Context.MODE_PRIVATE);
        return preferences.getString(key, defValue);
    }
}
