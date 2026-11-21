package com.resid.manager.helpers

import com.resid.manager.dto.EquipmentDto
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.dto.UnitStatusDto

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