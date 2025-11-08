package nbu.cscb869.services.services.unittests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.data.dto.YearMonthSickLeaveCountDTO;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.SickLeave;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.SickLeaveRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.services.data.dtos.SickLeaveCreateDTO;
import nbu.cscb869.services.data.dtos.SickLeaveUpdateDTO;
import nbu.cscb869.services.data.dtos.SickLeaveViewDTO;
import nbu.cscb869.services.services.SickLeaveServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SickLeaveServiceImplUnitTests {

    @Mock
    private SickLeaveRepository sickLeaveRepository;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private SickLeaveServiceImpl sickLeaveService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Create Tests")
    class CreateTests {
        @Test
        void create_WithValidData_ShouldSucceed_HappyPath() {
            SickLeaveCreateDTO createDTO = new SickLeaveCreateDTO();
            createDTO.setVisitId(1L);
            createDTO.setStartDate(LocalDate.now());
            createDTO.setDurationDays(5);

            Visit visit = new Visit();
            SickLeave savedSickLeave = new SickLeave();

            when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));
            when(sickLeaveRepository.save(any(SickLeave.class))).thenReturn(savedSickLeave);
            when(modelMapper.map(savedSickLeave, SickLeaveViewDTO.class)).thenReturn(new SickLeaveViewDTO());

            SickLeaveViewDTO result = sickLeaveService.create(createDTO);

            assertNotNull(result);
            verify(sickLeaveRepository).save(any(SickLeave.class));
        }

        @Test
        void create_WithNullDto_ShouldThrowInvalidDTOException_ErrorCase() {
            assertThrows(InvalidDTOException.class, () -> sickLeaveService.create(null));
        }

        @Test
        void create_WithNonExistentVisit_ShouldThrowEntityNotFoundException_ErrorCase() {
            SickLeaveCreateDTO createDTO = new SickLeaveCreateDTO();
            createDTO.setVisitId(99L);
            when(visitRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> sickLeaveService.create(createDTO));
        }
    }

    @Nested
    @DisplayName("Update Tests")
    class UpdateTests {
        @Test
        void update_WithValidData_ShouldSucceed_HappyPath() {
            SickLeaveUpdateDTO updateDTO = new SickLeaveUpdateDTO();
            updateDTO.setId(10L);
            updateDTO.setVisitId(1L);
            updateDTO.setStartDate(LocalDate.now().plusDays(1));
            updateDTO.setDurationDays(10);

            SickLeave existingSickLeave = new SickLeave();
            Visit visit = new Visit();

            when(sickLeaveRepository.findById(10L)).thenReturn(Optional.of(existingSickLeave));
            when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));
            when(sickLeaveRepository.save(any(SickLeave.class))).thenReturn(existingSickLeave);
            when(modelMapper.map(existingSickLeave, SickLeaveViewDTO.class)).thenReturn(new SickLeaveViewDTO());

            SickLeaveViewDTO result = sickLeaveService.update(updateDTO);

            assertNotNull(result);
            assertEquals(10, existingSickLeave.getDurationDays());
            verify(sickLeaveRepository).save(existingSickLeave);
        }

        @Test
        void update_WithNullDto_ShouldThrowInvalidDTOException_ErrorCase() {
            assertThrows(InvalidDTOException.class, () -> sickLeaveService.update(null));
        }

        @Test
        void update_WithNullId_ShouldThrowInvalidDTOException_ErrorCase() {
            assertThrows(InvalidDTOException.class, () -> sickLeaveService.update(new SickLeaveUpdateDTO()));
        }

        @Test
        void update_WithNonExistentSickLeave_ShouldThrowEntityNotFoundException_ErrorCase() {
            SickLeaveUpdateDTO updateDTO = new SickLeaveUpdateDTO();
            updateDTO.setId(99L);
            when(sickLeaveRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> sickLeaveService.update(updateDTO));
        }

        @Test
        void update_WithNonExistentVisit_ShouldThrowEntityNotFoundException_ErrorCase() {
            SickLeaveUpdateDTO updateDTO = new SickLeaveUpdateDTO();
            updateDTO.setId(10L);
            updateDTO.setVisitId(99L);

            when(sickLeaveRepository.findById(10L)).thenReturn(Optional.of(new SickLeave()));
            when(visitRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> sickLeaveService.update(updateDTO));
        }
    }

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {
        @Test
        void delete_WithExistingId_ShouldSucceed_HappyPath() {
            SickLeave sickLeave = new SickLeave();
            Visit visit = new Visit();
            sickLeave.setVisit(visit);

            when(sickLeaveRepository.findById(1L)).thenReturn(Optional.of(sickLeave));

            sickLeaveService.delete(1L);

            verify(visitRepository).save(visit);
            verify(sickLeaveRepository).delete(sickLeave);
            assertNull(visit.getSickLeave());
        }

        @Test
        void delete_WithNullId_ShouldThrowInvalidDTOException_ErrorCase() {
            assertThrows(InvalidDTOException.class, () -> sickLeaveService.delete(null));
        }

        @Test
        void delete_WithNonExistentId_ShouldThrowEntityNotFoundException_ErrorCase() {
            when(sickLeaveRepository.findById(99L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> sickLeaveService.delete(99L));
        }
    }

    @Nested
    @DisplayName("GetById Tests")
    class GetByIdTests {
        @Test
        void getById_AsPatientOwner_ShouldSucceed_HappyPath() {
            String patientKeycloakId = "patient-owner";
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(patientKeycloakId, "pass", List.of(new SimpleGrantedAuthority("ROLE_PATIENT")))
            );

            Patient patient = new Patient();
            patient.setKeycloakId(patientKeycloakId);
            Visit visit = new Visit();
            visit.setPatient(patient);
            SickLeave sickLeave = new SickLeave();
            sickLeave.setVisit(visit);

            when(sickLeaveRepository.findById(1L)).thenReturn(Optional.of(sickLeave));
            when(modelMapper.map(sickLeave, SickLeaveViewDTO.class)).thenReturn(new SickLeaveViewDTO());

            SickLeaveViewDTO result = sickLeaveService.getById(1L);

            assertNotNull(result);
        }

        @Test
        void getById_AsOtherPatient_ShouldThrowAccessDeniedException_ErrorCase() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("other-patient", "pass", List.of(new SimpleGrantedAuthority("ROLE_PATIENT")))
            );

            Patient patient = new Patient();
            patient.setKeycloakId("patient-owner"); // Different ID
            Visit visit = new Visit();
            visit.setPatient(patient);
            SickLeave sickLeave = new SickLeave();
            sickLeave.setVisit(visit);

            when(sickLeaveRepository.findById(1L)).thenReturn(Optional.of(sickLeave));

            assertThrows(AccessDeniedException.class, () -> sickLeaveService.getById(1L));
        }

        @Test
        void getById_WithNonExistentId_ShouldThrowEntityNotFoundException_ErrorCase() {
            when(sickLeaveRepository.findById(99L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> sickLeaveService.getById(99L));
        }

        @Test
        void getById_WithNullId_ShouldThrowInvalidDTOException_ErrorCase() {
            assertThrows(InvalidDTOException.class, () -> sickLeaveService.getById(null));
        }
    }

    @Nested
    @DisplayName("GetAll Tests")
    class GetAllTests {
        @Test
        void getAll_WithValidPagination_ShouldReturnPage_HappyPath() {
            Page<SickLeave> sickLeavePage = new PageImpl<>(List.of(new SickLeave()));
            when(sickLeaveRepository.findAll(any(Pageable.class))).thenReturn(sickLeavePage);
            when(modelMapper.map(any(SickLeave.class), eq(SickLeaveViewDTO.class))).thenReturn(new SickLeaveViewDTO());

            CompletableFuture<Page<SickLeaveViewDTO>> future = sickLeaveService.getAll(0, 10, "startDate", true);
            Page<SickLeaveViewDTO> result = future.join();

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(sickLeaveRepository).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Reporting Tests")
    class ReportingTests {
        @Test
        void getMonthsWithMostSickLeaves_ShouldReturnListOfDTOs_HappyPath() {
            List<YearMonthSickLeaveCountDTO> expectedList = List.of(
                    YearMonthSickLeaveCountDTO.builder().year(2023).month(5).count(10).build()
            );
            when(sickLeaveRepository.findYearMonthWithMostSickLeaves()).thenReturn(expectedList);

            List<YearMonthSickLeaveCountDTO> result = sickLeaveService.getMonthsWithMostSickLeaves();

            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            assertEquals(2023, result.get(0).getYear());
            verify(sickLeaveRepository).findYearMonthWithMostSickLeaves();
        }

        @Test
        void getMonthsWithMostSickLeaves_WhenNoData_ShouldReturnEmptyList_EdgeCase() {
            when(sickLeaveRepository.findYearMonthWithMostSickLeaves()).thenReturn(Collections.emptyList());

            List<YearMonthSickLeaveCountDTO> result = sickLeaveService.getMonthsWithMostSickLeaves();

            assertTrue(result.isEmpty());
        }
    }
}
