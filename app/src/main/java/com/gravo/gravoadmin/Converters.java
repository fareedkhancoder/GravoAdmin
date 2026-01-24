package com.gravo.gravoadmin;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Converters {

    // --- DATE CONVERTERS ---
    @TypeConverter
    public static Long fromDate(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static Date toDate(Long timestamp) {
        return timestamp == null ? null : new Date(timestamp);
    }

    // --- LIST CONVERTERS (Renamed to match your error log) ---
    @TypeConverter
    public static String fromList(List<String> list) { // Changed from fromStringList
        return new Gson().toJson(list);
    }

    @TypeConverter
    public static List<String> toList(String value) {
        Type listType = new TypeToken<List<String>>() {}.getType();
        return new Gson().fromJson(value, listType);
    }

    // --- MAP CONVERTERS (Renamed to match your error log) ---
    @TypeConverter
    public static String fromMap(Map<String, String> map) { // Changed from fromStringMap
        return new Gson().toJson(map);
    }

    @TypeConverter
    public static Map<String, String> toMap(String value) {
        Type mapType = new TypeToken<Map<String, String>>() {}.getType();
        return new Gson().fromJson(value, mapType);
    }
}