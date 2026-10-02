import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

// A client's contacts: a name, an email and a role, each carrying the id of the client they belong
// to. Everything that shows contacts shares the ['contacts'] key; adding one anywhere invalidates
// it and refreshes every view, with no import between features (CLAUDE.md rule 5).
export type Contact = {
  id: number;
  clientId: number;
  name: string;
  email: string;
  role: string;
  primary: boolean;
};

export const contactsKey = ['contacts'] as const;

export function useContacts() {
  return useQuery({
    queryKey: contactsKey,
    queryFn: () => getJson<Contact[]>('/api/contacts'),
  });
}

export function useCreateContact() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { clientId: number; name: string; email: string; role: string }) =>
      postJson<Contact>('/api/contacts', input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: contactsKey }),
  });
}

// Pick a client's one main contact: POSTs {id} (the contact id) and returns the id made primary.
// The server clears the other contacts' primary flag so exactly one is main; invalidating
// ['contacts'] re-reads every view so the shown main contact updates with no hand-maintained state
// (CLAUDE.md rule 5). The audit record is written server-side.
export function useSetPrimaryContact() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => postJson<{ id: number }>('/api/contacts/primary', { id }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: contactsKey }),
  });
}
