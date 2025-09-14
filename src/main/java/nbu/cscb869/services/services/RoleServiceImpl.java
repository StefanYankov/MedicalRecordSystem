//package nbu.cscb869.services.services;
//
//import nbu.cscb869.common.exceptions.EntityNotFoundException;
//import nbu.cscb869.common.exceptions.InvalidDTOException;
//import nbu.cscb869.data.models.identity.Role;
//import nbu.cscb869.data.repositories.identity.RoleRepository;
//import nbu.cscb869.services.data.dtos.identity.RoleCreateDTO;
//import nbu.cscb869.services.data.dtos.identity.RoleUpdateDTO;
//import nbu.cscb869.services.data.dtos.identity.RoleViewDTO;
//import nbu.cscb869.services.services.contracts.RoleService;
//import org.keycloak.admin.client.Keycloak;
//import org.keycloak.representations.idm.RoleRepresentation;
//import org.modelmapper.ModelMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.data.domain.*;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.concurrent.CompletableFuture;
//
///**
// * Implementation of {@link RoleService} for managing role operations with Keycloak integration.
// */
//@Service
//public class RoleServiceImpl implements RoleService {
//    private static final Logger logger = LoggerFactory.getLogger(RoleServiceImpl.class);
//    private static final String ENTITY_NAME = "Role";
//    private static final int MAX_PAGE_SIZE = 100;
//
//    private final RoleRepository roleRepository;
//    private final Keycloak keycloakAdminClient;
//    private final ModelMapper modelMapper;
//
//    public RoleServiceImpl(RoleRepository roleRepository, Keycloak keycloakAdminClient, ModelMapper modelMapper) {
//        this.roleRepository = roleRepository;
//        this.keycloakAdminClient = keycloakAdminClient;
//        this.modelMapper = modelMapper;
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @Transactional
//    @PreAuthorize("hasRole('ADMIN')")
//    public RoleViewDTO create(RoleCreateDTO dto) {
//        if (dto == null) {
//            logger.error("Cannot create {}: DTO is null", ENTITY_NAME);
//            throw new InvalidDTOException("RoleCreateDTO cannot be null");
//        }
//        logger.debug("Creating {} with name: {}", ENTITY_NAME, dto.getName());
//
//        if (roleRepository.findByName(dto.getName()).isPresent()) {
//            throw new InvalidDTOException("Role with name " + dto.getName() + " already exists");
//        }
//
//        // Create role in Keycloak
//        RoleRepresentation roleRep = new RoleRepresentation();
//        roleRep.setName(dto.getName());
//        keycloakAdminClient.realm("medical-realm").roles().create(roleRep);
//
//        Role role = modelMapper.map(dto, Role.class);
//        roleRepository.save(role);
//        logger.info("Created {} with ID: {}", ENTITY_NAME, role.getId());
//        return modelMapper.map(role, RoleViewDTO.class);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @Transactional
//    @PreAuthorize("hasRole('ADMIN')")
//    public RoleViewDTO update(RoleUpdateDTO dto) {
//        if (dto == null || dto.getId() == null) {
//            logger.error("Cannot update {}: DTO or ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("RoleUpdateDTO or ID cannot be null");
//        }
//        logger.debug("Updating {} with ID: {}", ENTITY_NAME, dto.getId());
//
//        Role role = roleRepository.findById(dto.getId())
//                .orElseThrow(() -> new EntityNotFoundException("Role not found with ID: " + dto.getId()));
//
//        if (!role.getName().equals(dto.getName()) && roleRepository.findByName(dto.getName()).isPresent()) {
//            throw new InvalidDTOException("Role with name " + dto.getName() + " already exists");
//        }
//
//        // Update role in Keycloak
//        keycloakAdminClient.realm("medical-realm").roles().get(role.getName()).update(new RoleRepresentation() {{
//            setName(dto.getName());
//        }});
//
//        modelMapper.map(dto, role);
//        roleRepository.save(role);
//        logger.info("Updated {} with ID: {}", ENTITY_NAME, role.getId());
//        return modelMapper.map(role, RoleViewDTO.class);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @Transactional
//    @PreAuthorize("hasRole('ADMIN')")
//    public void delete(Long id) {
//        if (id == null) {
//            logger.error("Cannot delete {}: ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("ID cannot be null");
//        }
//        logger.debug("Deleting {} with ID: {}", ENTITY_NAME, id);
//
//        Role role = roleRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("Role not found with ID: " + id));
//
//        // Delete role from Keycloak
//        keycloakAdminClient.realm("medical-realm").roles().deleteRole(role.getName());
//
//        // Soft delete by setting isDeleted = true
//        role.setIsDeleted(true);
//        roleRepository.save(role);
//        logger.info("Soft deleted {} with ID: {}", ENTITY_NAME, id);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @PreAuthorize("hasRole('ADMIN')")
//    public RoleViewDTO getById(Long id) {
//        if (id == null) {
//            logger.error("Cannot retrieve {}: ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("ID cannot be null");
//        }
//        logger.debug("Retrieving {} with ID: {}", ENTITY_NAME, id);
//
//        Role role = roleRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("Role not found with ID: " + id));
//        logger.info("Retrieved {} with ID: {}", ENTITY_NAME, id);
//        return modelMapper.map(role, RoleViewDTO.class);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @Async
//    @PreAuthorize("hasRole('ADMIN')")
//    public CompletableFuture<Page<RoleViewDTO>> getAll(int page, int size, String orderBy, boolean ascending) {
//        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
//            logger.error("Invalid pagination: page={}, size={}", page, size);
//            throw new InvalidDTOException("Invalid pagination parameters");
//        }
//        logger.debug("Retrieving all {}: page={}, size={}, orderBy={}, ascending={}", ENTITY_NAME, page, size, orderBy, ascending);
//
//        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
//        Pageable pageable = PageRequest.of(page, size, sort);
//        Page<RoleViewDTO> result = roleRepository.findAllActive(pageable)
//                .map(r -> modelMapper.map(r, RoleViewDTO.class));
//        logger.info("Retrieved {} {} for page {}, size {}", result.getTotalElements(), ENTITY_NAME, page, size);
//        return CompletableFuture.completedFuture(result);
//    }
//}