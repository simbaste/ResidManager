package com.resid.manager.helpers

import com.resid.manager.dto.AppCurrency
import com.resid.manager.dto.ElectricityStatementDto
import com.resid.manager.dto.ElectricityStatusDto
import com.resid.manager.dto.EntityTypeDto
import com.resid.manager.dto.EquipmentDto
import com.resid.manager.dto.FinanceTransactionDto
import com.resid.manager.dto.GuarantorDto
import com.resid.manager.dto.InvitationStatusDto
import com.resid.manager.dto.LeaseCategory
import com.resid.manager.dto.LeaseDto
import com.resid.manager.dto.LeasePaymentDto
import com.resid.manager.dto.LeaseStatusDto
import com.resid.manager.dto.PaymentFrequencyDto
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceMemberSummaryDto
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.dto.RoleDto
import com.resid.manager.dto.TransactionCategoryDto
import com.resid.manager.dto.TransactionTypeDto
import com.resid.manager.dto.UnitStatusDto
import com.resid.manager.dto.UserRole
import com.resid.manager.features.electricity.mvi.ElectricityUiState

fun <T> tryOptional(block: () -> T): T? {
    return try {
        block()
    } catch (_: Exception) {
        null
    }
}

suspend fun <T> tryOptionalSuspend(block: suspend  () -> T): T? {
    return try {
        block()
    } catch (_: Exception) {
        null
    }
}

object PreviewResidenceUnits {

    val acSplit = EquipmentDto(
        id = "eq-1",
        key = "ac_unit",
        label = "Climatiseur Split 12000 BTU"
    )
    val fridgeInox = EquipmentDto(
        id = "eq-2",
        key = "kitchen",
        label = "Réfrigérateur Combiné Inox"
    )
    val doubleBed = EquipmentDto(
        id = "eq-3",
        key = "bed",
        label = "Lit Double 160x200 & Literie"
    )
    val diningSet = EquipmentDto(
        id = "eq-4",
        key = "table_restaurant",
        label = "Table & 2 Chaises Scandinaves"
    )
    val washingMachine = EquipmentDto(
        id = "eq-5",
        key = "local_laundry_service",
        label = "Lave-linge Hublot 8kg"
    )
    val waterHeater = EquipmentDto(
        id = "eq-6",
        key = "water_heater",
        label = "Chauffe-eau Électrique 100L"
    )

    val unitA1 = ResidenceUnitDto(
        id = "unit-a1",
        residenceId = "res-euro",
        name = "Unité A1",
        floor = "1er étage",
        type = "Studio",
        nominalRent = 500.0,
        serviceCharges = 50.0,
        initialElectricityIndex = 1849.0,
        status = UnitStatusDto.AVAILABLE,
        photoUrl = "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800&q=80",
        equipments = listOf(acSplit, fridgeInox, doubleBed, diningSet),
        currentTenantName = null
    )

    val unitB2 = ResidenceUnitDto(
        id = "unit-b2",
        residenceId = "res-euro",
        name = "Unité B2",
        floor = "2e étage",
        type = "T2",
        nominalRent = 750.0,
        serviceCharges = 65.0,
        initialElectricityIndex = 3420.5,
        status = UnitStatusDto.AVAILABLE,
        photoUrl = "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800&q=80",
        equipments = listOf(waterHeater),
        currentTenantName = null
    )

    val unitC1 = ResidenceUnitDto(
        id = "unit-c1",
        residenceId = "res-euro",
        name = "Unité C1",
        floor = "3e étage",
        type = "Studio",
        nominalRent = 520.0,
        serviceCharges = 45.0,
        initialElectricityIndex = 912.0,
        status = UnitStatusDto.AVAILABLE,
        photoUrl = "https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800&q=80",
        equipments = listOf(acSplit, fridgeInox, doubleBed, washingMachine),
        currentTenantName = null
    )

    val unitD3Reserved = ResidenceUnitDto(
        id = "unit-d3",
        residenceId = "res-euro",
        name = "Unité D3",
        floor = "Rez-de-chaussée",
        type = "T3",
        nominalRent = 950.0,
        serviceCharges = 90.0,
        initialElectricityIndex = 5104.2,
        status = UnitStatusDto.RESERVED,
        photoUrl = null, // Tests null photo fallback in UI
        equipments = listOf(acSplit, fridgeInox, washingMachine, waterHeater),
        currentTenantName = "Hervé Nsigne"
    )

    val unitA2Occupied = ResidenceUnitDto(
        id = "unit-a2",
        residenceId = "res-euro",
        name = "Unité A2",
        floor = "1er étage",
        type = "T2",
        nominalRent = 500.0,
        serviceCharges = 50.0,
        initialElectricityIndex = 2780.0,
        status = UnitStatusDto.OCCUPIED,
        photoUrl = "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=800&q=80",
        equipments = emptyList(), // Tests empty equipments state in UI
        currentTenantName = "Gladys Naoussi"
    )

    val allUnits: List<ResidenceUnitDto> = listOf(
        unitA1,
        unitB2,
        unitC1,
        unitD3Reserved,
        unitA2Occupied
    )

    val availableUnits: List<ResidenceUnitDto> = allUnits.filter {
        it.status == UnitStatusDto.AVAILABLE
    }
}

object PreviewResidences {

