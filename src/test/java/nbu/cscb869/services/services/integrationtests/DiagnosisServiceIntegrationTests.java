package nbu.cscb869.services.services.integrationtests;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import nbu.cscb869.services.data.dtos.DiagnosisCreateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisUpdateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisViewDTO;
import nbu.cscb869.services.services.contracts.DiagnosisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DiagnosisServiceIntegrationTests {
    @Autowired
    private DiagnosisService diagnosisService;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        entityManager.createNativeQuery("DELETE FROM diagnoses").executeUpdate();
        entityManager.flush();
    }

    private DiagnosisCreateDTO createDiagnosisCreateDTO(String name, String description) {
        return DiagnosisCreateDTO.builder()
                .name(name)
                .description(description)
                .build();
    }

    private DiagnosisUpdateDTO createDiagnosisUpdateDTO(Long id, String name, String description) {
        return DiagnosisUpdateDTO.builder()
                .id(id)
                .name(name)
                .description(description)
                .build();
    }

    // Happy Path
    @Test
    void Create_ValidDTO_SavesSuccessfully() {
        DiagnosisCreateDTO dto = createDiagnosisCreateDTO("Flu", "Viral infection");
        DiagnosisViewDTO result = diagnosisService.create(dto);
        entityManager.flush();

        assertNotNull(result.getId());
        assertEquals("Flu", result.getName());
        assertEquals("Viral infection", result.getDescription());

        Diagnosis saved = diagnosisRepository.findById(result.getId()).orElseThrow();
        assertFalse(saved.getIsDeleted());
        assertNotNull(saved.getCreatedOn());
        assertNotNull(saved.getVersion());
    }

    @Test
    void Update_ValidDTO_UpdatesSuccessfully() {
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setName("Asthma");
        diagnosis.setDescription("Respiratory condition");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();

        DiagnosisUpdateDTO dto = createDiagnosisUpdateDTO(diagnosis.getId(), "Updated Asthma", "Updated condition");
        DiagnosisViewDTO result = diagnosisService.update(dto);
        entityManager.flush();

        assertEquals(diagnosis.getId(), result.getId());
        assertEquals("Updated Asthma", result.getName());
        assertEquals("Updated condition", result.getDescription());

        Diagnosis updated = diagnosisRepository.findById(diagnosis.getId()).orElseThrow();
        assertEquals(1L, updated.getVersion());
        assertNotNull(updated.getModifiedOn());
    }

    @Test
    void Delete_ExistingId_SoftDeletesSuccessfully() {
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setName("Bronchitis");
        diagnosis.setDescription("Lung inflammation");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();

        diagnosisService.delete(diagnosis.getId());
        entityManager.flush();

        Diagnosis deleted = diagnosisRepository.findById(diagnosis.getId()).orElseThrow();
        assertTrue(deleted.getIsDeleted());
        assertNotNull(deleted.getDeletedOn());
        assertEquals(0, diagnosisRepository.findAllActive().size());
    }

    @Test
    void GetById_ExistingId_ReturnsDiagnosis() {
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setName("Migraine");
        diagnosis.setDescription("Severe headache");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();

        DiagnosisViewDTO result = diagnosisService.getById(diagnosis.getId());

        assertEquals(diagnosis.getId(), result.getId());
        assertEquals("Migraine", result.getName());
        assertEquals("Severe headache", result.getDescription());
    }

    @Test
    void GetAll_ValidParameters_ReturnsPaged() {
        Diagnosis diagnosis1 = new Diagnosis();
        diagnosis1.setName("Pneumonia");
        diagnosis1.setDescription("Lung infection");
        Diagnosis diagnosis2 = new Diagnosis();
        diagnosis2.setName("Pneumonitis");
        diagnosis2.setDescription("Lung inflammation");
        diagnosisRepository.saveAndFlush(diagnosis1);
        diagnosisRepository.saveAndFlush(diagnosis2);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        Page<DiagnosisViewDTO> result = diagnosisService.getAll(0, 2, "name", true, "Pneum").join();

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().anyMatch(d -> d.getName().equals("Pneumonia")));
        assertTrue(result.getContent().stream().anyMatch(d -> d.getName().equals("Pneumonitis")));
    }

    // Error Cases
    @Test
    void Create_NullDTO_ThrowsInvalidDTOException() {
        Exception exception = null;
        try {
            diagnosisService.create(null);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(InvalidDTOException.class, exception);
    }

    @Test
    void Create_BlankName_ThrowsConstraintViolationException() {
        DiagnosisCreateDTO dto = createDiagnosisCreateDTO("", "Viral infection");
        Exception exception = null;
        try {
            diagnosisService.create(dto);
            entityManager.flush();
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(ConstraintViolationException.class, exception);
    }

    @Test
    void Create_DuplicateName_ThrowsDataIntegrityViolationException() {
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setName("Flu");
        diagnosis.setDescription("Viral infection");
        diagnosisRepository.save(diagnosis);
        entityManager.flush();

        DiagnosisCreateDTO dto = createDiagnosisCreateDTO("Flu", "Another infection");
        Exception exception = null;
        try {
            diagnosisService.create(dto);
            entityManager.flush();
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(DataIntegrityViolationException.class, exception);
    }

    @Test
    void Update_NullDTO_ThrowsInvalidDTOException() {
        Exception exception = null;
        try {
            diagnosisService.update(null);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(InvalidDTOException.class, exception);
    }

    @Test
    void Update_NullId_ThrowsInvalidDTOException() {
        DiagnosisUpdateDTO dto = createDiagnosisUpdateDTO(null, "Flu", "Viral infection");
        Exception exception = null;
        try {
            diagnosisService.update(dto);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(InvalidDTOException.class, exception);
    }

    @Test
    void Update_BlankName_ThrowsConstraintViolationException() {
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setName("Asthma");
        diagnosis.setDescription("Respiratory condition");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();

        DiagnosisUpdateDTO dto = createDiagnosisUpdateDTO(diagnosis.getId(), "", "Viral infection");
        Exception exception = null;
        try {
            diagnosisService.update(dto);
            entityManager.flush();
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(ConstraintViolationException.class, exception);
    }

    @Test
    void Update_NonExistentId_ThrowsEntityNotFoundException() {
        DiagnosisUpdateDTO dto = createDiagnosisUpdateDTO(999L, "Flu", "Viral infection");
        Exception exception = null;
        try {
            diagnosisService.update(dto);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(EntityNotFoundException.class, exception);
    }

    @Test
    void Delete_NullId_ThrowsInvalidDTOException() {
        Exception exception = null;
        try {
            diagnosisService.delete(null);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(InvalidDTOException.class, exception);
    }

    @Test
    void Delete_NonExistentId_ThrowsEntityNotFoundException() {
        Exception exception = null;
        try {
            diagnosisService.delete(999L);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(EntityNotFoundException.class, exception);
    }

    @Test
    void GetById_NullId_ThrowsInvalidDTOException() {
        Exception exception = null;
        try {
            diagnosisService.getById(null);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(InvalidDTOException.class, exception);
    }

    @Test
    void GetById_NonExistentId_ThrowsEntityNotFoundException() {
        Exception exception = null;
        try {
            diagnosisService.getById(999L);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(EntityNotFoundException.class, exception);
    }

    @Test
    void GetAll_NegativePage_ThrowsInvalidDTOException() {
        Exception exception = null;
        try {
            diagnosisService.getAll(-1, 10, "name", true, null).join();
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(CompletionException.class, exception);
        assertNotNull(exception.getCause());
        assertInstanceOf(InvalidDTOException.class, exception.getCause());
    }

    @Test
    void GetAll_ZeroPageSize_ThrowsInvalidDTOException() {
        Exception exception = null;
        try {
            diagnosisService.getAll(0, 0, "name", true, null).join();
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(CompletionException.class, exception);
        assertNotNull(exception.getCause());
        assertInstanceOf(InvalidDTOException.class, exception.getCause());
    }

    @Test
    void GetAll_ExcessivePageSize_ThrowsInvalidDTOException() {
        Exception exception = null;
        try {
            diagnosisService.getAll(0, 101, "name", true, null).join();
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(CompletionException.class, exception);
        assertNotNull(exception.getCause());
        assertInstanceOf(InvalidDTOException.class, exception.getCause());
    }

    // Edge Cases
    @Test
    void Create_MaximumNameLength_SavesSuccessfully() {
        String maxName = "A".repeat(100);
        DiagnosisCreateDTO dto = createDiagnosisCreateDTO(maxName, "Description");
        DiagnosisViewDTO result = diagnosisService.create(dto);
        entityManager.flush();

        assertEquals(maxName, result.getName());
        assertEquals("Description", result.getDescription());
    }

    @Test
    void Create_MaximumDescriptionLength_SavesSuccessfully() {
        String maxDescription = "D".repeat(500);
        DiagnosisCreateDTO dto = createDiagnosisCreateDTO("Flu", maxDescription);
        DiagnosisViewDTO result = diagnosisService.create(dto);
        entityManager.flush();

        assertEquals("Flu", result.getName());
        assertEquals(maxDescription, result.getDescription());
    }

    @Test
    void Update_SameName_UpdatesSuccessfully() {
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setName("Asthma");
        diagnosis.setDescription("Respiratory condition");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();

        DiagnosisUpdateDTO dto = createDiagnosisUpdateDTO(diagnosis.getId(), "Asthma", "Updated condition");
        DiagnosisViewDTO result = diagnosisService.update(dto);
        entityManager.flush();

        assertEquals("Asthma", result.getName());
        assertEquals("Updated condition", result.getDescription());
    }

    @Test
    void GetAll_EmptyFilter_ReturnsAll() {
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setName("Migraine");
        diagnosis.setDescription("Severe headache");
        diagnosisRepository.saveAndFlush(diagnosis);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        Page<DiagnosisViewDTO> result = diagnosisService.getAll(0, 10, "name", true, "").join();

        assertEquals(1, result.getTotalElements());
        assertEquals("Migraine", result.getContent().get(0).getName());
    }

    @Test
    void GetAll_NonExistentFilter_ReturnsEmpty() {
        Page<DiagnosisViewDTO> result = diagnosisService.getAll(0, 10, "name", true, "Nonexistent").join();

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void GetAll_LastPageFewerElements_ReturnsCorrectPage() {
        Diagnosis diagnosis1 = new Diagnosis();
        diagnosis1.setName("Pneumonia");
        Diagnosis diagnosis2 = new Diagnosis();
        diagnosis2.setName("Pneumonitis");
        Diagnosis diagnosis3 = new Diagnosis();
        diagnosis3.setName("Pneumothorax");
        diagnosisRepository.saveAndFlush(diagnosis1);
        diagnosisRepository.saveAndFlush(diagnosis2);
        diagnosisRepository.saveAndFlush(diagnosis3);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        Page<DiagnosisViewDTO> result = diagnosisService.getAll(1, 2, "name", true, "Pneum").join();

        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(2, result.getTotalPages());
        assertEquals("Pneumothorax", result.getContent().get(0).getName());
    }
}