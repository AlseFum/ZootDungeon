package com.zootdungeon.items.cheat

import com.zootdungeon.Assets
import com.zootdungeon.Dungeon
import com.zootdungeon.actors.buffs.Buff
import com.zootdungeon.actors.buffs.Invisibility
import com.zootdungeon.actors.hero.Hero
import com.zootdungeon.items.scrolls.ScrollOfTeleportation
import com.zootdungeon.items.stones.Runestone
import com.zootdungeon.scenes.GameScene
import com.zootdungeon.scenes.InterlevelScene
import com.zootdungeon.sprites.ItemSpriteSheet
import com.zootdungeon.messages.Messages
import com.zootdungeon.utils.GLog
import com.zootdungeon.windows.WndOptions
import com.zootdungeon.levels.LevelGraph
import com.zootdungeon.levels.LevelGraph.LevelNode
import com.watabou.noosa.Game
import com.watabou.noosa.audio.Sample

class StoneOfLevelSelect : Runestone() {

    init {
        image = ItemSpriteSheet.STONE_CLAIRVOYANCE
        defaultAction = AC_APPLY
    }

    override fun execute(hero: Hero, action: String) {
        super.execute(hero, action)

        if (AC_APPLY == action) {
            openSelector(hero)
        }
    }

    override fun activate(cell: Int) {
        if (Dungeon.hero != null) {
            openSelector(Dungeon.hero!!)
        }
    }

    private fun openSelector(hero: Hero) {
        if (!Dungeon.interfloorTeleportAllowed()) {
            GLog.w(Messages.get(ScrollOfTeleportation::class.java, "no_tele"))
            return
        }

        val nodes = LevelGraph.generatedNodes()
        val options = Array(nodes.size + 2) { i ->
            when {
                i < nodes.size -> {
                    val n = nodes[i]
                    "${n.id} (depth ${n.depth}, branch ${n.branch})"
                }
                i == nodes.size -> "新建特殊楼层（继承父节点深度）"
                else -> "新建独立楼层（完全自定义）"
            }
        }

        val wnd = object : WndOptions("楼层选择", "选择一个已生成的楼层，前往它的入口。", *options) {
            override fun onSelect(index: Int) {
                when {
                    index < 0 -> return
                    index == nodes.size -> {
                        val special = LevelGraph.createSpecialNode(
                            Dungeon.currentLevelId,
                            Dungeon.depth,
                            Dungeon.branch
                        )
                        teleportTo(hero, special)
                    }
                    index == nodes.size + 1 -> {
                        GLog.i("standalone node创建中... 请在控制台设置 node.levelClass")
                        val standalone = LevelGraph.createStandaloneNode(99)
                        teleportTo(hero, standalone)
                    }
                    else -> {
                        val target = nodes[index]
                        teleportTo(hero, target)
                    }
                }
            }
        }
        GameScene.show(wnd)
    }

    private fun teleportTo(hero: Hero, node: LevelNode?) {
        if (node == null) return

        Buff.affect(hero, Invisibility::class.java, 2f)
        InterlevelScene.mode = InterlevelScene.Mode.RETURN
        InterlevelScene.returnDepth = node.depth
        InterlevelScene.returnBranch = node.branch
        InterlevelScene.returnPos = -1

        Sample.INSTANCE.play(Assets.Sounds.TELEPORT)

        detach(hero.belongings.backpack)

        Game.switchScene(InterlevelScene::class.java)
    }

    override fun name(): String = "楼层选择魔石"

    override fun desc(): String = buildString {
        append("允许你在已生成的楼层之间自由旅行，直接前往目标楼层的入口。\n")
        append("这是一个调试/管理用道具，正常游戏过程中不应获得。")
    }

    override fun isIdentified(): Boolean = true

    companion object {
        const val AC_APPLY = "APPLY"
    }
}
