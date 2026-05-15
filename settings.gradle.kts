plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "lab5.2"

include("server")
include("client")
include("shared")
include("gateway_server")