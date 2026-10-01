package net.officefloor.hq.app;

/** The request body for restoring a client: which tucked-away client to bring back. */
public class RestoreClientForm {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
