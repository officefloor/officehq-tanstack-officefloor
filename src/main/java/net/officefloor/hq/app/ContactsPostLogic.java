package net.officefloor.hq.app;

import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;

/**
 * {@code POST /api/contacts} — create a contact from a {clientId, name, email, role} body and return
 * the saved row (with its generated id). Wired by {@code officefloor/rest/api/contacts.POST.yml}. A
 * contact must belong to an existing client: the clientId is validated here and rejected with 400
 * before it reaches the repository (the DB foreign key in Flyway V10 is the matching last line of
 * defence). Name, email and role are required.
 */
public class ContactsPostLogic {

    public void service(@RequestBody NewContact newContact, ContactRepository contacts,
            ClientRepository clients, ObjectResponse<Contact> response) {
        Long clientId = newContact.getClientId();
        String name = newContact.getName();
        String email = newContact.getEmail();
        String role = newContact.getRole();
        if (clientId == null || !clients.existsById(clientId)) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A valid client is required");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A contact name is required");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A contact email is required");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A contact role is required");
        }
        Contact saved = contacts.save(
                new Contact(clientId, name.trim(), email.trim(), role.trim()));
        response.send(saved);
    }

    /** Request body for creating a contact. */
    public static class NewContact {
        private Long clientId;
        private String name;
        private String email;
        private String role;

        public Long getClientId() {
            return clientId;
        }

        public void setClientId(Long clientId) {
            this.clientId = clientId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}
