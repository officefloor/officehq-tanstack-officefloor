package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/clients/contacts/primary — choose a client's MAIN contact: mark the given contact
 * primary and clear the flag on its siblings so exactly one contact of a client is primary at a
 * time. Returns the updated contact. Wired by officefloor/rest/api/clients/contacts/primary.POST.yml.
 *
 * Choosing the main contact is an audited side-effect: alongside the change we append one record
 * through the {@link Audit} service ({@code CONTACT_PRIMARY_SET client=<clientId> contact=<id>}) so
 * there is a durable note every time a client's main contact changes. We reject a missing or unknown
 * contact id before writing anything so a bad request neither changes state nor audits.
 */
public class SetPrimaryContact {

    public void service(@RequestBody SetPrimaryContactForm form, ContactRepository contacts,
            Audit audit, ObjectResponse<ContactView> response) {
        Long id = form.getId();
        if (id == null) {
            throw new IllegalArgumentException("A contact id is required");
        }
        Contact contact = contacts.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("A valid contact is required"));
        // Exactly one primary per client: set the chosen contact and clear its siblings in one pass.
        List<Contact> siblings = contacts.findByClientIdOrderByIdAsc(contact.getClientId());
        for (Contact sibling : siblings) {
            sibling.setPrimary(sibling.getId().equals(id));
        }
        contacts.saveAll(siblings);
        audit.record("CONTACT_PRIMARY_SET client=" + contact.getClientId() + " contact=" + id);
        response.send(ContactView.of(contact));
    }
}
