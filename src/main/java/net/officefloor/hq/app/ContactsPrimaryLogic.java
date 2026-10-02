package net.officefloor.hq.app;

import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;

/**
 * {@code POST /api/contacts/primary} — pick a client's one main contact, from an {id} (contact id)
 * body, and return the id that was made primary. Wired by
 * {@code officefloor/rest/api/contacts/primary.POST.yml}. The contact must exist (404 otherwise).
 * "One main contact per client" is kept here: every other contact of the same client has its primary
 * flag cleared before this one is set, so exactly one row is primary. One {@code CONTACT_PRIMARY_SET}
 * audit record is appended so the change can be checked back later through the audit file.
 */
public class ContactsPrimaryLogic {

    public void service(@RequestBody PrimaryContact body, ContactRepository contacts, Audit audit,
            ObjectResponse<PrimaryContactSet> response) {
        Long id = body.getId();
        if (id == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A contact id is required");
        }
        Contact target = contacts.findById(id)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "No such contact"));
        for (Contact other : contacts.findByClientId(target.getClientId())) {
            if (other.isPrimary() && !other.getId().equals(id)) {
                other.setPrimary(false);
                contacts.save(other);
            }
        }
        target.setPrimary(true);
        contacts.save(target);
        audit.record("CONTACT_PRIMARY_SET client=" + target.getClientId() + " contact=" + id);
        response.send(new PrimaryContactSet(id));
    }

    /** Request body for picking a client's main contact. */
    public static class PrimaryContact {
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    /** The id of the contact that was made primary. */
    public static class PrimaryContactSet {
        private final Long id;

        public PrimaryContactSet(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }
    }
}
