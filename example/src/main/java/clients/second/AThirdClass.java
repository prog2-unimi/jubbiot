package clients.second;

import code.Adder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class AThirdClass {
  public static void main(String[] args) {
    List<Integer> valuesFromArgs = Arrays.stream(args).map(Integer::parseInt).toList();

    List<Integer> valuesFromStdIn = new ArrayList<>();
    try (Scanner in = new Scanner(System.in)) {
      while (in.hasNextInt()) valuesFromStdIn.add(in.nextInt());
    }

    List<Integer> values = List.of(Adder.add(valuesFromArgs), Adder.add(valuesFromStdIn));
    System.out.println(Adder.add(values));
  }
}
