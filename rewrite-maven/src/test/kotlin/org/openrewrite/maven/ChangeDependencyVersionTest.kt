/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.openrewrite.Issue
import java.nio.file.Path

class ChangeDependencyVersionTest : MavenRecipeTest {
    @ParameterizedTest
    @ValueSource(strings = ["com.google.guava:guava", "*:*"])
    fun upgradeVersion(ga: String) = assertChanged(
        recipe = UpgradeDependencyVersion(
            ga.substringBefore(':'),
            ga.substringAfter(':'),
            "latest.patch",
            null,
            null
        ),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <dependencies>
                <dependency>
                  <groupId>com.google.guava</groupId>
                  <artifactId>guava</artifactId>
                  <version>13.0</version>
                </dependency>
              </dependencies>
            </project>
        """,
        after = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <dependencies>
                <dependency>
                  <groupId>com.google.guava</groupId>
                  <artifactId>guava</artifactId>
                  <version>13.0.1</version>
                </dependency>
              </dependencies>
            </project>
        """
    )

    @Test
    fun trustParent() = assertUnchanged(
        recipe = UpgradeDependencyVersion(
            "junit",
            "junit",
            "4.x",
            null,
            true
        ),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
                <groupId>com.fasterxml.jackson</groupId>
                <artifactId>jackson-parent</artifactId>
                <version>2.12</version>
              </parent>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <dependencies>
                <dependency>
                  <groupId>junit</groupId>
                  <artifactId>junit</artifactId>
                </dependency>
              </dependencies>
            </project>
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/739")
    fun upgradeVersionWithGroupIdAndArtifactIdDefinedAsProperty() = assertChanged(
        recipe = UpgradeDependencyVersion(
            "io.quarkus",
            "quarkus-universe-bom",
            "1.13.7.Final",
            null,
            null
        ),
        before = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                    <quarkus.platform.artifact-id>quarkus-universe-bom</quarkus.platform.artifact-id>
                    <quarkus.platform.group-id>io.quarkus</quarkus.platform.group-id>
                    <quarkus.platform.version>1.11.7.Final</quarkus.platform.version>
                    <jboss.groupId>org.jboss.resteasy</jboss.groupId>
                    <jboss.artifactId>resteasy-jaxrs</jboss.artifactId>
                </properties>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>io.quarkus</groupId>
                            <artifactId>quarkus-universe-bom</artifactId>
                            <version>${"$"}{quarkus.platform.version}</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                <dependencies>
                    <dependency>
                        <groupId>io.quarkus</groupId>
                        <artifactId>quarkus-arc</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>${"$"}{jboss.groupId}</groupId>
                        <artifactId>${"$"}{jboss.artifactId}</artifactId>
                        <version>3.0.24.Final</version>
                    </dependency>
                    <dependency>
                        <groupId>org.mindrot</groupId>
                        <artifactId>jbcrypt</artifactId>
                        <version>0.4</version>
                    </dependency>
                </dependencies>
            </project>
        """,
        after = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                    <quarkus.platform.artifact-id>quarkus-universe-bom</quarkus.platform.artifact-id>
                    <quarkus.platform.group-id>io.quarkus</quarkus.platform.group-id>
                    <quarkus.platform.version>1.13.7.Final</quarkus.platform.version>
                    <jboss.groupId>org.jboss.resteasy</jboss.groupId>
                    <jboss.artifactId>resteasy-jaxrs</jboss.artifactId>
                </properties>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>io.quarkus</groupId>
                            <artifactId>quarkus-universe-bom</artifactId>
                            <version>${"$"}{quarkus.platform.version}</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                <dependencies>
                    <dependency>
                        <groupId>io.quarkus</groupId>
                        <artifactId>quarkus-arc</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>${"$"}{jboss.groupId}</groupId>
                        <artifactId>${"$"}{jboss.artifactId}</artifactId>
                        <version>3.0.24.Final</version>
                    </dependency>
                    <dependency>
                        <groupId>org.mindrot</groupId>
                        <artifactId>jbcrypt</artifactId>
                        <version>0.4</version>
                    </dependency>
                </dependencies>
            </project>
        """
    )

    @Test
    fun upgradeVersionSuccessively() = assertChanged(
        recipe = UpgradeDependencyVersion(
            "com.google.guava",
            null,
            "28.x",
            "-jre",
            null
        ).doNext(
            UpgradeDependencyVersion(
                "com.google.guava",
                null,
                "29.x",
                "-jre",
                null
            )
        ),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <dependencies>
                <dependency>
                  <groupId>com.google.guava</groupId>
                  <artifactId>guava</artifactId>
                  <version>27.0-jre</version>
                </dependency>
              </dependencies>
            </project>
        """,
        after = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <dependencies>
                <dependency>
                  <groupId>com.google.guava</groupId>
                  <artifactId>guava</artifactId>
                  <version>29.0-jre</version>
                </dependency>
              </dependencies>
            </project>
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/565")
    fun handlesPropertiesInDependencyGroupIdAndArtifactId() = assertChanged(
        recipe = UpgradeDependencyVersion(
            "com.google.guava",
            null,
            "latest.patch",
            null,
            null
        ),
        before = """
            <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <properties>
                    <dependency.group-id>com.google.guava</dependency.group-id>
                    <dependency.artifact-id>guava</dependency.artifact-id>
                    <dependency.version>13.0</dependency.version>
                </properties>

                <dependencies>
                    <dependency>
                        <groupId>${'$'}{dependency.group-id}</groupId>
                        <artifactId>${'$'}{dependency.artifact-id}</artifactId>
                        <version>${'$'}{dependency.version}</version>
                    </dependency>
                </dependencies>
            </project>
        """,
        after = """
            <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <properties>
                    <dependency.group-id>com.google.guava</dependency.group-id>
                    <dependency.artifact-id>guava</dependency.artifact-id>
                    <dependency.version>13.0.1</dependency.version>
                </properties>

                <dependencies>
                    <dependency>
                        <groupId>${'$'}{dependency.group-id}</groupId>
                        <artifactId>${'$'}{dependency.artifact-id}</artifactId>
                        <version>${'$'}{dependency.version}</version>
                    </dependency>
                </dependencies>
            </project>
        """
    )

    @Test
    fun upgradeGuava() = assertChanged(
        recipe = UpgradeDependencyVersion(
            "com.google.guava",
            null as String?,
            "25-28",
            "-jre",
            null
        ),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <dependencies>
                <dependency>
                  <groupId>com.google.guava</groupId>
                  <artifactId>guava</artifactId>
                  <version>25.0-android</version>
                </dependency>
              </dependencies>
            </project>
        """,
        after = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>28.0-jre</version>
                    </dependency>
                  </dependencies>
                </project>
            """
    )

    @Test
    fun upgradeGuavaInParent(@TempDir tempDir: Path) {
        val parent = tempDir.resolve("pom.xml")
        val server = tempDir.resolve("server/pom.xml")
        server.toFile().parentFile.mkdirs()

        parent.toFile().writeText(
            """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                
                  <packaging>pom</packaging>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <properties>
                    <guava.version>25.0-jre</guava.version>
                  </properties>
                </project>
            """.trimIndent()
        )

        server.toFile().writeText(
            """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  
                  <parent>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </parent>
                
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app-server</artifactId>
                  <version>1</version>
                  
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>${'$'}{guava.version}</version>
                    </dependency>
                  </dependencies>
                </project>
            """.trimIndent()
        )

        assertChanged(
            recipe = UpgradeDependencyVersion(
                "com.google.guava",
                null,
                "25-28",
                "-jre",
                null
            ),
            dependsOn = arrayOf(server.toFile()),
            before = parent.toFile(),
            after = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                
                  <packaging>pom</packaging>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <properties>
                    <guava.version>28.0-jre</guava.version>
                  </properties>
                </project>
            """
        )
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/891")
    fun upgradeDependencyOnlyTargetsSpecificDependencyProperty(@TempDir tempDir: Path) {
        val parent = tempDir.resolve("pom.xml")
        val server = tempDir.resolve("server/pom.xml")
        server.toFile().parentFile.mkdirs()
        parent.toFile().writeText(
            """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <packaging>pom</packaging>
                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                    <guava.version>25.0-jre</guava.version>
                    <spring.version>5.3.9</spring.version>
                    <spring.artifact-id>spring-jdbc</spring.artifact-id>
                  </properties>
                </project>
            """.trimIndent()
        )
        server.toFile().writeText(
            """
                <project>
                  <modelVersion>4.0.0</modelVersion>

                  <parent>
                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </parent>

                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app-server</artifactId>
                  <version>1</version>

                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>${'$'}{guava.version}</version>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>${'$'}{spring.artifact-id}</artifactId>
                        <version>${'$'}{spring.version}</version>
                    </dependency>
                  </dependencies>
                </project>
            """.trimIndent()
        )
        assertChanged(
            recipe = UpgradeDependencyVersion(
                "com.google.guava",
                null,
                "25-28",
                "-jre",
                null
            ),
            dependsOn = arrayOf(server.toFile()),
            before = parent.toFile(),
            after = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <packaging>pom</packaging>
                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                    <guava.version>28.0-jre</guava.version>
                    <spring.version>5.3.9</spring.version>
                    <spring.artifact-id>spring-jdbc</spring.artifact-id>
                  </properties>
                </project>
            """
        )
    }

    @Test
    fun upgradeDependencyHandlesDependencyManagement() = assertChanged(
        recipe = UpgradeDependencyVersion(
            "io.micronaut",
            "micronaut-bom",
            "3.0.0-M5",
            null,
            null
        ),
        before = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <packaging>pom</packaging>
                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app-server</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>io.micronaut</groupId>
                        <artifactId>micronaut-bom</artifactId>
                        <version>2.5.11</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
            """,
        after = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <packaging>pom</packaging>
                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app-server</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>io.micronaut</groupId>
                        <artifactId>micronaut-bom</artifactId>
                        <version>3.0.0-M5</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
            """
    )

    @Test
    fun upgradeDependencyHandlesDependencyManagementResolvedFromProperty(@TempDir tempDir: Path) {
        val parent = tempDir.resolve("pom.xml")
        val server = tempDir.resolve("server/pom.xml")
        server.toFile().parentFile.mkdirs()
        parent.toFile().writeText(
            """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <packaging>pom</packaging>
                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                    <micronaut.version>2.5.11</micronaut.version>
                    <spring.version>5.3.9</spring.version>
                    <spring.artifact-id>spring-jdbc</spring.artifact-id>
                  </properties>
                </project>
            """.trimIndent()
        )
        server.toFile().writeText(
            """
                <project>
                  <modelVersion>4.0.0</modelVersion>

                  <parent>
                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </parent>

                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app-server</artifactId>
                  <version>1</version>

                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>io.micronaut</groupId>
                        <artifactId>micronaut-bom</artifactId>
                        <version>${'$'}{micronaut.version}</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>

                  <dependencies>
                    <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>${'$'}{spring.artifact-id}</artifactId>
                        <version>${'$'}{spring.version}</version>
                    </dependency>
                  </dependencies>
                </project>
            """.trimIndent()
        )
        assertChanged(
            recipe = UpgradeDependencyVersion(
                "io.micronaut",
                "micronaut-bom",
                "3.0.0-M5",
                null,
                null
            ),
            dependsOn = arrayOf(server.toFile()),
            before = parent.toFile(),
            after = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <packaging>pom</packaging>
                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                    <micronaut.version>3.0.0-M5</micronaut.version>
                    <spring.version>5.3.9</spring.version>
                    <spring.artifact-id>spring-jdbc</spring.artifact-id>
                  </properties>
                </project>
            """
        )
    }

    @Test
    fun upgradeToExactVersion() = assertChanged(
        recipe = UpgradeDependencyVersion(
            "org.thymeleaf",
            "thymeleaf-spring5",
            "3.0.12.RELEASE",
            null,
            null
        ),
        before = """
            <project>
            <modelVersion>4.0.0</modelVersion>
            
                <packaging>pom</packaging>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
    
                <dependencies>
                    <dependency>
                        <groupId>org.thymeleaf</groupId>
                        <artifactId>thymeleaf-spring5</artifactId>
                        <version>3.0.8.RELEASE</version>
                    </dependency>
                </dependencies>
            </project>
        """,
        after = """
            <project>
            <modelVersion>4.0.0</modelVersion>
            
                <packaging>pom</packaging>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
            
                <dependencies>
                    <dependency>
                        <groupId>org.thymeleaf</groupId>
                        <artifactId>thymeleaf-spring5</artifactId>
                        <version>3.0.12.RELEASE</version>
                    </dependency>
                </dependencies>
            </project>
        """
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = UpgradeDependencyVersion(null, null, null, null, null)
        var valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isFalse
        Assertions.assertThat(valid.failures()).hasSize(2)
        Assertions.assertThat(valid.failures()[0].property).isEqualTo("groupId")
        Assertions.assertThat(valid.failures()[1].property).isEqualTo("newVersion")

        recipe = UpgradeDependencyVersion(null, "rewrite-maven", "latest.release", null, null)
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isFalse
        Assertions.assertThat(valid.failures()).hasSize(1)
        Assertions.assertThat(valid.failures()[0].property).isEqualTo("groupId")

        recipe = UpgradeDependencyVersion("org.openrewrite", null, null, null, null)
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isFalse
        Assertions.assertThat(valid.failures()).hasSize(1)
        Assertions.assertThat(valid.failures()[0].property).isEqualTo("newVersion")

        recipe = UpgradeDependencyVersion("org.openrewrite", "rewrite-maven", "latest.release", null, null)
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isTrue

        recipe = UpgradeDependencyVersion("org.openrewrite", "rewrite-maven", "latest.release", "123", null)
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isTrue

        recipe = UpgradeDependencyVersion("org.openrewrite", "rewrite-maven", "1.0.0", null, null)
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isTrue
    }
}