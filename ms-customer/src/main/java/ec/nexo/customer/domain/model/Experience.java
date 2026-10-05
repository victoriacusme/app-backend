package ec.nexo.customer.domain.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Pantalla compuesta para un cliente: la lista ordenada de componentes que la app debe dibujar. */
public record Experience(String screen, Segment segment, List<Component> components) {

    public record Component(UUID id, String type, Map<String, Object> props) {
    }
}
