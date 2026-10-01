package net.officefloor.hq.app;

/** The request body for choosing a client's main contact: the id of the contact to make primary. */
public class SetPrimaryContactForm {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
