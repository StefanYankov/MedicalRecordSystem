//package nbu.cscb869.services.services;
//
//import nbu.cscb869.common.exceptions.EntityNotFoundException;
//import nbu.cscb869.common.exceptions.InvalidDTOException;
//import nbu.cscb869.common.exceptions.InvalidInputException;
//import nbu.cscb869.data.models.identity.User;
//import nbu.cscb869.data.repositories.identity.UserRepository;
//import nbu.cscb869.data.repositories.identity.UserRoleAssignmentRepository;
//import nbu.cscb869.services.data.dtos.identity.RoleViewDTO;
//import nbu.cscb869.services.data.dtos.identity.UserCreateDTO;
//import nbu.cscb869.services.data.dtos.identity.UserUpdateDTO;
//import nbu.cscb869.services.data.dtos.identity.UserViewDTO;
//import nbu.cscb869.services.services.contracts.UserService;
//import org.keycloak.admin.client.Keycloak;
//import org.keycloak.admin.client.resource.UserResource;
//import org.keycloak.representations.idm.UserRepresentation;
//import org.modelmapper.ModelMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.data.domain.*;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.CompletableFuture;
//import java.util.stream.Collectors;
//
///**
// * Implementation of {@link UserService} for managing user operations with Keycloak integration.
// */
//@Service
//public class UserServiceImpl implements UserService {
//    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
//    private static final String ENTITY_NAME = "User";
//    private static final int MAX_PAGE_SIZE = 100;
//
//    private final UserRepository userRepository;
//    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
//    private final Keycloak keycloakAdminClient;
//    private final ModelMapper modelMapper;
//
//    public UserServiceImpl(UserRepository userRepository, UserRoleAssignmentRepository userRoleAssignmentRepository,
//                           Keycloak keycloakAdminClient, ModelMapper modelMapper) {
//        this.userRepository = userRepository;
//        this.userRoleAssignmentRepository = userRoleAssignmentRepository;
//        this.keycloakAdminClient = keycloakAdminClient;
//        this.modelMapper = modelMapper;
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @Transactional
//    @PreAuthorize("hasRole('ADMIN')")
//    public UserViewDTO create(UserCreateDTO dto) {
//        if (dto == null) {
//            logger.error("Cannot create {}: DTO is null", ENTITY_NAME);
//            throw new InvalidDTOException("UserCreateDTO cannot be null");
//        }
//        logger.debug("Creating {} with Keycloak ID: {}", ENTITY_NAME, dto.getKeycloakId());
//
//        if (userRepository.findByKeycloakId(dto.getKeycloakId()).isPresent()) {
//            throw new InvalidInputException("User with Keycloak ID " + dto.getKeycloakId() + " already exists");
//        }
//        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
//            throw new InvalidInputException("User with email " + dto.getEmail() + " already exists");
//        }
//
//        // Create user in Keycloak
//        UserRepresentation userRep = new UserRepresentation();
//        userRep.setUsername(dto.getEmail());
//        userRep.setEmail(dto.getEmail());
//        userRep.setFirstName(dto.getName());
//        userRep.setEnabled(true);
//        var response = keycloakAdminClient.realm("medical-realm").users().create(userRep);
//        if (response.getStatus() != 201) {
//            throw new InvalidInputException("Failed to create user in Keycloak");
//        }
//
//        User user = modelMapper.map(dto, User.class);
//        userRepository.save(user);
//        logger.info("Created {} with ID: {}", ENTITY_NAME, user.getId());
//        return mapToViewDTO(user);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @Transactional
//    @PreAuthorize("hasRole('ADMIN')")
//    public UserViewDTO update(UserUpdateDTO dto) {
//        if (dto == null || dto.getId() == null) {
//            logger.error("Cannot update {}: DTO or ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("UserUpdateDTO or ID cannot be null");
//        }
//        logger.debug("Updating {} with ID: {}", ENTITY_NAME, dto.getId());
//
//        User user = userRepository.findById(dto.getId())
//                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + dto.getId()));
//
//        if (!user.getKeycloakId().equals(dto.getKeycloakId()) && userRepository.findByKeycloakId(dto.getKeycloakId()).isPresent()) {
//            throw new InvalidInputException("Keycloak ID " + dto.getKeycloakId() + " is already in use");
//        }
//        if (!user.getEmail().equals(dto.getEmail()) && userRepository.findByEmail(dto.getEmail()).isPresent()) {
//            throw new InvalidInputException("Email " + dto.getEmail() + " is already in use");
//        }
//
//        // Update user in Keycloak
//        UserResource userResource = keycloakAdminClient.realm("medical-realm").users().get(user.getKeycloakId());
//        UserRepresentation userRep = userResource.toRepresentation();
//        userRep.setEmail(dto.getEmail());
//        userRep.setFirstName(dto.getName());
//        userResource.update(userRep);
//
//        modelMapper.map(dto, user);
//        userRepository.save(user);
//        logger.info("Updated {} with ID: {}", ENTITY_NAME, user.getId());
//        return mapToViewDTO(user);
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
//        User user = userRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));
//
//        // Delete user from Keycloak
//        keycloakAdminClient.realm("medical-realm").users().delete(user.getKeycloakId());
//
//        // Soft delete by setting isDeleted = true
//        user.setIsDeleted(true);
//        userRepository.save(user);
//        logger.info("Soft deleted {} with ID: {}", ENTITY_NAME, id);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
//    public UserViewDTO getById(Long id) {
//        if (id == null) {
//            logger.error("Cannot retrieve {}: ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("ID cannot be null");
//        }
//        logger.debug("Retrieving {} with ID: {}", ENTITY_NAME, id);
//
//        User user = userRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));
//        logger.info("Retrieved {} with ID: {}", ENTITY_NAME, id);
//        return mapToViewDTO(user);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
//    public UserViewDTO getByKeycloakId(String keycloakId) {
//        if (keycloakId == null) {
//            logger.error("Cannot retrieve {}: Keycloak ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("Keycloak ID cannot be null");
//        }
//        logger.debug("Retrieving {} with Keycloak ID: {}", ENTITY_NAME, keycloakId);
//
//        User user = userRepository.findByKeycloakId(keycloakId)
//                .orElseThrow(() -> new EntityNotFoundException("User not found with Keycloak ID: " + keycloakId));
//        logger.info("Retrieved {} with Keycloak ID: {}", ENTITY_NAME, keycloakId);
//        return mapToViewDTO(user);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @Async
//    @PreAuthorize("hasRole('ADMIN')")
//    public CompletableFuture<Page<UserViewDTO>> getAll(int page, int size, String orderBy, boolean ascending) {
//        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
//            logger.error("Invalid pagination: page={}, size={}", page, size);
//            throw new InvalidDTOException("Invalid pagination parameters");
//        }
//        logger.debug("Retrieving all {}: page={}, size={}, orderBy={}, ascending={}", ENTITY_NAME, page, size, orderBy, ascending);
//
//        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
//        Pageable pageable = PageRequest.of(page, size, sort);
//        Page<UserViewDTO> result = userRepository.findAllActive(pageable)
//                .map(this::mapToViewDTO);
//        logger.info("Retrieved {} {} for page {}, size {}", result.getTotalElements(), ENTITY_NAME, page, size);
//        return CompletableFuture.completedFuture(result);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @Transactional
//    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
//    public void switchRole(String keycloakId, String newRole) {
//        if ("ADMIN".equals(newRole)) {
//            logger.error("Switching to ADMIN role is not allowed for Keycloak ID: {}", keycloakId);
//            throw new InvalidInputException("Switching to ADMIN role is not allowed");
//        }
//        logger.debug("Switching role to {} for Keycloak ID: {}", newRole, keycloakId);
//
//        User user = userRepository.findByKeycloakId(keycloakId)
//                .orElseThrow(() -> new EntityNotFoundException("User not found with Keycloak ID: " + keycloakId));
//
//        if (userRoleAssignmentRepository.findByUserAndRoleName(user, newRole).isEmpty()) {
//            throw new InvalidInputException("User does not have role: " + newRole);
//        }
//
//        UserResource userResource = keycloakAdminClient.realm("medical-realm").users().get(keycloakId);
//        UserRepresentation userRep = userResource.toRepresentation();
//        Map<String, java.util.List<String>> attributes = userRep.getAttributes() != null
//                ? userRep.getAttributes()
//                : new HashMap<>();
//        attributes.put("activeRole", java.util.List.of(newRole));
//        userRep.setAttributes(attributes);
//        userResource.update(userRep);
//        logger.info("Switched role to {} for Keycloak ID: {}", newRole, keycloakId);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public String getCurrentUserId(Jwt jwt) {
//        if (jwt == null) {
//            logger.error("JWT is null");
//            throw new InvalidInputException("JWT cannot be null");
//        }
//        String userId = jwt.getSubject();
//        logger.debug("Retrieved current user ID: {}", userId);
//        return userId;
//    }
//
//    private UserViewDTO mapToViewDTO(User user) {
//        UserViewDTO viewDTO = modelMapper.map(user, UserViewDTO.class);
//        Set<RoleViewDTO> roles = userRoleAssignmentRepository.findByUser(user).stream()
//                .map(assignment -> modelMapper.map(assignment.getRole(), RoleViewDTO.class))
//                .collect(Collectors.toSet());
//        viewDTO.setRoles(roles);
//        return viewDTO;
//    }
//}