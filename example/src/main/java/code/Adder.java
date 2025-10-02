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

package code;

import java.util.List;

/** An utility class for summing integers. */
public class Adder {

  private Adder() {}

  /**
   * Adds integers.
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
