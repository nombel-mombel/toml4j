package de.tronka.toml.comments;

import java.util.HashMap;
import java.util.Map;

public class CommentData {
    private Map<String, CommentData> children = new HashMap<>();
    private final String[] comment;
    private String nullComment;

    public CommentData(String[] comment) {
        this.comment = comment;
    }

    public CommentData getChild(String key) {
        return this.children.get(key);
    }

    public boolean hasChildWithComment(String key) {
        return this.children.containsKey(key) && this.children.get(key).getComment() != null;
    }

    public CommentData getOrCreateChild(String key) {
        return this.children.computeIfAbsent(key, k -> new CommentData(null));
    }

    public CommentData addChild(String key, String[] comment) {
        return this.children.put(key, new CommentData(comment));
    }

    public String[] getComment() {
        return this.comment;
    }

    public boolean hasChildWithNullComment(String key) {
        return this.children.containsKey(key) && this.children.get(key).getNullComment() != null;
    }

    public void setNullComment(String nullComment) {
        this.nullComment = nullComment;
    }

    public String getNullComment() {
        return this.nullComment;
    }
}
