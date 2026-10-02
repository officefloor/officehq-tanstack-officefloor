package net.officefloor.hq.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/projects} — list every project with the NAME of the client it is for (not just the
 * id), plus the ids of the tags it carries so the list can be filtered by label. Wired by
 * {@code officefloor/rest/api/projects.GET.yml}.
 */
public class ProjectsGetLogic {

    public void service(ProjectRepository projects, ClientRepository clients,
            ProjectTagRepository projectTags, ObjectResponse<List<ProjectView>> response) {
        Map<Long, String> nameByClient = clients.findAll().stream()
                .collect(Collectors.toMap(Client::getId, Client::getName));
        Map<Long, List<Long>> tagsByProject = projectTags.findAll().stream()
                .collect(Collectors.groupingBy(ProjectTag::getProjectId,
                        Collectors.mapping(ProjectTag::getTagId, Collectors.toList())));
        List<ProjectView> views = projects.findAll().stream()
                .map(p -> new ProjectView(p.getId(), p.getName(), p.getClientId(),
                        nameByClient.get(p.getClientId()), p.isArchived(), p.getStatus(),
                        tagsByProject.getOrDefault(p.getId(), new ArrayList<>()), p.getCode()))
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
        private final String status;
        private final List<Long> tagIds;
        private final String code;

        public ProjectView(Long id, String name, Long clientId, String clientName,
                boolean archived, String status, List<Long> tagIds, String code) {
            this.id = id;
            this.name = name;
            this.clientId = clientId;
            this.clientName = clientName;
            this.archived = archived;
            this.status = status;
            this.tagIds = tagIds;
            this.code = code;
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

        public String getStatus() {
            return status;
        }

        public List<Long> getTagIds() {
            return tagIds;
        }

        public String getCode() {
            return code;
        }
    }
}
