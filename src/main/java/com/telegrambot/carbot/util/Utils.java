package com.telegrambot.carbot.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class Utils {

    public static InputStream convertSetToInputStream(Set<String> stringSet) {
        String content = String.join("\n", stringSet);
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
