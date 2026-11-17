package com.wexresprise.sample.neon;

import com.wexresprise.store.style.StyleConfig;
import com.wexresprise.store.style.WexStyle;

/**
 * Minimal .ws style. Build it with the steps in docs/styles.md:
 * compile against a copy of style/WexStyle.java and style/StyleConfig.java,
 * convert the classes to classes.dex with d8, and zip the dex together with
 * manifest.json into neon.ws.
 */
public class NeonStyle implements WexStyle {

    @Override
    public void apply(StyleConfig s) {
        s.setAccent(0xFF00E5FF);
        s.background = 0xFF05070C;
        s.surface = 0xFF0A0F16;
        s.backgroundMode = StyleConfig.BACKGROUND_SOLID;
        s.cardStyle = StyleConfig.CARDS_OUTLINE;
        s.cardFill = 0x1400E5FF;
        s.cardStroke = 0xAA00E5FF;
        s.cardRadius = 4f;
        s.radius = 4f;
        s.text = 0xFFE8FBFF;
        s.subtext = 0xFF7FD8E8;
        s.outline = 0x5500E5FF;
        s.density = StyleConfig.DENSITY_COMPACT;
        s.fontScale = 0.95f;
        s.iconTint = 0xFF00E5FF;
    }
}
