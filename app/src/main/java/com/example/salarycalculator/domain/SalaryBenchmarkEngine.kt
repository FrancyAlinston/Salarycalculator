package com.example.salarycalculator.domain

/**
 * Data structures and domain calculation engine for UK salary benchmarking,
 * industry percentile evaluation, and regional market analysis.
 */

data class IndustryRoleBenchmark(
    val categoryId: String,
    val title: String,
    val sector: String,
    val p10: Double, // 10th percentile (Entry)
    val p25: Double, // 25th percentile (Junior)
    val p50: Double, // 50th percentile (Median)
    val p75: Double, // 75th percentile (Senior)
    val p90: Double  // 90th percentile (Lead / Director)
)

enum class BenchmarkRegion(val displayName: String, val multiplier: Double) {
    NATIONAL_AVERAGE("UK National Average", 1.00),
    LONDON("Greater London (+22%)", 1.22),
    SOUTH_EAST("South East England (+10%)", 1.10),
    EAST_ENGLAND("East of England (+4%)", 1.04),
    MIDLANDS("West & East Midlands (-2%)", 0.98),
    NORTH_WEST("North West & Manchester (-5%)", 0.95),
    YORKSHIRE("Yorkshire & The Humber (-5%)", 0.95),
    SCOTLAND("Scotland (-3%)", 0.97),
    WALES("Wales (-8%)", 0.92),
    NORTHERN_IRELAND("Northern Ireland (-10%)", 0.90)
}

data class BenchmarkEvaluation(
    val roleTitle: String,
    val sector: String,
    val userAnnualGross: Double,
    val userHourlyRate: Double,
    val region: BenchmarkRegion,
    val adjustedP10: Double,
    val adjustedP25: Double,
    val adjustedP50: Double,
    val adjustedP75: Double,
    val adjustedP90: Double,
    val percentileRank: Int, // 0 to 100
    val quartileRating: String, // "Top 10%", "Above Median", "Below Median", etc.
    val gapToNextMilestone: Double,
    val nextMilestoneName: String,
    val summaryInsight: String
)

object SalaryBenchmarkEngine {

    val UK_ROLE_BENCHMARKS = listOf(
        IndustryRoleBenchmark("tech_swe", "Software Engineer / Developer", "Technology", 32000.0, 45000.0, 62000.0, 85000.0, 115000.0),
        IndustryRoleBenchmark("tech_data", "Data Analyst / Data Scientist", "Technology", 30000.0, 42000.0, 58000.0, 78000.0, 105000.0),
        IndustryRoleBenchmark("tech_devops", "DevOps / Cloud Engineer", "Technology", 35000.0, 48000.0, 68000.0, 92000.0, 125000.0),
        IndustryRoleBenchmark("health_nurse", "NHS Staff Nurse (Band 5/6)", "Healthcare", 28407.0, 32000.0, 37338.0, 43742.0, 50056.0),
        IndustryRoleBenchmark("health_doctor", "Medical Doctor / Registrar", "Healthcare", 38000.0, 48000.0, 64000.0, 88000.0, 120000.0),
        IndustryRoleBenchmark("finance_acct", "Chartered Accountant (ACA/ACCA)", "Finance", 28000.0, 38000.0, 52000.0, 72000.0, 98000.0),
        IndustryRoleBenchmark("finance_banking", "Financial Analyst / Banker", "Finance", 34000.0, 46000.0, 65000.0, 95000.0, 140000.0),
        IndustryRoleBenchmark("edu_teacher", "Secondary / Primary Teacher (QTS)", "Education", 30000.0, 34000.0, 41333.0, 48000.0, 56000.0),
        IndustryRoleBenchmark("construct_pm", "Construction Site / Project Manager", "Construction", 32000.0, 42000.0, 55000.0, 70000.0, 92000.0),
        IndustryRoleBenchmark("construct_trade", "Electrician / Plumber / Trade", "Construction", 26000.0, 34000.0, 44000.0, 56000.0, 72000.0),
        IndustryRoleBenchmark("legal_solicitor", "Solicitor / Legal Counsel", "Legal", 35000.0, 50000.0, 72000.0, 105000.0, 160000.0),
        IndustryRoleBenchmark("mktg_mgr", "Marketing / Product Manager", "Marketing", 28000.0, 38000.0, 50000.0, 68000.0, 90000.0),
        IndustryRoleBenchmark("sales_rep", "B2B Sales / Account Executive", "Sales", 26000.0, 36000.0, 48000.0, 70000.0, 110000.0),
        IndustryRoleBenchmark("logistics_hgv", "HGV Driver / Logistics Coordinator", "Logistics", 25000.0, 32000.0, 40000.0, 50000.0, 62000.0),
        IndustryRoleBenchmark("retail_mgr", "Retail Store / Operations Manager", "Retail", 24000.0, 28000.0, 36000.0, 46000.0, 60000.0),
        IndustryRoleBenchmark("hr_mgr", "Human Resources (HR) Advisor/Manager", "HR", 27000.0, 35000.0, 46000.0, 62000.0, 82000.0)
    )

