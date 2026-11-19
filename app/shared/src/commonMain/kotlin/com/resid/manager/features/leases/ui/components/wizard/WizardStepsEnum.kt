package com.resid.manager.features.leases.ui.components.wizard

enum class WizardStep(val stepNumber: Int, val isOptional: Boolean) {
    TENANT(1, isOptional = false),
    GUARANTOR(2, isOptional = true),
    UNIT(3, isOptional = false),
    FINANCIALS(4, isOptional = false),
    TIMELINE(5, isOptional = false),
    INVENTORY(6, isOptional = true),
    SIGNATURE(7, isOptional = false)
}
