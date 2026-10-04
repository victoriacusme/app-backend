package ec.nexo.accounts.infrastructure.adapter.out.persistence;

import ec.nexo.accounts.application.port.out.DuplicateDefaultAccountException;
import ec.nexo.accounts.application.usecase.MovementCursor;
import ec.nexo.accounts.domain.model.Account;
import ec.nexo.accounts.domain.model.Movement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Adaptadores JPA contra PostgreSQL real (Testcontainers), con las migraciones y la semilla de Flyway. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({AccountRepositoryAdapter.class, MovementRepositoryAdapter.class})
class PersistenceAdaptersIT {

    private static final UUID ANA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CARLOS = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ANA_SAVINGS = UUID.fromString("c0000000-0000-0000-0000-000000000011");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private AccountRepositoryAdapter accounts;
    @Autowired
    private MovementRepositoryAdapter movements;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void laSemillaEsCoherenteSaldoIgualASumaDeMovimientosYNuncaNegativo() {
        Integer inconsistent = jdbc.queryForObject("""
                SELECT count(*) FROM accounts a
                WHERE a.balance <> (SELECT COALESCE(SUM(CASE WHEN m.type = 'CREDIT' THEN m.amount ELSE -m.amount END), 0)
                                    FROM movements m WHERE m.account_id = a.id)
                   OR a.balance <> (SELECT m.balance_after FROM movements m WHERE m.account_id = a.id
                                    ORDER BY m.booked_at DESC, m.id DESC LIMIT 1)
                   OR EXISTS (SELECT 1 FROM movements m WHERE m.account_id = a.id AND m.balance_after < 0)""",
                Integer.class);

        assertThat(inconsistent).isZero();
    }

    @Test
    void listaLasCuentasDelClienteConLaPorDefectoPrimero() {
        List<Account> found = accounts.findByCustomerId(ANA);

        assertThat(found).hasSize(2).allMatch(a -> a.customerId().equals(ANA));
        assertThat(found.getFirst().isDefault()).isTrue();
        assertThat(found.getFirst().balance()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void noEncuentraLaCuentaDeOtroCliente() {
        assertThat(accounts.findByIdAndCustomerId(ANA_SAVINGS, ANA)).isPresent();
        assertThat(accounts.findByIdAndCustomerId(ANA_SAVINGS, CARLOS)).isEmpty();
    }

    @Test
    void recorrerTodasLasPaginasDevuelveCadaMovimientoUnaVezYEnOrdenDescendente() {
        List<Movement> all = new ArrayList<>();
        MovementCursor cursor = null;
        do {
            List<Movement> page = movements.findPage(ANA_SAVINGS, cursor, 7);
            all.addAll(page);
            cursor = page.size() < 7 ? null : MovementCursor.of(page.getLast());
        } while (cursor != null);

        Integer total = jdbc.queryForObject("SELECT count(*) FROM movements WHERE account_id = ?", Integer.class,
                ANA_SAVINGS);
        assertThat(all).hasSize(total);
        assertThat(new HashSet<>(all.stream().map(Movement::id).toList())).hasSize(total);
        assertThat(all).isSortedAccordingTo(Comparator.comparing(Movement::bookedAt).reversed());
    }

    @Test
    void laCuentaPorDefectoEsUnicaPorCliente() {
        UUID customerId = UUID.randomUUID();
        Account first = accounts.save(Account.openDefault(customerId, accounts.nextAccountNumber(), Instant.now()));

        assertThat(accounts.findDefaultByCustomerId(customerId)).get().extracting(Account::id).isEqualTo(first.id());
        assertThatThrownBy(() -> accounts.save(
                Account.openDefault(customerId, accounts.nextAccountNumber(), Instant.now())))
                .isInstanceOf(DuplicateDefaultAccountException.class);
    }

    @Test
    void losNumerosNuevosNoChocanConLosDeLaSemilla() {
        long first = Long.parseLong(accounts.nextAccountNumber());
        long second = Long.parseLong(accounts.nextAccountNumber());

        assertThat(first).isGreaterThanOrEqualTo(2200100000L);
        assertThat(second).isGreaterThan(first);
    }
}
