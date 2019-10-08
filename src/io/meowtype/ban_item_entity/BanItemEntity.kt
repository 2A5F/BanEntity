package io.meowtype.ban_item_entity

import de.tr7zw.nbtapi.*
import org.bukkit.*
import org.bukkit.command.*
import org.bukkit.entity.*
import org.bukkit.event.*
import org.bukkit.event.entity.*
import org.bukkit.plugin.java.JavaPlugin

class BanItemEntity : JavaPlugin() {

    override fun onEnable() {
        saveDefaultConfig()
        readConfig()
        getCommand("ban-item-entity").executor = this
        server.pluginManager.registerEvents(listener, this)
        logger.info("Enabled")
    }

    override fun onDisable() {
        logger.info("Disabled")
    }

    private fun senderOrLogger(sender: CommandSender?, msg: String, warn: Boolean = false) {
        if(sender != null) sender.sendMessage(msg) else if(warn) logger.warning(msg) else logger.info(msg)
    }

    private val idReg = Regex("[^:]+:[^:]+")
    private fun readConfig(sender: CommandSender? = null) {
        senderOrLogger(sender, "Loading ban list")
        reloadConfig()
        banList.clear()
        var count = 0
        if(config.isConfigurationSection("ban_list")) {
            val list = config.getConfigurationSection("ban_list")

            for (key in list.getKeys(false)) {
                if(!(idReg matches key)) {
                    senderOrLogger(sender, "[$key is not a id]", true)
                    continue
                }
                if(list.isBoolean(key)) {
                    banList[key] = BanOption(list.getBoolean(key))
                    count++
                    continue
                }
                if(list.isConfigurationSection(key)) {
                    val map = list.getConfigurationSection(key)
                    val pickup = if(map.isBoolean("pickup")) map.getBoolean("pickup") else false
                    val remove = if(map.isBoolean("remove")) map.getBoolean("remove") else false
                    val create = if(map.isBoolean("create")) map.getBoolean("create") else false
                    banList[key] = BanOption(pickup, remove, create)
                    count++
                    continue
                } else {
                    senderOrLogger(sender, "'$key': ... is not a {} or bool", true)
                }
            }
        }
        senderOrLogger(sender, "Load $count ban item")
    }

    private fun saveBanList() {
        val section = config.createSection("ban_list")
        for ((id, opt) in banList) {
            if(opt.isAll) {
                section.set(id, true)
                continue
            }
            if(opt.isNone) {
                section.set(id, false)
                continue
            }
            val ban = section.createSection(id)
            if(opt.pickup) ban.set("pickup", true)
            if(opt.remove) ban.set("remove", true)
            if(opt.create) ban.set("create", true)
        }
        saveConfig()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        fun showHelp() {
            sender.sendMessage("")
            sender.sendMessage("/ban-item-entity help")
            sender.sendMessage("/ban-item-entity reload")
            sender.sendMessage("/ban-item-entity list")
            sender.sendMessage("/ban-item-entity <namespace:id>")
            sender.sendMessage("/ban-item-entity <namespace:id> all")
            sender.sendMessage("/ban-item-entity <namespace:id> [pickup | remove | create ...]")
            sender.sendMessage("/ban-item-entity <namespace:id> !")
            sender.sendMessage("/ban-item-entity <namespace:id> !all")
            sender.sendMessage("/ban-item-entity <namespace:id> [!pickup | !remove | !create ...]")
            sender.sendMessage("")
            sender.sendMessage("pickup:    let player cant pick up this item")
            sender.sendMessage("remove:    remove or kill the item entity")
            sender.sendMessage("create:    let this item entity cant be create or summon or spawn")
            sender.sendMessage("")
        }
        if(args.isNotEmpty()) {
            when(args[0]) {
                "help" -> {
                    showHelp()
                    return true
                }
                "list" -> {
                    sender.sendMessage("${banList.size} ban items")
                    for ((id, opt) in banList) {
                        sender.sendMessage("[$id] ${if(opt.isAll) "all" else opt.str}")
                    }
                    return true
                }
                "reload" -> {
                    readConfig(sender)
                    return true
                }
                else -> {
                    if(idReg matches args[0]) {
                        if(args.size == 1) {
                            banList[args[0]] = BanOption(true)
                            sender.sendMessage("[${args[0]}] set to ${banList[args[0]]!!.str}")
                            saveBanList()
                        }else if(args.size >= 2) {
                            var pickup = false
                            var remove = false
                            var create = false
                            if(banList.containsKey(args[0])) {
                                pickup = banList[args[0]]!!.pickup
                                remove = banList[args[0]]!!.remove
                                create = banList[args[0]]!!.create
                            }
                            for (i in 1..(args.size-1)){
                                when(args[i]) {
                                    "all" -> {
                                        pickup = true
                                        remove = true
                                        create = true
                                    }
                                    "!", "!all" -> {
                                        pickup = false
                                        remove = false
                                        create = false
                                    }
                                    "pickup" -> pickup = true
                                    "remove" -> remove = true
                                    "create" -> create = true
                                    "!pickup" -> pickup = false
                                    "!remove" -> remove = false
                                    "!create" -> create = false
                                    else -> sender.sendMessage("unknow keyword [${args[i]}]")
                                }
                            }
                            banList[args[0]] = BanOption(pickup, remove, create)
                            sender.sendMessage("[${args[0]}] set to ${banList[args[0]]!!.str}")
                            saveBanList()
                        }
                    } else {
                        sender.sendMessage("${args[0]} is not a id")
                        return false
                    }
                }
            }
        } else showHelp()
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String> {
        return if(args.size == 1) mutableListOf("help", "list", "reload")
        else if(args.size > 1) mutableListOf("all", "pickup", "remove", "create", "!", "!all", "!pickup", "!remove", "!create")
        else mutableListOf()
    }

    val banList = mutableMapOf<String, BanOption>()

    private val listener = object : Listener {

        @EventHandler
        fun onItemSpawn(event: ItemSpawnEvent) {
            val item = event.entity
            val nbt = NBTEntity(item)
            val id = nbt.getCompound("Item").getString("id")
            val ban = banList[id] ?: return
            if(ban.create || ban.remove) {
                event.isCancelled = true
            }
            if(ban.remove) {
                item.remove()
            }
        }

        @EventHandler
        fun onEntityPickupItem(event: EntityPickupItemEvent) {
            val item = event.item
            val nbt = NBTEntity(item)
            val id = nbt.getCompound("Item").getString("id")

            val ban = banList[id] ?: return
            if(ban.pickup || ban.remove) {
                event.isCancelled = true
            }
            if(ban.remove) {
                item.remove()
            }
        }

        @EventHandler
        fun onItemMerge(event: ItemMergeEvent) {
            val item = event.entity
            val nbt = NBTEntity(item)
            val id = nbt.getCompound("Item").getString("id")
            val ban = banList[id] ?: return
            if(ban.remove) {
                event.isCancelled = true
                item.remove()
                event.target.remove()
            }
        }
    }
}

data class BanOption(val pickup: Boolean, val remove: Boolean, val create: Boolean) {
    constructor(all: Boolean): this(all, all, all)
    val isAll get() = pickup && remove && create
    val isNone get() = !pickup && !remove && !create
    val str get() = if(isAll) "all" else {
        val list = mutableListOf<String>()
        if(pickup) list.add("pickup")
        if(remove) list.add("remove")
        if(create) list.add("create")
        if(list.isEmpty()) "none"
        else list.joinToString(" ")
    }
}