import { getJson, postJson } from '../../api/http';

// Server data under ONE shared key: anything showing clients reads ['clients'], and a write
// invalidates the same key to refresh them all (CLAUDE.md rule 5).
export type Client = { id: number; name: string; email: string };

export const clientsKey = ['clients'] as const;

// A "proper email address": one @, non-empty local and domain parts, and a dotted domain. Shared by
// the form and mirrored by the server so a malformed address can never be saved.
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function isValidEmail(email: string): boolean {
  return EMAIL_PATTERN.test(email.trim());
}

export function fetchClients(): Promise<Client[]> {
  return getJson<Client[]>('/api/clients');
}

export function createClient(input: { name: string; email: string }): Promise<Client> {
  return postJson<Client>('/api/clients', input);
}
