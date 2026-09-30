package net.officefloor.hq.app;

/**
 * The request body for creating a contact: the name, email and role, and the id of the client it is
 * for (the contact id is generated). Bound from the POST JSON body via {@code @RequestBody}.
 */
public class NewContact {

    private String name;

    private String email;

    private String role;

    private Long clientId;

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

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }
}
