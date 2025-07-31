plugins {
    id("java")
}

group = "cloud.reivax"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // We could use this dependency to really sped up things, not needing to implement the algorithm ourselves. For demonstration purposes, we won't
    implementation("org.ahocorasick:ahocorasick:0.4.0")}

tasks.test {
    useJUnitPlatform()
}