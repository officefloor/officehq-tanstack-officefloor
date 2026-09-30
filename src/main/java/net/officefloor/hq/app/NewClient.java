package net.officefloor.hq.app;

/**
 * The request body for creating a client: just the fields the user supplies (the id is generated).
 * Bound from the POST JSON body via {@code @RequestBody}.
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
