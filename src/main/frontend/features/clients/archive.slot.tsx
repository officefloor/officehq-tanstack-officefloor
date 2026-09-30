import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { ClientRowActions } from '../../slots/defs/clientRowActions';
import type { Client } from './ClientsPage';

// The button that archives a client — tucks it away instead of deleting it — its own file, filling
// the client-row-actions slot. Archiving is a useMutation that POSTs the id to /api/clients/archive;
// on success it invalidates ['clients'] so the list (and the search, which shares the key) refetches
// and the row drops out, while the row is kept on the server. Carries
// data-testid="client-archive-<id>" (the test contract).
export const contribution = ClientRowActions.fill({
  order: 15,
  Component: ({ clientId }) => {
    const queryClient = useQueryClient();
    const archive = useMutation({
      mutationFn: () => postJson<Client>('/api/clients/archive', { id: clientId }),
      onSuccess: () => {
        void queryClient.invalidateQueries({ queryKey: ['clients'] });
      },
    });
    return (
      <button
        type="button"
        data-testid={`client-archive-${clientId}`}
        disabled={archive.isPending}
        onClick={() => archive.mutate()}
      >
        Archive
      </button>
    );
  },
});