    // Secondary residence units to test residence switching & filtering
    val palmierUnit101 = ResidenceUnitDto(
        id = "unit-p101",
        residenceId = "res-palmiers",
        name = "Appartement P1",
        floor = "1er étage",
        type = "T2",
        nominalRent = 620.0,
        serviceCharges = 55.0,
        initialElectricityIndex = 1205.0,
        status = UnitStatusDto.AVAILABLE,
        photoUrl = "https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800&q=80",
        equipments = listOf(
            PreviewResidenceUnits.acSplit,
            PreviewResidenceUnits.fridgeInox,
            PreviewResidenceUnits.doubleBed
        ),
        currentTenantName = null
    )

    val palmierUnit202 = ResidenceUnitDto(
        id = "unit-p202",
        residenceId = "res-palmiers",
        name = "Appartement P2",
        floor = "2e étage",
        type = "T3",
        nominalRent = 890.0,
        serviceCharges = 80.0,
        initialElectricityIndex = 4310.8,
        status = UnitStatusDto.OCCUPIED,
        photoUrl = "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800&q=80",
        equipments = listOf(
            PreviewResidenceUnits.acSplit,
            PreviewResidenceUnits.washingMachine,
            PreviewResidenceUnits.waterHeater
        ),
        currentTenantName = "Armand Teko"
    )
}

object PreviewElectricityData {

    // 1. Residence Contexts (EUR & XOF/FCFA)
    val residenceEuroContext = ResidenceContext(
        residenceId = "res-euro",
        residenceName = "Residence EURO",
        residenceAddress = "14 Avenue de la République, Entrée Nord, Lille",
        userRoleInResidence = UserRole.OWNER,
        totalUnits = 5,
        currencySymbol = AppCurrency.EUR.symbol,
        currencyCode = AppCurrency.EUR.code,
        kWhPrice = 0.25,
        photoUrl = "https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?w=800&q=80"
    )

    val residencePalmiersContext = ResidenceContext(
        residenceId = "res-palmiers",
        residenceName = "Résidence Les Palmiers",
        residenceAddress = "28 Rue des Jardins, Quartier Bastos",
        userRoleInResidence = UserRole.OWNER,
        totalUnits = 8,
        currencySymbol = AppCurrency.XOF.symbol,
        currencyCode = AppCurrency.XOF.code,
        kWhPrice = 150.0,
        photoUrl = "https://images.unsplash.com/photo-1460317442991-0ec209397118?w=800&q=80"
    )

    // 2. Units for Electricity Metering (linked to res-euro)
    val unitA2 = ResidenceUnitDto(
        id = "unit-a2",
        residenceId = "res-euro",
        name = "A2",
        floor = "1er étage",
        type = "T2",
        nominalRent = 500.0,
        serviceCharges = 50.0,
        initialElectricityIndex = 2780.0,
        status = UnitStatusDto.OCCUPIED,
        photoUrl = "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=800&q=80",
        equipments = emptyList(),
        currentTenantName = "Gladys Naoussi"
    )

    val unitA4 = ResidenceUnitDto(
        id = "unit-a4",
        residenceId = "res-euro",
        name = "A4",
        floor = "2e étage",
        type = "T3",
        nominalRent = 700.0,
        serviceCharges = 75.0,
        initialElectricityIndex = 4100.0,
        status = UnitStatusDto.OCCUPIED,
        photoUrl = "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800&q=80",
        equipments = emptyList(),
        currentTenantName = "Armand Teko"
    )

    val unitA1 = ResidenceUnitDto(
        id = "unit-a1",
        residenceId = "res-euro",
        name = "A1",
        floor = "1er étage",
        type = "Studio",
        nominalRent = 500.0,
        serviceCharges = 50.0,
        initialElectricityIndex = 1849.0,
        status = UnitStatusDto.RESERVED,
        photoUrl = "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800&q=80",
        equipments = emptyList(),
        currentTenantName = "Hervé Nsigne"
    )

    val unitB2Vacant = ResidenceUnitDto(
        id = "unit-b2",
        residenceId = "res-euro",
        name = "B2",
        floor = "2e étage",
        type = "T2",
        nominalRent = 750.0,
        serviceCharges = 65.0,
        initialElectricityIndex = 3420.5,
        status = UnitStatusDto.AVAILABLE,
        photoUrl = null,
        equipments = emptyList(),
        currentTenantName = null
    )

    val euroUnits: List<ResidenceUnitDto> = listOf(
        unitA1,
        unitA2,
        unitA4,
        unitB2Vacant
    )

    // 3. Electricity Statements (consistent math: (newIndex - previousIndex) * kWhPriceApplied)
    val statementOctA2Paid = ElectricityStatementDto(
        id = "stmt-001",
        unitId = "unit-a2",
        previousIndex = 2780.0,
        newIndex = 2910.0, // 130 kWh
        kWhPriceApplied = 0.25,
        amountDue = 32.50,
        statementDate = "2026-10-31",
        status = ElectricityStatusDto.PAID,
        createdAt = "2026-10-31T18:10:00Z",
        updatedAt = "2026-11-03T09:45:00Z"
    )

    val statementNovA2Unpaid = ElectricityStatementDto(
        id = "stmt-002",
        unitId = "unit-a2",
        previousIndex = 2910.0,
        newIndex = 3064.0, // 154 kWh
        kWhPriceApplied = 0.25,
        amountDue = 38.50,
        statementDate = "2026-11-20",
        status = ElectricityStatusDto.UNPAID,
        createdAt = "2026-11-20T17:30:00Z",
        updatedAt = "2026-11-20T17:30:00Z"
    )

