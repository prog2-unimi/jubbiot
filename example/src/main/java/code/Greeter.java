package code;

/** An utility class to say hello. */
public class Greeter {

  private Greeter() {}

  /**
   * Greets someone.
   *
   * @param name the name of the person to greet.
   * @return a greeting.
   */
  public static String greet(String name) {
    return "Hello, " + name + "!";
  }
}
