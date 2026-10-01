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
