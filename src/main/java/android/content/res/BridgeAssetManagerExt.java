/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package android.content.res;

import com.android.ide.common.rendering.api.AssetRepository;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

public class BridgeAssetManagerExt extends BridgeAssetManager {

    private static String sSdkPath = null;

    public static void init(@NotNull String path) {
        AssetRepository repo = null;
        try {
            Class cls = Class.forName("android.content.res.AssetManager");
            Field f = cls.getDeclaredField("sSystem");
            f.setAccessible(true);
            Object value = f.get(null);

            if (value instanceof BridgeAssetManagerExt) {
                return;
            }

            if (value instanceof BridgeAssetManager) {
                try {
                    repo = ((BridgeAssetManager) value).getAssetRepository();
                } catch (IllegalStateException e) {
                    // Thrown when Asset repository is not set
                    repo = null;
                }
            }

            BridgeAssetManagerExt manager = new BridgeAssetManagerExt();
            if (repo != null) {
                manager.setAssetRepository(repo);
            }

            f.set(null, manager);
            sSdkPath = path;
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream openNonAsset(int cookie, String fileName, int accessMode) throws IOException {
        boolean isFont = fileName.endsWith(".otf") || fileName.endsWith(".ttf");
        if (sSdkPath != null && isFont && FileUtil.isAncestor(sSdkPath, fileName, false)) {
            return new FileInputStream(fileName);
        }
        return super.openNonAsset(cookie, fileName, accessMode);
    }
}
