package com.resid.manager.validation

object AuthValidator {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")

    fun isValidEmail(email: String): Boolean {
        return EMAIL_REGEX.matches(email)
    }

    fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        return hasDigit && hasSpecial
    }

    fun validateLogin(email: String, passwordPlain: String): Result<Unit> {
        if (!isValidEmail(email)) {
            return Result.failure(IllegalArgumentException("Format d'email invalide."))
        }
        if (passwordPlain.isBlank()) {
            return Result.failure(IllegalArgumentException("Le mot de passe ne peut pas être vide."))
        }
        return Result.success(Unit)
    }

    fun validateRegister(
        firstName: String,
        lastName: String,
        email: String,
        passwordPlain: String
    ): Result<Unit> {
        if (firstName.isBlank()) {
            return Result.failure(IllegalArgumentException("Le prénom est obligatoire."))
        }
        if (lastName.isBlank()) {
            return Result.failure(IllegalArgumentException("Le nom est obligatoire."))
        }
        if (!isValidEmail(email)) {
            return Result.failure(IllegalArgumentException("Format d'email invalide (ex: utilisateur@domaine.com)."))
        }
        if (!isValidPassword(passwordPlain)) {
            return Result.failure(IllegalArgumentException("Le mot de passe doit faire au moins 8 caractères et contenir au moins 1 chiffre et 1 caractère spécial."))
        }
        return Result.success(Unit)
    }
}