    /**
     * Evaluates a given salary against industry and regional benchmarks.
     */
    fun evaluateBenchmark(
        annualGross: Double,
        roleId: String = "tech_swe",
        region: BenchmarkRegion = BenchmarkRegion.NATIONAL_AVERAGE
    ): BenchmarkEvaluation {
        val benchmark = UK_ROLE_BENCHMARKS.find { it.categoryId == roleId } ?: UK_ROLE_BENCHMARKS.first()
        val m = region.multiplier

        val p10 = benchmark.p10 * m
        val p25 = benchmark.p25 * m
        val p50 = benchmark.p50 * m
        val p75 = benchmark.p75 * m
        val p90 = benchmark.p90 * m

        val hourly = annualGross / (52.0 * 37.5)

        // Interpolate percentile
        val percentile = when {
            annualGross <= p10 -> ((annualGross / p10) * 10).toInt().coerceIn(1, 10)
            annualGross <= p25 -> (10 + ((annualGross - p10) / (p25 - p10)) * 15).toInt()
            annualGross <= p50 -> (25 + ((annualGross - p25) / (p50 - p25)) * 25).toInt()
            annualGross <= p75 -> (50 + ((annualGross - p50) / (p75 - p50)) * 25).toInt()
            annualGross <= p90 -> (75 + ((annualGross - p75) / (p90 - p75)) * 15).toInt()
            else -> (90 + ((annualGross - p90) / p90) * 10).toInt().coerceIn(90, 99)
        }

        val rating = when {
            percentile >= 90 -> "Top 10% (Elite / Lead)"
            percentile >= 75 -> "Top 25% (Senior / Upper Quartile)"
            percentile >= 50 -> "Above Median (Mid-Level)"
            percentile >= 25 -> "Lower Quartile (Developing)"
            else -> "Entry Level (Bottom 10%)"
        }

        val (gap, nextName) = when {
            annualGross < p25 -> Pair(p25 - annualGross, "25th Percentile (Junior)")
            annualGross < p50 -> Pair(p50 - annualGross, "50th Percentile (Market Median)")
            annualGross < p75 -> Pair(p75 - annualGross, "75th Percentile (Senior)")
            annualGross < p90 -> Pair(p90 - annualGross, "90th Percentile (Lead / Director)")
            else -> Pair(0.0, "Top 10% Achieved")
        }

        val insight = when {
            percentile >= 75 -> "Your salary is in the top quartile (${percentile}th percentile) for ${benchmark.title} roles in ${region.displayName}."
            percentile >= 50 -> "Your compensation is above market median (${percentile}th percentile), with an opportunity gap of £${"%,.0f".format(gap)} to reach senior grade."
            else -> "Your salary sits below market median (${percentile}th percentile). An increase of £${"%,.0f".format(gap)} is required to reach the regional median benchmark (£${"%,.0f".format(p50)})."
        }

        return BenchmarkEvaluation(
            roleTitle = benchmark.title,
            sector = benchmark.sector,
            userAnnualGross = annualGross,
            userHourlyRate = hourly,
            region = region,
            adjustedP10 = p10,
            adjustedP25 = p25,
            adjustedP50 = p50,
            adjustedP75 = p75,
            adjustedP90 = p90,
            percentileRank = percentile,
            quartileRating = rating,
            gapToNextMilestone = gap,
            nextMilestoneName = nextName,
            summaryInsight = insight
        )
    }
}
