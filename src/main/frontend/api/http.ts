// The app's fetch surface. A non-2xx REJECTS, so useQuery/useMutation surface the failure instead
// of a silent `if (res.ok)` that leaves the UI showing stale data.
/** A non-2xx answer; `status` tells a refusal (400) from a conflict with someone else's save (409). */
export class HttpError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

/**
 * The date this screen shows the sheet as of (yyyy-MM-dd), or '' when it shows the sheet now. It is
 * the `asof` search param (features/session/AsOf), read here so every read of the sheet carries it
 * and every change is refused while it is set.
 */
export function currentAsOf(): string {
  const raw = new URLSearchParams(window.location.search).get('asof') ?? '';
  return raw.replace(/^"(.*)"$/, '$1');
}

/**
 * The date this screen previews the sheet's figures on (yyyy-MM-dd), or '' when it shows them by the sheet's today. It
 * is the `preview` search param (features/session/Preview), read here so every read of the sheet carries it; a change
 * is made as ever, by the sheet's real today.
 */
export function currentPreview(): string {
  const raw = new URLSearchParams(window.location.search).get('preview') ?? '';
  return raw.replace(/^"(.*)"$/, '$1');
}

async function send<T>(path: string, init?: RequestInit): Promise<T> {
  // A screen showing the sheet as of a date changes nothing; loading a sheet is not a change.
  if (init?.method && init.method !== 'GET' && path !== '/api/sheets' && currentAsOf()) {
    throw new HttpError(`${init.method} ${path} refused: the sheet is shown as of ${currentAsOf()}`, 400);
  }
  const res = await fetch(path, init);
  if (!res.ok) {
    throw new HttpError(`${init?.method ?? 'GET'} ${path} -> ${res.status}`, res.status);
  }
  return res.status === 204 ? (undefined as T) : ((await res.json()) as T);
}

export function getJson<T>(path: string): Promise<T> {
  const asOf = currentAsOf();
  if (asOf && path.startsWith('/api/sheets/')) {
    return send<T>(`${path}${path.includes('?') ? '&' : '?'}asOf=${encodeURIComponent(asOf)}`);
  }
  const preview = currentPreview();
  if (preview && path.startsWith('/api/sheets/')) {
    return send<T>(`${path}${path.includes('?') ? '&' : '?'}preview=${encodeURIComponent(preview)}`);
  }
  return send<T>(path);
}

/** Read the store as it holds it now, even while the screen shows the sheet as of a date. */
export function getLiveJson<T>(path: string): Promise<T> {
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
