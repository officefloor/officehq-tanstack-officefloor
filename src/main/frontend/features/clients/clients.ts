import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

// The shape the server returns and the query key everything that shows clients shares. Two features
// stay in step by invalidating ['clients']; nothing imports a sibling to refresh it.
export type Client = { id: number; name: string; email: string; archived: boolean };

export const clientsKey = ['clients'] as const;

export function useClients() {
  return useQuery({
    queryKey: clientsKey,
    queryFn: () => getJson<Client[]>('/api/clients'),
  });
}

export function useCreateClient() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { name: string; email: string }) =>
      postJson<Client>('/api/clients', input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: clientsKey }),
  });
}

// Correct a client's name and email: POSTs {id, name, email} and returns the saved row. The server
// (ClientsUpdateLogic) holds the email to the same rules as creation and keeps the row's id;
// invalidating ['clients'] re-reads every view so the corrected name/email show with no
// hand-maintained list (CLAUDE.md rule 5). The correction is recorded to the audit file server-side.
export function useUpdateClient() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number; name: string; email: string }) =>
      postJson<Client>('/api/clients/update', input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: clientsKey }),
  });
}

// Archive a client instead of deleting it: POSTs {id} and returns the id archived. The client is
// kept server-side with its archived flag set; invalidating ['clients'] re-reads every view so the
// row drops off the list and search with no hand-maintained list (CLAUDE.md rule 5). The audit
// record is written server-side.
export function useArchiveClient() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => postJson<{ id: number }>('/api/clients/archive', { id }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: clientsKey }),
  });
}
