import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { InvoiceDetail } from '../../slots/defs/invoiceDetail';
import { createLineItem, lineItemsKey } from './lineItemsApi';

// Add a line to this invoice — one panel filling the invoice.detail region. Instead of typing one
// figure, the user lists what they are charging for: a description, how many, and the price each.
// What the user is currently typing lives in useState (uncommitted input); the saved lines live on
// the server. On success we invalidate ['invoice-lineitems', invoiceId] so the list + derived total
// refetch, and ['invoices'] so the project's invoices list picks up the invoice's new amount — we
// never hand-maintain either list.
function LineItemForm({ invoiceId }: { invoiceId: number }) {
  const queryClient = useQueryClient();
  const [description, setDescription] = useState('');
  const [qty, setQty] = useState('');
  const [unitPrice, setUnitPrice] = useState('');
  const [error, setError] = useState('');

  const mutation = useMutation({
    mutationFn: createLineItem,
    onSuccess: () => {
      setDescription('');
      setQty('');
      setUnitPrice('');
      void queryClient.invalidateQueries({ queryKey: lineItemsKey(invoiceId) });
      void queryClient.invalidateQueries({ queryKey: ['invoices'] });
    },
  });

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const qtyValue = Number(qty);
    const priceValue = Number(unitPrice);
    if (description.trim() === '') {
      setError('A description is required');
      return;
    }
    // Each line has to be for something: a positive quantity at a price that is not negative.
    if (qty.trim() === '' || Number.isNaN(qtyValue) || qtyValue <= 0) {
      setError('Quantity must be greater than zero');
      return;
    }
    if (unitPrice.trim() === '' || Number.isNaN(priceValue) || priceValue < 0) {
      setError('Price each must be zero or more');
      return;
    }
    setError('');
    mutation.mutate({
      invoiceId,
      description: description.trim(),
      qty: qtyValue,
      unitPrice: priceValue,
    });
  };

  return (
    <form onSubmit={submit}>
      <input
        data-testid="lineitem-form-description"
        placeholder="Description"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
      />
      <input
        data-testid="lineitem-form-qty"
        placeholder="Qty"
        value={qty}
        onChange={(e) => setQty(e.target.value)}
      />
      <input
        data-testid="lineitem-form-unitprice"
        placeholder="Price each"
        value={unitPrice}
        onChange={(e) => setUnitPrice(e.target.value)}
      />
      <button data-testid="lineitem-form-submit" type="submit">
        Add line item
      </button>
      {error && <p data-testid="lineitem-form-error">{error}</p>}
    </form>
  );
}

export const contribution = InvoiceDetail.fill({ order: 20, Component: LineItemForm });
