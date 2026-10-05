package ec.nexo.accounts.application.usecase;

import ec.nexo.accounts.application.usecase.TransferBetweenOwnAccountsUseCase.Result;
import ec.nexo.accounts.application.usecase.TransferBetweenOwnAccountsUseCase.TransferCommand;
import ec.nexo.accounts.domain.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El caso de uso real (transacción, bloqueos FOR UPDATE, índices únicos) contra PostgreSQL, con solicitudes
 * lanzadas en paralelo para reproducir lo que pasa cuando la app reintenta o el usuario toca dos veces.
 */
@SpringBootTest
@Testcontainers
class TransferConcurrencyIT {

    private static final int THREADS = 8;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TransferBetweenOwnAccountsUseCase useCase;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void enParaleloNuncaSeGastaMasDelSaldoDisponible() throws Exception {
        UUID customer = UUID.randomUUID();
        UUID source = createAccount(customer, "100.00");
        UUID target = createAccount(customer, "0.00");

        List<Outcome> outcomes = runConcurrently(30, i -> new TransferCommand(customer, "debit-" + i, source, target,
                new BigDecimal("10.00"), null));

        assertThat(outcomes.stream().filter(Outcome::succeeded)).hasSize(10);
        assertThat(outcomes.stream().filter(o -> o.error() instanceof InsufficientFundsException)).hasSize(20);
        assertThat(balance(source)).isEqualByComparingTo("0.00");
        assertThat(balance(target)).isEqualByComparingTo("100.00");
        assertThat(count("SELECT count(*) FROM movements WHERE transfer_id IS NOT NULL AND account_id IN (?, ?)",
                source, target)).isEqualTo(20);
        assertLastMovementMatchesBalance(source);
        assertLastMovementMatchesBalance(target);
    }

    @Test
    void transferenciasCruzadasSimultaneasNoProducenDeadlock() throws Exception {
        UUID customer = UUID.randomUUID();
        UUID a = createAccount(customer, "1000.00");
        UUID b = createAccount(customer, "1000.00");

        List<Outcome> outcomes = runConcurrently(40, i -> i % 2 == 0
                ? new TransferCommand(customer, "cross-" + i, a, b, new BigDecimal("3.00"), null)
                : new TransferCommand(customer, "cross-" + i, b, a, new BigDecimal("1.00"), null));

        assertThat(outcomes).allSatisfy(o -> assertThat(o.error()).isNull());
        // 20 envíos de 3.00 de A a B y 20 de 1.00 de B a A: el total del cliente no cambia.
        assertThat(balance(a)).isEqualByComparingTo("960.00");
        assertThat(balance(b)).isEqualByComparingTo("1040.00");
    }

    @Test
    void laMismaClaveEnviadaVariasVecesALaVezCreaUnaSolaTransferencia() throws Exception {
        UUID customer = UUID.randomUUID();
        UUID source = createAccount(customer, "50.00");
        UUID target = createAccount(customer, "0.00");

        List<Outcome> outcomes = runConcurrently(10, i -> new TransferCommand(customer, "doble-toque", source, target,
                new BigDecimal("50.00"), "Pago"));

        assertThat(outcomes).allSatisfy(o -> assertThat(o.error()).isNull());
        assertThat(outcomes.stream().map(o -> o.result().transfer().id()).distinct()).hasSize(1);
        assertThat(outcomes.stream().filter(o -> !o.result().replayed())).hasSize(1);
        assertThat(count("SELECT count(*) FROM transfers WHERE customer_id = ?", customer)).isEqualTo(1);
        assertThat(balance(source)).isEqualByComparingTo("0.00");
        assertThat(balance(target)).isEqualByComparingTo("50.00");
    }

    private interface CommandFactory {
        TransferCommand create(int index);
    }

    private record Outcome(Result result, Throwable error) {
        boolean succeeded() {
            return error == null;
        }
    }

    private List<Outcome> runConcurrently(int requests, CommandFactory factory) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Outcome>> futures = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            TransferCommand command = factory.create(i);
            Callable<Outcome> task = () -> {
                start.await();
                try {
                    return new Outcome(useCase.execute(command), null);
                } catch (RuntimeException e) {
                    return new Outcome(null, e);
                }
            };
            futures.add(executor.submit(task));
        }
        start.countDown();
        List<Outcome> outcomes = new ArrayList<>();
        for (Future<Outcome> future : futures) {
            outcomes.add(future.get(30, TimeUnit.SECONDS));
        }
        executor.shutdown();
        return outcomes;
    }

    private UUID createAccount(UUID customer, String balance) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO accounts (id, customer_id, number, type, currency, balance, status, is_default)
                VALUES (?, ?, nextval('account_number_seq')::text, 'SAVINGS', 'USD', ?, 'ACTIVE', FALSE)""",
                id, customer, new BigDecimal(balance));
        return id;
    }

    private BigDecimal balance(UUID account) {
        return jdbc.queryForObject("SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, account);
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    private void assertLastMovementMatchesBalance(UUID account) {
        BigDecimal last = jdbc.queryForObject("""
                SELECT balance_after FROM movements WHERE account_id = ?
                ORDER BY booked_at DESC, id DESC LIMIT 1""", BigDecimal.class, account);
        assertThat(last).isEqualByComparingTo(balance(account));
    }
}
