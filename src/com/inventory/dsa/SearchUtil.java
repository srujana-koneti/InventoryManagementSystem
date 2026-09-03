package com.inventory.dsa;

import com.inventory.model.Product;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom Search Utility demonstrating Linear Search and Binary Search algorithms.
 *
 * DSA Concepts:
 * 1. Linear Search:
 *    - Sequentially checks each element in the list.
 *    - Works on unsorted data and supports partial substring matches.
 *    - Time Complexity: O(n), Space Complexity: O(1) auxiliary.
 *
 * 2. Binary Search:
 *    - Repeatedly divides the search interval in half.
 *    - STRICT PRE-CONDITION: The list MUST be sorted by the search key (Product Name).
 *    - Time Complexity: O(log n), Space Complexity: O(1) iterative.
 *
 * NOTE: Does NOT use Collections.binarySearch() or library search routines.
 */
public class SearchUtil {

    /**
     * Performs a Linear Search for products whose names contain the query string (case-insensitive).
     * Works on unsorted lists.
     *
     * @param list  the list of products (can be unsorted)
     * @param query the partial name to look for
     * @return a list of matching Product objects
     */
    public static List<Product> linearSearchByName(List<Product> list, String query) {
        List<Product> results = new ArrayList<>();
        if (list == null || query == null) {
            return results;
        }

        String lowerQuery = query.trim().toLowerCase();
        for (int i = 0; i < list.size(); i++) {
            Product p = list.get(i);
            if (p.getName().toLowerCase().contains(lowerQuery)) {
                results.add(p);
            }
        }
        return results;
    }

    /**
     * Performs a Linear Search for products matching a given category (case-insensitive).
     * Works on unsorted lists.
     *
     * @param list     the list of products (can be unsorted)
     * @param category the category to match
     * @return a list of matching Product objects
     */
    public static List<Product> linearSearchByCategory(List<Product> list, String category) {
        List<Product> results = new ArrayList<>();
        if (list == null || category == null) {
            return results;
        }

        String lowerCat = category.trim().toLowerCase();
        for (int i = 0; i < list.size(); i++) {
            Product p = list.get(i);
            if (p.getCategory().toLowerCase().contains(lowerCat)) {
                results.add(p);
            }
        }
        return results;
    }

    /**
     * Performs an exact Binary Search on a name-sorted list of products.
     *
     * IMPORTANT PRE-CONDITION:
     * The input list MUST already be sorted in ascending order by product name!
     * (e.g. using SortUtil.sortByNameAscending(list))
     *
     * @param sortedList the list sorted by product name in ascending order
     * @param exactName  the exact product name to find (case-insensitive match)
     * @return the matching Product, or null if not found
     */
    public static Product binarySearchByName(List<Product> sortedList, String exactName) {
        if (sortedList == null || sortedList.isEmpty() || exactName == null) {
            return null;
        }

        int low = 0;
        int high = sortedList.size() - 1;

        while (low <= high) {
            // Calculate mid to avoid potential integer overflow: low + (high - low) / 2
            int mid = low + (high - low) / 2;
            Product midProduct = sortedList.get(mid);

            int comparison = midProduct.getName().compareToIgnoreCase(exactName.trim());

            if (comparison == 0) {
                // Key found at index mid!
                return midProduct;
            } else if (comparison < 0) {
                // midProduct.name comes before exactName alphabetically
                // The target must be in the right half
                low = mid + 1;
            } else {
                // midProduct.name comes after exactName alphabetically
                // The target must be in the left half
                high = mid - 1;
            }
        }

        // Element not found in the list
        return null;
    }
}
