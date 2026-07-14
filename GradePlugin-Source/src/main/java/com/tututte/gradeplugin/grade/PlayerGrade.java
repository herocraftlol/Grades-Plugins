package com.tututte.gradeplugin.grade;

import java.time.Instant;

public class PlayerGrade {

    private final String gradeId;
    private final String source;
    private final Instant grantedAt;
    private final Instant expiresAt; // null = permanent

    public PlayerGrade(String gradeId, String source, Instant grantedAt, Instant expiresAt) {
        this.gradeId = gradeId;
        this.source = source;
        this.grantedAt = grantedAt;
        this.expiresAt = expiresAt;
    }

    public String getGradeId() {
        return gradeId;
    }

    public String getSource() {
        return source;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }
}
