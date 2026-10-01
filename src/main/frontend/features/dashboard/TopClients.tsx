import { useQuery } from '@tanstack/react-query';
import { topClientsKey, getTopClients } from './topClientsApi';
import { formatMoney } from '../../ui/money';

// The home screen's "top clients" panel: the five clients that owe the most, ranked by how much
// they owe. Reads server data under ['dashboard', 'top-clients'] (never copied into state); the
// ranking and the cap live on the server, so this just renders the rows in the order they arrive.
// Each amount is money, shown with two decimal places.
export function TopClients() {
  const { data } = useQuery({ queryKey: topClientsKey, queryFn: getTopClients });

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
            <span data-testid="top-client-amount">{formatMoney(client.amount)}</span>
          </li>
        ))}
      </ol>
    </section>
  );
}
