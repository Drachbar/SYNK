import { describe, it, expect } from 'vitest';
import { fields, fieldsFor, type Field } from './fields';

const all: Field[] = [
	{ id: 'a', type: 'textarea', label: 'A', active: true },
	{ id: 'old', type: 'text', label: 'Gammalt', active: false }
];

describe('fieldsFor', () => {
	it('shows only active fields for a new entry', () => {
		expect(fieldsFor({}, all).map((f) => f.id)).toEqual(['a']);
	});

	it('keeps an inactive field that the entry has an answer for', () => {
		expect(fieldsFor({ old: 'svar' }, all).map((f) => f.id)).toEqual(['a', 'old']);
	});
});

describe('fields', () => {
	it('has unique ids', () => {
		const ids = fields.map((f) => f.id);
		expect(new Set(ids).size).toBe(ids.length);
	});
});
