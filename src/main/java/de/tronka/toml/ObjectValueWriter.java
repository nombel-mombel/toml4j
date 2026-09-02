package de.tronka.toml;

import de.tronka.toml.comments.CommentData;
import de.tronka.toml.comments.TomlComment;
import de.tronka.toml.comments.TomlMapComment;
import de.tronka.toml.comments.TomlMapComments;
import de.tronka.toml.comments.TomlNullComment;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import static de.tronka.toml.MapValueWriter.MAP_VALUE_WRITER;

class ObjectValueWriter implements ValueWriter {
  static final ValueWriter OBJECT_VALUE_WRITER = new ObjectValueWriter();

  @Override
  public boolean canWrite(Object value) {
    return true;
  }

  @Override
  public void write(Object value, WriterContext context) {
    write(value, context, null);
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

  public void write(Object value, WriterContext context, CommentData data) {
    final Map<String, Object> to = new LinkedHashMap<>();
    final Set<Field> fields = getFields(value.getClass());
    if (data == null) {
      data = new CommentData(null);
    }

    for (Field field : fields) {
      final Object fieldValue = getFieldValue(field, value);
      to.put(field.getName(), fieldValue);
      if (field.isAnnotationPresent(TomlComment.class)) {
        TomlComment comment = field.getAnnotation(TomlComment.class);
        data.addChild(field.getName(), comment.value());
      }
      if (field.isAnnotationPresent(TomlMapComments.class)) {
        TomlMapComments mapComments = field.getAnnotation(TomlMapComments.class);
        CommentData child = data.getOrCreateChild(field.getName());
        for (TomlMapComment mapComment : mapComments.value()) {
          child.addChild(mapComment.key(), mapComment.value());
        }
      }
      if (field.isAnnotationPresent(TomlNullComment.class)) {
        TomlNullComment nullComment = field.getAnnotation(TomlNullComment.class);
        CommentData child = data.getOrCreateChild(field.getName());
        child.setNullComment(nullComment.value());
      }
    }
    ((MapValueWriter) MAP_VALUE_WRITER).write(to, context, data);
  }
}
