package io.github.messaoudidev.terminbuchung.arzt;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArztService {

    private final ArztRepository arztRepository;
    private final ArztMapper arztMapper;

    public ArztService(ArztRepository arztRepository, ArztMapper arztMapper) {
        this.arztRepository = arztRepository;
        this.arztMapper = arztMapper;
    }

    @Transactional(readOnly = true)
    public List<ArztResponse> findeAlle() {
        return arztRepository.findAll(Sort.by("name")).stream()
                .map(arztMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ArztResponse findeNachId(Long id) {
        return arztMapper.toResponse(ladeArzt(id));
    }

    @Transactional
    public ArztResponse anlegen(ArztRequest request) {
        Arzt arzt = arztRepository.save(arztMapper.toEntity(request));
        return arztMapper.toResponse(arzt);
    }

    // Kein save() nötig: Hibernate schreibt Änderungen an geladenen Entities beim Commit.
    @Transactional
    public ArztResponse aendern(Long id, ArztRequest request) {
        Arzt arzt = ladeArzt(id);
        arzt.aendern(request.name(), request.fachrichtung());
        return arztMapper.toResponse(arzt);
    }

    @Transactional
    public void loeschen(Long id) {
        Arzt arzt = ladeArzt(id);
        arztRepository.delete(arzt);
    }

    private Arzt ladeArzt(Long id) {
        return arztRepository.findById(id)
                .orElseThrow(() -> new ArztNotFoundException(id));
    }
}
