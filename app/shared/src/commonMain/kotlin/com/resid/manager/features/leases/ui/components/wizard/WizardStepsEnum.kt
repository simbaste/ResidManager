package com.resid.manager.features.leases.ui.components.wizard

enum class WizardStep(val stepNumber: Int, val isOptional: Boolean) {
    UNIT(1, isOptional = false),
    TENANT(2, isOptional = false),
    GUARANTOR(3, isOptional = true),
    FINANCIALS(4, isOptional = false),
    TIMELINE(5, isOptional = false),
    INVENTORY(6, isOptional = true),
    SIGNATURE(7, isOptional = false)
}
