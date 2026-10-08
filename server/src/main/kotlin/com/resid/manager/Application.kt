package com.resid.manager

import at.favre.lib.crypto.bcrypt.BCrypt
import com.resid.manager.auth.JwtConfig
import com.resid.manager.data.Users
import com.resid.manager.routes.applications
import com.resid.manager.routes.authRoutes
import com.resid.manager.routes.configureAppRoutes
import com.resid.manager.routes.electricitiesRoutes
import com.resid.manager.routes.financialTransactionsRoutes
import com.resid.manager.routes.inspectionsRoutes
import com.resid.manager.routes.invitationsRoutes
import com.resid.manager.routes.leasesRoutes
import com.resid.manager.routes.residencesRoutes
import com.resid.manager.routes.ticketsRoutes
import com.resid.manager.routes.unitsRoutes
import com.resid.manager.routes.userRoutes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Clock
import java.time.LocalDateTime

fun main() {
    val serverPort = System.getenv("PORT")?.toInt() ?: 8081
    embeddedServer(Netty, port = serverPort, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun ensureDatabaseExists(dbUrl: String, dbUser: String, dbPassword: String) {
    if (!dbUrl.startsWith("jdbc:postgresql://")) return
    val urlWithoutDb = dbUrl.substringBeforeLast("/")
    val dbName = dbUrl.substringAfterLast("/")
    val postgresUrl = "$urlWithoutDb/postgres"
    try {
        Class.forName("org.postgresql.Driver")
        java.sql.DriverManager.getConnection(postgresUrl, dbUser, dbPassword).use { connection ->
            connection.createStatement().use { statement ->
                val rs = statement.executeQuery("SELECT 1 FROM pg_database WHERE datname = '$dbName'")
                val exists = rs.next()
                rs.close()
                if (!exists) {
                    statement.executeUpdate("CREATE DATABASE $dbName")
                    println("Database '$dbName' successfully created!")
                }
            }
        }
    } catch (e: Exception) {
        println("Could not verify/create database '$dbName' via standard postgres db fallback: ${e.message}")
    }
}

fun runFlywayMigrations(dbUrl: String, dbUser: String, dbPassword: String) {
    if (!dbUrl.startsWith("jdbc:postgresql://")) return
    try {
        println("Running database migrations via Flyway...")
        val flyway = Flyway.configure()
            .dataSource(dbUrl, dbUser, dbPassword)
            .baselineOnMigrate(true)
            .load()
        // Automatically repair checksums if migration scripts were modified during development
        flyway.repair()
        flyway.migrate()
        println("Database migrations successfully applied!")
    } catch (e: Exception) {
        println("Flyway migration failed: ${e.message}")
        throw e
    }
}

fun Application.module() {
    // 1. Initialize Database (Postgres or fallback to local postgres if env not specified)
    val dbUrl = System.getenv("JDBC_DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/residmanager_db"
    val dbUser = System.getenv("JDBC_DATABASE_USERNAME") ?: "postgres"
    val dbPassword = System.getenv("JDBC_DATABASE_PASSWORD") ?: "postgres"

    // Automatically create database if it doesn't exist
    ensureDatabaseExists(dbUrl, dbUser, dbPassword)

    // Run safe migrations via Flyway
    runFlywayMigrations(dbUrl, dbUser, dbPassword)

    try {
        Database.connect(
            url = dbUrl,
            driver = "org.postgresql.Driver",
            user = dbUser,
            password = dbPassword
        )

        // Seed default testing data if necessary
        transaction {
            // Seed a default admin user for testing if users table is empty
            if (Users.selectAll().count() == 0L) {
                Users.insertIgnore {
                    it[email] = "admin@residmanager.com"
                    it[passwordHash] = BCrypt.withDefaults().hashToString(12, "admin123".toCharArray())
                    it[firstName] = "Admin"
                    it[lastName] = "ResidManager"
                    it[phone] = "+22501020304"
                    it[createdAt] = LocalDateTime.now(Clock.systemUTC())
                }
            }
        }
    } catch (e: Exception) {
        log.error("Failed to connect to Database. Verify your PostgreSQL settings: ${e.message}")
    }

    // 2. Install ContentNegotiation
    install(ContentNegotiation) {
        json(kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }

    // Install CORS for Multiplatform client access (Web browser)
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }

    // 3. Install Authentication with JWT
    install(Authentication) {
        jwt("auth-jwt") {
            realm = JwtConfig.realm
            verifier(JwtConfig.verifier)
            validate { credential ->
                if (credential.payload.audience.contains(JwtConfig.audience)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }

    // 4. Register Routes
    routing {
        get("/") {
            call.respondText(sayHello("Ktor"))
        }
        
        // Login route
        authRoutes()

        // Residences
        residencesRoutes()

        // Invitations
        invitationsRoutes()

        // Applications
        applications()

        // Users
        userRoutes()

        // Units
        unitsRoutes()

        // Leases
        leasesRoutes()

        // Inspection Reports (Move-in / Move-out)
        inspectionsRoutes()

        // Electricity Statements
        electricitiesRoutes()

        // Tickets & Ticket Categories
        ticketsRoutes()

        // Financial Transactions
        financialTransactionsRoutes()
        
        // Secured routes example
        authenticate("auth-jwt") {
            get("/api/secure-ping") {
                val principal = call.principal<JWTPrincipal>()
                val email = principal?.payload?.getClaim("email")?.asString()
                call.respondText("Ping success! Logged in as: $email")
            }
        }
    }

    // Register all prepared API routes
    configureAppRoutes()
}
