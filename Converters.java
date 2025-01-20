package com.jio.jiotalkie.db;

import androidx.room.TypeConverter;

import java.util.Arrays;
import java.util.List;

public class Converters {

    @TypeConverter
    public String fromList(List<String> list) {
        // Convert List<String> to a single String (comma-separated)
        return list != null ? String.join(",", list) : null;
    }

    @TypeConverter
    public List<String> toList(String concatenatedString) {
        // Convert a single String back to List<String>
        return concatenatedString != null ? Arrays.asList(concatenatedString.split(",")) : null;
    }
}