    val statementOctA4Paid = ElectricityStatementDto(
        id = "stmt-003",
        unitId = "unit-a4",
        previousIndex = 4100.0,
        newIndex = 4320.0, // 220 kWh
        kWhPriceApplied = 0.25,
        amountDue = 55.00,
        statementDate = "2026-10-31",
        status = ElectricityStatusDto.PAID,
        createdAt = "2026-10-31T18:15:00Z",
        updatedAt = "2026-11-02T14:20:00Z"
    )

    val statementNovA4Unpaid = ElectricityStatementDto(
        id = "stmt-004",
        unitId = "unit-a4",
        previousIndex = 4320.0,
        newIndex = 4582.0, // 262 kWh
        kWhPriceApplied = 0.25,
        amountDue = 65.50,
        statementDate = "2026-11-20",
        status = ElectricityStatusDto.UNPAID,
        createdAt = "2026-11-20T17:35:00Z",
        updatedAt = "2026-11-20T17:35:00Z"
    )

    val statementNovA1Unpaid = ElectricityStatementDto(
        id = "stmt-005",
        unitId = "unit-a1",
        previousIndex = 1849.0,
        newIndex = 1937.0, // 88 kWh
        kWhPriceApplied = 0.25,
        amountDue = 22.00,
        statementDate = "2026-11-21",
        status = ElectricityStatusDto.UNPAID,
        createdAt = "2026-11-21T11:00:00Z",
        updatedAt = "2026-11-21T11:00:00Z"
    )

    val allEuroStatements: List<ElectricityStatementDto> = listOf(
        statementNovA1Unpaid,
        statementNovA4Unpaid,
        statementNovA2Unpaid,
        statementOctA4Paid,
        statementOctA2Paid
    )

    // FCFA / XOF Statement sample (150 FCFA / kWh)
    val statementPalmiersUnpaid = ElectricityStatementDto(
        id = "stmt-xof-001",
        unitId = "unit-p101",
        previousIndex = 1205.0,
        newIndex = 1390.0, // 185 kWh * 150 = 27 750 FCFA
        kWhPriceApplied = 150.0,
        amountDue = 27750.0,
        statementDate = "2026-11-20",
        status = ElectricityStatusDto.UNPAID,
        createdAt = "2026-11-20T16:00:00Z",
        updatedAt = "2026-11-20T16:00:00Z"
    )

    // 4. Ready-to-use ElectricityUiState Variants for @Preview Composables

    /** Standard populated state (EUR, mixed PAID/UNPAID, 2 items selected) */
    val defaultLoadedState = ElectricityUiState(
        isLoading = false,
        activeResidence = residenceEuroContext,
        isAuthorized = true,
        currencySymbol = AppCurrency.EUR.symbol,
        residenceUnits = euroUnits,
        statements = allEuroStatements,
        statusFilter = "ALL",
        selectedStatementIds = mapOf(
            "stmt-002" to true,
            "stmt-004" to true
        )
    )

    /** Filtered state showing only UNPAID statements + active Create Statement dialog */
    val createDialogOpenState = defaultLoadedState.copy(
        statusFilter = "UNPAID",
        statements = allEuroStatements.filter { it.status == ElectricityStatusDto.UNPAID },
        selectedStatementIds = emptyMap(),
        showCreateDialog = true,
        formPreviousIndex = 3064.0,
        isLoadingPreviousIndex = false
    )

    /** State with Mark as Paid confirmation dialog open */
    val confirmPaymentDialogState = defaultLoadedState.copy(
        confirmingPaymentStatement = statementNovA4Unpaid
    )

    /** FCFA (XOF) currency formatting preview state */
    val xofCurrencyState = ElectricityUiState(
        isLoading = false,
        activeResidence = residencePalmiersContext,
        isAuthorized = true,
        currencySymbol = AppCurrency.XOF.symbol,
        residenceUnits = listOf(
            unitA2.copy(id = "unit-p101", residenceId = "res-palmiers", name = "P101", nominalRent = 150000.0)
        ),
        statements = listOf(statementPalmiersUnpaid)
    )

    /** Loading skeleton state */
    val loadingState = ElectricityUiState(
        isLoading = true,
        activeResidence = residenceEuroContext,
        isAuthorized = true,
        currencySymbol = AppCurrency.EUR.symbol
    )

    /** Empty state (authorized, units exist, zero statements yet) */
    val emptyStatementsState = ElectricityUiState(
        isLoading = false,
        activeResidence = residenceEuroContext,
        isAuthorized = true,
        currencySymbol = AppCurrency.EUR.symbol,
        residenceUnits = euroUnits,
        statements = emptyList()
    )

    /** Unauthorized / Error banner state */
    val unauthorizedOrErrorState = ElectricityUiState(
        isLoading = false,
        activeResidence = residenceEuroContext,
        isAuthorized = false,
        errorMessage = "Vous n'avez pas les permissions requises pour gérer les relevés électriques de cette résidence."
    )
}

object PreviewLeasesData {

