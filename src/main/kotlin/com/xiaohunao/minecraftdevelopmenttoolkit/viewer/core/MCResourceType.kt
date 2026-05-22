package com.xiaohunao.minecraftdevelopmenttoolkit.viewer.core

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.vfs.VirtualFile

/**
 * Minecraft 数据包中所有标准 JSON 资源类型的枚举。
 *
 * 类型由数据包文件夹路径决定，而非 JSON 内容结构。
 * 例如 `data/<namespace>/advancement/<path>.json` 即为 ADVANCEMENT。
 */
enum class MCResourceType(
    val folderName: String,
    val displayName: String,
    val subFolderPrefix: String? = null,
    /** 旧版 MC（1.20.x 及之前）使用的复数形式文件夹名 */
    private val aliases: List<String> = emptyList()
) {
    ADVANCEMENT("advancement", "进度", aliases = listOf("advancements")),
    RECIPE("recipe", "配方", aliases = listOf("recipes")),
    LOOT_TABLE("loot_table", "战利品表", aliases = listOf("loot_tables")),
    PREDICATE("predicate", "谓词", aliases = listOf("predicates")),
    ITEM_MODIFIER("item_modifier", "物品修改器", aliases = listOf("item_modifiers")),
    TAG("tags", "标签", "tags/"),
    WORLDGEN("worldgen", "世界生成", "worldgen/"),
    DIMENSION("dimension", "维度"),
    DIMENSION_TYPE("dimension_type", "维度类型"),
    ENCHANTMENT("enchantment", "附魔"),
    ENCHANTMENT_PROVIDER("enchantment_provider", "附魔提供器"),
    DAMAGE_TYPE("damage_type", "伤害类型"),
    BANNER_PATTERN("banner_pattern", "旗帜图案"),
    CHAT_TYPE("chat_type", "聊天类型"),
    INSTRUMENT("instrument", "乐器"),
    JUKEBOX_SONG("jukebox_song", "唱片"),
    PAINTING_VARIANT("painting_variant", "画变种"),
    TRIM_MATERIAL("trim_material", "锻造材料"),
    TRIM_PATTERN("trim_pattern", "锻造图案"),
    WOLF_VARIANT("wolf_variant", "狼变种"),
    CAT_VARIANT("cat_variant", "猫变种"),
    FROG_VARIANT("frog_variant", "青蛙变种"),
    COW_VARIANT("cow_variant", "牛变种"),
    PIG_VARIANT("pig_variant", "猪变种"),
    CHICKEN_VARIANT("chicken_variant", "鸡变种"),
    TRIAL_SPAWNER("trial_spawner", "试炼刷怪笼"),
    TRADE_SET("trade_set", "交易集合"),
    VILLAGER_TRADE("villager_trade", "村民交易"),
    WOLF_SOUND_VARIANT("wolf_sound_variant", "狼音效变种"),
    ZOMBIE_NAUTILUS_VARIANT("zombie_nautilus_variant", "僵尸鹦鹉螺变种"),
    DIALOG("dialog", "对话"),
    WORLD_CLOCK("world_clock", "世界时钟"),
    TEST_ENVIRONMENT("test_environment", "测试环境"),
    TEST_INSTANCE("test_instance", "测试实例"),
    TIMELINE("timeline", "时间线"),

    UNKNOWN("", "未知资源");

    companion object {
        /**
         * 根据文件在数据包中的路径检测资源类型。
         *
         * 路径格式应为 `data/<namespace>/<type_folder>/<subpath>.json`。
         * 如果文件不在数据包目录结构中，返回 null。
         */
        fun detect(file: VirtualFile): MCResourceType? {
            val path = file.path.replace('\\', '/')
            val dataIndex = path.indexOf("/data/")
            if (dataIndex < 0) return null

            val afterData = path.substring(dataIndex + "/data/".length)
            val segments = afterData.split("/")
            // segments[0] = namespace, segments[1] = type folder
            if (segments.size < 3) return null

            val typeFolder = segments[1]

            // 先检查带子文件夹前缀的类型（tags、worldgen）
            for (type in entries) {
                if (type == UNKNOWN) continue
                if (type.subFolderPrefix != null && typeFolder == type.subFolderPrefix.removeSuffix("/")) {
                    return type
                }
            }

            // 再检查普通类型（主名称 + 别名）
            return entries.firstOrNull { type ->
                type != UNKNOWN && (type.folderName == typeFolder || type.aliases.contains(typeFolder))
            }
        }

        /**
         * 尝试将 JSON 字符串解析为 JsonObject。
         * 返回 null 表示 JSON 无效。
         */
        fun tryParseJson(text: String): JsonObject? {
            return try {
                JsonParser.parseString(text)?.asJsonObject
            } catch (_: Exception) {
                null
            }
        }
    }
}
