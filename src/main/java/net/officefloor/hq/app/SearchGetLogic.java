package net.officefloor.hq.app;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/search} — the one collection the global search box reads: every client and every
 * project (each project carrying the NAME of its client, like the projects list) in a single
 * payload. The box filters by name on the client, mirroring how the per-section lists filter their
 * own shared collection. Wired by {@code officefloor/rest/api/search.GET.yml}.
 */
public class SearchGetLogic {

    public void service(ClientRepository clients, ProjectRepository projects,
            ObjectResponse<SearchResult> response) {
        Map<Long, String> nameByClient = clients.findAll().stream()
                .collect(Collectors.toMap(Client::getId, Client::getName));
        List<ProjectHit> projectHits = projects.findAll().stream()
                .map(p -> new ProjectHit(p.getId(), p.getName(), p.getClientId(),
                        nameByClient.get(p.getClientId()), p.isArchived()))
                .collect(Collectors.toList());
        response.send(new SearchResult(clients.findAll(), projectHits));
    }

    /** Everything the global search box looks across: the clients and the projects. */
    public static class SearchResult {
        private final List<Client> clients;
        private final List<ProjectHit> projects;

        public SearchResult(List<Client> clients, List<ProjectHit> projects) {
            this.clients = clients;
            this.projects = projects;
        }

        public List<Client> getClients() {
            return clients;
        }

        public List<ProjectHit> getProjects() {
            return projects;
        }
    }

    /** A project as the search box shows it: enough to name it and link to it. */
    public static class ProjectHit {
        private final Long id;
        private final String name;
        private final Long clientId;
        private final String clientName;
        private final boolean archived;

        public ProjectHit(Long id, String name, Long clientId, String clientName,
                boolean archived) {
            this.id = id;
            this.name = name;
            this.clientId = clientId;
            this.clientName = clientName;
            this.archived = archived;
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

        public boolean isArchived() {
            return archived;
        }
    }
}