    // 1. Guarantors
    val guarantorValairKabi = GuarantorDto(
        id = "guar-001",
        tenantId = "tenant-herve",
        lastName = "Kabi",
        firstName = "Valair",
        phone = "+33 6 98 76 54 32",
        monthlySalary = 3200.0,
        relationship = "Oncle",
        jobTitle = "Directeur Financier",
        employer = "Groupe EuroTech"
    )

    val guarantorMarieTeko = GuarantorDto(
        id = "guar-002",
        tenantId = "tenant-armand",
        lastName = "Teko",
        firstName = "Marie-Claire",
        phone = "+237 6 77 45 12 89",
        monthlySalary = 2850.0,
        relationship = "Mère",
        jobTitle = "Médecin",
        employer = "Clinique Centrale"
    )

    // 2. Payments
    val a1DepositPayment = LeasePaymentDto(
        id = "pay-a1-01",
        category = LeaseCategory.DEPOSIT,
        amount = 500.0,
        description = "Dépôt de garantie (Caution 1 mois HC)",
        transactionDate = "2026-11-22"
    )

    val a1AdvanceRentPayment = LeasePaymentDto(
        id = "pay-a1-02",
        category = LeaseCategory.RENT,
        amount = 550.0,
        description = "Avance de loyer — Décembre 2026 (CC)",
        transactionDate = "2026-11-22"
    )

    val a2DepositPayment = LeasePaymentDto(
        id = "pay-a2-01",
        category = LeaseCategory.DEPOSIT,
        amount = 500.0,
        description = "Dépôt de garantie encaissé",
        transactionDate = "2026-10-28"
    )

    val a2NovRentPayment = LeasePaymentDto(
        id = "pay-a2-02",
        category = LeaseCategory.RENT,
        amount = 550.0,
        description = "Loyer Novembre 2026",
        transactionDate = "2026-11-01"
    )

    val a4DepositPayment = LeasePaymentDto(
        id = "pay-a4-01",
        category = LeaseCategory.DEPOSIT,
        amount = 1000.0,
        description = "Caution 2 mois",
        transactionDate = "2026-10-25"
    )

    val a4AnnualRentPayment = LeasePaymentDto(
        id = "pay-a4-02",
        category = LeaseCategory.RENT,
        amount = 8400.0,
        description = "Règlement annuel 2026-2027",
        transactionDate = "2026-11-01"
    )

    val d3PartialDepositPayment = LeasePaymentDto(
        id = "pay-d3-01",
        category = LeaseCategory.DEPOSIT,
        amount = 450.0,
        description = "Acompte partiel sur caution (450 € / 950 €)",
        transactionDate = "2026-11-18"
    )

    // 3. Leases covering all LeaseStatusDto states

    /** 1. DRAFT — Early incomplete draft (only unit & tenant selected, nullable financials/dates) */
    val leaseDraftEarly = LeaseDto(
        id = "lease-draft-b2",
        residenceUnitId = "unit-b2",
        tenantId = "tenant-new-01",
        startDate = null,
        endDate = null,
        depositAmount = null,
        monthlyRentAtSign = null,
        status = LeaseStatusDto.DRAFT,
        createdAt = "2026-11-23T14:15:00Z",
        updatedAt = "2026-11-23T14:20:00Z",
        paymentFrequencyDto = PaymentFrequencyDto.MONTHLY,
        advanceMonths = 1,
        payments = emptyList(),
        guarantor = null
    )

    /** 2. PENDING_PAYMENT — Financials & timeline filled, waiting for deposit + advance payment */
    val leasePendingPayment = LeaseDto(
        id = "lease-pending-pay-c1",
        residenceUnitId = "unit-c1",
        tenantId = "tenant-sophie",
        startDate = "2026-12-15",
        endDate = "2027-12-14",
        depositAmount = 520.0,
        monthlyRentAtSign = 565.0,
        status = LeaseStatusDto.PENDING_PAYMENT,
        createdAt = "2026-11-20T10:00:00Z",
        updatedAt = "2026-11-21T09:30:00Z",
        paymentFrequencyDto = PaymentFrequencyDto.MONTHLY,
        advanceMonths = 2,
        payments = emptyList(),
        guarantor = null
    )

    /** 3. PARTIALLY_PAID — Part of the deposit paid, still locked before signature */
    val leasePartiallyPaid = LeaseDto(
        id = "lease-partial-d3",
        residenceUnitId = "unit-d3",
        tenantId = "tenant-marc",
        startDate = "2026-12-01",
        endDate = "2027-11-30",
        depositAmount = 950.0,
        monthlyRentAtSign = 1040.0,
        status = LeaseStatusDto.PARTIALLY_PAID,
        createdAt = "2026-11-18T11:00:00Z",
        updatedAt = "2026-11-18T16:45:00Z",
        paymentFrequencyDto = PaymentFrequencyDto.MONTHLY,
        advanceMonths = 1,
        payments = listOf(d3PartialDepositPayment),
        guarantor = null
    )

    /** 4. DOWN_PAYMENT_PAID — Initial deposit covered, advance rent remaining */
    val leaseDownPaymentPaid = LeaseDto(
        id = "lease-downpaid-p101",
        residenceUnitId = "unit-p101",
        tenantId = "tenant-paul",
        startDate = "2026-12-01",
        endDate = "2027-11-30",
        depositAmount = 620.0,
        monthlyRentAtSign = 675.0,
        status = LeaseStatusDto.DOWN_PAYMENT_PAID,
        createdAt = "2026-11-19T08:30:00Z",
        updatedAt = "2026-11-22T15:10:00Z",
        paymentFrequencyDto = PaymentFrequencyDto.MONTHLY,
        advanceMonths = 1,
        payments = listOf(
            LeasePaymentDto(
                id = "pay-p101-01",
                category = LeaseCategory.DEPOSIT,
                amount = 620.0,
                description = "Dépôt de garantie réglé",
                transactionDate = "2026-11-22"
            )
        ),
        guarantor = null
    )

