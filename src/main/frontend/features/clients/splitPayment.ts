import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';

// Record one lump-sum payment and split it across several of a client's open invoices. POSTs
// {amount, date, allocations:[{invoiceId, amount}]} to /api/payments/split, which saves one payment
// row per allocation (see PaymentsSplitLogic). On success it invalidates the shared ['invoices']
// prefix — so every invoice's amount-due and derived status refresh — and the ['clients'] prefix, so
// the client's statement refreshes too. The two stay in step by sharing the keys, with no import
// between them (CLAUDE.md rule 5).
export type SplitAllocation = { invoiceId: number; amount: number };

export function useSplitPayment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { amount: number; date: string; allocations: SplitAllocation[] }) =>
      postJson('/api/payments/split', input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['invoices'] });
      void queryClient.invalidateQueries({ queryKey: ['clients'] });
    },
  });
}
