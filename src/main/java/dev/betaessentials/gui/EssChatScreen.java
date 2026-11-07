package dev.betaessentials.gui;

import dev.betaessentials.Ess;
import dev.betaessentials.command.Commands;
import net.minecraft.client.gui.screen.ingame.Chat;
import net.minecraft.util.CharacterUtils;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Chat with singleplayer commands, TAB completion and history. Extends Chat so the HUD keeps showing old messages. */
public class EssChatScreen extends Chat {
    private static final List<String> HISTORY = new ArrayList<>();
    private static final int MAX_LEN = 256;

    private String text;
    private int ticks;
    private int historyPos = -1;
    private String historyDraft = "";

    private List<String> suggestions = Collections.emptyList();
    private List<String> cycle;
    private int cycleIdx;
    private String cycleBase;

    public EssChatScreen(String initial) {
        this.text = initial;
    }

    @Override
    public void init() {
        Keyboard.enableRepeatEvents(true);
        updateSuggestions();
    }

    @Override
    public void onClose() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void tick() {
        ticks++;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void keyPressed(char c, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            minecraft.openScreen(null);
            return;
        }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            submit(text.trim());
            minecraft.openScreen(null);
            return;
        }
        if (key == Keyboard.KEY_TAB) {
            tab();
            return;
        }
        cycle = null;
        if (key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN) {
            if (HISTORY.isEmpty()) return;
            if (historyPos < 0) historyDraft = text;
            if (key == Keyboard.KEY_UP) historyPos = historyPos < 0 ? HISTORY.size() - 1 : Math.max(0, historyPos - 1);
            else historyPos = historyPos < 0 ? -1 : historyPos + 1;
            if (historyPos >= HISTORY.size()) historyPos = -1;
            text = historyPos < 0 ? historyDraft : HISTORY.get(historyPos);
            updateSuggestions();
            return;
        }
        if (key == Keyboard.KEY_BACK) {
            if (text.length() > 0) {
                if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                    String t = text.replaceAll("\\s+$", "");
                    int sp = t.lastIndexOf(' ');
                    text = sp < 0 ? "" : t.substring(0, sp + 1);
                } else text = text.substring(0, text.length() - 1);
            }
        } else if (CharacterUtils.validCharacters.indexOf(c) >= 0 && text.length() < MAX_LEN) {
            text += c;
        }
        updateSuggestions();
    }

    private void submit(String s) {
        if (s.isEmpty()) return;
        if (HISTORY.isEmpty() || !HISTORY.get(HISTORY.size() - 1).equals(s)) HISTORY.add(s);
        if (HISTORY.size() > 100) HISTORY.remove(0);
        if (!Ess.active()) {
            // multiplayer: the server handles chat and commands
            if (!minecraft.isCommand(s)) minecraft.player.sendChatMessage(s.length() > 100 ? s.substring(0, 100) : s);
            return;
        }
        if (s.startsWith("/")) Commands.execute(s);
        else Ess.msg("<" + minecraft.player.name + "> " + s);
    }

    private boolean commandMode() {
        return text.startsWith("/") && Ess.active();
    }

    private void updateSuggestions() {
        suggestions = commandMode() ? Commands.complete(text) : Collections.<String>emptyList();
        Collections.sort(suggestions, String.CASE_INSENSITIVE_ORDER);
    }

    private void tab() {
        if (!commandMode()) return;
        if (cycle == null) {
            List<String> c = Commands.complete(text);
            if (c.isEmpty()) return;
            Collections.sort(c, String.CASE_INSENSITIVE_ORDER);
            int cut = text.lastIndexOf(' ') + 1;
            cycleBase = cut <= 0 ? "/" : text.substring(0, cut);
            // complete the common prefix first, like a shell, when it is longer than what was typed
            String typed = text.substring(cycleBase.length());
            String common = commonPrefix(c);
            if (c.size() > 1 && common.length() > typed.length()) {
                text = cycleBase + common;
                updateSuggestions();
                return;
            }
            cycle = c;
            cycleIdx = 0;
        } else {
            cycleIdx = (cycleIdx + 1) % cycle.size();
        }
        text = cycleBase + cycle.get(cycleIdx);
        if (cycle.size() == 1) {
            text += " ";
            cycle = null;
            updateSuggestions();
        }
    }

    private static String commonPrefix(List<String> l) {
        String p = l.get(0);
        for (String s : l) {
            int i = 0;
            while (i < p.length() && i < s.length() && Character.toLowerCase(p.charAt(i)) == Character.toLowerCase(s.charAt(i))) i++;
            p = p.substring(0, i);
        }
        return p;
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        fill(2, height - 14, width - 2, height - 2, Integer.MIN_VALUE);
        String shown = text;
        int maxW = width - 16;
        while (textManager.getTextWidth("> " + shown + "_") > maxW && shown.length() > 0) shown = shown.substring(1);
        drawTextWithShadow(textManager, "> " + shown + (ticks / 6 % 2 == 0 ? "_" : ""), 4, height - 12, 0xE0E0E0);

        List<String> list = cycle != null ? cycle : suggestions;
        if (!list.isEmpty() && !(cycle == null && list.size() == 1 && text.endsWith(list.get(0)))) {
            int n = Math.min(list.size(), 10);
            int start = cycle != null ? Math.max(0, Math.min(cycleIdx - n / 2, list.size() - n)) : 0;
            int w = 0;
            for (int i = start; i < start + n; i++) w = Math.max(w, textManager.getTextWidth(list.get(i)));
            int x = 4 + textManager.getTextWidth("> " + text.substring(0, text.lastIndexOf(' ') + 1));
            x = Math.min(x, width - w - 6);
            int y0 = height - 16 - n * 10 - (list.size() > n ? 10 : 0);
            fill(x - 2, y0 - 2, x + w + 2, height - 15, 0xD0000000);
            for (int i = 0; i < n; i++) {
                int idx = start + i;
                int col = cycle != null && idx == cycleIdx ? 0xFFFF55 : 0xAAAAAA;
                drawTextWithShadow(textManager, list.get(idx), x, y0 + i * 10, col);
            }
            if (list.size() > n) drawTextWithShadow(textManager, "... " + (list.size() - n) + " more", x, y0 + n * 10, 0x777777);
        }
        // widgets (none), but keep vanilla behaviour
        for (Object b : buttons) ((net.minecraft.client.gui.widgets.Button) b).render(minecraft, mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
    }
}
