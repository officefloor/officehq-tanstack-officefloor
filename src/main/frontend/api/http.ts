import { currentAsOf } from './asOf';

// The app's fetch surface. A non-2xx REJECTS, so useQuery/useMutation surface the failure instead
// of a silent `if (res.ok)` that leaves the UI showing stale data.
/** A write refused because the record was saved by someone else since it was loaded (409). */
export class ConflictError extends Error {}

/** A write refused by a rule the screen cannot see in full (422), e.g. a claim dated in a closed period; the message is the server's. */
export class RefusedError extends Error {}

async function send<T>(path: string, init?: RequestInit): Promise<T> {
  // A screen shown as of a date names it on every request: reads come back as of it, changes are refused (422).
  const asOf = currentAsOf();
  const res = await fetch(path, asOf ? { ...init, headers: { ...init?.headers, 'X-As-Of': asOf } } : init);
  if (res.status === 409) {
    throw new ConflictError(`${init?.method ?? 'GET'} ${path} -> 409`);
  }
  if (res.status === 422) {
    throw new RefusedError(await res.text());
  }
  if (!res.ok) {
    throw new Error(`${init?.method ?? 'GET'} ${path} -> ${res.status}`);
  }
  return res.status === 204 ? (undefined as T) : ((await res.json()) as T);
}

/** A read; with a user, it names the signed-in user (X-User) for a read whose answer depends on who asks. */
export function getJson<T>(path: string, user?: string): Promise<T> {
  return send<T>(path, user ? { headers: { 'X-User': user } } : undefined);
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
