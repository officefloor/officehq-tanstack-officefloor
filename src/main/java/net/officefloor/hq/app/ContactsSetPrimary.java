package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/contacts/primary} — make one contact the client's main (primary) contact. A
 * client has exactly one: the chosen contact is flagged primary and every other contact of the same
 * client is cleared, so the flag moves rather than accumulates. Wired by
 * {@code officefloor/rest/api/contacts/primary.POST.yml}. The contact must exist; the change is
 * audited through {@link Audit}.
 */
public class ContactsSetPrimary {

    public void service(@RequestBody SetPrimaryContact body, ContactRepository contacts,
            Audit audit, ObjectResponse<Contact> response) {
        Long contactId = body.getContactId();
        Contact chosen = contactId == null ? null : contacts.findById(contactId).orElse(null);
        if (chosen == null) {
            throw new IllegalArgumentException("no such contact");
        }
        List<Contact> siblings = contacts.findByClientId(chosen.getClientId());
        for (Contact c : siblings) {
            boolean isChosen = c.getId().equals(contactId);
            if (c.isPrimary() != isChosen) {
                c.setPrimary(isChosen);
                contacts.save(c);
            }
        }
        chosen.setPrimary(true);
        audit.record("CONTACT_PRIMARY_SET id=" + contactId + " client=" + chosen.getClientId());
        response.send(chosen);
    }
}
