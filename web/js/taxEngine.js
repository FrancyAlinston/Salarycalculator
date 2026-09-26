/**
 * Salary Calculator - UK Tax, National Insurance & Payroll Engine
 * 100% Mathematical Parity with Android TaxCalculator.kt
 */

export const TaxRegion = {
  UK_STANDARD: 'UK_STANDARD',
  SCOTLAND: 'SCOTLAND'
};

export const TaxYear = {
  YEAR_2024_2025: 'YEAR_2024_2025',
  YEAR_2025_2026: 'YEAR_2025_2026'
};

export const StudentLoanPlan = {
  NONE: 'NONE',
  PLAN_1: 'PLAN_1',
  PLAN_2: 'PLAN_2',
  PLAN_4: 'PLAN_4',
  PLAN_5: 'PLAN_5',
  POSTGRADUATE: 'POSTGRADUATE',
  PLAN_1_POSTGRAD: 'PLAN_1_POSTGRAD',
  PLAN_2_POSTGRAD: 'PLAN_2_POSTGRAD'
};

export class TaxEngine {

  static parseTaxCodeAllowance(taxCode = '1257L', hasMarriage = false, hasBlind = false) {
    let cleanCode = (taxCode || '1257L').trim().toUpperCase();
    let baseAllowance = 12570.0;

    if (cleanCode === '0T' || cleanCode === 'BR' || cleanCode === 'D0' || cleanCode === 'D1' || cleanCode === 'NT') {
      baseAllowance = 0.0;
    } else {
      const match = cleanCode.match(/^([S|C])?(\d+)([L|M|N|T|Y])?$/);
      if (match && match[2]) {
        baseAllowance = parseFloat(match[2]) * 10.0;
      }
    }

    if (hasMarriage) {
      baseAllowance += 1260.0;
    }
    if (hasBlind) {
      baseAllowance += 3070.0;
    }

    return Math.max(0.0, baseAllowance);
  }

  static computePAYE(taxableIncomeAnnual, taxRegion = TaxRegion.UK_STANDARD, taxCode = '1257L') {
    const code = (taxCode || '').trim().toUpperCase();
    if (code === 'NT') return 0.0;
    if (code === 'BR') return taxableIncomeAnnual * 0.20;
    if (code === 'D0') return taxableIncomeAnnual * 0.40;
    if (code === 'D1') return taxableIncomeAnnual * 0.45;

    if (taxRegion === TaxRegion.SCOTLAND || code.startsWith('S')) {
      // Scottish 6-Tier Tax Rates (2024/2025 & 2025/2026)
      // Starter: £0 - £2,306 (19%)
      // Basic: £2,306 - £13,991 (20%)
      // Intermediate: £13,991 - £31,092 (21%)
      // Higher: £31,092 - £62,430 (42%)
      // Advanced: £62,430 - £125,140 (45%)
      // Top: Above £125,140 (48%)
      let tax = 0.0;
      let remaining = Math.max(0.0, taxableIncomeAnnual);

      const starterChunk = Math.min(remaining, 2306.0);
      tax += starterChunk * 0.19;
      remaining = Math.max(0.0, remaining - 2306.0);

      const basicChunk = Math.min(remaining, 13991.0 - 2306.0);
      tax += basicChunk * 0.20;
      remaining = Math.max(0.0, remaining - (13991.0 - 2306.0));

      const interChunk = Math.min(remaining, 31092.0 - 13991.0);
      tax += interChunk * 0.21;
      remaining = Math.max(0.0, remaining - (31092.0 - 13991.0));

      const higherChunk = Math.min(remaining, 62430.0 - 31092.0);
      tax += higherChunk * 0.42;
      remaining = Math.max(0.0, remaining - (62430.0 - 31092.0));

      const advChunk = Math.min(remaining, 125140.0 - 62430.0);
      tax += advChunk * 0.45;
      remaining = Math.max(0.0, remaining - (125140.0 - 62430.0));

      tax += remaining * 0.48;
      return Math.max(0.0, tax);
    } else {
      // Standard UK Tax Rates
      // Basic: £0 - £37,700 (20%)
      // Higher: £37,700 - £125,140 (40%)
      // Additional: Above £125,140 (45%)
      let tax = 0.0;
      let remaining = Math.max(0.0, taxableIncomeAnnual);

      const basicChunk = Math.min(remaining, 37700.0);
      tax += basicChunk * 0.20;
      remaining = Math.max(0.0, remaining - 37700.0);

      const higherChunk = Math.min(remaining, 125140.0 - 37700.0);
      tax += higherChunk * 0.40;
      remaining = Math.max(0.0, remaining - (125140.0 - 37700.0));

      tax += remaining * 0.45;
      return Math.max(0.0, tax);
    }
  }

