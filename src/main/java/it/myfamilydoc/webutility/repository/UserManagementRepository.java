package it.myfamilydoc.webutility.repository;

import it.myfamilydoc.webutility.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserManagementRepository extends JpaRepository<UserEntity, Long> {

    /**
     * Ricerca utenti per username, email o ID (case-insensitive).
     */
    @Query("SELECT u FROM UserEntity u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "CAST(u.id AS string) LIKE CONCAT('%', :search, '%')")
    List<UserEntity> searchUsers(String search);

    /**
     * Tutti gli utenti ordinati per data creazione (più recenti prima).
     */
    List<UserEntity> findAllByOrderByCreatedAtDesc();

    // ── Conteggi per statistiche ──────────────────────────────────

    long countByEnabledTrueAndDeletedAtIsNull();

    long countByEnabledFalseAndDeletedAtIsNull();

    long countByDeletedAtIsNotNull();
}