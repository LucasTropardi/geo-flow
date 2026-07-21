package br.com.lucastropardi.geoflow.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.lucastropardi.geoflow.api.exception.IdGeneratorNotConfiguredException;
import br.com.lucastropardi.geoflow.api.repository.IdGeneratorRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdGeneratorServiceTest {

    @Mock
    private IdGeneratorRepository idGeneratorRepository;

    @Test
    void shouldReturnIncreasingIdsWhenCalledSequentially() {
        when(idGeneratorRepository.findAndLockNextValue("processing_job"))
                .thenReturn(Optional.of(1L), Optional.of(2L), Optional.of(3L));

        IdGeneratorService service = new IdGeneratorService(idGeneratorRepository);

        long first = service.nextId("processing_job");
        long second = service.nextId("processing_job");
        long third = service.nextId("processing_job");

        assertEquals(1L, first);
        assertEquals(2L, second);
        assertEquals(3L, third);

        verify(idGeneratorRepository).updateNextValue(eq("processing_job"), eq(2L), any());
        verify(idGeneratorRepository).updateNextValue(eq("processing_job"), eq(3L), any());
        verify(idGeneratorRepository).updateNextValue(eq("processing_job"), eq(4L), any());
    }

    @Test
    void shouldFailWhenEntityIsMissingFromIdGenerator() {
        when(idGeneratorRepository.findAndLockNextValue("missing_entity"))
                .thenReturn(Optional.empty());

        IdGeneratorService service = new IdGeneratorService(idGeneratorRepository);

        assertThrows(IdGeneratorNotConfiguredException.class, () -> service.nextId("missing_entity"));

        verify(idGeneratorRepository, never()).updateNextValue(eq("missing_entity"), anyLong(), any());
    }
}
