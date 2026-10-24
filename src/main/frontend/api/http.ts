// The app's fetch surface. A non-2xx REJECTS, so useQuery/useMutation surface the failure instead
// of a silent `if (res.ok)` that leaves the UI showing stale data.
// A non-2xx answer: its status tells a refusal (400) from a conflict with someone else's save (409).
export class HttpError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

async function send<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, init);
  if (!res.ok) {
    throw new HttpError(`${init?.method ?? 'GET'} ${path} -> ${res.status}`, res.status);
  }
  return res.status === 204 ? (undefined as T) : ((await res.json()) as T);
}

export function getJson<T>(path: string): Promise<T> {
  return send<T>(path);
}

// A change sends the signed-in name (`user`) as `X-User`, URI-encoded (a header is Latin-1 only),
// so the server records who made it.
const jsonHeaders = (user?: string): Record<string, string> =>
  user ? { 'Content-Type': 'application/json', 'X-User': encodeURIComponent(user) } : { 'Content-Type': 'application/json' };

export function postJson<T>(path: string, body: unknown, user?: string): Promise<T> {
  return send<T>(path, {
    method: 'POST',
    headers: jsonHeaders(user),
    body: JSON.stringify(body),
  });
}

export function putJson<T>(path: string, body?: unknown, user?: string): Promise<T> {
  return send<T>(path, {
    method: 'PUT',
    headers: jsonHeaders(user),
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}
