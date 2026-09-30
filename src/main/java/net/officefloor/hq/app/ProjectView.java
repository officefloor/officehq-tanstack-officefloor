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

    public ProjectView(Long id, String name, String clientName) {
        this.id = id;
        this.name = name;
        this.clientName = clientName;
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
}
