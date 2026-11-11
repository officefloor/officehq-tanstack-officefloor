// The app's fetch surface. A non-2xx REJECTS, so useQuery/useMutation surface the failure instead
// of a silent `if (res.ok)` that leaves the UI showing stale data.
// A non-2xx response, carrying its status so a caller can tell one refusal from another.
export class HttpError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

// The as-of date the screen reads the sheet at ('' for now): the `asof` search param. While it is set
// every read names it, so the server answers with the sheet as it stood then, and every change is
// refused here, never sent — the form that made it shows its usual error. Loading a sheet is no
// change: it only re-reads one.
export const ASOF_PARAM = 'asof';

function asOf(): string {
  return new URLSearchParams(window.location.search).get(ASOF_PARAM) ?? '';
}

function refuseWhileAsOf(method: string, path: string): void {
  if (asOf() !== '' && !path.endsWith('/load')) {
    throw new HttpError(`${method} ${path} -> refused while viewing as of ${asOf()}`, 409);
  }
}

// The preview date the screen reads the sheet at ('' for none): the `preview` search param. While it
// is set (and no as-of date is) every read names it, so the server answers with the sheet's current
// records as if its today were that date; changes never carry it and use the sheet's real today.
export const PREVIEW_PARAM = 'preview';

export function previewDate(): string {
  if (asOf() !== '') return '';
  return new URLSearchParams(window.location.search).get(PREVIEW_PARAM) ?? '';
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
  const headers: Record<string, string> = {};
  if (user !== undefined) headers['X-User'] = encodeURIComponent(user);
  const date = asOf();
  if (date !== '') headers['X-As-Of'] = date;
  const preview = previewDate();
  if (preview !== '') headers['X-Preview'] = preview;
  return send<T>(path, { headers });
}

// A read at the sheet's real today, whatever preview date the screen shows (an as-of date still applies).
export function getJsonUnpreviewed<T>(path: string): Promise<T> {
  const date = asOf();
  return send<T>(path, date !== '' ? { headers: { 'X-As-Of': date } } : undefined);
}

// A read of the sheet as it is now, whatever as-of date the screen shows.
export function getJsonNow<T>(path: string): Promise<T> {
  return send<T>(path);
}

// A change is made by the signed-in user: name them, and the server records who made it.
function jsonHeaders(user?: string): HeadersInit {
  return user === undefined
    ? { 'Content-Type': 'application/json' }
    : { 'Content-Type': 'application/json', 'X-User': encodeURIComponent(user) };
}

export async function postJson<T>(path: string, body: unknown, user?: string): Promise<T> {
  refuseWhileAsOf('POST', path);
  return send<T>(path, {
    method: 'POST',
    headers: jsonHeaders(user),
    body: JSON.stringify(body),
  });
}

// A change to the signed-in user's own preferences, not to any sheet: open while viewing as of a date.
export async function postOwnJson<T>(path: string, body: unknown, user: string): Promise<T> {
  return send<T>(path, {
    method: 'POST',
    headers: jsonHeaders(user),
    body: JSON.stringify(body),
  });
}

export async function putJson<T>(path: string, body: unknown, user?: string): Promise<T> {
  refuseWhileAsOf('PUT', path);
  return send<T>(path, {
    method: 'PUT',
    headers: jsonHeaders(user),
    body: JSON.stringify(body),
  });
}
