package de.tronka.toml;

import de.tronka.toml.comments.CommentData;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.tronka.toml.CommentUtil.addComments;
import static de.tronka.toml.PrimitiveArrayValueWriter.PRIMITIVE_ARRAY_VALUE_WRITER;
import static de.tronka.toml.TableArrayValueWriter.TABLE_ARRAY_VALUE_WRITER;
import static de.tronka.toml.ValueWriters.WRITERS;

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
    write(value, context, null);
  }

  public void write(Object value, WriterContext context, CommentData commentData) {
    CommentData data = commentData;
    if (data == null) {
      data = new CommentData(null);
    }
    Map<?, ?> from = (Map<?, ?>) value;
    if (hasPrimitiveValues(from, context)) {
      context.writeKey(data.getComment());
    }

    // Render primitive types and arrays of primitive first so they are
    // grouped under the same table (if there is one)
    for (Object key : from.keySet()) {
      Object fromValue = from.get(key);
      if (fromValue == null) {
        if (data.hasChildWithNullComment(key.toString())) {
          if (data.hasChildWithComment(key.toString())) {
            addComments(data.getChild(key.toString()).getComment(), context);
          }
          String[] nullComment = new String[1];
          nullComment[0] = key + " = " + data.getChild(key.toString()).getNullComment();
          addComments(nullComment, context);
        }
        continue;
      }
      ValueWriter valueWriter = WRITERS.findWriterFor(fromValue);
      if (data.hasChildWithComment(key.toString())) {
        if (valueWriter.isPrimitiveType() || valueWriter == PRIMITIVE_ARRAY_VALUE_WRITER)
          addComments(data.getChild(key.toString()).getComment(), context);
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
      final boolean hasComment = data.hasChildWithComment(key.toString());
      if (valueWriter == this || valueWriter == TABLE_ARRAY_VALUE_WRITER) {
        if (hasComment) {
          context.write('\n');
          addComments(data.getChild(key.toString()).getComment(), context);
        }
        if (valueWriter == this) {
          this.write(fromValue, context.pushTable(quoteKey(key)), data.getChild(key.toString()));
        } else {
          valueWriter.write(fromValue, context.pushTable(quoteKey(key)));
        }
      } else if (valueWriter == ObjectValueWriter.OBJECT_VALUE_WRITER) {
        ((ObjectValueWriter) valueWriter).write(fromValue, context.pushTable(quoteKey(key)), data.getChild(key.toString()));
      }
    }
  }

  @Override
  public boolean isPrimitiveType() {
    return false;
  }

}
