# BanItemEntity
Just do one thing, ban entity form of item

## Commands
- `/ban-item-entity help`  
    Show help
- `/ban-item-entity reload`  
    Reload ban list
- `/ban-item-entity list`  
    Show all banned items
- `/ban-item-entity <namespace:id>`  
    `/ban-item-entity <namespace:id> all`  
    Ban pickup remove and create for `<namespace:id>`
- `/ban-item-entity <namespace:id> [pickup | remove | create ...]`  
    Order independent
- `/ban-item-entity <namespace:id> !`
- `/ban-item-entity <namespace:id> !all`
- `/ban-item-entity <namespace:id> [!pickup | !remove | !create ...]`  
    Unban

## Config
```yml
ban_list:
  # the item or block id
  minecraft:tnt:
    # set true to let player cant pick up this item
    pickup: true
    # set true to remove / kill the item entity
    remove: true
    # set true to let this item entity cant be create / summon
    create: true
  # set true to ban all
  minecraft:tnt_minecart: true
```