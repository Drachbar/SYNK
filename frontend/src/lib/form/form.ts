// Veckoformuläret. Definitionen hämtas från backend (tabellen form_version) och svaren sparas
// i entry.content, nycklade på fältens id. Formatet beskrivs i docs/databas.md.

export type FieldType = 'text' | 'textarea' | 'perPerson' | 'checklist' | 'weekdays';

export type Field = {
	/** Nyckeln i content. Får aldrig återanvändas till något annat. */
	id: string;
	type: FieldType;
	/** Kan vara tom; då visas bara avsnittets rubrik. */
	label: string;
	/** Bara checklistor: fältet vars punkter hämtas från förra veckans anteckning. */
	carryFrom?: string | null;
};

export type Section = { title: string; fields: Field[] };
export type FormDefinition = { sections: Section[] };
export type Form = { version: number; definition: FormDefinition; createdAt: string };

export type User = { id: string; name: string };

export type ChecklistItem = { id: string; text: string; done: boolean };
/** text och textarea: sträng. perPerson: användar-id → text. weekdays: dag → text. */
export type Answer = string | Record<string, string> | ChecklistItem[];
/** Svaren som de sparas. Läses tolerant: ett fält kan saknas eller ha ett oväntat format. */
export type Content = Record<string, unknown>;
/** Svaren medan man skriver: varje fält i formuläret har ett värde i rätt format. */
export type Values = Record<string, Answer>;

export const FIELD_TYPES: { type: FieldType; label: string }[] = [
	{ type: 'textarea', label: 'Textruta' },
	{ type: 'text', label: 'En rad' },
	{ type: 'perPerson', label: 'En per person' },
	{ type: 'checklist', label: 'Checklista' },
	{ type: 'weekdays', label: 'Veckodagar' }
];

export const WEEKDAYS = [
	['mon', 'Måndag'],
	['tue', 'Tisdag'],
	['wed', 'Onsdag'],
	['thu', 'Torsdag'],
	['fri', 'Fredag'],
	['sat', 'Lördag'],
	['sun', 'Söndag']
] as const;

export function fieldsOf(form: FormDefinition): Field[] {
	return form.sections.flatMap((s) => s.fields);
}

function randomId(): string {
	return crypto.randomUUID().replaceAll('-', '').slice(0, 10);
}

/** Id för ett nytt fält. Slumpas, så att ett borttaget fälts id aldrig återanvänds. */
export function newFieldId(): string {
	return 'f' + randomId();
}

export function newItem(text: string, done = false): ChecklistItem {
	return { id: randomId(), text, done };
}

function isRecord(v: unknown): v is Record<string, unknown> {
	return typeof v === 'object' && v !== null && !Array.isArray(v);
}

function stringsOf(v: unknown): Record<string, string> {
	if (!isRecord(v)) return {};
	return Object.fromEntries(Object.entries(v).filter(([, s]) => typeof s === 'string')) as Record<
		string,
		string
	>;
}

/** Ett sparat svar i fältets format. Text blir en punkt per rad i en checklista. */
export function toAnswer(field: Field, raw: unknown): Answer {
	switch (field.type) {
		case 'text':
		case 'textarea':
			return typeof raw === 'string' ? raw : '';
		case 'perPerson':
		case 'weekdays':
			return stringsOf(raw);
		case 'checklist':
			if (typeof raw === 'string') {
				return raw
					.split('\n')
					.map((line) => line.trim())
					.filter((line) => line !== '')
					.map((line) => newItem(line));
			}
			if (!Array.isArray(raw)) return [];
			return raw
				.filter((i) => isRecord(i) && typeof i.text === 'string')
				.map((i) => ({
					id: typeof i.id === 'string' ? i.id : randomId(),
					text: i.text as string,
					done: i.done === true
				}));
	}
}

export function toValues(form: FormDefinition, content: Content): Values {
	return Object.fromEntries(fieldsOf(form).map((f) => [f.id, toAnswer(f, content[f.id])]));
}

/** Svaret som det ska sparas, eller undefined om det är tomt. */
function cleaned(answer: Answer): Answer | undefined {
	if (typeof answer === 'string') return answer.trim() === '' ? undefined : answer;
	if (Array.isArray(answer)) {
		const items = answer
			.filter((i) => i.text.trim() !== '')
			.map((i) => ({ id: i.id, text: i.text, done: i.done }));
		return items.length ? items : undefined;
	}
	const entries = Object.entries(answer).filter(([, v]) => v.trim() !== '');
	return entries.length ? Object.fromEntries(entries) : undefined;
}

/** Det som sparas: bara formulärets fält, och inga tomma svar. */
export function toContent(form: FormDefinition, values: Values): Content {
	const content: Content = {};
	for (const field of fieldsOf(form)) {
		const answer = values[field.id] === undefined ? undefined : cleaned(values[field.id]);
		if (answer !== undefined) content[field.id] = answer;
	}
	return content;
}

/** JSON med sorterade nycklar; jsonb returnerar inte nycklarna i samma ordning som de skickades. */
function canonical(v: unknown): string {
	if (Array.isArray(v)) return `[${v.map(canonical).join(',')}]`;
	if (isRecord(v)) {
		const keys = Object.keys(v).sort();
		return `{${keys.map((k) => `${JSON.stringify(k)}:${canonical(v[k])}`).join(',')}}`;
	}
	return JSON.stringify(v);
}

export function sameContent(a: Content, b: Content): boolean {
	return canonical(a) === canonical(b);
}

/**
 * Förifyllda svar för en ny vecka: checklistor med carryFrom får punkterna från förra
 * anteckningens fält, avbockade om de bockades av där.
 */
export function carriedOver(
	form: FormDefinition,
	previous: Content | null,
	only?: (field: Field) => boolean
): Content {
	if (!previous) return {};
	const content: Content = {};
	for (const field of fieldsOf(form)) {
		if (field.type !== 'checklist' || !field.carryFrom || (only && !only(field))) continue;
		// Läses som en checklista oavsett typ i förra veckans formulär; text blir en punkt per rad.
		const items = toAnswer(field, previous[field.carryFrom]);
		if (Array.isArray(items) && items.length) {
			content[field.id] = items.map((i) => newItem(i.text, i.done));
		}
	}
	return content;
}

/** Lägger till de ogjorda punkterna i target, utom de som redan finns där. Returnerar antalet. */
export function moveUndone(from: ChecklistItem[], target: ChecklistItem[]): number {
	const existing = new Set(target.map((i) => i.text.trim().toLowerCase()));
	let moved = 0;
	for (const item of from) {
		const key = item.text.trim().toLowerCase();
		if (item.done || key === '' || existing.has(key)) continue;
		target.push(newItem(item.text));
		existing.add(key);
		moved++;
	}
	return moved;
}
