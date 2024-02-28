package code;

import java.util.List;

/** An utility class for summing integers. */
public class Adder {

  private Adder() {}

  /** Adds integers.
   * 
   * @param values the values to add.
   * @return their sum.
   */
  public static int add(List<Integer> values) {
    int sum = 0;
    for (int value : values) sum += value;
    return sum;
  }
}
