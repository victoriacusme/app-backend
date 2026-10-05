package ec.nexo.accounts.application.usecase;

import ec.nexo.accounts.application.port.out.TransferRepositoryPort;
import ec.nexo.accounts.domain.exception.TransferNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTransferUseCaseTest {

    @Mock
    private TransferRepositoryPort transfers;
    @InjectMocks
    private GetTransferUseCase useCase;

    @Test
    void unaTransferenciaDeOtroClienteSeRespondeComoInexistente() {
        UUID customerId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        when(transfers.findByIdAndCustomerId(transferId, customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(customerId, transferId))
                .isInstanceOf(TransferNotFoundException.class);
    }
}
