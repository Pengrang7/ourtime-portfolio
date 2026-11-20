package com.placeboard.board;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MemberInviteRequest(
        @NotBlank @Email String email,
        boolean canEditPlace,
        boolean canManageTag,
        boolean canDeletePlace,
        boolean canInviteMember
) {
}
