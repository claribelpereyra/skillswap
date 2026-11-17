package com.uade.skillswap.controllers.publication;

import com.uade.skillswap.application.dto.publication.CatalogEntryView;
import com.uade.skillswap.application.ports.in.publication.ListCatalogUseCase;
import com.uade.skillswap.application.ports.in.publication.ManageCatalogUseCase;
import com.uade.skillswap.application.ports.out.IdentityService;
import com.uade.skillswap.controllers.ControllerResult.ErrorType;
import com.uade.skillswap.domain.common.UserId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CatalogControllerTest {

    private final ListCatalogUseCase list = mock(ListCatalogUseCase.class);
    private final ManageCatalogUseCase manage = mock(ManageCatalogUseCase.class);
    private final IdentityService identity = mock(IdentityService.class);
    private final CatalogController controller = new CatalogControllerImpl(list, manage, identity);

    @Test
    void theActiveUserAddsCategoriesAndSkills() {
        when(identity.currentUserId()).thenReturn(new UserId("ana"));
        when(manage.addCategory(null, "Technology")).thenReturn(new CatalogEntryView(1, "Technology", "CATEGORY"));
        when(manage.addSkill(1, "Java")).thenReturn(new CatalogEntryView(2, "Java", "SKILL"));

        assertThat(controller.addCategory("ana", null, "Technology").getData().id()).isEqualTo(1);
        assertThat(controller.addSkill("ana", 1, "Java").getData().kind()).isEqualTo("SKILL");
    }

    @Test
    void addingRequiresTheActiveUser() {
        when(identity.currentUserId()).thenReturn(new UserId("ana"));

        assertThat(controller.addCategory("bruno", null, "Technology").getErrorType())
                .isEqualTo(ErrorType.ACCESS_DENIED);
        assertThat(controller.addSkill(" ", 1, "Java").getErrorType()).isEqualTo(ErrorType.ACCESS_DENIED);
        verifyNoInteractions(manage);
    }
}
