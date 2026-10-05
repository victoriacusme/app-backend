package ec.nexo.accounts.infrastructure.adapter.in.web.dto;

import java.util.List;

public record AccountListResponse(List<AccountResponse> items) {
}
