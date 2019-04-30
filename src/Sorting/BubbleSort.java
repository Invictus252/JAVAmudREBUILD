package Sorting;

import java.io.PrintWriter;
import mclamud.Ansi;

public class BubbleSort {
    // logic to sort the elements
    public static void bubble_srt(PrintWriter out,int array[]) {
        int n = array.length;
        int k;
        for (int m = n; m >= 0; m--) {
            for (int i = 0; i < n - 1; i++) {
                k = i + 1;
                if (array[i] > array[k]) {
                    swapNumbers(out,i, k, array);
                }
            }
            printNumbers(out,array);
        }
    }
  
    private static void swapNumbers( PrintWriter out,int i, int j, int[] array) {
  
        int temp;
        temp = array[i];
        //out.print(Ansi.YELLOW + String.valueOf(temp));
        array[i] = array[j];
        //out.print(Ansi.RED + String.valueOf(array[i]));
        array[j] = temp;
        //out.print(Ansi.GREEN + String.valueOf(array[j]));
        //out.print(Ansi.SANE);
    }
  
    private static void printNumbers(PrintWriter out,int[] input) {
          
        for (int i = 0; i < input.length; i++) {
            out.print(Ansi.RED + input[i] + ", ");
        }
        out.println("\n" + Ansi.SANE);
    }    
    
}
