package com.moandjiezana.toml;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import static com.moandjiezana.toml.MapValueWriter.MAP_VALUE_WRITER;

class ObjectValueWriter implements ValueWriter {
  static final ValueWriter OBJECT_VALUE_WRITER = new ObjectValueWriter();

  @Override
  public boolean canWrite(Object value) {
    return true;
  }

  @Override
  public void write(Object value, WriterContext context) {
    write(value, context, null, null);
  }

  private ObjectValueWriter() {
  }

  private static Set<Field> getFields(Class<?> cls) {
    Set<Field> fields = new LinkedHashSet<>(Arrays.asList(cls.getDeclaredFields()));
    while (cls != Object.class) {
      fields.addAll(Arrays.asList(cls.getDeclaredFields()));
      cls = cls.getSuperclass();
    }
    removeConstantsAndSyntheticFields(fields);

    return fields;
  }

  private static void removeConstantsAndSyntheticFields(Set<Field> fields) {
    fields.removeIf(field -> (Modifier.isFinal(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) || field.isSynthetic() || Modifier.isTransient(field.getModifiers()) || field.isAnnotationPresent(TomlIgnore.class));
  }

  private static Object getFieldValue(Field field, Object o) {
    if (field.isAnnotationPresent(TomlIgnore.class)) return null;
    //noinspection deprecation
    boolean isAccessible = field.isAccessible();

    if (!field.trySetAccessible()) return null;
    Object value = null;
    try {
      value = field.get(o);
    } catch (IllegalAccessException ignored) {
    }
    field.setAccessible(isAccessible);
    return value;
  }

  @Override
  public boolean isPrimitiveType() {
    return false;
  }

  public void write(Object value, WriterContext context, Map<String, String[]> nestedOverlay, String[] objectComment) {
    final Map<String, Object> to = new LinkedHashMap<>();
    final Set<Field> fields = getFields(value.getClass());

    final Map<String, String[]> comments = new HashMap<>();
    final Map<String, Map<String, String[]>> nestedComments = new HashMap<>();

    for (Field field : fields) {
      final Object fieldValue = getFieldValue(field, value);
      to.put(field.getName(), fieldValue);
      if (field.isAnnotationPresent(TomlComment.class)) {
        TomlComment comment = field.getAnnotation(TomlComment.class);
        comments.put(field.getName(), comment.value());
      }
      if (field.isAnnotationPresent(TomlMapComments.class)) {
        TomlMapComments mapComments = field.getAnnotation(TomlMapComments.class);
        Map<String, String[]> nested = new HashMap<>();
        for (TomlMapComment mapComment : mapComments.value()) {
          nested.put(mapComment.key(), mapComment.value());
        }
        nestedComments.put(field.getName(), nested);
      }

    }
    if (nestedOverlay != null) {
      comments.putAll(nestedOverlay);
    }
    ((MapValueWriter) MAP_VALUE_WRITER).write(to, context, comments, nestedComments, objectComment);
  }
}
