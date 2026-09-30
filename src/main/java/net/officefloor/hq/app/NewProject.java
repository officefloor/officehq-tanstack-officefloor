package net.officefloor.hq.app;

/**
 * Request body for {@code POST /api/projects}: the fields the user supplies when adding a project —
 * its name and the id of the client it is for. Bound from the JSON payload by the Spring MVC
 * {@code @RequestBody} argument resolver.
 */
public class NewProject {

    private String name;

    private Long clientId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }
}
