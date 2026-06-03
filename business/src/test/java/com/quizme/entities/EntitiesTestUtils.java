package com.quizme.entities;

import java.lang.reflect.Field;

public class EntitiesTestUtils {
    public static void setId(Object entity, long id) throws Exception {
        Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }
}
