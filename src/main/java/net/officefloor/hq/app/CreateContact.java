package net.officefloor.hq.app;

import java.util.regex.Pattern;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/clients/contacts — create a contact from the submitted name + email + role for a client,
 * returning the saved row (with its generated id). Wired by
 * officefloor/rest/api/clients/contacts.POST.yml.
 *
 * A contact must name a real client and carry a name, a proper email and a role: we reject a blank
 * field, a malformed email or an unknown/absent client id before persisting so a bad row can never be
 * saved, mirroring the UI check in contactForm.slot.tsx (the DB CHECK constraint in
 * V12__contact_email_format.sql is the final guard).
 */
public class CreateContact {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public void service(@RequestBody ContactForm form, ContactRepository contacts,
            ClientRepository clients, ObjectResponse<ContactView> response) {
        String name = form.getName() == null ? "" : form.getName().trim();
        String email = form.getEmail() == null ? "" : form.getEmail().trim();
        String role = form.getRole() == null ? "" : form.getRole().trim();
        if (name.isEmpty() || email.isEmpty() || role.isEmpty()) {
            throw new IllegalArgumentException("A contact name, email and role are required");
        }
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("A valid email address is required");
        }
        Long clientId = form.getClientId();
        Client client = clientId == null ? null : clients.findById(clientId).orElse(null);
        if (client == null) {
            throw new IllegalArgumentException("A valid client is required");
        }
        Contact saved = contacts.save(new Contact(name, email, role, clientId));
        response.send(ContactView.of(saved));
    }
}
