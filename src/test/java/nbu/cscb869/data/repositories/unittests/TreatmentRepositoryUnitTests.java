package nbu.cscb869.data.repositories.unittests;

import nbu.cscb869.data.models.Treatment;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.TreatmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreatmentRepositoryUnitTests {

    @Mock
    private TreatmentRepository treatmentRepository;

    private Treatment createTreatment(String description, Visit visit) {
        return Treatment.builder()
                .description(description)
                .visit(visit)
                .build();
    }

    private Visit createVisit() {
        return Visit.builder().build();
    }

    @Test
    void findAll_WithData_ReturnsPaged_HappyPath() {
        Visit visit = createVisit();
        Treatment treatment = createTreatment("Antibiotic therapy", visit);
        Page<Treatment> page = new PageImpl<>(List.of(treatment));
        when(treatmentRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<Treatment> result = treatmentRepository.findAll(PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals("Antibiotic therapy", result.getContent().getFirst().getDescription());
        verify(treatmentRepository).findAll(any(Pageable.class));
    }

    @Test
    void findAll_NoData_ReturnsEmptyPage_ErrorCase() {
        Page<Treatment> emptyPage = new PageImpl<>(List.of());
        when(treatmentRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        Page<Treatment> result = treatmentRepository.findAll(PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(treatmentRepository).findAll(any(Pageable.class));
    }

    @Test
    void findAll_WithLargePageSize_EdgeCase() {
        Visit visit = createVisit();
        List<Treatment> treatments = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            treatments.add(createTreatment("Treatment" + i, visit));
        }
        Page<Treatment> page = new PageImpl<>(treatments);
        when(treatmentRepository.findAll(eq(PageRequest.of(0, 10)))).thenReturn(page);

        Page<Treatment> result = treatmentRepository.findAll(PageRequest.of(0, 10));

        assertEquals(5, result.getTotalElements());
        assertEquals(5, result.getContent().size());
        verify(treatmentRepository).findAll(eq(PageRequest.of(0, 10)));
    }
}