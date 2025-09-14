//package nbu.cscb869.services.services;
//
//import nbu.cscb869.common.exceptions.EntityNotFoundException;
//import nbu.cscb869.common.exceptions.InvalidDTOException;
//import nbu.cscb869.data.models.identity.Role;
//import nbu.cscb869.data.models.identity.User;
//import nbu.cscb869.data.models.identity.UserRoleAssignment;
//import nbu.cscb869.data.repositories.identity.RoleRepository;
//import nbu.cscb869.data.repositories.identity.UserRepository;
//import nbu.cscb869.data.repositories.identity.UserRoleAssignmentRepository;
//import nbu.cscb869.services.data.dtos.identity.UserRoleAssignmentCreateDTO;
//import nbu.cscb869.services.data.dtos.identity.UserRoleAssignmentUpdateDTO;
//import nbu.cscb869.services.data.dtos.identity.UserRoleAssignmentViewDTO;
//import nbu.cscb869.services.services.contracts.UserRoleAssignmentService;
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
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//
///**
// * Implementation of {@link UserRoleAssignmentService} for managing user role assignments with Keycloak integration.
// */
//@Service
//public class UserRoleAssignmentServiceImpl implements UserRoleAssignmentService {
//    private static final Logger logger = LoggerFactory.getLogger(UserRoleAssignmentServiceImpl.class);
//    private static final String ENTITY_NAME = "UserRoleAssignment";
//    private static final int MAX_PAGE_SIZE = 100;
//
//    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
//    private final UserRepository userRepository;
//    private final RoleRepository roleRepository;
//    private final Keycloak keycloakAdminClient;
//    private final ModelMapper modelMapper;
//
//    public UserRoleAssignmentServiceImpl(UserRoleAssignmentRepository userRoleAssignmentRepository,
//                                         UserRepository userRepository, RoleRepository roleRepository,
//                                         Keycloak keycloakAdminClient, ModelMapper modelMapper) {
//        this.userRoleAssignmentRepository = userRoleAssignmentRepository;
//        this.userRepository = userRepository;
//        this.roleRepository = roleRepository;
//        this.keycloakAdminClient = keycloakAdminClient;
//        this.modelMapper = modelMapper;
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @Transactional
//    @PreAuthorize("hasRole('ADMIN')")
//    public UserRoleAssignmentViewDTO create(UserRoleAssignmentCreateDTO dto) {
//        if (dto == null) {
//            logger.error("Cannot create {}: DTO is null", ENTITY_NAME);
//            throw new InvalidDTOException("UserRoleAssignmentCreateDTO cannot be null");
//        }
//        logger.debug("Creating {} for user ID: {}, role ID: {}", ENTITY_NAME, dto.getUserId(), dto.getRoleId());
//
//        User user = userRepository.findById(dto.getUserId())
//                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + dto.getUserId()));
//        Role role = roleRepository.findById(dto.getRoleId())
//                .orElseThrow(() -> new EntityNotFoundException("Role not found with ID: " + dto.getRoleId()));
//
//        if (userRoleAssignmentRepository.findByUserAndRoleAndEntityTypeAndEntityId(user, role, dto.getEntityType(), dto.getEntityId()).isPresent()) {
//            throw new InvalidDTOException("Role assignment already exists");
//        }
//
//        // Assign role in Keycloak
//        RoleRepresentation roleRep = keycloakAdminClient.realm("medical-realm").roles().get(role.getName()).toRepresentation();
//        keycloakAdminClient.realm("medical-realm").users().get(user.getKeycloakId()).roles().realmLevel().add(List.of(roleRep));
//
//        UserRoleAssignment assignment = modelMapper.map(dto, UserRoleAssignment.class);
//        userRoleAssignmentRepository.save(assignment);
//        logger.info("Created {} with ID: {}", ENTITY_NAME, assignment.getId());
//        return modelMapper.map(assignment, UserRoleAssignmentViewDTO.class);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @Transactional
//    @PreAuthorize("hasRole('ADMIN')")
//    public UserRoleAssignmentViewDTO update(UserRoleAssignmentUpdateDTO dto) {
//        if (dto == null || dto.getId() == null) {
//            logger.error("Cannot update {}: DTO or ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("UserRoleAssignmentUpdateDTO or ID cannot be null");
//        }
//        logger.debug("Updating {} with ID: {}", ENTITY_NAME, dto.getId());
//
//        UserRoleAssignment assignment = userRoleAssignmentRepository.findById(dto.getId())
//                .orElseThrow(() -> new EntityNotFoundException("UserRoleAssignment not found with ID: " + dto.getId()));
//        User user = userRepository.findById(dto.getUserId())
//                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + dto.getUserId()));
//        Role role = roleRepository.findById(dto.getRoleId())
//                .orElseThrow(() -> new EntityNotFoundException("Role not found with ID: " + dto.getRoleId()));
//
//        if (!assignment.getUser().equals(user) || !assignment.getRole().equals(role) ||
//                !assignment.getEntityType().equals(dto.getEntityType()) || !assignment.getEntityId().equals(dto.getEntityId())) {
//            if (userRoleAssignmentRepository.findByUserAndRoleAndEntityTypeAndEntityId(user, role, dto.getEntityType(), dto.getEntityId()).isPresent()) {
//                throw new InvalidDTOException("Role assignment already exists");
//            }
//        }
//
//        // Update role assignment in Keycloak
//        RoleRepresentation roleRep = keycloakAdminClient.realm("medical-realm").roles().get(role.getName()).toRepresentation();
//        keycloakAdminClient.realm("medical-realm").users().get(user.getKeycloakId()).roles().realmLevel().add(List.of(roleRep));
//
//        modelMapper.map(dto, assignment);
//        userRoleAssignmentRepository.save(assignment);
//        logger.info("Updated {} with ID: {}", ENTITY_NAME, assignment.getId());
//        return modelMapper.map(assignment, UserRoleAssignmentViewDTO.class);
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
//        UserRoleAssignment assignment = userRoleAssignmentRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("UserRoleAssignment not found with ID: " + id));
//
//        // Remove role from Keycloak
//        RoleRepresentation roleRep = keycloakAdminClient.realm("medical-realm").roles().get(assignment.getRole().getName()).toRepresentation();
//        keycloakAdminClient.realm("medical-realm").users().get(assignment.getUser().getKeycloakId()).roles().realmLevel().remove(List.of(roleRep));
//
//        // Soft delete by setting isDeleted = true
//        assignment.setIsDeleted(true);
//        userRoleAssignmentRepository.save(assignment);
//        logger.info("Soft deleted {} with ID: {}", ENTITY_NAME, id);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @PreAuthorize("hasRole('ADMIN')")
//    public UserRoleAssignmentViewDTO getById(Long id) {
//        if (id == null) {
//            logger.error("Cannot retrieve {}: ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("ID cannot be null");
//        }
//        logger.debug("Retrieving {} with ID: {}", ENTITY_NAME, id);
//
//        UserRoleAssignment assignment = userRoleAssignmentRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("UserRoleAssignment not found with ID: " + id));
//        logger.info("Retrieved {} with ID: {}", ENTITY_NAME, id);
//        return modelMapper.map(assignment, UserRoleAssignmentViewDTO.class);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @Async
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
//    public CompletableFuture<Page<UserRoleAssignmentViewDTO>> getAllByUser(Long userId, int page, int size, String orderBy, boolean ascending) {
//        if (userId == null || page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
//            logger.error("Invalid parameters: userId={}, page={}, size={}", userId, page, size);
//            throw new InvalidDTOException("Invalid parameters");
//        }
//        logger.debug("Retrieving {} for user ID: {}, page={}, size={}, orderBy={}, ascending={}", ENTITY_NAME, userId, page, size, orderBy, ascending);
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
//        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
//        Pageable pageable = PageRequest.of(page, size, sort);
//        Page<UserRoleAssignmentViewDTO> result = userRoleAssignmentRepository.findByUser(user, pageable)
//                .map(a -> modelMapper.map(a, UserRoleAssignmentViewDTO.class));
//        logger.info("Retrieved {} {} for user ID: {}", result.getTotalElements(), ENTITY_NAME, userId);
//        return CompletableFuture.completedFuture(result);
//    }
//}