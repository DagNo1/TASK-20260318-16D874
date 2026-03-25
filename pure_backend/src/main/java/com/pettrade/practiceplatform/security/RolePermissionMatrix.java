package com.pettrade.practiceplatform.security;

import java.util.Map;
import java.util.Set;

public final class RolePermissionMatrix {

    public static final Map<String, Set<String>> ENDPOINT_ACCESS = Map.of(
            "PRACTICE_CREATE_OR_UPDATE",
            Set.of(Roles.PLATFORM_ADMIN, Roles.MERCHANT_OPERATOR, Roles.REGULAR_BUYER),
            "PRACTICE_READ",
            Set.of(Roles.PLATFORM_ADMIN, Roles.MERCHANT_OPERATOR, Roles.REGULAR_BUYER, Roles.REVIEWER),
            "AUTH_LOGIN",
            Set.of(Roles.PLATFORM_ADMIN, Roles.MERCHANT_OPERATOR, Roles.REGULAR_BUYER, Roles.REVIEWER)
    );

    private RolePermissionMatrix() {
    }
}
