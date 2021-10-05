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
package org.openrewrite.groovy;

import groovy.lang.GroovyClassLoader;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.*;
import org.codehaus.groovy.control.io.InputStreamReaderSource;
import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.style.NamedStyles;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class GroovyParser implements Parser<G.CompilationUnit> {
    @Nullable
    private final Collection<Path> classpath;

    @Override
    public List<G.CompilationUnit> parse(@Language("groovy") String... sources) {
        Pattern packagePattern = Pattern.compile("^package\\s+([^;]+);");
        Pattern classPattern = Pattern.compile("(class|interface|enum)\\s*(<[^>]*>)?\\s+(\\w+)");

        Function<String, String> simpleName = sourceStr -> {
            Matcher classMatcher = classPattern.matcher(sourceStr);
            return classMatcher.find() ? classMatcher.group(3) : null;
        };

        return parseInputs(
                Arrays.stream(sources)
                        .map(sourceFile -> {
                            Matcher packageMatcher = packagePattern.matcher(sourceFile);
                            String pkg = packageMatcher.find() ? packageMatcher.group(1).replace('.', '/') + "/" : "";

                            String className = Optional.ofNullable(simpleName.apply(sourceFile))
                                    .orElse(Long.toString(System.nanoTime())) + ".java";

                            Path path = Paths.get(pkg + className);
                            return new Input(
                                    path,
                                    () -> new ByteArrayInputStream(sourceFile.getBytes())
                            );
                        })
                        .collect(toList()),
                null,
                new InMemoryExecutionContext()
        );
    }

    @Override
    public List<G.CompilationUnit> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        List<G.CompilationUnit> cus = new ArrayList<>();
        Map<String, JavaType.Class> sharedClassTypes = new HashMap<>();

        for (Input input : sources) {
            CompilerConfiguration configuration = new CompilerConfiguration();
            configuration.setTolerance(Integer.MAX_VALUE);
            configuration.setDebug(true);
            configuration.setClasspathList(classpath == null ? emptyList() : classpath.stream()
                    .map(cp -> cp.toFile().toString())
                    .collect(toList()));

            ErrorCollector errorCollector = new ErrorCollector(configuration);
            SourceUnit unit = new SourceUnit(
                    "doesntmatter",
                    new InputStreamReaderSource(input.getSource(), configuration),
                    configuration,
                    null,
                    errorCollector
            );

            GroovyClassLoader transformLoader = new GroovyClassLoader(getClass().getClassLoader());

            CompilationUnit compUnit = new CompilationUnit(configuration, null, null, transformLoader);
            compUnit.addSource(unit);

            try {
                compUnit.compile(Phases.CANONICALIZATION);
                ModuleNode ast = unit.getAST();
                GroovyParserVisitor mappingVisitor = new GroovyParserVisitor(
                        input.getPath(),
                        StringUtils.readFully(input.getSource()),
                        sharedClassTypes,
                        ctx
                );
                cus.add(mappingVisitor.visit(unit, ast));
            } catch (Throwable t) {
                ctx.getOnError().accept(t);
            } finally {
                if(errorCollector.hasErrors() || errorCollector.hasWarnings()) {
//                    org.slf4j.LoggerFactory.getLogger(GroovyParser.class).warn(log);
                    errorCollector.write(new PrintWriter(System.out), new Janitor());
                }
            }
        }

        return cus;
    }

    @Override
    public boolean accept(Path path) {
        return path.toString().endsWith(".groovy");
    }

    @Override
    public GroovyParser reset() {
        return this;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        @Nullable
        protected Collection<Path> classpath = JavaParser.runtimeClasspath();

        protected Collection<byte[]> classBytesClasspath = emptyList();

        @Nullable
        protected Collection<Parser.Input> dependsOn;

        protected boolean logCompilationWarningsAndErrors = false;
        protected final List<NamedStyles> styles = new ArrayList<>();

        public Builder logCompilationWarningsAndErrors(boolean logCompilationWarningsAndErrors) {
            this.logCompilationWarningsAndErrors = logCompilationWarningsAndErrors;
            return this;
        }

        public Builder dependsOn(Collection<Input> inputs) {
            this.dependsOn = inputs;
            return this;
        }

        public Builder classpath(Collection<Path> classpath) {
            this.classpath = classpath;
            return this;
        }

        public Builder classpath(String... classpath) {
            this.classpath = JavaParser.dependenciesFromClasspath(classpath);
            return this;
        }

        public Builder classpath(byte[]... classpath) {
            this.classBytesClasspath = Arrays.asList(classpath);
            return this;
        }

        public Builder styles(Iterable<? extends NamedStyles> styles) {
            for (NamedStyles style : styles) {
                this.styles.add(style);
            }
            return this;
        }

        public GroovyParser build() {
            return new GroovyParser(classpath);
        }
    }
}