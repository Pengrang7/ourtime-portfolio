package com.placeboard.board;

public record BoardMemberResponse(
        long userId,
        String email,
        String nickname,
        boolean owner,
        boolean canEditPlace,
        boolean canManageTag,
        boolean canDeletePlace,
        boolean canInviteMember
) {
}
