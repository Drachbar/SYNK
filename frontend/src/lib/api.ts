// Anrop mot backend. Inloggning och CSRF beskrivs i docs/inloggning.md, resten i docs/api.md.

import type { Content } from './form/form';

export type Me = { id: string; name: string };

export type EntryResponse = {
	isoYear: number;
	isoWeek: number;
	formVersion: number;
	content: Content;
	version: number;
	updatedAt: string;
	updatedBy: string;
};

export type EntrySummary = {
	isoYear: number;
	isoWeek: number;
	updatedAt: string;
	updatedBy: string;
};

/** Läses på nytt inför varje anrop, eftersom token byts vid in- och utloggning. */
function csrfToken(): string {
	const token = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/)?.[1] ?? '';
	return decodeURIComponent(token);
}

/** fetch med CSRF-token på anrop som ändrar något. */
export function send(path: string, init: RequestInit): Promise<Response> {
	const headers = new Headers(init.headers);
	headers.set('X-XSRF-TOKEN', csrfToken());
	return fetch(path, { ...init, headers });
}

export function formatTimestamp(iso: string): string {
	return new Date(iso).toLocaleString('sv-SE', {
		day: 'numeric',
		month: 'short',
		hour: '2-digit',
		minute: '2-digit'
	});
}
