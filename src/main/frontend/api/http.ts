// The app's fetch surface. A non-2xx REJECTS, so useQuery/useMutation surface the failure instead
// of a silent `if (res.ok)` that leaves the UI showing stale data.
/** A non-2xx answer; `status` lets a caller tell one refusal from another (e.g. 409 Conflict). */
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

// `user` is the signed-in name, sent as X-User so the server can record who made a change.
const headers = (user?: string): Record<string, string> => ({
  'Content-Type': 'application/json',
  ...(user ? { 'X-User': user } : {}),
});

export function postJson<T>(path: string, body: unknown, user?: string): Promise<T> {
  return send<T>(path, {
    method: 'POST',
    headers: headers(user),
    body: JSON.stringify(body),
  });
}

export function putJson<T>(path: string, body: unknown, user?: string): Promise<T> {
  return send<T>(path, {
    method: 'PUT',
    headers: headers(user),
    body: JSON.stringify(body),
  });
}
