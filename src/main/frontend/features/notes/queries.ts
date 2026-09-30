import { getJson, postJson } from '../../api/http';

// A project's notes under a key nested beneath ['projects'] so invalidating projects refreshes them
// too (CLAUDE.md rule 5). Each note carries its text and the instant it was written; the server
// returns them newest first.
export type Note = {
  id: number;
  targetType: string;
  targetId: number;
  text: string;
  at: string;
};

export const projectNotesKey = (projectId: number) =>
  ['projects', projectId, 'notes'] as const;

export function fetchProjectNotes(projectId: number): Promise<Note[]> {
  return getJson<Note[]>(`/api/projects/${projectId}/notes`);
}

/** An invoice's notes, nested beneath ['invoices'] so invalidating invoices refreshes them too. */
export const invoiceNotesKey = (invoiceId: number) =>
  ['invoices', invoiceId, 'notes'] as const;

export function fetchInvoiceNotes(invoiceId: number): Promise<Note[]> {
  return getJson<Note[]>(`/api/invoices/${invoiceId}/notes`);
}

/** Write a note on a project; the server stamps its id and written-at instant. */
export function createNote(input: {
  targetType: string;
  targetId: number;
  text: string;
}): Promise<Note> {
  return postJson<Note>('/api/notes', input);
}
