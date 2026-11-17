package com.uade.skillswap.controllers.publication;

import com.uade.skillswap.application.common.*;
import com.uade.skillswap.application.dto.publication.CatalogEntryView;
import com.uade.skillswap.application.ports.in.publication.ListCatalogUseCase;
import com.uade.skillswap.application.ports.in.publication.ManageCatalogUseCase;
import com.uade.skillswap.application.ports.out.IdentityService;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.domain.common.UserId;
import java.util.Objects;

public final class CatalogControllerImpl implements CatalogController {
    private final ListCatalogUseCase query;
    private final ManageCatalogUseCase manage;
    private final IdentityService identity;

    public CatalogControllerImpl(ListCatalogUseCase query, ManageCatalogUseCase manage, IdentityService identity) {
        this.query = Objects.requireNonNull(query);
        this.manage = Objects.requireNonNull(manage);
        this.identity = Objects.requireNonNull(identity);
    }

    @Override public ControllerResult<Page<CatalogEntryView>> list(int page, int size) {
        return ControllerResult.of(() -> query.list(new PageRequest(page, size)));
    }

    @Override public ControllerResult<CatalogEntryView> addCategory(String userId, Long parentId, String name) {
        return ControllerResult.of(() -> {
            currentUser(userId, "add a category");
            return manage.addCategory(parentId, name);
        });
    }

    @Override public ControllerResult<CatalogEntryView> addSkill(String userId, long categoryId, String name) {
        return ControllerResult.of(() -> {
            currentUser(userId, "add a skill");
            return manage.addSkill(categoryId, name);
        });
    }

    private UserId currentUser(String userId, String operation) {
        if (userId == null || userId.isBlank()) {
            throw new AccessDeniedException("No active user");
        }
        UserId user = new UserId(userId);
        if (!user.equals(identity.currentUserId())) {
            throw new AccessDeniedException(user, operation);
        }
        return user;
    }
}
