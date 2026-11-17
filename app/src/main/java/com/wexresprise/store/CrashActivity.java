package com.wexresprise.store;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Shows the uncaught exception that killed the app. Runs in its own process, so it survives the
 * death of the process that crashed.
 */
public class CrashActivity extends Activity {

    public static final String EXTRA_TRACE = "trace";

    public static Intent intent(Context context, String trace) {
        Intent intent = new Intent(context, CrashActivity.class);
        intent.putExtra(EXTRA_TRACE, trace == null ? "" : trace);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String trace = getIntent() == null ? "" : getIntent().getStringExtra(EXTRA_TRACE);
        if (trace == null) {
            trace = "";
        }
        if (trace.trim().isEmpty()) {
            trace = CrashLog.peek(this);
        }
        // The screen now owns the trace: do not show the "previous crash" dialog as well.
        CrashLog.clear(this);

        setContentView(CrashScreen.build(this, trace, new Runnable() {
            @Override
            public void run() {
                resetAppearance();
                Intent main = new Intent(CrashActivity.this, MainActivity.class);
                main.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                try {
                    startActivity(main);
                } catch (Throwable ignored) {
                }
                finish();
            }
        }));
    }

    private void resetAppearance() {
        try {
            new Prefs(this).resetAppearance();
        } catch (Throwable ignored) {
        }
        try {
            com.wexresprise.store.style.StyleManager.get(this).select("");
        } catch (Throwable ignored) {
        }
    }
}
