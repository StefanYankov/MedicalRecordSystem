package nbu.cscb869.config;

import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Specialty;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Collectors;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE);

        // Doctor to DoctorViewDTO: Map specialties to specialtyNames
        modelMapper.addMappings(new PropertyMap<Doctor, DoctorViewDTO>() {
            @Override
            protected void configure() {
                map().setSpecialtyNames(source.getSpecialties().stream()
                        .map(Specialty::getName)
                        .collect(Collectors.toList()));
            }
        });

        // Patient to PatientViewDTO: Map generalPractitioner to name and ID
        modelMapper.addMappings(new PropertyMap<Patient, PatientViewDTO>() {
            @Override
            protected void configure() {
                map().setGeneralPractitionerName(source.getGeneralPractitioner().getName());
                map().setGeneralPractitionerId(source.getGeneralPractitioner().getId());
            }
        });

        // Visit to VisitViewDTO: Map patient, doctor, diagnosis to names and IDs
        modelMapper.addMappings(new PropertyMap<Visit, VisitViewDTO>() {
            @Override
            protected void configure() {
                map().setPatientName(source.getPatient().getName());
                map().setPatientId(source.getPatient().getId());
                map().setDoctorName(source.getDoctor().getName());
                map().setDoctorId(source.getDoctor().getId());
                map().setDiagnosisName(source.getDiagnosis().getName());
                map().setDiagnosisId(source.getDiagnosis().getId());
            }
        });

        return modelMapper;
    }
}