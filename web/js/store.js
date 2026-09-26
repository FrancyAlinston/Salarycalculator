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

export const DEFAULT_2026_SHIFTS = {
  "2026-2": {"20": 12.0, "21": 12.0, "22": 12.0, "24": 12.0, "25": 12.0, "26": 12.0, "27": 12.0},
  "2026-3": {"3": 12.0, "4": 12.0, "7": 12.0, "8": 12.0, "9": 12.0, "12": 12.0, "14": 12.0, "15": 12.0, "17": 12.0, "18": 12.0, "20": 12.0, "22": 12.0, "24": 12.0, "25": 12.0, "26": 12.0, "27": -12.0, "29": 12.0, "31": 12.0},
  "2026-4": {"4": 12.0, "5": 12.0, "6": 12.0, "9": 12.0, "10": 12.0, "12": 12.0, "13": 12.0, "14": 12.0, "17": 12.0, "18": 12.0, "19": 12.0, "21": 12.0, "22": 12.0, "23": 12.0, "24": -12.0, "27": 12.0, "28": 12.0},
  "2026-5": {"2": 12.0, "3": 12.0, "4": 12.0, "7": 12.0, "8": 12.0, "10": 12.0, "11": 12.0, "13": 12.0, "15": 12.0, "16": 12.0, "17": 12.0, "19": -12.0, "20": 12.0, "21": 12.0, "22": 12.0, "25": 12.0, "26": 12.0},
  "2026-6": {"2": 12.0, "3": 12.0, "4": 12.0, "5": 12.0, "8": 12.0, "9": 12.0, "13": 12.0, "14": 12.0, "16": 12.0, "17": 12.0, "18": 12.0, "19": 12.0, "22": 12.0, "23": 12.0, "24": 12.0, "27": 12.0, "28": 12.0, "30": 12.0},
  "2026-7": {"1": 12.0, "3": 12.0, "5": 12.0, "6": 12.0, "7": 12.0, "11": 12.0, "12": 12.0, "15": -12.0, "16": -12.0, "17": -12.0, "20": 12.0, "21": 12.0, "22": 12.0, "25": 12.0, "26": 12.0, "28": 12.0, "29": 12.0, "30": 12.0},
  "2026-8": {"2": 12.0, "4": 12.0, "5": 12.0, "6": 12.0, "7": 12.0, "10": 12.0, "11": 12.0, "12": 12.0, "15": -12.0, "16": -12.0, "18": 12.0, "19": 12.0, "20": 12.0, "21": 12.0, "24": 12.0, "25": 12.0, "29": 12.0, "30": 12.0},
  "2026-9": {"1": 12.0, "3": 12.0, "4": 12.0, "7": 12.0, "8": 12.0, "11": 12.0, "12": 12.0, "15": 12.0, "16": 12.0, "17": 12.0, "18": 12.0, "20": 12.0, "21": 12.0, "22": 12.0, "26": 12.0, "27": 12.0, "29": 12.0, "30": 12.0}
};

export const DEFAULT_2026_RECORDS = [
  {
    id: "rec_august_2026",
    monthYear: "August 2026",
    month: "August 2026",
    timestamp: 1724580000000,
    daysWorked: 16.0,
    hoursPerDay: 12.0,
    overtimeHours: 0.0,
    overtimeMultiplier: 1.5,
    hourlyRate: 12.82,
    grossPay: 2461.44,
    salarySacrifice: 0.0,
    pensionRate: 0.0,
    pensionContribution: 0.0,
    employerPension: 0.0,
    taxablePay: 1413.02,
    incomeTax: 282.60,
    nationalInsurance: 113.07,
    studentLoanPlan: "NONE",
    studentLoanDeduction: 0.0,
    totalDeductions: 395.67,
    netPay: 2065.77,
    taxCode: "1257L",
    taxRegion: "UK_STANDARD",
    note: "2026 Rota Shift Calibration"
  }
];

