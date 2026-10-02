import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

// The shape the server returns and the query key everything that shows clients shares. Two features
// stay in step by invalidating ['clients']; nothing imports a sibling to refresh it.
export type Client = {
  id: number;
  name: string;
  email: string;
  archived: boolean;
  currency: string;
};

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

// Set the currency a client is billed in: POSTs {id, currency} and returns the saved row. The server
// (ClientsCurrencyLogic) records the change to the audit file. Changing a client's currency changes
// how their money reads everywhere, so this invalidates ['clients'] (the detail panel, the list),
// ['invoices'] (their project invoices) and ['dashboard'] (the per-currency outstanding totals and
// the top clients list) — each re-reads under its shared key with no hand-maintained copy (rule 5).
export function useSetClientCurrency() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number; currency: string }) =>
      postJson<Client>('/api/clients/currency', input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: clientsKey });
      queryClient.invalidateQueries({ queryKey: ['invoices'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
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

// Restore an archived client so they return to the main list: POSTs {id} and returns the id
// restored. The mirror of archiving — the server clears the archived flag and keeps the row's id;
// invalidating ['clients'] re-reads every view so the row returns to the list and search with no
// hand-maintained list (CLAUDE.md rule 5). The audit record is written server-side.
export function useRestoreClient() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => postJson<{ id: number }>('/api/clients/restore', { id }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: clientsKey }),
  });
}
