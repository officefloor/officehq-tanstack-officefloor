import { useState } from 'react';
import { useAddLineItem } from './lineItems';

// Add-a-line-item form. useState holds only what the user is currently typing (the description, how
// many, and the price of each) and the validation error for the current attempt. A line needs a
// description, a positive quantity and a positive unit price — otherwise a visible error is shown
// and no row is added (the server + DB enforce the same). On a valid submit it POSTs the line and
// invalidates ['lineItems', invoiceId]; the list and the worked-out total refresh from the server —
// nothing is hand-maintained here.
export function LineItemForm({ invoiceId }: { invoiceId: number }) {
  const [description, setDescription] = useState('');
  const [qty, setQty] = useState('');
  const [unitPrice, setUnitPrice] = useState('');
  const [error, setError] = useState('');
  const add = useAddLineItem(invoiceId);

  return (
    <form
      data-testid="lineitem-form"
      onSubmit={(event) => {
        event.preventDefault();
        const qtyValue = Number(qty);
        const priceValue = Number(unitPrice);
        if (!description.trim()) {
          setError('A description is required');
          return;
        }
        if (!qty.trim() || Number.isNaN(qtyValue) || qtyValue <= 0) {
          setError('Quantity must be more than zero');
          return;
        }
        if (!unitPrice.trim() || Number.isNaN(priceValue) || priceValue <= 0) {
          setError('Unit price must be more than zero');
          return;
        }
        setError('');
        add.mutate(
          { description: description.trim(), qty: qtyValue, unitPrice: priceValue },
          {
            onSuccess: () => {
              setDescription('');
              setQty('');
              setUnitPrice('');
            },
          },
        );
      }}
    >
      <input
        data-testid="lineitem-form-description"
        placeholder="Description"
        value={description}
        onChange={(event) => {
          setDescription(event.target.value);
          setError('');
        }}
      />
      <input
        data-testid="lineitem-form-qty"
        placeholder="Qty"
        value={qty}
        onChange={(event) => {
          setQty(event.target.value);
          setError('');
        }}
      />
      <input
        data-testid="lineitem-form-unitprice"
        placeholder="Unit price"
        value={unitPrice}
        onChange={(event) => {
          setUnitPrice(event.target.value);
          setError('');
        }}
      />
      <button data-testid="lineitem-form-submit" type="submit">
        Add line item
      </button>
      {error && (
        <p data-testid="lineitem-form-error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}
