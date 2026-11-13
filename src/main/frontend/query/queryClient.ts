import { MutationCache, QueryClient } from '@tanstack/react-query';
import { currentPreview } from '../session/session';

// Server data is NEVER copied into component state. A feature reads it with useQuery under its own
// key (e.g. ['clients'] / ['project', id, 'notes']) and changes it with useMutation +
// invalidateQueries. Two features share data by sharing a KEY, never by a parent holding it.
// retry:false + no focus refetching keep the e2e runs deterministic.
// A change answers with figures as of the sheet's real today; while the screen previews a date, it re-reads
// the sheet instead, so every figure stays as of the preview date.
export const queryClient: QueryClient = new QueryClient({
  mutationCache: new MutationCache({
    onSuccess: () => {
      if (currentPreview() !== '') void queryClient.invalidateQueries({ queryKey: ['sheet'] });
    },
  }),
  defaultOptions: {
    queries: { retry: false, refetchOnWindowFocus: false },
    mutations: { retry: false },
  },
});
