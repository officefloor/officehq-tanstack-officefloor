import { useQuery } from '@tanstack/react-query';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { ClientRow } from '../../slots/defs/clientRow';
import {
  CLIENT_SEARCH_PARAM,
  CLIENT_SORT_PARAM,
  clientsKey,
  clientsOutstandingKey,
  fetchClients,
  fetchClientsOutstanding,
  matchesClientSearch,
} from './queries';

// The list. Queries for itself under ['clients'] — never handed its data by a parent (CLAUDE.md
// rule 5). The search box owns the filter text in the URL; the list reads the same key (rule 4) and
// filters the already-loaded rows. The sort control owns the `clientSort` key; the list reads it and
// the outstanding amounts (its own query) to order the rows. Empty state and table carry the stable
// data-testid contract.
export function ClientsTable() {
  const { data: clients } = useQuery({ queryKey: clientsKey, queryFn: fetchClients });
  const { data: outstanding } = useQuery({
    queryKey: clientsOutstandingKey,
    queryFn: fetchClientsOutstanding,
  });
  const [query] = useSearchParam(CLIENT_SEARCH_PARAM, asString);
  const [sort] = useSearchParam(CLIENT_SORT_PARAM, asString);

  if (!clients) {
    return null;
  }

  if (clients.length === 0) {
    return <p data-testid="clients-empty">No clients yet.</p>;
  }

  // Owed per client, keyed by id, for the 'outstanding' order (0 when not yet loaded or unknown).
  const owedById = new Map((outstanding ?? []).map((o) => [o.clientId, Number(o.outstanding)]));
  const owed = (clientId: number) => owedById.get(clientId) ?? 0;

  const visible = clients
    .filter((client) => matchesClientSearch(client, query))
    .sort((a, b) =>
      sort === 'outstanding'
        ? owed(b.id) - owed(a.id)
        : a.name.localeCompare(b.name),
    );

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
