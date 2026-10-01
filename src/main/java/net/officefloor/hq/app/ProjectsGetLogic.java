package net.officefloor.hq.app;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/projects} — list every project with the NAME of the client it is for (not just the
 * id), so the UI can show the join directly. Wired by {@code officefloor/rest/api/projects.GET.yml}.
 */
public class ProjectsGetLogic {

    public void service(ProjectRepository projects, ClientRepository clients,
            ObjectResponse<List<ProjectView>> response) {
        Map<Long, String> nameByClient = clients.findAll().stream()
                .collect(Collectors.toMap(Client::getId, Client::getName));
        List<ProjectView> views = projects.findAll().stream()
                .map(p -> new ProjectView(p.getId(), p.getName(), p.getClientId(),
                        nameByClient.get(p.getClientId()), p.isArchived()))
                .collect(Collectors.toList());
        response.send(views);
    }

    /** A project plus its client's name, as the projects list needs it. */
    public static class ProjectView {
        private final Long id;
        private final String name;
        private final Long clientId;
        private final String clientName;
        private final boolean archived;

        public ProjectView(Long id, String name, Long clientId, String clientName,
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
