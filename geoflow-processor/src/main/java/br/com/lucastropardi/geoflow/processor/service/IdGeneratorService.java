package br.com.lucastropardi.geoflow.processor.service;

import br.com.lucastropardi.geoflow.processor.exception.IdGeneratorNotConfiguredException;
import br.com.lucastropardi.geoflow.processor.repository.IdGeneratorRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdGeneratorService {

    private final IdGeneratorRepository idGeneratorRepository;
    private final Clock applicationClock;

    public IdGeneratorService(IdGeneratorRepository idGeneratorRepository, Clock applicationClock) {
        this.idGeneratorRepository = idGeneratorRepository;
        this.applicationClock = applicationClock;
    }

    @Transactional
    public long nextId(String entityName) {
        long currentValue = idGeneratorRepository.findAndLockNextValue(entityName)
                .orElseThrow(() -> new IdGeneratorNotConfiguredException(entityName));

        idGeneratorRepository.updateNextValue(entityName, currentValue + 1, OffsetDateTime.now(applicationClock));
        return currentValue;
    }
}
