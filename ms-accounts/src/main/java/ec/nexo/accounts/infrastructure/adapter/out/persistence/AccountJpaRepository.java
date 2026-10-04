package ec.nexo.accounts.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {

    List<AccountEntity> findByCustomerIdOrderByIsDefaultDescCreatedAtAsc(UUID customerId);

    Optional<AccountEntity> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<AccountEntity> findByCustomerIdAndIsDefaultTrue(UUID customerId);

    @Query(value = "SELECT nextval('account_number_seq')", nativeQuery = true)
    long nextAccountNumber();
}
