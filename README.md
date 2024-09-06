Rivals intends to address the common shortcomings of traditional Minecraft factions by promoting balance and variety in gameplay. The plugin aims to level the playing field between large and small factions, ensuring solo players can still thrive through strategic choices. It supports diverse playstyles, allowing players to focus on commerce, politics, or combat as they see fit. By introducing a political system, Rivals gives players a voice in shaping the game's balance, while resource-rich territories provide clear incentives to capture and defend land, making every piece of land truly valuable.

This plugin is still under active development, which means the default configuration is somewhat unbalanced. We are open to feedback on all aspects of the plugin.

## Features
1. **Factions**
   - Players can create factions with custom names and colors.
   - Factions can invite new players to join
   - Factions can claim land
   - Factions can declare war, make peace, and form alliances
   - Factions can create shops
   - Factions can gain or lose power in combat with players and mobs, or through commerce
   - Power changes follow a curve so that it changes slowly once a high power is achieved
   - Power changes more slowly for factions with many members
   - Power is converted to influence over time, which is useful in politics
   - Help/Info command
2. **Shops**
   - Implemented with Shopkeepers plugin API
   - Players can edit their faction's shop with a command
3. **Land Claims**
   - Factions can claim land based on how much power they have.
   - Claimed land can only be edited by Faction members
   - Factions at war can steal land from each other using power.
   - Land claims can be visualized using a command.
   - Faction claim strength is spread evenly across all chunks.
        The more chunks a faction claims, the weaker their claims are
        The more chunks you take from a faction, the harder it is to take more
4. **Homes**
   - Factions may set homes to teleport between.
   - Number of homes scales with faction power.
   - Players may not use homes while in combat.
5. **Scoreboard**
   - Displayed on right side of screen to show current info on your faction
6. **War Declarations**
   - War declarations have a time delay by default.
   - Factions can skip the delay in exchange for a power penalty.
7. **Resource Chunks**
   - Produce items at certain intervals
   - Item type and quantity are random
   - Chance of producing items decays over time
   - When a chunk stops producing resources, it is reset and moved to a new random location
   - Resource types are somewhat biome-specific
   - `/rivals resource` command to list all resource chunks, with options to filter by item type and distance
8. **Politics**
   - Factions can vote on resolutions to buff/debuff certain factions or adjust game rules
   - Spend influence for a stronger vote
   - Proposal types:
        - Denounce: Condemn a faction's actions temporarily
        - Sanction: Reduces a faction's power temporarily
        - Unsanction: Removes sanction on a faction
        - Intervention: Faction may be attacked by all non-allied 
        - Setting: Modify a game balancing setting.
        - Custodian: Declares a faction to be the custodian for a set time
        - Budget: Sets the amount of influence the custodian taxes from the other factions
        - Mandate: Sets the Custodian's official goal
        - Amnesty: Prevents a faction from recieving WarMongering debuffs temporarily
9. **WarMongering**
   - A per-player AND per-faction stat which tracks violence
   - Debuffs are applied as WarMongering increases
   - Value decreases gradually over time
   - Factions with Amnesty do not gain WarMongering, intended to deter factions from starting wars of aggression
