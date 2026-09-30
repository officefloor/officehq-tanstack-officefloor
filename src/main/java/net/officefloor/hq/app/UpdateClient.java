package net.officefloor.hq.app;

/**
 * Request body for {@code POST /api/clients/update}: the id of the client to correct plus the new
 * name and email. Bound from the JSON payload by the Spring MVC {@code @RequestBody} argument
 * resolver.
 */
public class UpdateClient {

    private Long id;

    private String name;

    private String email;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
