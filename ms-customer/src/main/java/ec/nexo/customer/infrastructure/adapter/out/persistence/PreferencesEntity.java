package ec.nexo.customer.infrastructure.adapter.out.persistence;

import ec.nexo.customer.domain.model.Theme;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "preferences")
class PreferencesEntity {

    @Id
    @Column(name = "customer_id")
    private UUID customerId;

    @Column(nullable = false, length = 2)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Theme theme;

    @Column(name = "notifications_enabled", nullable = false)
    private boolean notificationsEnabled;

    @Column(name = "show_promotions", nullable = false)
    private boolean showPromotions;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PreferencesEntity() {
    }

    PreferencesEntity(UUID customerId, String language, Theme theme, boolean notificationsEnabled,
                      boolean showPromotions, Instant updatedAt) {
        this.customerId = customerId;
        this.language = language;
        this.theme = theme;
        this.notificationsEnabled = notificationsEnabled;
        this.showPromotions = showPromotions;
        this.updatedAt = updatedAt;
    }

    UUID getCustomerId() {
        return customerId;
    }

    String getLanguage() {
        return language;
    }

    Theme getTheme() {
        return theme;
    }

    boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    boolean isShowPromotions() {
        return showPromotions;
    }
}
