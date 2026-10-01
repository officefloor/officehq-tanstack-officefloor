import { useQuery } from '@tanstack/react-query';
import { useSearchParam, asString, asFlag } from '../../url/useSearchParam';
import { clientsKey, listClientsSorted, type Client } from './api';
import { ClientRow } from '../../slots/defs/clientRow';
import { ClientsToolbar } from '../../slots/defs/clientsToolbar';

// The clients list. Reads server data under ['clients'] (never copied into state); the create form
// shares the key, so a successful create refreshes this list with no import between them.
//
// The list grew a search box: the committed filter lives in the URL under 'q' (not useState — it
// outlives the click and is shareable/back-button friendly), so the box owns the key and the rows
// read the same key. Matching is a case-insensitive substring on the client name; an empty box
// shows every client.
export function ClientsList() {
  // The ordering lives in the URL under the shared `clientSort` key — the sort control writes it,
  // this list reads it. It is part of the query key, so each ordering caches on its own, and the
  // server returns the rows already sorted (by name, by amount owed, or the default id order).
  const [sort] = useSearchParam('clientSort', asString);
  const { data: clients } = useQuery({
    queryKey: [...clientsKey, 'sorted', sort],
    queryFn: () => listClientsSorted(sort),
  });
  const [query, setQuery] = useSearchParam('q', asString);
  // Archived (tucked-away) clients are hidden by default: they drop off the list unless the shared
  // `showArchived` URL key is on. The toggle control (its own *.slot.tsx) owns that key; this list
  // reads the same key and filters, so they stay in step with nothing passed between them.
  const [showArchived] = useSearchParam('showArchived', asFlag);

  if (!clients) {
    return null;
  }

  const needle = query.trim().toLowerCase();
  const shown = clients.filter(
    (client: Client) =>
      (showArchived || !client.archived) &&
      (needle === '' || client.name.toLowerCase().includes(needle)),
  );

  return (
    <>
      <input
        data-testid="client-search"
        type="search"
        placeholder="Search clients by name"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />
      <ClientsToolbar.Slot />
      {clients.length === 0 ? (
        <p data-testid="clients-empty">No clients yet.</p>
      ) : (
        <table data-testid="clients-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {shown.map((client: Client) => (
              <tr key={client.id} data-testid={`client-row-${client.id}`}>
                <td data-testid="client-name">{client.name}</td>
                <td data-testid="client-email">{client.email}</td>
                <td>
                  <ClientRow.Slot clientId={client.id} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  );
}
