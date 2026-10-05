package ec.nexo.customer.infrastructure.adapter.out.persistence;

import ec.nexo.customer.application.port.out.DeviceTokenRepositoryPort;
import ec.nexo.customer.domain.model.DeviceToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
class DeviceTokenRepositoryAdapter implements DeviceTokenRepositoryPort {

    private final DeviceTokenJpaRepository repository;

    DeviceTokenRepositoryAdapter(DeviceTokenJpaRepository repository) {
        this.repository = repository;
    }

    /** save() con el token como id hace upsert: si el token existía (de este u otro cliente), se reasigna. */
    @Override
    public void register(DeviceToken deviceToken) {
        repository.save(new DeviceTokenEntity(deviceToken.token(), deviceToken.customerId(),
                deviceToken.platform(), Instant.now()));
    }

    @Override
    @Transactional
    public void deleteByTokenAndCustomerId(String token, UUID customerId) {
        repository.deleteByTokenAndCustomerId(token, customerId);
    }

    @Override
    @Transactional
    public void deleteByToken(String token) {
        repository.deleteById(token);
    }

    @Override
    public List<DeviceToken> findByCustomerId(UUID customerId) {
        return repository.findByCustomerId(customerId).stream()
                .map(e -> new DeviceToken(e.getToken(), e.getCustomerId(), e.getPlatform()))
                .toList();
    }
}
