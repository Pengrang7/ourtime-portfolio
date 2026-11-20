package com.placeboard.tag;

public record BoardTagsChangedEvent(long boardId, long changedByUserId) {
}
