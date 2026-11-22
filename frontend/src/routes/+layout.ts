// Run as a single-page app: everything renders in the browser, so API calls
// always go through the Vite dev proxy (or the same origin in production).
export const ssr = false;

import { error, redirect } from '@sveltejs/kit';
import type { Me } from '#lib/api.ts';
import type { LayoutLoad } from './$types';

/** Ser till att man är inloggad på alla sidor utom /login. */
export const load: LayoutLoad = async ({ fetch, url }) => {
	const onLogin = url.pathname === '/login';
	const res = await fetch('/api/auth/me');
	if (res.status === 401) {
		if (!onLogin) redirect(307, '/login');
		return { user: null };
	}
	if (!res.ok) error(res.status, 'Kunde inte nå servern');
	if (onLogin) redirect(307, '/');
	return { user: (await res.json()) as Me };
};
