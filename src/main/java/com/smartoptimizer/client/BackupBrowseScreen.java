package com.smartoptimizer.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class BackupBrowseScreen extends Screen {

    private static final int PW = 420;
    private static final int PH = 240;

    private final Screen parent;
    private final List<Path> backups;
    private int selected = 0;
    private String feedback = "";
    private long feedbackAt = 0;

    public BackupBrowseScreen(Screen parent) {
        super(Component.empty());
        this.parent = parent;
        this.backups = loadBackups();
    }

    private static List<Path> loadBackups() {
        try {
            Path dir = FMLPaths.GAMEDIR.get().resolve("config/smartoptimizer/backups");
            if (!Files.exists(dir)) return List.of();
            List<Path> result = new ArrayList<>();
            try (var stream = Files.newDirectoryStream(dir)) {
                for (Path p : stream) {
                    if (Files.isDirectory(p)) result.add(p);
                }
            }
            result.sort(Comparator.reverseOrder());
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    protected void init() {
        int cx   = this.width  / 2;
        int py   = this.height / 2 - PH / 2;
        int btnY = py + PH - 28;

        this.addRenderableWidget(Button.builder(
            Component.literal("Open Folder"),
            btn -> openSelected()
        ).bounds(cx - 165, btnY, 105, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Restore"),
            btn -> restoreSelected()
        ).bounds(cx - 55, btnY, 105, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Close"),
            btn -> { if (this.minecraft != null) this.minecraft.setScreen(parent); }
        ).bounds(cx + 55, btnY, 105, 20).build());
    }

    private void openSelected() {
        if (backups.isEmpty()) return;
        try {
            Desktop.getDesktop().open(backups.get(selected).toFile());
        } catch (Exception e) {
            try {
                Runtime.getRuntime().exec(new String[]{"explorer", backups.get(selected).toAbsolutePath().toString()});
            } catch (Exception ignored) {}
        }
    }

    private void restoreSelected() {
        if (backups.isEmpty()) return;
        try {
            Path backup  = backups.get(selected);
            Path gameDir = FMLPaths.GAMEDIR.get();

            Path backupOptions = backup.resolve("options.txt");
            if (Files.exists(backupOptions))
                Files.copy(backupOptions, gameDir.resolve("options.txt"), StandardCopyOption.REPLACE_EXISTING);

            Path configDir = gameDir.resolve("config");
            Files.walkFileTree(backup, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.getFileName().toString().equals("options.txt")) return FileVisitResult.CONTINUE;
                    Path rel  = backup.relativize(file);
                    Path dest = configDir.resolve(rel);
                    Files.createDirectories(dest.getParent());
                    Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });

            feedback   = "Restored: " + backup.getFileName();
            feedbackAt = System.currentTimeMillis();
        } catch (Exception e) {
            feedback   = "Restore failed: " + e.getMessage();
            feedbackAt = System.currentTimeMillis();
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int cx = this.width  / 2;
        int cy = this.height / 2;
        int px = cx - PW / 2;
        int py = cy - PH / 2;

        g.fill(0, 0, this.width, this.height, 0x88000000);
        g.fill(px, py, px + PW, py + PH, 0xE0101820);
        drawBorder(g, px, py, PW, PH, 0xFF4466AA);

        g.drawCenteredString(this.font, "Config Backups", cx, py + 9, 0xFF88AAFF);

        if (backups.isEmpty()) {
            g.drawCenteredString(this.font, "No backups found.", cx, cy - 10, 0xFF888888);
        } else {
            g.drawString(this.font, "Select a backup to restore or open:", px + 12, py + 25, 0xFF666688, false);
            int lineY = py + 38;
            for (int i = 0; i < Math.min(backups.size(), 10); i++) {
                String name = backups.get(i).getFileName().toString();
                if (i == selected) g.fill(px + 8, lineY - 1, px + PW - 8, lineY + 10, 0x33AACCFF);
                int col = (i == selected) ? 0xFFFFFF88 : 0xFFAAAAAA;
                g.drawString(this.font, (i == selected ? "> " : "  ") + name, px + 12, lineY, col, false);
                lineY += 13;
            }
        }

        if (!feedback.isEmpty() && System.currentTimeMillis() - feedbackAt < 4000)
            g.drawCenteredString(this.font, feedback, cx, py + PH - 44, 0xFF55FF88);

        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && !backups.isEmpty()) {
            int cx = this.width  / 2;
            int cy = this.height / 2;
            int px = cx - PW / 2;
            int py = cy - PH / 2;
            int listY = py + 38;
            int row = (int)((my - listY) / 13);
            if (row >= 0 && row < Math.min(backups.size(), 10)) {
                selected = row;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
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
