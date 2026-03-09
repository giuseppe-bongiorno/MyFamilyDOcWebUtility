package it.myfamilydoc.webutility.dto;

/**
 * DTO per le statistiche utenti nella pagina User Management.
 * Mappa i campi: total, active, inactive, deleted
 */
public class UserStatsDto {

    private long total;
    private long active;
    private long inactive;
    private long deleted;

    public UserStatsDto() {}

    public UserStatsDto(long total, long active, long inactive, long deleted) {
        this.total = total;
        this.active = active;
        this.inactive = inactive;
        this.deleted = deleted;
    }

    // ── Getters & Setters ────────────────────────────────────────

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public long getActive() { return active; }
    public void setActive(long active) { this.active = active; }

    public long getInactive() { return inactive; }
    public void setInactive(long inactive) { this.inactive = inactive; }

    public long getDeleted() { return deleted; }
    public void setDeleted(long deleted) { this.deleted = deleted; }
}