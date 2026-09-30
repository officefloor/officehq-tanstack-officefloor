package net.officefloor.hq.app;

/**
 * Request body for {@code POST /api/projects}: the fields the user supplies when adding a project —
 * its name and the id of the client it is for. Bound from the JSON payload by the Spring MVC
 * {@code @RequestBody} argument resolver.
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
