package net.officefloor.hq.app;

/**
 * Request body for {@code POST /api/clients}: the fields the user supplies when adding a client.
 * Bound from the JSON payload by the Spring MVC {@code @RequestBody} argument resolver.
 */
public class NewClient {

    private String name;

    private String email;

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
