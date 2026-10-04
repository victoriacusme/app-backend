package ec.nexo.customer.infrastructure.adapter.in.web.dto;

import ec.nexo.customer.domain.model.Experience;
import ec.nexo.customer.domain.model.Segment;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Contrato SDUI: la app recorre {@code components} en orden y dibuja cada {@code type} que conozca. */
public record ExperienceResponse(String screen, Segment segment, List<ComponentResponse> components) {

    public static ExperienceResponse from(Experience experience) {
        return new ExperienceResponse(experience.screen(), experience.segment(), experience.components().stream()
                .map(c -> new ComponentResponse(c.id(), c.type(), c.props()))
                .toList());
    }

    public record ComponentResponse(UUID id, String type, Map<String, Object> props) {
    }
}
