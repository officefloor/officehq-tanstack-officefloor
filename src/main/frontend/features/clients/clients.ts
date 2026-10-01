import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

// The shape the server returns and the query key everything that shows clients shares. Two features
// stay in step by invalidating ['clients']; nothing imports a sibling to refresh it.
export type Client = { id: number; name: string; email: string };

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
