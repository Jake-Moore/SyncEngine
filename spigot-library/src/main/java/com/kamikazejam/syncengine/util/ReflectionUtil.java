package com.kamikazejam.syncengine.util;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class ReflectionUtil {
    public static List<Field> getAllFields(@Nullable Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null) {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field field : declaredFields) {
                // Skip transient and static fields
                int m = field.getModifiers();
                if (Modifier.isTransient(m) || Modifier.isStatic(m)) { continue; }
                fields.add(field);
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }
}
