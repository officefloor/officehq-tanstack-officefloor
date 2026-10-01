package net.officefloor.hq.app;

import java.util.List;

/**
 * What the API exposes for a project: the shape the front-end renders into the projects table. The
 * cross-entity join lives here — the project carries its client's NAME (not just the id) so the list
 * can show the client without a second lookup, and the ids of the tags on it so the list can be
 * filtered by tag without a second lookup per row.
 */
public record ProjectView(Long id, String name, Long clientId, String clientName,
        boolean archived, String status, List<Long> tagIds) {

    public static ProjectView of(Project project, String clientName) {
        return of(project, clientName, List.of());
    }

    public static ProjectView of(Project project, String clientName, List<Long> tagIds) {
        return new ProjectView(project.getId(), project.getName(), project.getClientId(),
                clientName, project.isArchived(), project.getStatus(), tagIds);
    }
}
