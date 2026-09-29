plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinxSerialization)
}

group = "com.resid.manager"
version = "1.0.0"
application {
    mainClass = "com.resid.manager.ApplicationKt"
}

dependencies {
    api(projects.core)
    implementation(projects.app.shared)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.postgresql)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverAuth)
    implementation(libs.ktor.serverAuthJwt)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serverCors)
    implementation(libs.ktor.serializationKotlinxJson)
    implementation(libs.bcrypt)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.openpdf)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}

// Dans /server/build.gradle.kts

tasks.withType<Copy>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.withType<Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

// C'est cette tâche spécifique requise par le Dockerfile qui refusait les doublons :
tasks.named<Sync>("installDist") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

// Tâche pour lancer le serveur pointant sur la base de données locale DEVELOP (Docker port 5433)
tasks.register<JavaExec>("runDevelop") {
    group = "application"
    description = "Runs the server connected to the local develop database (Docker port 5433)"
    mainClass.set("com.resid.manager.ApplicationKt")
    classpath = sourceSets["main"].runtimeClasspath
    environment("JDBC_DATABASE_URL", "jdbc:postgresql://localhost:5433/residmanager_db")
}

// Tâche pour lancer le serveur pointant sur la base de données locale reproduisant MAIN (Docker port 5434)
tasks.register<JavaExec>("runMain") {
    group = "application"
    description = "Runs the server connected to the local main replica database (Docker port 5434)"
    mainClass.set("com.resid.manager.ApplicationKt")
    classpath = sourceSets["main"].runtimeClasspath
    environment("JDBC_DATABASE_URL", "jdbc:postgresql://localhost:5434/residmanager_db")
}