/**
 * Salary Calculator Web App - State Store & LocalStorage Manager
 * Exact schema matching Android SalaryRepository.kt DataStore
 */

export const ThemeMode = {
  SYSTEM: 'SYSTEM',
  LIGHT: 'LIGHT',
  DARK: 'DARK',
  SOLAR: 'SOLAR'
};

export const ThemePalette = {
  OCEAN: 'OCEAN',
  EMERALD: 'EMERALD',
  VIOLET: 'VIOLET',
  AMBER: 'AMBER'
};

export class Store {
  static PREFIX = 'salary_calc_';

  static get(key, defaultValue) {
    try {
      const item = localStorage.getItem(this.PREFIX + key);
      if (item === null || item === undefined) return defaultValue;
      return JSON.parse(item);
    } catch {
      return defaultValue;
    }
  }

  static set(key, value) {
    try {
      localStorage.setItem(this.PREFIX + key, JSON.stringify(value));
    } catch (e) {
      console.error('Failed to write to localStorage', e);
    }
  }

  // --- Core State Accessors ---
  static getHourlyRate() { return this.get('default_hourly_rate', 12.71); }
  static setHourlyRate(val) { this.set('default_hourly_rate', parseFloat(val) || 12.71); }

  static getStandardHours() { return this.get('standard_hours', 160.0); }
  static setStandardHours(val) { this.set('standard_hours', parseFloat(val) || 160.0); }

  static getHoursPerDay() { return this.get('default_hours_per_day', 12.0); }
  static setHoursPerDay(val) { this.set('default_hours_per_day', parseFloat(val) || 12.0); }

  static getTaxCode() { return this.get('tax_code', '1257L'); }
  static setTaxCode(val) { this.set('tax_code', (val || '1257L').toUpperCase().trim()); }

  static getTaxRegion() { return this.get('tax_region', 'UK_STANDARD'); }
  static setTaxRegion(val) { this.set('tax_region', val); }

  static getTaxYear() { return this.get('tax_year', 'YEAR_2024_2025'); }
  static setTaxYear(val) { this.set('tax_year', val); }

  static getPensionRate() { return this.get('pension_rate', 5.0); }
  static setPensionRate(val) { this.set('pension_rate', parseFloat(val) || 5.0); }

  static isSalarySacrifice() { return this.get('is_salary_sacrifice', true); }
  static setSalarySacrifice(val) { this.set('is_salary_sacrifice', Boolean(val)); }

  static getStudentLoanPlan() { return this.get('student_loan_plan', 'NONE'); }
  static setStudentLoanPlan(val) { this.set('student_loan_plan', val); }

  static getOvertimeMultiplier() { return this.get('overtime_multiplier', 1.5); }
  static setOvertimeMultiplier(val) { this.set('overtime_multiplier', parseFloat(val) || 1.5); }

  static getWeekendOvertimeMultiplier() { return this.get('weekend_overtime_multiplier', 2.0); }
  static setWeekendOvertimeMultiplier(val) { this.set('weekend_overtime_multiplier', parseFloat(val) || 2.0); }

  static getBankHolidayOvertimeMultiplier() { return this.get('bank_holiday_overtime_multiplier', 2.5); }
  static setBankHolidayOvertimeMultiplier(val) { this.set('bank_holiday_overtime_multiplier', parseFloat(val) || 2.5); }

  static getWeeklyOvertimeThreshold() { return this.get('weekly_overtime_threshold', 37.5); }
  static setWeeklyOvertimeThreshold(val) { this.set('weekly_overtime_threshold', parseFloat(val) || 37.5); }

  static hasMarriageAllowance() { return this.get('has_marriage_allowance', false); }
  static setMarriageAllowance(val) { this.set('has_marriage_allowance', Boolean(val)); }

  static hasBlindAllowance() { return this.get('has_blind_persons_allowance', false); }
  static setBlindAllowance(val) { this.set('has_blind_persons_allowance', Boolean(val)); }

  static getAnnualLeaveHourlyRate() { return this.get('annual_leave_hourly_rate', 0.0); }
  static setAnnualLeaveHourlyRate(val) { this.set('annual_leave_hourly_rate', parseFloat(val) || 0.0); }

  static getAnnualLeaveHoursPerDay() { return this.get('annual_leave_hours_per_day', 7.5); }
  static setAnnualLeaveHoursPerDay(val) { this.set('annual_leave_hours_per_day', parseFloat(val) || 7.5); }

  static getSspDailyRate() { return this.get('ssp_daily_rate', 23.35); }
  static setSspDailyRate(val) { this.set('ssp_daily_rate', parseFloat(val) || 23.35); }