    /** 5. PENDING_SIGNATURE — 100% ready (Unit A1, Hervé Nsigne, Valair Kabi, Deposit + Advance paid) */
    val leasePendingSignatureA1 = LeaseDto(
        id = "lease-a1-2026",
        residenceUnitId = "unit-a1",
        tenantId = "tenant-herve",
        startDate = "2026-12-01",
        endDate = "2027-11-30",
        depositAmount = 500.0,
        monthlyRentAtSign = 550.0,
        status = LeaseStatusDto.PENDING_SIGNATURE,
        createdAt = "2026-11-21T09:00:00Z",
        updatedAt = "2026-11-23T10:15:00Z",
        paymentFrequencyDto = PaymentFrequencyDto.MONTHLY,
        advanceMonths = 1,
        payments = listOf(a1DepositPayment, a1AdvanceRentPayment),
        guarantor = guarantorValairKabi
    )

    /** 6. SIGNED_ACTIVE (Monthly) — Unit A2 (Gladys Naoussi), locked for deletion & unit change */
    val leaseSignedActiveA2 = LeaseDto(
        id = "lease-a2-2026",
        residenceUnitId = "unit-a2",
        tenantId = "tenant-gladys",
        startDate = "2026-11-01",
        endDate = "2027-11-01",
        depositAmount = 500.0,
        monthlyRentAtSign = 500.0,
        status = LeaseStatusDto.SIGNED_ACTIVE,
        createdAt = "2026-10-25T14:00:00Z",
        updatedAt = "2026-11-01T08:00:00Z",
        paymentFrequencyDto = PaymentFrequencyDto.MONTHLY,
        advanceMonths = 1,
        payments = listOf(a2DepositPayment, a2NovRentPayment),
        guarantor = null
    )

    /** 7. SIGNED_ACTIVE (Annual) — Unit A4 (Armand Teko), tests ANNUAL payment frequency */
    val leaseSignedActiveAnnualA4 = LeaseDto(
        id = "lease-a4-2026",
        residenceUnitId = "unit-a4",
        tenantId = "tenant-armand",
        startDate = "2026-11-01",
        endDate = "2027-11-01",
        depositAmount = 1000.0,
        monthlyRentAtSign = 700.0,
        status = LeaseStatusDto.SIGNED_ACTIVE,
        createdAt = "2026-10-20T12:00:00Z",
        updatedAt = "2026-11-01T09:00:00Z",
        paymentFrequencyDto = PaymentFrequencyDto.ANNUAL,
        advanceMonths = 12,
        payments = listOf(a4DepositPayment, a4AnnualRentPayment),
        guarantor = guarantorMarieTeko
    )

    /** 8. TERMINATED — Historical closed lease for archive/read-only UI testing */
    val leaseTerminatedOldA1 = LeaseDto(
        id = "lease-a1-2025",
        residenceUnitId = "unit-a1",
        tenantId = "tenant-former-01",
        startDate = "2025-11-01",
        endDate = "2026-10-31",
        depositAmount = 480.0,
        monthlyRentAtSign = 480.0,
        status = LeaseStatusDto.TERMINATED,
        createdAt = "2025-10-15T10:00:00Z",
        updatedAt = "2026-10-31T18:00:00Z",
        paymentFrequencyDto = PaymentFrequencyDto.MONTHLY,
        advanceMonths = 1,
        payments = listOf(
            LeasePaymentDto(
                id = "pay-old-01",
                category = LeaseCategory.DEPOSIT,
                amount = 480.0,
                description = "Caution initiale 2025",
                transactionDate = "2025-10-20"
            )
        ),
        guarantor = null
    )

    val allLeases: List<LeaseDto> = listOf(
        leasePendingSignatureA1,
        leaseDraftEarly,
        leasePendingPayment,
        leasePartiallyPaid,
        leaseDownPaymentPaid,
        leaseSignedActiveA2,
        leaseSignedActiveAnnualA4,
        leaseTerminatedOldA1
    )

    /** Unsigned leases (can be edited in full & deleted in LeaseDetailHeader) */
    val unsignedLeases: List<LeaseDto> = allLeases.filter {
        it.status != LeaseStatusDto.SIGNED_ACTIVE && it.status != LeaseStatusDto.TERMINATED
    }

    /** Active signed leases (cannot be deleted, unit/financials/timeline locked) */
    val activeLeases: List<LeaseDto> = allLeases.filter {
        it.status == LeaseStatusDto.SIGNED_ACTIVE
    }
}

object PreviewResidenceMembersData {

    // 1. Management & Operations Team (OWNER, ADMIN, MANAGER, STAFF)
    val ownerAdmin = ResidenceMemberSummaryDto(
        userId = "user-owner-01",
        firstName = "Admin",
        lastName = "ResidManager",
        email = "owner@residmanager.com",
        phone = "+33 6 11 22 33 44",
        roleDto = RoleDto.OWNER,
        status = InvitationStatusDto.ACCEPTED
    )

