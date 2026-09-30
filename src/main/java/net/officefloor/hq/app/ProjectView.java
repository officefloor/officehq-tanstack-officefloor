package net.officefloor.hq.app;

/**
 * What the /api/projects routes return: a project plus the NAME of the client it is for, so the UI
 * shows the client's name (not its id) without a second lookup. Built by joining {@link Project}
 * against {@link Client} in the logic classes.
 */
public class ProjectView {

    private final Long id;
    private final String name;
    private final Long clientId;
    private final String clientName;

    public ProjectView(Long id, String name, Long clientId, String clientName) {
        this.id = id;
        this.name = name;
        this.clientId = clientId;
        this.clientName = clientName;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }
}
