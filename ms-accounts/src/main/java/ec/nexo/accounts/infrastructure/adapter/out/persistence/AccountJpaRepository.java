package ec.nexo.accounts.infrastructure.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {

    List<AccountEntity> findByCustomerIdOrderByIsDefaultDescCreatedAtAsc(UUID customerId);

    Optional<AccountEntity> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<AccountEntity> findByCustomerIdAndIsDefaultTrue(UUID customerId);

    @Query(value = "SELECT nextval('account_number_seq')", nativeQuery = true)
    long nextAccountNumber();

    // ORDER BY id: PostgreSQL bloquea las filas en el orden en que las devuelve, siempre el mismo.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountEntity a WHERE a.id IN :ids ORDER BY a.id")
    List<AccountEntity> findAllByIdInForUpdate(Collection<UUID> ids);
}
