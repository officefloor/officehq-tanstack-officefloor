package net.officefloor.hq.app;

import java.util.regex.Pattern;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/contacts} — add a contact (name, email, role) to a client and return the created
 * row. Wired by {@code officefloor/rest/api/contacts.POST.yml}. The client must exist; the create is
 * audited through {@link Audit}.
 */
public class ContactsPost {

    // Every contact must carry a proper email. Mirror of the UI check
    // (ClientContactsPanel.isValidEmail) so a request that bypasses the form is still rejected
    // before any row or audit record is written.
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public void service(@RequestBody NewContact body, ContactRepository contacts,
            ClientRepository clients, Audit audit, ObjectResponse<Contact> response) {
        String name = body.getName() == null ? "" : body.getName().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("a contact requires a name");
        }
        String email = body.getEmail() == null ? "" : body.getEmail().trim();
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("a contact requires a valid email address");
        }
        String role = body.getRole() == null ? "" : body.getRole().trim();
        if (role.isEmpty()) {
            throw new IllegalArgumentException("a contact requires a role");
        }
        Long clientId = body.getClientId();
        Client client = clientId == null ? null : clients.findById(clientId).orElse(null);
        if (client == null) {
            throw new IllegalArgumentException("a contact requires an existing client");
        }
        Contact contact = new Contact();
        contact.setClientId(clientId);
        contact.setName(name);
        contact.setEmail(email);
        contact.setRole(role);
        Contact saved = contacts.save(contact);
        audit.record("CONTACT_CREATED id=" + saved.getId() + " name=" + saved.getName()
                + " client=" + client.getName());
        response.send(saved);
    }
}
