package com.wexresprise.store;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Switches the launcher icon by enabling exactly one of the activity-alias entries declared in
 * the manifest. Launchers usually pick the new icon up after a second or two.
 */
public final class LauncherIcons {

    public static final int GREEN = 0;
    public static final int BLUE = 1;
    public static final int VIOLET = 2;
    public static final int ORANGE = 3;
    public static final int PINK = 4;
    public static final int TEAL = 5;
    public static final int COUNT = 6;

    private static final String[] ALIASES = {
            ".IconGreen",
            ".IconBlue",
            ".IconViolet",
            ".IconOrange",
            ".IconPink",
            ".IconTeal"
    };

    private LauncherIcons() {
    }

    public static void apply(Context context, int index) {
        int chosen = Math.max(0, Math.min(COUNT - 1, index));
        PackageManager pm = context.getPackageManager();
        try {
            pm.setComponentEnabledSetting(component(context, chosen),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
            for (int i = 0; i < COUNT; i++) {
                if (i == chosen) {
                    continue;
                }
                pm.setComponentEnabledSetting(component(context, i),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            }
        } catch (Throwable ignored) {
        }
    }

    private static ComponentName component(Context context, int index) {
        return new ComponentName(context.getPackageName(), context.getPackageName() + ALIASES[index]);
    }
}
