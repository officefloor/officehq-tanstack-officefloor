import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ClientDetail } from '../../slots/defs/clientDetail';
import {
  CLIENT_CURRENCIES,
  clientsKey,
  fetchClients,
  updateClientCurrency,
} from './queries';

// The currency a client pays in — one new *.slot.tsx filling the ClientDetail region (CLAUDE.md
// rule 3). Nothing existing is edited to add it. It reads the client from the shared ['clients'] key
// (rule 5) so it shows the saved currency, and saving is a mutation that invalidates ['clients']
// (rule 5), so every place that shows this client's money — their invoices, their statement, the
// dashboard totals — refreshes to the new currency for free, with no import between features. The
// select is uncommitted input, so it is the one thing held in useState (rule 4). Carries the stable
// data-testid contract: client-currency (the saved value), client-currency-select, client-currency-save.
function ClientCurrency({ clientId }: { clientId: number }) {
  const queryClient = useQueryClient();
  const { data: clients } = useQuery({ queryKey: clientsKey, queryFn: fetchClients });
  const client = clients?.find((c) => c.id === clientId);
  const [choice, setChoice] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: (currency: string) => updateClientCurrency(clientId, currency),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: clientsKey });
      setChoice(null);
    },
  });

  if (!client) {
    return null;
  }

  // What the select shows: the user's pending choice while editing, otherwise the saved currency.
  const selected = choice ?? client.currency;

  return (
    <section data-testid="client-currency-panel">
      <h3>Currency</h3>
      <p data-testid="client-currency">{client.currency}</p>
      <select
        data-testid="client-currency-select"
        value={selected}
        onChange={(event) => setChoice(event.target.value)}
      >
        {CLIENT_CURRENCIES.map((currency) => (
          <option key={currency} value={currency}>
            {currency}
          </option>
        ))}
      </select>
      <button
        type="button"
        data-testid="client-currency-save"
        onClick={() => mutation.mutate(selected)}
      >
        Save
      </button>
    </section>
  );
}

export const contribution = ClientDetail.fill({ order: 8, Component: ClientCurrency });
