// The app's fetch surface. A non-2xx REJECTS, so useQuery/useMutation surface the failure instead
// of a silent `if (res.ok)` that leaves the UI showing stale data.
/** A non-2xx answer; `status` lets a form tell one refusal from another. */
export class HttpError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

/** The date the screen shows the sheet as of (search param `asof`), or '' when it shows it as it is now. */
export function currentAsOf(): string {
  const raw = new URLSearchParams(window.location.search).get('asof') ?? '';
  return raw.replace(/^"|"$/g, '');
}

/** Every request carries the screen's as-of date: the server answers each read as the sheet stood then, and
 *  refuses every write while it is set. */
function withAsOf(path: string): string {
  const asOf = currentAsOf();
  return asOf ? `${path}${path.includes('?') ? '&' : '?'}asOf=${encodeURIComponent(asOf)}` : path;
}

async function send<T>(path: string, init?: RequestInit, now = false): Promise<T> {
  const res = await fetch(now ? path : withAsOf(path), init);
  if (!res.ok) {
    throw new HttpError(`${init?.method ?? 'GET'} ${path} -> ${res.status}`, res.status);
  }
  return res.status === 204 ? (undefined as T) : ((await res.json()) as T);
}

export function getJson<T>(path: string): Promise<T> {
  return send<T>(path);
}

/** A read answered as the sheet is now, whatever the screen's as-of date. */
export function getJsonNow<T>(path: string): Promise<T> {
  return send<T>(path, undefined, true);
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
