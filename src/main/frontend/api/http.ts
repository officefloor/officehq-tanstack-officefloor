// The app's fetch surface. A non-2xx REJECTS, so useQuery/useMutation surface the failure instead
// of a silent `if (res.ok)` that leaves the UI showing stale data.
/** A refused request: the reason, and the HTTP status (409: someone else saved the record first). */
export class HttpError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

// The date the signed-in user's screen shows the sheet as of (none: as it stands now). Every request carries
// it as `asof`: a read answers as the sheet stood at the end of that date, and a change is refused while it is set.
// It belongs to this screen only, so it is kept here and in the URL (search param `asof`), never in the store.
let viewAsOf = new URLSearchParams(window.location.search).get('asof') ?? '';

export function getViewAsOf(): string {
  return viewAsOf;
}

export function setViewAsOf(date: string): void {
  viewAsOf = date;
}

// Set only while readLive runs its read: the request then goes without the screen's as-of date.
let live = false;

/** Run the read (which must make its request at once) as the sheet stands now, whatever the screen's as-of date. */
export function readLive<T>(read: () => Promise<T>): Promise<T> {
  live = true;
  try {
    return read();
  } finally {
    live = false;
  }
}

async function send<T>(path: string, init?: RequestInit): Promise<T> {
  const asOf = live ? '' : viewAsOf;
  const url = asOf === '' ? path : `${path}${path.includes('?') ? '&' : '?'}asof=${encodeURIComponent(asOf)}`;
  const res = await fetch(url, init);
  if (!res.ok) {
    // A refusal carries {"error": "<why>"}; show that, else the status line.
    const body = (await res.json().catch(() => null)) as { error?: string } | null;
    throw new HttpError(body?.error ?? `${init?.method ?? 'GET'} ${path} -> ${res.status}`, res.status);
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

export function putJson<T>(path: string, body?: unknown): Promise<T> {
  return send<T>(path, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}
