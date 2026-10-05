package ec.nexo.accounts.infrastructure.adapter.in.web.dto;

import java.util.List;

/** {@code nextCursor} es nulo en la última página. */
public record MovementPageResponse(List<MovementResponse> items, String nextCursor) {
}
