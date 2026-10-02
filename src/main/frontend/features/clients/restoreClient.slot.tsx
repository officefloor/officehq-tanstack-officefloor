import { ClientRow } from '../../slots/defs/clientRow';
import type { Client } from './clients';
import { useRestoreClient } from './clients';

// "Restore this client" — a self-contained row action filling the client-row region, the mirror of
// archiving. It only shows for an archived client (an active one has nothing to restore), so it
// surfaces exactly in the "show archived" view. Clicking restores it through the shared mutation,
// which invalidates ['clients'] so every view re-reads from the server and the row returns to the
// main list with no hand-maintained list. The restore is recorded to the audit file server-side.
function RestoreClient({ client }: { client: Client }) {
  const restore = useRestoreClient();
  if (!client.archived) {
    return null;
  }
  return (
    <button
      type="button"
      data-testid={`client-restore-${client.id}`}
      disabled={restore.isPending}
      onClick={() => restore.mutate(client.id)}
    >
      Restore
    </button>
  );
}

export const contribution = ClientRow.fill({ order: 30, Component: RestoreClient });
