package net.officefloor.hq.app;

/** The request body for deleting a project: which project to remove. */
public class DeleteProjectForm {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
