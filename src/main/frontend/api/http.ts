// The app's fetch surface. A non-2xx REJECTS, so useQuery/useMutation surface the failure instead
// of a silent `if (res.ok)` that leaves the UI showing stale data.
/** A non-2xx response: its status and the text the server sent with it. */
export class HttpError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly body: string,
  ) {
    super(message);
  }
}

/** The date this screen shows the sheet as of ('' when it shows it as it is now): every read asks for the sheet as
 *  of it (?asOf=), and every change is sent marked with it (X-As-Of), so the server refuses it. Kept in step with
 *  the URL's asOf by features/sheet/asof; read from the URL at start-up so the first reads already carry it. */
let asOf = typeof window === 'undefined' ? '' : new URLSearchParams(window.location.search).get('asOf') ?? '';

export function setRequestAsOf(date: string): void {
  asOf = date;
}

function withAsOf(path: string): string {
  return asOf === '' ? path : `${path}${path.includes('?') ? '&' : '?'}asOf=${encodeURIComponent(asOf)}`;
}

function changeHeaders(): Record<string, string> {
  return asOf === '' ? { 'Content-Type': 'application/json' } : { 'Content-Type': 'application/json', 'X-As-Of': asOf };
}

async function send<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, init);
  if (!res.ok) {
    throw new HttpError(`${init?.method ?? 'GET'} ${path} -> ${res.status}`, res.status, await res.text());
  }
  return res.status === 204 ? (undefined as T) : ((await res.json()) as T);
}

export function getJson<T>(path: string): Promise<T> {
  return send<T>(withAsOf(path));
}

export function postJson<T>(path: string, body: unknown): Promise<T> {
  return send<T>(path, {
    method: 'POST',
    headers: changeHeaders(),
    body: JSON.stringify(body),
  });
}

export function putJson<T>(path: string, body: unknown): Promise<T> {
  return send<T>(path, {
    method: 'PUT',
    headers: changeHeaders(),
    body: JSON.stringify(body),
  });
}
