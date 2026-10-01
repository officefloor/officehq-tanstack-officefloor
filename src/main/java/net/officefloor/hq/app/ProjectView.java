package net.officefloor.hq.app;

/**
 * What the API exposes for a project: the shape the front-end renders into the projects table. The
 * cross-entity join lives here — the project carries its client's NAME (not just the id) so the list
 * can show the client without a second lookup.
 */
public record ProjectView(Long id, String name, Long clientId, String clientName) {

    public static ProjectView of(Project project, String clientName) {
        return new ProjectView(project.getId(), project.getName(), project.getClientId(), clientName);
    }
}
