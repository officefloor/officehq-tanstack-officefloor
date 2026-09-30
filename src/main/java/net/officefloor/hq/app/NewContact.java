package net.officefloor.hq.app;

/**
 * Request body for {@code POST /api/contacts}: the fields the user supplies when adding a contact —
 * the id of the client they belong to, plus their name, email and role. Bound from the JSON payload
 * by the Spring MVC {@code @RequestBody} argument resolver.
 */
public class NewContact {

    private Long clientId;

    private String name;

    private String email;

    private String role;

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
