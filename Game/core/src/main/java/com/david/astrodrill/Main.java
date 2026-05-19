package com.david.astrodrill;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.david.astrodrill.network.BackendClient;
import com.david.astrodrill.network.SaveStateSerializer;
import com.david.astrodrill.network.dto.SaveRequest;
import com.david.astrodrill.screen.PlayScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {

    private static final long EXIT_SAVE_TIMEOUT_MS = 2000;

    @Override
    public void create() {
        GameManager.getInstance().initialize(this);
        GameManager.getInstance().changeScreen(GameManager.ScreenType.LOGIN);
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        attemptExitSave();
        super.dispose();
    }

    private void attemptExitSave() {
        GameManager gm = GameManager.getInstance();
        if (!gm.isLoggedIn()) return;
        if (!(getScreen() instanceof PlayScreen)) return;

        PlayScreen ps = (PlayScreen) getScreen();
        try {
            String json = SaveStateSerializer.snapshot(ps, ps.getPlayer(), ps.getLanderHub());
            SaveRequest req = new SaveRequest(
                    gm.getCurrentPlayerId(), json, 0, "PROXIMA_B", ps.getSessionMaxDepth(), 0L);
            boolean ok = BackendClient.saveAndWait(req, EXIT_SAVE_TIMEOUT_MS);
            if (!ok) Gdx.app.log("Main", "Exit save failed or timed out");
        } catch (Exception ex) {
            Gdx.app.error("Main", "Exit save threw", ex);
        }
    }
}
