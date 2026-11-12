import { currentAsOf } from '../session/session';

// The app's fetch surface. A non-2xx REJECTS, so useQuery/useMutation surface the failure instead
// of a silent `if (res.ok)` that leaves the UI showing stale data.
/** A refused request; `reason` is the text the server answered with (empty when none). */
export class HttpError extends Error {
  constructor(message: string, readonly reason: string, readonly status: number) {
    super(message);
  }
}

/** A change sent from a screen showing the sheet as of a date carries it, so the server refuses it. */
function withAsOf(init?: RequestInit): RequestInit | undefined {
  const asOf = currentAsOf();
  if (init?.method === undefined || asOf === '') return init;
  return { ...init, headers: { ...(init.headers as Record<string, string>), 'X-As-Of': asOf } };
}

async function send<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, withAsOf(init));
  if (!res.ok) {
    const reason = await res.text().catch(() => '');
    throw new HttpError(`${init?.method ?? 'GET'} ${path} -> ${res.status}`, reason, res.status);
  }
  return res.status === 204 ? (undefined as T) : ((await res.json()) as T);
}

export function getJson<T>(path: string): Promise<T> {
  return send<T>(path);
}

export function postJson<T>(path: string, body: unknown): Promise<T> {
  return send<T>(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
}

export function putJson<T>(path: string, body: unknown): Promise<T> {
  return send<T>(path, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
}
