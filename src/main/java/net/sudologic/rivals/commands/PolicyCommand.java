package net.sudologic.rivals.commands;

import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import net.sudologic.rivals.Faction;
import net.sudologic.rivals.Rivals;
import net.sudologic.rivals.util.Policy;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static net.sudologic.rivals.util.Policy.PolicyType.unintervention;
import static net.sudologic.rivals.util.Policy.PolicyType.unsanction;

public class PolicyCommand implements CommandExecutor {

    private static final Set<String> ALLOWED_SETTINGS = new HashSet<>(Arrays.asList(
            "warDelay",
            "nowWarPower",
            "votePassRatio",
            "votePassTime",
            "minVotes",
            "minShopPower",
            "killEntityPower",
            "killAllyPower",
            "killEnemyPower",
            "killNeutralPower",
            "deathPowerLoss",
            "tradePower",
            "defaultPower",
            "killMonsterPower"
    ));

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        //policy propose <type> <arg1> <arg2>
        //policy vote <id>
        //policy get <sel1> <sel2>
        if(commandSender instanceof Player) {
            Player p = (Player) commandSender;
            if(args.length < 1) {
                if(args.length < 1) {
                    commandSender.sendMessage("[Rivals] Usage: /policy <propose|vote|get|proposals|help> [arguments]\n" +
                            "For detailed instructions on each subcommand, use /policy help");
                    return true;
                }
            }
            if(args[0].equals("propose")) {
                if(args.length < 3) {
                    commandSender.sendMessage("[Rivals] Usage: /policy propose <type> <arg1> <arg2>...");
                    return true;
                }
                Faction f = Rivals.getFactionManager().getFactionByPlayer(p.getUniqueId());
                if(f == null) {
                    commandSender.sendMessage("[Rivals] You must be in a faction to propose a policy.");
                    return true;
                }
                Policy.PolicyType type;
                try {
                    type = Policy.PolicyType.valueOf(args[1].toLowerCase());
                } catch(IllegalArgumentException e) {
                    commandSender.sendMessage("[Rivals] No policy type with that name");
                    return true;
                }
                Policy policy = null;
                switch (type) {
                    case denounce, sanction, intervention, custodian, amnesty -> {
                        if(args.length < 4) {
                            commandSender.sendMessage("[Rivals] Usage: /policy propose " + type.name() + " <faction> <hours>");
                            return true;
                        }
                        Faction target = Rivals.getFactionManager().getFactionByName(args[2]);
                        Integer time = Ints.tryParse(args[3]);
                        if(target != null && time != null)
                            policy = Rivals.getPoliticsManager().propose(new Policy(type, target.getID(), time, f.getID()));
                        else {
                            commandSender.sendMessage("[Rivals] Usage: /policy propose " + type.name() + " <faction> <hours>");
                            return true;
                        }
                    }
                    case budget -> {
                        Float budget = Floats.tryParse(args[2]) / 100;
                        if(budget > 100 || budget < 0) {
                            commandSender.sendMessage("[Rivals] Budget must be between 0 and 100.");
                            return true;
                        }
                        if(budget != null)
                            policy = Rivals.getPoliticsManager().propose(new Policy(type, budget, null, f.getID()));
                        else {
                            commandSender.sendMessage("[Rivals] Usage: /policy propose budget <percentage>");
                            return true;
                        }
                    }
                    case mandate -> {
                        policy = Rivals.getPoliticsManager().propose(new Policy(type, args[2], null, f.getID()));
                    }
                    case setting -> {
                        if(args.length < 4) {
                            commandSender.sendMessage("[Rivals] Usage: /policy propose setting <setting> <value>");
                            return true;
                        }
                        if(!ALLOWED_SETTINGS.contains(args[2])) {
                            commandSender.sendMessage("[Rivals] Proposals for this setting are not permitted.");
                            return true;
                        }
                        if(!Rivals.validSetting(args[2], args[3])) {
                            commandSender.sendMessage("[Rivals] Not a valid value for that setting.");
                            return true;
                        }
                        policy = Rivals.getPoliticsManager().propose(new Policy(type, args[2], args[3], f.getID()));
                    }
                    case unsanction, unintervention -> {
                        Faction target = Rivals.getFactionManager().getFactionByName(args[2]);
                        if(type == unsanction && !Rivals.getPoliticsManager().getSanctionedFactions().containsKey(target.getID())) {
                            commandSender.sendMessage("[Rivals] That faction is not currently sanctioned.");
                            return true;
                        }
                        if(type == unintervention && !Rivals.getPoliticsManager().getInterventionFactions().containsKey(target.getID())) {
                            commandSender.sendMessage("[Rivals] There is no intervention against that faction.");
                            return true;
                        }
                        if(target != null) {
                            policy = Rivals.getPoliticsManager().propose(new Policy(type, target.getID(), null, f.getID()));
                        } else {
                            commandSender.sendMessage("[Rivals] Usage: /policy propose " + type.name() + " <faction>");
                            return true;
                        }
                    }
                    default -> {
                        commandSender.sendMessage("[Rivals] Provide a subcommand.");
                        return true;
                    }
                }
                if(policy == null) {
                    commandSender.sendMessage("[Rivals] Failed to propose policy. Try again later.");
                    return true;
                }
                commandSender.sendMessage("[Rivals] Proposed resolution. Vote on it with /policy vote " + policy.getId());
                commandSender.sendMessage(describePolicy(policy));
                return true;

            } else if(args[0].equals("vote")) {
                if(args.length < 4) {
                    commandSender.sendMessage("[Rivals] Usage /policy vote <proposal-id> <yay|nay> <influence>");
                    return true;
                }
                Integer id = Ints.tryParse(args[1]);
                if(id == null || !Rivals.getPoliticsManager().getProposed().keySet().contains(id)) {
                    commandSender.sendMessage("[Rivals] No resolution with that id.");
                    return true;
                }
                Policy policy = Rivals.getPoliticsManager().getProposed().get(id);
                Integer amount = Ints.tryParse(args[3]);
                Faction f = Rivals.getFactionManager().getFactionByPlayer(p.getUniqueId());
                boolean yay = true;
                if(args[2].equalsIgnoreCase("nay") || args[2].equalsIgnoreCase("no")) {
                    yay = false;
                }
                if(f == null) {
                    commandSender.sendMessage("[Rivals] You must be in a faction to vote on a policy.");
                    return true;
                }
                if(amount == null) {
                    commandSender.sendMessage("[Rivals] Usage /policy vote <proposal-id> <yay|nay> <influence>");
                    return true;
                }
                if(amount > f.getInfluence() || amount < 0) {
                    commandSender.sendMessage("[Rivals] Your faction only has " + f.getInfluence() + " influence.");
                    return true;
                }
                if(yay = false) {
                    amount = -amount;
                }
                policy.vote(f.getID(), amount);
                f.remInfluence(Math.abs(amount));
                String vote = yay ? "yay" : "nay";
                commandSender.sendMessage("[Rivals] Voted " + vote + " on resolution " + policy.getId() + " with " + Math.abs(amount) + " influence.");
                return true;
            }
            else if(args[0].equals("proposals")) {
                listProposals((Player) commandSender);
                return true;
            }
            else if(args[0].equals("get")) {
                if(args.length < 2) {
                    commandSender.sendMessage("[Rivals] Usage: /policy get <setting | proposal-id>");
                    return true;
                }
                if(args[1].equals("setting") && args.length < 3) {
                    commandSender.sendMessage("[Rivals] Usage: /policy get setting <setting>");
                    return true;
                }
                try {//see if user passed a proposal id, if so describe that proposal
                    if (Integer.parseInt(args[1]) != 0) {
                        Policy policy = Rivals.getPoliticsManager().getProposed().get(Integer.parseInt(args[1]));
                        if (policy == null) {
                            commandSender.sendMessage("[Rivals] No proposal with that id.");
                            return true;
                        }
                        commandSender.sendMessage(describePolicy(policy));
                        return true;
                    }
                } catch (NumberFormatException e) {//otherwise see if user passed a setting name
                    Rivals.getPoliticsManager().displayPolicy(args, (Player) commandSender);
                    return true;
                }
            } else if(args[0].equals("help")) {
                commandSender.sendMessage("[Rivals] Policy Command Help:\n" +
                        "- /policy propose <type> <arg1> <arg2>...: Propose a new policy.\n" +
                        "- /policy vote <id> <yay|nay> <influence>: Vote on a proposed policy.\n" +
                        "- /policy get <setting | proposal-id>: Get details on a specific policy, setting, or proposal\n" +
                        "- /policy proposals: List all current proposals\n" +
                        "Types include: denounce, sanction, intervention, custodian, budget, mandate, setting, unsanction.");
                return true;
            }
        }
        return false;
    }

    public void listProposals(Player p) {
        p.sendMessage("[Rivals] Current proposals:");
        for(Policy policy : Rivals.getPoliticsManager().getProposed().values()) {
            p.sendMessage(describePolicy(policy));
        }
    }

    public String describePolicy(Policy p) {
        String s = "" + p.getId() + ": " + (p.getSupport()) + " support | Proposed by " + Rivals.getFactionManager().getFactionByID(p.getProposedBy()).getColor() + Rivals.getFactionManager().getFactionByID(p.getProposedBy()).getName() + ChatColor.RESET + " | " + (p.getTimeLeft() / 3600000) + " hours remaining\n";
        switch (p.getType()) {
            case denounce -> s += "Denounce Faction " + p.getTargetFaction().getColor() + p.getTargetFaction().getName() + ChatColor.RESET + " for " + p.getTime() + " hours";
            case sanction -> s += "Sanction Faction " + p.getTargetFaction().getColor() + p.getTargetFaction().getName() + ChatColor.RESET +  " for " + p.getTime() + " hours";
            case intervention -> s += "Intervene against Faction " + p.getTargetFaction().getColor() + p.getTargetFaction().getName() + ChatColor.RESET +  " for " + p.getTime() + " hours";
            case unintervention -> s += "End Intervention against Faction " + p.getTargetFaction().getColor() + p.getTargetFaction().getName() + ChatColor.RESET;
            case custodian -> s += "Nominate Custodian " + p.getTargetFaction().getColor() + p.getTargetFaction().getName() + ChatColor.RESET + " for " + p.getTime() + " hours";
            case unsanction -> s += "Unsanction Faction " + p.getTargetFaction().getColor() + p.getTargetFaction().getName() + ChatColor.RESET;
            case setting -> s += "Set " + p.getSettingName() + " to " + p.getSettingValue();
            case budget -> s += "Set Custodian budget to " + (p.getBudget() * 100) + "%";
            case mandate -> s += "Set Custodian mandate to " + p.getMandate();
            case amnesty -> s += "Amnesty for Faction " + p.getTargetFaction().getColor() + p.getTargetFaction().getName() + ChatColor.RESET + " for " + p.getTime() + " hours";
        }
        return s;
    }
}
