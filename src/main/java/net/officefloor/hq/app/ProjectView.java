package net.officefloor.hq.app;

import java.util.List;

/**
 * A project as the list shows it: its own id and name plus the belonging client's NAME (not id), so
 * the front-end renders the cross-entity join without a second request. Built by the JPQL
 * constructor expression in {@link ProjectRepository#findAllViews()}, which supplies no labels;
 * {@link ProjectsGet} then attaches the project's {@code tagIds} so the list can filter by label
 * without a second request per row.
 */
public class ProjectView {

    private final Long id;
    private final String name;
    private final String clientName;
    private final boolean archived;
    private final String status;
    private final String code;
    private final List<Long> tagIds;

    /** The shape the JPQL constructor expression builds — no labels yet, so tagIds starts empty. */
    public ProjectView(Long id, String name, String clientName, boolean archived, String status,
            String code) {
        this(id, name, clientName, archived, status, code, List.of());
    }

    public ProjectView(Long id, String name, String clientName, boolean archived, String status,
            String code, List<Long> tagIds) {
        this.id = id;
        this.name = name;
        this.clientName = clientName;
        this.archived = archived;
        this.status = status;
        this.code = code;
        this.tagIds = tagIds;
    }

    /** A copy carrying the ids of the labels attached to this project (for the label filter). */
    public ProjectView withTagIds(List<Long> tagIds) {
        return new ProjectView(id, name, clientName, archived, status, code, tagIds);
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

    /** Whether the project has been archived (tucked off the lists but retained). */
    public boolean isArchived() {
        return archived;
    }

    /** The project's lifecycle status: ACTIVE, ON_HOLD or FINISHED — shown on the row. */
    public String getStatus() {
        return status;
    }

    /** The project's short reference code — shown on the row, unique across projects. */
    public String getCode() {
        return code;
    }

    /** The ids of the labels attached to this project — the list filters by label on these. */
    public List<Long> getTagIds() {
        return tagIds;
    }
}
