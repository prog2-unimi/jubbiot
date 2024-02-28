package clients.first;

import code.Adder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AnotherClass {
  public static void main(String[] args) {
    List<Integer> values = new ArrayList<>();
    try (Scanner in = new Scanner(System.in)) {
      while (in.hasNextInt()) values.add(in.nextInt());
    }
    System.out.println(Adder.add(values));
  }
}
