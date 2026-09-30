import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ClientRow } from '../../slots/defs/clientRow';
import { clientsKey, archiveClient } from './queries';

// The "archive this client" action on every client row — its own file (CLAUDE.md rule 3), so the
// clients table never lists what goes at the end of a row. Archiving tucks the client away instead
// of deleting it: the write is a mutation that invalidates ['clients'] (rule 5), so the row drops off
// the list and its search for free while the client is retained. The server records the audited
// CLIENT_ARCHIVED entry. Carries data-testid="client-archive-<id>" (the test contract).
function ClientArchive({ clientId }: { clientId: number }) {
  const queryClient = useQueryClient();

  const archive = useMutation({
    mutationFn: () => archiveClient(clientId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: clientsKey });
    },
  });

  return (
    <button
      type="button"
      data-testid={`client-archive-${clientId}`}
      onClick={() => archive.mutate()}
    >
      Archive
    </button>
  );
}

export const contribution = ClientRow.fill({ order: 5, Component: ClientArchive });
