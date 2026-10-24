import { useState } from 'react';
import { AppSession } from '../../slots/defs/appSession';
import { useCurrentUser } from './session';

// Sign in by typing a name (no password); the signed-in name shows once in.
function SignIn() {
  const [user, setUser] = useCurrentUser();
  const [name, setName] = useState('');

  if (user) {
    return (
      <span>
        Signed in as <strong data-testid="current-user">{user}</strong>
      </span>
    );
  }
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (name.trim()) setUser(name.trim());
      }}
    >
      <input data-testid="signin-name" placeholder="Your name" value={name} onChange={(e) => setName(e.target.value)} />
      <button type="submit" data-testid="signin-submit">
        Sign in
      </button>
    </form>
  );
}

export const contribution = AppSession.fill({ order: 0, Component: SignIn });
