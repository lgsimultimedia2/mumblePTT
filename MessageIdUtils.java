package com.jio.jiotalkie.util;

import java.util.UUID;

public class MessageIdUtils {
    public static String generateUUID(){
        return UUID.randomUUID().toString();
    }
}
