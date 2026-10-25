// The app's fetch surface. A non-2xx REJECTS, so useQuery/useMutation surface the failure instead
// of a silent `if (res.ok)` that leaves the UI showing stale data.
// A non-2xx answer: its status tells a refusal (400) from a conflict with someone else's save (409).
export class HttpError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

// The as-of view: while the screen's `asof` search param holds a date, every read of a sheet asks for the
// sheet as it stood at the end of that date, and every change is refused here, before it reaches the store
// (so each form and action shows its usual error). Loading a sheet (PUT /api/sheets/<id>) is not a change.
export const ASOF_PARAM = 'asof';

/** The date the screen shows the sheet as of (YYYY-MM-DD), or '' while it shows the sheet as it is now. */
export function currentAsOf(): string {
  const raw = new URLSearchParams(window.location.search).get(ASOF_PARAM) ?? '';
  try {
    const parsed: unknown = JSON.parse(raw);
    return typeof parsed === 'string' ? parsed : raw;
  } catch {
    return raw;
  }
}

const isSheetLoad = (path: string) => /^\/api\/sheets\/[^/?]+$/.test(path);

// The preview: while the screen's `preview` search param holds a date (and no as-of date is set), every read of
// a sheet's records asks for them with that date standing in for the sheet's today, and the screen works out its
// figures on it (session.ts). The sheet itself (GET /api/sheets/<id>) is read as it is, so the sheet's today and
// every default date of a change stay the real ones; changes are sent as usual and use the sheet's real today.
export const PREVIEW_PARAM = 'preview';

/** The date the screen previews the sheet's today as (YYYY-MM-DD), or '' while it shows the sheet's own today. */
export function currentPreview(): string {
  if (currentAsOf()) return '';
  const raw = new URLSearchParams(window.location.search).get(PREVIEW_PARAM) ?? '';
  try {
    const parsed: unknown = JSON.parse(raw);
    return typeof parsed === 'string' ? parsed : raw;
  } catch {
    return raw;
  }
}

/** The date a figure is worked out on: the preview date while one is set, else the sheet's today. */
export function figureToday(today: string): string;
export function figureToday(today: string | undefined): string | undefined;
export function figureToday(today: string | undefined): string | undefined {
  return currentPreview() || today;
}

async function send<T>(path: string, init?: RequestInit, live = false): Promise<T> {
  const asOf = live ? '' : currentAsOf();
  if (asOf && init?.method && init.method !== 'GET' && !isSheetLoad(path)) {
    throw new HttpError(`${init.method} ${path} -> refused while the sheet is shown as of ${asOf}`, 400);
  }
  if (asOf && !init?.method && path.startsWith('/api/sheets/')) {
    path += `${path.includes('?') ? '&' : '?'}asOf=${encodeURIComponent(asOf)}`;
  }
  const preview = live ? '' : currentPreview();
  if (preview && !init?.method && path.startsWith('/api/sheets/') && !isSheetLoad(path)) {
    path += `${path.includes('?') ? '&' : '?'}preview=${encodeURIComponent(preview)}`;
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

// A read of the store as it is now, whatever date the screen shows the sheet as of.
export function getJsonLive<T>(path: string): Promise<T> {
  return send<T>(path, undefined, true);
}

// A change sends the signed-in name (`user`) as `X-User`, URI-encoded (a header is Latin-1 only),
// so the server records who made it.
const jsonHeaders = (user?: string): Record<string, string> =>
  user ? { 'Content-Type': 'application/json', 'X-User': encodeURIComponent(user) } : { 'Content-Type': 'application/json' };

export function postJson<T>(path: string, body: unknown, user?: string): Promise<T> {
  return send<T>(path, {
    method: 'POST',
    headers: jsonHeaders(user),
    body: JSON.stringify(body),
  });
}

export function putJson<T>(path: string, body?: unknown, user?: string, live = false): Promise<T> {
  return send<T>(path, {
    method: 'PUT',
    headers: jsonHeaders(user),
    body: body === undefined ? undefined : JSON.stringify(body),
  }, live);
}

// A change that is not a change to the sheet (a user's own preference): made whatever date the screen shows the
// sheet as of.
export function putJsonLive<T>(path: string, body?: unknown, user?: string): Promise<T> {
  return putJson<T>(path, body, user, true);
}
