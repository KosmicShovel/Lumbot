package slaynash.lum.bot.discord.slashs;

import java.awt.Color;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.utils.ExceptionUtils;

public class Replies extends Slash {
    @Override
    protected CommandData globalSlashData() {
        return new CommandData("reply", "Custom Replies")
            .addSubcommands(new SubcommandData("list","List all current replies"))
            .addSubcommands(new SubcommandData("add","Add or Update reply") // Only the first 10 options are shown,
                .addOption(OptionType.INTEGER, "ukey", "Reply Key used to update existing Reply", false)
                .addOption(OptionType.STRING , "message", "Enter Message to send on trigger", false)
                .addOption(OptionType.STRING , "regex", "Use regex matching (regex needs to match all of user's message)", false)
                .addOption(OptionType.STRING , "contains", "Term that message needs to contain", false)
                .addOption(OptionType.STRING , "equals", "Term that message needs to equal", false)
                .addOption(OptionType.USER   , "user", "Trigger if someone sends a message", false)
                .addOption(OptionType.BOOLEAN, "delete", "Should User's message be deleted?", false)
                .addOption(OptionType.BOOLEAN, "kick", "Should the User be kicked?", false)
                .addOption(OptionType.BOOLEAN, "ban", "Should the User be banned?", false)
                .addOption(OptionType.BOOLEAN, "bot", "Allow replying to other bots", false)
                .addOption(OptionType.BOOLEAN, "edit", "Allow replying to when member edits their message", false)
                .addOption(OptionType.CHANNEL, "channel", "Allow reply in only a single channel", false) //todo Maybe later
                .addOption(OptionType.ROLE   , "ignorerole", "Prevent triggering if user has role", false) //todo Maybe later
                // .addOption(OptionType.INTEGER, "repeat", "Trigger if repeated command", false) //todo Maybe later
            )
            .addSubcommands(new SubcommandData("delete","Remove a reply")
                .addOption(OptionType.INTEGER, "ukey", "Reply Key used to delete existing Reply", true))
            .setDefaultEnabled(false);
    }

