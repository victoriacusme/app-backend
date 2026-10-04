package ec.nexo.accounts.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface MovementJpaRepository extends JpaRepository<MovementEntity, UUID> {

    @Query(value = """
            SELECT * FROM movements
            WHERE account_id = :accountId
            ORDER BY booked_at DESC, id DESC
            LIMIT :limit""", nativeQuery = true)
    List<MovementEntity> findFirstPage(UUID accountId, int limit);

    // Comparación de filas (booked_at, id) < (...): usa el índice idx_movements_account_booked.
    @Query(value = """
            SELECT * FROM movements
            WHERE account_id = :accountId AND (booked_at, id) < (:bookedAt, :id)
            ORDER BY booked_at DESC, id DESC
            LIMIT :limit""", nativeQuery = true)
    List<MovementEntity> findPageAfter(UUID accountId, Instant bookedAt, UUID id, int limit);
}
