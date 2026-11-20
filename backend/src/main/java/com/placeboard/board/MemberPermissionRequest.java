package com.placeboard.board;

public record MemberPermissionRequest(
        boolean canEditPlace,
        boolean canManageTag,
        boolean canDeletePlace,
        boolean canInviteMember
) {
}
