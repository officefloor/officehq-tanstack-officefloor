package net.officefloor.hq.app;

/** The request body for creating a project: the name typed and the id of the chosen client. */
public class ProjectForm {

    private String name;
    private Long clientId;
    private String status;

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
}
