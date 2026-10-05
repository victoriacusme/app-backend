package ec.nexo.customer.application.port.out;

import ec.nexo.customer.domain.model.ExperienceComponent;

import java.util.List;

public interface ExperienceRepositoryPort {

    /** Componentes activos de una pantalla; el filtrado fino (segmento, fechas, horario) lo hace el composer. */
    List<ExperienceComponent> findActiveByScreen(String screen);
}
