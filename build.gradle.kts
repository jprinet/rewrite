plugins {
    id("org.openrewrite.build.root") version("latest.release")
    id("org.openrewrite.build.java-base") version("latest.release")
    id("org.openrewrite.rewrite") version("latest.release")
}

repositories {
    mavenCentral()
}

dependencies {
    rewrite(project(":rewrite-core"))
}

rewrite {
    failOnDryRunResults = true
    activeRecipe("org.openrewrite.self.Rewrite")
}


allprojects {
    group = "org.openrewrite"
    description = "Eliminate tech-debt. Automatically."
}
