/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.completion.fields

interface BlueprintField {
    val name: String
    val descr: String
}

data class BlueprintBooleanField(
    override val name: String,
    override val descr: String
) : BlueprintField


data class BlueprintStringField(
    override val name: String,
    override val descr: String
) : BlueprintField

data class BlueprintNumberField(
    override val name: String,
    override val descr: String
) : BlueprintField

data class BlueprintStringListField(
    override val name: String,
    override val descr: String
) : BlueprintField

data class BlueprintLibraryField(
    override val name: String,
    override val descr: String
) : BlueprintField

data class BlueprintLibrariesListField(
    override val name: String,
    override val descr: String
) : BlueprintField

data class BlueprintReferencesListField(
    override val name: String,
    override val descr: String
) : BlueprintField

data class BlueprintReferenceField(
    override val name: String,
    override val descr: String
) : BlueprintField

data class BlueprintInterfaceField(
    override val name: String,
    override val descr: String
) : BlueprintField


data class BlueprintObjectField(
    override val name: String,
    override val descr: String,
    val fields: List<BlueprintField>
) : BlueprintField