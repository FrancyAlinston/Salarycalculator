/**
 * Main Web Application Controller for Salary Calculator
 */

import { TaxEngine, TaxRegion, TaxYear, StudentLoanPlan } from './taxEngine.js';
import { Store, ThemeMode, ThemePalette } from './store.js';
import { HeatmapManager } from './heatmap.js';
import { AuthManager } from './auth.js';
import { SecurityGuard } from './security.js';

class App {
  constructor() {
    this.currentTab = 'calculator';
    this.heatmap = null;
    this.chart = null;
    this.currencyRates = {
      EUR: 1.18,
      USD: 1.32,
      INR: 110.5,
      PHP: 74.2,
      CAD: 1.78,
      AUD: 1.95
    };
  }

  init() {
    SecurityGuard.init();
    this.applyThemeAndPalette();
    this.setupNavigation();
    this.setupCalculatorListeners();
    this.setupSettingsListeners();
    this.setupBackupRestore();
    this.setupAuth();
  }

  applyThemeAndPalette() {
    const theme = Store.getThemeMode();
    const palette = Store.getThemePalette();

    document.documentElement.setAttribute('data-theme', theme.toLowerCase());
    document.documentElement.setAttribute('data-palette', palette.toLowerCase());

    // Update Theme Buttons UI
    document.querySelectorAll('[data-set-theme]').forEach(btn => {
      const mode = btn.getAttribute('data-set-theme');
      btn.classList.toggle('ring-2', mode === theme);
      btn.classList.toggle('ring-primary', mode === theme);
    });

    document.querySelectorAll('[data-set-palette]').forEach(btn => {
      const pal = btn.getAttribute('data-set-palette');
      btn.classList.toggle('ring-2', pal === palette);
      btn.classList.toggle('ring-primary', pal === palette);
    });
  }

