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

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.function.Executable;

/**
 * A class representing a black-box test.
 *
 * <p>A <em>black-box test</em> is a collection of <em>test cases</em> for a class endowed with a
 * {@code main} method. Each test case is defined by a set of files containing <em>command line
 * arguments</em>, the content for the <em>standard input</em>, and the expected content of the
 * <em>standard output</em> stream (the only mandatory file is the one containing the expected
 * result).
 *
 * <p>The files must be contained in a directory structure mimicking the package structure of the
 * class under test. The file names are determined respectively by the following patterns: {@link
 * #ARGS_FORMAT}, {@link #INPUT_FORMAT}, and {@link #EXPECTED_FORMAT}; if the environment variable
 * {@code GENERATE_ACTUAL_FILES} the <em>standard output</em> produced by the class is saved in a
 * file named following the {@link #ACTUAL_FORMAT} pattern.
 *
 * <p>Every test case works by invoking the {@code main} method with the given <em>command line
 * arguments</em>, providing the content for the <em>standard input</em>, generating (and possibly
 * saving) the <em>actual output</em> that is finally compared with the <em>expected output</em>.
 * Every test is run for at most {@link #TIMEOUT} seconds.
 */
public class BlackBoxTest {

  /** The maximum duration of a test case, in seconds. */
  public static final Duration TIMEOUT = Duration.ofSeconds(10);

  /**
   * The format for the filename containing <em>command line arguments</em> for the <em>test
   * case</em> run.
   */
  public static final String ARGS_FORMAT = "args-%d.txt";

  /**
   * The format for the filename where (if the environment variable {@code GENERATE_ACTUAL_FILES} is
   * set) the <em>actual</em> <em>standard output</em> of the <em>test case</em> run will be saved.
   */
  public static final String ACTUAL_FORMAT = "actual-%d.txt";

  /**
   * The format for the filename with the content of the <em>standard input</em> for the <em>test
   * case</em> run.
   */
  public static final String INPUT_FORMAT = "input-%d.txt";

  /**
   * The format for the filename with the expected content of the <em>standard output</em> for the
   * <em>test case</em> run.
   */
  public static final String EXPECTED_FORMAT = "expected-%d.txt";

  private static final Pattern TASK_PATTERN = Pattern.compile("expected-(\\d+).txt");
  private static final boolean GENERATE_ACTUAL_FILES =
      System.getenv("GENERATE_ACTUAL_FILES") != null;

  private final Method main;
  private final Path path;
  private final List<DynamicTest> cases;
  private final ReusableByteArrayInputStream inputStream;

  private class ReusableByteArrayInputStream extends InputStream {

    private InputStream bis;

    public ReusableByteArrayInputStream() {
      this.bis = InputStream.nullInputStream();
    }

    public void resetTo(byte[] buf) {
      Objects.requireNonNull(buf);
      this.bis = new ByteArrayInputStream(buf);
    }

    @Override
    public int read() throws IOException {
      return bis.read();
    }
  }

  private class Case implements Executable {
    private final int num;
    private final String[] args;
    private final byte[] input;
    private final List<String> expected;

    private Case(final int num) throws IOException {
      if (num < 0) throw new IllegalArgumentException("Test number " + num + " is negative");
      this.num = num;
      Path args = path.resolve(String.format(ARGS_FORMAT, num));
      Path input = path.resolve(String.format(INPUT_FORMAT, num));
      Path expected = path.resolve(String.format(EXPECTED_FORMAT, num));
      this.input = input.toFile().exists() ? Files.readAllBytes(input) : new byte[0];
      this.args =
          args.toFile().exists()
              ? trim(Files.readAllLines(args).stream()).toArray(new String[0])
              : new String[0];
      this.expected = trim(Files.readAllLines(expected).stream());
    }

    private static List<String> trim(Stream<String> in) {
      return in.map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public void execute() {
      InputStream stdin = System.in;
      PrintStream stdout = System.out;
      inputStream.resetTo(input);
      System.setIn(inputStream);
      ByteArrayOutputStream actual = new ByteArrayOutputStream();
      System.setOut(new PrintStream(actual));
      try {
        main.invoke(null, (Object) args.clone());
        actual.close();
        if (GENERATE_ACTUAL_FILES)
          Files.write(path.resolve(String.format(ACTUAL_FORMAT, num)), actual.toByteArray());
      } catch (IllegalAccessException
          | IllegalArgumentException
          | InvocationTargetException
          | IOException
          | UncheckedIOException e) {
        fail("Error executing tests", e);
      }
      System.setIn(stdin);
      System.setOut(stdout);
      assertIterableEquals(expected, trim(Arrays.stream(actual.toString().split("\n"))));
    }
  }

  /**
   * Creates a black-box test for the class at the given path.
   *
   * <p>Collects all the <em>test cases</em> for the class at the given path. The class must have a
   * {@code main} method that will be run to execute every <em>test case</em>.
   *
   * @param testsDir the directory containing the tests.
   * @param clsPath the directory containing the <em>test cases</em> for the class, must be a
   *     subdirectory of {@code testsDir}.
   */
  public BlackBoxTest(final Path testsDir, final Path clsPath) {
    if (!Objects.requireNonNull(clsPath, "The test cases directory must not be null")
        .startsWith(Objects.requireNonNull(testsDir, "The tests directory must not be null")))
      throw new IllegalArgumentException(
          "Trying to produce test for " + clsPath + " outside of " + testsDir);
    this.path = clsPath;
    this.inputStream = new ReusableByteArrayInputStream();
    final String fqClsName = testsDir.relativize(clsPath).toString().replace(File.separator, ".");
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
    final Map<Integer, DynamicTest> casesMap = new TreeMap<>();
    final ReusableByteArrayInputStream inputStream = new ReusableByteArrayInputStream();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "expected-*.txt")) {
      for (Path path : stream) {
        final Matcher m = TASK_PATTERN.matcher(path.getFileName().toString());
        if (m.matches()) {
          final int num = Integer.parseInt(m.group(1));
          final String name = fqClsName + " - " + num;
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

  /**
   * Returns the list of <em>test cases</em>.
   *
   * @return the list of <em>test cases</em>.
   */
  public List<DynamicTest> cases() {
    return cases;
  }

  /**
   * Returns a {@link DynamicContainer} wrapping the <em>test cases</em> named after the class name.
   *
   * @return a {@link DynamicContainer} wrapping the <em>test cases</em>.
   */
  public DynamicContainer wrappedCases() {
    return dynamicContainer(path.getFileName().toString(), cases);
  }
}
