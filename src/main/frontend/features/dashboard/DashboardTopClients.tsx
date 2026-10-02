import { formatMoney } from '../../ui/money';
import { useTopClients } from './topClients';

// A dashboard tile: the top five clients ranked by how much they still owe, highest first. Each row
// carries the data-testids the test reads; the owed amount is a money amount.
export function DashboardTopClients() {
  const { data, isPending, isError } = useTopClients();

  if (isPending) {
    return <p data-testid="dashboard-top-clients-loading">Loading…</p>;
  }
  if (isError) {
    return <p data-testid="dashboard-top-clients-error">Could not load the top clients.</p>;
  }

  return (
    <section data-testid="dashboard-top-clients">
      <h2>Top clients</h2>
      <ol>
        {data.map((client) => (
          <li key={client.clientId} data-testid={`top-client-row-${client.clientId}`}>
            <span data-testid="top-client-name">{client.name}</span>
            <span data-testid="top-client-amount">{formatMoney(client.outstanding)}</span>
          </li>
        ))}
      </ol>
    </section>
  );
}
