package it.myfamilydoc.webutility.service;

import it.myfamilydoc.webutility.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service("userAuthorizationService")
public class UserAuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(UserAuthorizationService.class);

    /**
     * Verifica se l'utente corrente può accedere ai dati dell'ID richiesto
     */
    public boolean canAccessUserData(Long requestedUserId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            logger.warn("No valid authentication found");
            return false;
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Collection<? extends GrantedAuthority> authorities = userPrincipal.getAuthorities();

        // ROLE_ADMIN: Accesso completo a tutto
        if (hasRole(authorities, "ROLE_ADMIN")) {
            logger.debug("Admin user {} accessing user data for ID: {}", 
                        userPrincipal.getUsername(), requestedUserId);
            return true;
        }

        // ROLE_DEV: Accesso completo per sviluppo/debug (solo in sviluppo!)
        if (hasRole(authorities, "ROLE_DEV")) {
            logger.debug("Dev user {} accessing user data for ID: {}", 
                        userPrincipal.getUsername(), requestedUserId);
            return true; // In produzione, potresti voler limitare questo
        }

        // ROLE_DOC: Può vedere pazienti associati + propri dati
        if (hasRole(authorities, "ROLE_DOC")) {
            // Il dottore può vedere i propri dati
            if (userPrincipal.getId().equals(requestedUserId)) {
                return true;
            }
            
            // TODO: Implementa logica per verificare se il paziente è associato al dottore
            // return isDoctorPatientRelationship(userPrincipal.getId(), requestedUserId);
            
            logger.debug("Doctor {} attempting to access patient data for ID: {}", 
                        userPrincipal.getUsername(), requestedUserId);
            
            // Per ora, consenti accesso (implementa la logica paziente-dottore dopo)
            return true;
        }

        // ROLE_USER: Solo propri dati
        if (hasRole(authorities, "ROLE_USER")) {
            boolean canAccess = userPrincipal.getId().equals(requestedUserId);
            
            if (!canAccess) {
                logger.warn("User {} (ID: {}) attempted to access data for user ID: {}", 
                           userPrincipal.getUsername(), userPrincipal.getId(), requestedUserId);
            }
            
            return canAccess;
        }

        // Nessun ruolo riconosciuto
        logger.warn("User {} has no recognized role: {}", 
                   userPrincipal.getUsername(), authorities);
        return false;
    }

    /**
     * Verifica se l'utente ha un ruolo specifico
     */
    private boolean hasRole(Collection<? extends GrantedAuthority> authorities, String roleName) {
        return authorities.stream()
                .anyMatch(auth -> roleName.equals(auth.getAuthority()));
    }

    /**
     * TODO: Implementa la verifica relazione dottore-paziente
     */
    private boolean isDoctorPatientRelationship(Long doctorId, Long patientId) {
        // Query per verificare se il dottore può accedere ai dati del paziente
        // Esempio:
        // SELECT COUNT(*) FROM doctor_patient_relationships 
        // WHERE doctor_id = ? AND patient_id = ?
        
        return false; // Implementa questa logica
    }
}

/*
ROLE_ADMIN:
✅ Propri dati
✅ Dati di tutti gli utenti  
✅ Funzioni amministrative
✅ Gestione utenti e ruoli

ROLE_DEV:
✅ Propri dati
✅ Dati di tutti gli utenti (solo in sviluppo)
✅ Endpoint di debug
❌ Funzioni amministrative in produzione

ROLE_DOC:
✅ Propri dati
✅ Dati dei pazienti associati
❌ Dati di altri dottori
❌ Dati di utenti non pazienti

ROLE_USER:
✅ Solo propri dati
❌ Dati di altri utenti
❌ Funzioni privilegiate
*/