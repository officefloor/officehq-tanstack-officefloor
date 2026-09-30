package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * POST /api/clients/{clientId}/contacts/{contactId}/primary — make one contact the client's MAIN
 * contact and return the updated row. Wired by
 * {@code officefloor/rest/api/clients/{clientId}/contacts/{contactId}/primary.POST.yml}. A client has
 * at most one primary contact, so the named contact is marked primary and every other contact of the
 * same client is cleared. Choosing the primary is an audited side-effect: one
 * {@code CONTACT_PRIMARY_SET clientId=<clientId> id=<contactId>} record is appended per change
 * (CLAUDE.md — audited behaviour goes through {@link Audit}). An unknown contact, or one that does not
 * belong to the named client, is rejected with 404.
 */
public class ContactSetPrimary {

    public void service(@HttpPathParameter("clientId") String clientId,
            @HttpPathParameter("contactId") String contactId, ContactRepository contacts,
            Audit audit, ObjectResponse<Contact> response) {
        Long cid = Long.valueOf(clientId);
        Long targetId = Long.valueOf(contactId);
        Contact target = contacts.findById(targetId)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND));
        if (!cid.equals(target.getClientId())) {
            throw new HttpException(HttpStatus.NOT_FOUND);
        }
        // Exactly one primary per client: set the chosen contact, clear every sibling.
        List<Contact> siblings = contacts.findByClientIdOrderByIdAsc(cid);
        for (Contact contact : siblings) {
            boolean shouldBePrimary = contact.getId().equals(targetId);
            if (contact.isPrimary() != shouldBePrimary) {
                contact.setPrimary(shouldBePrimary);
                contacts.save(contact);
            }
        }
        audit.record("CONTACT_PRIMARY_SET clientId=" + cid + " id=" + targetId);
        target.setPrimary(true);
        response.send(target);
    }
}
