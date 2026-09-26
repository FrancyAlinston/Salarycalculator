/**
 * Interactive Shift Heatmap Calendar & Roster Engine
 * Exact visual and functional parity with Android ShiftHeatmapCard.kt and ShiftHeatmapWidgetProvider.kt
 */

import { Store } from './store.js';

export class HeatmapManager {
  constructor(containerId, onShiftChanged) {
    this.container = document.getElementById(containerId);
    this.onShiftChanged = onShiftChanged;
    const now = new Date();
    this.selectedYear = now.getFullYear();
    this.selectedMonth = now.getMonth() + 1;
  }

  setMonth(year, month) {
    this.selectedYear = year;
    this.selectedMonth = month;
    this.render();
  }

  nextMonth() {
    if (this.selectedMonth === 12) {
      this.selectedMonth = 1;
      this.selectedYear++;
    } else {
      this.selectedMonth++;
    }
    this.render();
  }

  prevMonth() {
    if (this.selectedMonth === 1) {
      this.selectedMonth = 12;
      this.selectedYear--;
    } else {
      this.selectedMonth--;
    }
    this.render();
  }

  resetToToday() {
    const now = new Date();
    this.selectedYear = now.getFullYear();
    this.selectedMonth = now.getMonth() + 1;
    this.render();
  }

  cycleDayShift(day) {
    const monthShifts = Store.getMonthShifts(this.selectedYear, this.selectedMonth);
    const current = monthShifts[day] || 0.0;
    const stdHours = Store.getHoursPerDay() || 12.0;

    let nextHours = 0.0;
    if (current === 0.0) {
      nextHours = stdHours; // Standard (Emerald)
    } else if (current === stdHours) {
      nextHours = stdHours + 2.0; // Overtime (Amber)
    } else if (current > stdHours && current > 0.0) {
      nextHours = -999.0; // Annual Leave (Blue A/L)
    } else if (current === -999.0) {
      nextHours = -888.0; // Statutory Sick Pay (Purple SSP)
    } else {
      nextHours = 0.0; // Off
    }

    Store.setDayShift(this.selectedYear, this.selectedMonth, day, nextHours);
    Store.syncToNasServer();
    this.render();
    if (this.onShiftChanged) this.onShiftChanged();
  }

  // Calculate Pay Day (Last Friday of month) and Cutoff (Sunday prior)
  getPayScheduleDetails() {
    const daysInMonth = new Date(this.selectedYear, this.selectedMonth, 0).getDate();
    let payDay = 0;
    for (let d = daysInMonth; d >= 1; d--) {
      const date = new Date(this.selectedYear, this.selectedMonth - 1, d);
      if (date.getDay() === 5) { // Friday
        payDay = d;
        break;
      }
    }
    const cutoffDay = Math.max(1, payDay - 5);
    return { payDay, cutoffDay };
  }

