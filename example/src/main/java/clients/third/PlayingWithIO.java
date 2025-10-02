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

package clients.third;

import code.Adder;
import java.util.ArrayList;
import java.util.List;

public class PlayingWithIO {
  public static void main(String[] args) {
    List<Integer> values = new ArrayList<>();
    String line;
    while ((line = IO.readln()) != null) values.add(Integer.parseInt(line));
    IO.println(Adder.add(values));
  }
}
