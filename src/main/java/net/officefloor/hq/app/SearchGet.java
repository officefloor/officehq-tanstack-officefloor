package net.officefloor.hq.app;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.web.HttpQueryParameter;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/search?q=... — the one search box that looks across BOTH clients and projects. Returns
 * the matching clients and matching projects together (grouped by kind) so a single box can show
 * both. A match is a case-insensitive substring of the entity's name; archived (tucked-away)
 * clients and projects are excluded, exactly as they are from their own lists. An empty query
 * matches nothing. Wired by {@code officefloor/rest/api/search.GET.yml}.
 */
public class SearchGet {

    public void service(@HttpQueryParameter("q") String q, ClientRepository clients,
            ProjectRepository projects, ObjectResponse<SearchResults> response) {
        String needle = q == null ? "" : q.trim().toLowerCase();

        List<Client> matchedClients = new ArrayList<>();
        List<ProjectView> matchedProjects = new ArrayList<>();
        if (!needle.isEmpty()) {
            for (Client client : clients.findAllByArchivedFalseOrderByIdAsc()) {
                if (client.getName().toLowerCase().contains(needle)) {
                    matchedClients.add(client);
                }
            }
            for (ProjectView project : projects.findAllViews()) {
                if (!project.isArchived() && project.getName().toLowerCase().contains(needle)) {
                    matchedProjects.add(project);
                }
            }
        }
        response.send(new SearchResults(matchedClients, matchedProjects));
    }
}
