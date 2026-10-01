import { Link } from '@tanstack/react-router';
import { ClientRow } from '../../slots/defs/clientRow';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { ClientSearch } from './ClientSearch';
import { useClients } from './clients';

// The clients list. Reads server data straight from its query key — never copied into state, never
// handed down from a parent. The name filter lives in the URL (`clientSearch`), read directly here;
// the search box owns that same key, so the two stay in step without any import between them.
// Archived clients are tucked away: they drop off the list and search but are retained server-side.
export function ClientList() {
  const { data: clients } = useClients();
  const [query] = useSearchParam('clientSearch', asString);

  if (!clients) {
    return null;
  }

  if (clients.length === 0) {
    return (
      <>
        <ClientSearch />
        <p data-testid="clients-empty">No clients yet.</p>
      </>
    );
  }

  const active = clients.filter((client) => !client.archived);
  const needle = query.trim().toLowerCase();
  const visible = needle
    ? active.filter((client) => client.name.toLowerCase().includes(needle))
    : active;

  return (
    <>
      <ClientSearch />
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