  static computeClass1NI(grossMonthly) {
    // Primary Threshold: £1,048/month (£12,576/year)
    // Upper Earnings Limit: £4,189/month (£50,268/year)
    // Rate: 8% main rate, 2% upper rate
    const PT = 1048.0;
    const UEL = 4189.0;

    if (grossMonthly <= PT) return 0.0;

    let ni = 0.0;
    if (grossMonthly > UEL) {
      ni += (UEL - PT) * 0.08;
      ni += (grossMonthly - UEL) * 0.02;
    } else {
      ni += (grossMonthly - PT) * 0.08;
    }
    return Math.max(0.0, ni);
  }

  static computeStudentLoan(grossMonthly, plan = StudentLoanPlan.NONE) {
    if (plan === StudentLoanPlan.NONE) return 0.0;

    let deduction = 0.0;
    const computePlanDeduction = (threshold, rate) => {
      return grossMonthly > threshold ? (grossMonthly - threshold) * rate : 0.0;
    };

    switch (plan) {
      case StudentLoanPlan.PLAN_1:
        deduction = computePlanDeduction(2082.91, 0.09); // £24,995/yr
        break;
      case StudentLoanPlan.PLAN_2:
        deduction = computePlanDeduction(2274.58, 0.09); // £27,295/yr
        break;
      case StudentLoanPlan.PLAN_4:
        deduction = computePlanDeduction(2616.25, 0.09); // £31,395/yr
        break;
      case StudentLoanPlan.PLAN_5:
        deduction = computePlanDeduction(2083.33, 0.09); // £25,000/yr
        break;
      case StudentLoanPlan.POSTGRADUATE:
        deduction = computePlanDeduction(1750.00, 0.06); // £21,000/yr
        break;
      case StudentLoanPlan.PLAN_1_POSTGRAD:
        deduction = computePlanDeduction(2082.91, 0.09) + computePlanDeduction(1750.00, 0.06);
        break;
      case StudentLoanPlan.PLAN_2_POSTGRAD:
        deduction = computePlanDeduction(2274.58, 0.09) + computePlanDeduction(1750.00, 0.06);
        break;
      default:
        deduction = 0.0;
    }
    return Math.max(0.0, deduction);
  }

