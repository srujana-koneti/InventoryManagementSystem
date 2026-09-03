package com.inventory.dsa;

import com.inventory.model.Product;
import java.util.Comparator;
import java.util.List;

/**
 * Custom Sorting Utility implementing Merge Sort from scratch.
 *
 * DSA Concept: Divide and Conquer Merge Sort
 * Time Complexity:  O(n log n) in all cases (Best, Average, Worst)
 * Space Complexity: O(n) auxiliary space for temporary arrays during merge
 * Stability: Stable (preserves relative order of equal elements)
 *
 * NOTE: Does NOT use Collections.sort(), Arrays.sort(), or any built-in sorting routines.
 */
public class SortUtil {

    /**
     * Entry point to sort a List of Product objects using custom Merge Sort.
     *
     * @param list       the list of products to sort in-place
     * @param comparator the comparison logic
     */
    public static void mergeSort(List<Product> list, Comparator<Product> comparator) {
        if (list == null || list.size() <= 1) {
            return;
        }
        mergeSortInternal(list, 0, list.size() - 1, comparator);
    }

    /**
     * Recursive helper method that divides the list into halves.
     */
    private static void mergeSortInternal(List<Product> list, int left, int right, Comparator<Product> comparator) {
        if (left < right) {
            // Find the middle point to divide the array into two halves
            int mid = left + (right - left) / 2;

            // Recursively sort the first and second halves
            mergeSortInternal(list, left, mid, comparator);
            mergeSortInternal(list, mid + 1, right, comparator);

            // Merge the two sorted halves
            merge(list, left, mid, right, comparator);
        }
    }

    /**
     * Merges two sorted subarrays of list:
     * Subarray 1: list[left ... mid]
     * Subarray 2: list[mid + 1 ... right]
     */
    private static void merge(List<Product> list, int left, int mid, int right, Comparator<Product> comparator) {
        int n1 = mid - left + 1;
        int n2 = right - mid;

        // Create auxiliary temporary arrays
        Product[] leftArray = new Product[n1];
        Product[] rightArray = new Product[n2];

        // Copy data into temporary arrays
        for (int i = 0; i < n1; i++) {
            leftArray[i] = list.get(left + i);
        }
        for (int j = 0; j < n2; j++) {
            rightArray[j] = list.get(mid + 1 + j);
        }

        // Merge the temporary arrays back into the original list[left ... right]
        int i = 0; // Initial index of first subarray
        int j = 0; // Initial index of second subarray
        int k = left; // Initial index of merged subarray in original list

        while (i < n1 && j < n2) {
            // Using comparator: if left <= right, take left (ensuring stability)
            if (comparator.compare(leftArray[i], rightArray[j]) <= 0) {
                list.set(k, leftArray[i]);
                i++;
            } else {
                list.set(k, rightArray[j]);
                j++;
            }
            k++;
        }

        // Copy remaining elements of leftArray[], if any
        while (i < n1) {
            list.set(k, leftArray[i]);
            i++;
            k++;
        }

        // Copy remaining elements of rightArray[], if any
        while (j < n2) {
            list.set(k, rightArray[j]);
            j++;
            k++;
        }
    }

    // Convenience sorting methods for Product properties

    /**
     * Sorts products alphabetically by name (A to Z).
     */
    public static void sortByNameAscending(List<Product> list) {
        mergeSort(list, new Comparator<Product>() {
            @Override
            public int compare(Product p1, Product p2) {
                return p1.getName().compareToIgnoreCase(p2.getName());
            }
        });
    }

    /**
     * Sorts products by price ascending (low to high).
     */
    public static void sortByPriceAscending(List<Product> list) {
        mergeSort(list, new Comparator<Product>() {
            @Override
            public int compare(Product p1, Product p2) {
                return Double.compare(p1.getPrice(), p2.getPrice());
            }
        });
    }

    /**
     * Sorts products by price descending (high to low).
     */
    public static void sortByPriceDescending(List<Product> list) {
        mergeSort(list, new Comparator<Product>() {
            @Override
            public int compare(Product p1, Product p2) {
                return Double.compare(p2.getPrice(), p1.getPrice());
            }
        });
    }

    /**
     * Sorts products by quantity ascending (low to high).
     */
    public static void sortByQuantityAscending(List<Product> list) {
        mergeSort(list, new Comparator<Product>() {
            @Override
            public int compare(Product p1, Product p2) {
                return Integer.compare(p1.getQuantity(), p2.getQuantity());
            }
        });
    }

    /**
     * Sorts products by quantity descending (high to low).
     */
    public static void sortByQuantityDescending(List<Product> list) {
        mergeSort(list, new Comparator<Product>() {
            @Override
            public int compare(Product p1, Product p2) {
                return Integer.compare(p2.getQuantity(), p1.getQuantity());
            }
        });
    }
}
