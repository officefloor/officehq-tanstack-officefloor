import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ClientDetail } from '../../slots/defs/clientDetail';
import { dashboardKey } from '../dashboard/api';
import {
  clientsKey,
  listClients,
  setClientCurrency,
  CURRENCIES,
  type Client,
} from './api';

// Set the currency a client is billed in — one new file filling the client.detail region. The
// client's money is shown in this currency everywhere the user sees it, so saving invalidates the
// shared ['clients'] key (every panel showing the client refetches) and ['dashboard'] (the home
// figures regroup by currency). The chosen-but-unsaved currency is uncommitted user input, so it
// lives in useState; the saved value comes from the server query, never copied into state. Its
// anchors are client-currency (the saved value), client-currency-select and client-currency-save
// (the test contract; CLAUDE.md).
function CurrencyPicker({ client }: { client: Client }) {
  const queryClient = useQueryClient();
  const [choice, setChoice] = useState(client.currency);

  const mutation = useMutation({
    mutationFn: () => setClientCurrency({ id: client.id, currency: choice }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: clientsKey });
      void queryClient.invalidateQueries({ queryKey: dashboardKey });
    },
  });

  return (
    <section data-testid="client-currency-panel">
      <p>
        Currency: <span data-testid="client-currency">{client.currency}</span>
      </p>
      <select
        data-testid="client-currency-select"
        value={choice}
        onChange={(e) => setChoice(e.target.value)}
      >
        {CURRENCIES.map((code) => (
          <option key={code} value={code}>
            {code}
          </option>
        ))}
      </select>
      <button
        data-testid="client-currency-save"
        type="button"
        disabled={mutation.isPending}
        onClick={() => mutation.mutate()}
      >
        Save
      </button>
    </section>
  );
}

function ClientCurrency({ clientId }: { clientId: number }) {
  const { data: clients } = useQuery({ queryKey: clientsKey, queryFn: listClients });
  const client = clients?.find((c) => c.id === clientId);
  if (!client) {
    return null;
  }
  // Re-key on the saved currency so the uncommitted select resets to it after a save lands.
  return <CurrencyPicker key={client.currency} client={client} />;
}

export const contribution = ClientDetail.fill({ order: 5, Component: ClientCurrency });
