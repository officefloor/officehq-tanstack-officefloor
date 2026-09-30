package net.officefloor.hq.app;

/**
 * The request body for creating a project: the name and the id of the client it is for (the project
 * id is generated). Bound from the POST JSON body via {@code @RequestBody}.
 */
public class NewProject {

    private String name;

    private Long clientId;

    private String status;

    private String code;

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

    /** The chosen lifecycle status (ACTIVE, ON_HOLD, FINISHED); absent falls back to ACTIVE. */
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /** The short reference code to give the new project; required and unique across projects. */
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