  static getSspWaitingDays() { return this.get('ssp_waiting_days', 3); }
  static setSspWaitingDays(val) { this.set('ssp_waiting_days', parseInt(val) || 3); }

  static getThemeMode() { return this.get('theme_mode', ThemeMode.DARK); }
  static setThemeMode(val) { this.set('theme_mode', val); }

  static getThemePalette() { return this.get('theme_palette', ThemePalette.OCEAN); }
  static setThemePalette(val) { this.set('theme_palette', val); }

  static getPreferredCurrency() { return this.get('preferred_currency', 'EUR'); }
  static setPreferredCurrency(val) { this.set('preferred_currency', val); }

  static getSecondaryCurrency() { return this.get('secondary_currency', 'USD'); }
  static setSecondaryCurrency(val) { this.set('secondary_currency', val); }

  static getGoogleAccount() {
    return this.get('google_account', { email: '', name: '', picture: '', isAuthenticated: false });
  }
  static setGoogleAccount(acc) {
    this.set('google_account', acc);
  }

  // --- Shift Roster Schedule ---
  // Format: { "2026-9": { 1: 12.0, 2: 14.0, 3: -999.0, 4: -888.0 } }
  // -999.0 = Annual Leave (A/L)
  // -888.0 = Statutory Sick Pay (SSP)
  static getShiftSchedule() {
    return this.get('annual_shift_schedule', {});
  }
  static setShiftSchedule(schedule) {
    this.set('annual_shift_schedule', schedule);
  }

  static getMonthShifts(year, month) {
    const key = `${year}-${month}`;
    const schedule = this.getShiftSchedule();
    return schedule[key] || {};
  }

  static setDayShift(year, month, day, hours) {
    const key = `${year}-${month}`;
    const schedule = { ...this.getShiftSchedule() };
    const monthMap = { ...(schedule[key] || {}) };
    if (hours === 0.0) {
      delete monthMap[day];
    } else {
      monthMap[day] = hours;
    }
    if (Object.keys(monthMap).length === 0) {
      delete schedule[key];
    } else {
      schedule[key] = monthMap;
    }
    this.setShiftSchedule(schedule);
  }

  // --- Salary History Records ---
  static getSalaryHistory() {
    return this.get('salary_history', []);
  }
  static setSalaryHistory(history) {
    this.set('salary_history', history);
  }
  static addSalaryRecord(record) {
    const history = this.getSalaryHistory().filter(r => r.id !== record.id);
    history.unshift(record);
    this.setSalaryHistory(history);
  }

  // --- Employer Profiles ---
  static getProfiles() {
    return this.get('employer_profiles', [
      { id: 'primary_default', companyName: 'Primary Healthcare NHS / Agency', hourlyRate: 12.71, standardHoursPerDay: 12.0, taxCode: '1257L' }
    ]);
  }
  static setProfiles(profiles) {
    this.set('employer_profiles', profiles);
  }

  // --- Custom Deductions ---
  static getCustomDeductions() {
    return this.get('custom_deductions', []);
  }
  static setCustomDeductions(deductions) {
    this.set('custom_deductions', deductions);
  }

  // --- Full Backup Bundle Export & Import (100% APK Interoperability) ---
  static exportBackupBundle() {
    return {
      version: 2,
      exportTimestamp: new Date().toISOString(),
      deviceModel: 'Salary Calculator Web',
      appName: 'Salary Calculator',
      taxCode: this.getTaxCode(),
      defaultHourlyRate: this.getHourlyRate(),
      defaultHoursPerDay: this.getHoursPerDay(),
      themeMode: this.getThemeMode(),
      themePalette: this.getThemePalette(),
      taxRegion: this.getTaxRegion(),
      taxYear: this.getTaxYear(),
      pensionRate: this.getPensionRate(),
      studentLoanPlan: this.getStudentLoanPlan(),
      overtimeMultiplier: this.getOvertimeMultiplier(),
      weekendOvertimeMultiplier: this.getWeekendOvertimeMultiplier(),
      bankHolidayOvertimeMultiplier: this.getBankHolidayOvertimeMultiplier(),
      weeklyOvertimeThreshold: this.getWeeklyOvertimeThreshold(),
      hasMarriageAllowance: this.hasMarriageAllowance(),
      hasBlindPersonsAllowance: this.hasBlindAllowance(),
      annualLeaveHourlyRate: this.getAnnualLeaveHourlyRate(),
      annualLeaveHoursPerDay: this.getAnnualLeaveHoursPerDay(),
      sspDailyRate: this.getSspDailyRate(),
      sspWaitingDays: this.getSspWaitingDays(),
      googleAccountEmail: this.getGoogleAccount().email,
      googleAccountName: this.getGoogleAccount().name,
      annualShiftScheduleJson: JSON.stringify(this.getShiftSchedule()),
      employerProfilesJson: JSON.stringify(this.getProfiles()),
      customDeductionsJson: JSON.stringify(this.getCustomDeductions()),
      salaryHistoryJson: JSON.stringify(this.getSalaryHistory())
    };
  }

