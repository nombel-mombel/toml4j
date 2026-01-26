package com.moandjiezana.toml;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When added to a field (typically a Map), it specifies comments to write
 * for a particular key when writing the config. Use the key element to
 * indicate which map entry the comments apply to and the value element
 * to provide one or more comment lines.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Repeatable(TomlMapComments.class)
public @interface TomlMapComment {
    String key();

    String[] value();
}
