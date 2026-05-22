package com.david.astrodrill.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.david.astrodrill.GameManager;
import com.david.astrodrill.crafting.MachineRecipe;
import com.david.astrodrill.crafting.UpgradeDefinition;
import com.david.astrodrill.entity.Block;
import com.david.astrodrill.entity.LanderHub;
import com.david.astrodrill.entity.Player;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.observer.InventoryObserver;
import com.david.astrodrill.observer.VaultObserver;

import java.util.HashMap;
import java.util.Map;

public class Hud implements InventoryObserver, VaultObserver, Disposable {
    public Stage stage;
    private Table table;
    private Label.LabelStyle labelStyle, warningStyle;
    private BitmapFont font;
    private Label batteryLabel, batteryWarningLabel;
    private Player player;
    private LanderHub landerHub;

    private Map<Block.BlockType, Integer> currentInventory = new HashMap<>();
    private Map<ItemType, Integer> currentVault = new HashMap<>();
    private static final float LOW_BATTERY_THRESHOLD = 20f;

    private static final Block.BlockType[] INVENTORY_ORDER = {
        Block.BlockType.DIRT, Block.BlockType.STONE, Block.BlockType.COAL_ORE,
        Block.BlockType.COPPER_ORE, Block.BlockType.IRON_ORE, Block.BlockType.BASALT,
        Block.BlockType.GOLD_ORE, Block.BlockType.SILICON_ORE, Block.BlockType.URANIUM_ORE,
        Block.BlockType.OBSIDIAN
    };
    private static final ItemType[] VAULT_ORDER = {
        ItemType.RAW_IRON, ItemType.RAW_COPPER, ItemType.RAW_COAL,
        ItemType.RAW_GOLD, ItemType.RAW_SILICON, ItemType.RAW_URANIUM, ItemType.RAW_OBSIDIAN,
        ItemType.DIRT, ItemType.STONE,
        ItemType.IRON_INGOT, ItemType.COPPER_INGOT, ItemType.GOLD_INGOT, ItemType.SILICON_WAFER,
        ItemType.IRON_GEAR, ItemType.COPPER_WIRE, ItemType.CIRCUIT_BOARD,
        ItemType.HULL_PLATING, ItemType.ROCKET_FUEL
    };

    // Hub Panel
    private Table hubPanel;
    private boolean hubPanelVisible = false;
    private Texture panelBgTexture, buttonUpTexture, buttonOverTexture, buttonDownTexture;
    private Texture tabActiveTexture, tabInactiveTexture;
    private BitmapFont hubTitleFont, hubButtonFont, hubSmallFont;

    // Tab state
    private enum HubTab { UPGRADES, MANUFACTURING, LAUNCH }
    private HubTab activeTab = HubTab.UPGRADES;
    private Table hubContentArea;

    // Recipe info popup (one at a time)
    private Window activeInfoPopup;

    // Hotbar
    private Table hotbarWrapper, hotbarTable;
    private Label hotbarItemNameLabel;
    private final Label[] slotLabels = new Label[Player.HOTBAR_SLOTS];
    private final Image[] slotIcons = new Image[Player.HOTBAR_SLOTS];
    private final Label[] slotNumberLabels = new Label[Player.HOTBAR_SLOTS];
    private final Table[] slotCells = new Table[Player.HOTBAR_SLOTS];
    private Texture slotBgTexture, slotActiveBgTexture;
    private BitmapFont hotbarFont, hotbarSmallFont, hotbarNameFont;

    // Banking Popup
    private Label bankingPopupLabel;
    private float bankingPopupTimer = 0f;
    private BitmapFont bankingFont;

    // Menu Popup
    private Window menuPopup;
    private Texture sliderBgTexture, sliderKnobTexture;

    public Hud(SpriteBatch batch) {
        stage = new Stage(new ScreenViewport(), batch);
        table = new Table();
        table.top().left();
        table.setFillParent(true);

        font = Fonts.create(20, Color.WHITE, 1f);

        labelStyle = new Label.LabelStyle(font, Color.WHITE);
        warningStyle = new Label.LabelStyle(font, Color.RED);
        batteryLabel = new Label("Battery: 100%", labelStyle);
        batteryWarningLabel = new Label("", warningStyle);

        table.setTouchable(Touchable.childrenOnly);
        stage.addActor(table);
        rebuildTable();
        buildHubPanel();
        buildHotbarUI();
        buildBankingPopup();
        buildMenuButton();
    }

    private void rebuildTable() {
        table.clear();
        table.top().left();
        table.add(batteryLabel).pad(10).left().row();
        table.add(batteryWarningLabel).padLeft(10).padBottom(5).left().row();

        boolean hasInv = false;
        for (Integer c : currentInventory.values()) if (c > 0) { hasInv = true; break; }
        if (hasInv) {
            table.add(new Label("--- Inventory ---", labelStyle)).padLeft(10).padBottom(5).left().row();
            for (Block.BlockType t : INVENTORY_ORDER) {
                int c = currentInventory.getOrDefault(t, 0);
                if (c > 0) {
                    Table row = new Table();
                    TextureRegion tex = GameManager.getInstance().getBlockTex(t);
                    if (tex != null) {
                        Image img = new Image(tex);
                        img.setScaling(com.badlogic.gdx.utils.Scaling.fit);
                        row.add(img).size(20, 20).padRight(6);
                    }
                    row.add(new Label(formatBlockName(t) + ": " + c, labelStyle));
                    table.add(row).padLeft(10).padBottom(5).left().row();
                }
            }
        }
        boolean hasVault = false;
        for (Integer c : currentVault.values()) if (c > 0) { hasVault = true; break; }
        if (hasVault) {
            table.add(new Label("--- Vault ---", labelStyle)).padLeft(10).padBottom(5).left().row();
            for (ItemType t : VAULT_ORDER) {
                int c = currentVault.getOrDefault(t, 0);
                if (c > 0) {
                    Table row = new Table();
                    TextureRegion tex = GameManager.getInstance().getItemTex(t);
                    if (tex != null) {
                        Image img = new Image(tex);
                        img.setScaling(com.badlogic.gdx.utils.Scaling.fit);
                        row.add(img).size(20, 20).padRight(6);
                    }
                    row.add(new Label(formatItemName(t) + ": " + c, labelStyle));
                    table.add(row).padLeft(10).padBottom(5).left().row();
                }
            }
        }
    }

