package com.david.astrodrill.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.david.astrodrill.GameManager;
import com.david.astrodrill.GameManager.ScreenType;
import com.david.astrodrill.network.BackendClient;
import com.david.astrodrill.network.BackendException;
import com.david.astrodrill.network.dto.LeaderboardEntry;
import com.david.astrodrill.ui.MenuSkin;

import java.util.List;

public class LeaderboardScreen implements Screen {

    private Stage stage;
    private MenuSkin skin;
    private Table entriesTable;
    private Label statusLabel;

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        skin = new MenuSkin();

        Table root = new Table();
        root.setFillParent(true);
        root.top().pad(40);
        stage.addActor(root);

        Label title = new Label("LEADERBOARD", skin.titleStyle);
        statusLabel = new Label("Loading...", skin.smallStyle);

        Table panel = new Table();
        panel.setBackground(skin.panelBackground);
        panel.pad(16);

        Table header = new Table();
        header.defaults().pad(4);
        header.add(new Label("#", skin.labelStyle)).width(40);
        header.add(new Label("Player", skin.labelStyle)).width(220).left();
        header.add(new Label("Max Depth", skin.labelStyle)).width(120).right();
        header.add(new Label("Fastest Launch", skin.labelStyle)).width(160).right();
        panel.add(header).row();

        entriesTable = new Table();
        entriesTable.top();
        entriesTable.defaults().pad(2);
        ScrollPane scroll = new ScrollPane(entriesTable);
        scroll.setFadeScrollBars(false);
        panel.add(scroll).width(560).height(360).row();

        panel.add(statusLabel).padTop(8).row();

        TextButton back = new TextButton("Back", skin.buttonStyle);
        back.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameManager.getInstance().playMenuSound();
                GameManager.getInstance().changeScreen(ScreenType.MAIN_MENU);
            }
        });
        panel.add(back).width(180).height(40).padTop(12).row();

        root.add(title).padBottom(20).row();
        root.add(panel);

        Gdx.input.setInputProcessor(stage);
        loadEntries();
    }

    private void loadEntries() {
        BackendClient.getLeaderboard(new BackendClient.Callback<List<LeaderboardEntry>>() {
            @Override
            public void onSuccess(List<LeaderboardEntry> result) {
                entriesTable.clear();
                if (result == null || result.isEmpty()) {
                    statusLabel.setText("No entries yet — be the first!");
                    return;
                }
                int rank = 1;
                String me = GameManager.getInstance().getCurrentUsername();
                for (LeaderboardEntry e : result) {
                    Table row = new Table();
                    boolean isMe = me != null && me.equals(e.username);
                    row.setBackground(isMe ? skin.rowBackground : null);
                    row.defaults().pad(4);
                    row.add(new Label(String.valueOf(rank++), skin.labelStyle)).width(40);
                    row.add(new Label(e.username, skin.labelStyle)).width(220).left();
                    row.add(new Label(String.valueOf(e.maxDepthMined), skin.labelStyle)).width(120).right();
                    row.add(new Label(formatTime(e.fastestLaunchTime), skin.labelStyle)).width(160).right();
                    entriesTable.add(row).fillX().row();
                }
                statusLabel.setText(result.size() + " entries");
            }

            @Override
            public void onError(BackendException ex) {
                if (ex.statusCode == 0) {
                    statusLabel.setStyle(skin.errorStyle);
                    statusLabel.setText("Backend unreachable.");
                } else {
                    statusLabel.setStyle(skin.errorStyle);
                    statusLabel.setText("Failed: " + ex.getMessage());
                }
            }
        });
    }

    private String formatTime(long ms) {
        if (ms <= 0) return "—";
        long totalSec = ms / 1000;
        long m = totalSec / 60;
        long s = totalSec % 60;
        return String.format("%d:%02d", m, s);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.02f, 0.03f, 0.06f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
    }
}
