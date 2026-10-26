// The app's fetch surface. A non-2xx REJECTS, so useQuery/useMutation surface the failure instead
// of a silent `if (res.ok)` that leaves the UI showing stale data.
/** A write refused because the record was saved by someone else since it was loaded (409). */
export class ConflictError extends Error {}

async function send<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, init);
  if (res.status === 409) {
    throw new ConflictError(`${init?.method ?? 'GET'} ${path} -> 409`);
  }
  if (!res.ok) {
    throw new Error(`${init?.method ?? 'GET'} ${path} -> ${res.status}`);
  }
  return res.status === 204 ? (undefined as T) : ((await res.json()) as T);
}

export function getJson<T>(path: string): Promise<T> {
  return send<T>(path);
}

// A write names the signed-in user (X-User) so the server can record who made the change.
function jsonHeaders(user?: string): HeadersInit {
  return user ? { 'Content-Type': 'application/json', 'X-User': user } : { 'Content-Type': 'application/json' };
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
