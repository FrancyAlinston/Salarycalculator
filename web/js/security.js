/**
 * Salary Calculator Web App - Security & Anti-Inspection Guard
 * Protects confidential payroll numbers from browser inspection, context snooping,
 * and unauthenticated DOM exposure.
 */

export class SecurityGuard {
  static init() {
    // 1. Block Context Menu (Right-Click) on confidential elements & app shell
    document.addEventListener('contextmenu', (e) => {
      const target = e.target;
      if (target.closest('.glass-card') || target.closest('#app-shell') || target.closest('table')) {
        e.preventDefault();
        return false;
      }
    });

    // 2. Block Developer Tools & View-Source Keyboard Shortcuts
    document.addEventListener('keydown', (e) => {
      // F12 (DevTools)
      if (e.key === 'F12' || e.keyCode === 123) {
        e.preventDefault();
        return false;
      }
      // Ctrl+Shift+I / Cmd+Option+I (Inspect Element)
      if ((e.ctrlKey || e.metaKey) && e.shiftKey && (e.key === 'I' || e.key === 'i' || e.keyCode === 73)) {
        e.preventDefault();
        return false;
      }
      // Ctrl+Shift+J / Cmd+Option+J (Console)
      if ((e.ctrlKey || e.metaKey) && e.shiftKey && (e.key === 'J' || e.key === 'j' || e.keyCode === 74)) {
        e.preventDefault();
        return false;
      }
      // Ctrl+Shift+C / Cmd+Option+C (Inspect Selector)
      if ((e.ctrlKey || e.metaKey) && e.shiftKey && (e.key === 'C' || e.key === 'c' || e.keyCode === 67)) {
        e.preventDefault();
        return false;
      }
      // Ctrl+U / Cmd+Option+U (View Source)
      if ((e.ctrlKey || e.metaKey) && (e.key === 'u' || e.key === 'U' || e.keyCode === 85)) {
        e.preventDefault();
        return false;
      }
      // Ctrl+S / Cmd+S (Save Page HTML)
      if ((e.ctrlKey || e.metaKey) && (e.key === 's' || e.key === 'S' || e.keyCode === 83)) {
        e.preventDefault();
        return false;
      }
    });

    // 3. Clear and sanitize all confidential figures when user is unauthenticated
    this.sanitizeConfidentialDOM();
  }

  static sanitizeConfidentialDOM() {
    const sensitiveIds = [
      'res-monthly-net', 'res-annual-net', 'res-takehome-pct',
      'res-gross-pay', 'res-taxable-pay', 'res-paye-tax', 'res-ni-deduction',
      'res-pension-deduction', 'res-student-loan', 'res-effective-tax-rate',
      'res-hourly-rate-effective', 'res-breakdown-gross', 'res-breakdown-allowance',
      'res-breakdown-taxable', 'res-breakdown-tax', 'res-breakdown-ni',
      'res-breakdown-pension', 'res-breakdown-student-loan', 'res-breakdown-net'
    ];

    sensitiveIds.forEach(id => {
      const el = document.getElementById(id);
      if (el) el.textContent = '••••••';
    });
  }
}