  static async syncToNasServer() {
    try {
      const bundle = this.exportBackupBundle();
      const email = this.getGoogleAccount().email || 'francyalinston@gmail.com';
      await fetch('/api/backup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, bundle })
      });
    } catch (e) {
      console.warn('NAS auto-sync deferred:', e);
    }
  }

  static importBackupBundle(bundle) {
    if (!bundle) return false;
    try {
      if (bundle.taxCode) this.setTaxCode(bundle.taxCode);
      const rate = bundle.hourlyRate !== undefined ? bundle.hourlyRate : bundle.defaultHourlyRate;
      if (rate !== undefined) this.setHourlyRate(rate);

      const hpd = bundle.hoursPerDay !== undefined ? bundle.hoursPerDay : bundle.defaultHoursPerDay;
      if (hpd !== undefined) this.setHoursPerDay(hpd);

      if (bundle.themeMode) this.setThemeMode(bundle.themeMode);
      if (bundle.themePalette) this.setThemePalette(bundle.themePalette);
      if (bundle.taxRegion) this.setTaxRegion(bundle.taxRegion);
      if (bundle.taxYear) this.setTaxYear(bundle.taxYear);
      if (bundle.pensionRate !== undefined) this.setPensionRate(bundle.pensionRate);
      if (bundle.studentLoanPlan) this.setStudentLoanPlan(bundle.studentLoanPlan);
      if (bundle.overtimeMultiplier !== undefined) this.setOvertimeMultiplier(bundle.overtimeMultiplier);
      if (bundle.weekendOvertimeMultiplier !== undefined) this.setWeekendOvertimeMultiplier(bundle.weekendOvertimeMultiplier);
      if (bundle.bankHolidayOvertimeMultiplier !== undefined) this.setBankHolidayOvertimeMultiplier(bundle.bankHolidayOvertimeMultiplier);
      if (bundle.weeklyOvertimeThreshold !== undefined) this.setWeeklyOvertimeThreshold(bundle.weeklyOvertimeThreshold);
      if (typeof bundle.hasMarriageAllowance === 'boolean') this.setMarriageAllowance(bundle.hasMarriageAllowance);
      if (typeof bundle.hasBlindPersonsAllowance === 'boolean') this.setBlindAllowance(bundle.hasBlindPersonsAllowance);
      if (bundle.annualLeaveHourlyRate !== undefined) this.setAnnualLeaveHourlyRate(bundle.annualLeaveHourlyRate);
      if (bundle.annualLeaveHoursPerDay !== undefined) this.setAnnualLeaveHoursPerDay(bundle.annualLeaveHoursPerDay);
      if (bundle.sspDailyRate !== undefined) this.setSspDailyRate(bundle.sspDailyRate);
      if (bundle.sspWaitingDays !== undefined) this.setSspWaitingDays(bundle.sspWaitingDays);

      // Annual Shift Schedule (JSON string or object)
      if (bundle.annualShiftScheduleJson) {
        try {
          const parsed = typeof bundle.annualShiftScheduleJson === 'string' 
            ? JSON.parse(bundle.annualShiftScheduleJson) 
            : bundle.annualShiftScheduleJson;
          this.setShiftSchedule(parsed);
        } catch {}
      }

      // Profiles (Array or JSON string)
      if (Array.isArray(bundle.profiles)) {
        this.setProfiles(bundle.profiles);
      } else if (bundle.employerProfilesJson) {
        try {
          const parsed = JSON.parse(bundle.employerProfilesJson);
          this.setProfiles(parsed);
        } catch {}
      }

      // Custom Deductions (Array or JSON string)
      if (Array.isArray(bundle.customDeductions)) {
        this.setCustomDeductions(bundle.customDeductions);
      } else if (bundle.customDeductionsJson) {
        try {
          const parsed = JSON.parse(bundle.customDeductionsJson);
          this.setCustomDeductions(parsed);
        } catch {}
      }

      // Salary History Records (Array from APK `records` or JSON string from Web `salaryHistoryJson`)
      if (Array.isArray(bundle.records)) {
        this.setSalaryHistory(bundle.records);
      } else if (bundle.salaryHistoryJson) {
        try {
          const parsed = JSON.parse(bundle.salaryHistoryJson);
          this.setSalaryHistory(parsed);
        } catch {}
      }

      return true;
    } catch (e) {
      console.error('Import failed', e);
      return false;
    }
  }
}
