import { useQuery } from '@tanstack/react-query';
import { topClientsKey, fetchTopClients } from './queries';
import { formatMoney } from '../../ui/money';

// The dashboard's "top clients" panel: the five clients who owe the most, most owed first. Queries
// for itself under ['dashboard', 'top-clients'] — never handed its data by a parent (CLAUDE.md
// rule 5). Each amount renders through the shared money primitive so it matches every other figure
// in the app. Each row and value carries its stable data-testid (the test contract).
export function TopClients() {
  const { data } = useQuery({ queryKey: topClientsKey, queryFn: fetchTopClients });

  if (!data) {
    return null;
  }

  return (
    <section data-testid="dashboard-top-clients">
      <h2>Top clients</h2>
      <ol>
        {data.map((client) => (
          <li key={client.clientId} data-testid={`top-client-row-${client.clientId}`}>
            <span data-testid="top-client-name">{client.name}</span>
            <span data-testid="top-client-amount">
              {formatMoney(client.outstanding, client.currency)}
            </span>
          </li>
        ))}
      </ol>
    </section>
  );
}
