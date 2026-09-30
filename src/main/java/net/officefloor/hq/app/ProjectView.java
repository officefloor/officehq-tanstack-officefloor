package net.officefloor.hq.app;

/**
 * A project as the list shows it: its own id and name plus the belonging client's NAME (not id), so
 * the front-end renders the cross-entity join without a second request. Built by the JPQL
 * constructor expression in {@link ProjectRepository#findAllViews()}.
 */
public class ProjectView {

    private final Long id;
    private final String name;
    private final String clientName;
    private final boolean archived;

    public ProjectView(Long id, String name, String clientName, boolean archived) {
        this.id = id;
        this.name = name;
        this.clientName = clientName;
        this.archived = archived;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getClientName() {
        return clientName;
    }

    /** Whether the project has been archived (tucked off the lists but retained). */
    public boolean isArchived() {
        return archived;
    }
}
