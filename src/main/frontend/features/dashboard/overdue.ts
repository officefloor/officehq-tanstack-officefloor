import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

// How many SENT invoices are overdue, as measured against the dashboard's fixed reference date. Shares
// the ['dashboard', 'overdue'] key; any feature that sends or pays an invoice can invalidate it to
// keep the count in step.
export type OverdueSummary = {
  overdueCount: number;
};

export const overdueKey = ['dashboard', 'overdue'] as const;

export function useOverdue() {
  return useQuery({
    queryKey: overdueKey,
    queryFn: () => getJson<OverdueSummary>('/api/dashboard/overdue'),
  });
}
