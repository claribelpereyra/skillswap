package com.uade.skillswap.domain.publication;

import com.uade.skillswap.domain.profile.AccountType;

public enum PublicationType {
    MENTORING(AccountType.PROFESSIONAL),
    SERVICE(AccountType.PROFESSIONAL),
    CHALLENGE(AccountType.ORGANIZATION);

    private final AccountType publishedBy;

    PublicationType(AccountType publishedBy) {
        this.publishedBy = publishedBy;
    }

    // Una mentoría o un servicio los publica un profesional; un desafío, una organización.
    public boolean canBePublishedBy(AccountType accountType) {
        return publishedBy == accountType;
    }
}