  render() {
    if (!this.container) return;

    const monthNames = [
      'January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'
    ];
    const monthTitle = `${monthNames[this.selectedMonth - 1]} ${this.selectedYear}`;

    const daysInMonth = new Date(this.selectedYear, this.selectedMonth, 0).getDate();
    const firstDayOfWeek = (new Date(this.selectedYear, this.selectedMonth - 1, 1).getDay() + 6) % 7; // Mon = 0, Sun = 6

    const monthShifts = Store.getMonthShifts(this.selectedYear, this.selectedMonth);
    const { payDay, cutoffDay } = this.getPayScheduleDetails();

    const now = new Date();
    const isCurrentCalendarMonth = (now.getFullYear() === this.selectedYear && now.getMonth() + 1 === this.selectedMonth);
    const todayDate = now.getDate();

    // Compute month totals
    let stdShiftsCount = 0;
    let otHoursTotal = 0;
    let alDaysCount = 0;
    let sspDaysCount = 0;
    let totalDutyHours = 0;

    for (let d = 1; d <= daysInMonth; d++) {
      const hrs = monthShifts[d] || 0.0;
      if (hrs === -888.0) {
        sspDaysCount++;
      } else if (hrs < 0.0) {
        alDaysCount++;
      } else if (hrs > 0.0) {
        stdShiftsCount++;
        totalDutyHours += hrs;
        const std = Store.getHoursPerDay() || 12.0;
        if (hrs > std) otHoursTotal += (hrs - std);
      }
    }

    // Header Controls & Summary
    let html = `
      <div class="glass-card rounded-2xl p-5 mb-6 border border-slate-700/60 shadow-xl backdrop-blur-xl">
        <!-- Month Navigation Header -->
        <div class="flex flex-wrap items-center justify-between gap-3 mb-4 pb-3 border-b border-slate-700/50">
          <div class="flex items-center gap-3">
            <h3 class="text-xl font-bold text-white tracking-wide flex items-center gap-2">
              <span class="text-emerald-400">📅</span> ${monthTitle}
            </h3>
            ${isCurrentCalendarMonth ? `<span class="px-2.5 py-0.5 text-xs font-semibold rounded-full bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">Current Month</span>` : ''}
          </div>
          <div class="flex items-center gap-2">
            <button id="btn-prev-month" class="p-2 rounded-xl bg-slate-800/80 hover:bg-slate-700 text-slate-300 hover:text-white transition-all border border-slate-700/60 active:scale-95">
              ◀ Prev
            </button>
            <button id="btn-reset-month" class="px-3 py-2 rounded-xl bg-slate-800/80 hover:bg-slate-700 text-slate-300 hover:text-white transition-all text-xs font-semibold border border-slate-700/60 active:scale-95">
              Today
            </button>
            <button id="btn-next-month" class="p-2 rounded-xl bg-slate-800/80 hover:bg-slate-700 text-slate-300 hover:text-white transition-all border border-slate-700/60 active:scale-95">
              Next ▶
            </button>
          </div>
        </div>

        <!-- Shift Stat Pills -->
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-2.5 mb-5 text-center">
          <div class="p-2.5 rounded-xl bg-emerald-950/40 border border-emerald-500/30">
            <span class="text-xs text-emerald-300 font-medium block">Duty Shifts</span>
            <span class="text-lg font-bold text-emerald-400">${stdShiftsCount} (${totalDutyHours.toFixed(1)}h)</span>
          </div>
          <div class="p-2.5 rounded-xl bg-amber-950/40 border border-amber-500/30">
            <span class="text-xs text-amber-300 font-medium block">Overtime</span>
            <span class="text-lg font-bold text-amber-400">${otHoursTotal.toFixed(1)}h OT</span>
          </div>
          <div class="p-2.5 rounded-xl bg-blue-950/40 border border-blue-500/30">
            <span class="text-xs text-blue-300 font-medium block">Annual Leave (A/L)</span>
            <span class="text-lg font-bold text-blue-400">${alDaysCount} Days</span>
          </div>
          <div class="p-2.5 rounded-xl bg-purple-950/40 border border-purple-500/30">
            <span class="text-xs text-purple-300 font-medium block">Sick Leave (SSP)</span>
            <span class="text-lg font-bold text-purple-400">${sspDaysCount} Days</span>
          </div>
        </div>

        <!-- 7-Column Day Names -->
        <div class="grid grid-cols-7 gap-1.5 sm:gap-2 mb-2 text-center text-xs font-semibold text-slate-400">
          <div>Mon</div><div>Tue</div><div>Wed</div><div>Thu</div><div>Fri</div><div>Sat</div><div>Sun</div>
        </div>

        <!-- Calendar Heatmap Grid -->
        <div class="grid grid-cols-7 gap-1.5 sm:gap-2">
    `;

    // Blank preceding days
    for (let i = 0; i < firstDayOfWeek; i++) {
      html += `<div class="h-14 sm:h-16 rounded-xl bg-slate-900/20 opacity-30 border border-slate-800/40"></div>`;
    }

    // Days in Month
    for (let day = 1; day <= daysInMonth; day++) {
      const hours = monthShifts[day] || 0.0;
      const isToday = (isCurrentCalendarMonth && day === todayDate);
      const isPayDay = (day === payDay);
      const isCutoff = (day === cutoffDay);

      let cellBg = 'bg-slate-800/40 hover:bg-slate-800/80 border-slate-700/40 text-slate-300';
      let badgeHtml = '';

      if (hours === -888.0) {
        // Statutory Sick Pay (Purple SSP)
        cellBg = 'bg-purple-950/80 hover:bg-purple-900/90 border-purple-500/50 text-purple-200 shadow-purple-900/20';
        badgeHtml = `<span class="px-1.5 py-0.5 rounded text-[10px] font-extrabold bg-purple-500 text-white shadow">SSP</span>`;
      } else if (hours < 0.0) {
        // Annual Leave (Blue A/L)
        cellBg = 'bg-blue-950/80 hover:bg-blue-900/90 border-blue-500/50 text-blue-200 shadow-blue-900/20';
        badgeHtml = `<span class="px-1.5 py-0.5 rounded text-[10px] font-extrabold bg-blue-500 text-white shadow">A/L</span>`;
      } else if (hours > 0.0) {
        const std = Store.getHoursPerDay() || 12.0;
        if (hours > std) {
          // Overtime (Amber)
          cellBg = 'bg-amber-950/80 hover:bg-amber-900/90 border-amber-500/50 text-amber-200 shadow-amber-900/20';
          badgeHtml = `<span class="px-1.5 py-0.5 rounded text-[10px] font-extrabold bg-amber-500 text-slate-950 shadow">${hours}h OT</span>`;
        } else {
          // Standard Duty Shift (Emerald)
          cellBg = 'bg-emerald-950/80 hover:bg-emerald-900/90 border-emerald-500/50 text-emerald-200 shadow-emerald-900/20';
          badgeHtml = `<span class="px-1.5 py-0.5 rounded text-[10px] font-extrabold bg-emerald-500 text-slate-950 shadow">${hours}h</span>`;
        }
      }

      // Today Outline Ring
      const todayRing = isToday ? 'ring-2 ring-cyan-400 ring-offset-2 ring-offset-slate-900' : '';

      html += `
        <div 
          data-day="${day}" 
          class="heatmap-cell h-14 sm:h-16 p-1.5 rounded-xl border flex flex-col justify-between items-center cursor-pointer transition-all duration-200 active:scale-90 select-none relative ${cellBg} ${todayRing}"
          title="Day ${day}: Tap to cycle shift status"
        >
          <div class="w-full flex items-center justify-between text-[11px] font-semibold leading-none">
            <span class="${isToday ? 'text-cyan-300 font-bold' : ''}">${day}</span>
            <div class="flex gap-1">
              ${isPayDay ? `<span title="Pay Day" class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>` : ''}
              ${isCutoff ? `<span title="Cutoff Date" class="w-2 h-2 rounded-full bg-rose-400"></span>` : ''}
            </div>
          </div>
          <div class="w-full flex items-center justify-center">
            ${badgeHtml}
          </div>
        </div>
      `;
    }

    html += `
        </div>

        <!-- Legend -->
        <div class="mt-4 pt-3 border-t border-slate-700/50 flex flex-wrap items-center justify-between gap-2 text-xs text-slate-400">
          <div class="flex items-center gap-3 flex-wrap">
            <span class="flex items-center gap-1.5"><span class="w-3 h-3 rounded bg-emerald-500"></span> Std (12h)</span>
            <span class="flex items-center gap-1.5"><span class="w-3 h-3 rounded bg-amber-500"></span> OT (14h)</span>
            <span class="flex items-center gap-1.5"><span class="w-3 h-3 rounded bg-blue-500"></span> A/L (Holiday)</span>
            <span class="flex items-center gap-1.5"><span class="w-3 h-3 rounded bg-purple-500"></span> SSP (Sick)</span>
            <span class="flex items-center gap-1.5"><span class="w-2 h-2 rounded-full bg-emerald-400"></span> Pay Day</span>
            <span class="flex items-center gap-1.5"><span class="w-2 h-2 rounded-full bg-rose-400"></span> Cutoff</span>
          </div>
          <span class="text-[11px] text-slate-500">Tap date to cycle status</span>
        </div>
      </div>
    `;

    // Render into all active containers (both full tab and calculator embed)
    const targetContainers = document.querySelectorAll(`#${this.containerId}, #heatmap-container, #calculator-heatmap-container`);
    if (targetContainers.length === 0) return;

    targetContainers.forEach(container => {
      container.innerHTML = html;

      container.querySelector('#btn-prev-month')?.addEventListener('click', () => this.prevMonth());
      container.querySelector('#btn-next-month')?.addEventListener('click', () => this.nextMonth());
      container.querySelector('#btn-reset-month')?.addEventListener('click', () => this.resetToToday());

      container.querySelectorAll('.heatmap-cell').forEach(cell => {
        cell.addEventListener('click', () => {
          const day = parseInt(cell.getAttribute('data-day'));
          if (day) this.cycleDayShift(day);
        });
      });
    });
  }
}
