package com.shr.hihotfix;

import android.content.Context;
import android.widget.Toast;

public class Test {

    public void test(Context context) {
        int i = 9;
        int j = 3;
        Toast.makeText(context, (i / j) + "", Toast.LENGTH_SHORT).show();
    }
}
