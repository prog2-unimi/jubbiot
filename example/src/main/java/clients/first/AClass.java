package clients.first;

import code.Adder;
import java.util.Arrays;
import java.util.List;

public class AClass {
  public static void main(String[] args) {
    List<Integer> values = Arrays.stream(args).map(Integer::parseInt).toList();
    System.out.println(Adder.add(values));
  }
}
