import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

// A line item on an invoice: one thing being charged for — a description, how many (qty) and the
// price of each (unitPrice). Everything that shows an invoice's lines shares the key
// ['lineItems', invoiceId]; invalidating it after a write refreshes the list and its total together.
export type LineItem = {
  id: number;
  invoiceId: number;
  description: string;
  qty: number;
  unit: string;
  unitPrice: number;
};

export const lineItemsKey = (invoiceId: number) => ['lineItems', invoiceId] as const;

export function useLineItems(invoiceId: number) {
  return useQuery({
    queryKey: lineItemsKey(invoiceId),
    queryFn: () => getJson<LineItem[]>(`/api/lineitems?invoiceId=${invoiceId}`),
  });
}

// Add a line to an invoice: POSTs {invoiceId, description, qty, unitPrice} and returns the saved row.
// Invalidates ['lineItems', invoiceId] so the line list and its total re-read from the server, and
// the ['invoices'] prefix so every invoice list (which shows the worked-out amount) refreshes too —
// the two stay in step by sharing the key, with no import between them.
export function useAddLineItem(invoiceId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { description: string; qty: number; unit: string; unitPrice: number }) =>
      postJson<LineItem>('/api/lineitems', { invoiceId, ...input }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: lineItemsKey(invoiceId) });
      void queryClient.invalidateQueries({ queryKey: ['invoices'] });
    },
  });
}

// Take a line off an invoice: POSTs {id} and returns the updated invoice. Invalidates the same keys
// as adding — ['lineItems', invoiceId] so the line list and its worked-out total re-read, and the
// ['invoices'] prefix so every invoice list showing the amount refreshes too. The total is never
// edited here; it drops because the list re-reads from the server (CLAUDE.md rule 5).
export function useRemoveLineItem(invoiceId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => postJson<unknown>('/api/lineitems/remove', { id }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: lineItemsKey(invoiceId) });
      void queryClient.invalidateQueries({ queryKey: ['invoices'] });
    },
  });
}
