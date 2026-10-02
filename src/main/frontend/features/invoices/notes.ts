import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

// A note written against an invoice. Everything that shows an invoice's notes shares the key
// ['invoice', invoiceId, 'notes']; writing one invalidates it so the list re-reads from the server.
// The server (the same generic /api/notes endpoint the project notes use) returns notes newest
// first and stamps each new one with the current instant, so a freshly written note sorts on top.
export type Note = {
  id: number;
  targetType: string;
  targetId: number;
  text: string;
  at: string;
};

export const invoiceNotesKey = (invoiceId: number) =>
  ['invoice', invoiceId, 'notes'] as const;

export function useInvoiceNotes(invoiceId: number) {
  return useQuery({
    queryKey: invoiceNotesKey(invoiceId),
    queryFn: () =>
      getJson<Note[]>(`/api/notes?targetType=invoice&targetId=${invoiceId}`),
  });
}

export function useCreateInvoiceNote(invoiceId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { text: string }) =>
      postJson<Note>('/api/notes', {
        targetType: 'invoice',
        targetId: invoiceId,
        text: input.text,
      }),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: invoiceNotesKey(invoiceId) }),
  });
}
