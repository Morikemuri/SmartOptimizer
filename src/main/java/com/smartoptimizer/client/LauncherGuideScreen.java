package com.smartoptimizer.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public class LauncherGuideScreen extends Screen {

    private static final int PW = 440;
    private static final int PH = 210;

    private static final String[][] LAUNCHERS = {
        { "CurseForge",
          "1. Open CurseForge app",
          "2. My Modpacks → click [...] next to your pack",
          "3. Select 'Profile Options'",
          "4. Enable the 'Java Settings' toggle",
          "5. Paste into 'Additional Java Arguments'",
          "6. Click Done → relaunch"
        },
        { "Prism Launcher",
          "1. Right-click your instance → 'Edit'",
          "2. Go to the 'Settings' tab",
          "3. Find 'JVM Arguments' field",
          "4. Paste at the end of the existing text",
          "5. Close the dialog → relaunch"
        },
        { "TLauncher",
          "1. Click the gear icon next to 'Enter'",
          "2. Select 'Advanced settings'",
          "3. Find 'JVM arguments' field",
          "4. Paste after existing arguments",
          "5. Save → relaunch"
        },
        { "MultiMC / ATLauncher",
          "1. Right-click instance → 'Edit Instance'",
          "2. Go to Settings → Java tab",
          "3. Check 'Override Java arguments'",
          "4. Paste into the 'JVM Arguments' field",
          "5. Close → relaunch"
        }
    };

    private static final String[] TAB_NAMES = { "CurseForge", "Prism", "TLauncher", "MultiMC" };

    private final Screen parent;
    private final String fixArgs;
    private int selected = 0;

    public LauncherGuideScreen(Screen parent, String fixArgs) {
        super(Component.empty());
        this.parent  = parent;
        this.fixArgs = fixArgs;
    }

    @Override
    protected void init() {
        int cx   = this.width  / 2;
        int py   = this.height / 2 - PH / 2;
        // Tabs sit below the argument line (which is at py+26)
        int tabY  = py + 46;
        int backY = py + PH - 28;

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            this.addRenderableWidget(Button.builder(
                Component.literal(TAB_NAMES[i]),
                btn -> selected = idx
            ).bounds(cx - 210 + i * 107, tabY, 102, 16).build());
        }

        this.addRenderableWidget(Button.builder(
            Component.literal(I18n.get("smartoptimizer.jvm.guide.back")),
            btn -> { if (this.minecraft != null) this.minecraft.setScreen(parent); }
        ).bounds(cx - 50, backY, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int cx = this.width  / 2;
        int cy = this.height / 2;
        int px = cx - PW / 2;
        int py = cy - PH / 2;

        g.fill(0, 0, this.width, this.height, 0x88000000);
        g.fill(px, py, px + PW, py + PH, 0xE0101020);
        drawBorder(g, px, py, PW, PH, 0xFF4466AA);

        // Title
        g.drawCenteredString(this.font,
                I18n.get("smartoptimizer.jvm.guide.title"), cx, py + 9, 0xFF88AAFF);

        // Argument to add (above the tab row)
        String argDisplay = fixArgs;
        while (this.font.width(argDisplay) > PW - 40) {
            argDisplay = argDisplay.substring(0, argDisplay.length() - 4) + "...";
        }
        g.drawCenteredString(this.font, argDisplay, cx, py + 27, 0xFF88FFAA);

        // Active launcher label (below tab buttons at py+46+16 = py+62)
        g.drawCenteredString(this.font,
                "[ " + LAUNCHERS[selected][0] + " ]", cx, py + 68, 0xFFFFCC44);

        // Step-by-step instructions
        int lineY = py + 84;
        for (int i = 1; i < LAUNCHERS[selected].length; i++) {
            g.drawString(this.font, LAUNCHERS[selected][i], px + 20, lineY, 0xFFCCCCCC, false);
            lineY += 12;
        }

        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int col) {
        g.fill(x,         y,         x + w,     y + 1,     col);
        g.fill(x,         y + h - 1, x + w,     y + h,     col);
        g.fill(x,         y,         x + 1,     y + h,     col);
        g.fill(x + w - 1, y,         x + w,     y + h,     col);
    }
}
