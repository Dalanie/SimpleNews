package de.dala.simplenews.utilities;

import android.content.Context;
import android.os.Environment;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.dala.simplenews.common.Entry;

/**
 * Created by Daniel-L on 17.06.2015.
 */
public class Utilities {
    private static final String SMARTIES_VERSION = "smarties";

    public static boolean equals(Object a, Object b) {
        return (a == null) ? (b == null) : a.equals(b);
    }

    public static boolean isSmartiesVersion(Context context) {
        return context.getPackageName().toLowerCase().contains(SMARTIES_VERSION);
    }

    public static boolean isFreeVersion(Context context) {
        return !isSmartiesVersion(context);
    }

    public static <T> List<T> intersection(List<T> list1, List<T> list2) {
        List<T> list = new ArrayList<T>();
        for (T t : list1) {
            if (list2.contains(t)) {
                list.add(t);
            }
        }
        return list;
    }

    public static <T> List<T> nonIntersection(List<T> list1, List<T> list2) {
        List<T> list = new ArrayList<T>();
        for (T t : list1) {
            if (!list2.contains(t)) {
                list.add(t);
            }
        }
        return list;
    }
}
