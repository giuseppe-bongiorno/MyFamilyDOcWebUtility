package it.myfamilydoc.webutility.model;

import jakarta.persistence.*;

/**
 * Entity per la tabella auth_schema.roles.
 */
@Entity
@Table(name = "roles", schema = "auth_schema")
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String name;

    public RoleEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}