    val coAdmin = ResidenceMemberSummaryDto(
        userId = "user-admin-01",
        firstName = "Claire",
        lastName = "Moreau",
        email = "claire.moreau@residmanager.com",
        phone = "+33 6 44 55 66 77",
        roleDto = RoleDto.ADMIN,
        status = InvitationStatusDto.ACCEPTED
    )

    val propertyManager = ResidenceMemberSummaryDto(
        userId = "user-manager-01",
        firstName = "Samuel",
        lastName = "Mbarga",
        email = "s.mbarga@residmanager.com",
        phone = "+237 6 91 02 03 04",
        roleDto = RoleDto.MANAGER,
        status = InvitationStatusDto.ACCEPTED
    )

    val maintenanceStaffAccepted = ResidenceMemberSummaryDto(
        userId = "user-staff-01",
        firstName = "Lucas",
        lastName = "Bernard",
        email = "lucas.maintenance@residmanager.com",
        phone = null, // Tests nullable phone fallback in UI
        roleDto = RoleDto.STAFF,
        status = InvitationStatusDto.ACCEPTED
    )

    val caretakerStaffInvited = ResidenceMemberSummaryDto(
        userId = "user-staff-02",
        firstName = "Nadine",
        lastName = "Essomba",
        email = "nadine.essomba@email.com",
        phone = "+237 6 70 88 99 00",
        roleDto = RoleDto.STAFF,
        status = InvitationStatusDto.INVITED
    )

    // 2. Tenants (Matching IDs & names from PreviewLeasesData & PreviewResidenceUnits)
    val tenantHerve = ResidenceMemberSummaryDto(
        userId = "tenant-herve",
        firstName = "Hervé",
        lastName = "Nsigne",
        email = "herve.nsigne@email.com",
        phone = "+237 6 99 12 34 56",
        roleDto = RoleDto.TENANT,
        status = InvitationStatusDto.ACCEPTED
    )

    val tenantGladys = ResidenceMemberSummaryDto(
        userId = "tenant-gladys",
        firstName = "Gladys",
        lastName = "Naoussi",
        email = "gladys.naoussi@email.com",
        phone = "+33 6 78 12 34 90",
        roleDto = RoleDto.TENANT,
        status = InvitationStatusDto.ACCEPTED
    )

    val tenantArmand = ResidenceMemberSummaryDto(
        userId = "tenant-armand",
        firstName = "Armand",
        lastName = "Teko",
        email = "armand.teko@email.com",
        phone = "+237 6 55 44 33 22",
        roleDto = RoleDto.TENANT,
        status = InvitationStatusDto.ACCEPTED
    )

    val tenantSophieInvited = ResidenceMemberSummaryDto(
        userId = "tenant-sophie",
        firstName = "Sophie",
        lastName = "Laurent",
        email = "sophie.laurent@email.com",
        phone = "+33 6 23 45 67 89",
        roleDto = RoleDto.TENANT,
        status = InvitationStatusDto.INVITED
    )

    val tenantMarcPendingApproval = ResidenceMemberSummaryDto(
        userId = "tenant-marc",
        firstName = "Marc",
        lastName = "Kouamé",
        email = "marc.kouame@email.com",
        phone = null, // Tests nullable phone with pending badge
        roleDto = RoleDto.TENANT,
        status = InvitationStatusDto.PENDING_APPROVAL
    )

    val tenantPaulDeclined = ResidenceMemberSummaryDto(
        userId = "tenant-paul",
        firstName = "Paul",
        lastName = "Tchoumi",
        email = "paul.tchoumi@email.com",
        phone = "+33 7 81 92 03 14",
        roleDto = RoleDto.TENANT,
        status = InvitationStatusDto.DECLINED
    )

    // 3. Ready-to-use Filtered Lists for @Preview Composables
    val allMembers: List<ResidenceMemberSummaryDto> = listOf(
        ownerAdmin,
        coAdmin,
        propertyManager,
        maintenanceStaffAccepted,
        caretakerStaffInvited,
        tenantHerve,
        tenantGladys,
        tenantArmand,
        tenantSophieInvited,
        tenantMarcPendingApproval,
        tenantPaulDeclined
    )

    val staffAndManagementMembers: List<ResidenceMemberSummaryDto> = allMembers.filter {
        it.roleDto != RoleDto.TENANT
    }

    val tenantMembers: List<ResidenceMemberSummaryDto> = allMembers.filter {
        it.roleDto == RoleDto.TENANT
    }

    val pendingAndInvitedMembers: List<ResidenceMemberSummaryDto> = allMembers.filter {
        it.status == InvitationStatusDto.INVITED || it.status == InvitationStatusDto.PENDING_APPROVAL
    }
}

object PreviewFinanceTransactionsData {

    // =========================================================================
    // 1. INCOME TRANSACTIONS (Residence EURO — EUR)
    // =========================================================================

    val incomeDepositA1 = FinanceTransactionDto(
        id = "tx-inc-001",
        residenceId = "res-euro",
        type = TransactionTypeDto.INCOME,
        category = TransactionCategoryDto.DEPOSIT,
        amount = 500.0,
        description = "Dépôt de garantie (Caution) — Unité A1 (Hervé Nsigne)",
        relatedEntityType = EntityTypeDto.BAIL,
        relatedEntityId = "lease-a1-2026",
        transactionDate = "2026-11-22",
        createdAt = "2026-11-22T10:15:00Z"
    )

