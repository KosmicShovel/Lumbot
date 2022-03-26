package slaynash.lum.bot.discord.commands;

import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.gcardone.junidecode.Junidecode;
import slaynash.lum.bot.discord.Command;

public class DumpID extends Command {
    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!includeInHelp(event))
            return;
        String[] parts = paramString.split(" ", 2);
        if (parts.length < 2) {
            event.getMessage().reply("Usage: " + getName() + " <Regex>").queue();
            return;
        }
        String regex = Junidecode.unidecode(parts[1]).toLowerCase();
        List<Member> members = new ArrayList<>();
        event.getGuild().loadMembers(m -> {
            if (Junidecode.unidecode(m.getEffectiveName()).toLowerCase().matches(regex))
                members.add(m);
        });
        if (members.size() == 0) {
            event.getMessage().reply("No users found.").queue();
            return;
        }
        members.sort((m1, m2) -> m1.getUser().getId().compareTo(m2.getUser().getId()));
        StringBuilder sb = new StringBuilder();
        for (Member m : members) {
            sb.append(m.getUser().getId()).append(" ").append(m.getEffectiveName()).append("\n");
        }
        event.getMessage().reply(sb.toString().getBytes(), event.getGuild().getName() + " " + regex + ".txt").queue();
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.startsWith(getName());
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return event.getMember().hasPermission(Permission.MESSAGE_MANAGE);
    }

    @Override
    public String getHelpDescription() {
        return "Dump all user IDs in the server that match a regex";
    }

    @Override
    public String getName() {
        return "l!dumpid";
    }
}