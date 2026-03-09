package it.myfamilydoc.webutility.repository;

import it.myfamilydoc.webutility.model.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRoleEntity, Long> {

    List<UserRoleEntity> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    /**
     * Recupera il nome del ruolo principale di un utente.
     * Se un utente ha più ruoli, restituisce quello con priorità più alta
     * (ADMIN > DEV > DOC > USER).
     */
    @Query("SELECT r.name FROM UserRoleEntity ur " +
           "JOIN RoleEntity r ON r.id = ur.roleId " +
           "WHERE ur.userId = :userId " +
           "ORDER BY r.id ASC")
    List<String> findRoleNamesByUserId(Long userId);
}