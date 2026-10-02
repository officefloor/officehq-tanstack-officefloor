import { Link } from '@tanstack/react-router';
import { ClientRow } from '../../slots/defs/clientRow';
import { ClientsToolbar } from '../../slots/defs/clientsToolbar';
import { asFlag, asString, useSearchParam } from '../../url/useSearchParam';
import { ClientSearch } from './ClientSearch';
import { ClientSort } from './ClientSort';
import { useClients } from './clients';
import { useClientOutstanding } from './outstanding';

// The clients list. Reads server data straight from its query key — never copied into state, never
// handed down from a parent. The name filter lives in the URL (`clientSearch`), read directly here;
// the search box owns that same key, so the two stay in step without any import between them.
// Archived clients are tucked away: they drop off the list and search but are retained server-side.
export function ClientList() {
  const { data: clients } = useClients();
  const { data: outstanding } = useClientOutstanding();
  const [query] = useSearchParam('clientSearch', asString);
  const [sort] = useSearchParam('clientSort', asString);
  // Shared with the "show archived" toolbar control via the `clientsShowArchived` key (no import).
  // Archived clients are tucked away by default; when it is on, they are revealed too.
  const [showArchived] = useSearchParam('clientsShowArchived', asFlag);

  if (!clients) {
    return null;
  }

  if (clients.length === 0) {
    return (
      <>
        <ClientSearch />
        <ClientSort />
        <ClientsToolbar.Slot />
        <p data-testid="clients-empty">No clients yet.</p>
      </>
    );
  }

  const active = clients.filter((client) => showArchived || !client.archived);
  const needle = query.trim().toLowerCase();
  const filtered = needle
    ? active.filter((client) => client.name.toLowerCase().includes(needle))
    : active;

  // Sort by how much each client owes (most first) when asked; otherwise by name. The owed figures
  // come from the shared outstanding query; a client with no figure yet sorts as owing nothing.
  const owed = new Map((outstanding ?? []).map((o) => [o.clientId, o.outstanding]));
  const visible = [...filtered].sort((a, b) =>
    sort === 'outstanding'
      ? (owed.get(b.id) ?? 0) - (owed.get(a.id) ?? 0)
      : a.name.localeCompare(b.name),
  );

  return (
    <>
      <ClientSearch />
      <ClientSort />
      <ClientsToolbar.Slot />
      <table data-testid="clients-table">
      <thead>
        <tr>
          <th>Name</th>
          <th>Email</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {visible.map((client) => (
          <tr key={client.id} data-testid={`client-row-${client.id}`}>
            <td data-testid="client-name">{client.name}</td>
            <td data-testid="client-email">{client.email}</td>
            <td>
              <Link
                to="/clients/$clientId"
                params={{ clientId: String(client.id) }}
                data-testid={`client-open-${client.id}`}
              >
                Open
              </Link>
            </td>
            <td>
              <ClientRow.Slot client={client} />
            </td>
          </tr>
        ))}
        </tbody>
      </table>
    </>
  );
}
