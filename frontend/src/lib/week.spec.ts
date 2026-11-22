import { describe, it, expect } from 'vitest';
import { addWeeks, formatRange, isValidWeek, mondayOf, weekOf, weeksInYear } from './week';

describe('weekOf', () => {
	it('uses the week-based year around new year', () => {
		// Exemplet i docs/databas.md
		expect(weekOf(new Date(2024, 11, 30))).toEqual({ year: 2025, week: 1 });
		expect(weekOf(new Date(2021, 0, 3))).toEqual({ year: 2020, week: 53 });
	});

	it('handles an ordinary week', () => {
		expect(weekOf(new Date(2026, 10, 22))).toEqual({ year: 2026, week: 47 });
		expect(weekOf(new Date(2026, 10, 16))).toEqual({ year: 2026, week: 47 });
	});
});

describe('mondayOf', () => {
	it('finds the monday', () => {
		expect(mondayOf({ year: 2026, week: 47 }).toISOString().slice(0, 10)).toBe('2026-11-16');
		expect(mondayOf({ year: 2025, week: 1 }).toISOString().slice(0, 10)).toBe('2024-12-30');
	});
});

describe('weeksInYear / isValidWeek', () => {
	it('knows which years have 53 weeks', () => {
		expect(weeksInYear(2026)).toBe(53);
		expect(weeksInYear(2025)).toBe(52);
		expect(isValidWeek({ year: 2026, week: 53 })).toBe(true);
		expect(isValidWeek({ year: 2025, week: 53 })).toBe(false);
		expect(isValidWeek({ year: 2026, week: 0 })).toBe(false);
		expect(isValidWeek({ year: NaN, week: 1 })).toBe(false);
	});
});

describe('addWeeks', () => {
	it('crosses year boundaries', () => {
		expect(addWeeks({ year: 2025, week: 52 }, 1)).toEqual({ year: 2026, week: 1 });
		expect(addWeeks({ year: 2027, week: 1 }, -1)).toEqual({ year: 2026, week: 53 });
	});
});

describe('formatRange', () => {
	it('formats within and across months', () => {
		expect(formatRange({ year: 2026, week: 47 })).toBe('16–22 nov');
		expect(formatRange({ year: 2025, week: 1 })).toBe('30 dec – 5 jan');
	});
});
