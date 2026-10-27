import { QueryClient } from '@tanstack/react-query';
import { withoutAsOf } from '../api/asOf';

/**
 * A query client of its own whose queries read the sheet as it is now, even while the screen is shown as of a date:
 * every query function it runs makes its requests without the as-of date. Same defaults as the app's client.
 */
export function nowQueryClient(): QueryClient {
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false, refetchOnWindowFocus: false },
      mutations: { retry: false },
    },
  });
  const defaulted = client.defaultQueryOptions.bind(client);
  client.defaultQueryOptions = ((options: Parameters<typeof defaulted>[0]) => {
    const o = defaulted(options);
    const fn = o.queryFn;
    if (typeof fn === 'function') {
      o.queryFn = (context: Parameters<typeof fn>[0]) => withoutAsOf(() => fn(context));
    }
    return o;
  }) as typeof client.defaultQueryOptions;
  return client;
}
