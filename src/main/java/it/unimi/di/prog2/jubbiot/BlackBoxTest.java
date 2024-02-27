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

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.function.Executable;

public class BlackBoxTest {

  public static final boolean GENERATE_ACTUAL_FILES =
      System.getenv("GENERATE_ACTUAL_FILES") != null;

  public static final Duration TIMEOUT = Duration.ofSeconds(10);
  public static final String ARGS_FORMAT = "args-%d.txt";
  public static final String INPUT_FORMAT = "input-%d.txt";
  public static final String EXPECTED_FORMAT = "expected-%d.txt";
  public static final String ACTUAL_FORMAT = "actual-%d.txt";
  public static final Pattern TASK_PATTERN = Pattern.compile("expected-(\\d+).txt");

  private final Method main;
  public final Path path;
  public final List<DynamicTest> cases;

  private class Case implements Executable {
    private final int num;
    private final String[] args;
    private final byte[] input;
    private final List<String> expected;

    private Case(int num) throws IOException {
      this.num = num;
      Path args = path.resolve(String.format(ARGS_FORMAT, num));
      Path input = path.resolve(String.format(INPUT_FORMAT, num));
      Path expected = path.resolve(String.format(EXPECTED_FORMAT, num));
      this.input = input.toFile().exists() ? Files.readAllBytes(input) : new byte[0];
      this.args =
          args.toFile().exists()
              ? trim(Files.readAllLines(args)).toArray(new String[0])
              : new String[0];
      this.expected = trim(Files.readAllLines(expected));
    }

    private static List<String> trim(List<String> in) {
      List<String> out = new ArrayList<>();
      for (String s : in) {
        String t = s.trim();
        if (s.isEmpty()) continue;
        out.add(t);
      }
      return Collections.unmodifiableList(out);
    }

    public void execute() {
      InputStream stdin = System.in;
      PrintStream stdout = System.out;
      System.setIn(new ByteArrayInputStream(input));
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      System.setOut(new PrintStream(baos));
      try {
        main.invoke(null, (Object) args.clone());
        baos.close();
        if (GENERATE_ACTUAL_FILES)
          Files.write(path.resolve(String.format(ACTUAL_FORMAT, num)), baos.toByteArray());
      } catch (IllegalAccessException
          | IllegalArgumentException
          | InvocationTargetException
          | IOException
          | UncheckedIOException e) {
        fail("Error executing tests", e);
      }
      System.setIn(stdin);
      System.setOut(stdout);
      assertIterableEquals(expected, trim(Arrays.asList(baos.toString().split("\n"))));
    }
  }

  public BlackBoxTest(final Path testsDir, final Path fClsPath) {
    if (!fClsPath.startsWith(testsDir))
      throw new IllegalArgumentException(
          "Trying to produce test for " + fClsPath + " outside of " + testsDir);
    Path rClsPath = testsDir.relativize(fClsPath);
    this.path = Objects.requireNonNull(fClsPath);
    String fqClsName = rClsPath.toString().replace(File.separator, ".");
    Method main = null;
    try {
      main = Class.forName(fqClsName).getMethod("main", String[].class);
    } catch (NoSuchMethodException | SecurityException | ClassNotFoundException e) {
      this.main = null;
      cases =
          List.of(
              dynamicTest(
                  fqClsName + " [missing main method]",
                  () -> {
                    assumeTrue(false, "Main not found");
                  }));
      return;
    }
    this.main = main;
    Map<Integer, DynamicTest> casesMap = new TreeMap<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "expected-*.txt")) {
      for (Path path : stream) {
        Matcher m = TASK_PATTERN.matcher(path.getFileName().toString());
        if (m.matches()) {
          int num = Integer.parseInt(m.group(1));
          String name = fqClsName + " - " + num;
          try {
            final Case tc = new Case(num);
            casesMap.put(
                num,
                dynamicTest(
                    name,
                    () -> {
                      assertTimeoutPreemptively(TIMEOUT, tc);
                    }));
          } catch (IOException | UncheckedIOException e) {
            casesMap.put(
                num,
                dynamicTest(
                    name + " [problem reading testcase]",
                    () -> {
                      fail("Problems reading test case", e);
                    }));
          }
        }
      }
    } catch (IOException | UncheckedIOException e) {
      casesMap.put(
          -1,
          dynamicTest(
              fqClsName + " [missing tests dir]",
              () -> {
                fail("Problems reading tests", e);
              }));
    }
    this.cases = List.copyOf(casesMap.values());
  }
}