    // ── Hub Terminal Panel (Tabbed) ──────────────────────────────────────

    private void buildHubPanel() {
        panelBgTexture = createSolidTexture(0.08f, 0.08f, 0.15f, 0.85f);
        buttonUpTexture = createSolidTexture(0.15f, 0.25f, 0.45f, 0.9f);
        buttonOverTexture = createSolidTexture(0.2f, 0.35f, 0.6f, 0.95f);
        buttonDownTexture = createSolidTexture(0.1f, 0.18f, 0.35f, 1f);
        tabActiveTexture = createSolidTexture(0.2f, 0.4f, 0.7f, 1f);
        tabInactiveTexture = createSolidTexture(0.1f, 0.15f, 0.25f, 0.8f);

        hubTitleFont = Fonts.create(24, new Color(0.6f, 0.85f, 1f, 1f), 1.5f);
        hubButtonFont = Fonts.create(18, Color.WHITE, 1f);
        hubSmallFont = Fonts.create(14, Color.WHITE, 0.5f);

        hubPanel = new Table();
        hubPanel.setFillParent(true);
        hubPanel.top().right();
        hubPanel.setTouchable(Touchable.childrenOnly);
        hubPanel.setVisible(false);

        Table outerPanel = new Table();
        outerPanel.setBackground(new TextureRegionDrawable(new TextureRegion(panelBgTexture)));
        outerPanel.pad(12);

        // Title
        Label.LabelStyle titleStyle = new Label.LabelStyle(hubTitleFont, hubTitleFont.getColor());
        outerPanel.add(new Label("Lander Hub Terminal", titleStyle)).colspan(2).padBottom(8).center().row();

        // Recharging status
        Label.LabelStyle statusStyle = new Label.LabelStyle(hubButtonFont, new Color(0.3f, 1f, 0.5f, 1f));
        outerPanel.add(new Label("Recharging...", statusStyle)).colspan(2).padBottom(8).center().row();

        // Tab buttons
        Table tabRow = new Table();
        TextButton.TextButtonStyle tabStyle = new TextButton.TextButtonStyle();
        tabStyle.font = hubButtonFont; tabStyle.fontColor = Color.WHITE;
        tabStyle.up = new TextureRegionDrawable(new TextureRegion(tabInactiveTexture));
        tabStyle.over = new TextureRegionDrawable(new TextureRegion(buttonOverTexture));
        tabStyle.down = new TextureRegionDrawable(new TextureRegion(buttonDownTexture));

        TextButton upgradesTab = new TextButton("Upgrades", tabStyle);
        TextButton mfgTab = new TextButton("Crafting", tabStyle);
        TextButton launchTab = new TextButton("Launch", tabStyle);

        upgradesTab.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                GameManager.getInstance().playMenuSound();
                closeRecipeInfoPopup();
                activeTab = HubTab.UPGRADES;
                rebuildHubContent();
            }
        });
        mfgTab.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                GameManager.getInstance().playMenuSound();
                closeRecipeInfoPopup();
                activeTab = HubTab.MANUFACTURING;
                rebuildHubContent();
            }
        });
        launchTab.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                GameManager.getInstance().playMenuSound();
                closeRecipeInfoPopup();
                activeTab = HubTab.LAUNCH;
                rebuildHubContent();
            }
        });

        tabRow.add(upgradesTab).width(88).height(36).pad(2);
        tabRow.add(mfgTab).width(88).height(36).pad(2);
        tabRow.add(launchTab).width(88).height(36).pad(2);
        outerPanel.add(tabRow).colspan(2).padBottom(6).row();

        // Separator
        Label.LabelStyle sepStyle = new Label.LabelStyle(hubButtonFont, new Color(0.4f, 0.5f, 0.6f, 1f));
        outerPanel.add(new Label("━━━━━━━━━━━━━━━━━━━━", sepStyle)).colspan(2).center().padBottom(6).row();

        // Dynamic content area (scrollable so all recipes/upgrades stay reachable)
        hubContentArea = new Table();
        hubContentArea.top();
        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        ScrollPane scrollPane = new ScrollPane(hubContentArea, scrollStyle);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setForceScroll(false, true);
        outerPanel.add(scrollPane).colspan(2).width(280).height(360).row();

        // Hint
        Label.LabelStyle hintStyle = new Label.LabelStyle(hubSmallFont, new Color(0.5f, 0.5f, 0.5f, 1f));
        outerPanel.add(new Label("Walk away to close", hintStyle)).colspan(2).padTop(10).center().row();

        hubPanel.add(outerPanel).width(300).padTop(60).padRight(20);
        stage.addActor(hubPanel);

        rebuildHubContent();
    }

    /** Rebuilds the content area of the hub panel based on the active tab. */
    private void rebuildHubContent() {
        hubContentArea.clear();
        if (activeTab == HubTab.UPGRADES) {
            buildUpgradesContent();
        } else if (activeTab == HubTab.MANUFACTURING) {
            buildManufacturingContent();
        } else {
            buildLaunchContent();
        }
    }

    private void buildUpgradesContent() {
        GameManager gm = GameManager.getInstance();
        TextButton.TextButtonStyle tbs = makeButtonStyle();
        Label.LabelStyle nameStyle = new Label.LabelStyle(hubButtonFont, Color.WHITE);
        Label.LabelStyle costOkStyle = new Label.LabelStyle(hubSmallFont, new Color(0.3f, 1f, 0.4f, 1f));
        Label.LabelStyle costBadStyle = new Label.LabelStyle(hubSmallFont, new Color(1f, 0.3f, 0.3f, 1f));
        Label.LabelStyle maxStyle = new Label.LabelStyle(hubSmallFont, new Color(0.6f, 0.85f, 1f, 1f));

        for (UpgradeDefinition def : UpgradeDefinition.ALL) {
            final UpgradeDefinition d = def;
            int currentTier = getCurrentTier(def.key);
            Map<ItemType, Integer> cost = def.costToNext(currentTier);
            boolean atMax = (cost == null);

            hubContentArea.add(new Label(def.displayName + "  Lv " + currentTier + "/" + def.maxTier, nameStyle))
                .fillX().padTop(4).row();

            if (atMax) {
                hubContentArea.add(new Label("MAX", maxStyle)).fillX().padLeft(8).padBottom(6).row();
                continue;
            }

            boolean canAfford = true;
            for (Map.Entry<ItemType, Integer> entry : cost.entrySet()) {
                Table matRow = new Table();
                matRow.left();
                int have = gm.getItemCount(entry.getKey());
                int need = entry.getValue();
                if (have < need) canAfford = false;

                TextureRegion tex = gm.getItemTex(entry.getKey());
                if (tex != null) {
                    Image img = new Image(tex);
                    img.setScaling(com.badlogic.gdx.utils.Scaling.fit);
                    matRow.add(img).size(16, 16).padRight(4);
                }
                String matName = formatItemName(entry.getKey());
                Label costLabel = new Label(matName + " " + have + "/" + need, have >= need ? costOkStyle : costBadStyle);
                matRow.add(costLabel).padRight(12);
                hubContentArea.add(matRow).fillX().padLeft(8).padBottom(2).row();
            }
            hubContentArea.add(new Table()).padBottom(2).row();

            if (canAfford) {
                TextButton btn = new TextButton("Upgrade", tbs);
                btn.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent e, float x, float y) {
                        GameManager.getInstance().playMenuSound();
                        applyUpgrade(d);
                    }
                });
                hubContentArea.add(btn).fillX().height(32).padBottom(6).row();
            } else {
                hubContentArea.add(new Label("Insufficient", new Label.LabelStyle(hubSmallFont, new Color(0.5f, 0.5f, 0.5f, 1f))))
                    .padBottom(6).row();
            }
        }
    }

    private int getCurrentTier(String key) {
        if (player == null) return 1;
        switch (key) {
            case "DRILL":   return player.drillStrength;
            case "BATTERY": return player.batteryTier;
            case "JETPACK": return player.jetpackTier;
            case "WHEEL":   return player.wheelTier;
            case "HUB":     return (landerHub != null) ? landerHub.tier : 1;
            default:        return 1;
        }
    }

    private void applyUpgrade(UpgradeDefinition def) {
        int currentTier = getCurrentTier(def.key);
        Map<ItemType, Integer> cost = def.costToNext(currentTier);
        if (cost == null) return;

        GameManager gm = GameManager.getInstance();
        for (Map.Entry<ItemType, Integer> e : cost.entrySet()) {
            if (!gm.hasItems(e.getKey(), e.getValue())) {
                showBankingPopup("Not enough materials");
                return;
            }
        }
        for (Map.Entry<ItemType, Integer> e : cost.entrySet()) {
            gm.consumeItems(e.getKey(), e.getValue());
        }

        switch (def.key) {
            case "DRILL":
                player.drillStrength++;
                break;
            case "BATTERY":
                player.batteryTier++;
                player.applyBatteryUpgrade();
                break;
            case "JETPACK":
                player.jetpackTier++;
                player.applyJetpackUpgrade();
                break;
            case "WHEEL":
                player.wheelTier++;
                player.applyWheelUpgrade();
                break;
            case "HUB":
                if (landerHub != null) landerHub.upgradeTier();
                break;
        }
        showBankingPopup(def.displayName + " upgraded");
        rebuildHubContent();
    }

    private void buildManufacturingContent() {
        GameManager gm = GameManager.getInstance();
        int hubTier = (landerHub != null) ? landerHub.tier : 1;

        Label.LabelStyle nameStyle = new Label.LabelStyle(hubButtonFont, Color.WHITE);
        Label.LabelStyle costOkStyle = new Label.LabelStyle(hubSmallFont, new Color(0.3f, 1f, 0.4f, 1f));
        Label.LabelStyle costBadStyle = new Label.LabelStyle(hubSmallFont, new Color(1f, 0.3f, 0.3f, 1f));
        Label.LabelStyle lockedStyle = new Label.LabelStyle(hubSmallFont, new Color(0.5f, 0.5f, 0.5f, 1f));

        for (MachineRecipe recipe : MachineRecipe.getAllRecipes()) {
            final MachineRecipe rForInfo = recipe;

            // Tier gate
            if (recipe.requiredHubTier > hubTier) {
                Table lockedRow = new Table();
                lockedRow.center();
                lockedRow.add(new Label(recipe.displayName + " [Tier " + recipe.requiredHubTier + "]", lockedStyle))
                    .padRight(8);
                lockedRow.add(makeInfoButton(rForInfo)).width(28).height(24).padRight(2);
                hubContentArea.add(lockedRow).center().padBottom(6).row();
                continue;
            }

            // Machine name + info button
            Table nameRow = new Table();
            nameRow.center();
            TextureRegion outTex = gm.getItemTex(recipe.outputItemType);
            if (outTex != null) {
                Image outImg = new Image(outTex);
                outImg.setScaling(com.badlogic.gdx.utils.Scaling.fit);
                nameRow.add(outImg).size(40, 40).padRight(8).padTop(4);
            }
            nameRow.add(new Label(recipe.displayName, nameStyle)).padTop(4);
            nameRow.add(makeInfoButton(rForInfo)).width(28).height(24).padTop(4).padLeft(8);
            hubContentArea.add(nameRow).center().row();

            // Check affordability and build cost row
            boolean canAfford = true;
            for (Map.Entry<ItemType, Integer> entry : recipe.cost.entrySet()) {
                Table matRow = new Table();
                matRow.center();
                int have = gm.getItemCount(entry.getKey());
                int need = entry.getValue();
                if (have < need) canAfford = false;

                TextureRegion tex = gm.getItemTex(entry.getKey());
                if (tex != null) {
                    Image img = new Image(tex);
                    img.setScaling(com.badlogic.gdx.utils.Scaling.fit);
                    matRow.add(img).size(16, 16).padRight(4);
                }
                String matName = formatItemName(entry.getKey());
                Label costLabel = new Label(matName + " " + have + "/" + need, have >= need ? costOkStyle : costBadStyle);
                matRow.add(costLabel).padRight(12);
                hubContentArea.add(matRow).center().padBottom(2).row();
            }
            hubContentArea.add(new Table()).padBottom(2).row();

            // Craft button
            if (canAfford) {
                TextButton craftBtn = new TextButton("Craft", makeButtonStyle());
                final MachineRecipe r = recipe;
                craftBtn.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent e, float x, float y) {
                        GameManager.getInstance().playMenuSound();
                        craftMachine(r);
                    }
                });
                hubContentArea.add(craftBtn).width(100).height(32).padBottom(4).row();
            } else {
                Label insuffLabel = new Label("Insufficient", lockedStyle);
                hubContentArea.add(insuffLabel).padBottom(4).row();
            }
        }
    }

    private void craftMachine(MachineRecipe recipe) {
        GameManager gm = GameManager.getInstance();
        // Verify resources
        for (Map.Entry<ItemType, Integer> entry : recipe.cost.entrySet()) {
            if (!gm.hasItems(entry.getKey(), entry.getValue())) {
                showBankingPopup("Not enough resources!");
                return;
            }
        }
        // Deduct
        for (Map.Entry<ItemType, Integer> entry : recipe.cost.entrySet()) {
            gm.consumeItems(entry.getKey(), entry.getValue());
        }
        // Add to hotbar
        if (player != null) {
            player.addMachineToHotbar(recipe.outputItemType);
            showBankingPopup("Crafted: " + recipe.displayName);
        }
        rebuildHubContent();
    }

    private void buildLaunchContent() {
        GameManager gm = GameManager.getInstance();

        final int needFuel = 20, needPlate = 10, needChip = 5;
        int haveFuel  = gm.getItemCount(ItemType.ROCKET_FUEL);
        int havePlate = gm.getItemCount(ItemType.HULL_PLATING);
        int haveChip  = gm.getItemCount(ItemType.CIRCUIT_BOARD);

        Label.LabelStyle titleStyle = new Label.LabelStyle(hubButtonFont, new Color(1f, 0.85f, 0.4f, 1f));
        Label.LabelStyle okStyle    = new Label.LabelStyle(hubButtonFont, new Color(0.3f, 1f, 0.4f, 1f));
        Label.LabelStyle badStyle   = new Label.LabelStyle(hubButtonFont, new Color(1f, 0.4f, 0.4f, 1f));
        Label.LabelStyle hintStyle  = new Label.LabelStyle(hubSmallFont,  new Color(0.7f, 0.7f, 0.7f, 1f));

        hubContentArea.add(new Label("ROCKET LAUNCH", titleStyle)).fillX().padTop(4).padBottom(8).row();
        hubContentArea.add(new Label("Required Cargo:", hintStyle)).fillX().padBottom(4).row();

        addRequirementRow(ItemType.ROCKET_FUEL,   haveFuel,  needFuel,  okStyle, badStyle);
        addRequirementRow(ItemType.HULL_PLATING,  havePlate, needPlate, okStyle, badStyle);
        addRequirementRow(ItemType.CIRCUIT_BOARD, haveChip,  needChip,  okStyle, badStyle);

        boolean ready = haveFuel >= needFuel && havePlate >= needPlate && haveChip >= needChip;

        if (ready) {
            TextButton launchBtn = new TextButton("LAUNCH", makeButtonStyle());
            launchBtn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    GameManager.getInstance().playMenuSound();
                    GameManager.getInstance().changeScreen(GameManager.ScreenType.FLIGHT);
                }
            });
            hubContentArea.add(launchBtn).fillX().height(44).padTop(12).padBottom(6).row();
            hubContentArea.add(new Label("(or press L)", hintStyle)).fillX().padBottom(4).row();
        } else {
            hubContentArea.add(new Label("Build the production chain:", hintStyle))
                .fillX().padTop(10).padBottom(2).row();
            if (haveFuel  < needFuel)  hubContentArea.add(new Label("- Fuel Mixer (Hub Tier 3)",  hintStyle)).fillX().padLeft(8).row();
            if (havePlate < needPlate) hubContentArea.add(new Label("- Hull Press (Hub Tier 3)",  hintStyle)).fillX().padLeft(8).row();
            if (haveChip  < needChip)  hubContentArea.add(new Label("- Circuit Fab (Hub Tier 2)", hintStyle)).fillX().padLeft(8).row();
        }
    }

    private void addRequirementRow(ItemType type, int have, int need, Label.LabelStyle okStyle, Label.LabelStyle badStyle) {
        boolean met = have >= need;
        String mark = met ? "[OK]" : "[ ]";
        
        Table rowTable = new Table();
        rowTable.left();
        rowTable.add(new Label(mark + "  ", met ? okStyle : badStyle));
        
        TextureRegion tex = GameManager.getInstance().getItemTex(type);
        if (tex != null) {
            Image img = new Image(tex);
            img.setScaling(com.badlogic.gdx.utils.Scaling.fit);
            rowTable.add(img).size(16, 16).padRight(4);
        }
        
        String name = formatItemName(type);
        rowTable.add(new Label(name + ":  " + have + " / " + need, met ? okStyle : badStyle));
        
        hubContentArea.add(rowTable).fillX().padLeft(8).padBottom(4).row();
    }

    private TextButton makeInfoButton(final MachineRecipe recipe) {
        TextButton btn = new TextButton("?", makeButtonStyle());
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                GameManager.getInstance().playMenuSound();
                showRecipeInfoPopup(recipe);
            }
        });
        return btn;
    }

    private void showRecipeInfoPopup(MachineRecipe recipe) {
        closeRecipeInfoPopup();

        Window.WindowStyle ws = new Window.WindowStyle();
        ws.titleFont = hubButtonFont;
        ws.titleFontColor = new Color(1f, 0.85f, 0.4f, 1f);
        ws.background = new TextureRegionDrawable(new TextureRegion(panelBgTexture));

        final Window w = new Window(recipe.displayName, ws);
        w.setModal(false);
        w.setMovable(false);
        w.padTop(28).padLeft(10).padRight(10).padBottom(10);
        
        TextureRegion outTex = GameManager.getInstance().getItemTex(recipe.outputItemType);
        if (outTex != null) {
            Image outImg = new Image(outTex);
            outImg.setScaling(com.badlogic.gdx.utils.Scaling.fit);
            w.add(outImg).size(64, 64).padBottom(8).row();
        }

        Label.LabelStyle bodyStyle = new Label.LabelStyle(hubSmallFont, new Color(0.85f, 0.85f, 0.9f, 1f));
        Label body = new Label(recipe.description, bodyStyle);
        body.setWrap(true);
        w.add(body).width(280).pad(6).row();

        if (recipe.productionInputs != null || recipe.productionOutputs != null) {
            Table prodRow = new Table();
            prodRow.setBackground(new TextureRegionDrawable(new TextureRegion(createSolidTexture(0.05f, 0.05f, 0.1f, 0.6f))));
            prodRow.pad(6);
            
            // Inputs
            if (recipe.productionInputs != null) {
                for (Map.Entry<ItemType, Integer> entry : recipe.productionInputs.entrySet()) {
                    TextureRegion tex = GameManager.getInstance().getItemTex(entry.getKey());
                    if (tex != null) {
                        Image img = new Image(tex);
                        img.setScaling(com.badlogic.gdx.utils.Scaling.fit);
                        prodRow.add(img).size(20, 20).padRight(2);
                    }
                    prodRow.add(new Label(entry.getValue() + " " + formatItemName(entry.getKey()), bodyStyle)).padRight(6);
                }
            }
            
            // Arrow
            Label.LabelStyle arrowStyle = new Label.LabelStyle(hubButtonFont, new Color(0.5f, 0.8f, 1f, 1f));
            prodRow.add(new Label("-> ", arrowStyle)).padRight(6);
            
            // Outputs
            if (recipe.productionOutputs != null) {
                for (Map.Entry<ItemType, Integer> entry : recipe.productionOutputs.entrySet()) {
                    TextureRegion tex = GameManager.getInstance().getItemTex(entry.getKey());
                    if (tex != null) {
                        Image img = new Image(tex);
                        img.setScaling(com.badlogic.gdx.utils.Scaling.fit);
                        prodRow.add(img).size(24, 24).padRight(2);
                    }
                    Label.LabelStyle outStyle = new Label.LabelStyle(hubButtonFont, new Color(1f, 0.9f, 0.4f, 1f));
                    prodRow.add(new Label(entry.getValue() + " " + formatItemName(entry.getKey()), outStyle)).padRight(6);
                }
            }
            
            // Time
            if (recipe.productionTime > 0) {
                Label.LabelStyle timeStyle = new Label.LabelStyle(hubSmallFont, new Color(0.6f, 0.6f, 0.6f, 1f));
                prodRow.add(new Label("(" + (int)recipe.productionTime + "s)", timeStyle));
            }
            
            w.add(prodRow).fillX().padBottom(10).row();
        }

        Label.LabelStyle metaStyle = new Label.LabelStyle(hubSmallFont, new Color(0.6f, 0.85f, 1f, 1f));
        w.add(new Label("Construction Cost:", metaStyle)).left().padBottom(4).row();

        Label.LabelStyle costStyle = new Label.LabelStyle(hubSmallFont, new Color(1f, 1f, 1f, 1f));
        for (Map.Entry<ItemType, Integer> entry : recipe.cost.entrySet()) {
            Table matRow = new Table();
            matRow.left();
            TextureRegion tex = GameManager.getInstance().getItemTex(entry.getKey());
            if (tex != null) {
                Image img = new Image(tex);
                img.setScaling(com.badlogic.gdx.utils.Scaling.fit);
                matRow.add(img).size(16, 16).padRight(4);
            }
            String matName = formatItemName(entry.getKey());
            matRow.add(new Label(matName + " x" + entry.getValue(), costStyle));
            w.add(matRow).fillX().padLeft(8).padBottom(2).row();
        }

        w.add(new Label("Hub Tier " + recipe.requiredHubTier + " required", metaStyle)).padTop(8).padBottom(8).row();

        TextButton close = new TextButton("Close", makeButtonStyle());
        close.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                GameManager.getInstance().playMenuSound();
                closeRecipeInfoPopup();
            }
        });
        w.add(close).width(90).height(32).padTop(4);

        w.pack();
        w.setPosition((stage.getWidth() - w.getWidth()) / 2f,
                      (stage.getHeight() - w.getHeight()) / 2f);
        stage.addActor(w);
        activeInfoPopup = w;
    }

    private void closeRecipeInfoPopup() {
        if (activeInfoPopup != null) {
            activeInfoPopup.remove();
            activeInfoPopup = null;
        }
    }

    private TextButton.TextButtonStyle makeButtonStyle() {
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        s.font = hubButtonFont; s.fontColor = Color.WHITE;
        s.overFontColor = new Color(0.6f, 0.85f, 1f, 1f);
        s.downFontColor = new Color(0.3f, 0.5f, 0.8f, 1f);
        s.up = new TextureRegionDrawable(new TextureRegion(buttonUpTexture));
        s.over = new TextureRegionDrawable(new TextureRegion(buttonOverTexture));
        s.down = new TextureRegionDrawable(new TextureRegion(buttonDownTexture));
        return s;
    }

    // ── Hotbar UI ────────────────────────────────────────────────────────

    private void buildHotbarUI() {
        slotBgTexture = createSolidTexture(0.12f, 0.12f, 0.18f, 0.8f);
        slotActiveBgTexture = createSolidTexture(0.25f, 0.55f, 0.9f, 0.9f);

        hotbarFont = Fonts.create(16, Color.WHITE, 1f);
        hotbarSmallFont = Fonts.create(11, new Color(0.7f, 0.7f, 0.7f, 1f), 0.5f);
        hotbarNameFont = Fonts.create(16, new Color(0.85f, 0.9f, 1f, 1f), 1f);

        hotbarWrapper = new Table();
        hotbarWrapper.setFillParent(true);
        hotbarWrapper.setTouchable(Touchable.childrenOnly);
        hotbarWrapper.add().expandY();
        hotbarWrapper.row();

        Label.LabelStyle nStyle = new Label.LabelStyle(hotbarNameFont, hotbarNameFont.getColor());
        hotbarItemNameLabel = new Label("", nStyle);
        hotbarItemNameLabel.setAlignment(Align.center);
        hotbarItemNameLabel.setTouchable(Touchable.disabled);
        hotbarWrapper.add(hotbarItemNameLabel).padBottom(4).row();

        hotbarTable = new Table();
        hotbarTable.setBackground(new TextureRegionDrawable(new TextureRegion(createSolidTexture(0.05f, 0.05f, 0.1f, 0.7f))));
        hotbarTable.setTouchable(Touchable.childrenOnly);
        hotbarTable.pad(4);

        Label.LabelStyle slotStyle = new Label.LabelStyle(hotbarFont, Color.WHITE);
        Label.LabelStyle numStyle = new Label.LabelStyle(hotbarSmallFont, hotbarSmallFont.getColor());

        for (int i = 0; i < Player.HOTBAR_SLOTS; i++) {
            final int idx = i;
            Table cell = new Table();
            cell.setBackground(new TextureRegionDrawable(new TextureRegion(slotBgTexture)));

            Label num = new Label(String.valueOf(i + 1), numStyle);
            num.setTouchable(Touchable.disabled);
            slotNumberLabels[i] = num;

            Label item = new Label("", slotStyle);
            item.setAlignment(Align.center);
            item.setTouchable(Touchable.disabled);
            slotLabels[i] = item;

            Image icon = new Image();
            icon.setScaling(com.badlogic.gdx.utils.Scaling.fit);
            icon.setTouchable(Touchable.disabled);
            slotIcons[i] = icon;

            // Image draws first (background of the stack), label text overlays on top so
            // either the count (when textured) or the 2-letter fallback symbol stays readable.
            Stack content = new Stack();
            content.add(icon);
            content.add(item);
            content.setTouchable(Touchable.disabled);

            cell.add(num).top().left().expandX().padLeft(3).padTop(1).row();
            cell.add(content).expand().fill().pad(4).row();
            slotCells[i] = cell;

            cell.setTouchable(Touchable.enabled);
            cell.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
                @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    if (player != null) player.setActiveSlot(idx);
                    return true;
                }
            });
            hotbarTable.add(cell).size(54, 54).pad(1);
        }

        hotbarWrapper.add(hotbarTable);
        stage.addActor(hotbarWrapper);
    }

    // ── Banking Popup ────────────────────────────────────────────────────

    private void buildBankingPopup() {
        bankingFont = Fonts.create(18, new Color(0.3f, 1f, 0.5f, 1f), 1.5f);

        Table pw = new Table();
        pw.setFillParent(true);
        pw.center().center();
        pw.setTouchable(Touchable.disabled);

        Label.LabelStyle ps = new Label.LabelStyle(bankingFont, bankingFont.getColor());
        bankingPopupLabel = new Label("", ps);
        bankingPopupLabel.setAlignment(Align.center);
        bankingPopupLabel.setVisible(false);
        pw.add(bankingPopupLabel);
        stage.addActor(pw);
    }

    public void showBankingPopup(String text) {
        bankingPopupLabel.setText(text);
        bankingPopupLabel.setVisible(true);
        bankingPopupTimer = 2.5f;
    }

    private void buildMenuButton() {
        sliderBgTexture = createSolidTexture(0.3f, 0.3f, 0.3f, 1f);
        sliderKnobTexture = createSolidTexture(0.8f, 0.8f, 0.8f, 1f);

        Table wrapper = new Table();
        wrapper.setFillParent(true);
        wrapper.top().right();
        wrapper.setTouchable(Touchable.childrenOnly);

        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        s.font = hubButtonFont; s.fontColor = Color.WHITE;
        s.overFontColor = new Color(0.6f, 0.85f, 1f, 1f);
        s.up = new TextureRegionDrawable(new TextureRegion(buttonUpTexture));
        s.over = new TextureRegionDrawable(new TextureRegion(buttonOverTexture));
        s.down = new TextureRegionDrawable(new TextureRegion(buttonDownTexture));

        TextButton menuButton = new TextButton("MENU", s);
        menuButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                GameManager.getInstance().playMenuSound();
                showMenuPopup();
            }
        });

        wrapper.add(menuButton).width(100).height(32).padTop(8).padRight(8);
        stage.addActor(wrapper);
    }

    private void showMenuPopup() {
        if (menuPopup != null) {
            menuPopup.remove();
            menuPopup = null;
            return;
        }

        Window.WindowStyle ws = new Window.WindowStyle();
        ws.titleFont = hubButtonFont;
        ws.titleFontColor = new Color(1f, 0.85f, 0.4f, 1f);
        ws.background = new TextureRegionDrawable(new TextureRegion(panelBgTexture));

        final Window w = new Window("Menu", ws);
        w.setModal(true);
        w.setMovable(false);
        w.padTop(40).padLeft(20).padRight(20).padBottom(20);
        
        TextButton.TextButtonStyle s = makeButtonStyle();
        
        TextButton resumeBtn = new TextButton("Resume", s);
        resumeBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                GameManager.getInstance().playMenuSound();
                if (menuPopup != null) menuPopup.remove();
                menuPopup = null;
            }
        });
        
        TextButton saveAndExitBtn = new TextButton("Save & Exit", s);
        saveAndExitBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                GameManager.getInstance().playMenuSound();
                com.badlogic.gdx.Screen screen = GameManager.getInstance().getGame().getScreen();
                if (screen instanceof com.david.astrodrill.screen.PlayScreen) {
                    ((com.david.astrodrill.screen.PlayScreen) screen).requestSave(true);
                }
                GameManager.getInstance().changeScreen(GameManager.ScreenType.MAIN_MENU);
            }
        });

        // Volume Slider
        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        TextureRegionDrawable bg = new TextureRegionDrawable(new TextureRegion(sliderBgTexture));
        bg.setMinHeight(10);
        sliderStyle.background = bg;

        TextureRegionDrawable knob = new TextureRegionDrawable(new TextureRegion(sliderKnobTexture));
        knob.setMinWidth(24);
        knob.setMinHeight(36);
        sliderStyle.knob = knob;

        final Slider volumeSlider = new Slider(0f, 1f, 0.05f, false, sliderStyle);
        volumeSlider.setValue(GameManager.getInstance().getSfxVolume());
        volumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameManager.getInstance().setSfxVolume(volumeSlider.getValue());
            }
        });

        Label.LabelStyle lblStyle = new Label.LabelStyle(hubSmallFont, Color.WHITE);
        Label volumeLabel = new Label("SFX Volume", lblStyle);

        w.add(volumeLabel).padBottom(4).row();
        w.add(volumeSlider).width(240).padBottom(20).row();
        
        w.add(resumeBtn).width(240).height(48).pad(8).row();
        w.add(saveAndExitBtn).width(240).height(48).pad(8).row();

        w.pack();
        w.setPosition((stage.getWidth() - w.getWidth()) / 2f,
                      (stage.getHeight() - w.getHeight()) / 2f);
        stage.addActor(w);
        menuPopup = w;
    }

    public void updateHotbar(float delta) {
        if (player == null) return;

        if (bankingPopupTimer > 0) {
            bankingPopupTimer -= delta;
            if (bankingPopupTimer <= 0) bankingPopupLabel.setVisible(false);
        }

        player.rebuildHotbar();

        GameManager gm = GameManager.getInstance();
        int active = player.activeSlot;
        for (int i = 0; i < Player.HOTBAR_SLOTS; i++) {
            ItemType it = player.hotbar[i];
            TextureRegion region = it == null ? null : gm.getItemTex(it);

            if (region != null) {
                slotIcons[i].setDrawable(new TextureRegionDrawable(region));
                int count = (it != ItemType.DECONSTRUCT_TOOL) ? player.getSlotItemCount(it) : 0;
                slotLabels[i].setText(count > 0 ? String.valueOf(count) : "");
                slotLabels[i].setAlignment(Align.bottomRight);
            } else {
                slotIcons[i].setDrawable(null);
                String symbol = getSlotSymbol(it);
                if (it != null && it != ItemType.DECONSTRUCT_TOOL) {
                    int count = player.getSlotItemCount(it);
                    if (count > 0) symbol += "\n" + count;
                }
                slotLabels[i].setText(symbol);
                slotLabels[i].setAlignment(Align.center);
            }

            slotCells[i].setBackground(new TextureRegionDrawable(new TextureRegion(
                i == active ? slotActiveBgTexture : slotBgTexture)));
        }

        ItemType activeItem = player.getActiveHotbarItem();
        hotbarItemNameLabel.setText(activeItem != null ? formatItemName(activeItem) : "Empty");

        // Manufacturing tab refresh is now handled by the dirty flag (see onVaultUpdated)
    }

    private String getSlotSymbol(ItemType item) {
        if (item == null) return "";
        switch (item) {
            case DECONSTRUCT_TOOL: return "X";
            case DIRT:             return "DT";
            case STONE:            return "ST";
            case AUTO_MINER:       return "AM";
            case COAL_GENERATOR:   return "CG";
            case IRON_SMELTER:     return "IS";
            case COPPER_SMELTER:   return "CS";
            case GOLD_SMELTER:     return "GS";
            case GEAR_ASSEMBLER:   return "GA";
            case WIRE_ASSEMBLER:   return "WA";
            case REFINERY:         return "RF";
            case CIRCUIT_FAB:      return "CF";
            case FUEL_MIXER:       return "FM";
            case HULL_PRESS:       return "HP";
            case GOLD_INGOT:       return "GI";
            case SILICON_WAFER:    return "SW";
            case CIRCUIT_BOARD:    return "CB";
            case HULL_PLATING:     return "HL";
            case ROCKET_FUEL:      return "FL";
            default:
                String n = item.name();
                return n.length() > 2 ? n.substring(0, 2) : n;
        }
    }

    // ── Hub Panel Visibility ─────────────────────────────────────────────

    public void setHubPanelVisible(boolean visible) {
        if (visible == hubPanelVisible) return;
        hubPanelVisible = visible;
        hubPanel.setVisible(visible);
        if (visible) rebuildHubContent();
        else closeRecipeInfoPopup();
    }

    public boolean isHubPanelVisible() { return hubPanelVisible; }

    // ── Setters ──────────────────────────────────────────────────────────

    public void setPlayer(Player player) { this.player = player; }
    public void setLanderHub(LanderHub hub) { this.landerHub = hub; }

    // ── Utility ──────────────────────────────────────────────────────────

    private Texture createSolidTexture(float r, float g, float b, float a) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(r, g, b, a); pm.fill();
        Texture tex = new Texture(pm); pm.dispose();
        return tex;
    }

    private String formatBlockName(Block.BlockType type) {
        String name = type.name();
        if (name.endsWith("_ORE")) {
            name = "RAW_" + name.substring(0, name.length() - 4);
        }
        return formatNameString(name);
    }

    private String formatItemName(ItemType type) {
        if (type == ItemType.RAW_COAL) return "Coal";
        return formatNameString(type.name());
    }

    private String formatNameString(String input) {
        String[] parts = input.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(p.substring(0, 1).toUpperCase()).append(p.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }

    public void updateBattery() {
        if (player == null) return;
        float pct = (player.currentBattery / player.maxBattery) * 100f;
        batteryLabel.setText("Battery: " + Math.round(pct) + "%");
        if (pct <= LOW_BATTERY_THRESHOLD) {
            batteryLabel.getStyle().fontColor = Color.RED;
            batteryWarningLabel.setText("!! LOW BATTERY !!");
        } else if (pct <= 50f) {
            batteryLabel.getStyle().fontColor = Color.YELLOW;
            batteryWarningLabel.setText("");
        } else {
            batteryLabel.getStyle().fontColor = Color.GREEN;
            batteryWarningLabel.setText("");
        }
    }

    @Override
    public void onInventoryUpdated(Map<Block.BlockType, Integer> inventory) {
        currentInventory.clear(); currentInventory.putAll(inventory); rebuildTable();
    }

    @Override
    public void onVaultUpdated(Map<ItemType, Integer> vault) {
        currentVault.clear(); currentVault.putAll(vault); rebuildTable();
        // Refresh manufacturing / launch tab costs when vault changes
        if (hubPanelVisible && (activeTab == HubTab.MANUFACTURING || activeTab == HubTab.LAUNCH)) {
            rebuildHubContent();
        }
    }

    @Override
    public void dispose() {
        if (font != null) font.dispose();
        if (hubTitleFont != null) hubTitleFont.dispose();
        if (hubButtonFont != null) hubButtonFont.dispose();
        if (hubSmallFont != null) hubSmallFont.dispose();
        if (hotbarFont != null) hotbarFont.dispose();
        if (hotbarSmallFont != null) hotbarSmallFont.dispose();
        if (hotbarNameFont != null) hotbarNameFont.dispose();
        if (bankingFont != null) bankingFont.dispose();
        if (panelBgTexture != null) panelBgTexture.dispose();
        if (buttonUpTexture != null) buttonUpTexture.dispose();
        if (buttonOverTexture != null) buttonOverTexture.dispose();
        if (buttonDownTexture != null) buttonDownTexture.dispose();
        if (tabActiveTexture != null) tabActiveTexture.dispose();
        if (tabInactiveTexture != null) tabInactiveTexture.dispose();
        if (slotBgTexture != null) slotBgTexture.dispose();
        if (slotActiveBgTexture != null) slotActiveBgTexture.dispose();
        if (sliderBgTexture != null) sliderBgTexture.dispose();
        if (sliderKnobTexture != null) sliderKnobTexture.dispose();
        stage.dispose();
    }
}
