package com.moandjiezana.toml;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.moandjiezana.toml.CommentUtil.addComments;
import static com.moandjiezana.toml.PrimitiveArrayValueWriter.PRIMITIVE_ARRAY_VALUE_WRITER;
import static com.moandjiezana.toml.TableArrayValueWriter.TABLE_ARRAY_VALUE_WRITER;
import static com.moandjiezana.toml.ValueWriters.WRITERS;

class MapValueWriter implements ValueWriter {
  static final ValueWriter MAP_VALUE_WRITER = new MapValueWriter();

  private static final Pattern REQUIRED_QUOTING_PATTERN = Pattern.compile("^.*[^A-Za-z\\d_-].*$");

  private MapValueWriter() {
  }

  private static String quoteKey(Object key) {
    String stringKey = key.toString();
    Matcher matcher = REQUIRED_QUOTING_PATTERN.matcher(stringKey);
    if (matcher.matches()) {
      stringKey = "\"" + stringKey + "\"";
    }

    return stringKey;
  }

  private static boolean hasPrimitiveValues(Map<?, ?> values, WriterContext context) {
    for (Object key : values.keySet()) {
      Object fromValue = values.get(key);
      if (fromValue == null) {
        continue;
      }

      ValueWriter valueWriter = WRITERS.findWriterFor(fromValue);
      if (valueWriter.isPrimitiveType() || valueWriter == PRIMITIVE_ARRAY_VALUE_WRITER) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean canWrite(Object value) {
    return value instanceof Map;
  }

  @Override
  public void write(Object value, WriterContext context) {
    write(value, context, null, null, null);
  }

  public void write(Object value, WriterContext context, final Map<String, String[]> comments, final Map<String, Map<String, String[]>> nestedComments, final String[] objectComment) {
    Map<?, ?> from = (Map<?, ?>) value;
    if (hasPrimitiveValues(from, context)) {
      context.writeKey(objectComment);
    }

    // Render primitive types and arrays of primitive first so they are
    // grouped under the same table (if there is one)
    for (Object key : from.keySet()) {
      Object fromValue = from.get(key);
      if (fromValue == null) {
        continue;
      }
      ValueWriter valueWriter = WRITERS.findWriterFor(fromValue);
      if (comments != null && comments.containsKey(key.toString())) {
        if (valueWriter.isPrimitiveType() || valueWriter == PRIMITIVE_ARRAY_VALUE_WRITER)
          addComments(comments.get(key.toString()), context);
      }
      if (valueWriter.isPrimitiveType()) {
        context.indent();
        context.write(quoteKey(key)).write(" = ");
        valueWriter.write(fromValue, context);
        context.write('\n');
      } else if (valueWriter == PRIMITIVE_ARRAY_VALUE_WRITER) {
        context.setArrayKey(key.toString());
        context.write(quoteKey(key)).write(" = ");
        valueWriter.write(fromValue, context);
        context.write('\n');
      }
    }


    // Now render (sub)tables and arrays of tables
    for (Object key : from.keySet()) {
      Object fromValue = from.get(key);
      if (fromValue == null) {
        continue;
      }

      ValueWriter valueWriter = WRITERS.findWriterFor(fromValue);
      final boolean hasComment = comments != null && comments.containsKey(key.toString());
      if (valueWriter == this || valueWriter == TABLE_ARRAY_VALUE_WRITER) {
        if (comments != null && comments.containsKey(key.toString())) {
          context.write('\n');
          addComments(comments.get(key.toString()), context);
        }
        if (valueWriter == this) {
          Map<String, String[]> nestedOverlay = nestedComments != null ? nestedComments.get(key.toString()) : null;
          this.write(fromValue, context.pushTable(quoteKey(key)), nestedOverlay, null, null);
        } else {
          valueWriter.write(fromValue, context.pushTable(quoteKey(key)));
        }
      } else if (valueWriter == ObjectValueWriter.OBJECT_VALUE_WRITER) {
        String[] comment = hasComment ? comments.get(key.toString()) : null;
        Map<String, String[]> nestedOverlay = nestedComments != null ? nestedComments.get(key.toString()) : null;
        ((ObjectValueWriter) valueWriter).write(fromValue, context.pushTable(quoteKey(key)), nestedOverlay, comment);
      }
    }
  }

  @Override
  public boolean isPrimitiveType() {
    return false;
  }

}
