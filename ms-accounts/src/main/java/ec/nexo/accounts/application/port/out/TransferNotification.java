package ec.nexo.accounts.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

/** Datos del aviso de una transferencia. La cuenta destino va enmascarada: nunca se comparte el número completo. */
public record TransferNotification(UUID customerId, UUID transferId, BigDecimal amount, String currency,
                                   String targetAccount) {
}
