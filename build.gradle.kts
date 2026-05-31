import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import javax.inject.Inject

// Workaround for Kotlin 2.3.21 bug:
// kotlin-build-tools-impl-2.3.21.pom omits kotlin-build-statistics as a runtime dependency,
// but BuildMetricsReporterAdapterKt (inside kotlin-build-tools-impl) references GradleBuildTimeMetric
// from that artifact at class-init time.  Patching the component metadata adds the missing JAR to
// the transitive classpath that Kotlin's internal detached configuration resolves, which becomes the
// URL list for the ClasspathEntrySnapshotTransform worker classloader.
abstract class AddKotlinBuildStatistics @Inject constructor() : ComponentMetadataRule {
    override fun execute(context: ComponentMetadataContext) {
        context.details.allVariants {
            withDependencies {
                add("org.jetbrains.kotlin:kotlin-build-statistics:2.3.21")
            }
        }
    }
}

plugins {
    kotlin("jvm") version "2.3.21" apply false
    kotlin("plugin.spring") version "2.3.21" apply false
    kotlin("plugin.jpa") version "2.3.21" apply false
    kotlin("kapt") version "2.3.21" apply false
    id("org.springframework.boot") version "4.0.2" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

subprojects {
    group = rootProject.group
    version = rootProject.version

    // Tell Spring Boot's dependency management BOM to use Kotlin 2.3.21 instead of its
    // default (2.2.21), so that kotlin-compiler-embeddable and friends are NOT downgraded.
    // This must be set before the subproject's dependencyManagement { imports { mavenBom } }
    // block runs.  The io.spring.dependency-management plugin reads this lazily at resolution
    // time, so configuring it here (root subprojects block, before subproject script body) is
    // early enough.
    extra["kotlin.version"] = "2.3.21"

    repositories {
        mavenCentral()
    }

    dependencies {
        components {
            withModule<AddKotlinBuildStatistics>("org.jetbrains.kotlin:kotlin-build-tools-impl")
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            jvmToolchain(25)
        }
    }
}
