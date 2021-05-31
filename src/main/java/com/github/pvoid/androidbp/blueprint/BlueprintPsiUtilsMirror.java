/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint;

import com.github.pvoid.androidbp.blueprint.completion.BlueprintField;
import com.github.pvoid.androidbp.blueprint.model.BlueprintPsiUtils;
import com.github.pvoid.androidbp.blueprint.psi.BlueprintBlueprint;
import com.github.pvoid.androidbp.blueprint.psi.BlueprintFieldName;
import com.github.pvoid.androidbp.blueprint.psi.BlueprintPair;
import com.github.pvoid.androidbp.blueprint.psi.BlueprintStringExpr;

public class BlueprintPsiUtilsMirror {
    public static String getValue(BlueprintStringExpr element) {
        return BlueprintPsiUtils.INSTANCE.getValue(element);
    }

    public static String getBlueprintName(BlueprintBlueprint element) { return BlueprintPsiUtils.INSTANCE.getBlueprintName(element); }

    public static boolean isBlueprintField(BlueprintFieldName element) { return BlueprintPsiUtils.INSTANCE.isBlueprintField(element); }

    public static BlueprintBlueprint getFieldBlueprint(BlueprintFieldName element) { return BlueprintPsiUtils.INSTANCE.getFieldBlueprint(element); }

    public static BlueprintField getFieldDef(BlueprintPair element) { return BlueprintPsiUtils.INSTANCE.getFieldDef(element); }
}
