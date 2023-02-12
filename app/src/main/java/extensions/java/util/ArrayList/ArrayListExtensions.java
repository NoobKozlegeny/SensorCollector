package extensions.java.util.ArrayList;

import java.util.ArrayList;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;

@Extension
public class ArrayListExtensions {
    public static ArrayList<String> convertEmptyDataToDefault(@This ArrayList<String> thiz) {
        for (int i = 0; i < thiz.size(); i++) {
            if (thiz[i].equals("")) {
                thiz[i] = "0,0,0";
            }
        }
        return thiz;
    }

    public static ArrayList<String> equalizeSensorList(@This ArrayList<String> thiz, int maxLength, String lineToAdd) {
        int i = thiz.size();
        while (i < maxLength) {
            thiz.add(lineToAdd);
            i++;
        }
        return thiz;
    }
}