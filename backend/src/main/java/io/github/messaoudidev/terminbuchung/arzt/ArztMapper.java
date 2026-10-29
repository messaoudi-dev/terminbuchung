package io.github.messaoudidev.terminbuchung.arzt;

import org.springframework.stereotype.Component;

@Component
public class ArztMapper {

    public ArztResponse toResponse(Arzt arzt) {
        return new ArztResponse(arzt.getId(), arzt.getName(), arzt.getFachrichtung());
    }

    public Arzt toEntity(ArztRequest request) {
        return new Arzt(request.name(), request.fachrichtung());
    }
}
