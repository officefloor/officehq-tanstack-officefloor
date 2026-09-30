package net.officefloor.hq.app;

import java.util.List;

/**
 * The result of the one global search: the matching clients and the matching projects, kept in
 * separate lists so the front-end shows each kind under its own group. Jackson serialises the
 * getters as the JSON the search box reads. Built by {@link SearchGet}.
 */
public class SearchResults {

    private final List<Client> clients;
    private final List<ProjectView> projects;

    public SearchResults(List<Client> clients, List<ProjectView> projects) {
        this.clients = clients;
        this.projects = projects;
    }

    /** The clients whose name matched the search text (archived clients excluded). */
    public List<Client> getClients() {
        return clients;
    }

    /** The projects whose name matched the search text (archived projects excluded). */
    public List<ProjectView> getProjects() {
        return projects;
    }
}
