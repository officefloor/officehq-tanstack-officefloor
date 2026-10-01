import { useQuery } from '@tanstack/react-query';
import { useSearchParam, asString } from '../../url/useSearchParam';
import { clientsKey, listClients, type Client } from './api';
import { ClientRow } from '../../slots/defs/clientRow';

// The clients list. Reads server data under ['clients'] (never copied into state); the create form
// shares the key, so a successful create refreshes this list with no import between them.
//
// The list grew a search box: the committed filter lives in the URL under 'q' (not useState — it
// outlives the click and is shareable/back-button friendly), so the box owns the key and the rows
// read the same key. Matching is a case-insensitive substring on the client name; an empty box
// shows every client.
export function ClientsList() {
  const { data: clients } = useQuery({ queryKey: clientsKey, queryFn: listClients });
  const [query, setQuery] = useSearchParam('q', asString);

  if (!clients) {
    return null;
  }

  const needle = query.trim().toLowerCase();
  const shown = needle
    ? clients.filter((client: Client) => client.name.toLowerCase().includes(needle))
    : clients;

  return (
    <>
      <input
        data-testid="client-search"
        type="search"
        placeholder="Search clients by name"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />
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
