import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { lineItemsKey, createLineItem } from './queries';

// Add a line to an invoice: a description, how many, and the price each. useState holds only what the
// user is currently entering (CLAUDE.md rule 4); the write is a mutation that invalidates
// ['lineitems', invoiceId] so the list and its derived total refresh themselves (rule 5). Carries
// the stable data-testid contract the test reads.
export function LineItemForm({ invoiceId }: { invoiceId: number }) {
  const [description, setDescription] = useState('');
  const [qty, setQty] = useState('');
  const [unit, setUnit] = useState('');
  const [unitPrice, setUnitPrice] = useState('');
  const [error, setError] = useState('');
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: (input: { description: string; qty: number; unit: string; unitPrice: number }) =>
      createLineItem(invoiceId, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: lineItemsKey(invoiceId) });
      setDescription('');
      setQty('');
      setUnit('');
      setUnitPrice('');
    },
  });

  return (
    <form
      data-testid="lineitem-form"
      onSubmit={(event) => {
        event.preventDefault();
        const quantity = Number(qty);
        const price = Number(unitPrice);
        if (
          description.trim() === '' ||
          !Number.isInteger(quantity) ||
          quantity <= 0 ||
          unit.trim() === '' ||
          Number.isNaN(price) ||
          price <= 0
        ) {
          setError('Enter a description, a whole quantity, a unit and a price above zero.');
          return;
        }
        setError('');
        mutation.mutate({
          description: description.trim(),
          qty: quantity,
          unit: unit.trim(),
          unitPrice: price,
        });
      }}
    >
      <input
        data-testid="lineitem-form-description"
        placeholder="Description"
        value={description}
        onChange={(event) => setDescription(event.target.value)}
      />
      <input
        data-testid="lineitem-form-qty"
        placeholder="Qty"
        value={qty}
        onChange={(event) => setQty(event.target.value)}
      />
      <input
        data-testid="lineitem-form-unit"
        placeholder="Unit"
        value={unit}
        onChange={(event) => setUnit(event.target.value)}
      />
      <input
        data-testid="lineitem-form-unitprice"
        placeholder="Unit price"
        value={unitPrice}
        onChange={(event) => setUnitPrice(event.target.value)}
      />
      <button data-testid="lineitem-form-submit" type="submit">
        Add line item
      </button>
      {error !== '' && <p data-testid="lineitem-form-error">{error}</p>}
    </form>
  );
}
