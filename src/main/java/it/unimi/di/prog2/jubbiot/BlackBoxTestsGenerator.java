/*

Copyright 2025 Massimo Santini

This file is part of jubbiot.

This is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This material is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this file.  If not, see <https://www.gnu.org/licenses/>.

*/

package it.unimi.di.prog2.jubbiot;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;

/** A class to generate {@link BlackBoxTest} instances from a directory tree. */
public class BlackBoxTestsGenerator {

  private static final String TASK_STRING = String.format(BlackBoxTest.EXPECTED_FORMAT, 1);

  private final Path testsDir;

  /**
   * Creates the generator given a directory hierarchy.
   *
   * @param testsDir the root directory containing the hierarchy of <em>test cases</em> directories.
   */
  public BlackBoxTestsGenerator(final String testsDir) {
    this.testsDir = Paths.get(Objects.requireNonNull(testsDir)).toAbsolutePath();
    if (!this.testsDir.toFile().isDirectory())
      throw new IllegalArgumentException("Tests directory not found: " + this.testsDir);
  }

  private static List<DynamicContainer> wrap(List<BlackBoxTest> tc) {
    return tc.stream().map(BlackBoxTest::wrappedCases).toList();
  }

  /**
   * Generates all the <em>test cases</em>.
   *
   * @return the list of <em>test cases</em>.
   */
  public List<? extends DynamicNode> generate() {
    return generate("");
  }

  /**
   * Generates the <em>test cases</em> given a package, or class, name.
   *
   * @param subPkgFqName the fully qualified name of the package or class.
   * @return the list of <em>test cases</em> for the given package, or class.
   */
  public List<? extends DynamicNode> generate(final String subPkgFqName) {
    final Path path =
        testsDir.resolve(
            Paths.get(
                Objects.requireNonNull(subPkgFqName, "The package or class name must not be null")
                    .replace(".", File.separator)));
    if (!path.toFile().isDirectory())
      throw new IllegalArgumentException(
          "Test cases directory "
              + path
              + " relative to package or class "
              + subPkgFqName
              + " not found");
    final Map<Path, List<BlackBoxTest>> p2t = new TreeMap<>();
    try {
      Files.find(
              path,
              Integer.MAX_VALUE,
              (p, a) -> a.isDirectory() && p.resolve(TASK_STRING).toFile().exists(),
              FileVisitOption.FOLLOW_LINKS)
          .forEach(
              p ->
                  p2t.computeIfAbsent(path.relativize(p.getParent()), k -> new LinkedList<>())
                      .add(new BlackBoxTest(testsDir, p)));
    } catch (IOException | UncheckedIOException e) {
      return List.of(
          dynamicTest(
              subPkgFqName + " [missing tests dir]",
              () -> {
                fail("Problems reading tests", e);
              }));
    }
    if (p2t.size() == 0)
      return List.of(
          dynamicTest(
              subPkgFqName + " [missing test cases]",
              () -> {
                fail("No test cases found");
              }));
    else if (p2t.size() == 1) {
      final List<BlackBoxTest> lt = p2t.entrySet().iterator().next().getValue();
      return lt.size() == 1 ? lt.get(0).cases() : wrap(lt);
    } else {
      return p2t.entrySet().stream()
          .map(
              e ->
                  dynamicContainer(
                      e.getKey().toString().isEmpty()
                          ? "[pkg]"
                          : e.getKey().toString().replace(File.separator, "."),
                      wrap(e.getValue())))
          .toList();
    }
  }
}
