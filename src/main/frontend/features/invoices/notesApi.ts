import { getJson, postJson } from '../../api/http';

// Notes written against an invoice, as the API exposes them. The notes endpoint is generic over a
// target (type + id), so this reuses it with targetType 'invoice'. Features never import each other,
// so the invoices feature keeps its own tiny client here; it stays in step with any other notes
// surface by sharing the KEY ['notes', targetType, targetId] (never an import): the list reads the
// key, the add form invalidates it. The server returns a target's notes newest-first.
export type Note = {
  id: number;
  targetType: string;
  targetId: number;
  text: string;
  at: string;
};
export type NewNote = {
  targetType: string;
  targetId: number;
  text: string;
};

export const notesKey = (targetType: string, targetId: number) =>
  ['notes', targetType, targetId] as const;

export const listNotes = (targetType: string, targetId: number): Promise<Note[]> =>
  getJson<Note[]>(`/api/notes?targetType=${targetType}&targetId=${targetId}`);

export const createNote = (body: NewNote): Promise<Note> => postJson<Note>('/api/notes', body);
