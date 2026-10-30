// The app's fetch surface. A non-2xx REJECTS, so useQuery/useMutation surface the failure instead
// of a silent `if (res.ok)` that leaves the UI showing stale data.
/** A non-2xx answer; `status` lets a form tell a conflict (409) from a refusal. */
export class HttpError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

/** The status a change is refused with while the screen is shown as of a date. */
export const AS_OF_REFUSED = 423;

/** Whether the screen is shown as of a date (the `asof` search param, see features/asof): every change is refused. */
function viewingAsOf(): boolean {
  return new URLSearchParams(window.location.search).get('asof') ? true : false;
}

async function send<T>(path: string, init?: RequestInit): Promise<T> {
  // An as-of view shows the sheet as it stood: a change asked for from it is refused, as any refused change, and
  // nothing reaches the store.
  if ((init?.method ?? 'GET') !== 'GET' && viewingAsOf()) {
    throw new HttpError(`${init?.method} ${path} -> refused: the screen is shown as of a date`, AS_OF_REFUSED);
  }
  const res = await fetch(path, init);
  if (!res.ok) {
    throw new HttpError(`${init?.method ?? 'GET'} ${path} -> ${res.status}`, res.status);
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