export const DEFAULT_2026_PROFILES = [
  {
    id: "profile_care_worker",
    name: "Care Worker (Nights)",
    hourlyRate: 12.82,
    hoursPerDay: 12.0,
    taxCode: "1257L",
    pensionRate: 0.0,
    isPrimary: true
  }
];

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
  static getHourlyRate() { return this.get('default_hourly_rate', 12.82); }
  static setHourlyRate(val) { this.set('default_hourly_rate', parseFloat(val) || 12.82); }

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

  static getPensionRate() { return this.get('pension_rate', 0.0); }
  static setPensionRate(val) { this.set('pension_rate', parseFloat(val) || 0.0); }

  static isSalarySacrifice() { return this.get('is_salary_sacrifice', false); }
  static setSalarySacrifice(val) { this.set('is_salary_sacrifice', Boolean(val)); }

  static getStudentLoanPlan() { return this.get('student_loan_plan', 'NONE'); }
  static setStudentLoanPlan(val) { this.set('student_loan_plan', val); }

  static getOvertimeMultiplier() { return this.get('overtime_multiplier', 1.5); }
  static setOvertimeMultiplier(val) { this.set('overtime_multiplier', parseFloat(val) || 1.5); }

  static getWeekendOvertimeMultiplier() { return this.get('weekend_overtime_multiplier', 1.5); }
  static setWeekendOvertimeMultiplier(val) { this.set('weekend_overtime_multiplier', parseFloat(val) || 1.5); }

  static getBankHolidayOvertimeMultiplier() { return this.get('bank_holiday_overtime_multiplier', 2.0); }
  static setBankHolidayOvertimeMultiplier(val) { this.set('bank_holiday_overtime_multiplier', parseFloat(val) || 2.0); }

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
    return this.get('google_account', { email: 'francyalinston@gmail.com', name: 'Francy Alinston', picture: '', isAuthenticated: false });
  }
  static setGoogleAccount(acc) {
    this.set('google_account', acc);
  }

  // --- Shift Roster Schedule ---
  static getShiftSchedule() {
    const sched = this.get('annual_shift_schedule', null);
    if (!sched || Object.keys(sched).length === 0) {
      return DEFAULT_2026_SHIFTS;
    }
    return sched;
  }
  static setShiftSchedule(schedule) {
    this.set('annual_shift_schedule', schedule);
  }

  static getMonthShifts(year, month) {
    const schedule = this.getShiftSchedule();
    const key1 = `${year}-${month}`;
    const key2 = `${year}-${String(month).padStart(2, '0')}`;
    const key3 = `${month}`;
    const monthData = schedule[key1] || schedule[key2] || schedule[key3] || {};
    
    const result = {};
    if (monthData && typeof monthData === 'object') {
      Object.entries(monthData).forEach(([d, hrs]) => {
        const dayNum = parseInt(d);
        if (!isNaN(dayNum)) {
          result[dayNum] = parseFloat(hrs);
        }
      });
    }
    return result;
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
    const hist = this.get('salary_history', null);
    if (!hist || !Array.isArray(hist) || hist.length === 0) {
      return DEFAULT_2026_RECORDS;
    }
    return hist;
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
    const profs = this.get('employer_profiles', null);
    if (!profs || !Array.isArray(profs) || profs.length === 0) {
      return DEFAULT_2026_PROFILES;
    }
    return profs;
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
      const sched = bundle.annualShiftScheduleJson || bundle.annualShiftSchedule;
      if (sched) {
        try {
          const parsed = typeof sched === 'string' ? JSON.parse(sched) : sched;
          if (parsed && typeof parsed === 'object' && Object.keys(parsed).length > 0) {
            this.setShiftSchedule(parsed);
          }
        } catch {}
      }

      // Profiles (Array or JSON string)
      if (Array.isArray(bundle.profiles) && bundle.profiles.length > 0) {
        this.setProfiles(bundle.profiles);
      } else if (bundle.employerProfilesJson) {
        try {
          const parsed = JSON.parse(bundle.employerProfilesJson);
          if (Array.isArray(parsed) && parsed.length > 0) this.setProfiles(parsed);
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
      if (Array.isArray(bundle.records) && bundle.records.length > 0) {
        this.setSalaryHistory(bundle.records);
      } else if (Array.isArray(bundle.salary_history) && bundle.salary_history.length > 0) {
        this.setSalaryHistory(bundle.salary_history);
      } else if (bundle.salaryHistoryJson) {
        try {
          const parsed = JSON.parse(bundle.salaryHistoryJson);
          if (Array.isArray(parsed) && parsed.length > 0) this.setSalaryHistory(parsed);
        } catch {}
      }

      return true;
    } catch (e) {
      console.error('Import failed', e);
      return false;
    }
  }
}
