package net.officefloor.hq.app;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.web.HttpQueryParameter;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/search?q=...} — one search box across both clients and projects. Returns the
 * clients and the projects whose name contains {@code q} (case-insensitive), grouped by kind in a
 * {@link SearchView}. Archived rows are tucked away, so they drop off the search on both sides.
 * Wired by {@code officefloor/rest/api/search.GET.yml}.
 *
 * <p>When {@code q} is absent or blank there is nothing to search for, so both groups come back
 * empty rather than the whole database.
 */
public class SearchGet {

    public void service(@HttpQueryParameter("q") String q, ClientRepository clients,
            ProjectRepository projects, ObjectResponse<SearchView> response) {
        String term = q == null ? "" : q.trim();
        if (term.isEmpty()) {
            response.send(new SearchView(List.of(), List.of()));
            return;
        }

        List<Client> matchedClients =
                clients.findByArchivedFalseAndNameContainingIgnoreCaseOrderByIdAsc(term);

        // Projects carry their client's NAME (like /api/projects), so join against the client names.
        Map<Long, String> nameById = clients.findAll().stream()
                .collect(Collectors.toMap(Client::getId, Client::getName));
        List<ProjectView> matchedProjects =
                projects.findByArchivedFalseAndNameContainingIgnoreCaseOrderByIdAsc(term).stream()
                        .map(p -> new ProjectView(p.getId(), p.getName(), p.getClientId(),
                                nameById.get(p.getClientId()), p.isArchived(), p.getStatus(),
                                p.getCode()))
                        .collect(Collectors.toList());

        response.send(new SearchView(matchedClients, matchedProjects));
    }
}
