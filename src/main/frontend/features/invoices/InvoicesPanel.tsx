import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { InvoicesToolbar } from '../../slots/defs/invoicesToolbar';
import { InvoiceRowCells } from '../../slots/defs/invoiceRowCells';
import { money } from '../../ui/money';
import { invoiceStatus } from '../../ui/invoiceStatus';

// An invoice as the server returns it: which project it belongs to, its amount, and its lifecycle
// status (DRAFT until sent, SENT until paid, then PAID).
export type Invoice = {
  id: number;
  projectId: number;
  amount: number;
  status: string;
  issuedDate: string;
  dueDate: string;
  // How much is still owed after payments (amount minus every payment recorded against it), derived
  // and sent by the server. The amount-due cell (features/invoices/dueAmount.slot.tsx) reads this.
  amountDue: number;
  // The currency this invoice's money is shown in — the billing currency of its project's client,
  // derived and sent by the server (USD or EUR). Every amount on the row renders in it.
  currency: string;
};

// A project's invoices: the list scoped to this project, their derived total, and the form to add a
// new one. Server data is read with useQuery under the ['invoices'] key and changed with
// useMutation + invalidateQueries — never copied into state, never hand-maintained. The only
// useState is the amount the user is currently typing (rule 4).
export function InvoicesPanel({ projectId }: { projectId: number }) {
  const queryClient = useQueryClient();
  // The sort lives in the URL (owned by features/invoices/sortByDue.slot.tsx). The panel reads the
  // same key and asks the server for the ordered list, so it stays a single source of truth — no
  // server data copied into state or re-sorted by hand. The key still starts with 'invoices', so
  // writes that invalidateQueries({ queryKey: ['invoices'] }) still refresh it.
  const [sort] = useSearchParam('invoiceSort', asString);
  const invoices = useQuery({
    queryKey: ['invoices', sort],
    queryFn: () =>
      getJson<Invoice[]>(
        sort ? `/api/invoices?sort=${encodeURIComponent(sort)}` : '/api/invoices',
      ),
  });

  const [amount, setAmount] = useState('');
  const [error, setError] = useState('');

  const create = useMutation({
    mutationFn: () =>
      postJson<Invoice>('/api/invoices', { projectId, amount: Number(amount) }),
    onSuccess: () => {
      setAmount('');
      void queryClient.invalidateQueries({ queryKey: ['invoices'] });
    },
  });

  // Sending a draft invoice is a write: POST then invalidate ['invoices'] so the row re-renders with
  // its new status (DRAFT -> SENT) — never hand-maintained. The server also records the send to the
  // audit file.
  const send = useMutation({
    mutationFn: (id: number) => postJson<Invoice>('/api/invoices/send', { id }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['invoices'] });
    },
  });

  const rows = (invoices.data ?? []).filter((i) => i.projectId === projectId);
  const total = rows.reduce((sum, i) => sum + Number(i.amount), 0);
  // A project belongs to one client, so all its invoices share one currency — show the total in it.
  const currency = rows[0]?.currency ?? 'USD';

  return (
    <section data-testid="project-invoices">
      <form
        data-testid="invoice-form"
        onSubmit={(e) => {
          e.preventDefault();
          const value = Number(amount);
          if (!amount.trim() || Number.isNaN(value) || value <= 0) {
            setError('Amount must be more than zero.');
            return;
          }
          setError('');
          create.mutate();
        }}
      >
        <input
          data-testid="invoice-form-amount"
          type="number"
          step="0.01"
          placeholder="Amount"
          value={amount}
          onChange={(e) => {
            setAmount(e.target.value);
            setError('');
          }}
        />
        <button data-testid="invoice-form-submit" type="submit">
          Add invoice
        </button>
        {error && (
          <p data-testid="invoice-form-amount-error" role="alert">
            {error}
          </p>
        )}
      </form>

      <div data-testid="project-invoices-toolbar">
        <InvoicesToolbar.Slot />
      </div>

      <table data-testid="project-invoices-table">
        <thead>
          <tr>
            <th>Amount</th>
            <th>Amount due</th>
            <th>Issued</th>
            <th>Due</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {rows.map((i) => (
            <tr key={i.id} data-testid={`invoice-row-${i.id}`}>
              <td data-testid="invoice-amount">{money(Number(i.amount), i.currency)}</td>
              <InvoiceRowCells.Slot invoiceId={i.id} />
              <td data-testid="invoice-issued">{i.issuedDate}</td>
              <td data-testid="invoice-due">{i.dueDate}</td>
              <td data-testid="invoice-status">{invoiceStatus(i)}</td>
              <td>
                <Link
                  to="/invoice/$invoiceId"
                  params={{ invoiceId: String(i.id) }}
                  data-testid={`invoice-open-${i.id}`}
                >
                  Open
                </Link>
                {i.status === 'DRAFT' && (
                  <button
                    type="button"
                    data-testid={`invoice-send-${i.id}`}
                    onClick={() => send.mutate(i.id)}
                  >
                    Send
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr>
            <td data-testid="project-invoices-total">{money(total, currency)}</td>
          </tr>
        </tfoot>
      </table>
    </section>
  );
}