  setupNavigation() {
    const navButtons = document.querySelectorAll('[data-tab-target]');
    navButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        const target = btn.getAttribute('data-tab-target');
        this.switchTab(target);
      });
    });
  }

  switchTab(tabId) {
    this.currentTab = tabId;
    document.querySelectorAll('.tab-pane').forEach(pane => {
      pane.classList.add('hidden');
      pane.classList.remove('view-transition');
    });

    const activePane = document.getElementById(`tab-${tabId}`);
    if (activePane) {
      activePane.classList.remove('hidden');
      activePane.classList.add('view-transition');
    }

    document.querySelectorAll('[data-tab-target]').forEach(btn => {
      const isActive = btn.getAttribute('data-tab-target') === tabId;
      btn.classList.toggle('text-white', isActive);
      btn.classList.toggle('bg-slate-800/80', isActive);
      btn.classList.toggle('text-slate-400', !isActive);
    });

    if (tabId === 'history') {
      this.renderHistory();
    }
  }

  setupAuth() {
    const gateScreen = document.getElementById('auth-gate-screen');
    const appShell = document.getElementById('app-shell');
    const authChipName = document.getElementById('auth-chip-name');
    const authChipEmail = document.getElementById('auth-chip-email');
    const authChipStatus = document.getElementById('auth-chip-status');
    const authChipAvatar = document.getElementById('auth-chip-avatar');
    const authBtn = document.getElementById('btn-open-auth-modal');

    AuthManager.init(async (account) => {
      if (account && account.isAuthenticated) {
        // Authenticated: Hide Gatekeeper and Reveal Protected App
        if (gateScreen) {
          gateScreen.classList.add('hidden');
          gateScreen.classList.remove('flex');
        }
        if (appShell) {
          appShell.classList.remove('hidden');
          appShell.classList.remove('locked');
        }

        if (authChipName) authChipName.textContent = account.name || account.email;
        if (authChipEmail) authChipEmail.textContent = account.email;
        if (authChipStatus) {
          authChipStatus.classList.remove('bg-slate-500');
          authChipStatus.classList.add('bg-emerald-400');
        }
        if (account.picture && authChipAvatar) {
          authChipAvatar.src = account.picture;
        }

        // Auto-fetch latest NAS backup & synchronize state across Web and Android APK
        await this.loadNasBackupAndRefresh();
        this.startNasSyncPolling();

      } else {
        // Unauthenticated: Lock App and Enforce Mandatory Gatekeeper
        if (this.pollingInterval) clearInterval(this.pollingInterval);
        SecurityGuard.sanitizeConfidentialDOM();
        if (appShell) {
          appShell.classList.add('hidden');
          appShell.classList.add('locked');
        }
        if (gateScreen) {
          gateScreen.classList.remove('hidden');
          gateScreen.classList.add('flex');
        }

        if (authChipName) authChipName.textContent = 'Sign In';
        if (authChipEmail) authChipEmail.textContent = 'Locked';
        if (authChipStatus) {
          authChipStatus.classList.add('bg-slate-500');
          authChipStatus.classList.remove('bg-emerald-400');
        }
      }
    });

    authBtn?.addEventListener('click', () => {
      const current = Store.getGoogleAccount();
      if (current.isAuthenticated) {
        if (confirm(`Signed in as ${current.email}.\n\nDo you want to sign out and lock the Salary Calculator suite?`)) {
          AuthManager.logout();
        }
      } else {
        AuthManager.logout();
      }
    });
  }

  async loadNasBackupAndRefresh() {
    try {
      const email = Store.getGoogleAccount().email;
      const res = await fetch(`/api/backup?email=${encodeURIComponent(email || '')}`);
      if (res.ok) {
        const data = await res.json();
        if (data && data.success && data.backup) {
          Store.importBackupBundle(data.backup);
          this.lastSyncTimestamp = data.created_at || Date.now();
        }
      }
    } catch (e) {
      console.warn('Could not fetch initial NAS backup:', e);
    }

    this.populateInitialValues();

    if (!this.heatmap) {
      this.heatmap = new HeatmapManager('heatmap-container', () => {
        this.syncRosterHoursToCalculator();
        this.recalculate();
      });
    }
    this.heatmap.render();
    this.syncRosterHoursToCalculator();
    this.recalculate();
    this.fetchLiveCurrencyRates();
    if (this.currentTab === 'history') {
      this.renderHistory();
    }
  }

  startNasSyncPolling() {
    if (this.pollingInterval) clearInterval(this.pollingInterval);
    this.pollingInterval = setInterval(async () => {
      const currentAcc = Store.getGoogleAccount();
      if (!currentAcc || !currentAcc.isAuthenticated) return;
      try {
        const email = currentAcc.email;
        const res = await fetch(`/api/backup?email=${encodeURIComponent(email || '')}`);
        if (res.ok) {
          const data = await res.json();
          if (data && data.success && data.backup) {
            const serverTs = data.created_at || 0;
            if (serverTs > (this.lastSyncTimestamp || 0)) {
              this.lastSyncTimestamp = serverTs;
              Store.importBackupBundle(data.backup);
              this.populateInitialValues();
              this.heatmap?.render();
              this.syncRosterHoursToCalculator();
              this.recalculate();
              if (this.currentTab === 'history') {
                this.renderHistory();
              }
            }
          }
        }
      } catch (e) {
        // Quietly retry on next interval
      }
    }, 10000);
  }

  setupCalculatorListeners() {
    const inputs = [
      'input-hourly-rate',
      'input-standard-hours',
      'input-overtime-hours',
      'input-overtime-multiplier',
      'input-tax-code',
      'select-tax-region',
      'input-pension-rate',
      'switch-salary-sacrifice',
      'select-student-loan',
      'switch-marriage-allowance',
      'switch-blind-allowance'
    ];

    inputs.forEach(id => {
      const el = document.getElementById(id);
      if (!el) return;
      el.addEventListener('input', () => this.handleInputChange(id, el));
      el.addEventListener('change', () => this.handleInputChange(id, el));
    });

    // Save To History Record Button
    document.getElementById('btn-save-record')?.addEventListener('click', () => {
      this.saveCurrentToHistory();
    });

    // Populate Initial UI Values from Store
    this.populateInitialValues();
  }

  populateInitialValues() {
    this.setVal('input-hourly-rate', Store.getHourlyRate());
    this.setVal('input-standard-hours', Store.getStandardHours());
    this.setVal('input-overtime-multiplier', Store.getOvertimeMultiplier());
    this.setVal('input-tax-code', Store.getTaxCode());
    this.setVal('select-tax-region', Store.getTaxRegion());
    this.setVal('input-pension-rate', Store.getPensionRate());
    this.setChecked('switch-salary-sacrifice', Store.isSalarySacrifice());
    this.setVal('select-student-loan', Store.getStudentLoanPlan());
    this.setChecked('switch-marriage-allowance', Store.hasMarriageAllowance());
    this.setChecked('switch-blind-allowance', Store.hasBlindAllowance());
  }

  setVal(id, val) {
    const el = document.getElementById(id);
    if (el) el.value = val;
  }

  setChecked(id, checked) {
    const el = document.getElementById(id);
    if (el) el.checked = Boolean(checked);
  }

  handleInputChange(id, el) {
    switch (id) {
      case 'input-hourly-rate':
        Store.setHourlyRate(el.value);
        break;
      case 'input-standard-hours':
        Store.setStandardHours(el.value);
        break;
      case 'input-overtime-multiplier':
        Store.setOvertimeMultiplier(el.value);
        break;
      case 'input-tax-code':
        Store.setTaxCode(el.value);
        break;
      case 'select-tax-region':
        Store.setTaxRegion(el.value);
        break;
      case 'input-pension-rate':
        Store.setPensionRate(el.value);
        break;
      case 'switch-salary-sacrifice':
        Store.setSalarySacrifice(el.checked);
        break;
      case 'select-student-loan':
        Store.setStudentLoanPlan(el.value);
        break;
      case 'switch-marriage-allowance':
        Store.setMarriageAllowance(el.checked);
        break;
      case 'switch-blind-allowance':
        Store.setBlindAllowance(el.checked);
        break;
    }
    this.recalculate();
    Store.syncToNasServer();
  }

  syncRosterHoursToCalculator() {
    const now = new Date();
    const monthShifts = Store.getMonthShifts(now.getFullYear(), now.getMonth() + 1);
    const stdRate = Store.getHoursPerDay() || 12.0;

    let totalStd = 0.0;
    let totalOt = 0.0;

    Object.values(monthShifts).forEach(hrs => {
      if (hrs > 0.0) {
        if (hrs > stdRate) {
          totalStd += stdRate;
          totalOt += (hrs - stdRate);
        } else {
          totalStd += hrs;
        }
      }
    });

    if (totalStd > 0.0) {
      this.setVal('input-standard-hours', totalStd);
      Store.setStandardHours(totalStd);
    }
    if (totalOt >= 0.0) {
      this.setVal('input-overtime-hours', totalOt);
    }
  }

  recalculate() {
    const hourlyRate = parseFloat(document.getElementById('input-hourly-rate')?.value) || 12.71;
    const standardHours = parseFloat(document.getElementById('input-standard-hours')?.value) || 0.0;
    const overtimeHours = parseFloat(document.getElementById('input-overtime-hours')?.value) || 0.0;
    const overtimeMultiplier = parseFloat(document.getElementById('input-overtime-multiplier')?.value) || 1.5;
    const taxCode = document.getElementById('input-tax-code')?.value || '1257L';
    const taxRegion = document.getElementById('select-tax-region')?.value || 'UK_STANDARD';
    const pensionPercent = parseFloat(document.getElementById('input-pension-rate')?.value) || 0.0;
    const isSalarySacrifice = document.getElementById('switch-salary-sacrifice')?.checked ?? true;
    const studentLoanPlan = document.getElementById('select-student-loan')?.value || 'NONE';
    const hasMarriageAllowance = document.getElementById('switch-marriage-allowance')?.checked ?? false;
    const hasBlindAllowance = document.getElementById('switch-blind-allowance')?.checked ?? false;

    const result = TaxEngine.calculateSalary({
      hourlyRate,
      standardHours,
      overtimeHours,
      overtimeMultiplier,
      taxCode,
      taxRegion,
      pensionPercent,
      isSalarySacrifice,
      studentLoanPlan,
      hasMarriageAllowance,
      hasBlindAllowance
    });

    this.renderCalculationResults(result);
  }

  renderCalculationResults(res) {
    // Format helpers
    const fmt = (n) => '£' + n.toLocaleString('en-GB', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

    // Hero Card Values
    const netEl = document.getElementById('res-net-pay');
    if (netEl) netEl.textContent = fmt(res.netPay);

    const grossEl = document.getElementById('res-gross-pay');
    if (grossEl) grossEl.textContent = fmt(res.grossPay);

    const deductionsEl = document.getElementById('res-total-deductions');
    if (deductionsEl) deductionsEl.textContent = fmt(res.totalDeductions);

    const takeHomePctEl = document.getElementById('res-takehome-pct');
    if (takeHomePctEl) takeHomePctEl.textContent = `${res.takeHomePercent.toFixed(1)}% Take-Home`;

    // Breakdown Table Values
    this.setText('breakdown-standard-pay', fmt(res.standardPay));
    this.setText('breakdown-overtime-pay', fmt(res.overtimePay));
    this.setText('breakdown-gross-pay', fmt(res.grossPay));
    this.setText('breakdown-pension', fmt(res.pensionContribution));
    this.setText('breakdown-taxable-pay', fmt(res.taxablePayMonthly));
    this.setText('breakdown-paye-tax', fmt(res.payeTaxMonthly));
    this.setText('breakdown-ni', fmt(res.class1NI));
    this.setText('breakdown-student-loan', fmt(res.studentLoanDeduction));
    this.setText('breakdown-net-pay', fmt(res.netPay));

    // Annual Summary
    this.setText('annual-gross', fmt(res.annualGross));
    this.setText('annual-net', fmt(res.annualNet));
    this.setText('annual-tax', fmt(res.annualTax));
    this.setText('annual-ni', fmt(res.annualNI));

    // Currency Badges
    const eurRate = this.currencyRates.EUR || 1.18;
    const usdRate = this.currencyRates.USD || 1.32;
    this.setText('currency-eur-val', '€' + (res.netPay * eurRate).toLocaleString('en-GB', { minimumFractionDigits: 2, maximumFractionDigits: 2 }));
    this.setText('currency-usd-val', '$' + (res.netPay * usdRate).toLocaleString('en-GB', { minimumFractionDigits: 2, maximumFractionDigits: 2 }));
  }

  setText(id, text) {
    const el = document.getElementById(id);
    if (el) el.textContent = text;
  }

  async fetchLiveCurrencyRates() {
    try {
      const res = await fetch('https://open.er-api.com/v6/latest/GBP');
      if (res.ok) {
        const data = await res.json();
        if (data.rates) {
          this.currencyRates = data.rates;
          this.recalculate();
        }
      }
    } catch {
      // Graceful offline fallback
    }
  }

  saveCurrentToHistory() {
    const now = new Date();
    const monthNames = [
      'January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'
    ];
    const monthLabel = `${monthNames[now.getMonth()]} ${now.getFullYear()}`;

    const gross = parseFloat(document.getElementById('res-gross-pay')?.textContent.replace(/[^0-9.]/g, '')) || 0.0;
    const net = parseFloat(document.getElementById('res-net-pay')?.textContent.replace(/[^0-9.]/g, '')) || 0.0;
    const tax = parseFloat(document.getElementById('breakdown-paye-tax')?.textContent.replace(/[^0-9.]/g, '')) || 0.0;
    const ni = parseFloat(document.getElementById('breakdown-ni')?.textContent.replace(/[^0-9.]/g, '')) || 0.0;
    const pension = parseFloat(document.getElementById('breakdown-pension')?.textContent.replace(/[^0-9.]/g, '')) || 0.0;
    const stdHours = parseFloat(document.getElementById('input-standard-hours')?.value) || 0.0;
    const otHours = parseFloat(document.getElementById('input-overtime-hours')?.value) || 0.0;
    const hourlyRate = parseFloat(document.getElementById('input-hourly-rate')?.value) || Store.getHourlyRate();
    const hpd = Store.getHoursPerDay() || 12.0;
    const daysWorked = hpd > 0 ? (stdHours / hpd) : 0;

    const record = {
      id: 'rec_' + Date.now(),
      monthYear: monthLabel,
      month: monthLabel,
      timestamp: now.getTime(),
      daysWorked: daysWorked,
      hoursPerDay: hpd,
      overtimeHours: otHours,
      overtimeMultiplier: Store.getOvertimeMultiplier(),
      hourlyRate: hourlyRate,
      grossPay: gross,
      salarySacrifice: 0.0,
      pensionRate: Store.getPensionRate(),
      pensionContribution: pension,
      employerPension: 0.0,
      taxablePay: gross - pension,
      incomeTax: tax,
      nationalInsurance: ni,
      studentLoanPlan: Store.getStudentLoanPlan(),
      studentLoanDeduction: 0.0,
      totalDeductions: tax + ni + pension,
      netPay: net,
      taxCode: Store.getTaxCode(),
      taxRegion: Store.getTaxRegion(),
      note: 'Saved from Web Suite'
    };

    Store.addSalaryRecord(record);
    Store.syncToNasServer();
    alert(`Saved ${monthLabel} payslip record to History and synced with mobile app!`);
    if (this.currentTab === 'history') {
      this.renderHistory();
    }
  }

  renderHistory() {
    const container = document.getElementById('history-records-container');
    if (!container) return;

    const records = Store.getSalaryHistory();
    if (!records || records.length === 0) {
      container.innerHTML = `
        <div class="glass-card rounded-2xl p-8 text-center text-slate-400 border border-slate-700/60 shadow-xl">
          <div class="w-12 h-12 rounded-full bg-slate-800 flex items-center justify-center mx-auto mb-3 text-2xl">📑</div>
          <p class="text-base font-semibold text-white mb-1">No Saved Salary History Records</p>
          <p class="text-xs text-slate-400">Click "Save Monthly Record" in the calculator or log shifts in your Android app to record payslips.</p>
        </div>
      `;
      return;
    }

    let html = `<div class="grid gap-3">`;

    records.forEach(r => {
      const monthTitle = r.monthYear || r.month || 'Payslip Record';
      const taxCode = r.taxCode || '1257L';
      const gross = (r.grossPay || 0.0);
      const net = (r.netPay || 0.0);
      const tax = r.taxDeduction !== undefined ? r.taxDeduction : (r.incomeTax || 0.0);
      const ni = r.niDeduction !== undefined ? r.niDeduction : (r.nationalInsurance || 0.0);

      html += `
        <div class="glass-card rounded-2xl p-4 sm:p-5 border border-slate-700/60 shadow-lg flex flex-wrap items-center justify-between gap-4">
          <div>
            <h4 class="text-lg font-bold text-white flex items-center gap-2">
              <span>🗓️</span> ${monthTitle}
            </h4>
            <div class="flex items-center gap-2 mt-1 text-xs text-slate-400 font-mono">
              <span class="px-2 py-0.5 rounded bg-slate-800 border border-slate-700">Code: ${taxCode}</span>
              ${r.daysWorked ? `<span>${r.daysWorked.toFixed(1)} Days Worked</span>` : ''}
              ${r.hourlyRate ? `<span>@ £${r.hourlyRate.toFixed(2)}/h</span>` : ''}
            </div>
          </div>
          <div class="flex items-center gap-4 sm:gap-6 text-right">
            <div>
              <span class="text-xs text-slate-400 block">Gross Pay</span>
              <span class="text-sm sm:text-base font-semibold text-slate-200">£${gross.toFixed(2)}</span>
            </div>
            <div>
              <span class="text-xs text-rose-400/80 block">Tax + NI</span>
              <span class="text-xs sm:text-sm font-medium text-rose-300">-£${(tax + ni).toFixed(2)}</span>
            </div>
            <div>
              <span class="text-xs text-emerald-400 font-semibold block">Net Take-Home</span>
              <span class="text-base sm:text-lg font-bold text-emerald-300">£${net.toFixed(2)}</span>
            </div>
          </div>
        </div>
      `;
    });

    html += `</div>`;
    container.innerHTML = html;
  }

  setupSettingsListeners() {
    // Theme Mode Selection
    document.querySelectorAll('[data-set-theme]').forEach(btn => {
      btn.addEventListener('click', () => {
        const theme = btn.getAttribute('data-set-theme');
        Store.setThemeMode(theme);
        this.applyThemeAndPalette();
      });
    });

    // Theme Palette Selection
    document.querySelectorAll('[data-set-palette]').forEach(btn => {
      btn.addEventListener('click', () => {
        const pal = btn.getAttribute('data-set-palette');
        Store.setThemePalette(pal);
        this.applyThemeAndPalette();
      });
    });

    // Standard Shift Length in Settings
    const shiftLenInput = document.getElementById('settings-shift-length');
    if (shiftLenInput) {
      shiftLenInput.value = Store.getHoursPerDay();
      shiftLenInput.addEventListener('change', (e) => {
        Store.setHoursPerDay(e.target.value);
        this.heatmap?.render();
      });
    }
  }

  setupBackupRestore() {
    // JSON Backup Export
    document.getElementById('btn-export-backup')?.addEventListener('click', () => {
      const bundle = Store.exportBackupBundle();
      const json = JSON.stringify(bundle, null, 2);
      const blob = new Blob([json], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `SalaryCalculator_Backup_${new Date().toISOString().slice(0, 10)}.json`;
      a.click();
      URL.revokeObjectURL(url);
    });

    // JSON Backup Import
    const fileInput = document.getElementById('input-import-backup');
    document.getElementById('btn-import-backup')?.addEventListener('click', () => {
      fileInput?.click();
    });

    fileInput?.addEventListener('change', (e) => {
      const file = e.target.files?.[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = (event) => {
        try {
          const bundle = JSON.parse(event.target.result);
          const success = Store.importBackupBundle(bundle);
          if (success) {
            alert('Backup successfully imported! Reloading app state...');
            window.location.reload();
          } else {
            alert('Invalid backup file format.');
          }
        } catch {
          alert('Could not parse backup JSON file.');
        }
      };
      reader.readAsText(file);
    });
  }
}

// Instantiate and Initialize App on DOM Loaded
document.addEventListener('DOMContentLoaded', () => {
  const app = new App();
  app.init();
});
