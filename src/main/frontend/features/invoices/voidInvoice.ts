import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import type { Invoice } from './invoices';

// Cancel (void) a sent invoice: POSTs {id} and invalidates the shared ['invoices'] prefix so both a
// project's invoice list and any invoice-due panel re-read the new VOID status from the server. A
// voided invoice stops counting toward what is owed; voiding is what records the audit entry.
export function useVoidInvoice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number }) =>
      postJson<Invoice>('/api/invoices/void', { id: input.id }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['invoices'] }),
  });
}
