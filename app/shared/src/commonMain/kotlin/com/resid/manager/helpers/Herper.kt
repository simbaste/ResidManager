package com.resid.manager.helpers

import com.resid.manager.dto.AppCurrency
import com.resid.manager.dto.ElectricityStatementDto
import com.resid.manager.dto.ElectricityStatusDto
import com.resid.manager.dto.EquipmentDto
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceUnitDto
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