    val incomeAdvanceRentA1 = FinanceTransactionDto(
        id = "tx-inc-002",
        residenceId = "res-euro",
        type = TransactionTypeDto.INCOME,
        category = TransactionCategoryDto.RENT,
        amount = 550.0,
        description = "Avance de loyer Décembre 2026 CC — Unité A1 (Hervé Nsigne)",
        relatedEntityType = EntityTypeDto.BAIL,
        relatedEntityId = "lease-a1-2026",
        transactionDate = "2026-11-22",
        createdAt = "2026-11-22T10:16:00Z"
    )

    val incomePartialDepositD3 = FinanceTransactionDto(
        id = "tx-inc-003",
        residenceId = "res-euro",
        type = TransactionTypeDto.INCOME,
        category = TransactionCategoryDto.LEASE_PAYMENT,
        amount = 450.0,
        description = "Acompte partiel sur dossier de bail — Unité D3 (Marc Kouamé)",
        relatedEntityType = EntityTypeDto.BAIL,
        relatedEntityId = "lease-partial-d3",
        transactionDate = "2026-11-18",
        createdAt = "2026-11-18T16:45:00Z"
    )

    val incomeNovRentA2 = FinanceTransactionDto(
        id = "tx-inc-004",
        residenceId = "res-euro",
        type = TransactionTypeDto.INCOME,
        category = TransactionCategoryDto.RENT,
        amount = 550.0,
        description = "Loyer Novembre 2026 — Unité A2 (Gladys Naoussi)",
        relatedEntityType = EntityTypeDto.BAIL,
        relatedEntityId = "lease-a2-2026",
        transactionDate = "2026-11-01",
        createdAt = "2026-11-01T08:05:00Z"
    )

    val incomeAnnualRentA4 = FinanceTransactionDto(
        id = "tx-inc-005",
        residenceId = "res-euro",
        type = TransactionTypeDto.INCOME,
        category = TransactionCategoryDto.RENT,
        amount = 8400.0,
        description = "Règlement loyer annuel 2026-2027 — Unité A4 (Armand Teko)",
        relatedEntityType = EntityTypeDto.BAIL,
        relatedEntityId = "lease-a4-2026",
        transactionDate = "2026-11-01",
        createdAt = "2026-11-01T09:12:00Z"
    )

    val incomeElecOctA2 = FinanceTransactionDto(
        id = "tx-inc-006",
        residenceId = "res-euro",
        type = TransactionTypeDto.INCOME,
        category = TransactionCategoryDto.ELECTRICITY,
        amount = 32.50,
        description = "Remboursement consommation électrique Octobre (130 kWh) — Unité A2",
        relatedEntityType = EntityTypeDto.ELECTRICITY_STATEMENT,
        relatedEntityId = "stmt-001",
        transactionDate = "2026-11-03",
        createdAt = "2026-11-03T09:45:00Z"
    )

    val incomeElecOctA4 = FinanceTransactionDto(
        id = "tx-inc-007",
        residenceId = "res-euro",
        type = TransactionTypeDto.INCOME,
        category = TransactionCategoryDto.ELECTRICITY,
        amount = 55.00,
        description = "Remboursement consommation électrique Octobre (220 kWh) — Unité A4",
        relatedEntityType = EntityTypeDto.ELECTRICITY_STATEMENT,
        relatedEntityId = "stmt-003",
        transactionDate = "2026-11-02",
        createdAt = "2026-11-02T14:20:00Z"
    )

    val incomeParkingExtra = FinanceTransactionDto(
        id = "tx-inc-008",
        residenceId = "res-euro",
        type = TransactionTypeDto.INCOME,
        category = TransactionCategoryDto.OTHER,
        amount = 45.0,
        description = "Badge Vigik supplémentaire & télécommande portail",
        relatedEntityType = null, // Tests standalone transaction without linked entity
        relatedEntityId = null,
        transactionDate = "2026-11-10",
        createdAt = "2026-11-10T11:30:00Z"
    )

    // =========================================================================
    // 2. EXPENSE TRANSACTIONS (Covering all operational categories & TICKET link)
    // =========================================================================

    val expensePlumbingTicket = FinanceTransactionDto(
        id = "tx-exp-001",
        residenceId = "res-euro",
        type = TransactionTypeDto.EXPENSE,
        category = TransactionCategoryDto.MAINTENANCE,
        amount = 185.0,
        description = "Remplacement mitigeur et joint siphon cuisine — Unité B2",
        relatedEntityType = EntityTypeDto.TICKET,
        relatedEntityId = "ticket-maint-014",
        transactionDate = "2026-11-19",
        createdAt = "2026-11-19T15:40:00Z"
    )

    val expenseAcServiceTicket = FinanceTransactionDto(
        id = "tx-exp-002",
        residenceId = "res-euro",
        type = TransactionTypeDto.EXPENSE,
        category = TransactionCategoryDto.MAINTENANCE,
        amount = 90.0,
        description = "Entretien annuel et recharge gaz climatiseur Split — Unité A1",
        relatedEntityType = EntityTypeDto.TICKET,
        relatedEntityId = "ticket-maint-011",
        transactionDate = "2026-11-14",
        createdAt = "2026-11-14T17:00:00Z"
    )

