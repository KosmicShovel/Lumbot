package slaynash.lum.bot.discord;

import java.awt.Color;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ServerMessagesHandler {

    private static Map<Long, long[]> whitelistedRolesServers = new HashMap<>() {{
        put(439093693769711616L /* VRCMG */, new long[] {
                631581319670923274L /* Staff */,
                662720231591903243L /* Helper */,
                825266051277258754L /* cutie */,
                673725166626406428L /* Modder */ });
        put(600298024425619456L /* emmVRC */, new long[] {
                748392927365169233L /* Admin */,
                653722281864069133L /* Helper */ });
        put(663449315876012052L /* MelonLoader */, new long[] {
                663450403194798140L /* Lava Gang */,
                663450567015792670L /* Administrator */,
                663450611253248002L /* Moderator */,
                663450655775522843L /* Modder */ });
    }};

    private static String[] alreadyHelpedSentences = new String[] {
        "I already answered you <:konataCry:553690186022649858>",
        "Why won't you read my answer <:angry:835647632843866122>",
        "There's already the answer up here!! <:cirHappy:829458722634858496>"
    };

    private static String[] alreadyHelpedSentencesRare = new String[] {
        "I wish I wasn't doing this job sometimes <:02Dead:835648208272883712>",
        "https://cdn.discordapp.com/attachments/657545944136417280/836231859998031932/unknown.png"
    };
    
    private static String[] thankedSentences = new String[] {
        "You're Welcome <:EmmyLove:603759032284741664>",
        "<:cirHappy:829458722634858496>"
    };

    private static String[] thankedSentencesRare = new String[] {
        "Notices you senpai <:cirHappy:829458722634858496>",
        "https://tenor.com/view/barrack-obama-youre-welcome-welcome-gif-12542858"
    };

    private static Random random = new Random();

    public static void handle(MessageReceivedEvent event) {
        System.out.printf("[%s] [%s][%s] %s: %s\n",
                TimeManager.getTimeForLog(),
                event.getGuild().getName(),
                event.getTextChannel().getName(),
                event.getAuthor().getName(),
                event.getMessage().getContentRaw() );

        if (!checkDllPostPermission(event)) {
            event.getMessage().delete().queue();
            event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("<@!" + event.getMessage().getMember().getId() + "> tried to post a file format that is not allowed.\nPlease only download mods from trusted sources.", Color.YELLOW)).queue();
            return;
        }
    
        CommandManager.runAsServer(event);

        
        if (event.getMessage().getContentRaw().toLowerCase().contains("[error]") || event.getMessage().getContentRaw().toLowerCase().contains("developer:")) {
            System.out.println("Log was typed");
            event.getChannel().sendMessage("Please upload your `MelonLoader/Latest.log` instead of printing parts of it.").queue();
        }
        
        if (event.getMessage().getContentRaw().toLowerCase().contains("help") || event.getMessage().getContentRaw().toLowerCase().contains("fix")) {
            System.out.println("Help was detected");
            if (MelonLoaderScanner.wasHelpedRecently(event)) {
                boolean rare = random.nextInt(10) == 9;
                String sentence;
                    if (rare) {
                        sentence = random.nextInt(1000) == 420 
                        ? "Shut the fuck up, I literally answered your dumb ass!"
                        : alreadyHelpedSentencesRare[random.nextInt(alreadyHelpedSentencesRare.length)];
                    }
                    else sentence = alreadyHelpedSentences [random.nextInt(alreadyHelpedSentences.length)];
                event.getChannel().sendMessage(sentence).queue();
            }
        }
        
        if (event.getMessage().getContentRaw().toLowerCase().contains("thank") || event.getMessage().getContentRaw().toLowerCase().contains("neat") || event.getMessage().getContentRaw().toLowerCase().contains("cool")  || event.getMessage().getContentRaw().toLowerCase().contains("nice")) {
            System.out.println("Thanks was detected");
            if (MelonLoaderScanner.wasHelpedRecently(event)) {
                boolean rare = random.nextInt(10) == 9;
                String sentence = rare
                    ? thankedSentencesRare[random.nextInt(thankedSentencesRare.length)]
                    : thankedSentences    [random.nextInt(thankedSentences.length)];
                event.getChannel().sendMessage(sentence).queue();
            }
        }
        
        if (event.getMessage().getContentRaw().toLowerCase().contains("late")) {
            System.out.println("Late was detected");
            if (MelonLoaderScanner.wasHelpedRecently(event)) {
                event.getChannel().sendMessage("https://media.discordapp.net/attachments/807109298982354955/810352859539439646/geuCSLseKf.gif").queue();
            }
        }
        
        new Thread(() -> {
            try {
                MelonLoaderScanner.scanLogs(event);
            }
            catch(Exception e) {
                e.printStackTrace();
                
                String error = "**An error has occured while reading logs:**\n" + getStackTrace(e);
                
                if (error.length() > 1000) {
                    String[] lines = error.split("\n");
                    String toSend = "";
                    int i = 0;
                    
                    while (i < lines.length) {
                        if ((toSend + lines[i] + 1).length() > 1000) {
                            toSend += "...";
                            break;
                        }
                        
                        toSend += "\n" + lines[i];
                    }
                    
                    event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed(toSend, Color.RED)).queue();
                }
                else
                    event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed(error, Color.RED)).queue();
            }
        }).start();

        new Thread(() -> {
            try {
                CrasherVideoChecker.check(event);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Check if the message is posted in a guild using a whitelist and if it contains a DLL
     * @param event
     * @return true if the message is posted in a guild using a whitelist, contains a DLL attachment, and isn't posted by a whitelisted user
     */
    private static boolean checkDllPostPermission(MessageReceivedEvent event) {
        long guildId = event.getGuild().getIdLong();
        boolean postedInWhitelistedServer = false;
        for (long whitelistedGuildId : whitelistedRolesServers.keySet()) {
            if (whitelistedGuildId == guildId) {
                postedInWhitelistedServer = true;
                break;
            }
        }
        
        if (!postedInWhitelistedServer)
            return true; // Not a whitelisted server
        
        for (Attachment attachment : event.getMessage().getAttachments()) {
            String fileExt = attachment.getFileExtension();
            if (fileExt == null) fileExt = "";
            fileExt = fileExt.toLowerCase();
            
            if (fileExt.equals("dll") || fileExt.equals("exe") || fileExt.equals("zip") || fileExt.equals("7z") || fileExt.equals("rar")) {

                for (Entry<Long, long[]> whitelistedRolesServer : whitelistedRolesServers.entrySet()) {
                    Guild targetGuild;
                    Member serverMember;
                    if ((targetGuild = event.getJDA().getGuildById(whitelistedRolesServer.getKey())) != null &&
                        (serverMember = targetGuild.getMember(event.getAuthor())) != null) {
                        
                        List<Role> roles = serverMember.getRoles();
                        for (Role role : roles) {
                            long roleId = role.getIdLong();
                            for (long whitelistedRoleId : whitelistedRolesServer.getValue())
                                if (whitelistedRoleId == roleId)
                                    return true; // The sender is whitelisted
                        }
                    }
                    
                }

                return false; // The sender isn't allowed to send a DLL file
            }
        }

        return true; // No attachement, or no DLL
    }

    private static String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        e.printStackTrace(pw);

        return sw.toString();
    }
}
