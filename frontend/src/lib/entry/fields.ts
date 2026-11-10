// Formulärdefinitionen för veckoanteckningar. Svaren sparas i entry.content,
// nycklade på fältets id. Reglerna står i docs/databas.md.

/** Sparas i entry.schema_version. Höjs bara när befintliga svar måste tolkas om. */
export const SCHEMA_VERSION = 1;

/** Alla typer sparas som sträng i content; 'date' som ISO-datum (ÅÅÅÅ-MM-DD). */
export type FieldType = 'text' | 'textarea' | 'date';

export type Field = {
	/** Nyckeln i content. Får aldrig byta betydelse eller återanvändas. */
	id: string;
	type: FieldType;
	label: string;
	/** false = visas inte i nya formulär, men finns kvar för gamla anteckningar. */
	active: boolean;
};

export type EntryContent = Record<string, string>;

export const fields: Field[] = [
	{ id: 'summary', type: 'textarea', label: 'Summering', active: true },
	{ id: 'statements', type: 'textarea', label: 'Yrkanden/Yttranden', active: true },
	{ id: 'next_week', type: 'textarea', label: 'Nästa vecka', active: true },
	{ id: 'meals', type: 'textarea', label: 'Käk', active: true }
];

/**
 * Fälten som ska visas för en anteckning: alla aktiva, plus avaktiverade
 * som anteckningen har ett svar på.
 */
export function fieldsFor(content: EntryContent, all: Field[] = fields): Field[] {
	return all.filter((field) => field.active || Boolean(content[field.id]));
}
