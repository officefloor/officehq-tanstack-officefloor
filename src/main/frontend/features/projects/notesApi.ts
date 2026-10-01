import { getJson, postJson } from '../../api/http';

// A note as the API exposes it — a free-text remark written against a target (a project today). The
// server returns a target's notes newest-first, so the panel renders them in the order received.
// The query key is scoped to the target, ['notes', targetType, targetId], so each detail page reads
// (and invalidates) only its own notes: the list reads the key, the add form invalidates it.
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
