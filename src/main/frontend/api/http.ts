// The app's fetch surface. A non-2xx REJECTS, so useQuery/useMutation surface the failure instead
// of a silent `if (res.ok)` that leaves the UI showing stale data.
// A non-2xx response, carrying its status so a caller can tell one refusal from another.
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

// A read that depends on who is signed in names them too.
export function getJson<T>(path: string, user?: string): Promise<T> {
  return user === undefined ? send<T>(path) : send<T>(path, { headers: { 'X-User': encodeURIComponent(user) } });
}

// A change is made by the signed-in user: name them, and the server records who made it.
function jsonHeaders(user?: string): HeadersInit {
  return user === undefined
    ? { 'Content-Type': 'application/json' }
    : { 'Content-Type': 'application/json', 'X-User': encodeURIComponent(user) };
}

export function postJson<T>(path: string, body: unknown, user?: string): Promise<T> {
  return send<T>(path, {
    method: 'POST',
    headers: jsonHeaders(user),
    body: JSON.stringify(body),
  });
}

export function putJson<T>(path: string, body: unknown, user?: string): Promise<T> {
  return send<T>(path, {
    method: 'PUT',
    headers: jsonHeaders(user),
    body: JSON.stringify(body),
  });
}
