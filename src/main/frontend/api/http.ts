// The app's fetch surface. A non-2xx REJECTS, so useQuery/useMutation surface the failure instead
// of a silent `if (res.ok)` that leaves the UI showing stale data.
/** A non-2xx answer; `status` tells a conflict (409) from a refusal. */
export class HttpError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

/** A value the screen keeps in the URL under `key` ('': none). */
function dateParam(key: string): string {
  const raw = new URLSearchParams(window.location.search).get(key) ?? '';
  try {
    const parsed: unknown = JSON.parse(raw);
    return typeof parsed === 'string' ? parsed : raw;
  } catch {
    return raw;
  }
}

/** While the screen shows the sheet as of a date (`asof`), every request about it says so: the server
 *  answers a read as the sheet stood then and refuses every change. Otherwise, while the screen previews
 *  a date (`preview`), every read says so: the server answers it from the sheet's current records with
 *  that date as its today; a change is sent as it is, so it goes by the sheet's real today. */
function withAsOf(path: string, live: boolean, read: boolean): string {
  if (live || !path.startsWith('/api/sheets/')) return path;
  const sep = path.includes('?') ? '&' : '?';
  const date = dateParam('asof');
  if (date !== '') return `${path}${sep}shown-asof=${encodeURIComponent(date)}`;
  // The preview is the signed-in user's: one left in the URL by someone signed in before is not theirs.
  const preview = dateParam('preview-by') === dateParam('user') ? dateParam('preview') : '';
  if (preview !== '' && read) return `${path}${sep}shown-preview=${encodeURIComponent(preview)}`;
  return path;
}

/** A read: any GET, and loading the sheet. */
function isRead(path: string, init?: RequestInit): boolean {
  return (init?.method ?? 'GET') === 'GET' || /^\/api\/sheets\/[^/?]+\/open(\?|$)/.test(path);
}

async function send<T>(path: string, init?: RequestInit, live = false): Promise<T> {
  const res = await fetch(withAsOf(path, live, isRead(path, init)), init);
  if (!res.ok) {
    throw new HttpError(`${init?.method ?? 'GET'} ${path} -> ${res.status}`, res.status);
  }
  return res.status === 204 ? (undefined as T) : ((await res.json()) as T);
}

/** `live`: read the sheet as it is now, even while the screen shows it as of a date. */
export function getJson<T>(path: string, live = false): Promise<T> {
  return send<T>(path, undefined, live);
}

export function postJson<T>(path: string, body: unknown, live = false): Promise<T> {
  return send<T>(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }, live);
}

export function putJson<T>(path: string, body: unknown): Promise<T> {
  return send<T>(path, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
}