    @Override
    public void slashRun(SlashCommandEvent event) {
        System.out.println("Options size: " + event.getOptions().size());
        if (event.getChannelType() == ChannelType.PRIVATE) {
            event.reply("Replies currently does not work in DMs").queue();
            return;
        }
        if (event.getSubcommandName() == null) { // Don't think this is needed but won't hurt to have
            event.reply("Please specify a subcommand").queue();
            return;
        }
        boolean delete = false;
        if (event.getOption("delete") != null && event.getOption("delete").getAsBoolean()) {
            delete = true;
            if (!event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_MANAGE)) {
                event.reply("I don't have the `MESSAGE_MANAGE` permission in this channel. I won't be able to delete the triggered message.").queue();
                return;
            }
        }
        boolean kick = false;
        if (event.getOption("kick") != null && event.getOption("kick").getAsBoolean()) {
            kick = true;
            if (!event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.KICK_MEMBERS)) {
                event.reply("I don't have the `KICK_MEMBERS` permission in this channel. I won't be able to kick the user.").queue();
                return;
            }
        }
        boolean ban = false;
        if (event.getOption("ban") != null && event.getOption("ban").getAsBoolean()) {
            ban = true;
            if (!event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.BAN_MEMBERS)) {
                event.reply("I don't have the `BAN_MEMBERS` permission in this channel. I won't be able to ban the user.").queue();
                return;
            }
        }
        boolean bot = event.getOption("bot") != null && event.getOption("bot").getAsBoolean();
        boolean edit = event.getOption("edit") != null && event.getOption("edit").getAsBoolean();

        if (event.getSubcommandName().equals("list")) {
            int c = 0;
            StringBuilder sb = new StringBuilder();
            try {
                String sql = "SELECT * FROM `Replies` WHERE `guildID` = '" + event.getGuild().getId() + "' ORDER BY `ukey` ASC";
                ResultSet rs = DBConnectionManagerLum.sendRequest(sql);
                sb.append("Current replies in this guild:\n");

                while (rs.next()) {
                    sb.append("Reply ukey: ").append(rs.getInt("ukey"));
                    sb.append(rs.getBoolean("bdelete") ? "\tdelete " : "");
                    sb.append(rs.getBoolean("bkick") ? "\tkick " : "");
                    sb.append(rs.getBoolean("bban") ? "\tban" : "");
                    sb.append(rs.getBoolean("bbot") ? "\tbot" : "");
                    sb.append(rs.getBoolean("bedit") ? "\tedit" : "");
                    sb.append("\n");
                    if (rs.getString("regex") != null) {
                        sb.append("\tRegex: ").append(rs.getString("regex")).append("\n");
                    }
                    if (rs.getString("contains") != null) {
                        sb.append("\tContains: ").append(rs.getString("contains")).append("\n");
                    }
                    if (rs.getString("equals") != null) {
                        sb.append("\tEquals: ").append(rs.getString("equals")).append("\n");
                    }
                    if (rs.getString("user") != null) {
                        sb.append("\tUser: ").append(event.getJDA().getUserById(rs.getString("user")).getAsTag()).append("\n");
                    }
                    if (rs.getString("channel") != null) {
                        sb.append("\tChannel: ").append(event.getJDA().getTextChannelById(rs.getString("channel")).getName()).append("\n");
                    }
                    if (rs.getString("ignorerole") != null) {
                        sb.append("\tIgnored Role: ").append(event.getJDA().getRoleById(rs.getString("ignorerole")).getName()).append("\n");
                    }
                    if (rs.getString("message") != null) {
                        sb.append("\tMessage: ").append(rs.getString("message")).append("\n");
                    }
                    c++;
                }
                DBConnectionManagerLum.closeRequest(rs);
            } catch (SQLException e) {
                ExceptionUtils.reportException("Failed to get reply to List", e, event.getTextChannel());
                return;
            }

            if (c == 0) {
                event.reply("No replies in this guild").queue();
            }
            else {
                if (event.getGuild().getSelfMember().hasPermission(event.getTextChannel(),Permission.MESSAGE_ATTACH_FILES))
                    event.reply("").addFile(sb.toString().getBytes(), event.getGuild().getName() + " replies.txt").queue();
                else
                    event.reply("I cant send logs into this channel. Please give me MESSAGE_ATTACH_FILES perms and try again.").queue();
            }
        } else if (event.getSubcommandName().equals("add")) {
            if ((event.getOption("ukey") == null && event.getOptions().size() == 0) || (event.getOption("ukey") != null && event.getOptions().size() == 1)) {
                event.reply("Please set at least one option").queue();
                return;
            }
            if (event.getOption("regex") != null) {
                try {
                    Pattern.compile(event.getOption("regex").getAsString());
                }
                catch (Exception e) {
                    event.replyEmbeds(new EmbedBuilder().setTitle("Invalid Regex!").setDescription("Please use a site like [regex101](https://regex101.com/) to test regex").setColor(Color.RED).build()).queue();
                    return;
                }
            }
            if (event.getOption("message") != null) {
                OptionMapping messageop = event.getOption("message");
                if (messageop.getAsString().length() > Message.MAX_CONTENT_LENGTH) {
                    event.reply("Message is too long. Please use a shorter message.").queue();
                    return;
                }
                Pattern p = Pattern.compile("<a?:\\w+:(?<id>\\d+)>", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(messageop.getAsString());
                while (m.find()) {
                    Emote emote = event.getJDA().getEmoteById(m.group("id"));
                    try {
                        if (!emote.canInteract(emote.getGuild().getSelfMember())) {
                            event.reply("Lum can not use that emote.").queue();
                            return;
                        }
                    } catch (Exception e) {
                        event.reply("Lum can not use that emote as I also need to be in that emote's server.").queue();
                        return;
                    }
                }
            }

            if (event.getOption("ukey") == null) {
                if (event.getOption("regex") == null && event.getOption("contains") == null && event.getOption("equals") == null)
                    event.getTextChannel().sendMessage("This will trigger on every message, I hope you know what you are going").queue();
                try {
                    DBConnectionManagerLum.sendUpdate("INSERT INTO `Replies` (`guildID`, `regex`, `contains`, `equals`, `message`, `user`, `channel`, `ignorerole`, `bdelete`, `bkick`, `bban`, `bbot`, `bedit`, `lastedited`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    event.getGuild().getIdLong(),
                    event.getOption("regex") == null ? null : event.getOption("regex").getAsString(),
                    event.getOption("contains") == null ? null : event.getOption("contains").getAsString(),
                    event.getOption("equals") == null ? null : event.getOption("equals").getAsString(),
                    event.getOption("message") == null ? null : event.getOption("message").getAsString(),
                    event.getOption("user") == null ? null : event.getOption("user").getAsUser().getId(),
                    event.getOption("channel") == null ? null : event.getOption("channel").getAsMessageChannel().getId(),
                    event.getOption("ignorerole") == null ? null : event.getOption("ignorerole").getAsRole().getId(),
                    delete, kick, ban, bot, edit, event.getUser().getIdLong());

                    ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT `AUTO_INCREMENT` FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Replies'");
                    rs.next();
                    int ukey = rs.getInt("AUTO_INCREMENT") - 1; // pretty bad way to get the ukey

                    event.reply("Added reply, ukey: " + ukey).queue();

                    DBConnectionManagerLum.closeRequest(rs);
                } catch (SQLException e) {
                    ExceptionUtils.reportException("Failed to Add reply", e, event.getTextChannel());
                }
            }
            else {
                long ukey = event.getOption("ukey").getAsLong();
                try {
                    ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT `Replies`.`ukey` FROM `Replies` WHERE `Replies`.`ukey` = ?", ukey);
                    if (!rs.next()) {
                        event.reply("Invalid ukey or reply not found!").setEphemeral(true).queue();
                        DBConnectionManagerLum.closeRequest(rs);
                        return;
                    }
                    DBConnectionManagerLum.closeRequest(rs);
                } catch (SQLException e) {
                    ExceptionUtils.reportException("Failed to check for reply", e, event.getTextChannel());
                    return;
                }

                try { // not sure how to combine them so updating one at a time
                    DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set lastedited = ? WHERE `Replies`.`ukey` = ?", event.getUser().getIdLong(), ukey);
                    if (event.getOption("message") != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set message = ? WHERE `Replies`.`ukey` = ?", event.getOption("message").getAsString(), ukey);
                    if (event.getOption("regex") != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set regex = ? WHERE `Replies`.`ukey` = ?", event.getOption("regex").getAsString(), ukey);
                    if (event.getOption("contains") != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set contains = ? WHERE `Replies`.`ukey` = ?", event.getOption("contains").getAsString(), ukey);
                    if (event.getOption("equals") != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set equals = ? WHERE `Replies`.`ukey` = ?", event.getOption("equals").getAsString(), ukey);
                    if (event.getOption("user") != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set user = ? WHERE `Replies`.`ukey` = ?", event.getOption("user").getAsString(), ukey);
                    if (event.getOption("channel") != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set channel = ? WHERE `Replies`.`ukey` = ?", event.getOption("channel").getAsString(), ukey);
                    if (event.getOption("ignorerole") != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set ignorerole = ? WHERE `Replies`.`ukey` = ?", event.getOption("ignorerole").getAsString(), ukey);
                    if (event.getOption("delete") != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set bdelete = ? WHERE `Replies`.`ukey` = ?", event.getOption("delete").getAsBoolean(), ukey);
                    if (event.getOption("kick") != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set bkick = ? WHERE `Replies`.`ukey` = ?", event.getOption("kick").getAsBoolean(), ukey);
                    if (event.getOption("ban") != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set bban = ? WHERE `Replies`.`ukey` = ?", event.getOption("ban").getAsBoolean(), ukey);
                    if (event.getOption("bot") != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set bbot = ? WHERE `Replies`.`ukey` = ?", event.getOption("bot").getAsBoolean(), ukey);
                    if (event.getOption("edit") != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set bedit = ? WHERE `Replies`.`ukey` = ?", event.getOption("edit").getAsBoolean(), ukey);
                    event.reply("Reply updated!").queue();
                } catch (SQLException e) {
                    ExceptionUtils.reportException("Failed to Update reply", e, event.getTextChannel());
                }
            }
        } else if (event.getSubcommandName().equals("delete")) {
            int deleted;
            long ukey = event.getOption("ukey").getAsLong();
            try {
                String update = "DELETE FROM `Replies` WHERE `guildID` = '" + event.getGuild().getId() + "' AND `ukey` = '" + ukey + "'";
                deleted = DBConnectionManagerLum.sendUpdate(update);
            } catch (SQLException e) {
                ExceptionUtils.reportException("Failed to Delete reply", e, event.getTextChannel());
                return;
            }
            if (deleted == 0) {
                event.reply("No reply found with that trigger").queue();
            }
            else {
                event.reply("Deleted reply").queue();
            }

        } else {
            event.reply("Unknown subcommand").queue();
        }
    }
}