package com.ledgerpilot.ledgerpilot_backend.authorization;

import java.util.*;

public enum Role {
    OWNER, ADMIN, ACCOUNTANT, APPROVER, EMPLOYEE, VIEWER;

    public Set<Permission> permissions() {
        return switch (this) {
            case OWNER -> EnumSet.allOf(Permission.class);
            case ADMIN -> EnumSet.of(Permission.TENANT_READ, Permission.TENANT_UPDATE, Permission.MEMBER_READ,
                    Permission.MEMBER_INVITE, Permission.MEMBER_MANAGE, Permission.AUDIT_READ);
            case ACCOUNTANT, APPROVER, EMPLOYEE, VIEWER -> EnumSet.of(Permission.TENANT_READ, Permission.MEMBER_READ);
        };
    }
}
