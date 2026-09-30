import { useQuery } from '@tanstack/react-query';
import { clientsKey, fetchClients } from './queries';

// The list. Queries for itself under ['clients'] — never handed its data by a parent (CLAUDE.md
// rule 5). Empty state and table carry the stable data-testid contract.
export function ClientsTable() {
  const { data: clients } = useQuery({ queryKey: clientsKey, queryFn: fetchClients });

  if (!clients) {
    return null;
  }

  if (clients.length === 0) {
    return <p data-testid="clients-empty">No clients yet.</p>;
  }

  return (
    <table data-testid="clients-table">
      <thead>
        <tr>
          <th>Name</th>
          <th>Email</th>
        </tr>
      </thead>
      <tbody>
        {clients.map((client) => (
          <tr key={client.id} data-testid={`client-row-${client.id}`}>
            <td data-testid="client-name">{client.name}</td>
            <td data-testid="client-email">{client.email}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
