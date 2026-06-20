package com.zootdungeon;

import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.scenes.TitleScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.zootdungeon.utils.FileHandle;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.PlatformSupport;

// Application entry point. Bootstraps assets, settings, audio, and launches the first scene.
// Also provides scene-switching utilities and window save/restore across transitions.
// The only root-package class that does not participate in save/load.
// Normally you shouldn't modify this file. You may check Dungeon.java
public class ColaDungeon extends Game {

    // Save format version; bump when breaking save compatibility.
    public static final int v_latest = 1;

    public ColaDungeon(PlatformSupport platform) {
        super(sceneClass == null ? TitleScene.class : sceneClass, platform);
    }

    // Called once at launch: loads asset indexes, key bindings, audio prefs, and pre-caches sounds.
    @Override
    public void create() {
        super.create();

        for (Assets.ResourceIndex index : FileHandle.scanResourceIndex()) {
            Assets.addIndex(index);
        }

        updateSystemUI();
        CDKeyBinding.loadBindings();

        Music.INSTANCE.enable(CDSettings.music());
        Music.INSTANCE.volume(CDSettings.musicVol() * CDSettings.musicVol() / 100f);
        Sample.INSTANCE.enable(CDSettings.soundFx());
        Sample.INSTANCE.volume(CDSettings.SFXVol() * CDSettings.SFXVol() / 100f);

        Sample.INSTANCE.load(Assets.getSoundsAllResolved());
    }

    // Exit on desktop/Android; return to title on iOS (Apple HIG — apps should not quit).
    @Override
    public void finish() {
        if (!DeviceCompat.isiOS()) {
            super.finish();
        } else {
            switchScene(TitleScene.class);
        }
    }

    // Switch scenes without a fade transition.
    public static void switchNoFade(Class<? extends PixelScene> c) {
        switchNoFade(c, null);
    }

    // Switch without fade, with an optional post-switch callback.
    public static void switchNoFade(Class<? extends PixelScene> c, SceneChangeCallback callback) {
        PixelScene.noFade = true;
        switchScene(c, callback);
    }

    // Save open windows, then reload the current scene in-place. Used when a setting change
    // requires rebuilding the UI without a full scene switch.
    public static void seamlessResetScene(SceneChangeCallback callback) {
        if (scene() instanceof PixelScene _scene) {
            _scene.saveWindows();
            switchNoFade((Class<? extends PixelScene>) sceneClass, callback);
        } else {
            resetScene();
        }
    }

    public static void seamlessResetScene() {
        seamlessResetScene(null);
    }

    /** After a scene switch, restore any windows that were saved from the previous scene. */
    @Override
    protected void switchScene() {
        super.switchScene();
        if (scene instanceof PixelScene _scene) {
            _scene.restoreWindows();
        }
    }

    /**
     * Called when the window is resized. Saves open windows on the old size,
     * then delegates to the platform for display size recalculation.
     */
    @Override
    public void resize(int width, int height) {
        if (width == 0 || height == 0) {
            return;
        }

        if (scene instanceof PixelScene _scene
                && (height != Game.height || width != Game.width)) {
            PixelScene.noFade = true;
            _scene.saveWindows();
        }

        super.resize(width, height);
        updateDisplaySize();
    }

    /** Called on application exit. Shuts down the background actor thread. */
    @Override
    public void destroy() {
        super.destroy();
        GameScene.endActorThread();
    }

    public void updateDisplaySize() {
        platform.updateDisplaySize();
    }

    public static void updateSystemUI() {
        platform.updateSystemUI();
    }
}
