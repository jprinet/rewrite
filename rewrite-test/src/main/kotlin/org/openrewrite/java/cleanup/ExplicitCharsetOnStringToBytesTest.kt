/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface ExplicitCharsetOnStringToBytesTest: JavaRecipeTest {
    override val recipe: Recipe
        get() = ExplicitCharsetOnStringToBytes(null)

    @Suppress("ResultOfMethodCallIgnored")
    @Test
    fun addUtf8(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class Test {
                void test() {
                    String s = "hi";
                    s.getBytes();
                }
            }
        """,
        after = """
            import java.nio.charset.StandardCharsets;
            
            public class Test {
                void test() {
                    String s = "hi";
                    s.getBytes(StandardCharsets.UTF_8);
                }
            }
        """
    )
}