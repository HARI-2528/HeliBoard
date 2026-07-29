// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

data class AiAction(
    val id: String,
    val name: String,
    val prompt: String,
    val iconName: String,
    val keyCode: Int,
    val needsInput: Boolean = false
) {
    companion object {
        private const val PREF_AI_ACTIONS = "pref_ai_actions"

        fun getDefaultActions(): List<AiAction> = emptyList()

        fun loadActions(context: Context): List<AiAction> {
            val prefs = context.prefs()
            val json = prefs.getString(PREF_AI_ACTIONS, null) ?: return emptyList()
            return try {
                val array = JSONArray(json)
                val actions = mutableListOf<AiAction>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    actions.add(
                        AiAction(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            prompt = obj.getString("prompt"),
                            iconName = obj.getString("iconName"),
                            keyCode = obj.getInt("keyCode"),
                            needsInput = obj.optBoolean("needsInput", false)
                        )
                    )
                }
                actions
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun saveActions(context: Context, actions: List<AiAction>) {
            val array = JSONArray()
            actions.forEach { action ->
                val obj = JSONObject().apply {
                    put("id", action.id)
                    put("name", action.name)
                    put("prompt", action.prompt)
                    put("iconName", action.iconName)
                    put("keyCode", action.keyCode)
                    put("needsInput", action.needsInput)
                }
                array.put(obj)
            }
            context.prefs().edit { putString(PREF_AI_ACTIONS, array.toString()) }
        }

        fun getNextCustomKeyCode(context: Context): Int {
            val actions = loadActions(context)
            val usedCodes = actions.map { it.keyCode }.toSet()
            for (code in KeyCode.AI_CUSTOM_1..KeyCode.AI_CUSTOM_10) {
                if (code !in usedCodes) return code
            }
            return KeyCode.AI_CUSTOM_10
        }

        fun findActionByKeyCode(context: Context, keyCode: Int): AiAction? {
            return loadActions(context).find { it.keyCode == keyCode }
        }
    }
}
