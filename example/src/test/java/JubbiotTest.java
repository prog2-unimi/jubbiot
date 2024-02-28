import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import it.unimi.di.prog2.jubbiot.BlackBoxTestsGenerator;

/** The class running tests discovered by {@link BlackBoxTestsGenerator}. */
public class JubbiotTest {

  /**
   * Generates tests for the class {@code clients.first.AClass}.
   * 
   * @return the tests.
   * @throws IOException in case test discovery fails.
   */
  @TestFactory
  public List<? extends DynamicNode> testAClass() throws IOException {
    return new BlackBoxTestsGenerator("tests").generate("clients.first.AClass");
  }

  @TestFactory
  /**
   * Generates tests for the package {@code clients.first}.
   * 
   * @return the tests.
   * @throws IOException in case test discovery fails.
   */
  public List<? extends DynamicNode> testFirstPackage() throws IOException {
    return new BlackBoxTestsGenerator("tests").generate("clients.first");
  }

  /**
   * Generates tests for all the classes.
   * 
   * @return the tests.
   * @throws IOException in case test discovery fails.
   */
  @TestFactory
  public List<? extends DynamicNode> testAll() throws IOException {
    return new BlackBoxTestsGenerator("tests").generate();
  }
}