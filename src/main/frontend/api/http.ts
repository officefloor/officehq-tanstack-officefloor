// The app's fetch surface. A non-2xx REJECTS, so useQuery/useMutation surface the failure instead
// of a silent `if (res.ok)` that leaves the UI showing stale data.
/** A non-2xx answer; `status` tells a conflict (409) from a refusal. */
export class HttpError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

/** The date the screen shows the sheet as of ('': as it is now), from the URL (`asof`). */
function asOf(): string {
  const raw = new URLSearchParams(window.location.search).get('asof') ?? '';
  try {
    const parsed: unknown = JSON.parse(raw);
    return typeof parsed === 'string' ? parsed : raw;
  } catch {
    return raw;
  }
}

/** While the screen shows the sheet as of a date, every request about it says so: the server answers
 *  a read as the sheet stood then and refuses every change. */
function withAsOf(path: string): string {
  const date = asOf();
  if (date === '' || !path.startsWith('/api/sheets/')) return path;
  return `${path}${path.includes('?') ? '&' : '?'}shown-asof=${encodeURIComponent(date)}`;
}

async function send<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(withAsOf(path), init);
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
