package com.crispy.utils;

import fraser.neil.plaintext.diff_match_patch;

import java.util.LinkedList;

/**
 * Created by harsh on 2/23/17.
 */
public class Diff {

    public static String compare(String s1, String s2) {
        StringBuilder sb = new StringBuilder();
        diff_match_patch differ = new diff_match_patch();

        LinkedList<diff_match_patch.Diff> l = differ.diff_main(s1, s2);
        differ.diff_cleanupSemantic(l);
        for (diff_match_patch.Diff d : l) {
            if (d.operation == diff_match_patch.Operation.EQUAL) continue;
            sb.append(d.operation.toString() + " " + d.text.trim());
            sb.append("\n");
        }
        return sb.toString();
    }


    public static void main(String[] args) {
        String s1 = "a\nb\nc";
        String s2 = "a\nc";

        String diff = compare(s1, s2);
        System.out.println(diff);
    }
}
