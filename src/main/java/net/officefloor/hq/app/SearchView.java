package net.officefloor.hq.app;

import java.util.List;

/**
 * What {@code GET /api/search} returns: the matches for one search term, grouped by kind — the
 * clients whose name matched and the projects whose name matched. One search box looks across both,
 * so both groups come back in a single response. A read-only aggregate joined in {@link SearchGet};
 * no entity of its own. Projects carry their client's NAME ({@link ProjectView}) like the /api/projects list.
 */
public class SearchView {

    private final List<Client> clients;
    private final List<ProjectView> projects;

    public SearchView(List<Client> clients, List<ProjectView> projects) {
        this.clients = clients;
        this.projects = projects;
    }

    public List<Client> getClients() {
        return clients;
    }

    public List<ProjectView> getProjects() {
        return projects;
    }
}