  /**
   * Complete Comprehensive Monthly Salary Computation
   */
  static calculateSalary({
    hourlyRate = 12.71,
    standardHours = 160.0,
    overtimeHours = 0.0,
    overtimeMultiplier = 1.5,
    taxCode = '1257L',
    taxRegion = TaxRegion.UK_STANDARD,
    pensionPercent = 5.0,
    isSalarySacrifice = true,
    studentLoanPlan = StudentLoanPlan.NONE,
    hasMarriageAllowance = false,
    hasBlindAllowance = false,
    customPreTaxDeductions = 0.0,
    customPostTaxDeductions = 0.0,
    annualLeavePay = 0.0,
    statutorySickPay = 0.0,
    bonusAmount = 0.0
  }) {
    // 1. Hours & Gross Earnings
    const standardPay = standardHours * hourlyRate;
    const overtimePay = overtimeHours * (hourlyRate * overtimeMultiplier);
    const grossPay = Math.max(0.0, standardPay + overtimePay + annualLeavePay + statutorySickPay + bonusAmount);

    // 2. Pre-Tax Deductions & Pension
    const pensionContribution = (grossPay * (pensionPercent / 100.0));
    const totalPreTaxDeductions = customPreTaxDeductions + (isSalarySacrifice ? pensionContribution : 0.0);
    const adjustedGrossPay = Math.max(0.0, grossPay - totalPreTaxDeductions);

    // 3. Tax-Free Allowance with Personal Allowance Taper (£100k taper)
    let annualEquivalentGross = adjustedGrossPay * 12.0;
    let baseAnnualAllowance = this.parseTaxCodeAllowance(taxCode, hasMarriageAllowance, hasBlindAllowance);

    if (annualEquivalentGross > 100000.0) {
      const reduction = Math.min(baseAnnualAllowance, (annualEquivalentGross - 100000.0) / 2.0);
      baseAnnualAllowance = Math.max(0.0, baseAnnualAllowance - reduction);
    }
    const monthlyAllowance = baseAnnualAllowance / 12.0;

    // 4. Taxable Pay & PAYE Tax
    const taxablePayMonthly = Math.max(0.0, adjustedGrossPay - monthlyAllowance);
    const taxablePayAnnual = taxablePayMonthly * 12.0;
    const payeTaxAnnual = this.computePAYE(taxablePayAnnual, taxRegion, taxCode);
    const payeTaxMonthly = payeTaxAnnual / 12.0;

    // 5. National Insurance (Class 1)
    const niSubjectGross = isSalarySacrifice ? adjustedGrossPay : grossPay;
    const class1NI = this.computeClass1NI(niSubjectGross);

    // 6. Student Loan
    const studentLoanSubjectGross = isSalarySacrifice ? adjustedGrossPay : grossPay;
    const studentLoanDeduction = this.computeStudentLoan(studentLoanSubjectGross, studentLoanPlan);

    // 7. Post-Tax Pension (if relief at source) & Post-Tax Deductions
    const nonSacrificePension = (!isSalarySacrifice ? pensionContribution : 0.0);
    const totalPostTaxDeductions = customPostTaxDeductions + nonSacrificePension;

    // 8. Net Take-Home Pay
    const totalDeductions = payeTaxMonthly + class1NI + studentLoanDeduction + (isSalarySacrifice ? pensionContribution : 0.0) + totalPostTaxDeductions;
    const netPay = Math.max(0.0, grossPay - totalDeductions);

    // 9. Employer NI & Pension
    const employerSecThresholdMonthly = 758.0; // £9,100/yr (£758/mo)
    const employerNI = (grossPay > employerSecThresholdMonthly) ? (grossPay - employerSecThresholdMonthly) * 0.138 : 0.0;
    const employerPension = grossPay * 0.03; // Standard 3% statutory minimum
    const totalEmployerCost = grossPay + employerNI + employerPension;

    return {
      grossPay,
      standardPay,
      overtimePay,
      annualLeavePay,
      statutorySickPay,
      bonusAmount,
      pensionContribution,
      customPreTaxDeductions,
      adjustedGrossPay,
      monthlyAllowance,
      taxablePayMonthly,
      payeTaxMonthly,
      class1NI,
      studentLoanDeduction,
      customPostTaxDeductions,
      totalDeductions,
      netPay,
      takeHomePercent: grossPay > 0 ? (netPay / grossPay) * 100.0 : 0.0,
      annualGross: grossPay * 12.0,
      annualNet: netPay * 12.0,
      annualTax: payeTaxMonthly * 12.0,
      annualNI: class1NI * 12.0,
      employerNI,
      employerPension,
      totalEmployerCost
    };
  }
}
