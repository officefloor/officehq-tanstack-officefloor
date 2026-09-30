import { getJson, postJson } from '../../api/http';

// A client's contacts under a key nested beneath ['clients'] so invalidating clients refreshes them
// too (CLAUDE.md rule 5). Each contact carries a name, an email and a role.
export type Contact = { id: number; name: string; email: string; role: string };

export const clientContactsKey = (clientId: number) =>
  ['clients', clientId, 'contacts'] as const;

// A "proper email address": one @, non-empty local and domain parts, and a dotted domain. Shared by
// the form and mirrored by the server so a malformed address can never be saved.
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function isValidEmail(email: string): boolean {
  return EMAIL_PATTERN.test(email.trim());
}

export function fetchClientContacts(clientId: number): Promise<Contact[]> {
  return getJson<Contact[]>(`/api/clients/${clientId}/contacts`);
}

export function createContact(input: {
  name: string;
  email: string;
  role: string;
  clientId: number;
}): Promise<Contact> {
  return postJson<Contact>('/api/contacts', input);
}
