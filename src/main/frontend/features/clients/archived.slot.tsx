import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { asFlag, useSearchParam } from '../../url/useSearchParam';
import { ClientsToolbar } from '../../slots/defs/clientsToolbar';
import type { Client } from './ClientsPage';

// The archived-clients panel — its own file, filling the clients toolbar slot. It READS the
// `showArchived` URL key owned by features/clients/showArchived.slot.tsx: when the toggle is off it
// renders nothing, when on it reveals the tucked-away clients so they can be brought back. The list
// is server data read with useQuery under a key that still starts with 'clients', so any write that
// invalidates ['clients'] refreshes it; restoring is a useMutation that POSTs the id to
// /api/clients/restore and on success invalidates ['clients'] — the archived client drops out of
// this panel and reappears in the default clients table, with nothing hand-maintained. Each restore
// button carries data-testid="client-restore-<id>" (the test contract).
function ArchivedClients() {
  const [showArchived] = useSearchParam('showArchived', asFlag);
  const queryClient = useQueryClient();
  const archived = useQuery({
    queryKey: ['clients', 'archived'],
    queryFn: () => getJson<Client[]>('/api/clients/archived'),
    enabled: showArchived,
  });
  const restore = useMutation({
    mutationFn: (id: number) => postJson<Client>('/api/clients/restore', { id }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['clients'] });
    },
  });

  if (!showArchived) {
    return null;
  }

  const rows = archived.data ?? [];
  return (
    <div data-testid="clients-archived">
      {rows.length === 0 ? (
        <p data-testid="clients-archived-empty">No archived clients.</p>
      ) : (
        <ul data-testid="clients-archived-list">
          {rows.map((c) => (
            <li key={c.id} data-testid={`client-archived-row-${c.id}`}>
              <span data-testid="client-name">{c.name}</span>
              <button
                type="button"
                data-testid={`client-restore-${c.id}`}
                disabled={restore.isPending}
                onClick={() => restore.mutate(c.id)}
              >
                Restore
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export const contribution = ClientsToolbar.fill({ order: 20, Component: ArchivedClients });