    val expenseCommonAreasCleaning = FinanceTransactionDto(
        id = "tx-exp-003",
        residenceId = "res-euro",
        type = TransactionTypeDto.EXPENSE,
        category = TransactionCategoryDto.CLEANING,
        amount = 240.0,
        description = "Nettoyage mensuel des parties communes et remise à neuf Unité A1",
        relatedEntityType = null,
        relatedEntityId = null,
        transactionDate = "2026-11-15",
        createdAt = "2026-11-15T18:00:00Z"
    )

    val expenseGeneratorFuel = FinanceTransactionDto(
        id = "tx-exp-004",
        residenceId = "res-euro",
        type = TransactionTypeDto.EXPENSE,
        category = TransactionCategoryDto.FUEL,
        amount = 160.0,
        description = "Approvisionnement gasoil groupe électrogène de secours (100L)",
        relatedEntityType = null,
        relatedEntityId = null,
        transactionDate = "2026-11-12",
        createdAt = "2026-11-12T10:20:00Z"
    )

    val expenseNightSecurity = FinanceTransactionDto(
        id = "tx-exp-005",
        residenceId = "res-euro",
        type = TransactionTypeDto.EXPENSE,
        category = TransactionCategoryDto.SECURITY,
        amount = 420.0,
        description = "Prestation gardiennage & télésurveillance — Novembre 2026",
        relatedEntityType = null,
        relatedEntityId = null,
        transactionDate = "2026-11-05",
        createdAt = "2026-11-05T09:00:00Z"
    )

    val expenseGridElectricityBill = FinanceTransactionDto(
        id = "tx-exp-006",
        residenceId = "res-euro",
        type = TransactionTypeDto.EXPENSE,
        category = TransactionCategoryDto.ELECTRICITY,
        amount = 310.0,
        description = "Facture fournisseur électricité compteur général résidence — Octobre",
        relatedEntityType = null,
        relatedEntityId = null,
        transactionDate = "2026-11-04",
        createdAt = "2026-11-04T13:10:00Z"
    )

    val expensePropertyTaxes = FinanceTransactionDto(
        id = "tx-exp-007",
        residenceId = "res-euro",
        type = TransactionTypeDto.EXPENSE,
        category = TransactionCategoryDto.TAXES,
        amount = 650.0,
        description = "Taxe d'enlèvement des ordures ménagères & impôt foncier T4",
        relatedEntityType = null,
        relatedEntityId = null,
        transactionDate = "2026-11-01",
        createdAt = "2026-11-01T11:00:00Z"
    )

    // =========================================================================
    // 3. SECONDARY RESIDENCE SAMPLE (Résidence Les Palmiers — XOF / FCFA)
    // =========================================================================

    val palmiersDepositIncomeXof = FinanceTransactionDto(
        id = "tx-xof-001",
        residenceId = "res-palmiers",
        type = TransactionTypeDto.INCOME,
        category = TransactionCategoryDto.DEPOSIT,
        amount = 300000.0,
        description = "Caution 2 mois — Appartement P101 (Paul Tchoumi)",
        relatedEntityType = EntityTypeDto.BAIL,
        relatedEntityId = "lease-downpaid-p101",
        transactionDate = "2026-11-22",
        createdAt = "2026-11-22T15:10:00Z"
    )

    val palmiersFuelExpenseXof = FinanceTransactionDto(
        id = "tx-xof-002",
        residenceId = "res-palmiers",
        type = TransactionTypeDto.EXPENSE,
        category = TransactionCategoryDto.FUEL,
        amount = 65000.0,
        description = "Carburant groupe électrogène — Novembre",
        relatedEntityType = null,
        relatedEntityId = null,
        transactionDate = "2026-11-16",
        createdAt = "2026-11-16T12:00:00Z"
    )

    // =========================================================================
    // 4. READY-TO-USE LISTS & KPI HELPERS FOR @Preview COMPOSABLES
    // =========================================================================

    /** Chronologically ordered (most recent first) transactions for Residence EURO */
    val euroTransactions: List<FinanceTransactionDto> = listOf(
        incomeDepositA1,
        incomeAdvanceRentA1,
        expensePlumbingTicket,
        incomePartialDepositD3,
        expenseCommonAreasCleaning,
        expenseAcServiceTicket,
        expenseGeneratorFuel,
        incomeParkingExtra,
        expenseNightSecurity,
        expenseGridElectricityBill,
        incomeElecOctA2,
        incomeElecOctA4,
        incomeAnnualRentA4,
        incomeNovRentA2,
        expensePropertyTaxes
    )

    val euroIncomes: List<FinanceTransactionDto> = euroTransactions.filter {
        it.type == TransactionTypeDto.INCOME
    }

    val euroExpenses: List<FinanceTransactionDto> = euroTransactions.filter {
        it.type == TransactionTypeDto.EXPENSE
    }

    val palmiersTransactionsXof: List<FinanceTransactionDto> = listOf(
        palmiersDepositIncomeXof,
        palmiersFuelExpenseXof
    )

    val totalEuroIncome: Double = euroIncomes.sumOf { it.amount }     // 10,582.50 €
    val totalEuroExpense: Double = euroExpenses.sumOf { it.amount }   //  2,055.00 €
    val netEuroBalance: Double = totalEuroIncome - totalEuroExpense   //  8,527.50 €
}