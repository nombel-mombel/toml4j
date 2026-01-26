package com.moandjiezana.toml;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

public class TomlCommentInjectionTest {
    @Test
    public void should_add_value_comment() {
        TomlWriter writer = new TomlWriter();
        String expected = """
                # TEST
                value = "string"
                """;
        Assert.assertEquals(expected, writer.write(new ValueTestClass()));
    }

    static class ValueTestClass {
        @TomlComment("TEST")
        public String value = "string";
    }

    @Test
    public void should_add_object_comment() {
        TomlWriter writer = new TomlWriter();
        String expected = """
                # SECOND
                [test]
                # TEST
                value = "string"
                """;
        Assert.assertEquals(expected, writer.write(new MapTestClass()));
    }

    static class MapTestClass {
        @TomlComment("SECOND")
        public ValueTestClass test = new ValueTestClass();
    }

    @Test
    public void should_add_all_comment() {
        TomlWriter writer = new TomlWriter();
        String expected = """
                # FIRST
                integer = 1
                
                # SECOND
                [test]
                # TEST
                value = "string"
                """;
        Assert.assertEquals(expected, writer.write(new MapAndValueTestClass()));
    }

    static class MapAndValueTestClass {
        @TomlComment("SECOND")
        public ValueTestClass test = new ValueTestClass();
        @TomlComment("FIRST")
        public int integer = 1;
    }

    @Test
    public void should_add_nested_comments() {
        TomlWriter writer = new TomlWriter();
        String expected = """
                
                # Normal Comment
                
                # AA
                [test.a]
                # TEST
                value = "string"
                
                # BB
                [test.b]
                # TEST
                value = "string"
                """;
        Assert.assertEquals(expected, writer.write(new NestedMapCommentTestClass()));
    }

    static class NestedMapCommentTestClass {
        @TomlComment("Normal Comment")
        @TomlMapComment(key = "a", value = "AA")
        @TomlMapComment(key = "b", value = "BB")
        public Map<String, ValueTestClass> test = new TreeMap<>(Map.of("a", new ValueTestClass(), "b", new ValueTestClass()));
    }

    @Test
    public void should_add_nested_comments_enum_key() {
        TomlWriter writer = new TomlWriter();
        String expected = """
                
                # Normal Comment
                
                # AA
                [test.A]
                # TEST
                value = "string"
                
                # BB
                [test.B]
                # TEST
                value = "string"
                """;
        Assert.assertEquals(expected, writer.write(new NestedMapCommentTestClassEnum()));
    }

    enum KEY {
        A, B
    }

    static class NestedMapCommentTestClassEnum {
        @TomlComment("Normal Comment")
        @TomlMapComment(key = "A", value = "AA")
        @TomlMapComment(key = "B", value = "BB")
        public Map<KEY, ValueTestClass> test = new TreeMap<>(Map.of(KEY.A, new ValueTestClass(), KEY.B, new ValueTestClass()));
    }
}