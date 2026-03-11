package it.myfamilydoc.webutility.service;

import it.myfamilydoc.webutility.dto.UserManagementDto;
import it.myfamilydoc.webutility.dto.UserStatsDto;
import it.myfamilydoc.webutility.model.RoleEntity;
import it.myfamilydoc.webutility.model.UserEntity;
import it.myfamilydoc.webutility.model.UserRoleEntity;
import it.myfamilydoc.webutility.repository.RoleRepository;
import it.myfamilydoc.webutility.repository.UserManagementRepository;
import it.myfamilydoc.webutility.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserManagementService {

    private static final Logger log = LoggerFactory.getLogger(UserManagementService.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Priorità dei ruoli: il primo trovato nella lista è il "principale"
    private static final List<String> ROLE_PRIORITY = List.of("ADMIN", "DEV", "DOC", "USER");

    private final UserManagementRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    public UserManagementService(UserManagementRepository userRepository,
                                  UserRoleRepository userRoleRepository,
                                  RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
    }

    // ══════════════════════════════════════════════════════════════
    // READ
    // ══════════════════════════════════════════════════════════════

    /**
     * Recupera tutti gli utenti con filtri opzionali.
     * I filtri vengono applicati in-memory per semplicità dato che il volume
     * di utenti è gestibile. Per scale maggiori, usare Specification JPA.
     */
    public List<UserManagementDto> getUsers(String search, String role, String status, String emailVerified) {
        log.info("Recupero utenti con filtri - search: {}, role: {}, status: {}, emailVerified: {}",
                search, role, status, emailVerified);

        // Precarica la mappa userId -> roleName per tutti gli utenti
        List<UserEntity> allUsers = userRepository.findAllByOrderByCreatedAtDesc();
        Map<Long, String> roleMap = buildUserRoleMap(
                allUsers.stream().map(UserEntity::getId).collect(Collectors.toList())
        );

        return allUsers.stream()
                .map(user -> toDto(user, roleMap.getOrDefault(user.getId(), "USER")))
                .filter(dto -> matchesSearch(dto, search))
                .filter(dto -> matchesRole(dto, role))
                .filter(dto -> matchesStatus(dto, status))
                .filter(dto -> matchesEmailVerified(dto, emailVerified))
                .collect(Collectors.toList());
    }

    /**
     * Statistiche utenti.
     */
    public UserStatsDto getUserStats() {
        long total = userRepository.count();
        long active = userRepository.countByEnabledTrueAndDeletedAtIsNull();
        long inactive = userRepository.countByEnabledFalseAndDeletedAtIsNull();
        long deleted = userRepository.countByDeletedAtIsNotNull();

        return new UserStatsDto(total, active, inactive, deleted);
    }

    // ══════════════════════════════════════════════════════════════
    // WRITE
    // ══════════════════════════════════════════════════════════════

    /**
     * Abilita un utente.
     */
    @Transactional
    public void enableUser(Long userId) {
        UserEntity user = findUserOrThrow(userId);
        user.setEnabled(true);
        userRepository.save(user);
        log.info("Utente {} (ID: {}) abilitato", user.getUsername(), userId);
    }

    /**
     * Disabilita un utente.
     */
    @Transactional
    public void disableUser(Long userId) {
        UserEntity user = findUserOrThrow(userId);
        user.setEnabled(false);
        userRepository.save(user);
        log.info("Utente {} (ID: {}) disabilitato", user.getUsername(), userId);
    }

    /**
     * Verifica forzata dell'email (admin override).
     */
    @Transactional
    public void verifyEmail(Long userId) {
        UserEntity user = findUserOrThrow(userId);
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Email verificata manualmente per utente {} (ID: {})", user.getUsername(), userId);
    }

    /**
     * Avvia il reset della password per un utente.
     * Genera un token di reset e imposta la scadenza.
     * In un'architettura a microservizi, qui si potrebbe chiamare l'AuthService via HTTP.
     * Per ora gestiamo direttamente il flag nel DB e l'admin informa l'utente.
     */
    @Transactional
    public void resetPassword(Long userId) {
        UserEntity user = findUserOrThrow(userId);
        // Imposta un token di reset che scade in 24h
        // Il token reale andrebbe generato e hashato come nel AuthService
        user.setResetTokenExpiryDate(LocalDateTime.now().plusHours(24));
        userRepository.save(user);
        log.info("Reset password avviato per utente {} (ID: {})", user.getUsername(), userId);
        // TODO: Integrare con il servizio email per inviare il link di reset
        //       oppure chiamare POST /auth/forgot-password con l'email dell'utente
    }

    /**
     * Cambia il ruolo di un utente.
     * Rimuove tutti i ruoli esistenti e assegna il nuovo ruolo.
     */
    @Transactional
    public void changeRole(Long userId, String newRoleName) {
        UserEntity user = findUserOrThrow(userId);

        // Trova il ruolo nel DB
        // Il ruolo nel DB potrebbe essere salvato con o senza prefisso ROLE_
        String roleToSearch = newRoleName.startsWith("ROLE_") ? newRoleName : newRoleName;
        RoleEntity role = roleRepository.findByName(roleToSearch)
                .or(() -> roleRepository.findByName("ROLE_" + newRoleName))
                .orElseThrow(() -> new IllegalArgumentException("Ruolo non trovato: " + newRoleName));

        // Rimuovi tutti i ruoli esistenti
        userRoleRepository.deleteByUserId(userId);

        // Assegna il nuovo ruolo
        UserRoleEntity userRole = new UserRoleEntity(userId, role.getId());
        userRoleRepository.save(userRole);

        log.info("Ruolo cambiato per utente {} (ID: {}) -> {}", user.getUsername(), userId, newRoleName);
    }

    /**
     * Soft-delete di un utente.
     * Non elimina fisicamente il record (GDPR: conservazione per audit).
     */
    @Transactional
    public void deleteUser(Long userId, String reason) {
        UserEntity user = findUserOrThrow(userId);
        user.setDeletedAt(LocalDateTime.now());
        user.setDeletionReason(reason != null ? reason : "Eliminato dall'amministratore");
        user.setEnabled(false);
        userRepository.save(user);
        log.info("Utente {} (ID: {}) eliminato (soft-delete). Motivo: {}", 
                user.getUsername(), userId, reason);
    }

    /**
     * Ripristina un utente precedentemente eliminato (soft-delete).
     * Rimuove il flag deleted_at e riabilita l'account.
     */
    @Transactional
    public void restoreUser(Long userId) {
        UserEntity user = findUserOrThrow(userId);

        if (user.getDeletedAt() == null) {
            throw new IllegalStateException("L'utente non è eliminato, non può essere ripristinato");
        }
        if (Boolean.TRUE.equals(user.getAnonymized())) {
            throw new IllegalStateException("L'utente è stato anonimizzato e non può essere ripristinato");
        }

        user.setDeletedAt(null);
        user.setDeletionReason(null);
        user.setEnabled(true);
        userRepository.save(user);
        log.info("Utente {} (ID: {}) ripristinato con successo", user.getUsername(), userId);
    }

    /**
     * Anonimizza definitivamente un utente (GDPR Art. 17 - Diritto all'oblio).
     * 
     * Sovrascrive tutti i dati personali con valori anonimi.
     * Il record resta nel DB per integrità referenziale e audit,
     * ma non è più riconducibile alla persona fisica.
     * 
     * ATTENZIONE: Questa operazione è IRREVERSIBILE.
     */
    @Transactional
    public void anonymizeUser(Long userId) {
        UserEntity user = findUserOrThrow(userId);

        if (user.getDeletedAt() == null) {
            throw new IllegalStateException("L'utente deve essere prima eliminato per poter essere anonimizzato");
        }
        if (Boolean.TRUE.equals(user.getAnonymized())) {
            throw new IllegalStateException("L'utente è già stato anonimizzato");
        }

        String anonId = "ANON_" + userId;

        // Sovrascrive dati personali
        user.setUsername(anonId);
        user.setEmail(anonId + "@anonymized.local");
        user.setPasswordHash("ANONYMIZED");
        user.setAnonymized(true);
        user.setEnabled(false);

        // Rimuove token e dati sensibili residui
        user.setResetTokenHash(null);
        user.setResetTokenExpiryDate(null);
        user.setLastLoginIp(null);

        // Preserva: id, createdAt, deletedAt, deletionReason (per audit trail)

        userRepository.save(user);
        log.info("Utente ID: {} anonimizzato definitivamente (GDPR Art. 17)", userId);
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════

    private UserEntity findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato con ID: " + userId));
    }

    /**
     * Costruisce una mappa userId -> roleName principale per una lista di utenti.
     */
    private Map<Long, String> buildUserRoleMap(List<Long> userIds) {
        return userIds.stream().collect(Collectors.toMap(
                userId -> userId,
                userId -> {
                    List<String> roles = userRoleRepository.findRoleNamesByUserId(userId);
                    return determinePrimaryRole(roles);
                }
        ));
    }

    /**
     * Determina il ruolo principale tra una lista di ruoli.
     * Rimuove il prefisso ROLE_ se presente e applica la priorità.
     */
    private String determinePrimaryRole(List<String> roles) {
        if (roles == null || roles.isEmpty()) return "USER";

        List<String> normalizedRoles = roles.stream()
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .collect(Collectors.toList());

        // Restituisce il ruolo con priorità più alta
        for (String priority : ROLE_PRIORITY) {
            if (normalizedRoles.contains(priority)) {
                return priority;
            }
        }

        return normalizedRoles.get(0);
    }

    /**
     * Converte UserEntity + ruolo in DTO.
     */
    private UserManagementDto toDto(UserEntity user, String role) {
        UserManagementDto dto = new UserManagementDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(role);
        dto.setEnabled(user.isEnabled());
        dto.setEmailVerified(Boolean.TRUE.equals(user.getEmailVerified()));
        dto.setAnonymized(Boolean.TRUE.equals(user.getAnonymized()));
        dto.setLastLoginAt(user.getLastLoginAt() != null ? user.getLastLoginAt().format(ISO_FORMATTER) : null);
        dto.setLastLoginIp(user.getLastLoginIp() != null ? user.getLastLoginIp().getHostAddress() : null);
        dto.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().format(ISO_FORMATTER) : null);
        dto.setDeletedAt(user.getDeletedAt() != null ? user.getDeletedAt().format(ISO_FORMATTER) : null);
        return dto;
    }

    // ── Filtri ───────────────────────────────────────────────────

    private boolean matchesSearch(UserManagementDto dto, String search) {
        if (search == null || search.isBlank()) return true;
        String q = search.toLowerCase();
        return dto.getUsername().toLowerCase().contains(q)
                || dto.getEmail().toLowerCase().contains(q)
                || dto.getId().toString().contains(q);
    }

    private boolean matchesRole(UserManagementDto dto, String role) {
        if (role == null || "ALL".equalsIgnoreCase(role)) return true;
        return role.equalsIgnoreCase(dto.getRole());
    }

    private boolean matchesStatus(UserManagementDto dto, String status) {
        if (status == null || "ALL".equalsIgnoreCase(status)) return true;
        return switch (status.toLowerCase()) {
            case "enabled" -> dto.isEnabled() && dto.getDeletedAt() == null;
            case "disabled" -> !dto.isEnabled() && dto.getDeletedAt() == null;
            case "deleted" -> dto.getDeletedAt() != null;
            default -> true;
        };
    }

    private boolean matchesEmailVerified(UserManagementDto dto, String emailVerified) {
        if (emailVerified == null || "ALL".equalsIgnoreCase(emailVerified)) return true;
        return switch (emailVerified.toLowerCase()) {
            case "true", "verified" -> dto.isEmailVerified();
            case "false", "unverified" -> !dto.isEmailVerified();
            default -> true;
        };
    }
}