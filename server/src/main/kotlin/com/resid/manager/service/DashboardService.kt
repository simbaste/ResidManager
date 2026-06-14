package com.resid.manager.service

import com.resid.manager.data.*
import com.resid.manager.dto.DashboardDataDto
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.util.UUID

object DashboardService {

    fun getDashboardData(
        residenceId: UUID, 
        filterType: String, 
        customStart: String?, 
        customEnd: String?
    ): DashboardDataDto {
        val now = LocalDate.now()
        val (startDate, endDate) = when (filterType.uppercase()) {
            "MONTH" -> {
                val start = now.withDayOfMonth(1)
                val end = now.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth())
                start to end
            }
            "QUARTER" -> {
                val month = now.monthValue
                val quarterStartMonth = ((month - 1) / 3) * 3 + 1
                val start = LocalDate.of(now.year, quarterStartMonth, 1)
                val end = start.plusMonths(2).with(java.time.temporal.TemporalAdjusters.lastDayOfMonth())
                start to end
            }
            "YEAR" -> {
                val start = LocalDate.of(now.year, 1, 1)
                val end = LocalDate.of(now.year, 12, 31)
                start to end
            }
            "CUSTOM" -> {
                val start = LocalDate.parse(customStart ?: now.toString())
                val end = LocalDate.parse(customEnd ?: now.toString())
                start to end
            }
            else -> {
                val start = now.withDayOfMonth(1)
                val end = now.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth())
                start to end
            }
        }

        return transaction {
            // 1. REVENUES COLLECTED
            val revenuesSum = FinancialTransactions
                .select(FinancialTransactions.amount.sum())
                .where { 
                    (FinancialTransactions.residenceId eq residenceId) and 
                    (FinancialTransactions.type eq "INCOME") and 
                    (FinancialTransactions.transactionDate.between(startDate, endDate)) 
                }
                .map { it[FinancialTransactions.amount.sum()] }
                .firstOrNull() ?: 0.0

            // 2. EXPENSES INCURRED
            val expensesSum = FinancialTransactions
                .select(FinancialTransactions.amount.sum())
                .where { 
                    (FinancialTransactions.residenceId eq residenceId) and 
                    (FinancialTransactions.type eq "EXPENSE") and 
                    (FinancialTransactions.transactionDate.between(startDate, endDate)) 
                }
                .map { it[FinancialTransactions.amount.sum()] }
                .firstOrNull() ?: 0.0

            val netCashflow = revenuesSum - expensesSum

            // 3. Occupancy Rate (Instant picture)
            val totalUnitsCount = Logements
                .select(Logements.id)
                .where { Logements.residenceId eq residenceId }
                .count()

            val occupiedUnitsCount = Logements
                .select(Logements.id)
                .where { (Logements.residenceId eq residenceId) and (Logements.status eq "OCCUPIED") }
                .count()

            val occupancyRate = if (totalUnitsCount > 0) {
                (occupiedUnitsCount.toDouble() / totalUnitsCount) * 100.0
            } else 0.0

            // 4. Delinquency Rate
            val bauxList = Lease.all().filter { it.logement.residence.id.value == residenceId }
            val totalRentAmountGenerated = bauxList.sumOf { it.logement.nominalRent }
            val unpaidRentAmount = bauxList.filter { 
                it.status == "PENDING_PAYMENT" || it.status == "DOWN_PAYMENT_PAID" 
            }.sumOf { it.logement.nominalRent }

            val delinquencyRate = if (totalRentAmountGenerated > 0.0) {
                (unpaidRentAmount / totalRentAmountGenerated) * 100.0
            } else 0.0

            DashboardDataDto(
                residenceId = residenceId.toString(),
                totalRevenuesCollected = revenuesSum,
                totalExpensesIncurred = expensesSum,
                netCashflow = netCashflow,
                delinquencyRate = delinquencyRate,
                occupancyRate = occupancyRate
            )
        }
    }
}
