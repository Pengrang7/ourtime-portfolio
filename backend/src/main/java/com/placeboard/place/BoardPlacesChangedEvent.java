package com.placeboard.place;

public record BoardPlacesChangedEvent(long boardId, long changedByUserId) {
}
