import { useQuery } from '@tanstack/react-query';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { ClientRow } from '../../slots/defs/clientRow';
import {
  CLIENT_SEARCH_PARAM,
  clientsKey,
  fetchClients,
  matchesClientSearch,
} from './queries';

// The list. Queries for itself under ['clients'] — never handed its data by a parent (CLAUDE.md
// rule 5). The search box owns the filter text in the URL; the list reads the same key (rule 4) and
// filters the already-loaded rows. Empty state and table carry the stable data-testid contract.
export function ClientsTable() {
  const { data: clients } = useQuery({ queryKey: clientsKey, queryFn: fetchClients });
  const [query] = useSearchParam(CLIENT_SEARCH_PARAM, asString);

  if (!clients) {
    return null;
  }

  if (clients.length === 0) {
    return <p data-testid="clients-empty">No clients yet.</p>;
  }

  const visible = clients.filter((client) => matchesClientSearch(client, query));

  return (
    <table data-testid="clients-table">
      <thead>
        <tr>
          <th>Name</th>
          <th>Email</th>
          <th />
        </tr>
      </thead>
      <tbody>
        {visible.map((client) => (
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
  );
}
