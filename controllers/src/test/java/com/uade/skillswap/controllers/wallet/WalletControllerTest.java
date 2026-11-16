package com.uade.skillswap.controllers.wallet;

import com.uade.skillswap.application.dto.wallet.WalletView;
import com.uade.skillswap.application.ports.in.wallet.LoadWalletCommand;
import com.uade.skillswap.application.ports.in.wallet.LoadWalletUseCase;
import com.uade.skillswap.application.ports.out.IdentityService;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.controllers.ControllerResult.ErrorType;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WalletControllerTest {

    private static final UserId USER = new UserId("ana");

    private final LoadWalletUseCase loadWallet = mock(LoadWalletUseCase.class);
    private final IdentityService identity = mock(IdentityService.class);
    private final WalletController controller = new WalletControllerImpl(loadWallet, identity);

    @BeforeEach
    void activeIdentity() {
        when(identity.currentUserId()).thenReturn(USER);
    }

    @Test
    void theActiveUserCanLoadCredits() {
        WalletView wallet = new WalletView("ana", 50, java.util.List.of());
        when(loadWallet.loadCredits(new LoadWalletCommand(USER, new SkillCoins(50)))).thenReturn(wallet);

        ControllerResult<WalletView> result = controller.loadCredits("ana", 50L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(wallet);
        ArgumentCaptor<LoadWalletCommand> command = ArgumentCaptor.forClass(LoadWalletCommand.class);
        verify(loadWallet).loadCredits(command.capture());
        assertThat(command.getValue().userId()).isEqualTo(USER);
        assertThat(command.getValue().amount()).isEqualTo(new SkillCoins(50));
    }

    @Test
    void onlyTheActiveUserCanLoadCreditsAndTheAmountMustBePositive() {
        assertThat(controller.loadCredits("bruno", 50L).getErrorType()).isEqualTo(ErrorType.ACCESS_DENIED);
        assertThat(controller.loadCredits("ana", null).getErrorType()).isEqualTo(ErrorType.INVALID_INPUT);
        assertThat(controller.loadCredits("ana", 0L).getErrorType()).isEqualTo(ErrorType.INVALID_INPUT);
        assertThat(controller.loadCredits("ana", -1L).getErrorType()).isEqualTo(ErrorType.INVALID_INPUT);
        verifyNoInteractions(loadWallet);
    }
}
