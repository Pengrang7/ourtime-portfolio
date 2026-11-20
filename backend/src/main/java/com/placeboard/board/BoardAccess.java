package com.placeboard.board;

public record BoardAccess(
        long boardId,
        long ownerId,
        BoardMapType mapType,
        boolean canEditPlace,
        boolean canManageTag,
        boolean canDeletePlace,
        boolean canInviteMember
) {
    public BoardAccess(long boardId, long ownerId, boolean canEditPlace, boolean canManageTag,
                       boolean canDeletePlace, boolean canInviteMember) {
        this(boardId, ownerId, BoardMapType.DOMESTIC, canEditPlace, canManageTag,
                canDeletePlace, canInviteMember);
    }

    public boolean isOwner(long userId) {
        return ownerId == userId;
    }
}
