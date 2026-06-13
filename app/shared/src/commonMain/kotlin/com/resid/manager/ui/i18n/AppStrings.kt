package com.resid.manager.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf

interface AppStrings {
    val appName: String
    val dashboard: String
    val residences: String
    val logements: String
    val leases: String
    val members: String
    val electricity: String
    val tickets: String
    val finances: String
    val profile: String
    
    val loginTitle: String
    val registerTitle: String
    val emailLabel: String
    val passwordLabel: String
    val loginButton: String
    val registerButton: String
    val logoutButton: String
    val noResidenceActive: String
    val welcomeMessage: String
    val selectOrJoinResidence: String
    val createResidence: String
    val joinResidence: String
    
    val deleteResidenceTitle: String
    val deleteResidenceWarning: String
    val editResidenceTitle: String
    val nameLabel: String
    val addressLabel: String
    val kWhPriceLabel: String
    val cancel: String
    val confirm: String
    val save: String
}

object FrStrings : AppStrings {
    override val appName = "Resid Manager"
    override val dashboard = "Tableau de Bord"
    override val residences = "Résidences"
    override val logements = "Logements"
    override val leases = "Contrats"
    override val members = "Membres"
    override val electricity = "Facturation"
    override val tickets = "Tickets"
    override val finances = "Finance"
    override val profile = "Compte"
    
    override val loginTitle = "Connexion à Resid Manager"
    override val registerTitle = "Créer un compte"
    override val emailLabel = "Adresse E-mail"
    override val passwordLabel = "Mot de passe"
    override val loginButton = "Se connecter"
    override val registerButton = "S'enregistrer"
    override val logoutButton = "Déconnexion"
    override val noResidenceActive = "Aucune résidence active"
    override val welcomeMessage = "Bienvenue dans Resid Manager"
    override val selectOrJoinResidence = "Pour commencer, vous devez soit créer votre propre résidence, soit en rechercher une existante pour envoyer une demande d'adhésion."
    override val createResidence = "Créer une résidence"
    override val joinResidence = "Rejoindre une résidence"
    
    override val deleteResidenceTitle = "Supprimer la résidence"
    override val deleteResidenceWarning = "Êtes-vous sûr de vouloir supprimer définitivement cette résidence ainsi que toutes ses données ? Cette action est irréversible."
    override val editResidenceTitle = "Modifier la résidence"
    override val nameLabel = "Nom *"
    override val addressLabel = "Adresse *"
    override val kWhPriceLabel = "Prix kWh Électricité (XOF) *"
    override val cancel = "Annuler"
    override val confirm = "Confirmer"
    override val save = "Sauvegarder"
}

object EnStrings : AppStrings {
    override val appName = "Resid Manager"
    override val dashboard = "Dashboard"
    override val residences = "Properties & Residences"
    override val logements = "Housing Units"
    override val leases = "Lease Agreements"
    override val members = "Members & Roles"
    override val electricity = "Electricity Billing"
    override val tickets = "Maintenance Tickets"
    override val finances = "Cashflow & Finances"
    override val profile = "My Account"
    
    override val loginTitle = "Login to Resid Manager"
    override val registerTitle = "Create an Account"
    override val emailLabel = "Email Address"
    override val passwordLabel = "Password"
    override val loginButton = "Log In"
    override val registerButton = "Register"
    override val logoutButton = "Log Out"
    override val noResidenceActive = "No Active Residence"
    override val welcomeMessage = "Welcome to Resid Manager"
    override val selectOrJoinResidence = "To begin, you must either create your own residence or search for an existing one to submit a membership request."
    override val createResidence = "Create a Residence"
    override val joinResidence = "Join a Residence"
    
    override val deleteResidenceTitle = "Delete Residence"
    override val deleteResidenceWarning = "Are you sure you want to permanently delete this residence and all its data? This action is irreversible."
    override val editResidenceTitle = "Edit Residence"
    override val nameLabel = "Name *"
    override val addressLabel = "Address *"
    override val kWhPriceLabel = "Electricity kWh Price (XOF) *"
    override val cancel = "Cancel"
    override val confirm = "Confirm"
    override val save = "Save"
}

val LocalStrings = staticCompositionLocalOf<AppStrings> { FrStrings }
