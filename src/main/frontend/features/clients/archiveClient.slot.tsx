import { ClientRow } from '../../slots/defs/clientRow';
import type { Client } from './clients';
import { useArchiveClient } from './clients';

// "Archive this client" — a self-contained row action filling the client-row region. Archiving tucks
// the client away rather than deleting it: nothing is lost, it just drops off the list and search.
// Clicking archives it through the shared mutation, which invalidates ['clients'] so every view
// re-reads from the server and the row drops with no hand-maintained list. The server keeps the row
// and records the archiving to the audit file.
function ArchiveClient({ client }: { client: Client }) {
  const archive = useArchiveClient();
  if (client.archived) {
    return null;
  }
  return (
    <button
      type="button"
      data-testid={`client-archive-${client.id}`}
      disabled={archive.isPending}
      onClick={() => archive.mutate(client.id)}
    >
      Archive
    </button>
  );
}

export const contribution = ClientRow.fill({ order: 20, Component: ArchiveClient });
