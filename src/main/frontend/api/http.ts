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

async function send<T>(path: string, init?: RequestInit): Promise<T> {
  const url = viewAsOf === '' ? path : `${path}${path.includes('?') ? '&' : '?'}asof=${encodeURIComponent(viewAsOf)}`;
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
