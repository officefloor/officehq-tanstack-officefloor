package net.officefloor.hq.app;

/** The request body for archiving a client: which client to tuck away. */
public class ArchiveClientForm {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
