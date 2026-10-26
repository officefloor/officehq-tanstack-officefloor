import { QueryClient } from '@tanstack/react-query';
import { readLive } from '../api/http';

// Server data is NEVER copied into component state. A feature reads it with useQuery under its own
// key (e.g. ['clients'] / ['project', id, 'notes']) and changes it with useMutation +
// invalidateQueries. Two features share data by sharing a KEY, never by a parent holding it.
// retry:false + no focus refetching keep the e2e runs deterministic.
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: false, refetchOnWindowFocus: false },
    mutations: { retry: false },
  },
});

// The client for figures that always show the sheet as it stands now, even while the screen shows it as of a date
// (a search's results): each of its reads goes without the as-of date. Nothing is kept once no one reads it.
export const liveQueryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
      refetchOnWindowFocus: false,
      gcTime: 0,
      persister: (queryFn, context) => readLive(() => Promise.resolve(queryFn(context))),
    },
    mutations: { retry: false },
  },
});
