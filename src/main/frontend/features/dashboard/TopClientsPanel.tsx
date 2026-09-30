import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { money } from '../../ui/money';

// The home screen's top clients as the server ranks them: each client with the amount it still owes
// (its outstanding total), most owed first, capped at five.
type TopClient = { clientId: number; name: string; amount: number };

// The top-clients panel, rendered in the dashboard-panels context: the five clients who owe the most,
// ranked by how much. Server data is read with useQuery under a key that starts with ['dashboard'],
// so anything that invalidates ['dashboard'] refreshes this too — never copied into state, never
// hand-ranked (rule 5). The ranking and amounts are server-derived (like the dashboard's outstanding
// total).
export function TopClientsPanel() {
  const query = useQuery({
    queryKey: ['dashboard', 'top-clients'],
    queryFn: () => getJson<TopClient[]>('/api/dashboard/top-clients'),
  });

  const rows = query.data ?? [];

  return (
    <section data-testid="dashboard-top-clients">
      <h2>Top clients</h2>
      <table>
        <thead>
          <tr>
            <th>Client</th>
            <th>Owed</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((c) => (
            <tr key={c.clientId} data-testid={`top-client-row-${c.clientId}`}>
              <td data-testid="top-client-name">{c.name}</td>
              <td data-testid="top-client-amount">{money(Number(c.amount))}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
