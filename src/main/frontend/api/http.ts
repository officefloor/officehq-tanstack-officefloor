// The app's fetch surface. A non-2xx REJECTS, so useQuery/useMutation surface the failure instead
// of a silent `if (res.ok)` that leaves the UI showing stale data. The rejection's message is the
// server's own `error` text when it sent one.
//
// Every request carries the signed-in name in X-User (the server records it as `by=<user>`).
let signedInUser = '';

/** A non-2xx answer: the server's message, and its status (409 when someone else saved first). */
export class HttpError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

/** Called by the session (features/session) whenever the signed-in name changes. */
export function setSignedInUser(name: string): void {
  signedInUser = name;
}

async function send<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  if (signedInUser) headers.set('X-User', signedInUser);
  const res = await fetch(path, { ...init, headers });
  if (!res.ok) {
    let message = `${init?.method ?? 'GET'} ${path} -> ${res.status}`;
    try {
      const body = (await res.json()) as { error?: string };
      if (body?.error) message = body.error;
    } catch {
      /* no JSON body: keep the status message */
    }
    throw new HttpError(message, res.status);
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
