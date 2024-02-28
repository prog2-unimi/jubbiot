package clients.first;

import java.util.Arrays;
import java.util.List;

import code.Adder;

public class AClass {
  public static void main(String[] args) {
    List<Integer> values = Arrays.stream(args).map(Integer::parseInt).toList();
    System.out.println(Adder.add(values));
  }
}
