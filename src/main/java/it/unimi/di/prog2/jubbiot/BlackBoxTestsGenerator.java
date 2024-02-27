/*

Copyright 2024 Massimo Santini

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

public class BlackBoxTestsGenerator {

  public final Path testsDir;

  public BlackBoxTestsGenerator(final String testsDir) {
    this.testsDir = Paths.get(Objects.requireNonNull(testsDir)).toAbsolutePath();
    if (!this.testsDir.toFile().isDirectory())
      throw new IllegalArgumentException("Tests directory not found: " + this.testsDir);
  }

  private static List<DynamicContainer> wrap(List<BlackBoxTest> tc) {
    return tc.stream()
        .map(t -> dynamicContainer(t.path.getFileName().toString(), t.cases))
        .toList();
  }

  public List<? extends DynamicNode> generate() {
    return generateFromSubPackage("");
  }

  public List<? extends DynamicNode> generateFromSubPackage(final String subPkgFqName) {
    final Path path =
        testsDir.resolve(Paths.get(Objects.requireNonNull(subPkgFqName).replace(".", File.separator)));
    if (!path.toFile().isDirectory())
      throw new IllegalArgumentException(
          "Tests directory " + path + " relative to package " + subPkgFqName + " not found");
    final Map<Path, List<BlackBoxTest>> p2t = new TreeMap<>();
    try {
      Files.find(
              path,
              Integer.MAX_VALUE,
              (p, a) -> a.isDirectory() && p.resolve("expected-1.txt").toFile().exists())
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
      List<BlackBoxTest> lt = p2t.entrySet().iterator().next().getValue();
      if (lt.size() == 1) return lt.get(0).cases;
      else return wrap(lt);
    } else {
      return p2t.entrySet().stream()
          .map(
              e ->
                  dynamicContainer(
                      e.getKey().toString().isEmpty() ? "[pkg]" : e.getKey().toString(),
                      wrap(e.getValue())))
          .toList();
    }
  }
}
