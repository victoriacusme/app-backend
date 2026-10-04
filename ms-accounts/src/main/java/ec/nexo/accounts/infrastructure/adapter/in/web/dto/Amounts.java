package ec.nexo.accounts.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Los montos viajan como texto con dos decimales ("1250.50") para no perder precisión en el cliente. */
final class Amounts {

    private Amounts() {
    }

    static String format(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }
}
