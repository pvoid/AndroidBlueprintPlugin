/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.pvoid.androidbp.idea.project

import com.android.tools.idea.projectsystem.*
import com.github.pvoid.androidbp.idea.project.rendering.BlueprintToken
import com.intellij.openapi.project.Project

class BlueprintApplicationProjectContext(
    private val project: Project
) : ApplicationProjectContext {
    override val applicationId: String
        get() = project.getProjectSystem().getKnownApplicationIds().first()
}

class BlueprintApplicationProjectContextProvider : ApplicationProjectContextProvider<BlueprintProjectSystem>, BlueprintToken {
    override fun computeApplicationProjectContext(
        projectSystem: BlueprintProjectSystem,
        client: ApplicationProjectContextProvider.RunningApplicationIdentity
    ): ApplicationProjectContext {
        return BlueprintApplicationProjectContext(projectSystem.project)
    }
}
