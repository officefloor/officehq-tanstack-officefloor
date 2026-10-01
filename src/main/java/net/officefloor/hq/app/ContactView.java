package net.officefloor.hq.app;

/**
 * What the API exposes for a contact: the shape the front-end renders into a client's contacts
 * table — a name, an email and a role, scoped to the client it belongs to.
 */
public record ContactView(Long id, String name, String email, String role, Long clientId,
        boolean primary) {

    public static ContactView of(Contact contact) {
        return new ContactView(contact.getId(), contact.getName(), contact.getEmail(),
                contact.getRole(), contact.getClientId(), contact.isPrimary());
    }
}
