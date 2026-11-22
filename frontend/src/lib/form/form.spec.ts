import { describe, it, expect } from 'vitest';
import {
	carriedOver,
	moveUndone,
	newItem,
	sameContent,
	toContent,
	toValues,
	type FormDefinition
} from './form';

const form: FormDefinition = {
	sections: [
		{
			title: 'Summering',
			fields: [
				{ id: 'mood', type: 'perPerson', label: 'Hur mår du?' },
				{ id: 'last_plan', type: 'checklist', label: 'Förra veckans plan', carryFrom: 'plan' }
			]
		},
		{ title: 'Nästa vecka', fields: [{ id: 'plan', type: 'checklist', label: '' }] },
		{ title: 'Käk', fields: [{ id: 'menu', type: 'weekdays', label: '' }] }
	]
};

describe('toValues', () => {
	it('gives every field a value of the right shape', () => {
		expect(toValues(form, {})).toEqual({ mood: {}, last_plan: [], plan: [], menu: {} });
	});

	it('reads text as one checklist item per line', () => {
		const values = toValues(form, { plan: 'Klippa sig\n\n  Panta  \n' });
		expect((values.plan as { text: string }[]).map((i) => i.text)).toEqual(['Klippa sig', 'Panta']);
	});

	it('ignores answers in an unexpected format', () => {
		expect(toValues(form, { mood: 'bra', menu: ['x'], plan: [{ nope: 1 }] })).toEqual({
			mood: {},
			last_plan: [],
			plan: [],
			menu: {}
		});
	});
});

describe('toContent', () => {
	it('leaves out empty answers and fields that are not in the form', () => {
		const content = toContent(form, {
			mood: { a: 'Bra', b: ' ' },
			plan: [newItem('Panta'), newItem('  ')],
			menu: {},
			last_plan: [],
			old: 'borttaget fält'
		});
		expect(Object.keys(content)).toEqual(['mood', 'plan']);
		expect(content.mood).toEqual({ a: 'Bra' });
	});
});

describe('sameContent', () => {
	it('ignores key order', () => {
		expect(sameContent({ a: { x: '1', y: '2' }, b: '3' }, { b: '3', a: { y: '2', x: '1' } })).toBe(
			true
		);
		expect(sameContent({ a: [1, 2] }, { a: [2, 1] })).toBe(false);
	});
});

describe('carriedOver', () => {
	it('copies the source field from the previous note, keeping what was done', () => {
		const content = carriedOver(form, {
			plan: [
				{ id: '1', text: 'Panta', done: false },
				{ id: '2', text: 'Klippa sig', done: true }
			]
		});
		expect(content.last_plan).toMatchObject([
			{ text: 'Panta', done: false },
			{ text: 'Klippa sig', done: true }
		]);
	});

	it('reads a text answer from an older form as a checklist', () => {
		expect(carriedOver(form, { plan: 'Panta\nTandläkaren' }).last_plan).toMatchObject([
			{ text: 'Panta' },
			{ text: 'Tandläkaren' }
		]);
	});

	it('gives nothing without a previous note', () => {
		expect(carriedOver(form, null)).toEqual({});
	});
});

describe('moveUndone', () => {
	it('moves undone items that are not already in the target', () => {
		const target = [newItem('Panta')];
		const moved = moveUndone(
			[newItem('panta'), newItem('Klippa sig'), newItem('Handla', true)],
			target
		);
		expect(moved).toBe(1);
		expect(target.map((i) => i.text)).toEqual(['Panta', 'Klippa sig']);
	});
});
