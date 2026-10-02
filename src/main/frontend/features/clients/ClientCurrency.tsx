import { useState } from 'react';
import { useClients, useSetClientCurrency } from './clients';

// The currencies the owner can bill a client in — kept in step with the server's supported set
// (ClientsCurrencyLogic). Each client is billed in exactly one of these.
const CURRENCIES = ['USD', 'EUR'] as const;

// Set a client's currency — a self-contained panel on the client detail page. It shows the client's
// current currency (client-currency), a dropdown to pick a new one and a Save button. The current
// currency is server data read from the shared ['clients'] key (never copied into state); useState
// holds only the pick the owner is currently making in the uncommitted dropdown (CLAUDE.md rule 4).
// Saving POSTs through the shared mutation, which invalidates the keys so every surface that shows
// this client's money re-reads it in the new currency.
export function ClientCurrency({ clientId }: { clientId: number }) {
  const { data: clients } = useClients();
  const setCurrency = useSetClientCurrency();
  const [choice, setChoice] = useState<string | null>(null);

  const client = clients?.find((c) => c.id === clientId);
  if (!client) {
    return null;
  }

  const selected = choice ?? client.currency;

  return (
    <div data-testid="client-currency-panel">
      <dl>
        <dt>Currency</dt>
        <dd data-testid="client-currency">{client.currency}</dd>
      </dl>
      <select
        data-testid="client-currency-select"
        value={selected}
        onChange={(event) => setChoice(event.target.value)}
      >
        {CURRENCIES.map((currency) => (
          <option key={currency} value={currency}>
            {currency}
          </option>
        ))}
      </select>
      <button
        type="button"
        data-testid="client-currency-save"
        disabled={setCurrency.isPending}
        onClick={() => setCurrency.mutate({ id: clientId, currency: selected })}
      >
        Save
      </button>
    </div>
  );
}
