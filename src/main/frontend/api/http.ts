// The app's fetch surface. A non-2xx REJECTS, so useQuery/useMutation surface the failure instead
// of a silent `if (res.ok)` that leaves the UI showing stale data.
export class HttpError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

/** The date this screen shows the sheet as of (yyyy-MM-dd; the `asof` search param, see features/asof), or ''. While
 *  set, every read of a sheet asks for it as it stood then (`asOf`), and every change carries it (the X-As-Of header)
 *  so the server refuses it. */
export function viewAsOf(): string {
  const raw = new URLSearchParams(window.location.search).get('asof') ?? '';
  return /^"?(\d{4}-\d{2}-\d{2})"?$/.exec(raw)?.[1] ?? '';
}

async function send<T>(path: string, init?: RequestInit): Promise<T> {
  const asOf = viewAsOf();
  if (asOf !== '' && path.startsWith('/api/sheets/')) {
    if ((init?.method ?? 'GET') === 'GET') {
      path = `${path}${path.includes('?') ? '&' : '?'}asOf=${asOf}`;
    } else {
      init = { ...init, headers: { ...(init?.headers as Record<string, string>), 'X-As-Of': asOf } };
    }
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
