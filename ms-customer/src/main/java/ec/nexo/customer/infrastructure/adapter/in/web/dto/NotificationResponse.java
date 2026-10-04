package ec.nexo.customer.infrastructure.adapter.in.web.dto;

import ec.nexo.customer.application.usecase.NotifyCustomerUseCase;

public record NotificationResponse(int delivered, int invalidTokensRemoved, int failed, String skipped) {

    public static NotificationResponse from(NotifyCustomerUseCase.Result result) {
        return new NotificationResponse(result.delivered(), result.invalidTokensRemoved(), result.failed(),
                result.skipped() == null ? null : result.skipped().name());
    }
}
