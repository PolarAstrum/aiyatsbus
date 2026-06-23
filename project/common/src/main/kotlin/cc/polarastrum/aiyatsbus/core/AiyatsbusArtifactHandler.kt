package cc.polarastrum.aiyatsbus.core

import org.bukkit.inventory.EquipmentSlot
import taboolib.module.configuration.Configuration

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.core.AiyatsbusArtifactHandler
 *
 * @author mical
 * @since 2026/3/17 00:31
 */
interface AiyatsbusArtifactHandler {

    /**
     * 获取粒子配置
     *
     * @return 粒子处理相关的配置对象
     */
    fun getSettings(): Settings

    /**
     * 粒子配置接口
     *
     * 定义相关的可配置项。
     */
    interface Settings {

        /** 配置文件 */
        var conf: Configuration

        /** 普通粒子附魔不同体型 RNA 或环形粒子的半径 */
        var range: List<Double>

        /** 普通粒子附魔不同体型 RNA 粒子的高度 */
        var height: List<Double>

        /** 所有粒子附魔在破坏哪些方块时触发粒子 */
        val blocks: List<String>

        /** 专精粒子附魔每个装备槽的粒子形态，范围和高度 */
        val customParticleData: Map<EquipmentSlot, Pair<Double, Double>>

        /** 专精粒子附魔每个装备槽的粒子形态类型，包括 RNA、Simple、Circle */
        val customParticleType: Map<EquipmentSlot, String>
    }
}