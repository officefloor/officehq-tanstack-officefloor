import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ClientRow } from '../../slots/defs/clientRow';
import { archivedClientsKey, clientsKey, fetchArchivedClients, restoreClient } from './queries';

// The "bring this client back" action — its own file (CLAUDE.md rule 3), the mirror of the archive
// action. It only appears on a tucked-away row: it reads the SAME ['clients','archived'] key the list
// uses (rule 5) and renders nothing unless this client is archived, so an active row shows no restore
// control. The write is a mutation that invalidates ['clients'] (rule 5), so the client returns to
// the main list and search for free. The server records the audited CLIENT_RESTORED entry. Carries
// data-testid="client-restore-<id>" (the test contract).
function ClientRestore({ clientId }: { clientId: number }) {
  const queryClient = useQueryClient();
  const { data: archived } = useQuery({
    queryKey: archivedClientsKey,
    queryFn: fetchArchivedClients,
  });

  const restore = useMutation({
    mutationFn: () => restoreClient(clientId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: clientsKey });
    },
  });

  if (!archived?.some((client) => client.id === clientId)) {
    return null;
  }

  return (
    <button
      type="button"
      data-testid={`client-restore-${clientId}`}
      onClick={() => restore.mutate()}
    >
      Restore
    </button>
  );
}

export const contribution = ClientRow.fill({ order: 6, Component: ClientRestore });
