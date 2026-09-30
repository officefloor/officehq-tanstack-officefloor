package net.officefloor.hq.app;

/**
 * The request body for creating a project: the name and the id of the client it is for (the project
 * id is generated). Bound from the POST JSON body via {@code @RequestBody}.
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
