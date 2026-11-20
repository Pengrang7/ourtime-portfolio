package com.placeboard.board;

public record BoardSummary(long boardId, String name, BoardMapType mapType, long ownerId, String ownerNickname) {
}
