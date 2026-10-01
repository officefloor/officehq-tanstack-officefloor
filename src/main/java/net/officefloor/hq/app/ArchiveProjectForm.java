package net.officefloor.hq.app;

/** The request body for archiving a project: which project to tuck away. */
public class ArchiveProjectForm {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
