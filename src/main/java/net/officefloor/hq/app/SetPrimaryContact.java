package net.officefloor.hq.app;

/**
 * Request body for {@code POST /api/contacts/primary}: the id of the contact to make the client's
 * one main contact. Bound from the JSON payload by the Spring MVC {@code @RequestBody} resolver.
 */
public class SetPrimaryContact {

    private Long contactId;

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }
}
